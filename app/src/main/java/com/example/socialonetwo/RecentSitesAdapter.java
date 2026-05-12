package com.example.socialonetwo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter for the "Recently Visited" grid on the home dashboard.
 * Displays a filtered list of unique domains from the browsing history.
 * Uses Glide to load favicons for each site.
 */
public class RecentSitesAdapter extends RecyclerView.Adapter<RecentSitesAdapter.ViewHolder> {

    private final List<String> historyList;
    private final List<String> filteredRecentSites = new ArrayList<>();
    private final OnRecentClickListener listener;
    private final int maxItems;
    private boolean filterUniqueDomains = true;

    /**
     * Interface for handling clicks and long-clicks on recent site items.
     */
    public interface OnRecentClickListener {
        /**
         * Called when a recent site item is clicked.
         * @param url The URL of the clicked site.
         */
        void onRecentClick(String url);

        /**
         * Called when a recent site item is long-clicked.
         * @param url The URL of the long-clicked site.
         */
        default void onRecentLongClick(String url) {}
    }

    /**
     * Constructor for RecentSitesAdapter.
     * @param historyList The full browsing history list to filter from.
     * @param limit The maximum number of items to display (use -1 for no limit).
     * @param listener The listener for click events.
     */
    public RecentSitesAdapter(List<String> historyList, int limit, OnRecentClickListener listener) {
        this.historyList = historyList;
        this.maxItems = limit;
        this.listener = listener;
        updateFilteredList();
    }

    /**
     * Sets whether to filter unique domains or show all URLs.
     * @param filter True to filter by domain, false to show all unique URLs.
     */
    public void setFilterUniqueDomains(boolean filter) {
        this.filterUniqueDomains = filter;
        updateFilteredList();
    }

    /**
     * Filters the history list and updates the display list.
     * If filterUniqueDomains is true, it only shows the first occurrence of each domain.
     * Otherwise, it shows all unique URLs.
     */
    public void updateFilteredList() {
        filteredRecentSites.clear();
        if (historyList == null) {
            notifyDataSetChanged();
            return;
        }
        Set<String> seen = new HashSet<>();
        
        for (String entry : historyList) {
            String url = entry.contains("|") ? entry.split("\\|")[0] : entry;
            String key = filterUniqueDomains ? getDomain(url) : url;
            if (!seen.contains(key)) {
                seen.add(key);
                filteredRecentSites.add(entry);
            }
            if (maxItems > 0 && filteredRecentSites.size() >= maxItems) break;
        }
        notifyDataSetChanged();
    }

    /**
     * Extracts the domain name from a URL for filtering and display purposes.
     */
    private String getDomain(String url) {
        String cleanUrl = url.contains("|") ? url.split("\\|")[0] : url;
        String domain = cleanUrl.replace("https://", "").replace("http://", "").replace("www.", "");
        int slashIndex = domain.indexOf('/');
        if (slashIndex != -1) domain = domain.substring(0, slashIndex);
        return domain;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String entry = filteredRecentSites.get(position);
        String url = entry.contains("|") ? entry.split("\\|")[0] : entry;
        String displayName;

        if (filterUniqueDomains) {
            displayName = getDomain(url);
        } else {
            // For bookmarks, show domain + path if available to distinguish links
            String cleanUrl = url.replace("https://", "").replace("http://", "").replace("www.", "");
            if (cleanUrl.endsWith("/")) cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 1);
            displayName = cleanUrl;
        }
        
        holder.recentName.setText(displayName);

        // Load favicon using Google's favicon service
        String faviconUrl = "https://www.google.com/s2/favicons?sz=128&domain=" + url;
        Glide.with(holder.itemView.getContext())
                .load(faviconUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.recentLogo);

        holder.itemView.setOnClickListener(v -> listener.onRecentClick(url));
        
        holder.itemView.setOnLongClickListener(v -> {
            listener.onRecentLongClick(url);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return filteredRecentSites.size();
    }

    /**
     * ViewHolder class for recent site items.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView recentLogo;
        TextView recentName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            recentLogo = itemView.findViewById(R.id.recentLogo);
            recentName = itemView.findViewById(R.id.recentName);
        }
    }
}