# Changelog

## 1.21.0 (2026-11-26)
- **Countdown on every run's header.** In the Runs section, each run you do shows its live status without opening it: "5/7 ready" in green or the time until the next patch ("59m 59s", "1h 59m") in yellow. Hover for details.
- **"Next up" at the top of the panel.** Replaces the old summary: every run you do, soonest first, with a progress bar of how far it has grown. Ready runs sit on top with a full green bar. Hover a run to see when the next patch and the whole run will be ready.
- **One notification per run.** When every patch of a run you do is ready, you get one RuneLite notification and a chat message ("Your herb run is ready (7 patches)"), instead of one notification per patch. At login, one chat line lists runs that finished while you were away. Runs you've turned off never notify. The setting is "Notify when a run is ready".

## 1.20.1 (2026-11-26)
- **Only your runs in the panel summary.** The summary at the top of the panel ("Herbs: ready in 15m", "Cactus: 1 need you"...) and the patch timers now only list runs you have turned on. Before, any patch the game had told the plugin about showed up, even for runs you don't do (an empty patch you walked past, an old spirit tree).

## 1.20.0 (2026-11-26)
- **Runs stay separate.** When you do more than one run (say allotments and trees), the plugin now finishes one before starting the next instead of mixing their stops. Within a run, a stop only links to stops of the same run. The runs go in the panel's order (herbs, allotments, flowers, trees...). Turn "Keep runs separate" off in Settings to get the old combined route back.
- **Put the stops in your own order.** Every stop card has ▲ and ▼ buttons. Move a stop and the plan follows your order from then on, and it's saved for next time. Travel is picked again for each stop based on where you're coming from: a teleport when you're coming from somewhere else, "Walk over from the…" when the stop before is right next to it, and fairy ring directions that follow your order. "Stops in your own order · Reset" above the stops goes back to the automatic order.
- **Flower run.** Marigold, rosemary, nasturtium, woad, limpwurt and white lily. It covers the flower patches at Falador, Catherby, Ardougne, Morytania, Hosidius, Ortus Farm (Varlamore), the Farming Guild (45+) and Kastori, with timers, steps, XP, seeds and Farming Guild flower contracts. Flowers can't be protected, so there's nothing to pay.

## 1.19.0 (2026-11-24)
- **Your house as a travel option.** Pick where your house portal is under **House** in the panel's Travel section (or "House location" in Settings). Teleport to House (40 Magic), house teleport tablets and the Construction cape are then used for the patches near it:
  - **Rimmington:** the bush patch.
  - **Taverley:** the tree patch.
  - **Hosidius:** the herb patch, both allotments and the spirit tree.
  - **Brimhaven:** the fruit tree, the spirit tree and the Tai Bwo Wannai calquat.
  - **Yanille:** the hops patch.
  - **Aldarin:** the hops patch.
  The Construction cape (or max cape) can teleport to any house portal, so it's used at all of these whatever your house location. Directions say to leave by the portal when the spell or tablet lands you inside.
- **First visit to the Locus Oasis hardwood patch:** Shortest Path is sent to a walkable tile next to the patch instead of the patch itself, so it no longer stops with "Destination could not be reached" before you've been there. Marcellus is now recognised under all of his NPC ids, so his spot is learned for next time.
- **Rake only when it's needed:** the rake is only on the "Take with you" list when a patch on the run might have weeds. Hardwood and fruit tree runs where every tree is growing don't ask for one.
- **No false "you don't own a…" warnings:** until you've opened your bank since logging in, the plugin just says "Open your bank once" instead of claiming tools like the rake are missing.

## 1.18.2 (2026-11-23)
- **New art.** A new GitHub banner, a new Plugin Hub logo, a "Farm Run Optimizer" banner at the top of the side panel, and a new sidebar icon (the sprout in a circle of arrows).

## 1.18.1 (2026-11-22)
- **Romte1's Farm run bank button.** Our square is replaced with Romte1's own button, his code used with his permission. It's a real bank-style button with a spade icon in the bank's title bar, placed just left of where Quest Helper puts its button so the two sit side by side. While the run view is closed it flashes gently and has his garden animation (gold sheen, green halo, ring of leaves, fireflies). Click it to show just the run's items, and click again for your whole bank. "Animated bank button" turns the animation off.
- The labels, item spacing, Now / next box and "Not in your bank" list stay as they were.

