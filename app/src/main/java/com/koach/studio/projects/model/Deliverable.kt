package com.koach.studio.projects.model

data class Deliverable(
    val id: String,
    val projectId: String,
    val name: String,
    val type: String,
    val size: String,
    val version: String,
    val notes: String = "",
    val status: DeliverableStatus = DeliverableStatus.DRAFT,
    /** Earlier versions, oldest first, e.g. ["v1.0"]. The current one is [version]. */
    val previousVersions: List<String> = emptyList()
) {
    /** "v1.0" → "v2.0"; falls back to appending ".1" for non-standard labels. */
    fun nextVersion(): String {
        val major = version.removePrefix("v").substringBefore(".").toIntOrNull() ?: return "$version.1"
        return "v${major + 1}.0"
    }
}
