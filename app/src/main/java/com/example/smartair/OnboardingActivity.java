package com.example.smartair;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;


import java.util.ArrayList;
import java.util.List;

import static com.example.smartair.Constants.*;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button btnNext, btnSkip;

    private List<OnboardingPage> onboardingPages;
    private OnboardingAdapter adapter;
    private String userRole; // Parent / Child / Provider
    private String userUid;

    private String childName; // only for child
    private String parentUid; // only for child


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_onboarding);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Retrieve role passed from login / register screen
        userRole = getIntent().getStringExtra(KEY_ROLE);
        userUid = getIntent().getStringExtra("USER_UID");

        childName = getIntent().getStringExtra("CHILD_NAME");
        parentUid = getIntent().getStringExtra("PARENT_UID");

        initViews();
        initPages();
        initViewPager();
        initButtons();
    }

    /**
     * Initialize UI Elements.
     */
    private void initViews() {
        viewPager = findViewById(R.id.onbViewPager);
        btnNext = findViewById(R.id.onbBtnNext);
        btnSkip = findViewById(R.id.onbBtnSkip);
    }

    /**
     * Initialize pages based on user role.
     */
    private void initPages() {

        onboardingPages = new ArrayList<>();

        switch (userRole) {

            case ROLE_PARENT:
                onboardingPages.add(new OnboardingPage(
                        getString(R.string.on_parent_privacy_title),
                        getString(R.string.on_parent_privacy_text)
                ));
                onboardingPages.add(new OnboardingPage(
                        getString(R.string.on_parent_addchild_title),
                        getString(R.string.on_parent_addchild_text)
                ));
                onboardingPages.add(new OnboardingPage(
                        getString(R.string.on_parent_invite_title),
                        getString(R.string.on_parent_invite_text)
                ));
                break;

            case ROLE_DOCTOR:
                onboardingPages.add(new OnboardingPage(
                        getString(R.string.on_provider_readonly_title),
                        getString(R.string.on_provider_readonly_text)
                ));
                onboardingPages.add(new OnboardingPage(
                        getString(R.string.on_provider_invite_title),
                        getString(R.string.on_provider_invite_text)
                ));
                break;

            case ROLE_CHILD:
            default:
                onboardingPages.add(new OnboardingPage(
                        getString(R.string.on_child_sos_title),
                        getString(R.string.on_child_sos_text)
                ));
                onboardingPages.add(new OnboardingPage(
                        getString(R.string.on_child_tasks_title),
                        getString(R.string.on_child_tasks_text)
                ));
                onboardingPages.add(new OnboardingPage(
                        getString(R.string.on_child_privacy_title),
                        getString(R.string.on_child_privacy_text)
                ));
                break;
        }

        viewPager.setAdapter(new OnboardingAdapter(onboardingPages));
    }

    /**
     * Set adapter and register page callback
     */
    private void initViewPager() {
        adapter = new OnboardingAdapter(onboardingPages);
        viewPager.setAdapter(adapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {

                boolean isLastPage = (position == adapter.getItemCount() - 1);

                btnNext.setText(
                        isLastPage
                                ? getString(R.string.onboarding_done)
                                : getString(R.string.onboarding_next)
                );

                btnSkip.setVisibility(isLastPage ? Button.INVISIBLE : Button.VISIBLE);
            }
        });
    }


    /**
     * Handle Next & Skip buttons as well as UI updates based on page changes.
     */
    private void initButtons() {

        btnSkip.setOnClickListener(v -> finishOnboarding());

        btnNext.setOnClickListener(v -> {
            int currentPage = viewPager.getCurrentItem();
            int lastIndex = adapter.getItemCount() - 1;

            if (currentPage < lastIndex) {
                viewPager.setCurrentItem(currentPage + 1);
            } else {
                finishOnboarding();
            }
        });
    }

    /**
     * Completes the onboarding flow for the current user role.
     */
    private void finishOnboarding() {
        SharedPreferences prefs = getSharedPreferences("onboarding", MODE_PRIVATE);
        prefs.edit()
                .putBoolean("done_" + userRole + "_" + userUid, true)
                .apply();

        Intent intent;

        // Redirect the user to the correct dashboard depending on their role
        switch(userRole) {
            case ROLE_PARENT:
                intent = new Intent(this, ParentDashboardActivity.class);
                intent.putExtra(PARENT_UID, userUid);
                break;
            case ROLE_DOCTOR:
                intent = new Intent(this, ProviderDashboardActivity.class);
                break;
            case ROLE_CHILD:
                intent = new Intent(this, ChildDashboardActivity.class);
                intent.putExtra("CHILD_UID", userUid);
                intent.putExtra("CHILD_NAME", childName);
                intent.putExtra("PARENT_UID", parentUid);
                break;
            default:
                finish();
                return;
        }
        startActivity(intent);
        finish();
    }
}