package com.kmno.dropdate

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.svg.SvgDecoder
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import javax.inject.Inject

@HiltAndroidApp
class DropDateApp : Application(), SingletonImageLoader.Factory {

    @Inject lateinit var okHttpClient: Lazy<OkHttpClient>

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient.get() }))
                add(SvgDecoder.Factory())
            }
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
