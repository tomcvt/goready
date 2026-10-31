# GoReady → Flutter host: alarm/routine porting audit

Audit only. No code was modified. Not inspected: `BleDeviceManager` beyond line 200, `TargetMinigame`, `RoutineLauncherView`, `SettingsView`, `Modals`, `DaysRow`, `domain/*Draft`, `AppPrefsRepository`, `SeedLoader/Parser`, other DAOs, `ads/*`, `premium/*`, tests. The project was not built.

## 0. Assumptions and scope

| # | Assumption |
|---|---|
| A1 | No user data worth migrating. Room uses `fallbackToDestructiveMigration(true)` (`data/AlarmDatabase.kt:34`). |
| A2 | The BLE/ESP companion (`ble/*`, `espEnabled`, `syncAllEspEnabledAlarms`) is out of scope. See §3. |
| A3 | Ads (`play-services-ads`, `BottomBarBannerAdView`, `MobileAds.initialize`) are dropped from native Activities. |
| A4 | Native Activities stay Jetpack Compose (CameraX + ML Kit stay native, UI exists). Not ported to Views. |
| A5 | "Ordered tasks" does not exist today: one task per alarm (`task` + `taskData`). `TASK_CHAIN` is an unimplemented premium stub. An `alarm_tasks` table is proposed (one row now, chains later). |
| A6 | Routine "steps" are timed activities (e.g. brush teeth, 5 min), not dismissal tasks. |
| A7 | `TIMER` task is unimplemented; it falls back to `SimpleAlarmScreen` (`ui/composables/AlarmScreen.kt:122-130`). |

## 1. Inventory and classification

Paths are under `app/src/main/java/com/tomcvt/goready/` unless noted. Classes: COPY, ADAPT, REWRITE (Flutter), DROP.

### 1a. Manifest, resources, Gradle

| File | Role | Class | Required changes |
|---|---|---|---|
| `app/src/main/AndroidManifest.xml` | Permissions, 2 Activities, 2 receivers, 1 service | ADAPT | See §6. Drop `.application.AlarmApp` and `MainActivity`. `AlarmReceiver`/`RoutineReceiver` are `exported="true"` (any app can trigger a ring): make `false`. Split boot into its own receiver. Drop AdMob meta-data and BLE permissions. Remove `tools:replace` on backup attributes (conflicts with host). |
| `res/values/themes.xml` | `Theme.App.Fullscreen` (Material3 DayNight), `Theme.GoReady` | ADAPT | `Theme.Material3.*` needs `com.google.android.material`, not declared in `gradle/libs.versions.toml` (probably transitive; unverified). Add explicitly or use `android:Theme.Material.*`. |
| `res/values/strings.xml` | `attribution_alarm`, `attribution_empty`, `empty` | COPY | Needed by `<attribution>` tags and `createAttributionContext("alarm")`. |
| `res/drawable/ic_alarm.xml`, `ic_gicon_mono.xml` | Notification small icons | COPY | Rename to avoid host clashes. |
| `res/xml/backup_rules.xml`, `data_extraction_rules.xml` | Backup config | DROP | Host owns backup policy; decide whether shared DB is backed up. |
| `app/build.gradle.kts`, `gradle/libs.versions.toml` | Build | ADAPT | See §6d. |
| `app/src/main/assets/webview_games/**` | Phaser JS games | COPY | Move to native module `assets/`. `.py` bundlers, `*_old.html`, `types/*.d.ts`, `tsconfig.json` are not runtime. |
| `app/src/main/assets/seeds/steps/*.json` | Step-definition seeds | REWRITE (Flutter) | Flutter assets, seed via Drift. |
| `app/src/proto/routine_session.proto`, `data/RoutineSessionKeys.kt`, `repository/RoutineSessionDataRepository.kt`, `data/AppPrefs.kt` | DataStore/protobuf session state | DROP | No use in the live path (`RoutineSessionDataRepository(` only defined). Removes protobuf plugin + datastore. |

### 1b. Alarm and routine runtime chain

