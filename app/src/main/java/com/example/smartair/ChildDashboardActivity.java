package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ContentInfoCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class ChildDashboardActivity extends AppCompatActivity {
    private static final String TAG = "ChildDashboardActivity";
    private String childName;
    private String childId;

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

        Button buttonRecordPEF = findViewById(R.id.buttonRecordPEF);

        // ChildDashboard -> RecordPEFFeature
        // only pass the child ID
        buttonRecordPEF.setOnClickListener(v -> {
            Intent childInfoIntent = new Intent(ChildDashboardActivity.this, RecordPEFFeature.class);
            childInfoIntent.putExtra("CHILD_ID", childId);
            childInfoIntent.putExtra("CHILD_NAME", childName);
            startActivity(childInfoIntent);
        });

    }
}