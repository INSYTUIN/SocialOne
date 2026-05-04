package com.example.socialonetwo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class PostComposerActivity extends AppCompatActivity {

    private TextInputEditText etPostContent;
    private TextView tvCharCount;
    private RecyclerView rvMediaPreviews, rvDrafts;
    private MediaPreviewAdapter mediaAdapter;
    private DraftAdapter draftAdapter;
    private List<Uri> selectedMediaUris = new ArrayList<>();
    private List<JSONObject> drafts = new ArrayList<>();
    private TextView tvDraftsTitle, tvMediaPlaceholder, tvDraftsPlaceholder;
    private static final String KEY_SELECTED_MEDIA = "selected_media_uris";
    private static final String PREFS_NAME = "PostDraftsPrefs";
    private static final String KEY_DRAFTS = "saved_drafts";

    // Using the modern Photo Picker for best compatibility and user experience
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher = registerForActivityResult(
            new ActivityResultContracts.PickMultipleVisualMedia(10),
            uris -> {
                if (uris != null && !uris.isEmpty()) {
                    selectedMediaUris.addAll(uris);
                    mediaAdapter.notifyDataSetChanged();
                    updateMediaVisibility();
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
            btnClose.setOnClickListener(v -> onBackPressed());
        }

        ImageButton btnClearAll = findViewById(R.id.btnClearAll);
        if (btnClearAll != null) {
            btnClearAll.setOnClickListener(v -> showClearConfirmDialog());
        }

        etPostContent = findViewById(R.id.etPostContent);
        tvCharCount = findViewById(R.id.tvCharCount);
        rvMediaPreviews = findViewById(R.id.rvMediaPreviews);
        rvDrafts = findViewById(R.id.rvDrafts);
        tvDraftsTitle = findViewById(R.id.tvDraftsTitle);
        tvMediaPlaceholder = findViewById(R.id.tvMediaPlaceholder);
        tvDraftsPlaceholder = findViewById(R.id.tvDraftsPlaceholder);
        MaterialButton btnAddMedia = findViewById(R.id.btnAddMedia);
        MaterialButton btnSaveDraft = findViewById(R.id.btnSaveDraft);
        ExtendedFloatingActionButton fabPost = findViewById(R.id.fabPost);

        setClickAnimations(btnClose, btnClearAll, btnAddMedia, btnSaveDraft, fabPost);

        mediaAdapter = new MediaPreviewAdapter(selectedMediaUris);
        rvMediaPreviews.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvMediaPreviews.setAdapter(mediaAdapter);

        draftAdapter = new DraftAdapter(drafts);
        rvDrafts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvDrafts.setAdapter(draftAdapter);

        loadDrafts();
        updateMediaVisibility();

        // Correctly launch the picker for both images and videos
        btnAddMedia.setOnClickListener(v -> pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE)
                .build()));

        btnSaveDraft.setOnClickListener(v -> saveCurrentAsDraft());

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

    @Override
    public void onBackPressed() {
        if (!etPostContent.getText().toString().trim().isEmpty() || !selectedMediaUris.isEmpty()) {
            showExitConfirmationDialog();
        } else {
            super.onBackPressed();
        }
    }

    private void showExitConfirmationDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_draft, null);
        Button btnSaveDraft = dialogView.findViewById(R.id.btnSaveDraft);
        Button btnDiscard = dialogView.findViewById(R.id.btnDiscard);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        builder.setView(dialogView);
        final AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnSaveDraft.setOnClickListener(v -> {
            saveCurrentAsDraft();
            dialog.dismiss();
            finish();
        });

        btnDiscard.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
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

    private void saveCurrentAsDraft() {
        String content = etPostContent.getText().toString().trim();
        if (content.isEmpty() && selectedMediaUris.isEmpty()) {
            Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject draft = new JSONObject();
            draft.put("text", content);
            JSONArray mediaArray = new JSONArray();
            for (Uri uri : selectedMediaUris) {
                mediaArray.put(uri.toString());
            }
            draft.put("media", mediaArray);
            draft.put("timestamp", System.currentTimeMillis());

            drafts.add(0, draft);
            saveDraftsToPrefs();
            draftAdapter.notifyItemInserted(0);
            rvDrafts.scrollToPosition(0);
            updateDraftsVisibility();
            Toast.makeText(this, "Draft saved", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void saveDraftsToPrefs() {
        JSONArray array = new JSONArray(drafts);
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_DRAFTS, array.toString())
                .apply();
    }

    private void loadDrafts() {
        String json = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_DRAFTS, null);
        if (json != null) {
            try {
                JSONArray array = new JSONArray(json);
                drafts.clear();
                for (int i = 0; i < array.length(); i++) {
                    drafts.add(array.getJSONObject(i));
                }
                draftAdapter.notifyDataSetChanged();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        updateDraftsVisibility();
    }

    private void updateDraftsVisibility() {
        if (drafts.isEmpty()) {
            rvDrafts.setVisibility(View.GONE);
            tvDraftsPlaceholder.setVisibility(View.VISIBLE);
        } else {
            rvDrafts.setVisibility(View.VISIBLE);
            tvDraftsPlaceholder.setVisibility(View.GONE);
        }
        tvDraftsTitle.setVisibility(View.VISIBLE); // Always visible as requested
    }

    private void updateMediaVisibility() {
        if (selectedMediaUris.isEmpty()) {
            rvMediaPreviews.setVisibility(View.GONE);
            tvMediaPlaceholder.setVisibility(View.VISIBLE);
        } else {
            rvMediaPreviews.setVisibility(View.VISIBLE);
            tvMediaPlaceholder.setVisibility(View.GONE);
        }
    }

    private void useDraft(JSONObject draft) {
        try {
            etPostContent.setText(draft.optString("text", ""));
            selectedMediaUris.clear();
            JSONArray mediaArray = draft.optJSONArray("media");
            if (mediaArray != null) {
                for (int i = 0; i < mediaArray.length(); i++) {
                    selectedMediaUris.add(Uri.parse(mediaArray.getString(i)));
                }
            }
            mediaAdapter.notifyDataSetChanged();
            updateMediaVisibility();
            Toast.makeText(this, "Draft loaded", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void showClearConfirmDialog() {
        if (etPostContent.getText().toString().trim().isEmpty() && selectedMediaUris.isEmpty()) {
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_caution, null);
        TextView dTitle = dialogView.findViewById(R.id.confirmTitle);
        TextView dMessage = dialogView.findViewById(R.id.confirmMessage);
        Button dBtnConfirm = dialogView.findViewById(R.id.btnProceedConfirm);
        Button dBtnCancel = dialogView.findViewById(R.id.btnCancelConfirm);

        dTitle.setText("Clear Post?");
        dMessage.setText("This will remove all text and media currently in the composer. This cannot be undone.");
        dBtnConfirm.setText("Clear All");
        dBtnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF5252")));

        setClickAnimations(dBtnConfirm, dBtnCancel);

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        builder.setView(dialogView);
        AlertDialog clearDialog = builder.create();

        if (clearDialog.getWindow() != null) {
            clearDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dBtnConfirm.setOnClickListener(v -> {
            clearDialog.dismiss();
            clearComposer();
        });

        dBtnCancel.setOnClickListener(v -> clearDialog.dismiss());
        clearDialog.show();
    }

    private void clearComposer() {
        etPostContent.setText("");
        selectedMediaUris.clear();
        mediaAdapter.notifyDataSetChanged();
        updateMediaVisibility();
        Toast.makeText(this, "Composer cleared", Toast.LENGTH_SHORT).show();
    }

    private String getMimeType(Uri uri) {
        String type = getContentResolver().getType(uri);
        if (type == null) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }
        return (type == null) ? "*/*" : type;
    }

    /**
     * Applies a scale animation to multiple views when touched.
     */
    private void setClickAnimations(View... views) {
        for (View view : views) {
            setClickAnimation(view);
        }
    }

    /**
     * Applies a scale animation to a view when touched to provide visual feedback.
     * @param view The view to apply the animation to.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setClickAnimation(View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_down));
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_up));
            }
            return false;
        });
    }

    private class DraftAdapter extends RecyclerView.Adapter<DraftAdapter.ViewHolder> {
        private final List<JSONObject> items;

        DraftAdapter(List<JSONObject> items) { this.items = items; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_draft_preview, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            JSONObject draft = items.get(position);
            String text = draft.optString("text", "");
            if (text.isEmpty()) {
                holder.tvContent.setText("Empty text draft");
                holder.tvContent.setAlpha(0.6f);
            } else {
                holder.tvContent.setText(text);
                holder.tvContent.setAlpha(1.0f);
            }

            JSONArray media = draft.optJSONArray("media");
            int mediaCount = media != null ? media.length() : 0;
            holder.tvMediaCount.setText(mediaCount + " items");
            holder.ivMediaIcon.setVisibility(mediaCount > 0 ? View.VISIBLE : View.GONE);
            holder.tvMediaCount.setVisibility(mediaCount > 0 ? View.VISIBLE : View.GONE);

            setClickAnimations(holder.itemView, holder.btnDelete);

            holder.itemView.setOnClickListener(v -> useDraft(draft));
            holder.btnDelete.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    items.remove(pos);
                    saveDraftsToPrefs();
                    notifyItemRemoved(pos);
                    updateDraftsVisibility();
                }
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvContent, tvMediaCount;
            ImageView ivMediaIcon;
            ImageButton btnDelete;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvContent = itemView.findViewById(R.id.tvDraftContent);
                tvMediaCount = itemView.findViewById(R.id.tvMediaCount);
                ivMediaIcon = itemView.findViewById(R.id.ivMediaIcon);
                btnDelete = itemView.findViewById(R.id.btnDeleteDraft);
            }
        }
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

            setClickAnimation(holder.btnRemove);

            holder.btnRemove.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    uris.remove(pos);
                    notifyItemRemoved(pos);
                    updateMediaVisibility();
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
