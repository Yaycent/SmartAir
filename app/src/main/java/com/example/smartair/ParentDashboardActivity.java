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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.example.smartair.Constants.*;

public class ParentDashboardActivity extends AppCompatActivity {
    private static final String TAG = "ParentDashboardActivity";

    // UI components
    private Spinner spinnerChild;
    private Button buttonAddChild;
    private TextView tvGoToChildDashboard;
    private TextView tvLogout;
    private Button buttonMedicineCabinet;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Data
    private ArrayList<String> childNames = new ArrayList<>();
    private ArrayList<String> childIds = new ArrayList<>();
    private String parentUid;

    private int savedChildIndex = 0;
    private RecyclerView recyclerView;
    private MedicineAdapter adapter;
    private ArrayList<MedicineItem> medicineList;

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

        parentUid = getIntent().getStringExtra(PARENT_UID);

        if (parentUid == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            parentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        if (parentUid == null) {
            Log.e(TAG, "Parent UID is NULL!");
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // All buttons
        initViews();

        // realtime load medicines for this parent
        loadMedicinesForParent();

        // Add listener
        RescueUsageManager rescueManager = new RescueUsageManager();
        rescueManager.startListening(parentUid);
    }
    @Override
    protected void onResume() {
        super.onResume();

        if (parentUid == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            parentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        loadMedicinesForParent();
        loadChildrenFromFirestore();
    }
    private void initViews() {
        spinnerChild = findViewById(R.id.spinnerChildren);
        buttonAddChild = findViewById(R.id.buttonAddChild);
        tvGoToChildDashboard = findViewById(R.id.tvGoToChildDashboard);
        tvLogout = findViewById(R.id.tvLogout);
        recyclerView = findViewById(R.id.recyclerMedicineInventory);
        Button buttonAddMedicine = findViewById(R.id.buttonAddMedicine);

        // Spinner
        spinnerChild.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                savedChildIndex = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // --- Logout ---
        tvLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(ParentDashboardActivity.this, MainActivity.class);
            // Clear the task stack to prevent returning by pressing the back button
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // --- Add Child ---
        buttonAddChild.setOnClickListener(v -> {
            Intent intent = new Intent(ParentDashboardActivity.this, AddChildActivity.class);
            intent.putExtra(PARENT_UID, parentUid); // 使用常量
            startActivity(intent);
        });

        // --- Go to Child Dashboard ---
        tvGoToChildDashboard.setOnClickListener(v -> {
            int index = spinnerChild.getSelectedItemPosition();

            // Exclude the first item “Select Child” (Index 0)
            if (index <= 0) {
                Toast.makeText(this, "Please select a child first.", Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedChildName = childNames.get(index);
            String selectedChildUid = childIds.get(index);

            Intent intent = new Intent(ParentDashboardActivity.this, ChildDashboardActivity.class);
            intent.putExtra(CHILD_UID, selectedChildUid);
            intent.putExtra(CHILD_NAME, selectedChildName);
            intent.putExtra(PARENT_UID, parentUid);
            startActivity(intent);
        });

        // Medicine Inventory
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        medicineList = new ArrayList<>();
        adapter = new MedicineAdapter(this, medicineList);
        recyclerView.setAdapter(adapter);

        // "Add medicine" button
        buttonAddMedicine.setOnClickListener(v -> {
            Intent addMedIntent =
                    new Intent(ParentDashboardActivity.this, AddMedicineActivity.class);
            addMedIntent.putExtra(PARENT_UID, parentUid);
            startActivity(addMedIntent);
        });


    }

    private void loadChildrenFromFirestore() {

        db.collection("children")
                .whereEqualTo("parentId", parentUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    childNames.clear();
                    childIds.clear();

                    // Add default option
                    childNames.add("Select Child");
                    childIds.add("NONE");

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("childName");
                        String uid = doc.getId();

                        if (name != null) {
                            childNames.add(name);
                            childIds.add(uid);
                        }
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
                    Log.e(TAG, "Error loading children", e);
                    Toast.makeText(this, "Failed to load children.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Loads the list of medicines in real-time from Firebase.
     */
    private void loadMedicinesForParent() {
        if (parentUid == null) {
            Log.e(TAG, "Cannot load medicine: parentUid STILL NULL.");
            return;
        }

        db.collection("medicine")
                .whereEqualTo("parentUid", parentUid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen failed: ", e);
                        return;
                    }
                    if (snap == null) return;

                    medicineList.clear();
                    snap.getDocuments().forEach(doc -> {
                        MedicineItem item = doc.toObject(MedicineItem.class);
                        if (item != null) {
                            item.setId(doc.getId());
                            medicineList.add(item);
                        }
                    });
                    adapter.notifyDataSetChanged();
                });
    }
}
