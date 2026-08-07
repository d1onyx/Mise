package com.d1onix.dishlab.feature.recipes.presentation.list

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.d1onix.dishlab.feature.recipes.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

@Composable
internal actual fun RecipeBannerAd(modifier: Modifier) {
    val context = LocalContext.current
    val activity = context.findActivity() ?: return
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val adSize = remember(context, screenWidthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
    }
    val adView = remember(context, adSize) {
        AdView(context).apply {
            adUnitId = context.getString(R.string.admob_recipe_banner_unit_id)
            // Adaptive banners use the available full width but keep their
            // standard, short height instead of the old 320×100 large unit.
            setAdSize(adSize)
        }
    }

    LaunchedEffect(activity, adView) {
        requestConsentThenLoadAd(activity, adView)
    }
    DisposableEffect(adView) {
        onDispose(adView::destroy)
    }
    AndroidView(factory = { adView }, modifier = modifier)
}

private fun requestConsentThenLoadAd(activity: Activity, adView: AdView) {
    val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
    val loadAdIfAllowed = {
        if (consentInformation.canRequestAds()) {
            MobileAds.initialize(activity) {
                adView.loadAd(AdRequest.Builder().build())
            }
        }
    }
    consentInformation.requestConsentInfoUpdate(
        activity,
        ConsentRequestParameters.Builder().build(),
        {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                loadAdIfAllowed()
            }
        },
        {
            // A valid decision from an earlier session can still permit an ad request.
            loadAdIfAllowed()
        },
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
