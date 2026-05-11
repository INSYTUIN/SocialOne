package com.example.socialonetwo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class WorkspaceAdapter extends RecyclerView.Adapter<WorkspaceAdapter.ViewHolder> {

    private final List<WorkspaceFragment.Workspace> list;
    private final OnWorkspaceActionListener listener;

    public interface OnWorkspaceActionListener {
        void onLaunch(WorkspaceFragment.Workspace workspace);
        void onEdit(int position, WorkspaceFragment.Workspace workspace);
        void onDelete(int position);
    }

    public WorkspaceAdapter(List<WorkspaceFragment.Workspace> list, OnWorkspaceActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workspace, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkspaceFragment.Workspace item = list.get(position);
        holder.name.setText(item.name);
        holder.count.setText(item.tabs.size() + (item.tabs.size() == 1 ? " tab" : " tabs"));

        holder.btnLaunch.setOnClickListener(v -> listener.onLaunch(item));
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(holder.getAdapterPosition(), item));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(holder.getAdapterPosition()));
        
        UIUtils.setClickAnimation(holder.itemView.getContext(), holder.btnLaunch);
        UIUtils.setClickAnimation(holder.itemView.getContext(), holder.btnEdit);
        UIUtils.setClickAnimation(holder.itemView.getContext(), holder.btnDelete);

        // Preview Tabs - shortened names in flowing justified layout
        holder.previewContainer.removeAllViews();
        int maxPreviews = 6;
        for (int i = 0; i < Math.min(item.tabs.size(), maxPreviews); i++) {
            Chip chip = new Chip(holder.itemView.getContext());
            String url = item.tabs.get(i);
            String display;
            if (url.startsWith("home://")) {
                switch (url) {
                    case "home://dashboard":
                        display = "Home";
                        break;
                    case "home://quickaccess_messages":
                        display = "Messages";
                        break;
                    case "home://workspace":
                        display = "Workspace";
                        break;
                    default:
                        display = "App";
                        break;
                }
            } else {
                display = url.replace("https://", "").replace("http://", "").replace("www.", "");
                int slash = display.indexOf('/');
                if (slash != -1) display = display.substring(0, slash);
            }

            chip.setText(display);
            chip.setChipMinHeight(UIUtils.dpToPx(holder.itemView.getContext(), 32));
            chip.setTextSize(13);
            chip.setClickable(false);
            chip.setCheckable(false);
            
            holder.previewContainer.addView(chip);
        }
        
        if (item.tabs.size() > maxPreviews) {
            Chip moreChip = new Chip(holder.itemView.getContext());
            moreChip.setText("+" + (item.tabs.size() - maxPreviews));
            moreChip.setChipMinHeight(UIUtils.dpToPx(holder.itemView.getContext(), 32));
            moreChip.setTextSize(13);
            moreChip.setClickable(false);
            holder.previewContainer.addView(moreChip);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, count;
        MaterialButton btnLaunch;
        ImageButton btnDelete, btnEdit;
        ChipGroup previewContainer;

        ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.workspaceName);
            count = v.findViewById(R.id.workspaceTabsCount);
            btnLaunch = v.findViewById(R.id.btnLaunchWorkspace);
            btnEdit = v.findViewById(R.id.btnEditWorkspace);
            btnDelete = v.findViewById(R.id.btnDeleteWorkspace);
            previewContainer = v.findViewById(R.id.tabsPreviewContainer);
        }
    }
}