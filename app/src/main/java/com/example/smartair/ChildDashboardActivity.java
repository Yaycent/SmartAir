package com.example.smartair;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class ChildDashboardActivity extends AppCompatActivity {

    private static final String TAG = "ChildDashboardActivity";
    private Button buttonRescue, buttonController, buttonSubmit;
    private Spinner spinnerPreFeeling, spinnerPostFeeling;
    private EditText editDoseCount;

    private String activeMedicationType = "";  // "Rescue" or "Controller"
    private String childUid = "TEST_CHILD";

    private FirebaseFirestore db;

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
        db = FirebaseFirestore.getInstance();

        buttonRescue = findViewById(R.id.buttonRescue);
        buttonController = findViewById(R.id.buttonController);
        buttonSubmit = findViewById(R.id.buttonSubmitLog);

        spinnerPreFeeling = findViewById(R.id.spinnerPreFeeling);
        spinnerPostFeeling = findViewById(R.id.spinnerPostFeeling);
        editDoseCount = findViewById(R.id.editDoseCount);

        setupFeelingSpinners();
        setupButtons();
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
     * Sets up click listeners for the Rescue, Controller,
     * and Submit buttons.
     */
    private void setupButtons(){
        // For Rescue
        buttonRescue.setOnClickListener(v -> {
            activeMedicationType = "Rescue";
            Toast.makeText(this, "Rescue medication selected", Toast.LENGTH_SHORT).show();
        });

        // For controller
        buttonController.setOnClickListener(v -> {
            activeMedicationType = "Controller";
            Toast.makeText(this, "Controller medication selected", Toast.LENGTH_SHORT).show();
        });

        // Submit medication log
        buttonSubmit.setOnClickListener(v -> submitMedicationLog());
    }

    /**
     * Submits a complete medication log to Firestore.
     * Includes feelings, dose count, timestamp, and medication
     * type.
     */
    private void submitMedicationLog(){
        // Ensure the user selected Rescue or Controller
        if (activeMedicationType.isEmpty()){
            Toast.makeText(this, "Please select Rescue or Controller", Toast.LENGTH_SHORT).show();
            return;
        }

        // Read feelings from the spinners
        String preFeeling = spinnerPreFeeling.getSelectedItem().toString();
        String postFeeling = spinnerPostFeeling.getSelectedItem().toString();
        String doseStr = editDoseCount.getText().toString().trim();

        // Validate dose count
        if (doseStr.isEmpty()){
            Toast.makeText(this, "Please enter dose count", Toast.LENGTH_SHORT).show();
            return;
        }

        int doseCount = Integer.parseInt(doseStr);

        // Build a log object to upload to Firestore
        Map<String, Object> log = new HashMap<>();
        log.put("childId", childUid);
        log.put("timestamp", FieldValue.serverTimestamp());
        log.put("type", activeMedicationType);
        log.put("preFeeling", preFeeling);
        log.put("postFeeling", postFeeling);
        log.put("doseCount", doseCount);

        // Upload the log
        db.collection("medicationLogs")
                .add(log)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Medication log submitted", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Log added: " + ref.getId());
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error adding log", e));
    }

}