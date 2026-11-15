package com.austinhodak.thehideout.realm.models

import io.github.xilinjia.krdb.ext.realmListOf
import io.github.xilinjia.krdb.types.RealmList
import io.github.xilinjia.krdb.types.RealmObject
import io.github.xilinjia.krdb.types.annotations.PrimaryKey

class ItemCategory : RealmObject {
    @PrimaryKey
    var id: String = ""
    var name: String = ""
    var normalizedName: String = ""
    var parent: ItemCategory? = null
    var children: RealmList<ItemCategory> = realmListOf()
}
