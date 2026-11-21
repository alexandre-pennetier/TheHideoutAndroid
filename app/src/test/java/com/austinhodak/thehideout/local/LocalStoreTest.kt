package com.austinhodak.thehideout.local

import com.austinhodak.thehideout.firebase.FSUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocalStoreTest {
    @Before
    fun reset() {
        LocalStore.importJson("{}")
    }

    @Test
    fun mergeWritesNestedValuesAndIncrementsCounters() {
        UserDoc.set(mapOf("progress" to mapOf("quests" to mapOf("q1" to mapOf("completed" to true)))))
        UserDoc.update("progress.questObjectives.o1.progress", FieldValue.increment(1))
        UserDoc.update("progress.questObjectives.o1.progress", FieldValue.increment(1))

        val user = LocalStore.convert(LocalStore.get("user"), FSUser::class.java)!!
        assertTrue(user.progress!!.isQuestCompleted("q1"))
        assertEquals(2, user.progress!!.getQuestObjectiveProgress("o1"))
    }

    @Test
    fun removingTheLastChildPrunesEmptyParents() {
        LocalStore.set("tracker/items/i1/hideoutObjective/a", 3)
        LocalStore.remove("tracker/items/i1/hideoutObjective/a")

        assertNull(LocalStore.get("tracker/items/i1"))
        assertNull(LocalStore.get("tracker/items"))
    }

    @Test
    fun deleteMarkerInMergeRemovesOnlyThatKey() {
        UserDoc.set(mapOf("progress" to mapOf("quests" to mapOf("q1" to mapOf("completed" to true), "q2" to mapOf("completed" to true)))))
        UserDoc.set(mapOf("progress" to mapOf("quests" to mapOf("q1" to FieldValue.delete()))))

        assertNull(LocalStore.get("user/progress/quests/q1"))
        assertEquals(true, LocalStore.get("user/progress/quests/q2/completed"))
    }

    @Test
    fun exportThenImportRestoresEverythingAndBadJsonKeepsData() {
        LocalStore.set("tracker/cart/x", 4)
        val backup = LocalStore.exportJson()
        LocalStore.importJson("{}")
        assertNull(LocalStore.get("tracker/cart/x"))

        LocalStore.importJson(backup)
        assertEquals(4L, (LocalStore.get("tracker/cart/x") as Number).toLong())

        runCatching { LocalStore.importJson("[1,2") }
        assertEquals(4L, (LocalStore.get("tracker/cart/x") as Number).toLong())
    }
}
