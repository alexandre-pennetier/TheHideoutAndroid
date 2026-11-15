package com.austinhodak.tarkovapi.utils

import com.austinhodak.tarkovapi.BartersQuery
import com.austinhodak.tarkovapi.CraftsQuery
import com.austinhodak.tarkovapi.ItemsByTypeQuery
import com.austinhodak.tarkovapi.QuestsQuery
import com.austinhodak.tarkovapi.fragment.ContainsItem
import com.austinhodak.tarkovapi.fragment.ItemFragment
import com.austinhodak.tarkovapi.fragment.ItemPrice
import com.austinhodak.tarkovapi.fragment.QuestFragment
import com.austinhodak.tarkovapi.room.enums.ItemTypes
import com.austinhodak.tarkovapi.room.models.Barter
import com.austinhodak.tarkovapi.room.models.Craft
import com.austinhodak.tarkovapi.room.models.Pricing
import com.austinhodak.tarkovapi.room.models.Quest
import com.austinhodak.tarkovapi.type.ItemSourceName
import com.austinhodak.tarkovapi.type.ItemType
import com.austinhodak.tarkovapi.type.RequirementType
import org.json.JSONObject
import kotlin.math.roundToInt

// Maps a tarkov.dev Task onto the legacy Room Quest (tarkovdata ids and objective type names).
fun QuestsQuery.Quest.toQuest(): Quest {
    val task = this.questFragment
    val trader = Quest.TraderFragment(task.trader.name, task.trader.id)
    return Quest(
        id = task.tarkovDataId?.toString() ?: task.id ?: "",
        title = task.name,
        wikiLink = task.wikiLink,
        exp = task.experience,
        giver = trader,
        turnin = trader,
        unlocks = task.finishRewards?.offerUnlock?.mapNotNull { it?.item?.id },
        requirement =
            Quest.QuestRequirement(
                level = task.minPlayerLevel,
                quests = task.taskRequirements.mapNotNull { it?.task?.tarkovDataId }.map { listOf(it) },
            ),
        objective = task.objectives.filterNotNull().map { it.toLegacyObjective() },
    )
}

private fun QuestFragment.Objective.toLegacyObjective(): Quest.QuestObjective {
    val item = onTaskObjectiveItem
    val questItem = onTaskObjectiveQuestItem
    val shoot = onTaskObjectiveShoot
    val skill = onTaskObjectiveSkill
    val traderLevel = onTaskObjectiveTraderLevel
    val firstItem = item?.items?.firstOrNull()?.itemFragment
    val (legacyType, target, number) =
        when {
            type == "findItem" && item != null -> Triple("find", firstItem?.name, item.count)
            type == "giveItem" && item != null -> Triple("collect", firstItem?.name, item.count)
            type == "findQuestItem" -> Triple("pickup", questItem?.questItem?.name, questItem?.count)
            type == "giveQuestItem" -> Triple("collect", questItem?.questItem?.name, questItem?.count)
            type == "plantItem" || type == "plantQuestItem" -> Triple("place", firstItem?.name ?: questItem?.questItem?.name, null)
            type == "mark" -> Triple("mark", description, null)
            type == "visit" -> Triple("locate", description, null)
            shoot != null -> Triple("kill", shoot.targetNames.filterNotNull().joinToString(", "), shoot.count)
            skill != null -> Triple("skill", skill.skillLevel.name, skill.skillLevel.level.toInt())
            traderLevel != null -> Triple("reputation", traderLevel.trader.tarkovDataId?.toString(), traderLevel.level)
            else -> Triple("warning", description, null)
        }
    return Quest.QuestObjective(
        id = id,
        type = legacyType,
        target = listOfNotNull(target),
        number = number,
        // Legacy code parses location as an Int map id; newer maps have none.
        location = maps.firstOrNull()?.tarkovDataId?.takeIf { it.toIntOrNull() != null },
        targetItem = firstItem?.toClass(),
    )
}

