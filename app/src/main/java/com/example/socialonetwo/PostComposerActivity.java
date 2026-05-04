package com.example.socialonetwo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class PostComposerActivity extends AppCompatActivity {

    private TextInputEditText etPostContent;
    private TextView tvCharCount;
    private RecyclerView rvMediaPreviews;
    private MediaPreviewAdapter mediaAdapter;
    private List<Uri> selectedMediaUris = new ArrayList<>();
    private static final String KEY_SELECTED_MEDIA = "selected_media_uris";

    // Using the modern Photo Picker for best compatibility and user experience
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher = registerForActivityResult(
            new ActivityResultContracts.PickMultipleVisualMedia(10),
            uris -> {
                if (uris != null && !uris.isEmpty()) {
                    selectedMediaUris.addAll(uris);
                    mediaAdapter.notifyDataSetChanged();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_post_composer);

        // Restore state if available
        if (savedInstanceState != null) {
            ArrayList<Uri> restoredUris = savedInstanceState.getParcelableArrayList(KEY_SELECTED_MEDIA);
            if (restoredUris != null) {
                selectedMediaUris.addAll(restoredUris);
            }
        }

        View rootView = findViewById(R.id.postComposerRoot);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
        }

        ImageButton btnClose = findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }

        etPostContent = findViewById(R.id.etPostContent);
        tvCharCount = findViewById(R.id.tvCharCount);
        rvMediaPreviews = findViewById(R.id.rvMediaPreviews);
        MaterialButton btnAddMedia = findViewById(R.id.btnAddMedia);
        ExtendedFloatingActionButton fabPost = findViewById(R.id.fabPost);

        mediaAdapter = new MediaPreviewAdapter(selectedMediaUris);
        rvMediaPreviews.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvMediaPreviews.setAdapter(mediaAdapter);

        // Correctly launch the picker for both images and videos
        btnAddMedia.setOnClickListener(v -> pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE)
                .build()));

        etPostContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String countText = s.length() + " characters";
                tvCharCount.setText(countText);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        fabPost.setOnClickListener(v -> handleManualShare());

        // Only handle incoming intent on fresh start, not on configuration change
        if (savedInstanceState == null) {
            handleIncomingIntent();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(KEY_SELECTED_MEDIA, new ArrayList<>(selectedMediaUris));
    }

    private void handleIncomingIntent() {
        Intent intent = getIntent();
        if (intent == null) return;
        
        String action = intent.getAction();
        String type = intent.getType();

        // Handle text or explicit image_url
        if (intent.hasExtra("image_url")) {
            String imageUrl = intent.getStringExtra("image_url");
            if (imageUrl != null) {
                selectedMediaUris.add(Uri.parse(imageUrl));
                mediaAdapter.notifyDataSetChanged();
            }
        }

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    etPostContent.setText(sharedText);
                }
            } else if (type.startsWith("image/") || type.startsWith("video/")) {
                Uri sharedUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (sharedUri != null) {
                    selectedMediaUris.add(sharedUri);
                    mediaAdapter.notifyDataSetChanged();
                }
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null && etPostContent.length() == 0) {
                    etPostContent.setText(sharedText);
                }
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/") || type.startsWith("video/")) {
                ArrayList<Uri> sharedUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (sharedUris != null) {
                    selectedMediaUris.addAll(sharedUris);
                    mediaAdapter.notifyDataSetChanged();
                }
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null && etPostContent.length() == 0) {
                    etPostContent.setText(sharedText);
                }
            }
        }
    }

    private void handleManualShare() {
        String content = etPostContent.getText().toString().trim();

        if (content.isEmpty() && selectedMediaUris.isEmpty()) {
            Toast.makeText(this, "Please add some content or media", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent();
        if (selectedMediaUris.isEmpty()) {
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, content);
            shareIntent.setType("text/plain");
        } else if (selectedMediaUris.size() == 1) {
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, content);
            Uri uri = selectedMediaUris.get(0);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.setType(getMimeType(uri));
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.putExtra(Intent.EXTRA_TEXT, content);
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, new ArrayList<>(selectedMediaUris));
            shareIntent.setType("*/*");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        startActivity(Intent.createChooser(shareIntent, "Share post via..."));
    }

    private String getMimeType(Uri uri) {
        String type = getContentResolver().getType(uri);
        if (type == null) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }
        return (type == null) ? "*/*" : type;
    }

    private class MediaPreviewAdapter extends RecyclerView.Adapter<MediaPreviewAdapter.ViewHolder> {
        private final List<Uri> uris;

        MediaPreviewAdapter(List<Uri> uris) { this.uris = uris; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media_preview, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Uri uri = uris.get(position);
            Glide.with(holder.itemView.getContext()).load(uri).into(holder.ivPreview);
            
            String mimeType = getMimeType(uri);
            if (mimeType != null && mimeType.startsWith("video")) {
                holder.ivVideoIcon.setVisibility(View.VISIBLE);
            } else {
                holder.ivVideoIcon.setVisibility(View.GONE);
            }

            holder.btnRemove.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    uris.remove(pos);
                    notifyItemRemoved(pos);
                }
            });
        }

        @Override
        public int getItemCount() { return uris.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivPreview;
            ImageView ivVideoIcon;
            ImageButton btnRemove;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivPreview = itemView.findViewById(R.id.ivPreview);
                ivVideoIcon = itemView.findViewById(R.id.ivVideoIcon);
                btnRemove = itemView.findViewById(R.id.btnRemoveMedia);
            }
        }
    }
}
