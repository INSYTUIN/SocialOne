package com.example.socialonetwo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "WebWrapperPrefs";
    private static final String SITES_KEY = "SavedSites";
    private static final String HISTORY_KEY = "BrowsingHistory";
    private static final String BOOKMARKS_KEY = "SavedBookmarks";
    private static final String PREDICTIONS_KEY = "SearchPredictionsEnabled";
    private static final String SEARCH_ENGINE_KEY = "DefaultSearchEngine";
    private static final String ADVANCED_ANIM_KEY = "AdvancedAnimationsEnabled";
    private static final String AD_BLOCKER_KEY = "AdBlockerEnabled";

    private static final String DRAFTS_PREFS_NAME = "PostDraftsPrefs";
    private static final String KEY_DRAFTS = "saved_drafts";

    private static final int[] SEARCH_ENGINE_ICONS = {
            R.drawable.google,
            R.drawable.bing,
            R.drawable.brave,
            R.drawable.duckduckgo,
            R.drawable.startpage,
            R.drawable.ecosia
    };

    private MaterialSwitch switchPredictions;
    private AlertDialog searchEngineDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        View rootView = findViewById(R.id.settingsRoot);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        ImageButton btnBack = findViewById(R.id.btnBack);
        Button btnChangeSearchEngine = findViewById(R.id.btnChangeSearchEngine);
        switchPredictions = findViewById(R.id.switchPredictions);
        MaterialSwitch switchAdvancedAnim = findViewById(R.id.switchAdvancedAnim);

        setClickAnimation(btnBack);
        setClickAnimation(btnChangeSearchEngine);

        btnBack.setOnClickListener(v -> finish());

        boolean predictionsEnabled = sharedPreferences.getBoolean(PREDICTIONS_KEY, true);
        switchPredictions.setChecked(predictionsEnabled);

        switchPredictions.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(PREDICTIONS_KEY, isChecked).apply();
        });

        MaterialSwitch switchAdBlocker = findViewById(R.id.switchAdBlocker);
        boolean adBlockerEnabled = sharedPreferences.getBoolean(AD_BLOCKER_KEY, false);
        switchAdBlocker.setChecked(adBlockerEnabled);
        switchAdBlocker.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(AD_BLOCKER_KEY, isChecked).apply();
        });

        boolean advancedAnimEnabled = sharedPreferences.getBoolean(ADVANCED_ANIM_KEY, false);
        switchAdvancedAnim.setChecked(advancedAnimEnabled);

        switchAdvancedAnim.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(ADVANCED_ANIM_KEY, isChecked).apply();
            showRestartDialog();
        });

        btnChangeSearchEngine.setOnClickListener(v -> showSearchEngineDialog());

        MaterialButton btnQuickSync = findViewById(R.id.btnQuickSync);
        setClickAnimation(btnQuickSync);
        btnQuickSync.setOnClickListener(v -> performQuickSync());

        updateAccountUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAccountUI();
    }

    private void updateAccountUI() {
        TextView tvUserAccount = findViewById(R.id.tvUserAccount);
        MaterialButton btnManageAccount = findViewById(R.id.btnManageGoogleAccount);
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String accountInfo = user.getDisplayName();
            if (accountInfo == null || accountInfo.isEmpty()) {
                accountInfo = user.getEmail();
            } else if (user.getEmail() != null) {
                accountInfo += " (" + user.getEmail() + ")";
            }
            tvUserAccount.setText(accountInfo);
            btnManageAccount.setVisibility(View.VISIBLE);
        } else {
            tvUserAccount.setText(R.string.not_signed_in);
            btnManageAccount.setVisibility(View.GONE);
        }

        setClickAnimation(btnManageAccount);
        btnManageAccount.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, 
                    android.net.Uri.parse("https://myaccount.google.com/"));
            startActivity(intent);
        });
    }

    private void performQuickSync() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in to use Cloud Sync", Toast.LENGTH_SHORT).show();
            return;
        }

        FirestoreManager firestoreManager = new FirestoreManager();
        Toast.makeText(this, "Syncing...", Toast.LENGTH_SHORT).show();

        // Download first to merge, then upload result to ensure data integrity
        firestoreManager.loadUserData(new FirestoreManager.OnDataLoadedListener() {
            @Override
            public void onDataLoaded(List<String> tabs, List<String> bookmarks, List<String> history) {
                // 1. Merge Remote into Local
                if (tabs != null) mergeSet(SITES_KEY, tabs);
                if (bookmarks != null) mergeSet(BOOKMARKS_KEY, bookmarks);
                if (history != null) mergeSet(HISTORY_KEY, history);

                // 2. Upload Merged result back to Cloud
                Set<String> mergedSites = sharedPreferences.getStringSet(SITES_KEY, new HashSet<>());
                Set<String> mergedBookmarks = sharedPreferences.getStringSet(BOOKMARKS_KEY, new HashSet<>());
                Set<String> mergedHistory = sharedPreferences.getStringSet(HISTORY_KEY, new HashSet<>());

                firestoreManager.saveTabs(new ArrayList<>(mergedSites));
                firestoreManager.saveBookmarks(new ArrayList<>(mergedBookmarks));
                firestoreManager.saveHistory(new ArrayList<>(mergedHistory));
                
                runOnUiThread(() -> Toast.makeText(SettingsActivity.this, "Browser data synced", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onPostDraftsLoaded(List<String> cloudDrafts) {
                if (cloudDrafts != null) {
                    // 1. Merge Remote Drafts into Local
                    mergeDrafts(cloudDrafts);

                    // 2. Upload Merged Drafts back to Cloud
                    SharedPreferences draftPrefs = getSharedPreferences(DRAFTS_PREFS_NAME, MODE_PRIVATE);
                    String draftsJson = draftPrefs.getString(KEY_DRAFTS, "[]");
                    try {
                        JSONArray array = new JSONArray(draftsJson);
                        List<String> draftTexts = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject d = array.getJSONObject(i);
                            String t = d.optString("text");
                            if (t != null && !t.isEmpty()) {
                                draftTexts.add(t);
                            }
                        }
                        firestoreManager.savePostDrafts(draftTexts);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(SettingsActivity.this, "Sync failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void mergeSet(String key, List<String> remoteData) {
        Set<String> local = new HashSet<>(sharedPreferences.getStringSet(key, new HashSet<>()));
        local.addAll(remoteData);
        sharedPreferences.edit().putStringSet(key, local).apply();
    }

    private void mergeDrafts(List<String> cloudDrafts) {
        SharedPreferences draftPrefs = getSharedPreferences(DRAFTS_PREFS_NAME, MODE_PRIVATE);
        String localJson = draftPrefs.getString(KEY_DRAFTS, "[]");
        try {
            JSONArray localArray = new JSONArray(localJson);
            Set<String> localTexts = new HashSet<>();
            for (int i = 0; i < localArray.length(); i++) {
                localTexts.add(localArray.getJSONObject(i).optString("text", ""));
            }

            boolean changed = false;
            for (String remoteText : cloudDrafts) {
                if (!localTexts.contains(remoteText)) {
                    JSONObject newDraft = new JSONObject();
                    newDraft.put("text", remoteText);
                    newDraft.put("media", new JSONArray());
                    newDraft.put("timestamp", System.currentTimeMillis());
                    localArray.put(newDraft);
                    changed = true;
                }
            }

            if (changed) {
                draftPrefs.edit().putString(KEY_DRAFTS, localArray.toString()).apply();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void showRestartDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Restart Required")
                .setMessage(R.string.restart_app_message)
                .setPositiveButton("Restart Now", (dialog, which) -> {
                    android.content.Intent i = getBaseContext().getPackageManager()
                            .getLaunchIntentForPackage(getBaseContext().getPackageName());
                    if (i != null) {
                        i.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        System.exit(0);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

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

    private void showSearchEngineDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_history, null);
        TextView title = dialogView.findViewById(R.id.historyTitle);
        title.setText(R.string.search_engine_label);

        RecyclerView rv = dialogView.findViewById(R.id.historyRecyclerView);
        Button clearAll = dialogView.findViewById(R.id.btnClearAllHistory);
        ImageButton closeBtn = dialogView.findViewById(R.id.btnCloseHistory);

        clearAll.setVisibility(View.GONE);
        setClickAnimation(closeBtn);

        class SearchEngineAdapter extends RecyclerView.Adapter<SearchEngineAdapter.ViewHolder> {
            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
                return new ViewHolder(v);
            }
            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                holder.name.setText(SearchEngineManager.SEARCH_ENGINE_NAMES[position]);
                int savedIndex = sharedPreferences.getInt(SEARCH_ENGINE_KEY, 0);
                if (position == savedIndex) {
                    holder.name.setTextColor(getResources().getColor(R.color.button_background));
                } else {
                    holder.name.setTextColor(getResources().getColor(R.color.primary_text));
                }

                holder.icon.setImageResource(SEARCH_ENGINE_ICONS[position]);
                holder.icon.setImageTintList(null);

                holder.itemView.setOnClickListener(v -> {
                    sharedPreferences.edit().putInt(SEARCH_ENGINE_KEY, position).apply();
                    searchEngineDialog.dismiss();
                    Toast.makeText(SettingsActivity.this, "Search engine set to " + SearchEngineManager.SEARCH_ENGINE_NAMES[position], Toast.LENGTH_SHORT).show();
                });
            }
            @Override
            public int getItemCount() { return SearchEngineManager.SEARCH_ENGINE_NAMES.length; }
            class ViewHolder extends RecyclerView.ViewHolder {
                TextView name;
                ImageView icon;
                ViewHolder(View v) {
                    super(v);
                    name = v.findViewById(R.id.historyUrl);
                    icon = v.findViewById(R.id.historyIcon);
                }
            }
        }

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new SearchEngineAdapter());

        com.google.android.material.dialog.MaterialAlertDialogBuilder searchEngineDialogBuilder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        searchEngineDialogBuilder.setView(dialogView);
        searchEngineDialog = searchEngineDialogBuilder.create();

        if (searchEngineDialog.getWindow() != null) {
            searchEngineDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        closeBtn.setOnClickListener(v -> searchEngineDialog.dismiss());
        searchEngineDialog.show();
    }
}
