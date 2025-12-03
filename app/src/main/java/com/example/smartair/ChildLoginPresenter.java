package com.example.smartair;

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
