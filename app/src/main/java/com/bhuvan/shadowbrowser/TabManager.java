package com.bhuvan.shadowbrowser;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Message;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/** Creates, switches and closes tabs. All WebView setup lives here. */
public class TabManager {

    public interface Listener {
        /** Title, url, progress or navigation state of a tab changed. */
        void onTabUpdated(Tab tab);

        /** The visible tab changed. */
        void onTabSwitched(Tab tab);

        /** A tab was added or closed. */
        void onTabsChanged(int count);
    }

    private final Context context;
    private final FrameLayout container;
    private final Listener listener;
    private final List<Tab> tabs = new ArrayList<>();
    private int currentIndex = -1;
    private int nextId = 1;

    public TabManager(Context context, FrameLayout container, Listener listener) {
        this.context = context;
        this.container = container;
        this.listener = listener;
    }

    // ---------- Public API ----------

    public Tab addTab(String url) {
        WebView webView = new WebView(context);
        Tab tab = new Tab(nextId++, webView);
        setupWebView(tab);
        tabs.add(tab);
        listener.onTabsChanged(tabs.size());
        switchTo(tabs.size() - 1);
        if (url != null) {
            webView.loadUrl(url);
        }
        return tab;
    }

    public void switchTo(int index) {
        if (index < 0 || index >= tabs.size()) return;
        currentIndex = index;
        Tab tab = tabs.get(index);
        container.removeAllViews();
        container.addView(tab.webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        listener.onTabSwitched(tab);
    }

    public void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        boolean wasCurrent = (index == currentIndex);
        Tab tab = tabs.remove(index);
        destroy(tab);

        if (tabs.isEmpty()) {
            currentIndex = -1;
            addTab(Config.HOME_URL);
            return;
        }

        if (index < currentIndex) currentIndex--;
        listener.onTabsChanged(tabs.size());
        if (wasCurrent) {
            switchTo(Math.min(index, tabs.size() - 1));
        }
    }

    public Tab current() {
        return (currentIndex >= 0 && currentIndex < tabs.size()) ? tabs.get(currentIndex) : null;
    }

    public int currentIndex() {
        return currentIndex;
    }

    public int size() {
        return tabs.size();
    }

    public Tab get(int index) {
        return tabs.get(index);
    }

    public void destroyAll() {
        for (Tab tab : tabs) destroy(tab);
        tabs.clear();
        currentIndex = -1;
    }

    // ---------- Internals ----------

    private void destroy(Tab tab) {
        ViewGroup parent = (ViewGroup) tab.webView.getParent();
        if (parent != null) parent.removeView(tab.webView);
        tab.webView.destroy();
    }

    private void setupWebView(final Tab tab) {
        WebView webView = tab.webView;

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setAllowFileAccess(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("about:")) {
                    return false;
                }
                // tel:, mailto:, intent: and other schemes go to other apps
                try {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception ignored) {
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                tab.url = url;
                tab.progress = 0;
                listener.onTabUpdated(tab);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                tab.url = url;
                tab.progress = 100;
                if (view.getTitle() != null) tab.title = view.getTitle();
                listener.onTabUpdated(tab);
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                tab.url = url;
                listener.onTabUpdated(tab);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                tab.progress = newProgress;
                listener.onTabUpdated(tab);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                tab.title = title;
                listener.onTabUpdated(tab);
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog,
                                          boolean isUserGesture, Message resultMsg) {
                Tab newTab = addTab(null);
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newTab.webView);
                resultMsg.sendToTarget();
                return true;
            }
        });
    }
          }
