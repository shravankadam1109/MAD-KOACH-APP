package com.koach.studio.core.data

import com.koach.studio.core.model.AppNotification
import com.koach.studio.core.model.Booking
import com.koach.studio.core.model.BookingStatus
import com.koach.studio.core.model.Client
import com.koach.studio.core.model.ClientStatus
import com.koach.studio.core.model.CreativeService
import com.koach.studio.core.model.DocumentCategory
import com.koach.studio.core.model.DocumentVisibility
import com.koach.studio.core.model.Feedback
import com.koach.studio.core.model.FeedbackStatus
import com.koach.studio.core.model.FeedbackType
import com.koach.studio.core.model.Message
import com.koach.studio.core.model.MessageThread
import com.koach.studio.core.model.NotificationType
import com.koach.studio.core.model.Payment
import com.koach.studio.core.model.PaymentStatus
import com.koach.studio.core.model.PortfolioItem
import com.koach.studio.core.model.Role
import com.koach.studio.core.model.ServiceCategory
import com.koach.studio.core.model.ServicePackage
import com.koach.studio.core.model.StudioDocument
import com.koach.studio.core.model.TaskItem
import com.koach.studio.core.model.TaskPriority
import com.koach.studio.core.model.TaskStatus
import com.koach.studio.core.model.User
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Seed data for the demo build. Every person, client and portfolio piece here is
 * fictional sample content, not a real KOACH Studio client. Dates are relative
 * to [today] so the demo always looks current.
 *
 * Projects and deliverables live in FakeProjectDataSource (projects module) and
 * are referenced here by id.
 */
class DemoData(private val today: LocalDate) {

    private fun daysAgo(days: Long) = today.minusDays(days)
    private fun inDays(days: Long) = today.plusDays(days)
    private fun at(date: LocalDate, hour: Int, minute: Int = 0) = LocalDateTime.of(date, LocalTime.of(hour, minute))

    val users: List<User> = listOf(
        User("u-maya", "Maya Rao", "maya@aetheria.example", Role.CLIENT, "Label Manager, Aetheria Sounds", clientId = "cl-aetheria", phone = "+91 98200 11001"),
        User("u-dev", "Dev Malhotra", "dev@apexathletics.example", Role.CLIENT, "Brand Lead, Apex Athletics", clientId = "cl-apex", phone = "+91 98200 11002"),
        User("u-arjun", "Arjun Mehta", "arjun@koach.example", Role.ADMIN, "Studio Director", phone = "+91 98200 22001"),
        User("u-zoya", "Zoya Khan", "zoya@koach.example", Role.TEAM, "Lead Editor", phone = "+91 98200 22002"),
        User("u-rohan", "Rohan Das", "rohan@koach.example", Role.TEAM, "Cinematographer", phone = "+91 98200 22003"),
        User("u-isha", "Isha Patel", "isha@koach.example", Role.TEAM, "Producer", phone = "+91 98200 22004")
    )

    val clients: List<Client> = listOf(
        Client("cl-aetheria", "Aetheria Sounds", "Maya Rao", "maya@aetheria.example", "+91 98200 11001", "Music label", "Mumbai", ClientStatus.ACTIVE, daysAgo(240), "Indie label; prefers WhatsApp for quick approvals."),
        Client("cl-riverbed", "Riverbed Records", "Kabir Sen", "kabir@riverbed.example", "+91 98200 11003", "Recording studio", "Pune", ClientStatus.ACTIVE, daysAgo(180)),
        Client("cl-apex", "Apex Athletics", "Dev Malhotra", "dev@apexathletics.example", "+91 98200 11002", "Sportswear", "Bengaluru", ClientStatus.ACTIVE, daysAgo(120), "Quarterly campaign retainer under discussion."),
        Client("cl-nexus", "Nexus Dynamics", "Priya Iyer", "priya@nexusdyn.example", "+91 98200 11004", "Technology", "Hyderabad", ClientStatus.PAUSED, daysAgo(300)),
        Client("cl-lumen", "Lumen Café Co.", "Neha Kapoor", "neha@lumencafe.example", "+91 98200 11005", "Hospitality", "Mumbai", ClientStatus.LEAD, daysAgo(6), "Asked about a monthly social content package."),
        Client("cl-orbit", "Orbit Fintech", "Sameer Joshi", "sameer@orbitfin.example", "+91 98200 11006", "Financial services", "Delhi", ClientStatus.LEAD, daysAgo(2))
    )

