package com.oscarruiz.birthdates.backup

import com.oscarruiz.birthdates.domain.BirthdayCalculator
import com.oscarruiz.birthdates.domain.model.Birthday
import com.oscarruiz.birthdates.domain.model.Relation
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class BackupFile(
    val schemaVersion: Int,
    val exportedAt: Long,
    val birthdays: List<BackupBirthday>,
)

@Serializable
data class BackupBirthday(
    val id: String = "",
    val name: String,
    val day: Int,
    val month: Int,
    val year: Int? = null,
    val relation: String? = null,
    val notes: String? = null,
    val updatedAt: Long = 0,
)

class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Mismo formato JSON para la copia en Drive y para exportar/importar un archivo. */
object BackupSerializer {

    const val SCHEMA_VERSION = 1

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(birthdays: List<Birthday>, now: Long = System.currentTimeMillis()): String =
        json.encodeToString(
            BackupFile.serializer(),
            BackupFile(SCHEMA_VERSION, now, birthdays.map { it.toBackup() }),
        )

    /** Devuelve los cumpleaños válidos del archivo. Descarta entradas corruptas. */
    fun decode(text: String): List<Birthday> {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: IllegalArgumentException) { // Incluye SerializationException.
            throw InvalidBackupException("Not a valid backup file", e)
        }
        if (file.schemaVersion > SCHEMA_VERSION) {
            throw InvalidBackupException("Backup created by a newer version of the app")
        }
        return file.birthdays.mapNotNull { it.toDomainOrNull() }
    }

    private fun Birthday.toBackup() = BackupBirthday(
        id = id,
        name = name,
        day = day,
        month = month,
        year = year,
        relation = relation?.name,
        notes = notes,
        updatedAt = updatedAt,
    )

    private fun BackupBirthday.toDomainOrNull(): Birthday? {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || !BirthdayCalculator.isValidDate(day, month, year)) return null
        return Birthday(
            id = id.ifBlank { UUID.randomUUID().toString() },
            name = cleanName,
            day = day,
            month = month,
            year = year,
            relation = relation?.let { value -> Relation.entries.firstOrNull { it.name == value } },
            notes = notes?.trim()?.ifEmpty { null },
            updatedAt = updatedAt,
        )
    }
}
