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
import android.view.View;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ParentDashboardActivity extends AppCompatActivity {

    private static final String TAG = "ParentDashboardActivity";
    private String parentUid; // To store the parent UID received from login page

    // UI elements for medicine inventory
    private EditText editPurchaseDate, editTotalDose, editExpiryDate;
    private TextView textRemainingDose;
    private TextView textLowStockAlert; // UI element for low stock warning
    private FirebaseFirestore db;

    // Define the fixed document ID for Rescue medication inventory
    private static final String RESCUE_DOC_ID = "rescue";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_parent_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        parentUid = intent.getStringExtra("PARENT_UID");

        if (parentUid != null) {
            Log.d(TAG, "Logged in Parent UID: " + parentUid);
        } else {
            Log.e(TAG, "Error: PARENT_UID not received!");
        }

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Bind medicine inventory UI elements
        editPurchaseDate = findViewById(R.id.editPurchaseDate);
        editTotalDose = findViewById(R.id.editTotalDose);
        editExpiryDate = findViewById(R.id.editExpiryDate);
        textRemainingDose = findViewById(R.id.textRemainingDose);
        textLowStockAlert = findViewById(R.id.textLowStockAlert); // Initialize new TextView
        Button buttonSaveMedicine = findViewById(R.id.buttonSaveMedicine);

        // Save button
        buttonSaveMedicine.setOnClickListener(v -> saveMedicineInfo());

        // Listener for child medication logs (to decrease inventory)
        listenForMedicationLogs();

        // Listener for inventory changes (to show alert) - Task 2.4
        listenForInventory();

        // --- Button setup ---

        // Add child button
        Button buttonAddChild = findViewById(R.id.buttonAddChild);
        buttonAddChild.setOnClickListener(v -> {
            // Jump to AddChildActivity
            Intent addChildIntent = new Intent(ParentDashboardActivity.this, AddChildActivity.class);
            // Pass parent id
            addChildIntent.putExtra("PARENT_UID", parentUid);
            startActivity(addChildIntent);
        });

        // Child dashboard button
        Button buttonGoToChildDashboard = findViewById(R.id.buttonGoToChildDashboard);
        buttonGoToChildDashboard.setOnClickListener(v -> {
            Intent childDashIntent = new Intent(ParentDashboardActivity.this, ChildDashboardActivity.class);
            // childDashIntent.putExtra("PARENT_UID", parentUid); // Pass parent id (optional for child dashboard)
            startActivity(childDashIntent);
        });
    }

    /**
     * Saves or updates medicine inventory info entered by the parent
     * into a fixed document (RESCUE_DOC_ID) in the 'medicine' collection.
     * Includes validation, remaining dose calculation, expiry check,
     * and writing to Firestore.
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

        // Initial remaining dose is set to total dose when saving new info
        int remainingDose = totalDose;

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

        // Firestore document map
        Map<String, Object> med = new HashMap<>();
        med.put("purchaseDate", purchaseDate);
        med.put("totalDose", totalDose);
        med.put("remainingDose", remainingDose);
        med.put("expiryDate", expiryDate);
        med.put("parentUid", parentUid); // link to the parent

        // Upload/Update to a fixed document ID for inventory tracking
        db.collection("medicine")
                .document(RESCUE_DOC_ID)
                .set(med, SetOptions.merge()) // Use merge to avoid overwriting other fields if necessary
                .addOnSuccessListener(doc -> Toast.makeText(this, "Medicine saved/updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error saving data", Toast.LENGTH_SHORT).show());
    }

    /**
     * Listens for new medication logs from the child in the Firestore.
     * If a new "Rescue" log is detected, the inventory will be decreased.
     */
    private void listenForMedicationLogs(){
        db.collection("medicationLogs")
                .addSnapshotListener((snap,error) ->{
                    if (error!=null) {
                        Log.e(TAG, "Medication Logs listen failed.", error);
                        return;
                    }
                    if (snap==null || snap.isEmpty()) return;
                    snap.getDocumentChanges().forEach(change -> {
                        String medType = change.getDocument().getString("type");
                        // Only decrease inventory if the log is newly added and is "Rescue"
                        if (change.getType() == com.google.firebase.firestore.DocumentChange.Type.ADDED && "Rescue".equals(medType)){
                            Log.d(TAG, "Rescue detected -> decrease inventory");
                            decreaseRescueInventory();
                        }
                    });
                });
    }

    /**
     * Decreases the remaining dose of the Rescue medication by 1.
     * This is triggered when the child submits a Rescue log.
     */
    private void decreaseRescueInventory(){
        db.collection("medicine") // Collection must match saveMedicineInfo
                .document(RESCUE_DOC_ID)
                .update("remainingDose", FieldValue.increment(-1))
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "Rescue inventory decreased by 1")
                )
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to decrease rescue inventory", e)
                );
    }

    /**
     * [Task 2.4 Implementation]
     * Listens for changes in the Rescue medication inventory document.
     * Updates the remaining dose display and shows a low stock alert
     * if the remaining dose is <= 20% of the total dose.
     */
    private void listenForInventory() {
        db.collection("medicine").document(RESCUE_DOC_ID)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Inventory listen failed.", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        // Read remainingDose and totalDose
                        Long remainingDoseLong = snapshot.getLong("remainingDose");
                        Long totalDoseLong = snapshot.getLong("totalDose");

                        if (remainingDoseLong != null && totalDoseLong != null && totalDoseLong > 0) {
                            int remainingDose = remainingDoseLong.intValue();
                            int totalDose = totalDoseLong.intValue();

                            // Update remaining dose display
                            textRemainingDose.setText("Remaining: " + remainingDose + "/" + totalDose);

                            // Calculate percentage and check for low stock (<= 20%)
                            double percentage = (double) remainingDose / totalDose;

                            if (percentage <= 0.2) {
                                // Show yellow low stock warning on the dashboard
                                textLowStockAlert.setText("🚨 LOW STOCK ALERT (Remaining " + String.format("%.0f", percentage * 100) + "%)");
                                textLowStockAlert.setVisibility(View.VISIBLE);
                                // Note: We use context.getColor for API compatibility
                                textLowStockAlert.setBackgroundColor(getColor(android.R.color.holo_orange_light));
                            } else {
                                // Hide the warning
                                textLowStockAlert.setVisibility(View.GONE);
                            }
                        } else {
                            // Data is present but doses are invalid/missing
                            textLowStockAlert.setVisibility(View.GONE);
                            textRemainingDose.setText("Inventory Data Invalid");
                        }
                    } else {
                        // No inventory data saved yet
                        Log.d(TAG, "Current inventory data: null");
                        textLowStockAlert.setVisibility(View.GONE);
                        textRemainingDose.setText("No Inventory Data Saved");
                    }
                });
    }
}