    val services: List<CreativeService> = listOf(
        service(
            "svc-edit", ServiceCategory.VIDEO_EDITING, "Video Editing",
            "Story-first cuts with rhythm and polish.",
            "From raw footage to a finished film: selects, story edit, pacing, colour and sound finishing, delivered in every aspect ratio you need.",
            listOf("Edited master", "Colour grade", "Sound mix", "Social cutdowns"),
            pkg("edit-1", "Essential Cut", 18_000, "5 days", "Up to 3 min edit", "1 revision round", "1080p master"),
            pkg("edit-2", "Signature Edit", 45_000, "8 days", "Up to 10 min edit", "Colour grade + sound mix", "3 revision rounds", "4K master", recommended = true),
            pkg("edit-3", "Campaign Suite", 95_000, "12 days", "Hero film + 6 cutdowns", "Full finishing", "Unlimited minor tweaks")
        ),
        service(
            "svc-content", ServiceCategory.CONTENT_CREATION, "Content Creation",
            "Always-on content that sounds like you.",
            "Concept, shoot and edit short-form content in batches, planned around your launches and calendar.",
            listOf("Content plan", "Shoot day", "Edited reels", "Thumbnails"),
            pkg("cont-1", "Starter Batch", 30_000, "10 days", "6 short-form videos", "1 shoot day"),
            pkg("cont-2", "Growth Batch", 65_000, "14 days", "15 short-form videos", "2 shoot days", "Hooks and captions", recommended = true),
            pkg("cont-3", "Studio Retainer", 1_20_000, "Monthly", "30 videos per month", "Dedicated producer")
        ),
        service(
            "svc-social", ServiceCategory.SOCIAL_MEDIA, "Social Media",
            "Strategy, publishing and community, handled.",
            "Channel strategy, calendars, publishing and monthly reporting across Instagram, YouTube and LinkedIn.",
            listOf("Strategy deck", "Content calendar", "Publishing", "Monthly report"),
            pkg("soc-1", "Presence", 25_000, "Monthly", "2 platforms", "12 posts per month"),
            pkg("soc-2", "Momentum", 55_000, "Monthly", "3 platforms", "24 posts per month", "Community management", recommended = true)
        ),
        service(
            "svc-brand", ServiceCategory.BRANDING, "Branding",
            "Identity systems built to move.",
            "Logo, visual language and motion-ready brand guidelines designed for screens first.",
            listOf("Logo suite", "Typography and colour", "Brand guidelines", "Motion logo"),
            pkg("brand-1", "Identity Kit", 60_000, "3 weeks", "Logo + palette + type", "Mini guidelines"),
            pkg("brand-2", "Brand System", 1_50_000, "6 weeks", "Full identity", "Motion logo", "Templates", recommended = true)
        ),
        service(
            "svc-film", ServiceCategory.FILMING, "Filming",
            "Cinema-grade production, any scale.",
            "Crewed shoots with cinema cameras, lighting and sound for commercials, music videos, events and interviews.",
            listOf("Production plan", "Crew and kit", "Raw footage", "On-set direction"),
            pkg("film-1", "Half Day", 35_000, "Shoot + 3 days", "4 hours on set", "2-person crew"),
            pkg("film-2", "Full Day", 70_000, "Shoot + 5 days", "10 hours on set", "4-person crew", "Lighting package", recommended = true),
            pkg("film-3", "Multi-cam Event", 1_40_000, "Shoot + 7 days", "Up to 4 cameras", "Live switching")
        ),
        service(
            "svc-motion", ServiceCategory.MOTION_GRAPHICS, "Motion Graphics",
            "Kinetic type, explainers and brand motion.",
            "2D and 3D motion design: explainers, title sequences, lower thirds and animated brand assets.",
            listOf("Style frames", "Animatic", "Final animation", "Project files"),
            pkg("mo-1", "Motion Pack", 28_000, "7 days", "Lower thirds + titles", "Editable templates"),
            pkg("mo-2", "Explainer", 75_000, "3 weeks", "60–90s explainer", "Script + VO direction", recommended = true)
        ),
        service(
            "svc-vfx", ServiceCategory.VFX, "VFX",
            "Invisible fixes and impossible shots.",
            "Compositing, clean-up, set extensions, screen replacements and CG integration.",
            listOf("VFX breakdown", "Shot comps", "Clean plates", "Final renders"),
            pkg("vfx-1", "Clean-up", 20_000, "5 days", "Up to 10 shots", "Removals and fixes"),
            pkg("vfx-2", "Hero Shots", 90_000, "3 weeks", "Up to 8 complex shots", "CG integration", recommended = true)
        )
    )

