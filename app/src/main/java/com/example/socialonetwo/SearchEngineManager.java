package com.example.socialonetwo;

import android.net.Uri;

/**
 * Utility class to manage search engine configurations and URL generation.
 */
public class SearchEngineManager {

    public static final String[] SEARCH_ENGINE_NAMES = {
            "Google", "Bing", "Brave", "DuckDuckGo", "Startpage", "Ecosia"
    };

    public static final String[] SEARCH_ENGINE_URLS = {
            "https://www.google.com/search?q=",
            "https://www.bing.com/search?q=",
            "https://search.brave.com/search?q=",
            "https://duckduckgo.com/?q=",
            "https://www.startpage.com/do/dsearch?query=",
            "https://www.ecosia.org/search?q="
    };

    /**
     * Gets the base search URL for a given engine index.
     */
    public static String getSearchBaseUrl(int index) {
        if (index >= 0 && index < SEARCH_ENGINE_URLS.length) {
            return SEARCH_ENGINE_URLS[index];
        }
        return SEARCH_ENGINE_URLS[0]; // Default to Google
    }

    /**
     * Generates a platform-specific search URL for a given query based on the base URL.
     * This is used for global searches across different platforms.
     */
    public static String getFormattedSearchUrl(String baseUrl, String query) {
        String encodedQuery = Uri.encode(query);
        
        if (baseUrl.contains("google.com")) return "https://www.google.com/search?q=" + encodedQuery;
        if (baseUrl.contains("yandex.com")) return "https://yandex.com/search/?text=" + encodedQuery;
        if (baseUrl.contains("twitter.com") || baseUrl.contains("x.com")) return "https://twitter.com/search?q=" + encodedQuery;
        if (baseUrl.contains("facebook.com")) return "https://www.facebook.com/search/top/?q=" + encodedQuery;
        if (baseUrl.contains("instagram.com")) return "https://www.instagram.com/explore/search/keyword/?q=" + encodedQuery;
        if (baseUrl.contains("youtube.com")) return "https://www.youtube.com/results?search_query=" + encodedQuery;
        if (baseUrl.contains("bing.com")) return "https://www.bing.com/search?q=" + encodedQuery;
        if (baseUrl.contains("duckduckgo.com")) return "https://duckduckgo.com/?q=" + encodedQuery;
        if (baseUrl.contains("yahoo.com")) return "https://search.yahoo.com/search?p=" + encodedQuery;
        if (baseUrl.contains("baidu.com")) return "https://www.baidu.com/s?wd=" + encodedQuery;
        if (baseUrl.contains("reddit.com")) return "https://www.reddit.com/search/?q=" + encodedQuery;
        if (baseUrl.contains("amazon.com")) return "https://www.amazon.com/s?k=" + encodedQuery;
        if (baseUrl.contains("ebay.com")) return "https://www.ebay.com/sch/i.html?_nkw=" + encodedQuery;
        if (baseUrl.contains("wikipedia.org")) return "https://en.wikipedia.org/wiki/Special:Search?search=" + encodedQuery;
        if (baseUrl.contains("github.com")) return "https://github.com/search?q=" + encodedQuery;
        if (baseUrl.contains("stackoverflow.com")) return "https://stackoverflow.com/search?q=" + encodedQuery;
        if (baseUrl.contains("linkedin.com")) return "https://www.linkedin.com/search/results/all/?keywords=" + encodedQuery;
        if (baseUrl.contains("pinterest.com")) return "https://www.pinterest.com/search/pins/?q=" + encodedQuery;
        if (baseUrl.contains("tiktok.com")) return "https://www.tiktok.com/search?q=" + encodedQuery;
        if (baseUrl.contains("brave.com")) return "https://search.brave.com/search?q=" + encodedQuery;
        if (baseUrl.contains("ecosia.org")) return "https://www.ecosia.org/search?q=" + encodedQuery;
        
        return baseUrl + "/search?q=" + encodedQuery;
    }
}
