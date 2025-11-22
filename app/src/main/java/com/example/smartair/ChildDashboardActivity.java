package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ContentInfoCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class ChildDashboardActivity extends AppCompatActivity {
    private static final String TAG = "ChildDashboardActivity";
    private FirebaseFirestore db;
    private String childId;
    private Double personalBest;
    private EditText editTextChildPEF;
    private EditText editTextPEFTag;
    private TextView textViewZone;

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

        // Initialize Firebase Auth
        db = FirebaseFirestore.getInstance();
        Intent intent = getIntent();
        childId = intent.getStringExtra("CHILD_ID");

        // checking childID
        if (childId == null) {
            String parentUID = null;
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                parentUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            }

            if (parentUID == null) {
                Toast.makeText(this, "No child selected.", Toast.LENGTH_SHORT).show();
                finish();
            }

            // Query the database to find the child id
            db.collection("children")
                    .whereEqualTo("parentId", parentUID)
                    .limit(1)
                    .get()
                    .addOnSuccessListener((OnSuccessListener<QuerySnapshot>) queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                            childId = doc.getId();
                            Log.d(TAG, "Current childID: " + childId);
                            retrieveChildPB(childId);
                        } else {
                            Toast.makeText(this, "No child found for the current parent", Toast.LENGTH_SHORT).show();
                        }
                    })
                    // cannot query
                    .addOnFailureListener(f -> {
                        Log.w(TAG, "Failed to load child documents", f);
                        Toast.makeText(this, "Error finding child", Toast.LENGTH_SHORT).show();
                    });
        }
        else retrieveChildPB(childId);

        // UI elements
        editTextChildPEF = findViewById(R.id.editChildPEF);
        editTextPEFTag = findViewById(R.id.editTextPEFTag);
        textViewZone = findViewById(R.id.textViewZone);
        Button buttonRecordPEF = findViewById(R.id.buttonRecordPEF);

        //Initialize zone
        textViewZone.setText(getString(R.string.zone_unknown));
        textViewZone.setTextColor(ContextCompat.getColor(this, android.R.color.black));

        buttonRecordPEF.setOnClickListener(v -> recordChildPEF());
    }

    private void retrieveChildPB(String childDocID) {
        db.collection("children")
                .document(childDocID)
                .get()
                .addOnSuccessListener((OnSuccessListener<DocumentSnapshot>) documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Double PB = null;
                        Object pbObject = documentSnapshot.get("childPB");
                        if (pbObject instanceof Number) {
                            PB = ((Number) pbObject).doubleValue();
                        }
                        if (PB==null){
                            Toast.makeText(this, "Personal Best is not set for this child.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        else {
                            personalBest = PB;
                            Log.d(TAG, "Loaded PB: " + personalBest);
                        }
                    }
                    else {
                        Toast.makeText(this, "No record found.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(f -> {
                    Log.w(TAG, "Error loading child data.", f);
                    Toast.makeText(this, "Error loading child data", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
    private void recordChildPEF() {
        if (personalBest == null){
            Toast.makeText(this, "Personal Best is not loaded. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String childPEF = editTextChildPEF.getText().toString().trim();
        String zoneTag = editTextPEFTag.getText().toString().trim();

        if (childPEF.isEmpty()) {
            Toast.makeText(this, "PEF is empty. Please enter a PEF value.", Toast.LENGTH_SHORT).show();
            return;
        }
        final double PEFValue;
        try {
            PEFValue = Double.parseDouble(childPEF);
        }
        catch (NumberFormatException ex) {
            Toast.makeText(this, "Please enter a valid PEF value.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Determine zone
        String zone = determineZone(PEFValue, personalBest);

        // Update UI text color
        updateZoneText(zone);

        // Save PEF value
        savePEFLog(PEFValue, zone, zoneTag);
    }

    private String determineZone(double PEFValue, double PB){
        if (PB <= 0){
            return "Unknown";
        }

        double value = (PEFValue/PB)*100;
        if (value >= 80){
            return "Green";
        }
        if (value >= 50){
            return "Yellow";
        }
        else return "Red";
    }

    private void updateZoneText(String zone) {
        textViewZone.setText(getString(R.string.zoneSetting, zone));
        int textColor;
        if (zone.equals("Green")) {
            textColor = android.R.color.holo_green_light;
        }
        else if (zone.equals("Yellow")) {
            textColor = android.R.color.holo_orange_light;
        }
        else textColor = android.R.color.holo_red_light;
        textViewZone.setTextColor(ContextCompat.getColor(this, textColor));
    }

    private void savePEFLog(double PEFValue, String zone, String zoneTag) {
        if (childId == null)    {
            Toast.makeText(this, "Child Id missing. Cannot save data.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Create a Data Model (Document)
        Map<String, Object> childData = new HashMap<>();
        childData.put("childId", childId);
        childData.put("timeStamp", System.currentTimeMillis());
        childData.put("value", PEFValue);
        childData.put("zone", zone);
        childData.put("tag", zoneTag);

        // Call the Firestore API to create a new document in the “children” collection.
        db.collection("children")
                .document("childId")
                .collection("PEFLogs")
                .add(childData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "Child document added with ID: " + documentReference.getId());
                        Toast.makeText(ChildDashboardActivity.this, "Child information saved successfully!", Toast.LENGTH_SHORT).show();

                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding child document", e);
                        Toast.makeText(ChildDashboardActivity.this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }


}