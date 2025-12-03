package com.example.smartair.ui.activities;

import android.app.DatePickerDialog;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartair.R;
import com.example.smartair.ui.adapters.SymptomHistoryAdapter;
import com.example.smartair.models.SymptomLog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.example.smartair.utils.Constants.CHILD_NAME;
import static com.example.smartair.utils.Constants.CHILD_UID;
import static com.example.smartair.utils.Constants.PARENT_UID;
/**
 * SymptomHistoryActivity.java
 * <p>
 * Displays a historical log of the child's symptoms and provides filtering and export capabilities (Requirement R5 & R6).
 * </p>
 * <b>Key Functionalities:</b>
 * <ul>
 * <li><b>History Browser:</b> Loads symptom logs from Firestore and displays them in a chronological list.</li>
 * <li><b>Advanced Filtering:</b> Allows users to filter logs by date range, specific symptoms (e.g., Night Waking), or triggers.</li>
 * <li><b>Dynamic Filters:</b> Trigger filter options are dynamically populated based on recorded data.</li>
 * <li><b>Data Export (R6):</b> Generates downloadable CSV and PDF reports for sharing with healthcare providers.</li>
 * </ul>
 *
 * @author Rphael Wang
 * @version 1.0
 */
public class SymptomHistoryActivity extends AppCompatActivity {

    private static final String TAG = "SymptomHistoryActivity";

    private String parentUid;
    private String childUid;
    private String childName;

    // UI
    private TextView tvTitle;
    private TextView tvPlaceholder;
    private Spinner spinnerSymptom;
    private Spinner spinnerTrigger;
    private Button buttonStartDate;
    private Button buttonEndDate;
    private Button buttonApplyFilter;
    private Button buttonExportCsv;
    private Button buttonExportPdf;
    private RecyclerView recyclerView;

    // Data
    private FirebaseFirestore db;
    private final List<SymptomLog> allLogs = new ArrayList<>();
    private final List<SymptomLog> filteredLogs = new ArrayList<>();
    private SymptomHistoryAdapter adapter;

