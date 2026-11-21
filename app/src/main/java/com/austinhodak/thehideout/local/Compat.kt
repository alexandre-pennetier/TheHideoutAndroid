package com.austinhodak.thehideout.local

/** Firestore-shaped markers so call sites keep reading like before: `FieldValue.delete()` / `increment(n)`. */
object FieldValue {
    fun delete(): Any = LocalStore.Delete

    fun increment(by: Long): Any = LocalStore.Increment(by)
}

/** Realtime-Database-shaped counter marker for [LocalRef.setValue]. */
object ServerValue {
    fun increment(by: Long): Any = LocalStore.Increment(by)
}

/** Kept so `set(map, SetOptions.merge())` call sites stay as they were; every write merges. */
object SetOptions {
    fun merge(): SetOptions = this
}

/** The user's progress document (`user` node): player level, per-profile progress and keys. */
object UserDoc {
    /** [field] is a dotted path such as `progress.quests.<id>`. */
    fun update(
        field: String,
        value: Any?,
    ) = LocalStore.set("user/" + LocalStore.dotted(field), value)

    fun set(
        values: Map<String, Any?>,
        @Suppress("UNUSED_PARAMETER") options: SetOptions = SetOptions,
    ) = LocalStore.merge("user", values)
}

/** A node under `tracker` (needed items, cart). */
class LocalRef(
    private val path: String,
) {
    fun setValue(value: Any?) = LocalStore.set("tracker/$path", value)

    fun removeValue() = LocalStore.remove("tracker/$path")
}

data class GeoPoint(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
)
