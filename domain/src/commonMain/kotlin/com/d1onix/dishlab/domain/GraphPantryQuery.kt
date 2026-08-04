package com.d1onix.dishlab.domain

import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductConnection
import com.d1onix.dishlab.domain.model.ProductId

data class PantryMatchQuery(
    val exactGroups: List<List<String>>,
    val ingredients: List<String>,
)

fun translateGraphToPantryQuery(
    products: List<Product>,
    connections: Set<ProductConnection>,
): PantryMatchQuery {
    val byId = products.associateBy(Product::id)
    val neighbors = products.associate { it.id to mutableSetOf<ProductId>() }.toMutableMap()
    connections.forEach { edge ->
        if (edge.first in byId && edge.second in byId) {
            neighbors.getValue(edge.first) += edge.second
            neighbors.getValue(edge.second) += edge.first
        }
    }
    val unseen = products.map(Product::id).toMutableSet()
    val groups = mutableListOf<List<ProductId>>()
    while (unseen.isNotEmpty()) {
        val component = mutableListOf<ProductId>()
        val queue = ArrayDeque<ProductId>().apply { add(unseen.first()) }
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (!unseen.remove(id)) continue
            component += id
            queue.addAll(neighbors.getValue(id))
        }
        groups += component
    }
    return PantryMatchQuery(
        exactGroups = groups.filter { it.size > 1 }.map { ids -> ids.flatMap { byId.getValue(it).canonicalTags }.distinct() },
        ingredients = groups.filter { it.size == 1 }.flatMap { byId.getValue(it.single()).canonicalTags }.distinct(),
    )
}