| File | Role | Class | Required changes |
|---|---|---|---|
| `manager/AlarmReceiver.kt` | Handles `BOOT_COMPLETED`, `ACTION_ALARM_TRIGGERED`; reschedules next; starts service | ADAPT | Remove `context.applicationContext as AlarmApp` / `app.appAlarmManager` (lines 32-35). Use lazy context-only `AlarmRuntimeDb` + `AlarmScheduleCoordinator`. Split boot into `BootReceiver`. Handle `ACTION_ALARM_SNOOZE_TRIGGERED` explicitly. Start service before any DB work. Delete unused `showNotification`. |
| `manager/SystemAlarmScheduler.kt`, `manager/AlarmScheduler.kt` | `setAlarmClock` scheduling, snooze, cancel | ADAPT | Signatures from `AlarmEntity` → `(alarmId, triggerAtMs, remainingSnooze)`. One-time trigger calc moves to calculator. `cancelAlarm` must also cancel the snooze PendingIntent (C-3). Remove `scheduleRTCWakeup`'s `context.startActivity(ACTION_REQUEST_SCHEDULE_EXACT_ALARM)` (Settings from non-Activity context). Remove `scheduleTestAlarm`. `showIntent` targets `MainActivity`: make it a configurable host launch intent. |
| `time/RepeatAlarmCalculator.kt` | Next-fire computation | ADAPT | Take `(hour, minute, repeatMask)` instead of `AlarmEntity`; bitmask instead of `Set<DayOfWeek>`. Port to Dart with shared golden vectors. |
| `time/TimeProvider.kt`, `RealTimeProvider.kt` | Clock | COPY | |
| `manager/AppAlarmManager.kt`, `AppAlarmManagerImpl.kt` | CRUD, toggle, schedule, ESP sync | SPLIT | CRUD/validation/toggle/ESP sync: REWRITE (Flutter). `scheduleNextAlarmOrDisable`, `scheduleAllEnabledAlarms`: ADAPT into native `AlarmScheduleCoordinator`. The "disable one-time alarm" write (`:151`) is forbidden for native → event + view-derived flag (§4d). |
| `service/AlarmForegroundService.kt` | Ring: audio, FG notification, full-screen intent, mute/snooze/stop | ADAPT (heavy) | Remove `AlarmDatabase`, `AlarmRepositoryImpl`, `AlarmApp`, `ble` and every `ble.*` call (lines 47,69-70,89,104,135,150,203,225,299). Load via `AlarmRuntimeDb`. Call `startForeground` immediately, then load DB and start audio (C-5). Persist an active-ring record (alarmId, remainingSnooze, startedAt, routineId) so a `START_STICKY` null-intent restart resumes instead of `stopSelf()` at line 160 (C-4). Merge duplicate `STOP_ALARM_SERVICE`/`STOP_ALARM_SOUND` branches (84-112). Replace `currentAlarm!!` (line 124). Drop dead `alarmActivityNotifyPendingIntent`. |
| `activities/AlarmActivity.kt` | Ring UI host: lock flags, back blocked, snooze, routine hand-off | ADAPT | Remove `app.bleDeviceManager/alarmRepository/appAlarmManager/routineRepository`. Load through `AlarmRuntimeDb` off main thread; call `setContent` before load. Snooze calls native scheduler directly. Remove `TestAlarm` extras (→ Pigeon `previewAlarm`). Remove `serviceMessages` + `QueuedOverlay` toasts (BLE only). `onBackPressed` override is redundant with the callback. Rework `onStop` → `ACTION_UI_HIDDEN` (§7). |
| `ui/composables/AlarmScreen.kt` | `AlarmScreen`, `SimpleAlarmScreen`, Text/Countdown/Math/Target/Game/Barcode screens, `InteractionDetectorWrapper` | ADAPT | Delete `AlarmEntity` overload (43-69). Delete `Debug*` variants, `dismissable`, `BuildConfig` use, `MultiBarcodesSaverView` import. Typed payloads instead of `taskData: String?` (§4f). Drop per-recomposition `GamesRegistry()`. Make `passedGate`/math list/barcode index saveable (C-9). |
| `ui/composables/TargetMinigame.kt`, `SimpleAnimations.kt`, `Buttons.kt` (`FlexCloseButton`) | Task UI widgets | ADAPT | Not inspected; copy only what `AlarmScreen`/routine flow use transitively. |
| `ui/theme/*` | `VibrantLightTheme`, `VibrantTheme` | COPY | `StepType` colours depend on `util/ColorUtils.contrastText`; split. |
| `ui/overlay/*` | `QueuedOverlay` toasts | DROP | Only used for BLE messages in `AlarmActivity`. |
| `scanner/ScannerComposables.kt` | Camera + ML Kit + scan-set editor | SPLIT | `BarcodeScannerView`, `MultiBarcodeScannerView`, `CameraPermissionWrapper`, `ScanFeedback`: COPY to native. `SaveBarcodeScreen`, `MultiBarcodesSaverView`, `TipInputDialog`, `BarcodeValueInputDialog`, `ScanCodeItemCard`, `AddBarcodeCard`, `ScanSetToolbarRow`, `DebugScanView`: REWRITE (Flutter). Editor-side scanning: `mobile_scanner` or Pigeon `scanBarcodeOnce()`. |
| `scanner/ScanCode.kt`, `scanner/Converters.kt` | `"barcode:tip,barcode:tip"` encoding | ADAPT | Breaks if a tip contains `,` or `:` (`split(":")` destructures exactly 2). Replace with JSON (§4f). |
| `games/GameBridge.kt`, `GameAlarmScreen.kt`, `GamesRegistry.kt`, `GameEntry.kt` | WebView games + JS bridge | COPY | `GamesRegistry` → constants/`object`. Uses `androidx.webkit.WebViewAssetLoader` without an explicit dependency (unverified transitive): add `androidx.webkit:webkit`. |
| `activities/RoutineFlowActivity.kt` | Routine UI host | ADAPT (heavy) | Remove `AlarmApp`, `RoutineRepository(appObject.db...)`, ads. Replace `viewModel(factory=...)` with Activity-scoped state holder over `AlarmRuntimeDb` + event writer. Remove `setContent` inside `onNewIntent`, and `vm.set*` calls in composition (run every recomposition). `onNewIntent(Intent, ComponentCaller)` is an API 34 override, never delivered on API 24-33. |
| `ui/composables/RoutineFlowView.kt`, `RoutineLauncherView.kt` | Routine UI | ADAPT | Remove `BottomBarBannerAdView` (RoutineFlowView:84). Swap `RoutineFlowViewModel` for state holder. Launcher not inspected. |
| `viewmodel/RoutineFlowViewModel.kt` | Room Flows → UI state | ADAPT | Keep derivation (`currentStepFinishTime = length*MINUTE + stepStartTime`). Replace Room Flows with in-memory session + event writes. An Activity-scoped ViewModel is fine; the "no ViewModel" rule is for receiver/service. |
| `manager/RoutineFlowManager.kt` | Session state machine, notifications, step timeout | ADAPT (heavy) | Remove 4 repositories. Replace `routineSessionRepository.update*` with events (§4g). Replace `.first()` Flow reads with reader calls. Keep notification building. `clearRunningRoutines` must also cancel the previous session's timeout/notification. |
| `manager/RoutineReceiver.kt` | `ACTION_STEP_TIMEOUT`, `ACTION_FF_TO_NEXT_STEP`, `ACTION_ROUTINE_COMPLETE` | ADAPT | Remove `AlarmApp`; build manager from context. Delete `ACTION_START_ROUTINE` (no sender) and empty `ACTION_ROUTINE_ABANDONED` branch. Add `goAsync()` (it launches unbounded coroutines and returns). Remove `BOOT_COMPLETED` filter or implement active-session restore. |
| `manager/RoutineScheduler.kt` | Step-timeout / abandoned scheduling | ADAPT | **Bug:** `scheduleRTCWakeup` has no `else` for SDK < S (lines 82-97): on API 24-30 step timeouts are never scheduled. Cancel must also cover the `ACTION_ROUTINE_ABANDONED` PendingIntent. |
| `constants/Constants.kt` | Intent constants, `TaskType`, `MathType`, `StepType`, `SNOOZE_*` | SPLIT | `EXTRA_*`/`ACTION_*`: COPY. `TaskType`: ADAPT (stable string key; drop `label`/`premium`). `MathType`: ADAPT (generator is ring-time). `StepType`: REWRITE (Compose `Color`). `SNOOZE_MINUTES/COUNTS`: validate in Flutter, clamp in native. |
| `constants/TaskTypeContext.kt` | Unused class | DROP | Never referenced. |
| `data/StepStatus.kt`, `RoutineStatus.kt` | Enums | ADAPT | Native keeps for event/state naming. |
| `data/StepWithDefinition.kt` | Join DTO | ADAPT | Becomes `RoutineStepRuntime` from `routine_runtime` view. |
| `data/AlarmEntity.kt`, `RoutineEntity.kt`, `RoutineStepEntity.kt`, `StepDefinitionEntity.kt`, `RoutineSession.kt`, `ScanSetEntity.kt`, `*Dao.kt`, `AlarmDatabase.kt`, `Converters.kt`, `app/schemas/**` | Room | REWRITE (Drift) | §4c. |
| `data/seeding/*`, `repository/*`, `domain/*`, `OpResult`, `PermissionSpec` | Persistence/business layer | REWRITE (Flutter) | |
| `viewmodel/AlarmViewModel.kt` (`validateData`, `trimTaskData`, `parseData`), `RoutinesViewModel`, `SettingsViewModel`, `*Factory` | Editing logic | REWRITE (Flutter) | `validateData` rules are the authoritative per-type payload rules (§4f). |
| `ui/composables/{Alarms,AddAlarmView,Routines,Inputs,MainTabs,AlarmsNavHost,SettingsView,Modals,DaysRow,SnapWheels,TestingPreview}.kt`, `ui/{DialogModal,AlertDialogModal}.kt`, `ui/utils/*`, `ui/ble/*`, `util/*`, `preview/*`, `MainActivity.kt` | Editing/management UI | REWRITE / DROP | `Alarms.kt:120-127` debug `sendBroadcast` → Pigeon `previewAlarm`/`triggerRingNow`. `MainActivity.kt:147-152` notification permission and battery prompts → Pigeon-driven. |
| `ble/*`, `ads/*`, `premium/*`, `registries/AppPermissionRegistry.kt`, `app/src/debug/.../SettingsDebugMenu.kt`, `test/AlarmActivityWrapper.kt` | Out of scope / host-owned | DROP | `premium/*` becomes Flutter state (premium gating is an edit-time rule). |
| `app/src/test/**` | Unit tests | SPLIT | `RepeatAlarmCalculatorTest` COPY, export cases as golden vectors for Dart. Manager/ViewModel tests REWRITE. |

