package com.austinhodak.thehideout.di

import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.MavericksViewModelComponent
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.Multibinds

@Module
@InstallIn(MavericksViewModelComponent::class)
interface MavericksModule {

    // Mavericks-Hilt requires this map to exist even though no ViewModel is bound to it any more.
    @Multibinds
    fun viewModelFactories(): Map<Class<out MavericksViewModel<*>>, AssistedViewModelFactory<*, *>>
}
