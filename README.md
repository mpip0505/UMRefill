# UMRefill

**Find the nearest working water refill point at Universiti Malaya, and cut single-use plastic.**

UMRefill is an Android app for the UM community. Pick a faculty and a floor, and see a 2D floor plan with the refill points pinned on it, the nearest working one highlighted. Users can report a station that is broken, empty or dirty, so the map stays trustworthy.

> **Status:** planning and early development. Group project for **WIA2007 Mobile Application Development**, Semester 1, 2026/2027. Sections marked _(planned)_ describe the design, not finished code.

**SDG:** 6 (Clean Water and Sanitation). Primary target 6.b (community participation in water management); 6.1 (safe drinking water access) as a secondary link, pending lecturer confirmation.

---

## The problem

Students and staff often don't know where the nearest working fountain or refill point is in a given faculty, so they buy bottled water instead. Broken, switched-off or dirty fountains make it worse, with no way to know a station works before walking to it. This is a working hypothesis the team is validating through building walk-throughs, a student survey and contact with JPPHB or the Sustainability Centre.

## Signature feature: the 2D floor guide

Four taps: **pick faculty → pick floor → 2D floor plan with pinned points → nearest working point highlighted.**

This is a static floor-plan viewer with pins, **not** live indoor turn-by-turn navigation:
- Each floor is a simple 2D plan (traced by the team), stored as an image with points placed by coordinates.
- "Nearest" is measured from a reference point the user picks (entrance, lift or stairs), not live GPS, which does not work reliably indoors.
- "Directions" are a pin on the plan plus a short note ("Level 2, left of the lifts").

## Features _(planned)_

Six modules, three or more functional requirements each. Full list with acceptance criteria: [`docs/UMRefill Product Requirements Document.md`](docs/).

1. **Access and profile:** browse without login; UM email login for reporting and favourites; normal-user and admin roles.
2. **Faculty and floor browser:** pick faculty then floor, list points with location notes, filter by status and type.
3. **2D floor guide:** floor plan with pinned points, nearest working point highlighted, tap a pin for type, status and last-confirmed date.
4. **Community reporting:** report a point broken, empty, dirty or working, with optional photo; status updates; admin review.
5. **Impact and admin:** personal and campus counters of refills and bottles avoided; admin manages points, floor plans and CSV export.
6. **AI-assisted reporting:** on-device photo check, report text classification, and "likely faulty" flagging.

### Non-functional requirements

- **Usability:** a first-time user finds the nearest working point in a chosen faculty within 3 taps and under 10 seconds, no login.
- **Map performance:** a floor plan with pins loads and renders within 2 seconds on a mid-range Android phone.

## Tech stack

| Layer | Choice |
|---|---|
| Platform | Native Android, **Java + XML layouts** |
| IDE | **Android Studio (required for all team members)** |
| Build | Gradle 9.6 wrapper, Android Gradle Plugin 9.4, JDK 25, compile/target SDK 37, min SDK 26 |
| Database | Room (SQLite) on the device |
| Accounts | Local accounts, hashed passwords; browsing needs no login |
| Floor plans | Images bundled in the app, points placed by coordinates |
| Images | App internal storage |
| Collaboration | GitHub, Trello |

### Architecture _(planned)_

The app reads and writes data only through repository interfaces, for example `RefillPointRepository` and `ReportRepository`. A Room implementation is built now. A shared backend (such as the Supabase free plan) can be added later without changing the screens, so a report from one phone becomes visible to everyone.

**Limitation:** data is stored on one device. Reports are not shared between phones, and the demo uses seeded sample data with the normal-user and admin roles on one device. Cross-device sharing is future work.

## Getting started

> **Android Studio is the required IDE for this project.** Every team member codes, builds, runs and debugs in Android Studio. Don't use VS Code, IntelliJ Community or other editors for project work. Layout previews, the emulator, run configurations and the shared `.idea` settings all assume Android Studio.

### Requirements