## 2. Runtime chain trace

1. **Schedule.** `AppAlarmManagerImpl.createAlarm/updateAlarm/toggleAlarm` → `scheduleBasedOnRepeatDays` → `SystemAlarmScheduler.scheduleOneTimeAlarm|scheduleNextAlarm` → `AlarmManager.setAlarmClock(AlarmClockInfo(trigger, showPending), pi)` (`SystemAlarmScheduler.kt:234-244`).
   - PendingIntent: `getBroadcast(requestCode=alarmId.toInt(), AlarmReceiver, action=ACTION_ALARM_TRIGGERED, extras{EXTRA_ALARM_ID, EXTRA_REMAINING_SNOOZE}, UPDATE_CURRENT|IMMUTABLE)`.
   - `remainingSnooze` computed at schedule time: `snoozeEnabled ? snoozeMaxCount!! : 0`.
   - `showPending` → `MainActivity` with `EXTRA_ALARM_ID`.
2. **Fire → `AlarmReceiver.onReceive`.**
   - Reads `AlarmApp` via application context (**process singleton**).
   - For `ACTION_ALARM_TRIGGERED`: `goAsync()` + fire-and-forget coroutine `scheduleNextAlarmOrDisable(alarmId)`: reloads alarm, reschedules repeating; for one-time writes `isEnabled=false` to Room.
   - **Concurrently** `ContextCompat.startForegroundService(AlarmForegroundService, extras{alarmId, remainingSnooze})`.
   - `ACTION_ALARM_SNOOZE_TRIGGERED` and action-less broadcasts skip reschedule but still start the service.
3. **Service `onStartCommand`.**
   - Loads `AlarmEntity` via a Room repository created in `onCreate`. Stores `currentAlarm/currentAlarmId/currentSnooze` in **instance fields** (lost on process death).
   - Audio focus → BLE auto-connect + `requestStartAlarm` → `startAlarmSound` → `startAsForeground` (notif id 1, channel `alarm_channel`, full-screen intent) → 2 s un-mute poll loop.
   - Full-screen PendingIntent: `getActivity(requestCode=alarmId.toInt(), AlarmActivity, NEW_TASK|NO_HISTORY, extras{alarmId, remainingSnooze})`.
4. **AlarmActivity.**
   - Reads `EXTRA_ALARM_ID`, `EXTRA_REMAINING_SNOOZE`, `TestAlarm` (debug). Reloads alarm (`app.appAlarmManager.getAlarm`) and routine (`app.routineRepository.getRoutineById`).
   - **State only in lambdas / local vars in `onCreate`:** routine hand-off decision, debounce clock, test override.
   - Calls service with `ACTION_USER_INTERACTION` (mute 5 s, debounced 1 s) and `ACTION_UI_HIDDEN` (from `onStop`).
5. **Task completion → dismissal.**
   - `AlarmScreen`: swipe gate (`passedGate`, composition state) → task by `TaskType`. Task progress is composition state.
   - `onStopAlarm` without routine: `ACTION_STOP_ALARM_SERVICE` + `finish()`; service stops sound/BLE, delays 5 s, `finalizeAlarm()` → `stopSelf()`.
   - With routine: `launchRoutine(routineId, alarmId)` → `RoutineFlowActivity` (`ACTION_RF_UI_LAUNCHER`, `NEW_TASK`, extras `{routineId, alarmId}`), plus `ACTION_STOP_ALARM_SOUND` (identical to stop-service path), `finish()`.
   - Snooze: `scheduleSnoozeById` → `SystemAlarmScheduler.scheduleSnooze` (`setAlarmClock`, action `ACTION_ALARM_SNOOZE_TRIGGERED`, `remainingSnooze-1`, requestCode `alarmId.toInt()`), stop service, finish.
