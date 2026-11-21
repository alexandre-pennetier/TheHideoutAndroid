package com.austinhodak.thehideout.utils

import com.austinhodak.thehideout.local.LocalRef
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent

fun log(event: String, itemID: String, itemName: String, contentType: String) {
    Firebase.analytics.logEvent(event) {
        param(FirebaseAnalytics.Param.ITEM_ID, itemID)
        param(FirebaseAnalytics.Param.ITEM_NAME, itemName)
        param(FirebaseAnalytics.Param.CONTENT_TYPE, contentType)
    }
}

fun logNotification(event: String, itemName: String, contentType: String) {
    Firebase.analytics.logEvent(event) {
        param(FirebaseAnalytics.Param.ITEM_NAME, itemName)
        param(FirebaseAnalytics.Param.CONTENT_TYPE, contentType)
    }
}

fun logScreen(name: String) {
    Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
        param(FirebaseAnalytics.Param.SCREEN_NAME, name)
        param(FirebaseAnalytics.Param.SCREEN_CLASS, name)
    }
}

fun userRefTracker(ref: String? = null) = LocalRef(ref.orEmpty().trim('/'))
