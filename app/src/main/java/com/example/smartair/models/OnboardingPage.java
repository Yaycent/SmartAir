package com.example.smartair.models;
/**
 * OnboardingPage.java
 * <p>
 * Data model representing a single slide in the Onboarding tutorial (Requirement R1).
 * Used to dynamically populate the ViewPager with role-specific titles and descriptions.
 * </p>
 * <b>Key Fields:</b>
 * <ul>
 * <li><b>Title:</b> The headline text for the tutorial step (e.g., "Privacy First").</li>
 * <li><b>Description:</b> The detailed explanation text for the user.</li>
 * </ul>
 *
 * @author Zhan Tian
 * @version 1.0
 */
public class OnboardingPage {
    private final String pageTitle;
    private final String pageDescription;

    public OnboardingPage(String pageTitle, String pageDescription) {
        this.pageTitle = pageTitle;
        this.pageDescription = pageDescription;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public String getPageDescription() {
        return pageDescription;
    }

}
