package com.d1onix.dishlab.data.session

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "graph_products")
data class GraphProductEntity(
    @PrimaryKey val productId: String,
    val sortOrder: Int,
    val xFraction: Float? = null,
    val yFraction: Float? = null,
)

@Entity(
    tableName = "graph_connections",
    primaryKeys = ["firstProductId", "secondProductId"],
)
data class GraphConnectionEntity(
    val firstProductId: String,
    val secondProductId: String,
)

@Dao
abstract class GraphDao {
    @Query("SELECT * FROM graph_products ORDER BY sortOrder")
    abstract fun observeProducts(): Flow<List<GraphProductEntity>>

    @Query("SELECT * FROM graph_connections")
    abstract fun observeConnections(): Flow<List<GraphConnectionEntity>>

    @Query("SELECT * FROM graph_products ORDER BY sortOrder")
    abstract suspend fun products(): List<GraphProductEntity>

    @Upsert
    abstract suspend fun upsertProduct(product: GraphProductEntity)

    @Upsert
    abstract suspend fun upsertProducts(products: List<GraphProductEntity>)

    @Upsert
    abstract suspend fun upsertConnections(connections: List<GraphConnectionEntity>)

    @Query("DELETE FROM graph_products WHERE productId = :id")
    abstract suspend fun deleteProduct(id: String)

    @Query(
        "DELETE FROM graph_connections " +
            "WHERE firstProductId = :id OR secondProductId = :id"
    )
    abstract suspend fun deleteProductConnections(id: String)

    @Query(
        "DELETE FROM graph_connections " +
            "WHERE firstProductId = :first AND secondProductId = :second"
    )
    abstract suspend fun deleteConnection(first: String, second: String)

    @Query(
        "UPDATE graph_products SET xFraction = :x, yFraction = :y " +
            "WHERE productId = :id"
    )
    abstract suspend fun updatePosition(id: String, x: Float, y: Float)

    @Query("DELETE FROM graph_connections")
    abstract suspend fun clearConnections()

    @Query("DELETE FROM graph_products")
    abstract suspend fun clearProducts()

    @Transaction
    open suspend fun add(productId: String) {
        val current = products()
        if (current.any { it.productId == productId }) return
        upsertProduct(GraphProductEntity(productId, current.size))
        upsertConnections(
            current.map { existing ->
                val (first, second) = listOf(existing.productId, productId).sorted()
                GraphConnectionEntity(first, second)
            }
        )
    }

    @Transaction
    open suspend fun remove(id: String) {
        deleteProductConnections(id)
        deleteProduct(id)
    }

    @Transaction
    open suspend fun replaceGraph(
        products: List<GraphProductEntity>,
        connections: List<GraphConnectionEntity>,
    ) {
        clearConnections()
        clearProducts()
        upsertProducts(products)
        upsertConnections(connections)
    }
}

@Database(
    entities = [GraphProductEntity::class, GraphConnectionEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(GraphDatabaseConstructor::class)
abstract class GraphDatabase : RoomDatabase() {
    abstract fun graphDao(): GraphDao
}

@Suppress("KotlinNoActualForExpect")
expect object GraphDatabaseConstructor : RoomDatabaseConstructor<GraphDatabase> {
    override fun initialize(): GraphDatabase
}

fun buildGraphDatabase(builder: RoomDatabase.Builder<GraphDatabase>): GraphDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .build()
