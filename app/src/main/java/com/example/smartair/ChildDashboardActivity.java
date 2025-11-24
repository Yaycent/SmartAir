package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import static com.example.smartair.Constants.*;

public class ChildDashboardActivity extends AppCompatActivity {  
    private static final String TAG = "ChildDashboardActivity";

    // Control Variables
    private Button buttonRescue, buttonController, buttonCheckin;

    // Data Variables
    private String childName;
    private String childUid;
    private String parentUid;

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
        buttonRescue = findViewById(R.id.buttonRescue);
        buttonRescue.setOnClickListener(view -> openEmergencyMedicationScreen());

        // --- Controller ---
        buttonController = findViewById(R.id.buttonController);
        buttonController.setOnClickListener(view -> {
            Toast.makeText(this, "Controller clicked", Toast.LENGTH_SHORT).show();
        });

        // --- Check-in ---
        buttonCheckin = findViewById(R.id.buttonCheckin);
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
        Button buttonBackToParentDashboard = findViewById(R.id.buttonBackToParentDashboard2);
        buttonBackToParentDashboard.setOnClickListener(v -> {
            finish();
        });
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