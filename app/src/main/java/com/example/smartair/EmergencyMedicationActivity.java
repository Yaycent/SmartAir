package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import static com.example.smartair.Constants.*;

public class EmergencyMedicationActivity extends AppCompatActivity {
    private static final String TAG = "EmergencyMedication";

    private Spinner spinnerPreFeeling, spinnerPostFeeling;
    private EditText editDoseCount;
    private Button buttonSubmit;
    private String childUid;    // from ChildDashboardActivity
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_emergency_medication);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        childUid = intent.getStringExtra(CHILD_UID);

        if (childUid == null) {
            Log.e(TAG, "Error: CHILD_UID missing!");
            Toast.makeText(this, "Child information missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        spinnerPreFeeling = findViewById(R.id.spinnerPreFeeling);
        spinnerPostFeeling = findViewById(R.id.spinnerPostFeeling);
        editDoseCount = findViewById(R.id.editDoseCount);
        buttonSubmit = findViewById(R.id.buttonSubmitLog);
        ImageButton imageButtonBackEmergencyMedication = findViewById(R.id.imageButtonBackEmergencyMedication);
        ImageButton imageButtonClickForHelp = findViewById(R.id.imageButtonClickForHelp);

        // Set click listener for the back button
        imageButtonBackEmergencyMedication.setOnClickListener(v -> finish());

        // Set click listener for the help button
        imageButtonClickForHelp.setOnClickListener(v ->{
            Intent clickForHelpintent = new Intent(EmergencyMedicationActivity.this, ClickForHelpFeature.class);
            startActivity(clickForHelpintent);
        });

        setupFeelingSpinners();
        setupSubmitButton();
    }

    /**
     * Initializes the Better / Same / Worse spinners
     * using the feeling_options string array
     */
    private void setupFeelingSpinners() {
        // Adapter loads choices from strings.xml
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.feeling_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set adapter to both spinners
        spinnerPreFeeling.setAdapter(adapter);
        spinnerPostFeeling.setAdapter(adapter);
    }

    /**
     * Sets click listener for the submit button.
     */
    private void setupSubmitButton() {
        buttonSubmit.setOnClickListener(v -> submitMedicationLog());
    }

    /**
     * Submits a complete medication log to Firestore.
     * Includes feelings, dose count, timestamp, and medication
     * type.
     */
    private void submitMedicationLog(){
        // Read feelings from the spinners
        String preFeeling = spinnerPreFeeling.getSelectedItem().toString();
        String postFeeling = spinnerPostFeeling.getSelectedItem().toString();
        String doseStr = editDoseCount.getText().toString().trim();

        // Validate dose count
        if (doseStr.isEmpty()){
            Toast.makeText(this, "Please enter dose count", Toast.LENGTH_SHORT).show();
            return;
        }

        int doseCount;
        try {
            doseCount = Integer.parseInt(doseStr);
        } catch (NumberFormatException e) {
            editDoseCount.setError("Invalid number");
            return;
        }

        // Build a log object to upload to Firestore
        Map<String, Object> log = new HashMap<>();
        log.put("childUid", childUid);
        log.put("timestamp", FieldValue.serverTimestamp());
        log.put("type", "Rescue");
        log.put("preFeeling", preFeeling);
        log.put("postFeeling", postFeeling);
        log.put("doseCount", doseCount);

        // Upload the log
        db.collection("medicationLogs")
                .add(log)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Medication log submitted", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Log added: " + ref.getId());
                    finish();
                })
                .addOnFailureListener(e -> {
                        Log.e(TAG, "Error adding log", e);
                        Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}