## 1.18.0 (2026-11-22)
- **Animated bank button by Romte1.** While the run view is closed, the Farm run square by the bank's close button gets Romte1's garden animation: a breathing gold sheen and green halo, a slow ring of tiny leaves, and golden fireflies rising from it. While the view is open it shows a steady green rim. Turn it off with "Animated bank button".
- **Cleaner labels:** smaller text on a small dark pill, with a few pixels of space between the item and its label.
- **More space between items** in the run view: 7 wider columns and taller rows, so every label fits and nothing touches. The bank's tab divider lines are hidden in this view. It only moves items on screen, and clicking still withdraws as normal.
- **Credits for Romte1** in the README and NOTICE: the bank button animation is his code, used with his permission, and the Now / next box and the spaced-out bank view follow ideas from his pull request.

## 1.17.2 (2026-11-22)
- **The bank tab is now a small glowing square** with the magic secateurs emblem, right next to the bank's close button, so it no longer covers the bank's title (like the GE / HA values). Hover over it to see "Farm run 8/10" and what a click does. It turns green once you carry everything. Clicking it still shows just the run's items, and clicking again brings your whole bank back.

## 1.17.1 (2026-11-22)
- **Bank labels sit underneath each item** ("4 unnoted", "15 noted", ✓) instead of on top, so they don't cover the item or the bank's own count.
- **The Now / next box hides while the bank is open**, so it never covers the bank, and comes back as soon as you close it.

## 1.17.0 (2026-11-22)
- **The bank tab is now a real tab you withdraw from.** Click the glowing **Farm run** tab on the bank's title bar and the bank shows just this run's items, through RuneLite's Bank Tags (the same way Quest Helper does it). They're your real bank items, so you withdraw them with normal clicks. Click the tab again to go back to your whole bank.
- **The tab** sits on the right of the title bar, next to the bank's own buttons. It has a magic secateurs icon, shows how many of the run's items you already carry ("Farm run 2/6"), and has a soft pulsing glow (turn that off with "Glowing bank tab").
- **Each item is labelled** along the bottom with how many more to take and how:
  - **"15 noted"** (blue): gardener payments, which can be handed over noted.
  - **"3 unnoted"** (yellow): compost, saplings, tools and teleport items you need to use.
  - A plain number for stackable items, and **✓** once you're carrying enough.
- Anything the run needs that isn't in your bank is listed under the tab.
- Needs RuneLite's Bank Tags plugin, which is on by default.

## 1.16.0 (2026-11-22)
- **Supply tab in the bank.** A small "Farm run 3/6" tab sits on the bank's title bar and shows how many of the run's items you already carry. Click it to open a quick supply list: each item with how many you carry out of how many you need. Green means you have them all, white means the rest is in your bank, red means you're short. If the run needs a bank stop on the way, it says so. Click the tab again to close it; it stays as you left it. It only draws on screen: it never withdraws anything, and clicks on the open list are ignored so you can't withdraw through it by accident.
- **"Now / next" box at the top of the screen.** It shows what to do right now and the stop after that, e.g. "Now: Pay: 10 x Coconut at Catherby fruit tree" and "Next: Ardougne herb (Ardougne cloak)". "Now" covers the fairy ring code to dial, the spirit tree to pick, the teleport to use and each step at the patch. It also shows "stop 3 of 7". It replaces the separate "Dial AKR..." box while it's on.
- Both can be turned off in Settings ("Now / next box", "Supply tab in bank").

## 1.15.0 (2026-11-21)
- **Herb profit now uses the OSRS Wiki's method.** Before, every herb was priced at a flat 8 herbs a patch (or your own average), minus the seed. Now each herb's expected yield is worked out the way the wiki's herb calculator does it:
  - **Harvest lives:** 3, plus 1, 2 or 3 for compost, supercompost or ultracompost (the bottomless bucket counts as ultracompost).
  - **Chance to save a life:** depends on the herb and your Farming level, plus magic secateurs (+10%) and the Farming cape (+5%) if you own them.
  - **Diary boosts:** Catherby gets +10, +17 or +25 for the Kandarin medium, hard or elite diary; Hosidius and the Farming Guild get +10 for the Kourend hard diary. These are read from the game.
  - **Disease:** the chance a patch dies is counted, except at disease-free patches (Troll Stronghold, Weiss, Harmony, and Hosidius with the easy Kourend diary).
  - It's averaged over the herb patches you actually run, and **compost cost** is now taken off along with the seed.
- The profit list shows each herb's expected herbs a patch, e.g. "Ranarr (8.6)", and a line saying what it assumes. Run history still shows your own real average.

