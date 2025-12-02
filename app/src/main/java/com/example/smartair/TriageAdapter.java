package com.example.smartair;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TriageAdapter extends RecyclerView.Adapter<TriageAdapter.ViewHolder> {

    // 引用我们在 Activity 里定义的静态内部类 TriageItem
    private List<ProviderDashboardActivity.TriageItem> list;

    public TriageAdapter(List<ProviderDashboardActivity.TriageItem> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_triage_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProviderDashboardActivity.TriageItem item = list.get(position);

        holder.tvDate.setText(item.date);
        holder.tvTrigger.setText(item.trigger);
        holder.tvStatus.setText(item.status);

        // 简单的样式逻辑：根据状态变色
        if ("New".equalsIgnoreCase(item.status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#D32F2F")); // 红色
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // 绿色
        }
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTrigger, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTrigger = itemView.findViewById(R.id.tvTrigger);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}