package com.dishlab.infrastructure.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object FirebaseInitializer {
    fun init() {
        if (FirebaseApp.getApps().isEmpty()) {
            val projectId = System.getenv("FIREBASE_PROJECT_ID") ?: "dishlab-1"
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .setProjectId(projectId)
                .build()

            FirebaseApp.initializeApp(options)
        }
    }
}