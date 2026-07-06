// Pet-flip scanner — runs in GitHub Actions (free, GitHub IPs, full CPU).
//
// Downloads the entire Hypixel auction house (public, no API key), filters to BIN pet
// auctions, reads each pet's exact level/rarity/XP from its NBT, computes the best
// leveling flip per pet (buy low → grind to cap → resell) after Hypixel AH tax, and
// writes the finished payload to Cloudflare KV. The website reads that KV unchanged.
//
// Env (set as GitHub Actions secrets):
//   CF_API_TOKEN   - Cloudflare API token with "Workers KV Storage: Edit"
//   CF_ACCOUNT_ID  - Cloudflare account id
//   KV_NAMESPACE_ID- the PETFLIPS namespace id

import nbt from 'prismarine-nbt';

const AH = 'https://api.hypixel.net/v2/skyblock/auctions';
const DRAGONS = new Set(['ENDER_DRAGON', 'GOLDEN_DRAGON', 'JADE_DRAGON', 'ROSE_DRAGON']);
const maxLevel = (type) => (DRAGONS.has(type) ? 200 : 100);

/******** Hypixel AH tax (seller side, BIN) ********/
const listingFee = (p) => (p > 100e6 ? 0.025 : p >= 10e6 ? 0.02 : 0.01) * p;
function claimTax(p) {
  if (p <= 1e6) return 0;
  let t = 0.01 * p;
  if (p - t < 1e6) t = p - 1e6;
  return t;
}
const netSell = (p) => p - listingFee(p) - claimTax(p);

/******** Fetch the whole AH ********/
async function getJson(url) {
  const r = await fetch(url, { headers: { 'user-agent': 'pet-flip-scanner (adong.dev)' } });
  if (!r.ok) throw new Error(`HTTP ${r.status} for ${url}`);
  return r.json();
}

async function fetchAllAuctions() {
  const first = await getJson(`${AH}?page=0`);
  const total = first.totalPages;
  const all = [...first.auctions];
  // Fetch remaining pages with light concurrency.
  const CONC = 6;
  let next = 1;
  async function worker() {
    while (next < total) {
      const p = next++;
      try {
        const d = await getJson(`${AH}?page=${p}`);
        all.push(...d.auctions);
      } catch (e) {
        console.warn(`page ${p} failed: ${e.message}`);
      }
    }
  }
  await Promise.all(Array.from({ length: CONC }, worker));
  console.log(`fetched ${all.length} auctions across ${total} pages`);
  return all;
}

/******** Pet XP thresholds (NEU constants) ********/
// The XP needed to *reach* the cap level, not to match the sell listing's stored exp:
// pets keep accumulating exp past max level, so the cheapest cap-level listing is often
// over-ground by tens of millions — using its exp inflates "XP to grind" and understates
// coins/XP. Thresholds are exact game data from NotEnoughUpdates' public constants.
const NEU_PETS = 'https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/constants/pets.json';
let XP = null; // { offsets: {TIER: idx}, levels: [xp per level-up], custom: {TYPE: {type, pet_levels}} }

async function loadXpTable() {
  try {
    const d = await getJson(NEU_PETS);
    if (!d.pet_rarity_offset || !Array.isArray(d.pet_levels)) throw new Error('unexpected shape');
    XP = { offsets: d.pet_rarity_offset, levels: d.pet_levels, custom: d.custom_pet_leveling || {} };
    // Sanity-check against known values; a bad table is worse than the fallback.
    const gd200 = expToReach('GOLDEN_DRAGON', 'LEGENDARY', 200);
    const leg100 = expToReach('WOLF', 'LEGENDARY', 100);
    if (gd200 !== 210255385 || leg100 !== 25353230) {
      console.warn(`XP table failed sanity check (gd200=${gd200}, leg100=${leg100}) — falling back to sell-listing exp`);
      XP = null;
    }
  } catch (e) {
    console.warn(`XP table fetch failed (${e.message}) — falling back to sell-listing exp`);
  }
}

// Total XP needed to reach `level` from level 1 for this pet type + tier, or null if unknown.
function expToReach(type, tier, level) {
  if (!XP) return null;
  const off = XP.offsets[tier];
  if (off == null) return null;
  let total = 0;
  const base = Math.min(level, 100);
  for (let l = 1; l < base; l++) total += XP.levels[off + l - 1];
  if (level > 100) {
    const custom = XP.custom[type];
    // type 1 = extra per-level costs appended after level 100 (the 200-cap dragons)
    if (custom?.type !== 1 || !Array.isArray(custom.pet_levels)) return null;
    for (let i = 0; i < level - 100; i++) total += custom.pet_levels[i] ?? 0;
  }
  return total;
}

