package com.dishlab.infrastructure.db

import com.dishlab.application.service.UserAccountRepository
import com.dishlab.domain.model.UserAccount
import java.util.UUID
import javax.sql.DataSource

class PostgresUserAccountRepository(
    private val dataSource: DataSource,
) : UserAccountRepository {

    override fun findOrCreateByFirebaseUid(firebaseUid: String): UserAccount {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO users (firebase_uid) VALUES (?) ON CONFLICT (firebase_uid) DO UPDATE SET firebase_uid = EXCLUDED.firebase_uid RETURNING id",
            ).use { ps ->
                ps.setString(1, firebaseUid)
                ps.executeQuery().use { rs ->
                    rs.next()
                    return UserAccount(
                        id = rs.getObject("id", UUID::class.java),
                        firebaseUid = firebaseUid,
                    )
                }
            }
        }
    }

    override fun save(user: UserAccount): UserAccount = user
}
