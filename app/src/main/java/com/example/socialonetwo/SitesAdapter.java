package com.example.socialonetwo;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter for the horizontal RecyclerView that displays saved and active social media sites.
 * Manages item selection, visual feedback for the active tab, and site locking functionality.
 */
public class SitesAdapter extends RecyclerView.Adapter<SitesAdapter.SiteViewHolder> {

    private final List<String> siteList;
    private final OnSiteClickListener listener;
    private int selectedPosition = 0;
    private final Set<String> lockedSites = new HashSet<>();
    private Set<String> incognitoTabs = new HashSet<>();
    private static final int MAX_LOCKED = 3;

    /**
     * Interface for handling click and long-click events on site items.
     */
    public interface OnSiteClickListener {
        /**
         * Called when a site item is clicked.
         * @param position The position of the clicked item.
         */
        void onSiteClick(int position);

        /**
         * Called when a site item is long-clicked.
         * @param position The position of the long-clicked item.
         */
        void onSiteLongClick(int position);
    }

    /**
     * Constructor for SitesAdapter.
     * @param siteList The list of site URLs to display.
     * @param listener The listener for click events.
     */
    public SitesAdapter(List<String> siteList, OnSiteClickListener listener) {
        this.siteList = siteList;
        this.listener = listener;
    }

    /**
     * Updates the currently selected position and refreshes the affected items.
     * @param position The new selected position.
     */
    public void setSelectedPosition(int position) {
        int previousPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(previousPosition);
        notifyItemChanged(selectedPosition);
    }

    /**
     * Sets the set of locked sites and refreshes the UI.
     * @param locked A set of URLs that should be marked as locked.
     */
    public void setLockedSites(Set<String> locked) {
        lockedSites.clear();
        lockedSites.addAll(locked);
        notifyDataSetChanged();
    }

    public void setIncognitoTabs(Set<String> incognitoTabs) {
        this.incognitoTabs = incognitoTabs;
        // Do not call notifyDataSetChanged here to allow item animations
    }

    /**
     * Toggles the lock status of a specific site.
     * @param url The URL of the site to toggle.
     * @param anchorView A view used as an anchor for displaying error messages (Toasts).
     */
    public void toggleLock(String url, View anchorView) {
        if (lockedSites.contains(url)) {
            lockedSites.remove(url);
        } else {
            if (lockedSites.size() < MAX_LOCKED) {
                lockedSites.add(url);
            } else {
                Toast.makeText(anchorView.getContext(), R.string.max_locked_limit, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        int index = siteList.indexOf(url);
        if (index != -1) {
            notifyItemChanged(index);
        }
    }

    /**
     * Checks if a site is currently locked.
     * @param url The URL to check.
     * @return True if the site is locked, false otherwise.
     */
    public boolean isLocked(String url) {
        return lockedSites.contains(url);
    }

    @NonNull
    @Override
    public SiteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_site, parent, false);
        return new SiteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SiteViewHolder holder, int position) {
        String url = siteList.get(position);
        
        // Format the URL for display (remove protocol and www)
        String displayName;
        if (url.equals("home://dashboard")) {
            displayName = "Home";
        } else if (url.equals("home://business")) {
            displayName = "Business";
        } else if (url.startsWith("Global Search: ")) {
            displayName = url; 
        } else {
            displayName = url.replace("https://", "").replace("http://", "").replace("www.", "");
            int slashIndex = displayName.indexOf('/');
            if (slashIndex != -1) {
                displayName = displayName.substring(0, slashIndex);
            }
        }

        holder.siteName.setText(displayName);

        // Apply visual feedback for selected item
        if (position == selectedPosition) {
            holder.card.setStrokeWidth(4);
            holder.card.setCardBackgroundColor(Color.parseColor("#1A000000"));
        } else {
            holder.card.setStrokeWidth(1);
            holder.card.setCardBackgroundColor(Color.TRANSPARENT);
        }

        // Show lock icon if site is locked
        holder.lockIndicator.setVisibility(lockedSites.contains(url) ? View.VISIBLE : View.GONE);
        
        // Show incognito icon if site is incognito
        holder.incognitoIndicator.setVisibility(incognitoTabs.contains(url) ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                listener.onSiteClick(currentPos);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                listener.onSiteLongClick(currentPos);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return siteList.size();
    }

    /**
     * ViewHolder class for site items.
     */
    public static class SiteViewHolder extends RecyclerView.ViewHolder {
        TextView siteName;
        MaterialCardView card;
        ImageView lockIndicator;
        ImageView incognitoIndicator;

        public SiteViewHolder(@NonNull View itemView) {
            super(itemView);
            siteName = itemView.findViewById(R.id.siteName);
            card = itemView.findViewById(R.id.siteCard);
            lockIndicator = itemView.findViewById(R.id.lockIndicator);
            incognitoIndicator = itemView.findViewById(R.id.incognitoIndicator);
        }
    }
}
