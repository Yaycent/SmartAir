package com.example.smartair;

import android.util.Log;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class RescueUsageManager {
    private static final String TAG = "RescueUsageManager";
    private final FirebaseFirestore db;
    private ListenerRegistration registration = null;
    public RescueUsageManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Stop previous listeners.
     */
    public void stop() {
        if (registration != null) {
            Log.d(TAG, "Removing previous listener to prevent duplicate triggers.");
            registration.remove();
            registration = null;
        }
    }

    /**
     * Listens for new medication logs from the child
     * in the Firestore. If a new Rescue log is detected,
     * inventory will be updated (decrease inventory ONCE per log).
     */
    public void startListening(String parentUid, String childUid) {
        stop();

        if (parentUid == null || childUid == null) {
            Log.e(TAG, "startListening called with null UIDs. parentUid=" + parentUid
                    + " childUid=" + childUid);
            return;
        }

        Log.d(TAG, "Start listening for Rescue logs. parent=" + parentUid
                + " child=" + childUid);

        registration = db.collection("medicationLogs")
                .whereEqualTo("parentUid", parentUid) // only see own child logs
                .whereEqualTo("childUid", childUid) // one child one inventory
                .whereEqualTo("type", "Rescue")      // only rescue logs
                .whereEqualTo("processed", false)    // only unprocessed logs
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    for (DocumentChange dc : snap.getDocumentChanges()) {

                        // Only react to NEW logs
                        if (dc.getType() != DocumentChange.Type.ADDED) continue;

                        String type = dc.getDocument().getString("type");
                        if (!"Rescue".equals(type)) continue;

                        String medicineId = dc.getDocument().getString("medicineId");
                        if (medicineId == null) {
                            Log.e(TAG, "Rescue log missing medicineId!");
                            continue;
                        }

                        Long doseCountLong = dc.getDocument().getLong("doseCount");
                        int doseCount = (doseCountLong != null) ? doseCountLong.intValue() : 1;

                        String logId = dc.getDocument().getId();

                        Log.d(TAG, "Detected Rescue use → Decrease "
                                + doseCount + " from medicine " + medicineId);

                        decreaseDose(medicineId, doseCount, logId);
                    }
                });
    }

    /**
     * Decreases the remaining dose of the Rescue
     * medication by doseCount. This is triggered when the child
     * submits a Rescue log.
     */
    private void decreaseDose(String medicineId, int doseCount, String logId) {
        db.collection("medicine")
                .document(medicineId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Log.e(TAG, "Medicine not found: " + medicineId);
                        return;
                    }

                    Long remainingLong = doc.getLong("remainingDose");
                    if (remainingLong == null) return;

                    int remaining = remainingLong.intValue();

                    // Clamp doseCount so it never goes negative
                    int actualDecrease = Math.min(doseCount, remaining);

                    Log.d(TAG, "[RescueUsageManager] Will decrease "
                            + actualDecrease + " (remaining=" + remaining + ")");

                    // Apply decrease
                    db.collection("medicine")
                            .document(medicineId)
                            .update("remainingDose", FieldValue.increment(-actualDecrease))
                            .addOnSuccessListener(a -> {
                                Log.d(TAG, "[RescueUsageManager] Applied -"
                                        + actualDecrease + " to " + medicineId);

                                // Mark log as processed
                                db.collection("medicationLogs")
                                        .document(logId)
                                        .update("processed", true);
                            })
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "Failed to update dose", e));
                });

    }
}
