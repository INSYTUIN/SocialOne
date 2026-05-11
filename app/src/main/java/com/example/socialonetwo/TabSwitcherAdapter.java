package com.example.socialonetwo;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapter for the Tab Switcher grid.
 * Displays a list of open tabs with their titles and visual previews (screenshots).
 * Allows users to switch between tabs or close them.
 */
public class TabSwitcherAdapter extends RecyclerView.Adapter<TabSwitcherAdapter.ViewHolder> {

    private List<String> filteredList;
    private final Map<String, Bitmap> tabPreviews;
    private final OnTabClickListener listener;
    private Set<String> incognitoTabs = new HashSet<>();
    private Set<String> workspaceTabs = new HashSet<>();
    private int selectedPosition = -1;

    /**
     * Interface for handling interactions with tab items in the switcher.
     */
    public interface OnTabClickListener {
        /**
         * Called when a tab preview is clicked to switch to that tab.
         * @param position The position of the clicked tab.
         */
        void onTabClick(int position);

        /**
         * Called when the close button on a tab preview is clicked.
         * @param position The position of the tab to be closed.
         */
        void onTabClose(int position);

        /**
         * Called when a tab preview is long-clicked.
         * @param position The position of the long-clicked tab.
         */
        void onTabLongClick(int position);
    }

    /**
     * Constructor for TabSwitcherAdapter.
     * @param siteList The list of open site URLs (master list).
     * @param tabPreviews A map containing cached bitmap previews for each URL.
     * @param listener The listener for tab click and close events.
     */
    public TabSwitcherAdapter(List<String> siteList, Map<String, Bitmap> tabPreviews, OnTabClickListener listener) {
        this.filteredList = siteList;
        this.tabPreviews = tabPreviews;
        this.listener = listener;
    }

    public void setFilteredList(List<String> list) {
        // Create a new list to avoid identity issues and shared reference mutations
        this.filteredList = new ArrayList<>(list);
        notifyDataSetChanged();
    }

    public void setIncognitoTabs(Set<String> incognitoTabs) {
        this.incognitoTabs = incognitoTabs;
        // Do not notifyDataSetChanged here to allow item animations in RecyclerView
    }

    public void setWorkspaceTabs(Set<String> workspaceTabs) {
        this.workspaceTabs = workspaceTabs;
    }

    public void setSelectedPosition(int position) {
        int previousPosition = selectedPosition;
        selectedPosition = position;
        if (previousPosition >= 0 && previousPosition < getItemCount()) {
            notifyItemChanged(previousPosition);
        }
        if (selectedPosition >= 0 && selectedPosition < getItemCount()) {
            notifyItemChanged(selectedPosition);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tab_preview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String url = filteredList.get(position);
        
        // Determine the display name for the tab
        String displayName;
        if (url.equals("home://dashboard")) {
            displayName = "Home";
        } else if (url.equals("home://quickaccess_messages")) {
            displayName = "Messages";
        } else if (url.equals("home://workspace")) {
            displayName = "Workspace";
        } else if (url.startsWith("home://incognito")) {
            displayName = "Incognito";
        } else {
            displayName = url.replace("https://", "").replace("http://", "").replace("www.", "");
            int slashIndex = displayName.indexOf('/');
            if (slashIndex != -1) displayName = displayName.substring(0, slashIndex);
        }

        holder.tvTitle.setText(displayName);
        
        // Only show close button if there is more than one tab open
        holder.btnClose.setVisibility(filteredList.size() > 1 ? View.VISIBLE : View.GONE);
        
        // Show incognito icon if tab is incognito
        holder.incognitoIcon.setVisibility(incognitoTabs.contains(url) ? View.VISIBLE : View.GONE);
        
        // Show workspace icon if tab belongs to workspace
        holder.workspaceIcon.setVisibility(workspaceTabs.contains(url) ? View.VISIBLE : View.GONE);
        
        // Adjust title position if icons are visible
        RelativeLayout.LayoutParams titleParams = (RelativeLayout.LayoutParams) holder.tvTitle.getLayoutParams();
        if (holder.incognitoIcon.getVisibility() == View.VISIBLE) {
            titleParams.addRule(RelativeLayout.END_OF, R.id.incognitoIcon);
        } else if (holder.workspaceIcon.getVisibility() == View.VISIBLE) {
            titleParams.addRule(RelativeLayout.END_OF, R.id.workspaceIcon);
        } else {
            titleParams.removeRule(RelativeLayout.END_OF);
        }
        holder.tvTitle.setLayoutParams(titleParams);
        
        // Highlight current tab with border and dynamic glow based on theme
        if (position == selectedPosition) {
            holder.card.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.selected_tab_stroke));
            holder.card.setStrokeWidth(8);
            holder.card.setCardElevation(20f);
            holder.selectedGlow.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.selected_tab_glow));
            holder.selectedGlow.setVisibility(View.VISIBLE);
        } else {
            holder.card.setStrokeColor(Color.parseColor("#E0E0E0"));
            holder.card.setStrokeWidth(2);
            holder.card.setCardElevation(4f);
            holder.selectedGlow.setVisibility(View.GONE);
        }

        // Load the cached preview bitmap if available
        Bitmap preview = tabPreviews.get(url);
        if (preview != null) {
            holder.ivPreview.setImageBitmap(preview);
        } else {
            // Fallback icon if no preview exists
            if (url.startsWith("home://incognito")) {
                holder.ivPreview.setImageResource(R.drawable.incognito);
                holder.ivPreview.setBackgroundColor(Color.parseColor("#202124"));
            } else {
                holder.ivPreview.setImageResource(R.drawable.tabswitcher_image_preview);
                holder.ivPreview.setBackgroundColor(Color.parseColor("#F5F5F5"));
            }
        }

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                listener.onTabClick(pos);
            }
        });
        holder.btnClose.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                listener.onTabClose(pos);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                listener.onTabLongClick(pos);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    /**
     * ViewHolder class for tab preview items.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        ImageView ivPreview;
        TextView tvTitle;
        ImageButton btnClose;
        ImageView incognitoIcon;
        ImageView workspaceIcon;
        View selectedGlow;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            ivPreview = itemView.findViewById(R.id.ivTabPreview);
            tvTitle = itemView.findViewById(R.id.tvTabTitle);
            btnClose = itemView.findViewById(R.id.btnCloseTab);
            incognitoIcon = itemView.findViewById(R.id.incognitoIcon);
            workspaceIcon = itemView.findViewById(R.id.workspaceIcon);
            selectedGlow = itemView.findViewById(R.id.selectedGlow);
        }
    }
}
