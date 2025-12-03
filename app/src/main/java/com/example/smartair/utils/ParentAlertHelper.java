package com.example.smartair.utils;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
/**
 * ParentAlertHelper.java
 * <p>
 * Utility class responsible for analyzing health data to trigger safety alerts (Requirement R4 & R6).
 * It acts as a central hub for generating critical notifications for the Parent.
 * </p>
 * <b>Alert Conditions Handled:</b>
 * <ul>
 * <li><b>Red Zone:</b> Triggers when a child's PEF reading falls into the Red Zone (< 50% PB).</li>
 * <li><b>Rapid Rescue:</b> Alerts if rescue medication is used ≥3 times within 3 hours.</li>
 * <li><b>Worse Condition:</b> Notifies if a child reports feeling "Worse" after medication.</li>
 * <li><b>Inventory Issues:</b> Flags low stock (≤20%) or expiring medications (R3).</li>
 * <li><b>SOS/Triage:</b> Handles immediate alerts from the One-Tap Triage system.</li>
 * </ul>
 *
 * @author Jiayi Qi
 * @version 1.0
 */
public class ParentAlertHelper {

    private static final String TAG = "ParentAlertHelper";

    /**
     * Core Firestore writing function for creating parent alerts.
     * This method stores alert data into the "parentAlerts" collection.
     */
    public static void createParentAlertInFirestore(String parentUid,
                                                    String childUid,
                                                    String childName,
                                                    String message) {

        final String finalChildName = (childName == null) ? "Your child" : childName;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Step 1: read parent's sharing settings
        db.collection("users")
                .document(parentUid)
                .collection("settings")
                .document("preferences")
                .get()
                .addOnSuccessListener(prefDoc -> {

                    boolean shareEmergency =
                            prefDoc.exists() &&
                                    Boolean.TRUE.equals(prefDoc.getBoolean("shareEmergencyEvent"));

                    // Step 2: write alert based on preference
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("parentUid", parentUid);
                    alert.put("childUid", childUid);
                    alert.put("childName", finalChildName);
                    alert.put("message", message);
                    alert.put("timestamp", Timestamp.now());
                    alert.put("processed", false);
                    alert.put("sharedToProvider", shareEmergency);
                    db.collection("parentAlerts")
                            .add(alert)
                            .addOnSuccessListener(doc -> Log.d(TAG, "Alert created: " + doc.getId()))
                            .addOnFailureListener(e -> Log.e(TAG, "Alert creation failed", e));
                });
    }

    /**
     * Medication Low / Expiring Soon
     * Generates alerts when a medication is running low or close to expiry.
     */
    public static void alertMedicineLowOrExpired(String parentUid,
                                                 String childUid,
                                                 String childName,
                                                 String medName,
                                                 boolean low,
                                                 boolean expiringSoon) {

        if (childName == null) childName = "Your child";

        String message;
        if (low && expiringSoon) {
            message = childName + "'s " + medName + " is low AND expiring soon!";
        } else if (low) {
            message = childName + "'s " + medName + " is running low!";
        } else {
            message = childName + "'s " + medName + " is expiring soon!";
        }

        createParentAlertInFirestore(parentUid, childUid, childName, message);
    }

    /**
     * Excessive Rescue Medication Usage
     * Triggered when the child uses rescue medication too frequently.
     */
    public static void alertRescueOveruse(String parentUid,
                                          String childUid,
                                          String childName,
                                          int count) {

        if (childName == null) childName = "Your child";

        String message = childName + " used rescue medication " +
                count + " times in 3 hours!";

        createParentAlertInFirestore(parentUid, childUid, childName, message);
    }

    /**
     * Reports "Worse" condition after rescue medication.
     * This is typically triggered by the child's self-reporting.
     */
    public static void alertRescueWorse(String parentUid,
                                        String childUid,
                                        String childName,
                                        String feeling) {

        if (childName == null) childName = "Your child";

        String message = childName + " is feeling " +
                feeling + " after rescue medication!";

        createParentAlertInFirestore(parentUid, childUid, childName, message);
    }

    /**
     * PEF Red Zone Alert
     * Triggered when a PEF reading falls into a dangerous red zone.
     */
    public static void alertPEFRed(String parentUid,
                                   String childUid,
                                   String childName,
                                   double pefValue) {

        if (childName == null) childName = "Your child";

        String message = childName + "'s PEF reading is RED (" +
                pefValue + "). Immediate attention may be required!";

        createParentAlertInFirestore(parentUid, childUid, childName, message);
    }
}
