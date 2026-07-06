/**
 * Pet-flip spreadsheet — pulls live BIN auctions from the Coflnet API for a set
 * of pets and works out the best leveling flip and the coins-per-XP for each.
 *
 * Setup: see README.md. In short — open a Google Sheet, Extensions ▸ Apps Script,
 * paste this file, Save, reload the sheet, then use the "Pet Flips" menu ▸ Refresh.
 *
 * Metric: you buy a low-level pet, grind it to a higher level, and resell.
 *   profit      = (sell price after AH tax) − (buy price)
 *   coins / XP  = profit ÷ (sell pet XP − buy pet XP)
 * Higher coins/XP = more coins earned per unit of pet XP you grind. Compare the
 * three pets to decide which is most worth leveling.
 */

/******************** CONFIG ********************/
const PETS = [
  { name: 'Golden Dragon',      tag: 'PET_GOLDEN_DRAGON' },
  { name: 'Rose Dragon',        tag: 'PET_ROSE_DRAGON' },
  { name: 'Legendary Mosquito', tag: 'PET_MOSQUITO' },
];
const RARITY = 'LEGENDARY';   // dragons are legendary-only; Mosquito spans rarities, so we filter
const MAX_PAGES = 40;         // active-BIN pages to scan per pet (10 listings/page). Raise if a pet
                              // shows "no LEGENDARY listings found" — legendaries sit on later pages.
const PAGE_SLEEP_MS = 250;    // pause between page requests to stay under the API rate limit

/******************** HYPIXEL AH TAX (seller side, BIN) ********************/
// BIN listing fee, paid up front when you create the auction:
//   2.5% if price > 100M, 2% if 10M–100M, else 1%.
function listingFee(price) {
  if (price > 100000000) return 0.025 * price;
  if (price >= 10000000) return 0.02 * price;
  return 0.01 * price;
}
// Claim tax, taken when the sale is collected: 1% on anything over 1M,
// capped so collecting never drops you below 1M coins.
function claimTax(price) {
  if (price <= 1000000) return 0;
  let tax = 0.01 * price;
  if (price - tax < 1000000) tax = price - 1000000;
  return tax;
}
// Net coins you actually keep from selling a BIN at `price`.
function netSell(price) {
  return price - listingFee(price) - claimTax(price);
}

/******************** DATA ********************/
function fetchJson(url) {
  const res = UrlFetchApp.fetch(url, { muteHttpExceptions: true, headers: { accept: 'application/json' } });
  if (res.getResponseCode() !== 200) return null;
  try { return JSON.parse(res.getContentText()); } catch (e) { return null; }
}

function getPetAuctions(tag) {
  const out = [];
  for (let p = 0; p < MAX_PAGES; p++) {
    const page = fetchJson('https://sky.coflnet.com/api/auctions/tag/' + tag + '/active/bin?page=' + p);
    if (!page || page.length === 0) break;
    for (const a of page) {
      let pet;
      try { pet = JSON.parse(a.nbtData.data.petInfo); } catch (e) { continue; }
      if (pet.tier !== RARITY) continue;
      const m = /\[Lvl (\d+)\]/.exec(a.itemName || '');
      out.push({
        level: m ? parseInt(m[1], 10) : 0,
        exp: pet.exp || 0,
        price: a.startingBid,
        uuid: a.uuid,
      });
    }
    Utilities.sleep(PAGE_SLEEP_MS);
  }
  return out;
}

// Clickable link to the auction's Coflnet page (shows the listing + in-game open command).
function auctionLink(uuid, text) {
  if (!uuid) return '';
  return '=HYPERLINK("https://sky.coflnet.com/auction/' + uuid + '","' + text + '")';
}

