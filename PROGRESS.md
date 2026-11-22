# Farm Run Optimizer - Progress

Made by ToadFx. Last updated: 2026-11-18.

## Status

Version **1.16.0**. Live on the Plugin Hub.

## 1.16.0 (2026-11-22)
- [x] "Farm run" bank view: farming button in the bank title bar, Bank Tags view with only the needed items.
- [x] Rune engine: rune pouch (read from varbits), staves/tomes with infinite runes, combination runes, per-rune slot accounting.
- [x] "Take with you" shows carried/need and updates as items are withdrawn.
- [x] Seed vault and tool leprechaun counts, with collection notes.
- [x] Harvest slot reservation in bank stops.
- [x] Route order: wiki, nearest first, custom (up/down arrows on stop cards).
- [x] Step box under the player.
- [x] Bank stops are navigation targets (hint arrow, minimap, Shortest Path).
- [x] P0 fixes: diseased/growing step logic, per-patch progress, spirit tree limits, growing patches not replanted, account reset, done stops in slot count, hops dibber, potato cactus clock, notification reset.
- [x] Real JUnit tests for the planner, rune supply, step items, growth clock and bank tiles (26 tests).

## Done

### Planning
- [x] Herb, tree, fruit tree and hardwood runs, combined into one route
- [x] Reads Farming level, Magic level, quests and spellbook automatically
- [x] Fastest teleport you own per patch; Ardougne cloak 2+ only; ectophial for Morytania
- [x] Fairy rings chained back to back from the nearest ring, with the code to dial
- [x] Spells / tabs checkboxes; click any teleport item to stop using it
- [x] Pick which herbs and trees to plant, and which patches to visit
- [x] Seeds go to the patches you use first; patches you can't plant (and don't use) are left off
- [x] "Take with you" list, with bank stops when it won't fit in 28 slots

### During the run
- [x] A card for every stop with its to-do list; next stop in gold, finished stops in green
- [x] "Rake the weeds" only when the patch actually has weeds
- [x] NOW banner and the current step's item highlighted in the inventory (seed, compost, payment)
- [x] Next patch outlined on the ground (exact patch object), hint arrow, Shortest Path line
- [x] Exact patch tiles learned from the game; built-in tiles from RuneLite's world map
- [x] Items to withdraw highlighted in the bank

### Timers
- [x] Patch state read from the game (growing stage, ready, chop down, stump, diseased, dead, weeds)
- [x] Learns your account's growth-tick offset for exact countdowns
- [x] Imports RuneLite Time Tracking timers; notifications when ready

### Panel and project
- [x] Runs, Travel and Settings sections; lock icons; new logo; "Made by ToadFx"
- [x] GitHub repo, README banner, build check, issue templates, changelog
- [x] Patch data generated from RuneLite source (`scripts/`), all 28 patches verified

