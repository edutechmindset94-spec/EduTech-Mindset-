package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.ArabicStrings
import com.example.core.ConnectivityObserver
import com.example.core.EnglishStrings
import com.example.core.LocalAppPreferences
import com.example.core.LocalAppStrings
import com.example.core.PreferencesStore
import com.example.core.configureAppCheck
import com.example.data.repository.AiEducationRepository
import com.example.data.repository.EduTechRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.EduTechAppShell
import com.example.ui.theme.EduTechMindsetTheme
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Step 1: Configure Firebase App Check with launch intent before any UI or ViewModels.
        configureAppCheck(this, intent)

        setContent {
            AppRoot()
        }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesStore = remember(context) { PreferencesStore(context) }
    val localPrefsFlow = remember(preferencesStore) { preferencesStore.preferences }
    val localPrefs by localPrefsFlow.collectAsStateWithLifecycle(
        initialValue = LocalAppPreferences()
    )
    val firebaseAuth = remember { Firebase.auth }
    val authFlow = remember(firebaseAuth) { firebaseAuth.authStateFlow() }
    val currentUser by authFlow.collectAsStateWithLifecycle(
        initialValue = firebaseAuth.currentUser
    )

    val isArabic = localPrefs.language != "EN"
    val appStrings = if (isArabic) ArabicStrings else EnglishStrings
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalAppStrings provides appStrings
    ) {
        EduTechMindsetTheme(darkTheme = localPrefs.darkMode) {
            val user = currentUser
            if (user == null) {
                AuthScreen(
                    darkMode = localPrefs.darkMode,
                    onToggleDarkMode = {
                        scope.launch { preferencesStore.setDarkMode(!localPrefs.darkMode) }
                    },
                    onToggleLanguage = {
                        scope.launch {
                            preferencesStore.setLanguage(if (isArabic) "EN" else "AR")
                        }
                    },
                    onAuthSuccess = {}
                )
            } else {
                val uid = user.uid
                key(uid) {
                    val mainViewModel: MainViewModel = viewModel(
                        key = uid,
                        factory = viewModelFactory {
                            initializer {
                                val app = checkNotNull(this[APPLICATION_KEY]) {
                                    "APPLICATION_KEY missing from CreationExtras"
                                }
                                MainViewModel(
                                    repository = EduTechRepository(app),
                                    aiRepository = AiEducationRepository(),
                                    preferencesStore = PreferencesStore(app),
                                    connectivityObserver = ConnectivityObserver(app),
                                    currentUserId = uid,
                                    initialDisplayName = user.displayName ?: "أ. باسل الفقعاوي"
                                )
                            }
                        }
                    )
                    EduTechAppShell(
                        currentUserId = uid,
                        viewModel = mainViewModel
                    )
                }
            }
        }
    }
}