    val portfolio: List<PortfolioItem> = listOf(
        PortfolioItem("pf-1", "Neon Monsoon", ServiceCategory.FILMING, 2026, "Night-time music video shot entirely in the rain.", "An indie artist wanted a moody, city-at-night film for a lead single.", "Practical neon, rain towers and a single-take chorus on a gimbal.", listOf(ServiceCategory.FILMING, ServiceCategory.VIDEO_EDITING), listOf("1 night" to "Shoot", "4K" to "Delivery", "3" to "Camera setups")),
        PortfolioItem("pf-2", "Run The City", ServiceCategory.VIDEO_EDITING, 2026, "30-second sportswear spot cut to a custom beat.", "Launch a running shoe with energy, in under 30 seconds.", "Speed-ramped edit synced to a bespoke percussion track; 9:16 and 1:1 cutdowns.", listOf(ServiceCategory.VIDEO_EDITING, ServiceCategory.VFX), listOf("30s" to "Hero cut", "6" to "Cutdowns", "2" to "Revisions")),
        PortfolioItem("pf-3", "Brew Stories", ServiceCategory.CONTENT_CREATION, 2025, "Monthly reels series for a neighbourhood café.", "Turn a café's regulars into the face of the brand.", "Batch-shot portraits and process clips, edited into weekly reels.", listOf(ServiceCategory.CONTENT_CREATION, ServiceCategory.SOCIAL_MEDIA), listOf("15" to "Reels / month", "1" to "Shoot day", "Weekly" to "Publishing")),
        PortfolioItem("pf-4", "Circuit Identity", ServiceCategory.BRANDING, 2025, "Motion-first identity for a hardware startup.", "A logo that works as well animated as it does printed.", "A modular mark built from circuit traces, animated as a loading sequence.", listOf(ServiceCategory.BRANDING, ServiceCategory.MOTION_GRAPHICS), listOf("1" to "Logo system", "24" to "Templates", "6 wk" to "Timeline")),
        PortfolioItem("pf-5", "Ledger, Explained", ServiceCategory.MOTION_GRAPHICS, 2026, "90-second explainer for a payments product.", "Explain a complex product in plain language.", "Flat 2D illustration with kinetic type and a calm voice-over.", listOf(ServiceCategory.MOTION_GRAPHICS), listOf("90s" to "Runtime", "12" to "Style frames", "2" to "Languages")),
        PortfolioItem("pf-6", "Skyline Swap", ServiceCategory.VFX, 2025, "Set extension turning a rooftop into a future city.", "A sci-fi short needed a skyline it could not afford to build.", "Tracked plates with matte-painted towers and volumetric haze.", listOf(ServiceCategory.VFX, ServiceCategory.FILMING), listOf("8" to "Hero shots", "3 wk" to "Turnaround", "4K" to "Renders")),
        PortfolioItem("pf-7", "Keynote Live", ServiceCategory.FILMING, 2026, "Four-camera live production for a tech keynote.", "Capture a 2-hour keynote and turn it into highlights the same week.", "Live switching on site, then a 3-minute highlights film within 48 hours.", listOf(ServiceCategory.FILMING, ServiceCategory.VIDEO_EDITING), listOf("4" to "Cameras", "48h" to "Highlights", "2h" to "Live")),
        PortfolioItem("pf-8", "Feed Refresh", ServiceCategory.SOCIAL_MEDIA, 2026, "Social relaunch with a new grid system.", "Give a tired feed a consistent, recognisable look.", "Template system, three content pillars and a 6-week launch calendar.", listOf(ServiceCategory.SOCIAL_MEDIA, ServiceCategory.BRANDING), listOf("3" to "Platforms", "36" to "Posts", "6 wk" to "Launch"))
    )

    val bookings: List<Booking> = listOf(
        Booking("bk-1", "cl-aetheria", "u-maya", "Maya Rao", "maya@aetheria.example", "svc-film", "film-2", inDays(21), 70_000, "Second single video, daytime rooftop shoot with a small dance crew.", BookingStatus.PENDING, daysAgo(1)),
        Booking("bk-2", "cl-aetheria", "u-maya", "Maya Rao", "maya@aetheria.example", "svc-edit", "edit-2", daysAgo(40), 45_000, "Edit for the Cyberpunk music video.", BookingStatus.COMPLETED, daysAgo(60)),
        Booking("bk-3", "cl-apex", "u-dev", "Dev Malhotra", "dev@apexathletics.example", "svc-content", "cont-2", inDays(14), 65_000, "Winter collection reels for Instagram.", BookingStatus.CONFIRMED, daysAgo(5)),
        Booking("bk-4", "cl-lumen", "u-arjun", "Neha Kapoor", "neha@lumencafe.example", "svc-social", "soc-1", inDays(10), 25_000, "Enquiry from the website: monthly social content.", BookingStatus.PENDING, daysAgo(3))
    )

