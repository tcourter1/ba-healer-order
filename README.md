# BA Utilities

BA Utilities is a RuneLite Plugin Hub plugin for Barbarian Assault.

It provides visual tracking, menu helpers, side-panel tools, and optional party sync features for Barbarian Assault. While BA Utilities began with a focus on Penance Healer utilities, version 2.0.0 expands the plugin into a broader role-based Barbarian Assault utility suite.

## Features

* Tracks and labels Penance Healers by spawn order or common time-based BA labels.
* Provides configurable healer highlighting, label styling, stack handling, and right-click menu labels.
* Tracks successful poisoned-food usage by healer number.
* Supports healer-code presets, custom wave codes, import/export, and side-panel code management.
* Displays healer food counts and code progress on NPCs, in the side panel, and optionally in right-click menus.
* Supports optional Penance Healer time-to-kill indicators.
* Includes healer dispenser menu helpers for called food, Take-Vial, and Take-Meat.
* Includes optional poisoned-food protection to prevent using healer food on the wrong target.
* Supports optional BA Party Sync for sharing NPC information with teammates who also use the plugin.
* Adds Attacker role utilities, starting with configurable Ranger/Fighter cave spawn-count overlays.
* And a ton more! See [this document](https://docs.google.com/document/d/e/2PACX-1vTHMC5GHx8T8Y1dKU5voarGItnU5oorXI9S9WufXcLP-SL0yb0ietfNxwtPfIgF2A/pub) for a detailed look at every feature this plugin has to offer.

## 2.0.0 Update

BA Utilities 2.0.0 expands the plugin beyond healer-only tools and begins adding support for additional Barbarian Assault roles.

This update introduces the first Attacker utility: a configurable cave spawn-count overlay. When playing Attacker, the plugin can display spawned/total counts above the Ranger and Fighter cave entrances, helping attackers track how many Penance Rangers and Penance Fighters have appeared during the wave.

This update also marks the broader transition from a healer-focused plugin into a role-based Barbarian Assault utility suite. The codebase now supports cleaner role separation, with Attacker functionality living separately from Healer functionality. This should make it easier to add Collector, Defender, and additional Attacker tools in future updates.

Party Sync has also become a major part of BA Utilities. When enabled, teammates using the plugin can share useful wave information through RuneLite's built-in Party system, helping keep healer labels, food tracking, death times, and time-to-kill information consistent between clients.

## Party Sync

BA Party Sync lets teammates using BA Utilities share useful wave information automatically.

When enabled, the plugin can connect players on the same Barbarian Assault team through RuneLite's built-in Party system. This can help keep healer labels, food tracking, death times, and time-to-kill information consistent between teammates, even when one player did not personally see a healer spawn or die.

Party Sync is optional and off by default. It is intended to make shared information smoother during coordinated runs without requiring players to manually copy codes, call out every healer state, or track everything from only one client's point of view.

## Side Panel

The side panel contains information about your recent runs, overviews of each wave, as well as setup for healer and defender (coming soon) strategies.

### Recent Runs and Wave Overviews

This section displays your last ten runs, including teammates, roles, durations, and an overview of each wave. While a wave is in-progress, the wave overview updates live with each NPC's spawn status, predicted TTK (healers only), and death time. The more team members are in a BA Party, the more information can be shared here.

<img width="209" height="328" alt="image" src="https://github.com/user-attachments/assets/298a7512-e94e-4890-9224-65cd21873e21" />

_List of recent runs, including a button to delete the selected one, and a button to open the folder where the runs are stored._

<img width="217" height="694" alt="image" src="https://github.com/user-attachments/assets/0e4f8490-9627-49d2-991b-2ef1b597d39b" />

_Summary of a completed wave. Usernames redacted._

<img width="293" height="391" alt="image" src="https://github.com/user-attachments/assets/3dc85eb3-a76f-4fd6-a08e-32fb0768b213" />

_NPC columns can be removed to clean up the display and only track relevant NPCs_

### Healer Codes

BA Utilities includes a healer-code system for managing expected food counts and healer targets across Barbarian Assault waves.

From this tab, users can edit healer codes, select wave-specific codes, save custom presets, and manage their active healer-code setup. Healer codes can be pasted in and parsed automatically, as well as customized manually in a user-friendly editor.

<img width="976" height="676" alt="image" src="https://github.com/user-attachments/assets/ecc0fd07-3131-4909-b3d9-4363887379b2" />

The plugin also supports importing and exporting healer-code presets as JSON. This makes it easier to share code setups with teammates or back up custom configurations.

Pre-loaded presets are available for waves 4 through 10, giving users a ready-to-use starting point for common healer-code setups.

Healer-code entries can include time-based progression. Additional lines represent later wave timing increments, allowing the plugin to update expected food counts as the wave progresses.

### Tile Markers and Strategies

BA Utilities includes a fully interactable map of the Barbarian Assault arena that can be used to mark and label useful tiles.

<img width="1064" height="690" alt="image" src="https://github.com/user-attachments/assets/43bfa6e8-3ffd-4114-bfe6-c7be0156cd08" />

Dynamic notes can also be added that change color as the wave progresses.

<img width="403" height="176" alt="image" src="https://github.com/user-attachments/assets/d13e789a-b55b-4e70-b6fe-96df2aa169ec" />
<img width="403" height="171" alt="image" src="https://github.com/user-attachments/assets/61dcd87c-5865-40ad-9992-825cea5de692" />

These tile markers and notes can be set per-wave and per-role, so your strategy updates dynamically depending on the conditions.

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

Healer labels and codes can also be displayed in the right-click menu.

<img width="413" height="249" alt="image" src="https://github.com/user-attachments/assets/4f12e807-1992-4530-b050-381c0448e283" />

Labels can also be disabled entirely.

### Highlighting

Tracked healers can be highlighted using common highlight styles, including tile, true tile, and hull highlighting.

Highlight color, label color, label size, and stacked-label spacing are configurable.

### Food Count Tracking

Food counts are based on inventory consumption after using poisoned food on a tracked Penance Healer.

The plugin does not count every menu click as a successful feed. Instead, it tracks when a poisoned food item is actually consumed from the inventory after being used on a tracked healer. Wrong-food attempts are ignored when the Barbarian Assault wrong-food penalty message appears.

### Food Panel

BA Utilities includes an optional side-panel display for healer food tracking.

Depending on configuration, the panel can show healer food counts, code progress, simplified death-time information, or code-only views.

### Healer TTK

BA Utilities can optionally display when poisoned Penance Healers are expected to die.

This can be shown on the healer itself and in the food panel, depending on configuration. When Party Sync is enabled, TTK information can also be shared with teammates using the plugin.

### Stack Handling

When multiple Penance Healers occupy the same tile, BA Utilities can horizontally spread their labels so the healer numbers are easier to read.

This helps prevent stacked healers from hiding each other's labels.

### Right-Click Menu Labels

BA Utilities can optionally add the tracked healer label and/or code to Penance Healers in the right-click menu.

This makes it easier to identify the correct healer when multiple healers are close together or stacked.

### Healer Food Protection

BA Utilities includes an optional setting to prevent poisoned healer food from being used on anything except a Penance Healer.

This helps avoid accidental feeds on players, NPCs, or other invalid targets while poisoned food is selected.

## Attacker Features

BA Utilities now includes early Attacker role support.

### Cave Spawn Count Overlay

When playing Attacker, BA Utilities can display Ranger and Fighter spawn counts above the Penance cave entrances.

The overlay shows spawned enemies compared to the total expected spawns for the wave:

```
2 / 4
```

The left number is the amount spawned so far, and the right number is the total expected for that cave during the current wave.

Separate counts are shown for:

* Penance Rangers
* Penance Fighters

The overlay is configurable, including:

* Enable/disable toggle
* Text color
* Text size
* Horizontal positioning
* Height/vertical positioning

This feature is role-locked and only appears while playing Attacker.

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

BA Utilities does not automate gameplay. It provides visual tracking, menu organization, code progression, poisoned-food tracking, role-specific overlays, and optional party-shared information for Barbarian Assault.

## Version History

<details>
<summary>Show version history</summary>

### 2.2.0
* Added options for customizing overlay panel
* Improved ::bawave dev command support 
* Added a tile marker editor for creating, editing, importing, and exporting tile markers
* Added dynamic notes that update as wave time progresses
* Implemented per-wave and per-role strategies that combine tile markers and notes
* Added runtime tile marker rendering

### 2.1.1
* Re-added food count height config
* Added time codes to reserve Healer labeling
* Fixed horizontal alignment over Attacker caves

### 2.1.0
* Added Wave Overview and Recent Runs in side panel
* Reworked TTK calculations to use precise food tracking when possible, but switch to HP-ratio-estimate mode when necessary
* Added healer inventory counts

### 2.0.0

* Expanded BA Utilities beyond healer-only tools into a role-based Barbarian Assault utility suite.
* Added first Attacker role utility: configurable Ranger/Fighter cave spawn-count overlays.
* Added Attacker configuration section with overlay color, font size, horizontal offset, and height offset options.
* Added role-separated Attacker code structure to support future Attacker, Collector, and Defender utilities.
* Continued expansion of optional BA Party Sync for sharing wave information between teammates using the plugin.

### 1.1.1

* Fixed a bug where, in certain circumstances, fed-food could be applied to the incorrect Penance Healer.
* Opened Hide Dead NPCs up to all roles.

### 1.1.0

* Added Penance Healer TTK timer.
* Expanded UI panel functionality with several new options.
* Fixed a bug with healer labeling logic.

### 1.0.9

* Added function to disable most features when you're not in the Healer role.
* Added option to instantly hide dead NPCs, including Penance Healers and/or Penance Healers/Fighters/Rangers.

### 1.0.8

* Added healer-only poisoned food menu protection.
* Added option to show healer codes in the right-click menu on tagged healers.
* Added dispenser menu options.
* Updated healer code side panel visually.
* Redesigned import/export/saving function to work more reliably.

### 1.0.7

* Lyelt added a fully redesigned healer code system complete with import/export/saving functions.
* Food count tracking updated with color-coding to track healer code progress.
* Updated labeling logic to label all healers by Index ID sequentially, rather than by render order.
* Labels now self-correct when labeled incorrectly on render.

### 1.0.6

* Added more copy/paste friendly meta healer code support.
* Added healer target labels.
* Added optional right-click menu labels.
* Added true-tile option to Healer highlighting config.

### 1.0.5

* Added stack handling logic.
* Added highlight style options.
* Added label display options.
* Added config cleanup.

### 1.0.4

* Added healer-code configuration and healer roles.

### 1.0.3

* Added time-based numbering option for healer labels.

### 1.0.2

* Added food-fed counts directly on Healer NPCs and text configuration options.

### 1.0.0

* Added basic healer highlighting, numbering, and food-fed tracking.

</details>
