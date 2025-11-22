package com.example.smartair;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MedicineCabinetActivity extends AppCompatActivity {
    private static final String TAG = "MedicineCabinet";

    private EditText editPurchaseDate, editTotalDose, editExpiryDate;
    private TextView textRemainingDose;
    private FirebaseFirestore db;
    private String parentUid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medicine_cabinet);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        parentUid = intent.getStringExtra("PARENT_UID");

        if (parentUid == null) {
            Log.e(TAG, "Error: PARENT_UID not received!");
        }

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Bind UI
        editPurchaseDate = findViewById(R.id.editPurchaseDate);
        editTotalDose = findViewById(R.id.editTotalDose);
        editExpiryDate = findViewById(R.id.editExpiryDate);
        textRemainingDose = findViewById(R.id.textRemainingDose);
        Button buttonSave = findViewById(R.id.buttonSaveMedicine);

        // Save button
        buttonSave.setOnClickListener(v -> saveMedicineInfo());

        // Start listening for child's rescue usage
        listenForMedicationLogs();
    }
    /**
     * Saves medicine inventory info entered by the parent.
     * Includes validation, remaining dose calculation,
     * expiry check, low stock alert, and writing to Firestore.
     */
    @SuppressLint("SetTextI18n")
    private void saveMedicineInfo(){
        String purchaseDate = editPurchaseDate.getText().toString().trim();
        String totalDoseStr = editTotalDose.getText().toString().trim();
        String expiryDate = editExpiryDate.getText().toString().trim();

        // Validate empty fields
        if (purchaseDate.isEmpty()||totalDoseStr.isEmpty()||expiryDate.isEmpty()){
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse total dose
        int totalDose;
        try {
            totalDose = Integer.parseInt(totalDoseStr);
        } catch (Exception e) {
            Toast.makeText(this, "Total dose must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initial remaining dose
        int remainingDose = totalDose;

        textRemainingDose.setText("Remaining: " + remainingDose + "/" + totalDose);

        // Expiry check
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setLenient(false);
        try {
            Date exp = sdf.parse(expiryDate);
            if (exp!=null && exp.before(new Date())) {
                Toast.makeText(this, "!Medicine has expired!", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Invalid expiry date format (YYYY-MM-DD)", Toast.LENGTH_SHORT).show();
            return;
        }

        // Low stock alert
        if (remainingDose / (double) totalDose <= 0.2){
            Toast.makeText(this, "!Medicine running low (<=20%)!", Toast.LENGTH_LONG).show();
        }

        // Firestore document
        Map<String, Object> med = new HashMap<>();
        med.put("purchaseDate", purchaseDate);
        med.put("totalDose", totalDose);
        med.put("remainingDose", remainingDose);
        med.put("expiryDate", expiryDate);
        med.put("parentUid", parentUid); // link to the parent

        // Upload
        db.collection("medicine")
                .add(med)
                .addOnSuccessListener(doc -> Toast.makeText(this, "Medicine saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error saving data", Toast.LENGTH_SHORT).show());
    }
    /**
     * Listens for new medication logs from the child
     * in the Firestore. If a new Rescue log is detected,
     * inventory will be updated.
     */
    private void listenForMedicationLogs(){
        db.collection("medicationLogs")
                .addSnapshotListener((snap,error) ->{
                    if (error!=null) {
                        Log.e(TAG, "Listen failed.", error);
                        return;
                    }
                    if (snap==null || snap.isEmpty()) return;
                    snap.getDocumentChanges().forEach(change -> {
                        String medType = change.getDocument().getString("type");
                        if ("Rescue".equals(medType)){
                            Log.d(TAG, "Rescue detected -> decrease inventory");
                            decreaseRescueInventory();
                        }
                    });
                });
    }
    /**
     * Decreases the remaining dose of the Rescue
     * medication by 1. This is triggered when the child
     * submits a Rescue log.
     */
    private void decreaseRescueInventory(){
        db.collection("parentInventory")
                .document("rescue")
                .update("remainingDose", FieldValue.increment(-1))
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "Rescue inventory decreased by 1")
                )
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to decrease rescue inventory", e)
                );
    }
}