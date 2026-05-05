package com.example.socialonetwo;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PreferenceManager {

    private static final String PREFS_NAME = "WebWrapperPrefs";
    private static final String SITES_KEY = "SavedSites";
    private static final String HISTORY_KEY = "BrowsingHistory";
    private static final String BOOKMARKS_KEY = "SavedBookmarks";
    private static final String LOCKED_SITES_KEY = "LockedSites";

    private final SharedPreferences sharedPreferences;
    private final FirestoreManager firestoreManager;

    public PreferenceManager(Context context, FirestoreManager firestoreManager) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.firestoreManager = firestoreManager;
    }

    public List<String> loadSites(String homeUrl, String quickAccessUrl) {
        Set<String> set = sharedPreferences.getStringSet(SITES_KEY, null);
        List<String> siteList = new ArrayList<>();
        if (set != null && !set.isEmpty()) {
            for (String url : set) {
                if ("home://business".equals(url)) {
                    siteList.add(quickAccessUrl);
                } else {
                    siteList.add(url);
                }
            }
        }
        return siteList;
    }

    public void saveSites(List<String> siteList, Set<String> incognitoTabs, String homeUrl, String quickAccessUrl, String incognitoPrefix) {
        Set<String> set = new HashSet<>();
        List<String> filteredList = new ArrayList<>();

        for (String url : siteList) {
            if (url.equals(homeUrl) || url.equals(quickAccessUrl) || url.startsWith(incognitoPrefix) || incognitoTabs.contains(url)) {
                continue;
            }
            set.add(url);
            filteredList.add(url);
        }

        sharedPreferences.edit().putStringSet(SITES_KEY, set).apply();
        if (firestoreManager != null) {
            firestoreManager.saveTabs(filteredList);
        }
    }

    public List<String> loadHistory() {
        Set<String> set = sharedPreferences.getStringSet(HISTORY_KEY, new HashSet<>());
        return new ArrayList<>(set);
    }

    public void saveHistory(List<String> historyList) {
        Set<String> historySet = new HashSet<>(historyList);
        sharedPreferences.edit().putStringSet(HISTORY_KEY, historySet).apply();
        if (firestoreManager != null) {
            firestoreManager.saveHistory(historyList);
        }
    }

    public List<String> loadBookmarks() {
        Set<String> set = sharedPreferences.getStringSet(BOOKMARKS_KEY, new HashSet<>());
        return new ArrayList<>(set);
    }

    public void saveBookmarks(List<String> bookmarksList) {
        Set<String> bookmarkSet = new HashSet<>(bookmarksList);
        sharedPreferences.edit().putStringSet(BOOKMARKS_KEY, bookmarkSet).apply();
        if (firestoreManager != null) {
            firestoreManager.saveBookmarks(bookmarksList);
        }
    }

    public Set<String> loadLockedSites() {
        return sharedPreferences.getStringSet(LOCKED_SITES_KEY, new HashSet<>());
    }

    public void saveLockedSites(List<String> siteList, SitesAdapter sitesAdapter) {
        Set<String> locked = new HashSet<>();
        for (String url : siteList) {
            if (sitesAdapter.isLocked(url)) {
                locked.add(url);
            }
        }
        sharedPreferences.edit().putStringSet(LOCKED_SITES_KEY, locked).apply();
    }

    public void clearLocalData() {
        sharedPreferences.edit().clear().apply();
    }
}
