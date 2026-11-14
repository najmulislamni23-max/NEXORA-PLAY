package com.nexora.games;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class MainActivity extends Activity {
    private WebView webView;
    private RewardedAd rewardedAd;
    // Google's official SAMPLE rewarded-ad unit ID. Test only; replace before release.
    private static final String TEST_REWARDED_AD_UNIT =
            "ca-app-pub-3940256099942544/5224354917";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new GameBridge(), "AndroidGame");
        setContentView(webView);

        MobileAds.initialize(this, initializationStatus -> loadRewardedAd());
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void loadRewardedAd() {
        AdRequest request = new AdRequest.Builder().build();
        RewardedAd.load(this, TEST_REWARDED_AD_UNIT, request,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedAd ad) {
                        rewardedAd = ad;
                        runOnUiThread(() -> webView.evaluateJavascript(
                                "window.onRewardAdReady && window.onRewardAdReady(true)", null));
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        rewardedAd = null;
                        runOnUiThread(() -> webView.evaluateJavascript(
                                "window.onRewardAdReady && window.onRewardAdReady(false)", null));
                    }
                });
    }

    private class GameBridge {
        @JavascriptInterface
        public void showRewardedAd() {
            runOnUiThread(() -> {
                if (rewardedAd == null) {
                    Toast.makeText(MainActivity.this,
                            "বিজ্ঞাপন এখনো প্রস্তুত নয়। একটু পরে চেষ্টা করুন।",
                            Toast.LENGTH_SHORT).show();
                    loadRewardedAd();
                    return;
                }

                RewardedAd adToShow = rewardedAd;
                rewardedAd = null;
                adToShow.show(MainActivity.this, rewardItem -> {
                    int rewardAmount = rewardItem.getAmount();
                    webView.evaluateJavascript(
                            "window.addVirtualCoins(" + rewardAmount + ")", null);
                    loadRewardedAd();
                });
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
