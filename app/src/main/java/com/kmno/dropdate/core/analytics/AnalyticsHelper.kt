package com.kmno.dropdate.core.analytics

import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.kmno.dropdate.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("CompositionLocalAllowlist")
val LocalAnalyticsHelper =
    staticCompositionLocalOf<AnalyticsHelper> {
        error("No AnalyticsHelper provided")
    }

@Singleton
class AnalyticsHelper
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        private val crashlytics = FirebaseCrashlytics.getInstance()

        init {
            val isEnabled = !BuildConfig.DEBUG
            firebaseAnalytics.setAnalyticsCollectionEnabled(isEnabled)
            crashlytics.setCrashlyticsCollectionEnabled(isEnabled)
        }

        fun logEvent(
            name: String,
            params: Bundle? = null,
        ) {
            firebaseAnalytics.logEvent(name, params)
        }

        fun logScreenView(
            screenName: String,
            screenClass: String? = null,
        ) {
            val params =
                Bundle().apply {
                    putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                    putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass ?: screenName)
                }
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params)
        }

        fun logException(throwable: Throwable) {
            if (!BuildConfig.DEBUG) {
                crashlytics.recordException(throwable)
            }
        }

        // Predefined professional events
        object Events {
            const val CONTENT_SELECTED = "content_selected"
            const val SEARCH = "search_query"
            const val FAVORITE_ADDED = "favorite_added"
            const val FAVORITE_REMOVED = "favorite_removed"
            const val APP_OPEN_FIRST_TIME = "app_first_open"
        }

        object Params {
            const val CONTENT_ID = "content_id"
            const val CONTENT_TYPE = "content_type"
            const val SEARCH_TERM = "search_term"
        }
    }