//Determines item type from the raw item file
fun JSONObject.itemType(): ItemTypes {
    val props = getJSONObject("_props")
    if (
        !props.has("Name")
        || !props.has("ShortName")
        || getString("_name").equals("Ammo")
        || getString("_parent").isNullOrBlank()
        || getString("_parent") == "54009119af1c881c07000029"
        || getString("_parent") == "5661632d4bdc2d903d8b456b"
        || getString("_id") == "5996f6cb86f774678763a6ca"
        || getString("_id") == "5996f6d686f77467977ba6cc"
        || getString("_id") == "5996f6fc86f7745e585b4de3"
        || getString("_id") == "5943d9c186f7745a13413ac9"
    ) {
        return ItemTypes.NULL
    }

    return when {
        props.has("weapFireType") -> ItemTypes.WEAPON
        props.has("Caliber") && !props.getString("Name")
            .contains("Shrapnel", true) -> ItemTypes.AMMO
        props.has("Prefab") && props.getJSONObject("Prefab").getString("path")
            .contains("assets/content/items/mods") -> ItemTypes.MOD
        else -> {
            ItemTypes.NONE
        }
    }
}

//Maps TarkovTools Item to our Pricing Item.
fun ItemFragment.toClass(): Pricing {
    val item = this
    return Pricing(
        item.id,
        item.name,
        item.shortName,
        item.iconLink,
        item.imageLink,
        item.gridImageLink,
        item.avg24hPrice,
        item.basePrice,
        item.lastLowPrice,
        item.changeLast48h,
        item.low24hPrice,
        item.high24hPrice,
        item.updated,
        types = item.types,
        item.width,
        item.height,
        sellFor = item.sellFor?.map { s1 ->
            val s = s1.itemPrice
            s.toBuySell()
        },
        buyFor = item.buyFor?.map { s1 ->
            val s = s1.itemPrice
            s.toBuySell()
        },
        item.wikiLink,
        item.types.contains(ItemType.noFlea),
        containsItem = item.containsItems?.mapNotNull {
            val i = it?.containsItem
            Pricing.Contains(
                quantity = i?.quantity?.toInt(),
                count = i?.count?.toInt(),
                item = i?.toClass()
            )
        }
    )
}

fun ContainsItem.toClass(): Pricing.ContainsItem {
    val item = this.item
    return Pricing.ContainsItem(
        item.id,
        item.name,
        item.shortName,
        item.iconLink,
        item.imageLink,
        item.gridImageLink,
        item.avg24hPrice,
        item.basePrice,
        item.lastLowPrice,
        item.changeLast48h,
        item.low24hPrice,
        item.high24hPrice,
        item.updated,
        types = item.types,
        item.width,
        item.height,
        sellFor = item.sellFor?.map { s1 ->
            val s = s1.itemPrice
            s.toBuySell()
        },
        buyFor = item.buyFor?.map { s1 ->
            val s = s1.itemPrice
            s.toBuySell()
        },
        item.wikiLink,
        item.types.contains(ItemType.noFlea)
    )
}

fun ItemPrice?.toBuySell(): Pricing.BuySellPrice {
    val vendor = this?.vendor
    val trader = vendor?.onTraderOffer
    val flea = vendor?.onFleaMarket
    // Rebuild the legacy source/requirements shape from ItemPrice.vendor.
    val requirements =
        listOfNotNull(
            trader?.minTraderLevel?.let { Pricing.BuySellPrice.Requirement(RequirementType.loyaltyLevel.rawValue, it) },
            trader?.taskUnlock?.tarkovDataId?.let { Pricing.BuySellPrice.Requirement(RequirementType.questCompleted.rawValue, it) },
            flea?.minPlayerLevel?.let { Pricing.BuySellPrice.Requirement(RequirementType.playerLevel.rawValue, it) },
        )
    return Pricing.BuySellPrice(
        if (flea != null) ItemSourceName.fleaMarket.rawValue else vendor?.normalizedName,
        this?.price,
        requirements,
        this?.currency,
    )
}

