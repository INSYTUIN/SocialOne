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
import androidx.recyclerview.widget.DiffUtil;
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
        String finalMimeType = mimetype;
        // Fix for APK files often being served with generic MIME types
        if (url != null && url.toLowerCase().contains(".apk")) {
            if (finalMimeType == null || finalMimeType.equalsIgnoreCase("application/octet-stream") || finalMimeType.equalsIgnoreCase("binary/octet-stream")) {
                finalMimeType = "application/vnd.android.package-archive";
            }
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        if (finalMimeType != null) {
            request.setMimeType(finalMimeType);
        }
        
        String fileName = URLUtil.guessFileName(url, contentDisposition, finalMimeType);
        
        // Ensure APK extension if we detected it's an APK
        if (url != null && url.toLowerCase().contains(".apk") && !fileName.toLowerCase().endsWith(".apk")) {
            // Remove .bin if guessFileName added it erroneously
            if (fileName.toLowerCase().endsWith(".bin")) {
                fileName = fileName.substring(0, fileName.length() - 4) + ".apk";
            } else {
                fileName = fileName + ".apk";
            }
        }

        String cookies = CookieManager.getInstance().getCookie(url);
        request.addRequestHeader("cookie", cookies);
        request.addRequestHeader("User-Agent", userAgent);
        request.setDescription("Downloading file...");
        request.setTitle(fileName);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

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

        // Perform an initial update before showing to ensure content is ready
        updateDownloadListFromManager(adapter, tvEmpty, rv);

        if (downloadsDialog.getWindow() != null) {
            // Set width to 95% before showing to avoid wonky jumping animation
            android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
            lp.copyFrom(downloadsDialog.getWindow().getAttributes());
            lp.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.95);
            lp.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            downloadsDialog.getWindow().setAttributes(lp);
        }

        // Hide keyboard when opening downloads to prevent layout glitches
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            android.view.View focus = activity.getCurrentFocus();
            if (focus != null) {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
                focus.clearFocus();
            }
        }
        
        downloadsDialog.show();

        // Delay polling slightly to allow opening animation to finish smoothly
        downloadUpdateHandler.postDelayed(() -> startDownloadPolling(adapter, tvEmpty, rv), 300);
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 KB";
        if (size < 1000 * 1024) { // Less than 1000 KB
            return (size / 1024) + " KB";
        } else if (size < 1000 * 1024 * 1024) { // Less than 1000 MB
            return String.format(java.util.Locale.US, "%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format(java.util.Locale.US, "%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    private void showFileDetailsDialog(DownloadItem item) {
        StringBuilder details = new StringBuilder();
        details.append("File Name: ").append(item.title).append("\n\n");
        details.append("Size: ").append(formatFileSize(item.totalSize)).append("\n\n");
        if (item.mimeType != null) {
            details.append("Type: ").append(item.mimeType).append("\n\n");
        }
        if (item.localUri != null) {
            try {
                details.append("Path: ").append(Uri.parse(item.localUri).getPath()).append("\n\n");
            } catch (Exception ignored) {}
        }
        if (item.lastModified > 0) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault());
            details.append("Date: ").append(sdf.format(new java.util.Date(item.lastModified)));
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setTitle("File Details")
                .setMessage(details.toString())
                .setPositiveButton("OK", null)
                .show();
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

    private void updateDownloadListFromManager(DownloadsAdapter adapter, TextView tvEmpty, RecyclerView rv) {
        new Thread(() -> {
            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm == null) return;

            DownloadManager.Query query = new DownloadManager.Query();
            try (Cursor cursor = dm.query(query)) {
                List<DownloadItem> items = new ArrayList<>();
                if (cursor != null && cursor.moveToFirst()) {
                    int idIdx = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
                    int titleIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
                    int statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                    int bytesIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                    int uriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                    int mimeIdx = cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE);
                    int dateIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP);

                    do {
                        DownloadItem item = new DownloadItem();
                        item.id = cursor.getLong(idIdx);
                        item.title = cursor.getString(titleIdx);
                        item.status = cursor.getInt(statusIdx);
                        item.totalSize = cursor.getLong(totalIdx);
                        item.bytesSoFar = cursor.getLong(bytesIdx);
                        item.localUri = cursor.getString(uriIdx);
                        item.mimeType = cursor.getString(mimeIdx);
                        item.lastModified = cursor.getLong(dateIdx);
                        items.add(item);
                    } while (cursor.moveToNext());
                }

                downloadUpdateHandler.post(() -> {
                    if (tvEmpty != null) tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    if (rv != null) rv.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
                    adapter.setItems(items);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static class DownloadItem {
        long id;
        String title;
        int status;
        long totalSize;
        long bytesSoFar;
        String localUri;
        String mimeType;
        long lastModified;
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
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DownloadDiffCallback(this.items, newItems));
            this.items = newItems;
            diffResult.dispatchUpdatesTo(this);
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
                    holder.fileDetails.setText("Downloading... " + formatFileSize(item.bytesSoFar) + " / " + formatFileSize(item.totalSize) + " (" + progress + "%)");
                } else {
                    holder.progressBar.setIndeterminate(true);
                    holder.fileDetails.setText("Downloading...");
                }
            } else {
                holder.progressBar.setVisibility(View.GONE);
                if (item.status == DownloadManager.STATUS_SUCCESSFUL) {
                    holder.fileDetails.setText("Completed (" + formatFileSize(item.totalSize) + ")");
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
                    String finalMime;
                    if (extension != null && extension.equalsIgnoreCase("apk")) {
                        finalMime = "application/vnd.android.package-archive";
                    } else if (extension != null) {
                        finalMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
                    } else {
                        finalMime = "*/*";
                    }
                    if (finalMime == null) finalMime = "*/*";

                    intent.setDataAndType(contentUri, finalMime);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "Cannot open file", Toast.LENGTH_SHORT).show();
                }
            });

            holder.btnOptions.setOnClickListener(v -> {
                androidx.appcompat.view.ContextThemeWrapper wrapper = new androidx.appcompat.view.ContextThemeWrapper(context, R.style.PopupMenuTheme);
                androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(wrapper, v);
                popup.getMenu().add(0, 0, 0, R.string.option_file_details);
                popup.getMenu().add(0, 1, 1, R.string.dialog_delete_file);
                
                popup.setOnMenuItemClickListener(menuItem -> {
                    if (menuItem.getItemId() == 0) {
                        showFileDetailsDialog(item);
                        return true;
                    } else if (menuItem.getItemId() == 1) {
                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                                .setTitle(R.string.dialog_remove_download_title)
                                .setMessage(context.getString(R.string.dialog_remove_download_message, item.title))
                                .setPositiveButton(R.string.delete, (dialog, which) -> {
                                    DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                                    if (dm != null) dm.remove(item.id);
                                    updateDownloadListFromManager(DownloadsAdapter.this, tvEmpty, rv);
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

    private static class DownloadDiffCallback extends DiffUtil.Callback {
        private final List<DownloadItem> oldList;
        private final List<DownloadItem> newList;

        public DownloadDiffCallback(List<DownloadItem> oldList, List<DownloadItem> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() { return oldList.size(); }
        @Override
        public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).id == newList.get(newPos).id;
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            DownloadItem oldItem = oldList.get(oldPos);
            DownloadItem newItem = newList.get(newPos);
            return oldItem.status == newItem.status &&
                   oldItem.bytesSoFar == newItem.bytesSoFar &&
                   oldItem.totalSize == newItem.totalSize &&
                   (oldItem.title != null && oldItem.title.equals(newItem.title));
        }
    }
}
