package com.example.smartair.mvp;
/**
 * ChildLoginContract.java
 * <p>
 * Defines the interface contract between the View, Presenter, and Model for the Child Login module.
 * This ensures a clear separation of concerns, allowing the business logic to be tested independently of the Android UI.
 * </p>
 * <b>Components:</b>
 * <ul>
 * <li><b>View:</b> Methods to update the UI (loading state, error messages, navigation).</li>
 * <li><b>Presenter:</b> Methods to handle user actions (login attempt via code, logout).</li>
 * <li><b>LoginCallback:</b> Interface for the Model to return async login results to the Presenter.</li>
 * </ul>
 *
 * @author Judy Xu
 * @version 1.0
 */
public interface ChildLoginContract {

    interface View {
        void showLoading();
        void hideLoading();
        void showError(String msg);
        void setButtonState(boolean enabled, String text);

        void onLoginSuccess(String childUid, String childName, String parentId);

        void onLogout();
    }

    // Presenter
    interface Presenter {
        void attemptLogin(String code);
        void logout();
        void onDestroy();
    }

    // Model
    interface LoginCallback {
        void onSuccess(String childUid, String childName, String parentId);
        void onError(String msg);
    }
}
