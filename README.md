<p align="center">
  <img src="https://raw.githubusercontent.com/triggleman/farm-run-optimizer/main/images/banner.png" alt="Farm Run Optimizer - a farming companion for RuneLite" width="100%">
</p>

<p align="center">
  <b>Plan every farm run (herbs, allotments, trees, fruit trees, hardwood, bushes, hops, cactus, calquat, spirit trees, celastrus and redwood): what to bring, the fastest route, what to do at each patch, and every growth timer.</b><br>
  Made by ToadFx
</p>

---

<p align="center">
  <img src="https://raw.githubusercontent.com/triggleman/farm-run-optimizer/main/images/screenshots/bank-and-timers.png" alt="Items to withdraw outlined in the bank, with every patch timer in the sidebar" width="100%">
</p>

## Patch notes

**Latest: 1.21.0**
- **Countdown on every run's header**: "5/7 ready" or the time until the next patch, without opening the run.
- **"Next up"** at the top of the panel: your runs, soonest first, with progress bars.
- **One notification per run** when it's fully ready, plus a chat line at login for runs that finished while you were away.

**1.20.1**
- **Panel summary and timers only show runs you have turned on.**

**1.20.0**
- **Runs stay separate:** one run is finished before the next starts (turn it off in Settings for the old combined route).
- **Your own order:** move any stop up or down with ▲ / ▼; travel is re-planned for the order you pick, and "Reset" goes back to automatic.
- **Flower run:** marigold to white lily at all eight flower patches.

**1.19.0**
- **Your house as a travel option:** set your house location and Teleport to House, house tabs or the Construction cape get used for nearby patches (Hosidius, Taverley, Brimhaven, Yanille, Rimmington, Aldarin).
- **Locus Oasis fix:** Shortest Path no longer stops short of the hardwood patch on your first visit.
- **Rake only when needed**, and no "you don't own a…" warnings before you've opened your bank.

- **New look:** new logo, sidebar icon and panel banner.
- **Romte1's Farm run bank button** in the bank's title bar opens the run view.
- **Bank view polish:** Romte1's animated bank button, smaller labels with a little space under each item, and wider spacing between items.