6. **Routine.**
   - User taps start → `RoutineFlowViewModel.launchRoutine` → `RoutineFlowManager.startRoutine`: marks RUNNING sessions CANCELED (DB only), inserts `RoutineSession`, schedules step timeout (request code `sessionId.toInt()`), posts ongoing status notification (id 1119).
   - First UI touch (`firstClicked`) → `ACTION_FINALIZE_ALARM` to service. Redundant after routine dismissal; if the service is gone, `startService` recreates it just to stop it.
   - Step timeout → `RoutineReceiver(ACTION_STEP_TIMEOUT)` → `manageStepFinishedTimeout` → HIGH notification with full-screen intent to `RoutineFlowActivity` (`ACTION_RF_UI_STEP_TIMEOUT`, `NEW_TASK|CLEAR_TOP`, extras `{sessionId, stepNumber}`).
   - Notification actions → `RoutineReceiver(ACTION_FF_TO_NEXT_STEP | ACTION_ROUTINE_COMPLETE)`.
   - Activity reads session/routine/steps via Room Flows through ViewModel; only `sessionId` passes by extra.
7. **Event persistence.** None. Rings, snoozes, dismissals are not recorded. `routine_sessions` is the only history and is mutated in place.
8. **Boot reschedule.**
   - `AlarmReceiver(BOOT_COMPLETED)` → `scheduleAllEnabledAlarms()` → `scheduleBasedOnRepeatDays` for every enabled alarm; `remainingSnooze` reset to max.
   - One-time alarm whose time passed while off is rescheduled for the next day with no missed-alarm record.
   - Not handled: `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`. In-flight snooze lost on reboot.
   - `RoutineReceiver` registers for `BOOT_COMPLETED` but does nothing; routine timeouts do not survive reboot.

### 2a. Breakages once Room / ViewModels / `AlarmApp` are gone

| # | Location | Problem |
|---|---|---|
| C-1 | `AlarmReceiver:32-35`, `RoutineReceiver:22-32` | Cast to `AlarmApp`, use its repositories. Without it: `ClassCastException`. |
| C-2 | `AlarmForegroundService:47,69-70` | `ble` via `AlarmApp`; `AlarmDatabase.getDatabase` Room singleton. |
| C-3 | `SystemAlarmScheduler:162-174` vs `121-133` | `cancelAlarm` uses `ACTION_ALARM_TRIGGERED`; snooze uses `ACTION_ALARM_SNOOZE_TRIGGERED`: different PendingIntents, so deleting/disabling during a snooze does not cancel it. Pre-existing bug. |
| C-4 | `AlarmForegroundService:79-250` | Ring state in instance fields; `START_STICKY` restarts with null intent → `alarmId == -1 → stopSelf()` (line 160). Process death silently ends the ring. |
| C-5 | `AlarmForegroundService:168-230` | `startForeground` only after DB read, unbounded BLE connect and audio prepare. Android 12+ risk of `ForegroundServiceDidNotStartInTimeException`. |
| C-6 | `AlarmActivity:119-215` | `setContent` inside a coroutine after a DB read; missing alarm → stop service + finish. |
| C-7 | `AlarmActivity:142-146` | Routine hand-off is a captured lambda/`var`, not state. |
| C-8 | `AlarmActivity:178-192` | Collects process-singleton `bleDeviceManager.serviceMessages`. |
| C-9 | `AlarmScreen.kt`, `ScannerComposables.kt:406-408` | `passedGate`, math list (`remember { generateMathTaskList }`, regenerated), barcode index use `remember`, not `rememberSaveable`; no `configChanges`/orientation lock in manifest, so rotation/recreation resets the task and changes math questions. |
| C-10 | `RoutineFlowActivity:129,153-168` | `setContent` re-called in `onNewIntent`; API 34-only override; `vm.set*` in composition; `standard` launch mode + `CLEAR_TOP` without `SINGLE_TOP` recreates the instance (unverified), so `onNewIntent` may not be exercised. |
| C-11 | `RoutineFlowManager`, `RoutineFlowViewModel` | Room `Flow` and mutable updates; mutability forbidden for native. |
| C-12 | `AppAlarmManagerImpl:151` | Native writes `isEnabled=false`; violates "native writes only `alarm_events`". |
| C-13 | `Alarms.kt:120-127` | Debug broadcast without action: receiver rings without reschedule. |
| C-14 | `RoutineScheduler:82-97` | No scheduling on API < 31. |
| C-15 | `AlarmForegroundService:124` | `currentAlarm!!` can NPE if `ACTION_UI_HIDDEN` arrives after finalise while `alarmStopped` is false. |

## 3. BLE (A2): hooks to cut

- `application/AlarmApp.kt:81-93` builds the BLE manager.
- Service calls `ble.requestAndReturnAutoConnect/requestStartAlarm/requestStopAlarm/requestSnoozeAlarm/emitServiceMessage/closeNotifications` (`AlarmForegroundService.kt:89,104,135,150,203,225,299`).
- `AlarmActivity.kt:63,178-192` collects `serviceMessages`.
- `BleNotificationManager` owns channel `ble_channel` ("BT Connection").
- Manifest: `BLUETOOTH*`, `ACCESS_FINE_LOCATION ≤30`.

If BLE returns, define a `RingCompanion` interface (`onRingStart/Stop/Snooze`) with a no-op default in the native module. Do not reintroduce a singleton `Application` graph.

## 4. Data contract

### 4a. What native needs at ring time

- **Alarm:** `id, hour, minute, enabled, repeat days, label, soundUri, snoozeEnabled, snoozeDurationMinutes, snoozeMaxCount, routineId`.
- **Tasks (ordered):** `task_id, position, type, payload`.
- **Routine:** `id, name, description, icon`.
- **Steps (ordered):** `step_number, length_minutes, name, description, icon, step_type`.
- **Not needed natively:** `systemHookId` (never read in inspected files), `espEnabled`, `lastUpdate`, `suggestedTimeMinutes`, `seedKey`, scan sets (barcode payload is inlined into the task).

### 4b. Conventions (Drift side)

- Booleans 0/1. Time as **epoch-millis `int64`** (avoid `dateTime()`). Enums as `text()` with stable string keys, never ordinals.
- Repeat days bitmask: `MON=1, TUE=2, WED=4, THU=8, FRI=16, SAT=32, SUN=64`; `0` = one-time (replaces `"MONDAY,TUESDAY"` from `data/Converters.kt:10-15`).
- Set `PRAGMA journal_mode=WAL` and `PRAGMA busy_timeout=3000` (two processes/engines touch the file).
- Keep SQL basic for API 24's engine: no generated columns, `STRICT`, `RETURNING`, or JSON functions in anything native reads. Parse payload JSON in Kotlin (`org.json`).

### 4c. Drift tables (Flutter-owned)

