package com.oscarruiz.birthdates.data

import com.oscarruiz.birthdates.data.local.BirthdayDao
import com.oscarruiz.birthdates.data.local.toDomain
import com.oscarruiz.birthdates.data.local.toEntity
import com.oscarruiz.birthdates.domain.model.Birthday
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Único punto de acceso a los cumpleaños. [onDataChanged] se llama tras cada cambio
 * para marcar que hay una copia de seguridad pendiente.
 */
class BirthdayRepository(
    private val dao: BirthdayDao,
    private val onDataChanged: suspend () -> Unit,
) {
    fun observeAll(): Flow<List<Birthday>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAll(): List<Birthday> = dao.getAll().map { it.toDomain() }

    suspend fun get(id: String): Birthday? = dao.getById(id)?.toDomain()

    suspend fun save(birthday: Birthday) {
        dao.upsert(birthday.copy(updatedAt = System.currentTimeMillis()).toEntity())
        onDataChanged()
    }

    /** Vuelve a insertar un cumpleaños borrado (Deshacer) sin tocar su fecha de modificación. */
    suspend fun restore(birthday: Birthday) {
        dao.upsert(birthday.toEntity())
        onDataChanged()
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
        onDataChanged()
    }

    /** Sustituye todo por el contenido de una copia (restaurar desde Drive). */
    suspend fun replaceAll(birthdays: List<Birthday>, markChanged: Boolean = false) {
        dao.replaceAll(birthdays.map { it.toEntity() })
        if (markChanged) onDataChanged()
    }

    /**
     * Añade los cumpleaños de una importación. Si el id ya existe, gana la versión más reciente.
     * Devuelve cuántos se han añadido o actualizado.
     */
    suspend fun merge(incoming: List<Birthday>): Int {
        val existing = dao.getAll().associateBy { it.id }
        val toWrite = incoming.filter { item ->
            val current = existing[item.id]
            current == null || current.updatedAt < item.updatedAt
        }
        if (toWrite.isNotEmpty()) {
            dao.upsertAll(toWrite.map { it.toEntity() })
            onDataChanged()
        }
        return toWrite.size
    }
}
