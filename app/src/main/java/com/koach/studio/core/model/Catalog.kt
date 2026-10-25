package com.koach.studio.core.model

import java.time.LocalDate

enum class ServiceCategory(val displayName: String) {
    VIDEO_EDITING("Video Editing"),
    CONTENT_CREATION("Content Creation"),
    SOCIAL_MEDIA("Social Media"),
    BRANDING("Branding"),
    FILMING("Filming"),
    MOTION_GRAPHICS("Motion Graphics"),
    VFX("VFX")
}

data class ServicePackage(
    val id: String,
    val name: String,
    /** Starting price in Indian rupees. */
    val priceInr: Long,
    val turnaround: String,
    val features: List<String>,
    val recommended: Boolean = false
)

data class CreativeService(
    val id: String,
    val category: ServiceCategory,
    val name: String,
    val tagline: String,
    val description: String,
    val deliverables: List<String>,
    val packages: List<ServicePackage>
) {
    val startingPriceInr: Long get() = packages.minOf { it.priceInr }
}

/**
 * A showcase piece. Every item bundled with the app is sample content
 * ([isSample] = true) and must be labelled as such in the UI.
 */
data class PortfolioItem(
    val id: String,
    val title: String,
    val category: ServiceCategory,
    val year: Int,
    val summary: String,
    val brief: String,
    val approach: String,
    val services: List<ServiceCategory>,
    val metrics: List<Pair<String, String>>,
    val isSample: Boolean = true
)

enum class BookingStatus(val displayName: String) {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    DECLINED("Declined"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

data class Booking(
    val id: String,
    val clientId: String?,
    val requestedBy: String,
    val contactName: String,
    val contactEmail: String,
    val serviceId: String,
    val packageId: String?,
    val preferredDate: LocalDate,
    val budgetInr: Long?,
    val details: String,
    val status: BookingStatus,
    val createdOn: LocalDate,
    /** Note shared with the client about the latest status change, e.g. why it was declined. */
    val statusNote: String? = null
)
