package com.example.smartair;

import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import static com.example.smartair.Constants.*;

public class RecordPEFFeature extends AppCompatActivity {
    private static final String TAG = "RecordPEFFeature";

    private FirebaseFirestore db;
    private String childName;
    private String childUid;
    private Double personalBest;

    private EditText editTextChildPEF;
    private Spinner spinnerZoneTag;
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
        childUid = intent.getStringExtra(CHILD_UID);
        childName = intent.getStringExtra(CHILD_NAME);

        // checking childName and childId
        if (childName == null || childUid == null) {
            Log.e(TAG, "Failed to retrieve child's name or child document id.");
            Toast.makeText(this, "Error retrieving child's name or Id. Please try again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // find child's PB
        retrieveChildPB(childUid);

        // connect UI elements
        editTextChildPEF = findViewById(R.id.editTextChildPEF);
        spinnerZoneTag = findViewById(R.id.spinnerZoneTag);
        textViewZone = findViewById(R.id.textViewZone);
        Button buttonRecordPEF = findViewById(R.id.buttonRecordPEF);
        ImageButton imageButtonBackRecordPEF = findViewById(R.id.imageButtonBackRecordPEF);

        // initialize zone (black for unknown)
        textViewZone.setText(getString(R.string.zone_unknown));
        textViewZone.setTextColor(ContextCompat.getColor(this, android.R.color.black));

        buttonRecordPEF.setOnClickListener(v -> recordChildPEF());
        imageButtonBackRecordPEF.setOnClickListener(v -> finish());
        setUpZoneTagSpinners();
    }

    private void retrieveChildPB(String childUid) {
        // query the database to find PB based on childUid
        db.collection("children")
                .document(childUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Double PB = null;
                        Object pbObject = documentSnapshot.get("childPB");
                        if (pbObject instanceof Number) {
                            PB = ((Number) pbObject).doubleValue();
                        }
                        if (PB == null || PB == 0){
                            Toast.makeText(this, "Personal Best is not set for this child.", Toast.LENGTH_SHORT).show();
                            personalBest = null;
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

    private void setUpZoneTagSpinners() {
        // Adapter loads choices from string.xml
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.zone_tag_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerZoneTag.setAdapter(adapter);
    }

    private void recordChildPEF() {
        if (personalBest == null){
            Toast.makeText(this, "Personal Best is not loaded. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String childPEF = editTextChildPEF.getText().toString().trim();
        String zoneTag = spinnerZoneTag.getSelectedItem().toString();

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
        if (childUid == null) return;

        // create a document
        Map<String, Object> logData = new HashMap<>();

        logData.put("childId", childUid);
        logData.put("childName", childName);
        logData.put("timeStamp", System.currentTimeMillis());
        logData.put("value", PEFValue);
        logData.put("zone", zone);
        logData.put("tag", zoneTag);

        // Save to the root directory pefLogs
        db.collection("pefLogs")
                .add(logData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "PEF Log saved: " + documentReference.getId());
                    Toast.makeText(RecordPEFFeature.this, "Saved successfully!", Toast.LENGTH_SHORT).show();
                    new Handler().postDelayed(this::finish, 2500);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving PEF", e);
                    Toast.makeText(RecordPEFFeature.this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}