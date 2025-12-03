package com.example.smartair.mvp;
/**
 * Defines the contract between the View and the Presenter for the Login module.
 * <p>
 * This interface is central to the MVP (Model-View-Presenter) architecture,
 * decoupling the UI logic (View) from the business logic (Presenter).
 * </p>
 *
 * @author Judy Xu
 */
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
