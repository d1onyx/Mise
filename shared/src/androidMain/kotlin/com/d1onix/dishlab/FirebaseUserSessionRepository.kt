package com.d1onix.dishlab

import com.d1onix.dishlab.data.storage.UserSessionPreferenceKeys
import com.d1onix.dishlab.domain.model.UserSession
import com.d1onix.dishlab.domain.repository.UserSessionRepository
import com.d1onyx.core.datastore.KeyValueStorage
import com.google.firebase.auth.FirebaseAuth
import com.d1onyx.core.essentials.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

/** Android account session backed by Firebase Authentication. */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class FirebaseUserSessionRepository(
    private val firebaseAuth: FirebaseAuth,
    private val storage: KeyValueStorage,
) : UserSessionRepository {

    override val session: Flow<UserSession> = combine(
        firebaseAuth.authenticatedState(),
        storage.observe(UserSessionPreferenceKeys.OnboardingCompleted),
    ) { isAuthenticated, onboardingCompleted ->
        UserSession(
            isAuthenticated = isAuthenticated,
            onboardingCompleted = onboardingCompleted ?: false,
        )
    }

    override suspend fun signIn(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun register(email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        storage.put(UserSessionPreferenceKeys.OnboardingCompleted, false)
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun markOnboardingCompleted() {
        storage.put(UserSessionPreferenceKeys.OnboardingCompleted, true)
    }
}

private fun FirebaseAuth.authenticatedState(): Flow<Boolean> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser != null)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}.distinctUntilChanged()
