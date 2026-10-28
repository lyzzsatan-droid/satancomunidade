package com.feliplucas.satancomunidades;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * Activity principal do app Satan Comunidades.
 *
 * Visual inspirado no Discord: tema escuro, barra superior fixa com
 * indicador de conexão, "puxar pra atualizar" e tela de abertura (splash).
 * Mantém o login salvo entre aberturas via CookieManager.
 */
public class MainActivity extends AppCompatActivity {

    private static final String HOME_URL = "https://feliplucas152-creator.github.io/Satan./";
    private static final int FILE_PICKER_REQUEST = 1;

    // Paleta inspirada no Discord (dark theme)
    private static final int COLOR_BG = Color.parseColor("#1E1F22");       // fundo principal
    private static final int COLOR_SURFACE = Color.parseColor("#2B2D31"); // barra superior / cards
    private static final int COLOR_ACCENT = Color.parseColor("#5865F2"); // "blurple"
    private static final int COLOR_TEXT = Color.parseColor("#F2F3F5");
    private static final int COLOR_TEXT_MUTED = Color.parseColor("#949BA4");
    private static final int COLOR_ONLINE = Color.parseColor("#23A55A");
    private static final int COLOR_OFFLINE = Color.parseColor("#F23F42");

    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private View errorView;
    private View splashView;
    private View statusDot;
    private TextView statusText;
    private ValueCallback<Uri[]> uploadCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(COLOR_SURFACE);
        getWindow().setNavigationBarColor(COLOR_BG);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(COLOR_BG);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        content.addView(buildTopBar());

        webView = new WebView(this);
        webView.setBackgroundColor(COLOR_BG);

        swipeRefresh = new SwipeRefreshLayout(this);
        swipeRefresh.setColorSchemeColors(COLOR_ACCENT);
        swipeRefresh.setProgressBackgroundColorSchemeColor(COLOR_SURFACE);
        swipeRefresh.addView(webView);
        swipeRefresh.setOnRefreshListener(() -> webView.reload());

        LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        content.addView(swipeRefresh, webParams);

        root.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.getProgressDrawable().setColorFilter(COLOR_ACCENT, android.graphics.PorterDuff.Mode.SRC_IN);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, 8);
        progressParams.topMargin = dp(56); // logo abaixo da barra superior
        root.addView(progressBar, progressParams);

        errorView = buildErrorView();
        errorView.setVisibility(View.GONE);
        root.addView(errorView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        splashView = buildSplashView();
        root.addView(splashView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        setContentView(root);

        configureWebView();

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
            splashView.setVisibility(View.GONE);
        } else {
            loadHome();
        }
    }

    // ---------- UI ----------

    private View buildTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(COLOR_SURFACE);
        bar.setPadding(dp(16), dp(14), dp(16), dp(14));
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_launcher);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(28), dp(28));
        iconParams.rightMargin = dp(12);
        bar.addView(icon, iconParams);

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bar.addView(titleBlock, titleParams);

        TextView title = new TextView(this);
        title.setText("Satan Comunidades");
        title.setTextColor(COLOR_TEXT);
        title.setTextSize(16);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        titleBlock.addView(title);

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        titleBlock.addView(statusRow);

        statusDot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(8), dp(8));
        dotParams.rightMargin = dp(6);
        android.graphics.drawable.GradientDrawable dot = new android.graphics.drawable.GradientDrawable();
        dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        dot.setColor(COLOR_ONLINE);
        statusDot.setBackground(dot);
        statusRow.addView(statusDot, dotParams);

        statusText = new TextView(this);
        statusText.setText("Conectado");
        statusText.setTextColor(COLOR_TEXT_MUTED);
        statusText.setTextSize(12);
        statusRow.addView(statusText);

        return bar;
    }

    private View buildErrorView() {
        FrameLayout container = new FrameLayout(this);
        container.setBackgroundColor(COLOR_BG);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(32), 0, dp(32), 0);

        TextView emoji = new TextView(this);
        emoji.setText("💀");
        emoji.setTextSize(48);
        emoji.setGravity(Gravity.CENTER);
        content.addView(emoji);

        TextView text = new TextView(this);
        text.setText("Não foi possível conectar.\nVerifique sua internet e tente de novo.");
        text.setTextColor(COLOR_TEXT);
        text.setGravity(Gravity.CENTER);
        text.setPadding(0, dp(16), 0, dp(24));
        content.addView(text);

        Button retry = new Button(this);
        retry.setText("Tentar de novo");
        retry.setTextColor(Color.WHITE);
        retry.setAllCaps(false);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(COLOR_ACCENT);
        bg.setCornerRadius(dp(24));
        retry.setBackground(bg);
        retry.setPadding(dp(32), dp(12), dp(32), dp(12));
        retry.setOnClickListener(v -> loadHome());
        content.addView(retry);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        container.addView(content, params);
        return container;
    }

    private View buildSplashView() {
        FrameLayout container = new FrameLayout(this);
        container.setBackgroundColor(COLOR_BG);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        container.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_launcher);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(96), dp(96));
        logoParams.bottomMargin = dp(16);
        content.addView(logo, logoParams);

        TextView loading = new TextView(this);
        loading.setText("Entrando na comunidade...");
        loading.setTextColor(COLOR_TEXT_MUTED);
        content.addView(loading);

        return container;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    // ---------- WebView ----------

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportZoom(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                errorView.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                setOnlineStatus(true);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                CookieManager.getInstance().flush(); // grava o login no disco
                hideSplash();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                if (host != null && host.contains("feliplucas152-creator.github.io")) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Não foi possível abrir o link.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    errorView.setVisibility(View.VISIBLE);
                    setOnlineStatus(false);
                    hideSplash();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progressBar.setProgress(newProgress);
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                              FileChooserParams fileChooserParams) {
                uploadCallback = filePathCallback;
                try {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("image/*");
                    startActivityForResult(Intent.createChooser(intent, "Escolher foto"), FILE_PICKER_REQUEST);
                } catch (Exception e) {
                    uploadCallback = null;
                    Toast.makeText(MainActivity.this, "Não foi possível abrir a galeria.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });
    }

    private void loadHome() {
        if (isOnline()) {
            webView.loadUrl(HOME_URL);
        } else {
            errorView.setVisibility(View.VISIBLE);
            setOnlineStatus(false);
            hideSplash();
        }
    }

    private void setOnlineStatus(boolean online) {
        android.graphics.drawable.GradientDrawable dot = new android.graphics.drawable.GradientDrawable();
        dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        dot.setColor(online ? COLOR_ONLINE : COLOR_OFFLINE);
        statusDot.setBackground(dot);
        statusText.setText(online ? "Conectado" : "Sem conexão");
    }

    private void hideSplash() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> splashView.setVisibility(View.GONE), 300);
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm != null ? cm.getActiveNetworkInfo() : null;
        return info != null && info.isConnected();
    }

    // ---------- Ciclo de vida ----------

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_PICKER_REQUEST) {
            if (uploadCallback == null) return;
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    results = new Uri[]{data.getData()};
                }
            }
            uploadCallback.onReceiveValue(results);
            uploadCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }
}
