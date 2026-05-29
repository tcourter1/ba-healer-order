\# BA Healer Utilities



BA Healer Utilities is a RuneLite Plugin Hub plugin for Barbarian Assault.



It provides visual tracking for Penance Healers, including spawn-order labeling, time-based labeling, configurable highlighting, healer food-count tracking, and healer-code support.



\## Features



\* Assigns each Penance Healer a spawn-order number.

\* Displays optional labels above each tracked healer.

\* Supports spawn-order labels or time-based BA healer labels.

\* Supports configurable highlight styles: none, tile, or hull.

\* Preserves assigned healer numbers when healers leave and re-enter render distance.

\* Tracks successful poisoned-food usage by healer number.

\* Displays an optional semi-transparent food-count panel.

\* Optionally displays each healer’s food count directly on the NPC.

\* Supports count-up and count-down food display modes.

\* Supports configurable healer roles and healer-code lists.

\* Supports stack handling by spreading labels when multiple healers occupy the same tile.

\* Supports configurable highlight color, label color, label size, food-count color, food-count size, food-count height, and stacked-label spacing.



\## Food Count Tracking



Food counts are based on inventory consumption after using poisoned food on a tracked Penance Healer.



The plugin does not count every menu click as a successful feed. Instead, it tracks when a poisoned food item is actually consumed from the inventory after being used on a tracked healer. Wrong-food attempts are ignored when the Barbarian Assault wrong-food penalty message appears.



\## Labeling



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



\## Highlighting



Tracked healers can be highlighted using one of three styles:



\* \*\*None\*\*: No healer highlight is drawn.

\* \*\*Tile\*\*: Highlights the healer’s tile.

\* \*\*Hull\*\*: Draws an outline around the healer’s model.



\## Food Count Panel



The food-count panel lists each tracked healer by spawn order and shows how many poisoned food items have been successfully consumed for that healer.



\## Healer Codes and Roles



BA Healer Utilities supports configurable healer roles and wave-based healer-code lists. These settings can be used to display expected food counts for each healer based on the selected role and wave configuration.



\## Stack Handling



When multiple Penance Healers occupy the same tile, BA Healer Utilities can horizontally spread their labels so the healer numbers are easier to read.



This helps prevent stacked healers from hiding each other’s labels.



\## Credits



Thanks to Brehski/vibecodeandy for contributing improvements and helping a ton with the project.



\## Notes



BA Healer Utilities does not automate gameplay. It provides visual tracking for Penance Healer spawn order, labels, highlighting, and poisoned-food usage.



\## Version History



\* \*\*1.0.0\*\* - Added basic healer highlighting, numbering, and food-fed tracking.

\* \*\*1.0.2\*\* - Added food-fed counts directly on Healer NPCs and text configuration options.

\* \*\*1.0.3\*\* - Added time-based numbering option for healer labels.

\* \*\*1.0.4\*\* - Added healer-code configuration and healer roles.

\* \*\*1.0.5\*\* - Added stack handling logic, highlight style options, label display options, and config cleanup.