```dart
class Alarms extends Table {
  IntColumn get id => integer().autoIncrement()();
  IntColumn get hour => integer()();
  IntColumn get minute => integer()();
  BoolColumn get isEnabled => boolean().withDefault(const Constant(true))();
  TextColumn get label => text().nullable()();
  IntColumn get repeatMask => integer().withDefault(const Constant(0))();
  TextColumn get soundUri => text().nullable()();
  BoolColumn get snoozeEnabled => boolean().withDefault(const Constant(false))();
  IntColumn get snoozeDurationMinutes => integer().withDefault(const Constant(5))();
  IntColumn get snoozeMaxCount => integer().withDefault(const Constant(0))(); // non-null: fixes snoozeMaxCount!! NPE
  IntColumn get routineId => integer().nullable().references(Routines, #id, onDelete: KeyAction.setNull)();
  Int64Column get createdAtMs => int64()();
  Int64Column get updatedAtMs => int64()();
}

class AlarmTasks extends Table {
  IntColumn get id => integer().autoIncrement()();
  IntColumn get alarmId => integer().references(Alarms, #id, onDelete: KeyAction.cascade)();
  IntColumn get position => integer()();
  TextColumn get type => text()();        // none|timer|countdown|text|math|barcode|target|game
  TextColumn get payloadJson => text().withDefault(const Constant('{}'))();
  @override List<Set<Column>> get uniqueKeys => [{alarmId, position}];
}

class Routines extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get name => text()();
  TextColumn get description => text()();
  TextColumn get icon => text()();
}

class StepDefinitions extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get seedKey => text().nullable().unique()();
  TextColumn get stepType => text()();
  TextColumn get name => text()();
  TextColumn get description => text()();
  TextColumn get icon => text()();
  IntColumn get suggestedTimeMinutes => integer().withDefault(const Constant(15))();
}

class RoutineSteps extends Table {
  IntColumn get id => integer().autoIncrement()();
  IntColumn get routineId => integer().references(Routines, #id, onDelete: KeyAction.cascade)();
  IntColumn get stepId => integer().references(StepDefinitions, #id, onDelete: KeyAction.restrict)();
  IntColumn get stepNumber => integer()();
  IntColumn get lengthMinutes => integer()();   // Room `length: Long`
  @override List<Set<Column>> get uniqueKeys => [{routineId, stepNumber}];
}

class RoutineSessions extends Table {            // Flutter-owned projection, rebuilt from alarm_events
  IntColumn get id => integer().autoIncrement()();
  IntColumn get routineId => integer()();
  IntColumn get stepNumber => integer()();
  TextColumn get stepStatus => text()();        // awaiting|running|paused|completed
  Int64Column get stepStartMs => int64()();
  IntColumn get maxSteps => integer()();
  TextColumn get status => text()();            // running|paused|completed|canceled
  Int64Column get startMs => int64()();
  Int64Column get endMs => int64().nullable()();
}

class ScanSets extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get name => text()();
  TextColumn get codesJson => text()();         // [{"barcode":"...","tip":"..."}]
}

class AlarmEvents extends Table {               // append-only, written by native
  IntColumn get id => integer().autoIncrement()();
  Int64Column get tsMs => int64()();
  TextColumn get type => text()();
  IntColumn get alarmId => integer().nullable()();
  IntColumn get taskId => integer().nullable()();
  IntColumn get routineId => integer().nullable()();
  IntColumn get sessionKey => integer().nullable()();   // native-allocated, see §4g
  IntColumn get stepNumber => integer().nullable()();
  TextColumn get dataJson => text().nullable()();
}
```

**Cross-process note.** Drift streams only invalidate on writes through that Drift instance; native writes to `alarm_events` are invisible to them. Flutter must re-query on `AppLifecycleState.resumed` and on `onNativeEventsAvailable`.

### 4d. `alarm_runtime` view

One row per alarm task (one row if the alarm has no tasks).

```sql
CREATE VIEW alarm_runtime AS
SELECT
  1                          AS contract_version,
  a.id                       AS alarm_id,
  a.hour, a.minute,
  a.repeat_mask,
  a.label, a.sound_uri,
  a.snooze_enabled, a.snooze_duration_minutes, a.snooze_max_count,
  a.routine_id,
  a.updated_at_ms,
  CASE
    WHEN a.is_enabled = 0 THEN 0
    WHEN a.repeat_mask = 0 AND EXISTS (
      SELECT 1 FROM alarm_events e
      WHERE e.alarm_id = a.id AND e.type = 'RING_STARTED' AND e.ts_ms >= a.updated_at_ms
    ) THEN 0
    ELSE 1
  END                        AS effective_enabled,
  t.id                       AS task_id,
  t.position                 AS task_position,
  t.type                     AS task_type,
  t.payload_json             AS task_payload
FROM alarms a
LEFT JOIN alarm_tasks t ON t.alarm_id = a.id
ORDER BY a.id, t.position;
```

`effective_enabled` resolves C-12: a one-time alarm that has rung since its last edit counts as disabled without native writing to `alarms`. Flutter folds this into `is_enabled` when it ingests the event.

### 4e. `routine_runtime` view

```sql
CREATE VIEW routine_runtime AS
SELECT
  1 AS contract_version,
  r.id AS routine_id, r.name AS routine_name, r.description AS routine_description, r.icon AS routine_icon,
  rs.step_number, rs.length_minutes,
  sd.name AS step_name, sd.description AS step_description, sd.icon AS step_icon, sd.step_type
FROM routines r
JOIN routine_steps rs ON rs.routine_id = r.id
JOIN step_definitions sd ON sd.id = rs.step_id
ORDER BY r.id, rs.step_number;
```

**Native schema check:** open read-only; check `PRAGMA user_version` against a native-held supported range; check `SELECT contract_version FROM alarm_runtime LIMIT 1`. On any failure or exception, fall back to a plain ring (loop default audio, basic dismiss screen, default snooze) and append `RING_FALLBACK_PLAIN` if the events table is reachable. Native uses a separate read/write connection only for event appends.

### 4f. Task payload JSON (`alarm_tasks.type`)

Existing encodings come from `AlarmViewModel.validateData` and `AlarmScreen.kt`.

