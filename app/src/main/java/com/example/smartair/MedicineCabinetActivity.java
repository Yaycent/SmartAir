package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;

public class MedicineCabinetActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private MedicineAdapter adapter;
    private ArrayList<MedicineItem> list;
    private TextView textWarning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medicine_cabinet);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        // RecyclerView setup
        recyclerView = findViewById(R.id.recyclerMedicine);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        list = new ArrayList<>();
        adapter = new MedicineAdapter(this,list);
        recyclerView.setAdapter(adapter);

        textWarning = findViewById(R.id.textWarning);

        // Add medicine button
        Button btnAdd = findViewById(R.id.buttonAddMedicine);
        btnAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddMedicineActivity.class))
        );

        // Load from Firestore
        loadMedicines();

        // Listener
        new RescueUsageManager().startListening();
    }

    private void loadMedicines() {
        db.collection("medicine")
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;

                    list.clear();
                    boolean hasLowStock = false;
                    boolean hasExpired = false;

                    for (var doc : snap.getDocuments()) {
                        MedicineItem item = doc.toObject(MedicineItem.class);
                        // ensure Firestore doc ID is stored
                        if (item != null) {
                            item.setId(doc.getId());
                            list.add(item);

                            // low stock
                            if (item.getPercentage() <= 20) {
                                hasLowStock = true;
                            }

                            // expire
                            String exp = item.getExpiryDate();
                            if (exp != null && !exp.isEmpty()){
                                try {
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                                    Date expDate = sdf.parse(exp);

                                    if (expDate != null && expDate.before(new Date())) {
                                        hasExpired = true;
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }

                    if (hasExpired) {
                        textWarning.setText("!Some medicines have expired!");
                        textWarning.setTextColor(0xFFFF4444);  // red
                    }
                    else if (hasLowStock) {
                        textWarning.setText("!Some medicines are low (≤20%)!");
                        textWarning.setTextColor(0xFFFFBB33);  // orange
                    }
                    else {
                        textWarning.setText("");
                    }

                    adapter.notifyDataSetChanged();
                });
    }
}