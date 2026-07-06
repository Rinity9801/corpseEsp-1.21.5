# Pet Flip Tracker — website

The same analysis as the spreadsheet, but as a live web page. A single Cloudflare
Worker serves the site **and** the data: it fetches Coflnet server-side (so no browser
CORS or rate-limit problems), works out the best buy level by coins/XP for each pet,
applies Hypixel AH tax, ranks the pets, and caches the result for ~3 minutes.

## What you get

- A **recommendation banner**: which pet to buy, at what level, for how much, and the coins/XP.
- A **ranked table** (best coins/XP first) with buy level, prices, net-after-tax, XP to grind, profit, and a direct **buy ↗** link.
- **Click any pet row** to expand its per-level breakdown (the `★` marks the recommended buy level).
- A **Refresh** button.

## Deploy

```bash
cd tools/pet-flip/website
npm install -g wrangler   # if you don't have it
wrangler login
wrangler deploy
```

Wrangler prints a URL like `https://pet-flip-tracker.<your-subdomain>.workers.dev` — that's your site. No secrets or config needed (the Coflnet API is public).

Local preview without deploying: `wrangler dev`.

## Tuning (top of `worker.js`)

- `PETS` — add/remove pets by Coflnet tag, and set `pages` per pet (10 listings/page).
  Dragons are legendary-only so a few pages cover them; Mosquito's legendaries sit behind
  many cheaper commons, so it gets more pages.
- **Subrequest budget:** Cloudflare's free plan allows 50 `fetch()` calls per request. The
  page counts (6 + 6 + 20 = 32) stay under that. If you add pets or raise `pages`, keep the
  total under ~45.
- `RESULT_TTL` — how long the computed result is cached (seconds). Lower = fresher, more API load.

## How it relates to the spreadsheet

Same metric and tax math as `../PetFlips.gs`, just rendered as a site instead of a Google
Sheet. Use whichever you prefer — the sheet is handy for keeping history; the site is
zero-setup once deployed and shareable by URL.
