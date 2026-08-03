package com.dishlab.infrastructure.firebase

import com.google.firebase.auth.FirebaseAuth
import java.util.Base64

data class VerifiedFirebaseUser(
    val uid: String,
    val email: String? = null,
)

interface FirebaseAuthVerifier {
    suspend fun verify(idToken: String): VerifiedFirebaseUser?
}

class DevFirebaseAuthVerifier : FirebaseAuthVerifier {
    override suspend fun verify(idToken: String): VerifiedFirebaseUser? {
        // ":uid" format for tests (e.g. "Bearer :my-test-user")
        if (idToken.startsWith(":")) {
            val uid = idToken.removePrefix(":").trim()
            return uid.takeIf { it.isNotBlank() }?.let { VerifiedFirebaseUser(uid = it) }
        }
        // Real Firebase JWT — decode payload without signature verification
        // to extract the stable user UID (needed for local dev with a real Android app)
        return runCatching {
            val parts = idToken.split(".")
            if (parts.size < 2) return null
            val paddedPayload = parts[1].let { it + "=".repeat((4 - it.length % 4) % 4) }
            val json = String(Base64.getUrlDecoder().decode(paddedPayload))
            val uid = Regex(""""user_id"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1)
                ?: Regex(""""sub"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1)
                ?: return null
            val email = Regex(""""email"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1)
            VerifiedFirebaseUser(uid = uid, email = email)
        }.getOrNull()
    }
}

class FirebaseAuthVerifierImpl : FirebaseAuthVerifier {
    override suspend fun verify(idToken: String): VerifiedFirebaseUser? {
        return try{
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken)

            VerifiedFirebaseUser(
                uid = decodedToken.uid,
                email = decodedToken.email
            )
        }catch (e: Exception){
            println("Token verification failed: ${e.message}")
            null
        }
    }
}
