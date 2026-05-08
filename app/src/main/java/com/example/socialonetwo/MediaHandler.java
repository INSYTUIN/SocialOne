package com.example.socialonetwo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility class to handle media detection and extraction from WebViews.
 * Provides UI for selecting and downloading detected media files.
 */
public class MediaHandler {

    /** The context in which the dialogs will be shown. */
    private final Context context;
    /** Handler for managing file downloads. */
    private final DownloadHandler downloadHandler;
    /** User agent string used for download requests. */
    private final String userAgent;

    /**
     * Constructs a MediaHandler.
     * @param context Activity context.
     * @param downloadHandler The download manager.
     * @param userAgent User agent for network requests.
     */
    public MediaHandler(Context context, DownloadHandler downloadHandler, String userAgent) {
        this.context = context;
        this.downloadHandler = downloadHandler;
        this.userAgent = userAgent;
    }

    /**
     * Displays a dialog containing a list of media URLs found on the current page.
     * @param detectedMediaUrls A set of unique media URLs detected.
     */
    public void showMediaGrabberDialog(Set<String> detectedMediaUrls) {
        if (detectedMediaUrls.isEmpty()) {
            Toast.makeText(context, "No media found on this page.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> urls = new ArrayList<>(detectedMediaUrls);

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_history, null);
        TextView title = dialogView.findViewById(R.id.historyTitle);
        title.setText("Media Found on Page");

        RecyclerView rv = dialogView.findViewById(R.id.historyRecyclerView);
        Button downloadAll = dialogView.findViewById(R.id.btnClearAllHistory);
        ImageButton closeBtn = dialogView.findViewById(R.id.btnCloseHistory);
        TextView tvEmpty = dialogView.findViewById(R.id.tvEmptyMessage);

        tvEmpty.setVisibility(View.GONE);
        downloadAll.setText("Download All");
        downloadAll.setVisibility(View.VISIBLE);
        UIUtils.setClickAnimation(context, closeBtn);
        UIUtils.setClickAnimation(context, downloadAll);

        class MediaGrabberAdapter extends RecyclerView.Adapter<MediaGrabberAdapter.ViewHolder> {
            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grab_media, parent, false);
                return new ViewHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                String url = urls.get(position);
                String ext = MimeTypeMap.getFileExtensionFromUrl(url);
                String domain = getDomain(url);

                holder.name.setText("Media " + (position + 1) + " (" + domain + ")");
                holder.extension.setText(ext.isEmpty() ? "unknown" : ext.toUpperCase());

                com.bumptech.glide.Glide.with(context)
                        .load(url)
                        .centerCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .into(holder.preview);

                holder.itemView.setOnClickListener(v -> {
                    downloadHandler.downloadFile(url, null, null, userAgent);
                });
            }

            @Override
            public int getItemCount() { return urls.size(); }

            class ViewHolder extends RecyclerView.ViewHolder {
                ImageView preview;
                TextView name, extension;
                ViewHolder(View v) {
                    super(v);
                    preview = v.findViewById(R.id.ivMediaPreview);
                    name = v.findViewById(R.id.tvMediaName);
                    extension = v.findViewById(R.id.tvMediaExtension);
                }
            }
        }

        rv.setLayoutManager(new LinearLayoutManager(context));
        rv.setAdapter(new MediaGrabberAdapter());

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(context);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        closeBtn.setOnClickListener(v -> dialog.dismiss());
        downloadAll.setOnClickListener(v -> {
            for (String url : urls) {
                downloadHandler.downloadFile(url, null, null, userAgent);
            }
            dialog.dismiss();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.95);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    public void showQRCodeDialog(String currentUrl) {
        if (currentUrl == null || currentUrl.startsWith("home://")) {
            Toast.makeText(context, "Cannot share this page", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_caution, null);
        TextView title = dialogView.findViewById(R.id.confirmTitle);
        TextView message = dialogView.findViewById(R.id.confirmMessage);
        Button btnClose = dialogView.findViewById(R.id.btnProceedConfirm);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelConfirm);

        title.setText(R.string.qr_title);
        message.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);
        btnClose.setText("Close");

        LinearLayout container = dialogView.findViewById(R.id.dialogContainer);
        ImageView qrView = new ImageView(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(UIUtils.dpToPx(context, 250), UIUtils.dpToPx(context, 250));
        lp.gravity = Gravity.CENTER;
        lp.setMargins(0, UIUtils.dpToPx(context, 20), 0, UIUtils.dpToPx(context, 20));
        qrView.setLayoutParams(lp);

        String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=500x500&data=" + Uri.encode(currentUrl);
        com.bumptech.glide.Glide.with(context).load(qrUrl).into(qrView);

        container.addView(qrView, 1);

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(context);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private String getDomain(String url) {
        if (url == null || url.startsWith("home://")) return url;
        String domain = url.replace("https://", "").replace("http://", "").replace("www.", "");
        int slashIndex = domain.indexOf('/');
        if (slashIndex != -1) domain = domain.substring(0, slashIndex);
        return domain;
    }
}
