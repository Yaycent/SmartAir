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

public class ChildDashboardActivity extends AppCompatActivity {  
    private static final String TAG = "ChildDashboardActivity";
    private Button buttonRescue, buttonController;
    private String childName;
    private String childId;
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

        // Uniformly get Intent
        Intent intent = getIntent();
        childUid = intent.getStringExtra("CHILD_UID");
        childId = intent.getStringExtra("CHILD_ID");
        parentUid = intent.getStringExtra("PARENT_UID");
        String childName = intent.getStringExtra("CHILD_NAME");


        Button buttonToRecordPEF = findViewById(R.id.buttonToRecordPEF);
        Button buttonBackToParentDashboard = findViewById(R.id.buttonBackToParentDashboard2);

        // ChildDashboard -> RecordPEFFeature
        // pass childId and childName
        buttonToRecordPEF.setOnClickListener(v -> {
            Intent childInfoIntent = new Intent(ChildDashboardActivity.this, RecordPEFFeature.class);
            childInfoIntent.putExtra("CHILD_ID", childId);
            childInfoIntent.putExtra("CHILD_NAME", childName);
            startActivity(childInfoIntent);
        });

        buttonBackToParentDashboard.setOnClickListener(v -> {
            Intent backToParentDashboardIntent = new Intent(ChildDashboardActivity.this, ParentDashboardActivity.class);
            backToParentDashboardIntent.putExtra("PARENT_UID", parentUid);
            startActivity(backToParentDashboardIntent);

            if (childUid != null) {
                Log.d(TAG, "Logged in Child UID: " + childUid);
            } else {
                Log.e(TAG, "Error: CHILD_UID not received!");
            }

            if (parentUid == null) {
                Log.w(TAG, "Warning: PARENT_UID missing from Intent, trying FirebaseAuth...");
                // Try to get it from the currently logged-in user as a backup
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    parentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                }
            }
            Log.d(TAG, "Parent UID: " + parentUid);

            // Hi, name
            TextView hiText = findViewById(R.id.tvHiChild);
            if (childName != null && hiText != null) {
                hiText.setText("Hi, " + childName);
            }

            // Rescue button
            buttonRescue = findViewById(R.id.buttonRescue);
            buttonRescue.setOnClickListener(view -> {
                openEmergencyMedicationScreen();
            });

            // Controller button
            buttonController = findViewById(R.id.buttonController);
            buttonController.setOnClickListener(view -> {
                Toast.makeText(this, "Controller clicked", Toast.LENGTH_SHORT).show();
            });

            // Back to Parent Dashboard
            TextView tvBackToParent = findViewById(R.id.tvBackToParent);
            tvBackToParent.setOnClickListener(view -> {
                Intent backintent = new Intent(ChildDashboardActivity.this, ParentDashboardActivity.class);
                backintent.putExtra("PARENT_UID", parentUid);
                startActivity(backintent);
                finish();
            });

        });


    };
    /**
     * Opens EmergencyMedicationActivity and passes
     * the medication type + child UID.
     */
    private void openEmergencyMedicationScreen() {
        Intent intent = new Intent(ChildDashboardActivity.this, EmergencyMedicationActivity.class);
        intent.putExtra("MED_TYPE", "Rescue");
        intent.putExtra("CHILD_UID", childUid);
        Log.d(TAG, "Child selected: " + "Rescue");
        startActivity(intent);
    }

}