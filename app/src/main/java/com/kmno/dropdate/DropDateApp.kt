package com.kmno.dropdate

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.svg.SvgDecoder
import com.kmno.dropdate.worker.AiringReminderWorker
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import javax.inject.Inject

@HiltAndroidApp
class DropDateApp :
    Application(),
    SingletonImageLoader.Factory,
    Configuration.Provider {
    @Inject lateinit var okHttpClient: Lazy<OkHttpClient>

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    override fun onCreate() {
        super.onCreate()
        AiringReminderWorker.schedule(WorkManager.getInstance(this))
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(this)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient.get() }))
                add(SvgDecoder.Factory())
            }.memoryCache {
                MemoryCache
                    .Builder()
                    .maxSizePercent(this, 0.25)
                    .build()
            }.diskCache {
                DiskCache
                    .Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }.build()
}
