package com.example.smartair;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
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


public class ProviderLoginActivity extends AppCompatActivity {

    private static final String TAG = "ProviderLoginActivity";
    private FirebaseAuth mAuth;

    private EditText editTextEmail;
    private EditText editTextPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_provider_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.providerlogin), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // UI elements
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        Button buttonLogin = findViewById(R.id.buttonLogin);
        TextView btnGoToRegister = findViewById(R.id.btnGoToRegister);

        // login button
        buttonLogin.setOnClickListener(v -> {
            signInUser();
        });

        // RegisterPage
        btnGoToRegister.setOnClickListener(v -> {

        });

        TextView btnForgotPassword = findViewById(R.id.btnForgotPassword);
        // Forgot Password Logic
        btnForgotPassword.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(ProviderLoginActivity.this, "Please enter your email to reset password.", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ProviderLoginActivity.this, "Password reset email sent.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ProviderLoginActivity.this, "Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
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
                            // NO JUMP: Only show success Toast as requested
                            Toast.makeText(ProviderLoginActivity.this, "Provider Login successful! (No navigation)", Toast.LENGTH_LONG).show();

                        } else {
                            // login fail
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(ProviderLoginActivity.this, "Login failed: Check your email and password.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }


}