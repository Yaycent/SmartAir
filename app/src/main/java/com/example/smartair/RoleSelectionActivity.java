package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

import static com.example.smartair.Constants.*;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        // botton
        MaterialCardView btnKid = findViewById(R.id.btnRoleKid);
        MaterialCardView btnParent = findViewById(R.id.btnRoleParent);
        MaterialCardView btnDoctor = findViewById(R.id.btnRoleDoctor);

        // botton logic
        btnKid.setOnClickListener(v -> {
            Intent childIntent = new Intent(RoleSelectionActivity.this, ChildLoginActivity.class);
            childIntent.putExtra(KEY_ROLE, ROLE_CHILD);
            startActivity(childIntent);
        });

        btnParent.setOnClickListener(v -> {
            goToLogin(ROLE_PARENT);
        });

        btnDoctor.setOnClickListener(v -> {
            goToLogin(ROLE_DOCTOR);
        });
    }

    private void goToLogin(String role) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(KEY_ROLE, role);
        startActivity(intent);
    }

}
