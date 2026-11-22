package com.austinhodak.thehideout

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.annotation.ExperimentalCoilApi
import com.austinhodak.tarkovapi.repository.TarkovRepo
import com.austinhodak.tarkovapi.room.enums.ItemTypes
import com.austinhodak.tarkovapi.room.models.Item
import com.austinhodak.thehideout.features.ammunition.AmmunitionListScreen
import com.austinhodak.thehideout.features.bosses.BossesListScreen
import com.austinhodak.thehideout.features.calculator.CalculatorMainActivity
import com.austinhodak.thehideout.features.currency.CurrenyConverterScreen
import com.austinhodak.thehideout.features.flea_market.FleaMarketScreen
import com.austinhodak.thehideout.features.flea_market.grid.ItemQuestHideoutGridScreen
import com.austinhodak.thehideout.features.flea_market.viewmodels.FleaViewModel
import com.austinhodak.thehideout.features.gear.GearListScreen
import com.austinhodak.thehideout.features.gear.viewmodels.GearViewModel
import com.austinhodak.thehideout.features.hideout.HideoutMainScreen
import com.austinhodak.thehideout.features.hideout.viewmodels.HideoutMainViewModel
import com.austinhodak.thehideout.features.keys.KeyListScreen
import com.austinhodak.thehideout.features.keys.viewmodels.KeysViewModel
import com.austinhodak.thehideout.features.map.MapsActivity
import com.austinhodak.thehideout.features.map.MapsListScreen
import com.austinhodak.thehideout.features.medical.MedicalListScreen
import com.austinhodak.thehideout.features.news.NewsScreen
import com.austinhodak.thehideout.features.provisions.ProvisionListScreen
import com.austinhodak.thehideout.features.quests.QuestMainScreen
import com.austinhodak.thehideout.features.quests.viewmodels.QuestMainViewModel
import com.austinhodak.thehideout.features.skills.CharacterSkillsScreen
import com.austinhodak.thehideout.features.tools.SensitivityCalculatorScreen
import com.austinhodak.thehideout.features.tools.ServerPingScreen
import com.austinhodak.thehideout.features.tools.viewmodels.SensitivityViewModel
import com.austinhodak.thehideout.features.traders.RestockTimersScreen
import com.austinhodak.thehideout.features.traders.TraderScreen
import com.austinhodak.thehideout.features.weapons.WeaponListScreen
import com.austinhodak.thehideout.features.weapons.builder.WeaponLoadoutScreen
import com.austinhodak.thehideout.features.weapons.builder.viewmodel.WeaponLoadoutViewModel
import com.austinhodak.thehideout.features.weapons.detail.WeaponDetailActivity
import com.austinhodak.thehideout.features.weapons.mods.ModsListScreen
import com.austinhodak.thehideout.ui.common.PERMANENT_DRAWER_MIN_WIDTH_DP
import com.austinhodak.thehideout.ui.legacy.MainDrawer
import com.austinhodak.thehideout.ui.theme.HideoutTheme
import com.austinhodak.thehideout.utils.*
import com.google.android.play.core.review.ReviewManagerFactory
import com.maxkeppeker.sheets.core.models.base.Header
import com.maxkeppeker.sheets.core.models.base.SelectionButton
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.info.InfoDialog
import com.maxkeppeler.sheets.info.models.InfoBody
import com.maxkeppeler.sheets.info.models.InfoSelection
import com.skydoves.only.only
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@ExperimentalFoundationApi
@ExperimentalCoilApi
@ExperimentalCoroutinesApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@AndroidEntryPoint
class NavActivity : GodActivity() {
    private val navViewModel: NavViewModel by viewModels()
    private val fleaViewModel: FleaViewModel by viewModels()
    private val questViewModel: QuestMainViewModel by viewModels()
    private val hideoutViewModel: HideoutMainViewModel by viewModels()
    private val keysViewModel: KeysViewModel by viewModels()
    private val gearViewModel: GearViewModel by viewModels()
    private val loadoutViewModel: WeaponLoadoutViewModel by viewModels()
    private val sensitivityViewModel: SensitivityViewModel by viewModels()

