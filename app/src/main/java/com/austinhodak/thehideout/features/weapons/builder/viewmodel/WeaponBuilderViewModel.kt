package com.austinhodak.thehideout.features.weapons.builder.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.austinhodak.tarkovapi.repository.ModsRepo
import com.austinhodak.tarkovapi.repository.TarkovRepo
import com.austinhodak.tarkovapi.room.models.Mod
import com.austinhodak.tarkovapi.room.models.Weapon
import com.austinhodak.thehideout.features.weapons.builder.WeaponBuild
import com.austinhodak.thehideout.firebase.WeaponBuildFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WeaponBuilderViewModel @Inject constructor(
    private val tarkovRepo: TarkovRepo,
    private val modsRepo: ModsRepo
) : ViewModel() {

    private val _userLoadouts = MutableLiveData<List<WeaponBuildFirestore>?>(null)
    val userLoadouts = _userLoadouts

    private val loadoutsListener = WeaponBuildFirestore.listen { _userLoadouts.value = it }

    override fun onCleared() {
        loadoutsListener.close()
        super.onCleared()
    }

    var buildState by mutableStateOf<WeaponBuild?>(WeaponBuild())
    //var buildStateLive = MutableLiveData<WeaponBuild?>(WeaponBuild())

    var savedBuild: WeaponBuildFirestore? = null

    fun loadBuild(build: WeaponBuildFirestore) {
        //Timber.d(build.toString())
        //if (savedBuild != null) return
        savedBuild = build
        val b = buildState?.copy() ?: WeaponBuild()
        viewModelScope.launch {
            build.weapon?.id?.let {
                setParentWeapon(it)
            }
            build.ammo?.let {
                setAmmo(it)
            }
            build.mods?.forEach { (slotID, modID) ->
                if (buildState?.mods?.containsKey(slotID) == false) {
                    updateMod(modID, slotID, null)
                }
            }
        }

        buildState = b
        //buildStateLive.value = b

        buildState!!.name = build.name
        buildState!!.uid = build.uid

        //buildStateLive.value!!.name =  build.name
        //buildStateLive.value!!.uid = build.uid
    }

    fun setAmmo(ammoID: String) {
        val b = buildState?.copy() ?: WeaponBuild()
        viewModelScope.launch {
            tarkovRepo.getAmmoByID(ammoID).first {
                b.ammo = it
                buildState = b
                //buildStateLive.value = b
                true
            }
        }
    }

    fun setParentWeapon(weaponID: String) {
        val b = buildState?.copy() ?: WeaponBuild()

        viewModelScope.launch {
            tarkovRepo.getWeaponByID(weaponID).first {
                b.parentWeapon = it
                it.Slots?.forEach {
                    val s = Pair(it, null)
                    //b.slots = b.slots?.plus(s)
                }
                buildState = b
                //buildStateLive.value = b

                it.defAmmo?.let {
                    tarkovRepo.getAmmoByID(it).first {
                        b.ammo = it
                        buildState = b
                        //buildStateLive.value = b
                        true
                    }
                }

                true
            }
        }
    }

    fun setParentWeapon(weapon: Weapon) {
        val b = buildState?.copy() ?: WeaponBuild()

        viewModelScope.launch {
            b.parentWeapon = weapon
            buildState = b
            //buildStateLive.value = b

            weapon.defAmmo?.let {
                tarkovRepo.getAmmoByID(it).first {
                    b.ammo = it
                    buildState = b
                    //buildStateLive.value = b
                    true
                }
            }
        }
    }

    fun resetBuild() {
        val b = buildState?.copy()

        buildState = WeaponBuild()
        b?.parentWeapon?.id?.let { setParentWeapon(it) }
        //buildStateLive.value = b
    }

    fun removeMod(mod: Mod?, slotID: String?, parentID: String?, slot: Weapon.Slot) {
        val b = buildState?.copy() ?: WeaponBuild()

        b.mods?.get(slotID)?.mod?.Slots?.let {
            slotID?.let {
                b.mods = b.mods!! - it
            }
            buildState = b
            //buildStateLive.value = b
            it.forEach { slot ->
                Timber.d(slot._id)
                removeMod(null, slot._id, null, slot)
            }
        }
    }

    fun updateMod(mod: Mod, slot: Weapon.Slot) {
        val b = buildState?.copy() ?: WeaponBuild()
        val mMod = b.parentWeapon?.Slots?.find { it == slot }?.apply {
            this.mod = mod
        }
        Timber.d(mMod.toString())
        buildState = b
        //buildStateLive.value = b
    }

    fun updateMod(modID: String?, slotID: String?, parentID: String?) {
        if (modID == null || slotID == null) return
        viewModelScope.launch {
            modsRepo.getModByID(modID).first {
                updateMod(it, slotID)
                true
            }
        }
    }

    fun updateMod(mod: Mod, slotID: String) {
        val b = buildState?.copy() ?: WeaponBuild()
        /*if (parentID == b.parentWeapon?.id) {
            b.mods = b.mods!! + mapOf(
                    Pair(
                            slotID,
                            WeaponBuild.BuildMod(
                                    //parent = parentID,
                                    mod = mod
                            )
                    )
            )
            buildState = b
        } else {
            b.mods = b.mods!! + mapOf(
                    Pair(
                            slotID,
                            WeaponBuild.BuildMod(
                                    //parent = parentID,
                                    mod = mod
                            )
                    )
            )
            buildState = b
        }*/

        b.mods = b.mods!! + mapOf(
            Pair(
                slotID,
                WeaponBuild.BuildMod(
                    //parent = parentID,
                    mod = mod
                )
            )
        )
        buildState = b
        //buildStateLive.value = b
    }

    fun saveBuild(done: (Boolean) -> Unit) {
        val build = buildState?.toFirestore() ?: return
        val id = savedBuild?.id
        if (id == null) WeaponBuildFirestore.add(build) else WeaponBuildFirestore.replace(id, build)
        done(true)
    }
}
