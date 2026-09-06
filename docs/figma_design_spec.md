# FireCash — Figma Design Specification

> **Source**: Figma file `Firecash` (Key: `7Sh8y8Jb39DLSKL8fkkBOW`)
> **Device**: iPhone 15/16 Pro (390×874 base, also 390×902 and 390×1186.25 variants)
> **Design System**: iOS-native inspired with Klyro-branded components
> **Last Updated**: 2026-09-06

---

## Overview

The Figma file contains **3 screens** under the "Klyro" brand umbrella, adapted for FireCash:

| Screen | Frame ID | Dimensions | Purpose |
|---|---|---|---|
| Klyro Banking Dashboard | 1:497 | 390×874 | Main home screen — balance card, transactions, bottom nav |
| Klyro Transactions & Analytics | 1:2 | 390×902 | Analytics chart + recent transaction list |
| Klyro All Transactions | 1:231 | 390×1186.25 | Full transaction list with search, filters, date grouping |

---

## 1. Klyro Banking Dashboard (Home Screen)

### 1.1 Shell & Status Bar

- **Bezel**: `#1E232E`, 9px border, rounded 50px, shadow `0px 25px 60px -15px rgba(0,0,0,0.3)`
- **Dynamic Island**: Black pill, 112×24px, centered, rounded-full
- **Status Bar Time**: "9:41", Plus Jakarta Sans SemiBold 15px, `#171717`
- **System Icons**: Cellular signal, Wi-Fi, Battery (black, right-aligned)
- **Home Indicator**: White pill 128×4px, `#171717` 80% opacity, bottom center

### 1.2 App Header

| Element | Position | Details |
|---|---|---|
| FireCash Logo | Top-left, 56×56px | Rendered brand mark (SVG/PNG asset) |
| FireCash Brand Name | Right of logo, 20px Bold | Plus Jakarta Sans Bold, `#171717`, tracking -0.5px |
| Bank/Cash Toggle | Right side, pill shape | "Bank" (active, white bg, `#171717`) / "Cash" (inactive, `#a3a3a3`), rounded-full, 12px text |
| Settings Button | Far right, 40×40px circle | Settings gear icon SVG, `#fafafa` bg, rounded-full |

### 1.3 Virtual Debit Card

**Card Background**: Dark gradient
```
linear-gradient(135deg, #091A3C 0%, #0D2959 30%, #08486F 65%, #056272 100%)
```
- **Border radius**: 24px
- **Shadow**: `0px 20px 40px -15px rgba(16,42,102,0.45)`
- **Glow effects**: Radial gradient blobs (cyan/teal) at top-right and bottom-left

#### Card Content

| Field | Value | Style |
|---|---|---|
| Cardholder Name | "Elanor Quen" | Plus Jakarta Sans Medium 13px, `#cbd5e1`, tracking 0.325px |
| Card Number | "0144-510-2008" | Liberation Mono Regular 12px, `#94a3b8`, tracking 0.6px |
| Decoration Dots | 3 white dots (4px each) | 40% opacity, centered row |
| Label | "Total Balance" | Plus Jakarta Sans Medium 12px, `#cbd5e1` |
| Balance Eye Toggle | 14×14px eye icon | Toggle balance visibility |
| Balance Amount | **$12,840.50** | Plus Jakarta Sans Bold 32px, White, tracking -0.8px |
| Trend Indicator | "+3.4% this month" | Green pill `rgba(23,217,130,0.2)` bg, `#40f1a4` text, 11px, up arrow |
| VISA Logo | Top-right, 24px Bold Italic | Plus Jakarta Sans ExtraBold Italic, White, tracking -1.2px |

#### Card Actions (Bottom Buttons)

| Button | Icon | Label | Style |
|---|---|---|---|
| Scan Slip | QR icon | "Scan Slip" | Frosted glass `rgba(255,255,255,0.1)` bg, 12px white text, rounded-12px |
| Analytics | Chart icon | "Analytics" | Same frosted glass style |

### 1.4 Recent Transactions Section

**Section Header**: "Recent Transactions" — Plus Jakarta Sans Bold 14px, `#171717`
**"View All" link**: Plus Jakarta Sans SemiBold 12px, `#2563eb` (blue), with `>` arrow

#### Transaction Items

| # | Merchant | Icon BG | Amount | Category | Time |
|---|---|---|---|---|---|
| 1 | Spotify | `#1db954` (green) | -$9.99 | Subscriptions | Today, 09:21 AM |
| 2 | Netflix | Black, red "N" | -$15.40 | Entertainment | Yesterday, 06:12 PM |
| 3 | Starbucks | `#006241` (dark green) | -$4.75 | Food & Drink | Today, 08:45 AM |

