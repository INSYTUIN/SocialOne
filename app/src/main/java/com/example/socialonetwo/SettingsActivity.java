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

import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "WebWrapperPrefs";
    private static final String PREDICTIONS_KEY = "SearchPredictionsEnabled";
    private static final String SEARCH_ENGINE_KEY = "DefaultSearchEngine";
    private static final String ADVANCED_ANIM_KEY = "AdvancedAnimationsEnabled";

    private static final String[] SEARCH_ENGINES = {"Google", "Bing", "Brave", "DuckDuckGo", "Startpage", "Ecosia"};
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

        boolean advancedAnimEnabled = sharedPreferences.getBoolean(ADVANCED_ANIM_KEY, false);
        switchAdvancedAnim.setChecked(advancedAnimEnabled);

        switchAdvancedAnim.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(ADVANCED_ANIM_KEY, isChecked).apply();
            showRestartDialog();
        });

        btnChangeSearchEngine.setOnClickListener(v -> showSearchEngineDialog());
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
                holder.name.setText(SEARCH_ENGINES[position]);
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
                    Toast.makeText(SettingsActivity.this, "Search engine set to " + SEARCH_ENGINES[position], Toast.LENGTH_SHORT).show();
                });
            }
            @Override
            public int getItemCount() { return SEARCH_ENGINES.length; }
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
