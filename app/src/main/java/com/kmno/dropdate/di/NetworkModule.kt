package com.kmno.dropdate.di

import com.kmno.dropdate.BuildConfig
import com.kmno.dropdate.data.remote.anilist.AniListApi
import com.kmno.dropdate.data.remote.tmdb.TmdbApi
import com.kmno.dropdate.data.remote.tvmaze.TvMazeApi
// import com.kmno.dropdate.data.remote.trakt.TraktApi
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

// Register at https://trakt.tv/oauth/applications → replace with your Client ID
// private const val TRAKT_CLIENT_ID = "YOUR_TRAKT_CLIENT_ID_HERE"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = buildBaseClient()

    private fun buildBaseClient() = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS
            else HttpLoggingInterceptor.Level.NONE
        })
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
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

    /*
        @Provides @Singleton
        fun provideTraktApi(json: Json): TraktApi {
            val traktClient = buildBaseClient().newBuilder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .addHeader("Content-Type", "application/json")
                            .addHeader("trakt-api-version", "2")
                            .addHeader("trakt-api-key", TRAKT_CLIENT_ID)
                            .build()
                    )
                }
                .build()
            return buildRetrofit("https://api.trakt.tv/", traktClient, json).create(TraktApi::class.java)
        }
    */

    @Provides @Singleton
    fun provideAniListApi(client: OkHttpClient, json: Json): AniListApi =
        buildRetrofit("https://graphql.anilist.co/", client, json).create(AniListApi::class.java)

    @Provides @Singleton
    fun provideTvMazeApi(client: OkHttpClient, json: Json): TvMazeApi =
        buildRetrofit("https://api.tvmaze.com/", client, json).create(TvMazeApi::class.java)
}
