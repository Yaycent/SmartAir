package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
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

        Intent intent = getIntent();
        childUid = intent.getStringExtra("CHILD_UID");

        if (childUid != null) {
            Log.d(TAG, "Logged in Child UID: " + childUid);
        } else {
            Log.e(TAG, "Error: CHILD_UID not received!");
        }

        if (childUid == null) {
            Log.e(TAG, "Error: PARENT_UID not received!");
        }

        buttonRescue = findViewById(R.id.buttonRescue);
        buttonController = findViewById(R.id.buttonController);

        setupButtons();
        // -----------------------------
        // 1. Get parent UID safely
        // -----------------------------
        parentUid = getIntent().getStringExtra("PARENT_UID");

        if (parentUid == null) {
            parentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // -----------------------------
        // 2. Back to Parent Dashboard
        // -----------------------------
        TextView tvBackToParent = findViewById(R.id.tvBackToParent);

        tvBackToParent.setOnClickListener(v -> {
            Intent intent = new Intent(ChildDashboardActivity.this, ParentDashboardActivity.class);
            intent.putExtra("PARENT_UID", parentUid);
            startActivity(intent);
            finish();
        });

        String childName = getIntent().getStringExtra("CHILD_NAME");

        TextView hiText = findViewById(R.id.tvHiChild);
        hiText.setText("Hi, " + childName);

    }

    /**
     * Sets up click listeners for the Rescue, Controller,
     * and Submit buttons.
     */
    private void setupButtons(){
        // For Rescue
        buttonRescue.setOnClickListener(v -> {
            Toast.makeText(this, "Rescue medication selected", Toast.LENGTH_SHORT).show();
            openEmergencyMedicationScreen();
        });

        // For controller
        buttonController.setOnClickListener(v -> Toast.makeText(this, "Controller medication selected", Toast.LENGTH_SHORT).show());
    }

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