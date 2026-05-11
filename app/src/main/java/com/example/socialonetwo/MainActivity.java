package com.example.socialonetwo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.WebViewFeature;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewCompat;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.biometric.BiometricPrompt;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Main activity for the Social One application.
 * This activity manages a multi-tab web browser, a dashboard for social sites,
 * and integration with social media platforms for posting and messaging.
 */
public class MainActivity extends AppCompatActivity implements SitesAdapter.OnSiteClickListener, HistoryAdapter.OnHistoryClickListener, RecentSitesAdapter.OnRecentClickListener, TabSwitcherAdapter.OnTabClickListener {


    // UI Components for WebView and Fullscreen management
    /** FrameLayout for holding the active WebView or dashboard view. */
    private FrameLayout webViewContainer;
    /** FrameLayout for holding dashboard fragments. */
    private FrameLayout fragmentContainer;
    /** FrameLayout for displaying fullscreen content like videos. */
    private FrameLayout fullscreenContainer;

    private HomeFragment homeFragment;
    private QuickAccessFragment quickAccessFragment;
    private IncognitoFragment incognitoFragment;
    private WorkspaceFragment workspaceFragment;

    /** The root layout of the activity. */
    private View mainLayout;
    /** Input field for entering URLs or search terms in the controls panel. */
    private AutoCompleteTextView urlInput;
    /** Input field for global search across multiple platforms. */
    private AutoCompleteTextView searchInput;
    /** Horizontal progress bar indicating page loading status. */
    private ProgressBar progressBar;

    // Lists and Maps for managing sites, history, and tabs
    /** List of all currently open site URLs. */
    private List<String> siteList;
    /** List of recently visited URLs for the history display. */
    private List<String> historyList;
    /** List of bookmarked URLs. */
    private List<String> bookmarksList;
    /** Mapping of URLs to their respective View (WebView or ScrollView) instances. */
    private final Map<String, View> tabMap = new HashMap<>();
    /** Mapping of URLs to their captured bitmap previews for the tab switcher. */
    private final Map<String, Bitmap> tabPreviews = new HashMap<>();
    /** Set of URLs that are currently in incognito mode. */
    private final Set<String> incognitoTabs = new HashSet<>();
    /** Mapping of WebViews to URLs that failed to load, used for retry logic. */
    private final Map<WebView, String> failingUrls = new HashMap<>();
    /** Mapping of WebViews to their respective loading timeout Runnables. */
    private final Map<WebView, Runnable> timeoutRunnables = new HashMap<>();
    /** Mapping of WebViews to their desktop mode state. */
    private final Map<WebView, Boolean> desktopModeMap = new HashMap<>();
    /** Set of URLs that belong to the active workspace. */
    private final Set<String> workspaceTabs = new HashSet<>();
    /** Mapping of tab URLs to their parent tab URLs (who opened them). */
    private final Map<String, String> parentTabMap = new HashMap<>();

    // Adapters for various UI lists
    private SitesAdapter sitesAdapter;
    private SearchSitesAdapter searchSitesAdapter;
    private HistoryAdapter historyAdapter;
    private TabSwitcherAdapter tabSwitcherAdapter;
    private RecyclerView tabSwitcherRecyclerView;
    private RecyclerView sitesRecyclerView;
    private ImageButton btnNormalMode, btnWorkspaceMode;
    private boolean isWorkspaceTabMode = false;
    private final List<String> filteredTabList = new ArrayList<>();
    private final List<Integer> filteredIndices = new ArrayList<>();


    // Shared Preferences keys and settings
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "WebWrapperPrefs";
    private static final String SITES_KEY = "SavedSites";
    private static final String LOCKED_SITES_KEY = "LockedSites";
    private static final String HISTORY_KEY = "BrowsingHistory";
    private static final String BOOKMARKS_KEY = "SavedBookmarks";
    private static final String PREDICTIONS_KEY = "SearchPredictionsEnabled";
    private static final String SEARCH_ENGINE_KEY = "DefaultSearchEngine";
    private static final String ADVANCED_ANIM_KEY = "AdvancedAnimationsEnabled";
    private static final String AD_BLOCKER_KEY = "AdBlockerEnabled";
    private static final String FORCE_DARK_KEY = "ForceDarkModeEnabled";
    private static final String SAFE_BROWSING_KEY = "SafeBrowsingEnabled";
    private static final String BIOMETRIC_LOCK_KEY = "BiometricLockEnabled";

    private String currentUrl = null;
    private int currentPosition = -1;
    private static final String HOME_URL = "home://dashboard";
    private static final String QUICK_ACCESS_MESSAGES_URL = "home://quickaccess_messages";
    private static final String INCOGNITO_HOME_URL = "home://incognito";
    private static final String WORKSPACE_URL = "home://workspace";

    // UI Panels and interactive elements
    private CardView controlsPanel, searchPanel, moreOptionsPanel, tabSwitcherPanel;
    private Button btnGrabMedia, btnFindOnPage;
    private View btnExpandTabs, handleTouchArea;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MaterialSwitch switchDesktopSite;
    private TextView tvTabCount, tvUserStatus;
    private ViewGroup bottomUiContainer;

    private View bottomBar;
    private View dragHandle;
    private boolean isToolbarVisible = true;
    private boolean isManualHide = false;
    private boolean isAnimatingToolbar = false;
    private boolean isFindOnPageActive = false;
    private View.OnScrollChangeListener scrollListener;

