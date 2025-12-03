package com.example.smartair;

public interface ChildLoginModel {
    void verifyCode(String code, ChildLoginContract.LoginCallback callback);
}
