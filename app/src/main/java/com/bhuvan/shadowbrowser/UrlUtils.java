package com.bhuvan.shadowbrowser;

import android.net.Uri;

import java.util.Locale;

/** Turns whatever the user typed into a loadable URL or a search. */
public final class UrlUtils {
    private UrlUtils() {}

    public static String toUrl(String input) {
        if (input == null) return Config.HOME_URL;
        String s = input.trim();
        if (s.isEmpty()) return Config.HOME_URL;

        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("about:")) {
            return s;
        }

        boolean looksLikeHost = !s.contains(" ") && (s.contains(".") || lower.startsWith("localhost"));
        if (looksLikeHost) {
            return "https://" + s;
        }

        return Config.SEARCH_URL + Uri.encode(s);
    }
                                            }