/******** Read level/rarity/exp from a pet auction's NBT ********/
async function readPet(auction) {
  const m = /\[Lvl (\d+)\]/.exec(auction.item_name || '');
  if (!m) return null; // not a pet
  const level = parseInt(m[1], 10);
  try {
    const buf = Buffer.from(auction.item_bytes, 'base64');
    const { parsed } = await nbt.parse(buf);
    const item = parsed.value.i.value.value[0];
    const petStr = item.tag?.value?.ExtraAttributes?.value?.petInfo?.value;
    if (!petStr) return null;
    const info = JSON.parse(petStr);
    if (!info.type) return null;
    return {
      type: info.type,
      tier: info.tier || auction.tier || 'UNKNOWN',
      exp: info.exp || 0,
      level,
      price: auction.starting_bid,
      uuid: auction.uuid,
    };
  } catch (e) {
    return null;
  }
}

/******** Per-pet flip analysis (sell pinned to the cap level) ********/
// Compute the flip for a single rarity tier, or null if it has no usable buy/sell pair.
function flipForTier(type, tier, list, cap) {
  const byLevel = {};
  for (const p of list) if (!byLevel[p.level] || p.price < byLevel[p.level].price) byLevel[p.level] = p;
  const sell = byLevel[cap];
  if (!sell) return null;
  const sellNet = netSell(sell.price);
  const buyLevels = Object.keys(byLevel).map(Number).filter((L) => L < cap).sort((a, b) => a - b);
  if (buyLevels.length === 0) return null;

  // Grind target: just crossing the cap threshold (exact table), not matching the
  // possibly over-ground exp stored on the sell listing.
  const capExp = expToReach(type, tier, cap);
  const rows = buyLevels.map((L) => {
    const a = byLevel[L];
    const xp = (capExp != null ? capExp : sell.exp) - a.exp;
    const profit = sellNet - a.price;
    return { level: L, price: a.price, uuid: a.uuid, exp: a.exp, xp, profit, perXp: xp > 0 ? profit / xp : null };
  });
  let bb = null;
  for (const r of rows) if (r.perXp !== null && (bb === null || r.perXp > bb.perXp)) bb = r;
  if (!bb) return null;
  return { tier, sell: { level: cap, price: sell.price, uuid: sell.uuid }, sellNet, best: bb, levels: rows, perXp: bb.perXp };
}

// Returns a pet with a flip for EVERY rarity that has one, plus a default = best coins/XP.
// Top-level fields mirror the default rarity (so ranking/sorting use the best one).
function analyse(type, name, pets) {
  const cap = maxLevel(type);
  const byTier = {};
  for (const p of pets) (byTier[p.tier] = byTier[p.tier] || []).push(p);

  const rarities = {};
  for (const tier of Object.keys(byTier)) {
    const flip = flipForTier(type, tier, byTier[tier], cap);
    if (flip) rarities[tier] = flip;
  }

  const tiers = Object.keys(rarities);
  const listings = pets.length;
  if (tiers.length === 0) return { name, tag: 'PET_' + type, listings, perXp: null, note: 'no usable buy/sell pair' };

  let def = tiers[0];
  for (const t of tiers) if (rarities[t].perXp > rarities[def].perXp) def = t;
  const d = rarities[def];
  return {
    name, tag: 'PET_' + type, listings, rarities, defaultTier: def,
    tier: def, sell: d.sell, sellNet: d.sellNet, best: d.best, perXp: d.perXp, levels: d.levels,
  };
}

function prettyName(type) {
  return type.toLowerCase().replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
}

/******** 7-day sold volume per level, from Coflnet's sold-auction history ********/
// Coflnet's /sold returns ~the last 1000 sales (≈up to 7 days). We count, per rarity+level,
// how many sold in the last 7 days — i.e. how liquid each level is.
const WEEK_MS = 7 * 24 * 60 * 60 * 1000;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function fetchSoldCounts(tag, cutoff) {
  try {
    const r = await fetch(`https://sky.coflnet.com/api/auctions/tag/${tag}/sold`, {
      headers: { accept: 'application/json', 'user-agent': 'pet-flip-scanner (adong.dev)' },
    });
    if (!r.ok) return null; // rate-limited or error: just omit sold data for this pet
    const sold = await r.json();
    const counts = {}; // tier -> { level -> count }, last 7 days
    let windowOldest = Infinity;
    for (const s of sold) {
      const t = Date.parse(s.end);
      if (isNaN(t)) continue;
      if (t < windowOldest) windowOldest = t;
      if (t < cutoff) continue;
      const tier = s.tier || 'UNKNOWN';
      const m = /\[Lvl (\d+)\]/.exec(s.itemName || '');
      if (!m) continue;
      const lvl = parseInt(m[1], 10);
      (counts[tier] = counts[tier] || {})[lvl] = (counts[tier]?.[lvl] || 0) + 1;
    }
    const capped = sold.length >= 1000 && windowOldest > cutoff;
    return { counts, capped };
  } catch (e) {
    return null;
  }
}