| Type | Current `taskData` | New `payloadJson` | Rules |
|---|---|---|---|
| `none` | null | `{}` | |
| `timer` | null | `{}` | Unimplemented; swipe-gate fallback. |
| `countdown` ("Clicker") | `"7"` | `{"count": 7}` | int > 0 |
| `text` | free text | `{"text": "..."}` | non-blank, trimmed |
| `math` | `"FIRST\|3"` | `{"level":"FIRST","count":3}` | level ∈ `FIRST,SECOND,THIRD,FOURTH`; count 1..15 (`Inputs.kt:140`) |
| `barcode` | `"code:tip,code:tip"` | `{"codes":[{"barcode":"...","tip":"..."}]}` | non-empty (`MultiBarcodeScannerView` `require`s it). Formats today: QR, EAN-13, EAN-8. |
| `target` | `"7"` | `{"rounds": 7}` | int > 0, premium |
| `game` | game id | `{"gameId":"spikes"}` | ids in `GamesRegistry`: `test, gltest, spikes, spikesgl` |

Native must tolerate unknown types and malformed payloads by falling back to a swipe-to-dismiss gate, never crash the ring.

### 4g. `alarm_events` types and routine session state

Types: `RING_STARTED`, `RING_FALLBACK_PLAIN`, `SNOOZED`, `TASK_STARTED`, `TASK_COMPLETED`, `DISMISSED`, `RING_MISSED`, `SCHEDULED`, `SCHEDULE_FAILED`, `BOOT_RESCHEDULED`, `ROUTINE_STARTED`, `STEP_STARTED`, `STEP_SKIPPED`, `STEP_COMPLETED`, `STEP_TIMEOUT`, `ROUTINE_COMPLETED`, `ROUTINE_ABANDONED`.

**Routine session state** (native may not update `routine_sessions`). Proposal: event-sourcing plus a small native state file.
- Native allocates `session_key = max(existing)+1` and keeps the *active* session pointer (sessionKey, routineId, stepNumber, stepStartMs, status) in a dedicated `SharedPreferences` file, read by `RoutineReceiver` and `RoutineFlowActivity`, surviving process death.
- Flutter folds events into `RoutineSessions` for history.
- Open decision: alternatively give native its own small `native_state.db`. Not decided.

## 5. Pigeon API

```dart
// pigeons/alarm_api.dart
import 'package:pigeon/pigeon.dart';

enum PermissionKind { exactAlarm, fullScreenIntent, notifications, batteryOptimization, camera }
enum PermissionStatus { granted, denied, permanentlyDenied, notApplicable }
enum ScheduleError { none, exactAlarmDenied, notificationsDisabled, invalidTime, unknown }
enum ContractStatus { ok, versionMismatch, viewMissing, dbUnavailable }

class ScheduleRequest {
  ScheduleRequest({required this.alarmId, required this.triggerAtEpochMs, required this.remainingSnooze});
  int alarmId;
  int triggerAtEpochMs;
  int remainingSnooze;
}
class ScheduleResult {
  ScheduleResult({required this.alarmId, required this.error, this.scheduledAtEpochMs});
  int alarmId;
  ScheduleError error;
  int? scheduledAtEpochMs;
}
class NativeCapabilities {
  NativeCapabilities({required this.sdkInt, required this.manufacturer,
    required this.canScheduleExact, required this.canUseFullScreenIntent,
    required this.notificationsEnabled, required this.ignoringBatteryOptimizations,
    required this.contractStatus, required this.dbUserVersion});
  int sdkInt;
  String manufacturer;
  bool canScheduleExact;
  bool canUseFullScreenIntent;
  bool notificationsEnabled;
  bool ignoringBatteryOptimizations;
  ContractStatus contractStatus;
  int dbUserVersion;
}
class AlarmEventDto {
  AlarmEventDto({required this.id, required this.tsMs, required this.type,
    this.alarmId, this.routineId, this.sessionKey, this.stepNumber, this.dataJson});
  int id; int tsMs; String type;
  int? alarmId; int? routineId; int? sessionKey; int? stepNumber; String? dataJson;
}
class ActiveRing {
  ActiveRing({required this.alarmId, required this.remainingSnooze, required this.startedAtMs});
  int alarmId; int remainingSnooze; int startedAtMs;
}

@HostApi()
abstract class AlarmHostApi {
  String getDbPath();
  NativeCapabilities getCapabilities();
  ScheduleResult scheduleAlarm(ScheduleRequest request);
  void cancelAlarm(int alarmId);          // also cancels any pending snooze
  void rescheduleAll();                   // native reads alarm_runtime; after bulk edits
  PermissionStatus getPermissionStatus(PermissionKind kind);
  @async PermissionStatus requestPermission(PermissionKind kind);
  void openSystemSettings(PermissionKind kind);
  void previewAlarm(int alarmId);         // non-ringing preview of the Activity
  void triggerRingNow(int alarmId);       // debug / "test alarm"
  ActiveRing? getActiveRing();
  void startRoutine(int routineId);       // user-launched, outside an alarm
  @async String? scanBarcodeOnce();       // editor-side scan, if native scanner is reused
}

@FlutterApi()
abstract class AlarmFlutterApi {
  // Best-effort: Flutter may not be running. The DB is the source of truth.
  void onNativeEventsAvailable(int latestEventId);
  void onRingStateChanged(ActiveRing? ring);
  void onContractMismatch(ContractStatus status, int dbUserVersion);
  void onPermissionChanged(PermissionKind kind, PermissionStatus status);
}
```

Notes:
- Flutter computes `triggerAtEpochMs`. Native also keeps a Kotlin `RepeatAlarmCalculator` because it must reschedule the next occurrence after a ring with no Flutter running. Keep both with shared golden vectors. On iOS the Dart calculator is the only one.
- Do not pass tasks or routine data through Pigeon at ring time; native reads the DB.
- `FlutterApi` calls need a live engine; native must guard "engine attached?" and drop silently otherwise.
- `AlarmEventDto` is defined for completeness; Flutter reads `alarm_events` directly from Drift.

## 6. Manifest, permissions, services, channels, Gradle

### 6a. Permissions

