package com.example.smartair;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddMedicineActivity extends AppCompatActivity {

    private EditText editName, editPurchaseDate, editExpiryDate, editTotalDose;
    private FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_medicine);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Bind UI
        editName = findViewById(R.id.editMedicineName);
        editPurchaseDate = findViewById(R.id.editPurchaseDate);
        editExpiryDate = findViewById(R.id.editExpiryDate);
        editTotalDose = findViewById(R.id.editTotalDose);
        Button buttonSave = findViewById(R.id.buttonSaveMedicine);
        buttonSave.setOnClickListener(v -> saveMedicineInfo());
    }
    private void saveMedicineInfo() {
        String name = editName.getText().toString().trim();
        String purchase = editPurchaseDate.getText().toString().trim();
        String expiry = editExpiryDate.getText().toString().trim();
        String totalStr = editTotalDose.getText().toString().trim();

        if (name.isEmpty() || purchase.isEmpty() || expiry.isEmpty() || totalStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int totalDose;
        try {
            totalDose = Integer.parseInt(totalStr);
        } catch (Exception e) {
            Toast.makeText(this, "Total dose must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> med = new HashMap<>();
        med.put("name", name);
        med.put("purchaseDate", purchase);
        med.put("expiryDate", expiry);
        med.put("totalDose", totalDose);
        med.put("remainingDose", totalDose);

        db.collection("medicine")
                .add(med)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Medicine added!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error saving medicine", Toast.LENGTH_SHORT).show()
                );
    }
}