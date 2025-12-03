package com.example.smartair.ui.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartair.R;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

import static com.example.smartair.utils.Constants.*;
/**
 * SettingActivity.java
 * <p>
 * Manages configuration settings for both the Action Plan and Data Sharing.
 * </p>
 * <b>Key Functionalities:</b>
 * <ul>
 * <li><b>Action Plan Configuration (R4):</b> Allows parents to customize the Green, Yellow, and Red zone instructions for triage.</li>
 * <li><b>Granular Sharing Controls (R2):</b> Provides toggle switches to control which data types (Meds, PEF, Symptoms, Triage) are shared with the Provider.</li>
 * <li><b>Real-time Updates:</b> Updates sharing permissions in Firestore immediately upon toggling.</li>
 * </ul>
 *
 * @author Judy Xu
 * @version 1.0
 */
public class SettingActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

    // --- Original UI Components (Action Plan) ---
    private EditText editGreen, editYellow, editRed;
    private Button btnSave;
    private ImageButton btnBack;

    // --- New UI Components (Sharing Switches) ---
    private SwitchMaterial switchShareMeds, switchSharePEF, switchShareSymptoms, switchShareTriage;

    // --- Firebase & Data ---
    private FirebaseFirestore db;
    private String parentUid;
    private String childUid;

    private boolean isUpdatingUI = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setting);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            parentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Error: Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        childUid = getIntent().getStringExtra("CHILD_UID");
        if (childUid == null) {
            Toast.makeText(this, "No child selected. Some features may be disabled.", Toast.LENGTH_SHORT).show();
        }

        initViews();
        loadActionPlanSettings();

        if (childUid != null) {
            loadSharingSettings();
        }
    }

    private void initViews() {

        editGreen = findViewById(R.id.editGreenZone);
        editYellow = findViewById(R.id.editYellowZone);
        editRed = findViewById(R.id.editRedZone);
        btnSave = findViewById(R.id.btnSaveAll);
        btnBack = findViewById(R.id.btnBack);

        switchShareMeds = findViewById(R.id.switchShareMeds);
        switchSharePEF = findViewById(R.id.switchSharePEF);
        switchShareSymptoms = findViewById(R.id.switchShareSymptoms);
        switchShareTriage = findViewById(R.id.switchShareTriage);


        switchShareTriage.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdatingUI) {
                updateSharingSetting("shareTriage", isChecked);
                updateParentEmergencySetting(isChecked);
            }
        });

        btnSave.setOnClickListener(v -> saveActionPlanSettings());

        btnBack.setOnClickListener(v -> finish());

        if (childUid != null) {
            setupSwitchListener(switchShareMeds, "shareMeds");
            setupSwitchListener(switchSharePEF, "sharePEF");
            setupSwitchListener(switchShareSymptoms, "shareSymptoms");

        } else {
            disableSwitches();
        }
    }

    private void updateParentEmergencySetting(boolean value) {
        Map<String, Object> map = new HashMap<>();
        map.put("shareEmergencyEvent", value);

        db.collection("users")
                .document(parentUid)
                .collection("settings")
                .document("preferences")
                .set(map, SetOptions.merge());
    }


    // ==========================================
    // PART 1: Action Plan Logic (Original Code)
    // ==========================================

    private void loadActionPlanSettings() {
        DocumentReference docRef = db.collection("users").document(parentUid)
                .collection("settings").document("action_plan");

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String g = documentSnapshot.getString("greenZone");
                String y = documentSnapshot.getString("yellowZone");
                String r = documentSnapshot.getString("redZone");

                editGreen.setText(g != null ? g : DEFAULT_GREEN);
                editYellow.setText(y != null ? y : DEFAULT_YELLOW);
                editRed.setText(r != null ? r : DEFAULT_RED);
            } else {
                editGreen.setText(DEFAULT_GREEN);
                editYellow.setText(DEFAULT_YELLOW);
                editRed.setText(DEFAULT_RED);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load settings", e);
            Toast.makeText(this, "Failed to load Action Plan", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveActionPlanSettings() {
        String rawGreen = editGreen.getText().toString().trim();
        String rawYellow = editYellow.getText().toString().trim();
        String rawRed = editRed.getText().toString().trim();

        String finalGreenText = rawGreen.isEmpty() ? DEFAULT_GREEN : rawGreen;
        String finalYellowText = rawYellow.isEmpty() ? DEFAULT_YELLOW : rawYellow;
        String finalRedText = rawRed.isEmpty() ? DEFAULT_RED : rawRed;

        Map<String, Object> data = new HashMap<>();
        data.put("greenZone", finalGreenText);
        data.put("yellowZone", finalYellowText);
        data.put("redZone", finalRedText);
        data.put("updatedAt", System.currentTimeMillis());

        db.collection("users").document(parentUid)
                .collection("settings").document("action_plan")
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
                    // Update UI to display final value
                    editGreen.setText(finalGreenText);
                    editYellow.setText(finalYellowText);
                    editRed.setText(finalRedText);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ==========================================
    // PART 2: Data Sharing Logic (New Code)
    // ==========================================

    private void loadSharingSettings() {
        isUpdatingUI = true;

        db.collection("children").document(childUid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Map<String, Object> settings = (Map<String, Object>) document.get("sharingSettings");

                        if (settings != null) {
                            switchShareMeds.setChecked(Boolean.TRUE.equals(settings.get("shareMeds")));
                            switchSharePEF.setChecked(Boolean.TRUE.equals(settings.get("sharePEF")));
                            switchShareSymptoms.setChecked(Boolean.TRUE.equals(settings.get("shareSymptoms")));
                            switchShareTriage.setChecked(Boolean.TRUE.equals(settings.get("shareTriage")));
                        }
                    }
                    isUpdatingUI = false;
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load sharing settings", e);
                    isUpdatingUI = false;
                });
    }

    private void setupSwitchListener(SwitchMaterial switchView, String key) {
        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdatingUI) {
                updateSharingSetting(key, isChecked);
            }
        });
    }

    private void updateSharingSetting(String key, boolean value) {
        Map<String, Object> nestedData = new HashMap<>();
        nestedData.put(key, value);

        Map<String, Object> rootData = new HashMap<>();
        rootData.put("sharingSettings", nestedData);

        db.collection("children").document(childUid)
                .set(rootData, SetOptions.merge())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Update sharing setting failed", e);
                    Toast.makeText(this, "Failed to update setting", Toast.LENGTH_SHORT).show();
                });
    }

    private void disableSwitches() {
        switchShareMeds.setEnabled(false);
        switchSharePEF.setEnabled(false);
        switchShareSymptoms.setEnabled(false);
        switchShareTriage.setEnabled(false);
    }
}