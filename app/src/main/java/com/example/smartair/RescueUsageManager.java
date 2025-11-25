package com.example.smartair;

import android.util.Log;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;

public class RescueUsageManager {
    private static final String TAG = "RescueUsageManager";
    private final FirebaseFirestore db;

    public RescueUsageManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    // Start listening for rescue medication usage
    public void startListening() {
        db.collection("medicationLogs")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    for (DocumentChange dc : snap.getDocumentChanges()) {

                        String type = dc.getDocument().getString("type");
                        if (!"Rescue".equals(type)) continue;

                        Log.d(TAG, "Detected Rescue medication use → decreasing remainingDose");

                        decreaseDose();
                    }
                });
    }

    private void decreaseDose() {
        db.collection("medicine")
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) return;

                    String docId = snap.getDocuments().get(0).getId();

                    db.collection("medicine")
                            .document(docId)
                            .update("remainingDose", com.google.firebase.firestore.FieldValue.increment(-1))
                            .addOnSuccessListener(a -> Log.d(TAG, "remainingDose -1 applied"))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to update remainingDose", e));
                });
    }
}
