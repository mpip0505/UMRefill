# CLAUDE.md

Context for Claude Code sessions on this repo. Read `README.md` (overview, stack, structure) and `CONTRIBUTING.md` (workflow) first.

## Key docs

- `docs/UMRefill Product Requirements Document.md`: requirements (FR/NFR IDs used everywhere)
- `docs/design/DESIGN_BRIEF.md`: personas, hard rules, screens. Check every UI change against its hard rules.
- `docs/AI_USAGE_LOG.md`: **add a row the same day for any AI-assisted work** (course requirement)
- `docs/evidence/`: surveys, interviews, photos, test results

## Who is working on what

Source of truth is GitHub Issues. Refresh before relying on this table:
`gh issue list --state all --json number,title,state,assignees`

Snapshot as of 2026-11-11:

| Issue | Member | GitHub | Task | Type |
|---|---|---|---|---|
| #2 | Member 1 & 2 | `wong-yanwen` (Member 2 not yet assigned) | Survey refill points in FSKTM + one other faculty, entry points, shortest paths, x/y % positions, evidence to `docs/evidence/` | survey |
| #3 | Member 1 & 2 | `wong-yanwen` | Floor maps in app by faculty → level → entrance/stair, with a **computed (non-static) path** to nearest working point. Depends on #2 | code: `ui/floorguide/` |
| #4 | Member 3 | `123EFD` | Admin login page + login/logout button on Account tab, UM-email only, Admin tab visibility | code: `ui/account/` |
| #5 | Member 4 | `AustinCYK` | Deliverables, report/video requirements, weekly updates | docs |
| #6 | Member 5 | `25-ESTHERKONG` | Interview KK office and Dean's office on how refill stations are managed | research |
| — | Afif | `mpip0505` | Repo owner: skeleton, architecture/data layer, design brief, writes the issues | |

When starting work on an issue, check its body (`gh issue view <n>`) for files, branch name and "Done when".

## Open inconsistencies (resolve before building the affected screens)

- **Pathway:** issue #3 requires an app-drawn path (graph/A*), while README and the design brief describe a static plan with pins and "no turn-by-turn directions". A computed path overlay is not live navigation, but README, PRD FR3.2 and the brief need updating once #3 picks an approach.
- **Reporting login:** the design brief implies anyone can update a status; README/PRD require UM email login to report.
- **Name:** brief uses "UM Refill", code/README use "UMRefill".

## Rules

- Java + XML only (no Kotlin/Compose). Do not change AGP, Gradle or SDK versions.
- Screens use repository interfaces via `ServiceLocator`, never `new FakeXxxRepository()`.
- UI text goes in `strings.xml`, with BM in `values-ms`.
- Feature branch + PR into `main` with one review; no direct pushes to `main`.
- Don't commit `.idea/` changes unless intended.
