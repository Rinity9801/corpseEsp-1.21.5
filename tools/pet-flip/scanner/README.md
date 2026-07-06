# Pet flip scanner (Hypixel → Cloudflare KV)

Scans the **Hypixel** auction house (public, no API key) and writes finished pet-flip
results to the Cloudflare KV that `pets.adong.dev` serves. Runs in **GitHub Actions** —
on GitHub's IPs, with full CPU — so it sidesteps Coflnet's per-IP rate-limit ban and
Cloudflare's Worker CPU limit entirely.

```
GitHub Action (every 15 min)
  → download Hypixel AH (~45 pages, no key)
  → read each pet's exact level / rarity / XP from its NBT
  → best buy level per pet, sell pinned to cap (200 dragons / 100 others), after AH tax
  → write payload → Cloudflare KV
pets.adong.dev reads KV (thin Worker, no external calls)
```

## How the numbers work

- **Sell** is always the cap level (Lvl 200 for the 4 dragons, Lvl 100 for everything else),
  priced from the cheapest current listing at that level.
- **Buy** is whichever lower level gives the best coins/XP, after Hypixel BIN tax
  (2.5%/2%/1% listing fee + 1% capped claim tax).
- XP is read **exactly** from each auction's NBT `petInfo.exp` — no XP-table guesswork.

## One-time setup (to enable the auto-refresh)

The scanner is already proven working and the site is live (seeded manually). To make it
refresh itself every 15 minutes, add three **GitHub Actions secrets** to whichever repo
holds `.github/workflows/pet-scan.yml`:

1. **`CF_ACCOUNT_ID`** = `b24b4165f348e8297cc5ad11001651f4`
2. **`KV_NAMESPACE_ID`** = `aa51de975fa6417fa48459bdea853a35`  (the PETFLIPS namespace)
3. **`CF_API_TOKEN`** = a Cloudflare API token with **Workers KV Storage : Edit**.
   Create at: Cloudflare dashboard → My Profile → API Tokens → Create Token →
   "Edit Cloudflare Workers" template (or a custom token with just *Account → Workers KV
   Storage → Edit*). Copy the token value.

Add them under the repo's **Settings → Secrets and variables → Actions → New repository secret**.

Then push this repo (with `.github/workflows/pet-scan.yml` and `tools/pet-flip/scanner/`),
open the **Actions** tab, and run **Pet flip scan → Run workflow** once to confirm it works.
After that it runs every 15 minutes on its own.

## Run it locally

```bash
cd tools/pet-flip/scanner
npm install

# dry run — writes payload.json and prints a summary, no Cloudflare needed
node scan.mjs

# write to KV directly (needs the three env vars above)
CF_ACCOUNT_ID=... KV_NAMESPACE_ID=... CF_API_TOKEN=... node scan.mjs
```
