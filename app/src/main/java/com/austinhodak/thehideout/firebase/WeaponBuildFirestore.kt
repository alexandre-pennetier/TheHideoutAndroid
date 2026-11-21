package com.austinhodak.thehideout.firebase

import com.austinhodak.thehideout.local.LocalStore
import java.io.Closeable
import java.io.Serializable
import java.util.UUID

data class WeaponBuildFirestore(
    val ammo: String? = null,
    val weapon: _Weapon? = null,
    val uid: String? = null,
    val name: String? = null,
    var id: String? = null,
    val stats: _Stats? = null,
    val mods: HashMap<String, String>? = null
) : Serializable {
    data class _Weapon(
        val id: String? = null,
        val name: String? = null,
        val shortName: String? = null
    ) : Serializable

    data class _Stats(
        val ergonomics: Int? = null,
        val verticalRecoil: Int? = null,
        val horizontalRecoil: Int? = null,
        val velocity: Int? = null,
        val weight: Double? = null,
        val costRoubles: Int? = null,
    ) : Serializable

    fun delete() {
        id?.let { LocalStore.remove("loadouts/$it") }
    }

    fun updateName(name: String) {
        id?.let { LocalStore.set("loadouts/$it/name", name) }
    }

    companion object {
        /** Calls [onChange] with every saved build, now and after each change. */
        fun listen(onChange: (List<WeaponBuildFirestore>) -> Unit): Closeable =
            LocalStore.listen("loadouts") { node ->
                onChange(
                    (node as? Map<*, *>)
                        ?.mapNotNull { (id, value) ->
                            LocalStore.convert(value, WeaponBuildFirestore::class.java)?.also { it.id = id.toString() }
                        }.orEmpty(),
                )
            }

        fun add(build: Map<String, Any?>) = LocalStore.set("loadouts/${UUID.randomUUID()}", build)

        fun replace(
            id: String,
            build: Map<String, Any?>,
        ) = LocalStore.set("loadouts/$id", build)
    }
}
