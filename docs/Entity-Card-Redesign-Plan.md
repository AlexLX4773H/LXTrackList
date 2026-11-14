# Entity Card Redesign Plan — Cross-Theme Symmetry & Density

> **Scope:** This is a planning document only. No code is changed here.
> All theme analysis is based on the five screenshots provided and direct inspection of the source (`MediaCard.kt`, `AppTheme.kt`, all `theme.json` files, and drawable assets).

---

## 1. Problem Statement

### What's Wrong Right Now

The current `MediaCard` composable stretches a single `card_frame_ornate` PNG sprite across the **entire card** using `ContentScale.FillBounds`. This causes:

| Issue | Root Cause |
|---|---|
| **Border art bleeds over thumbnail** | Frame sprite corners overlap the cover image area |
| **Border art bleeds over text/badges** | Frame sprite's side rails sit on top of content area |
| **FAB-style increment button overlaps card edge** | Button is positioned inside the frame but the frame has art extending beyond logical bounds |
| **Inconsistent card height across themes** | Beyond/Slime use tall padding; Gate/Sky use different sizing rules |
| **Too few cards on screen** | Current card height ≈ 120–130 dp; padding and spacing are generous |
| **Thumbnail is too small on advanced themes** | At 66×94 dp it's already shrunk for "advanced" themes but frame ornament eats into it visually |
| **Missing: inner data surface** | The ornate frame is the only background — there is no separate "inner panel" to contrast against |

### Theme-by-Theme Visual Audit

#### ✅ Default Dark / Clean Light / Crimson Dark / Slate Blue & Grey
- Cards look clean and functional
- Simple rounded rect with thin border
- No sprite issues — this is the reference baseline to maintain parity with

#### ⚠️ Beyond (Arcane Rune)
- Crimson ornate border PNG works well aesthetically but the **corners are very thick** and intrude into the thumbnail corner
- The thumbnail border sprite (`theme_beyond_thumbnail_border.png`) is not currently applied — there's a visual disconnect between the cover art and the ornate frame
- The `icon_card_star` increment button (22 dp) rides on top of the progress bar area
- The `icon_heart_active` / `icon_favorite` at 18 dp render too small inside the 24 dp IconButton touch target

#### ⚠️ Slime (Playful Meadow)
- The `card_frame_ornate` for Slime has animated-water-looking **bottom drips** that hang below the card's logical bottom edge, overlapping the next card's top margin
- The slime mascot (`icon_card_slime`) used as a quick-increment button renders well but is positioned inconsistently — sometimes it appears on the right of the progress bar, sometimes it appears as a floating FAB (the 4th partial card in the screenshot)
- Chips/badge area is tight because the slime frame insets are uneven (thicker on left/right than top)

#### ⚠️ Gate (Dungeon Tactical Steel)
- **Best-looking of the advanced themes** in the screenshots but still has issues:
- The riveted steel frame has corner bolts that visually fight with the rounded clip applied by the Surface
- Gate deliberately omits the increment `+` button (see `if (!isGate)` in code) which causes layout asymmetry in the progress bar row
- Icon sizes (heart, more) appear very small — 14 dp inside a 24 dp touch area
- The thumbnail isn't bordered at all — it looks floating inside the dark frame

#### ⚠️ Sky (Aincrad HUD)
- The HUD frame sprite clips poorly — its **outer glow bleeds** past the Surface's rounded clip, making the card look like it has a halo artifact on dark backgrounds
- The concentric-ring increment button (`theme_sky_icon_plus.png` at 24 dp) is too close to the progress bar track sprite
- The HUD progress bar fill image is narrow, causing a visual gap between fill and track sprites

---

## 2. Proposed Architecture — Layered Card System

### Core Concept: Outer Shell + Inner Panel

Replace the single-layer approach with a **two-layer nested card**:

