# Neuromind — Progress Tracker

**Current stable version:** v4.0-rc1  
**Next target:** v4.0 (Phase 1 — AI Intelligence Loop)  
**Last updated:** 2026-06-12

---

## What's Done (v3.9 Stable)

### Core Infrastructure
- [x] MVVM architecture with Room + DataStore + Kotlin Flows
- [x] Integer primary keys on all entities (migrated from UUID — fixes type-mismatch crashes)
- [x] Database version 8, destructive migration with demo data re-seed
- [x] Central `TaskRepository` as single source of truth
- [x] Theme persistence (Light / Dark / System) via DataStore
- [x] Edge-to-edge UI with transparent status bar
- [x] Bottom navigation bar with 4 tabs
- [x] App-wide compiler warnings cleaned (unchecked casts, deprecated statusBarColor)

### Dashboard
- [x] Dynamic greeting (Morning / Afternoon / Evening)
- [x] Pending + Completed task count cards
- [x] "Today's Priorities" — surfaces overdue + HIGH priority tasks
- [x] "Today's Timetable" — next 2 upcoming events for today
- [x] AI-Generated Plan — Scheduler slots tasks into free timetable gaps
- [x] Burnout Warning card (appears when recent feedback shows stress/low energy)

### Task Management
- [x] Task List with 5 filter chips: All, Today, Overdue, Upcoming, Completed
- [x] Priority badges (HIGH/MED/LOW) with color coding on task cards
- [x] Interactive checkboxes (mark complete, persists immediately)
- [x] Tap task card to edit
- [x] Add/Edit Task screen with full field set: title, description, priority, difficulty, due date
- [x] Reschedule mode (triggered from Dashboard, surfaces overdue tasks)
- [x] Delete task (trash icon on card)

### Timetable
- [x] Agenda view (replaces broken grid view) — orders days starting from today
- [x] Shows recurring (weekly) and one-time events correctly
- [x] Add/Edit entry dialog with title, venue, details, day picker, time pickers, recurring toggle
- [x] Edit by tapping a card; delete icon on each card
- [x] "TODAY" badge on current day header

### Feedback / End-of-Day Review
- [x] Mood selection (STRESSED → GREAT) with filter chips
- [x] Energy level slider (1–5)
- [x] Tasks completed input
- [x] Additional thoughts text field
- [x] Submits to Room `FeedbackLog` table

### Insights
- [x] Weekly completion bar chart (Vico library)
- [x] Wellness score progress bar (aggregated from recent mood + energy)

### Settings
- [x] Theme picker dialog (Light / Dark / System)
- [x] Navigation links to Timetable and Feedback screens
- [x] Developer mode (unlock by tapping version 4×)
  - Generate 20 diverse demo tasks
  - Generate full Mon–Fri academic timetable
  - Reset all app data
  - Test notification

### Notifications
- [x] Smart "Due in X mins" alerts
- [x] 45-minute check window (battery-saver friendly)
- [x] Test notification button in Settings

### Focus Mode
- [x] Screen + route exist (`focus_mode/{taskId}`, now in `Screen.FocusMode`)
- [ ] **No UI element navigates to it yet** — route is registered but unreachable from any screen
- [x] Shows task title prominently
- [x] Real 25-minute countdown timer with `LaunchedEffect`
- [x] Start / Pause / Resume / Reset controls
- [x] Circular progress ring (`CircularProgressIndicator`) around timer
- [x] Session-complete dialog with "Another round" option

### NLP
- [x] `SmartInputHelper` class with regex parsing (detects "tomorrow", "at Xpm")
- [x] `ParsedTask(title, dueDate)` output model

---

## In Progress / Partially Done

