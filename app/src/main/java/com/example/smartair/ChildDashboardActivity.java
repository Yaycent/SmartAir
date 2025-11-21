package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class ChildDashboardActivity extends AppCompatActivity {

    private String parentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_child_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // -----------------------------
        // 1. Get parent UID safely
        // -----------------------------
        parentUid = getIntent().getStringExtra("PARENT_UID");

        if (parentUid == null) {
            parentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // -----------------------------
        // 2. Back to Parent Dashboard
        // -----------------------------
        TextView tvBackToParent = findViewById(R.id.tvBackToParent);

        tvBackToParent.setOnClickListener(v -> {
            Intent intent = new Intent(ChildDashboardActivity.this, ParentDashboardActivity.class);
            intent.putExtra("PARENT_UID", parentUid);
            startActivity(intent);
            finish();
        });

        String childName = getIntent().getStringExtra("CHILD_NAME");

        TextView hiText = findViewById(R.id.tvHiChild);
        hiText.setText("Hi, " + childName);

    }
}