package com.d1onix.dishlab.data.session

import com.d1onix.dishlab.domain.model.ProductConnection
import com.d1onix.dishlab.domain.model.ProductGraphPosition
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.repository.ScanSessionStore
import com.d1onyx.core.essentials.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class RoomScanSessionStore(
    database: GraphDatabase,
) : ScanSessionStore {
    private val dao = database.graphDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val productEntities = dao.observeProducts().stateIn(
        scope,
        SharingStarted.Eagerly,
        emptyList(),
    )

    override val products: StateFlow<List<ProductId>> = productEntities
        .map { entities -> entities.map { ProductId(it.productId) } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val positions: StateFlow<Map<ProductId, ProductGraphPosition>> = productEntities
        .map { entities ->
            entities.mapNotNull { entity ->
                val x = entity.xFraction ?: return@mapNotNull null
                val y = entity.yFraction ?: return@mapNotNull null
                ProductId(entity.productId) to ProductGraphPosition(x, y)
            }.toMap()
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    override val connections: StateFlow<Set<ProductConnection>> = dao.observeConnections()
        .map { entities ->
            entities.mapTo(mutableSetOf()) {
                ProductConnection.between(
                    ProductId(it.firstProductId),
                    ProductId(it.secondProductId),
                )
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    override suspend fun add(id: ProductId) {
        dao.add(id.value)
    }

    override suspend fun remove(id: ProductId) = dao.remove(id.value)

    override suspend fun connect(first: ProductId, second: ProductId) {
        if (first == second) return
        val productIds = dao.products().mapTo(mutableSetOf()) { it.productId }
        if (first.value !in productIds || second.value !in productIds) return
        dao.upsertConnections(listOf(connectionEntity(first, second)))
    }

    override suspend fun disconnect(first: ProductId, second: ProductId) {
        val connection = ProductConnection.between(first, second)
        dao.deleteConnection(connection.first.value, connection.second.value)
    }

    override suspend fun updatePosition(id: ProductId, position: ProductGraphPosition) {
        dao.updatePosition(
            id = id.value,
            x = position.xFraction.coerceIn(0f, 1f),
            y = position.yFraction.coerceIn(0f, 1f),
        )
    }

    override suspend fun reset(ids: List<ProductId>) {
        val distinct = ids.distinct()
        dao.replaceGraph(
            products = distinct.mapIndexed { index, id ->
                GraphProductEntity(id.value, index)
            },
            connections = distinct.flatMapIndexed { index, first ->
                distinct.drop(index + 1).map { second -> connectionEntity(first, second) }
            },
        )
    }

    private fun connectionEntity(
        first: ProductId,
        second: ProductId,
    ): GraphConnectionEntity {
        val connection = ProductConnection.between(first, second)
        return GraphConnectionEntity(connection.first.value, connection.second.value)
    }
}