### Focus Mode (Pillar 3)
- [x] Real countdown timer (done — see What's Done above)
- [ ] Do Not Disturb integration (`NotificationManager.setInterruptionFilter`)
- [ ] Sound/vibration on session end

### Task Dependencies (Pillar 2)
- [ ] `prerequisiteTaskId: Int?` field exists on `Task` entity
- [ ] **No UI to set or view dependencies** — field is invisible to the user
- [ ] Dependent tasks are not visually blocked in the list

### SmartInputHelper / NLP (Pillar 4)
- [ ] `SmartInputHelper.parseInput()` exists in `domain/`
- [ ] **Not wired into `AddEditTaskScreen`** — the NLP text box UI is missing
- [ ] Only handles "tomorrow" and "at Xpm" — no day names, no relative dates like "next Monday"

---

## Planned — Phase 1: AI Intelligence Loop (v4.0)

> Goal: Make the feedback data actually drive the schedule.

### Pillar 9 — End-of-Day Review (Already UI-complete, needs data pipeline)
- [ ] Connect `FeedbackLog` trends to Dashboard Burnout Warning (currently hardcoded/mock)
- [ ] Surface "Recent Notes" section in Insights from past feedback comments

### Pillar 1 — Hyper-Personalized AI Core
- [ ] Add "Cognitive Profile" section to Settings:
  - Peak focus hours (e.g., 8–11 AM)
  - Preferred session length (25 / 45 / 90 min)
  - Task style preference (creative vs analytical)
- [ ] Persist profile in DataStore
- [ ] Upgrade `Scheduler` to prioritize HIGH difficulty tasks during peak hours

### Pillar 12 — Emotional Pattern Detection (Early Burnout Detection)
- [ ] Sliding window analysis over last 7 days of FeedbackLog
- [ ] Trigger: 3+ consecutive low-energy days → show "Schedule a rest day?" prompt
- [ ] Trigger: anxiety pattern on specific weekday → suggest lighter schedule
- [ ] Wire results to Dashboard Burnout Warning card

---

## Planned — Phase 2: Advanced Task & Project Handling (v4.5)

### Pillar 2 — Task Dependencies (UI)
- [ ] In AddEditTask: "Depends on…" dropdown to pick a prerequisite task
- [ ] In TaskListScreen: visually grey out / lock dependent tasks whose prerequisite is incomplete
- [ ] Tooltip: "Blocked by: [Task Name]"

### Pillar 5 — Study Plan Generator
- [ ] In AddEditTask: "Break this down" button for large tasks
- [ ] Input: goal text (e.g., "Revise Bio chapters 4, 5, 8")
- [ ] Output: auto-generated sub-tasks with suggested durations
- [ ] Schedule sub-tasks across the week using `Scheduler`

### Pillar 7 — Interactive Scheduling ("Find a Time")
- [ ] Button in AddEditTask: "Find a Time"
- [ ] Shows a list of 3–5 AI-calculated free slots
- [ ] User picks a slot → populates `dueDate` automatically

---

## Planned — Phase 3: UX & Focus Polish (v5.0)

### Pillar 3 — Real Focus Mode (Pomodoro Timer)
- [x] Working countdown timer (25 min, `LaunchedEffect`-driven)
- [x] Start / Pause / Resume / Reset controls
- [x] On completion: "Session Complete! Take a break?" dialog with "Another round"
- [ ] Do Not Disturb integration (`NotificationManager.setInterruptionFilter`)
- [ ] Sound/vibration on session end
- [ ] Configurable session length (currently hardcoded 25 min)

### Pillar 4 — Advanced NLP
- [ ] Wire SmartInputHelper to a dedicated "Quick Add" text field in AddEditTask
- [ ] Expand regex to cover: "next Monday", "in 3 days", "every Tuesday at 9am"
- [ ] Level 2 (future): on-device TFLite model for entity extraction (date/time/subject)

### Pillar 8 — Swipe Gestures
- [x] Swipe right on task card → toggle complete (haptic feedback, card snaps back)
- [x] Swipe left → delete task (card dismisses, removed from list)
- [x] `SwipeToDismissBox` (M3) implemented in `TaskListScreen.kt`
- [ ] Swipe left → "defer" option (currently only delete; defer would push due date by 1 day)

### Pillar 10 — Timetable Polish (ongoing)
- [ ] Smooth scroll to current time on open
- [ ] Color-code entries by category (lecture / gym / personal)
- [ ] Import timetable from photo using ML Kit OCR (long-term)

---

## Planned — Phase 4: Advanced Coaching (v6.0+)

### Pillar 11 — Context-Aware Suggestions
- [ ] Time-aware nudges: "You have 15 mins before your next class. Quick review?"
- [ ] Weather integration: postpone outdoor tasks when raining
- [ ] GPS trigger: "You're near the library. Add a study session?"

### Pillar 13 — Retrospective Insight Engine
- [ ] Track: how often tasks are deferred, avg completion time vs estimate
- [ ] Insights: "You usually underestimate Math tasks by ~20 min"
- [ ] Visual: colour-coded task history (green = on-time, yellow = deferred, red = overrun)
- [ ] GitHub-style heatmap for study activity

### Pillar 14 — Scheduled Task Rebalancing
- [ ] Detect: missed 2+ hard tasks yesterday → offer to redistribute today's load
- [ ] "Rebalance Day" button on Dashboard when schedule is overloaded

### Pillar 15 — AI Assistant Avatar
- [ ] Text-only chatbot persona in a dedicated screen
- [ ] Responds to: "What should I work on next?", "How am I doing this week?"
- [ ] Gentle reframing for negative patterns ("You've been tired — let's do a lighter session")
- [ ] Phase 1: rule-based responses; Phase 2: on-device LLM (TFLite)

---

## UI/UX Design Improvements

> Informed by the **UI/UX Pro Max** skill (Jetpack Compose stack) + design system analysis for a calm, focus-oriented productivity app.

### Design System Recommendation (from uipro skill)

The skill recommends a **Flat Design / Minimal Single Column** system for a student productivity/wellness app:

| Token | Current | Recommended |
|---|---|---|
| Style | Material 3 (good) | Keep M3 + flatten shadows further, increase whitespace |
| Primary feel | Deep Calming Blue | Good fit — teal/blue tones match "calm focus" |
| CTA color | Secondary (orange) | Keep Warm Orange for all action buttons |
| Typography mood | Default M3 | Heavier heading weight, lighter body weight for breathing room |
| Animation speed | None (missing) | 150–300ms ease for micro-interactions; 300ms for screen transitions |
| Whitespace | Moderate | Increase vertical padding between sections on Dashboard |

---

### Critical Issues (Fix First)

- [x] **Missing `contentDescription` on icon-only buttons** — added meaningful descriptions on all tappable icons (e.g., "Overdue" / "High priority" on Dashboard priority icon).

- [x] **`collectAsState()` → `collectAsStateWithLifecycle()`** — migrated all screens; added `lifecycle-runtime-compose` dependency to `build.gradle.kts`.

- [x] **`IntrinsicSize.Min` in TimetableScreen** — removed; `VerticalDivider` now uses explicit `height(40.dp)` instead of `fillMaxHeight()`.

- [x] **`key` missing from `items()` in `AgendaList`** — added `key = { it.id }` to both AgendaList and TaskItemList.

---

### High Impact UX Improvements

- [x] **Skeleton loading screens** — shimmer skeleton (`rememberInfiniteTransition`) shown while `isLoading = true` in `TaskListScreen`; replaces the empty flash on launch.

- [x] **Swipe-to-complete on task cards** — `SwipeToDismissBox` (M3): swipe right → toggle complete (with haptic feedback), swipe left → delete. Card snaps back on complete so the item stays visible.

- [x] **Empty state illustrations** — filter-aware `EmptyState` composable in `TaskListScreen` shows a distinct icon + message per filter (All Clear, No tasks today, Nothing overdue, etc.).

- [x] **Focus Mode screen real UI** — full rewrite: `CircularProgressIndicator` progress ring, Start / Pause / Resume / Reset buttons, session-complete dialog with "Another round" option.

- [x] **Feedback screen mood chips** — `Row` → `FlowRow` (M3 `ExperimentalLayoutApi`) with `spacedBy(8.dp)` horizontal + vertical spacing so chips wrap naturally on narrow screens.

- [x] **Loading button state** — "Submit Review" in FeedbackScreen and "Save" in AddEditTaskScreen both disable and show `CircularProgressIndicator` during the Room write.

---

### Medium Impact UX Improvements

- [x] **`AnimatedContent` on task list filter** — `AnimatedContent(targetState = TaskListContentState(filter, isLoading, tasks))` with `fadeIn(tween(200)) togetherWith fadeOut(tween(150))` crossfade; the content lambda reads only its target-state snapshot so the outgoing frame never shows new data.

- [x] **Task card priority left-border** — 4dp colored stripe (red = HIGH, amber = MED, grey = LOW) rendered as a `Box` inside a `Row(IntrinsicSize.Min)` as the Card's first child. (The original `drawBehind` approach was invisible — Card's Surface painted over it.)

