package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
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
    // UID passed from Login
    private String parentUid;

    private int savedChildIndex = 0;



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

        spinnerChild.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                savedChildIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

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
            int index = spinnerChild.getSelectedItemPosition();

            if (index <= 0) {
                Toast.makeText(this, "Please select a child.", Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedChildName = childNames.get(index);

            Intent intent = new Intent(ParentDashboardActivity.this, ChildDashboardActivity.class);
            intent.putExtra("CHILD_NAME", selectedChildName);
            startActivity(intent);
        });

        // Go to Medicine Cabinet
        Button buttonMedicineCabinet = findViewById(R.id.buttonOpenMedicineCabinet);
        buttonMedicineCabinet.setOnClickListener(v -> {
            Intent medIntent = new Intent(ParentDashboardActivity.this, MedicineCabinetActivity.class);
            medIntent.putExtra("PARENT_UID", parentUid);
            startActivity(medIntent);
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

        // Reset lists and add default option
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

                    // Set adapter only AFTER all items are added
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            ParentDashboardActivity.this,
                            android.R.layout.simple_spinner_item,
                            childNames
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerChild.setAdapter(adapter);

                    // Restore saved selection (must be AFTER adapter)
                    if (savedChildIndex < childNames.size()) {
                        spinnerChild.setSelection(savedChildIndex);
                    }

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
