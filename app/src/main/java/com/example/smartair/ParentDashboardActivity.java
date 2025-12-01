package com.example.smartair;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.example.smartair.Constants.*;

public class ParentDashboardActivity extends AppCompatActivity {
    private static final String TAG = "ParentDashboardActivity";

    // UI components
    private Spinner spinnerChild;
    private Spinner spinnerRange;
    private Button buttonAddChild;
    private TextView tvGoToChildDashboard;
    private TextView tvLogout;
    private Button buttonMedicineCabinet;

    private LineChart chartPEF;

    private TextView textViewTodayPEFZone;
    private TextView tvRescueSummary;
    private ListenerRegistration medicineListener;






    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Data
    private ArrayList<String> childNames = new ArrayList<>();
    private ArrayList<String> childIds = new ArrayList<>();
    private String parentUid;


    private String activeChildUid = null;
    private String activeChildName = null;

    private int savedChildIndex = 0;
    private RecyclerView recyclerView;
    private MedicineAdapter adapter;
    private ArrayList<MedicineItem> medicineList;
    private RescueUsageManager rescueManager;

    private ListenerRegistration singleAlertListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_parent_dashboard);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        parentUid = getIntent().getStringExtra(PARENT_UID);



        if (parentUid == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            parentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        if (parentUid == null) {
            Log.e(TAG, "Parent UID is NULL!");
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // All buttons
        initViews();
        loadChildrenFromFirestore();

        // Initialize rescueManager
        rescueManager = new RescueUsageManager();

        tvRescueSummary = findViewById(R.id.tvRescueSummary);





    }
    @Override
    protected void onResume() {
        super.onResume();

        if (parentUid == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            parentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        loadChildrenFromFirestore();
        listenForSingleAlert();
        if (activeChildUid != null) {
            loadMedicines(activeChildUid);
        }
    }
    private void initViews() {
        spinnerChild = findViewById(R.id.spinnerChildren);
        buttonAddChild = findViewById(R.id.buttonAddChild);
        tvGoToChildDashboard = findViewById(R.id.tvGoToChildDashboard);
        tvLogout = findViewById(R.id.tvLogout);
        recyclerView = findViewById(R.id.recyclerMedicineInventory);
        Button buttonAddMedicine = findViewById(R.id.buttonAddMedicine);
        spinnerRange = findViewById(R.id.spinnerTimeRange);
        chartPEF = findViewById(R.id.chartPEF);
        textViewTodayPEFZone = findViewById(R.id.textViewTodayPEFZone);

        // Spinner
        spinnerChild.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                savedChildIndex = position;
                if (rescueManager != null) {
                    rescueManager.stop();
                }
                if (position <= 0) {
                    activeChildUid = null;
                    medicineList.clear();
                    adapter.notifyDataSetChanged();
                    return;
                }

                activeChildUid = childIds.get(position);
                activeChildName = childNames.get(position);

                Log.d(TAG, "Selected child: " + activeChildUid);
                loadMedicines(activeChildUid);
                if (rescueManager != null) {
                    rescueManager.startListening(parentUid, activeChildUid);
                }
                findRescueMedicineAndListen(activeChildUid);


                if (position > 0) {
                    if (position < childIds.size()) {
                        String childId = childIds.get(position);
                        int days = (spinnerRange.getSelectedItemPosition() == 0) ? 7 : 30;

                        fetchTodayPEFZone(childId);

                        loadPEFData(childId, days);
                    }
                } else {
                    chartPEF.clear();
                    chartPEF.setNoDataText("Please select a child to view PEF data.");
                    chartPEF.invalidate();

                    if (textViewTodayPEFZone != null) {
                        textViewTodayPEFZone.setText("Select a Child");
                        textViewTodayPEFZone.setTextColor(Color.BLACK);
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (rescueManager != null) {
                    rescueManager.stop();
                }
            }
        });
        // Logout
        tvLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(ParentDashboardActivity.this,
                    RoleSelectionActivity.class);
            // Clear the task stack to prevent returning by pressing the back button
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        //Add Child
        buttonAddChild.setOnClickListener(v -> {
            Intent intent = new Intent(ParentDashboardActivity.this, AddChildActivity.class);
            intent.putExtra(PARENT_UID, parentUid);
            startActivity(intent);
        });

        // Go to Child Dashboard
        tvGoToChildDashboard.setOnClickListener(v -> {
            int index = spinnerChild.getSelectedItemPosition();

            // Exclude the first item “Select Child” (Index 0)
            if (index <= 0) {
                Toast.makeText(this, "Please select a child first.", Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedChildName = childNames.get(index);
            String selectedChildUid = childIds.get(index);

            Intent intent = new Intent(ParentDashboardActivity.this, ChildDashboardActivity.class);
            intent.putExtra(CHILD_UID, selectedChildUid);
            intent.putExtra(CHILD_NAME, selectedChildName);
            intent.putExtra(PARENT_UID, parentUid);
            startActivity(intent);
        });

        // Medicine Inventory
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        medicineList = new ArrayList<>();
        adapter = new MedicineAdapter(this, medicineList);
        recyclerView.setAdapter(adapter);

        // "Add medicine" button
        buttonAddMedicine.setOnClickListener(v -> {
            if (activeChildUid == null) {
                Toast.makeText(this, "Select a child first", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent addMedIntent =
                    new Intent(ParentDashboardActivity.this, AddMedicineActivity.class);
            if (activeChildName == null) {
                Toast.makeText(this, "Child name still loading... try again", Toast.LENGTH_SHORT).show();
                return;
            }
            addMedIntent.putExtra(PARENT_UID, parentUid);
            addMedIntent.putExtra(CHILD_UID, activeChildUid);
            addMedIntent.putExtra(CHILD_NAME, activeChildName);
            startActivity(addMedIntent);
        });

        ArrayAdapter<String> rangeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"7 Days", "30 Days"});
        rangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRange.setAdapter(rangeAdapter);

        spinnerRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int days = (position == 0) ? 7 : 30;

                int childIndex = spinnerChild.getSelectedItemPosition();
                if (childIndex > 0) {
                    loadPEFData(childIds.get(childIndex), days);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

    }

    private void loadChildrenFromFirestore() {

        db.collection("children")
                .whereEqualTo("parentId", parentUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    childNames.clear();
                    childIds.clear();

                    // Add default option
                    childNames.add("Select Child");
                    childIds.add("NONE");

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("childName");
                        String uid = doc.getId();

                        if (name != null) {
                            childNames.add(name);
                            childIds.add(uid);
                        }
                    }

                    // Set adapter only AFTER all items are added
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            ParentDashboardActivity.this,
                            android.R.layout.simple_spinner_item,
                            childNames
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerChild.setAdapter(adapter);

                    // Restore saved selection (must be AFTER adapter)
                    if (savedChildIndex < childNames.size()) {
                        spinnerChild.setSelection(savedChildIndex);
                    }

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading children", e);
                    Toast.makeText(this, "Failed to load children.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Loads the list of medicines in real-time from Firebase.
     */
    private void loadMedicines(String childUid) {
        if (parentUid == null) {
            Log.e(TAG, "Cannot load medicine: parentUid STILL NULL.");
            return;
        }

        db.collection("medicine")
                .whereEqualTo("parentUid", parentUid)
                .whereEqualTo("childUid", childUid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen failed: ", e);
                        return;
                    }
                    if (snap == null) return;

                    medicineList.clear();
                    snap.getDocuments().forEach(doc -> {
                        MedicineItem item = doc.toObject(MedicineItem.class);
                        if (item != null) {
                            item.setId(doc.getId());
                            medicineList.add(item);
                        }
                    });
                    // NEW
                    checkMedicineAlerts();
                    adapter.notifyDataSetChanged();
                });
    }
//ADD NEW
    private void checkMedicineAlerts() {
        if (medicineList == null || medicineList.isEmpty()) return;
        if (parentUid == null) return;

        for (MedicineItem item : medicineList) {
            if (item == null) continue;

            boolean low = item.getPercentage() <= 20;
            boolean expiringSoon = item.isExpiringSoon();

            if (!low && !expiringSoon) continue;

            // 如果你的 medicine 有 childUid，可以用这个；否则可以传 null 或默认孩子名
            String childUidForMed = item.getChildUid();
            String childNameForMed = item.getChildName();   // 如果你在 medicine 里有 childName 字段，也可以加 getter

            ParentAlertHelper.alertMedicineLowOrExpired(
                    parentUid,
                    childUidForMed,
                    childNameForMed,
                    item.getName(),
                    low,
                    expiringSoon
            );
        }
    }


    private void loadPEFData(String childId, int days) {

        long now = System.currentTimeMillis();
        long from = now - days * 24L * 60 * 60 * 1000;

        db.collection("pefLogs")
                .whereEqualTo("childId", childId)
                .whereGreaterThan("timeStamp", from)
                .orderBy("timeStamp")
                .get()
                .addOnSuccessListener(query -> {

                    Map<String, Float> dailyValues = new LinkedHashMap<>();
                    Map<String, String> dailyTags = new LinkedHashMap<>();

                    SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");

                    for (QueryDocumentSnapshot log : query) {

                        long ts = log.getLong("timeStamp");
                        float value = log.getDouble("value").floatValue();
                        String tag = log.getString("tag");
                        if (tag == null) tag = "";

                        String day = dayFormat.format(new Date(ts));


                        dailyValues.put(day, value);
                        dailyTags.put(day, tag);
                    }

                    ArrayList<Entry> finalEntries = new ArrayList<>();
                    ArrayList<String> finalTags = new ArrayList<>();

                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_YEAR, -(days - 1));

                    for (int i = 0; i < days; i++) {
                        String day = dayFormat.format(cal.getTime());
                        float x = i;

                        if (dailyValues.containsKey(day)) {
                            finalEntries.add(new Entry(x, dailyValues.get(day)));
                            finalTags.add(dailyTags.get(day));
                        } else {
                            finalEntries.add(new Entry(x, Float.NaN));
                            finalTags.add("");
                        }

                        cal.add(Calendar.DAY_OF_YEAR, 1);
                    }


                    db.collection("children").document(childId)
                            .get()
                            .addOnSuccessListener(doc -> {
                                Double PB = doc.getDouble("childPB");
                                if (PB == null) PB = 0.0;

                                drawPEFChart(finalEntries, finalTags, PB);
                            });
                });
    }



    private void drawPEFChart(ArrayList<Entry> entries, ArrayList<String> tags, double PB) {

        if (entries.isEmpty()) {
            chartPEF.clear();
            chartPEF.setNoDataText("No PEF data yet");
            return;
        }

        LineDataSet set = new LineDataSet(entries, "PEF");

        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setColor(Color.BLUE);
        set.setCircleColor(Color.BLUE);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.LINEAR);
        set.setDrawCircles(true);


        set.setDrawHighlightIndicators(false);
        set.setDrawFilled(false);
        set.setFormLineWidth(1f);

        LineData data = new LineData(set);
        chartPEF.setData(data);

        // Y Axis
        YAxis left = chartPEF.getAxisLeft();
        left.removeAllLimitLines();

        left.setAxisMinimum(0f);
        left.setAxisMaximum((float) PB);

        float green = (float) (PB * 0.8);
        float yellow = (float) (PB * 0.5);

        LimitLine greenZone = new LimitLine(green, "");
        greenZone.setLineWidth(0f);

        LimitLine yellowZone = new LimitLine(yellow, "");
        yellowZone.setLineWidth(0f);

        left.addLimitLine(greenZone);
        left.addLimitLine(yellowZone);

        chartPEF.getAxisRight().setEnabled(false);

        XAxis xAxis = chartPEF.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);

        xAxis.setAvoidFirstLastClipping(false);
        xAxis.setCenterAxisLabels(false);

        chartPEF.setVisibleXRangeMinimum(entries.size());
        chartPEF.setVisibleXRangeMaximum(entries.size());

        ArrayList<String> xLabels = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -(entries.size() - 1));

        SimpleDateFormat fmt = new SimpleDateFormat("MM/dd");

        for (int i = 0; i < entries.size(); i++) {
            xLabels.add(fmt.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }


        xAxis.setLabelCount(xLabels.size(), true);


        boolean isThirtyDays = (entries.size() > 10);
        SimpleDateFormat fmtFull = new SimpleDateFormat("MM/dd");

        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {

                int index = (int) value;
                if (index < 0 || index >= xLabels.size()) return "";

                String date = xLabels.get(index);
                String dayStr = date.substring(3, 5);
                int day = Integer.parseInt(dayStr);

                // 7 Days: Show all
                if (!isThirtyDays) {
                    return date;

                }

                if (isThirtyDays) {
                    xAxis.setLabelRotationAngle(0f);
                    xAxis.setAvoidFirstLastClipping(false);
                    xAxis.setGranularity(1f);
                    xAxis.setGranularityEnabled(true);


                    xAxis.setSpaceMin(0f);
                    xAxis.setSpaceMax(0f);
                    xAxis.setLabelCount(xLabels.size());
                    xAxis.setDrawLabels(true);
                }


                //30 Days: show 5/10/15/20/25
                if (day == 5 || day == 10 || day == 15 || day == 20 || day == 25)
                    return date;

                // Month end: auto detect
                Calendar c = Calendar.getInstance();
                try {
                    c.setTime(fmtFull.parse(date));
                } catch (Exception ignored) {}

                int maxDay = c.getActualMaximum(Calendar.DAY_OF_MONTH);
                if (day == maxDay)
                    return date;

                return "";
            }
        });

        chartPEF.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int index = entries.indexOf(e);
                String tag = tags.get(index);
                Toast.makeText(ParentDashboardActivity.this, "Tag: " + tag, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onNothingSelected() {}
        });


        chartPEF.setDragEnabled(false);
        chartPEF.setScaleEnabled(false);
        chartPEF.setPinchZoom(false);
        chartPEF.setDoubleTapToZoomEnabled(false);
        chartPEF.setHighlightPerDragEnabled(false);
        chartPEF.getViewPortHandler().setMaximumScaleX(1f);
        chartPEF.getViewPortHandler().setMaximumScaleY(1f);

        chartPEF.invalidate();
    }


    private void fetchTodayPEFZone(String childId) {

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfToday = calendar.getTimeInMillis();

        Log.d(TAG, "Start of Today = " + startOfToday);

        db.collection("pefLogs")
                .whereEqualTo("childId", childId)
                .whereGreaterThanOrEqualTo("timeStamp", startOfToday)
                .orderBy("timeStamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    Log.d(TAG, "Found " + snap.size() + " logs today");

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
                    Log.e(TAG, "Failed to fetch today PEF", e);
                    updateDashboardZoneUI("Error");
                });
    }



    private void updateDashboardZoneUI(String zone) {
        if (textViewTodayPEFZone == null) return;

        int textColor;
        String zoneText;


        switch (zone) {
            case "Green":
                zoneText = "Today's Zone: GREEN";
                textColor = android.R.color.holo_green_light;
                break;
            case "Yellow":
                zoneText = "Today's Zone: YELLOW";
                textColor = android.R.color.holo_orange_light;
                break;
            case "Red":
                zoneText = "Today's Zone: RED";
                textColor = android.R.color.holo_red_light;
                break;
            case "No Record":
                zoneText = "No PEF Record Today";
                textColor = android.R.color.darker_gray;
                break;
            case "Unknown":
            case "Error":
            default:
                zoneText = "Zone: Unknown/Error";
                textColor = android.R.color.black;
                break;
        }

        textViewTodayPEFZone.setText(zoneText);
        textViewTodayPEFZone.setTextColor(ContextCompat.getColor(this, textColor));
    }

    private void listenToMedicineUpdates(String medicineId) {

        medicineListener = db.collection("medicine")
                .document(medicineId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;

                    // Weekly count
                    Long weeklyLong = doc.getLong("weeklyRescueCount");
                    int weeklyCount = (weeklyLong != null) ? weeklyLong.intValue() : 0;

                    // Last time
                    Timestamp ts = doc.getTimestamp("lastRescueTime");
                    String lastUseString;

                    if (ts != null) {
                        lastUseString = formatRelative(ts.toDate());
                    } else {
                        lastUseString = "-";
                    }

                    String summary = "Rescue | Weekly uses: " + weeklyCount
                            + "   |   Last use: " + lastUseString;

                    tvRescueSummary.setText(summary);
                });
    }

    private String formatRelative(Date date) {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTime(date);

        SimpleDateFormat timeFmt = new SimpleDateFormat("h:mm a");
        SimpleDateFormat fullFmt = new SimpleDateFormat("MMM d, h:mm a");

        // Today check
        if (now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
            return "Today " + timeFmt.format(date);
        }

        // Yesterday check
        now.add(Calendar.DAY_OF_YEAR, -1);
        if (now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
            return "Yesterday " + timeFmt.format(date);
        }

        // Other dates
        return fullFmt.format(date);
    }


    private void findRescueMedicineAndListen(String childUid) {

        if (medicineListener != null) {
            medicineListener.remove();
            medicineListener = null;
        }

        db.collection("medicine")
                .whereEqualTo("childUid", childUid)
                .whereEqualTo("parentUid", parentUid)
                .whereEqualTo("medType", "Rescue")
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        tvRescueSummary.setText("No rescue medicine found");
                        return;
                    }

                    String medId = snap.getDocuments().get(0).getId();
                    Log.d(TAG, "Found rescue medicine: " + medId);

                    listenToMedicineUpdates(medId);
                });
    }


    /**
     * Listens for the most recent unprocessed alert for this parent.
     * This listener always retrieves only ONE document:
     * - the latest alert (timestamp DESC)
     * - where processed = false
     * When a new alert arrives, a popup dialog is shown to the parent.
     */

    private void listenForSingleAlert() {

        if (singleAlertListener != null) {
            singleAlertListener.remove();
        }

        singleAlertListener = db.collection("parentAlerts")
                .whereEqualTo("parentUid", parentUid)
                .whereEqualTo("processed", false)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snap, e) -> {

                    if (e != null || snap == null) return;
                    if (snap.isEmpty()) return;

                    // Only one unprocessed alert
                    var doc = snap.getDocuments().get(0);

                    String alertId = doc.getId();
                    String childName = doc.getString("childName");
                    String message = doc.getString("message");

                    if (childName == null) childName = "Your child";

                    // Show popup
                    showAlertPopup(alertId, childName, message);
                });
    }

    /**
     * Displays a blocking popup dialog for a single alert.
     * After the parent taps "OK", the alert will be marked as processed
     * so it will not appear again.
     *
     * @param alertId   Firestore document ID of the alert
     * @param childName Name of the child associated with the alert
     * @param message   Alert message shown to the parent
     */
    private void showAlertPopup(String alertId, String childName, String message) {

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Alert for " + childName)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    // Mark this alert as processed
                    db.collection("parentAlerts")
                            .document(alertId)
                            .update("processed", true);

                    dialog.dismiss();
                })
                .show();
    }


}
