package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.util.Log;
import android.widget.Button;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Calendar;

import static com.example.smartair.Constants.*;



public class ChildDashboardActivity extends AppCompatActivity {  
    private static final String TAG = "ChildDashboardActivity";

    // Data Variables
    private String childName;
    private String childUid;
    private String parentUid;
    private TextView tvDaysCount;

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

        tvDaysCount = findViewById(R.id.tvDaysCount);
        Log.d(TAG, "tvDaysCount object = " + tvDaysCount);
        loadControllerStreak();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadControllerStreak();   // Reload every time you come back to this screen
    }

    private void loadControllerStreak() {
        if (childUid == null) {
            Log.w(TAG, "childUid is null, cannot load streak");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("symptomLogs")
                .whereEqualTo("childUid", childUid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(this::handleSymptomSnapshot)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load symptom logs for streak", e);
                    updateStreakUI(0);
                });
    }

    private void handleSymptomSnapshot(QuerySnapshot snapshots) {
        int streak = 0;
        // Expected date = today (only keep the day)
        Calendar expected = Calendar.getInstance();
        truncateToDay(expected);

        for (QueryDocumentSnapshot doc : snapshots) {
            Long tsLong = doc.getLong("timestamp");
            if (tsLong == null) continue;

            Calendar record = Calendar.getInstance();
            record.setTimeInMillis(tsLong);
            truncateToDay(record);

            // If this record is the expected day → streak+1
            if (record.equals(expected)) {
                streak++;
                expected.add(Calendar.DAY_OF_YEAR, -1);
            }
            // If the record is more than one day before expected, there is a break, break the loop
            else if (record.before(expected)) {
                break;
            }
            // If the record is in the future (should not happen), ignore
        }
        updateStreakUI(streak);
    }

    private void truncateToDay(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private void updateStreakUI(int streak) {
         if (tvDaysCount == null) return;

         if (streak == 0) {
             tvDaysCount.setText("Start your streak!");
         } else if (streak == 1) {
             tvDaysCount.setText("1st Day Complete!");
         } else {
             tvDaysCount.setText("On a roll! 🔥 " + streak + " Days");
         }
    }

    private void initButtons() {
        // --- PEF ---
        Button buttonToRecordPEF = findViewById(R.id.buttonToRecordPEF);
        buttonToRecordPEF.setOnClickListener(v -> {
            Intent intent = new Intent(ChildDashboardActivity.this, RecordPEFFeature.class);
            intent.putExtra(CHILD_UID, childUid);
            intent.putExtra(CHILD_NAME, childName);
            startActivity(intent);
        });

        // --- Rescue ---
        // Control Variables
        Button buttonRescue = findViewById(R.id.buttonRescue);
        buttonRescue.setOnClickListener(view -> openEmergencyMedicationScreen());

        // --- Controller ---
        Button buttonController = findViewById(R.id.btnCheck);
        buttonController.setOnClickListener(view -> {
            Toast.makeText(this, "Controller clicked", Toast.LENGTH_SHORT).show();
        });

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
    }
    private void openEmergencyMedicationScreen() {
        if (childUid == null) return;

        Intent intent = new Intent(ChildDashboardActivity.this, EmergencyMedicationActivity.class);
        intent.putExtra(MED_TYPE, "Rescue");
        intent.putExtra(CHILD_UID, childUid);
        Log.d(TAG, "Opening Rescue for UID: " + childUid);
        startActivity(intent);
    }

}