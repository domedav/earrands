# Ballanceometer Remake — Master Plan

## 1. Goal
Transform `com.domedav.minitasklist` (Mini Task List) into `com.domedav.ballanceometer` — a "money unlocking" habit app with 2 screens + widget, Material 3 Expressive theme reuse.

## 2. Concept (Earrands)
- User sets `totalMonthlyBalance` and `minimalMonthlySpend` (floor).
- `earnable = max(0, total - minimal)` is locked until earned via tasks.
- User creates TaskGroups with weight 0-100. Weights normalized.
- Each group has Subtasks with recurrence: ONCE, DAILY, WEEKLY, MONTHLY.
- App computes monthly instance count per subtask, distributes groupShare equally among instances.
- Completing a subtask instance unlocks its value. Spending subtracts from available.

## 3. Math Spec (BalanceEngine)
```
weightSum = sum(group.weight)
groupShare = if weightSum==0 then 0 else earnable * (group.weight / weightSum)
monthlyInstances(subtask):
  ONCE -> 1
  DAILY -> daysInMonth
  WEEKLY -> ceil(daysInMonth/7) ~ 4 or exact weeks
  MONTHLY -> 1
totalInstancesInGroup = sum(monthlyInstances)
valuePerInstance(subtask) = groupShare / totalInstancesInGroup   (equal split within group)
unlocked = sum(completedInstances * valuePerInstance)
available = minimal + unlocked - totalSpent
remainingLocked = earnable - unlocked
```
Edge: weight 0 groups give 0 value. If no groups, available = minimal - spent (or 0).
Completion is per-day deduplicated: one completion per subtask per date.

## 4. Data Model (Room v2)
- `AppConfig(id=1, totalBalance: Double, minimalSpend: Double, currency: String="HUF")`
- `TaskGroup(id:String, name:String, weight:Int 0..100, createdAt:Long)`
- `Subtask(id:String, groupId:String FK CASCADE, title:String, recurrence:String, createdAt:Long, isOneTime:Boolean?)`
  - Recurrence enum: ONCE, DAILY, WEEKLY, MONTHLY stored as String
- `Completion(id:String, subtaskId:String FK CASCADE, date:LocalDate String yyyy-MM-dd, completedAt:Long, valueUnlocked:Double)`
- `Spending(id:String, amount:Double, note:String, timestamp:Long)`
- TypeConverters for enum/date.
- DAOs: AppConfigDao, TaskGroupDao, SubtaskDao, CompletionDao, SpendingDao
- Repository aggregates, BalanceEngine pure kotlin object.

## 5. App Structure (keep theme & simplicity)
- Package rename: `com.domedav.ballanceometer`
- Keep: `ui/theme/Color.kt, Theme.kt(rename -> BallanceometerTheme), Type.kt, GlanceTheme.kt`, `MinitasklistApp -> BallanceometerApp`
- New: `data/` entities above, `domain/BalanceEngine.kt`, `data/BallanceometerRepository.kt`
- UI: `MainActivity` with bottom nav / TabRow: Show | Config
  - `ui/show/ShowScreen.kt` + `ShowViewModel.kt`: balance meter (circular progress), available/unlocked/spent cards, today todos lazy list with checkbox, spend FAB/dialog
  - `ui/config/ConfigScreen.kt` + `ConfigViewModel.kt`: top cards for total/minimal with edit, groups list with weight slider 0-100, expand to subtasks, add/edit dialogs
  - `ui/components/BalanceMeter.kt`
- Widget: `widget/BallanceWidget.kt`, `BallanceWidgetReceiver.kt`, `widget/ToggleSubtaskAction.kt`, `widget/AddSpendingAction.kt` -> shows available + progress + today todos (max 5) with check action
- Navigation: simple `var selectedTab by rememberSaveable {0}` no nav-component to keep straightforward

## 6. Material You Design
- Reuse Expressive palette, use `primary` for unlocked, `tertiary` for available, `error` for spent, `surfaceVariant` cards.
- Show: large balance number, LinearProgressIndicator for unlock progress, cards with rounded 28dp.
- Config: sliders with weight badge, group cards elevated.

## 7. Execution Checklist (Atomic)
1. Rename package & app identity (namespace, applicationId, app name, strings, manifest, gradle)
2. Create data layer: enums, entities, DAOs, Database v2, converters
3. Create domain/BalanceEngine.kt + Repository + unit tests
4. Create shared ViewModels (AppConfig, Repository injection)
5. Build Show screen + ViewModel + BalanceMeter
6. Build Config screen + ViewModel + dialogs/sliders
7. Rewire MainActivity with 2-tab navigation
8. Rebuild Widget for balance + todos
9. Update theme/app class, strings, icons
10. Build verify `./gradlew :app:assembleDebug` + tests

## 8. Subagent Strategy
- Delegate data layer, show screen, config screen, widget as parallel subagents after step 1.

