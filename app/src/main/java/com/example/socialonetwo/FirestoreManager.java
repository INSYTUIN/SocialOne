package com.example.socialonetwo;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for managing Firestore operations related to user data.
 * Optimized for surgical updates and reduced write frequency.
 */
public class FirestoreManager {

    private static final String TAG = "FirestoreManager";
    private static final String COLLECTION_USERS = "users";
    private static final String KEY_TABS = "tabs";
    private static final String KEY_BOOKMARKS = "bookmarks";
    private static final String KEY_HISTORY = "history";
    private static final String KEY_POST_DRAFTS = "post_drafts";
    private static final String KEY_WORKSPACES = "workspaces";

    // Balancing user satisfaction (power use) vs data efficiency (document size)
    private static final int MAX_TABS = 30;
    private static final int MAX_BOOKMARKS = 100;
    private static final int MAX_HISTORY = 50;
    private static final int MAX_DRAFTS = 15;
    private static final int MAX_WORKSPACES = 10;

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    
    // Simple debouncing for history to save on write costs
    private long lastHistorySyncTime = 0;
    private static final long SYNC_THRESHOLD = TimeUnit.MINUTES.toMillis(2);

    public interface OnDataLoadedListener {
        void onDataLoaded(List<String> tabs, List<String> bookmarks, List<String> history);
        default void onPostDraftsLoaded(List<String> drafts) {}
        default void onWorkspacesLoaded(List<Map<String, Object>> workspaces) {}
        void onError(Exception e);
    }

