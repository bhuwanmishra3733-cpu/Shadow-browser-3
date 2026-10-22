package com.bhuvan.shadowbrowser;

import android.webkit.WebView;

/** One browser tab: its WebView plus the info the UI needs. */
public class Tab {
    public final int id;
    public final WebView webView;
    public String title = "";
    public String url = "";
    public int progress = 100;

    public Tab(int id, WebView webView) {
        this.id = id;
        this.webView = webView;
    }
}
