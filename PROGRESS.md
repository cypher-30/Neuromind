# Neuromind — Progress Tracker

**Current stable version:** v7.0 (all 15 pillars complete + Focus History + Home-screen Widget)
**Last updated:** 2026-06-16

---

## What's Done (v7.0 — Keyboard Fix, Focus History & Home-screen Widget)

### Keyboard Fix
- [x] **IME (keyboard) layout bug resolved** — global NavHost container now excludes the IME inset (`WindowInsets.safeDrawing.exclude(WindowInsets.ime)`); chat screen handles keyboard locally via `.imePadding()` on the input row so it docks neatly above the keyboard instead of compressing the whole layout
- [x] **AddEditTaskScreen** and **FeedbackScreen** each have `.imePadding()` on their scroll containers so text fields scroll above the keyboard

### Focus History & Stats (new feature)
- [x] **New `FocusSession` entity** — persists completed Pomodoro sessions; fields: taskId (nullable), taskTitle, durationMinutes, completedAt timestamp
- [x] **DB version 8 → 9** with **`MIGRATION_8_9`** — project's first real `Migration` (CREATE TABLE SQL); no data loss on upgrade
- [x] **`FocusSessionDao`** — insert, getAllSessions (Flow), deleteAllSessions
- [x] **`FocusViewModel`** — `recordCompletedSession()` writes to Room; invoked on natural timer completion (not on End Session / Reset)
- [x] **`FocusStats.summarize()`** — pure object; takes session list + optional referenceDate; returns `FocusSummary` (totalMinutes, sessionCount, minutesByDay × 7, bestDayLabel)
- [x] **Insights "Deep Work" card** — total focus minutes + session count + best day headline; 7-day animated mini bar chart (spring bounce, same pattern as Tasks Completed chart); uses tertiary colour for bars
- [x] **Dev tools** — "Seed Focus Sessions" (14 varied sessions over 7 days) and "Clear Focus Sessions"; "App & DB Info" dialog now shows focus session count and DB version 9

### Home-screen Widget (new feature)
- [x] **Jetpack Glance `TodayWidget`** — `GlanceAppWidget` reads today's incomplete tasks (up to 3) + next timetable entry; renders via Glance composables inside `GlanceTheme`; tapping opens the app
- [x] **`TodayWidgetReceiver`** — `GlanceAppWidgetReceiver` registered in AndroidManifest
- [x] **`res/xml/today_widget_info.xml`** — appwidget-provider (180×110dp, 30-min refresh, resizable)
- [x] **Glance 1.1.1** dependency added to `libs.versions.toml` and `build.gradle.kts`

### Version → v7.0
- [x] `build.gradle.kts`: `versionCode = 5`, `versionName = "7.0"`
- [x] `SettingsScreen` version subtitle: "Neuromind v7.0"
- [x] `PROGRESS.md` and `CLAUDE.md` updated

### Tests
- [x] **`FocusStatsTest`** — 6 new tests: empty-list guard, weekly total + session count, out-of-window exclusion, 7-entry minutesByDay, best-day label, no-best-day when all zeros

**Total: 63 unit tests — all passing** (was 57; +6 new)

---

## What's Done (v5.1 — Branding & Theme)

### Branding & Visual Identity
- [x] **Custom brain+node launcher icon** — adaptive icon replaces default Android robot; diagonal blue→violet gradient background, white neural-network brain mark with WarmOrange accent node, all within the 66dp safe zone
- [x] **In-app logo asset** (`drawable/ic_neuromind_logo.xml`) — same brain+node mark for use at any Compose size
- [x] **Animated splash screen** (`ui/splash/SplashScreen.kt`) — full-screen brand gradient, logo bouncy scale-in + fade, "Neuromind" + tagline fade-up; ~1.9s total; gated with `rememberSaveable` so it plays once per cold launch, not on rotation
- [x] **Anti-white-flash** — `windowBackground` set to brand blue in XML theme + `colors.xml` so the system window is colored before Compose draws
- [x] **Bottom-nav label fix** — renamed "Dashboard" → "Home" (prevents last-letter clip at 5-tab width); all labels rendered with `maxLines=1, softWrap=false`