    val tasks: List<TaskItem> = listOf(
        TaskItem("t-1", "Colour grade rough cut", "Apply the neon LUT and balance skin tones across the night scenes.", "proj-1", "u-zoya", TaskPriority.HIGH, TaskStatus.IN_PROGRESS, inDays(3), "u-arjun", daysAgo(5)),
        TaskItem("t-2", "Sync B-roll to final track", "Replace the scratch track and re-sync B-roll hits.", "proj-1", "u-zoya", TaskPriority.MEDIUM, TaskStatus.TODO, inDays(6), "u-arjun", daysAgo(4)),
        TaskItem("t-3", "Export lighting notes for client", "Turn on-set lighting notes into a shareable PDF.", "proj-1", "u-rohan", TaskPriority.LOW, TaskStatus.DONE, daysAgo(2), "u-isha", daysAgo(9)),
        TaskItem("t-4", "Address Apex revision notes", "Open on the sprinter close-up; shorten the end card to 2s.", "proj-3", "u-zoya", TaskPriority.URGENT, TaskStatus.IN_PROGRESS, inDays(1), "u-arjun", daysAgo(3)),
        TaskItem("t-5", "Prepare vertical cutdowns", "9:16 versions of the 30s spot for Reels and Shorts.", "proj-3", "u-rohan", TaskPriority.HIGH, TaskStatus.TODO, daysAgo(1), "u-isha", daysAgo(6)),
        TaskItem("t-6", "Book mastering engineer", "Confirm the mastering slot for the EP.", "proj-2", "u-isha", TaskPriority.MEDIUM, TaskStatus.IN_REVIEW, inDays(2), "u-arjun", daysAgo(7)),
        TaskItem("t-7", "Send listening-room invite", "Invite Riverbed for the client listening session.", "proj-2", "u-isha", TaskPriority.LOW, TaskStatus.TODO, inDays(5), "u-isha", daysAgo(1)),
        TaskItem("t-8", "Archive keynote raw footage", "Move raw camera cards to cold storage and log the drive.", "proj-4", "u-rohan", TaskPriority.LOW, TaskStatus.DONE, daysAgo(8), "u-arjun", daysAgo(14)),
        TaskItem("t-9", "Quote for Lumen Café", "Draft a social content proposal for the new lead.", null, "u-arjun", TaskPriority.MEDIUM, TaskStatus.TODO, inDays(2), "u-arjun", daysAgo(2)),
        TaskItem("t-10", "Update showreel", "Add Neon Monsoon and Run The City to the studio reel.", null, "u-zoya", TaskPriority.LOW, TaskStatus.TODO, daysAgo(4), "u-arjun", daysAgo(20))
    )

    val documents: List<StudioDocument> = listOf(
        doc("doc-1", "Creative_Treatment_Neon.pdf", DocumentCategory.BRIEF, "proj-1", "cl-aetheria", "application/pdf", 4_200_000, "u-isha", daysAgo(30), DocumentVisibility.SHARED),
        doc("doc-2", "Production_Agreement_Aetheria.pdf", DocumentCategory.CONTRACT, "proj-1", "cl-aetheria", "application/pdf", 860_000, "u-arjun", daysAgo(28), DocumentVisibility.SHARED),
        doc("doc-3", "Shot_List_Night_Shoot.xlsx", DocumentCategory.SCRIPT, "proj-1", "cl-aetheria", "application/vnd.ms-excel", 120_000, "u-rohan", daysAgo(15), DocumentVisibility.INTERNAL),
        doc("doc-4", "Storyboard_Chorus_Sequence.pdf", DocumentCategory.STORYBOARD, "proj-1", "cl-aetheria", "application/pdf", 9_800_000, "u-zoya", daysAgo(20), DocumentVisibility.SHARED),
        doc("doc-5", "Session_Notes_EP.docx", DocumentCategory.REPORT, "proj-2", "cl-riverbed", "application/msword", 54_000, "u-isha", daysAgo(9), DocumentVisibility.INTERNAL),
        doc("doc-6", "Apex_Brand_Guidelines.pdf", DocumentCategory.ASSET, "proj-3", "cl-apex", "application/pdf", 18_400_000, "u-dev", daysAgo(25), DocumentVisibility.SHARED),
        doc("doc-7", "Velocity_Script_v3.pdf", DocumentCategory.SCRIPT, "proj-3", "cl-apex", "application/pdf", 310_000, "u-zoya", daysAgo(18), DocumentVisibility.SHARED),
        doc("doc-8", "Invoice_KS-2026-014.pdf", DocumentCategory.INVOICE, "proj-3", "cl-apex", "application/pdf", 96_000, "u-arjun", daysAgo(12), DocumentVisibility.SHARED),
        doc("doc-9", "Keynote_Run_of_Show.pdf", DocumentCategory.BRIEF, "proj-4", "cl-nexus", "application/pdf", 420_000, "u-isha", daysAgo(40), DocumentVisibility.SHARED),
        doc("doc-10", "Crew_Call_Sheet_Keynote.pdf", DocumentCategory.REPORT, "proj-4", "cl-nexus", "application/pdf", 150_000, "u-isha", daysAgo(38), DocumentVisibility.INTERNAL)
    )