//Maps TarkovTools Craft to our Craft.
fun CraftsQuery.Craft.toCraft(): Craft {
    return Craft(
        id = id,
        duration = duration,
        requiredItems = requiredItems.map {
            Craft.CraftItem(
                it?.taskItem?.count?.roundToInt(),
                it?.taskItem?.item?.itemFragment?.toClass(),
                it?.taskItem?.attributes?.map { att ->
                    Craft.CraftItem.Attributes(
                        att?.type,
                        att?.name,
                        att?.value
                    )
                }
            )
        },
        rewardItems = rewardItems.map {
            Craft.CraftItem(
                it?.taskItem?.count?.roundToInt(),
                it?.taskItem?.item?.itemFragment?.toClass(),
                it?.taskItem?.attributes?.map { att ->
                    Craft.CraftItem.Attributes(
                        att?.type,
                        att?.name,
                        att?.value
                    )
                }
            )
        },
        source = "${station.name} level $level"
    )
}

//Maps TarkovTools Barter to our barter..
fun BartersQuery.Barter.toBarter(): Barter {
    return Barter(
        requiredItems = requiredItems.map {
            Craft.CraftItem(
                it?.taskItem?.count?.roundToInt(),
                it?.taskItem?.item?.itemFragment?.toClass()
            )
        },
        rewardItems = rewardItems.map {
            Craft.CraftItem(
                it?.taskItem?.count?.roundToInt(),
                it?.taskItem?.item?.itemFragment?.toClass()
            )
        },
        source = "${trader.name} LL$level"
    )
}

fun ItemsByTypeQuery.Item.toPricing(): Pricing {
    val item = this.itemFragment
    return item.toClass()
}

fun JSONObject.getItemType(): ItemTypes {
    val props = getJSONObject("_props")
    return when {
        props.has("weapFireType") -> ItemTypes.WEAPON
        props.has("Prefab") && props.getJSONObject("Prefab").getString("path")
            .contains("assets/content/items/mods") -> ItemTypes.MOD
        props.has("Caliber") -> ItemTypes.AMMO
        this.getString("_parent").equals("5448e54d4bdc2dcc718b4568") -> ItemTypes.ARMOR
        this.getString("_parent").equals("5448e5284bdc2dcb718b4567") -> ItemTypes.RIG
        this.getString("_parent").equals("5448e53e4bdc2d60728b4567") -> ItemTypes.BACKPACK
        this.getString("_parent").equals("5a341c4686f77469e155819e") -> ItemTypes.FACECOVER
        this.getString("_parent").equals("5448e5724bdc2ddf718b4568") -> ItemTypes.GLASSES
        this.getString("_parent").equals("543be6564bdc2df4348b4568") -> ItemTypes.GRENADE
        this.getString("_parent").equals("5645bcb74bdc2ded0b8b4578") -> ItemTypes.HEADSET
        this.getString("_parent").equals("5a341c4086f77401f2541505") || this.getString("_parent")
            .equals("57bef4c42459772e8d35a53b") -> ItemTypes.HELMET
        this.getString("_parent").equals("5c99f98d86f7745c314214b3") || this.getString("_parent")
            .equals("5c164d2286f774194c5e69fa") -> ItemTypes.KEY
        this.getString("_parent").equals("5448f3a64bdc2d60728b456a") -> ItemTypes.STIM
        this.getString("_parent").equals("5448f39d4bdc2d0a728b4568") -> ItemTypes.MED
        this.getString("_parent").equals("5448f3ac4bdc2dce718b4569") -> ItemTypes.MED
        this.getString("_parent").equals("5448f3a14bdc2d27728b4569") -> ItemTypes.MED
        this.getString("_parent").equals("5448e8d04bdc2ddf718b4569") -> ItemTypes.FOOD
        this.getString("_parent").equals("5448e8d64bdc2dce718b4568") -> ItemTypes.FOOD
        this.getString("_parent").equals("5447e1d04bdc2dff2f8b4567") -> ItemTypes.MELEE
        else -> ItemTypes.NONE
    }
}