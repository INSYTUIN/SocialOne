package com.example.socialonetwo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;
import android.view.View;
import android.webkit.WebBackForwardList;
import android.webkit.WebView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages saving and restoring WebView states and tab previews to disk.
 */
public class TabStateManager {
    private static final String TAG = "TabStateManager";
    private final Context context;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public TabStateManager(Context context) {
        this.context = context;
    }

    /**
     * Captures a screenshot of a specific view and stores it as a bitmap.
     * Also saves the preview to disk for persistence across app restarts.
     * @param url The URL associated with the view.
     * @param view The view to capture.
     * @param tabPreviews The map to store the captured bitmap in.
     */
    public void capturePreview(String url, View view, Map<String, Bitmap> tabPreviews) {
        if (view != null && view.getWidth() > 0 && view.getHeight() > 0) {
            try {
                // Capture original view
                Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                view.draw(canvas);

                // Scale down for memory and storage efficiency
                int targetWidth = view.getWidth() / 2;
                int targetHeight = view.getHeight() / 2;
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
                bitmap.recycle();

                tabPreviews.put(url, scaled);
                savePreviewToDisk(url, scaled);
            } catch (Exception e) {
                Log.e(TAG, "Error capturing preview", e);
            }
        }
    }

    /**
     * Captures and stores a bitmap preview of the given URL's view for persistence.
     * @param url The URL associated with the bitmap to save.
     * @param bitmap The scaled bitmap preview.
     */
    public void savePreviewToDisk(String url, Bitmap bitmap) {
        executorService.execute(() -> {
            try {
                File dir = new File(context.getFilesDir(), "tab_previews");
                if (!dir.exists()) dir.mkdirs();

                String fileName = "preview_" + Math.abs(url.hashCode()) + ".png";
                File file = new File(dir, fileName);

                try (FileOutputStream out = new FileOutputStream(file)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 70, out);
                    out.flush();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving preview to disk", e);
            }
        });
    }

    /**
     * Loads tab previews from internal storage into memory.
     */
    public void loadPreviewsFromDisk(List<String> siteList, Map<String, Bitmap> tabPreviews) {
        if (siteList == null) return;

        for (String url : siteList) {
            File dir = new File(context.getFilesDir(), "tab_previews");
            String fileName = "preview_" + Math.abs(url.hashCode()) + ".png";
            File file = new File(dir, fileName);

            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                if (bitmap != null) {
                    tabPreviews.put(url, bitmap);
                }
            }
        }
    }

    /**
     * Deletes a specific tab preview from internal storage.
     * @param url The URL of the preview to delete.
     */
    public void deletePreviewFromDisk(String url) {
        executorService.execute(() -> {
            File dir = new File(context.getFilesDir(), "tab_previews");
            String fileName = "preview_" + Math.abs(url.hashCode()) + ".png";
            File file = new File(dir, fileName);
            if (file.exists()) {
                file.delete();
            }
        });
    }

    /**
     * Deletes all saved tab previews from internal storage.
     */
    public void clearAllPreviewsFromDisk() {
        executorService.execute(() -> {
            File dir = new File(context.getFilesDir(), "tab_previews");
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }
        });
    }

    /**
     * Captures and saves the state of all active WebViews to disk.
     */
    public void saveAllWebViews(Map<String, View> tabMap, Set<String> incognitoTabs) {
        for (Map.Entry<String, View> entry : tabMap.entrySet()) {
            if (entry.getValue() instanceof WebView) {
                saveWebViewState(entry.getKey(), (WebView) entry.getValue(), incognitoTabs);
            }
        }
    }

    public void saveWebViewState(String url, WebView wv, Set<String> incognitoTabs) {
        if (url == null || url.startsWith("home://") || incognitoTabs.contains(url)) return;

        Bundle bundle = new Bundle();
        wv.saveState(bundle);

        executorService.execute(() -> {
            try {
                File dir = new File(context.getFilesDir(), "webview_states");
                if (!dir.exists()) dir.mkdirs();

                File file = new File(dir, "state_" + Math.abs(url.hashCode()));
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    Parcel parcel = Parcel.obtain();
                    bundle.writeToParcel(parcel, 0);
                    fos.write(parcel.marshall());
                    parcel.recycle();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error saving WebView state", e);
            }
        });
    }

    /**
     * Restores the state of a WebView from a previously saved bundle on disk.
     * @param url The URL identifying the saved state.
     * @param wv The WebView to restore state into.
     * @return True if state was successfully restored.
     */
    public boolean restoreWebViewState(String url, WebView wv, Set<String> incognitoTabs) {
        if (url == null || url.startsWith("home://") || incognitoTabs.contains(url)) return false;

        File file = new File(new File(context.getFilesDir(), "webview_states"), "state_" + Math.abs(url.hashCode()));
        if (!file.exists()) return false;

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);
            Bundle bundle = parcel.readBundle(context.getClassLoader());
            if (bundle != null) {
                WebBackForwardList list = wv.restoreState(bundle);
                parcel.recycle();
                return list != null;
            }
            parcel.recycle();
        } catch (Exception e) {
            Log.e(TAG, "Error restoring WebView state", e);
        }
        return false;
    }

    /**
     * Deletes the saved state bundle for a specific URL from disk.
     * @param url The URL of the state to delete.
     */
    public void deleteWebViewState(String url) {
        executorService.execute(() -> {
            File file = new File(new File(context.getFilesDir(), "webview_states"), "state_" + Math.abs(url.hashCode()));
            if (file.exists()) file.delete();
        });
    }

    /**
     * Deletes all saved WebView state bundles from disk.
     */
    public void clearAllWebViewStates() {
        executorService.execute(() -> {
            File dir = new File(context.getFilesDir(), "webview_states");
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }
        });
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
