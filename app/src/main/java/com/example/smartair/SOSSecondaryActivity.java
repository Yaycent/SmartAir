package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SOSSecondaryActivity extends AppCompatActivity {

    private EditText editRescueAttempts, editCurrentPEF;
    private FirebaseFirestore db;
    private String childUid, parentUid;

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

        childUid = getIntent().getStringExtra("CHILD_UID");
        parentUid = getIntent().getStringExtra("PARENT_UID");

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
        int pef;

        // Ensure number format is valid
        try {
            attempts = Integer.parseInt(attemptsStr);
            pef = Integer.parseInt(pefStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create log data
        Map<String, Object> data = new HashMap<>();
        data.put("childUid", childUid);
        data.put("parentUid", parentUid);
        data.put("timestamp", System.currentTimeMillis());
        data.put("rescueAttempts", attempts);
        data.put("currentPEF", pef);

        // Save to Firestore
        db.collection("sosTriageLogs")
                .add(data)
                .addOnSuccessListener(doc -> {
                    Log.d("SOS", "Secondary log saved.");
                    Toast.makeText(this, "Submitted!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(SOSSecondaryActivity.this, ChildDashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("CHILD_UID", childUid);
                    intent.putExtra("PARENT_UID", parentUid);
                    startActivity(intent);

                    finish();

                })
                .addOnFailureListener(e -> {
                    Log.e("SOS", "Failed to save SOS log: " + e.getMessage());
                    Toast.makeText(this, "Failed to submit", Toast.LENGTH_SHORT).show();
                });
    }
}