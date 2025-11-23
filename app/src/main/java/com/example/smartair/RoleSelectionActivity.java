package com.example.smartair;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout file for this Activity
        setContentView(R.layout.activity_role_selection);

        // Get the button instances from the layout
        Button childButton = findViewById(R.id.button_child);
        Button parentButton = findViewById(R.id.button_parent);
        Button providerButton = findViewById(R.id.button_provider);

        // Set click listener for the Child button
        childButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Display a Toast message on click
                Toast.makeText(RoleSelectionActivity.this, "Selected Role: Child", Toast.LENGTH_SHORT).show();
                // Future code for navigation goes here
            }
        });

        // Set click listener for the Parent button
        parentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Display a Toast message on click
                Toast.makeText(RoleSelectionActivity.this, "Selected Role: Parent", Toast.LENGTH_SHORT).show();
                // Future code for navigation goes here
            }
        });

        // Set click listener for the Provider button
        providerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Display a Toast message on click
                Toast.makeText(RoleSelectionActivity.this, "Selected Role: Provider", Toast.LENGTH_SHORT).show();
                // Future code for navigation goes here
            }
        });
    }
}
