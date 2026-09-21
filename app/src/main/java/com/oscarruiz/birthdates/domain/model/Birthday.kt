package com.oscarruiz.birthdates.domain.model

import java.util.UUID

enum class Relation { FAMILY, FRIEND, WORK, OTHER }

/**
 * Un cumpleaños guardado. Día, mes y año van por separado porque el año es opcional.
 * [id] es un UUID estable para poder fusionar copias de seguridad sin duplicados.
 */
data class Birthday(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val day: Int,
    val month: Int,
    val year: Int? = null,
    val relation: Relation? = null,
    val notes: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)
