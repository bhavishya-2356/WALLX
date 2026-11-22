package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

class WallxApplication : Application(), ImageLoaderFactory {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun newImageLoader(): ImageLoader {
        // Cap background fetch/decode parallelism to 2 threads so decoding never competes
        // with the UI thread, touch input, or RenderThread scroll frames on any device.
        val limitedIo = Dispatchers.IO.limitedParallelism(2)
        val limitedDecode = Dispatchers.Default.limitedParallelism(2)

        return ImageLoader.Builder(this)
            .fetcherDispatcher(limitedIo)
            .decoderDispatcher(limitedDecode)
            .transformationDispatcher(limitedDecode)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.35)
                    .build()
            }
            .allowHardware(true)
            .allowRgb565(false)
            .crossfade(false)
            .respectCacheHeaders(false)
            .build()
    }
}
