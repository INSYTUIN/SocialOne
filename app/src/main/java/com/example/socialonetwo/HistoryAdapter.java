package com.example.socialonetwo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for the browsing history list.
 * Displays a list of URLs that the user has previously visited.
 * Supports clicking a history item to navigate to it and long-clicking to delete it.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final List<String> historyList;
    private final OnHistoryClickListener listener;

    /**
     * Interface for handling interactions with history items.
     */
    public interface OnHistoryClickListener {
        /**
         * Called when a history item is clicked.
         * @param url The URL of the clicked history item.
         */
        void onHistoryClick(String url);

        /**
         * Called when a history item is long-clicked.
         * @param url The URL of the long-clicked item.
         * @param position The position of the item in the list.
         */
        void onHistoryLongClick(String url, int position);
    }

    /**
     * Constructor for HistoryAdapter.
     * @param historyList The list of history URLs to display.
     * @param listener The listener for interaction events.
     */
    public HistoryAdapter(List<String> historyList, OnHistoryClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        String url = historyList.get(position);
        holder.historyUrl.setText(url);
        
        holder.itemView.setOnClickListener(v -> listener.onHistoryClick(url));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onHistoryLongClick(url, holder.getAdapterPosition());
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    /**
     * ViewHolder class for history items.
     */
    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView historyUrl;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            historyUrl = itemView.findViewById(R.id.historyUrl);
        }
    }
}