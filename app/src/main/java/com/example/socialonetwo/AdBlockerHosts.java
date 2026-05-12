package com.example.socialonetwo;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility class to manage and apply ad-blocking host lists.
 * Loads and processes host lists asynchronously to avoid blocking the UI thread.
 */
public class AdBlockerHosts {

    private static final String TAG = "AdBlocker";
    private static final String EASYLIST_URL = "https://easylist.to/easylist/easylist.txt";
    
    // Fast lookup for domain-based filters
    private static final Set<String> BLOCKED_DOMAINS = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static final Set<String> WHITELIST_DOMAINS = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    
    // Slower lookup for path-based patterns
    private static final List<String> URL_FILTERS = Collections.synchronizedList(new ArrayList<String>());
    private static final List<String> URL_WHITELIST = Collections.synchronizedList(new ArrayList<String>());

    private static boolean isLoading = false;

    /**
     * Loads filters from the official EasyList URL asynchronously.
     */
    public static void loadFromUrl() {
        if (isLoading) return;
        isLoading = true;

        new Thread(() -> {
            Log.d(TAG, "Starting to load filters from URL: " + EASYLIST_URL);
            try {
                URL url = new URL(EASYLIST_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.connect();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    try (InputStream is = connection.getInputStream()) {
                        parseFilters(is);
                    }
                } else {
                    Log.e(TAG, "Failed to load filters from URL. Response code: " + connection.getResponseCode());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error downloading ad filters", e);
            } finally {
                isLoading = false;
            }
        }).start();
    }

    /**
     * Loads filter lists from the asset file as a fallback.
     */
    public static void loadFromAssets(Context context) {
        new Thread(() -> {
            try (InputStream is = context.getAssets().open("ad_hosts.txt")) {
                parseFilters(is);
            } catch (IOException e) {
                Log.e(TAG, "Error loading ad filters from assets", e);
            }
        }).start();
    }

    private static void parseFilters(InputStream is) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            // We don't clear until we have a successful stream to avoid losing current filters on failure
            Set<String> newBlockedDomains = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
            Set<String> newWhitelistDomains = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
            List<String> newUrlFilters = Collections.synchronizedList(new ArrayList<String>());
            List<String> newUrlWhitelist = Collections.synchronizedList(new ArrayList<String>());

            int count = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("!") || line.startsWith("#")) continue;

                boolean isException = line.startsWith("@@");
                if (isException) {
                    line = line.substring(2);
                }

                // Handle domain-based filters: ||domain.com^
                if (line.startsWith("||") && line.endsWith("^")) {
                    String domain = line.substring(2, line.length() - 1).toLowerCase();
                    if (isException) {
                        newWhitelistDomains.add(domain);
                    } else {
                        newBlockedDomains.add(domain);
                    }
                }
                // Handle path-based filters or simple substrings
                else if (line.contains("/") || line.contains("*")) {
                    String pattern = line.toLowerCase();
                    if (isException) {
                        newUrlWhitelist.add(pattern);
                    } else {
                        newUrlFilters.add(pattern);
                    }
                }
                // Default to host-based blocking
                else {
                    // Strip IP address prefix if present (0.0.0.0 host.com)
                    if (line.contains(" ") || line.contains("\t")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 2 && (parts[0].matches("[0-9.]+") || parts[0].equals("::1"))) {
                            line = parts[1];
                        } else {
                            line = parts[0];
                        }
                    }

                    String host = line.toLowerCase();
                    if (isException) {
                        newWhitelistDomains.add(host);
                    } else {
                        newBlockedDomains.add(host);
                    }
                }
                count++;
            }

            // Atomically update the active lists
            BLOCKED_DOMAINS.clear();
            BLOCKED_DOMAINS.addAll(newBlockedDomains);
            WHITELIST_DOMAINS.clear();
            WHITELIST_DOMAINS.addAll(newWhitelistDomains);
            URL_FILTERS.clear();
            URL_FILTERS.addAll(newUrlFilters);
            URL_WHITELIST.clear();
            URL_WHITELIST.addAll(newUrlWhitelist);

            Log.d(TAG, "Loaded " + count + " filters (Domains: " + BLOCKED_DOMAINS.size() + ", URL Patterns: " + URL_FILTERS.size() + ")");
        } catch (IOException e) {
            Log.e(TAG, "Error parsing ad filters", e);
        }
    }

    /**
     * Checks if a resource request should be blocked based on the loaded filter lists.
     */
    public static boolean shouldBlock(Uri uri) {
        if (uri == null) return false;

        String urlString = uri.toString().toLowerCase();
        String host = uri.getHost();
        
        if (host != null) {
            host = host.toLowerCase();
            if (host.endsWith(".")) host = host.substring(0, host.length() - 1);

            // 1. Check Whitelist Domains first (High Priority)
            if (isDomainMatch(host, WHITELIST_DOMAINS)) {
                return false;
            }

            // 2. Check Blocked Domains
            if (isDomainMatch(host, BLOCKED_DOMAINS)) {
                return true;
            }
        }

        // 3. Check URL Whitelist patterns
        for (String pattern : URL_WHITELIST) {
            if (matches(urlString, pattern)) return false;
        }

        // 4. Check URL Block filters
        for (String pattern : URL_FILTERS) {
            if (matches(urlString, pattern)) {
                Log.d(TAG, "Blocked by URL filter: " + pattern + " -> " + urlString);
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a host or any of its parent domains match a set of filter domains.
     */
    private static boolean isDomainMatch(String host, Set<String> domainSet) {
        if (host == null || domainSet.isEmpty()) return false;
        
        // Exact match
        if (domainSet.contains(host)) return true;

        // Subdomain matching
        int index = host.indexOf('.');
        while (index != -1 && index < host.length() - 1) {
            String parent = host.substring(index + 1);
            if (domainSet.contains(parent)) return true;
            index = host.indexOf('.', index + 1);
        }
        return false;
    }

    /**
     * Simple pattern matching for URL filters.
     * Supports basic wildcard (*) matching.
     */
    private static boolean matches(String url, String pattern) {
        if (url == null || pattern == null) return false;
        
        if (pattern.contains("*")) {
            // Convert simple wildcard to regex
            // Example: /ads/*.js -> .*/ads/.*\.js.*
            try {
                String regex = ".*" + pattern.replace(".", "\\.").replace("*", ".*") + ".*";
                return url.matches(regex);
            } catch (Exception e) {
                // Fallback to simple contains if regex fails
                return url.contains(pattern.replace("*", ""));
            }
        }
        return url.contains(pattern);
    }
}
