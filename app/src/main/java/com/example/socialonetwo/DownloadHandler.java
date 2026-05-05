package com.example.socialonetwo;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadHandler {

    private final Context context;
    private final Handler downloadUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable downloadUpdateRunnable;
    private AlertDialog downloadsDialog;

    public DownloadHandler(Context context) {
        this.context = context;
    }

    public void downloadFile(String url, String mimetype, String contentDisposition, String userAgent) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        if (mimetype != null) {
            request.setMimeType(mimetype);
        }
        String cookies = CookieManager.getInstance().getCookie(url);
        request.addRequestHeader("cookie", cookies);
        request.addRequestHeader("User-Agent", userAgent);
        request.setDescription("Downloading file...");
        request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype));
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype));

        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm != null) {
            dm.enqueue(request);
            Toast.makeText(context, "Downloading File", Toast.LENGTH_LONG).show();
            showDownloadsDialog();
        }
    }

    public void showDownloadsDialog() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_history, null);
        TextView title = dialogView.findViewById(R.id.historyTitle);
        title.setText(R.string.downloads_title);

        RecyclerView rv = dialogView.findViewById(R.id.historyRecyclerView);
        Button clearAll = dialogView.findViewById(R.id.btnClearAllHistory);
        ImageButton closeBtn = dialogView.findViewById(R.id.btnCloseHistory);
        TextView tvEmpty = dialogView.findViewById(R.id.tvEmptyMessage);

        tvEmpty.setText(R.string.downloads_empty);
        UIUtils.setClickAnimation(context, closeBtn);

        DownloadsAdapter adapter = new DownloadsAdapter(tvEmpty, rv);
        rv.setLayoutManager(new LinearLayoutManager(context));
        rv.setAdapter(adapter);

        com.google.android.material.dialog.MaterialAlertDialogBuilder downloadsDialogBuilder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(context);
        downloadsDialogBuilder.setView(dialogView);
        downloadsDialog = downloadsDialogBuilder.create();

        if (downloadsDialog.getWindow() != null) {
            downloadsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        closeBtn.setOnClickListener(v -> {
            stopDownloadPolling();
            downloadsDialog.dismiss();
        });

        clearAll.setVisibility(View.GONE);

        downloadsDialog.show();

        if (downloadsDialog.getWindow() != null) {
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.95);
            downloadsDialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        startDownloadPolling(adapter, tvEmpty, rv);
    }

    private void startDownloadPolling(DownloadsAdapter adapter, TextView tvEmpty, RecyclerView rv) {
        downloadUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateDownloadListFromManager(adapter, tvEmpty, rv);
                downloadUpdateHandler.postDelayed(this, 1000);
            }
        };
        downloadUpdateHandler.post(downloadUpdateRunnable);
    }

    public void stopDownloadPolling() {
        if (downloadUpdateRunnable != null) {
            downloadUpdateHandler.removeCallbacks(downloadUpdateRunnable);
        }
    }

    @SuppressLint("Range")
    private void updateDownloadListFromManager(DownloadsAdapter adapter, TextView tvEmpty, RecyclerView rv) {
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) return;

        DownloadManager.Query query = new DownloadManager.Query();
        Cursor cursor = dm.query(query);

        List<DownloadItem> items = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                DownloadItem item = new DownloadItem();
                item.id = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID));
                item.title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE));
                item.status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                item.totalSize = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                item.bytesSoFar = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                item.localUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                items.add(item);
            } while (cursor.moveToNext());
            cursor.close();
        }

        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        rv.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);

        adapter.setItems(items);
    }

    private static class DownloadItem {
        long id;
        String title;
        int status;
        long totalSize;
        long bytesSoFar;
        String localUri;
    }

    private class DownloadsAdapter extends RecyclerView.Adapter<DownloadsAdapter.ViewHolder> {
        private List<DownloadItem> items = new ArrayList<>();
        private final TextView tvEmpty;
        private final RecyclerView rv;

        public DownloadsAdapter(TextView tvEmpty, RecyclerView rv) {
            this.tvEmpty = tvEmpty;
            this.rv = rv;
        }

        @SuppressLint("NotifyDataSetChanged")
        public void setItems(List<DownloadItem> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DownloadItem item = items.get(position);
            holder.fileName.setText(item.title != null ? item.title : "Download");

            if (item.status == DownloadManager.STATUS_RUNNING) {
                holder.progressBar.setVisibility(View.VISIBLE);
                if (item.totalSize > 0) {
                    int progress = (int) ((item.bytesSoFar * 100) / item.totalSize);
                    holder.progressBar.setProgress(progress);
                    holder.fileDetails.setText("Downloading... " + progress + "%");
                } else {
                    holder.progressBar.setIndeterminate(true);
                    holder.fileDetails.setText("Downloading...");
                }
            } else {
                holder.progressBar.setVisibility(View.GONE);
                if (item.status == DownloadManager.STATUS_SUCCESSFUL) {
                    holder.fileDetails.setText("Completed (" + (item.totalSize / 1024) + " KB)");
                } else if (item.status == DownloadManager.STATUS_FAILED) {
                    holder.fileDetails.setText("Download Failed");
                } else {
                    holder.fileDetails.setText("Status: Queued");
                }
            }

            holder.itemView.setOnClickListener(v -> {
                if (item.status != DownloadManager.STATUS_SUCCESSFUL || item.localUri == null) return;
                try {
                    File file = new File(Uri.parse(item.localUri).getPath());
                    Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
                    Intent intent = new Intent(Intent.ACTION_VIEW);

                    String extension = MimeTypeMap.getFileExtensionFromUrl(contentUri.toString());
                    String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
                    if (mimeType == null) mimeType = "*/*";

                    intent.setDataAndType(contentUri, mimeType);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "Cannot open file", Toast.LENGTH_SHORT).show();
                }
            });

            holder.btnOptions.setOnClickListener(v -> {
                androidx.appcompat.view.ContextThemeWrapper wrapper = new androidx.appcompat.view.ContextThemeWrapper(context, R.style.PopupMenuTheme);
                androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(wrapper, v);
                popup.getMenu().add(R.string.dialog_delete_file);
                popup.setOnMenuItemClickListener(menuItem -> {
                    if (menuItem.getTitle() != null && menuItem.getTitle().toString().equals(context.getString(R.string.dialog_delete_file))) {
                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                                .setTitle(R.string.dialog_remove_download_title)
                                .setMessage(context.getString(R.string.dialog_remove_download_message, item.title))
                                .setPositiveButton(R.string.delete, (dialog, which) -> {
                                    DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                                    if (dm != null) dm.remove(item.id);
                                    updateDownloadListFromManager(this, tvEmpty, rv);
                                })
                                .setNegativeButton(R.string.cancel, null)
                                .show();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView fileName, fileDetails;
            ImageButton btnOptions;
            ProgressBar progressBar;

            ViewHolder(View itemView) {
                super(itemView);
                fileName = itemView.findViewById(R.id.tvFileName);
                fileDetails = itemView.findViewById(R.id.tvFileDetails);
                btnOptions = itemView.findViewById(R.id.btnDownloadOptions);
                progressBar = itemView.findViewById(R.id.pbDownload);
            }
        }
    }
}
