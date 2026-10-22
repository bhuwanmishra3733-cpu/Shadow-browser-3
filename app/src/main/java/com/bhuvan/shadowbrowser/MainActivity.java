package com.bhuvan.shadowbrowser;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/** The browser screen: URL bar, web content, bottom controls. */
public class MainActivity extends AppCompatActivity implements TabManager.Listener {

    private TabManager tabManager;
    private EditText urlBar;
    private ProgressBar progressBar;
    private ImageButton btnBack, btnForward, btnRefresh, btnHome;
    private TextView btnTabs;

    // ---------- Lifecycle ----------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        urlBar = findViewById(R.id.urlBar);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);
        btnForward = findViewById(R.id.btnForward);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnHome = findViewById(R.id.btnHome);
        btnTabs = findViewById(R.id.btnTabs);

        tabManager = new TabManager(this, (FrameLayout) findViewById(R.id.webContainer), this);

        setupUrlBar();
        setupButtons();
        setupBackHandling();

        String startUrl = getUrlFromIntent(getIntent());
        tabManager.addTab(startUrl != null ? startUrl : Config.HOME_URL);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String url = getUrlFromIntent(intent);
        if (url != null) tabManager.addTab(url);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Tab tab = tabManager.current();
        if (tab != null) tab.webView.onResume();
    }

    @Override
    protected void onPause() {
        Tab tab = tabManager.current();
        if (tab != null) tab.webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        tabManager.destroyAll();
        super.onDestroy();
    }

    // ---------- Setup ----------

    private void setupUrlBar() {
        urlBar.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_GO || enter) {
                loadFromUrlBar();
                return true;
            }
            return false;
        });

        urlBar.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                Tab tab = tabManager.current();
                if (tab != null) urlBar.setText(tab.url);
            }
        });
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> {
            Tab tab = tabManager.current();
            if (tab != null && tab.webView.canGoBack()) tab.webView.goBack();
        });

        btnForward.setOnClickListener(v -> {
            Tab tab = tabManager.current();
            if (tab != null && tab.webView.canGoForward()) tab.webView.goForward();
        });

        btnRefresh.setOnClickListener(v -> {
            Tab tab = tabManager.current();
            if (tab == null) return;
            if (tab.progress < 100) tab.webView.stopLoading();
            else tab.webView.reload();
        });

        btnHome.setOnClickListener(v -> {
            Tab tab = tabManager.current();
            if (tab != null) tab.webView.loadUrl(Config.HOME_URL);
        });

        btnTabs.setOnClickListener(v -> showTabSwitcher());
    }

    private void setupBackHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Tab tab = tabManager.current();
                if (tab != null && tab.webView.canGoBack()) {
                    tab.webView.goBack();
                } else if (tabManager.size() > 1) {
                    tabManager.closeTab(tabManager.currentIndex());
                } else {
                    finish();
                }
            }
        });
    }

    // ---------- Actions ----------

    private void loadFromUrlBar() {
        Tab tab = tabManager.current();
        if (tab == null) return;
        tab.webView.loadUrl(UrlUtils.toUrl(urlBar.getText().toString()));
        findViewById(R.id.webContainer).requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(urlBar.getWindowToken(), 0);
    }

    private void showTabSwitcher() {
        View view = getLayoutInflater().inflate(R.layout.dialog_tabs, null);
        final ListView list = view.findViewById(R.id.tabList);
        Button newTab = view.findViewById(R.id.btnNewTab);

        final AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();

        list.setAdapter(new TabsAdapter(this, tabManager, new TabsAdapter.Actions() {
            @Override
            public void onSelect(int index) {
                tabManager.switchTo(index);
                dialog.dismiss();
            }

            @Override
            public void onClose(int index) {
                tabManager.closeTab(index);
                ((BaseAdapter) list.getAdapter()).notifyDataSetChanged();
            }
        }));

        newTab.setOnClickListener(v -> {
            tabManager.addTab(Config.HOME_URL);
            dialog.dismiss();
        });

        dialog.show();
    }

    private String getUrlFromIntent(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())
                && intent.getData() != null) {
            return intent.getData().toString();
        }
        return null;
    }

    // ---------- UI updates (TabManager.Listener) ----------

    @Override
    public void onTabUpdated(Tab tab) {
        if (tab == tabManager.current()) refreshUi(tab, false);
    }

    @Override
    public void onTabSwitched(Tab tab) {
        refreshUi(tab, true);
    }

    @Override
    public void onTabsChanged(int count) {
        btnTabs.setText(String.valueOf(count));
    }

    private void refreshUi(Tab tab, boolean forceUrl) {
        if (forceUrl || !urlBar.hasFocus()) urlBar.setText(tab.url);

        progressBar.setProgress(tab.progress);
        progressBar.setVisibility(tab.progress < 100 ? View.VISIBLE : View.GONE);

        setEnabledLook(btnBack, tab.webView.canGoBack());
        setEnabledLook(btnForward, tab.webView.canGoForward());

        btnRefresh.setImageResource(tab.progress < 100 ? R.drawable.ic_close : R.drawable.ic_refresh);
    }

    private void setEnabledLook(View button, boolean enabled) {
        button.setAlpha(enabled ? 1f : 0.35f);
    }
              }
