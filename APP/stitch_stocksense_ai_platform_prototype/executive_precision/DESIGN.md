---
name: Executive Precision
colors:
  surface: '#f8f9ff'
  surface-dim: '#cbdbf5'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e5eeff'
  surface-container-high: '#dce9ff'
  surface-container-highest: '#d3e4fe'
  on-surface: '#0b1c30'
  on-surface-variant: '#45464d'
  inverse-surface: '#213145'
  inverse-on-surface: '#eaf1ff'
  outline: '#76777d'
  outline-variant: '#c6c6cd'
  surface-tint: '#565e74'
  primary: '#000000'
  on-primary: '#ffffff'
  primary-container: '#131b2e'
  on-primary-container: '#7c839b'
  inverse-primary: '#bec6e0'
  secondary: '#0051d5'
  on-secondary: '#ffffff'
  secondary-container: '#316bf3'
  on-secondary-container: '#fefcff'
  tertiary: '#000000'
  on-tertiary: '#ffffff'
  tertiary-container: '#002114'
  on-tertiary-container: '#069669'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dae2fd'
  primary-fixed-dim: '#bec6e0'
  on-primary-fixed: '#131b2e'
  on-primary-fixed-variant: '#3f465c'
  secondary-fixed: '#dbe1ff'
  secondary-fixed-dim: '#b4c5ff'
  on-secondary-fixed: '#00174b'
  on-secondary-fixed-variant: '#003ea8'
  tertiary-fixed: '#85f8c4'
  tertiary-fixed-dim: '#68dba9'
  on-tertiary-fixed: '#002114'
  on-tertiary-fixed-variant: '#005137'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e4fe'
typography:
  headline-xl:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-xl-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.015em
  headline-lg:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.015em
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.03em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1.5rem
  gutter-mobile: 1rem
  margin: 2rem
  margin-mobile: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style
The design system targets enterprise logistics managers, supply chain executives, and inventory directors. It conveys absolute operational certainty, real-time intelligence, and decisive action through a Corporate / Modern design aesthetic tuned for high-density analytical utility. 

The visual style prioritizes crisp structure, high-legibility typographic hierarchies, and functional color coding. It replaces decorative ornamentation with clear architectural layers, structured micro-surfaces, and clear data density that remains readable under intense operational pressure.

## Colors
The color architecture relies on a structured hierarchy engineered for high-density scanning:

- **Primary Canvas & Dominant Surfaces**: Deep Navy (`#0F172A`) anchors navigation rails, master headers, and critical high-contrast framing. Secondary navy tiers (`#1E293B`) establish grouped panels and structural controls.
- **Accents & Focus**: Electric Blue (`#2563EB` primary active, `#3B82F6` hover/subtle focus) provides unambiguous affordance for primary CTAs, active filter triggers, and interactive telemetry data points.
- **Semantic Status Signals**:
  - **Success (In-Stock / Optimal Velocity)**: Emerald Green (`#059669` base, `#10B981` light tint for surface badges).
  - **Warning (Low Stock / Reorder Point)**: Amber (`#D97706` base, `#F59E0B` highlight).
  - **Danger (Stockout / System Failure)**: Crimson Red (`#DC2626` base, `#EF4444` indicator text).
- **Neutrals & Surfaces**: Backgrounds use crisp off-whites (`#F8FAFC`) with card containers elevated via pure white (`#FFFFFF`) and bordered by slate lines (`#E2E8F0` resting, `#CBD5E1` active). Body copy relies on Slate (`#334155` primary text, `#64748B` supporting labels).

## Typography
Inter delivers utilitarian precision across both desktop workstations and handheld warehouse terminal screens. Numeric inventory counts and metric chips must consistently leverage tabular figure alignments (`font-feature-settings: "tnum" on, "cv05" on`) to eliminate jitter during real-time data refreshes.

All body copy prioritizes optical balance and tight vertical cadence. Uppercase transformations are strictly reserved for `label-sm` metadata flags, table header categories, and compact tracking states.

## Layout & Spacing
A rigorous 8-point base rhythm governs all layout structures:

- **Desktop (1200px+)**: 12-column fluid grid with `gutter` set to `1.5rem` (24px) and outer `margin` set to `2rem` (32px). Maximum container width is constrained to `1600px` for optimal field-of-view monitoring across wide enterprise displays.
- **Tablet (768px - 1199px)**: 8-column layout with `gutter` of `1rem` (16px) and canvas margin of `1.5rem` (24px). Analytical sidebars collapse into overlay slideouts.
- **Mobile (< 768px)**: 4-column flow with `gutter-mobile` and `margin-mobile` at `1rem` (16px). Metrics panels reflow from multi-column rows into horizontal swipe rails or vertically stacked mini-cards.

