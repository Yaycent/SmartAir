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
    private Spinner spinnerchild;
    private Button buttonAddChild;
    private Button buttonGoToChildDashboard;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Child info lists
    private ArrayList<String> childnames = new ArrayList<>();
    private ArrayList<String> childids = new ArrayList<>();

    private static final String TAG = "ParentDashboardActivity";
    private String parentUid;  // UID from login

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

        TextView tvLogout = findViewById(R.id.tvLogout);

        tvLogout.setOnClickListener(v -> {
            // optional: sign out from Firebase
            // FirebaseAuth.getInstance().signOut();

            Intent intent = new Intent(ParentDashboardActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // prevent returning with back button
        });


        // -----------------------------
        // Retrieve parent UID
        // -----------------------------
        parentUid = getIntent().getStringExtra("PARENT_UID");
        if (parentUid != null) {
            Log.d(TAG, "Logged in Parent UID: " + parentUid);
        } else {
            Log.e(TAG, "Error: parent UID missing");
        }

        // -----------------------------
        // UI Binding
        // -----------------------------
        buttonAddChild = findViewById(R.id.buttonAddChild);
        buttonGoToChildDashboard = findViewById(R.id.buttonGoToChildDashboard);
        spinnerchild = findViewById(R.id.spinnerChildren);

        // -----------------------------
        // Firebase Init
        // -----------------------------
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // -----------------------------
        // Add child button → go to AddChildActivity
        // -----------------------------
        buttonAddChild.setOnClickListener(v -> {
            Intent addChildIntent = new Intent(ParentDashboardActivity.this, AddChildActivity.class);
            addChildIntent.putExtra("PARENT_UID", parentUid);
            startActivity(addChildIntent);
        });

        // -----------------------------
        // Child Dashboard button
        // -----------------------------
        buttonGoToChildDashboard.setOnClickListener(v -> {
            Intent childDashIntent = new Intent(ParentDashboardActivity.this, ChildDashboardActivity.class);
            startActivity(childDashIntent);
        });

        // -----------------------------
        // Load children into Spinner
        // -----------------------------
        loadChildrenFromFirestore();
    }

    /**
     * loadChildrenFromFirestore()
     * <p>
     * Loads all children whose "parentId" == current parent's UID,
     * and displays them into the dropdown spinner.
     */
    private void loadChildrenFromFirestore() {

        db.collection("children")
                .whereEqualTo("parentId", parentUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    childnames.clear();
                    childids.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("childName");
                        String id = doc.getId();

                        childnames.add(name);
                        childids.add(id);
                    }

                    if (childnames.isEmpty()) {
                        Toast.makeText(
                                ParentDashboardActivity.this,
                                "Please add a child first.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            childnames
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerchild.setAdapter(adapter);

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