**Item Layout**:
- 40×40px circular icon (brand color) to 12px gap to merchant name (13px SemiBold, `#171717`) + category (11px Regular, `#a3a3a3`)
- Right side: amount (13px Bold, `#171717`) + timestamp (10px Regular, `#a3a3a3`)
- No dividers, clean spacing

### 1.5 Bottom Navigation (Frosted Glass Dock)

**Container**: `backdrop-blur-[12px]`, `rgba(255,255,255,0.8)` bg, border `rgba(255,255,255,0.6)`, rounded-full, shadow

| Tab | Label | State | Style |
|---|---|---|---|
| Home | Home | **Active** | Pill bg `rgba(245,245,245,0.9)`, 12px SemiBold, `#171717` |
| Cards | Cards | Inactive | 10px Medium, `#a3a3a3` |
| Spending | Spending | Inactive | 10px Medium, `#a3a3a3` |
| Profile | Profile | Inactive | Gradient avatar `#2563eb to #818cf8`, 10px Medium, `#a3a3a3` |

---

## 2. Klyro Transactions & Analytics Screen

### 2.1 Navigation

- **Back button**: Circular 40×40px, border `#e2e8f0`, chevron SVG
- **Title**: "Transactions" — Liberation Sans Bold 16px, `#0f172a`
- **More options**: 40×40px circle, 3-dot SVG

### 2.2 Transaction Overview Card

**Card**: White bg, border `#f1f5f9`, rounded-16px, shadow

#### Header
- "Transaction Overview" — Liberation Sans Bold 14px, `#0f172a`
- **Timeframe selector**: "Aug" pill with dropdown chevron, `#f8fafc` bg, border `#e2e8f0`, rounded-8px
- **Expand chart**: 28×28px square, border `#e2e8f0`, rounded-6px

#### Legend Tabs
- **Week** | **Month** (active) | **Year** — toggle pill group
- Active: white bg, Bold, `#0f172a`
- Inactive: `#f1f5f9` bg, Regular, `#64748b`

#### Legend Indicators
- **Income**: Blue dot `#2b66ff`, label "Income" 11px, `#64748b`
- **Outcome**: Light blue dot `#7dd3fc`, label "Outcome" 11px, `#64748b`

#### Bar Chart (SVG)
- 5 horizontal grid lines (100%, 75%, 50%, 25%, 0%)
- Vertical bars in blue `#2b66ff` for each month
- **X-axis**: JAN, MAR, MAY, JUL, **AUG** (highlighted blue), SEP, DEC

#### Tooltip Popup (Aug 2026)
- `rgba(255,255,255,0.95)` bg, `backdrop-blur-[2px]`, rounded-12px, shadow
- **Header**: "Aug 2026" — Liberation Sans Bold 11px, `#1e293b`
- **Data**: Income 60% (blue dot), Outcome 20% (light blue dot)

### 2.3 Recent Transactions (6 Items)

| Item | Merchant | Icon | Amount | Category | Date |
|---|---|---|---|---|---|
| 1 | Uber Ride | Black bg, "Uber" text | -$18.40 | Transport | Yesterday, 07:30 AM |
| 2 | Spotify | `#1ed760` bg, SVG logo | -$9.99 | Subscriptions | Today, 09:21 AM |
| 3 | Starbucks | `#006241` bg, star SVG | -$4.75 | Food & Drink | Today, 08:45 AM |
| 4 | Netflix | Black bg, red "N" | -$15.40 | Entertainment | Yesterday, 06:12 PM |
| 5 | Adobe | `rgba(250,15,0,0.1)` bg, red border | -$15.40 | Subscriptions | Aug 17, 06:12 PM |
| 6 | Canva | Cyan to purple gradient, "C" | -$9.99 | Subscriptions | Aug 15, 09:21 AM |

**Layout**: 44×44px icons, Liberation Sans Bold 14px merchant name, `#94a3b8` 12px category, right-aligned amounts

---

## 3. Klyro All Transactions Screen

### 3.1 Shell
- **Bezel**: `#0f172a`, 8px border, rounded-48px, shadow
- **Status Bar**: iOS style with time, cellular, WiFi, battery, dynamic island

### 3.2 Navigation
- **Back button**: 40×40px, border `rgba(226,232,240,0.9)`, rounded-full
- **Title**: "All Transactions" — Liberation Sans Bold 18px, `#0f172a`, tracking -0.45px
- **Filter button**: 40×40px, filter icon SVG

### 3.3 Search Bar
- **Container**: `rgba(241,245,249,0.9)` bg, rounded-16px, 40px height
- **Placeholder**: "Search transactions, merchant..." — Liberation Sans Regular 14px, `#94a3b8`
- **Leading icon**: Search magnifying glass SVG (left)
- **Trailing icon**: Voice search microphone SVG (right)

