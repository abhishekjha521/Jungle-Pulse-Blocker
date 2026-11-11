# YouTube Ad Skip Safeguards & Report Popup Elimination

Eliminates accidental triggering of YouTube's "Report this ad" / "About this ad" dialogs by enforcing strict node targeting on the genuine "Skip Ad" button and automatically dismissing any report or info sheets if they ever appear.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Confirmed Decision**: The user confirmed that the app should close any report or info popup if accidentally opened and never attempt to report ads. The skip mechanism must strictly target genuine skip buttons.

- **Strict Button Discrimination**: Completely remove loose parent/sibling clicking logic that previously clicked adjacent "ⓘ" (Info/Why this ad) icons on unskippable ads or countdown views.
- **Auto-Dismiss Shield**: If a YouTube "My Ad Center", "About this ad", or "Report" dialog/sheet appears, the service will detect the dismiss/cancel button or perform a back action to close it instantly, ensuring seamless viewing.
- **Direct Skip Targeting**: Target exact verified YouTube skip button resource IDs (`skip_ad_button`, `modern_skip_ad_button`) and verified skip texts ("Skip", "Skip Ad", "Skip in", "विज्ञापन छोड़ें") only when the node itself is verified as an active, enabled skip button.

---

## 1. Overview & Core Concept

- **What It Does**: Ensures smooth, uninterrupted YouTube video playback by clicking the skip button as soon as it becomes available (0ms) without ever interacting with YouTube's reporting or ad-detail dialogs.
- **Target Audience**: Users seeking seamless YouTube video watching without annoying ads or unexpected popup sheets.
- **Key Value**: Zero distraction, zero accidental touches on "Report Ad" or "Why this ad?", and instant ad skipping.

---

## 2. User Experience & Visual Design

- **Status & Feedback**:
  - The app's main screen clearly displays the active protection status: "⚡ Direct Skip Mode Active (Report Dialogs Blocked)".
  - A dedicated toggle in settings/home screen allows users to customize "Auto-dismiss Report Dialogs".
- **Visual Palette & Theme**:
  - Deep Jungle Green (`#0F2618`), Emerald Accent (`#10B981`), Warning Amber (`#F59E0B`), and Dark Surface (`#14281E`) matching the Jungle Pulse brand identity.
- **Interactive Feedback**:
  - Clean Toast alert on successful ad skip (`⚡ YouTube Ad Skipped!`).
  - No interrupting screens or dialogs while the user is inside YouTube.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Removal of Sibling Click Traversal in Accessibility Tree**
  - *Chosen Approach*: Disable recursive sibling clicking (`for (j in 0 until target.childCount) { target.getChild(j).performAction(...) }`). Instead, verify that the node or its immediate parent is specifically labeled as a skip container before performing `ACTION_CLICK`.
  - *Why*: In YouTube's player UI, the "ⓘ" (About this ad / Report) button is an immediate sibling of the ad countdown timer. Indiscriminate sibling clicking caused the "Report" menu to open.
  - *Alternatives Considered*: Blacklisting only the word "Report" — rejected because opening the dialog in the first place is what disrupts the user.
- **Decision 2: Active Popup Dismissal Watchdog**
  - *Chosen Approach*: If an overlay containing "About this ad", "Why this ad?", "My Ad Center", or "Report this ad" is detected, trigger the close icon (`close_button`, `dismiss`, or `GLOBAL_ACTION_BACK`) immediately.
  - *Why*: Fulfills the user's selected preference to automatically close any report/info sheet if accidentally shown.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│             AdSkipAccessibilityService (YouTube)            │
└──────────────────────────────┬──────────────────────────────┘
                               │ AccessibilityEvent
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                Event & Node Filtering Engine                │
├─────────────────────────────────────────────────────────────┤
│ 1. Check for Report / Info Dialog:                          │
│    Is "About this ad" / "Report ad" open?                   │
│    YES ──► Click Close (X) / Back (Dismiss immediately)     │
│                                                             │
│ 2. Find Verified Skip Buttons:                              │
│    - R.id.skip_ad_button / modern_skip_ad_button            │
│    - Text: "Skip Ad", "Skip", "Skip in", "विज्ञापन छोड़ें"   │
│                                                             │
│ 3. Strict Validation:                                       │
│    - Must be clickable or direct child of skip container    │
│    - EXCLUDE any node containing "info", "report", "why"    │
│    - Click verified skip button ──► 0ms Skip Toast          │
└─────────────────────────────────────────────────────────────┘
```

- **Interactive Verification**:
  - Test with standard skippable ads: Skip button clicked instantly at 5s.
  - Test with unskippable ads: Audio muted without touching the "ⓘ" info button.
  - Test report sheet detection: Instant dismissal without user intervention.
