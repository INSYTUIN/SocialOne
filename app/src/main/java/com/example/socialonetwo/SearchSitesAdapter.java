package com.example.socialonetwo;

import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter for the platform selection list in the Global Search panel.
 * Allows users to select multiple sites to perform a simultaneous search.
 * Limits the number of selectable sites to MAX_SELECTION.
 */
public class SearchSitesAdapter extends RecyclerView.Adapter<SearchSitesAdapter.ViewHolder> {

    private final List<String> siteList;
    private final List<String> filteredList = new ArrayList<>();
    private final Set<String> selectedSites = new HashSet<>();
    private static final String HOME_URL = "home://dashboard";
    private static final int MAX_SELECTION = 5;

    /**
     * Constructor for SearchSitesAdapter.
     * @param siteList The full list of sites available in the app.
     */
    public SearchSitesAdapter(List<String> siteList) {
        this.siteList = siteList;
        updateFilteredList();
    }

    /**
     * @return A set of URLs currently selected for global search.
     */
    public Set<String> getSelectedSites() {
        return selectedSites;
    }

    /**
     * Filters the site list to exclude internal dashboard pages and special search results.
     * Refreshes the UI after filtering.
     */
    public void updateFilteredList() {
        filteredList.clear();
        if (siteList == null) return;
        for (String url : siteList) {
            // Exclude internal dashboard pages and special search results
            if (!url.startsWith("home://") && !url.startsWith("Global Search:")) {
                filteredList.add(url);
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Extracts and cleans the domain name from a URL for display.
     */
    private String getDomainName(String url) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            Uri uri = Uri.parse(url);
            String domain = uri.getHost();
            if (domain == null) return url;
            return domain.startsWith("www.") ? domain.substring(4) : domain;
        } catch (Exception e) {
            return url;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_site, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String url = filteredList.get(position);
        holder.siteName.setText(getDomainName(url));

        // Apply visual selection state
        if (selectedSites.contains(url)) {
            holder.card.setStrokeWidth(4);
            holder.card.setCardBackgroundColor(Color.parseColor("#3303A9F4")); // Highlighted
        } else {
            holder.card.setStrokeWidth(1);
            holder.card.setCardBackgroundColor(Color.TRANSPARENT);
        }

        holder.itemView.setOnClickListener(v -> {
            if (selectedSites.contains(url)) {
                selectedSites.remove(url);
                notifyItemChanged(position);
            } else {
                if (selectedSites.size() < MAX_SELECTION) {
                    selectedSites.add(url);
                    notifyItemChanged(position);
                } else {
                    Toast.makeText(v.getContext(), R.string.max_sites_limit, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    /**
     * ViewHolder class for search site items.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView siteName;
        MaterialCardView card;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            siteName = itemView.findViewById(R.id.siteName);
            card = itemView.findViewById(R.id.siteCard);
        }
    }
}
