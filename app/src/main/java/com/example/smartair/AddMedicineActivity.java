package com.example.smartair;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import static com.example.smartair.Constants.CHILD_NAME;
import static com.example.smartair.Constants.CHILD_UID;
import static com.example.smartair.Constants.PARENT_UID;

public class AddMedicineActivity extends AppCompatActivity {

    private EditText editName, editPurchaseDate, editExpiryDate, editTotalDose, editDosePerUse;
    private Spinner spinnerMedType;
    private FirebaseFirestore db;
    private String parentUid, childUid;
    private String childName;//new
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

        // Get parent UID from the Intent
        parentUid = getIntent().getStringExtra(PARENT_UID);
        if (parentUid == null) {
            Toast.makeText(this, "Error: Parent UID missing!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Get child UID from the Intent
        childUid = getIntent().getStringExtra(CHILD_UID);
        if (childUid == null) {
            Toast.makeText(this, "Error: Child UID missing!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // add new
        childName = getIntent().getStringExtra(CHILD_NAME);



        // Bind UI
        editName = findViewById(R.id.editMedicineName);
        editPurchaseDate = findViewById(R.id.editPurchaseDate);
        editExpiryDate = findViewById(R.id.editExpiryDate);
        editTotalDose = findViewById(R.id.editTotalDose);
        editDosePerUse = findViewById(R.id.editDosePerUse);
        spinnerMedType = findViewById(R.id.spinnerMedType);

        // Save button
        Button buttonSave = findViewById(R.id.buttonSaveMedicine);

        setupTypeSpinner();
        buttonSave.setOnClickListener(v -> saveMedicineInfo());
    }

    private void setupTypeSpinner(){
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.medicine_type_options,  // ["Rescue", "Controller"]
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMedType.setAdapter(adapter);

        spinnerMedType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String type = spinnerMedType.getSelectedItem().toString();
                editDosePerUse.setVisibility("Controller".equals(type) ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    /**
     * Saves a new medicine entry into Firestore.
     */
    private void saveMedicineInfo() {
        String name = editName.getText().toString().trim();
        String purchase = editPurchaseDate.getText().toString().trim();
        String expiry = editExpiryDate.getText().toString().trim();
        String totalStr = editTotalDose.getText().toString().trim();
        String type = spinnerMedType.getSelectedItem().toString();

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

        int dosePerUse = 1;
        if (type.equals("Controller")) {
            String perUseStr = editDosePerUse.getText().toString().trim();
            if (perUseStr.isEmpty()) {
                Toast.makeText(this, "Enter dose per use", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                dosePerUse = Integer.parseInt(perUseStr);
            } catch (NumberFormatException e) {
                editDosePerUse.setError("Invalid number");
                return;
            }
        }

        Map<String, Object> med = new HashMap<>();
        med.put("name", name);
        med.put("purchaseDate", purchase);
        med.put("expiryDate", expiry);
        med.put("totalDose", totalDose);
        med.put("remainingDose", totalDose);
        med.put("medType", type);
        med.put("dosePerUse", dosePerUse);
        med.put("parentUid", parentUid);
        med.put("childUid", childUid);
        // addnew
        med.put("childName", childName);

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