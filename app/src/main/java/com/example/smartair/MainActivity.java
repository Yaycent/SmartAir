package com.example.smartair;

import android.content.Intent;
import android.view.View;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import static com.example.smartair.Constants.*;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String selectedRole;

    private EditText editTextEmail;
    private EditText editTextPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        selectedRole = getIntent().getStringExtra(KEY_ROLE);
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // Already logged in: Jump to the homepage
            checkRoleAndRedirect(currentUser.getUid());
            return;
        }

        // UI elements
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        Button buttonLogin = findViewById(R.id.buttonLogin);
        ImageButton imageButtonBackMain = findViewById(R.id.imageButtonBackMain);
        TextView btnGoToRegister = findViewById(R.id.btnGoToRegister);
        TextView btnForgotPassword = findViewById(R.id.btnForgotPassword);

        // back button
        imageButtonBackMain.setOnClickListener(v->finish());

        // login
        buttonLogin.setOnClickListener(v -> {
            signInUser();
        });

        // RegisterPage
        btnGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            intent.putExtra(KEY_ROLE, selectedRole);
            startActivity(intent);
        });

        // Forget password
        btnForgotPassword.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter your email to reset password.", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "Password reset email sent.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

    }

    private void signInUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter your email address and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Call the Firebase Sign-in API
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Login successful
                            FirebaseUser user = mAuth.getCurrentUser();

                            // check role and jump to the dashboard.
                            if (user != null) {
                                checkRoleAndRedirect(user.getUid());
                            }

                        } else {
                            // login fail
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Login failed: Check your email and password.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void checkRoleAndRedirect(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String actualRole;

                    // get role
                    if (documentSnapshot.exists() && documentSnapshot.contains("role")) {
                        // new user
                        actualRole = documentSnapshot.getString("role");
                    } else {
                        // old user default to parent
                        Log.d(TAG, "Old user or no role detected, defaulting to Parent.");
                        actualRole = ROLE_PARENT;
                    }

                    // check if right role to right account
                    if (selectedRole != null && !selectedRole.equals(actualRole)) {

                        // wrong role
                        Toast.makeText(MainActivity.this,
                                "Access Denied: This account is registered as a " + actualRole,
                                Toast.LENGTH_LONG).show();

                        // Forced logout
                        mAuth.signOut();
                        return;
                    }

                    // right role
                    Toast.makeText(MainActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    Intent intent;
                    // if provider
                    if (ROLE_DOCTOR.equals(actualRole)) {
                        intent = new Intent(MainActivity.this, ProviderDashboardActivity.class);
                    } else {
                        // Default to parent
                        intent = new Intent(MainActivity.this, ParentDashboardActivity.class);
                    }

                    intent.putExtra(PARENT_UID, uid);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Login Check Failed: " +
                            e.getMessage(), Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                });
    }
}