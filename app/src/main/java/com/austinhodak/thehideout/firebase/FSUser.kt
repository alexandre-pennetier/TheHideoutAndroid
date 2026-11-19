package com.austinhodak.thehideout.firebase

import com.austinhodak.tarkovapi.room.models.Item
import timber.log.Timber
import java.io.Serializable

data class FSUser(
    val discordUsername: String? = null,
    val displayName: String? = null,
    val playerLevel: Int? = null,
    val progress: Progress? = null,
    val teams: HashMap<String, Boolean>? = null,
    val keys: HashMap<String, Boolean>? = null,
    // PvE zone progress and keys; see utils/GameMode.kt.
    val progressPve: Progress? = null,
    val keysPve: HashMap<String, Boolean>? = null,
    // PvP season progress and keys.
    val progressSeason: Progress? = null,
    val keysSeason: HashMap<String, Boolean>? = null
) : Serializable {

    fun getUsername() = if (!displayName.isNullOrEmpty()) displayName else if (!discordUsername.isNullOrEmpty()) discordUsername else "Teammate"
    fun hasKey(item: Item) = keys?.containsKey(item.id) == true

    data class Progress(
        val quests: HashMap<String, Quest>? = null,
        val questObjectives: HashMap<String, QuestObjective>? = null,
        val hideoutModules: HashMap<String, Hideout>? = null,
        val hideoutObjectives: HashMap<String, HideoutObjective>? = null,
    ) {
        fun isQuestCompleted(id: String) = quests?.get(id)?.completed == true
        fun isQuestCompleted(quest: com.austinhodak.tarkovapi.room.models.Quest) = quests?.get(quest.id)?.completed == true
        fun isQuestObjectiveCompleted(id: String) = questObjectives?.get(id)?.completed == true
        fun isQuestObjectiveCompleted(objective: com.austinhodak.tarkovapi.room.models.Quest.QuestObjective) = when { questObjectives?.get(objective.id)?.completed == true -> true; questObjectives?.get(objective.id)?.progress == objective.number -> true; else -> false }
        fun getQuestObjectiveProgress(id: String) = questObjectives?.get(id)?.progress ?: 0
        fun getQuestObjectiveProgress(objective: com.austinhodak.tarkovapi.room.models.Quest.QuestObjective) = questObjectives?.get(objective.id)?.progress ?: 0

        fun isHideoutModuleCompleted(id: String) = hideoutModules?.get(id)?.completed == true
        fun isHideoutModuleCompleted(id: Int?) = hideoutModules?.get(id?.toString())?.completed == true
        fun isHideoutObjectiveCompleted(id: String) = hideoutObjectives?.get(id)?.completed == true
        fun isHideoutObjectiveCompleted(objective: com.austinhodak.tarkovapi.models.Hideout.Module.Require) = when { hideoutObjectives?.get(objective.id.toString())?.completed == true -> true; hideoutObjectives?.get(objective.id.toString())?.progress == objective.quantity -> true; else -> false }
        fun getHideoutObjectiveProgress(id: String) = hideoutObjectives?.get(id)?.progress ?: 0
        fun geHideoutObjectiveProgress(objective: com.austinhodak.tarkovapi.models.Hideout.Module.Require) = hideoutObjectives?.get(objective.id.toString())?.progress ?: 0

        fun getCompletedQuestIDs() = quests?.filter { it.value.completed == true }?.map { it.key }
        fun getCompletedQuestObjectiveIDs() = questObjectives?.filter { it.value.completed == true }?.map { it.key }
        fun getCompletedHideoutIDs() = hideoutModules?.filter { it.value.completed == true }?.map { it.key }
        fun getCompletedHideoutObjectiveIDs() = hideoutObjectives?.filter { it.value.completed == true }?.map { it.key }

        init {
            Timber.d(getCompletedHideoutIDs().toString())
        }

        data class Quest(
            val completed: Boolean? = null,
            val timestamp: Long? = null,
        )

        data class QuestObjective(
            val completed: Boolean? = null,
            val timestamp: Long? = null,
            val progress: Int? = null,
        )

        data class Hideout(
            val completed: Boolean? = null,
            val timestamp: Long? = null,
        )

        data class HideoutObjective(
            val completed: Boolean? = null,
            val timestamp: Long? = null,
            val progress: Int? = null,
        )
    }
}
