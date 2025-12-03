package com.example.smartair;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static com.example.smartair.utils.Constants.*;

import com.example.smartair.mvp.LoginContract;
import com.example.smartair.mvp.LoginPresenter;
import com.example.smartair.ui.activities.ParentDashboardActivity;
import com.example.smartair.ui.activities.ProviderDashboardActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
/**
 * LoginPresenterTest.java
 * <p>
 * Comprehensive Unit Tests for the {@link LoginPresenter}.
 * Validates business logic, authentication flows, and role-based navigation.
 * </p>
 * <b>Test Coverage:</b>
 * <ul>
 * <li><b>Input Validation:</b> Ensures empty credentials trigger UI warnings.</li>
 * <li><b>Authentication:</b> Mocks Firebase Auth results (Success/Failure) to verify UI responses.</li>
 * <li><b>Role Security:</b> Verifies that role mismatches (e.g., Parent trying to login as Provider) are blocked.</li>
 * <li><b>Data Integrity:</b> Handles edge cases like missing user data or null roles in Firestore.</li>
 * <li><b>Navigation:</b> Confirms correct routing to Dashboard or Onboarding based on user state.</li>
 * </ul>
 *
 * @author Judy Xu
 * @version 1.0
 * @see LoginPresenter
 * @see LoginContract
 */
@RunWith(MockitoJUnitRunner.class)
public class LoginPresenterTest {

    @Mock private LoginContract.View view;
    @Mock private FirebaseAuth mAuth;
    @Mock private FirebaseFirestore db;
    @Mock private Task<AuthResult> authTask;
    @Mock private FirebaseUser firebaseUser;

    // Firestore Mocks
    @Mock private CollectionReference mockCollection;
    @Mock private DocumentReference mockDocument;
    @Mock private Task<DocumentSnapshot> mockFirestoreTask;
    @Mock private DocumentSnapshot mockDocumentSnapshot;

    private LoginPresenter presenter;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        presenter = new LoginPresenter(view, mAuth, db);

        when(db.collection(anyString())).thenReturn(mockCollection);
        when(mockCollection.document(anyString())).thenReturn(mockDocument);
        when(mockDocument.get()).thenReturn(mockFirestoreTask);

