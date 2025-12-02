package com.example.smartair;

import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.example.smartair.Constants.*;

public class SOSSecondaryActivity extends AppCompatActivity {

    private EditText editRescueAttempts, editCurrentPEF;
    private FirebaseFirestore db;
    private String childUid, parentUid;
    private String childName;


    private double mChildPB = 1.0; // default

    private CountDownTimer countDownTimer;

    private boolean isFirstCheck = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sos_secondary);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        childUid = getIntent().getStringExtra(CHILD_UID);
        parentUid = getIntent().getStringExtra(PARENT_UID);
        //new
        childName = getIntent().getStringExtra(CHILD_NAME);


        if (childUid == null || parentUid == null) {
            Toast.makeText(this, "Missing child/parent data.", Toast.LENGTH_SHORT).show();
        }

        editRescueAttempts = findViewById(R.id.editRescueAttempts);
        editCurrentPEF = findViewById(R.id.editCurrentPEF);

        Button btnSubmit = findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(v -> handleSubmit());
    }

    /**
     * Validates user input, converts to integers,
     * then uploads the triage log to Firestore.
     * Return to Child Dashboard when success.
     */
    private void handleSubmit() {
        String attemptsStr = editRescueAttempts.getText().toString().trim();
        String pefStr = editCurrentPEF.getText().toString().trim();

        // Check if fields empty
        if (attemptsStr.isEmpty() || pefStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.sos_missing_secondary), Toast.LENGTH_SHORT).show();
            return;
        }

        int attempts;
        int currentPEF;

        // Ensure number format is valid
        try {
            attempts = Integer.parseInt(attemptsStr);
            currentPEF = Integer.parseInt(pefStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Calculating zone...", Toast.LENGTH_SHORT).show();

        db.collection("children").document(childUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // get PersonBest
                        Double pb = documentSnapshot.getDouble("childPB");
                        mChildPB = (pb == null || pb == 0) ? 1.0 : pb;

                        isFirstCheck = true;

                        processTriage(attempts, currentPEF);
                    } else {
                        Toast.makeText(this, "Error: Child profile not found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void processTriage(int attempts, int currentPEF) {
        String zone = calculateZone(currentPEF, mChildPB);

        // save log
        saveLogToFirestore(attempts, currentPEF, zone);

        if ("Red".equals(zone) || "Yellow".equals(zone)) {
            // NEW: Parent Alert when recheck PEF is Yellow or Red
            ParentAlertHelper.createParentAlertInFirestore(
                    parentUid,
                    childUid,
                    childName,
                    "SOS recheck PEF is in the " + zone + " zone."
            );
        }


        if ("Green".equals(zone) && !isFirstCheck) {
            showGreenZoneDialog();
            return;
        }

        isFirstCheck = false;

        // 获取建议并开启倒计时
        fetchActionPlanAndShowDialog(zone);
    }

    // zone logic
    private String calculateZone(int current, double best) {
        double ratio = (double) current / best;

        if (ratio >= 0.8) {
            return "Green";
        } else if (ratio >= 0.5) {
            return "Yellow";
        } else {
            return "Red";
        }
    }

    private void fetchActionPlanAndShowDialog(String zone) {
        db.collection("users").document(parentUid)
                .collection("settings").document("action_plan")
                .get()
                .addOnSuccessListener(planDoc -> {
                    String actionText = "";
                    if (planDoc.exists()) {
                        if ("Green".equals(zone)) actionText = planDoc.getString("greenZone");
                        else if ("Yellow".equals(zone)) actionText = planDoc.getString("yellowZone");
                        else actionText = planDoc.getString("redZone");
                    }

                    if (actionText == null || actionText.isEmpty()) {
                        if ("Green".equals(zone)) actionText = DEFAULT_GREEN;
                        else if ("Yellow".equals(zone)) actionText = DEFAULT_YELLOW;
                        else actionText = DEFAULT_RED;
                    }

                    showActionTimerDialog(zone, actionText);
                })
                .addOnFailureListener(e -> {
                    String fallbackText = "Yellow".equals(zone) ? DEFAULT_YELLOW : ("Red".equals(zone) ? DEFAULT_RED : DEFAULT_GREEN);
                    showActionTimerDialog(zone, fallbackText);
                });
    }

    private void showActionTimerDialog(String zone, String actionText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String emoji = "Green".equals(zone) ? "🟢" : ("Yellow".equals(zone) ? "🟡" : "🔴");

        String title = emoji + " You are in the " + zone + " Zone";
        if ("Green".equals(zone)) {
            title += " (Monitoring)";
        }
        builder.setTitle(title);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_timer_action, null);
        TextView tvInstruction = dialogView.findViewById(R.id.tvInstruction);
        TextView tvTimer = dialogView.findViewById(R.id.tvTimer);

        tvInstruction.setText(actionText);
        builder.setView(dialogView);
        builder.setCancelable(false);

        builder.setNegativeButton("I feel WORSE!", (dialog, which) -> {
            if (countDownTimer != null) countDownTimer.cancel();
            triggerEscalation();
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        countDownTimer = new CountDownTimer(600000, 1000) { // 10分钟
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                tvTimer.setText(String.format(Locale.getDefault(), "Wait: %02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                if (dialog.isShowing()) dialog.dismiss();
                handleTimeUp(); // 时间到
            }
        }.start();
    }

    private void handleTimeUp() {
        try {
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500);
        } catch (Exception e) { e.printStackTrace(); }

        showRecheckDialog();
    }

    private void showRecheckDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_recheck_pef, null);
        builder.setView(view);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();

        EditText editRecheck = view.findViewById(R.id.editRecheckPEF);
        Button btnConfirm = view.findViewById(R.id.btnConfirmRecheck);

        btnConfirm.setOnClickListener(v -> {
            String valStr = editRecheck.getText().toString().trim();
            if (valStr.isEmpty()) return;

            int newPEF = Integer.parseInt(valStr);
            dialog.dismiss();

            processTriage(0, newPEF);
        });

        dialog.show();
    }

    private void showGreenZoneDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🟢 Green Zone - All Clear!")
                .setMessage("Great! Your PEF is stable. You can go back now.")
                .setPositiveButton("Go Home", (d, w) -> goHome())
                .setCancelable(false)
                .show();
    }

    private void triggerEscalation() {

        // NEW: Alert parent when child feels worse during timer
        ParentAlertHelper.createParentAlertInFirestore(
                parentUid,
                childUid,
                childName,
                childName +" pressed 'I feel WORSE!' during SOS triage timer."
        );

        new AlertDialog.Builder(this)
                .setTitle("⚠️ Emergency")
                .setMessage("Your condition is worsening. Please call 911 immediately!")
                .setPositiveButton("Call 911 Now", (d, w) -> {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:911"));
                    startActivity(intent);

                    finish();
                })
                .setNegativeButton("Cancel", null)
                .setCancelable(false)
                .show();
    }

    private void saveLogToFirestore(int attempts, int currentPEF, String zone) {
        Map<String, Object> data = new HashMap<>();
        data.put("childUid", childUid);
        data.put("parentUid", parentUid);
        data.put("timestamp", System.currentTimeMillis());
        data.put("rescueAttempts", attempts);
        data.put("currentPEF", currentPEF);
        data.put("calculatedZone", zone);
        db.collection("sosTriageLogs").add(data);
    }

    private void goHome() {
        Intent intent = new Intent(SOSSecondaryActivity.this, ChildDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(CHILD_UID, childUid);
        intent.putExtra(PARENT_UID, parentUid);
        intent.putExtra(CHILD_NAME, childName);
        startActivity(intent);
        finish();
    }
}