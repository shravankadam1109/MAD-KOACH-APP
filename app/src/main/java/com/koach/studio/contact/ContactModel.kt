package com.koach.studio.contact

data class StudioContactInfo(
    val studioName: String = "KOACH Studio",
    val tagline: String = "Premium Creative Production & Post-Production",
    val addressLine1: String = "4th Floor, Apex Creative Hub",
    val addressLine2: String = "14th Road, Off Linking Road",
    val cityAndPin: String = "Bandra West, Mumbai 400050",
    val stateAndCountry: String = "Maharashtra, India",
    val deskPhone: String = "+91 98200 22000",
    val productionHotline: String = "+91 98200 22001",
    val generalEmail: String = "hello@koachstudio.example",
    val bookingsEmail: String = "production@koachstudio.example",
    val operatingHours: String = "Monday – Saturday: 10:00 AM – 8:00 PM IST",
    val shootAvailability: String = "24/7 on-location shoots worldwide",
    val instagramHandle: String = "@koachstudio",
    val vimeoHandle: String = "vimeo.com/koachstudio"
)