        when(mockFirestoreTask.addOnSuccessListener(any())).thenReturn(mockFirestoreTask);
        when(mockFirestoreTask.addOnFailureListener(any())).thenReturn(mockFirestoreTask);
    }

    // --- Tests ---

    @Test
    public void login_EmptyEmailOrPassword_ShowsMessage() {
        presenter.login("", "password123", ROLE_DOCTOR);
        verify(view).showMessage("Please enter your email address and password.");
    }

    @Test
    public void login_AuthFailure_ShowsError() {
        // Arrange
        mockAuthResult(false, null);
        when(authTask.getException()).thenReturn(new Exception("Auth Failed"));

        // Act
        presenter.login("a@b.com", "123", ROLE_DOCTOR);

        // Assert
        verify(view).hideLoading();
        verify(view).showMessage(contains("Auth Failed"));
    }

    @Test
    public void login_Success_RoleMatches_NavigatesToDoctorDashboard() {
        // Arrange
        mockAuthResult(true, "user123");
        mockFirestoreResult(ROLE_DOCTOR);
        when(view.checkLocalOnboardingStatus(ROLE_DOCTOR, "user123")).thenReturn(true);

        // Act
        presenter.login("doctor@test.com", "password", ROLE_DOCTOR);

        // Assert
        verify(view).hideLoading();
        verify(view).showMessage("Login successful!");
        verify(view).navigateToDashboard(ProviderDashboardActivity.class, "user123");
    }

    @Test
    public void login_Success_RoleParent_NavigatesToParentDashboard() {
        mockAuthResult(true, "user123");
        mockFirestoreResult(ROLE_PARENT);
        when(view.checkLocalOnboardingStatus(ROLE_PARENT, "user123")).thenReturn(true);

        presenter.login("parent@test.com", "password", ROLE_PARENT);

        verify(view).navigateToDashboard(ParentDashboardActivity.class, "user123");
    }

    @Test
    public void login_Success_OnboardingNotDone_NavigatesToOnboarding() {
        mockAuthResult(true, "user123");
        mockFirestoreResult(ROLE_DOCTOR);
        when(view.checkLocalOnboardingStatus(ROLE_DOCTOR, "user123")).thenReturn(false);

        presenter.login("doctor@test.com", "password", ROLE_DOCTOR);

        verify(view).navigateToOnboarding(ROLE_DOCTOR, "user123");
    }

    @Test
    public void login_RoleMismatch_ShowsError() {
        mockAuthResult(true, "user123");
        mockFirestoreResult(ROLE_PARENT);

        presenter.login("doctor@test.com", "password", ROLE_DOCTOR);

        verify(view).showMessage(contains("Access Denied"));
        verify(mAuth).signOut();
    }

    @Test
    public void login_UserDocNotFound_ShowsError() {
        mockAuthResult(true, "user123");

        setupAutoTriggerFirestoreSuccess();
        when(mockDocumentSnapshot.exists()).thenReturn(false);

        presenter.login("test@test.com", "password", ROLE_DOCTOR);

        verify(view).showMessage("User data not found in database.");
        verify(mAuth).signOut();
    }

    @Test
    public void login_FirestoreFailure_ShowsError() {
        mockAuthResult(true, "user123");

        doAnswer(invocation -> {
            OnFailureListener listener = invocation.getArgument(0);
            listener.onFailure(new Exception("Database connection failed"));
            return mockFirestoreTask;
        }).when(mockFirestoreTask).addOnFailureListener(any());

        presenter.login("test@test.com", "123", ROLE_DOCTOR);

        verify(view).showMessage(contains("Login Check Failed"));
    }

    @Test
    public void checkAutoLogin_UserLoggedIn_NavigatesToDashboard() {
        when(mAuth.getCurrentUser()).thenReturn(firebaseUser);
        when(firebaseUser.getUid()).thenReturn("user123");

        mockFirestoreResult(ROLE_DOCTOR);
        when(view.checkLocalOnboardingStatus(ROLE_DOCTOR, "user123")).thenReturn(true);

        presenter.checkAutoLogin(ROLE_DOCTOR);

        verify(view).navigateToDashboard(ProviderDashboardActivity.class, "user123");
    }

    @Test
    public void onDestroy_DetachesView() {
        presenter.onDestroy();
        presenter.login("", "", ROLE_DOCTOR);
        verify(view, never()).showMessage(anyString());
        verifyNoInteractions(view);
    }

    @Test
    public void login_AuthSuccess_ButUserIsNull_ShowsError() {
        when(mAuth.signInWithEmailAndPassword(anyString(), anyString())).thenReturn(authTask);
        when(authTask.isSuccessful()).thenReturn(true);
        when(mAuth.getCurrentUser()).thenReturn(null);

        doAnswer(invocation -> {
            ((OnCompleteListener) invocation.getArgument(0)).onComplete(authTask);
            return authTask;
        }).when(authTask).addOnCompleteListener(any());
        presenter.login("test@test.com", "123", ROLE_DOCTOR);

        verify(view).hideLoading();
        verify(view).showMessage("Login error: User is null.");
    }

    @Test
    public void login_RoleIsNull_ShowsError() {
        mockAuthResult(true, "user123");

        when(mockDocumentSnapshot.exists()).thenReturn(true);
        when(mockDocumentSnapshot.getString("role")).thenReturn(null);
        setupAutoTriggerFirestoreSuccess();

        presenter.login("test@test.com", "123", ROLE_DOCTOR);

        verify(view).showMessage("Error: User role is missing.");
        verify(mAuth).signOut();
    }

    @Test
    public void login_RoleIsEmptyString_ShowsError() {
        mockAuthResult(true, "user123");

        when(mockDocumentSnapshot.exists()).thenReturn(true);
        when(mockDocumentSnapshot.getString("role")).thenReturn("");
        setupAutoTriggerFirestoreSuccess();

        presenter.login("test@test.com", "123", ROLE_DOCTOR);

        verify(view).showMessage("Error: User role is missing.");
        verify(mAuth).signOut();
    }

    @Test
    public void login_UnknownRoleType_ShowsError() {
        String weirdRole = "ALIEN_INVADER";
        mockAuthResult(true, "user123");

        when(mockDocumentSnapshot.exists()).thenReturn(true);
        when(mockDocumentSnapshot.getString("role")).thenReturn(weirdRole);
        setupAutoTriggerFirestoreSuccess();

        when(view.checkLocalOnboardingStatus(weirdRole, "user123")).thenReturn(true);

        presenter.login("test@test.com", "123", weirdRole);

        verify(view).showMessage(contains("Unknown role type"));
        verify(mAuth).signOut();
    }

    // --- Helpers ---
    private void mockAuthResult(boolean isSuccess, String uid) {
        when(authTask.isSuccessful()).thenReturn(isSuccess);

        if (isSuccess) {
            when(mAuth.getCurrentUser()).thenReturn(firebaseUser);
            when(firebaseUser.getUid()).thenReturn(uid);
        }

        when(mAuth.signInWithEmailAndPassword(anyString(), anyString())).thenReturn(authTask);

        doAnswer(invocation -> {
            OnCompleteListener listener = invocation.getArgument(0);
            listener.onComplete(authTask);
            return authTask;
        }).when(authTask).addOnCompleteListener(any());
    }

    private void mockFirestoreResult(String role) {
        when(mockDocumentSnapshot.exists()).thenReturn(true);
        when(mockDocumentSnapshot.getString("role")).thenReturn(role);

        setupAutoTriggerFirestoreSuccess();
    }

    private void setupAutoTriggerFirestoreSuccess() {
        doAnswer(invocation -> {
            OnSuccessListener listener = invocation.getArgument(0);
            listener.onSuccess(mockDocumentSnapshot);
            return mockFirestoreTask;
        }).when(mockFirestoreTask).addOnSuccessListener(any());
    }
}