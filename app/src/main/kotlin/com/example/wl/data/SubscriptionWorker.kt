package com.example.wl.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class SubscriptionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val client = OkHttpClient()
    private val repository = SettingsRepository(appContext)

    override suspend fun doWork(): Result {
        val subscriptions = listOf(
            Subscription("WhiteList Bypass", "https://raw.githubusercontent.com/Ogshty/WhiteList-Bypass/main/whitelist.txt")
        )

        var success = false
        for (subscription in subscriptions) {
            try {
                val request = Request.Builder().url(subscription.url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        // In a real app, we might want to cache these in a database
                        // For now, we just ensure they are fetchable
                        success = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return if (success) Result.success() else Result.retry()
    }
}
