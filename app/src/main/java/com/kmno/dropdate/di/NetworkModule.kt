package com.kmno.dropdate.di

import com.kmno.dropdate.BuildConfig
import com.kmno.dropdate.data.remote.animeschedule.AnimeScheduleApi
import com.kmno.dropdate.data.remote.jikan.JikanApi
import com.kmno.dropdate.data.remote.tmdb.TmdbApi
import com.kmno.dropdate.data.remote.tvmaze.TvMazeApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS
                        else HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

    private fun buildRetrofit(baseUrl: String, client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton
    fun provideTmdbApi(client: OkHttpClient, json: Json): TmdbApi =
        buildRetrofit("https://api.themoviedb.org/3/", client, json).create(TmdbApi::class.java)

    @Provides @Singleton
    fun provideTvMazeApi(client: OkHttpClient, json: Json): TvMazeApi =
        buildRetrofit("https://api.tvmaze.com/", client, json).create(TvMazeApi::class.java)

    @Provides @Singleton
    fun provideJikanApi(client: OkHttpClient, json: Json): JikanApi =
        buildRetrofit("https://api.jikan.moe/v4/", client, json).create(JikanApi::class.java)

    @Provides @Singleton
    fun provideAnimeScheduleApi(client: OkHttpClient, json: Json): AnimeScheduleApi =
        buildRetrofit("https://animeschedule.net/api/v3/", client, json).create(AnimeScheduleApi::class.java)
}
