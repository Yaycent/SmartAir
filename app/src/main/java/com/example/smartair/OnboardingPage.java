package com.example.smartair;

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
