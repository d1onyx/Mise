package com.d1onix.dishlab.feature.home.presentation.auth

import com.d1onix.dishlab.domain.model.ProfileSettings
import com.d1onix.dishlab.domain.model.UserSession
import com.d1onix.dishlab.domain.repository.ProfileSettingsRepository
import com.d1onix.dishlab.domain.repository.UserSessionRepository
import com.d1onix.dishlab.feature.home.navigation.HomeRouter
import com.d1onix.dishlab.feature.home.navigation.ProtectedDestination
import com.d1onyx.core.essentials.exceptions.ExceptionHandler
import com.d1onyx.core.essentials.logger.DefaultLogger
import com.d1onyx.core.essentials.logger.RecordingLogSink
import com.d1onyx.core.presentation.CommonDependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `registration persists name and continues through onboarding`() = runTest(dispatcher) {
        val sessions = FakeSessions()
        val profile = FakeProfileSettings()
        val router = FakeHomeRouter()
        val viewModel = viewModel(sessions, profile, router, ProtectedDestination.Comparison)

        viewModel.onAction(AuthAction.ModeChanged(AuthMode.Register))
        viewModel.onAction(AuthAction.DisplayNameChanged("Dana"))
        viewModel.onAction(AuthAction.EmailChanged("dana@example.com"))
        viewModel.onAction(AuthAction.PasswordChanged("secret1"))
        viewModel.onAction(AuthAction.ContinueClicked)
        testScheduler.advanceUntilIdle()

        assertEquals("dana@example.com" to "secret1", sessions.registered)
        assertEquals("Dana", profile.name)
        assertEquals(ProtectedDestination.Comparison, router.onboardingDestination)
    }

    @Test
    fun `sign in resumes requested protected destination`() = runTest(dispatcher) {
        val sessions = FakeSessions()
        val router = FakeHomeRouter()
        val viewModel = viewModel(
            sessions,
            FakeProfileSettings(),
            router,
            ProtectedDestination.RecipeDiscovery,
        )

        viewModel.onAction(AuthAction.EmailChanged("dana@example.com"))
        viewModel.onAction(AuthAction.PasswordChanged("secret1"))
        viewModel.onAction(AuthAction.ContinueClicked)
        testScheduler.advanceUntilIdle()

        assertEquals("dana@example.com" to "secret1", sessions.signedIn)
        assertEquals(ProtectedDestination.RecipeDiscovery, router.completedDestination)
    }

    private fun viewModel(
        sessions: UserSessionRepository,
        profile: ProfileSettingsRepository,
        router: HomeRouter,
        destination: ProtectedDestination,
    ) = AuthViewModel(
        dependencies = CommonDependencies(DefaultLogger(RecordingLogSink()), ExceptionHandler { }),
        destination = destination,
        sessions = sessions,
        profileSettings = profile,
        router = router,
    )

    private class FakeSessions : UserSessionRepository {
        override val session: Flow<UserSession> = MutableStateFlow(UserSession())
        var signedIn: Pair<String, String>? = null
        var registered: Pair<String, String>? = null
        override suspend fun signIn(email: String, password: String) {
            signedIn = email to password
        }
        override suspend fun register(email: String, password: String) {
            registered = email to password
        }
        override suspend fun signOut() = Unit
        override suspend fun markOnboardingCompleted() = Unit
    }

    private class FakeProfileSettings : ProfileSettingsRepository {
        override val settings: Flow<ProfileSettings> = MutableStateFlow(ProfileSettings())
        var name: String? = null
        override suspend fun setDisplayName(value: String) {
            name = value
        }
        override suspend fun setAutoConnectNewProducts(enabled: Boolean) = Unit
        override suspend fun setReduceGraphMotion(enabled: Boolean) = Unit
        override suspend fun setShowProductScores(enabled: Boolean) = Unit
    }

    private class FakeHomeRouter : HomeRouter {
        var onboardingDestination: ProtectedDestination? = null
        var completedDestination: ProtectedDestination? = null
        override fun openPostRegistrationOnboarding(destination: ProtectedDestination) {
            onboardingDestination = destination
        }
        override fun completeProtectedNavigation(destination: ProtectedDestination) {
            completedDestination = destination
        }
        override fun openScanner() = Unit
        override fun openSavedRecipes() = Unit
        override fun openHistory() = Unit
        override fun openProfile() = Unit
        override fun openAuth(destination: ProtectedDestination) = Unit
        override fun openPreferenceSetup() = Unit
        override fun openComparison() = Unit
        override fun openRecipeDiscovery() = Unit
        override fun goBack() = Unit
    }
}
