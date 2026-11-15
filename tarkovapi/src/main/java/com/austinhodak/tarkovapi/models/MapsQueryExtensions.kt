package com.austinhodak.tarkovapi.models

import com.austinhodak.tarkovapi.MapsQuery

// BossSpawn.name and BossEscort.name were deprecated in favour of boss.name.
val MapsQuery.Boss.name: String get() = boss.name

val MapsQuery.Escort.name: String get() = boss.name