## Recent fixes
- 1.10.0: 7 new PatchTypes (BUSH, HOPS, CACTUS, CALQUAT, SPIRIT_TREE, CELASTRUS, REDWOOD) with Clear enum (HARVEST/GARDENER/SPADE), usesSaplings, configKey; 19 crops; 21 patches (verify_patches OK, 63 total); patch-states regenerated; spirit tree limit; extra payments; FAIRY_AIQ/ALS/CIP/CKR, glory, ring of wealth. Test RunTypesCheck (64).
- 1.9.0: walk-links.tsv now 5074 links (transports + agility_shortcuts; same floor <=15 tiles, floor changes <=6) with reqId -> walk-reqs.tsv (skills/items ids/quests/varbits/varplayers). WalkRequirements checks them (client thread, quests cached 60s, items = inventory+worn). PathGuide is multi-floor (4 planes if <=1.2M cells). Tests: ReqCheck (16); real map: Lumbridge castle upstairs -> tree, Nemus shortcut by Agility.
- 1.8.2: Decoded.checkHealth from patch-states 'check' flag -> healthCheckDue set -> StepItems 'Check the tree's health' before 'Pay the gardener to clear it'; panel marks current line (StopWork.isCurrent). Test FruitHardCheck (26). Not yet added: Anglers' Retreat hardwood patch (gardener 15065).
- 1.8.1: gameHintArrow setting (default on): client hint arrow on gardener NPC / lead object / patch object / nav target; set only on change; not before teleport; doesn't clear other plugins' arrows.
- 1.8.0: PathGuide.find floods the window then picks the goal like SP's ReachableGoalRule (ring 1 tiers: open side, walled side, corner; rings 2-5 any reachable); gardener-payment stops target the learned approach tile (onto); stuck-start step allowed; fairy ring log (group 381) code tagged green + scrolled once.
- 1.7.1: guideStyle (line/tiles/both), guideStatus debug box in LeadHintOverlay, no line >60 tiles from a teleport stop until travelDone. Tree-run missing line still being diagnosed (routes OK offline on real map with exact footprints).
- 1.7.0: line A to B, untrimmed, hidden once reached (guideReached) until a new target or teleport; travelDone per stop (at destination ring, or a jump that brought you >30 tiles closer and within 200) stops ring/tree leads; 28 FarmPatch tiles corrected from Quest Helper (verify_patches OK).
- 1.6.2: line points via LocalPoint.fromWorld per tile (was offset from the player's interpolated local position, so it slid); no re-plan when the box turns exact or the Agility boost changes.
- 1.15.0: HerbYield (wiki herb calculator model: CTS table, item/diary boosts, lives by compost, disease (floor(27/r)+1)/128 over 3 stages; Farming-page +0.5 rounding). Profit = avg expected over run herb patches x grimy price - seed - compost. Diary varbits Kandarin 4476-4478, Kourend 7925/7927. Varlamore Champion disease-free not detected. YieldCheck 10.
- 1.14.0: Crop CAMPHOR/IRONWOOD/ROSEWOOD (stages 9/9/10 from RuneLite Produce; payments 10 White berries / 10 Curry leaf / 8 Dragonfruit; XP 88/17840, 145/20380, 252/23100; log icons 32904/32907/32910). Anglers' Retreat hardwood (2472,2705) still not added. AllRunsCheck 92.
- 1.13.1: payment also detected from the NPC dialogue (group 231 child 6) via StepItems.saysProtected ("leave it with me", "make sure that patch grows", "already looking after"); PaidCheck 9 tests.
- 1.13.0: shortestPathNotice() (PluginManager, descriptor name "Shortest Path") -> panel note + one chat message per session; gameHintArrow + showSocials in panel Settings; Teleport.ARDOUGNE_CLOAK_MONASTERY (cloak 1-4, max cape) for ARDOUGNE_BUSH; SocialLinks (light grey buttons; official icons can be dropped in as resources social/<name>.png).
- 1.12.4: SP back to pre-1.12 behaviour (send the patch / gardener approach tile, SP plans the whole trip with the user's SP settings; resend after teleports). Todd: SP "used to work perfectly". Don't send SP legs or walk-only overrides again.
- 1.12.3: SP legs sent walk-only (walkOnlyOverrides: all teleports/transports off, items None, no bank path) and patch targets as the ring of tiles around the patch (tilesAround). pathMinigameTeleports setting removed.
- 1.12.2: StepItems.Grown (PRODUCE/CHOP/CLEAR) from varbit for bush/cactus/celastrus: pick before dig; spirit tree extra payments highlighted (isStepItem). AllRunsCheck 70 tests: compost -> pay -> done for all 11 run types.
- 1.12.1: planting also detected from the patch varbit turning GROWING stage 0 (not only the chat message), so compost/pay steps start for saplings.
- 1.12.0: built-in guide line removed (PathGuide, WorldWalkMap, WalkRequirements, RouteSession, walk-map/links/reqs). Shortest Path gets each leg: lead ring/tree, then patch/gardener spot after travel, nothing before a teleport (within 60 tiles counts as arrived).
- 1.11.0: guide line re-plan rules ported from Shortest Path (RouteSession): re-plan on new target or 10+ tiles off every line tile; refined target keeps the line and redoes its end; no gardener chasing; SP goal tiers (4) and never the start tile. SessionCheck 12 tests.
- 1.6.1: guide line static: re-planned only on target change, teleport (>15 tiles in one move) or >40 tiles from the line; walked part trimmed only while on the line.
- 1.6.0: guide line follows Shortest Path's approach: A* in ticks, walking legs rewritten to the game's BFS walk (order W,E,S,N,SW,SE,NW,NE), route kept until >10 tiles off, walked part hidden, gardener slack 3 tiles.
- 1.5.1: guide line uses doors, gates and Agility shortcuts (walk-links.tsv from Shortest Path's transports via scripts/build_walk_links.py; shortcuts filtered by boosted Agility).
- 1.5.0: guide line uses the whole-world walking map (Shortest Path's collision map, BSD-2), A* with real distances, smoothing, stays put while followed, minimap route. Tested on real map data: Morytania to ALQ, Varlamore, Catherby, all tree patches.
- Tree run no longer sends you to patches you don't use when you're short on saplings
- Patch outline finds the real patch even when something is growing on it
- Corrected tiles for Nemus Retreat, Varlamore and others
- Ardougne cloak 1 no longer counts as the farm teleport

## Next up
- [ ] In-game screenshots for the README
- [ ] Add a LICENSE (BSD 2-Clause, needed for the Plugin Hub)
- [ ] Count seed vault / tool leprechaun storage
- [ ] On-screen "run ready" box
- [ ] Profit estimate per run
- [ ] Submit to the RuneLite Plugin Hub

## 2026-11-13 (b3)
- [x] Runs list at top, on/off per run, auto-expanded
- [x] Seed pot / watering reminders for tree runs
- [x] Hint arrow uses the loaded patch object
- [x] Build version in footer

## 2026-11-13 (b4)
- [x] Timers on top, Runs below
- [x] Crop name under each patch in the timers
- [x] Travel tiles dimmed + padlock (not greyscale)
- [x] Item pictures for every jewellery/item teleport and spirit tree
- [x] Dramen staff fairy ring toggle

## 2026-11-13 (b5)
- [x] Plan only ready patches (setting, default on), done patches kept green, auto-replan when ready

## 2026-11-13 (b6)
- [x] Run history + profit
- [x] Seed profit picker (most profitable first)
- [x] Disease/dead alerts + plant cure
- [x] Run-ready overlay
- [x] Farming contract
- [x] LICENSE + Plugin Hub prep (docs/PLUGIN_HUB.md)
- [ ] In-game test run (Todd)
- [ ] Submit to Plugin Hub (Todd, following docs/PLUGIN_HUB.md)

## 2026-11-13 (b7)
- [x] Path line follows the real patch tile (Hosidius / Varlamore were pointing past the patch)
- [x] Lead to the fairy ring first on fairy ring steps

## 2026-11-13 (b8)
- [x] Half-done tree run kept Taverley/Stronghold off (just-planted patches were taking the saplings)
- [x] Spellbook icons for teleport spells

## 2026-11-13 (b9)
- [x] Highlight the gardener to pay

## 2026-11-13 (b10)
- [x] Renamed to Farm Run Optimizer (name clash on the Plugin Hub)
- [ ] New GitHub banner with the new name (Todd)
- [ ] Plugin Hub pull request (Todd)

## 2026-11-13 (b11)
- [x] Plugin Hub review fixes: build=standard, player-focused README, removed testing section

## 2026-11-13 (b12)
- [x] Run history: run in progress saved/restored, shown live in the section

## 2026-11-14 (b13)
- [x] Outline the real fairy ring object
- [x] Profit = harvest value minus the seeds that grew it

## 2026-11-14 - LIVE on the Plugin Hub (first version: commit 5f83a39)
- [x] 1.0.1 update: b12 + b13 fixes, support link, README screenshots
- [ ] Remaining screenshots: ground highlight, fairy ring, gardener, stop cards, run history

## 2026-11-14 (1.0.2)
- [x] Tree run fixes: health check only without sapling, coins only when replanting, no compost for empty stops

## 2026-11-14 (1.1.0)
- [x] Nearest fairy ring outlined + on-screen "Dial XXX" box (also for "any fairy ring" steps)
- [x] Spirit tree outlined + "pick Gnome Stronghold"
- [x] Tree path goes to a walkable tile (gardener spot)

## 2026-11-14 (1.1.4)
- [x] Compost/payment highlight after planting restored
- [x] Tree path target = walkable tile beside the patch (collision map)
- [x] Hint arrow off by default

## 2026-11-14 (1.2.0)
- [x] Allotment run (14 patches verified against RuneLite, 8 crops, 3 seeds/patch, payments)