    private static final int ANIM_DURATION = 100;
    private String userAgent;
    private static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    // Dialogs and Handlers for background tasks
    private AlertDialog historyDialog;
    private DownloadHandler downloadHandler;
    private MediaHandler mediaHandler;
    private PreferenceManager preferenceManager;
    private TabStateManager tabStateManager;
    private SecurityManager securityManager;
    private SearchHandler searchHandler;
    private FirestoreManager firestoreManager;
    private final Set<String> detectedMediaUrls = new HashSet<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // File chooser launcher for WebView file uploads
    private ValueCallback<Uri[]> filePathCallback;
    private final ActivityResultLauncher<String> fileChooserLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (filePathCallback != null) {
                    Uri[] uriArray = (uris == null || uris.isEmpty()) ? null : uris.toArray(new Uri[0]);
                    filePathCallback.onReceiveValue(uriArray);
                    filePathCallback = null;
                }
            }
    );

    // Permission launcher for runtime permission requests
    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                // Permissions handled via WebView callbacks
            }
    );

    // Result launcher for the Post Composer activity
    private final ActivityResultLauncher<Intent> postComposerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Toast.makeText(this, "Post scheduled successfully!", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private WebChromeClient.CustomViewCallback customViewCallback;
    private View customView;

    private boolean isAuthenticated = false;
    private boolean isAuthenticating = false;

    /**
     * Checks if biometric lock is enabled in settings and initiates authentication if needed.
     */
    private void checkBiometricLock() {
        if (sharedPreferences.getBoolean(BIOMETRIC_LOCK_KEY, false) && !isAuthenticated) {
            showBiometricPrompt();
        }
    }

    /**
     * Displays the biometric authentication prompt (fingerprint/face/credential).
     */
    private void showBiometricPrompt() {
        isAuthenticating = true;
        securityManager.applyBlurEffect(findViewById(R.id.mainLayout), true);
        View mainView = findViewById(R.id.main);
        if (mainView != null) ViewCompat.requestApplyInsets(mainView);

        securityManager.showBiometricPrompt(new SecurityManager.AuthCallback() {
            @Override
            public void onAuthenticated() {
                isAuthenticated = true;
                isAuthenticating = false;
                securityManager.applyBlurEffect(findViewById(R.id.mainLayout), false);
                if (mainView != null) ViewCompat.requestApplyInsets(mainView);
            }

            @Override
            public void onAuthError(int errorCode, @NonNull CharSequence errString) {
                isAuthenticating = false;
                securityManager.applyBlurEffect(findViewById(R.id.mainLayout), false);
                if (mainView != null) ViewCompat.requestApplyInsets(mainView);
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    finish(); // Close app if authentication is canceled
                }
            }
        });
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        Menu menu = mode.getMenu();

        // Add "Share to Post" option to the text selection menu
        MenuItem shareItem = menu.add(0, Menu.FIRST, 100, "Share to Post");
        shareItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        shareItem.setOnMenuItemClickListener(item -> {
            View currentView = tabMap.get(currentUrl);
            if (currentView instanceof WebView) {
                WebView wv = (WebView) currentView;
                String sourceUrl = wv.getUrl();

                // Fetch the selected text via JavaScript
                wv.evaluateJavascript("(function(){ return window.getSelection().toString(); })()", selectedText -> {
                    if (selectedText != null && !selectedText.equals("\"\"") && !selectedText.isEmpty()) {
                        // Clean up JSON string markers from evaluateJavascript
                        if (selectedText.startsWith("\"") && selectedText.endsWith("\"")) {
                            selectedText = selectedText.substring(1, selectedText.length() - 1);
                            selectedText = selectedText.replace("\\\"", "\"").replace("\\\\", "\\");
                        }

                        String finalPost = "\"" + selectedText + "\"\n\nSource: " + sourceUrl;

                        Intent intent = new Intent(this, PostComposerActivity.class);
                        intent.setAction(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_TEXT, finalPost);
                        postComposerLauncher.launch(intent);
                    } else {
                        Toast.makeText(this, "No text selected", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            mode.finish();
            return true;
        });

        super.onActionModeStarted(mode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AdBlockerHosts.loadFromAssets(this);
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        userAgent = WebSettings.getDefaultUserAgent(this);
        firestoreManager = new FirestoreManager();
        downloadHandler = new DownloadHandler(this);
        mediaHandler = new MediaHandler(this, downloadHandler, userAgent);
        preferenceManager = new PreferenceManager(this, firestoreManager);
        tabStateManager = new TabStateManager(this);
        securityManager = new SecurityManager(this);
        searchHandler = new SearchHandler(this);

        // Initialize Safe Browsing service globally
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebViewCompat.startSafeBrowsing(this, success -> {
                Log.d("SafeBrowsing", "Safe Browsing initialized: " + success);
            });
        }

        // Initialize main views and layouts
        View mainView = findViewById(R.id.main);
        mainLayout = findViewById(R.id.mainLayout);
        fullscreenContainer = findViewById(R.id.fullscreenContainer);
        bottomUiContainer = findViewById(R.id.bottomUiContainer);
        dragHandle = findViewById(R.id.dragHandle);
        handleTouchArea = findViewById(R.id.handleTouchArea);
        bottomBar = findViewById(R.id.bottomBar);

        webViewContainer = findViewById(R.id.webViewContainer);
        fragmentContainer = findViewById(R.id.fragmentContainer);


        // Ensure handle stays visible if content height changes while hidden
        bottomUiContainer.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (!isToolbarVisible && !isAnimatingToolbar && (bottom - top) != (oldBottom - oldTop)) {
                bottomUiContainer.setTranslationY(getHiddenTranslation());
            }
        });

        // Handle window insets for Edge-to-Edge display
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            boolean advancedAnim = sharedPreferences.getBoolean(ADVANCED_ANIM_KEY, false);
            boolean isHome = HOME_URL.equals(currentUrl);

            // If Find on Page is active or we are authenticating, we ignore the IME insets
            // so the keyboard (password prompt) appears "above" (over) the UI instead of pushing it.
            int effectiveImeBottom = (isFindOnPageActive || isAuthenticating) ? 0 : ime.bottom;
            int bottomPadding = Math.max(systemBars.bottom, effectiveImeBottom);

            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) findViewById(R.id.contentWrapper).getLayoutParams();

            if (!advancedAnim && isHome) {
                // Home tab in Normal Mode: Treat bottom bar as fixed boundary
                // This keeps the dashboard perfectly between status and navigation bars
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                bottomUiContainer.setPadding(0, 0, 0, Math.max(0, effectiveImeBottom - systemBars.bottom));
                lp.addRule(RelativeLayout.ABOVE, R.id.bottomUiContainer);

                // Content fits exactly above the bar, no extra bottom padding needed
                if (fragmentContainer != null) fragmentContainer.setPadding(0, 0, 0, 0);
                if (webViewContainer != null) webViewContainer.setPadding(0, 0, 0, 0);
            } else {

                // Other tabs or Advanced Mode: Immersive edge-to-edge drawing
                // This allows content to draw behind the floating/semi-transparent panels
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
                bottomUiContainer.setPadding(0, 0, 0, bottomPadding);
                lp.removeRule(RelativeLayout.ABOVE);

                int barHeight = UIUtils.dpToPx(this, 80);
                int extraContentPadding = bottomPadding + barHeight;

                if (webViewContainer != null) {
                    // Normal Mode + Website: Use padding to clear the fixed bar while staying immersive
                    // Advanced Mode: Use 0 padding for the truly floating experience
                    int webPadding = (!advancedAnim) ? extraContentPadding : 0;
                    webViewContainer.setPadding(0, 0, 0, webPadding);
                }

                if (fragmentContainer != null) fragmentContainer.setPadding(0, 0, 0, extraContentPadding);
            }


            boolean isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            dragHandle.setEnabled(!isKeyboardVisible);

            updateDragHandleState();

            // Auto-close tab switcher if keyboard pops up to avoid glitches
            if (isKeyboardVisible && tabSwitcherPanel != null && tabSwitcherPanel.getVisibility() == View.VISIBLE) {
                tabSwitcherPanel.setVisibility(View.GONE);
                controlsPanel.setVisibility(View.VISIBLE);
                if (advancedAnim && handleTouchArea != null) handleTouchArea.setVisibility(View.VISIBLE);
            }

            // Disable tab switcher button when keyboard is up
            if (btnExpandTabs != null) {
                btnExpandTabs.setEnabled(!isKeyboardVisible);
                btnExpandTabs.setAlpha(isKeyboardVisible ? 0.5f : 1.0f);
            }

            // If toolbar is hidden, adjust translation to keep it hidden even as keyboard height changes
            if (!isToolbarVisible && !isAnimatingToolbar) {
                bottomUiContainer.post(() -> {
                    if (!isToolbarVisible && !isAnimatingToolbar) {
                        bottomUiContainer.setTranslationY(getHiddenTranslation());
                    }
                });
            }

            if (!advancedAnim) {
                bottomUiContainer.setTranslationY(0);
                isToolbarVisible = true;
            }

            return WindowInsetsCompat.CONSUMED;
        });

        requestInitialPermissions();

        // Initialize UI components from layout
        urlInput = findViewById(R.id.urlInput);
        searchInput = findViewById(R.id.searchInput);
        progressBar = findViewById(R.id.progressBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setDistanceToTriggerSync(UIUtils.dpToPx(this, 200)); // Increased distance to make it less sensitive to accidental pulls

        Button btnDoGlobalSearch = findViewById(R.id.btnDoGlobalSearch);
        btnExpandTabs = findViewById(R.id.btnExpandTabs);
        tvTabCount = findViewById(R.id.tvTabCount);
        ImageButton btnDeleteAllTabs = findViewById(R.id.btnDeleteAllTabs);
        ImageButton btnIncognito = findViewById(R.id.btnIncognito);
        ImageButton btnNewTab = findViewById(R.id.btnNewTab);

        btnNormalMode = findViewById(R.id.btnNormalMode);
        btnWorkspaceMode = findViewById(R.id.btnWorkspaceMode);

        btnNormalMode.setOnClickListener(v -> {
            if (isWorkspaceTabMode) {
                isWorkspaceTabMode = false;
                updateTabSwitcherList();
            }
        });

        btnWorkspaceMode.setOnClickListener(v -> {
            if (!isWorkspaceTabMode) {
                isWorkspaceTabMode = true;
                updateTabSwitcherList();
            }
        });

        controlsPanel = findViewById(R.id.controlsPanel);

        searchPanel = findViewById(R.id.searchPanel);
        moreOptionsPanel = findViewById(R.id.moreOptionsPanel);
        tabSwitcherPanel = findViewById(R.id.tabSwitcherPanel);
        FloatingActionButton toggleButton = findViewById(R.id.toggleControlsButton);

        FloatingActionButton globalSearchFab = findViewById(R.id.globalSearchButton);
        FloatingActionButton moreOptionsButton = findViewById(R.id.moreOptionsButton);
        FloatingActionButton btnForward = findViewById(R.id.btnForward);
        FloatingActionButton btnCreatePost = findViewById(R.id.btnCreatePost);
        sitesRecyclerView = findViewById(R.id.sitesRecyclerView);
        RecyclerView searchSitesRecyclerView = findViewById(R.id.searchSitesRecyclerView);
        tabSwitcherRecyclerView = findViewById(R.id.tabSwitcherRecyclerView);
        btnGrabMedia = findViewById(R.id.btnGrabMedia);

        tvUserStatus = findViewById(R.id.tvUserStatus);

        Button btnViewHistory = findViewById(R.id.btnViewHistory);

        Button btnViewDownloads = findViewById(R.id.btnViewDownloads);
        btnFindOnPage = findViewById(R.id.btnFindOnPage);
        Button btnShareQR = findViewById(R.id.btnShareQR);
        Button btnSettings = findViewById(R.id.btnSettings);
        Button btnAuthAction = findViewById(R.id.btnAuthAction);
        switchDesktopSite = findViewById(R.id.switchDesktopSite);

        // Apply click animation to buttons

        UIUtils.setClickAnimation(this, globalSearchFab);
        UIUtils.setClickAnimation(this, moreOptionsButton);
        UIUtils.setClickAnimation(this, btnCreatePost);
        UIUtils.setClickAnimation(this, btnForward);
        UIUtils.setClickAnimation(this, toggleButton);
        UIUtils.setClickAnimation(this, btnDoGlobalSearch);
        UIUtils.setClickAnimation(this, btnViewHistory);
        UIUtils.setClickAnimation(this, btnViewDownloads);
        UIUtils.setClickAnimation(this, btnFindOnPage);
        UIUtils.setClickAnimation(this, btnShareQR);
        UIUtils.setClickAnimation(this, btnSettings);
        UIUtils.setClickAnimation(this, btnAuthAction);
        UIUtils.setClickAnimation(this, btnExpandTabs);

        UIUtils.setClickAnimation(this, btnGrabMedia);
        UIUtils.setClickAnimation(this, btnDeleteAllTabs);
        UIUtils.setClickAnimation(this, btnIncognito);
        UIUtils.setClickAnimation(this, btnNewTab);


        firestoreManager = new FirestoreManager();

        // Load saved state from PreferenceManager


        boolean advancedAnimEnabled = sharedPreferences.getBoolean(ADVANCED_ANIM_KEY, false);
        if (!advancedAnimEnabled) {
            handleTouchArea.setVisibility(View.GONE);
            isToolbarVisible = true;
            isManualHide = false;
            bottomBar.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.bottom_bar));
            bottomUiContainer.setBackgroundColor(Color.TRANSPARENT);
        } else {
            handleTouchArea.setVisibility(View.VISIBLE);
            bottomBar.setBackgroundColor(Color.TRANSPARENT);
            bottomUiContainer.setBackgroundColor(Color.TRANSPARENT);
            // Ensure system navigation bar is transparent for full floating effect
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }

        siteList = preferenceManager.loadSites(HOME_URL, QUICK_ACCESS_MESSAGES_URL);
        workspaceTabs.addAll(preferenceManager.loadWorkspaceTabs());
        historyList = preferenceManager.loadHistory();
        bookmarksList = preferenceManager.loadBookmarks();
        tabStateManager.loadPreviewsFromDisk(siteList, tabPreviews);


        // Initial Migration to Cloud
        firestoreManager.performInitialMigration(siteList, bookmarksList, historyList, preferenceManager.loadWorkspacesList());

        searchHandler.setupAutocomplete(urlInput, this::handleUrlInput);
        searchHandler.setupAutocomplete(searchInput, () -> btnDoGlobalSearch.performClick());

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });


        switchDesktopSite.setOnCheckedChangeListener((buttonView, isChecked) -> {
            View currentView = tabMap.get(currentUrl);
            if (currentView instanceof WebView) {
                WebView wv = (WebView) currentView;
                desktopModeMap.put(wv, isChecked);
                applyDesktopMode(wv, isChecked);
                wv.reload();
            } else if (currentUrl != null && !currentUrl.startsWith("home://")) {
                // If it's a URL but no WebView yet, it'll be picked up in createWebView
            }
        });

        // Initialize default tabs - always start with a fresh Home tab
        siteList.add(0, HOME_URL);

        updateTabCountDisplay();
        
        // Dashboard fragments are managed via fragmentContainer and FragmentManager
        // tabMap will store null for these URLs to indicate they are fragments
        tabMap.put(HOME_URL, null);
        tabMap.put(QUICK_ACCESS_MESSAGES_URL, null);
        tabMap.put(INCOGNITO_HOME_URL, null);
        tabMap.put(WORKSPACE_URL, null);


        // Pre-calculate panel heights to avoid first-time opening glitches
        mainView.post(() -> {
            updateTabSwitcherHeight();
            updateMoreOptionsHeight();
        });


        // Setup RecyclerView adapters
        sitesAdapter = new SitesAdapter(siteList, this);
        sitesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        sitesRecyclerView.setAdapter(sitesAdapter);

        Set<String> locked = sharedPreferences.getStringSet(LOCKED_SITES_KEY, new HashSet<>());
        sitesAdapter.setLockedSites(locked);
        sitesAdapter.setWorkspaceTabs(workspaceTabs);

        searchSitesAdapter = new SearchSitesAdapter(siteList);
        searchSitesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        searchSitesRecyclerView.setAdapter(searchSitesAdapter);

        tabSwitcherAdapter = new TabSwitcherAdapter(filteredTabList, tabPreviews, new TabSwitcherAdapter.OnTabClickListener() {
            @Override
            public void onTabClick(int position) {
                MainActivity.this.onTabClick(position);
            }

            @Override
            public void onTabClose(int position) {
                MainActivity.this.onTabClose(position);
            }

            @Override
            public void onTabLongClick(int position) {
                MainActivity.this.onTabLongClick(position);
            }
        });
        tabSwitcherAdapter.setWorkspaceTabs(workspaceTabs);

        tabSwitcherRecyclerView.setLayoutManager(new GridLayoutManager(this, getTabSwitcherSpanCount()));
        tabSwitcherRecyclerView.setAdapter(tabSwitcherAdapter);

        // Enable and customize animations
        androidx.recyclerview.widget.DefaultItemAnimator animator = new androidx.recyclerview.widget.DefaultItemAnimator();
        animator.setAddDuration(ANIM_DURATION);
        animator.setRemoveDuration(ANIM_DURATION);
        tabSwitcherRecyclerView.setItemAnimator(animator);

        tabSwitcherPanel.setOnTouchListener((v, event) -> {
            hideKeyboard();
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return false;
        });

        // Define scroll listener for auto-hiding the toolbar
        scrollListener = (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (v instanceof WebView && currentUrl != null && tabMap.get(currentUrl) == v) {
                updateSwipeRefreshState((WebView) v);
            }

            if (!sharedPreferences.getBoolean(ADVANCED_ANIM_KEY, false)) return;

            if (controlsPanel.getVisibility() == View.VISIBLE ||
                    searchPanel.getVisibility() == View.VISIBLE ||
                    moreOptionsPanel.getVisibility() == View.VISIBLE ||
                    tabSwitcherPanel.getVisibility() == View.VISIBLE) {
                return;
            }

            if (scrollY > oldScrollY + 10 && isToolbarVisible) {
                toggleToolbar(false);
            } else if (scrollY < oldScrollY - 10 && !isToolbarVisible && !isManualHide) {
                toggleToolbar(true);
            }
        };


        swipeRefreshLayout.setOnRefreshListener(() -> {
            View currentView = tabMap.get(currentUrl);
            if (currentView instanceof WebView) {
                ((WebView) currentView).reload();
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        // Pre-create WebViews for locked sites
        for (String url : locked) {
            if (siteList.contains(url)) {
                createWebView(url);
            }
        }

        // Setup button click listeners
        toggleButton.setOnClickListener(v -> {
            beginPanelTransition();

            if (controlsPanel.getVisibility() == View.VISIBLE || tabSwitcherPanel.getVisibility() == View.VISIBLE) {
                controlsPanel.setVisibility(View.GONE);
                tabSwitcherPanel.setVisibility(View.GONE);
                updateDragHandleState();
            } else {
                hideAllPanelsInternal();
                controlsPanel.setVisibility(View.VISIBLE);
                updateDragHandleState();
            }
        });

        btnExpandTabs.setOnClickListener(v -> {
            if (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE && !isTablet()) {
                Toast.makeText(this, "Tab switcher is not available in landscape mode", Toast.LENGTH_SHORT).show();
                return;
            }
            hideKeyboard(); // Hide keyboard first to ensure accurate height calculation
            updateTabSwitcherHeight(); // Calculate height before showing to avoid jumps
            beginPanelTransition();

            if (tabSwitcherPanel.getVisibility() == View.VISIBLE) {
                tabSwitcherPanel.setVisibility(View.GONE);
                boolean advancedAnim = sharedPreferences.getBoolean(ADVANCED_ANIM_KEY, false);
                if (advancedAnim && handleTouchArea != null) handleTouchArea.setVisibility(View.VISIBLE);
                updateDragHandleState();
            } else {
                updateCurrentTabPreview();

                // Sync tab mode with current URL on open
                if (currentUrl != null) {
                    if (incognitoTabs.contains(currentUrl)) {
                        isWorkspaceTabMode = false;
                    } else if (workspaceTabs.contains(currentUrl)) {
                        isWorkspaceTabMode = true;
                    } else if (WORKSPACE_URL.equals(currentUrl)) {
                        isWorkspaceTabMode = true;
                    } else if (!currentUrl.startsWith("home://")) {
                        isWorkspaceTabMode = false;
                    }
                }

                updateTabSwitcherHeight(); // Dynamically fit screen

                // Ensure other overlapping panels are hidden
                searchPanel.setVisibility(View.GONE);
                moreOptionsPanel.setVisibility(View.GONE);

                // Reset animation properties to ensure consistency with TransitionManager
                tabSwitcherPanel.setAlpha(1f);
                tabSwitcherPanel.setScaleX(1f);
                tabSwitcherPanel.setScaleY(1f);

                tabSwitcherPanel.setVisibility(View.VISIBLE);
                if (handleTouchArea != null) handleTouchArea.setVisibility(View.GONE);

                updateTabSwitcherList();
                updateDragHandleState();
            }
        });

        btnDeleteAllTabs.setOnClickListener(v -> showDeleteAllDialog());


        btnNewTab.setOnClickListener(v -> {
            openNewTab();
        });

        btnNewTab.setOnLongClickListener(v -> {
            openNewIncognitoTab("Incognito tab created");
            return true;
        });

        btnIncognito.setOnClickListener(v -> {
            openNewIncognitoTab("Incognito Tab Opened");
        });

        globalSearchFab.setOnClickListener(v -> {
            if (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE && !isTablet()) {
                Toast.makeText(this, "Global search is not available in landscape mode", Toast.LENGTH_SHORT).show();
                return;
            }
            beginPanelTransition();

            if (searchPanel.getVisibility() == View.VISIBLE) {
                searchPanel.setVisibility(View.GONE);
                updateDragHandleState();
            } else {
                hideAllPanelsInternal();
                searchPanel.setVisibility(View.VISIBLE);
                updateDragHandleState();
            }
        });

        moreOptionsButton.setOnClickListener(v -> {
            if (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE && !isTablet()) {
                Toast.makeText(this, "Options are not available in landscape mode", Toast.LENGTH_SHORT).show();
                return;
            }
            hideKeyboard(); // Hide keyboard first to ensure accurate height calculation
            updateMoreOptionsHeight(); // Calculate height before showing
            beginPanelTransition();

            if (moreOptionsPanel.getVisibility() == View.VISIBLE) {
                moreOptionsPanel.setVisibility(View.GONE);
                updateDragHandleState();
            } else {
                hideAllPanelsInternal();
                updateMoreOptionsHeight(); // Dynamically fit screen
                moreOptionsPanel.setVisibility(View.VISIBLE);
                updateDragHandleState();
            }
        });

        btnForward.setOnClickListener(v -> {
            View currentView = tabMap.get(currentUrl);
            if (currentView instanceof WebView) {
                WebView wv = (WebView) currentView;
                if (wv.canGoForward()) {
                    wv.goForward();
                } else {
                    Toast.makeText(this, "No page to go forward to", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCreatePost.setOnClickListener(v -> {
            Intent intent = new Intent(this, PostComposerActivity.class);
            postComposerLauncher.launch(intent);
        });

        btnViewHistory.setOnClickListener(v -> showHistoryDialog());
        btnViewDownloads.setOnClickListener(v -> downloadHandler.showDownloadsDialog());
        btnFindOnPage.setOnClickListener(v -> {
            View currentView = tabMap.get(currentUrl);
            if (currentView instanceof WebView) {
                showFindOnPageDialog();
            } else {
                Toast.makeText(this, "Find on Page is not available on this screen", Toast.LENGTH_SHORT).show();
            }
        });
        btnShareQR.setOnClickListener(v -> mediaHandler.showQRCodeDialog(currentUrl));

        btnAuthAction.setOnClickListener(v -> {
            FirebaseUser activeUser = FirebaseAuth.getInstance().getCurrentUser();
            if (activeUser != null) {
                // Logout flow
                View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_caution, null);
                TextView dTitle = dialogView.findViewById(R.id.confirmTitle);
                TextView dMessage = dialogView.findViewById(R.id.confirmMessage);
                Button dBtnConfirm = dialogView.findViewById(R.id.btnProceedConfirm);
                Button dBtnCancel = dialogView.findViewById(R.id.btnCancelConfirm);

                dTitle.setText(R.string.logout_title);
                dMessage.setText(R.string.logout_message);
                dBtnConfirm.setText(R.string.logout_confirm);
                dBtnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF5252")));

                UIUtils.setClickAnimation(this, dBtnConfirm);
                UIUtils.setClickAnimation(this, dBtnCancel);

                com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
                builder.setView(dialogView);
                AlertDialog logoutDialog = builder.create();

                if (logoutDialog.getWindow() != null) {
                    logoutDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                }

                dBtnConfirm.setOnClickListener(v2 -> {
                    logoutDialog.dismiss();
                    clearLocalData();
                    FirebaseAuth.getInstance().signOut();
                    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build();
                    GoogleSignIn.getClient(this, gso).signOut();

                    Toast.makeText(getApplicationContext(), R.string.logout_success, Toast.LENGTH_SHORT).show();

                    // Restart the app to clear all state
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });

                dBtnCancel.setOnClickListener(v2 -> logoutDialog.dismiss());
                logoutDialog.show();
            } else {
                // Login flow
                Intent intent = new Intent(this, AuthActivity.class);
                startActivity(intent);
            }
        });

        // Initialize fragments after UI components are ready
        if (savedInstanceState == null) {
            homeFragment = HomeFragment.newInstance(historyList, bookmarksList);
            quickAccessFragment = new QuickAccessFragment();
            incognitoFragment = new IncognitoFragment();
            workspaceFragment = new WorkspaceFragment();

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainer, homeFragment, "home")
                    .hide(homeFragment)
                    .add(R.id.fragmentContainer, quickAccessFragment, "quick")
                    .hide(quickAccessFragment)
                    .add(R.id.fragmentContainer, incognitoFragment, "incognito")
                    .hide(incognitoFragment)
                    .add(R.id.fragmentContainer, workspaceFragment, "workspace")
                    .hide(workspaceFragment)
                    .commit();
        } else {
            homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag("home");
            quickAccessFragment = (QuickAccessFragment) getSupportFragmentManager().findFragmentByTag("quick");
            incognitoFragment = (IncognitoFragment) getSupportFragmentManager().findFragmentByTag("incognito");
            workspaceFragment = (WorkspaceFragment) getSupportFragmentManager().findFragmentByTag("workspace");
        }

        if (homeFragment != null) {
            homeFragment.setData(historyList, bookmarksList);
            homeFragment.setListeners(this, v -> showAppInfoDialog(), v -> handleOpenQuickAccessMessages(), v -> handleOpenWorkspace());
        }
        if (quickAccessFragment != null) {
            quickAccessFragment.setListener(this::launchAppOrWeb);
        }
        if (workspaceFragment != null) {
            workspaceFragment.setListener(new WorkspaceFragment.WorkspaceListener() {
                @Override
                public void onApplyWorkspace(List<String> tabs) {
                    applyWorkspace(tabs);
                }

                @Override
                public void onClearWorkspace() {
                    handleClearWorkspace();
                }
            });
        }



        urlInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                handleUrlInput();
                return true;
            }
            return false;
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                btnDoGlobalSearch.performClick();
                return true;
            }
            return false;
        });

        btnDoGlobalSearch.setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();
            Set<String> selected = searchSitesAdapter.getSelectedSites();
            if (!query.isEmpty() && !selected.isEmpty()) {
                showConfirmSearchDialog(query, selected);
            } else if (selected.isEmpty()) {
                Toast.makeText(this, "Select at least one site", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Enter search query", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup drag handle for swipe gesture with improved robustness
        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            private float startY;
            private boolean isDragging = false;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!sharedPreferences.getBoolean(ADVANCED_ANIM_KEY, false)) return false;
                if (!v.isEnabled()) return false;

                // Disable handle interaction if any panel is open
                if (controlsPanel.getVisibility() == View.VISIBLE ||
                        searchPanel.getVisibility() == View.VISIBLE ||
                        moreOptionsPanel.getVisibility() == View.VISIBLE ||
                        tabSwitcherPanel.getVisibility() == View.VISIBLE) {
                    return false;
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        hideKeyboard();
                        startY = event.getRawY();
                        isDragging = true;
                        // Prevent parent views from intercepting while we interact with the handle
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // Optional: Could add real-time translation here for smoother feel
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (isDragging) {
                            float endY = event.getRawY();
                            float deltaY = endY - startY;

                            if (deltaY > 100) { // Significant swipe down
                                isManualHide = true;
                                toggleToolbar(false);
                            } else if (deltaY < -100) { // Significant swipe up
                                isManualHide = false;
                                toggleToolbar(true);
                            } else if (Math.abs(deltaY) < 10) {
                                // Simple tap on handle - toggle state
                                isManualHide = isToolbarVisible;
                                toggleToolbar(!isToolbarVisible);
                                v.performClick();
                            }
                        }
                        isDragging = false;
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        return true;
                }
                return false;
            }
        });

        btnGrabMedia.setOnClickListener(v -> {
            View currentView = tabMap.get(currentUrl);
            if (currentView instanceof WebView) {
                ((WebView) currentView).evaluateJavascript(
                        "(function() { " +
                                "  var media = [];" +
                                "  var imgs = document.getElementsByTagName('img');" +
                                "  for (var i = 0; i < imgs.length; i++) {" +
                                "    if (imgs[i].src && imgs[i].src.startsWith('http') && (imgs[i].naturalWidth > 50 || imgs[i].width > 50 || imgs[i].naturalWidth === undefined)) {" +
                                "      media.push(imgs[i].src);" +
                                "    }" +
                                "  }" +
                                "  var vids = document.getElementsByTagName('video');" +
                                "  for (var i = 0; i < vids.length; i++) {" +
                                "    if (vids[i].src) media.push(vids[i].src);" +
                                "    var sources = vids[i].getElementsByTagName('source');" +
                                "    for (var j = 0; j < sources.length; j++) {" +
                                "      if (sources[j].src) media.push(sources[j].src);" +
                                "    }" +
                                "  }" +
                                "  return JSON.stringify(media);" +
                                "})();",
                        value -> {
                            detectedMediaUrls.clear();
                            if (value != null && !value.equals("null") && !value.isEmpty() && !value.equals("\"[]\"")) {
                                try {
                                    String jsonStr = value;
                                    if (jsonStr.startsWith("\"") && jsonStr.endsWith("\"")) {
                                        jsonStr = jsonStr.substring(1, jsonStr.length() - 1);
                                        jsonStr = jsonStr.replace("\\\"", "\"").replace("\\\\", "\\");
                                    }
                                    JSONArray array = new JSONArray(jsonStr);
                                    for (int i = 0; i < array.length(); i++) {
                                        String mediaUrl = array.getString(i);
                                        if (!detectedMediaUrls.contains(mediaUrl)) {
                                            detectedMediaUrls.add(mediaUrl);
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            beginPanelTransition();
                            hideAllPanelsInternal();

                            mediaHandler.showMediaGrabberDialog(detectedMediaUrls);
                        }
                );
            } else {
                Toast.makeText(this, "No active web page", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle incoming intent if app was opened via a web link
        if (!handleIntent(getIntent())) {
            // Open home page by default if no external URL was provided
            onSiteClick(0);
        }

        // Auth state listener to update UI
        FirebaseAuth.getInstance().addAuthStateListener(auth -> updateAuthUI(btnAuthAction));

        // Handle back press navigation
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (tabSwitcherPanel.getVisibility() == View.VISIBLE) {
                    beginPanelTransition();
                    tabSwitcherPanel.setVisibility(View.GONE);
                    updateDragHandleState();
                } else if (searchPanel.getVisibility() == View.VISIBLE ||
                        moreOptionsPanel.getVisibility() == View.VISIBLE ||
                        controlsPanel.getVisibility() == View.VISIBLE) {

                    beginPanelTransition();
                    hideAllPanelsInternal();
                    updateDragHandleState();
                } else if (customView != null) {
                    hideCustomView();
                } else {
                    View currentView = tabMap.get(currentUrl);
                    if (currentView instanceof WebView && ((WebView) currentView).canGoBack()) {
                        ((WebView) currentView).goBack();
                    } else if (currentUrl != null && !currentUrl.equals(HOME_URL) && !currentUrl.equals(QUICK_ACCESS_MESSAGES_URL) && !currentUrl.startsWith(INCOGNITO_HOME_URL) && !currentUrl.equals(WORKSPACE_URL)) {
                        // If we are on a website but at the start of its history
                        String parentUrl = parentTabMap.get(currentUrl);
                        if (parentUrl != null && siteList.contains(parentUrl)) {
                            onSiteClick(siteList.indexOf(parentUrl));
                            parentTabMap.remove(currentUrl); // Clear link after using it once
                        } else if (currentPosition == 0) {
                            // On the first tab, go back to the Home dashboard
                            String targetHome = incognitoTabs.contains(currentUrl) ? (INCOGNITO_HOME_URL + "_" + System.currentTimeMillis()) : HOME_URL;
                            loadInCurrentTab(targetHome);
                        } else {
                            // On other tabs, just switch back to the main Home tab instead of overwriting this tab
                            onSiteClick(0);
                        }
                    } else if (currentUrl != null && !currentUrl.equals(HOME_URL)) {
                        // We are on a dashboard sub-page (Quick Access, Workspace, etc.)
                        if (currentPosition == 0) {
                            loadInCurrentTab(HOME_URL);
                        } else {
                            onSiteClick(0);
                        }
                    } else if (currentPosition != 0) {
                        // If we are on a different tab that is already at Home, switch back to the main first tab
                        onSiteClick(0);
                    } else {
                        // We are on the first tab and at the Home screen. Exit the app.
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    }
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    /**
     * Handles an incoming intent to see if it contains a URL to be opened.
     * @param intent The intent to check.
     * @return True if a URL was found and handled, false otherwise.
     */
    private boolean handleIntent(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data != null) {
                String url = data.toString();
                openInWebView(url);
                return true;
            }
        }
        return false;
    }

    private int getTabSwitcherSpanCount() {
        boolean isLandscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        return isLandscape ? getResources().getInteger(R.integer.tab_switcher_span_count_landscape)
                : getResources().getInteger(R.integer.tab_switcher_span_count_portrait);
    }

    private boolean isTablet() {
        return getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }

    private String getSearchBaseUrl() {
        int index = sharedPreferences.getInt(SEARCH_ENGINE_KEY, 0);
        return SearchEngineManager.getSearchBaseUrl(index);
    }

    /**
     * Updates the drag handle's appearance and enabled state based on keyboard and panel visibility.
     */
    private void updateDragHandleState() {
        if (dragHandle == null) return;

        boolean advancedAnim = sharedPreferences.getBoolean(ADVANCED_ANIM_KEY, false);
        if (!advancedAnim) {
            dragHandle.setEnabled(false);
            dragHandle.setAlpha(0.0f);
            return;
        }

        boolean isPanelOpen = isAnyPanelOpen();

        // Check if keyboard is visible
        boolean isKeyboardVisible = false;
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(mainView);
            if (insets != null) {
                isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            }
        }

        float targetAlpha = (isPanelOpen || isKeyboardVisible) ? 0.5f : 1.0f;

        // Animate the alpha change for a smoother transition and more reliable visual update
        if (dragHandle.getAlpha() != targetAlpha) {
            dragHandle.animate()
                    .alpha(targetAlpha)
                    .setDuration(ANIM_DURATION)
                    .start();
        }
    }

    /**
     * Animates transitions for UI panels in the bottom container.
     */
    private void beginPanelTransition() {
        hideKeyboard();
        isAnimatingToolbar = true;
        AutoTransition transition = new AutoTransition();
        transition.setDuration(ANIM_DURATION);
        TransitionManager.beginDelayedTransition(bottomUiContainer, transition);

        // Reset the animation flag after the transition completes
        mainHandler.postDelayed(() -> isAnimatingToolbar = false, ANIM_DURATION + 50);

        // Update immediately to reflect intended state change
        updateDragHandleState();

        // Update again after a short delay to catch the actual keyboard state change
        mainHandler.postDelayed(this::updateDragHandleState, 150);
        mainHandler.postDelayed(this::updateDragHandleState, 400);
    }

    private void hideKeyboard() {
        View currentFocus = this.getCurrentFocus();
        View viewToHide = (currentFocus != null) ? currentFocus : findViewById(android.R.id.content);

        if (viewToHide != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(viewToHide.getWindowToken(), 0);
            }
            viewToHide.clearFocus();
        }
    }

    /**
     * Hides all overlay panels without animation.
     */
    private void hideAllPanelsInternal() {
        controlsPanel.setVisibility(View.GONE);
        searchPanel.setVisibility(View.GONE);
        moreOptionsPanel.setVisibility(View.GONE);
        tabSwitcherPanel.setVisibility(View.GONE);

        boolean advancedAnim = sharedPreferences.getBoolean(ADVANCED_ANIM_KEY, false);
        if (advancedAnim && handleTouchArea != null) handleTouchArea.setVisibility(View.VISIBLE);

        updateDragHandleState();
    }

    /**
     * Updates the tab count badge display.
     */
    private void updateTabCountDisplay() {
        if (tvTabCount != null && siteList != null) {
            tvTabCount.setText(String.valueOf(siteList.size()));
        }
    }

    private void updateTabSwitcherList() {
        if (tabSwitcherAdapter == null || tabSwitcherRecyclerView == null) return;

        //TransitionManager.beginDelayedTransition(tabSwitcherRecyclerView, new AutoTransition().setDuration(200));
        
        filteredTabList.clear();
        filteredIndices.clear();

        if (siteList == null) return;

        for (int i = 0; i < siteList.size(); i++) {
            String url = siteList.get(i);
            if (url == null) continue;

            boolean isDashboard = url.equals(HOME_URL) || url.equals(QUICK_ACCESS_MESSAGES_URL) || url.equals(WORKSPACE_URL);
            boolean isWorkspace = workspaceTabs.contains(url);
            boolean isIncognito = incognitoTabs.contains(url);

            if (isDashboard) {
                filteredTabList.add(url);
                filteredIndices.add(i);
            } else if (isWorkspaceTabMode) {
                if (isWorkspace && !isIncognito) {
                    filteredTabList.add(url);
                    filteredIndices.add(i);
                }
            } else {
                if (!isWorkspace || isIncognito) {
                    filteredTabList.add(url);
                    filteredIndices.add(i);
                }
            }
        }

        tabSwitcherAdapter.setFilteredList(filteredTabList);
        tabSwitcherAdapter.setIncognitoTabs(incognitoTabs);

        // Find current URL's position in filtered list
        int filteredPos = -1;
        for (int i = 0; i < filteredIndices.size(); i++) {
            if (filteredIndices.get(i) == currentPosition) {
                filteredPos = i;
                break;
            }
        }
        tabSwitcherAdapter.setSelectedPosition(filteredPos);

        // Update switch UI
        if (btnNormalMode != null && btnWorkspaceMode != null) {
            btnNormalMode.setBackgroundResource(isWorkspaceTabMode ? android.R.color.transparent : R.drawable.bg_tab_switch_selected);
            btnNormalMode.setColorFilter(isWorkspaceTabMode ? androidx.core.content.ContextCompat.getColor(this, R.color.icon_tint) : androidx.core.content.ContextCompat.getColor(this, R.color.button_text));

            btnWorkspaceMode.setBackgroundResource(isWorkspaceTabMode ? R.drawable.bg_tab_switch_selected : android.R.color.transparent);
            btnWorkspaceMode.setColorFilter(isWorkspaceTabMode ? androidx.core.content.ContextCompat.getColor(this, R.color.button_text) : androidx.core.content.ContextCompat.getColor(this, R.color.icon_tint));
        }
    }

    private void updateTabSwitcherItem(int siteListIndex) {
        if (tabSwitcherAdapter == null) return;

        int filteredPos = -1;
        for (int i = 0; i < filteredIndices.size(); i++) {
            if (filteredIndices.get(i) == siteListIndex) {
                filteredPos = i;
                break;
            }
        }
        if (filteredPos != -1) {
            tabSwitcherAdapter.notifyItemChanged(filteredPos);
        }
    }

    /**
     * Hides all overlay panels with animation.
     */
    private void hideAllPanels() {
        beginPanelTransition();
        hideAllPanelsInternal();
    }

    private void applyDesktopMode(WebView wv, boolean enabled) {
        WebSettings settings = wv.getSettings();
        if (enabled) {
            settings.setUserAgentString(DESKTOP_USER_AGENT);
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
        } else {
            settings.setUserAgentString(userAgent);
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
        }
    }

    /**
     * Captures a preview of the currently active tab.
     */
    private void updateCurrentTabPreview() {
        if (currentUrl != null) {
            View viewToCapture = currentUrl.startsWith("home://") ? fragmentContainer : tabMap.get(currentUrl);
            tabStateManager.capturePreview(currentUrl, viewToCapture, tabPreviews);
        }
    }


    /**
     * Captures and saves the state of all active WebViews to disk.
     */
    private void saveAllWebViews() {
        tabStateManager.saveAllWebViews(tabMap, incognitoTabs);
    }

    @Override
    public void onTabClick(int position) {
        if (position >= 0 && position < filteredIndices.size()) {
            int realPosition = filteredIndices.get(position);
            onSiteClick(realPosition);
            beginPanelTransition();
            tabSwitcherPanel.setVisibility(View.GONE);

            boolean advancedAnim = sharedPreferences.getBoolean(ADVANCED_ANIM_KEY, false);
            if (advancedAnim && handleTouchArea != null) handleTouchArea.setVisibility(View.VISIBLE);

            updateDragHandleState();
        }
    }

    @Override
    public void onTabClose(int position) {
        if (position >= 0 && position < filteredIndices.size()) {
            int realPosition = filteredIndices.get(position);
            onTabCloseReal(realPosition);
            updateTabSwitcherList();
        }
    }

    @Override
    public void onTabLongClick(int position) {
        if (position >= 0 && position < filteredIndices.size()) {
            int realPosition = filteredIndices.get(position);
            onSiteLongClick(realPosition);
        }
    }
    
    private void onTabCloseReal(int position) {
        if (siteList == null || siteList.size() <= 1) {
            Toast.makeText(this, "Cannot close the only open tab", Toast.LENGTH_SHORT).show();
            return;
        }
        if (position >= 0 && position < siteList.size()) {
            final String removedUrl = siteList.get(position);

            // Check if any other tab is using the same URL before destroying the View
            final boolean isShared = isUrlShared(removedUrl, position);

            if (!isShared) {
                View tab = tabMap.remove(removedUrl);
                Bitmap preview = tabPreviews.remove(removedUrl);
                if (preview != null) preview.recycle();
                tabStateManager.deletePreviewFromDisk(removedUrl);
                tabStateManager.deleteWebViewState(removedUrl);

                if (tab != null) {
                    if (tab instanceof WebView) {
                        failingUrls.remove((WebView) tab);
                        cancelTimeout((WebView) tab);
                        if (incognitoTabs.contains(removedUrl)) {
                            ((WebView) tab).clearCache(true);
                            ((WebView) tab).clearHistory();
                        }
                        ViewGroup parent = (ViewGroup) tab.getParent();
                        if (parent != null) parent.removeView(tab);
                        ((WebView) tab).destroy();
                    }
                }
            }

            // Always ensure the incognito status is cleaned up if this specific URL is unique
            if (!isShared) {
                incognitoTabs.remove(removedUrl);
                workspaceTabs.remove(removedUrl);
            }

            siteList.remove(position);
            parentTabMap.remove(removedUrl);
            parentTabMap.values().removeIf(val -> val.equals(removedUrl));
            saveSites();
            updateTabCountDisplay();

            // Update adapters without calling notifyDataSetChanged to allow animations
            sitesAdapter.setIncognitoTabs(incognitoTabs);
            // tabSwitcherAdapter.setIncognitoTabs(incognitoTabs); // Will be handled in updateTabSwitcherList

            sitesAdapter.notifyItemRemoved(position);
            // tabSwitcherAdapter.notifyItemRemoved(position); // Will be handled in updateTabSwitcherList

            // Notify adapters that the item count has changed to update close button visibility
            if (siteList.size() == 1) {
                // tabSwitcherAdapter.notifyItemChanged(0);
            }

            // Update search sites adapter if needed
            searchSitesAdapter.updateFilteredList();

            if (siteList.isEmpty()) {
                openNewTab();
            } else if (position == currentPosition) {
                currentPosition = -1; // Reset to force refresh
                onSiteClick(Math.max(0, position - 1));
            } else if (position < currentPosition) {
                currentPosition--;
                // These now use notifyItemChanged internally
                sitesAdapter.setSelectedPosition(currentPosition);
                // tabSwitcherAdapter.setSelectedPosition(currentPosition);
            }
        }
    }

    private boolean isUrlShared(String url, int excludePosition) {
        for (int idx = 0; idx < siteList.size(); idx++) {
            if (idx != excludePosition && siteList.get(idx).equals(url)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates the correct translationY to hide the toolbar while keeping the handle
     * visible above the system navigation bars.
     */
    private float getHiddenTranslation() {
        int handleHeight = (handleTouchArea != null && handleTouchArea.getHeight() > 0)
                ? handleTouchArea.getHeight() : UIUtils.dpToPx(this, 40);
        // Slide down the entire height of the container except for the handle area
        // We no longer subtract bottomPadding to ensure no gap is left at the bottom
        return (float) (bottomUiContainer.getHeight() - handleHeight);
    }

    /**
     * Toggles the visibility of the bottom toolbar.
     * @param show True to show, false to hide.
     */
    private void toggleToolbar(boolean show) {
        if (!sharedPreferences.getBoolean(ADVANCED_ANIM_KEY, false)) {
            show = true;
        }
        if (isToolbarVisible == show) {
            if (show) hideKeyboard();
            return;
        }
        isToolbarVisible = show;
        hideKeyboard();

        isAnimatingToolbar = true;
        View spacer = findViewById(R.id.hideModeSpacer);
        int startHeight = spacer != null ? spacer.getHeight() : 0;
        int endHeight = show ? 0 : UIUtils.dpToPx(this, 70);

        // Prepare layout transition for height change
        AutoTransition transition = new AutoTransition();
        transition.setDuration(ANIM_DURATION);
        TransitionManager.beginDelayedTransition(bottomUiContainer, transition);

        // Change height (will be animated by TransitionManager)
        if (spacer != null) {
            spacer.getLayoutParams().height = endHeight;
            spacer.requestLayout();
        }

        // Calculate the translation based on the FUTURE height to avoid jumps
        int currentContainerHeight = bottomUiContainer.getHeight();
        int targetContainerHeight = currentContainerHeight - startHeight + endHeight;
        int handleHeight = (handleTouchArea != null && handleTouchArea.getHeight() > 0)
                ? handleTouchArea.getHeight() : UIUtils.dpToPx(this, 40);
        int bottomPadding = bottomUiContainer.getPaddingBottom();
        float targetTranslation = show ? 0 : (targetContainerHeight - handleHeight - bottomPadding);

        // Animate container translation (synced with height animation)
        bottomUiContainer.animate()
                .translationY(targetTranslation)
                .setDuration(ANIM_DURATION)
                .withEndAction(() -> isAnimatingToolbar = false)
                .start();
    }

    private void openNewTab() {
        int insertPos = currentPosition + 1;

        siteList.add(insertPos, HOME_URL);
        saveSites();
        updateTabCountDisplay();

        sitesAdapter.notifyItemInserted(insertPos);
        updateTabSwitcherList();

        searchSitesAdapter.updateFilteredList();

        // Open the new tab automatically but don't close the panel
        onSiteClick(insertPos);
    }

    private void openNewIncognitoTab(String toastMessage) {
        if (isWorkspaceTabMode) {
            isWorkspaceTabMode = false;
        }

        String incognitoUrl = INCOGNITO_HOME_URL + "_" + System.currentTimeMillis();
        int insertPos = currentPosition + 1;
        siteList.add(insertPos, incognitoUrl);
        incognitoTabs.add(incognitoUrl);
        saveSites();
        updateTabCountDisplay();

        sitesAdapter.setIncognitoTabs(incognitoTabs);
        sitesAdapter.notifyItemInserted(insertPos);
        updateTabSwitcherList();
        searchSitesAdapter.updateFilteredList();

        onSiteClick(insertPos);
        if (toastMessage != null) {
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loads a URL in the currently active tab, replacing its content.
     * @param url The URL to load.
     */
    private void loadInCurrentTab(String url) {
        if (currentPosition >= 0 && currentPosition < siteList.size()) {
            String oldUrl = siteList.get(currentPosition);
            boolean isIncognito = incognitoTabs.contains(oldUrl);

            // Check if we are navigating to a dashboard layout
            boolean isTargetHome = url.equals(HOME_URL) || url.equals(QUICK_ACCESS_MESSAGES_URL) || url.startsWith(INCOGNITO_HOME_URL) || url.equals(WORKSPACE_URL);

            siteList.set(currentPosition, url);
            if (isIncognito) {
                incognitoTabs.remove(oldUrl);
                incognitoTabs.add(url);
            }

            // Check if old URL is still used by other tabs before removing from map
            boolean isOldUrlShared = false;
            for (int i = 0; i < siteList.size(); i++) {
                if (i != currentPosition && siteList.get(i).equals(oldUrl)) {
                    isOldUrlShared = true;
                    break;
                }
            }

            if (!isOldUrlShared && !oldUrl.equals(HOME_URL) && !oldUrl.equals(QUICK_ACCESS_MESSAGES_URL) && !oldUrl.startsWith(INCOGNITO_HOME_URL) && !oldUrl.equals(WORKSPACE_URL)) {
                // If switching from WebView to Home, we can clean up the WebView if not shared
                if (isTargetHome) {
                    View oldTab = tabMap.remove(oldUrl);
                    if (oldTab instanceof WebView) {
                        ViewGroup parent = (ViewGroup) oldTab.getParent();
                        if (parent != null) parent.removeView(oldTab);
                        ((WebView) oldTab).destroy();
                    }
                }
            }

            saveSites();
            sitesAdapter.notifyItemChanged(currentPosition);
            updateTabSwitcherItem(currentPosition);
            searchSitesAdapter.updateFilteredList();

            if (isTargetHome) {
                currentUrl = null; // Force showWebView to re-evaluate
                showWebView(url);
            } else {
                View oldView = tabMap.get(oldUrl);
                if (oldView instanceof WebView) {
                    WebView wv = (WebView) oldView;
                    wv.loadUrl(url);

                    if (!isOldUrlShared && !oldUrl.equals(url)) {
                        tabMap.remove(oldUrl);
                    }
                    tabMap.put(url, wv);
                    currentUrl = url;
                    // Trigger animation/update for the current URL
                    showWebView(url);
                } else {
                    currentUrl = null; // Force showWebView to re-evaluate
                    showWebView(url);
                }
            }
        } else {
            // Fallback: if no valid position, open as new tab
            int insertPos = currentPosition + 1;
            siteList.add(insertPos, url);
            saveSites();
            updateTabCountDisplay();
            sitesAdapter.notifyItemInserted(insertPos);
            searchSitesAdapter.updateFilteredList();
            onSiteClick(insertPos);
        }
    }

    /**
     * Handles user input from the URL text field.
     */
    private void handleUrlInput() {
        String input = urlInput.getText().toString().trim();
        if (!input.isEmpty()) {
            String url;
            if (input.contains(".") && !input.contains(" ")) {
                url = input;
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
            } else {
                url = getSearchBaseUrl() + Uri.encode(input);
            }

            loadInCurrentTab(url);
            urlInput.setText("");
            beginPanelTransition();
            // controlsPanel.setVisibility(View.GONE);
            updateDragHandleState();
        }
    }

    /**
     * Attempts to launch a native app to a specific URL or its main screen.
     * Falls back to opening the web URL in the app's WebView if the app is not installed.
     * @param packageName The package name of the native app.
     * @param webUrl The URL to open as a fallback or deep link.
     */
    private void launchAppOrWeb(String packageName, String webUrl) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
        intent.setPackage(packageName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            // Try to open the specific section in the app first
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // Fallback to just launching the app's main screen
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                if (launchIntent != null) {
                    startActivity(launchIntent);
                } else {
                    // Fallback to internal WebView
                    openInWebView(webUrl);
                }
            }
        } catch (Exception e) {
            // Ultimate fallback
            openInWebView(webUrl);
        }
    }

    private void openOnPlayStore(String packageName) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }

    /**
     * Opens a URL in a WebView tab, switching to an existing one if available.
     * @param url The URL to open.
     */
    private void openInWebView(String url) {
        loadInCurrentTab(url);
    }

    /**
     * Extracts the domain from a URL string.
     */
    private String getDomain(String url) {
        if (url == null || url.startsWith("home://")) return url;
        String domain = url.replace("https://", "").replace("http://", "").replace("www.", "");
        int slashIndex = domain.indexOf('/');
        if (slashIndex != -1) domain = domain.substring(0, slashIndex);
        return domain;
    }

    /**
     * Requests necessary runtime permissions for the application.
     */
    private void requestInitialPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.RECORD_AUDIO);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO);
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        permissionLauncher.launch(permissions.toArray(new String[0]));
    }

    /**
     * Shows a dialog to confirm deleting all recent sites.
     */
    private void showDeleteAllDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_all, null);

        Button btnConfirm = dialogView.findViewById(R.id.btnConfirmDelete);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelDelete);

        UIUtils.setClickAnimation(this, btnConfirm);
        UIUtils.setClickAnimation(this, btnCancel);

        com.google.android.material.dialog.MaterialAlertDialogBuilder deleteDialogBuilder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        deleteDialogBuilder.setView(dialogView);
        AlertDialog deleteDialog = deleteDialogBuilder.create();

        if (deleteDialog.getWindow() != null) {
            deleteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnConfirm.setOnClickListener(v -> {
            for (View view : tabMap.values()) {
                if (view instanceof WebView) {
                    ViewGroup parent = (ViewGroup) view.getParent();
                    if (parent != null) parent.removeView(view);
                    ((WebView) view).destroy();
                }
            }

            tabStateManager.clearAllPreviewsFromDisk();
            tabStateManager.clearAllWebViewStates();
            incognitoTabs.clear();
            parentTabMap.clear();
            
            tabMap.clear();
            tabMap.put(HOME_URL, null);
            tabMap.put(QUICK_ACCESS_MESSAGES_URL, null);

            siteList.clear();
            siteList.add(HOME_URL);
            updateTabCountDisplay();

            currentPosition = 0;
            sitesAdapter.setSelectedPosition(0);
            sitesAdapter.notifyDataSetChanged();
            updateTabSwitcherList();
            searchSitesAdapter.updateFilteredList();

            saveSites();
            onSiteClick(0);
            
            if (homeFragment != null) homeFragment.notifyDataChanged();
            
            Toast.makeText(this, "All recent sites cleared", Toast.LENGTH_SHORT).show();

            deleteDialog.dismiss();

            beginPanelTransition();
            hideAllPanelsInternal();
        });

        btnCancel.setOnClickListener(v -> deleteDialog.dismiss());

        deleteDialog.show();
    }

    /**
     * Shows a dialog to confirm performing a global search.
     */
    private void showConfirmSearchDialog(String query, Set<String> selected) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_caution, null);
        Button btnProceed = dialogView.findViewById(R.id.btnProceedConfirm);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelConfirm);

        UIUtils.setClickAnimation(this, btnProceed);
        UIUtils.setClickAnimation(this, btnCancel);

        com.google.android.material.dialog.MaterialAlertDialogBuilder confirmDialogBuilder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        confirmDialogBuilder.setView(dialogView);
        AlertDialog confirmDialog = confirmDialogBuilder.create();

        if (confirmDialog.getWindow() != null) {
            confirmDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnProceed.setOnClickListener(v -> {
            performGlobalSearch(query, selected);
            beginPanelTransition();
            // searchPanel.setVisibility(View.GONE);
            updateDragHandleState();
            confirmDialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> confirmDialog.dismiss());

        confirmDialog.show();
    }

    /**
     * Shows the browsing history dialog.
     */
    @SuppressLint("NotifyDataSetChanged")
    private void showHistoryDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_history, null);
        RecyclerView rv = dialogView.findViewById(R.id.historyRecyclerView);
        Button clearAll = dialogView.findViewById(R.id.btnClearAllHistory);
        ImageButton closeBtn = dialogView.findViewById(R.id.btnCloseHistory);
        TextView tvEmpty = dialogView.findViewById(R.id.tvEmptyMessage);

        tvEmpty.setText(R.string.history_empty);
        tvEmpty.setVisibility(historyList.isEmpty() ? View.VISIBLE : View.GONE);
        rv.setVisibility(historyList.isEmpty() ? View.GONE : View.VISIBLE);

        UIUtils.setClickAnimation(this, clearAll);
        UIUtils.setClickAnimation(this, closeBtn);

        historyAdapter = new HistoryAdapter(historyList, this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(historyAdapter);

        com.google.android.material.dialog.MaterialAlertDialogBuilder historyDialogBuilder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        historyDialogBuilder.setView(dialogView);
        historyDialog = historyDialogBuilder.create();

        if (historyDialog.getWindow() != null) {
            historyDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        closeBtn.setOnClickListener(v -> historyDialog.dismiss());

        clearAll.setOnClickListener(v -> new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_clear_history_title)
                .setMessage(R.string.dialog_clear_history_message)
                .setPositiveButton(R.string.dialog_clear_all, (d, w) -> {
                    for (View view : tabMap.values()) {
                        if (view instanceof WebView) {
                            ((WebView) view).clearHistory();
                        }
                    }
                    historyList.clear();
                    saveHistory();
                    if (historyAdapter != null) historyAdapter.notifyDataSetChanged();
                    if (homeFragment != null) homeFragment.notifyDataChanged();
                    Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show();

                    historyDialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show());

        if (historyDialog.getWindow() != null) {
            // Set width to 95% before showing to avoid wonky jumping animation
            android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
            lp.copyFrom(historyDialog.getWindow().getAttributes());
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.95);
            lp.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            historyDialog.getWindow().setAttributes(lp);
        }

        hideKeyboard();
        historyDialog.show();
    }

    @Override
    public void onHistoryClick(String url) {
        loadInCurrentTab(url);
        if (historyDialog != null) historyDialog.dismiss();
        beginPanelTransition();
        moreOptionsPanel.setVisibility(View.GONE);
        updateDragHandleState();
    }

    @Override
    public void onHistoryLongClick(String url, int position) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_history_title)
                .setMessage(url)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    historyList.remove(position);
                    saveHistory();
                    if (historyAdapter != null) historyAdapter.notifyItemRemoved(position);

                    if (historyDialog != null && historyDialog.isShowing()) {
                        TextView tvEmpty = historyDialog.findViewById(R.id.tvEmptyMessage);
                        RecyclerView rv = historyDialog.findViewById(R.id.historyRecyclerView);
                        if (tvEmpty != null && rv != null) {
                            tvEmpty.setVisibility(historyList.isEmpty() ? View.VISIBLE : View.GONE);
                            rv.setVisibility(historyList.isEmpty() ? View.GONE : View.VISIBLE);
                        }
                    }

                    if (homeFragment != null) homeFragment.notifyDataChanged();
                })

                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onRecentClick(String url) {
        onHistoryClick(url);
    }

    @Override
    public void onRecentLongClick(String url) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_site_title)
                .setMessage(getString(R.string.delete_site_message, getDomain(url)))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (bookmarksList.contains(url)) {
                        bookmarksList.remove(url);
                        saveBookmarks();
                        if (homeFragment != null) homeFragment.notifyDataChanged();
                    } else if (historyList.contains(url)) {
                        // For recent sites, we remove all entries of this domain from history
                        String targetDomain = getDomain(url);
                        historyList.removeIf(u -> getDomain(u).equals(targetDomain));
                        saveHistory();
                        if (homeFragment != null) homeFragment.notifyDataChanged();
                    }
                })

                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    public PreferenceManager getPreferenceManager() {
        return preferenceManager;
    }

    @Override
    public void onSiteClick(int position) {
        hideKeyboard();
        if (siteList != null && position >= 0 && position < siteList.size()) {
            currentPosition = position;
            String selectedUrl = siteList.get(position);

            //  Update tab mode automatically based on selection
            boolean modeChanged = false;
            if (incognitoTabs.contains(selectedUrl)) {
                if (isWorkspaceTabMode) {
                    isWorkspaceTabMode = false;
                    modeChanged = true;
                }
            } else if (workspaceTabs.contains(selectedUrl)) {
                if (!isWorkspaceTabMode) {
                    isWorkspaceTabMode = true;
                    modeChanged = true;
                }
            } else if (!selectedUrl.startsWith("home://")) {
                if (isWorkspaceTabMode) {
                    isWorkspaceTabMode = false;
                    modeChanged = true;
                }
            }

            sitesAdapter.setSelectedPosition(position);

            // Map real position to filtered position for tab switcher
            int filteredPos = -1;
            for (int i = 0; i < filteredIndices.size(); i++) {
                if (filteredIndices.get(i) == position) {
                    filteredPos = i;
                    break;
                }
            }
            
            if (tabSwitcherPanel != null && tabSwitcherPanel.getVisibility() == View.VISIBLE) {
                if (modeChanged) {
                    updateTabSwitcherList();
                } else {
                    tabSwitcherAdapter.setSelectedPosition(filteredPos);
                }
            } else {
                tabSwitcherAdapter.setSelectedPosition(filteredPos);
            }

            // Automatically scroll the site recycler to keep the selected tab in view
            if (sitesRecyclerView != null) {
                sitesRecyclerView.smoothScrollToPosition(position);
            }

            // Automatically scroll the tab switcher grid ONLY if switched from site recycler
            if (tabSwitcherRecyclerView != null && filteredPos != -1 && tabSwitcherPanel.getVisibility() != View.VISIBLE) {
                tabSwitcherRecyclerView.smoothScrollToPosition(filteredPos);
            }

            showWebView(selectedUrl);
            isManualHide = false;
            toggleToolbar(true); // Ensure toolbar is visible when switching
        }
    }

    @Override
    public void onSiteLongClick(int position) {
        if (position >= 0 && position < siteList.size()) {
            String url = siteList.get(position);

            boolean isLocked = sitesAdapter.isLocked(url);
            boolean isBookmarked = bookmarksList.contains(url);
            boolean isWorkspace = workspaceTabs.contains(url);

            List<String> options = new ArrayList<>();
            List<Integer> icons = new ArrayList<>();

            options.add(isLocked ? getString(R.string.option_unlock) : getString(R.string.option_lock));
            icons.add(isLocked ? R.drawable.unlock : R.drawable.lock);

            options.add(isBookmarked ? getString(R.string.bookmark_removed) : getString(R.string.option_bookmark));
            icons.add(isBookmarked ? R.drawable.starcolor : R.drawable.starnocolor);

            options.add(getString(R.string.option_refresh));
            icons.add(R.drawable.sync);

            if (isWorkspace) {
                options.add("Clear Active Workspace");
                icons.add(R.drawable.trashbin);
            }

            options.add(getString(R.string.option_copy_link));
            icons.add(0);

            options.add(getString(R.string.option_edit_link));
            icons.add(0);


            options.add(getString(R.string.option_delete_tab));
            icons.add(0);


            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.item_dialog_option, R.id.optionText, options) {
                @NonNull
                @Override
                public View getView(int pos, @androidx.annotation.Nullable View convertView, @NonNull ViewGroup parent) {
                    View view = super.getView(pos, convertView, parent);
                    ImageView iconView = view.findViewById(R.id.optionIcon);
                    if (icons.get(pos) != 0) {
                        iconView.setImageResource(icons.get(pos));
                        iconView.setVisibility(View.VISIBLE);
                    } else {
                        iconView.setVisibility(View.GONE);
                    }
                    return view;
                }
            };

            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.site_options)
                    .setAdapter(adapter, (dialog, which) -> {
                        String selection = options.get(which);
                        if (selection.equals(getString(R.string.option_unlock)) || selection.equals(getString(R.string.option_lock))) {
                            handleToggleLock(url);
                        } else if (selection.equals(getString(R.string.bookmark_removed)) || selection.equals(getString(R.string.option_bookmark))) {
                            handleToggleBookmark(url);
                        } else if (selection.equals(getString(R.string.option_refresh))) {
                            handleRefreshTab(url);
                        } else if (selection.equals("Clear Active Workspace")) {
                            handleClearWorkspace();
                        } else if (selection.equals(getString(R.string.option_copy_link))) {
                            handleCopyLink(url);
                        } else if (selection.equals(getString(R.string.option_edit_link))) {
                            handleEditLink(url);
                        } else if (selection.equals(getString(R.string.option_delete_tab))) {
                            onTabCloseReal(position);
                            updateTabSwitcherList();
                        }
                    })
                    .show();
        }
    }

    private void handleRefreshTab(String url) {
        View view = tabMap.get(url);
        if (view instanceof WebView) {
            ((WebView) view).reload();
            Toast.makeText(this, "Refreshing " + getDomain(url), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Cannot refresh this page", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleOpenQuickAccessMessages() {
        if (!siteList.contains(QUICK_ACCESS_MESSAGES_URL)) {
            // Always insert dashboard fragments at the beginning (after Home)
            int insertPos = 1;
            siteList.add(insertPos, QUICK_ACCESS_MESSAGES_URL);
            saveSites();
            updateTabCountDisplay();
            sitesAdapter.notifyItemInserted(insertPos);
            searchSitesAdapter.updateFilteredList();
        }
        onSiteClick(siteList.indexOf(QUICK_ACCESS_MESSAGES_URL));
    }

    private void handleOpenWorkspace() {
        if (!siteList.contains(WORKSPACE_URL)) {
            // Always insert dashboard fragments at the beginning (after Home/Messages)
            int insertPos = 1;
            if (siteList.contains(QUICK_ACCESS_MESSAGES_URL)) insertPos = 2;
            
            siteList.add(insertPos, WORKSPACE_URL);
            saveSites();
            updateTabCountDisplay();
            sitesAdapter.notifyItemInserted(insertPos);
            searchSitesAdapter.updateFilteredList();
        }
        onSiteClick(siteList.indexOf(WORKSPACE_URL));
    }

    private void applyWorkspace(List<String> workspaceUrls) {
        if (workspaceUrls == null || workspaceUrls.isEmpty() || siteList == null) return;

        // 1. Remove all current workspace tabs from the site list and cleanup their resources
        List<String> currentWorkspaceUrls = new ArrayList<>(workspaceTabs);
        workspaceTabs.clear();

        for (String url : currentWorkspaceUrls) {
            // Never remove dashboard URLs
            if (url.startsWith("home://")) continue;

            // Find and remove all instances of this URL from the session
            while (siteList.contains(url)) {
                int index = siteList.indexOf(url);
                siteList.remove(index);

                // Only destroy the WebView if this URL is no longer used in any other tab
                boolean isShared = false;
                for (String s : siteList) {
                    if (s.equals(url)) {
                        isShared = true;
                        break;
                    }
                }

                if (!isShared) {
                    View tab = tabMap.remove(url);
                    if (tab instanceof WebView) {
                        ViewGroup parent = (ViewGroup) tab.getParent();
                        if (parent != null) parent.removeView(tab);
                        ((WebView) tab).destroy();
                    }
                    tabPreviews.remove(url);
                    tabStateManager.deletePreviewFromDisk(url);
                    tabStateManager.deleteWebViewState(url);
                }
            }
        }

        // 2. Add the new workspace URLs to the session (always at the end)
        for (String url : workspaceUrls) {
            if (url.startsWith("home://")) continue; // Never add internal dashboards to workspace list
            workspaceTabs.add(url);
            if (!siteList.contains(url)) {
                siteList.add(url);
            }
        }

        saveSites();
        updateTabCountDisplay();
        
        // 3. Update UI and switch to the first tab of the new workspace
        if (sitesAdapter != null) {
            sitesAdapter.setWorkspaceTabs(workspaceTabs);
        }
        if (tabSwitcherAdapter != null) {
            tabSwitcherAdapter.setWorkspaceTabs(workspaceTabs);
        }

        sitesAdapter.notifyDataSetChanged();
        updateTabSwitcherList();

        int firstTabPos = siteList.indexOf(workspaceUrls.get(0));
        if (firstTabPos != -1) {
            onSiteClick(firstTabPos);
        }

        Toast.makeText(this, "Workspace Applied", Toast.LENGTH_SHORT).show();
    }

    private void handleClearWorkspace() {
        if (workspaceTabs.isEmpty()) {
            Toast.makeText(this, "No active workspace tabs to clear", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> toRemove = new ArrayList<>(workspaceTabs);
        workspaceTabs.clear();

        for (String url : toRemove) {
            // Find and close all tabs matching workspace URLs
            for (int i = siteList.size() - 1; i >= 0; i--) {
                if (siteList.get(i).equals(url)) {
                    onTabClose(i);
                }
            }
        }

        if (sitesAdapter != null) sitesAdapter.setWorkspaceTabs(workspaceTabs);
        if (tabSwitcherAdapter != null) tabSwitcherAdapter.setWorkspaceTabs(workspaceTabs);

        Toast.makeText(this, "Workspace tabs cleared", Toast.LENGTH_SHORT).show();
    }

    private void handleCopyLink(String url) {

        if (url.startsWith("home://")) {
            Toast.makeText(this, "Cannot copy this link", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Site Link", url);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Link copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleEditLink(String url) {
        if (url.startsWith("home://")) {
            Toast.makeText(this, "Cannot edit this link", Toast.LENGTH_SHORT).show();
            return;
        }

        urlInput.setText(url);
        urlInput.setSelection(url.length()); // Put cursor at the end

        beginPanelTransition();
        hideAllPanelsInternal();
        controlsPanel.setVisibility(View.VISIBLE);
        urlInput.requestFocus();

        // Ensure keyboard is shown
        urlInput.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(urlInput, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 100);
    }

    private void handleToggleLock(String url) {
        sitesAdapter.toggleLock(url, webViewContainer);
        saveLockedSites();
        boolean isLockedNow = sitesAdapter.isLocked(url);
        if (isLockedNow) {
            if (!tabMap.containsKey(url)) {
                createWebView(url);
            } else {
                View v = tabMap.get(url);
                if (v instanceof WebView) {
                    ((WebView) v).onResume();
                }
            }
        } else {
            // If unlocked and not current URL, pause it
            if (!url.equals(currentUrl)) {
                View v = tabMap.get(url);
                if (v instanceof WebView) {
                    ((WebView) v).onPause();
                }
            }
        }
    }

    private void handleToggleBookmark(String url) {
        if (bookmarksList.contains(url)) {
            bookmarksList.remove(url);
            Toast.makeText(this, R.string.bookmark_removed, Toast.LENGTH_SHORT).show();
        } else {
            bookmarksList.add(0, url);
            Toast.makeText(this, R.string.bookmark_added, Toast.LENGTH_SHORT).show();
        }
        saveBookmarks();
        if (homeFragment != null) homeFragment.notifyDataChanged();
    }


    /**
     * Saves the list of currently locked sites to SharedPreferences.
     */
    private void saveLockedSites() {
        preferenceManager.saveLockedSites(siteList, sitesAdapter);
    }

    private void cancelTimeout(WebView view) {
        Runnable r = timeoutRunnables.remove(view);
        if (r != null) {
            mainHandler.removeCallbacks(r);
        }
    }

    /**
     * Hides the custom fullscreen view (e.g., for video playback).
     */
    private void hideCustomView() {
        if (customView == null) return;

        fullscreenContainer.removeView(customView);
        fullscreenContainer.setVisibility(View.GONE);
        mainLayout.setVisibility(View.VISIBLE);

        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
        }
        customView = null;
        customViewCallback = null;

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /**
     * Configures a WebView with required settings for social media browsing.
     * @param wv The WebView to configure.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView wv) {
        WebSettings settings = wv.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setUserAgentString(userAgent);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setLoadsImagesAutomatically(true);
        settings.setBlockNetworkImage(false);
        settings.setBlockNetworkLoads(false);

        settings.setGeolocationEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);

        // Force Dark Mode implementation
        if (sharedPreferences.getBoolean(FORCE_DARK_KEY, false)) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true);
            } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_ON);
            }
        }

        // Safe Browsing implementation
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(settings, sharedPreferences.getBoolean(SAFE_BROWSING_KEY, true));
        }

        Boolean isDesktop = desktopModeMap.get(wv);
        if (isDesktop != null && isDesktop) {
            settings.setUserAgentString(DESKTOP_USER_AGENT);
        } else {
            settings.setUserAgentString(userAgent);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        wv.setNestedScrollingEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true);

        wv.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            downloadHandler.downloadFile(url, mimetype, contentDisposition, userAgent);
        });

        wv.setOnLongClickListener(v -> {
            WebView.HitTestResult result = wv.getHitTestResult();
            int type = result.getType();
            String extra = result.getExtra();

            if (extra == null) return false;

            boolean isIncognito = false;
            for (Map.Entry<String, View> entry : tabMap.entrySet()) {
                if (entry.getValue() == wv && incognitoTabs.contains(entry.getKey())) {
                    isIncognito = true;
                    break;
                }
            }
            final boolean finalIncognito = isIncognito;

            if (type == WebView.HitTestResult.IMAGE_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                showImageOptionsDialog(extra, finalIncognito);
                return true;
            } else if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                showLinkOptionsDialog(extra, finalIncognito);
                return true;
            }
            return false;
        });

        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("Location Permission")
                        .setMessage(origin + " wants to access your location.")
                        .setPositiveButton("Allow", (d, w) -> {
                            saveSitePermission(origin, "Location", true);
                            callback.invoke(origin, true, true);
                        })
                        .setNegativeButton("Block", (d, w) -> {
                            saveSitePermission(origin, "Location", false);
                            callback.invoke(origin, false, true);
                        })
                        .show();
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;
                fileChooserLauncher.launch("*/*");
                return true;
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                MainActivity.this.runOnUiThread(() -> {
                    String origin = request.getOrigin().toString();
                    String[] resources = request.getResources();
                    StringBuilder message = new StringBuilder(origin + " wants to access:\n");
                    for (String r : resources) {
                        message.append("• ").append(r.replace("android.webkit.resource.", "")).append("\n");
                    }

                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(MainActivity.this)
                            .setTitle("Site Permission Request")
                            .setMessage(message.toString())
                            .setPositiveButton("Allow", (d, w) -> {
                                for (String r : resources) {
                                    saveSitePermission(origin, r.replace("android.webkit.resource.", ""), true);
                                }
                                request.grant(resources);
                            })
                            .setNegativeButton("Block", (d, w) -> {
                                for (String r : resources) {
                                    saveSitePermission(origin, r.replace("android.webkit.resource.", ""), false);
                                }
                                request.deny();
                            })
                            .show();
                });
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    hideCustomView();
                    return;
                }
                customView = view;
                customViewCallback = callback;

                mainLayout.setVisibility(View.GONE);
                fullscreenContainer.setVisibility(View.VISIBLE);
                fullscreenContainer.addView(customView);

                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            @Override
            public void onHideCustomView() {
                hideCustomView();
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                WebView newWebView = new WebView(MainActivity.this);
                setupWebView(newWebView);

                boolean originIncognito = false;
                for (Map.Entry<String, View> entry : tabMap.entrySet()) {
                    if (entry.getValue() == view && incognitoTabs.contains(entry.getKey())) {
                        originIncognito = true;
                        break;
                    }
                }
                final boolean finalIncognito = originIncognito;

                newWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        handleNewTabUrl(view, url, finalIncognito);
                        return true;
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                        handleNewTabUrl(view, request.getUrl().toString(), finalIncognito);
                        return true;
                    }
                });

                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }
        });

        wv.setOnScrollChangeListener(scrollListener);
    }

    private boolean handleExternalScheme(WebView view, String url) {
        if (url == null) return false;

        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("home://") || url.startsWith("data:")) {
            return false;
        }

        try {
            Intent intent;
            if (url.startsWith("intent://")) {
                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                if (intent != null) {
                    PackageManager pm = getPackageManager();
                    if (pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                        startActivity(intent);
                        return true;
                    }
                    // Try to extract browser fallback URL
                    String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                    if (fallbackUrl != null && view != null) {
                        view.loadUrl(fallbackUrl);
                        return true;
                    }
                    // If no fallback, maybe try to open Play Store if it's a market link or has package info
                    String packageName = intent.getPackage();
                    if (packageName != null) {
                        openOnPlayStore(packageName);
                        return true;
                    }
                }
            } else {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                if (getPackageManager().resolveActivity(intent, 0) != null) {
                    startActivity(intent);
                } else {
                    // Specific handling for common schemes if app not found
                    if (url.startsWith("tel:")) {
                        Toast.makeText(this, "No dialer app found", Toast.LENGTH_SHORT).show();
                    } else if (url.startsWith("mailto:")) {
                        Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true; // We handled it (even if it failed to launch) to prevent WebView error
    }

    private void handleNewTabUrl(WebView view, String url, boolean isIncognito) {
        if (handleExternalScheme(view, url)) {
            return;
        }

        String parentUrl = currentUrl;

        if (!siteList.contains(url)) {
            int insertPos = currentPosition + 1;
            
            siteList.add(insertPos, url);
            if (isIncognito) {
                incognitoTabs.add(url);
                sitesAdapter.setIncognitoTabs(incognitoTabs);
                tabSwitcherAdapter.setIncognitoTabs(incognitoTabs);
            }
            
            saveSites();
            updateTabCountDisplay();
            sitesAdapter.notifyItemInserted(insertPos);
            searchSitesAdapter.updateFilteredList();
        }

        if (parentUrl != null) {
            parentTabMap.put(url, parentUrl);
        }
        onSiteClick(siteList.indexOf(url));
    }

    /**
     * Shows a dialog with options for a long-pressed image.
     */
    private void showImageOptionsDialog(String imageUrl, boolean isIncognito) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_image_options_title)
                .setItems(new String[]{
                        "Open in New Tab",
                        getString(R.string.option_download_image),
                        getString(R.string.option_share_image_url),
                        getString(R.string.option_add_to_post_creator)
                }, (dialog, which) -> {
                    if (which == 0) {
                        handleNewTabUrl(null, imageUrl, isIncognito);
                    } else if (which == 1) {
                        downloadHandler.downloadFile(imageUrl, null, null, userAgent);
                    } else if (which == 2) {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, imageUrl);
                        startActivity(Intent.createChooser(shareIntent, "Share Image URL"));
                    } else if (which == 3) {
                        downloadAndShareImage(imageUrl);
                    }
                })
                .show();
    }

    private void showLinkOptionsDialog(String url, boolean isIncognito) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Link Options")
                .setItems(new String[]{"Open in New Tab", "Copy Link", "Share Link"}, (dialog, which) -> {
                    if (which == 0) {
                        handleNewTabUrl(null, url, isIncognito);
                    } else if (which == 1) {
                        handleCopyLink(url);
                    } else if (which == 2) {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, url);
                        startActivity(Intent.createChooser(shareIntent, "Share Link"));
                    }
                })
                .show();
    }

    private void downloadAndShareImage(String imageUrl) {
        Toast.makeText(this, "Preparing image for post...", Toast.LENGTH_SHORT).show();
        Glide.with(this)
                .asFile()
                .load(imageUrl)
                .into(new CustomTarget<File>() {
                    @Override
                    public void onResourceReady(@NonNull File resource, @Nullable Transition<? super File> transition) {
                        try {
                            // Create a temporary file in the cache directory
                            String fileName = "shared_image_" + System.currentTimeMillis() + ".jpg";
                            File cacheFile = new File(getCacheDir(), fileName);

                            // Copy Glide's cached file to our accessible cache file
                            copyFile(resource, cacheFile);

                            Uri contentUri = FileProvider.getUriForFile(MainActivity.this,
                                    getPackageName() + ".provider", cacheFile);

                            Intent intent = new Intent(MainActivity.this, PostComposerActivity.class);
                            intent.setAction(Intent.ACTION_SEND);
                            intent.setType("image/*");
                            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            // Also pass the original URL as text if the composer wants it
                            //intent.putExtra(Intent.EXTRA_TEXT, imageUrl);

                            postComposerLauncher.launch(intent);
                        } catch (IOException e) {
                            Log.e("MainActivity", "Failed to prepare image", e);
                            Toast.makeText(MainActivity.this, "Failed to prepare image", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}

                    @Override
                    public void onLoadFailed(@Nullable android.graphics.drawable.Drawable errorDrawable) {
                        Toast.makeText(MainActivity.this, "Failed to download image", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void copyFile(File source, File target) throws IOException {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(target)) {
            byte[] buf = new byte[8192];
            int length;
            while ((length = in.read(buf)) > 0) {
                out.write(buf, 0, length);
            }
        }
    }

    /**
     * Initiates a file download using DownloadManager.
     */
    /**
     * Creates and initializes a new WebView instance for a given URL.
     * @param url The URL to load in the new WebView.
     */
    private void createWebView(String url) {
        if (tabMap.containsKey(url)) return;

        WebView wv = new WebView(this);
        wv.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (sharedPreferences.getBoolean(AD_BLOCKER_KEY, false)) {
                    if (AdBlockerHosts.shouldBlock(request.getUrl())) {
                        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleExternalScheme(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                return handleExternalScheme(view, request.getUrl().toString());
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                cancelTimeout(view);

                if (currentUrl != null && tabMap.get(currentUrl) == view) {
                    progressBar.setVisibility(View.VISIBLE);
                    updateSwipeRefreshState(view);
                    // Clear media list when starting a new page
                    detectedMediaUrls.clear();
                }

                // Add a timeout logic to handle stuck tabs
                Runnable timeoutRunnable = () -> {
                    if (view.getProgress() < 100 && tabMap.containsValue(view)) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);
                        view.stopLoading();
                        showErrorPage(view, "Timeout", "The page took too long to respond.");
                    }
                    timeoutRunnables.remove(view);
                };
                timeoutRunnables.put(view, timeoutRunnable);
                mainHandler.postDelayed(timeoutRunnable, 60000);
            }




            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
                String message = "SSL Certificate error.";
                switch (error.getPrimaryError()) {
                    case SslError.SSL_UNTRUSTED:
                        message = "The certificate authority is not trusted.";
                        break;
                    case SslError.SSL_EXPIRED:
                        message = "The certificate has expired.";
                        break;
                    case SslError.SSL_IDMISMATCH:
                        message = "The certificate hostname does not match the website.";
                        break;
                    case SslError.SSL_NOTYETVALID:
                        message = "The certificate is not yet valid.";
                        break;
                }
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("Security Warning")
                        .setMessage(message + " This could mean someone is trying to intercept your data. Do you want to proceed anyway?")
                        .setPositiveButton("Proceed", (dialog, which) -> handler.proceed())
                        .setNegativeButton("Cancel", (dialog, which) -> handler.cancel())
                        .setCancelable(false)
                        .show();
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                cancelTimeout(view);
                if (currentUrl != null && tabMap.get(currentUrl) == view) {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                }
                failingUrls.put(view, failingUrl);
                showErrorPage(view, "Network Error", description);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                cancelTimeout(view);
                if (currentUrl != null && tabMap.get(currentUrl) == view) {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    updateSwipeRefreshState(view);
                }

                // We now scan for media dynamically when the user clicks the Grab Media button

                String oldUrl = null;
                int tabIndex = -1;

                // Try to match the current tab first to avoid desync
                if (currentPosition >= 0 && currentPosition < siteList.size()) {
                    String u = siteList.get(currentPosition);
                    if (tabMap.get(u) == view) {
                        oldUrl = u;
                        tabIndex = currentPosition;
                    }
                }

                if (tabIndex == -1) {
                    for (int i = 0; i < siteList.size(); i++) {
                        String u = siteList.get(i);
                        if (tabMap.get(u) == view) {
                            oldUrl = u;
                            tabIndex = i;
                            break;
                        }
                    }
                }

                boolean isIncognito = (oldUrl != null && incognitoTabs.contains(oldUrl));

                if (!url.startsWith("home://") && !isIncognito && !url.startsWith("data:text/html")) {
                    addToHistory(url);
                }

                if (oldUrl != null && !oldUrl.equals(url) && tabIndex != -1) {
                    siteList.set(tabIndex, url);

                    // Check if old URL is still used by other tabs before removing from map
                    boolean isOldUrlShared = false;
                    for (int i = 0; i < siteList.size(); i++) {
                        if (i != tabIndex && siteList.get(i).equals(oldUrl)) {
                            isOldUrlShared = true;
                            break;
                        }
                    }

                    if (!isOldUrlShared && !oldUrl.equals(HOME_URL) && !oldUrl.equals(QUICK_ACCESS_MESSAGES_URL) && !oldUrl.startsWith(INCOGNITO_HOME_URL) && !oldUrl.equals(WORKSPACE_URL)) {
                        tabMap.remove(oldUrl);
                        Bitmap preview = tabPreviews.remove(oldUrl);
                        if (preview != null) {
                            tabPreviews.put(url, preview);
                        }
                    }

                    tabMap.put(url, view);

                    if (tabIndex == currentPosition) {
                        currentUrl = url;
                    }

                    if (incognitoTabs.contains(oldUrl)) {
                        incognitoTabs.remove(oldUrl);
                        incognitoTabs.add(url);
                        sitesAdapter.setIncognitoTabs(incognitoTabs);
                    }

                    if (workspaceTabs.contains(oldUrl)) {
                        workspaceTabs.remove(oldUrl);
                        if (!url.startsWith("home://")) {
                            workspaceTabs.add(url);
                        }
                        if (sitesAdapter != null) {
                            sitesAdapter.setWorkspaceTabs(workspaceTabs);
                        }
                    }

                    if (searchSitesAdapter.getSelectedSites().contains(oldUrl)) {
                        searchSitesAdapter.getSelectedSites().remove(oldUrl);
                        searchSitesAdapter.getSelectedSites().add(url);
                    }

                    sitesAdapter.notifyItemChanged(tabIndex);
                    updateTabSwitcherList();
                    searchSitesAdapter.updateFilteredList();
                    saveSites();
                }
            }
        });

        wv.addJavascriptInterface(new WebAppInterface(this), "SocialOneNative");
        setupWebView(wv);

        if (incognitoTabs.contains(url)) {
            wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            wv.getSettings().setSaveFormData(false);
            wv.getSettings().setSavePassword(false);
        }

        if (!tabStateManager.restoreWebViewState(url, wv, incognitoTabs)) {
            wv.loadUrl(url);
        }

        wv.setAlpha(0f);
        wv.setVisibility(View.GONE);
        webViewContainer.addView(wv);
        tabMap.put(url, wv);
    }

    /**
     * JavaScript Interface to allow the error page to call back into Android.
     */
    private static class WebAppInterface {
        private final WeakReference<MainActivity> activityRef;

        WebAppInterface(MainActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @JavascriptInterface
        public void retry() {
            MainActivity activity = activityRef.get();
            if (activity == null) return;

            activity.mainHandler.post(() -> {
                for (Map.Entry<String, View> entry : activity.tabMap.entrySet()) {
                    if (entry.getValue() instanceof WebView) {
                        WebView wv = (WebView) entry.getValue();
                        String originalUrl = activity.failingUrls.get(wv);
                        if (originalUrl != null) {
                            wv.loadUrl(originalUrl);
                            activity.failingUrls.remove(wv);
                            return;
                        }
                    }
                }
                // Fallback if URL mapping is lost
                if (activity.currentUrl != null) {
                    activity.onSiteClick(activity.siteList.indexOf(activity.currentUrl));
                }
            });
        }

        @JavascriptInterface
        public void onMediaFound(String url) {
            MainActivity activity = activityRef.get();
            if (activity == null || url == null || url.isEmpty()) return;

            activity.mainHandler.post(() -> {
                if (!activity.detectedMediaUrls.contains(url)) {
                    activity.detectedMediaUrls.add(url);
                }
            });
        }
    }

    /**
     * Shows a custom error page in the WebView when a load fails.
     */
    private void showErrorPage(WebView view, String title, String description) {
        String failingUrl = view.getUrl();
        if (failingUrl != null && !failingUrl.startsWith("data:text/html")) {
            failingUrls.put(view, failingUrl);
        }

        String html = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>body { font-family: sans-serif; background-color: #FFFFFF; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; padding: 20px; text-align: center; color: #000000; }" +
                ".container { max-width: 400px; }" +
                "h1 { font-size: 24px; margin-bottom: 12px; }" +
                "p { font-size: 16px; color: #666666; margin-bottom: 24px; }" +
                ".btn { background-color: #000000; color: #FFFFFF; padding: 12px 24px; border-radius: 8px; text-decoration: none; display: inline-block; font-weight: bold; border: none; cursor: pointer; }" +
                "</style></head><body><div class='container'>" +
                "<h1>" + title + "</h1>" +
                "<p>" + description + "</p>" +
                "<button class='btn' onclick='SocialOneNative.retry()'>Try Again</button>" +
                "</div></body></html>";
        view.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }



    /**
     * Switches the UI to show the WebView (or Dashboard Fragment) associated with a URL.
     * @param url The URL of the tab to display.
     */
    private void showWebView(String url) {
        hideKeyboard();
        if (url == null) return;

        boolean isHome = url.equals(HOME_URL);
        boolean isQuickAccess = url.equals(QUICK_ACCESS_MESSAGES_URL);
        boolean isIncognitoHome = url.startsWith(INCOGNITO_HOME_URL);
        boolean isWorkspace = url.equals(WORKSPACE_URL);
        boolean isDashboard = isHome || isQuickAccess || isIncognitoHome || isWorkspace;
        
        boolean wasDashboard = currentUrl != null && currentUrl.startsWith("home://");

        View newTab = tabMap.get(url);
        if (newTab == null && !isDashboard) {
            createWebView(url);
            newTab = tabMap.get(url);
        }

        if (currentUrl != null && currentUrl.equals(url)) {
            // Pulse animation for identical view switch
            View targetView = isDashboard ? fragmentContainer : newTab;
            if (targetView != null) {
                targetView.setVisibility(View.VISIBLE);
                targetView.bringToFront();
                targetView.animate()
                        .alpha(0.6f)
                        .scaleX(0.98f)
                        .scaleY(0.98f)
                        .setDuration(ANIM_DURATION / 2)
                        .withEndAction(() -> targetView.animate()
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(ANIM_DURATION / 2)
                                .start())
                        .start();
            }
            return;
        }

        if (currentUrl != null) {
            final View oldTab = wasDashboard ? fragmentContainer : tabMap.get(currentUrl);
            if (oldTab != null) {
                tabStateManager.capturePreview(currentUrl, oldTab, tabPreviews);

                if (oldTab instanceof WebView && !sitesAdapter.isLocked(currentUrl)) {
                    ((WebView) oldTab).onPause();
                }

                // Only animate hiding the container if we are actually switching containers
                if (!(wasDashboard && isDashboard)) {
                    oldTab.animate()
                            .alpha(0f)
                            .scaleX(0.95f)
                            .scaleY(0.95f)
                            .setDuration(ANIM_DURATION)
                            .withEndAction(() -> {
                                oldTab.setVisibility(View.GONE);
                                oldTab.setScaleX(1.0f);
                                oldTab.setScaleY(1.0f);
                            })
                            .start();
                }
            }
        }

        // Manage Dashboard Fragments
        androidx.fragment.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (homeFragment != null) ft.hide(homeFragment);
        if (quickAccessFragment != null) ft.hide(quickAccessFragment);
        if (incognitoFragment != null) ft.hide(incognitoFragment);
        if (workspaceFragment != null) ft.hide(workspaceFragment);
        
        if (isDashboard) {
            fragmentContainer.setVisibility(View.VISIBLE);
            fragmentContainer.bringToFront();
            
            if (!wasDashboard) {
                // Animate showing the container if we are coming from a WebView
                fragmentContainer.setAlpha(0f);
                fragmentContainer.setScaleX(1.05f);
                fragmentContainer.setScaleY(1.05f);

                fragmentContainer.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(ANIM_DURATION)
                        .start();
            } else {
                // Switching between different dashboard sub-pages
                // Add a subtle cross-fade transition effect
                fragmentContainer.animate()
                        .alpha(0.6f)
                        .scaleX(0.98f)
                        .scaleY(0.98f)
                        .setDuration(ANIM_DURATION / 2)
                        .withEndAction(() -> {
                            // Actually swap the fragments here while partially hidden
                            androidx.fragment.app.FragmentTransaction ftInner = getSupportFragmentManager().beginTransaction();
                            ftInner.hide(homeFragment).hide(quickAccessFragment).hide(incognitoFragment).hide(workspaceFragment);
                            
                            if (isHome) ftInner.show(homeFragment);
                            else if (isQuickAccess) ftInner.show(quickAccessFragment);
                            else if (isIncognitoHome) ftInner.show(incognitoFragment);
                            else if (isWorkspace) {
                                if (workspaceFragment != null) workspaceFragment.setCurrentOpenTabs(siteList);
                                ftInner.show(workspaceFragment);
                            }
                            ftInner.commit();

                            fragmentContainer.animate()
                                    .alpha(1f)
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(ANIM_DURATION / 2)
                                    .start();
                        })
                        .start();
                
                // Return early as the animation endAction handles the fragment transaction and reset
                currentUrl = url;
                swipeRefreshLayout.setVisibility(View.GONE);
                swipeRefreshLayout.setEnabled(false);
                switchDesktopSite.setChecked(false);
                switchDesktopSite.setEnabled(false);
                progressBar.setVisibility(View.GONE);
                return;
            }

            if (isHome) ft.show(homeFragment);
            else if (isQuickAccess) ft.show(quickAccessFragment);
            else if (isIncognitoHome) ft.show(incognitoFragment);
            else if (isWorkspace) {
                if (workspaceFragment != null) {
                    workspaceFragment.setCurrentOpenTabs(siteList);
                }
                ft.show(workspaceFragment);
            }
            
            ft.commit();

            swipeRefreshLayout.setEnabled(false);
            switchDesktopSite.setChecked(false);
            switchDesktopSite.setEnabled(false);
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
        } else {
            ft.commit();
            fragmentContainer.setVisibility(View.GONE);
            
            if (newTab != null) {
                if (newTab instanceof WebView) {
                    WebView wv = (WebView) newTab;
                    wv.onResume();
                    updateSwipeRefreshState(wv);

                    Boolean isDesktop = desktopModeMap.get(wv);
                    switchDesktopSite.setChecked(isDesktop != null && isDesktop);
                    switchDesktopSite.setEnabled(true);
                }

                newTab.setAlpha(0f);
                newTab.setScaleX(1.05f);
                newTab.setScaleY(1.05f);
                newTab.setVisibility(View.VISIBLE);
                newTab.bringToFront();

                newTab.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(ANIM_DURATION)
                        .start();
                
                newTab.requestFocus();
            }
        }

        currentUrl = url;
        swipeRefreshLayout.setVisibility(!url.startsWith("home://") ? View.VISIBLE : View.GONE);

        // Re-apply insets to handle "Fixed Boundary" for Home tab in Normal Mode
        View mainView = findViewById(R.id.main);
        if (mainView != null) ViewCompat.requestApplyInsets(mainView);
    }


    /**
     * Performs a global search across selected platform URLs.
     */
    private void performGlobalSearch(String query, Set<String> urls) {
        for (String url : urls) {
            String searchUrl = SearchEngineManager.getFormattedSearchUrl(url, query);

            View tabView = tabMap.get(url);

            // If the URL is not found in tabMap, it might be because the tab has navigated
            // or the mapping is stale. We check if any WebView in tabMap is the one we want.
            if (!(tabView instanceof WebView)) {
                // Try to find if this URL belongs to any open tab's history or is a base URL
                for (String openUrl : siteList) {
                    if (openUrl.equals(url)) {
                        tabView = tabMap.get(openUrl);
                        break;
                    }
                }
            }

            if (!(tabView instanceof WebView)) {
                createWebView(url);
                tabView = tabMap.get(url);
            }

            if (tabView instanceof WebView) {
                ((WebView) tabView).loadUrl(searchUrl);
            }
        }


        Toast.makeText(this, "Global search success", Toast.LENGTH_SHORT).show();
    }

    private void updateSwipeRefreshState(WebView wv) {
        if (wv == null || swipeRefreshLayout == null) {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setEnabled(false);
            return;
        }

        String url = wv.getUrl();
        if (url != null) {
            // Disable pull-to-refresh on sites with vertical-swipe navigation to avoid conflicts
            boolean isShortFormVideo =
                            url.contains("youtube.com/shorts") ||
                            url.contains("instagram.com/reels") ||
                            url.contains("tiktok.com") ||
                            url.contains("facebook.com/reels") ||
                            url.contains("facebook.com/watch") ||
                            url.contains("snapchat.com") ||
                            url.contains("pinterest.com") ||
                            url.contains("reddit.com") ||
                            url.contains("threads.net") ||
                            url.contains("x.com/i/videos") ||
                            url.contains("twitter.com/i/videos") ||
                            (url.contains("bing.com") && !url.contains("search?q="));

            if (isShortFormVideo) {
                swipeRefreshLayout.setEnabled(false);
                return;
            }
        }

        // We only enable the pull-to-refresh if the WebView reports it is at the very top.
        // This allows refresh on all sites while minimizing accidental triggers.
        swipeRefreshLayout.setEnabled(!wv.canScrollVertically(-1));
    }

    private void updateWorkspacesWithCloudData(List<Map<String, Object>> cloudWorkspaces) {
        if (cloudWorkspaces == null || cloudWorkspaces.isEmpty()) return;

        String localJson = preferenceManager.loadWorkspacesJson();
        try {
            JSONArray localArray = new JSONArray(localJson);
            Set<String> localNames = new HashSet<>();
            for (int i = 0; i < localArray.length(); i++) {
                localNames.add(localArray.getJSONObject(i).optString("name", ""));
            }

            boolean changed = false;
            for (Map<String, Object> remote : cloudWorkspaces) {
                String name = (String) remote.get("name");
                if (name != null && !localNames.contains(name)) {
                    JSONObject newW = new JSONObject();
                    newW.put("name", name);
                    newW.put("tabs", new JSONArray((List<String>) remote.get("tabs")));
                    localArray.put(newW);
                    changed = true;
                }
            }

            if (changed) {
                preferenceManager.saveWorkspacesJson(localArray.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUIWithCloudData(List<String> tabs, List<String> bookmarks, List<String> history, boolean clearExisting) {
        if (siteList == null || bookmarksList == null || historyList == null) return;

        boolean changed = false;
        if (tabs != null && !tabs.isEmpty()) {
            if (clearExisting) siteList.clear();
            for (String tab : tabs) {
                String normalized = "home://business".equals(tab) ? QUICK_ACCESS_MESSAGES_URL : tab;
                if (!siteList.contains(normalized)) {
                    siteList.add(normalized);
                    changed = true;
                }
            }
            if (!siteList.contains(HOME_URL)) {
                siteList.add(0, HOME_URL);
                changed = true;
            }
            if (changed || clearExisting) {
                preferenceManager.saveSites(siteList, incognitoTabs, workspaceTabs, HOME_URL, QUICK_ACCESS_MESSAGES_URL, INCOGNITO_HOME_URL);
                sitesAdapter.notifyDataSetChanged();
                updateTabSwitcherList();
                if (searchSitesAdapter != null) searchSitesAdapter.updateFilteredList();
                updateTabCountDisplay();
            }
        }

        if (bookmarks != null) {
            boolean bChanged = false;
            if (clearExisting) bookmarksList.clear();
            for (String b : bookmarks) {
                String normalized = "home://business".equals(b) ? QUICK_ACCESS_MESSAGES_URL : b;
                if (!bookmarksList.contains(normalized)) {
                    bookmarksList.add(normalized);
                    bChanged = true;
                }
            }
            if (bChanged || clearExisting) {
                preferenceManager.saveBookmarks(bookmarksList);
                if (homeFragment != null) homeFragment.notifyDataChanged();
            }
        }

        if (history != null) {
            boolean hChanged = false;
            if (clearExisting) historyList.clear();
            for (String h : history) {
                String normalized = "home://business".equals(h) ? QUICK_ACCESS_MESSAGES_URL : h;
                if (!historyList.contains(normalized)) {
                    historyList.add(normalized);
                    hChanged = true;
                }
            }
            if (hChanged || clearExisting) {
                preferenceManager.saveHistory(historyList);
                if (homeFragment != null) homeFragment.notifyDataChanged();
            }
        }

    }

    /**
     * Clears all local user data from SharedPreferences and active WebViews.
     */
    private void updateAuthUI(Button btnAuthAction) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            if (name == null || name.isEmpty()) name = user.getEmail();
            if (tvUserStatus != null) tvUserStatus.setText("Logged in as " + name);
            btnAuthAction.setText("Logout");
            btnAuthAction.setTextColor(Color.WHITE);
            btnAuthAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF5252")));

            // Sync guest data to newly logged in account if necessary
            if (firestoreManager != null) {
                firestoreManager.performInitialMigration(siteList, bookmarksList, historyList, preferenceManager.loadWorkspacesList());

                // Also trigger a load to merge any existing cloud data
                firestoreManager.loadUserData(new FirestoreManager.OnDataLoadedListener() {
                    @Override
                    public void onDataLoaded(List<String> tabs, List<String> bookmarks, List<String> history) {
                        updateUIWithCloudData(tabs, bookmarks, history, false);
                    }

                    @Override
                    public void onWorkspacesLoaded(List<Map<String, Object>> workspaces) {
                        updateWorkspacesWithCloudData(workspaces);
                    }

                    @Override public void onError(Exception e) { Log.e("MainActivity", "Sync error", e); }
                });
            }
        } else {
            if (tvUserStatus != null) tvUserStatus.setText("Logged in as Guest");
            btnAuthAction.setText("Sign In");
            btnAuthAction.setTextColor(getResources().getColor(R.color.button_text));
            btnAuthAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.button_background)));
        }
    }

    private void clearLocalData() {
        // Clear SharedPreferences
        sharedPreferences.edit().clear().apply();

        // Destroy and clear all WebViews
        for (Map.Entry<String, View> entry : tabMap.entrySet()) {
            if (entry.getValue() instanceof WebView) {
                WebView wv = (WebView) entry.getValue();
                wv.clearCache(true);
                wv.clearHistory();
                wv.destroy();
            }
        }
        tabMap.clear();
        tabPreviews.clear();
        tabStateManager.clearAllPreviewsFromDisk();
        tabStateManager.clearAllWebViewStates();
        siteList.clear();
        historyList.clear();
        bookmarksList.clear();
        incognitoTabs.clear();

        // Clear global WebView cookies and data
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        WebStorage.getInstance().deleteAllData();
    }

    /**
     * Updates the tab switcher panel height to fit the screen while keeping bottom bar and handle visible.
     */
    private void updateTabSwitcherHeight() {
        if (tabSwitcherPanel == null) return;

        View mainView = findViewById(R.id.main);
        int totalHeight = mainView.getHeight();
        if (totalHeight == 0) {
            totalHeight = getResources().getDisplayMetrics().heightPixels;
        }

        View controls = findViewById(R.id.controlsPanel);

        // Prioritize toolbar height to prevent squishing
        int bHeight = (bottomBar != null && bottomBar.getHeight() > 0) ? bottomBar.getHeight() : UIUtils.dpToPx(this, 80);
        int cHeight = (controls != null && controls.getVisibility() == View.VISIBLE) ? controls.getHeight() : 0;

        // Get stable system insets to ignore the keyboard height and tab-specific padding in calculations
        int navBarHeight = 0;
        int statusBarHeight = 0;
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(mainView);
        if (insets != null) {
            statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            navBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
        } else {
            statusBarHeight = mainView.getPaddingTop();
            navBarHeight = bottomUiContainer.getPaddingBottom();
        }

        // Account for handle height only if it's visible (Advanced Mode)
        int hHeight = (handleTouchArea != null && handleTouchArea.getVisibility() == View.VISIBLE)
                ? (handleTouchArea.getHeight() > 0 ? handleTouchArea.getHeight() : UIUtils.dpToPx(this, 40))
                : 0;

        View spacer = findViewById(R.id.hideModeSpacer);
        int sHeight = (spacer != null && spacer.getVisibility() == View.VISIBLE) ? spacer.getHeight() : 0;

        // Consistent Usable Height across all tabs (Total - Status - Nav)
        int usableHeight = totalHeight - statusBarHeight - navBarHeight;

        // Panel space = UsableHeight - Toolbar - Active Controls - Handle - Buffer
        int targetHeight = usableHeight - bHeight - cHeight -sHeight - UIUtils.dpToPx(this, 40);

        if (targetHeight < UIUtils.dpToPx(this, 70)) targetHeight = UIUtils.dpToPx(this, 70);

        ViewGroup.LayoutParams params = tabSwitcherPanel.getLayoutParams();
        if (params != null) {
            params.height = targetHeight;
            tabSwitcherPanel.setLayoutParams(params);
        }
    }

    /**
     * Updates the more options panel height to fit small screens while being scrollable.
     */
    private void updateMoreOptionsHeight() {
        if (moreOptionsPanel == null) return;

        View mainView = findViewById(R.id.main);
        int totalHeight = mainView.getHeight();
        if (totalHeight == 0) {
            totalHeight = getResources().getDisplayMetrics().heightPixels;
        }

        View controls = findViewById(R.id.controlsPanel);

        // Prioritize toolbar and system bar space to prevent squishing on small screens
        int bHeight = (bottomBar != null && bottomBar.getHeight() > 0) ? bottomBar.getHeight() : UIUtils.dpToPx(this, 80);
        int cHeight = (controls != null && controls.getVisibility() == View.VISIBLE) ? controls.getHeight() : 0;

        // Get stable system insets to ignore the keyboard height and tab-specific padding in calculations
        int navBarHeight = 0;
        int statusBarHeight = 0;
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(mainView);
        if (insets != null) {
            statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            navBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
        } else {
            statusBarHeight = mainView.getPaddingTop();
            navBarHeight = bottomUiContainer.getPaddingBottom();
        }

        int hHeight = (handleTouchArea != null && handleTouchArea.getVisibility() == View.VISIBLE)
                ? (handleTouchArea.getHeight() > 0 ? handleTouchArea.getHeight() : UIUtils.dpToPx(this, 40))
                : 0;

        // Consistent Usable Height across all tabs (Total - Status - Nav)
        int usableHeight = totalHeight - statusBarHeight - navBarHeight;

        // Panel space = UsableHeight - Toolbar - Active Controls - Handle - Buffer
        int targetHeight = usableHeight - bHeight - cHeight - hHeight - UIUtils.dpToPx(this, 40);

        if (targetHeight < UIUtils.dpToPx(this, 100)) targetHeight = UIUtils.dpToPx(this, 100);

        ViewGroup.LayoutParams params = moreOptionsPanel.getLayoutParams();
        if (params != null) {
            // Force measurement of content to decide between WRAP_CONTENT and fixed height
            int widthSpec = View.MeasureSpec.makeMeasureSpec(Math.max(0, mainView.getWidth() - UIUtils.dpToPx(this, 24)), View.MeasureSpec.EXACTLY);
            if (mainView.getWidth() == 0) {
                widthSpec = View.MeasureSpec.makeMeasureSpec(getResources().getDisplayMetrics().widthPixels - UIUtils.dpToPx(this, 24), View.MeasureSpec.EXACTLY);
            }
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            moreOptionsPanel.measure(widthSpec, heightSpec);
            int contentHeight = moreOptionsPanel.getMeasuredHeight();

            if (contentHeight > targetHeight) {
                params.height = targetHeight;
            } else {
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            }
            moreOptionsPanel.setLayoutParams(params);
        }
    }


    /**
     * Saves the current list of sites (excluding dashboard and incognito URLs) to SharedPreferences and Cloud.
     */
    private void saveSites() {
        preferenceManager.saveSites(siteList, incognitoTabs, workspaceTabs, HOME_URL, QUICK_ACCESS_MESSAGES_URL, INCOGNITO_HOME_URL);
    }


    /**
     * Saves browsing history to SharedPreferences and Cloud.
     */
    private void saveHistory() {
        preferenceManager.saveHistory(historyList);
    }


    /**
     * Saves bookmarks to SharedPreferences and Cloud.
     */
    private void saveBookmarks() {
        preferenceManager.saveBookmarks(bookmarksList);
    }

    private void saveSitePermission(String origin, String permission, boolean granted) {
        SharedPreferences perms = getSharedPreferences("SitePermissions", MODE_PRIVATE);
        String key = origin + "|" + permission;
        perms.edit().putBoolean(key, granted).apply();
    }

    /**
     * Adds a URL to the browsing history, maintaining a maximum size.
     */
    private void addToHistory(String url) {
        if (url == null || url.isEmpty() || incognitoTabs.contains(url)) return;
        historyList.remove(url);
        historyList.add(0, url);
        if (historyList.size() > 50) {
            historyList.remove(historyList.size() - 1);
        }
        saveHistory();
        if (homeFragment != null) {
            homeFragment.notifyDataChanged();
        }
    }



    private void showAppInfoDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_app_info, null);
        Button btnGotIt = dialogView.findViewById(R.id.btnGotIt);
        ImageButton btnClose = dialogView.findViewById(R.id.btnCloseGuide);

        UIUtils.setClickAnimation(this, btnGotIt);
        UIUtils.setClickAnimation(this, btnClose);

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnGotIt.setOnClickListener(v -> dialog.dismiss());
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showFindOnPageDialog() {
        isFindOnPageActive = true;
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_caution, null);
        TextView title = dialogView.findViewById(R.id.confirmTitle);
        TextView message = dialogView.findViewById(R.id.confirmMessage);
        Button btnFind = dialogView.findViewById(R.id.btnProceedConfirm);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelConfirm);

        title.setText(R.string.find_on_page);
        message.setVisibility(View.GONE);

        LinearLayout container = dialogView.findViewById(R.id.dialogContainer);
        EditText input = new EditText(this);
        input.setHint(R.string.find_hint);
        input.setSingleLine(true);
        input.setTextColor(getResources().getColor(R.color.primary_text));
        input.setHintTextColor(getResources().getColor(R.color.secondary_text));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(UIUtils.dpToPx(this, 20), 0, UIUtils.dpToPx(this, 20), UIUtils.dpToPx(this, 20));
        input.setLayoutParams(lp);
        container.addView(input, 1);

        btnFind.setText("Find");

        // Setup Clear button with high-visibility matching design
        com.google.android.material.button.MaterialButton btnClear = new com.google.android.material.button.MaterialButton(this);
        btnClear.setText("Clear");
        btnClear.setTextSize(12);
        btnClear.setAllCaps(false);
        btnClear.setCornerRadius(UIUtils.dpToPx(this, 8));
        btnClear.setPadding(UIUtils.dpToPx(this, 12), 0, UIUtils.dpToPx(this, 12), 0);
        btnClear.setMinWidth(0);
        btnClear.setMinimumWidth(0);
        btnClear.setHeight(UIUtils.dpToPx(this, 36));

        // Use explicit colors from the theme for maximum contrast in both modes
        int contrastBg = getResources().getColor(R.color.button_background);
        int contrastText = getResources().getColor(R.color.button_text);
        btnClear.setBackgroundTintList(android.content.res.ColorStateList.valueOf(contrastBg));
        btnClear.setTextColor(contrastText);
        btnClear.setRippleColor(android.content.res.ColorStateList.valueOf(contrastText).withAlpha(40));

        // Use a wrapper to position it at the top right of the title area
        RelativeLayout titleWrapper = new RelativeLayout(this);
        titleWrapper.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Move title into the wrapper
        container.removeView(title);
        RelativeLayout.LayoutParams titleLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.addRule(RelativeLayout.ALIGN_PARENT_START);
        titleLp.addRule(RelativeLayout.CENTER_VERTICAL);
        title.setLayoutParams(titleLp);
        titleWrapper.addView(title);

        // Add clear button to the wrapper
        RelativeLayout.LayoutParams clearLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, UIUtils.dpToPx(this, 36));
        clearLp.addRule(RelativeLayout.ALIGN_PARENT_END);
        clearLp.addRule(RelativeLayout.CENTER_VERTICAL);
        btnClear.setLayoutParams(clearLp);
        titleWrapper.addView(btnClear);

        // Add the wrapper back to the container at the top
        container.addView(titleWrapper, 0);

        UIUtils.setClickAnimation(this, btnFind);
        UIUtils.setClickAnimation(this, btnCancel);
        UIUtils.setClickAnimation(this, btnClear);

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.setOnDismissListener(d -> {
            isFindOnPageActive = false;
            View mainView = findViewById(R.id.main);
            if (mainView != null) ViewCompat.requestApplyInsets(mainView);
        });

        btnFind.setOnClickListener(v -> {
            String query = input.getText().toString().trim();
            if (!query.isEmpty()) {
                View currentView = tabMap.get(currentUrl);
                if (currentView instanceof WebView) {
                    ((WebView) currentView).findAllAsync(query);

                    // Force hide keyboard from the dialog's input first to prevent layout jitter
                    input.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(input.getWindowToken(), 0);

                    dialog.dismiss();

                    // Close the options panel with transition after a brief pause to ensure smooth UI update
                    mainHandler.postDelayed(this::hideAllPanels, 50);
                }
            }
        });

        btnClear.setOnClickListener(v -> {
            View currentView = tabMap.get(currentUrl);
            if (currentView instanceof WebView) {
                ((WebView) currentView).clearMatches();
                input.setText("");
                Toast.makeText(this, "Search cleared", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

    }

    /**
     * Checks if any of the overlay panels are currently visible.
     */
    private boolean isAnyPanelOpen() {
        return (controlsPanel != null && controlsPanel.getVisibility() == View.VISIBLE) ||
                (searchPanel != null && searchPanel.getVisibility() == View.VISIBLE) ||
                (moreOptionsPanel != null && moreOptionsPanel.getVisibility() == View.VISIBLE) ||
                (tabSwitcherPanel != null && tabSwitcherPanel.getVisibility() == View.VISIBLE);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (isAnyPanelOpen()) {
                Rect outRect = new Rect();
                bottomUiContainer.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    hideAllPanels();
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onConfigurationChanged(@NonNull android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Update Tab Switcher layout based on new orientation
        if (tabSwitcherRecyclerView != null) {
            tabSwitcherRecyclerView.setLayoutManager(new GridLayoutManager(this, getTabSwitcherSpanCount()));
        }

        updateTabSwitcherHeight();
        updateMoreOptionsHeight();

        if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE && !isTablet()) {
            boolean panelsToHide = (tabSwitcherPanel != null && tabSwitcherPanel.getVisibility() == View.VISIBLE) ||
                    (moreOptionsPanel != null && moreOptionsPanel.getVisibility() == View.VISIBLE) ||
                    (searchPanel != null && searchPanel.getVisibility() == View.VISIBLE);

            if (panelsToHide) {
                beginPanelTransition();
                if (tabSwitcherPanel != null) tabSwitcherPanel.setVisibility(View.GONE);
                if (moreOptionsPanel != null) moreOptionsPanel.setVisibility(View.GONE);
                if (searchPanel != null) searchPanel.setVisibility(View.GONE);

                boolean advancedAnim = sharedPreferences.getBoolean(ADVANCED_ANIM_KEY, false);
                if (advancedAnim && handleTouchArea != null) handleTouchArea.setVisibility(View.VISIBLE);
                updateDragHandleState();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkBiometricLock();
        // Resume current tab and all locked tabs
        for (Map.Entry<String, View> entry : tabMap.entrySet()) {
            if (entry.getValue() instanceof WebView) {
                String url = entry.getKey();
                if (url.equals(currentUrl) || sitesAdapter.isLocked(url)) {
                    ((WebView) entry.getValue()).onResume();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        // Reset authentication state so it locks again on resume if enabled
        isAuthenticated = false;

        // Pause all WebViews and save their state to maintain history across app restarts
        saveAllWebViews();
        for (View v : tabMap.values()) {
            if (v instanceof WebView) {
                ((WebView) v).onPause();
            }
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        sharedPreferences.edit().remove(LOCKED_SITES_KEY).apply();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (tabStateManager != null) tabStateManager.shutdown();
        for (String url : incognitoTabs) {
            View v = tabMap.get(url);
            if (v instanceof WebView) {
                ((WebView) v).clearCache(true);
                ((WebView) v).clearHistory();
            }
        }
        for (View v : tabMap.values()) {
            if (v instanceof WebView) {
                cancelTimeout((WebView) v);
                ((WebView) v).destroy();
            }
        }
        timeoutRunnables.clear();
        tabMap.clear();
        tabPreviews.clear();
        incognitoTabs.clear();
        super.onDestroy();
    }
}