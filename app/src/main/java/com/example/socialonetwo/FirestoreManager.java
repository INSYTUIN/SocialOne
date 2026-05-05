package com.example.socialonetwo;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for managing Firestore operations related to user data.
 * Handles syncing of tabs (siteList) and bookmarks to the cloud.
 */
public class FirestoreManager {

    private static final String TAG = "FirestoreManager";
    private static final String COLLECTION_USERS = "users";
    private static final String KEY_TABS = "tabs";
    private static final String KEY_BOOKMARKS = "bookmarks";
    private static final String KEY_HISTORY = "history";
    private static final String KEY_POST_DRAFTS = "post_drafts";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public interface OnDataLoadedListener {
        void onDataLoaded(List<String> tabs, List<String> bookmarks, List<String> history);
        default void onPostDraftsLoaded(List<String> drafts) {}
        void onError(Exception e);
    }

    public FirestoreManager() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * Saves the current list of tabs to Firestore for the authenticated user.
     */
    public void saveTabs(List<String> tabs) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put(KEY_TABS, tabs);

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
        if (user == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put(KEY_BOOKMARKS, bookmarks);

        db.collection(COLLECTION_USERS).document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Bookmarks successfully synced to cloud"))
                .addOnFailureListener(e -> Log.e(TAG, "Error syncing bookmarks", e));
    }

    /**
     * Saves the current list of history to Firestore for the authenticated user.
     * Limits history to the last 50 items to avoid overloading.
     */
    public void saveHistory(List<String> history) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        List<String> limitedHistory = history;
        if (history.size() > 50) {
            limitedHistory = history.subList(0, 50);
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
        if (user == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put(KEY_POST_DRAFTS, draftTexts);

        db.collection(COLLECTION_USERS).document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Post drafts successfully synced to cloud"))
                .addOnFailureListener(e -> Log.e(TAG, "Error syncing post drafts", e));
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
                        
                        listener.onDataLoaded(tabs, bookmarks, history);
                        if (postDrafts != null) {
                            listener.onPostDraftsLoaded(postDrafts);
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
    public void performInitialMigration(List<String> localTabs, List<String> localBookmarks, List<String> localHistory) {
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
            public void onError(Exception e) {
                Log.e(TAG, "Failed to check cloud data for migration", e);
            }
        });
    }
}