## 1.14.0 (2026-11-21)
- **Camphor, ironwood and rosewood hardwoods.** These are the hardwood trees from the Sailing update, and the plugin can now plan, time and track them in every hardwood patch:
  - **Camphor:** level 66, pay 10 white berries.
  - **Ironwood:** level 80, pay 10 curry leaves.
  - **Rosewood:** level 92, pay 8 dragonfruit.
  - Growth times come from RuneLite and payments and XP from the OSRS Wiki. Before this, a camphor, ironwood or rosewood tree already in a patch showed as an unknown crop.
- **Hardwood run checked from start to finish** at Fossil Island and Locus Oasis, for all five hardwoods: check health, pay the gardener 200 coins to clear it, plant the sapling, add compost, then pay the gardener (or squirrel) to protect it. On Fossil Island each patch has its own squirrel, and the right one is outlined. Patch tiles match RuneLite's data.

## 1.13.1 (2026-11-21)
- **Paying the gardener always ends the step.** The plugin used to decide you'd paid only by seeing the payment item leave your inventory. If that didn't match up (for example, you paid with a different form of the item), "Pay the gardener" stayed up after you'd paid. Now it also reads the gardener's reply. As soon as they say "Leave it with me - I'll make sure that patch grows for you" (or that they're already looking after it), the step is done and you move on to the next stop. Asking the price, being refused, or "already fully grown" don't count.
- The order at each stop is unchanged: add compost, then pay the gardener, then on to the next patch.

## 1.13.0 (2026-11-21)
- **Shortest Path check.** If the Shortest Path plugin isn't installed, or is installed but turned off, the panel shows a note at the top saying so, and you get one chat message after logging in. The note goes away as soon as Shortest Path is installed and on. It also goes away if you turn off "Shortest Path walking line".
- **"Game hint arrow" is in the panel's Settings**, so you can turn the yellow arrow on or off without opening RuneLite's config.
- **Ardougne cloak 1 counts as travel.** Every Ardougne cloak (1 to 4, and the Ardougne max cape) teleports to Kandarin Monastery, right beside the Ardougne bush patch, so the bush run now suggests it even if you only have cloak 1. The farm teleport for the Ardougne herb and allotments still needs cloak 2 or higher.
- **Channel buttons** at the bottom of the panel (YouTube, Twitch, Kick, TikTok), all one light grey. Turn them off with "Channel links" in Settings.

## 1.12.4 (2026-11-19)
- **Shortest Path works the way it did before 1.12.** It's given the next patch (or the gardener's spot) and plans the whole trip itself, with your own Shortest Path settings: teleports, fairy rings, spirit trees, everything. This undoes 1.12.0's one-leg-at-a-time targets and 1.12.3's walking-only routes, which changed the routes it picked.
- "Path: minigame teleports" is back.
- Plugin Hub description now lists all eleven run types.
- Everything else from 1.12 stays: no built-in line, compost and payment steps that start from the patch itself, pick before dig, and the spirit tree payments.

## 1.12.3 (2026-11-19)
- **Shortest Path only walks now.** Every leg we send it (to the fairy ring or spirit tree, then to the patch) turns off its teleports, fairy rings, spirit trees, boats, POH and bank trips for that route. So it can't head off towards a teleport of its own, like going the wrong way back to AKR from the Hosidius patch. The teleport is always the one the panel names. Your own Shortest Path settings come back as soon as we clear the route.
- **The line ends on the nearest side of the patch.** Shortest Path is given every tile around the patch and goes to whichever is quickest to reach, instead of one corner.
- Removed the "Path: minigame teleports" setting, since Shortest Path only walks for us now.

## 1.12.2 (2026-11-18)
- **Checked every run type's steps after planting:** herb, tree, fruit tree, hardwood, bush, hops, cactus, calquat, spirit tree, celastrus and redwood. Each goes add compost (highlighted), then pay the gardener (payment highlighted, gardener outlined), then done. Poison ivy skips the payment, because the game doesn't let you protect it.
- **Bushes, cactus and celastrus: pick before you dig.** A fully grown bush or cactus used to highlight your spade straight away, so following the highlight would dig up a full bush. The plugin now reads how many berries or spines are left from the game:
  - While there are some left, the step is "Pick the berries" or "Pick the cactus spines", with no item highlighted.
  - Only once it's empty is your spade highlighted to dig it up.
  - Celastrus goes harvest the bark, then chop it down, then dig up the stump.
- **Spirit trees: all three payment items are highlighted** (5 monkey nuts, the monkey bar and the ground tooth), not just the monkey nuts.

