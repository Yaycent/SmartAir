package com.example.smartair;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

import static com.example.smartair.Constants.*;

public class SettingActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

    private EditText editGreen, editYellow, editRed;
    private Button btnSave;
    private ImageButton btnBack;

    // Firebase
    private FirebaseFirestore db;
    private String parentUid;


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

        editGreen = findViewById(R.id.editGreenZone);
        editYellow = findViewById(R.id.editYellowZone);
        editRed = findViewById(R.id.editRedZone);

        btnSave = findViewById(R.id.btnSaveAll);
        btnBack = findViewById(R.id.btnBack);
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            parentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            loadSettings();
        } else {
            Toast.makeText(this, "Error: Not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnSave.setOnClickListener(v -> saveSettings());

        btnBack.setOnClickListener(v -> finish());
    }

    private void loadSettings() {
        // users -> [parentUid] -> settings -> action_plan
        DocumentReference docRef = db.collection("users").document(parentUid)
                .collection("settings").document("action_plan");

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String g = documentSnapshot.getString("greenZone");
                String y = documentSnapshot.getString("yellowZone");
                String r = documentSnapshot.getString("redZone");

                editGreen.setText(g);
                editYellow.setText(y);
                editRed.setText(r);
            } else {
                // in case
                editGreen.setText(DEFAULT_GREEN);
                editYellow.setText(DEFAULT_YELLOW);
                editRed.setText(DEFAULT_RED);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load settings", e);
            Toast.makeText(this, "Failed to load settings", Toast.LENGTH_SHORT).show();
        });
    }

    // save setting change
    private void saveSettings() {
        String rawGreen = editGreen.getText().toString().trim();
        String rawYellow = editYellow.getText().toString().trim();
        String rawRed = editRed.getText().toString().trim();

        // in case
        String finalGreenText = rawGreen.isEmpty() ? DEFAULT_GREEN : rawGreen;
        String finalYellowText = rawYellow.isEmpty() ? DEFAULT_YELLOW : rawYellow;
        String finalRedText = rawRed.isEmpty() ? DEFAULT_RED : rawRed;

        Map<String, Object> data = new HashMap<>();
        data.put("greenZone", finalGreenText);
        data.put("yellowZone", finalYellowText);
        data.put("redZone", finalRedText);
        data.put("updatedAt", System.currentTimeMillis());

        // save in db
        db.collection("users").document(parentUid)
                .collection("settings").document("action_plan")
                .set(data, SetOptions.merge()) // merge
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Settings Saved!", Toast.LENGTH_SHORT).show();

                    editGreen.setText(finalGreenText);
                    editYellow.setText(finalYellowText);
                    editRed.setText(finalRedText);

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}