/******** Write the finished payload to Cloudflare KV ********/
async function putKV(key, value) {
  const { CF_API_TOKEN, CF_ACCOUNT_ID, KV_NAMESPACE_ID } = process.env;
  const url = `https://api.cloudflare.com/client/v4/accounts/${CF_ACCOUNT_ID}/storage/kv/namespaces/${KV_NAMESPACE_ID}/values/${key}`;
  const r = await fetch(url, {
    method: 'PUT',
    headers: { Authorization: `Bearer ${CF_API_TOKEN}`, 'content-type': 'text/plain' },
    body: value,
  });
  if (!r.ok) throw new Error(`KV put failed: HTTP ${r.status} ${await r.text()}`);
}

/******** Main ********/
async function main() {
  await loadXpTable();
  const auctions = await fetchAllAuctions();
  const bins = auctions.filter((a) => a.bin && /\[Lvl \d+\]/.test(a.item_name || ''));
  console.log(`pet BINs: ${bins.length}`);

  // Read NBT for each (concurrency-limited to keep memory sane).
  const byType = {};
  const CONC = 32;
  let idx = 0;
  async function worker() {
    while (idx < bins.length) {
      const a = bins[idx++];
      const pet = await readPet(a);
      if (pet) (byType[pet.type] = byType[pet.type] || []).push(pet);
    }
  }
  await Promise.all(Array.from({ length: CONC }, worker));

  const pets = Object.entries(byType)
    .map(([type, list]) => analyse(type, prettyName(type), list))
    .filter((p) => p.perXp !== null)
    .sort((a, b) => b.perXp - a.perXp);

  // Decorate each flip with 7-day sold counts (per level, for the flip's rarity).
  // Gentle sequential Coflnet calls with a small delay — fine from GitHub's IPs.
  const cutoff = Date.now() - WEEK_MS;
  for (const p of pets) {
    const sold = await fetchSoldCounts(p.tag, cutoff);
    if (sold) {
      for (const tier of Object.keys(p.rarities)) {
        const fl = p.rarities[tier];
        const c = sold.counts[tier] || {};
        for (const row of fl.levels) row.soldWeek = c[row.level] || 0;
        fl.sell.soldWeek = c[fl.sell.level] || 0;
        fl.best.soldWeek = c[fl.best.level] || 0;
      }
      p.soldWindowCapped = sold.capped;
    }
    await sleep(120);
  }

  const payload = {
    updated: new Date().toISOString(),
    lastFullSweep: new Date().toISOString(),
    source: 'hypixel',
    scanned: Object.keys(byType).length,
    total: Object.keys(byType).length,
    pets,
  };

  if (!process.env.CF_ACCOUNT_ID) {
    // Dry run (local): no KV creds — write payload.json locally and show a summary.
    const { writeFileSync } = await import('node:fs');
    writeFileSync('payload.json', JSON.stringify(payload));
    console.log(`DRY RUN — wrote payload.json — ${pets.length} flips from ${Object.keys(byType).length} pet types`);
    const dragons = pets.filter((p) => /Dragon/.test(p.name));
    console.log('Dragons:', dragons.map((p) => `${p.name} buyL${p.best.level}→sellL${p.sell.level} ${p.perXp.toFixed(2)}/xp`).join(' | ') || '(none)');
    console.log('Top 8:');
    for (const p of pets.slice(0, 8)) console.log(`  ${p.name.padEnd(20)} ${(p.tier||'').padEnd(10)} buyL${String(p.best.level).padStart(3)} sellL${p.sell.level} ${p.perXp.toFixed(3)}/xp`);
    return;
  }
  await putKV('payload', JSON.stringify(payload));
  console.log(`wrote ${pets.length} flips (from ${Object.keys(byType).length} pet types) to KV`);
}

main().catch((e) => { console.error(e); process.exit(1); });
