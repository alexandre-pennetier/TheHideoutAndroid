package com.austinhodak.thehideout.utils

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.austinhodak.tarkovapi.GameProfile
import com.austinhodak.tarkovapi.UserSettingsModel
import com.austinhodak.tarkovapi.repository.TarkovRepo
import com.austinhodak.thehideout.firebase.FSUser
import com.austinhodak.thehideout.workmanager.PriceUpdateWorker

// Firestore fields of the active profile (PvP zone keeps the original names).
val progressField: String
    get() = UserSettingsModel.gameProfile.value.progressField

val keysField: String
    get() = UserSettingsModel.gameProfile.value.keysField

/** Exposes the active profile's progress and keys through the `progress` / `keys` fields readers use. */
fun FSUser.forActiveMode(): FSUser =
    when (UserSettingsModel.gameProfile.value) {
        GameProfile.PVP_ZONE -> this
        GameProfile.PVE_ZONE -> copy(progress = progressPve, keys = keysPve)
        GameProfile.PVP_SEASON -> copy(progress = progressSeason, keys = keysSeason)
    }

private const val CACHED_MODE_KEY = "cachedGameMode"

/**
 * The Room price, quest, barter and craft tables hold one API mode's data (regular or PvE). When the API mode
 * differs from what they were filled with, empty them and refresh. Returns true if a switch was applied.
 */
fun Context.applyGameModeToCache(tarkovRepo: TarkovRepo): Boolean {
    val mode = if (UserSettingsModel.gameProfile.value.pveApi) "pve" else "regular"
    val preferences = getSharedPreferences("tarkov", Context.MODE_PRIVATE)
    if (preferences.getString(CACHED_MODE_KEY, "regular") == mode) return false

    tarkovRepo.getPriceDao().nukeTable()
    tarkovRepo.getQuestDao().nukeTable()
    tarkovRepo.getBarterDao().nukeTable()
    tarkovRepo.getCraftDao().nukeTable()
    preferences.edit().putString(CACHED_MODE_KEY, mode).apply()
    WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<PriceUpdateWorker>().build())
    return true
}