    val threads: List<MessageThread> = listOf(
        MessageThread("th-1", "proj-1", "Cyberpunk Indie Music Video", listOf("u-maya", "u-zoya", "u-isha", "u-arjun")),
        MessageThread("th-2", "proj-3", "Velocity Sprint Brand Commercial", listOf("u-dev", "u-zoya", "u-arjun")),
        MessageThread("th-3", "proj-2", "Acoustic Horizon EP Session", listOf("u-isha", "u-zoya", "u-arjun"))
    )

    val messages: List<Message> = listOf(
        msg("m-1", "th-1", "u-isha", "Hi Maya! Rough cut v1 is uploaded in Deliverables. Would love your notes by Friday.", at(daysAgo(2), 10, 15), "u-isha", "u-maya", "u-zoya", "u-arjun"),
        msg("m-2", "th-1", "u-maya", "Watching it now. The rain sequence looks unreal.", at(daysAgo(2), 11, 2), "u-maya", "u-isha", "u-zoya", "u-arjun"),
        msg("m-3", "th-1", "u-zoya", "Thanks! Grade is still a first pass, so skin tones will warm up in the final.", at(daysAgo(2), 11, 20), "u-zoya", "u-maya", "u-isha"),
        msg("m-4", "th-1", "u-zoya", "Uploaded the B-roll selects too, in case you want to pick favourites for the lyric video.", at(daysAgo(0), 9, 40), "u-zoya", "u-isha"),
        msg("m-5", "th-2", "u-dev", "Team loved the energy. Two notes coming through the revision form now.", at(daysAgo(3), 16, 5), "u-dev", "u-zoya", "u-arjun"),
        msg("m-6", "th-2", "u-arjun", "Got them, Dev. Zoya is on it; expect v3 within 48 hours.", at(daysAgo(3), 16, 30), "u-arjun", "u-dev", "u-zoya"),
        msg("m-7", "th-2", "u-zoya", "v3 is close. Quick question: keep the slow-mo on the starting blocks?", at(daysAgo(0), 12, 10), "u-zoya", "u-arjun"),
        msg("m-8", "th-3", "u-isha", "Mastering slot is pencilled for next Tuesday.", at(daysAgo(1), 15, 0), "u-isha", "u-arjun"),
        msg("m-9", "th-3", "u-zoya", "Perfect, I'll print the stems tonight.", at(daysAgo(1), 15, 12), "u-zoya", "u-isha")
    )

