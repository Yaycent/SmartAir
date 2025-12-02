package com.example.smartair;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;



public class SOSTriageActivity extends AppCompatActivity {

    private String childUid, parentUid;
    private String childName;

    private RadioGroup rgQ1, rgQ2, rgQ3;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sos_triage);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        childUid = getIntent().getStringExtra("CHILD_UID");
        parentUid = getIntent().getStringExtra("PARENT_UID");
        childName = getIntent().getStringExtra("CHILD_NAME");


        if (childUid == null || parentUid == null) {
            Toast.makeText(this, "Missing child/parent data.", Toast.LENGTH_SHORT).show();
        }

        rgQ1 = findViewById(R.id.rgQ1);
        rgQ2 = findViewById(R.id.rgQ2);
        rgQ3 = findViewById(R.id.rgQ3);

        Button btnNext = findViewById(R.id.btnNext);
        btnNext.setOnClickListener(v -> evaluateAnswers());

    }

    /**
     * Evaluate the three triage questions.
     * If ANY answer is “Yes”, then treat as emergency, show warning & call 911.
     * If all are “No”, then proceed to secondary SOS form page.
     */
    private void evaluateAnswers() {

        int q1 = rgQ1.getCheckedRadioButtonId();
        int q2 = rgQ2.getCheckedRadioButtonId();
        int q3 = rgQ3.getCheckedRadioButtonId();

        // Ensure all questions answered
        if (q1 == -1 || q2 == -1 || q3 == -1) {
            Toast.makeText(this, getString(R.string.sos_missing_input), Toast.LENGTH_SHORT).show();
            return;
        }

        boolean hasEmergency =
                q1 == R.id.q1Yes
                        || q2 == R.id.q2Yes
                        || q3 == R.id.q3Yes;

        savePage1Log(hasEmergency);


        if (hasEmergency) {
            //NEW: Parent Alert for SOS YES
            ParentAlertHelper.createParentAlertInFirestore(
                    parentUid,
                    childUid,
                    childName,
                    childName+ "SOS emergency triggered: At least one triage question was answered YES."
            );
            // Show emergency toast & trigger phone dialer
            Toast.makeText(this, getString(R.string.calling_911), Toast.LENGTH_LONG).show();
            call911();
            finish();
            return;
        }

        // Go to page 2
        Intent intent = new Intent(SOSTriageActivity.this, SOSSecondaryActivity.class);
        intent.putExtra("CHILD_UID", childUid);
        intent.putExtra("PARENT_UID", parentUid);
        intent.putExtra("CHILD_NAME", childName);
        startActivity(intent);

    }

    /**
     * Save the log data to Firestore.
     */
    private void savePage1Log(boolean emergency) {
        Map<String, Object> data = new HashMap<>();
        data.put("parentUid", parentUid);
        data.put("childUid", childUid);
        data.put("timestamp", System.currentTimeMillis());
        data.put("emergencyDetected", emergency);

        db.collection("sosTriageLogs")
                .add(data)
                .addOnSuccessListener(doc ->
                        Log.d("SOS_TRIAGE", "Page1 log saved " + doc.getId()))
                .addOnFailureListener(e ->
                        Log.e("SOS_TRIAGE", "Failed to save page1 log", e));
    }


    /**
     * Opens the phone dialer pre-filled with 911.
     * If emulator or device cannot open dialer, show fallback toast.
     */
    private void call911() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:911"));

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "No dialer available. Please call 911 immediately!", Toast.LENGTH_LONG).show();
        }
    }
}