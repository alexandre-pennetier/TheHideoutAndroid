package com.austinhodak.thehideout.di

import com.apollographql.apollo.api.http.HttpBody
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import com.austinhodak.tarkovapi.UserSettingsModel
import okio.Buffer
import okio.BufferedSink
import org.json.JSONObject

/**
 * Adds `gameMode: "pve"` to the variables of every operation that declares `$gameMode`, so the PvE profile
 * applies to all queries (both Apollo modules) without threading it through each call site.
 */
object GameModeInterceptor : HttpInterceptor {
    override suspend fun intercept(
        request: HttpRequest,
        chain: HttpInterceptorChain,
    ): HttpResponse {
        val body = request.body
        if (!UserSettingsModel.gameProfile.value.pveApi || body == null) return chain.proceed(request)

        val json = JSONObject(Buffer().also { body.writeTo(it) }.readUtf8())
        if (json.optString("query").contains("\$gameMode").not()) return chain.proceed(request)
        val variables = json.optJSONObject("variables") ?: JSONObject().also { json.put("variables", it) }
        variables.put("gameMode", "pve")

        val bytes = json.toString().toByteArray()
        val newBody =
            object : HttpBody {
                override val contentType = body.contentType
                override val contentLength = bytes.size.toLong()

                override fun writeTo(bufferedSink: BufferedSink) {
                    bufferedSink.write(bytes)
                }
            }
        return chain.proceed(request.newBuilder().body(newBody).build())
    }
}