- [x] **Insights wellness score animation** — `animateFloatAsState(target = score/100f, tween(1000, easing = EaseOut))` drives both a `LinearProgressIndicator` and a side `CircularProgressIndicator`. Score percentage shown in center of ring.

- [x] **Bottom nav icon scale animation** — `animateFloatAsState` with `spring(DampingRatioMediumBouncy)` scales selected icon to 1.15×. Gives a bouncy, satisfying selection feel.

- [x] **Add/Edit Task due date button** — label dynamically shows selected date/time; a "Clear date" `TextButton` (with × icon) appears when a date is set. Time picker opens automatically after date selection.

- [x] **Loading button state** — covered above under High Impact.

---

### Low Impact / Polish

- [x] **Transition animations between screens** — `NavHost` now uses `slideInHorizontally` / `slideOutHorizontally` (300ms) for forward navigation and reversed slides for back-navigation (`popEnter`/`popExit`).

- [x] **`TopAppBar` elevation on scroll** — `TopAppBarDefaults.pinnedScrollBehavior()` + `nestedScroll` modifier applied to Dashboard. TopAppBar shows surface tint shift as user scrolls.

- [x] **Haptic feedback on task complete** — `LocalHapticFeedback.performHapticFeedback(LongPress)` fires on the swipe-right complete gesture in `TaskListScreen`.