```
┌──────────────────────────────────────────────────────┐
│  OUTER SHELL  (theme sprite, full-bleed, no padding) │
│  ┌────────────────────────────────────────────────┐  │
│  │  INNER PANEL  (solid color surface, padded)    │  │
│  │  ┌──────┐  Title            ♥  ⋮              │  │
│  │  │Cover │  Subtitle                            │  │
│  │  │ Art  │  Ep / Progress  ★Score  [+]         │  │
│  │  └──────┘                                      │  │
│  │  ─────────────────────────────────────────────│  │
│  │  🔗 2 Related: [0 Tracking] [2 Untracked] View│  │
│  └────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
```

**Key rules:**
- The **outer shell** holds the theme sprite at full card size, with 9-patch-style padding that tells the inner panel how much inset to apply so the sprite's corner art never overlaps content.
- The **inner panel** is a `Surface` with a semi-transparent fill derived from the theme's `surface` color (typically ~85% alpha). It clips cleanly to a `RoundedCornerShape`.
- The **outer shell** does NOT clip — it needs to bleed into the margin to look natural (sprite corners, drips, glows).
- **Non-advanced themes** get a simple version: outer = solid rounded rect border, inner = same surface as now. No structural change.

### New Sizing Targets

| Measurement | Current (Advanced) | Proposed |
|---|---|---|
| Outer card margin (vertical between cards) | 10 dp | 8 dp |
| Inner panel padding (H) | 12–14 dp | 10 dp |
| Inner panel padding (V top/bottom) | 12 dp | 8 dp |
| Cover art width | 66 dp | 60 dp |
| Cover art height | 94 dp | 86 dp |
| Title font | `titleMedium` bold | `titleSmall` bold (13 sp) |
| Subtitle font | `bodySmall` | `labelMedium` (10 sp) |
| Progress row height | 16 dp | 11 dp |
| Increment button size | 24 dp | 22 dp |
| Minimum cards visible at once (phone) | ~3.5 | **~4.5–5** |

> Cover art shrinkage is intentional — 60×86 dp is still comfortably readable and aligns with the inner panel's reduced padding. Title and subtitle trimming prevents the card from growing to two lines on most entries.

---

## 3. Per-Theme Implementation Specification

### 3.1 Beyond (Arcane Rune / Mystic Night)

**Outer Shell:**
- Sprite: `theme_beyond_card_frame_ornate.png` — rendered as `ContentScale.FillBounds` on the outer Box
- Outer Box has **no clip** so corner runes extend into 4 dp margin
- Outer padding insets for inner panel: `start=14 dp, end=14 dp, top=10 dp, bottom=10 dp`

**Inner Panel:**
- Background: `BeyondThemeColors.surface` (`#181928`) at **88% alpha** → gives a readable dark surface while letting the background show slightly through
- Corner radius: `10 dp` (matching `cardCornerRadius`)
- **Thin crimson inner border**: 0.5 dp, `#8C2828` — this visually "grounds" the panel inside the ornate frame

**Cover Art:**
- Apply `theme_beyond_thumbnail_border.png` as an overlay on top of the cover image (currently unused!)
- Size: 60×86 dp

**Increment Button:**
- Keep `theme_beyond_icon_card_star.png` at 22 dp
- Enclose in a 28×28 dp clickable Box for better touch target

**Sprite Changes Needed:**
- `theme_beyond_card_frame_ornate.png` — the **inner content area** of the PNG should have a transparent window (alpha hole) rather than a filled dark background, so the inner panel color shows through cleanly. Recommend recutting the sprite so only the ornate border ring and corner runes are opaque; the center is fully transparent.

---

### 3.2 Slime (Playful Meadow & Bubbles)

**Outer Shell:**
- Sprite: `theme_slime_card_frame_ornate.png` — the dripping bottom edge is intentional but causes overlap. The sprite should be extended with 6 dp of transparent padding at the bottom — OR the outer Box should have `bottom padding = 6 dp` to push the next card away from the drip.
- Outer padding insets for inner panel: `start=14 dp, end=14 dp, top=8 dp, bottom=14 dp` (more bottom inset to stay above the drip art)

