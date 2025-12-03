package com.example.smartair.mvp;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
/**
 * ChildLoginModelImpl.java
 * <p>
 * Concrete implementation of the {@link ChildLoginModel} interface.
 * Acts as the <b>Model</b> layer in the MVP architecture for the Child Login module.
 * </p>
 * <b>Key Responsibilities:</b>
 * <ul>
 * <li><b>Firebase Integration:</b> Handles anonymous authentication to secure database access and performs Firestore queries.</li>
 * <li><b>Code Verification Logic:</b> Validates invite codes against specific business rules:
 * <ul>
 * <li>Checks if the code exists and matches the target role ("child").</li>
 * <li>Verifies that the code has not already been used.</li>
 * <li>Ensures the code has not expired.</li>
 * </ul>
 * </li>
 * <li><b>Data Retrieval & Update:</b> Fetches associated Child/Parent IDs upon success and marks codes as "used" in the database to prevent reuse.</li>
 * </ul>
 *
 * @author Judy Xu
 * @version 1.0
 */
public class ChildLoginModelImpl implements ChildLoginModel {
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public ChildLoginModelImpl() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public void verifyCode(String code, ChildLoginContract.LoginCallback callback) {
        // Anonymous Login
        auth.signInAnonymously()
                .addOnFailureListener(e -> callback.onError("Connection failed."))
                .addOnSuccessListener(authResult -> {
                    // Verify invitation code
                    checkFirestoreInvite(code, callback);
                });
    }

    private void checkFirestoreInvite(String code, ChildLoginContract.LoginCallback callback) {
        db.collection("invites").document(code).get()
                .addOnFailureListener(e -> callback.onError("Error checking code"))
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onError("Invalid Code: Code not found.");
                        return;
                    }

                    boolean isUsed = Boolean.TRUE.equals(doc.getBoolean("isUsed"));
                    String forWho = doc.getString("forWho");
                    Date expiresAt = doc.getDate("expiresAt");

                    if (!"child".equals(forWho)) {
                        callback.onError("Wrong Code Type: This code is for Doctors, not Children.");
                        return;
                    }
                    if (isUsed) {
                        callback.onError("Code Expired: This code has already been used.");
                        return;
                    }
                    if (expiresAt != null && expiresAt.before(new Date())) {
                        callback.onError("Code Expired: Time limit exceeded.");
                        return;
                    }

                    String parentId = doc.getString("parentId");
                    String targetChildId = doc.getString("targetChildId");
                    String targetChildName = doc.getString("targetChildName");

                    if (targetChildId == null || targetChildId.isEmpty()) {
                        callback.onError("Invalid Code: This code is not linked to a specific child.");
                        return;
                    }

                    // Mark as used
                    db.collection("invites").document(code).update("isUsed", true);

                    // Return successful data
                    callback.onSuccess(targetChildId, targetChildName, parentId);
                });
    }
}
