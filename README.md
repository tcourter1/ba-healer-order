# BA Utilities

BA Utilities is a RuneLite Plugin Hub plugin for Barbarian Assault.

It provides visual tracking, menu helpers, side-panel tools, and optional party sync features for Barbarian Assault, with a current focus on Penance Healer utilities.

## Features

* Tracks and labels Penance Healers by spawn order or common time-based BA labels.
* Provides configurable healer highlighting, label styling, stack handling, and right-click menu labels.
* Tracks successful poisoned-food usage by healer number.
* Supports healer-code presets, custom wave codes, import/export, and side-panel code management.
* Displays healer food counts and code progress on NPCs, in the side panel, and optionally in right-click menus.
* Supports optional Penance Healer time-to-kill indicators.
* Includes healer dispenser menu helpers for called food, Take-Vial, and Take-Meat.
* Includes optional poisoned-food protection to prevent using healer food on the wrong target.
* Supports optional BA Party Sync for sharing healer information with teammates who also use the plugin.

## Party Sync

BA Party Sync lets teammates using BA Utilities share useful wave information automatically.

When enabled, the plugin can connect players on the same Barbarian Assault team through RuneLiteâ€™s built-in Party system. This can help keep healer labels, food tracking, death times, and time-to-kill information consistent between teammates, even when one player did not personally see a healer spawn or die.

Party Sync is optional and off by default. It is intended to make shared information smoother during coordinated runs without requiring players to manually copy codes, call out every healer state, or track everything from only one clientâ€™s point of view.

## Healer Features

BA Utilities provides several tools for tracking Penance Healers during waves.

### Labeling

The plugin can label healers using either simple spawn-order numbering or time-based BA healer labels.

Spawn-order labels display healers as:

```
1, 2, 3, 4...
```
Time-based labels display healers using common BA timing labels, such as:
```
6, 12, 18, 24, R1, R2...
```
Healer labels can also be displayed in the right-click menu.

<img width="517" height="271" alt="image" src="https://github.com/user-attachments/assets/8663be8f-923b-408e-a0db-32232083a0ad" />

Labels can also be disabled entirely.

### Highlighting
Tracked healers can be highlighted using common highlight styles, including tile, true tile, and hull highlighting.

Highlight color, label color, label size, and stacked-label spacing are configurable.

### Food Count Tracking
Food counts are based on inventory consumption after using poisoned food on a tracked Penance Healer.

The plugin does not count every menu click as a successful feed. Instead, it tracks when a poisoned food item is actually consumed from the inventory after being used on a tracked healer. Wrong-food attempts are ignored when the Barbarian Assault wrong-food penalty message appears.

### Healer Codes
BA Utilities includes a healer-code system for managing expected food counts and healer targets across Barbarian Assault waves.

Healer codes live in a dedicated side panel. From this panel, users can select wave-specific codes, save custom presets, clear current selections, and manage their active healer-code setup.

The plugin also supports importing and exporting healer-code presets as JSON. This makes it easier to share code setups with teammates or back up custom configurations.

Pre-loaded presets are available for waves 4 through 10, giving users a ready-to-use starting point for common healer-code setups.

Healer-code entries can include time-based progression. Additional lines represent later wave timing increments, allowing the plugin to update expected food counts as the wave progresses.

Target labels can be included in healer-code entries using parentheses. When present, the plugin can display the active target label above the healer.

<img width="237" height="534" alt="image" src="https://github.com/user-attachments/assets/27866011-b816-40d2-b968-93f83c3465bb" /> <img width="218" height="394" alt="image" src="https://github.com/user-attachments/assets/d686fb70-9243-4f35-8344-c0e85fd3aa5c" />

The active code can also be displayed in the Penance Healers' right-click menu.

<img width="413" height="249" alt="image" src="https://github.com/user-attachments/assets/4f12e807-1992-4530-b050-381c0448e283" />

### Food Panel
BA Utilities includes an optional side-panel display for healer food tracking.

Depending on configuration, the panel can show healer food counts, code progress, simplified death-time information, or code-only views.

### Healer TTK
BA Utilities can optionally display when poisoned Penance Healers are expected to die.

This can be shown on the healer itself and in the food panel, depending on configuration. When Party Sync is enabled, TTK information can also be shared with teammates using the plugin.

### Stack Handling
When multiple Penance Healers occupy the same tile, BA Utilities can horizontally spread their labels so the healer numbers are easier to read.

This helps prevent stacked healers from hiding each otherâ€™s labels.

### Right-Click Menu Labels
BA Utilities can optionally add the tracked healer label and/or code to Penance Healers in the right-click menu.

This makes it easier to identify the correct healer when multiple healers are close together or stacked.

### Healer Food Protection
BA Utilities includes an optional setting to prevent poisoned healer food from being used on anything except a Penance Healer.

This helps avoid accidental feeds on players, NPCs, or other invalid targets while poisoned food is selected.

### Dispenser Options
The Dispenser Options section includes optional helpers for the Healer item machine menu:

* **Highlight Called Food** highlights the correct Take option based on the current defender call.
* **Remove Take-Vial** removes the Take-Vial option from the menu.
* **Move Take-Meat up** keeps Take-Meat with the other food options, above Walk here and Examine.
* **Deprioritize Other Dispensers** removes interaction options from non-role dispensers when applicable.

## Demonstrations
A player satisfies the requirement of a code at or after the correct time:

https://github.com/user-attachments/assets/959d4924-c2b3-41bb-8b4c-5b3a66208a2f

A player uses the correct amount of food, but too early:

https://github.com/user-attachments/assets/2a67d1fe-7bb6-4b26-ae4a-116e5f12f73b

## Credits
Thanks to Brehski/vibecodeandy and Lyelt/Not Bad for contributing improvements and helping a ton with the project.

## Notes
BA Utilities does not automate gameplay. It provides visual tracking, menu organization, code progression, poisoned-food tracking, and optional party-shared information for Barbarian Assault.

## Version History
1.0.0 - Added basic healer highlighting, numbering, and food-fed tracking.
1.0.2 - Added food-fed counts directly on Healer NPCs and text configuration options.
1.0.3 - Added time-based numbering option for healer labels.
1.0.4 - Added healer-code configuration and healer roles.
1.0.5 - Added stack handling logic, highlight style options, label display options, and config cleanup.
1.0.6 - Added more copy/paste friendly meta healer code support, healer target labels, and optional right-click menu labels. Added true-tile option to Healer highlighting config.
1.0.7 - Lyelt added a fully redesigned healer code system complete with import/export/saving functions. Food count tracking updated with color-coding to track healer code progress. Updated the labeling logic to label all healers by Index ID sequentially, rather than by render order. Labels also self correct when labeled incorrectly on render.
1.0.8 - Added healer-only poisoned food menu protection. Added option to show healer codes in the right-click menu on tagged healers. Added dispenser menu options. Healer code side panel updated visually and import/export/saving function redesigned to work more reliably.
1.0.9 - Added function to disable most features when you're not in the Healer role. Added option to instantly hide dead NPCs, including Penance Healers and/or Penance Healers/Fighters/Rangers.
1.1.0 - Added Penance Healer TTK timer, expanded UI panel functionality with several new options, fixed a bug with healer labeling logic.
1.1.1 - Fixed a bug where, in certain circumstances, fed-food could be applied to the incorrect Penance Healer. Opened Hide Dead NPCs up to all roles.
