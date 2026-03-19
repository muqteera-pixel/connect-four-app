package com.yourname.connectfour;

import android.annotation.SuppressLint;
import android.os.Bundle;
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
    private int levelsSinceInterstitial = 0;

    @SuppressLint({"SetJavaScriptEnabled","AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    private class AdMobBridge {

        @JavascriptInterface
        public void showReward() {
            runOnUiThread(() -> {
                if (rewardedAd != null) {
                    rewardEarned = false;

                    rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            if (!rewardEarned) {
                                // Closed without watching — stay on screen no coins
                                webView.post(() ->
                                    webView.evaluateJavascript("adFailed();", null)
                                );
                            }
                            rewardedAd = null;
                            loadRewardedAd();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError e) {
                            // Ad failed — stay on screen no coins
                            webView.post(() ->
                                webView.evaluateJavascript("adFailed();", null)
                            );
                            rewardedAd = null;
                            loadRewardedAd();
                        }
                    });

                    rewardedAd.show(MainActivity.this, rewardItem -> {
                        // User watched full ad — give coins
                        rewardEarned = true;
                        webView.post(() ->
                            webView.evaluateJavascript("receiveReward();", null)
                        );
                    });

                } else {
                    // Ad not loaded — stay on screen
                    webView.post(() ->
                        webView.evaluateJavascript("adFailed();", null)
                    );
                    loadRewardedAd();
                }
            });
        }

        @JavascriptInterface
        public void showInterstitial() {
            runOnUiThread(() -> {
                levelsSinceInterstitial++;
                if (levelsSinceInterstitial >= 3 && interstitialAd != null) {
                    levelsSinceInterstitial = 0;
                    interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            interstitialAd = null;
                            loadInterstitialAd();
                        }
                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError e) {
                            interstitialAd = null;
                            loadInterstitialAd();
                        }
                    });
                    interstitialAd.show(MainActivity.this);
                } else if (interstitialAd == null) {
                    loadInterstitialAd();
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