    val notifications: List<AppNotification> = listOf(
        AppNotification("n-1", "u-maya", NotificationType.DELIVERABLE, "Rough cut ready for review", "Director_Cut_Rough_v1.mp4 was shared on Cyberpunk Indie Music Video.", at(daysAgo(2), 10, 16), isRead = true, targetId = "proj-1"),
        AppNotification("n-2", "u-maya", NotificationType.MESSAGE, "New message from Zoya", "Uploaded the B-roll selects too…", at(today, 9, 41), isRead = false, targetId = "th-1"),
        AppNotification("n-3", "u-maya", NotificationType.PAYMENT, "Invoice due soon", "Invoice KS-2026-018 is due in 5 days.", at(daysAgo(1), 9, 0), isRead = false),
        AppNotification("n-4", "u-maya", NotificationType.BOOKING, "Booking received", "Your Filming · Full Day request is awaiting confirmation.", at(daysAgo(1), 18, 30), isRead = true),
        AppNotification("n-5", "u-dev", NotificationType.PROJECT, "Revision in progress", "Velocity Sprint v3 is being edited.", at(daysAgo(3), 16, 31), isRead = true, targetId = "proj-3"),
        AppNotification("n-6", "u-dev", NotificationType.PAYMENT, "Invoice overdue", "Invoice KS-2026-014 is past its due date.", at(daysAgo(2), 9, 0), isRead = false),
        AppNotification("n-7", "u-arjun", NotificationType.BOOKING, "New booking request", "Maya Rao requested Filming · Full Day.", at(daysAgo(1), 18, 30), isRead = false),
        AppNotification("n-8", "u-arjun", NotificationType.FEEDBACK, "New complaint", "Riverbed Records raised an issue about file delivery.", at(daysAgo(1), 11, 0), isRead = false),
        AppNotification("n-9", "u-zoya", NotificationType.TASK, "Task due tomorrow", "Address Apex revision notes is due tomorrow.", at(today, 8, 0), isRead = false, targetId = "t-4"),
        AppNotification("n-10", "u-zoya", NotificationType.MESSAGE, "New message from Dev", "Team loved the energy…", at(daysAgo(3), 16, 6), isRead = true, targetId = "th-2")
    )

    val payments: List<Payment> = listOf(
        Payment("pay-1", "KS-2026-009", "cl-aetheria", "proj-1", "Music video, 50% advance", 85_000, daysAgo(35), daysAgo(25), PaymentStatus.PAID, paidOn = daysAgo(27)),
        Payment("pay-2", "KS-2026-018", "cl-aetheria", "proj-1", "Music video, balance on delivery", 85_000, daysAgo(2), inDays(5), PaymentStatus.DUE),
        Payment("pay-3", "KS-2026-004", "cl-aetheria", null, "Lyric video edit", 18_000, daysAgo(70), daysAgo(55), PaymentStatus.PAID, paidOn = daysAgo(58)),
        Payment("pay-4", "KS-2026-014", "cl-apex", "proj-3", "Brand commercial, edit package", 95_000, daysAgo(12), daysAgo(2), PaymentStatus.OVERDUE),
        Payment("pay-5", "KS-2026-011", "cl-riverbed", "proj-2", "EP session recording", 60_000, daysAgo(20), daysAgo(6), PaymentStatus.PAID, paidOn = daysAgo(8)),
        Payment("pay-6", "KS-2026-007", "cl-nexus", "proj-4", "Keynote live production", 1_40_000, daysAgo(30), daysAgo(16), PaymentStatus.PAID, paidOn = daysAgo(18))
    )

    val feedback: List<Feedback> = listOf(
        Feedback("fb-1", "cl-aetheria", "u-maya", "proj-1", FeedbackType.FEEDBACK, 5, "Night shoot was smooth", "Crew was fast and kind to the artist even at 3am. Thank you!", FeedbackStatus.RESOLVED, daysAgo(9), response = "Means a lot. The crew loved working with you."),
        Feedback("fb-2", "cl-riverbed", "u-isha", "proj-2", FeedbackType.COMPLAINT, null, "Stems link expired", "The download link for the stems archive expired before we could pull it.", FeedbackStatus.IN_PROGRESS, daysAgo(1)),
        Feedback("fb-3", "cl-apex", "u-dev", "proj-3", FeedbackType.FEEDBACK, 4, "Great first cut", "Loved the pacing. Logo end card felt long, which we sent as a revision.", FeedbackStatus.OPEN, daysAgo(3))
    )

    private fun service(
        id: String, category: ServiceCategory, name: String, tagline: String, description: String,
        deliverables: List<String>, vararg packages: ServicePackage
    ) = CreativeService(id, category, name, tagline, description, deliverables, packages.toList())

    private fun pkg(id: String, name: String, price: Long, turnaround: String, vararg features: String, recommended: Boolean = false) =
        ServicePackage(id, name, price, turnaround, features.toList(), recommended)

    private fun doc(
        id: String, name: String, category: DocumentCategory, projectId: String?, clientId: String?, mime: String,
        size: Long, by: String, on: LocalDate, visibility: DocumentVisibility
    ) = StudioDocument(id, name, category, projectId, clientId, mime, size, by, at(on, 11), visibility)

    private fun msg(id: String, threadId: String, sender: String, body: String, sentAt: LocalDateTime, vararg readBy: String) =
        Message(id, threadId, sender, body, sentAt, readBy.toSet())
}
