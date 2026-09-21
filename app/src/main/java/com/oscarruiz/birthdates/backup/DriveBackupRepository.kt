package com.oscarruiz.birthdates.backup

import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.oscarruiz.birthdates.BirthdaysApp
import com.oscarruiz.birthdates.data.BirthdayRepository
import com.oscarruiz.birthdates.data.SettingsRepository
import com.oscarruiz.birthdates.domain.model.Birthday
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

class DriveException(val code: Int, body: String) : IOException("Drive HTTP $code: $body")

data class RemoteBackup(
    val fileId: String,
    val modifiedAt: Instant?,
    val birthdays: List<Birthday>,
)

/**
 * Copia en la carpeta oculta de la app en el Google Drive del usuario (appDataFolder).
 * Solo pide el ámbito drive.appdata: la app no ve el resto del Drive y nosotros no guardamos nada.
 * Usa la API REST v3 directamente para no añadir la librería completa de Google.
 */
class DriveBackupRepository(
    private val context: Context,
    private val birthdays: BirthdayRepository,
    private val settings: SettingsRepository,
) {
    companion object {
        private const val FILE_NAME = "birthdays.json"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
        private val DRIVE_APPDATA = Scope("https://www.googleapis.com/auth/drive.appdata")
    }

    private val authClient get() = Identity.getAuthorizationClient(context)

    /** Si el resultado trae resolución, la UI debe lanzar su PendingIntent para que el usuario acepte. */
    suspend fun authorize(): AuthorizationResult =
        authClient.authorize(
            AuthorizationRequest.builder().setRequestedScopes(listOf(DRIVE_APPDATA)).build(),
        ).await()

    fun tokenFromIntent(data: Intent?): String? {
        if (data == null) return null
        return try {
            authClient.getAuthorizationResultFromIntent(data).accessToken
        } catch (e: Exception) {
            null
        }
    }

    /** Para trabajos en segundo plano: solo devuelve token si ya hay permiso concedido. */
    suspend fun silentToken(): String? = try {
        val result = authorize()
        if (result.hasResolution()) null else result.accessToken
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    suspend fun accountEmail(token: String): String? = try {
        val response = request("GET", "$API/about?fields=${enc("user(emailAddress)")}", token)
        Json.parseToJsonElement(response).jsonObject["user"]?.jsonObject
            ?.get("emailAddress")?.jsonPrimitive?.content
    } catch (e: IOException) {
        null
    }

    /** Sube todos los cumpleaños. Devuelve cuántos se han copiado. */
    suspend fun backupNow(token: String): Int {
        val list = birthdays.getAll()
        upload(token, BackupSerializer.encode(list))
        settings.onBackupCompleted(System.currentTimeMillis())
        return list.size
    }

    suspend fun fetchBackup(token: String): RemoteBackup? {
        val file = findFile(token) ?: return null
        val text = request("GET", "$API/files/${file.id}?alt=media", token)
        return RemoteBackup(file.id, file.modifiedTime, BackupSerializer.decode(text))
    }

    suspend fun restore(backup: RemoteBackup) {
        birthdays.replaceAll(backup.birthdays, markChanged = false)
    }

    // ---------- REST ----------

    private data class DriveFile(val id: String, val modifiedTime: Instant?)

    private suspend fun findFile(token: String): DriveFile? {
        val url = "$API/files?spaces=appDataFolder" +
            "&q=${enc("name = '$FILE_NAME' and trashed = false")}" +
            "&fields=${enc("files(id,modifiedTime)")}" +
            "&orderBy=${enc("modifiedTime desc")}"
        val response = request("GET", url, token)
        val first = Json.parseToJsonElement(response).jsonObject["files"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: return null
        val id = first["id"]?.jsonPrimitive?.content ?: return null
        val modified = first["modifiedTime"]?.jsonPrimitive?.content?.let {
            runCatching { Instant.parse(it) }.getOrNull()
        }
        return DriveFile(id, modified)
    }

    private suspend fun upload(token: String, content: String) {
        val existing = findFile(token)
        if (existing == null) {
            val boundary = "birthdays_" + UUID.randomUUID()
            val metadata = """{"name":"$FILE_NAME","parents":["appDataFolder"]}"""
            val body = buildString {
                append("--").append(boundary).append("\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(metadata).append("\r\n")
                append("--").append(boundary).append("\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(content).append("\r\n")
                append("--").append(boundary).append("--")
            }.toByteArray(Charsets.UTF_8)
            request(
                "POST", "$UPLOAD/files?uploadType=multipart", token,
                contentType = "multipart/related; boundary=$boundary", body = body,
            )
        } else {
            // HttpURLConnection no admite PATCH: se usa la cabecera de sobrescritura que acepta Google.
            request(
                "POST", "$UPLOAD/files/${existing.id}?uploadType=media", token,
                contentType = "application/json; charset=UTF-8",
                body = content.toByteArray(Charsets.UTF_8),
                methodOverride = "PATCH",
            )
        }
    }

    private suspend fun request(
        method: String,
        url: String,
        token: String,
        contentType: String? = null,
        body: ByteArray? = null,
        methodOverride: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $token")
            methodOverride?.let { setRequestProperty("X-HTTP-Method-Override", it) }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", contentType)
                setFixedLengthStreamingMode(body.size)
            }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw DriveException(code, text)
            text
        } finally {
            connection.disconnect()
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}

/** Copia automática: una vez al día, con conexión, solo si hay cambios y Drive está conectado. */
class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as BirthdaysApp).container
        val settings = container.settingsRepository.settings.first()
        if (!settings.driveConnected || !settings.autoBackup || !settings.backupPending) return Result.success()

        val token = container.driveBackup.silentToken() ?: return Result.success()
        return try {
            container.driveBackup.backupNow(token)
            Result.success()
        } catch (e: IOException) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "daily_drive_backup"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
