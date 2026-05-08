package com.example.socialonetwo;

import android.annotation.SuppressLint;
import android.content.ClipData;
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
import android.util.Log;
import android.view.animation.AnimationUtils;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import androidx.core.content.FileProvider;
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

    /** Input field for the post's text content. */
    private TextInputEditText etPostContent;
    /** TextView displaying the current character count of the post. */
    private TextView tvCharCount;
    /** RecyclerView for displaying previews of selected media. */
    private RecyclerView rvMediaPreviews;
    /** RecyclerView for displaying saved post drafts. */
    private RecyclerView rvDrafts;
    /** Adapter for the media previews. */
    private MediaPreviewAdapter mediaAdapter;
    /** Adapter for the post drafts. */
    private DraftAdapter draftAdapter;
    /** Manager for Firestore database operations. */
    private FirestoreManager firestoreManager;
    /** List of URIs for media files selected by the user. */
    private List<Uri> selectedMediaUris = new ArrayList<>();
    /** List of post drafts retrieved from storage. */
    private List<JSONObject> drafts = new ArrayList<>();
    /** UI element for the drafts section title. */
    private TextView tvDraftsTitle;
    /** Placeholder text shown when no media is selected. */
    private TextView tvMediaPlaceholder;
    /** Placeholder text shown when no drafts are available. */
    private TextView tvDraftsPlaceholder;
    /** Message displayed on the loading overlay. */
    private TextView tvLoadingMessage;
    /** Overlay view shown during long-running operations. */
    private View loadingOverlay;
    private static final String KEY_SELECTED_MEDIA = "selected_media_uris";
    private static final String PREFS_NAME = "PostDraftsPrefs";
    private static final String KEY_DRAFTS = "saved_drafts";
    private static final int MAX_DRAFTS = 15;
    private static final String DRAFTS_MEDIA_DIR = "drafts_media";

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
        loadingOverlay = findViewById(R.id.loadingOverlay);
        tvLoadingMessage = findViewById(R.id.tvLoadingMessage);
        MaterialButton btnAddMedia = findViewById(R.id.btnAddMedia);
        MaterialButton btnSaveDraft = findViewById(R.id.btnSaveDraft);
        ExtendedFloatingActionButton fabPost = findViewById(R.id.fabPost);

        setClickAnimations(btnClose, btnClearAll, btnAddMedia, btnSaveDraft, fabPost);

        firestoreManager = new FirestoreManager();

        mediaAdapter = new MediaPreviewAdapter(selectedMediaUris);
        rvMediaPreviews.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvMediaPreviews.setAdapter(mediaAdapter);

        draftAdapter = new DraftAdapter(drafts);
        rvDrafts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvDrafts.setAdapter(draftAdapter);

        loadDrafts();
        updateMediaVisibility();
        updateCharCount(0);

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
                updateCharCount(s.length());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        fabPost.setOnClickListener(v -> handleManualShare());

        // Handle incoming intent (always do this to ensure data is picked up)
        handleIncomingIntent();
    }

    /**
     * Updates the character count display.
     * @param length The current length of the text.
     */
    private void updateCharCount(int length) {
        String countText = length + " characters";
        tvCharCount.setText(countText);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent();
    }

    /**
     * Handles intents that share text or media to this activity.
     */
    private void handleIncomingIntent() {
        Intent intent = getIntent();
        if (intent == null) return;

        String action = intent.getAction();
        String type = intent.getType();
        boolean dataChanged = false;

        // Handle text or explicit image_url
        if (intent.hasExtra("image_url")) {
            String imageUrl = intent.getStringExtra("image_url");
            if (imageUrl != null) {
                Uri uri = Uri.parse(imageUrl);
                if (!selectedMediaUris.contains(uri)) {
                    selectedMediaUris.add(uri);
                    dataChanged = true;
                }
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
                    if (!selectedMediaUris.contains(sharedUri)) {
                        selectedMediaUris.add(sharedUri);
                        dataChanged = true;
                    }
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
                    for (Uri uri : sharedUris) {
                        if (!selectedMediaUris.contains(uri)) {
                            selectedMediaUris.add(uri);
                            dataChanged = true;
                        }
                    }
                }
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null && etPostContent.length() == 0) {
                    etPostContent.setText(sharedText);
                }
            }
        }

        if (dataChanged) {
            mediaAdapter.notifyDataSetChanged();
            updateMediaVisibility();
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

    /**
     * Shows a confirmation dialog when the user attempts to exit with unsaved changes.
     */
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


    /**
     * Compiles the post content and launches the system share chooser.
     */
    private void handleManualShare() {
        String content = etPostContent.getText().toString().trim();

        if (content.isEmpty() && selectedMediaUris.isEmpty()) {
            Toast.makeText(this, "Please add some content or media", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Uri> localUris = new ArrayList<>();
        StringBuilder textWithLinks = new StringBuilder(content);

        for (Uri uri : selectedMediaUris) {
            if (uri == null) continue;
            
            if (isRemoteUri(uri)) {
                if (textWithLinks.length() > 0) textWithLinks.append("\n\n");
                textWithLinks.append(uri);
            } else if (isLocalUriAccessible(uri)) {
                localUris.add(uri);
            }
        }

        String finalContent = textWithLinks.toString();
        Intent shareIntent = new Intent();
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (localUris.isEmpty()) {
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, finalContent);
        } else {
            if (localUris.size() == 1) {
                shareIntent.setAction(Intent.ACTION_SEND);
                Uri uri = localUris.get(0);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.setType(getMimeType(uri));
            } else {
                shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, localUris);
                shareIntent.setType(getCombinedMimeType(localUris));
            }

            if (!finalContent.isEmpty()) {
                shareIntent.putExtra(Intent.EXTRA_TEXT, finalContent);
            }

            // Grant permissions via ClipData for Android 10+
            ClipData clipData = ClipData.newRawUri("Post Media", localUris.get(0));
            for (int k = 1; k < localUris.size(); k++) {
                clipData.addItem(new ClipData.Item(localUris.get(k)));
            }
            shareIntent.setClipData(clipData);
        }

        if (!finalContent.isEmpty()) {
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Shared via Social One");
        }

        try {
            Intent chooser = Intent.createChooser(shareIntent, "Share Post");
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(chooser);
        } catch (Exception e) {
            Log.e("PostComposer", "Final share failure", e);
            Toast.makeText(this, "Could not open sharing apps", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Checks if a URI points to a remote resource (http/https).
     * @param uri The URI to check.
     * @return True if remote.
     */
    private boolean isRemoteUri(Uri uri) {
        String scheme = uri.getScheme();
        return scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
    }

    /**
     * Checks if a local URI is currently accessible.
     * @param uri The URI to check.
     * @return True if accessible.
     */
    private boolean isLocalUriAccessible(Uri uri) {
        if (uri == null) return false;
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("content") && !scheme.equalsIgnoreCase("file"))) {
            return false;
        }

        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            return cursor != null;
        } catch (Exception e) {
            Log.w("PostComposer", "URI no longer accessible: " + uri);
        }
        return false;
    }

    /**
     * Determines a common MIME type for a list of URIs.
     * @param uris The list of URIs.
     * @return A MIME type string (e.g., "image/*").
     */
    private String getCombinedMimeType(List<Uri> uris) {
        if (uris == null || uris.isEmpty()) return "*/*";
        String firstType = getMimeType(uris.get(0));
        String baseType = firstType.split("/")[0];

        for (int i = 1; i < uris.size(); i++) {
            String currentType = getMimeType(uris.get(i));
            if (!currentType.startsWith(baseType + "/")) {
                return "*/*"; // Mixed media types (e.g. image + video)
            }
        }
        return baseType + "/*"; // Uniform media types (e.g. all images)
    }

    /**
     * Saves the current text and media as a draft in internal storage and Firestore.
     */
    private void saveCurrentAsDraft() {
        String content = etPostContent.getText().toString().trim();
        if (content.isEmpty() && selectedMediaUris.isEmpty()) {
            Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show();
            return;
        }

        if (drafts.size() >= MAX_DRAFTS) {
            Toast.makeText(this, R.string.max_drafts_limit, Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading("Saving draft...");

        new Thread(() -> {
            try {
                JSONObject draft = new JSONObject();
                draft.put("text", content);
                JSONArray mediaArray = new JSONArray();

                File mediaDir = new File(getFilesDir(), DRAFTS_MEDIA_DIR);
                if (!mediaDir.exists()) {
                    boolean created = mediaDir.mkdirs();
                    if (!created && !mediaDir.exists()) {
                        Log.e("PostComposer", "Failed to create media directory");
                    }
                }

                for (Uri uri : selectedMediaUris) {
                    if (isRemoteUri(uri)) {
                        mediaArray.put(uri.toString());
                    } else {
                        // Copy local media to internal storage to ensure persistence
                        String fileName = "draft_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
                        String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
                        if (extension == null || extension.isEmpty()) {
                            String type = getContentResolver().getType(uri);
                            if (type != null) extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
                        }
                        if (extension != null && !extension.isEmpty()) fileName += "." + extension;

                        File destFile = new File(mediaDir, fileName);
                        if (copyUriToFile(uri, destFile)) {
                            mediaArray.put(fileName);
                        }
                    }
                }
                draft.put("media", mediaArray);
                draft.put("timestamp", System.currentTimeMillis());

                runOnUiThread(() -> {
                    try {
                        drafts.add(0, draft);
                        saveDraftsToPrefs();
                        draftAdapter.notifyItemInserted(0);
                        rvDrafts.scrollToPosition(0);
                        updateDraftsVisibility();
                        hideLoading();
                        Toast.makeText(this, "Draft saved with media", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        hideLoading();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(this, "Error saving draft", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * Copies a file from a URI to a destination File.
     * @param uri The source URI.
     * @param destFile The destination File.
     * @return True if successful.
     */
    private boolean copyUriToFile(Uri uri, File destFile) {
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(destFile)) {
            if (in == null) return false;
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            return true;
        } catch (Exception e) {
            Log.e("PostComposer", "Error copying media to internal storage", e);
            return false;
        }
    }

    /**
     * Persists the current drafts list to SharedPreferences.
     */
    private void saveDraftsToPrefs() {
        JSONArray array = new JSONArray(drafts);
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_DRAFTS, array.toString())
                .apply();

        if (firestoreManager != null) {
            List<String> texts = new ArrayList<>();
            for (JSONObject d : drafts) {
                String t = d.optString("text");
                if (t != null && !t.isEmpty()) {
                    texts.add(t);
                }
            }
            firestoreManager.savePostDrafts(texts);
        }
    }

    private void loadDrafts() {
        String json = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_DRAFTS, null);
        if (json != null && !json.equals("[]")) {
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
        } else if (firestoreManager != null) {
            // Try to pull from cloud if local is empty
            firestoreManager.loadUserData(new FirestoreManager.OnDataLoadedListener() {
                @Override public void onDataLoaded(List<String> t, List<String> b, List<String> h) {}
                @Override
                public void onPostDraftsLoaded(List<String> cloudDrafts) {
                    if (cloudDrafts != null && !cloudDrafts.isEmpty()) {
                        drafts.clear();
                        for (String text : cloudDrafts) {
                            try {
                                JSONObject d = new JSONObject();
                                d.put("text", text);
                                d.put("media", new JSONArray());
                                d.put("timestamp", System.currentTimeMillis());
                                drafts.add(d);
                            } catch (JSONException e) { e.printStackTrace(); }
                        }
                        runOnUiThread(() -> {
                            draftAdapter.notifyDataSetChanged();
                            updateDraftsVisibility();
                        });
                    }
                }
                @Override public void onError(Exception e) { Log.e("PostComposer", "Cloud draft load error", e); }
            });
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
        showLoading("Loading draft...");
        new Thread(() -> {
            try {
                String text = draft.optString("text", "");
                List<Uri> newUris = new ArrayList<>();
                JSONArray mediaArray = draft.optJSONArray("media");
                if (mediaArray != null) {
                    for (int i = 0; i < mediaArray.length(); i++) {
                        String item = mediaArray.getString(i);
                        if (item.startsWith("http")) {
                            newUris.add(Uri.parse(item));
                        } else {
                            // Load from internal storage
                            File mediaFile = new File(new File(getFilesDir(), DRAFTS_MEDIA_DIR), item);
                            if (mediaFile.exists()) {
                                Uri internalUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", mediaFile);
                                newUris.add(internalUri);
                            }
                        }
                    }
                }

                runOnUiThread(() -> {
                    etPostContent.setText(text);
                    selectedMediaUris.clear();
                    selectedMediaUris.addAll(newUris);
                    mediaAdapter.notifyDataSetChanged();
                    updateMediaVisibility();
                    hideLoading();
                    Toast.makeText(this, "Draft loaded", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(this, "Error loading draft", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showLoading(String message) {
        if (loadingOverlay != null) {
            if (tvLoadingMessage != null) tvLoadingMessage.setText(message);
            loadingOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.GONE);
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
        if (uri == null) return "*/*";
        String type = null;
        try {
            type = getContentResolver().getType(uri);
        } catch (Exception e) {
            Log.w("PostComposer", "Could not get MIME type from resolver: " + uri);
        }
        
        if (type == null) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            if (extension != null) {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            }
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

            holder.imagesContainer.removeAllViews();
            if (mediaCount > 0) {
                holder.imagesScroll.setVisibility(View.VISIBLE);
                for (int i = 0; i < Math.min(mediaCount, 5); i++) {
                    try {
                        String item = media.getString(i);
                        Object loadSource;
                        if (item.startsWith("http")) {
                            loadSource = item;
                        } else {
                            loadSource = new File(new File(holder.itemView.getContext().getFilesDir(), DRAFTS_MEDIA_DIR), item);
                        }
                        
                        ImageView iv = new ImageView(holder.itemView.getContext());
                        iv.setClickable(false);
                        iv.setFocusable(false);
                        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(dpToPx(40, holder.itemView), dpToPx(40, holder.itemView));
                        lp.setMargins(0, 0, dpToPx(4, holder.itemView), 0);
                        iv.setLayoutParams(lp);
                        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        
                        // Load image with rounded corners
                        com.bumptech.glide.Glide.with(holder.itemView.getContext())
                            .load(loadSource)
                            .transform(new com.bumptech.glide.load.resource.bitmap.CenterCrop(), 
                                       new com.bumptech.glide.load.resource.bitmap.RoundedCorners(dpToPx(4, holder.itemView)))
                            .into(iv);
                        
                        holder.imagesContainer.addView(iv);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                holder.imagesScroll.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> useDraft(draft));
            setClickAnimations(holder.itemView, holder.btnDelete);

            holder.btnDelete.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    showDeleteDraftConfirmDialog(pos);
                }
            });
        }

        private int dpToPx(int dp, View itemView) {
            float density = itemView.getContext().getResources().getDisplayMetrics().density;
            return Math.round((float) dp * density);
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvContent, tvMediaCount;
            ImageView ivMediaIcon;
            ImageButton btnDelete;
            android.widget.LinearLayout imagesContainer;
            android.view.View imagesScroll;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvContent = itemView.findViewById(R.id.tvDraftContent);
                tvMediaCount = itemView.findViewById(R.id.tvMediaCount);
                ivMediaIcon = itemView.findViewById(R.id.ivMediaIcon);
                btnDelete = itemView.findViewById(R.id.btnDeleteDraft);
                imagesContainer = itemView.findViewById(R.id.draftImagesContainer);
                imagesScroll = itemView.findViewById(R.id.draftImagesScroll);
            }
        }
    }

    private void showDeleteDraftConfirmDialog(int position) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_caution, null);
        TextView dTitle = dialogView.findViewById(R.id.confirmTitle);
        TextView dMessage = dialogView.findViewById(R.id.confirmMessage);
        Button dBtnConfirm = dialogView.findViewById(R.id.btnProceedConfirm);
        Button dBtnCancel = dialogView.findViewById(R.id.btnCancelConfirm);

        dTitle.setText(R.string.delete_draft_title);
        dMessage.setText(R.string.delete_draft_message);
        dBtnConfirm.setText(R.string.delete);
        dBtnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF5252")));

        setClickAnimations(dBtnConfirm, dBtnCancel);

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        builder.setView(dialogView);
        AlertDialog deleteDialog = builder.create();

        if (deleteDialog.getWindow() != null) {
            deleteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dBtnConfirm.setOnClickListener(v -> {
            deleteDialog.dismiss();
            JSONObject draft = drafts.get(position);
            JSONArray media = draft.optJSONArray("media");
            if (media != null) {
                File mediaDir = new File(getFilesDir(), DRAFTS_MEDIA_DIR);
                for (int i = 0; i < media.length(); i++) {
                    try {
                        String item = media.getString(i);
                        if (!item.startsWith("http")) {
                            File file = new File(mediaDir, item);
                            if (file.exists()) {
                                boolean deleted = file.delete();
                                if (!deleted) Log.w("PostComposer", "Could not delete draft media: " + item);
                            }
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                }
            }

            drafts.remove(position);
            saveDraftsToPrefs();
            draftAdapter.notifyItemRemoved(position);
            updateDraftsVisibility();
            Toast.makeText(this, R.string.draft_deleted, Toast.LENGTH_SHORT).show();
        });

        dBtnCancel.setOnClickListener(v -> deleteDialog.dismiss());
        deleteDialog.show();
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
