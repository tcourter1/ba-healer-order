# BA Utilities

BA Utilities is a RuneLite Plugin Hub plugin for Barbarian Assault. It adds side-panel tools, healer codes, tile marker strategies, role-aware overlays, menu helpers, poisoned-food tracking, and optional BA Party Sync.

For a detailed feature guide, see [this document](https://docs.google.com/document/d/e/2PACX-1vTHMC5GHx8T8Y1dKU5voarGItnU5oorXI9S9WufXcLP-SL0yb0ietfNxwtPfIgF2A/pub).

## Features

* Side panel with recent runs, live wave overviews, healer codes, and tile marker strategies.
* Healer labels, highlighting, food tracking, code progress, TTK, stack handling, and right-click labels.
* User-friendly healer-code editor with presets, custom wave codes, import/export, and time-based progression.
* Interactive BA arena tile marker editor with per-wave/per-role strategies and timed notes.
* Optional BA Party Sync for sharing wave information with teammates using BA Utilities.
* Attacker cave spawn-count overlay for Rangers and Fighters.
* Dispenser menu helpers and click options.

## Side Panel

The side panel shows recent runs, wave overviews, healer-code setup, and tile marker strategies.

### Recent Runs and Wave Overview

BA Utilities tracks recent runs, including teammates, roles, durations, and per-wave NPC summaries. During a wave, the overview updates live with spawn status, death times, and healer TTK information when available.

<img width="209" height="328" alt="image" src="https://github.com/user-attachments/assets/298a7512-e94e-4890-9224-65cd21873e21" />

_List of recent runs, including a button to delete the selected one, and a button to open the folder where the runs are stored._

### Healer Codes

BA Utilities includes a healer-code system for expected food counts, healer targets, wave-specific code selection, presets, and JSON import/export. Codes can be pasted and parsed automatically or edited manually in the side panel.

<img width="976" height="676" alt="image" src="https://github.com/user-attachments/assets/ecc0fd07-3131-4909-b3d9-4363887379b2" />

### Tile Markers and Strategies

BA Utilities includes an interactive BA arena map for creating tile markers, labels, per-wave/per-role strategies, and dynamic notes that update as the wave progresses.

<img width="1064" height="690" alt="image" src="https://github.com/user-attachments/assets/43bfa6e8-3ffd-4114-bfe6-c7be0156cd08" />

<img width="403" height="176" alt="image" src="https://github.com/user-attachments/assets/d13e789a-b55b-4e70-b6fe-96df2aa169ec" />
<img width="403" height="171" alt="image" src="https://github.com/user-attachments/assets/61dcd87c-5865-40ad-9992-825cea5de692" />

## In-Game Tools

### Healer

BA Utilities can label Penance Healers by spawn order or common time-based BA labels, display healer codes, track poisoned-food usage from actual inventory consumption, show code progress, estimate TTK, spread labels for stacked healers, and add healer information to right-click menus.

<img width="413" height="249" alt="image" src="https://github.com/user-attachments/assets/4f12e807-1992-4530-b050-381c0448e283" />

Demonstration of a player satisfying the requirement of a code at or after the correct time:

https://github.com/user-attachments/assets/959d4924-c2b3-41bb-8b4c-5b3a66208a2f

### BA Party Sync

BA Party Sync uses RuneLite's Party system to share useful wave information with teammates using BA Utilities, including healer labels, food tracking, death times, TTK information, recent-run data, and optional BA Party Chat.

## Credits

Thanks to Brehski/vibecodeandy and Lyelt/Not Bad for contributing improvements and helping a ton with the project.

## Notes

BA Utilities does not automate gameplay. It provides visual tracking, menu organization, code progression, poisoned-food tracking, role-specific overlays, and optional party-shared information for Barbarian Assault.

## Version History

<details>
<summary>Show version history</summary>

### 2.3.1

* Minor bug fixes in healer code editor.
* Adjust tile marker chunk gridlines.

### 2.3.0

* Added onboarding and update side panel.
* Enhanced healer code parsing and introduced user-friendly healer code editor.
* Introduced highlight options for omega egg dupe and current wave ladder.
* Added public chat between BA Party members.
* Fixed a bug where renamed Penance NPCs could sometimes not be properly identified.

### Previous Versions

* 2.2.0: Added tile marker editing, dynamic notes, per-wave/per-role strategies, import/export, and runtime tile marker rendering.
* 2.1.x: Added recent runs, wave overview, improved healer TTK, healer inventory counts, reserve healer time codes, and related fixes.
* 2.0.0: Expanded BA Utilities from healer-only tools into a role-based BA utility suite and added Attacker cave spawn-count overlays.
* 1.1.x: Added healer TTK, panel improvements, hide-dead options, and healer labeling fixes.
* 1.0.x: Added healer highlighting, numbering, food tracking, healer codes, stack handling, menu labels, dispenser helpers, and poisoned-food protection.

</details>
