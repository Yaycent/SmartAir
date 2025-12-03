package com.example.smartair.utils;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartair.R;
/**
 * BadgeLibrary.java
 * <p>
 * Activity responsible for displaying the collection of earned badges (Gamification).
 * Supports Requirement R3 by providing visual positive reinforcement for the child's adherence [cite: 89-91].
 * </p>
 * <b>Key Functionalities:</b>
 * <ul>
 * <li><b>Badge Gallery:</b> Displays icons/images representing milestones like "First Perfect Week" or "Low Rescue Month".</li>
 * <li><b>Motivation:</b> Acts as a reward center to encourage consistent medication use and technique practice.</li>
 * </ul>
 *
 * @author Tan Ngo
 * @version 1.0
 */
public class BadgeLibrary extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_badge_library);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Back button
        ImageButton imageButtonBackBadgeLibrary = findViewById(R.id.imageButtonBackBadgeLibrary);
        imageButtonBackBadgeLibrary.setOnClickListener(v->finish());
    }
}