## 1.12.1 (2026-11-18)
- **Compost and pay-the-gardener steps after planting now always show.** They used to start only when the plugin read the game's "You plant..." chat message, and when that wording didn't match (as happened with fruit tree saplings), your compost and the gardener's payment were never highlighted. The plugin now also notices the patch itself turn freshly planted, for all 57 crops, so the steps run: add compost, then pay the gardener (payment highlighted in your inventory, gardener outlined).

## 1.12.0 (2026-11-18)
- **The built-in guide line is gone.** It kept disagreeing with the panel and with Shortest Path, so the plugin no longer draws a walking line of its own. That also removes its 1.6 MB walking map, so the plugin is much smaller. The "Guide line", "Guide line style" and "Guide line status" settings are gone too.
- **Shortest Path now follows your run.** Before, it was only given the next patch and picked its own way there, often a different teleport from the panel. Now it gets one leg at a time:
  - On a fairy ring or spirit tree stop, it walks you to the ring or tree the panel says to use.
  - Once you've travelled, it walks you to the patch, or to where you stand to pay the gardener.
  - Before a stop's teleport it gets nothing, so it can't pick a teleport of its own. Use the one the panel names.
- "Shortest Path walking line" is now on by default. It does nothing unless you have the Shortest Path plugin installed.
- These all stay: the game's yellow hint arrow, the patch, ring, tree and gardener highlights, the minimap arrow, the fairy ring code highlight, and the "Dial AKR for Hosidius herb" banner.

## 1.11.0 (2026-11-18)
- **Guide line uses Shortest Path's rules for when to redraw it.** Ported from its scheduler (PathScheduler and RefreshCoordinator):
  - The line is worked out once, from where you stand, when there's a new place to go.
  - It then stays exactly where it is on the ground while you're within 10 tiles of any part of it, wherever you walk.
  - Only when you're 10 or more tiles from every part of it (you walked off, or teleported) is it worked out again, from where you are.
  - Before, it was only redrawn after a teleport, so walking off it left a line that no longer started near you. It was also redrawn from scratch the moment the patch or ring loaded in, which put a hook at your feet halfway along the route.
- **When the patch loads in, the line is kept.** Only its last few tiles are redone to end beside the real patch.
- **The line no longer chases the gardener.** It goes to the spot you stood on when you last paid them, or else to the patch, so it doesn't keep re-aiming as they wander. The game hint arrow still follows the gardener.
- **Where the line ends now matches Shortest Path's goal rule exactly:** a side with no wall, then a side behind a wall, then an open corner, then a walled corner, and never the tile you're already standing on.
- The search, the map data and the game-walk order already matched Shortest Path's. Checked again line by line against its CollisionMap, ReachableGoalRule and AStarWalkCanonicalizer.

## 1.10.0 (2026-11-18)
- **Seven new run types** (all off by default; switch them on in the panel or settings):
  - **Bushes:** Champions' Guild, Rimmington, Ardougne, Etceteria, Farming Guild. Redberry up to poison ivy. Check health, pick the berries, dig up with a spade to replant.
  - **Hops:** Lumbridge, Seers' Village, Yanille, Entrana, Aldarin. Barley up to wildblood, 4 seeds a patch (jute 3).
  - **Cactus:** Al Kharid, Farming Guild. Cactus and potato cactus (which grows every 10 minutes).
  - **Calquat:** Tai Bwo Wannai, Kastori. The gardener clears it for 200 coins.
  - **Spirit trees:** Port Sarim, Etceteria, Brimhaven, Hosidius, Farming Guild. Only plants as many as your Farming level allows (1 at 83, 2 at 88, 3 at 93, 4 at 96, any at 99), counting ones already planted. Payment of 5 monkey nuts, 1 monkey bar and 1 ground tooth is on the bring list.
  - **Celastrus** and **redwood** in the Farming Guild's top tier.
- Each has timers, health-check-first steps, gardener payments (from the OSRS Wiki), XP estimates, highlights, the guide line and gardener highlighting (gardener ids from RuneLite).
- New ways in: Amulet of glory (Al Kharid), Ring of wealth (Miscellania) and fairy rings AIQ, ALS, CIP and CKR.
- Farming Guild contracts for bushes, cactus, celastrus and redwood now point at the right guild patch.
- Not yet: the Great Conch calquat and Anglers' Retreat hardwood patches (Sailing areas).