| What | Version | Notes |
|---|---|---|
| Android Studio | Latest stable, recent enough for **Android Gradle Plugin 9.4** | Older versions fail to sync with "AGP version not supported". Update via **Help → Check for Updates**. |
| JDK | **25** (the JetBrains Runtime bundled with Android Studio) | Gradle downloads it automatically on first sync (`gradle/gradle-daemon-jvm.properties`), so you don't need to install a JDK yourself. |
| Android SDK Platform | **API 37** (compile/target SDK) | Install in **Tools → SDK Manager → SDK Platforms**. Android Studio usually offers this when you sync. |
| Device | Emulator or phone on **Android 8.0 (API 26) or higher** | A Pixel emulator on a recent API level is the easiest option. |
| Gradle | 9.6 (via the wrapper) | Already in the repo. Don't install Gradle separately. |
| Git | Any recent version | And a GitHub account added as a collaborator on the repo. |

### First-time setup

1. **Clone the repo.** In Android Studio: **File → New → Project from Version Control**, paste the URL below, and pick a folder. Or from a terminal:
   ```bash
   git clone https://github.com/mpip0505/UMRefill.git
   ```
   Then open the `UMRefill` folder with **File → Open**. Open the root folder, not `app/`.
2. **Let Gradle sync finish.** The first sync downloads Gradle, JDK 25 and the dependencies, so it can take 5 to 10 minutes. Wait for the progress bar at the bottom of the window to finish.
3. **Install missing SDK parts** if Android Studio asks (for example "Install missing SDK platform 37"). Click the link in the sync error.
4. **Create an emulator** if you don't have one: **Tools → Device Manager → + → Create Virtual Device**, pick a Pixel phone and a recent system image.
5. **Run it.** Select the `app` run configuration and your device in the toolbar, then press **Run ▶** (`Shift+F10` on Windows, `Ctrl+R` on macOS). You should see 5 bottom tabs and the faculty list.

Android Studio creates `local.properties` with your SDK path. It's machine-specific and git-ignored, so never commit it.

### Run the tests

In Android Studio, right-click `app/src/test/java` and choose **Run 'Tests in ...'**. Or from the Android Studio **Terminal** tab:

```bash
./gradlew testDebugUnitTest     # unit tests (gradlew.bat on Windows)
./gradlew assembleDebug         # build a debug APK
```

### Troubleshooting

| Problem | Fix |
|---|---|
| Sync fails with an AGP or Gradle version error | Update Android Studio to the latest stable version. Don't downgrade the versions in `gradle/libs.versions.toml`. |
| "Failed to find target android-37" | Install SDK Platform 37 in the SDK Manager. |
| Strange build errors after pulling | **Build → Clean Project**, then **File → Sync Project with Gradle Files**. If that doesn't fix it, try **File → Invalidate Caches → Invalidate and Restart**. |
| App crashes after a teammate changed the database schema | Uninstall the app from the emulator, or clear its data, and run again. |
| Emulator very slow or won't start | Enable hardware virtualisation (Windows: turn on virtualisation in BIOS and Windows Hypervisor Platform), or run on a real phone with USB debugging on. |

For now the app runs on **in-memory fake data** (`data/fake/FakeStore.java`), so everyone sees the same sample faculties, floors and points, and nothing is saved between runs. The sample data is made up and will be replaced by the building walk-through data.

Demo accounts (temporary shortcuts are on the Account tab until login is built):

| Email | Password | Role |
|---|---|---|
| `admin@um.edu.my` | `admin123` | Admin |
| `student@siswa.um.edu.my` | `student123` | User |