- **Farm run bank tab:** click the Farm run button in the bank's title bar to see just the run's items, labelled with how many to take and whether noted or unnoted, and withdraw them with normal clicks.
- **Now / next box:** what to do right now and the next stop, at the top of the screen.
- **Herb profit the OSRS Wiki way:** expected herbs per patch from your level, compost, magic secateurs, Farming cape, diaries and disease, minus seed and compost.
- **New hardwoods:** camphor, ironwood and rosewood from the Sailing update are planned, timed and tracked in every hardwood patch.
- **Seven new runs** (turn them on in the panel): bushes, hops, cactus, calquat, spirit trees, celastrus and redwood, each with timers, steps, gardener payments and Farming Guild contracts.
- **Steps at each stop are more reliable:** compost and pay the gardener now show after planting for every run type, including fruit trees. Paying the gardener ends the step as soon as they agree to look after the patch. Bushes and cactus have you pick before you dig.
- **Walking lines come from Shortest Path:** the plugin hands each stop to the [Shortest Path](https://runelite.net/plugin-hub/show/shortest-path) plugin, and tells you if it isn't installed or is turned off. The built-in line is gone, which also makes the plugin much smaller.
- **More travel:** any Ardougne cloak (even cloak 1) for the Ardougne bush, plus amulet of glory, ring of wealth and more fairy rings.
- **Panel:** game hint arrow on/off in Settings, and channel buttons at the bottom.

Every change, version by version, is in the [changelog](CHANGELOG.md).

## Getting started

1. Install **Farm Run Optimizer** from the Plugin Hub.
2. Click the parchment-map icon in the RuneLite sidebar.
3. **Open your bank once** so the plugin can see your seeds, saplings, compost and teleports.
4. Tick the runs you want (Herbs, Allotments, Trees, Fruit trees, Hardwood) and follow the list.

Your Farming level, quests and diaries are read automatically, so the plan only uses patches and teleports you've unlocked.

## Features

### Plan the run
- **Twelve run types** that combine into one route: herbs, allotments, trees, fruit trees, hardwood, bushes, hops, cactus, calquat, spirit trees, celastrus and redwood. Allotments (potatoes up to snape grass, from level 1) and hops (from level 3) are a great start before herbs and trees unlock. Spirit tree runs only plant as many as your Farming level allows.
- **Only ready patches.** If one herb is ready, the run is just that patch. Growing patches are left off, with the time until the next one is ready.
- **Pick your crops and patches.** Each run lists every crop (locked ones show the level you need) and every patch. Click to choose what to plant and where to go.
- **Most profitable herb first.** See the profit per patch for every herb you can grow, at Grand Exchange prices, and plant the best one first.
- **Farming contracts.** Your Farming Guild contract is shown, and the contract crop is planted in the guild patch.

<p align="center"><img src="https://raw.githubusercontent.com/triggleman/farm-run-optimizer/main/images/screenshots/runs-and-profit.png" alt="Herb picker with profit per patch" height="520"></p>

### What to bring
- **Take with you.** One list of seeds, saplings, compost, gardener payments (noted is fine), coins, tools, teleport items and spell runes. Items you're carrying get a green border; the rest are outlined in your bank.
- **Bank stops.** If it won't all fit in 28 slots, the route adds a stop at the nearest bank with exactly what to withdraw.
- **Plant cure and secateurs** are added when a patch is diseased.
- **Tree seed reminders** to pot and water your seeds so you have saplings for next time.

<p align="center"><img src="https://raw.githubusercontent.com/triggleman/farm-run-optimizer/main/images/screenshots/take-with-you.png" alt="Take with you list, tips and finished stops" height="520"></p>

### Getting there
- **Fastest teleports you own**: jewellery, capes, the ectophial, spells or tablets (your choice).
- **Fairy rings** chained back to back, with the code to dial on every stop, e.g. Ardougne cloak, walk to BLR, dial ALQ, then AKR, then AJP.
- **On the ground**: the next patch is outlined, with a steady arrow over it and a marker or arrow on the minimap (no extra plugin needed); on fairy ring and spirit tree stops the ring or tree is marked with what to pick. The game's own yellow hint arrow points at the next ring, tree, gardener or patch. For a walking line, install the **[Shortest Path](https://runelite.net/plugin-hub/show/shortest-path)** plugin: Farm Run Optimizer hands it each stop and it draws the whole trip.
- **Gardeners** you need to pay are outlined, with what to give them.

<p align="center"><img src="https://raw.githubusercontent.com/triggleman/farm-run-optimizer/main/images/screenshots/walking-path-ardougne.png" alt="Ardougne herb patch with the run-ready box" width="100%"></p>
<p align="center"><img src="https://raw.githubusercontent.com/triggleman/farm-run-optimizer/main/images/screenshots/tree-run-nemus.png" alt="Tree run at Nemus Retreat: patch outlined, gardener highlighted, and the stop card" width="100%"></p>

### At each patch
- **A to-do card for every stop**: harvest, check health, pay the gardener, dig up a stump, plant, compost, pay for protection. The next stop is gold and finished stops turn green.
- **The item you need right now** (seed, compost, payment) is highlighted in your inventory.

<p align="center">
  <img src="https://raw.githubusercontent.com/triggleman/farm-run-optimizer/main/images/screenshots/herb-patch-outline.png" alt="Falador herb patch outlined with the hint arrow" width="49%">
  <img src="https://raw.githubusercontent.com/triggleman/farm-run-optimizer/main/images/screenshots/gardener-highlight.png" alt="Gardener outlined with what to pay" width="49%">
</p>

### Timers and alerts
<img src="https://raw.githubusercontent.com/triggleman/farm-run-optimizer/main/images/screenshots/timers-three-runs.png" alt="Timers for herbs, allotments and trees" align="right" height="520">

- **Exact timers** for every patch, read from the game when you're nearby, from your plantings, and from RuneLite's Time Tracking.
- **Notifications** when a patch is ready, and when a patch is diseased or dead.
- **Run-ready box**: a small on-screen box like "Herbs 3 of 6 ready" or "Trees in 1h 20m".

<br clear="right">

### Profit and XP
- **XP estimate** before every run, based on what's growing and what you're planting.
- **Run history**: every harvest is logged at Grand Exchange prices, minus the seeds that grew it, with profit and Farming XP per run and your average herbs per patch.

## Settings

Everything can be switched on or off in the panel's **Settings** section or in RuneLite's plugin settings: the ground highlight, the game's hint arrow or the plugin's own steady arrow, Shortest Path walking line, gardener highlight, inventory and bank highlights, notifications, the run-ready box, run history, which teleports to use (spells, tablets, individual items) and more.

<p align="center"><img src="https://raw.githubusercontent.com/triggleman/farm-run-optimizer/main/images/screenshots/travel-and-settings.png" alt="Travel and settings" height="520"></p>

## Patches

| Type | Patches |
|---|---|
| Herbs | Falador, Catherby, Ardougne, Morytania*, Hosidius, Farming Guild (65), Varlamore*, Troll Stronghold*, Weiss*, Harmony Island* |
| Allotments | 2 each at Falador, Catherby, Ardougne, Morytania*, Hosidius, Varlamore*, Farming Guild (45) |
| Trees | Varrock, Lumbridge, Falador, Taverley, Gnome Stronghold, Farming Guild (65), Nemus Retreat* |
| Fruit trees | Gnome Stronghold, Tree Gnome Village, Catherby, Brimhaven, Lletya*, Farming Guild (85), Kastori* |
| Hardwood | Fossil Island x3*, Locus Oasis* |

\* needs a quest or diary, which is detected automatically. For Harmony Island, tick "Morytania Elite done" in the plugin settings.

## Tips

- If something in the plan looks off, open your bank once to refresh what you own.
- Timers fill in as you visit patches; walking past a patch is enough to read its exact state.
- Found a bug or have an idea? [Open an issue](https://github.com/triggleman/farm-run-optimizer/issues).

## Credits

- **[RuneLite](https://github.com/runelite/runelite)**: patch states, regions and growth stages come from RuneLite's Time Tracking plugin.
- **[Shortest Path](https://github.com/Skretzo/shortest-path)** by Skretzo: draws the walking line when installed (Farm Run Optimizer sends it each stop). Spirit tree patch tiles come from its data (BSD 2-Clause, see NOTICE).
- **[Old School RuneScape Wiki](https://oldschool.runescape.wiki/)**: items, payments, growth times, teleports, directions and fairy ring codes.
- **[Romte1](https://github.com/Romte1)**: the Farm run bank button and its animation (garden glow, leaf ring and fireflies), and the ideas behind the on-screen step box and the spaced-out bank view, from his pull request #1.

Released under the [BSD 2-Clause License](LICENSE). Full attributions: [NOTICE](NOTICE). Old School RuneScape is a trademark of Jagex Ltd; this plugin is not affiliated with or endorsed by Jagex.

## Channels

Made by ToadFx: [YouTube](https://www.youtube.com/@toadfxx) · [Twitch](https://www.twitch.tv/toadfxx) · [Kick](https://kick.com/toadfx) · [TikTok](https://www.tiktok.com/@toadfx)
