package eu.kanade.tachiyomi.data.sync.service

import android.content.Context
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.sync.SyncNotifier
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.PUT
import eu.kanade.tachiyomi.network.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import logcat.logcat
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.HttpURLConnection
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class SyncYomiSyncService(
    context: Context,
    json: Json,
    syncPreferences: SyncPreferences,
    private val notifier: SyncNotifier,

    private val protoBuf: ProtoBuf = Injekt.get(),
) : SyncService(context, json, syncPreferences) {

    private class SyncYomiException(message: String?) : Exception(message)

    @Serializable
    private data class SyncEvent(
        val event: SyncEventStatus,
        @SerialName("device_name")
        val deviceName: String? = null,
        val message: String? = null,
    )

    @Serializable
    private enum class SyncEventStatus {
        SYNC_STARTED,
        SYNC_SUCCESS,
        SYNC_FAILED,
        SYNC_ERROR,
        SYNC_CANCELLED,
    }

    override suspend fun doSync(
        localBackupCreator: suspend () -> Backup,
        isLocalDirty: Boolean,
    ): Backup? {
        reportSyncEvent(SyncEventStatus.SYNC_STARTED)

        try {
            val (remoteData, etag) = pullSyncData()

            if (remoteData == null) {
                // Remote server not modified (304) or not found
                if (!isLocalDirty) {
                    logcat(LogPriority.INFO) { "Both local and remote are clean, skipping sync" }
                    reportSyncEvent(SyncEventStatus.SYNC_SUCCESS)
                    return null
                }

                // Local is dirty, remote is not. Push local to remote.
                val localBackup = localBackupCreator()
                val localSyncData = SyncData(
                    deviceId = syncPreferences.uniqueDeviceID(),
                    backup = localBackup,
                )
                val success = pushSyncData(localSyncData, etag)
                if (success) {
                    reportSyncEvent(SyncEventStatus.SYNC_SUCCESS)
                } else {
                    reportSyncEvent(SyncEventStatus.SYNC_FAILED, "Failed to push sync data")
                    // Throw to prevent SyncManager from updating timestamp
                    throw SyncYomiException("Failed to push sync data")
                }
                // Return null so SyncManager knows there's nothing to restore/merge
                return null
            }

            // Remote data is available
            if (!isLocalDirty) {
                // Local is clean, remote is dirty. Just take remote data.
                logcat(LogPriority.INFO) { "Local is clean, applying remote changes" }
                reportSyncEvent(SyncEventStatus.SYNC_SUCCESS)
                return remoteData.backup
            }

            // Both local and remote are dirty. Merge and push.
            val localBackup = localBackupCreator()
            val localSyncData = SyncData(
                deviceId = syncPreferences.uniqueDeviceID(),
                backup = localBackup,
            )

            logcat(LogPriority.DEBUG, "SyncService") {
                "Merging local and remote changes with ETag($etag)"
            }
            val finalSyncData = mergeSyncData(localSyncData, remoteData)

            val success = pushSyncData(finalSyncData, etag)
            if (success) {
                reportSyncEvent(SyncEventStatus.SYNC_SUCCESS)
            } else {
                reportSyncEvent(SyncEventStatus.SYNC_FAILED, "Failed to push sync data")
            }

            return finalSyncData.backup
        } catch (e: Exception) {
            if (e is CancellationException) {
                reportSyncEvent(SyncEventStatus.SYNC_CANCELLED, e.message)
                throw e
            }
            logcat(LogPriority.ERROR) { "Error syncing: ${e.message}" }
            notifier.showSyncError(e.message)
            reportSyncEvent(SyncEventStatus.SYNC_ERROR, e.message)
            return null
        }
    }

    private suspend fun pullSyncData(): Pair<SyncData?, String> {
        val host = syncPreferences.clientHost.get()
        val apiKey = syncPreferences.clientAPIKey.get()
        val downloadUrl = "$host/api/sync/content"

        val headersBuilder = Headers.Builder().add("X-API-Token", apiKey)
        val lastETag = syncPreferences.lastSyncEtag.get()
        if (lastETag != "") {
            headersBuilder.add("If-None-Match", lastETag)
        }
        val headers = headersBuilder.build()

        val downloadRequest = GET(
            url = downloadUrl,
            headers = headers,
        )

        val client = OkHttpClient()
        val response = client.newCall(downloadRequest).await()

        if (response.code == HttpURLConnection.HTTP_NOT_MODIFIED) {
            // not modified
            assert(lastETag.isNotEmpty())
            logcat(LogPriority.INFO) {
                "Remote server not modified"
            }
            return Pair(null, lastETag)
        } else if (response.code == HttpURLConnection.HTTP_NOT_FOUND) {
            // maybe got deleted from remote
            return Pair(null, "")
        }

        if (response.isSuccessful) {
            val newETag = response.headers["ETag"]
                .takeIf { it?.isNotEmpty() == true } ?: throw SyncYomiException("Missing ETag")

            val byteArray = response.body.byteStream().use {
                return@use it.readBytes()
            }

            return try {
                val backup = protoBuf.decodeFromByteArray(Backup.serializer(), byteArray)
                return Pair(SyncData(backup = backup), newETag)
            } catch (_: SerializationException) {
                logcat(LogPriority.INFO) {
                    "Bad content responsed from server"
                }
                // the body is invalid
                // return default value so we can overwrite it
                Pair(null, "")
            }
        } else {
            val responseBody = response.body.string()
            notifier.showSyncError("Failed to download sync data: $responseBody")
            logcat(LogPriority.ERROR) { "SyncError: $responseBody" }
            throw SyncYomiException("Failed to download sync data: $responseBody")
        }
    }

    /**
     * Return true if update success
     */
    private suspend fun pushSyncData(syncData: SyncData, eTag: String): Boolean {
        val backup = syncData.backup ?: return true

        val host = syncPreferences.clientHost.get()
        val apiKey = syncPreferences.clientAPIKey.get()
        val uploadUrl = "$host/api/sync/content"
        val timeout = 30L

        val headersBuilder = Headers.Builder().add("X-API-Token", apiKey)
        if (eTag.isNotEmpty()) {
            headersBuilder.add("If-Match", eTag)
        }
        val headers = headersBuilder.build()

        // Set timeout to 30 seconds
        val client = OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)
            .build()

        val byteArray = protoBuf.encodeToByteArray(Backup.serializer(), backup)
        if (byteArray.isEmpty()) {
            throw IllegalStateException(context.stringResource(MR.strings.empty_backup_error))
        }
        val body = byteArray.toRequestBody("application/octet-stream".toMediaType())

        val uploadRequest = PUT(
            url = uploadUrl,
            headers = headers,
            body = body,
        )

        val response = client.newCall(uploadRequest).await()

        if (response.isSuccessful) {
            val newETag = response.headers["ETag"]
                .takeIf { it?.isNotEmpty() == true } ?: throw SyncYomiException("Missing ETag")
            syncPreferences.lastSyncEtag.set(newETag)
            logcat(LogPriority.DEBUG) { "SyncYomi sync completed" }
            return true
        } else if (response.code == HttpURLConnection.HTTP_PRECON_FAILED) {
            // other clients updated remote data, will try next time
            logcat(LogPriority.DEBUG) { "SyncYomi sync failed with 412" }
            return false
        } else {
            val responseBody = response.body.string()
            notifier.showSyncError("Failed to upload sync data: $responseBody")
            logcat(LogPriority.ERROR) { "SyncError: $responseBody" }
            return false
        }
    }

    private suspend fun reportSyncEvent(event: SyncEventStatus, message: String? = null) {
        withContext(NonCancellable) {
            try {
                val host = syncPreferences.clientHost.get()
                val apiKey = syncPreferences.clientAPIKey.get()
                val url = "$host/api/sync/event"

                val headersBuilder = Headers.Builder().add("X-API-Token", apiKey)
                val headers = headersBuilder.build()

                val bodyObj = SyncEvent(
                    event = event,
                    deviceName = android.os.Build.MODEL,
                    message = message,
                )

                val jsonBody = json.encodeToString(SyncEvent.serializer(), bodyObj)
                val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = POST(
                    url = url,
                    headers = headers,
                    body = requestBody,
                )

                val client = OkHttpClient()
                client.newCall(request).await().close()
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) { "Failed to report sync event: ${e.message}" }
            }
        }
    }
}