**Who builds what, and how to start:** see [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Project structure

```
app/src/main/java/com/umrefill/app/
  data/
    entity/       Room entities: Faculty, Floor, EntryPoint, RefillPoint, Report, User, RefillLog
    repo/         repository interfaces (the only way screens touch data)
    fake/         in-memory fake repositories + sample data (used until Room is ready)
    ServiceLocator.java   picks which repository implementation the app uses
  ai/             PhotoChecker, ReportClassifier, FaultFlagger (Module 6)
  ui/
    main/         MainActivity: bottom navigation shell
    browse/       faculty list (worked example) and floor list
    floorguide/   floor screen shell, plan view, list view, point detail
    report/       report a point
    scan/         QR scan tab
    account/      account tab, login, reset password
    impact/       impact tab
    admin/        admin tab, report queue, manage points
    common/       BaseActivity, TwoLineAdapter, Placeholder (temporary)
  util/           Extras (intent keys), SessionManager, PasswordUtils, AppExecutors
app/src/test/     local unit tests (example: FakeRefillPointRepositoryTest)
docs/             PRD, AI usage log, evidence (survey, interviews, test results)
  design/         design brief (personas, hard rules, screens), see DESIGN_BRIEF.md
```

**Before building any screen, read [`docs/design/DESIGN_BRIEF.md`](docs/design/DESIGN_BRIEF.md).** It has the personas and the hard rules every screen is checked against.

## Team workflow

- Work on a **feature branch** and open a **pull request**. At least one teammate reviews before merge. No direct pushes to `main`.
- **Schema changes** (entities, DAOs, database version) go through the data layer lead via an issue or Trello card, in a small PR. Rebase after it merges.
- Build screens against the repository **interfaces** via `ServiceLocator`, never `new FakeXxxRepository()`. The fake version is used until Room is ready, and swapping it changes one file.
- While developing, Room may wipe and rebuild on schema change. Reinstall or clear app data after pulling a schema change.
- Do **not** commit `.db` files, `build/`, `local.properties` or personal `.idea` files (`workspace.xml`, `deviceManager.xml`, `deploymentTargetSelector.xml`). Shared `.idea` files such as code style and inspections are committed on purpose.
- Don't change the AGP, Gradle or SDK versions in your PR. If Android Studio offers an upgrade ("AGP Upgrade Assistant"), decline it and tell the data lead.
- Keep Trello cards and commit messages clear, since commit logs and board reports are used as contribution evidence.

## Team

| Role | Member | Main responsibilities |
|---|---|---|
| Project manager and stakeholder lead | _TBD_ | Plan, Trello, deadlines, JPPHB and faculty contacts, building surveys, video lead |
| Software architect and data layer lead | _TBD_ | Room schema, repository interfaces, integration, code review |
| Floor-guide developer | _TBD_ | 2D floor plan rendering, pins, nearest-point logic |
| Lead developer, browse and reporting | _TBD_ | Faculty and floor browser, report flow |
| UI/UX designer and developer | _TBD_ | Wireframes, colour scheme, accessibility, floor-plan assets |
| Tester and data lead | _TBD_ | Test plan, survey analysis, user testing, NFR measurements, dashboard and CSV export |

## Timeline

| Date | Milestone |
|---|---|
| 23 Oct 2026 | Project start |
| Late Oct to mid Nov 2026 | Building surveys, student survey, wireframes, floor plans, schema |
| Mid Nov to 20 Dec 2026 | Build: browse and floor guide, then reporting and impact |
| 6 Dec 2026 | Floor guide and core browse working for one faculty |
| 14 to 27 Dec 2026 | Testing and user testing |
| 12 pm, 3 Jan 2027 | Video due |
| 12 pm, 10 Jan 2027 | Report due |

Dates may change once the UM calendar is checked.

## AI usage

Teammates may use AI tools to brainstorm, explain and refine code. Every use is recorded in [`docs/AI_USAGE_LOG.md`](docs/AI_USAGE_LOG.md) with the tool, the prompt, and how the output was tested and changed, as the course requires.

## Future work

- Shared backend (Supabase free plan) so reports sync between phones, with push alerts for fixed stations
- Map all faculties and floors, not just the demo buildings
- iOS version through a cross-platform rewrite
- Live indoor positioning (Bluetooth beacons) for true nearest-point directions

## Course and licence

Universiti Malaya, WIA2007 Mobile Application Development, group project 2026/2027 Semester 1. Academic project, so no licence is granted yet.
