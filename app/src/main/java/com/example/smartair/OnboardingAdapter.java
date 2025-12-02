package com.example.smartair;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for onboarding ViewPager pages.
 */
public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {
    private final List<OnboardingPage> pages;

    public OnboardingAdapter(List<OnboardingPage> pages) {
        this.pages = pages;
    }

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding_page, parent, false);
        return new OnboardingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        OnboardingPage page = pages.get(position);
        holder.title.setText(page.getPageTitle());
        holder.description.setText(page.getPageDescription());
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    /**
     * ViewHolder representing a single onboarding page.
     */
    public static class OnboardingViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        TextView description;

        public OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);

            // Updated IDs
            title = itemView.findViewById(R.id.onbPageTitle);
            description = itemView.findViewById(R.id.onbPageDescription);
        }
    }

}



