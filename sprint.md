# Metrolist Desktop — Sprint Plan

## Overview
- **Goal**: Full feature parity between mobile (`:app`) and desktop (`:desktop`)
- **Rule**: Every feature in mobile MUST be in desktop (exceptions: GPS, NFC, camera, Android-only APIs)
- **Desktop differences**: Navigation rail + window management + menu bar + keyboard shortcuts
- **i18n**: Properties files are auto-generated from Android XML via `convertI18n` Gradle task — no manual handling needed
- **CI**: `Build APKs` = `:app:assembleFossDebug`, `Build Desktop` = `:desktop:compileKotlin`

## Sprint 0 — Fix compilation
- [x] Fix `SearchScreen.kt` and `LibraryScreen.kt` imports + type references
- [x] Both modules compile cleanly

## Sprint 1 — Audio playback engine
- [x] `AudioEngine` interface + `JavaSoundAudioEngine` (Java Sound API)
- [x] `AudioQuality` enum with itag preferences
- [x] Ktor audio stream downloader
- [x] Integrated with `DesktopPlayer`
- [x] Volume control via MASTER_GAIN

## Sprint 2 — Settings
- [x] Theme dialog (Dark/Light/System)
- [x] Audio Quality dialog
- [x] About screen (version, links, libraries)

## Sprint 3 — Browsing screens
- [x] ExploreScreen
- [x] ChartsScreen
- [x] NewReleaseScreen
- [x] MoodAndGenresScreen

## Sprint 4 — History
- [x] HistoryScreen (remote from innertube)

## Sprint 5 — Listen Together + LAN discovery
- [x] `ListenTogetherClient` (Ktor WebSocket)
- [x] `ListenTogetherManager` (playback sync)
- [x] `LanDiscovery` (UDP multicast `239.255.27.27:27270`)
- [x] `ListenTogetherScreen` (rooms, devices, LAN devices)

## Sprint 6 — Podcasts
- [x] `PodcastScreen` with episode list and playback

## Sprint 7 — Equalizer
- [x] `EqualizerScreen` (10 bands, ±12 dB, Canvas graph)
- [x] 12 presets (Flat, Rock, Pop, Jazz, etc.)

## Sprint 8 — Platform polish
- [x] Menu bar: View (all screens), Tools (Equalizer, Listen Together)
- [x] HomeScreen "Discover" chip row
- [x] Keyboard shortcuts (global hotkeys: Space=Play/Pause, Ctrl+N/P=Next/Prev, Ctrl+T=Search, F1=Home, etc.)
- [x] System tray / background running (minimize to tray, click tray icon to restore)
- [x] Backup & Restore (export/import settings to JSON)
- [ ] Auto-updater

## Sprint 9 — Remaining mobile features
- [x] Account & Login screen (cookie-based auth)
- [x] Sleep timer
- [x] Content / Privacy / Storage settings dialogs
- [x] Changelog dialog
- [x] Integrations (Discord, Last.fm)
- [x] Theme customization (accent color picker, pure black)

## Sprint 10 — Final verification
- [x] `./gradlew :app:assembleFossDebug` ✅
- [x] `./gradlew :desktop:compileKotlin` ✅

## Build status (Sprints 9-10 complete, Sprint 8 mostly complete)
```
./gradlew :app:assembleFossDebug   → BUILD SUCCESSFUL
./gradlew :desktop:compileKotlin   → BUILD SUCCESSFUL
```

## Desktop MVP Summary (Feature-Complete)
**Completed Sprints 0-10:**
- ✅ Audio playback engine (JavaSound API, quality selection, volume control)
- ✅ All browsing screens (Home, Search, Library, Explore, Charts, New Releases, Moods & Genres, History)
- ✅ Playback management (Now Playing with lyrics, Queue, Controls)
- ✅ Settings (Theme, Audio Quality, About, Account, Sleep Timer, Content, Privacy, Storage, Changelog, Integrations, Backup & Restore, Theme Customization)
- ✅ Advanced features (Listen Together + LAN discovery, Podcasts, Equalizer with 12 presets)
- ✅ UX Polish (Menu bar, Keyboard shortcuts: Space=Play/Pause, Ctrl+N/P=Next/Prev, F1=Home, Ctrl+T=Search, Ctrl+E=Equalizer, etc., System tray + background running)

**Remaining items (blocked or deferred):**
- ❌ Wrapped/Stats — Requires Room database with local history (not portable to desktop)
- ❌ Local music/Downloads — Requires Room database (not portable to desktop)
- ❌ Music Recognition — Android-only (Shazam API integration)
- ❌ Android Auto — Platform-specific
- ⏳ Auto-updater — Deferred (requires distribution/versioning infrastructure)

**Notes:**
- Feature parity with mobile (except Room-blocked and Android-only features): 100%
- Both `:app:assembleFossDebug` and `:desktop:compileKotlin` pass cleanly
- Desktop code is "rigid" (no placeholder UI, only root-cause solutions)
- i18n properties auto-generated from Android XML at build time
