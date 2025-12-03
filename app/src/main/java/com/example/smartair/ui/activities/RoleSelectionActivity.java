package com.example.smartair.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartair.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import static com.example.smartair.utils.Constants.*;
/**
 * RoleSelectionActivity.java
 * <p>
 * Serves as the initial entry point for the application (Requirement R1).
 * Allows users to select their identity (Parent, Child, or Healthcare Provider)
 * and directs them to the appropriate login flow.
 * </p>
 * <b>Key Functionalities:</b>
 * <ul>
 * <li><b>Auto-Login Check:</b> Verifies if a user is currently signed in via Firebase Auth and redirects them to their dashboard automatically.</li>
 * <li><b>Role Routing:</b> Provides distinct entry paths for Parents/Providers (via Email/Password) and Children (via Child Code).</li>
 * <li><b>User Experience:</b> Uses MaterialCardView for large, easy-to-tap selection buttons.</li>
 * </ul>
 *
 * @author Judy Xu
 * @version 1.0
 */
public class RoleSelectionActivity extends AppCompatActivity {

    private static final String TAG = "RoleSelectionActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check if login FIRST
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // if already login, go to dashboard
            checkRoleAndRedirect(currentUser.getUid());
            return;
        }

        // if not
        setContentView(R.layout.activity_role_selection);

        // button
        MaterialCardView btnKid = findViewById(R.id.btnRoleKid);
        MaterialCardView btnParent = findViewById(R.id.btnRoleParent);
        MaterialCardView btnDoctor = findViewById(R.id.btnRoleDoctor);


        // I am a Kid
        btnKid.setOnClickListener(v -> {
            Intent kidIntent = new Intent(RoleSelectionActivity.this, ChildLoginActivity.class);
            kidIntent.putExtra(KEY_ROLE, ROLE_CHILD);
            startActivity(kidIntent);
        });

        // Parent
        btnParent.setOnClickListener(v -> {
            goToLogin(ROLE_PARENT);
        });

        // Healthcare Provider
        btnDoctor.setOnClickListener(v -> {
            goToLogin(ROLE_DOCTOR);
        });
    }

    // Login
    private void goToLogin(String role) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(KEY_ROLE, role);
        startActivity(intent);
    }

    // same logic as MainActivity
    private void checkRoleAndRedirect(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String role;

                    if (documentSnapshot.exists() && documentSnapshot.contains("role")) {
                        role = documentSnapshot.getString("role");
                    } else {
                        Log.d(TAG, "Old user detected, defaulting to Parent.");
                        role = ROLE_PARENT;
                    }

                    Intent intent;
                    if (ROLE_DOCTOR.equals(role)) {
                        intent = new Intent(this, ProviderDashboardActivity.class);
                    } else {
                        intent = new Intent(this, ParentDashboardActivity.class);
                    }

                    intent.putExtra(PARENT_UID, uid);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Auto login check failed", e);
                    mAuth.signOut();
                    setContentView(R.layout.activity_role_selection);
                    Toast.makeText(this, "Auto login failed, please select role again.", Toast.LENGTH_SHORT).show();
                });
    }

}
