package com.dishlab.application.service

import com.dishlab.domain.model.UserAccount
import com.dishlab.domain.model.UserConsent
import com.dishlab.domain.model.UserProfile
import com.dishlab.domain.model.UserSettings
import com.dishlab.domain.policy.UserProfileValidator
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CurrentUserResolver(
    private val users: UserAccountRepository,
) {
    fun resolve(firebaseUid: String): UserAccount = users.findOrCreateByFirebaseUid(firebaseUid)
}

interface UserAccountRepository {
    fun findOrCreateByFirebaseUid(firebaseUid: String): UserAccount
    fun save(user: UserAccount): UserAccount
}

class InMemoryUserAccountRepository : UserAccountRepository {
    private val usersByFirebaseUid = ConcurrentHashMap<String, UserAccount>()

    override fun findOrCreateByFirebaseUid(firebaseUid: String): UserAccount =
        usersByFirebaseUid.computeIfAbsent(firebaseUid) {
            UserAccount(
                id = UUID.randomUUID(),
                firebaseUid = firebaseUid,
            )
        }

    override fun save(user: UserAccount): UserAccount {
        usersByFirebaseUid[user.firebaseUid] = user.copy(updatedAt = Instant.now())
        return usersByFirebaseUid.getValue(user.firebaseUid)
    }
}

class IdentityService(
    private val currentUserResolver: CurrentUserResolver,
    private val users: UserAccountRepository,
) {
    fun getMe(firebaseUid: String): UserAccount = currentUserResolver.resolve(firebaseUid)

    fun updateProfile(firebaseUid: String, profile: UserProfile): UserAccount {
        UserProfileValidator.validate(profile)
        val user = currentUserResolver.resolve(firebaseUid)
        return users.save(user.copy(profile = profile, deleted = false))
    }

    fun updateAllergens(firebaseUid: String, allergens: Set<String>): UserAccount {
        val user = currentUserResolver.resolve(firebaseUid)
        return users.save(user.copy(allergens = allergens.normalizedDistinct()))
    }

    fun updateDiets(firebaseUid: String, diets: Set<String>): UserAccount {
        val user = currentUserResolver.resolve(firebaseUid)
        return users.save(user.copy(diets = diets.normalizedDistinct()))
    }

    fun updateEquipment(firebaseUid: String, equipment: Set<String>): UserAccount {
        val user = currentUserResolver.resolve(firebaseUid)
        return users.save(user.copy(equipment = equipment.normalizedDistinct()))
    }

    fun updateGoals(firebaseUid: String, goals: Set<String>): UserAccount {
        val user = currentUserResolver.resolve(firebaseUid)
        return users.save(user.copy(goals = goals.normalizedDistinct()))
    }


    fun updateSettings(firebaseUid: String, settings: UserSettings): UserAccount {
        val user = currentUserResolver.resolve(firebaseUid)
        return users.save(user.copy(settings = settings))
    }

    fun setConsent(firebaseUid: String, consent: UserConsent): UserAccount {
        val user = currentUserResolver.resolve(firebaseUid)
        val consents = user.consents.filterNot { it.type == consent.type } + consent
        return users.save(user.copy(consents = consents))
    }

    fun deleteMe(firebaseUid: String): UserAccount {
        val user = currentUserResolver.resolve(firebaseUid)
        return users.save(
            user.copy(
                profile = null,
                allergens = emptySet(),
                diets = emptySet(),
                equipment = emptySet(),
                goals = emptySet(),
                consents = emptyList(),
                deleted = true,
            ),
        )
    }
}

private fun Set<String>.normalizedDistinct(): Set<String> =
    map { it.trim() }
        .filter { it.isNotBlank() }
        .toSet()
