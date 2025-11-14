package com.example.smartair;

import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btn1 = findViewById(R.id.btnTest1);
        Button btn2 = findViewById(R.id.btnTest2);

        FirebaseFirestore db = FirebaseFirestore.getInstance().collection("testLogs").getFirestore();

        btn1.setOnClickListener(v -> {

            Map<String, Object> data = new HashMap<>();
            data.put("type", "Rescue");
            data.put("time", "now");
            db.collection("testLogs").add(data);

        });

        btn2.setOnClickListener(v -> {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "PEF");
            data.put("value", 300);
            db.collection("testLogs").add(data);
        });


    }
}