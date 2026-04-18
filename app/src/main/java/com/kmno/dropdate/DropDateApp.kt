package com.kmno.dropdate

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath

@HiltAndroidApp
class DropDateApp : Application(), SingletonImageLoader.Factory {

    // OkHttpClient created directly here; will be refactored to @Inject in Task 10
    // once NetworkModule provides the singleton instance.
    private val okHttpClient: OkHttpClient by lazy { OkHttpClient() }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient })) }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(this, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .build()
}
