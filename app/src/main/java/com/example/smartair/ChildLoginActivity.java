package com.example.smartair;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.List;

public class ChildLoginActivity extends AppCompatActivity {
    private EditText etCode;
    private Button btnLogin;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LinearLayout layoutQuickLogin;
    private Button btnQuickLogin;
    private TextView tvLogoutChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_child_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        checkPreviousSession();
    }

    private void initViews() {
        etCode = findViewById(R.id.etCode);
        btnLogin = findViewById(R.id.btnLogin);
        layoutQuickLogin = findViewById(R.id.layoutQuickLogin);
        btnQuickLogin = findViewById(R.id.btnQuickLogin);
        tvLogoutChild = findViewById(R.id.tvLogoutChild);

        // code
        btnLogin.setOnClickListener(v -> {
            String code = etCode.getText().toString().trim();
            if (code.length() == 6) {
                signInAnonymouslyAndVerify(code);
            } else {
                Toast.makeText(this, "Please enter a 6-digit code", Toast.LENGTH_SHORT).show();
            }
        });

        // back
        tvLogoutChild.setOnClickListener(v -> {
            // A. 清除本地缓存
            clearLocalCache();
            Toast.makeText(this, "Cache Cleared", Toast.LENGTH_SHORT).show();

            // B. 跳转回角色选择页 (RoleSelectionActivity)
            Intent intent = new Intent(ChildLoginActivity.this, RoleSelectionActivity.class);
            // 清空返回栈，防止用户按手机返回键又回来
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void checkPreviousSession() {
        SharedPreferences prefs = getSharedPreferences("SmartAirChildPrefs", Context.MODE_PRIVATE);
        String savedUid = prefs.getString("CHILD_UID", null);
        String savedName = prefs.getString("CHILD_NAME", "Child");

        // 只有当 ID 存在且不为空时，才显示快速登录
        if (savedUid != null && !savedUid.isEmpty()) {
            layoutQuickLogin.setVisibility(View.VISIBLE);
            btnQuickLogin.setText("Continue as " + savedName);

            btnQuickLogin.setOnClickListener(v -> {
                // 用户主动点击了“继续”，才跳转
                goToDashboard(savedUid, savedName, prefs.getString("PARENT_UID", ""));
            });
        } else {
            layoutQuickLogin.setVisibility(View.GONE);
        }
    }

    // ... (signInAnonymouslyAndVerify 和 verifyInviteCode 保持不变) ...
    // ... (务必保留里面对于 targetChildId 的处理) ...

    private void signInAnonymouslyAndVerify(String code) {
        btnLogin.setEnabled(false);
        btnLogin.setText("Verifying...");
        auth.signInAnonymously()
                .addOnSuccessListener(authResult -> verifyInviteCode(code))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Connection failed.", Toast.LENGTH_SHORT).show();
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Connect");
                });
    }

    private void verifyInviteCode(String code) {
        db.collection("invites").document(code).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        showError("Invalid Code: Code not found."); // 码输错了
                        return;
                    }

                    boolean isUsed = Boolean.TRUE.equals(doc.getBoolean("isUsed"));
                    String forWho = doc.getString("forWho");
                    Date expiresAt = doc.getDate("expiresAt");

                    // 1. 检查类型 (最常见错误：家长选成了 Provider)
                    if (!"child".equals(forWho)) {
                        showError("Wrong Code Type: This code is for Doctors, not Children.");
                        return;
                    }
                    // 2. 检查是否已用过
                    if (isUsed) {
                        showError("Code Expired: This code has already been used.");
                        return;
                    }
                    // 3. 检查是否过期
                    if (expiresAt != null && expiresAt.before(new Date())) {
                        showError("Code Expired: Time limit exceeded.");
                        return;
                    }

                    String parentId = doc.getString("parentId");
                    String targetChildId = doc.getString("targetChildId");
                    String targetChildName = doc.getString("targetChildName");

                    // 验证通过，标记为已使用
                    db.collection("invites").document(code).update("isUsed", true);

                    if (targetChildId != null) {
                        saveAndStart(targetChildId, targetChildName, parentId);
                    } else {
                        selectChildProfile(parentId);
                    }
                })
                .addOnFailureListener(e -> showError("Error checking code"));
    }

    // ... selectChildProfile 等保持不变 ...

    private void saveAndStart(String childUid, String childName, String parentId) {
        SharedPreferences prefs = getSharedPreferences("SmartAirChildPrefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("CHILD_UID", childUid)
                .putString("CHILD_NAME", childName)
                .putString("PARENT_UID", parentId)
                .putBoolean("IS_LOGGED_IN", true)
                .apply();

        goToDashboard(childUid, childName, parentId);
    }

    // 修改跳转方法，接收参数，确保传给 Dashboard 的 Intent 里有数据
    private void goToDashboard(String uid, String name, String pid) {
        Intent intent = new Intent(this, ChildDashboardActivity.class);
        intent.putExtra("CHILD_UID", uid);
        intent.putExtra("CHILD_NAME", name);
        intent.putExtra("PARENT_UID", pid);
        startActivity(intent);
        finish();
    }

    private void showError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        btnLogin.setEnabled(true);
        btnLogin.setText("Connect");
    }

    private void clearLocalCache() {
        getSharedPreferences("SmartAirChildPrefs", Context.MODE_PRIVATE).edit().clear().apply();
        auth.signOut();
    }

    // ... selectChildProfile / showChildSelectionDialog 代码请保留 ...
    private void selectChildProfile(String parentId) {
        db.collection("children")
                .whereEqualTo("parentId", parentId)
                .get()
                .addOnSuccessListener(querySnap -> {
                    if (querySnap.isEmpty()) {
                        showError("Parent has no child profiles yet.");
                        return;
                    }

                    List<DocumentSnapshot> children = querySnap.getDocuments();

                    if (children.size() == 1) {
                        DocumentSnapshot child = children.get(0);
                        saveAndStart(child.getId(), child.getString("childName"), parentId);
                    } else {
                        showChildSelectionDialog(children, parentId);
                    }
                });
    }

    private void showChildSelectionDialog(List<DocumentSnapshot> children, String parentId) {
        String[] names = new String[children.size()];
        for (int i = 0; i < children.size(); i++) {
            names[i] = children.get(i).getString("childName");
        }

        new AlertDialog.Builder(this)
                .setTitle("Who are you?")
                .setItems(names, (dialog, which) -> {
                    DocumentSnapshot selected = children.get(which);
                    saveAndStart(selected.getId(), selected.getString("childName"), parentId);
                })
                .setCancelable(false)
                .show();
    }
}