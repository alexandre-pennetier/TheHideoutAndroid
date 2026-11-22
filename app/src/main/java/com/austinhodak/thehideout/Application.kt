package com.austinhodak.thehideout

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.airbnb.mvrx.Mavericks
import com.austinhodak.tarkovapi.UserSettingsModel
import com.austinhodak.tarkovapi.repository.TarkovRepo
import com.austinhodak.tarkovapi.utils.AmmoBallistics
import com.austinhodak.tarkovapi.utils.Hideout
import com.austinhodak.tarkovapi.utils.Maps
import com.austinhodak.tarkovapi.utils.Rigs
import com.austinhodak.tarkovapi.utils.Skills
import com.austinhodak.tarkovapi.utils.Stims
import com.austinhodak.tarkovapi.utils.Traders
import com.austinhodak.tarkovapi.utils.WeaponPresets
import com.austinhodak.thehideout.firebase.FSUser
import com.austinhodak.thehideout.local.LocalStore
import com.austinhodak.thehideout.utils.*
import com.austinhodak.thehideout.utils.applyGameModeToCache
import com.austinhodak.thehideout.utils.forActiveMode
import com.austinhodak.thehideout.workmanager.PriceUpdateFactory
import com.austinhodak.thehideout.workmanager.PriceUpdateWorker
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.messaging.messaging
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.skydoves.only.Only
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.*
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class Application : android.app.Application(), Configuration.Provider, ImageLoaderFactory {
    private val mainScope = MainScope()

    @Inject
    lateinit var coilExtensions: CoilExtensions

    @Inject
    lateinit var myWorkerFactory: PriceUpdateFactory

    @Inject
    lateinit var tarkovRepo: TarkovRepo

    private val notificationManager: NotificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    companion object {
        var fsUser: MutableLiveData<FSUser?> = MutableLiveData()
        lateinit var instance: Application
            private set
    }

    private fun createServerStatusChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Server Status"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("SERVER_STATUS", name, importance)
            // Register the channel with the system
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createRestockNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Trader Restocks"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("TRADER_RESTOCK", name, importance)
            // Register the channel with the system
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createPriceAlertsNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Price Alerts"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("PRICE_ALERTS", name, importance)
            // Register the channel with the system
            notificationManager.createNotificationChannel(channel)
        }
    }

    /** Publishes the stored progress of the active game profile to [fsUser]. */
    private fun publishUser(stored: Any?) {
        fsUser.postValue(LocalStore.convert(stored, FSUser::class.java)?.forActiveMode() ?: FSUser())
    }

    override fun onCreate() {
        super.onCreate()
        // Edge-to-edge is enforced from targetSdk 35; theme roots keep content clear of the bars (SystemBarsBox).
        registerActivityLifecycleCallbacks(EdgeToEdgeCallbacks)
        LocalStore.init(this)

        Mavericks.initialize(this)

        createServerStatusChannel()
        createRestockNotificationChannel()
        createPriceAlertsNotificationChannel()

        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)

        instance = this



        //Device is either Firebase Test Lab or Google Play Pre-launch test device, disable analytics.
        if ("true" == Settings.System.getString(contentResolver, "firebase.test.lab")) {
            Firebase.analytics.setAnalyticsCollectionEnabled(false)
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }

        if (LocalStore.get("user/playerLevel") == null) {
            LocalStore.set("user/playerLevel", UserSettingsModel.playerLevel.value)
        }
        LocalStore.listen("user") { publishUser(it) }

        Only.init(applicationContext)

        Firebase.messaging.token.addOnSuccessListener {
            Timber.d("Firebase Messaging Token: %s", it)
        }

        // Runs after onCreate returns: SDK setup is not needed to draw the first screen.
        mainScope.launch {
            setupRemoteConfig()
        }

        UserSettingsModel.dataSyncFrequency.observe(MainScope()) {
            val workManager = WorkManager.getInstance(this)
            val oldFrequency = UserSettingsModel.dataSyncFrequencyPrevious.value

            val isScheduled = isWorkScheduled(this, "price_update")
            val isRunning = isWorkRunning(this, "price_update")

            Timber.d("Scheduled $isScheduled || Running $isRunning")

            if (oldFrequency == it && isScheduled && !isRunning) {
                //Do nothing, already set.
            } else {
                //Possible fix for prices not updating properly
                val constraints =
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresDeviceIdle(false).build()

                val priceUpdateRequest = PeriodicWorkRequestBuilder<PriceUpdateWorker>(
                    it.toString().toLong(),
                    TimeUnit.MINUTES
                ).setConstraints(constraints).build()

                workManager.enqueueUniquePeriodicWork(
                    "price_update",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    priceUpdateRequest
                )

                MainScope().launch {
                    UserSettingsModel.dataSyncFrequencyPrevious.update(it)
                }
            }

            MainScope().launch(Dispatchers.IO) {
                if (tarkovRepo.isPriceDaoEmpty()) {
                    workManager.enqueueUniqueWork(
                        "price_update_first_run",
                        ExistingWorkPolicy.KEEP,
                        OneTimeWorkRequest.Builder(PriceUpdateWorker::class.java).build(),
                    )
                }
            }
        }

        UserSettingsModel.languageSetting.observe(MainScope()) {

        }

        UserSettingsModel.gameProfile.observe(MainScope()) {
            MainScope().launch(Dispatchers.IO) {
                applyGameModeToCache(tarkovRepo)
                // Re-read progress so screens switch to the other mode's data immediately.
                publishUser(LocalStore.get("user"))
            }
        }
    }


    /*
     * Sets up the Firebase remote config.
     */

    private fun setupRemoteConfig() {
        //Set remote config settings if debug.
        if (BuildConfig.DEBUG) {
            Firebase.remoteConfig.setConfigSettingsAsync(remoteConfigSettings {
                minimumFetchIntervalInSeconds = 30
            })
        }

        Firebase.remoteConfig.setDefaultsAsync(
            mapOf(
                "game_info" to "{\"version\":\"0.12.12.30.19047\",\"version_date\":\"07-15-2022\",\"wipe_date\":\"06-30-2022\"}"
            )
        )
    }

    override fun getWorkManagerConfiguration(): Configuration = Configuration.Builder()
        .setMinimumLoggingLevel(android.util.Log.INFO)
        .setWorkerFactory(myWorkerFactory)
        .build()

    override fun newImageLoader(): ImageLoader {
        return coilExtensions.crossFadeLoader
    }


}

// Loaded on first use, not in Application.onCreate, so startup does not parse these JSON files.
val extras: Extras by lazy { Extras(Application.instance) }

val fsUser: LiveData<FSUser?> = Application.fsUser

val mapsList: Maps by lazy { Maps(Application.instance) }

val tradersList: Traders by lazy { Traders(Application.instance) }

val rigsList: Rigs by lazy { Rigs(Application.instance) }

val skillsList: Skills by lazy { Skills(Application.instance) }

val presetList: WeaponPresets by lazy { WeaponPresets(Application.instance) }

val ballistics: AmmoBallistics by lazy { AmmoBallistics(Application.instance) }

val stims: Stims by lazy { Stims(Application.instance) }

val hideoutList: Hideout by lazy { Hideout(Application.instance) }

private object EdgeToEdgeCallbacks : android.app.Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        val transparent = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        (activity as? ComponentActivity)?.enableEdgeToEdge(transparent, transparent)
    }

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