| Permission | Keep? | Note |
|---|---|---|
| `CAMERA` + `uses-feature camera required=false` | Keep | Barcode task. Request in Flutter at setup; currently prompted at ring time over the lock screen (`CameraPermissionWrapper`). |
| `POST_NOTIFICATIONS` | Keep | FG notification visibility and full-screen intents. |
| `WAKE_LOCK` | Keep | |
| `SCHEDULE_EXACT_ALARM` (max 32), `USE_EXACT_ALARM` | Decide | Both declared. **Play policy:** `USE_EXACT_ALARM` is for alarm-clock/calendar apps; a habit app may not qualify. With `setAlarmClock`, `SCHEDULE_EXACT_ALARM` may be unnecessary (unverified for your target SDK). |
| `USE_FULL_SCREEN_INTENT` | Keep | Android 14+: user-revocable for non-alarm/calling apps; check `canUseFullScreenIntent()` (current code never does). |
| `FOREGROUND_SERVICE`, `_MEDIA_PLAYBACK`, `_SYSTEM_EXEMPTED` | Keep | See 6b. |
| `RECEIVE_BOOT_COMPLETED` | Keep | |
| `VIBRATE` | Keep (verify) | Code only enables vibration on the channel. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Keep (policy-sensitive) | Play restricts it. Current code also calls `checkSelfPermission` on it (`MainActivity` commented block, `AppPermissionRegistry`), which is meaningless for this permission. |
| `INTERNET` | Drop unless host needs it | Only for ads; WebView games load locally via `WebViewAssetLoader`. |
| `BLUETOOTH*`, `ACCESS_FINE_LOCATION (≤30)` | Drop (A2) | |

`<attribution android:tag="alarm">` + `createAttributionContext("alarm")` (API 30+): keep for attributed audio; the empty-tag attribution is unexplained.

### 6b. Components

- `AlarmActivity`: `exported=false`, `showWhenLocked`, `turnScreenOn`, `launchMode=singleTask`, `excludeFromRecents`, `Theme.App.Fullscreen`. Add `screenOrientation`/`configChanges` (C-9); consider `taskAffinity` (§7).
- `RoutineFlowActivity`: `exported=false`, `excludeFromRecents`. Has **no** `showWhenLocked`/`turnScreenOn` though it is a full-screen-intent target; add if step-timeout alerts must show over the lock screen.
- `AlarmReceiver`, `RoutineReceiver`: `exported=false`. Only the boot receiver needs the `BOOT_COMPLETED` filter; system broadcasts do not require `exported=true`.
- `AlarmForegroundService`: `exported=false`, `foregroundServiceType="systemExempted|mediaPlayback"`. On API 34+ `systemExempted` is valid only with exact-alarm permission/exemption, tying to the 6a policy decision. Pass an explicit type to `startForeground` on API 29+.
- Remove `<application android:name=".application.AlarmApp">`; host Application applies.
- Remove AdMob `meta-data` (A3).
- Packaging: native Android library or local Flutter plugin package. A plugin's manifest merges automatically and registers Pigeon in `onAttachedToEngine`.

### 6c. Notification channels

| Channel id | Name | Importance | Settings | Source |
|---|---|---|---|---|
| `alarm_channel` | Alarms | HIGH | sound null, vibration, `VISIBILITY_PUBLIC` | `AlarmForegroundService.kt:305-324` (`AlarmReceiver.showNotification` creates it differently but is dead code) |
| `routine_status_channel` | Routine Status | LOW | default notification sound on a LOW channel, vibration, public | `RoutineFlowManager.kt:364-384` |
| `routine_alarm_channel` | Routine Alarm | HIGH | default notification sound, vibration, public | `RoutineFlowManager.kt:342-362` |
| `ble_channel` | BT Connection | — | — | drop with BLE |

Channel settings are immutable after first creation: keep these ids if any build ever shipped with them; create channels once from one place. Notification ids: ring FG = 1; routine status/timeout/end all share 1119.

### 6d. Gradle / AGP / Kotlin conflicts

| Item | This project | Risk |
|---|---|---|
| AGP | 8.13.2 | Host decides AGP; one per build. |
| Kotlin | 2.1.0 (toml; also hard-coded `serialization` plugin 2.1.0) | Compose compiler plugin version must equal Kotlin. Older host Kotlin (e.g. 1.9.x) breaks Compose. Verify first. |
| compileSdk / targetSdk | 36 / 36 | CameraX 1.5.1 and `ComponentCaller` need recent compileSdk. |
| minSdk | 24 | May differ from `flutter.minSdkVersion`; API-24 paths need verification (C-14, `onNewIntent`). |
| JVM target | 11 | Align with host. |
| kapt, Room, protobuf, datastore, serialization | present | All removable (Room dropped; the rest serve dropped features). |
| Compose BOM | 2024.09.00 | Keep inside native module only. |
| CameraX 1.5.1, ML Kit barcode 17.3.0 | core, camera2, lifecycle, view | Bundled model grows APK per ABI. Unbundled `play-services-mlkit-barcode-scanning` needs Play Services and a possible first-use download at ring time. Not decided. |
| `androidx.webkit` | used, not declared | Add explicitly. |
| `com.google.android.material` | needed by `Theme.Material3.*`, not declared | Add explicitly. |
| `play-services-ads`, `places` | present | Drop ads (A3). `places` not referenced in files audited; verify before removing. |
| Flavors `prod/alarmTest1/alarmTest2`, `IS_ALARM_TEST`, `IS_ADS_ON` | present | Drop. |
| `repositoriesMode FAIL_ON_PROJECT_REPOS` (`settings.gradle.kts`) | present | Flutter's Gradle setup adds project repos; do not copy this setting. |
| Pigeon | n/a | Match the host's Dart/Flutter SDK; generated Kotlin needs a reasonably recent Kotlin. |
| R8/proguard | `isMinifyEnabled=false` | Host likely minifies: keep rules for ML Kit, CameraX, `@JavascriptInterface` (`GameBridge`). |

## 7. Risks and unknowns

