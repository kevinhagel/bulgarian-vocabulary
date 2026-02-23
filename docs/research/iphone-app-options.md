# iPhone App Options for Bulgarian Vocabulary Tutor

*Research date: 2026-02-23*

## The Existing App

**Bulgarian Vocabulary Tutor** — a personal language learning app with:
- **Frontend**: React 19 + Vite 6 + TypeScript (TanStack Query, Zustand), served via nginx from `frontend/dist`
- **Backend**: Spring Boot 3.4.2 / Java 25, REST API on port 8080
- **Database**: PostgreSQL 16 (local Mac Studio)
- **Auth**: Google OAuth2 (via Spring Security)
- **TTS**: edge-tts for Bulgarian audio
- **Infrastructure**: All runs on a local Mac Studio M4 Max — never cloud
- **External URL**: `https://hagelbg.dyndns-ip.com`

---

## Two Critical iOS Problems to Understand First

Before choosing an approach, two iOS-specific issues affect this app:

### 1. Google OAuth2 is broken in iOS standalone PWAs (and WKWebView wrappers)
WebKit Bug #100407, open since 2018. When the OAuth redirect navigates to `accounts.google.com`, iOS drops out of standalone mode into Safari. The user ends up authenticated in Safari but the PWA has no session. There is no clean fix without architectural changes.

**The workaround for personal use**: Log in once via Safari (session cookie persists), then install the PWA. The installed PWA inherits the Safari session. Works until the session expires.

### 2. Audio stops on screen lock in standalone PWAs
WebKit Bug #198277, open since 2019. Partially improved in iOS 18+ but not reliably fixed. For SRS flashcard study with TTS pronunciation, this is real friction — switching apps mid-session stops Bulgarian audio playback.

---

## Option A: PWA (Progressive Web App)

### What it is
Add a `manifest.json` and optionally a service worker to the existing React/Vite app, then install on iPhone via Safari's "Add to Home Screen". The app gets its own icon, launches without browser chrome, and looks native.

The `vite-plugin-pwa` package (Workbox-based) handles this elegantly for Vite projects.

**iOS 26 note**: As of September 2025, iOS 26 makes every site added to the Home Screen open as a web app by default, even without a manifest. The "Add to Home Screen" flow shows an "Open as Web App" toggle.

### Requirements
- `manifest.json` with name, short_name, start_url, `display: standalone`, colors
- Icons: 192×192 and 512×512 PNG minimum
- `apple-touch-icon` link in `index.html` (Safari ignores manifest icons)
- HTTPS — already have it via Let's Encrypt

**Current state**: Zero PWA infrastructure exists in `frontend/public/`. Starting from scratch.

### Changes Required
- Install `vite-plugin-pwa`, generate icons, write manifest — **2–4 hours**
- OAuth2 fix for standalone mode — **3–7 days** (see critical problem above)
- Audio restrictions handling — **4–8 hours**
- Mobile-responsive UI audit — **1–2 days**

### Effort: 4–10 days (dominated by OAuth2 fix)

### Distribution
No App Store needed. No Apple Developer account needed. Visit the URL in Safari → Share → Add to Home Screen.

### Key Limitations
- OAuth2 is broken in standalone mode (workaround: pre-authenticate in Safari)
- Audio stops on screen lock
- 50MB cache storage quota
- 7-day inactivity cache eviction
- No App Store presence (just a home screen icon)

---

## Option B: Capacitor (Recommended)

### What it is
Capacitor wraps the existing web app in a thin iOS native shell (WKWebView + JavaScript bridge to native APIs), then produces a proper `.ipa` that installs on iPhone. The existing React/Vite codebase runs **unchanged** — zero component rewriting.

Key advantage over a bare WKWebView wrapper: Capacitor provides `ASWebAuthenticationSession` via plugins, which is Google's approved OAuth flow for native iOS apps.

### Changes Required

**Setup (1 day):**
```bash
npm install @capacitor/core @capacitor/cli @capacitor/ios @capacitor/browser
npx cap init "Bulgarian Vocabulary" "com.hagel.bgvocab" --web-dir dist
npx cap add ios
```

Create `.env.production`:
```
VITE_API_BASE_URL=https://hagelbg.dyndns-ip.com/api
```

Add to `package.json`:
```json
"build:ios": "vite build && npx cap sync"
```

**OAuth2 fix (2–3 days):**

