// Pet Flip Tracker — Cloudflare Worker (data-serving + page).
//
// This Worker is now intentionally thin: a GitHub Action (see ../scanner) does the
// heavy Hypixel auction-house scan on GitHub's IPs and writes the finished payload to
// KV. This Worker just serves the web page and returns that payload at /api/flips.
// No external API calls, no cron, no rate-limit exposure.

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (url.pathname === '/api/flips') {
      const payload = (await env.PETFLIPS.get('payload')) || '{"pets":[],"scanned":0,"total":0,"updated":null}';
      return new Response(payload, {
        headers: {
          'content-type': 'application/json',
          'cache-control': 'no-store',
          'access-control-allow-origin': '*',
        },
      });
    }

    return new Response(PAGE_HTML, { headers: { 'content-type': 'text/html; charset=utf-8' } });
  },

  // Reliable scan trigger. GitHub's own cron scheduler delays/drops runs under load
  // (observed firing ~hourly despite a */15 schedule); Cloudflare crons are punctual,
  // so this fires every 15 min and force-dispatches the scan workflow on GitHub.
  // Requires the GH_PAT secret (fine-grained PAT scoped to pet-flip-tracker with
  // Actions read+write): `wrangler secret put GH_PAT`. No-ops until it's set.
  async scheduled(event, env, ctx) {
    if (!env.GH_PAT) return;
    const r = await fetch(
      'https://api.github.com/repos/Rinity9801/pet-flip-tracker/actions/workflows/scan.yml/dispatches',
      {
        method: 'POST',
        headers: {
          authorization: `Bearer ${env.GH_PAT}`,
          accept: 'application/vnd.github+json',
          'user-agent': 'pet-flip-tracker-cron',
          'x-github-api-version': '2022-11-28',
        },
        body: JSON.stringify({ ref: 'main' }),
      }
    );
    // 204 = dispatched. Anything else is worth a log line (visible via `wrangler tail`).
    if (r.status !== 204) console.log(`scan dispatch failed: HTTP ${r.status} ${await r.text()}`);
  },
};

