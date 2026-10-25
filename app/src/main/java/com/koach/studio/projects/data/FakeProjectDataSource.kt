package com.koach.studio.projects.data

import com.koach.studio.projects.model.Deliverable
import com.koach.studio.projects.model.DeliverableStatus
import com.koach.studio.projects.model.Milestone
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectStatus
import com.koach.studio.projects.model.Revision
import com.koach.studio.projects.model.ServiceType
import java.time.LocalDate

object FakeProjectDataSource {

    fun createInitialProjects(): List<Project> = listOf(
        Project(
            id = "proj-1",
            title = "Cyberpunk Indie Music Video",
            clientName = "Aetheria Sounds",
            serviceType = ServiceType.SHOOTING,
            status = ProjectStatus.IN_PROGRESS,
            dueDate = LocalDate.of(2026, 11, 20),
            description = "Full music video production including neon-lit night shoots, gimbal sequences, and multi-cam stage performances.",
            clientId = "cl-aetheria",
            teamIds = listOf("u-rohan", "u-zoya", "u-isha"),
            milestones = listOf(
                Milestone("ms-11", "Creative treatment approved", LocalDate.of(2026, 9, 28), completed = true),
                Milestone("ms-12", "Night shoot wrapped", LocalDate.of(2026, 10, 12), completed = true),
                Milestone("ms-13", "Rough cut review", LocalDate.of(2026, 10, 30)),
                Milestone("ms-14", "Colour grade and final delivery", LocalDate.of(2026, 11, 20))
            ),
            deliverables = listOf(
                Deliverable(
                    id = "del-101",
                    projectId = "proj-1",
                    name = "Director_Cut_Rough_v1.mp4",
                    type = "Video (4K)",
                    size = "1.8 GB",
                    version = "v1.0",
                    notes = "Rough cut with initial color profile applied.",
                    status = DeliverableStatus.IN_REVIEW
                ),
                Deliverable(
                    id = "del-102",
                    projectId = "proj-1",
                    name = "B-Roll_Neon_Streets_v1.mov",
                    type = "Raw Video",
                    size = "3.2 GB",
                    version = "v1.0",
                    notes = "Selected slow-motion b-roll takes from night shoot.",
                    status = DeliverableStatus.APPROVED
                ),
                Deliverable(
                    id = "del-103",
                    projectId = "proj-1",
                    name = "Lighting_Setup_Notes.pdf",
                    type = "Document",
                    size = "3.4 MB",
                    version = "v1.0",
                    notes = "Gaffer breakdown and lighting equipment plots.",
                    status = DeliverableStatus.DRAFT
                )
            )
        ),
        Project(
            id = "proj-2",
            title = "Acoustic Horizon EP Session",
            clientName = "Riverbed Records",
            serviceType = ServiceType.RECORDING,
            status = ProjectStatus.IN_REVIEW,
            dueDate = LocalDate.of(2026, 11, 8),
            description = "Studio recording sessions for 4-track acoustic EP. Multi-mic acoustic guitar setups, vocal captures, and cello overdubs.",
            clientId = "cl-riverbed",
            teamIds = listOf("u-zoya", "u-isha"),
            milestones = listOf(
                Milestone("ms-21", "Tracking sessions", LocalDate.of(2026, 10, 3), completed = true),
                Milestone("ms-22", "Mixdown", LocalDate.of(2026, 10, 18), completed = true),
                Milestone("ms-23", "Client listening review", LocalDate.of(2026, 10, 29)),
                Milestone("ms-24", "Mastered EP delivery", LocalDate.of(2026, 11, 8))
            ),
            deliverables = listOf(
                Deliverable(
                    id = "del-201",
                    projectId = "proj-2",
                    name = "Track_01_Wildflower_Master_v2.wav",
                    type = "Audio (Lossless)",
                    size = "94 MB",
                    version = "v2.0",
                    notes = "Mastered at 96kHz / 24-bit with analog tape warmth.",
                    status = DeliverableStatus.IN_REVIEW
                ),
                Deliverable(
                    id = "del-202",
                    projectId = "proj-2",
                    name = "Track_02_Echoes_Mixdown_v1.wav",
                    type = "Audio",
                    size = "86 MB",
                    version = "v1.1",
                    notes = "Balanced vocal presence, reverb adjusted per artist notes.",
                    status = DeliverableStatus.DRAFT
                ),
                Deliverable(
                    id = "del-203",
                    projectId = "proj-2",
                    name = "Stems_StemArchive_SessionB.zip",
                    type = "Archive",
                    size = "480 MB",
                    version = "v1.0",
                    notes = "Isolated dry stems for future remixing and licensing.",
                    status = DeliverableStatus.APPROVED
                )
            )
        ),
        Project(
            id = "proj-3",
            title = "Velocity Sprint Brand Commercial",
            clientName = "Apex Athletics",
            serviceType = ServiceType.EDITING,
            status = ProjectStatus.REVISION_REQUESTED,
            dueDate = LocalDate.of(2026, 10, 31),
            description = "High-energy commercial cut featuring rhythmic motion graphics, kinetic typography, dynamic sound design, and color grading.",
            clientId = "cl-apex",
            teamIds = listOf("u-zoya", "u-rohan"),
            milestones = listOf(
                Milestone("ms-31", "Footage ingest and selects", LocalDate.of(2026, 10, 6), completed = true),
                Milestone("ms-32", "30s cut v1", LocalDate.of(2026, 10, 14), completed = true),
                Milestone("ms-33", "Revisions round", LocalDate.of(2026, 10, 26)),
                Milestone("ms-34", "Final masters and social cutdowns", LocalDate.of(2026, 10, 31))
            ),
            revisions = listOf(
                Revision(
                    id = "rev-302",
                    projectId = "proj-3",
                    deliverableId = "del-301",
                    reason = "Color grade looks slightly saturated on the skin tones in the opening stadium scene.",
                    requestedBy = "u-dev",
                    requestedOn = LocalDate.of(2026, 10, 22),
                    resolved = false
                ),
                Revision(
                    id = "rev-301",
                    projectId = "proj-3",
                    deliverableId = "del-301",
                    reason = "Open on the sprinter close-up and tighten the logo end card to 2 seconds.",
                    requestedBy = "u-dev",
                    requestedOn = LocalDate.of(2026, 10, 19),
                    resolved = true
                )
            ),
            deliverables = listOf(
                Deliverable(
                    id = "del-301",
                    projectId = "proj-3",
                    name = "Commercial_Cut_30s_v2.mp4",
                    type = "Video",
                    size = "340 MB",
                    version = "v2.0",
                    notes = "Revised pacing on the final sprint cut. Needs tighter transition at 0:22.",
                    status = DeliverableStatus.REVISION_REQUESTED
                ),
                Deliverable(
                    id = "del-302",
                    projectId = "proj-3",
                    name = "Social_Teaser_Vertical_9x16.mp4",
                    type = "Video",
                    size = "120 MB",
                    version = "v1.0",
                    notes = "Optimized for Instagram Reels and TikTok placement.",
                    status = DeliverableStatus.APPROVED
                ),
                Deliverable(
                    id = "del-303",
                    projectId = "proj-3",
                    name = "Sound_FX_Stems_WAV.zip",
                    type = "Audio",
                    size = "75 MB",
                    version = "v1.0",
                    notes = "Custom foley, whooshes, and impact layers.",
                    status = DeliverableStatus.APPROVED
                )
            )
        ),
        Project(
            id = "proj-4",
            title = "Tech Summit Keynote Live Production",
            clientName = "Nexus Dynamics",
            serviceType = ServiceType.SHOOTING,
            status = ProjectStatus.COMPLETED,
            dueDate = LocalDate.of(2026, 10, 15),
            description = "3-camera live conference capture with teleprompter feed and direct digital board audio recording.",
            clientId = "cl-nexus",
            teamIds = listOf("u-rohan", "u-isha"),
            milestones = listOf(
                Milestone("ms-41", "Run of show locked", LocalDate.of(2026, 9, 20), completed = true),
                Milestone("ms-42", "Live multi-cam production", LocalDate.of(2026, 10, 2), completed = true),
                Milestone("ms-43", "Highlights edit delivered", LocalDate.of(2026, 10, 15), completed = true)
            ),
            deliverables = listOf(
                Deliverable(
                    id = "del-401",
                    projectId = "proj-4",
                    name = "Keynote_Full_Session_1080p.mp4",
                    type = "Video",
                    size = "4.5 GB",
                    version = "v1.0",
                    notes = "Full keynote with integrated slide feeds and synchronized audio.",
                    status = DeliverableStatus.APPROVED
                ),
                Deliverable(
                    id = "del-402",
                    projectId = "proj-4",
                    name = "Executive_QnA_Highlights.mp4",
                    type = "Video",
                    size = "620 MB",
                    version = "v1.0",
                    notes = "Short highlight package for press release distribution.",
                    status = DeliverableStatus.APPROVED
                )
            )
        )
    )
}