### 3.4 Filter Chips (Horizontal Scroll)

| Chip | State | Style |
|---|---|---|
| All | **Active** | `#0f172a` bg, white text, Bold 12px |
| Income | Default | `#f1f5f9` bg, `#475569` text, Regular 12px |
| Expense | Default | Same as Income |
| Subscriptions | Default | Same as Income |
| Food & Drink | Default | Same as Income |
| Transport | Default | Same as Income |

All chips: rounded-full, 14px horizontal padding, 6px vertical padding

### 3.5 Month Selector & Summary
- **Date badge**: "August 2026" pill with dropdown chevron, `#f1f5f9` bg, Liberation Sans Bold 12px, `#334155`, rounded-12px
- **Total Spent**: "Total Spent" label (11px, `#94a3b8`), "-$2,418.50" (12px Bold, `#0f172a`)

### 3.6 Grouped Transactions

#### Date Group Headers
Format: `"TODAY, 18 AUG 2026"` / `"YESTERDAY, 17 AUG 2026"` / `"15 AUG 2026"`
- Liberation Sans Bold 12px, `#94a3b8`, tracking 0.6px, UPPERCASE
- Right side: daily net total

#### Today (18 Aug 2026)
| Item | Amount | Category | Time | Card |
|---|---|---|---|---|
| Starbucks Coffee | -$4.75 | Food & Drink | 08:45 AM | .. 2008 |
| Spotify | -$9.99 | Subscriptions | 09:21 AM | .. 2008 |
| **Client Transfer** | **+$1,250.00** | Freelance Income | 02:15 PM | Received badge |

#### Yesterday (17 Aug 2026)
| Item | Amount | Category | Time | Card |
|---|---|---|---|---|
| Uber Ride | -$18.40 | Transport | 07:30 AM | .. 2008 |
| Netflix | -$15.40 | Entertainment | 06:12 PM | .. 2008 |
| Adobe Creative | -$54.99 | Subscriptions | 04:20 PM | .. 2008 |

#### 15 Aug 2026
| Item | Amount | Category | Time | Card |
|---|---|---|---|---|
| **Salary Deposit** | **+$4,500.00** | Acme Corp | 09:00 AM | Payroll badge |
| Canva Pro | -$12.99 | Subscriptions | 10:15 AM | .. 2008 |
| Apple Store | -$129.00 | Electronics | 03:40 PM | .. 2008 |
| Whole Foods Market | -$78.35 | Groceries | 05:10 PM | .. 2008 |

**Income items**: Green text `#059669`, green bg `#ecfdf5` "Received"/"Payroll" badge
**Expense items**: Dark text `#0f172a`
**Card suffix**: ".. 2008" in `#94a3b8` 10px

### 3.7 Floating Dock Navigation
Same frosted glass style as Dashboard, but **Spending tab is ACTIVE** (dark pill `#0f172a`, white text "Spending")

---

## 4. Typography

| Usage | Font | Weight | Size | Color |
|---|---|---|---|---|
| Brand Name | Plus Jakarta Sans | Bold | 20px | `#171717` |
| Balance Amount | Plus Jakarta Sans | Bold | 32px | White |
| Cardholder Name | Plus Jakarta Sans | Medium | 13px | `#cbd5e1` |
| Card Number | Liberation Mono | Regular | 12px | `#94a3b8` |
| Section Headers | Liberation Sans | Bold | 14-16px | `#0f172a` |
| Merchant Name | Liberation Sans | Bold | 14px | `#0f172a` |
| Category/Subtext | Liberation Sans | Regular | 11-12px | `#94a3b8` |
| Amount (expense) | Liberation Sans | Bold | 14px | `#0f172a` |
| Amount (income) | Liberation Sans | Bold | 14px | `#059669` |
| Tab Labels | Plus Jakarta Sans | Medium/SemiBold | 10-12px | Varies |
| Chart Labels | Liberation Sans | Regular/Bold | 10px | `#94a3b8` |
| Date Group Header | Liberation Sans | Bold | 12px | `#94a3b8` |
| Mini badge text | Liberation Sans | Bold | 10px | `rgba(5,150,105,0.9)` |
| Search placeholder | Liberation Sans | Regular | 14px | `#94a3b8` |

---

## 5. Color Palette

### Text Colors
| Token | Hex | Usage |
|---|---|---|
| Text Primary | `#171717` | Headings, merchant names, expense amounts |
| Text Secondary | `#a3a3a3` | Tab labels (inactive), timestamps, category |
| Text Muted | `#94a3b8` | Chart labels, date headers, placeholder text |
| Text Card Label | `#cbd5e1` | "Total Balance", cardholder name on card |
| Text Dark | `#0f172a` | Section titles, navigation titles |
| Text Link | `#2563eb` | "View All" link |
| Text Positive | `#059669` | Income amounts, received badges |
| Text Positive Glow | `#40f1a4` | Trend percentage indicator |
| Text White | `#FFFFFF` | Balance, card buttons, active tab |

