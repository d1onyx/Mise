package com.dishlab.application.service

fun interface ProductValidationProvider {
    suspend fun validate(name: String): Boolean
}

class NoOpProductValidationProvider : ProductValidationProvider {
    override suspend fun validate(name: String) = true
}