    @Inject
    lateinit var tarkovRepo: TarkovRepo

    private val weaponDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == RESULT_OK) {
            val caliber = res.data?.getStringExtra("caliber")
            if (caliber != null) {
                navViewModel.drawerItemSelected(Pair((101).toLong(), caliber))
                navViewModel.clearSearch()
                navViewModel.setSearchOpen(false)
            }
        }
    }

    private var doubleBackToExitPressedOnce = false

    private fun handleBack(navController: NavController) {
        when {
            navViewModel.isDrawerOpen.value == true -> {
                navViewModel.setDrawerOpen(false)
            }
            navViewModel.isSearchOpen.value == true -> {
                navViewModel.clearSearch()
                navViewModel.setSearchOpen(false)
            }
            fleaViewModel.isSearchOpen.value == true -> {
                fleaViewModel.clearSearch()
                fleaViewModel.setSearchOpen(false)
            }
            questViewModel.isSearchOpen.value == true -> {
                questViewModel.clearSearch()
                questViewModel.setSearchOpen(false)
            }
            hideoutViewModel.isSearchOpen.value == true -> {
                hideoutViewModel.clearSearch()
                hideoutViewModel.setSearchOpen(false)
            }
            keysViewModel.isSearchOpen.value == true -> {
                keysViewModel.clearSearch()
                keysViewModel.setSearchOpen(false)
            }
            gearViewModel.isSearchOpen.value == true -> {
                gearViewModel.clearSearch()
                gearViewModel.setSearchOpen(false)
            }
            loadoutViewModel.isSearchOpen.value == true -> {
                loadoutViewModel.clearSearch()
                loadoutViewModel.setSearchOpen(false)
            }
            else -> {
                if (doubleBackToExitPressedOnce) {
                    if (!navController.popBackStack()) finish()
                    return
                }

                this.doubleBackToExitPressedOnce = true
                Toast.makeText(this, getString(R.string.back_exit), Toast.LENGTH_SHORT).show()

                Handler(Looper.getMainLooper()).postDelayed(
                    { doubleBackToExitPressedOnce = false },
                    2000
                )
            }
        }
    }

    val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts
            .RequestPermission()
    ) { isGranted: Boolean ->

    }

    @OptIn(ExperimentalLayoutApi::class)
    @SuppressLint("CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupReviewPopup()

        val data = intent.extras
        data?.let {
            it.getString("url")?.openWithCustomTab(this)
        }

        setContent {
            val scaffoldState = rememberScaffoldState()
            val coroutineScope = rememberCoroutineScope()
            val lifeCycleOwner = this
            val navController = rememberNavController()
            val notificationDialogSheetState = rememberUseCaseState(visible = false)

            if (Build.VERSION.SDK_INT greaterThanOrEqualTo (33)) {
                val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
                val permissionStatus =
                    ContextCompat.checkSelfPermission(this, notificationPermission)

                if (permissionStatus == PackageManager.PERMISSION_DENIED) {
                    shouldShowRequestPermissionRationale(notificationPermission).let { shouldShow ->
                        if (shouldShow) {
                            notificationDialogSheetState.show()
                        } else {
                            requestPermissionLauncher.launch(
                                notificationPermission
                            )
                        }
                    }
                }
            }

            val tag: String =
                navViewModel.selectedDrawerItem.value?.tag?.toString() ?: extras.openingPageTag

            navViewModel.isDrawerOpen.observe(lifeCycleOwner) { isOpen ->
                coroutineScope.launch {
                    // No modal drawer is composed when it is shown permanently.
                    if (resources.configuration.screenWidthDp >= PERMANENT_DRAWER_MIN_WIDTH_DP) return@launch
                    if (isOpen) {
                        scaffoldState.drawerState.open()
                    } else {
                        scaffoldState.drawerState.close()
                    }
                }
            }

            HideoutTheme {
                InfoDialog(
                    state = notificationDialogSheetState,
                    header = Header.Default(
                        title = "Enable Notifications?",
                    ),
                    body = InfoBody.Default(
                        bodyText = "The Hideout needs to be able to send you notifications to alert you of restocks and more. Please enable notifications for The Hideout.",
                    ),
                    selection = InfoSelection(
                        onPositiveClick = {
                            requestPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        },
                        positiveButton = SelectionButton(text = "Okay"),
                        onNegativeClick = {},
                        negativeButton = SelectionButton(text = "No Thanks"),
                    ),
                )

                // Large screens (expanded width) keep the drawer open beside the content.
                val permanentDrawer = LocalConfiguration.current.screenWidthDp >= PERMANENT_DRAWER_MIN_WIDTH_DP
                val drawer: @Composable () -> Unit = {
                    MainDrawer(
                        navViewModel = navViewModel,
                        lifeCycleOwner,
                        this@NavActivity,
                        apolloClient,
                    )
                }
                Row {
                    if (permanentDrawer) {
                        Box(Modifier.width(320.dp).fillMaxHeight()) { drawer() }
                    }
                    Scaffold(
                        scaffoldState = scaffoldState,
                        drawerContent = if (permanentDrawer) null else ({ drawer() }),
                        drawerScrimColor = Color(0xFF121212)
                    ) { padding ->
                        NavHost(
                            navController = navController,
                            startDestination = tag,
                            modifier = Modifier.consumeWindowInsets(padding)
                        ) {
                            composable("ammunition/{caliber}") {
                                AmmunitionListScreen(
                                    caliber = it.arguments?.getString("caliber", null),
                                    navViewModel = navViewModel,
                                    tarkovRepo = tarkovRepo
                                )
                            }
                            composable("gear/{category}") {
                                GearListScreen(
                                    category = it.arguments?.getString("category", null),
                                    navViewModel = navViewModel,
                                    tarkovRepo = tarkovRepo,
                                    gearViewModel
                                )
                            }
                            composable("keys") {
                                KeyListScreen(
                                    navViewModel = navViewModel,
                                    tarkovRepo,
                                    keysViewModel
                                )
                            }
                            composable("medical") {
                                MedicalListScreen(
                                    tarkovRepo,
                                    navViewModel = navViewModel
                                )
                            }
                            composable("food") {
                                ProvisionListScreen(navViewModel = navViewModel, tarkovRepo = tarkovRepo)
                            }
                            composable("flea") {
                                FleaMarketScreen(navViewModel = navViewModel, fleaViewModel = fleaViewModel, tarkovRepo = tarkovRepo)
                            }
                            composable("weaponmods") {
                                ModsListScreen(
                                    tarkovRepo,
                                    navViewModel
                                )
                            }
                            composable("weapons/{classID}") {
                                WeaponListScreen(
                                    classID = it.arguments?.getString("classID") ?: "assaultRifle",
                                    navViewModel,
                                    tarkovRepo
                                ) {
                                    val intent = Intent(this@NavActivity, WeaponDetailActivity::class.java).apply {
                                        putExtra("weaponID", it)
                                    }
                                    weaponDetailLauncher.launch(intent)
                                }
                            }
                            composable("quests") {
                                QuestMainScreen(
                                    navViewModel,
                                    questViewModel,
                                    tarkovRepo
                                )
                            }
                            composable("hideout") {
                                HideoutMainScreen(
                                    navViewModel,
                                    hideoutViewModel,
                                    tarkovRepo
                                )
                            }
                            composable("weaponloadouts") {
                                WeaponLoadoutScreen(
                                    loadoutViewModel,
                                    navViewModel,
                                    tarkovRepo
                                )
                            }
                            composable("currency_converter") {
                                CurrenyConverterScreen(navViewModel, tarkovRepo)
                            }
                            composable("sensitivity") {
                                SensitivityCalculatorScreen(navViewModel, tarkovRepo, sensitivityViewModel, armorPickerLauncher)
                            }
                            composable("trader/{trader}") {
                                TraderScreen(
                                    it.arguments?.getString("trader"),
                                    navViewModel,
                                    tarkovRepo,
                                    apolloClient
                                )
                            }
                            composable("news") {
                                NewsScreen(
                                    navViewModel
                                )
                            }
                            composable("skills") {
                                CharacterSkillsScreen(navViewModel = navViewModel)
                            }
                            composable("restock") {
                                RestockTimersScreen(
                                    navViewModel,
                                    apolloClient
                                )
                            }
                            composable("server_pings") {
                                ServerPingScreen(navViewModel, tarkovRepo)
                            }
                            composable("neededGrid") {
                                ItemQuestHideoutGridScreen(
                                    navViewModel,
                                    tarkovRepo,
                                    questViewModel,
                                    hideoutViewModel
                                )
                            }
                            composable("bosses") {
                                BossesListScreen(
                                    navViewModel,
                                    apolloClient
                                )
                            }
                            composable("map_info") {
                                MapsListScreen(navViewModel = navViewModel, apolloClient = apolloClient)
                            }
                        }

                        // After NavHost so it runs before NavHost's own back callback, as onBackPressed did.
                        BackHandler { handleBack(navController) }

                        navViewModel.selectedDrawerItem.observe(lifeCycleOwner) { selectedItem ->
                            if (selectedItem == null) return@observe
                            val route = selectedItem.tag.toString()
                            val identifier = selectedItem.identifier.toInt()
                            when {
                                route == "activity:sim" -> openActivity(CalculatorMainActivity::class.java)
                                route == "activity:map" -> openActivity(MapsActivity::class.java)
                                route.contains("url:") -> {
                                    route.split(":")[1].openWithCustomTab(this@NavActivity)
                                }

                                else -> {
                                    navController.navigate(route) {
                                        restoreState = true
                                        navController.popBackStack()
                                    }
                                }
                            }
                        }

                        navViewModel.selectedDrawerItemIdentifier.observe(lifeCycleOwner) {
                            if (it != null)
                                navController.navigate(it.second)
                        }

                        coroutineScope.launch {
                            //TODO Default drawer selection still stays selected, needs fixed but works for now.
                            delay(100)
                            if (intent.hasExtra("fromNoti") && intent.hasExtra("trader")) {
                                val trader = "trader/${intent.getStringExtra("trader")?.lowercase() ?: "prapor"}"
                                when (intent.getStringExtra("trader")?.lowercase() ?: "prapor") {
                                    "prapor" -> navViewModel.drawerItemSelected(Pair((501).toLong(), trader))
                                    "therapist" -> navViewModel.drawerItemSelected(Pair((502).toLong(), trader))
                                    "skier" -> navViewModel.drawerItemSelected(Pair((503).toLong(), trader))
                                    "peacekeeper" -> navViewModel.drawerItemSelected(Pair((504).toLong(), trader))
                                    "mechanic" -> navViewModel.drawerItemSelected(Pair((505).toLong(), trader))
                                    "ragman" -> navViewModel.drawerItemSelected(Pair((506).toLong(), trader))
                                    "jaeger" -> navViewModel.drawerItemSelected(Pair((507).toLong(), trader))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupReviewPopup() {
        val manager = ReviewManagerFactory.create(this)
        only("reviewPopup", times = 5) {
            onLastDo {
                val request = manager.requestReviewFlow()
                request.addOnSuccessListener { reviewInfo ->
                    Timber.d("LAUNCHING REVIEW")
                    manager.launchReviewFlow(this@NavActivity, reviewInfo)
                }
            }
        }
    }

    private val armorPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val intent = result.data
            intent?.getSerializableExtra("item")?.let {
                if (it is Item) {
                    if (it.itemType == ItemTypes.HELMET) {
                        sensitivityViewModel.selectHelmet(it)
                    } else {
                        sensitivityViewModel.selectArmor(it)
                    }
                }
            }
        }
    }
}