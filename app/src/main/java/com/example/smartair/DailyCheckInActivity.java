package com.example.smartair;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import static com.example.smartair.Constants.*;

public class DailyCheckInActivity extends AppCompatActivity {

    private String childUid;
    private String parentUid;

    private RadioGroup rgAuthor;
    private RadioButton rbAuthorChild, rbAuthorParent;

    private CheckBox cbNightWaking, cbActivityLimit, cbCoughWheeze;
    private CheckBox cbTriggerExercise, cbTriggerColdAir, cbTriggerDustPets,
            cbTriggerSmoke, cbTriggerIllness, cbTriggerStrongOdors;

    private Button btnSave;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_daily_check_in);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get childUid / parentUid from Intent
        childUid = getIntent().getStringExtra(CHILD_UID);
        parentUid = getIntent().getStringExtra(PARENT_UID);

        if (childUid == null) {
            Toast.makeText(this, "Error: child UID missing", Toast.LENGTH_SHORT).show();
        }

        db = FirebaseFirestore.getInstance();

        // Bind views
        rgAuthor = findViewById(R.id.rgAuthor);
        rbAuthorChild = findViewById(R.id.rbAuthorChild);
        rbAuthorParent = findViewById(R.id.rbAuthorParent);

        cbNightWaking = findViewById(R.id.cbNightWaking);
        cbActivityLimit = findViewById(R.id.cbActivityLimit);
        cbCoughWheeze = findViewById(R.id.cbCoughWheeze);

        cbTriggerExercise = findViewById(R.id.cbTriggerExercise);
        cbTriggerColdAir = findViewById(R.id.cbTriggerColdAir);
        cbTriggerDustPets = findViewById(R.id.cbTriggerDustPets);
        cbTriggerSmoke = findViewById(R.id.cbTriggerSmoke);
        cbTriggerIllness = findViewById(R.id.cbTriggerIllness);
        cbTriggerStrongOdors = findViewById(R.id.cbTriggerStrongOdors);

        btnSave = findViewById(R.id.btnSaveSymptomLog);

        btnSave.setOnClickListener(v -> {
            Toast.makeText(this, "Save button clicked!", Toast.LENGTH_SHORT).show();
            saveSymptomLog();
        });
    }

    private void saveSymptomLog() {
        Toast.makeText(this, "saveSymptomLog() entered", Toast.LENGTH_SHORT).show();
        if (childUid == null) {
            Toast.makeText(this, "childUid is null, cannot save.", Toast.LENGTH_SHORT).show();
            return;
        }

        // author: Child or Parent
        String author;
        if (rbAuthorChild.isChecked()) {
            author = "Child";
        } else if (rbAuthorParent.isChecked()) {
            author = "Parent";
        } else {
            Toast.makeText(this, "Who entered the information（Child / Parent）", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean nightWaking = cbNightWaking.isChecked();
        boolean activityLimit = cbActivityLimit.isChecked();
        boolean coughWheeze = cbCoughWheeze.isChecked();

        // triggers 
        List<String> triggers = new ArrayList<>();
        if (cbTriggerExercise.isChecked()) triggers.add("Exercise");
        if (cbTriggerColdAir.isChecked()) triggers.add("Cold air");
        if (cbTriggerDustPets.isChecked()) triggers.add("Dust/pets");
        if (cbTriggerSmoke.isChecked()) triggers.add("Smoke");
        if (cbTriggerIllness.isChecked()) triggers.add("Illness");
        if (cbTriggerStrongOdors.isChecked()) triggers.add("Strong odors");

        long timestamp = System.currentTimeMillis();

        SymptomLog log = new SymptomLog(
                childUid,
                timestamp,
                author,
                nightWaking,
                activityLimit,
                coughWheeze,
                triggers
        );

        // Save to symptomLogs collection
        // db.collection("symptomLogs")
        //         .add(log)
        //         .addOnSuccessListener(docRef -> {
        //             Toast.makeText(this, "Symptom log saved", Toast.LENGTH_SHORT).show();
        //             finish(); 
        //         })
        //         .addOnFailureListener(e -> {
        //             Toast.makeText(this, "Symptom log save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        //         });
        db.collection("symptomLogs")
                .add(log)
                .addOnSuccessListener(docRef -> {
                    Log.d("DailyCheckInActivity",
                            "Symptom log saved with id = " + docRef.getId());
                    Toast.makeText(this, "Symptom log saved", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("DailyCheckInActivity",
                            "Symptom log save failed", e);
                    Toast.makeText(this,
                            "Symptom log save failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
