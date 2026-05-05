package com.example.socialonetwo;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchHandler {

    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final String PREDICTIONS_KEY = "SearchPredictionsEnabled";
    private static final String SEARCH_ENGINE_KEY = "DefaultSearchEngine";

    public SearchHandler(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences("WebWrapperPrefs", Context.MODE_PRIVATE);
    }

    public void setupAutocomplete(AutoCompleteTextView textView, final Runnable onAction) {
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.item_suggestion, android.R.id.text1, new ArrayList<String>());
        textView.setAdapter(adapter);
        textView.setDropDownBackgroundResource(R.drawable.bg_popup_rounded);

        textView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (sharedPreferences.getBoolean(PREDICTIONS_KEY, true) && s.length() > 0) {
                    fetchSuggestions(s.toString(), adapter);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        textView.setOnItemClickListener((parent, view, position, id) -> {
            String suggestion = adapter.getItem(position);
            textView.setText(suggestion);
            if (onAction != null) onAction.run();
        });
    }

    private void fetchSuggestions(final String query, final ArrayAdapter<String> adapter) {
        executorService.execute(() -> {
            try {
                String urlString = "https://suggestqueries.google.com/complete/search?client=firefox&q=" + Uri.encode(query);
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONArray jsonResponse = new JSONArray(response.toString());
                JSONArray suggestionsJson = jsonResponse.getJSONArray(1);
                final List<String> suggestions = new ArrayList<>();
                for (int i = 0; i < suggestionsJson.length(); i++) {
                    suggestions.add(suggestionsJson.getString(i));
                }

                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    adapter.clear();
                    adapter.addAll(suggestions);
                    adapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public String getSearchBaseUrl() {
        int index = sharedPreferences.getInt(SEARCH_ENGINE_KEY, 0);
        return SearchEngineManager.getSearchBaseUrl(index);
    }

    public void performGlobalSearch(String query, Set<String> urls, java.util.Map<String, View> tabMap, java.util.List<String> siteList, WebViewCreator webViewCreator) {
        for (String url : urls) {
            String searchUrl = SearchEngineManager.getFormattedSearchUrl(url, query);
            View tabView = tabMap.get(url);

            if (!(tabView instanceof WebView)) {
                for (String openUrl : siteList) {
                    if (openUrl.equals(url)) {
                        tabView = tabMap.get(openUrl);
                        break;
                    }
                }
            }

            if (!(tabView instanceof WebView)) {
                webViewCreator.createWebView(url);
                tabView = tabMap.get(url);
            }

            if (tabView instanceof WebView) {
                ((WebView) tabView).loadUrl(searchUrl);
            }
        }
        Toast.makeText(context, "Global search success", Toast.LENGTH_SHORT).show();
    }

    public interface WebViewCreator {
        void createWebView(String url);
    }
}