- [ ] **Color-code timetable entries by category** — would require a `category` field added to `TimetableEntry` (Room migration). Currently entries differentiate recurring vs one-time by container color. Full category tagging deferred to Phase 3.

- [ ] **Time format centralization** — due date formatting uses `SimpleDateFormat` inline in `AddEditTaskScreen` and timetable uses `DateTimeFormatter`. Create a `DateTimeUtils` object to unify. Deferred — low risk, cosmetic.

---

### Jetpack Compose Code Quality (from uipro skill — Compose stack guidelines)

These are code-level issues that directly cause visual/UX regressions:

| Issue | Location | Fix | Status |
|---|---|---|---|
| No stable `key` in lazy lists | `TimetableScreen:90`, `FeedbackScreen:51` | Add `key = { it.id }` or `key = { it.name }` | **DONE** |
| `collectAsState()` (lifecycle-unaware) | All screens | Migrate to `collectAsStateWithLifecycle()` | **DONE** |
| `IntrinsicSize.Min` in lazy list | `TimetableScreen:108` | Use explicit height or `wrapContentHeight()` | **DONE** |
| Priority border invisible (`drawBehind` under Card surface) | `TaskCard` in `TaskListScreen` | Restructured: `Row(IntrinsicSize.Min)` with a 4dp `Box` color stripe as first child — border now renders | **DONE** |
| `AnimatedContent` read outer `uiState` instead of target state | `TaskListScreen` | Target state is now a `TaskListContentState(filter, isLoading, tasks)` snapshot consumed via the lambda parameter — no stale-frame data bleed | **DONE** |
| `SimpleDateFormat` allocated every recomposition | `TaskCard`, `PriorityTaskRow`, `TaskDetailsDialog` | Wrapped in `remember { }` | **DONE** |
| Business logic in `NeuromindApp.kt` | NavHost composable (task filtering) | Move to ViewModels | Open |
| No `@Stable` / `@Immutable` on UiState | `TaskListUiState`, `TimetableUiState` | Annotate data classes to reduce recomposition | Open |
| No `rememberSaveable` on dialog state | `showAddDialog` in TimetableScreen | Use `rememberSaveable` so dialog survives rotation | Open |
| Missing `Modifier.testTag` | All screens | Add test tags to key interactive elements | Open |

