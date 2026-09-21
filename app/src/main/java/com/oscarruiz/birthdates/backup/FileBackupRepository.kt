package com.oscarruiz.birthdates.backup

import android.content.Context
import android.net.Uri
import com.oscarruiz.birthdates.data.BirthdayRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Exportar e importar un archivo JSON con el selector de archivos del sistema (sin permisos). */
class FileBackupRepository(
    private val context: Context,
    private val birthdays: BirthdayRepository,
) {
    /** Devuelve cuántos cumpleaños se han exportado. */
    suspend fun exportTo(uri: Uri): Int = withContext(Dispatchers.IO) {
        val list = birthdays.getAll()
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Cannot open $uri")
        stream.use { it.write(BackupSerializer.encode(list).toByteArray(Charsets.UTF_8)) }
        list.size
    }

    /** Devuelve cuántos cumpleaños se han añadido o actualizado. */
    suspend fun importFrom(uri: Uri): Int = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open $uri")
        val text = stream.use { it.bufferedReader(Charsets.UTF_8).readText() }
        birthdays.merge(BackupSerializer.decode(text))
    }
}
