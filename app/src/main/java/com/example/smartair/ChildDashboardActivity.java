package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import static com.example.smartair.Constants.*;

import java.util.ArrayList;
import java.util.List;

public class ChildDashboardActivity extends AppCompatActivity {  
    private static final String TAG = "ChildDashboardActivity";

    // Firestore
    private FirebaseFirestore db;

    // Data Variables
    private String childName;
    private String childUid;
    private String parentUid;

    // UI
    private TextView tvStreak;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_child_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Firestore instance
        db = FirebaseFirestore.getInstance();

        // Get Intent
        Intent intent = getIntent();
        childUid = intent.getStringExtra(CHILD_UID);
        parentUid = intent.getStringExtra(PARENT_UID);
        childName = intent.getStringExtra(CHILD_NAME);


        if (childUid != null) {
            Log.d(TAG, "Logged in Child UID: " + childUid);
        } else {
            Log.e(TAG, "Error: CHILD_UID not received!");
            Toast.makeText(this, "Error: Child data missing", Toast.LENGTH_SHORT).show();
        }

        if (parentUid == null) {
            Log.w(TAG, "Warning: PARENT_UID missing from Intent, trying FirebaseAuth...");
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            // Try to get it from the currently logged-in user as a backup
            if (currentUser != null) {
                parentUid = currentUser.getUid();
            }
        }
        Log.d(TAG, "Parent UID: " + parentUid);

        // All Buttons
        initButtons();

        // Hi, name
        TextView hiText = findViewById(R.id.tvHiChild);
        if (childName != null && hiText != null) {
            hiText.setText("Hi, " + childName);
        }

        // Streak
        tvStreak = findViewById(R.id.tvDaysCount);

        if (tvStreak == null) {
            Log.e("STREAK", "tvDaysCount NOT FOUND!");
        } else {
            Log.d("STREAK", "tvDaysCount FOUND.");
        }

