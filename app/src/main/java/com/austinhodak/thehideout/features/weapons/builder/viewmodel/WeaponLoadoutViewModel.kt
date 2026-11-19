package com.austinhodak.thehideout.features.weapons.builder.viewmodel

import androidx.lifecycle.MutableLiveData
import com.austinhodak.tarkovapi.repository.ModsRepo
import com.austinhodak.tarkovapi.repository.TarkovRepo
import com.austinhodak.thehideout.SearchViewModel
import com.austinhodak.thehideout.firebase.WeaponBuildFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WeaponLoadoutViewModel @Inject constructor(
        private val tarkovRepo: TarkovRepo,
        private val modsRepo: ModsRepo
) : SearchViewModel() {

    private val _userLoadouts = MutableLiveData<List<WeaponBuildFirestore>?>(null)
    val userLoadouts = _userLoadouts

    private val loadoutsListener = WeaponBuildFirestore.listen { _userLoadouts.value = it }

    override fun onCleared() {
        loadoutsListener.close()
        super.onCleared()
    }
}