**Inner Panel:**
- Background: `SlimeThemeColors.surface` (`#0A4D78`) at **85% alpha**
- Corner radius: `12 dp`
- Inner border: 1 dp, `#38BDF8` at 50% alpha (cyan, matching the frame's water ripple color)

**Cover Art:**
- Apply `theme_slime_thumbnail_border.png` overlay — currently assigned in `theme.json` but NOT rendered in the code
- Size: 60×86 dp

**Increment Button:**
- Keep `theme_slime_icon_card_slime.png` at 22 dp
- Properly position to the right of the progress bar only (currently the last card shows the slime FAB overlapping the card body — that's because the FAB and card-level increment are conflated)

**Sprite Changes Needed:**
- `theme_slime_card_frame_ornate.png` — the center fill should become **fully transparent** (alpha punched out), keeping only the ornate water/bubble border ring. Add ~6 dp transparent bleed at the bottom to give the drip art room.
- **New sprite needed:** `theme_slime_thumbnail_border.png` — a small decorative bubble/water frame around the cover art (60×86 dp, PNG with alpha). Already referenced in `theme.json` and drawable exists; needs to actually be rendered.

---

### 3.3 Gate (Dungeon Portal / Tactical Steel)

**Outer Shell:**
- Sprite: `theme_gate_card_frame_ornate.png` — rivet-and-steel frame
- The sprite's corner bolts are hard-edged so outer Box clips to `8 dp` radius matches well
- Outer padding insets for inner panel: `start=12 dp, end=12 dp, top=8 dp, bottom=8 dp`

**Inner Panel:**
- Background: `GateThemeColors.surface` (`#222227`) at **90% alpha** — almost opaque to contrast against the dark steel frame
- Corner radius: `6 dp` (tighter than the outer 8 dp, so the frame border is visible all around)
- Inner border: none — the steel frame itself acts as the border

**Cover Art:**
- **No thumbnail border sprite** currently assigned for Gate — a simple 1 dp amber (`#F59E0B`) inner border on the cover art Box would complement the theme
- Size: 60×86 dp

**Increment Button:**
- Gate intentionally omits the in-card `+` button (the shield FAB is the entry point)
- BUT this causes the progress bar row to be misaligned (all weight on the left). **Fix:** add a `Spacer(modifier = Modifier.width(30.dp))` on the right of the progress bar when Gate theme is active, so alignment is consistent with other themes.
- Alternatively: introduce a small **amber chevron** `>` icon (22 dp) as a visual indicator of "tap to log" — no functional change, just fills the visual gap.

**Sprite Changes Needed:**
- `theme_gate_card_frame_ornate.png` — same as other themes: punch out the center area to full transparency, keeping only the steel rivet border. The inner panel will then show through.
- **New sprite needed:** `theme_gate_thumbnail_border.png` — a 60×86 dp amber/bronze rectangular border to frame the cover art. Should look like a tactical display frame (simple, angular, no curves).

---

### 3.4 Sky (Aincrad HUD / Holographic)

**Outer Shell:**
- Sprite: `theme_sky_card_frame_ornate.png` — holographic cyan HUD frame
- The outer glow bleed is by design but the clip should be removed from the outer Box. Use `ContentScale.Fit` (not `FillBounds`) with explicit sizing to prevent the glow from stretching
- Outer padding insets for inner panel: `start=16 dp, end=16 dp, top=10 dp, bottom=10 dp` (Sky frame has thicker scanner-line borders)

**Inner Panel:**
- Background: `SkyThemeColors.surface` (`#14203D`) at **82% alpha** — slightly more transparent to let the HUD background show through (Sky has a unique low-dim background of 0.35 vs 0.6 for others)
- Corner radius: `10 dp`
- Inner border: 0.5 dp, `#06B6D4` at 60% alpha (holographic cyan)

**Cover Art:**
- Size: 60×86 dp
- Keep plain rounded rect clip — no border sprite assigned yet. A thin cyan glow border (0.5 dp) with a nested Box background imitation would look HUD-appropriate without needing a new asset.

**Increment Button:**
- Keep `theme_sky_icon_plus.png` (the concentric ring button) at 22 dp
- Enclose in a 28×28 dp clickable Box

**Sprite Changes Needed:**
- `theme_sky_card_frame_ornate.png` — punch center to full transparency, preserve the holographic scan-line border ring and corner reticle markers.
- **New sprite needed:** `theme_sky_thumbnail_border.png` — 60×86 dp cyan HUD targeting frame around cover art. Think angular corner brackets (like a gun-sight / targeting reticle frame), no solid fill. Transparent center.

---

## 4. Related Section Improvements

The Related Franchise footer strip has the same bleed problem on advanced themes — it draws a solid `Color(0xFF141414)` background regardless of theme, which looks jarring on the colorful Sky/Slime frames.

**Fix:** Use the theme's `surfaceVariant` color (at 85% alpha) instead of hardcoded black for the related section background. Keep the `HorizontalDivider` but use the theme's `border` color at 40% alpha.

---

## 5. Card Density: Grid Layout Changes

Currently in `HomeScreen.kt`:
```kotlin
LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 360.dp),  // only 1 column on phones
    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
)
```

**Proposed:**
```kotlin
LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 340.dp),  // still 1 col phone, 2 col tablet
    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)   // was 10 dp
)
```

Combined with the reduced card internal padding from Section 2, this should fit **~4.5–5 cards** on a standard phone screen (up from ~3.5).

> Do NOT go below 8 dp vertical spacing — the outer shell sprites need room to breathe (especially Slime's drip art and Beyond's corner runes).

---

## 6. Sprite Change Summary

### Sprites That Need Recutting (Center Punched to Transparent)

| File | Theme | Change |
|---|---|---|
| `theme_beyond_card_frame_ornate.png` | Beyond | Punch interior to alpha=0, keep only ornate border ring |
| `theme_slime_card_frame_ornate.png` | Slime | Punch interior to alpha=0, extend bottom by ~6 dp transparent bleed for drip art |
| `theme_gate_card_frame_ornate.png` | Gate | Punch interior to alpha=0, keep steel rivet border. Ensure corners are sharp (8 dp not rounded) |
| `theme_sky_card_frame_ornate.png` | Sky | Punch interior to alpha=0, keep HUD scan-line border ring and corner reticle marks |

### New Sprites Needed

| File | Theme | Description | Size |
|---|---|---|---|
| `theme_gate_thumbnail_border.png` | Gate | Angular amber/bronze tactical display frame around cover art | 60×86 dp @ 2x+3x |
| `theme_sky_thumbnail_border.png` | Sky | Cyan holographic corner-bracket targeting frame for cover art | 60×86 dp @ 2x+3x |

> `theme_beyond_thumbnail_border.png` and `theme_slime_thumbnail_border.png` already exist in drawables but are **not currently rendered** in `MediaCard.kt`. They just need to be wired up in code — no new artwork needed for Beyond and Slime.

### Sprites Already Existing — Just Not Wired

| File | Theme | Where to Apply |
|---|---|---|
| `theme_beyond_thumbnail_border.png` | Beyond | Overlay on the 60×86 cover art Box |
| `theme_slime_thumbnail_border.png` | Slime | Overlay on the 60×86 cover art Box |

---

## 7. Code Architecture for the New MediaCard

The refactor should create a clean split:

```
MediaCard()
 └── ThemedCardShell()     ← new composable: handles outer sprite & insets per theme
      └── CardInnerPanel() ← new composable: handles surface color, corner radius, border
           ├── CardCoverArt()     ← thumbnail + optional border sprite
           ├── CardDetailsColumn()
           │    ├── CardTitleRow()       ← title + favorite + more
           │    ├── CardSubtitle()
           │    ├── CardProgressRow()    ← ep count, badges, score
           │    └── CardProgressBar()   ← themed progress bar + increment btn
           └── CardRelatedFooter()      ← related franchise section
```

Each sub-composable receives the theme flags (`isBeyond`, `isSlime`, etc.) plus a new `CardInsets` data class:

```kotlin
data class ThemeCardInsets(
    val outerToInnerStart: Dp,
    val outerToInnerEnd: Dp,
    val outerToInnerTop: Dp,
    val outerToInnerBottom: Dp,
    val innerCornerRadius: Dp,
    val innerBorderColor: Color,
    val innerBorderWidth: Dp,
    val innerSurfaceAlpha: Float,
    val thumbnailWidth: Dp = 60.dp,
    val thumbnailHeight: Dp = 86.dp,
    val showThumbnailBorderSprite: Boolean = false
)
```

Instances of this data class are defined once per theme (similar to `LXThemeColors`) and accessed from `ThemeManager`, removing all the scattered `if (isBeyond) ... else if (isSlime)` branching from the card body.

---

## 8. Default / Non-Advanced Themes Impact

No visual change to Default Dark, Clean Light, Crimson Dark, or Slate Blue & Grey.

The `ThemedCardShell` composable simply renders a plain `Surface` for non-advanced themes (identical to current behaviour). The `CardInsets` for non-advanced themes match current padding values exactly.

---

## 9. Implementation Checklist

### Phase 1 — Sprite Art (Prerequisite)
- [ ] Recut `theme_beyond_card_frame_ornate.png` — transparent center
- [ ] Recut `theme_slime_card_frame_ornate.png` — transparent center + bottom bleed
- [ ] Recut `theme_gate_card_frame_ornate.png` — transparent center, sharp corners
- [ ] Recut `theme_sky_card_frame_ornate.png` — transparent center, preserve HUD glow ring
- [ ] Create `theme_gate_thumbnail_border.png` (new) — angular amber frame, 60×86 dp @ 2x & 3x
- [ ] Create `theme_sky_thumbnail_border.png` (new) — cyan HUD corner-bracket frame, 60×86 dp @ 2x & 3x

### Phase 2 — Code Refactor
- [ ] Define `ThemeCardInsets` data class in `AppTheme.kt`
- [ ] Add inset instances per theme preset in `ThemeManager`
- [ ] Create `ThemedCardShell` composable
- [ ] Create `CardInnerPanel` composable
- [ ] Create `CardCoverArt` composable (wire up `thumbnail_border` sprites for Beyond & Slime)
- [ ] Refactor `MediaCard` body to use the new sub-composables
- [ ] Remove scattered `if (isBeyond) ... else if (isSlime) ...` chains from card body
- [ ] Update `HomeScreen.kt`: reduce grid spacing from 10 dp → 8 dp, content padding 12 → 10 dp
- [ ] Update `CardRelatedFooter`: use `theme.surfaceVariant` instead of hardcoded `#141414`
- [ ] Fix Gate theme: add `Spacer` or chevron icon in progress row to maintain row balance

### Phase 3 — Visual QA
- [ ] Verify Beyond: corner runes don't overlap thumbnail; inner panel readable against background
- [ ] Verify Slime: drip art doesn't clip into next card; slime mascot button position consistent
- [ ] Verify Gate: rivet corners align with 6 dp inner radius; amber thumbnail border visible
- [ ] Verify Sky: outer glow doesn't artifact on dark bg; cyan target brackets on thumbnail
- [ ] Verify Default Dark: no visual regression — card looks identical to pre-refactor
- [ ] Count cards on 1080×2400 phone screen at each theme — target ≥ 4.5 visible

---

## 10. Visual Concept Sketches (ASCII)

### Beyond Card (Layered)
```
╔══[rune]══════════════════════════[rune]╗   ← sprite outer shell (bleeds into margin)
║  ┌─────────────────────────────────┐  ║
║  │ ╔══════╗  Yowayowa Sensei    ♥ ⋮│  ║   ← inner panel (crimson 0.5dp border)
║  │ ║Cover ║  よわよわ先生 • YowaYo  │  ║
║  │ ║ Art  ║  8/12 ep         ★ 6.4 │  ║
║  │ ╚══════╝  ████████░░░░░░░  ✦    │  ║   ← ✦ = star increment btn
║  └─────────────────────────────────┘  ║
║  🔗 1 Related: [0 Tracking] [1 Untr.] View ▼  ║
╚═══════════════════════════════════════╝
```

### Slime Card (Layered)
```
 ～～～[bubble]～～～～～～～～～～～[bubble]～
〰 ┌──────────────────────────────────┐  〰   ← sprite outer shell
〰 │ ╔══════╗  Yowayowa Sensei    ♥ ⋮│  〰   ← inner panel (cyan border)
〰 │ ║Cover ║  よわよわ先生 • YowaYo  │  〰
〰 │ ║ Art  ║  8/12 ep         ★ 6.4 │  〰
〰 │ ╚══════╝  ████████░░░░░░░  🫧   │  〰   ← 🫧 = slime mascot increment
〰 └──────────────────────────────────┘  〰
〰 🔗 1 Related: [0 Tracking] [1 Untr.] View ▼ 〰
  ~~~~[drip]~~~~~~~~~~~~~~~~~[drip]~~~~         ← drip art extends below card into margin
```

### Gate Card (Layered)
```
╔═[bolt]══════════════════════════[bolt]╗   ← steel rivet frame
║  ┌─────────────────────────────────┐  ║
║  │ ┌──────┐  Yowayowa Sensei    ♥ ⋮│  ║   ← inner panel (no extra border)
║  │ │Cover │  よわよわ先生 • YowaYo  │  ║
║  │ │ Art  │  8/12 ep         ★ 6.4 │  ║
║  │ └──────┘  ████████░░░░░░░░  ›   │  ║   ← › = amber chevron spacer
║  └─────────────────────────────────┘  ║
║  🔗 1 Related: [0 Tracking] [1 Untr.] View ▼  ║
╚═[bolt]══════════════════════════[bolt]╝
```

### Sky Card (Layered)
```
◈━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━◈   ← HUD scan-line frame
┃  ┌─────────────────────────────────┐  ┃
┃  │ ┌┄┄┄┄┄┄┐  Yowayowa Sensei    ♥ ⋮│  ┃   ← inner panel (cyan 0.5dp border)
┃  │ ┊Cover ┊  よわよわ先生 • YowaYo  │  ┃
┃  │ ┊ Art  ┊  8/12 ep         ★ 6.4 │  ┃   ← ┊ = HUD corner brackets on thumbnail
┃  │ └┄┄┄┄┄┄┘  ████████░░░░░░░  ⊕   │  ┃   ← ⊕ = concentric ring increment
┃  └─────────────────────────────────┘  ┃
┃  🔗 1 Related: [0 Tracking] [1 Untr.] View ▼  ┃
◈━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━◈
```

---

## 11. Handoff Notes for Implementor

> [!IMPORTANT]
> Read this section carefully before writing any code. It covers project-specific conventions that are not obvious from the plan alone.

---

### 11.1 Sprite Recutting Is a Human Task — Code Around It

The plan requires the four `card_frame_ornate` PNGs to have their center areas punched to full transparency. **This cannot be done in code — it requires a graphics tool (Photoshop / GIMP / Figma).** The sprites have NOT been recutted yet at the time of this handoff.

**Implement the layered architecture regardless.** To compensate for the opaque sprite center while the artwork is pending:
- Set the outer shell Box to `alpha = 0f` (invisible) temporarily, OR
- Apply the frame sprite normally and rely on the **inner panel's solid surface** rendering on top of it — the inner panel will visually cover the sprite's center fill, and only the border ring will peek out around the edges. This is the preferred approach; it works correctly once the sprites are recutted too.

The two thumbnail border sprites that need new artwork (`theme_gate_thumbnail_border.png` and `theme_sky_thumbnail_border.png`) also don't exist yet. For Gate, substitute a simple `border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(4.dp))` on the cover art Box. For Sky, substitute `border(0.5.dp, Color(0xFF06B6D4), RoundedCornerShape(4.dp))`. These are code-only placeholders until the real sprites arrive.

---

### 11.2 Drawable Naming Convention

All theme sprites live in one flat folder: `app/src/main/res/drawable/`

Naming pattern: `theme_<themeid>_<spritename>.png`

| theme id | Kotlin reference prefix |
|---|---|
| `beyond` | `R.drawable.theme_beyond_` |
| `slime` | `R.drawable.theme_slime_` |
| `gate` | `R.drawable.theme_gate_` |
| `sky` | `R.drawable.theme_sky_` |

Examples already in the project:
```
theme_beyond_card_frame_ornate     → R.drawable.theme_beyond_card_frame_ornate
theme_beyond_thumbnail_border      → R.drawable.theme_beyond_thumbnail_border  ✅ exists, wire it up
theme_slime_thumbnail_border       → R.drawable.theme_slime_thumbnail_border   ✅ exists, wire it up
theme_gate_progress_bar_bg         → R.drawable.theme_gate_progress_bar_bg
theme_sky_icon_plus                → R.drawable.theme_sky_icon_plus
```

There are **no drawable subdirectories** — do not try to create `drawable-xhdpi/` etc.; all assets are already density-independent PNGs in the flat `drawable/` folder.

---

### 11.3 `ThemeManager` Is a Kotlin `object` — Use It Directly

`ThemeManager` is a plain Kotlin singleton object, not a ViewModel or composable. Access it directly anywhere:

```kotlin
// Correct — direct property access
val isBeyond = ThemeManager.isBeyondTheme
val colors = ThemeManager.currentColors

// WRONG — do not do this
val themeManager = viewModel<ThemeManager>()          // won't compile
val themeManager = LocalThemeManager.current          // doesn't exist
```

The boolean helpers already on `ThemeManager`:
```kotlin
ThemeManager.isBeyondTheme   // true when Beyond theme active
ThemeManager.isSlimeTheme
ThemeManager.isGateTheme
ThemeManager.isSkyTheme
// isAdvanced = any of the above is true
```

The full color set for the active theme:
```kotlin
ThemeManager.currentColors   // LXThemeColors instance
ThemeManager.currentColors.primary
ThemeManager.currentColors.surface
ThemeManager.currentColors.border
// etc.
```

---

### 11.4 Two Color Systems — Use the Theme-Aware One for New Code

There are **two parallel color systems** in the project. The old one is static and always Default Dark green; the new one is per-theme. `MediaCard.kt` currently uses the old static system throughout. **Use the theme-aware system for all new code.**

| Old static (avoid in new code) | Theme-aware equivalent |
|---|---|
| `LXPrimaryGreen` | `ThemeManager.currentColors.primary` |
| `LXBorder` | `ThemeManager.currentColors.border` |
| `LXTextPrimary` | `ThemeManager.currentColors.textPrimary` |
| `LXTextSecondary` | `ThemeManager.currentColors.textSecondary` |
| `LXSurfaceBlack` | `ThemeManager.currentColors.surface` |
| `LXSurfaceElevated` | `ThemeManager.currentColors.surfaceElevated` |
| `LXProgressTrack` | `ThemeManager.currentColors.progressTrack` |
| `LXScoreGold` | `ThemeManager.currentColors.scoreGold` |
| `LXAiringBlue` | `ThemeManager.currentColors.airingBlue` |
| `LXErrorRed` | `ThemeManager.currentColors.error` |
| `LXBackgroundBlack` | `ThemeManager.currentColors.background` |

> **Note:** You do NOT need to migrate all existing usages of the old statics — only new composables you create. Avoid making the diff any larger than necessary.

---

### 11.5 Reference Implementation: `ThemedFloatingActionButton.kt`

Before writing `ThemedCardShell`, read `ThemedFloatingActionButton.kt` in full. It is the existing example of correctly structured per-theme sprite branching:
- Shows the `when (true)` / `isBeyond` / `isSlime` / `isGate` / `isSky` / `else` branching pattern
- Shows how to stack a background sprite + foreground icon using nested `Box` composables
- Shows `painterResource(R.drawable.theme_xyz_...)` usage alongside `ContentScale` choices

**File location:** `app/src/main/kotlin/com/lx/tracklist/ui/components/ThemedFloatingActionButton.kt`

---

### 11.6 Strict File Scope — Do Not Touch These

Only modify files directly relevant to the card rendering. **Do not modify:**

| File | Reason |
|---|---|
| `HomeScreen.kt` | Only the two grid spacing lines from Section 5 are permitted changes — nothing else |
| `DetailScreen.kt` | Out of scope |
| `AddEditScreen.kt` | Out of scope |
| `LibraryScreen.kt` | Out of scope |
| `SearchScreen.kt` | Out of scope |
| `AppNavigation.kt` | Out of scope |
| Any `*ViewModel.kt` | Out of scope |
| `theme/*.json` files | Out of scope |
| Any file in `themes/` folder | Out of scope |

**Files you will modify:**
- `app/src/main/kotlin/com/lx/tracklist/ui/components/MediaCard.kt` — primary target
- `app/src/main/kotlin/com/lx/tracklist/ui/theme/AppTheme.kt` — add `ThemeCardInsets` data class and instances
- `app/src/main/kotlin/com/lx/tracklist/ui/home/HomeScreen.kt` — grid spacing only (2 lines)

**New file you may create:**
- `app/src/main/kotlin/com/lx/tracklist/ui/components/CardInsets.kt` — if you prefer to keep `ThemeCardInsets` separate from `AppTheme.kt`

---

### 11.7 `ContentScale` Rules Per Theme

| Theme | Outer shell sprite ContentScale | Reason |
|---|---|---|
| Beyond | `ContentScale.FillBounds` | Frame designed to stretch to any card height |
| Slime | `ContentScale.FillBounds` | Same; drip art is at the bottom of the PNG canvas |
| Gate | `ContentScale.FillBounds` | Rivet frame tiles cleanly |
| Sky | `ContentScale.FillBounds` | Frame fills bounds; outer glow handled by inner panel opacity, not by Fit cropping |
| All non-advanced | n/a | No sprite — Surface only |

> **Correction from earlier draft:** Sky also uses `FillBounds`. The glow bleed issue is solved by the inner panel rendering on top (covering the center), not by switching to `ContentScale.Fit`. The outer Box for all advanced themes should have `Modifier.matchParentSize()` and no `.clip()` on it.

---

### 11.8 Inner Panel Alpha Values (Quick Reference)

| Theme | Surface color | Recommended alpha |
|---|---|---|
| Beyond | `#181928` | 0.88f |
| Slime | `#0A4D78` | 0.85f |
| Gate | `#222227` | 0.90f |
| Sky | `#14203D` | 0.82f (bg dim is 0.35, so more transparency is fine) |

Apply as: `color = ThemeManager.currentColors.surface.copy(alpha = <value>)`

---

### 11.9 Gate Theme — Progress Bar Row Balance Fix

Gate intentionally has **no increment button** (see existing `if (!isGate)` guard in `MediaCard.kt`). This leaves the progress bar `Row` unbalanced. Add a `Spacer` on the right to maintain symmetric layout:

```kotlin
// Inside the progress bar Row, after the themed progress bar Box:
if (isGate) {
    Spacer(modifier = Modifier.width(30.dp))  // balances the missing increment btn
} else {
    // ... existing increment button code
}
```

---

*Document version: 1.1 — 2026-11-14 (Implementor handoff notes added)*
