package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ParentDashboardActivity extends AppCompatActivity {

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
    }
}