## Elevation & Depth
Visual hierarchy relies on structural tonal separation combined with subtle, high-spread ambient shadows rather than harsh drop shadows. 

- **Level 0 (Base Canvas)**: Neutral tint (`#F8FAFC`), strictly 0px elevation.
- **Level 1 (Card & Module Surfaces)**: Pure white background (`#FFFFFF`) with a 1px border (`#E2E8F0`) and an ambient shadow: `0 1px 3px 0 rgba(15, 23, 42, 0.05), 0 1px 2px -1px rgba(15, 23, 42, 0.03)`.
- **Level 2 (Hover States, Data Popovers & Dropdowns)**: Ambient shadow: `0 4px 6px -1px rgba(15, 23, 42, 0.08), 0 2px 4px -2px rgba(15, 23, 42, 0.04)`.
- **Level 3 (Modals & Command Palettes)**: High-altitude ambient shadow: `0 20px 25px -5px rgba(15, 23, 42, 0.1), 0 8px 10px -6px rgba(15, 23, 42, 0.04)`, paired with a tinted backdrop overlay (`#0F172A` at 40% opacity).

## Shapes
The system implements roundedness level `2` across standard elements, providing approachable modernity while retaining structural enterprise discipline:

- **Primary Cards & Panels**: Default to `12px` (`rounded-md`) to frame information cleanly without wasting display real estate.
- **Parent Modals & Hero Data Blocks**: Scale to `16px` (`rounded-lg`).
- **Interactive Micro-Components (Buttons, Form Inputs, Segmented Controls)**: Fixed at `8px` (`rounded-base`) for balance between sharp utility and tactile feedback.
- **Badges, Tags, & Status Indicators**: Implement full pill styling (`9999px`) to visually differentiate metadata chips from actionable rectangular surfaces.

## Components

### Buttons & Trigger Controls
- **Primary**: Solid background in Deep Navy (`#0F172A`) or Electric Blue (`#2563EB`) with crisp white text, 8px border radius, 40px height for desktop (`36px` compact data mode). Hover states shift lightness up by 8%.
- **Secondary**: Surface-white background, 1px border (`#CBD5E1`), text `#1E293B`.
- **Ghost/Icon Action**: Transparent background with hover fill (`#F1F5F9`), maintaining strict 1:1 square bounding boxes.

### Metric Chips & Status Badges
- **Metric Chips**: Elevated micro-surfaces displaying dynamic KPI increments. Structured with a top-aligned `label-sm` category descriptor, followed by a bold `headline-sm` count, bounded by a 1px border with a soft background tint (`#F8FAFC`).
- **Status Badges**: Pill-shaped badges using 10% semantic color fills with bold text:
  - In Stock: Background `#ECFDF5`, text `#065F46`.
  - Low Stock: Background `#FFFBEB`, text `#92400E`.
  - Critical/Out: Background `#FEF2F2`, text `#991B1B`.

### Tabs & Navigation Switches
Underline tabs use a 2px active border indicator (`#2563EB`) with active label bolding (`label-lg`), aligned directly above component dividers. Segmented pill switchers sit inside an encased container (`#F1F5F9`) with active items elevated via white card background and Level 1 elevation.

### Data Tables & List Groups
Alternating zebra stripes are prohibited; hierarchy is established via 1px horizontal dividers (`#F1F5F9`). Headers use `label-md` uppercase text in Slate (`#64748B`) against a light neutral background (`#F8FAFC`). Row items transition to `#F8FAFC` on hover with a quick 150ms ease.

### Form Inputs & Filters
Inputs maintain a height of 40px, 8px corner radius, 1px border (`#CBD5E1`), and interior padding of 12px. Focus shifts border to `#2563EB` accompanied by a 3px outer ring tinted at 15% opacity (`rgba(37, 99, 235, 0.15)`).

### Timeline Trackers & Supply Chain Audits
Linear tracking components utilize a 2px vertical or horizontal connector line (`#E2E8F0`). Completed checkpoints use solid emerald nodes (`#059669`) with white vector checkmarks; active pending checkpoints display a pulsating Electric Blue halo (`#3B82F6`); scheduled future checkpoints rest in Slate (`#CBD5E1`).