package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.firebase.MiVueltaRepository
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.onGoogleSignInClicked
import com.example.data.firebase.signOutUser
import com.example.data.local.AppSession
import com.example.data.local.LocalSessionStore
import com.example.data.local.LocalVueltaRepository
import com.example.notifications.VueltaReminderScheduler
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CalendarioScreen
import com.example.ui.screens.CrearVueltaScreen
import com.example.ui.screens.DetalleVueltaScreen
import com.example.ui.screens.InicioScreen
import com.example.ui.screens.PerfilScreen
import com.example.ui.screens.SplashSequenceScreen
import com.example.ui.theme.AppThemeMode
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

const val ROUTE_INICIO = "inicio"
const val ROUTE_CALENDARIO = "calendario"
const val ROUTE_PERFIL = "perfil"
const val ROUTE_CREAR = "crear"
const val ROUTE_DETALLE = "detalle"

private data class TopTab(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private val topTabs = listOf(
  TopTab(ROUTE_INICIO, "Mis Vueltas", Icons.Rounded.Checklist, Icons.Outlined.Checklist, "tab_inicio"),
  TopTab(ROUTE_CALENDARIO, "Calendario", Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth, "tab_calendario"),
  TopTab(ROUTE_PERFIL, "Perfil / Pro", Icons.Rounded.Person, Icons.Outlined.Person, "tab_perfil"),
)

@Composable
fun AppNavigation(
  themeMode: AppThemeMode,
  onSetThemeMode: (AppThemeMode) -> Unit,
  onToggleTheme: () -> Unit,
  initialVueltaId: String? = null,
  auth: FirebaseAuth = Firebase.auth,
) {
  val context = LocalContext.current
  val localSessionStore = remember(context) { LocalSessionStore(context) }
  var showSplash by remember { mutableStateOf(true) }

  val cloudUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val localSession by localSessionStore.activeLocalSession.collectAsStateWithLifecycle()

  val activeSession: AppSession = when {
    cloudUser != null -> AppSession.GoogleCloud(
      uid = cloudUser!!.uid,
      displayName = cloudUser!!.displayName ?: "Usuario Mi Vuelta",
      email = cloudUser!!.email.orEmpty(),
    )
    localSession != null -> localSession!!
    else -> AppSession.SignedOut
  }

  if (showSplash) {
    SplashSequenceScreen(
      onSplashFinished = { showSplash = false },
    )
    return
  }

  when (activeSession) {
    is AppSession.SignedOut -> {
      AuthScreen(
        themeMode = themeMode,
        onToggleTheme = onToggleTheme,
        onAuthSuccess = {},
        onGuestMode = { localSessionStore.signInAsGuest() },
        onLocalSignIn = { email, password ->
          localSessionStore.signInLocalAccount(email, password).map { Unit }
        },
        onLocalRegister = { name, email, password ->
          localSessionStore.createLocalAccount(name, email, password).map { Unit }
        },
      )
    }
    is AppSession.GoogleCloud -> {
      key(activeSession.uid) {
        AuthenticatedApp(
          session = activeSession,
          cloudUser = cloudUser,
          localSessionStore = localSessionStore,
          themeMode = themeMode,
          onSetThemeMode = onSetThemeMode,
          initialVueltaId = initialVueltaId,
        )
      }
    }
    is AppSession.LocalAccount -> {
      key(activeSession.uid) {
        AuthenticatedApp(
          session = activeSession,
          cloudUser = null,
          localSessionStore = localSessionStore,
          themeMode = themeMode,
          onSetThemeMode = onSetThemeMode,
          initialVueltaId = initialVueltaId,
        )
      }
    }
  }
}

@Composable
private fun AuthenticatedApp(
  session: AppSession,
  cloudUser: FirebaseUser?,
  localSessionStore: LocalSessionStore,
  themeMode: AppThemeMode,
  onSetThemeMode: (AppThemeMode) -> Unit,
  initialVueltaId: String?,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val navController = rememberNavController()
  val snackbarHostState = remember { SnackbarHostState() }
  var showFreeLimitDialog by remember { mutableStateOf(false) }

  val uid = when (session) {
    is AppSession.GoogleCloud -> session.uid
    is AppSession.LocalAccount -> session.uid
    AppSession.SignedOut -> return
  }
  val userEmail = when (session) {
    is AppSession.GoogleCloud -> session.email
    is AppSession.LocalAccount -> session.email
    AppSession.SignedOut -> ""
  }
  val isCloud = session is AppSession.GoogleCloud
  val isGuest = (session as? AppSession.LocalAccount)?.isGuest == true

  val viewModel: MainViewModel = viewModel(
    key = uid,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY])
        val scheduler = VueltaReminderScheduler(app)
        val dataSource = if (session is AppSession.GoogleCloud) {
          val databaseId = app.getString(R.string.firestore_database_id)
          val db = FirebaseFirestore.getInstance(databaseId)
          MiVueltaRepository(db)
        } else {
          val local = session as AppSession.LocalAccount
          LocalVueltaRepository(
            context = app,
            uid = local.uid,
            displayName = local.displayName,
            email = local.email,
          )
        }
        MainViewModel(
          repository = dataSource,
          reminderScheduler = scheduler,
          currentUserId = uid,
          firebaseUser = cloudUser,
        )
      }
    },
  )

  val vueltasState by viewModel.vueltasState.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  val familyGroup by viewModel.familyGroup.collectAsStateWithLifecycle()
  val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

  LaunchedEffect(initialVueltaId) {
    if (!initialVueltaId.isNullOrBlank()) {
      navController.navigate("$ROUTE_DETALLE/$initialVueltaId") {
        launchSingleTop = true
      }
    }
  }

  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      when (event) {
        is UiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
        is UiEvent.ShowFreeLimitDialog -> showFreeLimitDialog = true
        is UiEvent.VueltaSaved -> navController.popBackStack()
        is UiEvent.VueltaDeleted -> navController.popBackStack()
      }
    }
  }

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route
  val showBottomOrRail = currentRoute in setOf(ROUTE_INICIO, ROUTE_CALENDARIO, ROUTE_PERFIL)

  fun navigateTopTab(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 600.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (showBottomOrRail && !useNavRail) {
          NavigationBar {
            topTabs.forEach { tab ->
              val selected = currentRoute == tab.route
              NavigationBarItem(
                selected = selected,
                onClick = { navigateTopTab(tab.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label) },
                modifier = Modifier.testTag(tab.testTag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (showBottomOrRail && useNavRail) {
          NavigationRail(modifier = Modifier.fillMaxHeight()) {
            topTabs.forEach { tab ->
              val selected = currentRoute == tab.route
              NavigationRailItem(
                selected = selected,
                onClick = { navigateTopTab(tab.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label) },
                modifier = Modifier.testTag(tab.testTag),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f)) {
          NavHost(
            navController = navController,
            startDestination = ROUTE_INICIO,
          ) {
            composable(ROUTE_INICIO) {
              InicioScreen(
                vueltasState = vueltasState,
                userProfile = userProfile,
                currentUserId = uid,
                onOpenCreate = { navController.navigate(ROUTE_CREAR) },
                onOpenDetail = { id -> navController.navigate("$ROUTE_DETALLE/$id") },
                onToggleDone = { vuelta -> viewModel.toggleCompleted(vuelta) },
                onOpenPro = { navigateTopTab(ROUTE_PERFIL) },
              )
            }

            composable(ROUTE_CALENDARIO) {
              CalendarioScreen(
                vueltasState = vueltasState,
                onOpenCreate = { navController.navigate(ROUTE_CREAR) },
                onOpenDetail = { id -> navController.navigate("$ROUTE_DETALLE/$id") },
                onToggleDone = { vuelta -> viewModel.toggleCompleted(vuelta) },
              )
            }

            composable(ROUTE_PERFIL) {
              PerfilScreen(
                userProfile = userProfile,
                userEmail = userEmail,
                isCloudSession = isCloud,
                isGuestSession = isGuest,
                themeMode = themeMode,
                onSetThemeMode = onSetThemeMode,
                onConnectGoogle = {
                  onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = { localSessionStore.signOutLocal() },
                    onAuthError = { msg ->
                      scope.launch { snackbarHostState.showSnackbar(msg) }
                    },
                    scope = scope,
                  )
                },
                activeOwnCount = viewModel.activeVueltasCount(),
                familyGroup = familyGroup,
                onActivatePro = { viewModel.activateProTrial() },
                onSwitchFree = { viewModel.switchToFreePlan() },
                onCreateFamily = { name -> viewModel.createFamilyGroup(name) },
                onJoinFamily = { code -> viewModel.joinFamilyByCode(code) },
                onLeaveFamily = { famId -> viewModel.leaveFamilyGroup(famId) },
                onSignOut = {
                  if (isCloud) {
                    signOutUser(
                      credentialManager = credentialManager,
                      scope = scope,
                    )
                  } else {
                    localSessionStore.signOutLocal()
                  }
                },
              )
            }

            composable(
              route = "$ROUTE_CREAR?vueltaId={vueltaId}",
              arguments = listOf(
                navArgument("vueltaId") {
                  type = NavType.StringType
                  nullable = true
                  defaultValue = null
                }
              ),
            ) { entry ->
              val editId = entry.arguments?.getString("vueltaId")
              val existing = editId?.let { viewModel.findVueltaById(it) }
              CrearVueltaScreen(
                existingVuelta = existing,
                userProfile = userProfile,
                familyGroup = familyGroup,
                isSaving = isSaving,
                onBack = { navController.popBackStack() },
                onSave = { title, templateKey, notes, dueDateMillis, r2, r1, r0, hr, min, shared, photo ->
                  viewModel.saveVuelta(
                    existingId = editId,
                    title = title,
                    templateKey = templateKey,
                    notes = notes,
                    dueDateMillis = dueDateMillis,
                    remind2Days = r2,
                    remind1Day = r1,
                    remindSameDay = r0,
                    reminderHour = hr,
                    reminderMinute = min,
                    sharedWithFamily = shared,
                    receiptBase64 = photo,
                  )
                },
              )
            }

            composable(
              route = "$ROUTE_DETALLE/{vueltaId}",
              arguments = listOf(navArgument("vueltaId") { type = NavType.StringType }),
            ) { entry ->
              val vueltaId = entry.arguments?.getString("vueltaId").orEmpty()
              val vuelta = (vueltasState as? UiState.Success)?.data?.firstOrNull { it.id == vueltaId }
              DetalleVueltaScreen(
                vuelta = vuelta,
                currentUserId = uid,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate("$ROUTE_CREAR?vueltaId=$id") },
                onToggleDone = { v -> viewModel.toggleCompleted(v) },
                onDelete = { id -> viewModel.deleteVuelta(id) },
              )
            }
          }
        }
      }
    }
  }

  if (showFreeLimitDialog) {
    AlertDialog(
      onDismissRequest = { showFreeLimitDialog = false },
      title = { Text("Límite de 5 vueltas gratis") },
      text = {
        Text("Ya tienes 5 vueltas activas en el plan Gratis. Marca una como hecha o activa Mi Vuelta Pro (RD$150/mes) para tener vueltas ilimitadas y compartir con tu familia.")
      },
      confirmButton = {
        TextButton(
          onClick = {
            showFreeLimitDialog = false
            navController.popBackStack()
            navigateTopTab(ROUTE_PERFIL)
          }
        ) {
          Text("Ver Mi Vuelta Pro")
        }
      },
      dismissButton = {
        TextButton(onClick = { showFreeLimitDialog = false }) { Text("Cerrar") }
      },
    )
  }
}
