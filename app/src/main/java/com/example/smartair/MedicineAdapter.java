package com.example.smartair;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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

        // Name
        holder.textName.setText(item.getName());

        // Type (Rescue / Controller)
        holder.textType.setText(item.getMedType());

        // Dose text (remaining/total)
        String doseText = item.getRemainingDose() + "/" + item.getTotalDose();
        holder.textDose.setText(doseText);

        // Controller → show “dose per use”
        if ("Controller".equalsIgnoreCase(item.getMedType())) {
            holder.textDosePerUse.setVisibility(View.VISIBLE);
            holder.textDosePerUse.setText(
                    context.getString(R.string.each_use_format, item.getDosePerUse())
            );
        } else {
            holder.textDosePerUse.setVisibility(View.GONE);
        }

        // Calculate %
        double percent = item.getPercentage();
        boolean isExpiring = item.isExpiringSoon();

        // Determine Status
        if (percent <= 0) {
            holder.textStatus.setVisibility(View.VISIBLE);
            holder.textStatus.setText(R.string.status_empty);
            holder.textStatus.setBackgroundResource(R.drawable.status_tag_bg);
            holder.textStatus.setTextColor(Color.WHITE);
        } else if (percent <= 20) {
            holder.textStatus.setVisibility(View.VISIBLE);
            holder.textStatus.setText(R.string.status_low);
            holder.textStatus.setBackgroundResource(R.drawable.status_tag_bg);
            holder.textStatus.setTextColor(Color.WHITE);
        } else {
            holder.textStatus.setText(R.string.status_ok);
            setTagColor(holder.textStatus, Color.parseColor("#4CAF50")); // green
        }

        if (isExpiring) {
            holder.textStatus.setVisibility(View.VISIBLE);
            holder.textStatus.setText("Expired soon");
            holder.textStatus.setBackgroundResource(R.drawable.status_tag_yellow);
            holder.textStatus.setTextColor(Color.BLACK);
        }

        if (item.isReplacementNeeded()) {
            holder.textReplacement.setVisibility(View.VISIBLE);
            holder.textReplacement.setText("Reminder: Please buy replacement!");
        } else {
            holder.textReplacement.setVisibility(View.GONE);
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
     * Set the color of the tag.
     */
    private void setTagColor(TextView tag, int color) {
        GradientDrawable bg = (GradientDrawable) tag.getBackground();
        bg.setColor(color);
    }

    /**
     * Holds all UI references for each single medicine item row.
     */
    public static class MedicineViewHolder extends RecyclerView.ViewHolder {
        TextView textName, textType, textDose, textDosePerUse, textStatus, textReplacement;

        public MedicineViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textMedicineName);
            textType = itemView.findViewById(R.id.textMedicineType);
            textDose = itemView.findViewById(R.id.textDose);
            textDosePerUse = itemView.findViewById(R.id.textDosePerUse);
            textStatus = itemView.findViewById(R.id.textStatus);
            textReplacement = itemView.findViewById(R.id.textReplacement);
        }
    }
}
