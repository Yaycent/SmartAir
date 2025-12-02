package com.example.smartair;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SymptomHistoryAdapter extends RecyclerView.Adapter<SymptomHistoryAdapter.LogViewHolder> {

    private final List<SymptomLog> logs;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());

    public SymptomHistoryAdapter(List<SymptomLog> logs) {
        this.logs = logs;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_symptom_log, parent, false);
        return new LogViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        SymptomLog log = logs.get(position);

        String dateString = dateFormat.format(new Date(log.getTimestamp()));
        holder.tvDate.setText(dateString);

        String author = log.getAuthor() != null ? log.getAuthor() : "-";
        holder.tvAuthor.setText("Author: " + author);

        StringBuilder sbSymptoms = new StringBuilder("Symptoms: ");
        boolean first = true;
        if (log.isNightWaking()) {
            sbSymptoms.append("Night waking");
            first = false;
        }
        if (log.isActivityLimit()) {
            if (!first) sbSymptoms.append(", ");
            sbSymptoms.append("Activity limit");
            first = false;
        }
        if (log.isCoughWheeze()) {
            if (!first) sbSymptoms.append(", ");
            sbSymptoms.append("Cough/wheeze");
            first = false;
        }
        if (first) {
            sbSymptoms.append("None");
        }
        holder.tvSymptoms.setText(sbSymptoms.toString());

        if (log.getTriggers() != null && !log.getTriggers().isEmpty()) {
            holder.tvTriggers.setText("Triggers: " + String.join(", ", log.getTriggers()));
        } else {
            holder.tvTriggers.setText("Triggers: None recorded");
        }
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvAuthor, tvSymptoms, tvTriggers;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvLogDate);
            tvAuthor = itemView.findViewById(R.id.tvLogAuthor);
            tvSymptoms = itemView.findViewById(R.id.tvLogSymptoms);
            tvTriggers = itemView.findViewById(R.id.tvLogTriggers);
        }
    }
}
