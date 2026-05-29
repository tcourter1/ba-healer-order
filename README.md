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
* Optionally adds healer labels to the right-click menu.
* Supports configurable highlight color, label color, label size, food-count color, food-count size, food-count height, stacked-label spacing, and menu label visibility.

## Food Count Tracking

Food counts are based on inventory consumption after using poisoned food on a tracked Penance Healer.

The plugin does not count every menu click as a successful feed. Instead, it tracks when a poisoned food item is actually consumed from the inventory after being used on a tracked healer. Wrong-food attempts are ignored when the Barbarian Assault wrong-food penalty message appears.

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

Labels can also be disabled entirely.

## Highlighting

Tracked healers can be highlighted common highlight styles.

## Food Count Panel

The food-count panel lists each tracked healer by spawn order and shows how many poisoned food items have been successfully consumed for that healer.

## Healer Codes and Roles

BA Healer Utilities supports configurable healer roles and wave-based healer-code lists. These settings can be used to display expected food counts for each healer based on the selected role and wave configuration.

Healer-code entries can also include time-based progression. Additional lines represent later wave timing increments, allowing the plugin to update expected food counts as the wave progresses.

Target labels can be included in healer-code entries using parentheses. When present, the plugin can display the active target label above the healer.

## Stack Handling

When multiple Penance Healers occupy the same tile, BA Healer Utilities can horizontally spread their labels so the healer numbers are easier to read.

This helps prevent stacked healers from hiding each other’s labels.

## Right-Click Menu Labels

BA Healer Utilities can optionally add the tracked healer label to Penance Healers in the right-click menu.

This makes it easier to identify the correct healer when multiple healers are close together or stacked.

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
