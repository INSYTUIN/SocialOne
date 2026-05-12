package com.example.socialonetwo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for the browsing history list.
 * Displays a list of URLs grouped by the date of visit.
 */
public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final List<String> historyList;
    private final List<Object> displayList = new ArrayList<>();
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
     * @param historyList The list of history URLs (format: url|timestamp) to display.
     * @param listener The listener for interaction events.
     */
    public HistoryAdapter(List<String> historyList, OnHistoryClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
        processHistory();
    }

    private void processHistory() {
        displayList.clear();
        if (historyList == null || historyList.isEmpty()) return;

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        String today = sdf.format(new Date());
        String yesterday = sdf.format(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));
        String lastDate = "";

        for (String entry : historyList) {
            String[] parts = entry.split("\\|", 2);
            long timestamp = parts.length > 1 ? Long.parseLong(parts[1]) : System.currentTimeMillis();

            String date = sdf.format(new Date(timestamp));
            
            String displayDate = date;
            if (date.equals(today)) displayDate = "Today";
            else if (date.equals(yesterday)) displayDate = "Yesterday";

            if (!displayDate.equals(lastDate)) {
                displayList.add(displayDate); // Header string
                lastDate = displayDate;
            }
            displayList.add(entry); // History entry string
        }
    }

    @Override
    public int getItemViewType(int position) {
        return (displayList.get(position) instanceof String && !((String) displayList.get(position)).contains("|")) 
                ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
            return new HistoryViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = displayList.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvHeader.setText((String) item);
        } else if (holder instanceof HistoryViewHolder) {
            String entry = (String) item;
            String url = entry.contains("|") ? entry.split("\\|")[0] : entry;
            ((HistoryViewHolder) holder).historyUrl.setText(url);
            
            holder.itemView.setOnClickListener(v -> listener.onHistoryClick(url));
            holder.itemView.setOnLongClickListener(v -> {
                // We need to find the real position in historyList for deletion
                int realPos = historyList.indexOf(entry);
                if (realPos != -1) {
                    listener.onHistoryLongClick(url, realPos);
                }
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    /**
     * Updates the data and refreshes the grouping.
     */
    public void notifyHistoryChanged() {
        processHistory();
        notifyDataSetChanged();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView historyUrl;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            historyUrl = itemView.findViewById(R.id.historyUrl);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tvHeader);
        }
    }
}