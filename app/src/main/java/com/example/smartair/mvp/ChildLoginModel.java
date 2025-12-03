package com.example.smartair.mvp;
/**
 * ChildLoginModel.java
 * <p>
 * Defines the Model layer interface for the Child Login module.
 * Responsible for the business logic of verifying login codes against the remote database.
 * </p>
 * <b>Key Responsibilities:</b>
 * <ul>
 * <li><b>Data Abstraction:</b> Decouples the database implementation (Firestore) from the Presenter.</li>
 * <li><b>Code Verification:</b> Provides the contract for checking if an entered invite code is valid and retrieving associated user data.</li>
 * </ul>
 *
 * @author Judy Xu
 * @version 1.0
 */
public interface ChildLoginModel {
    void verifyCode(String code, ChildLoginContract.LoginCallback callback);
}