    public FirestoreManager() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * Surgically adds a tab to the cloud.
     */
    public void addTab(String url) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        db.collection(COLLECTION_USERS).document(user.getUid())
                .update(KEY_TABS, FieldValue.arrayUnion(url))
                .addOnFailureListener(e -> saveTabs(java.util.Collections.singletonList(url))); // Fallback to set if doc doesn't exist
    }

    /**
     * Surgically removes a tab from the cloud.
     */
    public void removeTab(String url) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        db.collection(COLLECTION_USERS).document(user.getUid())
                .update(KEY_TABS, FieldValue.arrayRemove(url));
    }

    /**
     * Saves the current list of tabs to Firestore for the authenticated user.
     */
    public void saveTabs(List<String> tabs) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || tabs == null) return;

        List<String> limitedTabs = tabs;
        if (tabs.size() > MAX_TABS) {
            // Taking the first 30 tabs (most relevant/recent in this app's tab switcher)
            limitedTabs = tabs.subList(0, MAX_TABS);
        }

        Map<String, Object> data = new HashMap<>();
        data.put(KEY_TABS, limitedTabs);

        db.collection(COLLECTION_USERS).document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Tabs successfully synced to cloud"))
                .addOnFailureListener(e -> Log.e(TAG, "Error syncing tabs", e));
    }

    /**
     * Saves the current list of bookmarks to Firestore for the authenticated user.
     */
    public void saveBookmarks(List<String> bookmarks) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || bookmarks == null) return;

        List<String> limitedBookmarks = bookmarks;
        if (bookmarks.size() > MAX_BOOKMARKS) {
            // Newest bookmarks are added to the front (index 0) in this app
            limitedBookmarks = bookmarks.subList(0, MAX_BOOKMARKS);
        }

        Map<String, Object> data = new HashMap<>();
        data.put(KEY_BOOKMARKS, limitedBookmarks);

        db.collection(COLLECTION_USERS).document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Bookmarks successfully synced to cloud"))
                .addOnFailureListener(e -> Log.e(TAG, "Error syncing bookmarks", e));
    }

    /**
     * Saves the current list of history to Firestore for the authenticated user.
     * Limits history to the last 50 items and uses debouncing to reduce write frequency.
     */
    public void saveHistory(List<String> history) {
        saveHistory(history, false);
    }

    /**
     * Saves the current list of history to Firestore for the authenticated user.
     * Limits history to the last 50 items and uses debouncing to reduce write frequency.
     * @param history The history list to save.
     * @param force If true, bypasses the debounce timer (used for explicit deletions).
     */
    public void saveHistory(List<String> history, boolean force) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || history == null) return;

        long currentTime = System.currentTimeMillis();
        // Only sync if significant changes occurred, enough time has passed (2 mins), or force is requested
        if (!force && !history.isEmpty() && currentTime - lastHistorySyncTime < SYNC_THRESHOLD && history.size() < MAX_HISTORY) {
            return;
        }
        lastHistorySyncTime = currentTime;

        List<String> limitedHistory = history;
        if (history.size() > MAX_HISTORY) {
            // Newest history items are added to the front (index 0) in this app
            limitedHistory = history.subList(0, MAX_HISTORY);
        }

        Map<String, Object> data = new HashMap<>();
        data.put(KEY_HISTORY, limitedHistory);

        db.collection(COLLECTION_USERS).document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "History successfully synced to cloud"))
                .addOnFailureListener(e -> Log.e(TAG, "Error syncing history", e));
    }

    /**
     * Saves the current list of post drafts to Firestore for the authenticated user.
     * Only saves the text content, as requested.
     */
    public void savePostDrafts(List<String> draftTexts) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || draftTexts == null) return;

        List<String> limitedDrafts = draftTexts;
        if (draftTexts.size() > MAX_DRAFTS) {
            // Newest drafts are added to the front (index 0) in this app
            limitedDrafts = draftTexts.subList(0, MAX_DRAFTS);
        }

        Map<String, Object> data = new HashMap<>();
        data.put(KEY_POST_DRAFTS, limitedDrafts);

        db.collection(COLLECTION_USERS).document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Post drafts successfully synced to cloud"))
                .addOnFailureListener(e -> Log.e(TAG, "Error syncing post drafts", e));
    }

    /**
     * Saves the list of workspaces to Firestore.
     */
    public void saveWorkspaces(List<Map<String, Object>> workspaces) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || workspaces == null) return;

        List<Map<String, Object>> limitedWorkspaces = workspaces;
        if (workspaces.size() > MAX_WORKSPACES) {
            limitedWorkspaces = workspaces.subList(0, MAX_WORKSPACES);
        }

        Map<String, Object> data = new HashMap<>();
        data.put(KEY_WORKSPACES, limitedWorkspaces);

        db.collection(COLLECTION_USERS).document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Workspaces successfully synced to cloud"))
                .addOnFailureListener(e -> Log.e(TAG, "Error syncing workspaces", e));
    }

    /**
     * Loads user data from Firestore and triggers the callback with results.
     */
    @SuppressWarnings("unchecked")
    public void loadUserData(OnDataLoadedListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onError(new Exception("User not authenticated"));
            return;
        }

        db.collection(COLLECTION_USERS).document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> tabs = (List<String>) documentSnapshot.get(KEY_TABS);
                        List<String> bookmarks = (List<String>) documentSnapshot.get(KEY_BOOKMARKS);
                        List<String> history = (List<String>) documentSnapshot.get(KEY_HISTORY);
                        List<String> postDrafts = (List<String>) documentSnapshot.get(KEY_POST_DRAFTS);
                        List<Map<String, Object>> workspaces = (List<Map<String, Object>>) documentSnapshot.get(KEY_WORKSPACES);

                        listener.onDataLoaded(tabs, bookmarks, history);
                        if (postDrafts != null) {
                            listener.onPostDraftsLoaded(postDrafts);
                        }
                        if (workspaces != null) {
                            listener.onWorkspacesLoaded(workspaces);
                        }
                    } else {
                        listener.onDataLoaded(null, null, null);
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    /**
     * Migrates initial local data to Firestore if the cloud is empty.
     */
    public void performInitialMigration(List<String> localTabs, List<String> localBookmarks, List<String> localHistory, List<Map<String, Object>> localWorkspaces) {
        loadUserData(new OnDataLoadedListener() {
            @Override
            public void onDataLoaded(List<String> remoteTabs, List<String> remoteBookmarks, List<String> remoteHistory) {
                // If cloud is empty, upload local data
                if (remoteTabs == null || remoteTabs.isEmpty()) {
                    saveTabs(localTabs);
                }
                if (remoteBookmarks == null || remoteBookmarks.isEmpty()) {
                    saveBookmarks(localBookmarks);
                }
                if (remoteHistory == null || remoteHistory.isEmpty()) {
                    saveHistory(localHistory);
                }
            }

            @Override
            public void onWorkspacesLoaded(List<Map<String, Object>> remoteWorkspaces) {
                if (remoteWorkspaces == null || remoteWorkspaces.isEmpty()) {
                    saveWorkspaces(localWorkspaces);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to check cloud data for migration", e);
            }
        });
    }
}
