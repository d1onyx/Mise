package com.dishlab.backend

import com.dishlab.infrastructure.firebase.DevFirebaseAuthVerifier
import io.ktor.server.application.Application

/**
 * Test composition root — wires the full app but swaps the production
 * Firebase verifier for [DevFirebaseAuthVerifier], which accepts `Bearer :<uid>`
 * tokens. Skips `FirebaseInitializer.init()`, so tests need no Firebase
 * credentials. Acceptance tests use `application { testModule() }`.
 */
fun Application.testModule() = appModule(DevFirebaseAuthVerifier())
