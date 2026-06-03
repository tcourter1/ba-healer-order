# BA Healer Utilities

BA Healer Utilities is a RuneLite Plugin Hub plugin for Barbarian Assault.

It provides visual tracking for Penance Healers, including spawn-order labeling, time-based labeling, configurable highlighting, healer food-count tracking, healer-code support, stack handling, and right-click menu labels.

## Features

* Assigns each Penance Healer a spawn-order number.
* Displays optional labels above each tracked healer.
* Supports spawn-order labels or time-based BA healer labels.
* Supports configurable highlight styles: none, tile, or hull.
* Preserves assigned healer numbers when healers leave and re-enter render distance.
* Tracks successful poisoned-food usage by healer number.
* Displays an optional semi-transparent food-count panel.
* Optionally displays each healer’s food count directly on the NPC.
* Supports count-up and count-down food display modes.
* Supports configurable healer roles and healer-code lists.
* Supports time-based healer-code progression for expected food values.
* Supports optional healer target labels from healer-code entries.
* Supports stack handling by spreading labels when multiple healers occupy the same tile.
* Optionally adds healer labels and codes to the right-click menu.
* Supports configurable highlight color, label color, label size, food-count color, food-count size, food-count height, stacked-label spacing, and menu label visibility.
* Supports option to prevent using poisoned food on anything but a Penance Healer.
* Includes optional healer dispenser menu helpers for highlighting the called food, hiding Take-Vial, and moving Take-Meat above Walk here and Examine.

## Food Count Tracking

Food counts are based on inventory consumption after using poisoned food on a tracked Penance Healer.

The plugin does not count every menu click as a successful feed. Instead, it tracks when a poisoned food item is actually consumed from the inventory after being used on a tracked healer. Wrong-food attempts are ignored when the Barbarian Assault wrong-food penalty message appears.

Food counts can also sync with the selected healer-code entries. When expected food values are available, BA Healer Utilities can color-code food counts based on each healer’s progress:

* **Not Started**: No successful food has been fed yet.
* **In Progress**: Some food has been fed, but the expected amount has not been reached.
* **Complete**: The expected food count has been reached or exceeded.

This makes it easier to quickly see which healers still need food and which ones are already complete.


## Labeling

The plugin can label healers using either simple spawn-order numbering or time-based BA healer labels.

Spawn-order labels display healers as:

```text
1, 2, 3, 4...
```

Time-based labels display healers using common BA timing labels, such as:

```text
6, 12, 18, 24, R1, R2...
```
Healer labels can be displayed in their right-click menu as well.

<img width="517" height="271" alt="image" src="https://github.com/user-attachments/assets/8663be8f-923b-408e-a0db-32232083a0ad" />

Labels can also be disabled entirely.

## Highlighting

Tracked healers can be highlighted common highlight styles.


## Healer Codes and Roles

BA Healer Utilities includes a healer-code system for managing expected food counts and healer targets across Barbarian Assault waves.

Healer codes now live in a dedicated side panel instead of being managed only through plugin configuration. From this panel, users can select wave-specific codes, save custom presets, clear current selections, and manage their active healer-code setup more easily.

The plugin also supports importing and exporting healer-code presets as JSON. This makes it easier to share code setups with teammates or back up custom configurations.

Pre-loaded presets are available for waves 4 through 10, giving users a ready-to-use starting point for common healer-code setups.

Healer-code entries can include time-based progression. Additional lines represent later wave timing increments, allowing the plugin to update expected food counts as the wave progresses.

Target labels can be included in healer-code entries using parentheses. When present, the plugin can display the active target label above the healer.

<img width="237" height="534" alt="image" src="https://github.com/user-attachments/assets/27866011-b816-40d2-b968-93f83c3465bb" />
<img width="218" height="394" alt="image" src="https://github.com/user-attachments/assets/d686fb70-9243-4f35-8344-c0e85fd3aa5c" />


The active code can also be displayed in the Penance Healers' right-click menu.

<img width="413" height="249" alt="image" src="https://github.com/user-attachments/assets/4f12e807-1992-4530-b050-381c0448e283" />


# Demonstrations

_A player satisfies the requirement of a code at or after the correct time:_

https://github.com/user-attachments/assets/959d4924-c2b3-41bb-8b4c-5b3a66208a2f

_A player uses the correct amount of food, but too early:_

https://github.com/user-attachments/assets/2a67d1fe-7bb6-4b26-ae4a-116e5f12f73b




## Stack Handling

When multiple Penance Healers occupy the same tile, BA Healer Utilities can horizontally spread their labels so the healer numbers are easier to read.

This helps prevent stacked healers from hiding each other’s labels.

## Right-Click Menu Labels

BA Healer Utilities can optionally add the tracked healer label and/or code to Penance Healers in the right-click menu.

This makes it easier to identify the correct healer when multiple healers are close together or stacked.

## Dispenser Options

The Dispenser Options section includes optional helpers for the Healer item machine menu:

* **Highlight Called Food** highlights the correct Take option based on the current defender call.
* **Remove Take-Vial** removes the Take-Vial option from the menu.
* **Move Take-Meat up** keeps Take-Meat with the other food options, above Walk here and Examine.



## Credits

Thanks to Brehski/vibecodeandy and Lyelt/Not Bad for contributing improvements and helping a ton with the project.

## Notes

BA Healer Utilities does not automate gameplay. It provides visual tracking for Penance Healer spawn order, labels, highlighting, healer-code progression, and poisoned-food usage.

## Version History

* **1.0.0** - Added basic healer highlighting, numbering, and food-fed tracking.
* **1.0.2** - Added food-fed counts directly on Healer NPCs and text configuration options.
* **1.0.3** - Added time-based numbering option for healer labels.
* **1.0.4** - Added healer-code configuration and healer roles.
* **1.0.5** - Added stack handling logic, highlight style options, label display options, and config cleanup.
* **1.0.6** - Added more copy/paste friendly meta healer code support, healer target labels, and optional right-click menu labels. Added true-tile option to Healer highlighting config.
* **1.0.7** - Lyelt added a fully redesigned healer code system complete with import/export/saving functions. Food count tracking updated with color-coding to track healer code progress. Updated the labeling logic to label all healers by Index ID sequentially, rather than by render order. Labels also self correct when labeled incorrectly on render. 
* **1.0.8** - Added healer-only poisoned food menu protection. Added option to show healer codes in the right-click menu on tagged healers. Added dispenser menu options. Healer code side panel updated visually and import/export/saving function redesigned to work more reliably.
* **1.0.9** - Added function to disable most features when you're not in the Healer role. Added option to instantly hide dead NPCs (skip death animation, similar to Entity Hider), including Penance Healers and/or Penance Healers/Fighters/Rangers.