1. **OEM battery restrictions.** Xiaomi/Oppo/Vivo/Huawei/Samsung/OnePlus can kill the process or block `BOOT_COMPLETED`. `setAlarmClock` is more resistant but auto-start limits can still block the receiver. The code has only a standard ignore-battery-optimisation intent (`SettingsView.kt:204`).
2. **Full-screen intent.** Android 14+ revocable permission, never checked. Without `POST_NOTIFICATIONS` it does not fire. `ACTION_UI_HIDDEN` re-calls `startAsForeground` after 3 s, updating the same notification id; whether a re-notify re-fires the full-screen intent is OS/version-dependent. Unverified, and the "bring ring UI back" mechanism depends on it.
3. **Task affinity / launch modes.** `AlarmActivity` is `singleTask`, started with `NEW_TASK|NO_HISTORY`. `NO_HISTORY` finishes it when it leaves the screen (Home), leaving `onStop` → `ACTION_UI_HIDDEN` as the only recovery. In a Flutter host, `NEW_TASK` with default affinity places it in the host's task; `excludeFromRecents` blocks return via Recents. Needs a device test with the host in foreground, background and killed. A dedicated `taskAffinity` is an option, unverified.
4. **Lock-screen flags.** `AlarmActivity` sets show-when-locked/turn-screen-on in manifest and code. `RoutineFlowActivity` has none. `requestDismissKeyguard` is unused, so a camera task on a secure lock screen is untested.
5. **Process death during routine.** Session lives in Room; step timeout is an `AlarmManager` alarm (survives process, not reboot). Task progress and notification chronometer are not restored. During a ring, process death ends the ring (C-4).
6. **Flutter engine absence.** Native must never assume a live engine or registered Pigeon channel. `FlutterApi` calls are best-effort. Native Activities must not extend `FlutterActivity` or use the engine cache. Flutter state is stale until it ingests `alarm_events` on next start.
7. **Cross-process SQLite.** Needs WAL, `busy_timeout`, read-only reads, one short transaction per event append. Drift's bundled SQLite (`sqlite3_flutter_libs`) and Android's framework SQLite are different engine versions; keep SQL basic. DB is in credential-encrypted storage; `BOOT_COMPLETED` arrives after unlock so this works, but `LOCKED_BOOT_COMPLETED` handling would not.
8. **Schema drift.** A Flutter migration that fails midway leaves native with only the plain-ring fallback (default sound, default snooze, no task).
9. **Background start limits (Android 12+).** The `setAlarmClock` receiver path is exempt. `ACTION_FINALIZE_ALARM` via `startService` from the routine Activity works only while it is visible.
10. **Time / DST.** Next-fire uses `Calendar` in the default zone (DST handled by platform). No `TIME_SET`/`TIMEZONE_CHANGED` receiver, so alarms stay stale after a timezone change until boot or edit.
11. **Duplicated scheduling logic** (Dart vs Kotlin calculators) can diverge; golden vectors mitigate.
12. **Snooze state in extras.** `remainingSnooze - 1` lives in the PendingIntent extras and is lost if the PendingIntent is cancelled/recreated (e.g. `rescheduleAll`). Persist the snooze count in the active-ring record.
13. **Request codes.** `alarmId.toInt()` truncates a `Long`; routine code `sessionId.toInt() + 10000` can collide with alarm ids; fixed codes 13/14/15 are shared across sessions.
14. **WebView games.** JavaScript with an `AndroidBridge` interface in a lock-screen Activity; WebView startup cost at ring time; games not reviewed.
15. **Not determined from code:** see the not-inspected list at the top. `systemHookId` is declared in `AlarmEntity` and never read in inspected files. `OverriddenTaskType` is written by the debug wrapper and never read.

## 8. Migration checklist

Size: S under 0.5 day, M 1-3 days, L a week or more. N = native, F = Flutter.

| # | Item | Size | Side |
|---|---|---|---|
| 1 | Decide scope (A1-A7), Play policy for `USE_EXACT_ALARM`/battery exemption/full-screen intent, ML Kit bundled vs unbundled | S | both |
| 2 | Check host AGP, Kotlin, compileSdk, minSdk, Gradle; confirm Compose compiler plugin is viable | S | N |
| 3 | Create native module (local Flutter plugin or Android library); wire Pigeon codegen and a smoke `getDbPath()` call | M | both |
| 4 | Drift schema (§4c), `alarm_runtime`/`routine_runtime` views (§4d-e), WAL + busy-timeout, `user_version` handling, step-definition seeder | M | F |
| 5 | Native `AlarmRuntimeDb`: read-only reader over views, contract/version check, event appender, payload parser (`org.json`), plain-ring fallback | M | N |
| 6 | Port `RepeatAlarmCalculator` to bitmask input; Dart port; shared golden vectors | S | both |
| 7 | Native `AlarmScheduleCoordinator` + `SystemAlarmScheduler` (schedule, snooze, cancel incl. snooze PI, next-after-ring, one-time via events, boot reschedule); split `BootReceiver`; add `TIME_SET`/`TIMEZONE_CHANGED`/`MY_PACKAGE_REPLACED` | M | N |
| 8 | Rewrite `AlarmForegroundService`: immediate `startForeground`, persisted active-ring record, START_STICKY recovery, no DI/BLE, merged stop paths, event writes | L | N |
| 9 | Rewrite `AlarmActivity` loading (no singletons, `setContent` first, saved task state, orientation/config handling, snooze via scheduler) | M | N |
| 10 | Task UIs: typed payloads in `AlarmScreen`; remove Debug variants; barcode/camera screens; WebView games + assets | M | N |
| 11 | Routine native runtime: `RoutineFlowManager` over reader + events, `RoutineScheduler` API<31 fix, `RoutineReceiver` cleanup, `RoutineFlowActivity`/state holder (no composition side effects, correct `onNewIntent`, lock-screen flags), boot restore of active session | L | N |
| 12 | Manifest and resource merge (§6): permissions, `exported=false`, channels, themes, strings, icons | S | N |
| 13 | Flutter alarm and routine editor UI, task editors, repository/DAO layer, validation (`validateData` rules), snooze/day pickers | L | F |
| 14 | Flutter scan-set CRUD and editor-side scanning (`mobile_scanner` or Pigeon `scanBarcodeOnce`) | M | F |
| 15 | Flutter permission and capability screens (exact alarm, FSI, notifications, battery, OEM guidance) via Pigeon | M | F |
| 16 | Flutter event ingestion (fold `alarm_events` into `RoutineSessions`/alarm state; apply one-time disable), refresh on resume and `onNativeEventsAvailable` | M | F |
| 17 | Pigeon `previewAlarm`/`triggerRingNow` replacing debug broadcast and `AlarmActivityWrapper` | S | both |
| 18 | Device test matrix: killed/background/foreground host, lock screen, Doze, reboot, process death mid-ring and mid-routine, API 24/30/34/36, at least Xiaomi and Samsung | L | both |
| 19 | iOS: implement `schedule()` against the same Pigeon surface using the Dart calculator (AlarmKit/notification chain) | L | F + Swift |