/******** Web page ********/
const PAGE_HTML = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Pet Flip Tracker</title>
<style>
  :root { color-scheme: dark; }
  * { box-sizing: border-box; }
  body { margin: 0; font: 14px/1.5 system-ui, sans-serif; background: #14141a; color: #e6e6ee; }
  header { padding: 18px 20px; border-bottom: 1px solid #2a2a36; display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
  h1 { font-size: 18px; margin: 0; }
  .muted { color: #9aa0ad; font-size: 12px; }
  button { background: #3a7bd5; color: #fff; border: 0; border-radius: 6px; padding: 7px 14px; cursor: pointer; font-weight: 600; }
  button:disabled { opacity: .5; cursor: default; }
  input.search { background: #11141c; border: 1px solid #2a2a36; border-radius: 6px; color: #e6e6ee; padding: 7px 10px; min-width: 180px; }
  main { padding: 20px; max-width: 1150px; margin: 0 auto; }
  .banner { background: #1f3a24; border: 1px solid #2f6b3a; border-radius: 8px; padding: 14px 16px; margin-bottom: 14px; font-size: 15px; }
  .banner a { color: #8fd0ff; }
  .progress { color: #9aa0ad; font-size: 12px; margin-bottom: 14px; }
  table { width: 100%; border-collapse: collapse; }
  th, td { text-align: right; padding: 8px 10px; border-bottom: 1px solid #24242e; white-space: nowrap; }
  th:nth-child(2), td:nth-child(2), th:nth-child(3), td:nth-child(3) { text-align: left; }
  th { color: #9aa0ad; font-weight: 600; font-size: 12px; text-transform: uppercase; letter-spacing: .03em; cursor: pointer; user-select: none; }
  tr.pet { cursor: pointer; }
  tr.pet:hover { background: #1c1c25; }
  tr.best td { background: #18301d; }
  .star { color: #ffd24a; }
  .tier { font-size: 11px; padding: 1px 6px; border-radius: 4px; background: #2a2a36; color: #c7b3ff; }
  select.rarity { background: #2a2a36; color: #c7b3ff; border: 1px solid #3a3a48; border-radius: 4px; font-size: 11px; padding: 2px 4px; text-transform: capitalize; cursor: pointer; }
  a { color: #8fd0ff; text-decoration: none; }
  a:hover { text-decoration: underline; }
  .levels { background: #0f0f14; }
  .levels table { font-size: 13px; }
  .levels td, .levels th { border-bottom: 1px solid #1c1c24; }
  .hidden { display: none; }
  .pos { color: #6fcf7f; } .neg { color: #e07a7a; }
  .err { color: #e07a7a; }
</style>
</head>
<body>
<header>
  <h1>🐾 Pet Flip Tracker</h1>
  <span class="muted" id="updated"></span>
  <input class="search" id="search" placeholder="filter pets…">
  <button id="refresh">Refresh</button>
  <span class="muted">All pets · best buy level &amp; resell, by coins per XP (after AH tax).</span>
</header>
<main>
  <div id="banner" class="banner">Loading…</div>
  <div id="progress" class="progress"></div>
  <table id="summary"><thead><tr>
    <th data-k="rank">#</th><th data-k="name">Pet</th><th data-k="tier">Rarity</th>
    <th data-k="listings">Listings</th><th data-k="buy">Buy Lvl</th><th data-k="price">Buy Price</th>
    <th data-k="sell">Sell Lvl</th><th data-k="net">Net After Tax</th><th data-k="xp">XP to Grind</th>
    <th data-k="profit">Profit</th><th data-k="perxp">Coins/XP</th><th data-k="liq" title="Number sold in the last 7 days at the sell (cap) level">Sell Sold/7d</th><th>Buy</th>
  </tr></thead><tbody id="rows"></tbody></table>
</main>
<script>
let DATA = null, sortKey = 'perxp', sortDir = -1, filter = '';
const picked = {}; // tag -> chosen rarity (overrides the pet's default)
const fmt0 = n => n == null ? '' : Math.round(n).toLocaleString();
const fmt2 = n => n == null ? '' : Number(n).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const compact = n => n >= 1e9 ? (n/1e9).toFixed(2)+'B' : n >= 1e6 ? (n/1e6).toFixed(2)+'M' : n >= 1e3 ? (n/1e3).toFixed(1)+'k' : ''+n;
const auc = (uuid, text) => uuid ? '<a href="https://sky.coflnet.com/auction/'+uuid+'" target="_blank" rel="noopener">'+text+'</a>' : '';
// 7-day sold count, color-coded for liquidity: red=thin (hard to buy/sell), green=plenty.
const liqCell = n => { if (n == null) return '<span class="muted">–</span>'; const c = n === 0 ? 'neg' : n < 5 ? '' : 'pos'; return '<span class="'+c+'">'+n+'</span>'; };

// The rarities map + the currently-shown flip for a pet (defaults to its best rarity).
const raritiesOf = p => p.rarities || { [p.tier]: { tier: p.tier, sell: p.sell, sellNet: p.sellNet, best: p.best, levels: p.levels, perXp: p.perXp } };
const activeTier = p => picked[p.tag] && raritiesOf(p)[picked[p.tag]] ? picked[p.tag] : (p.defaultTier || p.tier);
const view = p => raritiesOf(p)[activeTier(p)];

async function load() {
  const btn = document.getElementById('refresh');
  const orig = btn.textContent;
  btn.disabled = true; btn.textContent = 'Refreshing…';
  try {
    // Unique URL each time so we always read the current KV, never a cached copy.
    const res = await fetch('/api/flips?t=' + Date.now(), { cache: 'no-store' });
    DATA = await res.json();
    render();
  } catch (e) {
    document.getElementById('banner').innerHTML = '<span class="err">Failed to load: ' + e + '</span>';
  }
  btn.textContent = orig; btn.disabled = false;
}

function sortedPets() {
  let pets = DATA.pets.slice();
  if (filter) pets = pets.filter(p => p.name.toLowerCase().includes(filter));
  if (sortKey !== 'rank') {
    pets.sort((a, b) => {
      const get = p => { const v = view(p); return ({ name: p.name, tier: activeTier(p), listings: p.listings,
        buy: v.best.level, price: v.best.price, sell: v.sell.level, net: v.sellNet, xp: v.best.xp,
        profit: v.best.profit, perxp: v.perXp, liq: (v.sell.soldWeek ?? -1) }[sortKey]); };
      const av = get(a), bv = get(b);
      if (typeof av === 'string') return sortDir * av.localeCompare(bv);
      return sortDir * (av - bv);
    });
  }
  return pets;
}

// The cells of a pet's main row (everything after the rarity selector cell).
function rowCells(p, i) {
  const v = view(p);
  return '<td>' + p.listings + '</td>' +
    '<td>' + v.best.level + '</td><td>' + fmt0(v.best.price) + '</td>' +
    '<td>' + v.sell.level + '</td><td>' + fmt0(v.sellNet) + '</td>' +
    '<td>' + fmt0(v.best.xp) + '</td>' +
    '<td class="' + (v.best.profit >= 0 ? 'pos' : 'neg') + '">' + fmt0(v.best.profit) + '</td>' +
    '<td><b>' + fmt2(v.perXp) + '</b></td>' +
    '<td>' + liqCell(v.sell.soldWeek) + '</td>' +
    '<td>' + auc(v.best.uuid, 'buy ↗') + '</td>';
}

function raritySelect(p) {
  const tiers = Object.keys(raritiesOf(p));
  if (tiers.length <= 1) return '<span class="tier">' + (activeTier(p) || '').toLowerCase() + '</span>';
  const cur = activeTier(p);
  const opts = tiers.sort((a, b) => raritiesOf(p)[b].perXp - raritiesOf(p)[a].perXp)
    .map(t => '<option value="' + t + '"' + (t === cur ? ' selected' : '') + '>' + t.toLowerCase() + '</option>').join('');
  return '<select class="rarity" data-tag="' + p.tag + '">' + opts + '</select>';
}

function detailHtml(p) {
  const v = view(p);
  let inner = '<td></td><td colspan="12"><table><thead><tr>' +
    '<th>Buy Lvl</th><th>Cheapest</th><th>XP to Grind</th><th>Profit</th><th>Coins/XP</th><th>Sold/7d</th><th></th></tr></thead><tbody>';
  v.levels.forEach(l => {
    inner += '<tr>' +
      '<td>' + (l.level === v.best.level ? '<span class="star">★</span> ' : '') + l.level + '</td>' +
      '<td>' + fmt0(l.price) + '</td><td>' + fmt0(l.xp) + '</td>' +
      '<td class="' + (l.profit >= 0 ? 'pos' : 'neg') + '">' + fmt0(l.profit) + '</td>' +
      '<td>' + fmt2(l.perXp) + '</td>' +
      '<td>' + liqCell(l.soldWeek) + '</td>' +
      '<td>' + auc(l.uuid, 'open ↗') + '</td></tr>';
  });
  return inner + '</tbody></table></td>';
}

function render() {
  document.getElementById('updated').textContent = 'updated ' + new Date(DATA.updated).toLocaleTimeString();
  document.getElementById('progress').textContent =
    'Scanned ' + DATA.scanned + ' / ' + DATA.total + ' pets'
    + (DATA.lastFullSweep ? ' · last full sweep ' + new Date(DATA.lastFullSweep).toLocaleTimeString() : ' · first sweep in progress…');

  const list = sortedPets();
  const top = list[0];
  const banner = document.getElementById('banner');
  if (DATA.pets.length === 0) {
    banner.innerHTML = '<span class="muted">No flips computed yet — the first scan is still running, check back in a minute.</span>';
  } else if (top) {
    const v = view(top);
    banner.innerHTML = '<span class="star">★</span> <b>Buy ' + top.name + '</b> (' + activeTier(top).toLowerCase() + ') at Lvl ' + v.best.level +
      ' for ' + compact(v.best.price) + ' → grind to Lvl ' + v.sell.level +
      ' = <b>' + fmt2(v.perXp) + ' coins/XP</b> &nbsp; ' + auc(v.best.uuid, 'open auction ↗');
  }

  const rows = document.getElementById('rows');
  rows.innerHTML = '';
  list.forEach((p, i) => {
    const tr = document.createElement('tr');
    tr.className = 'pet' + (i === 0 ? ' best' : '');
    tr.dataset.tag = p.tag;
    tr.innerHTML =
      '<td>' + (i === 0 ? '<span class="star">★</span>1' : i + 1) + '</td>' +
      '<td>' + p.name + '</td>' +
      '<td>' + raritySelect(p) + '</td>' +
      rowCells(p, i);
    rows.appendChild(tr);

    const det = document.createElement('tr');
    det.className = 'levels hidden';
    det.dataset.det = p.tag;
    det.innerHTML = detailHtml(p);
    rows.appendChild(det);

    // Clicking the row toggles the breakdown — but not when using the rarity dropdown.
    tr.addEventListener('click', e => { if (!e.target.closest('select')) det.classList.toggle('hidden'); });
  });
}

// Rarity change (delegated once, so it survives in-place row rebuilds): update just
// that pet's row + breakdown to the chosen rarity. No re-sort, so the row doesn't jump.
document.getElementById('rows').addEventListener('change', e => {
  if (!e.target.matches('select.rarity')) return;
  const tag = e.target.dataset.tag;
  picked[tag] = e.target.value;
  const p = DATA.pets.find(x => x.tag === tag);
  const rows = document.getElementById('rows');
  const tr = rows.querySelector('tr.pet[data-tag="' + tag + '"]');
  const det = rows.querySelector('tr[data-det="' + tag + '"]');
  const i = [...rows.querySelectorAll('tr.pet')].indexOf(tr);
  tr.innerHTML = '<td>' + (i === 0 ? '<span class="star">★</span>1' : i + 1) + '</td><td>' + p.name + '</td><td>' + raritySelect(p) + '</td>' + rowCells(p, i);
  det.innerHTML = detailHtml(p);
});

document.querySelectorAll('th[data-k]').forEach(th => th.addEventListener('click', () => {
  const k = th.dataset.k;
  if (sortKey === k) sortDir *= -1; else { sortKey = k; sortDir = (k === 'name' || k === 'tier') ? 1 : -1; }
  render();
}));
document.getElementById('search').addEventListener('input', e => { filter = e.target.value.toLowerCase(); render(); });
document.getElementById('refresh').addEventListener('click', load);
load();
setInterval(load, 60000);
</script>
</body>
</html>`;
