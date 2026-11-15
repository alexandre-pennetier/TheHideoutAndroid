package com.austinhodak.thehideout.data.sync

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.austinhodak.thehideout.SyncWorkName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import javax.inject.Inject

/**
 * [SyncStatusMonitor] backed by [WorkInfo] from [WorkManager]
 */
class SyncStatusMonitor @Inject constructor(
    @ApplicationContext context: Context
) {
    val isSyncing: Flow<Boolean> =
        WorkManager
            .getInstance(context)
            .getWorkInfosForUniqueWorkLiveData(SyncWorkName)
            .map { it.anyRunning }
            .asFlow()
            .conflate()
}

private val List<WorkInfo>.anyRunning get() = any { it.state == WorkInfo.State.RUNNING }