        // Load streak Data
        loadControllerStreak();

    }

    private void initButtons() {
        // --- PEF ---
        Button buttonToRecordPEF = findViewById(R.id.buttonToRecordPEF);
        buttonToRecordPEF.setOnClickListener(v -> {
            Intent intent = new Intent(ChildDashboardActivity.this, RecordPEFFeature.class);
            intent.putExtra(CHILD_UID, childUid);
            intent.putExtra(CHILD_NAME, childName);
            intent.putExtra(PARENT_UID, parentUid);
            startActivity(intent);
        });

        // --- Rescue ---
        // Control Variables
        Button buttonRescue = findViewById(R.id.buttonRescue);
        buttonRescue.setOnClickListener(view -> openEmergencyMedicationScreen());

        // --- Controller ---
        Button buttonController = findViewById(R.id.btnCheck);
        buttonController.setOnClickListener(view -> handleControllerCheck());

        // --- Check-in ---
        Button buttonCheckin = findViewById(R.id.buttonCheckin);
        buttonCheckin.setOnClickListener(v -> {
            if (childUid == null) {
                Toast.makeText(this, "Error: Child UID is missing!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(ChildDashboardActivity.this, DailyCheckInActivity.class);
            intent.putExtra(CHILD_UID, childUid);
            intent.putExtra(PARENT_UID, parentUid);
            startActivity(intent);
        });

        // --- Back ---
        ImageButton imageButtonBackChildDashboard = findViewById(R.id.imageButtonBackChildDashboard);
        imageButtonBackChildDashboard.setOnClickListener(v -> finish());

        // --- SOS ---
        Button buttonSOS = findViewById(R.id.buttonSOS);
        buttonSOS.setOnClickListener(v -> {
            //NEW: Send Parent Alert immediately when child presses SOS
            ParentAlertHelper.createParentAlertInFirestore(
                    parentUid,
                    childUid,
                    childName,
                    childName + " pressed the SOS button. Immediate attention recommended."
            );
            Intent intent = new Intent(ChildDashboardActivity.this, SOSTriageActivity.class);
            intent.putExtra(CHILD_UID, childUid);
            intent.putExtra(PARENT_UID, parentUid);
            //new
            intent.putExtra(CHILD_NAME, childName);
            startActivity(intent);
        });
    }

    // Streak section
    private void loadControllerStreak() {
        if (childUid == null || parentUid == null) {
            Toast.makeText(this, "Missing CHILD_UID or PARENT_UID. ", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("controllerLogs")
                .whereEqualTo("childUid", childUid)
                .whereEqualTo("parentUid", parentUid)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {


                    if (snap.isEmpty()) {
                        tvStreak.setText(getString(R.string.streak_start));
                        return;
                    }

                    ArrayList<Long> uniqueDays = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Long ts = d.getLong("date");
                        Log.d("STREAK", "date raw = " + ts);

                        if (ts == null) continue;

                        long dayIndex = ts / (24L * 60 * 60 * 1000);

                        if (!uniqueDays.contains(dayIndex)) {
                            uniqueDays.add(dayIndex);
                        }
                    }

                    int streak = uniqueDays.size();

                    if (streak == 0) {
                        tvStreak.setText(getString(R.string.streak_start));
                    } else if (streak == 1) {
                        tvStreak.setText(getString(R.string.streak_day1));
                    } else {
                        tvStreak.setText(getString(R.string.streak_on_a_roll, streak));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("STREAK", "Firestore ERROR: " + e.getMessage());
                    tvStreak.setText(getString(R.string.streak_start));
                });
    }

    // Rescue
    private void openEmergencyMedicationScreen() {

        if (childUid == null || parentUid == null) {
            Toast.makeText(this, "Missing CHILD_UID or PARENT_UID. ", Toast.LENGTH_SHORT).show();
            return;
        }


        db.collection("medicine")
                .whereEqualTo("parentUid", parentUid)
                .whereEqualTo("childUid", childUid)
                .whereEqualTo("medType", "Rescue")
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        Toast.makeText(this, "No rescue medication found!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Get the Rescue medicine document ID
                    String rescueMedId = snap.getDocuments().get(0).getId();

                    // Pass to EmergencyMedicationActivity
                    Intent intent = new Intent(ChildDashboardActivity.this, EmergencyMedicationActivity.class);
                    intent.putExtra(CHILD_UID, childUid);
                    intent.putExtra(PARENT_UID, parentUid);
                    intent.putExtra(CHILD_NAME, childName);
                    intent.putExtra(MEDICINE_ID, rescueMedId);
                    intent.putExtra(MED_TYPE, "Rescue");

                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load medication info." + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Controller
    private void handleControllerCheck() {
        db.collection("medicine")
                .whereEqualTo("parentUid", parentUid)
                .whereEqualTo("childUid", childUid)
                .whereEqualTo("medType", "Controller")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No controller medication found!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<DocumentSnapshot> meds = querySnapshot.getDocuments();

                    for (DocumentSnapshot med : meds) {
                        deductMedicine(med);
                    }

                    logControllerUse();
                    loadControllerStreak(); // refresh streak after check
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load medication info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deductMedicine(DocumentSnapshot document) {
        String docId = document.getId();
        String medName = document.getString("name");

        // find data
        Long currentDoseLong = document.getLong("remainingDose");
        Long dosePerUseLong = document.getLong("dosePerUse");

        int currentDose = (currentDoseLong != null) ? currentDoseLong.intValue() : 0;
        int dosePerUse = (dosePerUseLong != null) ? dosePerUseLong.intValue() : 1;
        if (dosePerUse <= 0) dosePerUse = 1;

        // check does
        if (currentDose < dosePerUse) {
            Toast.makeText(this, medName + "Not enough medicine!", Toast.LENGTH_LONG).show();
            return;
        }

        // calculate new does
        int newDose = currentDose - dosePerUse;

        // update firebase
        db.collection("medicine").document(docId)
                .update("remainingDose", newDose)
                .addOnSuccessListener(aVoid -> {
                    String msg = "Well Done!";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Network Error: Update Failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void logControllerUse() {
        long today = System.currentTimeMillis();

        db.collection("controllerLogs")
                .add(new ControllerLog(parentUid, childUid, today))
                .addOnSuccessListener(d -> Log.d(TAG, "Logged controller use"));
    }
}