## 1.9.0 (2026-11-18)
- **Guide line checks what doors, stairs and shortcuts need, like Shortest Path.** It now knows over 5,000 doors, gates, stairs, ladders and shortcuts (from Shortest Path's lists), and works out which you can use from your skill levels, finished quests, items carried or worn, and game progress. Before, anything needing more than an Agility level was treated as closed, so the line went the long way round (or nowhere) past quest gates and item shortcuts.
- **Stairs and ladders:** the line can start upstairs or downstairs and take you down (or up) to the stop. Only the part on your floor is drawn.
- Teleports are still chosen by the run plan, so the line and the panel always agree.

## 1.8.2 (2026-11-18)
- **Tree, fruit tree and hardwood stops go in the right order.** When the game says a grown tree's health hasn't been checked yet, the step is "Check the tree's health" first (that's where the XP is). Only after that does it highlight your coins and the gardener for "Pay 200 coins to chop it down". Before, it jumped straight to paying.
- **The step you're on is marked in the stop's card:** that line turns bright white with an arrow, and the others stay orange. Works for every step: rake, dig, check health, pay to clear, plant, compost, pay to protect.
- Checked fruit tree and hardwood gardeners (all 14 NPC ids match RuneLite's Time Tracking) and payments (all match the OSRS Wiki: e.g. 15 papaya fruit for palm, 25 Yanillian hops for mahogany, 15 limpwurt roots for teak).

## 1.8.1 (2026-11-17)
- **Game hint arrow is back** as a setting, "Game hint arrow" (on by default): the game's own yellow arrow, which also shows on the minimap. It sits on the same thing the guide line goes to: the gardener you need to pay (it follows them), the fairy ring or spirit tree to use, or the patch. It's only moved when that target changes, so it no longer jumps about, and it waits until you've done a stop's teleport. It won't clear an arrow another plugin (like Quest Helper) has set. The plugin's own "Arrow over target" stays available; turn either off if you only want one.
- The walking map now loads on RuneLite's own background worker.

## 1.8.0 (2026-11-16)
Guide line reworked to match how the Shortest Path plugin picks where to go:
- **Tree, fruit tree and hardwood stops aim at a fixed spot**: the tile where that patch's gardener stood when the plugin first saw them (the same tile Shortest Path is sent), not the moving gardener or the patch outline. That's why Shortest Path's line worked on tree runs and ours didn't.
- **Always somewhere to stop.** If the target can't be stood on, the line ends on the nearest tile you can reach: right beside it (an open side first, then a side behind a fence, then a corner), else up to 5 tiles out. Before, a slightly-off patch outline or a fence edge meant no line at all.
- **Starting on a "blocked" tile** (a doorway, standing in a fairy ring) no longer stops the line.
- **Fairy ring travel log:** when you open a fairy ring, the code for your next stop is turned green with "(Farm Run)" in front, and the list scrolls to it once, like Shortest Path does.

## 1.7.1 (2026-11-16)
- **New "Guide line style" setting:** Line (as before), Tiles (each step of the route highlighted on the ground, like Shortest Path's tile mode), or Line and tiles.
- **New "Guide line status" setting** (off by default): a small box under the top hint saying what the line is aiming at (patch, gardener, fairy ring or spirit tree, and where), which map it used, how many steps, and, when there's no line, why ("teleport first", "no route found from ...", "reached the end"). Turn it on and screenshot it if the line misbehaves.
- **No walking line before a teleport.** If the next stop starts with a teleport you haven't done yet and it's more than 60 tiles away, the line waits until you've teleported, instead of drawing a long walk there.

## 1.7.0 (2026-11-16)
- **Guide line goes from A to B and stays put.** The whole line, from where you start to the patch, fairy ring, spirit tree or gardener, is drawn once and kept exactly as it is. Nothing is trimmed while you walk. It disappears when you reach the end, and the next stop's line takes over. It's only worked out again for a new stop, or after a teleport.
- **No more being sent back to the ring after a fairy ring or spirit tree teleport.** Once you've arrived (at the destination ring, or a teleport took you much closer to the stop), the plugin leads you to the patch, not back to the ring you landed on. This fixes Hosidius after AKR.
- **Correct patch tiles** for 28 patches (checked against the Quest Helper plugin's farm-run tiles): herb patches at Falador, Catherby, Ardougne, Morytania, Hosidius, Varlamore, Farming Guild, Troll Stronghold, Weiss and Harmony; the tree, fruit tree and hardwood patches; the three Fossil Island hardwood patches were well off. Lines to stops that haven't loaded yet now head to the right place (Ortus Farm, Hosidius).

## 1.6.2 (2026-11-16)
- **Guide line no longer bounces as you walk.** Each point of the line was placed relative to your character, which glides between tiles, so the whole line slid with you. Every point is now pinned to its own map tile, like Shortest Path's line.
- The line stays the same until you reach the patch or fairy ring: it no longer re-plans when the patch or ring loads in (if the line already ends beside it) or when an Agility boost wears off.

## 1.6.1 (2026-11-16)
- **Guide line stays fixed on the ground.** Walking off it no longer moves it. It's only worked out again when the next stop changes, you teleport, or you end up more than 40 tiles away from it. The part you've walked still disappears once you're back on the line.

## 1.6.0 (2026-11-16)
- **Guide line steadied the way the Shortest Path plugin does it.**
  - It now draws the exact tiles your character walks when you click along it (the game's own walking order: west, east, south, north, then diagonals), not a straightened line.
  - It keeps the same route while you follow it, and only works it out again if you go more than 10 tiles off it (Shortest Path's default) or the next stop changes.
  - The part you've already walked disappears.
  - A gardener walking about no longer makes the line re-plan every tick; it only re-plans if they wander more than 3 tiles from the end of the line.

## 1.5.1 (2026-11-16)
- **Guide line uses doors, gates and Agility shortcuts.** The walking map counts doors and gates as closed, so the line was taking long detours (or going the wrong way, like at Nemus Retreat). It now goes through doors and gates, and through Agility shortcuts your current Agility level can use (broken walls, stepping stones, tunnels...). The list comes from the Shortest Path plugin (see NOTICE), filtered to ones that need nothing but an Agility level.

## 1.5.0 (2026-11-16)
- **Guide line reworked.** It now plans with a walking map of the whole game (from the Shortest Path plugin, credited in NOTICE) instead of only the area around you, so it no longer heads off the wrong way (into the sea at Morytania, away from the patch in Varlamore) when the stop isn't loaded yet.
- **Straighter, steadier line.** Routes use real distances and are smoothed into straight stretches instead of zigzagging, and the line stays the same while you follow it instead of jumping between equally short routes.
- Stops beside the patch on a side you can actually reach (not behind a fence), or in the fairy ring.
- **Route on the minimap** too, and the minimap arrow now points along the route.
- Map squares with no map data are never walked on.

## 1.4.0 (2026-11-15)
- **Built-in guide line.** The plugin now draws its own walking line, worked out from the game's collision map over the loaded area: to the patch (stopping beside it), the fairy ring or spirit tree first, or the gardener when it's time to pay. It goes round walls, fences and gates, and towards the patch when it's beyond the loaded area. No other plugin needed. Setting: "Guide line" (on by default).
- The Shortest Path plugin is now optional and off by default ("Shortest Path plugin line" in Settings). It kept saying "Destination could not be reached" at tree patches, because you can't stand on a 3x3 tree patch.

## 1.3.3 (2026-11-15)
- **Minimap arrow:** a marker on the minimap at the next patch, fairy ring or spirit tree, or an arrow near the minimap's edge pointing towards it when it's further away. Works without the Shortest Path plugin. Uses the "Arrow over target" setting.

## 1.3.2 (2026-11-15)
- **New arrow:** the game's blinking hint arrow is no longer used. The plugin draws its own steady arrow right above the label of the real thing on screen: the gardener to pay, the fairy ring or spirit tree to use, or the patch once it's loaded. Setting "Arrow over target" (on by default).
- Fairy rings: only real rings at known ring spots are outlined (scenery that shares the name was being picked up near Morytania).

## 1.3.1 (2026-11-15)
- **Gardeners found by their id** (from RuneLite's Time Tracking), so every patch's gardener is highlighted, including Lumbridge and Taverley.
- **Walking line for trees and allotments** aims at where the patch's gardener stands (always a walkable spot beside the patch), learned the first time you see them.
- **Gnome Stronghold via the Grand Exchange spirit tree:** "Varrock Teleport, spirit tree at the Grand Exchange to Gnome Stronghold". Varrock Teleport is highlighted (and its runes or tab added to the bring list), then the GE spirit tree is marked with what to pick.
- **Hint arrow pinned to real things only:** the gardener you need to pay, the fairy ring or spirit tree to use, or the patch itself once it's loaded. No more arrow on a guessed tile or jumping around.

## 1.3.0 (2026-11-14)
- **XP before a run:** "This run: about 12k Farming XP", from what you'll harvest or health-check (using what's growing in each patch and your average herbs per patch), planting and compost. XP values from the OSRS Wiki.
- **XP after a run:** Farming XP gained at the patches is logged with each run: "So far: 4 patches • +80k gp • +6.2k XP", and in Run history with the average XP per run.
- Fix: huasca needs 65 Farming (was 58).

## 1.2.5 (2026-11-14)
- Grown trees with no model outline (seen at Lumbridge and Gnome Stronghold) are still marked: the plugin falls back to the click area, then the patch's floor area.

## 1.2.4 (2026-11-14)
- The walking line is asked for again right after you teleport, from where you land. Before, a route worked out from the previous stop (e.g. Nemus Retreat to Varrock) could fail and wouldn't come back until you were next to the patch.

## 1.2.3 (2026-11-14)
- Tree and allotment patches: Shortest Path is given a ring of tiles around the patch (plus the learned walkable spot) and walks to whichever it can reach, so it no longer stops with "Destination could not be reached" before you've ever visited the patch.

## 1.2.2 (2026-11-14)
- Timers only list patches with something in them (growing, ready, diseased, dead, stump); empty and weedy patches are hidden with a "+6 empty patches hidden" note. Setting: "Timers: planted patches only" (on by default).

## 1.2.1 (2026-11-14)
- New setting "Path: minigame teleports" (off by default): the Shortest Path line no longer routes through minigame teleports like Rat Pits. Applies only to the routes this plugin asks for; your own Shortest Path settings are untouched.

## 1.2.0 (2026-11-14)
- **New: Allotment run.** 14 allotments (2 each at Falador, Catherby, Ardougne, Morytania, Hosidius, Varlamore and the Farming Guild at 45 Farming), all 8 crops from potatoes (level 1) to snape grass (61), 3 seeds per patch, compost, and the gardener's protection payment. Great for new players before herbs and trees unlock. Combines with the herb run (the allotments are right next to the herb patches).
- Farming Guild allotment contracts plant in the guild allotment.
- The two allotments side by side are told apart by their real position when you plant.
- Same item used two ways (e.g. compost to use and to pay for potatoes) is one line in the bring list.

## 1.1.4 (2026-11-14)
- Fix: after planting, the compost (and the gardener payment on tree runs) is highlighted again. Since 1.0.2 a just-planted stop lost its compost/payment step.
- Tree patches: the walking path aims at a walkable tile right beside the patch, worked out from the game's collision map (fixes "Destination could not be reached" at Varrock, Falador park, Nemus Retreat and others).
- The blinking yellow hint arrow is off by default (turn it back on in Settings if you like it).

## 1.1.3 (2026-11-14)
- Shortest Path is aimed at the patch again (not the fairy ring), so it plans and draws the whole trip including the ring, like before. The ring itself is still outlined with the code.
- "Dial AKR for Hosidius herb" box shows more reliably when the fairy ring interface is open, and is wide enough for the full text.
- Fairy rings are found whichever way the game sends them (ground objects, decorations, impostors).
- Directions say which way to walk to the ring: "Walk south (about 30 tiles) to fairy ring ALQ, dial AKR".

## 1.1.2 (2026-11-14)
- On the way to the next stop, the teleport is highlighted: the spell in your spellbook (e.g. Falador Teleport), or the teleport item/tab in your inventory. The NOW banner says "Cast Falador Teleport" / "Teleport".

## 1.1.1 (2026-11-14)
- Spirit tree travel tile and stop cards show a spirit tree instead of a spirit seed.

## 1.1.0 (2026-11-14) - big update
Everything since the first Plugin Hub version (1.0.1 + 1.0.2), plus:
- **Fairy rings everywhere**: on any fairy ring step ("From any fairy ring, dial AIS" too), the nearest ring is outlined with the code, and a big "Dial AIS for Nemus Retreat tree" box shows at the top of the screen while the fairy ring interface is open.
- **Spirit trees**: on spirit tree steps the nearest spirit tree is outlined ("Spirit tree: Gnome Stronghold"), the path and arrow lead to it, and the top-of-screen box says which destination to pick.
- **Tree patches**: the walking path goes to the tile where the gardener stands (learned the first time you visit), since the patch itself can't be walked on. Fixes "Destination could not be reached".

## 1.0.2 (2026-11-14)
Tree run fixes:
- No sapling for a grown tree: just check its health (XP) and leave it standing, instead of paying 200 coins to chop it with nothing to replant.
- Growing trees with no sapling are left off the run.
- Coins (200 each) only for trees you're replacing.
- Compost is no longer packed for stops where nothing is planted (no sapling, or already done this run). Also applies to herbs.
- Clearer sapling shortage warning.

## 1.0.1 (2026-11-14) - first Plugin Hub update
- Includes b12 and b13: run in progress saved, live "This run" in history, real fairy ring outline, fairer run profit.
- Support link on the Plugin Hub page; screenshots in the README.

## 1.0.0 b13 (2026-11-14)
- Fairy ring stops outline the real fairy ring (like patches), and the path and arrow go to it.
- Run profit now counts the seed that grew what you harvested, instead of the seed you planted for next time, so a run is no longer negative just because you planted pricier seeds.

## 1.0.0 b12 (2026-11-13)
- Fix: the run in progress is saved as you go, so closing the client mid-run no longer loses it.
- Run history shows "This run" straight away instead of only after the run ends.
- Harvests are picked up from a bit further from the patch.

## 1.0.0 b11 (2026-11-13)
- Plugin Hub review: added `build=standard`, rewrote the README for players and removed the developer testing section.

## 1.0.0 b10 (2026-11-13)
- Renamed to **Farm Run Optimizer**: "Farm Run Planner" is already taken on the Plugin Hub by another author.

## 1.0.0 b9 (2026-11-13)
- The gardener you need to pay is outlined in gold, with what to give them ("Pay: Oranges(5)" or "Pay 200 coins to chop it down"). Setting: "Highlight gardener to pay".

## 1.0.0 b8 (2026-11-13)
- Fix: patches you just planted no longer use up saplings in the plan, so the rest of the run (e.g. Taverley, Gnome Stronghold) stays on the list.
- Teleport spells show their spellbook picture (Varrock, Camelot, Ardougne...) instead of a law rune, on stop cards and in Travel.

## 1.0.0 b7 (2026-11-13)
- Walking path now goes to the real patch: it's re-sent whenever the target tile changes (e.g. when the patch loads in), instead of once per patch using the rough map tile.
- Fairy ring steps lead you to the ring first ("Fairy ring: dial AKR" on the ring, path and arrow), then on to the patch after you teleport.

## 1.0.0 b6 (2026-11-13)
- Run history & profit: harvests logged at GE prices, profit per run, average herbs per patch.
- Herb profit per patch, and "Most profitable herb first" planting (setting, on by default).
- Diseased/dead notifications; plant cure or secateurs added to the bank list for diseased patches.
- On-screen run-ready box (ready counts, next timer, diseased patches, contract).
- Farming Guild contract card; contract crop planted in the guild patch.
- BSD 2-Clause LICENSE and Plugin Hub submission guide (docs/PLUGIN_HUB.md).

## 1.0.0 b5 (2026-11-13)
- Only ready patches are planned: if 1 herb is ready, the run is just that patch (and only 1 seed to bring). Growing patches are left off with a "next up" tip.
- When nothing is ready, the full run is shown so you can prep.
- Patches you just did stay on the list (green) for 30 minutes.
- Replans automatically when a patch becomes ready.
- New setting: "Only ready patches" (on by default).

## 1.0.0 b4 (2026-11-13)
- Timers moved to the top, Runs underneath.
- Timers show what's growing in each patch (e.g. "Catherby / Ranarr weed").
- Travel tiles are faded with a padlock instead of greyed out.
- Pictures for the spirit tree, Kharedst's memoirs, quetzal whistle, hunter cape, pendant of ates, teleport crystal, digsite pendant and the Trollheim/Fenkenstrain/Brimhaven tabs.
- Dramen staff tile under Other turns all fairy rings on/off.

## 1.0.0 b3 (2026-11-13)
- Runs list moved to the top of the panel, with an on/off box per run; active runs open automatically.
- Tree runs: reminders to plant tree seeds in plant pots and water the seedlings when you have no saplings.
- Hint arrow now points at the real patch object (matches the outline).
- Version shown in the footer so you can tell which build is running.

## Unreleased

- Seeds and saplings go to the patches you use first; patches with nothing to plant and nothing of
  yours growing are left off the route and the bank list.
- Patch outline matches the real patch object by its farming varbit (works while crops grow).
- Exact patch tiles learned from the game; built-in tiles from RuneLite's world map.
- Patch data generated from RuneLite's Time Tracking source; all patches verified.
- New logo, GitHub banner, build check, issue templates, progress file.

## 1.0.0 - 2026-11-13

First release of Farm Run Planner.

- Herb, tree, fruit tree and hardwood runs, combined into one route.
- Fastest teleport you own per patch; fairy rings chained with the code to dial; spells and tabs toggles.
- "Take with you" list with bank stops when it won't fit in one inventory; bank items highlighted.
- A card for every stop with its to-do list; finished stops turn green; next stop in gold.
- Current step's item highlighted in your inventory (seed, compost, payment).
- Exact timers read from the game, growth-tick offset learning, Time Tracking import, notifications.
- Crop and patch pickers, Settings section, ground highlight, hint arrow, Shortest Path support.
