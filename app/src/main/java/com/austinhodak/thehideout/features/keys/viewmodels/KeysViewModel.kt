package com.austinhodak.thehideout.features.keys.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.austinhodak.tarkovapi.UserSettingsModel
import com.austinhodak.tarkovapi.room.models.Item
import com.austinhodak.thehideout.SearchViewModel
import com.austinhodak.thehideout.firebase.FSUser
import com.austinhodak.thehideout.local.FieldValue
import com.austinhodak.thehideout.utils.keysField
import com.austinhodak.thehideout.utils.userFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KeysViewModel @Inject constructor() : SearchViewModel() {

    var sortBy = MutableLiveData(UserSettingsModel.keySort.value)

    fun setSort(int: Int) {
        sortBy.value = int
        viewModelScope.launch {
            UserSettingsModel.keySort.update(int)
        }
    }

    fun toggleKey(item: Item, fsUser: FSUser) {
        if (fsUser.hasKey(item)) {
            userFirestore()?.update("$keysField.${item.id}", FieldValue.delete())
        } else {
            userFirestore()?.update("$keysField.${item.id}", true)
        }
    }
}