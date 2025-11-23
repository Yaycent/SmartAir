package com.example.smartair;

import android.os.Bundle;
import android.content.Intent;
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

public class RecordPEFFeature extends AppCompatActivity {
    private static final String TAG = "RecordPEFFeature";
    private FirebaseFirestore db;
    private String childName;
    private String childId;
    private Double personalBest;
    private EditText editTextChildPEF;
    private EditText editTextPEFTag;
    private TextView textViewZone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_record_pef);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // initialize Firebase Auth
        db = FirebaseFirestore.getInstance();

        // retrieve childId and childName from ChildDashboardActivity
        Intent intent = getIntent();
        childId = intent.getStringExtra("CHILD_ID");
        childName = intent.getStringExtra("CHILD_NAME");

        // checking childName and childId
        if (childName == null || childId == null) {
            Log.e(TAG, "Failed to retrieve child's name or child document id.");
            Toast.makeText(this, "Error retrieving child's name or Id. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        // find child's PB
        retrieveChildPB(childId);

        // connect UI elements
        editTextChildPEF = findViewById(R.id.editTextChildPEF);
        editTextPEFTag = findViewById(R.id.editTextPEFTag);
        textViewZone = findViewById(R.id.textViewZone);
        Button buttonRecordPEF = findViewById(R.id.buttonRecordPEF);

        // initialize zone (black for unknown)
        textViewZone.setText(getString(R.string.zone_unknown));
        textViewZone.setTextColor(ContextCompat.getColor(this, android.R.color.black));

        buttonRecordPEF.setOnClickListener(v -> recordChildPEF());
    }

    private void retrieveChildPB(String childId) {
        // query the database to find PB based on childId
        db.collection("children")
                .document(childId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
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
                    Toast.makeText(this, "Error loading child data.", Toast.LENGTH_SHORT).show();
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
        catch (NumberFormatException numEx) {
            Toast.makeText(this, "Please enter a valid PEF value.", Toast.LENGTH_SHORT).show();
            return;
        }

        // determine zone
        String zone = determineZone(PEFValue, personalBest);

        // update UI text color
        updateZoneText(zone);

        // save PEF value
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
        else if (value >= 50){
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
        if (childName == null || childId == null)    {
            Toast.makeText(this, "Child name or ID is missing. Cannot save data.", Toast.LENGTH_SHORT).show();
            return;
        }

        // create a document
        Map<String, Object> childData = new HashMap<>();
        childData.put("childId", childId);
        childData.put("timeStamp", System.currentTimeMillis());
        childData.put("value", PEFValue);
        childData.put("zone", zone);
        childData.put("tag", zoneTag);

        // call the Firestore API to create a new document in the “pefLogs” collection.
        db.collection("children")
                .document(childId)
                .collection("pefLogs")
                .add(childData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Child document added with ID: " + documentReference.getId());
                    Toast.makeText(RecordPEFFeature.this, "Child information saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding child document", e);
                    Toast.makeText(RecordPEFFeature.this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}