### Surface Colors
| Token | Hex | Usage |
|---|---|---|
| Card Gradient Start | `#091A3C` | Debit card background |
| Card Gradient Mid | `#0D2959` | Debit card background |
| Card Gradient End | `#056272` | Debit card background |
| Chip Active | `#0f172a` | "All" filter chip, active nav pill |
| Chip Default | `#f1f5f9` | Inactive filter chips |
| Nav Dock | `rgba(255,255,255,0.8)` | Frosted glass bottom nav |
| Search Bar | `rgba(241,245,249,0.9)` | Search input background |
| Status Bar | `#FFFFFF` | iOS status bar background |

### Accent Colors
| Token | Hex | Usage |
|---|---|---|
| Blue | `#2b66ff` | Chart bars, active x-axis label, Income dot |
| Light Blue | `#7dd3fc` | Outcome dot in chart legend |
| Green (trend) | `rgba(23,217,130,0.2)` | Trend pill background |
| Green (received) | `#ecfdf5` | Received/Payroll badge background |
| Red (Adobe) | `rgba(250,15,0,0.1)` | Adobe icon background |

### Brand Icon Colors
| Brand | Background |
|---|---|
| Spotify | `#1db954` / `#1ed760` |
| Netflix | Black |
| Starbucks | `#006241` |
| Uber | Black |
| Adobe | `rgba(250,15,0,0.1)` |
| Canva | Gradient `#00C4CC to #7D2AE8` |
| Apple | Black |
| Whole Foods | `#047857` |

---

## 6. Spacing & Layout

| Element | Value |
|---|---|
| Screen padding | 20px horizontal |
| Card padding | 20px |
| Transaction item padding | 10px |
| Icon size (transactions) | 40×40px / 44×44px |
| Icon-to-text gap | 12px |
| Chip padding | 14px horizontal, 6px vertical |
| Section spacing | 12-16px |
| Status bar height | 34.5px |
| Navigation height | 60px |
| Bottom nav height | 59px |

### Border Radius
| Element | Radius |
|---|---|
| Phone shell | 50px |
| Debit card | 24px |
| Transaction card | 16px |
| Filter chips | 9999px (full pill) |
| Bottom nav | 9999px (full pill) |
| Icons | 9999px (full circle) |
| Search bar | 16px |
| Date badge | 12px |
| Card buttons | 12px |
| Income badge | 4px |

---

## 7. Component Hierarchy

```
App Shell (iPhone 15/16 Pro frame)
+-- iOS Status Bar (Dynamic Island, time, signal, WiFi, battery)
+-- App Header
|   +-- FireCash Logo (56x56)
|   +-- "FireCash" Brand Name
|   +-- Bank/Cash Toggle Pill
|   +-- Settings Button (gear icon)
+-- Main Scrollable Area
|   +-- Virtual Debit Card
|   |   +-- Cardholder Info + VISA Logo
|   |   +-- Decoration Dots
|   |   +-- Balance Display (Total Balance, amount, eye toggle, trend)
|   |   +-- Card Actions (Scan Slip, Analytics)
|   +-- Recent Transactions Section
|       +-- Section Header + "View All" link
|       +-- Transaction Items (icon, name, category, amount, time)
+-- Frosted Glass Bottom Nav
    +-- Home (active)
    +-- Cards
    +-- Spending
    +-- Profile
```

---

## 8. Design Decisions & Notes

1. **Light theme**: The Figma design is a **light theme** iOS-native design, differing from the current Android app's Material 3 dark theme. Adaptation to the Android dark theme will require color mapping.
2. **Font**: The design uses **Plus Jakarta Sans** (Figma default) and **Liberation Sans** — on Android, these should map to Material 3 typography defaults or Google Fonts Jakarta Sans.
3. **Card gradient**: The debit card's dark gradient (`#091A3C -> #056272`) would need adaptation for the app's dark theme.
4. **Bottom nav**: The frosted glass dock style (`backdrop-blur`) is iOS-specific; Android adaptation would use a semi-transparent surface container.
5. **Income/Expense semantics**: Green (`#059669`) for positive/income, dark (`#0f172a`) for expense.
6. **Chart component**: The analytics bar chart is SVG-based in Figma; Android implementation would use a custom Canvas or Compose chart library.
7. **Data is sample**: All transactions (Spotify, Netflix, Starbucks, etc.) are sample data for the design mockup.