package com.example.smartair;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.example.smartair.Constants.*;

public class ProviderDashboardActivity extends AppCompatActivity {
    private static final String TAG = "ProviderDashboard";

    // UI Components
    private TextView tvHiParent; // Used for "Hi, Doctor"
    private Button btnLinkParent;
    private Spinner spinnerPatients;
    private Spinner spinnerTimeRange;
    private TextView textViewTodayPEFZone;
    private LineChart chartPEF;
    private Button buttonSymptomHistory;
    private TextView tvLogoutProvider;
    private RecyclerView recyclerMedicineInventory;
    private TextView tvRescueSummary;
    private MaterialCardView cardMedicationInventory;

    // firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String providerUid;

    // Data
    private ArrayList<String> patientNames = new ArrayList<>();
    private ArrayList<String> patientIds = new ArrayList<>(); // Child UIDs
    private String activeChildUid = null;
    private String activeChildName = null;
    private  String parentUid;

    private MedicineAdapter medicineAdapter;
    private ArrayList<MedicineItem> medicineList;

    // Listeners
    private ListenerRegistration patientsListener;
    private ListenerRegistration sharingSettingsListener;
    private ListenerRegistration medicineListListener;
    private ListenerRegistration rescueMedicineListener;

    private MaterialCardView cardEmergencyAlerts;
    private LinearLayout containerEmergencyAlerts;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_provider_dashboard);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            providerUid = auth.getCurrentUser().getUid();
        } else {
            finish();
            return;
        }

        initViews();
        loadLinkedPatients();

    }

    private void initViews() {
        tvHiParent = findViewById(R.id.tvHiParent); // 也许这里应该叫 tvHiProvider?
        tvLogoutProvider = findViewById(R.id.tvLogoutProvider);
        btnLinkParent = findViewById(R.id.btnLinkParent);
        spinnerPatients = findViewById(R.id.spinnerPatients);
        spinnerTimeRange = findViewById(R.id.spinnerTimeRange);
        textViewTodayPEFZone = findViewById(R.id.textViewTodayPEFZone);
        chartPEF = findViewById(R.id.chartPEF);
        buttonSymptomHistory = findViewById(R.id.buttonSymptomHistory);
        tvLogoutProvider = findViewById(R.id.tvLogoutProvider);
        recyclerMedicineInventory = findViewById(R.id.recyclerMedicineInventory);
        tvRescueSummary = findViewById(R.id.tvRescueSummary);
        cardMedicationInventory = findViewById(R.id.cardMedicationInventory);

        cardEmergencyAlerts = findViewById(R.id.cardEmergencyAlerts);
        containerEmergencyAlerts = findViewById(R.id.containerEmergencyAlerts);


        recyclerMedicineInventory = findViewById(R.id.recyclerMedicineInventory);
        recyclerMedicineInventory.setLayoutManager(new LinearLayoutManager(this));

        medicineList = new ArrayList<>();
        // Note: MedicineAdapter might ideally be read-only for providers.
        medicineAdapter = new MedicineAdapter(this, medicineList);
        recyclerMedicineInventory.setAdapter(medicineAdapter);

        // Link Parent
        btnLinkParent.setOnClickListener(v -> showLinkParentDialog());

        // Logout
        tvLogoutProvider.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(ProviderDashboardActivity.this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Symptom History
        buttonSymptomHistory.setOnClickListener(v -> {
            if (activeChildUid == null) {
                Toast.makeText(this, "Please select a patient first.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(ProviderDashboardActivity.this, SymptomHistoryActivity.class);
            intent.putExtra(CHILD_UID, activeChildUid);
            intent.putExtra(CHILD_NAME, activeChildName);
            // We pass providerUid as parentUid or add a new logic in SymptomHistory to handle provider view
            // allowing read-only access
            intent.putExtra(PARENT_UID, providerUid);
            startActivity(intent);
        });

        // Time Range Spinner
        ArrayAdapter<String> rangeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"7 Days", "30 Days"});
        rangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimeRange.setAdapter(rangeAdapter);

        spinnerTimeRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (activeChildUid != null) {
                    int days = (position == 0) ? 7 : 30;
                    loadPEFData(activeChildUid, days);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Patient Spinner Selection
        spinnerPatients.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                stopPatientDataListeners();
                clearDashboardData();
                if (position <= 0) {
                    // "Select Patient" or empty state
                    activeChildUid = null;
                    activeChildName = null;
                    clearDashboardData();
                    return;
                }

                activeChildUid = patientIds.get(position);
                activeChildName = patientNames.get(position);

                listenToSharingSettings(activeChildUid);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }

    // --- Logic 1: Linking Parent ---

    private void showLinkParentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Link Parent");
        builder.setMessage("Enter the code provided by the parent:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("Link", (dialog, which) -> {
            String code = input.getText().toString().trim();
            if (!code.isEmpty()) {
                processInviteCode(code);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void processInviteCode(String code) {
        db.collection("invites").document(code).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String forWho = documentSnapshot.getString("forWho");
                        Boolean isUsed = documentSnapshot.getBoolean("isUsed");
                        Date expiresAt = documentSnapshot.getDate("expiresAt");
                        String parentId = documentSnapshot.getString("parentId");

                        if (isUsed != null && isUsed) {
                            Toast.makeText(this, "This code has already been used.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (expiresAt != null && new Date().after(expiresAt)) {
                            Toast.makeText(this, "This code has expired.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (!"provider".equals(forWho)) {
                            Toast.makeText(this, "Invalid code type.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Code is valid, link children
                        linkChildrenOfParent(parentId, code);

                    } else {
                        Toast.makeText(this, "Invalid code.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error checking code.", Toast.LENGTH_SHORT).show());
    }

    private void linkChildrenOfParent(String parentId, String code) {
        // Find all children belonging to this parent
        db.collection("children")
                .whereEqualTo("parentId", parentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "No children found for this parent.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    WriteBatch batch = db.batch();
                    // Update invite status
                    batch.update(db.collection("invites").document(code), "isUsed", true);

                    // Add providerUid to each child's "providerAccess" array
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        // We use arrayUnion so we don't overwrite existing providers
                        batch.update(doc.getReference(), "providerAccess", FieldValue.arrayUnion(providerUid));
                    }

                    batch.commit().addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Successfully linked parent!", Toast.LENGTH_SHORT).show();
                        loadLinkedPatients(); // Refresh list
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to link.", Toast.LENGTH_SHORT).show();
                    });
                });
    }

    private void listenToSharingSettings(String childUid) {
        if (sharingSettingsListener != null) {
            sharingSettingsListener.remove();
        }

        sharingSettingsListener = db.collection("children").document(childUid)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    // 获取 parentUid
                    if (snapshot.contains("parentId")) {
                        parentUid = snapshot.getString("parentId");
                        if (parentUid != null) {
                            // 只根据 parent 的设置来决定是否显示 Alert
                            loadSharedEmergencyAlertsForProvider(parentUid);
                        }
                    }

                    // 其他 sharing 项照旧
                    boolean shareMeds = false;
                    boolean sharePEF = false;
                    boolean shareSymptoms = false;

                    if (snapshot.contains("sharingSettings")) {
                        Map<String, Object> settings = (Map<String, Object>) snapshot.get("sharingSettings");
                        if (settings != null) {
                            shareMeds = Boolean.TRUE.equals(settings.get("shareMeds"));
                            sharePEF = Boolean.TRUE.equals(settings.get("sharePEF"));
                            shareSymptoms = Boolean.TRUE.equals(settings.get("shareSymptoms"));
                        }
                    }

                    updateSectionVisibility(childUid, shareMeds, sharePEF, shareSymptoms);
                });
    }



    private void updateSectionVisibility(String childUid, boolean shareMeds, boolean sharePEF, boolean shareSymptoms){

        // 1. Medicine
        if (shareMeds) {
            cardMedicationInventory.setVisibility(View.VISIBLE);
            tvRescueSummary.setVisibility(View.VISIBLE);
            if (medicineListListener == null) {
                loadMedicines(childUid);
                loadRescueSummary(childUid);
            }
        } else {
            cardMedicationInventory.setVisibility(View.GONE);
            tvRescueSummary.setVisibility(View.GONE);
            if (medicineListListener != null) {
                medicineListListener.remove();
                medicineListListener = null;
            }
            if (rescueMedicineListener != null) {
                rescueMedicineListener.remove();
                rescueMedicineListener = null;
            }
        }

        // 2. PEF
        if (sharePEF) {
            chartPEF.setVisibility(View.VISIBLE);
            textViewTodayPEFZone.setVisibility(View.VISIBLE);
            spinnerTimeRange.setVisibility(View.VISIBLE);

            int days = (spinnerTimeRange.getSelectedItemPosition() == 0) ? 7 : 30;
            loadPEFData(childUid, days);
            fetchTodayPEFZone(childUid);
        } else {
            chartPEF.setVisibility(View.GONE);
            textViewTodayPEFZone.setVisibility(View.GONE);
            spinnerTimeRange.setVisibility(View.GONE);
            chartPEF.clear();
        }

        // 3. Symptoms
        if (shareSymptoms) {
            buttonSymptomHistory.setVisibility(View.VISIBLE);
            buttonSymptomHistory.setEnabled(true);
        } else {
            buttonSymptomHistory.setVisibility(View.GONE);
        }

    }

    private void stopPatientDataListeners() {
        if (sharingSettingsListener != null) {
            sharingSettingsListener.remove();
            sharingSettingsListener = null;
        }
        if (medicineListListener != null) {
            medicineListListener.remove();
            medicineListListener = null;
        }
        if (rescueMedicineListener != null) {
            rescueMedicineListener.remove();
            rescueMedicineListener = null;
        }
    }

    // --- Logic 2: Loading Patients ---

    private void loadLinkedPatients() {
        // Query children where 'providerAccess' array contains my providerUid
        db.collection("children")
                .whereArrayContains("providerAccess", providerUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    patientNames.clear();
                    patientIds.clear();
                    patientNames.add("Select Patient");
                    patientIds.add("NONE");

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("childName");
                        patientNames.add(name);
                        patientIds.add(doc.getId());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            patientNames
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerPatients.setAdapter(adapter);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading patients", e));
    }

    // --- Logic 3: Displaying Patient Data ---

    private void clearDashboardData() {
        medicineList.clear();
        medicineAdapter.notifyDataSetChanged();
        tvRescueSummary.setText("Rescue | Weekly uses: - | Last use: -");
        textViewTodayPEFZone.setText("Today's Zone: -");
        chartPEF.clear();
        chartPEF.invalidate();

        cardEmergencyAlerts.setVisibility(View.GONE);
        containerEmergencyAlerts.removeAllViews();

    }

    private void loadMedicines(String childUid) {
        if (medicineListListener != null) medicineListListener.remove();

        medicineListListener = db.collection("medicine")
                .whereEqualTo("childUid", childUid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) return;
                    if (cardMedicationInventory.getVisibility() != View.VISIBLE) return;
                    if (snap != null) {
                        medicineList.clear();
                        snap.getDocuments().forEach(doc -> {
                            MedicineItem item = doc.toObject(MedicineItem.class);
                            if (item != null) {
                                item.setId(doc.getId());
                                medicineList.add(item);
                            }
                        });
                        medicineAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void loadRescueSummary(String childUid) {
        if (rescueMedicineListener != null) rescueMedicineListener.remove();

        // Find the rescue medicine first
        db.collection("medicine")
                .whereEqualTo("childUid", childUid)
                .whereEqualTo("medType", "Rescue")
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        tvRescueSummary.setText("No rescue medicine configured");
                        return;
                    }
                    String medId = snap.getDocuments().get(0).getId();

                    // Listen to that specific medicine
                    rescueMedicineListener = db.collection("medicine").document(medId)
                            .addSnapshotListener((doc, e) -> {
                                if (e != null || doc == null || !doc.exists()) return;

                                Long weeklyLong = doc.getLong("weeklyRescueCount");
                                int weeklyCount = (weeklyLong != null) ? weeklyLong.intValue() : 0;

                                Timestamp ts = doc.getTimestamp("lastRescueTime");
                                String lastUseString = (ts != null) ? formatRelative(ts.toDate()) : "-";

                                String summary = "Rescue | Weekly uses: " + weeklyCount
                                        + "   |   Last use: " + lastUseString;
                                tvRescueSummary.setText(summary);
                            });
                });
    }

    // --- Charting Logic (Copied and adapted from ParentDashboard) ---

    private void loadPEFData(String childUid, int days) {
        long now = System.currentTimeMillis();
        long from = now - days * 24L * 60 * 60 * 1000;

        db.collection("pefLogs")
                .whereEqualTo("childId", childUid)
                .whereGreaterThan("timeStamp", from)
                .orderBy("timeStamp")
                .get()
                .addOnSuccessListener(query -> {
                    Map<String, Float> dailyValues = new LinkedHashMap<>();
                    SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");

                    for (QueryDocumentSnapshot log : query) {
                        long ts = log.getLong("timeStamp");
                        float value = log.getDouble("value").floatValue();
                        String day = dayFormat.format(new Date(ts));
                        dailyValues.put(day, value);
                    }

                    ArrayList<Entry> entries = new ArrayList<>();
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_YEAR, -(days - 1));

                    for (int i = 0; i < days; i++) {
                        String day = dayFormat.format(cal.getTime());
                        if (dailyValues.containsKey(day)) {
                            entries.add(new Entry(i, dailyValues.get(day)));
                        }
                        cal.add(Calendar.DAY_OF_YEAR, 1);
                    }

                    // Fetch Child Personal Best (PB) for Zone lines
                    db.collection("children").document(childUid).get()
                            .addOnSuccessListener(doc -> {
                                Double PB = doc.getDouble("childPB");
                                drawPEFChart(entries, (PB != null) ? PB : 0.0, days);
                            });
                });
    }

    private void drawPEFChart(ArrayList<Entry> entries, double PB, int days) {
        if (entries.isEmpty()) {
            chartPEF.clear();
            chartPEF.setNoDataText("No PEF data available.");
            return;
        }

        LineDataSet set = new LineDataSet(entries, "PEF");
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setColor(Color.BLUE);
        set.setCircleColor(Color.BLUE);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.LINEAR);

        LineData data = new LineData(set);
        chartPEF.setData(data);

        // Axis Settings
        YAxis left = chartPEF.getAxisLeft();
        left.removeAllLimitLines();
        left.setAxisMinimum(0f);
        left.setAxisMaximum((float) (PB > 0 ? PB * 1.1 : 600)); // Dynamic max

        // Zones
        if (PB > 0) {
            float green = (float) (PB * 0.8);
            float yellow = (float) (PB * 0.5);
            LimitLine greenLine = new LimitLine(green, "Green Zone");
            greenLine.setLineColor(Color.GREEN);
            greenLine.setLineWidth(2f);
            LimitLine yellowLine = new LimitLine(yellow, "Yellow Zone");
            yellowLine.setLineColor(Color.YELLOW);
            yellowLine.setLineWidth(2f);
            left.addLimitLine(greenLine);
            left.addLimitLine(yellowLine);
        }

        chartPEF.getAxisRight().setEnabled(false);
        XAxis xAxis = chartPEF.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        // Date formatting for X-Axis (Simple version)
        final SimpleDateFormat fmt = new SimpleDateFormat("MM/dd");
        final long now = System.currentTimeMillis();
        final long dayMillis = 24 * 60 * 60 * 1000L;
        final long startTimestamp = now - (days - 1) * dayMillis;

        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                long dateMillis = startTimestamp + (long)(value * dayMillis);
                return fmt.format(new Date(dateMillis));
            }
        });

        chartPEF.getDescription().setEnabled(false);
        chartPEF.invalidate();
    }


    private void fetchTodayPEFZone(String childUid) {

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfToday = calendar.getTimeInMillis();

        db.collection("pefLogs")
                .whereEqualTo("childId", childUid)
                .whereGreaterThanOrEqualTo("timeStamp", startOfToday)
                .orderBy("timeStamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {

                    if (!snap.isEmpty()) {

                        String zone = snap.getDocuments()
                                .get(snap.size() - 1)
                                .getString("zone");

                        updateDashboardZoneUI(zone);
                    } else {
                        updateDashboardZoneUI("No Record");
                    }
                })
                .addOnFailureListener(e -> {
                    updateDashboardZoneUI("Error");
                });
    }



    private String calculateZone(float value, Double PB) {
        if (PB == null || PB <= 0) return "Unknown";

        if (value >= PB * 0.8) return "Green";
        if (value >= PB * 0.5) return "Yellow";
        return "Red";
    }



    private void updateDashboardZoneUI(String zone) {
        int color;
        String text;

        if (zone == null) zone = "Unknown";

        switch (zone) {
            case "Green":
                text = "Today's Zone: GREEN";
                color = ContextCompat.getColor(this, android.R.color.holo_green_dark);
                break;
            case "Yellow":
                text = "Today's Zone: YELLOW";
                color = ContextCompat.getColor(this, android.R.color.holo_orange_dark);
                break;
            case "Red":
                text = "Today's Zone: RED";
                color = ContextCompat.getColor(this, android.R.color.holo_red_dark);
                break;
            default:
                text = "Today's Zone: -";
                color = Color.GRAY;
                break;
        }
        textViewTodayPEFZone.setText(text);
        textViewTodayPEFZone.setTextColor(color);
    }

    private String formatRelative(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a");
        return sdf.format(date);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (medicineListListener != null) medicineListListener.remove();
        if (rescueMedicineListener != null) rescueMedicineListener.remove();
    }

    private void loadSharedEmergencyAlertsForProvider(String parentUid) {

        db.collection("users")
                .document(parentUid)
                .collection("settings")
                .document("preferences")
                .addSnapshotListener((doc, e) -> {

                    if (e != null || doc == null || !doc.exists()) {
                        // 默认不分享，隐藏卡片
                        cardEmergencyAlerts.setVisibility(View.GONE);
                        return;
                    }


                    boolean share = Boolean.TRUE.equals(doc.getBoolean("shareEmergencyEvent"));


                    if (!share) {
                        cardEmergencyAlerts.setVisibility(View.GONE);
                        containerEmergencyAlerts.removeAllViews();

                        if (alertListener != null) {
                            alertListener.remove();
                            alertListener = null;
                        }
                        return;
                    }

                    // 监听 parentAlerts
                    loadEmergencyAlertsForProvider(parentUid);
                });
    }


    private ListenerRegistration alertListener;

    private void loadEmergencyAlertsForProvider(String parentUid) {

        if (alertListener != null) {
            alertListener.remove();
            alertListener = null;
        }

        long cutoffMillis = System.currentTimeMillis() - 24L * 60 * 60 * 1000;
        Timestamp cutoff = new Timestamp(new Date(cutoffMillis));

        alertListener = db.collection("parentAlerts")
                .whereEqualTo("parentUid", parentUid)
                .whereEqualTo("sharedToProvider", true)
                .whereGreaterThan("timestamp", cutoff)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(3)

                .addSnapshotListener((snap, e) -> {

                    if (e != null || snap == null) return;

                    containerEmergencyAlerts.removeAllViews();

                    if (snap.isEmpty()) {
                        cardEmergencyAlerts.setVisibility(View.GONE);
                        return;
                    }

                    cardEmergencyAlerts.setVisibility(View.VISIBLE);

                    // reverse manually to show oldest → newest
                    List<DocumentSnapshot> docs = snap.getDocuments();
                    Collections.reverse(docs);

                    for (DocumentSnapshot doc : docs) {
                        String message = doc.getString("message");

                        TextView tv = new TextView(this);
                        tv.setText("• " + message);
                        tv.setTextSize(14);
                        tv.setTextColor(Color.parseColor("#D32F2F"));
                        tv.setPadding(0, 6, 0, 6);

                        containerEmergencyAlerts.addView(tv);
                    }
                });
    }




}