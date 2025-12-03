package com.example.smartair;

import android.content.Intent;
import android.view.View;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import static com.example.smartair.Constants.*;

public class MainActivity extends AppCompatActivity implements LoginContract.View {
    private LoginPresenter presenter;
    private String selectedRole;

    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonLogin;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        presenter = new LoginPresenter(
                this,
                FirebaseAuth.getInstance(),
                FirebaseFirestore.getInstance()
        );
        selectedRole = getIntent().getStringExtra(KEY_ROLE);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            presenter.checkAutoLogin(selectedRole);
        }

        initViews();
    }

    private void initViews() {
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        ImageButton imageButtonBackMain = findViewById(R.id.imageButtonBackMain);
        TextView btnGoToRegister = findViewById(R.id.btnGoToRegister);
        TextView btnForgotPassword = findViewById(R.id.btnForgotPassword);

        progressBar = findViewById(R.id.progressBar);

        imageButtonBackMain.setOnClickListener(v -> finish());

        buttonLogin.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();
            presenter.login(email, password, selectedRole);
        });

        btnGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            intent.putExtra(KEY_ROLE, selectedRole);
            startActivity(intent);
        });

        btnForgotPassword.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            if (email.isEmpty()) {
                showMessage("Please enter your email to reset password.");
                return;
            }
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) showMessage("Password reset email sent.");
                        else showMessage("Failed: " + task.getException().getMessage());
                    });
        });
    }

    @Override
    public void showLoading() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        buttonLogin.setEnabled(false);
    }

    @Override
    public void hideLoading() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        buttonLogin.setEnabled(true);
    }

    @Override
    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean checkLocalOnboardingStatus(String role, String uid) {
        return getSharedPreferences("onboarding", MODE_PRIVATE)
                .getBoolean("done_" + role + "_" + uid, false);
    }

    @Override
    public void navigateToOnboarding(String role, String uid) {
        Intent intent = new Intent(MainActivity.this, OnboardingActivity.class);
        intent.putExtra(KEY_ROLE, role);
        intent.putExtra("USER_UID", uid);
        startActivity(intent);
        finish();
    }

    @Override
    public void navigateToDashboard(Class<?> targetActivity, String uid) {
        Intent intent = new Intent(MainActivity.this, targetActivity);
        intent.putExtra(PARENT_UID, uid);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        presenter.onDestroy();
        super.onDestroy();
    }
}