### Theme Overhaul (Vibrant & Playful)
- [x] **`Color.kt`** — fully expanded: vivid blue primary, punchy orange secondary, bright teal tertiary; complete light+dark container/on-container tokens; `SurfaceVariant`, `Outline`, `Error` container pairs; `GradientStart`/`GradientEnd` constants
- [x] **`Theme.kt`** — complete `lightColorScheme` + `darkColorScheme` with all M3 tokens; wires in `AppShapes` and the full Typography scale
- [x] **`Type.kt`** — full M3 scale (display → label) with ExtraBold/Bold headlines, SemiBold titles, Medium labels; drop-in custom font ready
- [x] **`Shape.kt`** — playful rounded radii: small=12dp, medium=18dp, large=28dp, extraLarge=36dp; propagates to all Card/Surface/TextField automatically
- [x] **Dashboard gradient header** — `TopAppBar` wrapped in `Box` with brand blue→violet `linearGradient`; title+date text in white

### Extras & UX Polish
- [x] **Smarter task rebalancer** (`TaskRebalancer`) — sorts overdue tasks by priority desc + difficulty desc before slot assignment; HIGH/HARD tasks prefer slots within the user's peak-focus window (`CognitiveProfile.peakStart..peakEnd`); falls back to first-fit; `DashboardViewModel` now passes `profile`
- [x] **Clearer Quick Add** — added `AutoAwesome` leading icon + helper label "Natural language — try 'Essay due Friday 3pm'" above the field; purpose is immediately obvious
- [x] **Styled Vico chart** — `WeeklyTrendCard` now shows styled primary-colored columns (16dp width) with a `rememberStartAxis()` Y-axis and `rememberBottomAxis()` with W1–W4 labels via `AxisValueFormatter`; manual label Row removed

### Tests
- [x] 2 new `TaskRebalancerTest` cases: priority-ordering assertion + HARD task peak-hour preference

**Total: 43 unit tests — all passing**

---

## What's Done (v6.0 — Assistant, Polish & Tooling)

### Ask Neuromind — Expanded (Pillar 15)
- [x] **6 new intents**: `CREATE_TASK`, `COMPLETE_TASK`, `STATS_QUERY`, `FREE_TIME_QUERY`, `SUGGESTION_QUERY`, `GREETING`
- [x] **Create task from chat** — "add task essay due Friday 3pm" → `SmartInputHelper` parses → `repository.insertTask` → confirms in chat
- [x] **Complete task from chat** — "mark essay done" → fuzzy title match; single match → `updateTask(isCompleted=true)`; multiple matches → asks to clarify
- [x] **Stats query** — "how am I doing?" → reuses `RetroAnalyzer`; reports on-track %, best day, avg mood/energy; navigates to Insights
- [x] **Free time query** — "when am I free?" → reuses `TimeFinder`; lists next 3 free 1-hr slots; navigates to Timetable
- [x] **Suggestion query** — "what should I do next?" → reuses `SuggestionEngine`; falls back to top-priority task
- [x] **Greeting** — "hi/hello/thanks" → friendly canned response (4 variants)
- [x] **`NAVIGATE_INSIGHTS` action** wired in `AssistantScreen` + `NeuromindApp`
- [x] Updated empty-state suggestion chips (6 chips incl. add-task and stats prompts)
- [x] Updated `helpResponse()` listing all capabilities

### DND Permission
- [x] `ACCESS_NOTIFICATION_POLICY` declared in `AndroidManifest.xml`
- [x] "Grant DND Access" button in `FocusModeScreen` (replaces static text) — deep-links to `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`

