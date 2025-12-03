package com.example.smartair;

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
