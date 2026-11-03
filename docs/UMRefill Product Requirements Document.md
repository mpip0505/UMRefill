# UMRefill: Product Requirements Document

Nov 3, 2026 · @Muhaimin Afif Mushahar

UMRefill is an Android app that shows students where the nearest working water refill point is, floor by floor, inside UM faculty buildings. It also lets them report stations that are broken, empty or dirty. This PRD is the single reference for what we build, who owns it and when it is due. If a requirement here and the code disagree, raise it in the group before changing either.

## Overview

| Item | Decision |
| --- | --- |
| Course | WIA2007 Mobile Application Development, Group Project (20%), Sem 1 2026/27 |
| SDG | SDG 6 Clean Water and Sanitation |
| Primary target | **6.b**: community participation in water management (on the brief's allowed list) |
| Secondary | 6.1 (pending lecturer confirmation), SDG 12 (less plastic), SDG 13 (lower emissions) |
| Platform | Native Android, Android Studio, **Java + XML layouts** |
| Storage | Room (SQLite) on the device; a Supabase backend is future work |
| Signature feature | 2D floor guide: faculty → floor → plan with pinned refill points |
| Team | 6 members, about 20 hours each (120 hours total) |
| Video due | 12 pm, Sunday 3 January 2027 (10%) |
| Report due | 12 pm, Sunday 10 January 2027 (10%) |

How to use it: find your module in Functional requirements, build to its acceptance criteria, and tick the matching test in Testing. Every member writes code. Nobody does documentation only.

## Problem and goals

Working hypothesis, still to be proven: students and staff often don't know where the nearest *working* fountain is in a faculty building, so they buy bottled water instead. Broken, switched-off or dirty fountains make it worse, because there is no way to check one before walking to it. The brief rewards problems we have observed and validated, so we treat this as a hypothesis until the evidence below is in.

**Evidence we will collect**

- A walk-through of 2–3 faculty buildings, with every refill point located and photographed by floor. This doubles as our seed data.
- A student survey with 30+ responses: how often people fail to find a point and buy bottled water instead.
- A count of fountains that are broken, switched off or unpleasant to use.
- A JPPHB or facilities interview: how faults are reported today, and whether our reports could reach them.

**Goals and success measures**

| Goal | Measure | Target |
| --- | --- | --- |
| Find water fast | Time for a first-time user to reach the nearest working point | 3 taps or fewer, under 10 s (NFR1) |
| Keep the map trustworthy | Points with a last-confirmed date | Every seeded point |
| Community participation (6.b) | Reports submitted in the user test | Every tester submits one |
| Validate with UM | Stakeholder engagement | 30+ survey responses, 1 JPPHB contact, 10+ user testers |
| Less plastic | Bottles avoided, shown in the app | Counter works per user and per faculty |

## Users and stakeholders

Students are the main users. JPPHB is the key stakeholder, because the app is only useful if broken fountains get fixed. UM office names are assumptions until we confirm them.

| Who | Uses the app to | Needs | How we engage them |
| --- | --- | --- | --- |
| Students (all faculties) | Find a working point, report problems, log refills | Fast lookup with no login | Survey of 30+, user test of 10+ |
| Staff and visitors | Occasional lookup | Simple, no account | Short feedback |
| Admin (team member in the demo) | Manage points, floor plans, fault reports | Fault list by location, CSV export | Demo role |
| JPPHB / facilities | Receive fault reports | A clear list of faults by location | Interview by 15 Nov |
| Sustainability Centre | SDG and plastic reporting | Bottles-avoided numbers | Share the impact counter |
| Faculty administrators | Confirm their building's layout | Accurate floor info | Ask for floor plans |

**Personas for design and testing**

- **Aina, Year 1, FSKTM:** has classes in three faculties and doesn't know where the fountains are outside her own. She wants water in under a minute, between classes.
- **Daniel, international student:** reads English better than BM and relies on clear icons and labels.
- **Encik Razak, JPPHB technician:** wants to know which fountain is broken and where, without chasing emails.

## Scope

Version 1 covers 2–3 faculty buildings, a few floors each, with six modules. Anything that needs a server or indoor positioning is out of scope.

**In scope (v1, must ship)**

- Modules 1–5 from the proposal: access, faculty/floor browser, 2D floor guide, community reporting, impact and admin
- Module 6, AI-assisted reporting (on-device, free tools)
- A QR code per station that opens its detail page
- English and BM strings, with status shown by icon and word as well as colour
- Seeded sample data for 2–3 faculties; normal-user and admin roles shown on one device

**Stretch (only after the 20 Dec milestone)**

- Favourite stations, with a local notification when a favourite is working again
- Badges or streaks, and a faculty leaderboard of bottles avoided
- A cleanliness or taste rating per station
- "Send fault list to JPPHB" through the email app

**Out of scope (named as future work in the report)**

- Live indoor positioning or turn-by-turn routes: no GPS indoors, and beacons cost money
- A shared backend, so reports stay on one phone for now (Supabase later)
- Mapping every UM faculty
- iOS

## Functional requirements

There are six modules with 19 FRs. The brief's minimum is five modules with three FRs each. Module 6 covers AI, as the brief requires for any app that uses it. Owners are role numbers from Team and workflow. "Must" items are needed for the video; "Should" items can drop if time runs out.

**Module 1: Access and profile** (owner: #5 screens, #2 data)

| ID | Requirement | Acceptance criteria | Priority |
| --- | --- | --- | --- |
| FR1.1 | Users browse refill points without logging in; reporting, logging refills and favourites need a UM email login | A guest reaches a floor plan with no login prompt; tapping Report as a guest opens Login; only @um.edu.my / @siswa.um.edu.my emails are accepted | Must |
| FR1.2 | Users reset a forgotten password | The reset flow sets a new password and the old one stops working (local demo: verify by security question or reset code) | Must |
| FR1.3 | The system assigns a user or admin role | An admin account sees the Admin tab; a normal user never does | Must |

**Module 2: Faculty and floor browser** (owner: #4)

| ID | Requirement | Acceptance criteria | Priority |
| --- | --- | --- | --- |
| FR2.1 | Users select a faculty, then a floor | The faculty list shows every seeded faculty; choosing one lists only its floors | Must |
| FR2.2 | The system lists a floor's refill points with a short location note | Each row shows name, type, status icon and note (for example "near the lifts") | Must |
| FR2.3 | Users filter points by status and type | "Working only" hides broken or empty points; the type filter (cold, filtered, normal) combines with it | Must |

**Module 3: 2D floor guide** (owner: #3)

| ID | Requirement | Acceptance criteria | Priority |
| --- | --- | --- | --- |
| FR3.1 | The system shows a 2D floor plan with every point pinned | Pins sit at their stored x/y percentages on any screen size and in both orientations | Must |
| FR3.2 | The system highlights the nearest working point from a chosen entry point (lift, stairs, entrance) and shows a direction note | Changing the entry point changes the highlight; broken points are never highlighted | Must |
| FR3.3 | Tapping a pin shows the point's type, status and last-confirmed date | The detail sheet opens in one tap and has a Report button | Must |
| FR3.4 | Scanning a station's QR code opens its detail page | Scanning a printed code opens the right station; an unknown code shows an error, not a crash | Should |

**Module 4: Community reporting** (owner: #4)

| ID | Requirement | Acceptance criteria | Priority |
| --- | --- | --- | --- |
| FR4.1 | Logged-in users report a point as broken, empty, dirty or working, with an optional photo | The report saves with user, time, status and photo path; the camera and gallery both work | Must |
| FR4.2 | The system updates the point's status and last-confirmed time | After a report, the list, pin colour and detail sheet all show the new status | Must |
| FR4.3 | Admins review reports and mark a point fixed or out of service | The admin queue lists open reports, newest first; resolving one updates the point | Must |

**Module 5: Impact and admin** (owners: #5 impact view, #1 admin screens, #6 dashboard and export)

| ID | Requirement | Acceptance criteria | Priority |
| --- | --- | --- | --- |
| FR5.1 | Users see a personal counter of refills logged and bottles avoided | "Log refill" adds 1; the counter survives an app restart | Must |
| FR5.2 | The system shows faculty and campus totals | The totals equal the sum of RefillLog rows per faculty | Must |
| FR5.3 | Admins add, edit or remove points and floor plans, and export a CSV of points and faults | A new point appears on the plan straight away; the CSV opens in Excel with headers | Must |

**Module 6: AI-assisted reporting** (owner: #2, accuracy test by #6)

| ID | Requirement | Acceptance criteria | Priority |
| --- | --- | --- | --- |
| FR6.1 | The system checks a report photo and warns if it doesn't appear to show a fountain or tap | ML Kit Image Labeling, on-device; the user can still submit after the warning; accuracy is measured on 30–50 UM photos | Must |
| FR6.2 | The system categorises a free-text report (leak, no water, dirty, low pressure) and suggests a status | Keyword rules or a small TFLite model; the suggestion fills the status field and the user can change it | Should |
| FR6.3 | The system flags a point as "likely faulty" after repeated negative reports in a short window | For example, 3 negative reports in 24 h pushes the point to the top of the admin queue with a badge; the threshold is set in one constant | Must |

Why the AI earns its place: FR6.1 cuts fake or spam reports, FR6.2 saves typing on the move, and FR6.3 helps JPPHB decide what to fix first. All three support target 6.b.

## Non-functional requirements

The brief allows at most two NFRs, so these are the only two. Each needs measured evidence in the video and the report.

| ID | Requirement | Why it matters at UM | How we prove it | Owner |
| --- | --- | --- | --- | --- |
| NFR1 Usability | A first-time user finds the nearest working point in a chosen faculty in 3 taps or fewer and under 10 s, with no login | People look for water between classes; a slow app loses to the vending machine | Timed runs with 10+ new users, tap count logged; report the median and the worst time | #6, with #5 |
| NFR2 Map performance | A floor plan with its pins loads and renders within 2 s on a mid-range Android phone | A slow or janky map pushes people back to bottled water | Time from tap to first frame with pins, measured 20 times on a named mid-range phone (log timestamps or use the Android Studio profiler) | #3, with #6 |

To hit NFR2, compress floor-plan images (WebP, at most 1080 px wide) and load point rows off the main thread.

## Screens and navigation

Five bottom-navigation tabs are the lateral level. Everything else is a descendant screen opened with an explicit Intent (for example `putExtra("pointId", id)`), and `finish()` goes back. No screen is a dead end, and each one must be declared in `AndroidManifest.xml`.

&#91;embedded content: UMRefill navigation · 5 tabs, 11 screens\]

The Floor plan and Point list are two views of the same floor, toggled with tabs. Both lead to Point detail, and so does scanning a station's QR code. The Impact tab is a single screen holding the personal counter and the faculty totals. #5 owns the wireframes for all 11 screens in Figma, and #4 keeps this chart up to date.

## Data model and architecture

Screens never touch Room directly. They call repository interfaces, so a Supabase version can be added later without changing any screen. Schema changes go through #2 only.

**Layers**

1. **UI**: Activities and XML layouts (ConstraintLayout at the root, RecyclerView for lists)
2. **Repository interfaces**: `RefillPointRepository`, `ReportRepository`, `UserRepository`, `ImpactRepository`
3. **Room implementation**: DAOs and entities; queries run on an `ExecutorService`, never on the main thread
4. **AI helpers**: `PhotoChecker` (ML Kit labelling), `ReportClassifier`, `FaultFlagger`, all in the `ai` package

**Package layout:** `my.edu.um.umrefill` → `ui.*` (one sub-package per module), `data.entity`, `data.dao`, `data.repo`, `ai`, `util`

**Entities**

| Entity | Key fields | Notes |
| --- | --- | --- |
| Faculty | facultyId, name, shortCode | Seeded |
| Floor | floorId, facultyId → Faculty, level, label, planImage | planImage = drawable name |
| EntryPoint | entryId, floorId → Floor, name, xPct, yPct | Lift, stairs or entrance used for "nearest" |
| RefillPoint | pointId, floorId → Floor, name, type, status, xPct, yPct, locationNote, lastConfirmedAt, qrCode, accessible | type: COLD, FILTERED, NORMAL; status: WORKING, BROKEN, EMPTY, DIRTY, OUT\_OF\_SERVICE |
| Report | reportId, pointId → RefillPoint, userId → User, status, note, photoPath, aiLabel, aiConfidence, createdAt, resolved, resolvedBy | Photos go to app internal storage |
| User | userId, email, passwordHash, salt, role, displayName | role: USER or ADMIN; never store plain-text passwords |
| RefillLog | logId, userId → User, pointId → RefillPoint, loggedAt | Feeds FR5.1 and FR5.2 |

**Nearest point (FR3.2):** take the chosen EntryPoint's (xPct, yPct) and pick the WORKING point on that floor with the smallest straight-line distance. Say in the report that this is approximate.

**Seed data:** walk-through teams fill one shared sheet (faculty, floor, point name, xPct, yPct, type, status, note, photo). #6 converts it to `assets/seed.json`, which loads on first launch.

## Team and workflow

Every member owns code and presents their own part in the video. The video's Team Contributions criterion and peer evaluation both depend on it. Write your name into the Member column.

| # | Role | Owns (code) | Also responsible for | Member |
| --- | --- | --- | --- | --- |
| 1 | Project manager and stakeholder lead | Module 5 admin screens, strings (EN/BM), About screen | Trello, deadlines, lecturer and JPPHB contact, video lead |  |
| 2 | Software architect and data lead | Room schema, repositories, Module 6 AI | Repo rules, code review, merging, integration | Afif |
| 3 | Floor-guide developer | Module 3 (plan, pins, nearest point, QR) | NFR2 measurement |  |
| 4 | Lead developer, browse and reporting | Modules 2 and 4 | Navigation chart |  |
| 5 | UI/UX designer and developer | Module 1 screens, impact view, theme, floor-plan images | Wireframes, colour scheme, accessibility |  |
| 6 | Tester and data lead | Module 5 dashboard and CSV export, seed loader | Survey, test matrix, user test, AI accuracy test |  |

**Conventions**

- Branches: `main` stays protected and always builds. Work on `feature/<module>-<short-name>` (for example `feature/m3-floor-pins`) and merge by pull request with at least one review.
- Commits: present tense, with the FR ID where it applies, for example `FR3.1: place pins by percent`.
- Code: Java naming (classes PascalCase, methods camelCase, constants UPPER\_SNAKE). View IDs `typeName`, for example `btnReport`, `rvPoints`. No hard-coded UI text; every string goes in `strings.xml`.
- Layouts: ConstraintLayout at the root, flat hierarchy, 0dp with constraints instead of fixed widths, `contentDescription` on every icon and pin.
- Trello: one card per FR or task, with the owner and due date, moved through To do → Doing → Review → Done. Weekly 15-minute stand-up, logged on the board.
- AI usage log: whenever you use an AI tool, add the tool, the prompt, and what you changed and tested to `AI_USAGE_LOG.md` the same day. The course policy requires this, and a missing log costs marks.

## Testing and validation

All testing happens 14–27 Dec. #6 keeps one test matrix with a row per FR and NFR: steps, expected result, pass or fail, tester, date, screenshot. Those results go straight into the video and the report.

| Activity | When | Output | Lead |
| --- | --- | --- | --- |
| Building walk-through, 2–3 faculties | 3–8 Nov | Points located and photographed by floor (seed data) | #1, #3, #4 |
| Student survey, 30+ responses | 3–10 Nov | How often people fail to find water and buy bottles | #6 |
| JPPHB or facilities interview | By 15 Nov | How faults are reported today; quotes for the video | #1 |
| Functional tests, FR1.1–FR6.3 | 14–27 Dec | Pass or fail per FR | #6, each owner tests another person's module |
| AI accuracy (FR6.1) | 14–27 Dec | Accuracy and false-warning rate on 30–50 labelled UM photos | #6, #2 |
| NFR1 timed runs | 14–27 Dec | Taps and seconds per new user | #6, #5 |
| NFR2 load timing | 14–27 Dec | 20 timings on one mid-range phone | #3 |
| User test, 10+ students | 14–27 Dec | Can they find water in an unfamiliar faculty? List of changes it led to | #6, all |

Keep the raw survey export, interview notes and test sheets in `docs/evidence/` in the repo. The report must show the data itself, not just a summary.

## Risks, open questions and decisions

| Risk | Effect | Mitigation |
| --- | --- | --- |
| Mapping buildings by hand is slow | Thin coverage | Only 2–3 buildings for the demo; full coverage is future work |
| No official floor plans | No base image | Trace a simple plan from the walk-through; a rough sketch is enough for pins |
| Reports never reach maintenance | Fountains stay broken | Interview JPPHB early; at minimum an admin fault list plus CSV export |
| Local storage only | Reports not shared between phones | Seeded demo data; Supabase as future work |
| AI photo check gives wrong warnings | Users ignore it | Warn, never block; report the measured accuracy honestly |
| Uneven contributions | Low peer-evaluation scores | Every member owns code from week 1; Trello and commits as evidence |
| Privacy | Ethics issue | No login to browse; store only UM email and reports; hash passwords |

**Open questions (owner #1 to close by 10 Nov)**

- [ ] Lecturer: is 6.1 acceptable as a secondary target, given that the brief lists 6.5, 6.6 and 6.b?
- [ ] Lecturer: is a local Room build with a backend as future work acceptable?
- [ ] JPPHB: can we get floor plans, and which unit receives fountain faults today?
- [ ] Team: agree which 2–3 faculties to map first
- [ ] Team: fill in names in the role table
- [ ] Check that the name "UMRefill" is free to use

**Decisions made**

- Java + XML instead of Kotlin. Update the proposal's tech-stack section to match.
- 6.b is the primary target.
- Module 6 (AI) and QR codes are added to v1.
