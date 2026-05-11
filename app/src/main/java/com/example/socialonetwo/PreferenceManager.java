package com.example.socialonetwo;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PreferenceManager {

    private static final String PREFS_NAME = "WebWrapperPrefs";
    private static final String SITES_KEY = "SavedSitesList";
    private static final String HISTORY_KEY = "BrowsingHistoryList";
    private static final String BOOKMARKS_KEY = "SavedBookmarksList";
    private static final String LOCKED_SITES_KEY = "LockedSites";
    private static final String WORKSPACE_TABS_KEY = "ActiveWorkspaceTabs";
    private static final String WORKSPACES_LIST_KEY = "WorkspacesList";

    private final SharedPreferences sharedPreferences;
    private final FirestoreManager firestoreManager;

    public PreferenceManager(Context context, FirestoreManager firestoreManager) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.firestoreManager = firestoreManager;
    }

    public List<String> loadSites(String homeUrl, String quickAccessUrl) {
        String json = sharedPreferences.getString(SITES_KEY, null);
        List<String> siteList = new ArrayList<>();
        if (json != null) {
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    String url = array.getString(i);
                    if ("home://business".equals(url)) {
                        siteList.add(quickAccessUrl);
                    } else {
                        siteList.add(url);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return siteList;
    }

    public void saveSites(List<String> siteList, Set<String> incognitoTabs, Set<String> workspaceTabs, String homeUrl, String quickAccessUrl, String incognitoPrefix) {
        JSONArray array = new JSONArray();
        List<String> filteredList = new ArrayList<>();

        if (siteList != null) {
            for (String url : siteList) {
                // Filter out Home and Incognito tabs as they are handled/discarded on boot
                if (url.equals(homeUrl) || url.startsWith(incognitoPrefix) || incognitoTabs.contains(url)) {
                    continue;
                }
                array.put(url);
                filteredList.add(url);
            }
        }

        sharedPreferences.edit()
                .putString(SITES_KEY, array.toString())
                .putStringSet(WORKSPACE_TABS_KEY, workspaceTabs)
                .apply();
        
        if (firestoreManager != null) {
            firestoreManager.saveTabs(filteredList);
        }
    }

    public List<String> loadHistory() {
        String json = sharedPreferences.getString(HISTORY_KEY, null);
        List<String> historyList = new ArrayList<>();
        if (json != null) {
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    historyList.add(array.getString(i));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return historyList;
    }

    public void saveHistory(List<String> historyList) {
        JSONArray array = new JSONArray();
        if (historyList != null) {
            for (String url : historyList) {
                array.put(url);
            }
        }
        sharedPreferences.edit().putString(HISTORY_KEY, array.toString()).apply();
        if (firestoreManager != null) {
            firestoreManager.saveHistory(historyList);
        }
    }

    public List<String> loadBookmarks() {
        String json = sharedPreferences.getString(BOOKMARKS_KEY, null);
        List<String> bookmarksList = new ArrayList<>();
        if (json != null) {
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    bookmarksList.add(array.getString(i));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return bookmarksList;
    }

    public void saveBookmarks(List<String> bookmarksList) {
        JSONArray array = new JSONArray();
        if (bookmarksList != null) {
            for (String url : bookmarksList) {
                array.put(url);
            }
        }
        sharedPreferences.edit().putString(BOOKMARKS_KEY, array.toString()).apply();
        if (firestoreManager != null) {
            firestoreManager.saveBookmarks(bookmarksList);
        }
    }

    public Set<String> loadWorkspaceTabs() {
        return sharedPreferences.getStringSet(WORKSPACE_TABS_KEY, new HashSet<>());
    }

    public Set<String> loadLockedSites() {
        return sharedPreferences.getStringSet(LOCKED_SITES_KEY, new HashSet<>());
    }

    public String loadWorkspacesJson() {
        return sharedPreferences.getString(WORKSPACES_LIST_KEY, "[]");
    }

    public List<Map<String, Object>> loadWorkspacesList() {
        String json = loadWorkspacesJson();
        List<Map<String, Object>> workspaces = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Map<String, Object> map = new HashMap<>();
                map.put("name", obj.optString("name", "Unnamed"));
                JSONArray tabsArray = obj.optJSONArray("tabs");
                List<String> tabs = new ArrayList<>();
                if (tabsArray != null) {
                    for (int j = 0; j < tabsArray.length(); j++) {
                        tabs.add(tabsArray.getString(j));
                    }
                }
                map.put("tabs", tabs);
                workspaces.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return workspaces;
    }

    public void saveWorkspacesJson(String json) {
        sharedPreferences.edit().putString(WORKSPACES_LIST_KEY, json).apply();
        if (firestoreManager != null) {
            firestoreManager.saveWorkspaces(loadWorkspacesList());
        }
    }

    public void saveLockedSites(List<String> siteList, SitesAdapter sitesAdapter) {
        Set<String> locked = new HashSet<>();
        if (siteList != null && sitesAdapter != null) {
            for (String url : siteList) {
                if (sitesAdapter.isLocked(url)) {
                    locked.add(url);
                }
            }
        }
        sharedPreferences.edit().putStringSet(LOCKED_SITES_KEY, locked).apply();
    }

    public void clearLocalData() {
        sharedPreferences.edit().clear().apply();
    }
}
