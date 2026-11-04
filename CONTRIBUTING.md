# Contributing to UMRefill

This file tells each developer which files are theirs, how to start, and the rules that keep `main` working.
Requirement IDs (FR1.1, NFR2...) refer to the PRD in `docs/`.

## 1. Get the project running (everyone, first day)

**Android Studio is mandatory.** All coding, building, running and debugging is done in Android Studio, not another editor.

1. Follow **Getting started** in [`README.md`](README.md#getting-started): install the latest stable Android Studio, clone the repo, and let Gradle sync finish (the first sync is slow).
2. Run the app on an emulator (API 26 or higher). You should see the 5 tabs and the faculty list.
3. Run the unit tests once (right-click `app/src/test/java` → **Run Tests**) so you know they pass before you change anything.
4. Make a warm-up PR: add your name to the Team table in `README.md` (see section 4 for how). This proves Git, Android Studio and reviews all work for you.

## 2. Who owns what

Each developer owns **separate files**, so nobody edits the same screen. Every screen you own is a placeholder that lists its TODOs and FR IDs. Replace the placeholder with the real screen.

| Owner | Requirements | Your files (in `app/src/main/`) |
|---|---|---|
| **Dev A: browse and list** | FR2.1, FR2.2, FR2.3 | `ui/browse/FloorListActivity`, `ui/floorguide/PointListFragment`, layouts `activity_floor_list`, `fragment_point_list` |
| **Dev B: floor guide and QR** | FR3.1, FR3.2, FR3.3, FR3.4, NFR2 | `ui/floorguide/FloorPlanFragment`, `PointDetailActivity`, `ui/scan/ScanFragment`, layouts `fragment_floor_plan`, `activity_point_detail`, `fragment_scan`, floor-plan images in `res/drawable` |
| **Dev C: reporting and admin** | FR4.1, FR4.2, FR4.3, FR5.3 | `ui/report/ReportActivity`, `ui/admin/*`, layouts `activity_report`, `fragment_admin`, `activity_report_queue`, `activity_manage_points` |
| **Dev D: accounts and impact** | FR1.1, FR1.2, FR1.3, FR5.1, FR5.2 | `ui/account/*`, `ui/impact/ImpactFragment`, layouts `fragment_account`, `activity_login`, `activity_reset_password`, `fragment_impact` |
| **Data and AI lead** | Room, FR6.1, FR6.2, FR6.3 | `data/**` (entities, Room DAOs and database, `ServiceLocator`), `ai/*`, code review and merging |

Non-developers on the team own the survey, test matrix, seed data, `strings.xml` translations (`values-ms`), colours, wireframes and floor-plan tracing. These need commits too, so they show in the Git log.

**Shared files.** These files belong to everyone: `AndroidManifest.xml`, `res/values/strings.xml`, `app/build.gradle.kts`, `gradle/libs.versions.toml`. In them, **add lines only**, keep the PR small, and merge it quickly. Add strings under a comment with your module name. A new dependency needs a word with the data lead first.

**Don't edit someone else's screen.** If you need something from it (an extra passed in, a method), ask the owner or open an issue.

## 3. How the data works

- Screens get data only through `ServiceLocator`: `ServiceLocator.refillPoints()`, `.reports()`, `.users()`, `.impact()`.
- These return **fake in-memory repositories** for now (`data/fake/`), loaded with sample data, so you can build your screen today. Nothing is saved between runs.
- When the Room versions are ready, the data lead changes `ServiceLocator` only. Your screen keeps working, but data calls must then run off the main thread. See `util/AppExecutors` for the pattern.
- Need a new query or field? Ask the data lead. Don't edit `data/` yourself, because schema changes break everyone.
- Who is logged in: `SessionManager.currentUser()`, `isLoggedIn()`, `isAdmin()`.
- Passing IDs between screens: `intent.putExtra(Extras.POINT_ID, id)`, then `getIntent().getLongExtra(Extras.POINT_ID, -1)`.

**Temporary shortcuts.** Placeholder screens show "(temp)" buttons, for example "Log in as demo admin" on the Account tab, so you can reach your own screen before the screens in front of it are built. When you build your real screen, delete its `Placeholder` calls. Once no screen uses `ui/common/Placeholder.java`, the data lead deletes it.

## 4. Git workflow

`main` is protected: no direct pushes, and every change goes through a pull request with one approving review.

```bash
git checkout main
git pull                                   # every time before you start
git checkout -b feature/fr2-floor-list     # feature/<fr-or-module>-<short-name>
# ...work, run the app, run the tests...
git add .
git commit -m "FR2.1: list floors sorted by level"
git push -u origin feature/fr2-floor-list
# On GitHub: open a pull request into main, fill in the template, request a review.
```

- Keep PRs small: one screen or one FR. A PR should be reviewable in 10 minutes.
- If `main` moved while you worked, run `git pull origin main` on your branch, fix any conflicts, test again, and push.
- Reviewers check that it builds, that it runs, and that it follows the conventions below. Comment kindly, and approve when it's good enough.
- Commit messages start with the FR ID when there is one. Clear commits are your contribution evidence for peer evaluation and the report.

## 5. Code conventions

- **Java**: classes `PascalCase`, methods and variables `camelCase`, constants `UPPER_SNAKE_CASE`.
- **View IDs**: type prefix + name, for example `btnReport`, `txtStatus`, `editEmail`, `recyclerPoints`, `imgPlan`.
- **Layouts**: `ConstraintLayout` as the root, a flat hierarchy, `0dp` plus constraints instead of fixed widths, every view constrained horizontally and vertically.
- **No hard-coded text** in layouts or Java: put it in `strings.xml`, and add the BM version in `values-ms/strings.xml` if you can.
- **Accessibility**: `contentDescription` on every icon, image and pin; touch targets at least 48dp; status always shown with a word or icon, never colour alone.
- **Lifecycle**: set up views in `onCreate` / `onViewCreated`; reload data that can change in `onResume`.
- **Tests**: logic that isn't UI (filters, nearest point, classifiers) goes in a plain Java method with a unit test in `app/src/test/`. See `FakeRefillPointRepositoryTest` for the pattern.

## 6. AI usage log (required by the course)

Whenever you use an AI tool (ChatGPT, Claude, Copilot, Gemini...) for this project, add a row to [`docs/AI_USAGE_LOG.md`](docs/AI_USAGE_LOG.md) **the same day**: the tool, the prompt, what you used, and how you tested or changed it. You must understand and be able to explain every line you commit, since you present your own code in the video.
