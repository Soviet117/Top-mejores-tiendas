package com.example.topmejorestiendas.utils

/**
 * Extensiones Kotlin para SessionManager (que es Java).
 * Permiten usar propiedades idiomáticas desde los Repositories en Kotlin.
 */

/** Token JWT de la sesión actual, o null si no está autenticado. */
val SessionManager.authToken: String?
    get() = this.getAuthToken()

/** ID del usuario de la sesión actual. */
val SessionManager.userId: Int
    get() = this.getUserId()

/** Indica si el usuario de la sesión es dueño de negocio. */
val SessionManager.favorites: Set<String>
    get() = this.getFavorites()
