# Powerblog - Design System

**Version:** 1.0.0
**Last Updated:** 2026-01-30
**Status:** Master Source of Truth

---

## Overview

This design system provides a comprehensive guide for building the Powerblog application. It follows the **Trust & Authority** style with **Professional Minimal** aesthetics, optimized for blog reading and content consumption.

**Design Philosophy:**
- **Clarity First**: Information hierarchy prioritizes readability
- **Trust & Professionalism**: Navy blue tones convey reliability
- **Efficiency**: Minimal UI clutter for maximum productivity
- **Accessibility**: WCAG AA compliant with 4.5:1 contrast minimum

---

## 1. Color System

### 1.1 Primary Colors (Brand)

| Token | Value | Usage |
|-------|-------|-------|
| `--primary-50` | `#eff6ff` | Hover backgrounds |
| `--primary-100` | `#dbeafe` | Active states, badges |
| `--primary-500` | `#3b82f6` | Links, accents |
| `--primary-600` | `#2563eb` | Primary buttons (default) |
| `--primary-700` | `#1d4ed8` | Primary buttons (hover) |
| `--primary-900` | `#1e3a8a` | Dark mode primary text |

In dark mode, the **light end** of the primary scale (50, 100, 200) is redefined to dark blue tints (e.g. `#1e2a4a`, `#253a5c`, `#2d4a7a`) for hover/active backgrounds on dark surfaces; the rest of the scale stays tuned for accents and text.

### 1.2 Neutral Colors (Gray Scale)