### Insights Chart Fix
- [x] **Weekly "Tasks Completed" chart** split into two stacked regions: fixed-height bars row (120dp, bottom-aligned) + always-visible labels row below — day labels (Mon, Tue…) can no longer be clipped by tall bars
- [x] **Animated bars** — `animateFloatAsState` with `DampingRatioMediumBouncy` spring for smooth entry

### Developer Mode Additions
- [x] **Seed 14-Day Feedback** — inserts 14 varied `FeedbackLog` rows (mood, energy, tasks done, comments) spread over the past 14 days; unlocks Insights retro cards and burnout detection immediately
- [x] **Clear Feedback Logs** — new `@Query("DELETE FROM feedback_logs")` in `FeedbackLogDao`; no schema change
- [x] **App & DB Info dialog** — shows versionName (6.0), DB version (8), live row counts from all three tables

### Version Standardisation → v6.0
- [x] `build.gradle.kts`: `versionCode = 4`, `versionName = "6.0"`
- [x] `SettingsScreen` version subtitle: "Neuromind v6.0"
- [x] `PROGRESS.md` and `CLAUDE.md` updated

### Known-Issue Resolutions
- [x] Focus Mode route reachable from TaskCard (was already wired in working tree — confirmed)
- [x] `window.statusBarColor` deprecation resolved via `enableEdgeToEdge()` (was already fixed — confirmed)
- [x] `isBlocked` uses full task list for blocking check (was already fixed — confirmed)
- [x] Feedback submit spinner (dead code) — removed; button shows "Submit Review" only

### Tests
- [x] `NeuromindAssistantTest` — 14 new tests covering all new intents, `extractTaskTarget` helper, stats/free-time/suggestion responses
- [x] `SuggestionEngineTest` — fixed 2 pre-existing flaky tests by using a late `offPeakTime` (21:00) so `TimeFinder` deterministically finds no today-slots; `TimeFinder.findSlots` now accepts `referenceDate`/`referenceTime` params

**Total: 57 unit tests — all passing**

---

## What's Done (v5.0)

### Core Infrastructure
- [x] MVVM architecture with Room + DataStore + Kotlin Flows
- [x] Integer primary keys on all entities (migrated from UUID — fixes type-mismatch crashes)
- [x] Database version 8, **`fallbackToDestructiveMigration` removed** — any future schema change must supply a proper `Migration` via `.addMigrations(...)`
- [x] Central `TaskRepository` as single source of truth
- [x] Theme persistence (Light / Dark / System) via DataStore
- [x] Edge-to-edge UI with transparent status bar
- [x] Bottom navigation bar with **5 tabs** (Dashboard, Tasks, Ask, Insights, Settings)
- [x] App-wide compiler warnings cleaned (unchecked casts, deprecated statusBarColor)
- [x] Proguard rules for Room, DataStore, and data models
- [x] Runtime `POST_NOTIFICATIONS` permission request on Android 13+ (API 33)

### Dashboard
- [x] Dynamic greeting (Morning / Afternoon / Evening)
- [x] Pending + Completed task count cards
- [x] "Today's Priorities" — surfaces overdue + HIGH priority tasks
- [x] "Today's Timetable" — next 2 upcoming events for today
- [x] AI-Generated Plan — Scheduler slots tasks into free timetable gaps
- [x] **Burnout Warning card (Pillar 9 + 12)** — real sliding-window analysis of FeedbackLog; triggers on 3+ consecutive low-energy days or repeated weekday stress
- [x] **Rebalance card (Pillar 14)** — shown when ≥ 3 overdue tasks; "Rebalance" button opens a dialog listing each task's proposed new date/time; Confirm bulk-updates due dates; Dismiss hides until next session
- [x] **Suggestion card (Pillar 11)** — context-aware nudge (peak-hour, free-slot, energy-match, overdue-alert); shown only when no burnout/rebalance card is active; taps can navigate to the suggested task