    private long startMillis;
    private long endMillis;
    private final SimpleDateFormat dateFmt =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom_history);

        db = FirebaseFirestore.getInstance();

        parentUid = getIntent().getStringExtra(PARENT_UID);
        childUid = getIntent().getStringExtra(CHILD_UID);
        childName = getIntent().getStringExtra(CHILD_NAME);

        if (childName == null) childName = "Child";

        initViews();
        initDefaultDateRange();
        setupSpinners();
        setupRecycler();

        loadLogsFromFirestore();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvSymptomHistoryTitle);
        tvPlaceholder = findViewById(R.id.tvSymptomHistoryPlaceholder);
        spinnerSymptom = findViewById(R.id.spinnerSymptom);
        spinnerTrigger = findViewById(R.id.spinnerTrigger);
        buttonStartDate = findViewById(R.id.buttonStartDate);
        buttonEndDate = findViewById(R.id.buttonEndDate);
        buttonApplyFilter = findViewById(R.id.buttonApplyFilter);
        buttonExportCsv = findViewById(R.id.buttonExportCsv);
        buttonExportPdf = findViewById(R.id.buttonExportPdf);
        recyclerView = findViewById(R.id.recyclerSymptomHistory);

        tvTitle.setText("Symptom History – " + childName);

        buttonStartDate.setOnClickListener(v -> showDatePicker(true));
        buttonEndDate.setOnClickListener(v -> showDatePicker(false));

        buttonApplyFilter.setOnClickListener(v -> loadLogsFromFirestore());
        buttonExportCsv.setOnClickListener(v -> exportCsv());
        buttonExportPdf.setOnClickListener(v -> exportPdf());
    }

    private void initDefaultDateRange() {
        Calendar cal = Calendar.getInstance();
        endMillis = cal.getTimeInMillis();

        cal.add(Calendar.MONTH, -3);
        startMillis = cal.getTimeInMillis();

        buttonStartDate.setText(dateFmt.format(cal.getTime()));
        Calendar endCal = Calendar.getInstance();
        buttonEndDate.setText(dateFmt.format(endCal.getTime()));
    }

    private void setupSpinners() {
        // Symptom
        String[] symptomOptions = new String[]{
                "All symptoms",
                "Night waking",
                "Activity limit",
                "Cough/wheeze"
        };
        ArrayAdapter<String> symptomAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                symptomOptions
        );
        symptomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSymptom.setAdapter(symptomAdapter);


        String[] triggerOptions = new String[]{"All triggers"};
        ArrayAdapter<String> triggerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                triggerOptions
        );
        triggerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTrigger.setAdapter(triggerAdapter);
    }

    private void setupRecycler() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SymptomHistoryAdapter(filteredLogs);
        recyclerView.setAdapter(adapter);
    }

    private void showDatePicker(boolean isStart) {
        final Calendar cal = Calendar.getInstance();
        long current = isStart ? startMillis : endMillis;
        cal.setTimeInMillis(current);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    // normalize: start to 00:00:00, end to 23:59:59
                    if (isStart) {
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        startMillis = cal.getTimeInMillis();
                        buttonStartDate.setText(dateFmt.format(cal.getTime()));
                    } else {
                        cal.set(Calendar.HOUR_OF_DAY, 23);
                        cal.set(Calendar.MINUTE, 59);
                        cal.set(Calendar.SECOND, 59);
                        endMillis = cal.getTimeInMillis();
                        buttonEndDate.setText(dateFmt.format(cal.getTime()));
                    }
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    /**
     * fetch records from Firestore for specified child + date range
     */
    private void loadLogsFromFirestore() {
        if (childUid == null) {
            Toast.makeText(this, "Child not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("symptomLogs")
                .whereEqualTo("childUid", childUid)
                .whereGreaterThanOrEqualTo("timestamp", startMillis)
                .whereLessThanOrEqualTo("timestamp", endMillis)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    allLogs.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        SymptomLog log = doc.toObject(SymptomLog.class);
                        if (log != null) {
                            allLogs.add(log);
                        }
                    }
                    updateTriggerSpinnerFromData();
                    applyFiltersAndUpdateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load symptom logs", e);
                    Toast.makeText(this, "Failed to load history.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * dynamically add Trigger options based on existing data
     */
    private void updateTriggerSpinnerFromData() {
        Set<String> triggerSet = new HashSet<>();
        for (SymptomLog log : allLogs) {
            if (log.getTriggers() != null) {
                triggerSet.addAll(log.getTriggers());
            }
        }
        List<String> options = new ArrayList<>();
        options.add("All triggers");
        options.addAll(triggerSet);

        ArrayAdapter<String> adapterTriggers = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                options
        );
        adapterTriggers.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTrigger.setAdapter(adapterTriggers);
    }

    /**
     * filter local data based on Symptom + Trigger and update RecyclerView
     */
    private void applyFiltersAndUpdateUI() {
        filteredLogs.clear();

        String selectedSymptom = (String) spinnerSymptom.getSelectedItem();
        String selectedTrigger = (String) spinnerTrigger.getSelectedItem();

        for (SymptomLog log : allLogs) {

            // Symptom filter
            if (!"All symptoms".equals(selectedSymptom)) {
                boolean match = false;
                switch (selectedSymptom) {
                    case "Night waking":
                        match = log.isNightWaking();
                        break;
                    case "Activity limit":
                        match = log.isActivityLimit();
                        break;
                    case "Cough/wheeze":
                        match = log.isCoughWheeze();
                        break;
                }
                if (!match) continue;
            }

            // Trigger filter
            if (!"All triggers".equals(selectedTrigger)) {
                if (log.getTriggers() == null || !log.getTriggers().contains(selectedTrigger)) {
                    continue;
                }
            }

            filteredLogs.add(log);
        }

        adapter.notifyDataSetChanged();

        if (filteredLogs.isEmpty()) {
            tvPlaceholder.setText("No symptom history in this range\n(or filters too strict).");
            tvPlaceholder.setAlpha(1f);
        } else {
            tvPlaceholder.setAlpha(0f);
        }
    }

    /**
     * export CSV to app's external file directory
     */
    private void exportCsv() {
        if (filteredLogs.isEmpty()) {
            Toast.makeText(this, "No data to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir == null) dir = getExternalFilesDir(null);

            if (dir == null) {
                Toast.makeText(this, "Storage not available.", Toast.LENGTH_SHORT).show();
                return;
            }

            String fileName = "symptom_history_" + childName.replace(" ", "_") + ".csv";
            File file = new File(dir, fileName);

            FileOutputStream fos = new FileOutputStream(file);
            String header = "Date,Author,NightWaking,ActivityLimit,CoughWheeze,Triggers\n";
            fos.write(header.getBytes());

            for (SymptomLog log : filteredLogs) {
                String date = dateFmt.format(new Date(log.getTimestamp()));
                String author = log.getAuthor() != null ? log.getAuthor() : "";
                String triggers = "";
                if (log.getTriggers() != null && !log.getTriggers().isEmpty()) {
                    triggers = String.join(";", log.getTriggers());
                }

                String line = String.format(Locale.getDefault(),
                        "%s,%s,%b,%b,%b,%s\n",
                        date,
                        author.replace(",", " "),
                        log.isNightWaking(),
                        log.isActivityLimit(),
                        log.isCoughWheeze(),
                        triggers.replace(",", " ")
                );
                fos.write(line.getBytes());
            }

            fos.flush();
            fos.close();

            Toast.makeText(this,
                    "CSV exported:\n" + file.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e(TAG, "Error exporting CSV", e);
            Toast.makeText(this, "Failed to export CSV.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * export simple PDF, summarize current filtered records
     * exists in app's external file directory, convenient for sharing with Provider
    * 
     * @return
     */
    private void exportPdf() {
        if (filteredLogs.isEmpty()) {
            Toast.makeText(this, "No data to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument pdf = new PdfDocument();
        Paint paint = new Paint();
        paint.setTextSize(12f);

        int pageWidth = 595;   // A4 width (72 dpi)
        int pageHeight = 842;

        int x = 40;
        int y = 50;
        int lineHeight = 18;

        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = pdf.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // title
        paint.setTextSize(16f);
        canvas.drawText("Symptom History – " + childName, x, y, paint);
        y += lineHeight + 10;
        paint.setTextSize(12f);

        for (int i = 0; i < filteredLogs.size(); i++) {
            SymptomLog log = filteredLogs.get(i);

            if (y > pageHeight - 60) {
                pdf.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight,
                        pdf.getPages().size() + 1).create();
                page = pdf.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }

            String dateStr = dateFmt.format(new Date(log.getTimestamp()));
            StringBuilder symptomSb = new StringBuilder();
            if (log.isNightWaking()) symptomSb.append("Night waking ");
            if (log.isActivityLimit()) symptomSb.append("Activity limit ");
            if (log.isCoughWheeze()) symptomSb.append("Cough/wheeze ");

            String triggerStr = "";
            if (log.getTriggers() != null && !log.getTriggers().isEmpty()) {
                triggerStr = String.join(", ", log.getTriggers());
            }

            canvas.drawText((i + 1) + ". " + dateStr, x, y, paint);
            y += lineHeight;
            canvas.drawText("   Symptoms: " +
                    (symptomSb.length() == 0 ? "None" : symptomSb.toString()), x, y, paint);
            y += lineHeight;
            canvas.drawText("   Triggers: " +
                    (triggerStr.isEmpty() ? "None" : triggerStr), x, y, paint);
            y += lineHeight + 4;
        }

        pdf.finishPage(page);

        try {
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir == null) dir = getExternalFilesDir(null);
            if (dir == null) {
                Toast.makeText(this, "Storage not available.", Toast.LENGTH_SHORT).show();
                pdf.close();
                return;
            }

            String fileName = "symptom_history_" + childName.replace(" ", "_") + ".pdf";
            File file = new File(dir, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            pdf.writeTo(fos);
            pdf.close();
            fos.close();

            Toast.makeText(this,
                    "PDF exported:\n" + file.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e(TAG, "Error exporting PDF", e);
            Toast.makeText(this, "Failed to export PDF.", Toast.LENGTH_SHORT).show();
        }
    }
}