The pragmatic approach: use `@capacitor/browser` to open the existing Spring Security OAuth flow in SFSafariViewController (Apple's in-app browser — Google-approved), then configure a custom URL scheme callback.

1. Add redirect URI `com.hagel.bgvocab://auth` to Google Console (iOS OAuth client type)
2. Spring Boot: handle `com.hagel.bgvocab://auth` as an alternate redirect URI
3. React app: detect Capacitor environment, use `Browser.open()` instead of `<a href>` for OAuth, use `App.addListener('appUrlOpen', ...)` to capture the callback

**Audio**: Works normally in Capacitor. No PWA standalone-mode audio restrictions apply.

**Apple Developer account + TestFlight (1 day of bureaucracy):**
1. Enroll at developer.apple.com ($99/year)
2. Create App ID `com.hagel.bgvocab` in App Store Connect
3. Archive in Xcode → Upload to TestFlight
4. Invite users (Kevin, Huw, Elena) via email

### Effort: 5–8 days

### Distribution
- **Xcode direct install** (free): phone plugged into Mac Studio, lasts 7 days on free account
- **TestFlight** ($99/year Apple Developer): install from TestFlight link, works for Kevin + Huw + Elena
- **App Store**: not needed for personal use

### Key Limitations
- Requires Apple Developer account ($99/year) for anything beyond local Xcode installs
- Requires `npx cap sync` + Xcode rebuild after frontend changes (not instant)
- The app is still WKWebView — not fully native rendering, but indistinguishable for a form-based CRUD app
- Backend stays on Mac Studio; the iPhone needs internet to reach `hagelbg.dyndns-ip.com`

---

## Option C: React Native

### What it is
A separate framework sharing React's component model (hooks, props, state management) but using different components (`<View>`, `<Text>`, `<FlatList>` instead of `<div>`, `<p>`, `<ul>`). Compiles to actual native iOS controls, not a WebView.

Business logic is portable: stores, API hooks, TypeScript types, utility functions all transfer. But every `.tsx` component must be rewritten. Tailwind CSS is replaced with `StyleSheet` or NativeWind.

**The table problem**: `InflectionsTable.tsx` (504 lines, complex CSS grid layout) would be genuinely painful to rebuild in React Native — no CSS `display: grid`, no `overflow-x: scroll` on tables. The current Tailwind version is elegant; the RN version would require custom scroll + layout logic.

### Effort: 3–6 weeks

### Why to skip it
Higher effort than Capacitor (full component rewrite), no meaningful advantage for this use case. The app is not animation-heavy or gesture-heavy in ways that would justify native rendering over WKWebView.

---

## Option D: SwiftUI Native

### What it is
A complete rewrite in Apple's native language and UI framework. Zero web technology. The most polished possible experience — native haptics on flashcard rating, native navigation gestures, lock screen audio controls for Bulgarian TTS, `LazyVGrid` for the inflections table.

SwiftUI's declarative model is familiar to a React developer. Swift `async/await` is clean. The learning curve is the Swift language itself (value types, optionals, `@State`/`@ObservableObject`/`@Observable`).

Google Sign-In has a first-class iOS SDK — the OAuth2 problem is trivially solved.

### Effort: 6–12 weeks

### When to consider it
If the Capacitor version gets heavy daily use and feels clunky, SwiftUI is the natural evolution. It's not the right first step — the time is better spent studying Bulgarian.

---

## Option E: WKWebView Wrapper

### What it is
A minimal Xcode project (50–100 lines of Swift) that presents a `WKWebView` loading `https://hagelbg.dyndns-ip.com`. Effectively a DIY Capacitor without the plugin ecosystem.

**Why to skip it**: Has the same OAuth2 problem as PWA (Google explicitly returns `disallowed_useragent` errors in WKWebView), and the same audio-on-lock-screen bug. When you add the custom user agent workaround and SFSafariViewController for auth, you've manually recreated what Capacitor gives you automatically — with no plugin ecosystem.

---

## Summary Comparison

| Option | Effort | Dev Account ($99/yr) | OAuth fix needed | Audio works | Verdict |
|--------|--------|----------------------|-----------------|-------------|---------|
| A: PWA | 4–10 days | No | Hard (workaround exists) | Partial | Quick win for personal use |
| **B: Capacitor** | **5–8 days** | **Yes** | **Medium** | **Yes** | **Best overall** |
| C: React Native | 3–6 weeks | Yes | Medium | Yes | Too much effort |
| D: SwiftUI | 6–12 weeks | Yes | Easy | Yes | Long game / future |
| E: WKWebView | 2–6 days | Yes | Hard | Partial | Worse Capacitor |

---

## Recommended Path

### Quick win (today, ~4–8 hours): Add PWA support

Add `vite-plugin-pwa` to `vite.config.ts`, generate icons, create `manifest.json`. Log in to the app once in Safari (session cookie persists), then Add to Home Screen. The PWA inherits the Safari session. Gets a home screen icon and no browser chrome with zero infrastructure change.

### Proper solution (~5–8 days): Capacitor

- All 79 existing React components run unchanged
- Solves OAuth2 via `ASWebAuthenticationSession` (Google-approved)
- Solves audio-on-lock-screen
- TestFlight distributes to Huw and Elena via email invite
- Backend stays on Mac Studio — internet required but no cloud migration
- $99/year Apple Developer account is the main new cost

---

*Research conducted by Claude Sonnet 4.6 on 2026-02-23.*
*Sources: WebKit Bugzilla, Apple Developer Forums, Capacitor docs, vite-plugin-pwa docs, Google OAuth2 native app docs.*
