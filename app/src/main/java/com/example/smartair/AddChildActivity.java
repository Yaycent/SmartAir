package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import static com.example.smartair.Constants.*;

public class AddChildActivity extends AppCompatActivity {
    private static final String TAG = "AddChildActivity";
    private FirebaseFirestore db;
    private String parentUid;

    // UI Variables
    private EditText editTextChildName;
    private EditText editTextChildDOB;
    private EditText editTextChildPB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_child);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        // get the parentId from the Intent
        Intent intent = getIntent();
        parentUid = intent.getStringExtra(PARENT_UID);

        if (parentUid == null) {
            Log.e(TAG, "ERROR: Parent UID is missing! Cannot save child data.");
            Toast.makeText(this, "Parent information has been lost and cannot be saved.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // UI elements
        editTextChildName = findViewById(R.id.editTextChildName);
        editTextChildDOB = findViewById(R.id.editTextChildDOB);
        editTextChildPB = findViewById(R.id.editTextChildPB);

        Button buttonSaveChild = findViewById(R.id.buttonSaveChild);
        Button buttonBackToParentDashboard = findViewById(R.id.buttonBackToParentDashboard1);
        ImageButton buttonBack = findViewById(R.id.buttonBack);

        // Set up button click event listener
        buttonSaveChild.setOnClickListener(v -> saveChildData());
        buttonBackToParentDashboard.setOnClickListener(v -> finish());
        buttonBack.setOnClickListener(v -> finish());
    }

    private void saveChildData() {
        String childName = editTextChildName.getText().toString().trim();
        String childDOB = editTextChildDOB.getText().toString().trim();
        String pbString = editTextChildPB.getText().toString().trim();

        // Basic Input Validation
        if (childName.isEmpty() || childDOB.isEmpty() || pbString.isEmpty()) {
            Toast.makeText(this, "Please enter your child's information.", Toast.LENGTH_SHORT).show();
            return;
        }

        double childPB;
        // Ensure parentId exists
        try {
            childPB = Double.parseDouble(pbString);
        } catch (NumberFormatException e) {
            editTextChildPB.setError("Invalid number");
            return;
        }

        // Create a Data Model (Document)
        Map<String, Object> childData = new HashMap<>();
        childData.put("childName", childName);
        childData.put("childDOB", childDOB);
        childData.put("childPB", childPB);
        // *** Most important key fields ***
        childData.put("parentId", parentUid);

        // Call the Firestore API to create a new document in the “children” collection.
        db.collection("children")
                .add(childData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "Child document added with ID: " + documentReference.getId());
                        Toast.makeText(AddChildActivity.this, "Child information saved successfully!", Toast.LENGTH_SHORT).show();

                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding child", e);
                    Toast.makeText(AddChildActivity.this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}