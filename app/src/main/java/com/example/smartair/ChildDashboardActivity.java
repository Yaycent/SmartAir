package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ChildDashboardActivity extends AppCompatActivity {
    private static final String TAG = "ChildDashboardActivity";
    private String childName;
    private String childId;
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

        // retrieve childId and childName from ParentDashboardActivity
        Intent intent = getIntent();
        childName = intent.getStringExtra("CHILD_NAME");
        childId = intent.getStringExtra("CHILD_ID");
        parentUid = intent.getStringExtra("PARENT_UID");

        Button buttonToRecordPEF = findViewById(R.id.buttonToRecordPEF);
        Button buttonBackToParentDashboard = findViewById(R.id.buttonBackToParentDashboard2);

        // ChildDashboard -> RecordPEFFeature
        // pass childId and childName
        buttonToRecordPEF.setOnClickListener(v -> {
            Intent childInfoIntent = new Intent(ChildDashboardActivity.this, RecordPEFFeature.class);
            childInfoIntent.putExtra("CHILD_ID", childId);
            childInfoIntent.putExtra("CHILD_NAME", childName);
            startActivity(childInfoIntent);
        });

        buttonBackToParentDashboard.setOnClickListener(v->{
            Intent backToParentDashboardIntent = new Intent(ChildDashboardActivity.this, ParentDashboardActivity.class );
            backToParentDashboardIntent.putExtra("PARENT_UID", parentUid);
            startActivity(backToParentDashboardIntent);
        });

    }
}