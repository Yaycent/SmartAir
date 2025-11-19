package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
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

    // UI component: dropdown menu for selecting a child
    private Spinner spinnerchild;
    // Firebase instances
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    //save childid and childname
    private ArrayList<String> childnames = new ArrayList<>();
    private ArrayList<String> childids= new ArrayList<>();

    private static final String TAG = "ParentDashboardActivity";
    private String parentUid; //to store the parent UID received from login page

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

        Intent intent = getIntent();
        parentUid = intent.getStringExtra("PARENT_UID");

        if (parentUid != null) {
            Log.d(TAG, "Logged in Parent UID: " + parentUid);
        } else {
            Log.e(TAG, "Error: PARENT_UID not received!");
        }

        // add child button
        Button buttonAddChild = findViewById(R.id.buttonAddChild);
        buttonAddChild.setOnClickListener(v -> {
            // jump to AddChildActivity
            Intent addChildIntent = new Intent(ParentDashboardActivity.this, AddChildActivity.class);

            // pass parent id
            addChildIntent.putExtra("PARENT_UID", parentUid);

            startActivity(addChildIntent);

        });
        spinnerchild = findViewById(R.id.spinnerChildren);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        parentUid= getIntent().getStringExtra("PARENT_UID");;
        loadChildrenFromFirestore();

    }
    /**
     * loadChildrenFromFirestore()
     * ---------------------------
     * This method queries Firestore:
     * It finds all documents in the "children" collection where
     * the "parentId" field matches the current logged-in parent's UID.
     */

    private void loadChildrenFromFirestore(){
        // Get the UID of the currently logged-in parent

        db.collection("children").whereEqualTo("parentId", parentUid)
                .get().addOnSuccessListener(queryDocumentSnapshots -> {
                    childnames.clear();
                    childids.clear();

                    for(QueryDocumentSnapshot doc: queryDocumentSnapshots){

                        String name = doc.getString("childName");
                        String id = doc.getId();
                        childids.add(id);
                        childnames.add(name);
                    }

                    if(childnames.isEmpty()){
                        Toast.makeText(ParentDashboardActivity.this,
                                "Please add a child first.", Toast.LENGTH_SHORT).show();

                    }

                    ArrayAdapter<String> adapter= new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, childnames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerchild.setAdapter(adapter);

                })
                .addOnFailureListener(e ->{
                    Toast.makeText(ParentDashboardActivity.this,
                            "loading fail:"+e.getMessage(), Toast.LENGTH_SHORT).show();
                });

    }
}