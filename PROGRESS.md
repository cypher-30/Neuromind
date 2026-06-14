# Neuromind — Progress Tracker

**Current stable version:** v4.0 (Phase 1 complete)  
**Next target:** v4.5 (Phase 2 — Advanced Task & Project Handling)  
**Last updated:** 2026-06-14

---

## What's Done (v4.0)

### Core Infrastructure
- [x] MVVM architecture with Room + DataStore + Kotlin Flows
- [x] Integer primary keys on all entities (migrated from UUID — fixes type-mismatch crashes)
- [x] Database version 8, destructive migration with demo data re-seed
- [x] Central `TaskRepository` as single source of truth
- [x] Theme persistence (Light / Dark / System) via DataStore
- [x] Edge-to-edge UI with transparent status bar
- [x] Bottom navigation bar with 4 tabs
- [x] App-wide compiler warnings cleaned (unchecked casts, deprecated statusBarColor)
- [x] Proguard rules for Room, DataStore, and data models

### Dashboard
- [x] Dynamic greeting (Morning / Afternoon / Evening)
- [x] Pending + Completed task count cards
- [x] "Today's Priorities" — surfaces overdue + HIGH priority tasks
- [x] "Today's Timetable" — next 2 upcoming events for today
- [x] AI-Generated Plan — Scheduler slots tasks into free timetable gaps
- [x] **Burnout Warning card (Pillar 9 + 12)** — real sliding-window analysis of FeedbackLog; triggers on 3+ consecutive low-energy days or repeated weekday stress

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

### Insights
- [x] Weekly completion bar chart
- [x] Wellness score progress bar (mood/5 + energy/5 correctly weighted)
- [x] Avg Mood / Avg Energy (1–5 scale, fixed from erroneous /10 display)
- [x] **Recent Journal Entries** — surfaces non-blank feedback comments, newest first

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

### Tests
- [x] `SmartInputHelperTest` — 5 NLP parsing tests
- [x] `BurnoutAnalyzerTest` — 9 tests covering both burnout triggers and edge cases
- [x] `SchedulerTest` — 7 tests covering peak-hour placement, priority+difficulty ordering, conflict avoidance

---

## Known Issues & Technical Debt

| Issue | Severity | Notes |
|---|---|---|
| Feedback submit spinner never visible | Low | `isSubmitting` is set then screen navigates away immediately; spinner is dead code |
| `window.statusBarColor` deprecated (API 35+) | Low | Suppressed with `@Suppress("DEPRECATION")` — needs migration to `enableEdgeToEdge()` in MainActivity |
| `isBlocked` checks visible list only | Low | Incomplete prerequisite hidden by an active filter won't block the dependent task |
| Database fallbackToDestructiveMigration | Low | Acceptable in dev; must switch to proper migrations before release |

---

## Planned — Phase 3: UX & Focus Polish (v5.0)

### Pillar 10 — Timetable Polish (remaining)
- [ ] Import timetable from photo using ML Kit OCR (long-term)
