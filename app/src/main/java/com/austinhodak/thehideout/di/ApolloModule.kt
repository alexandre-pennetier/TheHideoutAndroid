package com.austinhodak.thehideout.di

import com.apollographql.apollo.ApolloClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object ApolloModule {
    @Singleton
    @Provides
    fun providesApolloClient(
        okHttpClient: OkHttpClient
    ): ApolloClient = ApolloClient.Builder()
        .serverUrl("https://api.tarkov.dev/graphql")
        .addHttpInterceptor(GameModeInterceptor)
        //.normalizedCache(normalizedCacheFactory)
        //.okHttpClient(okHttpClient)
        .build()
}
