package com.example.socialonetwo;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectableTabsAdapter extends RecyclerView.Adapter<SelectableTabsAdapter.ViewHolder> {

    private final List<String> siteList;
    private final Set<String> selectedTabs = new HashSet<>();

    public SelectableTabsAdapter(List<String> siteList) {
        this.siteList = siteList;
    }

    public Set<String> getSelectedTabs() {
        return selectedTabs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selectable_tab, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String url = siteList.get(position);
        String display;
        if (url.startsWith("home://")) {
            if (url.equals("home://dashboard")) display = "Home";
            else if (url.equals("home://quickaccess_messages")) display = "Messages";
            else display = "App Tab";
        } else {
            display = url.replace("https://", "").replace("http://", "").replace("www.", "");
            int slash = display.indexOf('/');
            if (slash != -1) display = display.substring(0, slash);
        }

        if (display.isEmpty()) display = "Web Tab";
        holder.title.setText(display);
        holder.checkbox.setChecked(selectedTabs.contains(url));

        holder.card.setStrokeColor(selectedTabs.contains(url) ? Color.parseColor("#4285F4") : Color.parseColor("#E0E0E0"));
        holder.card.setStrokeWidth(selectedTabs.contains(url) ? 4 : 2);

        holder.itemView.setOnClickListener(v -> {
            boolean isSelected = selectedTabs.contains(url);
            if (isSelected) {
                selectedTabs.remove(url);
            } else {
                selectedTabs.add(url);
            }
            
            // Update UI directly for immediate feedback and to avoid scroll-jumping issues
            boolean nowSelected = !isSelected;
            holder.checkbox.setChecked(nowSelected);
            holder.card.setStrokeColor(nowSelected ? Color.parseColor("#4285F4") : Color.parseColor("#E0E0E0"));
            holder.card.setStrokeWidth(nowSelected ? 4 : 2);
        });
        
        // Placeholder for favicon logic if available
        // UIUtils.loadFavicon(url, holder.icon);
    }

    @Override
    public int getItemCount() {
        return siteList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        CheckBox checkbox;
        MaterialCardView card;

        ViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.tabTitle);
            checkbox = v.findViewById(R.id.tabCheckbox);
            card = v.findViewById(R.id.selectableCard);
        }
    }
}