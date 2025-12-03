package com.example.smartair;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.smartair.mvp.ChildLoginContract;
import com.example.smartair.mvp.ChildLoginModel;
import com.example.smartair.mvp.ChildLoginPresenter;
/**
 * ChildLoginPresenterTest.java
 * <p>
 * Local Unit Tests for the {@link ChildLoginPresenter}.
 * This class fulfills the assignment requirement to validate Presenter logic using JUnit and Mockito.
 * </p>
 * <b>Test Coverage:</b>
 * <ul>
 * <li><b>Input Validation:</b> Verifies that codes shorter than 6 digits trigger an immediate error (No Model call).</li>
 * <li><b>Success Flow:</b> Simulates a successful database response and verifies that the View navigates to the Dashboard.</li>
 * <li><b>Error Handling:</b> Simulates database errors (e.g., "Code Expired") and verifies the View displays the appropriate message.</li>
 * <li><b>Logout:</b> Verifies that the logout action is correctly delegated to the View.</li>
 * </ul>
 *
 * @author Judy Xu
 * @version 1.0
 * @see ChildLoginPresenter
 */
@RunWith(MockitoJUnitRunner.class)
public class ChildLoginPresenterTest {

    @Mock
    private ChildLoginContract.View view;

    @Mock
    private ChildLoginModel model;

    @Captor
    private ArgumentCaptor<ChildLoginContract.LoginCallback> callbackCaptor;

    private ChildLoginPresenter presenter;

    @Before
    public void setUp() {
        presenter = new ChildLoginPresenter(view, model);
    }

    /**
     * Test 1: Input Validation - Length Less Than 6 Digits
     * Expected: Display an error and do not request the Model
     */
    @Test
    public void attemptLogin_shortCode_showsError() {
        String shortCode = "123";

        presenter.attemptLogin(shortCode);

        verify(view).showError("Please enter a 6-digit code");
        verify(model, never()).verifyCode(any(), any());
    }

    /**
     * Test 2: Successful Login Process
     * Expected: Button state changes -> Model is invoked -> Success callback ->
     * Hide loading indicator -> Dashboard
     */
    @Test
    public void attemptLogin_validCode_success_navigates() {
        String validCode = "123456";
        String uid = "uid_1";
        String name = "Tom";
        String pid = "parent_1";

        presenter.attemptLogin(validCode);

        verify(view).setButtonState(false, "Verifying...");

        verify(model).verifyCode(eq(validCode), callbackCaptor.capture());

        callbackCaptor.getValue().onSuccess(uid, name, pid);

        verify(view).hideLoading();
        verify(view).onLoginSuccess(uid, name, pid);

        verify(view, never()).showError(any());
    }

    /**
     * Test 3: Invitation code has expired
     * Expected: Model invoked -> Failure callback -> Error displayed -> Button restored
     */
    @Test
    public void attemptLogin_validCode_failure_showsError() {
        String validCode = "654321";
        String errorMsg = "Code Expired";

        presenter.attemptLogin(validCode);

        verify(model).verifyCode(eq(validCode), callbackCaptor.capture());

        callbackCaptor.getValue().onError(errorMsg);

        verify(view).showError(errorMsg);

        verify(view).setButtonState(true, "Connect");
    }

    /**
     * Test 4: Log Out
     * Expected: View Execute Exit Operation
     */
    @Test
    public void logout_delegatesToView() {
        presenter.logout();

        verify(view).onLogout();
    }

    @Test
    public void testOnDestroy() {
        presenter.onDestroy();
    }
}