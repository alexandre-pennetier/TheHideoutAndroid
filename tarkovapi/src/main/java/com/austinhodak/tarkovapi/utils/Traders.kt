package com.austinhodak.tarkovapi.utils

import android.content.Context
import com.austinhodak.tarkovapi.R
import com.austinhodak.tarkovapi.models.TraderInfo
import com.google.gson.Gson
import org.json.JSONObject

class Traders(context: Context) {

    var traders: MutableList<TraderInfo> = mutableListOf()

    init {
        loadTraders(context)
    }

    private fun loadTraders(context: Context) {
        if (traders.isNullOrEmpty()) {
            val string = context.resources.openRawResource(R.raw.traders).bufferedReader().use { it.readText() }
            val json = JSONObject(string)
            val gson = Gson()
            val order = listOf("prapor", "therapist", "fence", "skier", "peacekeeper", "mechanic", "ragman", "jaeger")
            order.mapTo(traders) { gson.fromJson(json.getJSONObject(it).toString(), TraderInfo::class.java) }
        }
    }

    fun getTrader(string: String): TraderInfo? = traders.find { it.locale.en.equals(string, true) }


}