---

## Known Issues & Technical Debt

| Issue | Severity | Notes |
|---|---|---|
| Focus Mode timer is static | ~~Medium~~ **Fixed** | Real `LaunchedEffect` countdown with Start/Pause/Reset |
| Focus Mode route unreachable | Medium | `focus_mode/{taskId}` is registered in NavHost but no button/card navigates to it |
| Feedback submit spinner never visible | Low | `isSubmitting` is set then screen navigates away immediately; spinner is dead code |
| SmartInputHelper not connected to UI | Medium | Class exists in domain layer but no entry point in screens |
| `prerequisiteTaskId` never set via UI | Low | Column exists in DB, invisible to user |
| `window.statusBarColor` deprecated (API 35+) | Low | Suppressed with `@Suppress("DEPRECATION")` — needs migration to `enableEdgeToEdge()` in MainActivity |
| Scheduler ignores task difficulty | Low | Only uses priority; should factor difficulty + user peak hours |
| No empty-state illustration | Low | Empty screens show plain text; could use illustrated placeholders |
| No unit tests | Medium | No tests for Scheduler, SmartInputHelper, or Repository logic |
| Database fallbackToDestructiveMigration | Low | Acceptable in dev; must switch to proper migrations before release |

---

## Improvement Suggestions (From Document Review)

### High Impact, Relatively Low Effort
1. **Wire SmartInputHelper** — Add a "Quick Add" text box at the top of TaskListScreen. Parse input → pre-fill AddEditTask form. This directly delivers Pillar 4 NLP value.
2. **~~Real Pomodoro timer~~** — Done. Working countdown with Start/Pause/Resume/Reset in `FocusModeScreen.kt`.
3. **Dependency UI** — A simple "depends on" dropdown in AddEditTask + lock icon on blocked cards is straightforward to implement now that the DB field exists.
4. **Connect Feedback to Burnout Warning** — The Dashboard card is already there. Read the last 7 `FeedbackLog` entries and compute a real signal instead of a mock condition.
5. **Navigate to Focus Mode** — `FocusModeScreen` is complete but unreachable; add a "Start Focus" button to each task card or the task detail dialog.

### Medium Impact
1. **Cognitive Profile in Settings** — A few DataStore keys for peak hours and session length unlocks the whole Pillar 1 scheduler upgrade.
2. **~~Swipe-to-complete~~** — Done. `SwipeToDismissBox` on task cards: swipe right = complete, swipe left = delete.
3. **Home Screen Widget** — Show "Next Class" or "Top 3 Tasks" without opening the app. Android Glance library (Jetpack) is the modern approach.

### Longer-Term / Research Required
1. **On-device ML for NLP** — TFLite model for entity extraction from free text. Great ML engineering practice.
2. **OCR timetable import** — ML Kit Text Recognition to photograph a printed timetable and auto-populate entries.
3. **Emotional Pattern Detection** — Sliding window over `FeedbackLog`; the data is already being collected.
