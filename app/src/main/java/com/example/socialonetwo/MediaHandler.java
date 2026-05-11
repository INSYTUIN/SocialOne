package com.example.socialonetwo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
                String ext = getMediaExtension(url);
                String domain = getDomain(url);

                holder.name.setText("Media " + (position + 1) + " (" + domain + ")");
                holder.extension.setText(ext.isEmpty() ? "unknown" : ext.toUpperCase());

                Glide.with(context)
                        .load(url)
                        .centerCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .into(holder.preview);

                holder.itemView.setOnClickListener(v -> {
                    downloadHandler.downloadFile(url, null, null, userAgent);
                });

                holder.itemView.setOnLongClickListener(v -> {
                    showMediaOptions(url);
                    return true;
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

    private void showMediaOptions(String url) {
        String[] options = {"Download", "Add to Post Creator"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setTitle("Media Options")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        downloadHandler.downloadFile(url, null, null, userAgent);
                    } else if (which == 1) {
                        addMediaToPost(url);
                    }
                })
                .show();
    }

    public void addMediaToPost(String imageUrl) {
        Toast.makeText(context, "Preparing media for post...", Toast.LENGTH_SHORT).show();
        Glide.with(context)
                .asFile()
                .load(imageUrl)
                .into(new CustomTarget<File>() {
                    @Override
                    public void onResourceReady(@NonNull File resource, @Nullable Transition<? super File> transition) {
                        try {
                            String fileName = "grabbed_media_" + System.currentTimeMillis() + ".jpg";
                            File cacheFile = new File(context.getCacheDir(), fileName);
                            copyFile(resource, cacheFile);

                            Uri contentUri = FileProvider.getUriForFile(context,
                                    context.getPackageName() + ".provider", cacheFile);

                            Intent intent = new Intent(context, PostComposerActivity.class);
                            intent.setAction(Intent.ACTION_SEND);
                            intent.setType("image/*");
                            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            context.startActivity(intent);
                        } catch (IOException e) {
                            Log.e("MediaHandler", "Failed to prepare media", e);
                            Toast.makeText(context, "Failed to prepare media", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        Toast.makeText(context, "Failed to download media", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void copyFile(File source, File target) throws IOException {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(target)) {
            byte[] buf = new byte[8192];
            int length;
            while ((length = in.read(buf)) > 0) {
                out.write(buf, 0, length);
            }
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

    private String getMediaExtension(String url) {
        if (url == null || url.isEmpty()) return "";
        
        // Remove query parameters and fragments
        String cleanUrl = url;
        int queryIndex = cleanUrl.indexOf('?');
        if (queryIndex != -1) cleanUrl = cleanUrl.substring(0, queryIndex);
        int fragmentIndex = cleanUrl.indexOf('#');
        if (fragmentIndex != -1) cleanUrl = cleanUrl.substring(0, fragmentIndex);

        // Get extension from the cleaned path
        String extension = MimeTypeMap.getFileExtensionFromUrl(cleanUrl);
        
        // If extension is empty, try manual extraction
        if (extension.isEmpty()) {
            int lastDot = cleanUrl.lastIndexOf('.');
            int lastSlash = cleanUrl.lastIndexOf('/');
            if (lastDot > lastSlash && lastDot != -1) {
                extension = cleanUrl.substring(lastDot + 1);
            }
        }
        
        // Sanity check: extensions shouldn't be too long (handle cases like /some.path/without/ext)
        if (extension.length() > 5) return "";

        return extension.toLowerCase();
    }

    private String getDomain(String url) {

        if (url == null || url.startsWith("home://")) return url;
        String domain = url.replace("https://", "").replace("http://", "").replace("www.", "");
        int slashIndex = domain.indexOf('/');
        if (slashIndex != -1) domain = domain.substring(0, slashIndex);
        return domain;
    }
}
