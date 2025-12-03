package com.example.smartair;

public interface LoginContract {
    interface View {
        void showLoading();
        void hideLoading();

        void showMessage(String message);

        boolean checkLocalOnboardingStatus(String role, String uid);

        void navigateToOnboarding(String role, String uid);
        void navigateToDashboard(Class<?> targetActivity, String uid);
    }

    interface Presenter {
        void login(String email, String password, String selectedRole);
        void onDestroy();
        void checkAutoLogin(String selectedRole);
    }
}