**Light Mode:**
- Backgrounds: `gray-50` (#fafafa) → `gray-100` (#f4f4f5)
- Borders: `gray-200` (#e4e4e7) → `gray-300` (#d4d4d8)
- Text Muted: `gray-500` (#71717a) → `gray-600` (#52525b)
- Text Primary: `gray-900` (#18181b)

**Dark Mode:**
- Backgrounds: `gray-900` (#18181b) → `gray-950` (#09090b)
- Borders: `gray-700` (#3f3f46) → `gray-800` (#27272a)
- Text Muted: `gray-400` (#52525b) → `gray-500` (#71717a)
- Text Primary: `gray-50` (#fafafa)

**Semantic border:** Use `border-border-default` (or `divide-border-default`) for card outlines, section dividers, table borders, and sidebar edges. This token is **not** inverted in dark mode: light = gray-300, dark = #3f3f46, so borders stay visible in both themes. Prefer it over `border-gray-200` / `border-gray-300` so one class works correctly in dark mode.

### 1.3 Semantic Colors

| Role | Light | Dark | Usage |
|------|-------|------|-------|
| **Success** | `#22c55e` | `#4ade80` | Approved, completed, online |
| **Warning** | `#f59e0b` | `#fbbf24` | Pending, attention needed |
| **Error** | `#ef4444` | `#f87171` | Failed, rejected, offline |
| **Info** | `#0ea5e9` | `#38bdf8` | Neutral information |
| **Border default** | `border-border-default` | Same class in dark | Card/table/section borders, dividers |

### 1.4 Dark Mode Implementation (Token Overwrite)

Dark mode uses **token overwriting** on `<html class="dark">` (not `html { ... }`), so `.dark` has higher specificity than `:root` and always wins.

- **Complete coverage:** Every color token defined in `:root` must be redefined in `.dark`. Unset tokens inherit from `:root` and can cause contrast violations.
- **Accent tuning:** On dark backgrounds, primary/accent colors (primary-300 and up) should be slightly lighter (~5–15%) and less saturated (~10–20%) so they don't vibrate. The **light end** of primary (50, 100, 200) is redefined to dark blue tints for hover/active backgrounds on dark surfaces.
- **Shadows:** Dark mode shadow tokens use higher opacity (e.g. 0.3–0.4) because there's less ambient light.
- **Gray scale:** The same gray scale is shared; surfaces use `--surface-sidebar` / `--surface-page` (e.g. gray-900/950 in dark). No need to redefine gray in `.dark` unless you want different hex values.

**Do you need Tailwind `dark:`?** For most cases, **no**. Having `<html class="dark">` is enough: any class that uses a **token** we override in `.dark` (e.g. `bg-surface-page`, `text-primary-500`, `bg-success-100`, `border-gray-200`) automatically gets the dark value because the CSS variable changes. Use **one class** and rely on the root `.dark` switch. Use Tailwind `dark:` only for **one-off cases** not covered by tokens.

### 1.5 Color Usage Rules

```clojure
;; DO: Use semantic colors for status
[:div {:class "text-success-500"} "Online"]
[:div {:class "text-error-500"} "Failed"]

;; DON'T: Use arbitrary colors
[:div {:style {:color "#22c55e"}} "Online"]
```

**Contrast Requirements:**
- Body text: Minimum 4.5:1 contrast ratio
- Large text (18px+): Minimum 3:1 contrast ratio
- Interactive elements: Minimum 3:1 contrast ratio

---

## 2. Typography

### 2.1 Font Family

```css
--font-sans: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
```

### 2.2 Type Scale

| Size | Rem | Px | Weight | Usage |
|------|-----|----|--------|-------|
| `xs` | 0.75 | 12 | Normal/500 | Labels, captions |
| `sm` | 0.875 | 14 | Normal/500 | Secondary text |
| `base` | 1 | 16 | Normal/500 | Body text |
| `lg` | 1.125 | 18 | Normal/600 | Subheadings |
| `xl` | 1.25 | 20 | 600 | Cards headers |
| `2xl` | 1.5 | 24 | 600 | Section headers |
| `3xl` | 1.875 | 30 | 700 | Page titles |

### 2.3 Typography Patterns

```clojure
;; Page Title
[:h1 {:class "text-3xl font-bold text-gray-900"}
  "Blog Posts"]

;; Section Header
[:h2 {:class "text-xl font-semibold text-gray-900"}
  "Recent Articles"]

;; Card Header
[:h3 {:class "text-lg font-semibold text-gray-900"}
  "Article Details"]

;; Body Text
[:p {:class "text-base text-gray-600"}
  "Regular paragraph text goes here."]

;; Secondary/Muted Text
[:span {:class "text-sm text-gray-500"}
  "2 hours ago"]

;; Label
[:label {:class "text-sm font-medium text-gray-700"}
  "Email Address"]
```

### 2.4 Line Heights

| Context | Value | Class |
|---------|-------|-------|
| Headings | 1.25 | `leading-tight` |
| Body text | 1.5 | `leading-normal` |
| Dense text | 1.375 | `leading-snug` |

---

## 3. Spacing System

### 3.1 Spacing Scale

Based on 4px base unit:

| Token | Rem | Px | Usage |
|-------|-----|----|-------|
| `1` | 0.25 | 4 | Tight spacing, icon gaps |
| `2` | 0.5 | 8 | Compact padding |
| `3` | 0.75 | 12 | Small gaps |
| `4` | 1 | 16 | Default padding, card gaps |
| `5` | 1.25 | 20 | Section spacing |
| `6` | 1.5 | 24 | Component spacing |
| `8` | 2 | 32 | Large sections |
| `10` | 2.5 | 40 | Page sections |

### 3.2 Component Spacing Patterns

```clojure
;; Card internal spacing
[:div {:class "p-6 space-y-4"}]

;; Form field spacing
[:div {:class "space-y-4"}]

;; List item spacing
[:ul {:class "space-y-2"}]

;; Grid gaps
[:div {:class "grid gap-6"}]
```

### 3.3 Layout Containers

```clojure
;; Page container
[:div {:class "container mx-auto px-4 md:px-6 lg:px-8 max-w-7xl"}]

;; Section spacing
[:section {:class "py-8 md:py-12 lg:py-16"}]
```

---

## 4. Component Styles

### 4.1 Buttons

**Primary Button:**
```clojure
[:button {:class "inline-flex items-center justify-center gap-2
           px-4 py-2 bg-primary-600 hover:bg-primary-700 active:bg-primary-800
           text-white font-medium rounded-md
           transition-colors duration-200
           focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
           disabled:bg-gray-300 disabled:cursor-not-allowed"}
  "Submit"]
```

**Secondary Button:**
```clojure
[:button {:class "inline-flex items-center justify-center gap-2
           px-4 py-2 bg-surface-elevated
           border border-gray-300
           hover:bg-gray-50
           text-gray-700 font-medium rounded-md
           transition-colors duration-200
           focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
           disabled:opacity-50 disabled:cursor-not-allowed"}
  "Cancel"]
```

**Ghost/Text Button:**
```clojure
[:button {:class "inline-flex items-center gap-2
           px-3 py-1.5 text-primary-600 hover:text-primary-700
           hover:bg-primary-50
           font-medium rounded-md
           transition-colors duration-200
           focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2"}
  "Learn More"]
```

**Icon Button:**
```clojure
[:button {:class "p-2 text-gray-500 hover:text-gray-700
           hover:bg-gray-100 rounded-md
           transition-colors duration-200
           focus:outline-none focus:ring-2 focus:ring-primary-500"
          :aria-label "Settings"}
  [:svg {:class "w-5 h-5"} "..."]]
```

### 4.2 Cards

**Default Card:**
```clojure
[:div {:class "bg-surface-elevated
       rounded-lg border border-gray-200
       p-6 shadow-sm"}]
```

**Hoverable Card:**
```clojure
[:div {:class "bg-surface-elevated
       rounded-lg border border-gray-200
       p-6 shadow-sm
       hover:shadow-md hover:border-gray-300
       transition-all duration-200 cursor-pointer"}]
```

**Card Header:**
```clojure
[:div {:class "bg-surface-elevated rounded-lg border border-gray-200"}
  [:div {:class "px-6 py-4 border-b border-gray-200"}
    [:h3 {:class "text-lg font-semibold"} "Card Title"]]
  [:div {:class "p-6"}
    ;; Card content
    ]]
```

### 4.3 Badges

**Status Badges:**
```clojure
[:span {:class "inline-flex items-center gap-1.5 px-2.5 py-1
         text-xs font-medium rounded-full
         bg-success-100 text-success-700"}
  [:span {:class "w-1.5 h-1.5 rounded-full bg-success-500"}]
  "Online"]

[:span {:class "inline-flex items-center gap-1.5 px-2.5 py-1
         text-xs font-medium rounded-full
         bg-warning-100 text-warning-700"}
  "Pending"]
```

**Count Badge:**
```clojure
[:span {:class "inline-flex items-center justify-center
         min-w-[1.25rem] h-5 px-1.5
         text-xs font-medium rounded-full
         bg-gray-100 text-gray-600"}
  "5"]
```

### 4.4 Tables

```clojure
[:div {:class "overflow-x-auto"}
  [:table {:class "w-full"}
    [:thead
      [:tr {:class "border-b border-gray-200"}
        [:th {:class "px-4 py-3 text-left text-sm font-semibold text-gray-900"}
          "Name"]]]
    [:tbody {:class "divide-y divide-gray-200"}
      [:tr {:class "hover:bg-gray-50"}
        [:td {:class "px-4 py-3 text-sm text-gray-700"}
          "John Doe"]]]]]
```

**Mobile / narrow viewports:** Avoid horizontal scroll on small screens. Prefer:

- **Card list:** Below a breakpoint (e.g. `md`), render each row as a **card**
- **Reduced columns:** Show only 2–3 essential columns on mobile
- **Sticky primary action:** Make the first column and the actions column sticky

### 4.5 Navigation

**Top Nav Active State:**
```clojure
[:button {:class "px-3 py-4 text-sm font-medium
          text-primary-600 border-b-2 border-primary-600
          transition-colors"}]
```

**Sidebar Active Item:**
```clojure
[:button {:class "w-full flex items-center gap-2 px-3 py-2 rounded-md
          bg-primary-50 text-primary-600
          text-sm font-medium transition-colors"}]
```

**Sidebar Inactive Item:**
```clojure
[:button {:class "w-full flex items-center gap-2 px-3 py-2 rounded-md
          text-gray-700 hover:bg-gray-50
          text-sm font-medium transition-colors"}]
```

---

## 5. Layout Patterns

### 5.1 Responsive Breakpoints

| Breakpoint | Min Width | Usage |
|------------|-----------|-------|
| `sm` | 640px | Small tablets |
| `md` | 768px | Tablets |
| `lg` | 1024px | Small laptops |
| `xl` | 1280px | Desktops |
| `2xl` | 1536px | Large screens |

### 5.2 Dashboard Layout

```clojure
;; Main App Layout
[:div {:class "flex h-screen overflow-hidden"}
  ;; Sidebar
  [:aside {:class "layout-sidebar bg-surface-sidebar border-r border-border-default"}]
  
  ;; Main Content
  [:div {:class "flex-1 flex flex-col overflow-hidden"}
    ;; Top Navigation
    [:nav {:class "h-14 shrink-0 bg-gray-50 border-b border-gray-200 layout-nav-padding"}]
    
    ;; Scrollable Content
    [:main {:class "flex-1 overflow-y-auto layout-main"}
      ;; Page content
      ]]]
```

### 5.3 Grid Patterns

```clojure
;; 2 Column Grid
[:div {:class "layout-grid-2"}]

;; 3 Column Grid
[:div {:class "layout-grid-3"}]

;; 4 Column Grid
[:div {:class "layout-grid-4"}]

;; Auto-fit Grid (one-off)
[:div {:class "grid grid-cols-[repeat(auto-fit,minmax(250px,1fr))] gap-6"}]
```

---

## 6. States & Interactions

### 6.1 Focus States

All interactive elements must have visible focus states:

```clojure
;; Ring Focus
{:class "focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2"}

;; Simple Focus for Buttons
{:class "focus:outline-none focus:ring-2 focus:ring-primary-500"}
```

### 6.2 Hover States

```clojure
;; Color Change
{:class "text-gray-600 hover:text-primary-600 transition-colors duration-200"}

;; Background Change
{:class "hover:bg-gray-50 transition-colors duration-200"}

;; Shadow Change (Cards)
{:class "shadow-sm hover:shadow-md transition-shadow duration-200"}
```

### 6.3 Disabled States

```clojure
;; Disabled Button
[:button {:disabled true
          :class "bg-gray-300 cursor-not-allowed
                  disabled:opacity-50 disabled:cursor-not-allowed"}]

;; Disabled Input
[:input {:disabled true
         :class "bg-gray-100 cursor-not-allowed
                 disabled:bg-gray-100 disabled:cursor-not-allowed"}]
```

### 6.4 Loading States

```clojure
;; Loading Button
[:button {:disabled true
          :class "flex items-center gap-2 opacity-75 cursor-wait"}
  [:svg {:class "animate-spin h-4 w-4" :viewBox "0 0 24 24"}
    [:circle {:class "opacity-25" :cx "12" :cy "12" :r "10" 
              :stroke "currentColor" :stroke-width "4" :fill "none"}]
    [:path {:class "opacity-75" :fill "currentColor"
            :d "M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"}]]
  "Loading..."]
```

---

## 7. Animations

### 7.1 Duration Standards

| Type | Duration | Class |
|------|----------|-------|
| Micro-interactions | 150ms | `duration-150` |
| Standard transitions | 200ms | `duration-200` |
| Slow transitions | 300ms | `duration-300` |

### 7.2 Easing Functions

```clojure
;; Default (ease-in-out)
{:class "transition-colors duration-200"}

;; Custom easing
{:class "transition-all duration-200 ease-out"}
```

### 7.3 Common Transitions

```clojure
;; Color Transition (Recommended)
{:class "transition-colors duration-200"}

;; Transform/Scale (Use sparingly)
{:class "transition-transform duration-200"}

;; All Properties (Use sparingly)
{:class "transition-all duration-200"}
```

### 7.4 Reduced Motion

```css
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
```

---

## 8. Shadows & Elevation

### 8.1 Shadow Scale

| Level | Class | Usage |
|-------|-------|-------|
| None | `shadow-none` | Flat elements |
| Small | `shadow-sm` | Cards, panels |
| Medium | `shadow-md` | Dropdowns, hover cards |
| Large | `shadow-lg` | Modals, popovers |
| XL | `shadow-xl` | Toast notifications |

### 8.2 Dark Mode Shadows

In dark mode, shadow tokens are redefined in `.dark` (stronger opacity). Use `shadow-md`; it resolves via token.

---

## 9. Border Radius

| Size | Class | Usage |
|------|-------|-------|
| None | `rounded-none` | Tables, grid items |
| Small | `rounded-sm` | Input groups |
| Default | `rounded` | Cards, buttons |
| Medium | `rounded-md` | Inputs, badges |
| Large | `rounded-lg` | Modals, panels |
| Full | `rounded-full` | Avatars, pills |

---

## 10. Accessibility Guidelines

### 10.1 Color Contrast

- **Body text:** Minimum 4.5:1
- **Large text (18px+):** Minimum 3:1
- **Interactive elements:** Minimum 3:1

### 10.2 Focus Indicators

All interactive elements must have visible focus states:
```clojure
{:class "focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2"}
```

### 10.3 Touch Targets

Minimum 44x44px for touch interactions:
```clojure
[:button {:class "min-h-[44px] min-w-[44px]"}]
```

### 10.4 Screen Reader Support

```clojure
;; Icon Buttons
[:button {:aria-label "Close dialog"}
  [x-icon]]

;; Live Regions
[:div {:aria-live "polite" :aria-atomic "true"}
  status-message]

;; Hidden Labels
[:label {:for "search" :class "sr-only"} "Search"]
```

### 10.5 Keyboard Navigation

- Tab order must match visual order
- All functionality accessible via keyboard
- No keyboard traps

---

## 11. Anti-Patterns (What to Avoid)

### Visual

❌ **Don't use emojis as icons**
```clojure
;; BAD
[:button "🔔"]

;; GOOD
[:button [bell-icon {:class "w-5 h-5"}]]
```

❌ **Don't use scale on hover (causes layout shift)**
```clojure
;; BAD
{:class "hover:scale-105"}

;; GOOD
{:class "hover:bg-gray-50"}
```

❌ **Don't use invisible borders in light mode**
```clojure
;; BAD
{:class "border-white/10"}

;; GOOD (token switches in .dark)
{:class "border-gray-200"}
```

### Interaction

❌ **Don't leave default cursor on clickable elements**
```clojure
;; BAD
[:div {:class "hover:bg-gray-50"}]

;; GOOD
[:div {:class "hover:bg-gray-50 cursor-pointer"}]
```

❌ **Don't use instant transitions (jarring)**
```clojure
;; BAD
{:class "transition-colors duration-75"}

;; GOOD
{:class "transition-colors duration-200"}
```

### Dark Mode

❌ **Don't use transparent backgrounds in light mode**
```clojure
;; BAD
{:class "bg-white/10"}

;; GOOD (use token)
{:class "bg-surface-elevated"}
```

❌ **Don't use light text on light backgrounds**
```clojure
;; BAD
{:class "text-gray-400"}

;; GOOD - use token; contrast follows .dark
{:class "text-gray-600"}
```

---

## 12. Component Checklist

Before shipping any component, verify:

### Visual Quality
- [ ] No emojis used as icons (use SVG)
- [ ] All icons from consistent icon set
- [ ] Hover states don't cause layout shift
- [ ] Proper contrast ratios (4.5:1 minimum)

### Interaction
- [ ] All clickable elements have `cursor-pointer`
- [ ] Hover states provide clear visual feedback
- [ ] Transitions are smooth (150-300ms)
- [ ] Focus states visible for keyboard navigation

### Light/Dark Mode
- [ ] Light mode text has sufficient contrast
- [ ] Glass/transparent elements visible in light mode
- [ ] Borders visible in both modes
- [ ] Tested in both themes

### Layout
- [ ] Floating elements have proper spacing from edges
- [ ] No content hidden behind fixed elements
- [ ] Responsive at 375px, 768px, 1024px, 1440px
- [ ] No horizontal scroll on mobile

### Accessibility
- [ ] All images have alt text
- [ ] Form inputs have labels
- [ ] Color is not the only indicator
- [ ] `prefers-reduced-motion` respected

---

## 13. Resources

### Icon Libraries
- [Heroicons](https://heroicons.com/) - Recommended
- [Lucide](https://lucide.dev/) - Alternative option

### Font
- [Inter Font](https://fonts.google.com/share?selection.family=Inter:wght@300;400;500;600;700)

### Color Contrast Checker
- [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/)

### Accessibility Guidelines
- [WCAG 2.1 AA](https://www.w3.org/WAI/WCAG21/quickref/?currentsidebar=%23col_customize&levels=aaa)

---

**End of Master Design System**
