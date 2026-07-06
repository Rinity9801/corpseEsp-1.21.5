# Pet-flip spreadsheet

A live Google Sheet that pulls BIN auctions from the [Coflnet API](https://sky.coflnet.com)
for **Golden Dragon**, **Rose Dragon**, and **Legendary Mosquito**, and works out
which is the best leveling flip — and the coins per pet XP.

## What it tells you

You buy a low-level pet, grind it to a higher level, then resell. For each pet it shows:

| Column | Meaning |
|---|---|
| Best Buy Lvl / Buy Price | the level that gives the **best coins/XP** — found by testing every level on the market, not just Lvl 1 |
| Sell Lvl / Sell Price | cheapest listing at the highest level on the market (your resale target) |
| Net After Tax | what you actually keep after AH selling tax (see below) |
| XP to Grind | exact pet XP between the chosen buy level and the sell pet (read live from each auction) |
| Profit | Net After Tax − Buy Price |
| **Coins / XP** | Profit ÷ XP to Grind — **the headline number.** Higher = more coins per unit of pet XP you grind |

**Why not always Lvl 1?** Pet value isn't spread evenly across XP — for a Golden Dragon almost
all the value unlocks at levels 100 and 200, so the XP from 1→100 is nearly worthless and buying
low + grinding the whole way is poor coins/XP. The tool checks the cheapest listing at **every**
level, grinds each to the top level, and recommends whichever buy point has the best coins/XP. The
**Levels** tab shows that full breakdown per pet (cheapest price, XP to grind, profit, coins/XP),
with `★` on the recommended level — so you can see the curve and pick your own grind range.

**It tells you which to buy:** the pets are ranked by Coins / XP (best first, marked `★ 1`
and highlighted green), and row 1 is a banner naming the best pick. The **Buy Link**
column (and the banner's `BUY ↗`) opens that exact auction on Coflnet, where you can see
it and copy the in-game open command.

The **Auctions** tab lists every legendary listing (level, XP, price, price-per-XP, and a
direct `open ↗` link).

## Tax (baked into "Net After Tax")

Hypixel BIN selling tax, seller side:
- **Listing fee** (paid up front when you list): 2.5% if price > 100M, 2% if 10M–100M, else 1%.
- **Claim tax** (taken when you collect the sale): 1% on anything over 1M, capped so collecting never drops you below 1M.

So an expensive pet (e.g. a Golden Dragon at ~750M) loses ~3.5% to tax on sale; a ~50M pet loses ~3%.
Only **BIN** selling is modeled. Listing as a timed **auction** instead costs 5% + a duration fee — not used here, since flips are sold via BIN.

## Setup (one time)

1. Create a new Google Sheet.
2. **Extensions ▸ Apps Script**.
3. Delete the placeholder code, paste the contents of [`PetFlips.gs`](./PetFlips.gs), and **Save**.
4. Reload the Google Sheet. A **Pet Flips** menu appears.
5. **Pet Flips ▸ Refresh** → approve the one-time authorization → the sheet fills in.

Refresh whenever you want fresh prices.

## Notes / tuning (top of `PetFlips.gs`)

- `PETS` — add/remove pets by Coflnet tag (e.g. `PET_ENDER_DRAGON`, `PET_JADE_DRAGON`, `PET_SCATHA`).
- `RARITY` — defaults to `LEGENDARY`. Dragons are legendary-only; Mosquito is sold at many rarities, so this filters to the legendary ones you asked about.
- `MAX_PAGES` — pages of listings scanned per pet (10 each). Coflnet serves cheapest-first and legendary Mosquitos are pricier than the commons, so they sit on later pages. If Mosquito shows *"no LEGENDARY listings found"*, raise this (e.g. 60–80).
- The free Coflnet API **rejects sort and filter params** (`Rarity=` → 403, `orderBy=` → 400) — that sorting you see is the website UI, not the API. So the script pages through the cheapest-first listings and filters by rarity/level itself, which is why `MAX_PAGES` matters for Mosquito.
