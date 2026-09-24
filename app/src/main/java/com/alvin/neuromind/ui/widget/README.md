# Neuromind Widget Suite

This module now provides multiple Glance widgets:

- `TodayWidget` (priorities)
- `QuickActionsWidget`
- `FocusShortcutsWidget`
- `NextClassWidget`
- `DailyProgressWidget`
- `StreakWellbeingWidget`
- `SmartSuggestionWidget`
- `HeatmapWidget`
- `AcademicWidget`
- `MotivatorWidget`
- `MiniQuickLogWidget`
- `MiniStatusWidget`
- `MiniNextWidget`
- `ConfigurableStackWidget`
- `RotatingStack15Widget`
- `RotatingStack30Widget`
- `RotatingStack60Widget`

## Quick log behavior

- `MiniQuickLogWidget` is a dedicated 1x1 entry point for voice quick logging.
- Tap action launches `QuickLogEntryActivity` (permission check -> speech recognizer -> transcript handoff to Feedback).
- Provider metadata supports `home_screen|keyguard` where launcher/OS permits it.

## Rotation behavior

The stack widgets rotate content based on wall-clock slots:

- 15 min stack: updates every 15 minutes
- 30 min stack: updates every 30 minutes
- 60 min stack: updates every 60 minutes

## Refresh behavior

All widgets refresh when:

- Tasks are added/updated/deleted
- Timetable entries are added/updated/deleted
- Data import completes
- Date/time/timezone changes are received

## Notes

- Quick Settings tile support is separate from widgets (`ui/tile/QuickLogTileService`).
- Some launchers may cache previews briefly after install/update.

