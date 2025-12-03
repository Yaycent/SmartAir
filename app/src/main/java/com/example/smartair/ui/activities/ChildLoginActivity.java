package com.example.smartair.ui.activities;

import static com.example.smartair.utils.Constants.KEY_ROLE;
import static com.example.smartair.utils.Constants.ROLE_CHILD;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartair.mvp.ChildLoginPresenter;
import com.example.smartair.R;
import com.example.smartair.mvp.ChildLoginContract;
import com.example.smartair.mvp.ChildLoginModelImpl;
import com.google.firebase.auth.FirebaseAuth;
/**
 * ChildLoginActivity.java
 * <p>
 * Handles the authentication process specifically for Child users (Requirement R1).
 * This class serves as the <b>View</b> layer in the MVP (Model-View-Presenter) architecture.
 * </p>
 * <b>Key Responsibilities:</b>
 * <ul>
 * <li>Captures user input (Child Code) and delegates login logic to the {@link ChildLoginPresenter}.</li>
 * <li>Updates the UI based on presenter callbacks (Loading, Errors, Success).</li>
 * <li>Manages session persistence using SharedPreferences upon successful login.</li>
 * <li>Handles navigation routing to either Onboarding or the Child Dashboard.</li>
 * </ul>
 *
 * @author Judy Xu
 * @version 1.0
 * @see ChildLoginContract
 * @see ChildLoginPresenter
 */
public class ChildLoginActivity extends AppCompatActivity implements ChildLoginContract.View {
    private EditText etCode;
    private Button btnLogin;
    private ChildLoginContract.Presenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_child_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        presenter = new ChildLoginPresenter(this, new ChildLoginModelImpl());

        initViews();
    }

    private void initViews() {
        etCode = findViewById(R.id.etCode);
        btnLogin = findViewById(R.id.btnLogin);
        TextView tvLogoutChild = findViewById(R.id.tvLogoutChild);

        btnLogin.setOnClickListener(v -> {
            presenter.attemptLogin(etCode.getText().toString());
        });

        tvLogoutChild.setOnClickListener(v -> {
            presenter.logout();
        });
    }

    @Override
    public void showLoading() {
    }

    @Override
    public void hideLoading() {
    }

    @Override
    public void showError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void setButtonState(boolean enabled, String text) {
        btnLogin.setEnabled(enabled);
        btnLogin.setText(text);
    }

    @Override
    public void onLoginSuccess(String childUid, String childName, String parentId) {
        SharedPreferences prefs = getSharedPreferences("SmartAirChildPrefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("CHILD_UID", childUid)
                .putString("CHILD_NAME", childName)
                .putString("PARENT_UID", parentId)
                .putBoolean("IS_LOGGED_IN", true)
                .apply();

        checkOnboardingAndNavigate(childUid, childName, parentId);
    }

    private void checkOnboardingAndNavigate(String uid, String name, String pid) {
        SharedPreferences prefs = getSharedPreferences("onboarding", MODE_PRIVATE);
        boolean onboardingDone = prefs.getBoolean("done_Child_" + uid, false);

        Intent intent;
        if (!onboardingDone) {
            intent = new Intent(this, OnboardingActivity.class);
            intent.putExtra(KEY_ROLE, ROLE_CHILD);
            intent.putExtra("USER_UID", uid);
        } else {
            intent = new Intent(this, ChildDashboardActivity.class);
            intent.putExtra("CHILD_UID", uid);
        }

        intent.putExtra("CHILD_NAME", name);
        intent.putExtra("PARENT_UID", pid);
        startActivity(intent);
        finish();
    }

    @Override
    public void onLogout() {
        getSharedPreferences("SmartAirChildPrefs", Context.MODE_PRIVATE).edit().clear().apply();
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(this, RoleSelectionActivity.class);
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