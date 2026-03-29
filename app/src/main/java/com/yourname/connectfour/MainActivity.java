package com.yourname.connectfour;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final String REWARDED_AD_ID     = "ca-app-pub-3702571447385802/2978977236";
    private static final String INTERSTITIAL_AD_ID = "ca-app-pub-3702571447385802/6136190860";

    private RewardedAd rewardedAd;
    private InterstitialAd interstitialAd;
    private boolean rewardedAdLoading = false;
    private boolean rewardEarned = false;
    private int levelsSinceRewarded = 0;

    @SuppressLint({"SetJavaScriptEnabled","AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Force true fullscreen
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, status -> {
            loadRewardedAd();
            loadInterstitialAd();
        });

        webView = findViewById(R.id.webView);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);
        ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.addJavascriptInterface(new AdMobBridge(), "AdMob");
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");
    }

    // ── Force ad to use entire window ──────────────────────────
    private void forceFullScreen() {
        runOnUiThread(() -> {
            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            );
        });
    }

    // ── Hide WebView completely before showing ad ───────────────
    private void hideWebView() {
        runOnUiThread(() -> {
            if (webView != null) {
                webView.setVisibility(View.GONE);
                webView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(0, 0));
            }
        });
    }

    // ── Restore WebView after ad closes ────────────────────────
    private void showWebView() {
        runOnUiThread(() -> {
            if (webView != null) {
                webView.setVisibility(View.VISIBLE);
                webView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                ));
            }
        });
    }

    private class AdMobBridge {

        // User taps Watch Ad — shows interstitial — gives +10 coins
        @JavascriptInterface
        public void showInterstitialForCoins() {
            runOnUiThread(() -> {
                if (interstitialAd != null) {
                    interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdShowedFullScreenContent() {
                            forceFullScreen();
                        }
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            showWebView();
                            webView.post(() ->
                                webView.evaluateJavascript("receiveInterstitialCoins();", null)
                            );
                            interstitialAd = null;
                            loadInterstitialAd();
                        }
                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError e) {
                            showWebView();
                            interstitialAd = null;
                            loadInterstitialAd();
                        }
                    });
                    hideWebView();
                    interstitialAd.show(MainActivity.this);
                } else {
                    loadInterstitialAd();
                }
            });
        }

        // Auto rewarded ad every 2 levels — gives +10 coins if watched
        @JavascriptInterface
        public void showRewardedAuto() {
            runOnUiThread(() -> {
                levelsSinceRewarded++;
                if (levelsSinceRewarded < 2) return;

                if (rewardedAd != null) {
                    levelsSinceRewarded = 0;
                    rewardEarned = false;

                    rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdShowedFullScreenContent() {
                            forceFullScreen();
                        }
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            showWebView();
                            rewardedAd = null;
                            loadRewardedAd();
                            if (!rewardEarned) {
                                webView.post(() ->
                                    webView.evaluateJavascript("adFailed();", null)
                                );
                            }
                        }
                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError e) {
                            showWebView();
                            rewardedAd = null;
                            loadRewardedAd();
                            webView.post(() ->
                                webView.evaluateJavascript("adFailed();", null)
                            );
                        }
                    });

                    hideWebView();

                    rewardedAd.show(MainActivity.this, rewardItem -> {
                        rewardEarned = true;
                        webView.post(() ->
                            webView.evaluateJavascript("receiveReward();", null)
                        );
                    });

                } else {
                    loadRewardedAd();
                }
            });
        }

        @JavascriptInterface
        public boolean isRewardedReady() {
            return rewardedAd != null;
        }
    }

    private void loadRewardedAd() {
        if (rewardedAdLoading) return;
        rewardedAdLoading = true;
        RewardedAd.load(this, REWARDED_AD_ID, new AdRequest.Builder().build(),
            new RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull RewardedAd ad) {
                    rewardedAd = ad;
                    rewardedAdLoading = false;
                    webView.post(() ->
                        webView.evaluateJavascript(
                            "if(window.onRewardedAdLoaded) onRewardedAdLoaded();", null)
                    );
                }
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError e) {
                    rewardedAd = null;
                    rewardedAdLoading = false;
                    webView.postDelayed(() -> loadRewardedAd(), 30000);
                }
            });
    }

    private void loadInterstitialAd() {
        InterstitialAd.load(this, INTERSTITIAL_AD_ID, new AdRequest.Builder().build(),
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd ad) {
                    interstitialAd = ad;
                }
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError e) {
                    interstitialAd = null;
                    webView.postDelayed(() -> loadInterstitialAd(), 60000);
                }
            });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
        forceFullScreen();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) webView.destroy();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
