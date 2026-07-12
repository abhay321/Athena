# Project Athena: Bento Grid WCAG 2.1 AA Accessibility Checklist

This checklist specifies the technical implementation standards for Project Athena's dynamic Bento Grid layout and modal view interactions. Adherence to these guidelines ensures full compliance with **WCAG 2.1 Level AA** standards, delivering a fully usable experience for users relying on keyboard navigation, screen readers (such as TalkBack or NVDA), and assistive input hardware.

---

## 1. Grid Structure & Keyboard Navigation (WCAG 2.1.1 & 2.4.3)

Because a Bento Grid displays information asymmetrically, visual grouping must map directly to logical keyboard focus order to prevent navigation confusion.

- [ ] **Sequential Focus Order:** Focus order (`Tab` key) must move logically from left-to-right, top-to-bottom matching the visual structure.
- [ ] **Focusable Card Anchors:** Each interactive Bento card must be focusable using standard anchor tags (`<a>`), buttons (`<button>`), or an explicit tab index (`tabindex="0"`).
- [ ] **Explicit Action Activation:** Non-interactive `<div>` or `<section>` tags styled as Bento cards that trigger navigation or popups must handle both mouse click and keyboard trigger keys:
  - [ ] `Enter` key.
  - [ ] `Space` key (ensuring standard `preventDefault()` behavior to block screen scroll).
- [ ] **Visible Focus Indicators:** Maintain high-contrast visual focus outlines (`outline: 3px solid #60A5FA` or matching `MaterialTheme.colorScheme.primary` in dynamic Compose code).
  - [ ] **Never** suppress outlines unless implementing a superior customized `:focus-visible` state.
  - [ ] Focus outlines must achieve a contrast ratio of at least **3:1** against the background (WCAG 1.4.11).

---

## 2. Dynamic Card Updates & ARIA Roles (WCAG 1.3.1 & 4.1.2)

When Bento cards receive real-time updates—such as dynamic OCR status changes, active AI reasoning thresholds, or incremental database storage counts—screen readers must be notified of changes without requiring page reload.

- [ ] **Polite Live Regions:** Use `aria-live="polite"` on cards displaying dynamic background status transitions (e.g., *"Running OCR..."* to *"Synced with Athena Brain via Google ML Kit"*).
  - [ ] Avoid `aria-live="assertive"` unless there is an absolute critical system failure requiring immediate context interruption.
- [ ] **Cohesive Update Announcements:** Apply `aria-atomic="true"` to card sub-containers to ensure the screen reader reads the complete updated text block instead of isolated fragments.
- [ ] **Intermittent Loading States:** Apply `aria-busy="true"` during background server operations (e.g., while Gemini is generating a textbook summary) to prevent early reading of incomplete data. Once completion succeeds, change to `aria-busy="false"`.
- [ ] **Grid Accessibility Markup:** If the Bento Grid behaves like a unified interactive widget, use `role="grid"`, `role="row"`, and `role="gridcell"`. For standard layout panels, represent cards using `<section>` elements enriched with semantic `aria-label` or `aria-labelledby` headings.

---

## 3. Focus Trap Management for Modals (WCAG 2.4.3 & 1.4.13)

When a Bento card (e.g., *Smart Capture* or *Ask Athena*) expands into a modal overlay, full-screen canvas, or floating chat dialog, focus must be contained tightly to prevent mouse-less users from getting "lost" in background DOM elements.

- [ ] **Aria-Hidden Backgrounds:** Upon modal activation, set `aria-hidden="true"` on the primary background container (`.app-container`) to isolate the modal element for screen readers.
- [ ] **Focus Redirection:** Immediately transition keyboard focus to the modal's primary header, closest input element, or parent dialog wrapper upon open.
- [ ] **Bidirectional Tab Loop (Focus Trap):** Bind a keyboard listener inside the modal to restrict focus navigation within its visual boundary:
  - [ ] Pressing `Tab` on the *last* focusable element within the modal must redirect focus back to the *first* focusable element.
  - [ ] Pressing `Shift + Tab` on the *first* focusable element must redirect focus to the *last* focusable element.
- [ ] **Escape Key Close Action:** Pressing the `Escape` physical keyboard button must close the active modal overlay immediately.
- [ ] **Origin Focus Restoration:** When the modal is dismissed, return keyboard focus precisely to the originating Bento card button that triggered it.

---

## 4. Visual Contrast & Accessibility Styles (WCAG 1.4.3 & 1.4.4)

Project Athena's modern *Cosmic Slate Dark Theme* must accommodate users with partial sight or color deficiencies.

- [ ] **Text Contrast Ratios:** Ensure all text passes minimum contrast thresholds:
  - [ ] Standard Body Text: Minimum **4.5:1** contrast ratio against the solid card background.
  - [ ] Large Headers (18pt / 24px and above): Minimum **3:1** contrast ratio.
- [ ] **High Contrast Mode Support:** Implement a high contrast layout media query (`@media (prefers-contrast: more)`) to drop background color gradients and overlay high-contrast borders:
  ```css
  @media (prefers-contrast: more) {
    :root {
      --bg-primary: #000000;
      --bg-card: #111111;
      --color-primary-text: #FFFFFF;
      --color-secondary-text: #F1F5F9;
    }
    .bento-card {
      border: 2px solid #FFFFFF;
    }
  }
  ```
- [ ] **Responsive Text Scale:** Avoid fixed layout heights (such as setting cards with absolute heights like `height: 120px` without `min-height`). Cards must expand dynamically if user accessibility preferences scale system font sizes up to **200%**.

---

## 5. Non-Text Content & Image Alternatives (WCAG 1.1.1)

Project Athena's scanner uses visual representations of whiteboard scans and complex diagrams which require textual alternatives.

- [ ] **Functional Image Alt Texts:** Document preview cards must contain descriptive text alternatives explaining the visual layout rather than filenames (e.g., use *“Whiteboard sketch with architecture diagrams showing gateway boxes and connection arrows”* instead of *“img_onboarding_brain_1783818582428”*).
- [ ] **Decorative Icons:** Set `aria-hidden="true"` or `contentDescription = null` in Android Compose on decorative visual icons (e.g., arrow decorations, background grid dots) to bypass screen-reader clutter.
- [ ] **Descriptive Action Labels:** Dynamic action elements (such as the Recalculate layout button) must use precise labeling:
  - [ ] *Correct:* `aria-label="Recalculate dynamic force layout for semantic relationship graph"`
  - [ ] *Incorrect:* `aria-label="Refresh"`

---

## 6. Testing & Compliance Verification

To verify that the implementation is robust, complete the following automated and manual evaluation steps:

1. **Axe DevTools or Lighthouse Audit:** Run automated web accessibility scanners on the compiled Bento grid. Target: **0 accessibility violations found**.
2. **Keyboard-Only Execution:** Attempt to perform a complete document capture and RAG chat interaction using *only* the `Tab`, `Shift + Tab`, `Enter`, `Space`, and `Escape` keys. Verify no focus traps occur in standard views and that the active modal focus trap works correctly.
3. **Screen Reader Live Check:** Turn on VoiceOver (macOS/iOS), TalkBack (Android), or NVDA/Narrator (Windows) and perform a visual scan simulation. Ensure all spoken announcements logically correspond with real-time on-screen transitions.
