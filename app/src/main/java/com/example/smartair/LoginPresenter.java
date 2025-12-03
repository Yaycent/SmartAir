package com.example.smartair;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import static com.example.smartair.Constants.*;

public class LoginPresenter implements LoginContract.Presenter{
    private LoginContract.View view;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public LoginPresenter(LoginContract.View view, FirebaseAuth mAuth, FirebaseFirestore db) {
        this.view = view;
        this.mAuth = mAuth;
        this.db = db;
    }

    @Override
    public void login(String email, String password, String selectedRole) {
        if (view == null) return;

        if (email.isEmpty() || password.isEmpty()) {
            view.showMessage("Please enter your email address and password.");
            return;
        }

        view.showLoading();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (view == null) return;

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkRole(user.getUid(), selectedRole);
                        } else {
                            view.hideLoading();
                            view.showMessage("Login error: User is null.");
                        }
                    } else {
                        view.hideLoading();
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed.";
                        view.showMessage("Login failed: " + errorMsg);
                    }
                });
    }

    private void checkRole(String uid, String selectedRole) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (view == null) return;
                    view.hideLoading();

                    if (!documentSnapshot.exists()) {
                        view.showMessage("User data not found in database.");
                        mAuth.signOut();
                        return;
                    }

                    String actualRole = documentSnapshot.getString("role");

                    if (actualRole == null || actualRole.isEmpty()) {
                        view.showMessage("Error: User role is missing.");
                        mAuth.signOut();
                        return;
                    }

                    if (selectedRole != null && !selectedRole.equals(actualRole)) {
                        view.showMessage("Access Denied: This account is registered as a " + actualRole);
                        mAuth.signOut();
                        return;
                    }

                    boolean isOnboardingDone = view.checkLocalOnboardingStatus(actualRole, uid);

                    if (!isOnboardingDone) {
                        view.navigateToOnboarding(actualRole, uid);
                    } else {
                        Class<?> targetClass;
                        if (ROLE_DOCTOR.equals(actualRole)) {
                            targetClass = ProviderDashboardActivity.class;
                        } else if (ROLE_PARENT.equals(actualRole)) {
                            targetClass = ParentDashboardActivity.class;
                        } else {
                            view.showMessage("Unknown role type: " + actualRole);
                            mAuth.signOut();
                            return;
                        }
                        view.showMessage("Login successful!");
                        view.navigateToDashboard(targetClass, uid);
                    }
                })
                .addOnFailureListener(e -> {
                    if (view != null) {
                        view.hideLoading();
                        view.showMessage("Login Check Failed: " + e.getMessage());
                        mAuth.signOut();
                    }
                });
    }

    @Override
    public void onDestroy() {
        view = null;
    }

    @Override
    public void checkAutoLogin(String selectedRole) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            view.showLoading();
            checkRole(user.getUid(), selectedRole);
        }
    }

}
