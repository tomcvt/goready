package com.tomcvt.goready.ads

import android.util.Log
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView


@Composable
fun BottomBarBannerAdView(
    adUnitId: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            this.adUnitId = adUnitId
            adListener = object : com.google.android.gms.ads.AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    Log.d("AdTest", "Ad Loaded Successfully! (If you can't see it, it's a layout issue)")
                }

                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    super.onAdFailedToLoad(error)
                    // This is the error that actually matters
                    Log.e("AdTest", "Ad Failed to Load: ${error.code} - ${error.message}")
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    AndroidView(
        modifier = modifier.height(50.dp),
        factory = { adView }
    )
}

@Composable
fun BottomBarDynamicAdView(
    adUnitId: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    BoxWithConstraints(
        modifier = modifier.height(50.dp)
    ) {
        val widthDp = maxWidth
        val width = widthDp.value.toInt()
        val adSize =
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(LocalContext.current, width)
        val adView = remember {
            AdView(context).apply {
                setAdSize(adSize)
                this.adUnitId = adUnitId
                adListener = object : com.google.android.gms.ads.AdListener() {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        Log.d(
                            "AdTest",
                            "Ad Loaded Successfully! (If you can't see it, it's a layout issue)"
                        )
                    }

                    override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                        super.onAdFailedToLoad(error)
                        // This is the error that actually matters
                        Log.e("AdTest", "Ad Failed to Load: ${error.code} - ${error.message}")
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }

        AndroidView(
            modifier = modifier.height(50.dp),
            factory = { adView }
        )
    }
}