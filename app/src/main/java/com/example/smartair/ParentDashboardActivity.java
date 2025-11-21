package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class ParentDashboardActivity extends AppCompatActivity {

    // UI components
    private Spinner spinnerChild;
    private Button buttonAddChild;
    private TextView tvGoToChildDashboard;
    private TextView tvLogout;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Child data
    private ArrayList<String> childNames = new ArrayList<>();
    private ArrayList<String> childIds = new ArrayList<>();

    private static final String TAG = "ParentDashboardActivity";
    private String parentUid; // UID passed from Login

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

        // -----------------------------
        // Bind UI Elements
        // -----------------------------
        spinnerChild = findViewById(R.id.spinnerChildren);
        buttonAddChild = findViewById(R.id.buttonAddChild);
        tvGoToChildDashboard = findViewById(R.id.tvGoToChildDashboard);
        tvLogout = findViewById(R.id.tvLogout);

        // -----------------------------
        // Get Parent UID
        // -----------------------------
        parentUid = getIntent().getStringExtra("PARENT_UID");

        if (parentUid == null) {
            Log.e(TAG, "Parent UID is NULL! (Login may have failed or activity restarted)");
            Toast.makeText(this, "Parent information lost. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d(TAG, "Parent UID Loaded: " + parentUid);

        // -----------------------------
        // Firebase Init
        // -----------------------------
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // -----------------------------
        // Logout
        // -----------------------------
        tvLogout.setOnClickListener(v -> {
            Intent backToLogin = new Intent(ParentDashboardActivity.this, MainActivity.class);
            startActivity(backToLogin);
            finish();
        });

        // -----------------------------
        // Add Child → AddChildActivity
        // -----------------------------
        buttonAddChild.setOnClickListener(v -> {
            Intent addChildIntent = new Intent(ParentDashboardActivity.this, AddChildActivity.class);
            addChildIntent.putExtra("PARENT_UID", parentUid);
            startActivity(addChildIntent);
        });

        // -----------------------------
        // Go to Child Dashboard
        // -----------------------------
        tvGoToChildDashboard.setOnClickListener(v -> {
            Intent childDashIntent = new Intent(ParentDashboardActivity.this, ChildDashboardActivity.class);
            startActivity(childDashIntent);
        });

        // -----------------------------
        // Load children from Firestore
        // -----------------------------
        loadChildrenFromFirestore();
    }

    /**
     * Loads children of this parent from Firestore
     * and puts them into the spinner.
     */
    private void loadChildrenFromFirestore() {

        // Add default "Select Child" option at top
        childNames.clear();
        childIds.clear();
        childNames.add("Select Child");
        childIds.add("NONE");

        db.collection("children")
                .whereEqualTo("parentId", parentUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("childName");
                        String id = doc.getId();

                        childNames.add(name);
                        childIds.add(id);
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            ParentDashboardActivity.this,
                            android.R.layout.simple_spinner_item,
                            childNames
                    );

                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerChild.setAdapter(adapter);
                })

                .addOnFailureListener(e -> {
                    Toast.makeText(
                            ParentDashboardActivity.this,
                            "Failed to load children: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }
}
