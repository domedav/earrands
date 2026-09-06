# Ballanceometer

> Gamifikált havi költségvetés-kezelő Androidra — oldd fel a pénzed a szokásaiddal.
> Gamified monthly budget app for Android — unlock your money with your habits.

Beállítod a havi keretedet (`total`) és a minimális biztonsági tartalékot (`minimal`).
A kettő különbsége (`total − minimal`) zárolva van, és csak a napi teendők
kipipálásával „oldható fel" költésre. A súlyozott feladatcsoportok határozzák meg,
melyik szokás mennyi pénzt ér.

## Funkciók

- **Feloldás-matek (`BalanceEngine`):** `earnable = max(0, total − minimal)`,
  csoportrészesedés súlyarányosan, feladatonként havi példányszám alapján elosztva
  (`ONCE` = 1, `DAILY` = napok száma, `WEEKLY` ≈ hetek száma, `MONTHLY` = 1).
- **Show képernyő:** egyenleg-mérő (`BalanceMeter`), elérhető / feloldott / elköltött
  kártyák, mai teendők pipálható listája, költés hozzáadása/törlése.
- **Config képernyő:** havi keret + minimum + pénznem, feladatcsoportok
  0–100 súlycsúszkával, alcsoportonként feladatok ismétlődéssel
  (`ONCE`, `DAILY`, `WEEKLY`, `MONTHLY`).
- **Data képernyő:** havi összesítők, JSON export / import (felülírás megerősítéssel).
- **Otthoni widget (Glance):** aktuális egyenleg + haladás + mai teendők,
  egyérintéses kipipálással (`ToggleSubtaskAction`), WorkManager-alapú frissítéssel.
- **Perzisztencia:** Room adatbázis (`AppConfig`, `TaskGroup`, `Subtask`,
  `Completion`, `Spending`), DataStore beállítások.

## Tech stack

Kotlin 2.2 · Jetpack Compose (Material 3 Expressive) · Room 2.7 + KSP ·
Glance AppWidget · WorkManager · DataStore Preferences · Coroutines ·
AGP 9.3.1 · minSdk 26, target/compileSdk 37

## Projektstruktúra

```
app/src/main/java/com/domedav/ballanceometer/
├── BallanceometerApp.kt        # Application + Room singleton
├── MainActivity.kt             # 3 tab: Show / Data / Config (PillNav)
├── domain/BalanceEngine.kt     # tiszta feloldás-matek, unit-tesztelhető
├── data/                       # entitások, DAO-k, Repository, ExportImportHelper
├── ui/show|data|config/        # képernyő + ViewModel nézeteként
├── ui/components/BalanceMeter.kt
├── ui/theme/                   # M3 Expressive téma + Glance téma
└── widget/                     # Glance widget + Receiver + Worker + Action
```

## Build és futtatás

Követelmény: Android Studio (legfrissebb), JDK 11+, Android SDK 37.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Telepítés: `app/build/outputs/apk/debug/app-debug.apk`, vagy Run ▶ Android Studioban.

Release aláíráshoz (csak lokálisan, opcionális) hozz létre egy `key.properties`
fájlt a projekt gyökerében — ez **soha nem kerül gitbe**:

```properties
storeFile=releasekey.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Enélkül is fordul a debug build és az unsigned release.

## Biztonság

Publikáláskor semmilyen titok nem kerül ki: `key.properties`,
`local.properties`, `*.jks`, `*password*` fájlok a `.gitignore`-ban szerepelnek,
és a git-előzményekben sincs nyoma titoknak. A release signing csak akkor
aktiválódik, ha a `key.properties` lokálisan létezik.

## Licenc

Copyright © 2026 domedav. Minden jog fenntartva — lásd [`LICENSE`](LICENSE).
A projekt nem nyílt forráskódú: másolás, módosítás és terjesztés csak a szerző
írásos engedélyével.
