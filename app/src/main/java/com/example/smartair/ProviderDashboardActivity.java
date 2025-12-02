package com.example.smartair;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.smartair.TriageAdapter;

public class ProviderDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String providerUid;

    // UI Refs
    private TextView tvHiParent, tvLogoutProvider;
    private Button btnLinkParent;
    private Spinner spinnerPatients;

    // Stats Cards
    private TextView tvAdherence, tvRescueCount, tvSymptomDays;
    private TextView tvCurrentZone;
    private View viewZoneIndicator;

    // Chart & Bars
    private LineChart chartProvider;
    private View barGreen, barYellow, barRed;
    private TextView tvGreenLabel, tvYellowLabel, tvRedLabel;

    // List
    private RecyclerView recyclerTriage;
    private TriageAdapter triageAdapter; // 你需要创建一个简单的 Adapter
    private ArrayList<TriageItem> triageList = new ArrayList<>();

    // Data Holders
    private ArrayList<String> patientNames = new ArrayList<>();
    private ArrayList<String> childIds = new ArrayList<>();
    private ArrayList<String> parentIds = new ArrayList<>(); // To look up medicine/alerts

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_dashboard);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            providerUid = auth.getCurrentUser().getUid();
        }

        initViews();
        loadLinkedPatients(); // 加载患者列表
    }

    private void initViews() {
        // Old IDs
        tvHiParent = findViewById(R.id.tvHiParent);
        tvLogoutProvider = findViewById(R.id.tvLogoutProvider);
        btnLinkParent = findViewById(R.id.btnLinkParent);

        // New IDs
        spinnerPatients = findViewById(R.id.spinnerPatients);
        tvAdherence = findViewById(R.id.tvAdherence);
        tvRescueCount = findViewById(R.id.tvRescueCount);
        tvSymptomDays = findViewById(R.id.tvSymptomDays);
        tvCurrentZone = findViewById(R.id.tvCurrentZone);
        viewZoneIndicator = findViewById(R.id.viewZoneIndicator);

        chartProvider = findViewById(R.id.chartProvider);
        barGreen = findViewById(R.id.barGreen);
        barYellow = findViewById(R.id.barYellow);
        barRed = findViewById(R.id.barRed);
        tvGreenLabel = findViewById(R.id.tvGreenLabel);
        tvYellowLabel = findViewById(R.id.tvYellowLabel);
        tvRedLabel = findViewById(R.id.tvRedLabel);

        recyclerTriage = findViewById(R.id.recyclerTriage);
        recyclerTriage.setLayoutManager(new LinearLayoutManager(this));

        // Setup simple adapter
        triageAdapter = new TriageAdapter(triageList);
        recyclerTriage.setAdapter(triageAdapter);

        // --- Logic Integration ---

        // 1. Link Parent Button Logic
        btnLinkParent.setOnClickListener(v -> showLinkDialog());

        // 2. Logout Logic
        tvLogoutProvider.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(ProviderDashboardActivity.this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // 3. Spinner Logic
        spinnerPatients.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < childIds.size()) {
                    String selectedChildId = childIds.get(position);
                    String selectedParentId = parentIds.get(position);
                    loadPatientStats(selectedChildId, selectedParentId);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // --- Data Loading ---

    private void loadLinkedPatients() {
        // 1. Find links for this provider
        db.collection("providerPatientLinks")
                .whereEqualTo("providerId", providerUid)
                .get()
                .addOnSuccessListener(linkSnaps -> {
                    patientNames.clear();
                    childIds.clear();
                    parentIds.clear();

                    if (linkSnaps.isEmpty()) {
                        patientNames.add("No Patients Linked");
                        updateSpinner();
                        return;
                    }

                    // 2. Get all parent IDs
                    List<String> linkedParentIds = new ArrayList<>();
                    for (DocumentSnapshot doc : linkSnaps) {
                        linkedParentIds.add(doc.getString("parentId"));
                    }

                    // 3. For each parent, find their children
                    // (Simplified: Fetching one by one. In production, use tasks.whenAll)
                    for (String pId : linkedParentIds) {
                        db.collection("children")
                                .whereEqualTo("parentId", pId)
                                .get()
                                .addOnSuccessListener(childSnaps -> {
                                    for (DocumentSnapshot child : childSnaps) {
                                        String name = child.getString("childName");
                                        String cid = child.getId();

                                        patientNames.add(name);
                                        childIds.add(cid);
                                        parentIds.add(pId);
                                    }
                                    updateSpinner();
                                });
                    }
                });
    }

    private void updateSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, patientNames);
        spinnerPatients.setAdapter(adapter);
    }

    private void loadPatientStats(String childUid, String parentUid) {
        // Reset Views
        tvRescueCount.setText("-");
        tvAdherence.setText("-");
        tvSymptomDays.setText("-");
        tvCurrentZone.setText("-");
        chartProvider.clear();

        db.collection("children").document(childUid).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) return;

                    // 获取权限 Map
                    Map<String, Object> settings = (Map<String, Object>) document.get("sharingSettings");

                    // 默认全是 false (如果家长还没设置过，为了隐私默认不显示)
                    boolean shareMeds = false;
                    boolean sharePEF = false;
                    boolean shareSymptoms = false;
                    boolean shareTriage = false;

                    if (settings != null) {
                        shareMeds = Boolean.TRUE.equals(settings.get("shareMeds"));
                        sharePEF = Boolean.TRUE.equals(settings.get("sharePEF"));
                        shareSymptoms = Boolean.TRUE.equals(settings.get("shareSymptoms"));
                        shareTriage = Boolean.TRUE.equals(settings.get("shareTriage"));
                    }

                    // 3. 根据权限决定是否加载数据

                    // --- 权限 A: 药物 (Rescue Count & Adherence) ---
                    if (shareMeds) {
                        fetchMedicineData(childUid);
                    } else {
                        tvRescueCount.setText("🔒");
                        tvAdherence.setText("🔒");
                    }

                    // --- 权限 B: 症状 (Symptom Days) ---
                    if (shareSymptoms) {
                        fetchSymptomData(childUid);
                    } else {
                        tvSymptomDays.setText("🔒");
                    }

                    // --- 权限 C: PEF (Chart & Zone) ---
                    if (sharePEF) {
                        fetchPEFData(childUid); // 画图表
                    } else {
                        chartProvider.clear();
                        chartProvider.setNoDataText("Access restricted by parent");
                        chartProvider.invalidate();
                        tvCurrentZone.setText("Locked");
                        viewZoneIndicator.setBackgroundColor(Color.LTGRAY);
                    }

                    // --- 权限 D: 分诊记录 (Triage List) ---
                    if (shareTriage) {
                        fetchTriageData(parentUid);
                    } else {
                        triageList.clear();
                        // 可以加一个伪造的 item 提示用户
                        triageList.add(new TriageItem("-", "Data Access Restricted", "Locked"));
                        triageAdapter.notifyDataSetChanged();
                    }

                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load permissions", Toast.LENGTH_SHORT).show());
    }

    private void fetchMedicineData(String childUid) {
        // 计算 Rescue 使用次数
        db.collection("medicine")
                .whereEqualTo("childUid", childUid)
                .whereEqualTo("medType", "Rescue")
                .get()
                .addOnSuccessListener(snaps -> {
                    int total = 0;
                    for(DocumentSnapshot doc : snaps) {
                        Long count = doc.getLong("weeklyRescueCount");
                        if(count != null) total += count;
                    }
                    tvRescueCount.setText(String.valueOf(total));
                    // 这里也可以顺便算一下 Adherence (依从性) 如果你有数据的话
                    tvAdherence.setText("85%"); // 示例假数据
                });
    }

    private void fetchSymptomData(String childUid) {
        // 示例：查询 symptomLogs 集合
        // db.collection("symptomLogs").whereEqualTo("childUid", childUid)...
        tvSymptomDays.setText("5"); // 暂时用假数据演示
    }

    private void fetchPEFData(String childUid) {
        // 调用你之前的画图逻辑
        loadPEFChartData(childUid);
    }

    private void fetchTriageData(String parentUid) {
        db.collection("parentAlerts")
                .whereEqualTo("parentUid", parentUid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(snaps -> {
                    triageList.clear();
                    SimpleDateFormat fmt = new SimpleDateFormat("MM/dd");
                    for(DocumentSnapshot doc : snaps) {
                        String msg = doc.getString("message");
                        Date date = doc.getDate("timestamp");
                        boolean processed = Boolean.TRUE.equals(doc.getBoolean("processed"));

                        TriageItem item = new TriageItem(
                                date != null ? fmt.format(date) : "-",
                                msg != null ? msg : "Alert",
                                processed ? "Handled" : "New"
                        );
                        triageList.add(item);
                    }
                    triageAdapter.notifyDataSetChanged();
                });
    }
    private void loadPEFChartData(String childUid) {
        // 1. 先获取孩子的 Personal Best (PB) 值，用于计算红黄绿区
        db.collection("children").document(childUid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    Double pbVal = doc.getDouble("childPB");
                    double PB = (pbVal != null) ? pbVal : 0.0;

                    if (PB == 0) {
                        chartProvider.setNoDataText("Parent hasn't set Personal Best (PB) yet.");
                        chartProvider.invalidate();
                        return;
                    }

                    // 2. 获取最近 30 天的 PEF 日志
                    long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);

                    db.collection("pefLogs")
                            .whereEqualTo("childUid", childUid)
                            .whereGreaterThan("timeStamp", thirtyDaysAgo)
                            .orderBy("timeStamp", Query.Direction.ASCENDING)
                            .get()
                            .addOnSuccessListener(querySnap -> {
                                ArrayList<Entry> entries = new ArrayList<>();
                                ArrayList<String> dates = new ArrayList<>();
                                SimpleDateFormat fmt = new SimpleDateFormat("MM/dd");

                                // 统计分布用的计数器
                                int greenCount = 0, yellowCount = 0, redCount = 0;
                                double lastValue = 0;

                                int index = 0;
                                for (DocumentSnapshot log : querySnap.getDocuments()) {
                                    Double val = log.getDouble("value");
                                    Long ts = log.getLong("timeStamp");
                                    if (val == null || ts == null) continue;

                                    float fVal = val.floatValue();
                                    entries.add(new Entry(index++, fVal));
                                    dates.add(fmt.format(new Date(ts)));
                                    lastValue = fVal;

                                    // 统计区域
                                    if (fVal >= PB * 0.8) greenCount++;
                                    else if (fVal >= PB * 0.5) yellowCount++;
                                    else redCount++;
                                }

                                // 3. 绘制图表
                                drawProviderChart(entries, dates, PB);

                                // 4. 更新区域分布条
                                updateZoneDistribution(greenCount, yellowCount, redCount);

                                // 5. 更新“当前区域”卡片 (用最后一个数据)
                                updateCurrentZoneCard(lastValue, PB);

                            })
                            .addOnFailureListener(e -> Log.e("Provider", "Error loading PEF", e));
                });
    }

    private void drawProviderChart(ArrayList<Entry> entries, ArrayList<String> dates, double PB) {
        if (entries.isEmpty()) {
            chartProvider.clear();
            chartProvider.setNoDataText("No PEF data shared.");
            return;
        }

        LineDataSet set = new LineDataSet(entries, "PEF Trends");
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        set.setDrawFilled(true);
        set.setDrawCircles(true);
        set.setLineWidth(2f);
        set.setCircleRadius(3f);
        set.setColor(Color.parseColor("#2196F3"));
        set.setCircleColor(Color.parseColor("#2196F3"));
        set.setFillColor(Color.parseColor("#E3F2FD"));
        set.setFillAlpha(100);
        set.setDrawValues(false);

        LineData data = new LineData(set);
        chartProvider.setData(data);

        // 设置 Y 轴红黄绿线
        YAxis left = chartProvider.getAxisLeft();
        left.removeAllLimitLines();
        left.setAxisMinimum(0f);
        left.setAxisMaximum((float) (PB * 1.2));

        LimitLine lGreen = new LimitLine((float)(PB * 0.8), "Green Zone");
        lGreen.setLineColor(Color.parseColor("#4CAF50"));
        lGreen.setLineWidth(1f);
        lGreen.enableDashedLine(10f, 10f, 0f);

        LimitLine lRed = new LimitLine((float)(PB * 0.5), "Red Zone");
        lRed.setLineColor(Color.parseColor("#F44336"));
        lRed.setLineWidth(1f);
        lRed.enableDashedLine(10f, 10f, 0f);

        left.addLimitLine(lGreen);
        left.addLimitLine(lRed);

        chartProvider.getAxisRight().setEnabled(false);
        chartProvider.getDescription().setEnabled(false);
        chartProvider.getLegend().setEnabled(false);

        // 设置 X 轴日期
        XAxis xAxis = chartProvider.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int i = (int) value;
                if (i >= 0 && i < dates.size()) return dates.get(i);
                return "";
            }
        });

        chartProvider.invalidate();
    }

    private void updateZoneDistribution(int green, int yellow, int red) {
        int total = green + yellow + red;
        if (total == 0) return;

        // 更新文字
        tvGreenLabel.setText("Green: " + green + "d");
        tvYellowLabel.setText("Yellow: " + yellow + "d");
        tvRedLabel.setText("Red: " + red + "d");

        // 更新进度条权重 (长度)
        setWeight(barGreen, (float) green / total);
        setWeight(barYellow, (float) yellow / total);
        setWeight(barRed, (float) red / total);
    }

    private void setWeight(View view, float weight) {
        // 防止权重为0导致布局问题，稍微给一点点
        if (weight == 0) weight = 0.001f;
        android.widget.LinearLayout.LayoutParams params =
                (android.widget.LinearLayout.LayoutParams) view.getLayoutParams();
        params.weight = weight * 100;
        view.setLayoutParams(params);
    }

    private void updateCurrentZoneCard(double value, double PB) {
        if (value == 0) {
            tvCurrentZone.setText("No Data");
            viewZoneIndicator.setBackgroundColor(Color.GRAY);
            return;
        }

        String zoneText;
        int color;

        if (value >= PB * 0.8) {
            zoneText = "Green Zone";
            color = Color.parseColor("#4CAF50");
        } else if (value >= PB * 0.5) {
            zoneText = "Yellow Zone";
            color = Color.parseColor("#FFC107");
        } else {
            zoneText = "Red Zone";
            color = Color.parseColor("#F44336");
        }

        tvCurrentZone.setText(zoneText);
        tvCurrentZone.setTextColor(color);
        viewZoneIndicator.setBackgroundColor(color);
    }

    // --- Link Dialog (Copied from your request) ---
    private void showLinkDialog() {
        final EditText input = new EditText(this);
        input.setHint("Enter 6-digit code");
        input.setPadding(50,50,50,50);

        new AlertDialog.Builder(this)
                .setTitle("Link Parent")
                .setMessage("Enter the invitation code provided by the parent:")
                .setView(input)
                .setPositiveButton("Link", (dialog, which) -> {
                    String code = input.getText().toString().trim();
                    if (!code.isEmpty()) {
                        verifyAndLink(code);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void verifyAndLink(String code) {
        db.collection("invites").document(code).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String forWho = document.getString("forWho");
                        boolean isUsed = Boolean.TRUE.equals(document.getBoolean("isUsed"));

                        if (!"provider".equals(forWho)) {
                            Toast.makeText(this, "Invalid code type.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (isUsed) {
                            Toast.makeText(this, "Code already used.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String parentId = document.getString("parentId");
                        createLink(parentId, code);
                    } else {
                        Toast.makeText(this, "Code not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error checking code", Toast.LENGTH_SHORT).show());
    }

    private void createLink(String parentId, String code) {
        Map<String, Object> linkData = new HashMap<>();
        linkData.put("providerId", providerUid);
        linkData.put("parentId", parentId);
        linkData.put("linkedAt", new Date());

        db.collection("providerPatientLinks")
                .add(linkData)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Parent linked successfully!", Toast.LENGTH_SHORT).show();
                    db.collection("invites").document(code).update("isUsed", true);
                    loadLinkedPatients(); // Refresh list
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to link.", Toast.LENGTH_SHORT).show());
    }

    // --- Simple Classes for Adapter ---
    public static class TriageItem {
        String date;
        String trigger;
        String status;
        public TriageItem(String d, String t, String s) { date=d; trigger=t; status=s; }
    }

}