/******************** MAIN ********************/
function refreshPetFlips() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const summary = getOrCreate(ss, 'Summary');
  const detail = getOrCreate(ss, 'Auctions');
  summary.clear();
  detail.clear();
  const levelsSheet = getOrCreate(SpreadsheetApp.getActiveSpreadsheet(), 'Levels');
  levelsSheet.clear();
  detail.appendRow(['Pet', 'Level', 'Pet XP', 'Price', 'Price / XP', 'Link']);
  levelsSheet.appendRow(['Pet', 'Buy Lvl', 'Cheapest Price', 'Pet XP', 'XP to Grind',
    'Profit (net)', 'Coins / XP', 'Best?', 'Link']);

  const now = new Date();
  const results = [];

  for (const pet of PETS) {
    const aucs = getPetAuctions(pet.tag);
    if (aucs.length === 0) {
      results.push({ name: pet.name, listings: 0, perXp: null });
      continue;
    }

    // Cheapest listing at each level → the price curve you'd actually buy against.
    const byLevel = {};
    aucs.forEach(function (a) {
      if (!byLevel[a.level] || a.price < byLevel[a.level].price) byLevel[a.level] = a;
    });
    const levels = Object.keys(byLevel).map(Number).sort(function (x, y) { return x - y; });
    const sell = byLevel[levels[levels.length - 1]]; // cheapest pet at the top level present
    const sellNet = netSell(sell.price);

    // Evaluate buying at EACH level and grinding to the top — pick the best coins/XP.
    let bestBuy = null;
    const levelRows = [];
    levels.forEach(function (L) {
      const a = byLevel[L];
      const xp = sell.exp - a.exp;
      const profit = sellNet - a.price;
      const perXp = xp > 0 ? profit / xp : null;
      levelRows.push({ level: L, listing: a, xp: xp, profit: profit, perXp: perXp });
      if (perXp !== null && (bestBuy === null || perXp > bestBuy.perXp)) {
        bestBuy = { level: L, listing: a, xp: xp, profit: profit, perXp: perXp };
      }
    });

    results.push({
      name: pet.name, listings: aucs.length, sell: sell, sellNet: sellNet,
      bestBuy: bestBuy, perXp: bestBuy ? bestBuy.perXp : null, levelRows: levelRows,
    });

    // Levels tab: the full coins/XP-by-buy-level breakdown.
    levelRows.forEach(function (lr) {
      levelsSheet.appendRow([pet.name, lr.level, lr.listing.price, Math.round(lr.listing.exp),
        Math.round(lr.xp), Math.round(lr.profit),
        lr.perXp === null ? '' : Number(lr.perXp.toFixed(3)),
        (bestBuy && lr.level === bestBuy.level) ? '★' : '',
        auctionLink(lr.listing.uuid, 'open ↗')]);
    });

    // Auctions tab: every raw listing.
    aucs.sort(byPrice).forEach(function (a) {
      detail.appendRow([pet.name, a.level, Math.round(a.exp), a.price,
        a.exp > 0 ? Number((a.price / a.exp).toFixed(2)) : '',
        auctionLink(a.uuid, 'open ↗')]);
    });
  }

  // Rank pets: best coins/XP first; pets with no data sink to the bottom.
  results.sort(function (a, b) {
    if (a.perXp === null) return 1;
    if (b.perXp === null) return -1;
    return b.perXp - a.perXp;
  });

  // Recommendation banner (row 1), then header (row 2), then ranked rows.
  const best = results[0];
  if (best && best.perXp !== null) {
    summary.appendRow(['★ BUY: ' + best.name + ' at Lvl ' + best.bestBuy.level + ' @ '
      + compact(best.bestBuy.listing.price) + ' → grind to Lvl ' + best.sell.level + ', '
      + Math.round(best.perXp).toLocaleString() + ' coins/XP. Buy link in this row →',
      '', '', '', '', '', '', '', '', '', '', '', auctionLink(best.bestBuy.listing.uuid, 'BUY ↗')]);
  } else {
    summary.appendRow(['No flips found — try raising MAX_PAGES (Mosquito legendaries sit on later pages).']);
  }

  summary.appendRow(['Rank', 'Pet', 'Listings', 'Best Buy Lvl', 'Buy Price', 'Buy Link',
    'Sell Lvl', 'Sell Price', 'Net After Tax', 'XP to Grind', 'Profit', 'Coins / XP', 'Updated']);

  results.forEach(function (r, i) {
    if (r.perXp === null) {
      summary.appendRow(['', r.name, r.listings, '', '', '', '', '', '', '', '',
        r.listings === 0 ? 'no ' + RARITY + ' listings found' : 'only one level listed', now]);
      return;
    }
    summary.appendRow([
      (i === 0 ? '★ 1' : (i + 1)), r.name, r.listings, r.bestBuy.level, r.bestBuy.listing.price,
      auctionLink(r.bestBuy.listing.uuid, 'open ↗'), r.sell.level, r.sell.price,
      Math.round(r.sellNet), Math.round(r.bestBuy.xp), Math.round(r.bestBuy.profit),
      Number(r.perXp.toFixed(3)), now,
    ]);
  });

  format(summary, detail, levelsSheet);
  SpreadsheetApp.getUi().alert('Pet flips refreshed.\nBest: '
    + (best && best.perXp !== null ? best.name + ' at Lvl ' + best.bestBuy.level : 'none found'));
}

function byPrice(a, b) { return a.price - b.price; }

function compact(n) {
  if (n >= 1e9) return (n / 1e9).toFixed(2) + 'B';
  if (n >= 1e6) return (n / 1e6).toFixed(1) + 'M';
  if (n >= 1e3) return (n / 1e3).toFixed(0) + 'k';
  return '' + n;
}

/******************** SHEET HELPERS ********************/
function getOrCreate(ss, name) {
  return ss.getSheetByName(name) || ss.insertSheet(name);
}

function format(summary, detail, levels) {
  // Row 1 = recommendation banner, row 2 = header, data from row 3.
  summary.getRange('A1').setFontWeight('bold').setFontSize(12);
  summary.getRange('2:2').setFontWeight('bold');
  summary.setFrozenRows(2);
  summary.getRange('E3:E100').setNumberFormat('#,##0');   // Buy Price
  summary.getRange('H3:K100').setNumberFormat('#,##0');   // Sell..Profit
  summary.getRange('L3:L100').setNumberFormat('#,##0.000'); // Coins/XP
  summary.getRange('3:3').setBackground('#d9ead3');       // highlight the best (top-ranked) row
  summary.autoResizeColumns(1, 13);

  detail.getRange('C2:D5000').setNumberFormat('#,##0');
  detail.getRange('E2:E5000').setNumberFormat('#,##0.00');
  detail.setFrozenRows(1);
  detail.getRange('1:1').setFontWeight('bold');

  if (levels) {
    levels.getRange('C2:F5000').setNumberFormat('#,##0');
    levels.getRange('G2:G5000').setNumberFormat('#,##0.000');
    levels.setFrozenRows(1);
    levels.getRange('1:1').setFontWeight('bold');
    levels.autoResizeColumns(1, 9);
  }
}

/******************** MENU ********************/
function onOpen() {
  SpreadsheetApp.getUi()
    .createMenu('Pet Flips')
    .addItem('Refresh', 'refreshPetFlips')
    .addToUi();
}
