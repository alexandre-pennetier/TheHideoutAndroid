package com.austinhodak.thehideout.realm.models

import com.austinhodak.thehideout.BossesQuery
import com.austinhodak.thehideout.realm.converters.findObjectById
import io.github.xilinjia.krdb.MutableRealm
import io.github.xilinjia.krdb.ext.realmListOf
import io.github.xilinjia.krdb.ext.toRealmList
import io.github.xilinjia.krdb.types.EmbeddedRealmObject
import io.github.xilinjia.krdb.types.RealmList
import io.github.xilinjia.krdb.types.RealmObject
import io.github.xilinjia.krdb.types.annotations.PrimaryKey

class MobInfo : RealmObject {
    @PrimaryKey
    var id: String = ""
    var name: String = ""
    var normalizedName: String = ""
    var health: RealmList<HealthPart>? = null
    var equipment: RealmList<Item.ContainedItem> = realmListOf()
    var items: RealmList<Item> = realmListOf()

    class HealthPart : EmbeddedRealmObject {
        var max: Int? = null
        var bodyPart: String? = null
        var id: String? = null
    }
}

fun BossesQuery.Data.Boss.toRealm(realm: MutableRealm): MobInfo {
    val b = this
    return MobInfo().apply {
        id = b.normalizedName
        name = b.name
        normalizedName = b.normalizedName
        health = b.health?.map { h ->
            MobInfo.HealthPart().apply {
                max = h?.max
                bodyPart = h?.bodyPart
                id = h?.id
            }
        }?.toRealmList()
        equipment = b.equipment.filterNotNull().map {
            it.toRealm(realm)
        }.toRealmList()
        items = b.items.filterNotNull().mapNotNull {
            findObjectById<Item>(realm, it.id)
        }.toRealmList()
    }
}