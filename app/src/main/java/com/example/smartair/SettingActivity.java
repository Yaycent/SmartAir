package com.example.smartair;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

import static com.example.smartair.Constants.*;

public class SettingActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

    // --- Original UI Components (Action Plan) ---
    private EditText editGreen, editYellow, editRed;
    private Button btnSave;
    private ImageButton btnBack;

    // --- New UI Components (Sharing Switches) ---
    private SwitchMaterial switchShareMeds, switchSharePEF, switchShareSymptoms, switchShareTriage;

    // --- Firebase & Data ---
    private FirebaseFirestore db;
    private String parentUid;
    private String childUid; // 从 Dashboard 传过来的

    // 防止加载数据时触发 Switch 监听器的标志位
    private boolean isUpdatingUI = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setting);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Initialize Firebase
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            parentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Error: Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Get Child UID from Intent
        childUid = getIntent().getStringExtra("CHILD_UID");
        if (childUid == null) {
            Toast.makeText(this, "No child selected. Some features may be disabled.", Toast.LENGTH_SHORT).show();
            // 如果 Action Plan 是全局的，这里不 finish 也可以，但 Sharing 功能无法使用
        }

        // 3. Init Views & Load Data
        initViews();
        loadActionPlanSettings(); // 原来的逻辑

        if (childUid != null) {
            loadSharingSettings();    // 新的逻辑
        }
    }

    private void initViews() {
        // --- 绑定 Action Plan 控件 ---
        editGreen = findViewById(R.id.editGreenZone);
        editYellow = findViewById(R.id.editYellowZone);
        editRed = findViewById(R.id.editRedZone);
        btnSave = findViewById(R.id.btnSaveAll);
        btnBack = findViewById(R.id.btnBack);

        // --- 绑定 Sharing Switches 控件 ---
        switchShareMeds = findViewById(R.id.switchShareMeds);
        switchSharePEF = findViewById(R.id.switchSharePEF);
        switchShareSymptoms = findViewById(R.id.switchShareSymptoms);
        switchShareTriage = findViewById(R.id.switchShareTriage);

        // --- 设置监听器 ---

        // 保存 Action Plan
        btnSave.setOnClickListener(v -> saveActionPlanSettings());

        // 返回按钮
        btnBack.setOnClickListener(v -> finish());

        // 设置开关监听 (只有有 childUid 时才有效)
        if (childUid != null) {
            setupSwitchListener(switchShareMeds, "shareMeds");
            setupSwitchListener(switchSharePEF, "sharePEF");
            setupSwitchListener(switchShareSymptoms, "shareSymptoms");
            setupSwitchListener(switchShareTriage, "shareTriage");
        } else {
            // 如果没选孩子，禁用开关
            disableSwitches();
        }
    }

    // ==========================================
    // PART 1: Action Plan Logic (Original Code)
    // ==========================================

    private void loadActionPlanSettings() {
        DocumentReference docRef = db.collection("users").document(parentUid)
                .collection("settings").document("action_plan");

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String g = documentSnapshot.getString("greenZone");
                String y = documentSnapshot.getString("yellowZone");
                String r = documentSnapshot.getString("redZone");

                editGreen.setText(g != null ? g : DEFAULT_GREEN);
                editYellow.setText(y != null ? y : DEFAULT_YELLOW);
                editRed.setText(r != null ? r : DEFAULT_RED);
            } else {
                editGreen.setText(DEFAULT_GREEN);
                editYellow.setText(DEFAULT_YELLOW);
                editRed.setText(DEFAULT_RED);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load settings", e);
            Toast.makeText(this, "Failed to load Action Plan", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveActionPlanSettings() {
        String rawGreen = editGreen.getText().toString().trim();
        String rawYellow = editYellow.getText().toString().trim();
        String rawRed = editRed.getText().toString().trim();

        String finalGreenText = rawGreen.isEmpty() ? DEFAULT_GREEN : rawGreen;
        String finalYellowText = rawYellow.isEmpty() ? DEFAULT_YELLOW : rawYellow;
        String finalRedText = rawRed.isEmpty() ? DEFAULT_RED : rawRed;

        Map<String, Object> data = new HashMap<>();
        data.put("greenZone", finalGreenText);
        data.put("yellowZone", finalYellowText);
        data.put("redZone", finalRedText);
        data.put("updatedAt", System.currentTimeMillis());

        db.collection("users").document(parentUid)
                .collection("settings").document("action_plan")
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
                    // 更新 UI 显示最终值
                    editGreen.setText(finalGreenText);
                    editYellow.setText(finalYellowText);
                    editRed.setText(finalRedText);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ==========================================
    // PART 2: Data Sharing Logic (New Code)
    // ==========================================

    private void loadSharingSettings() {
        isUpdatingUI = true; // 锁定监听器

        db.collection("children").document(childUid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // 读取 Map 字段 "sharingSettings"
                        Map<String, Object> settings = (Map<String, Object>) document.get("sharingSettings");

                        if (settings != null) {
                            switchShareMeds.setChecked(Boolean.TRUE.equals(settings.get("shareMeds")));
                            switchSharePEF.setChecked(Boolean.TRUE.equals(settings.get("sharePEF")));
                            switchShareSymptoms.setChecked(Boolean.TRUE.equals(settings.get("shareSymptoms")));
                            switchShareTriage.setChecked(Boolean.TRUE.equals(settings.get("shareTriage")));
                        }
                    }
                    isUpdatingUI = false; // 解锁
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load sharing settings", e);
                    isUpdatingUI = false;
                });
    }

    private void setupSwitchListener(SwitchMaterial switchView, String key) {
        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdatingUI) {
                updateSharingSetting(key, isChecked);
            }
        });
    }

    private void updateSharingSetting(String key, boolean value) {
        // 更新 children/{childUid} 文档内的 sharingSettings Map
        Map<String, Object> nestedData = new HashMap<>();
        nestedData.put(key, value);

        Map<String, Object> rootData = new HashMap<>();
        rootData.put("sharingSettings", nestedData);

        db.collection("children").document(childUid)
                .set(rootData, SetOptions.merge()) // 必须用 merge，否则会覆盖其他数据
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Update sharing setting failed", e);
                    Toast.makeText(this, "Failed to update setting", Toast.LENGTH_SHORT).show();
                    // 可选：如果失败，可以在这里把 UI 拨回去
                });
    }

    private void disableSwitches() {
        switchShareMeds.setEnabled(false);
        switchSharePEF.setEnabled(false);
        switchShareSymptoms.setEnabled(false);
        switchShareTriage.setEnabled(false);
    }
}