### Task Management
- [x] Task List with 5 filter chips: All, Today, Overdue, Upcoming, Completed
- [x] Priority badges (HIGH/MED/LOW) with color coding on task cards
- [x] Interactive checkboxes (mark complete, persists immediately)
- [x] Tap task card to edit
- [x] Add/Edit Task screen with full field set: title, description, priority, difficulty, duration, due date
- [x] Reschedule mode (triggered from Dashboard, surfaces overdue tasks)
- [x] Delete task (trash icon on card)
- [x] **Task Dependencies (Pillar 2)**:
    - Set prerequisite task in Add/Edit Task screen
    - Blocked tasks (incomplete prerequisite) are visually greyed out with a lock icon
    - Checkbox and swipe actions are disabled for blocked tasks
- [x] **Study Plan Generator (Pillar 5)**:
    - "Break this down" button in AddEditTask (enabled when title is set)
    - Detects chapter patterns ("chapters 4, 5, 8") → one sub-task per chapter
    - Falls back to 3-phase split (Plan, Execute, Review) for general tasks
    - Preview dialog shows proposed sub-tasks with durations before confirming
- [x] **Find a Time (Pillar 7)**:
    - "Find a Time" button on AddEditTask date row
    - Scans timetable for next 5 free slots across the next 7 days
    - Selecting a slot populates dueDate automatically

### Focus Mode (Pillar 3)
- [x] Real countdown timer with Start / Pause / Resume / Reset controls
- [x] Circular progress ring around timer
- [x] Session-complete dialog with "Another round" option
- [x] Do Not Disturb integration (priority filter during focus session)
- [x] Sound and vibration alerts on session end
- [x] Adjustable session length (+/- 5 min buttons) before starting
- [x] Accessible via "Focus" timer icon on task cards

### NLP (Pillar 4)
- [x] `SmartInputHelper` class with regex parsing
- [x] Handles "tomorrow", "next [weekday]", "[weekday]", "in X days"
- [x] Handles times like "at 5pm", "at 14:30"
- [x] **Quick Add field** at the top of Task List for fast entry using NLP

### Timetable (Pillar 10)
- [x] Agenda view (replaces broken grid view) — orders days starting from today
- [x] Shows recurring (weekly) and one-time events correctly
- [x] Add/Edit entry dialog with title, venue, details, day picker, time pickers, recurring toggle
- [x] Edit by tapping a card; delete icon on each card
- [x] "TODAY" badge on current day header
- [x] **Smooth scroll** to the first upcoming event of the day on open
- [x] **Category color coding** — entries auto-categorised (ACADEMIC / FITNESS / SOCIAL / PERSONAL) by title keywords; each category gets a distinct container + accent color

### Feedback / End-of-Day Review (Pillar 9)
- [x] Mood selection (STRESSED → GREAT) with filter chips
- [x] Energy level slider (1–5)
- [x] Tasks completed input
- [x] Additional thoughts text field
- [x] Submits to Room `FeedbackLog` table with per-entry timestamp

### Insights (Pillar 13 — Retrospective Insight Engine)
- [x] Weekly completion bar chart
- [x] Wellness score progress bar (mood/5 + energy/5 correctly weighted)
- [x] Avg Mood / Avg Energy (1–5 scale)
- [x] **Recent Journal Entries** — surfaces non-blank feedback comments, newest first
- [x] **"Your Streak" card** — Vico bar chart showing 4-week completion % trend
- [x] **"Best Day" card** — top weekday over last 30 days (tie → earlier day of week)
- [x] **"On-Track Rate" card** — circular progress + coaching tip (<50%: suggest smaller tasks; >80%: positive reinforcement)
- [x] **Mood × Productivity table** — avg tasks completed per mood level (emoji + number)
- [x] All retrospective cards hidden when fewer than 7 days of data exist

