package com.example.smartair.mvp;
/**
 * ChildLoginPresenter.java
 * <p>
 * Implements the Presenter layer for the Child Login module.
 * Acts as the intermediary between the View (ChildLoginActivity) and the Model (ChildLoginModel).
 * </p>
 * <b>Key Responsibilities:</b>
 * <ul>
 * <li><b>Input Validation:</b> Ensures the entered invite code is exactly 6 digits before processing.</li>
 * <li><b>State Management:</b> Updates the View's UI state (e.g., disabling buttons) during network operations.</li>
 * <li><b>Business Logic:</b> Delegates the code verification to the Model and handles the asynchronous results (Success/Error).</li>
 * </ul>
 *
 * @author Judy Xu
 * @version 1.0
 */
public class ChildLoginPresenter implements ChildLoginContract.Presenter{
    private ChildLoginContract.View view;
    private ChildLoginModel model;

    public ChildLoginPresenter(ChildLoginContract.View view, ChildLoginModel model) {
        this.view = view;
        this.model = model;
    }

    @Override
    public void attemptLogin(String code) {
        if (view == null) return;

        String trimCode = (code == null) ? "" : code.trim();
        if (trimCode.length() != 6) {
            view.showError("Please enter a 6-digit code");
            return;
        }

        view.setButtonState(false, "Verifying...");

        // Model
        model.verifyCode(trimCode, new ChildLoginContract.LoginCallback() {
            @Override
            public void onSuccess(String childUid, String childName, String parentId) {
                if (view != null) {
                    view.hideLoading();
                    view.onLoginSuccess(childUid, childName, parentId);
                }
            }

            @Override
            public void onError(String msg) {
                if (view != null) {
                    view.showError(msg);
                    view.setButtonState(true, "Connect");
                }
            }
        });
    }

    @Override
    public void logout() {
        if (view != null) {
            view.onLogout();
        }
    }

    @Override
    public void onDestroy() {
        view = null;
    }
}
