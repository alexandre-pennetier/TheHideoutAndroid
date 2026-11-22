package com.austinhodak.thehideout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.austinhodak.tarkovapi.repository.TarkovRepo
import com.austinhodak.tarkovapi.room.models.Item
import com.austinhodak.thehideout.utils.Time
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@HiltViewModel
class NavViewModel @Inject constructor(
    private val tarkovRepo: TarkovRepo
) : SearchViewModel() {

    private val _isDrawerOpen = MutableLiveData(false)
    val isDrawerOpen = _isDrawerOpen

    fun setDrawerOpen(isOpen: Boolean) {
        isDrawerOpen.value = isOpen
    }

    private val _currentNavRoute = MutableLiveData<String>(null)
    val currentNavRoute = _currentNavRoute

    fun updateCurrentNavRoute(route: String) {
        _currentNavRoute.value = route
    }

    private val _selectedDrawerItem = MutableLiveData<IDrawerItem<*>>(null)
    val selectedDrawerItem = _selectedDrawerItem

    fun drawerItemSelected(item: IDrawerItem<*>) {
        _selectedDrawerItem.value = item
    }

    private val _selectedDrawerItemIdentifier = MutableLiveData<Pair<Long, String>>(null)
    val selectedDrawerItemIdentifier = _selectedDrawerItemIdentifier

    fun drawerItemSelected(identifier: Pair<Long, String>) {
        _selectedDrawerItemIdentifier.value = identifier
    }

    // The loop only runs while a clock is on screen (plus a short grace period), not for the whole process.
    private fun clock(left: Boolean) = flow {
        while (true) {
            emit(Time.realTimeToTarkovTime(left))
            delay(1000.div(7).toLong())
        }
    }.distinctUntilChanged().asLiveData()

    val timeLeft: LiveData<String> = clock(true)
    val timeRight: LiveData<String> = clock(false)

    // Starts on the first observer, not at launch; identical re-emissions (e.g. a price refresh that changed
    // nothing) are dropped so screens do not re-sort and re-filter for no reason.
    val allItems: LiveData<List<Item>> = tarkovRepo.getAllItems().distinctUntilChanged().flowOn(Dispatchers.Default).asLiveData()

}