### AI Assistant (Pillar 15)
- [x] 5th "Ask" tab in bottom nav with `SmartToy` icon
- [x] Chat interface — user bubbles right-aligned (primary color), assistant bubbles left-aligned (surfaceVariant)
- [x] Example prompt chips on empty state: "What's on today?", "I'm tired", "Start a focus session", "What's overdue?"
- [x] `NeuromindAssistant` intent classifier (keyword/regex): SCHEDULE_QUERY, TASK_QUERY, MOOD_RESPONSE, FOCUS_REQUEST, HELP, UNKNOWN
- [x] SCHEDULE_QUERY → lists today's timetable in time order (or "no classes today")
- [x] TASK_QUERY → overdue count + oldest task info; navigates to task list
- [x] MOOD_RESPONSE → suggests lowest-difficulty pending task
- [x] FOCUS_REQUEST → navigates to Focus Mode with highest-priority task
- [x] HELP → fixed list of example questions
- [x] Messages in-memory only (not persisted to Room)

### Context-Aware Suggestions (Pillar 11)
- [x] `SuggestionEngine` — pure function; first-match priority: PEAK_HOUR_NUDGE → FREE_SLOT_NUDGE → ENERGY_MATCH → OVERDUE_ALERT
- [x] `SuggestionWorker` — daily `PeriodicWorkRequest` aligned to peak start hour; re-scheduled when user updates peak hours in Settings
- [x] Dashboard Suggestion card with optional task navigation action

### Scheduled Task Rebalancing (Pillar 14)
- [x] `TaskRebalancer` — pure function; uses `TimeFinder.findSlots()` per overdue task; no slot reuse across proposals
- [x] Dashboard Rebalance card + `RebalanceDialog` with confirm/dismiss
- [x] `DashboardViewModel` confirm bulk-updates task due dates; dismiss hides until session restart

### Cognitive Profile (Pillar 1)
- [x] DataStore-persisted profile: peak focus hours, preferred session length, task style
- [x] "Cognitive Profile" section in Settings with three dialogs (peak hours ±1hr picker, session length radio, task style radio)
- [x] `Scheduler` now places highest-priority / hardest tasks at the peak start hour
- [x] Task ordering: priority desc then difficulty desc (HARD > MEDIUM > EASY as tiebreak)

### Emotional Pattern Detection (Pillar 12)
- [x] `BurnoutAnalyzer` — pure function over last 7 days of FeedbackLog
- [x] Trigger A: 3+ consecutive days with avg energy ≤ 2 → "Schedule a rest day?" card
- [x] Trigger B: 2+ STRESSED/TIRED entries on the same weekday → lighter-schedule suggestion
- [x] Dashboard Burnout Warning card driven by real data

### Settings
- [x] Theme picker dialog (Light / Dark / System)
- [x] **Cognitive Profile** section (peak hours, session length, task style)
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
- [x] Daily context-aware suggestion notification via `SuggestionWorker`

### Tests
- [x] `SmartInputHelperTest` — 5 NLP parsing tests
- [x] `BurnoutAnalyzerTest` — 9 tests covering both burnout triggers and edge cases
- [x] `SchedulerTest` — 7 tests covering peak-hour placement, priority+difficulty ordering, conflict avoidance
- [x] `RetroAnalyzerTest` — 4 tests (trend percentages, best-day tie, on-track rate, null guard)
- [x] `TaskRebalancerTest` — 4 tests (one-proposal-per-task, no slot reuse, full-day block exclusion, defensive empty)
- [x] `SuggestionEngineTest` — 5 tests (peak nudge, nudge-off-when-done, energy match, overdue alert, null when no tasks)
- [x] `NeuromindAssistantTest` — 6 tests (all intent classifications, UNKNOWN fallback, schedule order, overdue count, mood/difficulty, focus task id)

**Total: 41 unit tests — all passing**

---

## Known Issues & Technical Debt

All previously tracked issues resolved as of v6.0. No open technical debt.
