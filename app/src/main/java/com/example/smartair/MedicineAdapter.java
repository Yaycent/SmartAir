package com.example.smartair;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder>{
    private Context context;
    private List<MedicineItem> medicineList;

    public MedicineAdapter(Context context, List<MedicineItem> medicineList) {
        this.context = context;
        this.medicineList = medicineList;
    }

    /**
     * Called when RecyclerView needs a new ViewHolder.
     * Inflates the item_medicine.xml layout and returns its ViewHolder.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new MedicineViewHolder instance.
     */
    @NonNull
    @Override
    public MedicineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_medicine, parent, false);
        return new MedicineViewHolder(view);
    }

    /**
     * Binds a single item in the RecyclerView to its UI elements.
     *
     * @param holder The ViewHolder that holds references to the item's views.
     * @param position The position of this item in the list.
     */
    @Override
    public void onBindViewHolder(@NonNull MedicineViewHolder holder, int position) {
        MedicineItem item = medicineList.get(position);

        holder.textName.setText(item.getName());

        double percent = item.getPercentage();
        String percentText = String.format("%.0f%%", percent);
        holder.textPercentage.setText(percentText);

        // green (>20%), red (≤20%)
        if (percent <= 20) {
            holder.textPercentage.setTextColor(Color.RED);
        } else {
            holder.textPercentage.setTextColor(Color.parseColor("#33691E")); // green
        }
    }

    /**
     * @return how many items RecyclerView should display
     */
    @Override
    public int getItemCount() {
        return medicineList.size();
    }

    /**
     * Holds all UI references for each single medicine item row.
     */
    public static class MedicineViewHolder extends RecyclerView.ViewHolder {
        TextView textName, textPercentage;

        public MedicineViewHolder(@NonNull View itemView) {
            super(itemView);

            textName = itemView.findViewById(R.id.textMedicineName);
            textPercentage = itemView.findViewById(R.id.textPercentage);
        }
    }
}
