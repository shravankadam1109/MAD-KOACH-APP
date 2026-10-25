package com.koach.studio.projects.data

import com.koach.studio.projects.model.Deliverable
import com.koach.studio.projects.model.DownloadState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Transient progress of one download on this device. Never stored with the deliverable. */
data class DownloadProgress(val state: DownloadState, val progress: Float = 0f)

/**
 * Downloads deliverable files. Progress lives here, keyed by deliverable *and
 * version*, so a newly shared version starts fresh and the shared project data
 * never carries per-device UI state. A Firebase Storage implementation can
 * stream real bytes behind the same interface.
 */
interface DeliverableDownloader {
    val downloads: StateFlow<Map<String, DownloadProgress>>
    suspend fun download(deliverable: Deliverable, simulateFailure: Boolean = false)

    companion object {
        fun key(deliverable: Deliverable): String = "${deliverable.id}@${deliverable.version}"
    }
}

/** Demo downloader: animates progress and saves nothing. Clearly simulated in the UI. */
class SimulatedDeliverableDownloader(private val stepMillis: Long = 120) : DeliverableDownloader {

    private val _downloads = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    override val downloads: StateFlow<Map<String, DownloadProgress>> = _downloads.asStateFlow()

    override suspend fun download(deliverable: Deliverable, simulateFailure: Boolean) {
        val key = DeliverableDownloader.key(deliverable)
        if (_downloads.value[key]?.state == DownloadState.DOWNLOADING) return
        set(key, DownloadProgress(DownloadState.DOWNLOADING, 0.2f))
        delay(stepMillis)
        set(key, DownloadProgress(DownloadState.DOWNLOADING, 0.6f))
        delay(stepMillis)
        set(key, if (simulateFailure) DownloadProgress(DownloadState.FAILED) else DownloadProgress(DownloadState.DONE, 1f))
    }

    private fun set(key: String, progress: DownloadProgress) = _downloads.update { it + (key to progress) }
}
