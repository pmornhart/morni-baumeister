/**
 * Echter Test von Morni Baumeister mit Test-Spielern (mineflayer) auf Paper 26.1.2 – gestartet von test/run.sh.
 *   node baumeister-test.cjs <host> <port> <rcon-host:port> <rcon-passwort-datei> <server-ordner>
 *
 * Spieler: Anna (deutsch, Party-Leiterin mit Carl), Bert (englisch), Carl, Dora; in Runde 2 kommt Emil dazu, Fritz
 * schaut zu. Sprache und Party kommen vom Test-Netzwerk (test/fake-network), das Statistik, Zustand, „spielt gerade“,
 * „Platz freihalten“ und „zur Lobby“ in calls.log mitschreibt.
 *
 * Runde 1 (klassisch, vier Leute): Bauhütte, Abstimmung, Themen-Wahl (Fenster, Schnellleiste, Befehl), Bauen auf der
 * eigenen Parzelle, nichts außerhalb, zurück zur Parzelle, Zollstock, Werkzeugkiste (Füllen, Rückgängig, zu groß,
 * Boden, Uhrzeit, Wetter, Köpfe, Banner), Schutz (Schild mit Schimpfwort, TNT, Wasser- und Wesen-Grenze, Drache,
 * Wasser fließt nicht hinaus, Redstone-Uhr), Verbindung weg und rechtzeitig zurück, Bewertung (eigener Bau nicht,
 * weiter, sobald alle bewertet haben), Siegerehrung mit Podest und Meisterbrief, Statistik, Aufräumen.
 * Runde 2 (zu zweit, fünf Leute): Paare mit Party, Thema per Verwaltung, Zuschauer, zu spät zurück, Partner bewertet
 * den eigenen Bau nicht, Lobby-Knopf.
 *
 * Verfasst mit Claude 🤖
 */
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const { Vec3 } = require('vec3');
const { rcon } = require('./rcon.cjs');

const [HOST = '127.0.0.1', PORT = '29100', RCON = '127.0.0.1:29110', PASS_FILE, SERVER] = process.argv.slice(2);
const PASS = fs.readFileSync(PASS_FILE, 'utf8').trim();
const VERSION = process.env.MC_VERSION ?? '26.1';
const HALL = { x: 0.5, y: 101, z: 10.5 };
const COMEBACK = 20;
const FLOOR = 100;

const results = [];
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
function check(name, ok, detail = '') {
  results.push({ name, ok: Boolean(ok) });
  console.log(`${ok ? '✓' : '✗'} ${name}${detail ? ` – ${detail}` : ''}`);
}

/** Text aus Chat-Komponenten (String, JSON oder NBT) zusammensetzen. */
function plain(v) {
  if (v == null) return '';
  if (typeof v === 'string') {
    if (v.startsWith('{') || v.startsWith('[')) {
      try {
        return plain(JSON.parse(v));
      } catch {
        return v;
      }
    }
    return v;
  }
  if (Array.isArray(v)) return v.map(plain).join('');
  if (typeof v !== 'object') return String(v);
  if (v.type === 'compound') return plain(v.value);
  if (v.type === 'list') return plain(v.value?.value ?? []);
  if (v.type === 'string') return v.value;
  let s = '';
  if ('text' in v) s += plain(v.text);
  if ('' in v) s += plain(v['']);
  if ('translate' in v) s += plain(v.translate);
  if ('extra' in v) s += plain(v.extra);
  return s;
}

function offlineUuid(name) {
  const h = crypto.createHash('md5').update(`OfflinePlayer:${name}`, 'utf8').digest();
  h[6] = (h[6] & 0x0f) | 0x30;
  h[8] = (h[8] & 0x3f) | 0x80;
  const x = h.toString('hex');
  return `${x.slice(0, 8)}-${x.slice(8, 12)}-${x.slice(12, 16)}-${x.slice(16, 20)}-${x.slice(20)}`;
}

const cmd = (...commands) => rcon(RCON, PASS, commands);
const inWorld = (c) => `execute in minecraft:baumeister run ${c}`;
const passed = async (c) => /passed/i.test((await cmd(inWorld(c)))[0]);
const countOf = async (c) => {
  const m = /Count: (\d+)/i.exec((await cmd(inWorld(c)))[0]);
  return m ? Number(m[1]) : 0;
};
const isBlock = (x, y, z, b) => passed(`execute if block ${x} ${y} ${z} ${b}`);
const playerFile = (name) => path.join(SERVER, 'plugins/MorniBaumeister/spieler', `${offlineUuid(name)}.yml`);
const readFile = (file) => (fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '');
const calls = () =>
  readFile(path.join(SERVER, 'plugins/MoinMorni/calls.log'))
    .split('\n')
    .filter(Boolean)
    .map((l) => JSON.parse(l));
const callsOf = (t, name, from = 0) => calls().slice(from).filter((c) => c.t === t && c.name === name);

async function waitFor(cond, ms = 5000, step = 150) {
  const start = Date.now();
  while (Date.now() - start < ms) {
    if (await cond()) return true;
    await sleep(step);
  }
  return Boolean(await cond());
}

let lastJoin = 0;
async function join(name) {
  const wait = lastJoin + 3500 - Date.now();
  if (wait > 0) await sleep(wait);
  lastJoin = Date.now();
  const bot = mineflayer.createBot({ host: HOST, port: Number(PORT), username: name, version: VERSION, auth: 'offline' });
  bot.chats = [];
  bot.bars = [];
  bot.titles = [];
  bot.on('messagestr', (m, position) => (position === 'game_info' ? bot.bars : bot.chats).push(m));
  bot._client.on('action_bar', (p) => bot.bars.push(plain(p.text)));
  bot._client.on('set_title_text', (p) => bot.titles.push(plain(p.text)));
  bot._client.on('set_title_subtitle', (p) => bot.titles.push(plain(p.text)));
  bot.books = 0;
  bot._client.on('open_book', () => bot.books++);
  bot.bossPackets = 0;
  bot._client.on('boss_bar', () => bot.bossPackets++);
  bot.on('error', (e) => console.log(`  (${name}: ${e.message})`));
  await new Promise((resolve, reject) => {
    bot.once('spawn', resolve);
    bot.once('kicked', (r) => reject(new Error(`${name} gekickt: ${plain(r)}`)));
    setTimeout(() => reject(new Error(`${name}: kein Spawn`)), 25000);
  });
  await sleep(1500);
  return bot;
}

const slot = (bot, i) => bot.inventory.slots[36 + i];
const near = (p, q, d) => Math.abs(p.x - q.x) < d && Math.abs(p.y - q.y) < d && Math.abs(p.z - q.z) < d;
const inHall = (bot) => near(bot.entity.position, HALL, 5) && bot.game.gameMode === 'adventure';
const sees = (bot, re) => bot.chats.some((c) => re.test(c));
const sawTitle = (bot, re) => bot.titles.some((t) => re.test(t));
const sawBar = (bot, re) => bot.bars.some((t) => re.test(t));
const clear = (...bots) =>
  bots.forEach((b) => {
    b.chats.length = 0;
    b.bars.length = 0;
    b.titles.length = 0;
  });
const invHas = (bot, name) => bot.inventory.items().some((i) => i.name === name);

/** /baumeisteradmin status als Werte. */
async function status() {
  const lines = (await cmd('baumeisteradmin status'))[0].split('\n');
  const out = { info: {}, games: [] };
  for (const part of lines[0].split(' ')) {
    const [k, v] = part.split('=');
    if (v !== undefined) out.info[k] = v;
  }
  for (const line of lines.slice(1)) {
    if (!line.startsWith('bauplatz=')) continue;
    const g = { plots: {}, results: {}, players: {} };
    for (const part of line.split(' ')) {
      const [k, v] = part.split('=');
      if (v === undefined) continue;
      let m;
      if ((m = /^parzelle(\d+)$/.exec(k))) {
        const [minX, minZ, maxX, maxZ, maxY, state, liquids, time, rain] = v.split(':');
        g.plots[m[1]] = { minX: +minX, minZ: +minZ, maxX: +maxX, maxZ: +maxZ, maxY: +maxY, built: state === 'bebaut',
          liquids: +liquids, time: +time, rain: rain === '1' };
      } else if ((m = /^ergebnis(\d+)$/.exec(k))) {
        const [place, avg, votes] = v.split(':').map(Number);
        g.results[m[1]] = { place, avg, votes };
      } else if (/^[A-Z]/.test(k)) {
        if (v === 'zuschauer') {
          g.players[k] = { state: 'zuschauer' };
        } else {
          const [plot, state, blocks, given] = v.split(':');
          g.players[k] = { plot: +plot, state, blocks: +blocks, given: +given };
        }
      } else if (k === 'vorschlaege') {
        g.options = v.split(',').map((o) => ({ id: o.split(':')[0], votes: +o.split(':')[1] }));
      } else if (k === 'reihe') {
        g.order = v === '-' ? [] : v.split(',').map(Number);
      } else {
        g[k] = /^-?\d+$/.test(v) ? Number(v) : v;
      }
    }
    out.games.push(g);
  }
  return out;
}
const game = async () => (await status()).games[0];

async function openWithItem(bot, hotbar) {
  bot.setQuickBarSlot(hotbar);
  await sleep(250);
  const opened = new Promise((resolve) => {
    bot.once('windowOpen', resolve);
    setTimeout(() => resolve(null), 4000);
  });
  bot.activateItem();
  return opened;
}

async function useItem(bot, hotbar) {
  bot.setQuickBarSlot(hotbar);
  await sleep(200);
  bot.activateItem();
  await sleep(250);
  bot.deactivateItem();
}

async function click(bot, s) {
  const next = new Promise((resolve) => {
    bot.once('windowOpen', resolve);
    setTimeout(() => resolve(null), 2500);
  });
  await bot.clickWindow(s, 0, 0);
  return next;
}

function closeWin(bot) {
  if (bot.currentWindow) bot.closeWindow(bot.currentWindow);
}

async function tp(bot, x, y, z, yaw = 0, pitch = 0) {
  await cmd(inWorld(`tp ${bot.username} ${x} ${y} ${z} ${yaw} ${pitch}`));
  await waitFor(() => near(bot.entity.position, { x, y, z }, 1.5), 3000, 100);
  await sleep(300);
}

async function give(bot, hotbar, name, count = 1) {
  const Item = require('prismarine-item')(bot.registry);
  await bot.creative.setInventorySlot(36 + hotbar, new Item(bot.registry.itemsByName[name].id, count));
  bot.setQuickBarSlot(hotbar);
  await sleep(300);
}

/** Block auf die Oberseite von (x, y, z) setzen (true, wenn er danach dort steht). */
async function place(bot, x, y, z, name) {
  for (let attempt = 0; attempt < 2; attempt++) {
    const ref = bot.blockAt(new Vec3(x, y, z));
    try {
      await bot.placeBlock(ref, new Vec3(0, 1, 0));
    } catch {
      // abgelehnt oder Zeitüberschreitung – geprüft wird am Block
    }
    await sleep(400);
    if (await isBlock(x, y + 1, z, name)) return true;
  }
  return false;
}

const center = (p) => ({ x: Math.floor((p.minX + p.maxX + 1) / 2), z: Math.floor((p.minZ + p.maxZ + 1) / 2) });

async function main() {
  // ------------------------------------------------------------------ Bauhütte
  await sleep(5000);
  const holos = String(await cmd(inWorld('execute positioned 0 101 0 if entity @e[type=text_display,distance=..40]')));
  check('Tafeln in der Bauhütte stehen (4 Tafeln × 2 Sprachen)', /: 8\b/.test(holos), holos.trim());
  const anna = await join('Anna');
  const bert = await join('Bert');
  check('Ankunft in der Bauhütte (Abenteuermodus)', inHall(anna), JSON.stringify(anna.entity.position));
  check('Schnellleiste: Kompass, Farbstoff, Buch, Gold, Fernrohr, Bett',
    ['compass', 'gray_dye', 'writable_book', null, 'gold_ingot', null, 'spyglass', null, 'red_bed']
      .every((n, i) => n === null || slot(anna, i)?.name === n), [0, 1, 2, 4, 6, 8].map((i) => slot(anna, i)?.name).join(','));
  check('Deutsch: „Spielart wählen“', plain(slot(anna, 0)?.customName).includes('Spielart wählen'), plain(slot(anna, 0)?.customName));
  check('Englisch (Sprache vom Netzwerk): „Choose game type“', plain(slot(bert, 0)?.customName).includes('Choose game type'));
  check('Begrüßung im Chat (de/en)', sees(anna, /Servus in der Bauhütte/) && sees(bert, /Builders' Lodge/));
  check('Netzwerk: Zustand „waiting“', calls().some((c) => c.t === 'state' && c.state === 'waiting'));

  let win = await openWithItem(anna, 0);
  check('Kompass öffnet „Welche Spielart?“ mit drei Spielarten', win && /Welche Spielart/.test(plain(win.title)) &&
    [11, 13, 15].every((i) => win.slots[i]), win && plain(win.title));
  await anna.clickWindow(11, 0, 0);
  await sleep(600);
  check('Stimme für „Klassisch“', sees(anna, /Deine Stimme: Klassisch/), anna.chats.slice(-1)[0]);
  bert.chat('/spielart schnell');
  await sleep(800);
  check('Bert stimmt per Befehl (englisch)', sees(bert, /Your vote: Quick/), bert.chats.slice(-1)[0]);
  win = await openWithItem(anna, 2);
  check('Buch öffnet „So geht Baumeister“ mit sechs Kapiteln', win && /So geht Baumeister/.test(plain(win.title)) &&
    [10, 11, 12, 14, 15, 16].every((i) => win.slots[i]));
  await anna.clickWindow(14, 0, 0);
  await sleep(500);
  check('Kapitel „Bewerten“ erklärt sich im Chat', sees(anna, /Bewerten: Ihr werdet zu jedem Bau/));
  closeWin(anna);
  clear(anna, bert);
  await sleep(1500);
  check('Kein Countdown mit 2 von 3 nötigen Leuten', !anna.bars.some((b) => /Nächste Runde/.test(b)), anna.bars.slice(-1)[0]);

  // ------------------------------------------------------------------ Runde 1: Start und Themen-Wahl
  const carl = await join('Carl');
  const dora = await join('Dora');
  carl.chat('/spielart klassisch');
  dora.chat('/spielart klassisch');
  check('Mit 4 Leuten läuft der Countdown (Klassisch)', await waitFor(() => anna.bars.some((b) =>
    /Nächste Runde in \d+ s · Klassisch/.test(b)), 5000), anna.bars.slice(-1)[0]);
  const four = [anna, bert, carl, dora];
  for (const b of four) {
    b.setQuickBarSlot(1);
    await sleep(150);
    b.activateItem();
  }
  check('Start-Nachricht', await waitFor(() => sees(anna, /Auf geht's! Spielart: Klassisch · 4 Leute/), 12000),
    anna.chats.slice(-2).join(' | '));
  let g = await game();
  check('Themen-Wahl mit drei Vorschlägen', g?.phase === 'THEME' && g.options?.length === 3, JSON.stringify(g?.options));
  check('Jede Person hat eine eigene Parzelle', new Set(['Anna', 'Bert', 'Carl', 'Dora'].map((n) => g.players[n].plot)).size === 4);
  const plotOf = (n) => g.plots[g.players[n].plot];
  check('Alle stehen auf ihrer Parzelle', await waitFor(() => four.every((b) => {
    const p = plotOf(b.username);
    const pos = b.entity.position;
    return pos.x >= p.minX && pos.x <= p.maxX + 1 && pos.z >= p.minZ && pos.z <= p.maxZ + 1;
  }), 4000));
  check('Netzwerk: Zustand „running“ und alle vier „spielen“', calls().some((c) => c.t === 'state' && c.state === 'running') &&
    ['Anna', 'Bert', 'Carl', 'Dora'].every((n) => callsOf('playing', n).some((c) => c.playing === true)));
  check('Themen-Fenster öffnet sich von selbst', await waitFor(() => anna.currentWindow && /Welches Thema/.test(plain(anna.currentWindow.title)), 3000),
    anna.currentWindow && plain(anna.currentWindow.title));
  check('Themen in der Schnellleiste (Papier, Rechtsklick)', slot(bert, 2)?.name === 'paper' && slot(bert, 4)?.name === 'paper' &&
    slot(bert, 6)?.name === 'paper');
  await anna.clickWindow(11, 0, 0);
  await sleep(500);
  closeWin(anna);
  for (const b of [bert, carl, dora]) closeWin(b);
  await useItem(bert, 2);
  carl.chat('/thema 1');
  dora.chat('/thema 2');
  await sleep(1200);
  g = await game();
  check('Stimmen: Fenster, Schnellleiste und Befehl zählen', g.options[0].votes === 3 && g.options[1].votes === 1,
    JSON.stringify(g.options));
  check('Bestätigung „Deine Stimme“ (de/en)', sees(anna, /Deine Stimme:/) && sees(bert, /Your vote:/));
  const theme = g.options[0].id;
  await cmd('baumeisteradmin vorspulen weiter');
  check('Die Mehrheit gewinnt: Thema steht fest', await waitFor(async () => (await game()).phase === 'BUILD', 4000) &&
    (await game()).thema === theme && await waitFor(() => sees(anna, /Das Thema ist .* \(3 Stimmen\)/), 2000), theme);
  check('Bauen: Kreativmodus, Zollstock und Werkzeugkiste', await waitFor(() => anna.game.gameMode === 'creative' &&
    slot(anna, 7)?.name === 'blaze_rod' && slot(anna, 8)?.name === 'nether_star', 3000),
  `${anna.game.gameMode} ${slot(anna, 7)?.name} ${slot(anna, 8)?.name}`);
  check('Titel mit Thema und Leiste oben', sawTitle(anna, /\S/) && anna.bossPackets > 0);

  // ------------------------------------------------------------------ Bauen
  const pa = plotOf('Anna');
  const ca = center(pa);
  await tp(anna, ca.x + 0.5, FLOOR + 1, ca.z + 3.5, 180, 30);
  await give(anna, 0, 'stone', 64);
  check('Anna setzt einen Block auf ihrer Parzelle', await place(anna, ca.x, FLOOR, ca.z + 2, 'stone'));
  check('Gesetzte Blöcke werden gezählt', await waitFor(async () => (await game()).players.Anna.blocks >= 1, 2000));
  await tp(anna, pa.maxX + 0.5, FLOOR + 1, ca.z + 0.5, -90, 40);
  check('Auf dem Rand (außerhalb der Baufläche) geht nichts', !(await place(anna, pa.maxX + 1, FLOOR, ca.z, 'stone')) &&
    await waitFor(() => sawBar(anna, /nur auf deiner eigenen Parzelle/), 2000), anna.bars.slice(-1)[0]);
  await tp(anna, pa.maxX + 7.5, FLOOR + 2, ca.z + 0.5, -90, 0);
  anna.setControlState('forward', true);
  await sleep(1500);
  anna.setControlState('forward', false);
  check('Wer zu weit fliegt, landet wieder in der Mitte der Parzelle', await waitFor(() =>
    Math.abs(anna.entity.position.x - (ca.x)) < 8, 3000), JSON.stringify(anna.entity.position));
  // Zollstock und Werkzeugkiste
  await tp(anna, ca.x - 3, FLOOR + 1, ca.z - 3, 0, 60);
  clear(anna);
  anna.setQuickBarSlot(7);
  await sleep(300);
  const c1 = anna.blockAt(new Vec3(ca.x - 2, FLOOR, ca.z - 2));
  await anna.lookAt(c1.position.offset(0.5, 1, 0.5), true);
  anna.dig(c1, true).catch(() => {});
  await sleep(500);
  anna.stopDigging();
  const c2 = anna.blockAt(new Vec3(ca.x - 5, FLOOR, ca.z - 5));
  await anna.lookAt(c2.position.offset(0.5, 1, 0.5), true);
  await anna.activateBlock(c2).catch(() => {});
  await sleep(600);
  check('Zollstock: erste und zweite Ecke', sawBar(anna, /Erste Ecke/) && sawBar(anna, /Zweite Ecke .* 16 Blöcke/),
    anna.bars.slice(-3).join(' | '));
  check('Zollstock baut nichts ab', await isBlock(ca.x - 2, FLOOR, ca.z - 2, 'grass_block'));
  anna.setQuickBarSlot(0);
  win = await openWithItem(anna, 8);
  check('Stern öffnet die Werkzeugkiste', win && /Werkzeugkiste/.test(plain(win.title)), win && plain(win.title));
  anna.setQuickBarSlot(0);
  win = await click(anna, 30);
  check('Füllen: Blöcke aus der Schnellleiste zur Wahl', win && /Füllen mit/.test(plain(win.title)) && win.slots[9]?.name === 'stone');
  await anna.clickWindow(9, 0, 0);
  await sleep(700);
  check('Füllen: 16 Blöcke Stein', sees(anna, /Gefüllt: 16 Blöcke/) && await isBlock(ca.x - 3, FLOOR, ca.z - 4, 'stone'),
    anna.chats.slice(-1)[0]);
  anna.chat('/werkzeug');
  win = await new Promise((r) => {
    anna.once('windowOpen', r);
    setTimeout(() => r(null), 3000);
  });
  await anna.clickWindow(34, 0, 0);
  await sleep(700);
  check('Rückgängig: wieder Gras', sees(anna, /Zurückgenommen: 16 Blöcke/) && await isBlock(ca.x - 3, FLOOR, ca.z - 4, 'grass_block'));
  // zu groß (Grenze im Test: 16): 5 × 5 Blöcke
  await tp(anna, ca.x + 0.5, FLOOR + 1, ca.z + 0.5, 0, 60);
  anna.setQuickBarSlot(7);
  await sleep(200);
  for (const [dx, left] of [[-2, true], [2, false]]) {
    const b = anna.blockAt(new Vec3(ca.x + dx, FLOOR, ca.z + dx));
    await anna.lookAt(b.position.offset(0.5, 1, 0.5), true);
    if (left) {
      anna.dig(b, true).catch(() => {});
      await sleep(500);
      anna.stopDigging();
    } else {
      await anna.activateBlock(b).catch(() => {});
    }
    await sleep(400);
  }
  win = await openWithItem(anna, 8);
  await click(anna, 32);
  await sleep(600);
  check('Zu großer Bereich wird abgelehnt', sees(anna, /zu groß \(25 Blöcke, höchstens 16\)/), anna.chats.slice(-1)[0]);
  win = await openWithItem(anna, 8);
  win = await click(anna, 10);
  check('Boden-Fenster mit vielen Böden', win && /Boden wählen/.test(plain(win.title)) && win.slots[5]?.name === 'sand');
  await anna.clickWindow(5, 0, 0);
  await sleep(700);
  check('Boden der ganzen Parzelle ist jetzt Sand', await isBlock(pa.minX, FLOOR, pa.minZ, 'sand') &&
    await isBlock(pa.maxX, FLOOR, pa.maxZ, 'sand') && await isBlock(pa.maxX + 1, FLOOR, pa.maxZ, 'stripped_spruce_log'));
  win = await openWithItem(anna, 8);
  win = await click(anna, 12);
  await anna.clickWindow(16, 0, 0);
  await sleep(500);
  win = await openWithItem(anna, 8);
  await anna.clickWindow(14, 0, 0);
  await sleep(600);
  closeWin(anna);
  g = await game();
  check('Uhrzeit Nacht und Regen nur für die eigene Parzelle', g.plots[g.players.Anna.plot].time === 3 &&
    g.plots[g.players.Anna.plot].rain && sees(anna, /Uhrzeit deiner Parzelle: Nacht/) && sees(anna, /regnet es/));
  win = await openWithItem(anna, 8);
  win = await click(anna, 16);
  check('Köpfe: Wesen und alle Bauleute', win && /Köpfe/.test(plain(win.title)) && win.slots[0]?.name === 'skeleton_skull' &&
    [9, 10, 11, 12].every((i) => win.slots[i]?.name === 'player_head'));
  await anna.clickWindow(0, 0, 0);
  await sleep(500);
  closeWin(anna);
  win = await openWithItem(anna, 8);
  win = await click(anna, 28);
  check('Banner mit eigenen Mustern', win && /Banner/.test(plain(win.title)) && win.slots[0]?.name === 'white_banner');
  await anna.clickWindow(0, 0, 0);
  await sleep(500);
  closeWin(anna);
  check('Kopf und Banner landen im Inventar', invHas(anna, 'skeleton_skull') && invHas(anna, 'white_banner'));

  // ------------------------------------------------------------------ Schutz
  clear(anna);
  await tp(anna, ca.x + 0.5, FLOOR + 1, ca.z + 3.5, 180, 30);
  await give(anna, 1, 'tnt', 1);
  check('TNT gibt es nicht', !(await place(anna, ca.x + 1, FLOOR, ca.z + 2, 'tnt')) &&
    await waitFor(() => sawBar(anna, /gibt es beim Baumeister nicht/), 2000));
  await give(anna, 1, 'oak_sign', 4);
  const placedSign = await place(anna, ca.x - 1, FLOOR, ca.z + 2, 'oak_sign');
  if (placedSign) {
    anna.updateSign(anna.blockAt(new Vec3(ca.x - 1, FLOOR + 1, ca.z + 2)), 'du\narsch');
  }
  await sleep(1000);
  check('Schild mit Schimpfwort wird abgelehnt', placedSign && sees(anna, /Dieser Text geht so nicht/) &&
    !(await passed(`execute if block ${ca.x - 1} ${FLOOR + 1} ${ca.z + 2} oak_sign{front_text:{messages:['"du"','"arsch"','""','""']}}`)),
  anna.chats.slice(-1)[0]);
  clear(anna);
  await give(anna, 1, 'water_bucket', 1);
  for (const dx of [-2, 0, 2]) {
    const b = anna.blockAt(new Vec3(ca.x + dx, FLOOR, ca.z + 5));
    await anna.lookAt(b.position.offset(0.5, 1, 0.5), true);
    await sleep(200);
    anna.activateItem();
    await sleep(600);
    await give(anna, 1, 'water_bucket', 1);
  }
  g = await game();
  check('Höchstens 2 Wasserquellen je Parzelle (Test-Grenze)', g.plots[g.players.Anna.plot].liquids === 2 &&
    sees(anna, /Mehr als 2 Wasser- oder Lavaquellen/), `${g.plots[g.players.Anna.plot].liquids} ${anna.chats.slice(-1)[0]}`);
  clear(anna);
  for (let i = 0; i < 5; i++) {
    await cmd(inWorld(`summon cow ${ca.x + 0.5} ${FLOOR + 1} ${ca.z - 8.5}`));
  }
  await sleep(600);
  const cows = await countOf(`execute if entity @e[type=cow,x=${pa.minX},y=${FLOOR},z=${pa.minZ},dx=${pa.maxX - pa.minX},dy=40,dz=${pa.maxZ - pa.minZ}]`);
  check('Höchstens 3 Wesen je Parzelle (Test-Grenze)', cows === 3 && sees(anna, /Mehr als 3 Tiere und Dinge/), `${cows} Kühe`);
  await cmd(inWorld(`summon ender_dragon ${ca.x + 0.5} ${FLOOR + 5} ${ca.z + 0.5}`));
  await cmd(inWorld(`summon cow ${pa.maxX + 20} ${FLOOR + 1} ${ca.z}`));
  await sleep(500);
  check('Kein Drache, keine Wesen außerhalb der Parzellen', await countOf('execute if entity @e[type=ender_dragon]') === 0 &&
    await countOf(`execute if entity @e[type=cow,x=${pa.maxX + 15},y=${FLOOR},z=${ca.z - 5},dx=10,dy=10,dz=10]`) === 0);
  await cmd(inWorld(`setblock ${pa.maxX} ${FLOOR + 1} ${pa.minZ + 1} water`));
  await sleep(3000);
  check('Wasser fließt nicht aus der Parzelle', await isBlock(pa.maxX - 1, FLOOR + 1, pa.minZ + 1, 'water') &&
    await isBlock(pa.maxX + 1, FLOOR + 1, pa.minZ + 1, 'air'));
  clear(anna);
  const rx = pa.minX + 3;
  const rz = pa.minZ + 3;
  // Dauer-Schaltung: ein Redstone-Block neben drei Stück Redstone wird viele Male pro Sekunde gesetzt und
  // weggenommen (wie eine schnelle Redstone-Uhr) – drei Sekunden lang über der Grenze, dann baut der Schutz ab.
  await cmd(...[1, 2, 3].map((i) => inWorld(`setblock ${rx + i} ${FLOOR + 1} ${rz} redstone_wire`)));
  for (let t = 0; t < 16; t++) {
    const batch = [];
    for (let k = 0; k < 6; k++) {
      batch.push(inWorld(`setblock ${rx} ${FLOOR + 1} ${rz} redstone_block`), inWorld(`setblock ${rx} ${FLOOR + 1} ${rz} air`));
    }
    await cmd(...batch);
    await sleep(250);
  }
  check('Redstone-Uhr wird erst gebremst, dann abgebaut', await waitFor(() => sees(anna, /Redstone-Schaltung lief ohne Pause/), 12000, 300),
    anna.chats.slice(-1)[0]);
  check('Das Redstone daneben ist abgebaut', !(await isBlock(rx + 1, FLOOR + 1, rz, 'redstone_wire')));

  // Die anderen bauen auch (Dora baut nichts).
  for (const b of [bert, carl]) {
    const p = plotOf(b.username);
    const c = center(p);
    await tp(b, c.x + 0.5, FLOOR + 1, c.z + 3.5, 180, 30);
    await give(b, 0, b === bert ? 'oak_planks' : 'bricks', 64);
    await place(b, c.x, FLOOR, c.z + 1, b === bert ? 'oak_planks' : 'bricks');
    await place(b, c.x + 1, FLOOR, c.z + 1, b === bert ? 'oak_planks' : 'bricks');
  }
  g = await game();
  check('Bert und Carl haben gebaut, Dora nicht', g.players.Bert.blocks >= 1 && g.players.Carl.blocks >= 1 &&
    g.players.Dora.blocks === 0 && !g.plots[g.players.Dora.plot].built, JSON.stringify(g.players));

  // Verbindung weg und rechtzeitig zurück.
  let mark = calls().length;
  clear(anna);
  carl.quit();
  check(`Carl weg: Parzelle bleibt ${COMEBACK} s frei`, await waitFor(() => callsOf('hold-slot', 'Carl', mark).some((c) => c.seconds === COMEBACK), 5000) &&
    await waitFor(() => sees(anna, /Carl hat die Verbindung verloren/), 2000));
  await sleep(3000);
  const carl2 = await join('Carl');
  const pc = plotOf('Carl');
  check('Carl rechtzeitig zurück: wieder auf seiner Parzelle, Kreativmodus', await waitFor(() => carl2.game.gameMode === 'creative' &&
    carl2.entity.position.x >= pc.minX - 8 && carl2.entity.position.x <= pc.maxX + 8, 5000) &&
    sees(carl2, /Willkommen zurück/), `${carl2.game.gameMode} ${JSON.stringify(carl2.entity.position)}`);
  check('Netzwerk: Angebot zurückgenommen, spielt wieder', callsOf('hold-slot', 'Carl', mark).some((c) => c.seconds === 0) &&
    callsOf('playing', 'Carl', mark).some((c) => c.playing === true) && sees(anna, /Carl ist wieder da/));
  check('Carls Bau steht noch', await isBlock(center(pc).x, FLOOR + 1, center(pc).z + 1, 'bricks'));

  // ------------------------------------------------------------------ Bewertung
  const players1 = [anna, bert, carl2, dora];
  clear(...players1);
  await cmd('baumeisteradmin vorspulen weiter');
  check('Bewertung beginnt: Titel, Abenteuermodus, fliegen', await waitFor(async () => (await game()).phase === 'RATE', 4000) &&
    await waitFor(() => sawTitle(anna, /Bewertung/) && anna.game.gameMode === 'adventure', 3000), anna.titles.join(' | '));
  g = await game();
  check('Nur bebaute Parzellen werden gezeigt (Dora hat nichts gebaut)', g.order.length === 3 &&
    !g.order.includes(g.players.Dora.plot), JSON.stringify(g.order));
  const plan = { Anna: 5, Bert: 3, Carl: 4 };
  const ownerOf = (plot) => Object.keys(g.players).find((n) => g.players[n].plot === plot);
  let rounds = 0;
  let ownBlocked = false;
  let viewOk = true;
  let earlyNext = true;
  let barDe = false;
  let barEn = false;
  for (let i = 0; i < g.order.length; i++) {
    const cur = await game();
    const plot = cur.gezeigt;
    const owner = ownerOf(plot);
    const stars = plan[owner];
    await sleep(800);
    const p = cur.plots[plot];
    for (const b of players1) {
      const pos = b.entity.position;
      if (Math.hypot(pos.x - (p.minX + p.maxX + 1) / 2, pos.z - (p.maxZ + 7.5)) > 3) viewOk = false;
    }
    for (const b of players1) {
      if (b.username === owner) {
        clear(b);
        b.chat('/bewerten 5');
        await sleep(400);
        if (sees(b, /bewerten die anderen|others rate/) && slot(b, 4)?.name === 'oak_sign') ownBlocked = true;
      } else if (b === bert) {
        b.chat(`/bewerten ${stars}`);
      } else {
        await useItem(b, stars - 1);
      }
    }
    await sleep(500);
    barDe = barDe || sawBar(anna, /Deine Wertung: ★/);
    barEn = barEn || sawBar(bert, /Your rating: ★/);
    // Alle haben bewertet → nach drei Sekunden geht es von selbst weiter.
    const moved = await waitFor(async () => {
      const s = await game();
      return s.gezeigt !== plot || s.phase !== 'RATE';
    }, 8000, 300);
    if (!moved) {
      earlyNext = false;
      await cmd('baumeisteradmin vorspulen weiter');
    }
    rounds++;
  }
  check('Beim Bewerten stehen alle vor dem gezeigten Bau', viewOk);
  check('Den eigenen Bau bewertet man nicht (Schild statt Wolle)', ownBlocked);
  check('Haben alle bewertet, geht es von selbst zum nächsten Bau', earlyNext && rounds === 3);
  check('Wolle in der Schnellleiste und /bewerten zählen', barDe && barEn);

  // ------------------------------------------------------------------ Siegerehrung
  check('Siegerehrung: Anna gewinnt mit 5,00', await waitFor(async () => (await game())?.phase === 'AWARD', 6000) &&
    await waitFor(() => sees(anna, /1\. Anna – 5,00 ★/), 3000), anna.chats.slice(-4).join(' | '));
  g = await game();
  check('Plätze: Anna 1, Carl 2, Bert 3', g.results[g.players.Anna.plot]?.place === 1 && g.results[g.players.Carl.plot]?.place === 2 &&
    g.results[g.players.Bert.plot]?.place === 3, JSON.stringify(g.results));
  check('Anna: Titel „Meisterwerk!“ und Meisterbrief (Buch geöffnet und in der Schnellleiste)', sawTitle(anna, /Meisterwerk!/) &&
    anna.books > 0 && slot(anna, 4)?.name === 'written_book' && sees(anna, /Dein Meisterbrief/));
  check('Die anderen sehen den Sieger im Titel (de/en)', sawTitle(dora, /Anna/) && sawTitle(bert, /built the best/));
  check('Eigene Platzierung im Chat', sees(carl2, /Dein Bau: Platz 2 von 3 mit 4,00 ★/), carl2.chats.slice(-2).join(' | '));
  check('Anna steht ganz oben auf dem Podest', Math.abs(anna.entity.position.x - 1000.5) < 1.5 &&
    anna.entity.position.y >= FLOOR + 3.5, JSON.stringify(anna.entity.position));
  const stats1 = calls().filter((c) => c.t === 'stats');
  const sa = stats1.find((c) => c.name === 'Anna');
  const sd = stats1.find((c) => c.name === 'Dora');
  check('Netzwerk-Statistik für alle vier', stats1.length === 4 && sa?.add.wins === 1 && sa?.add.games === 1 &&
    sa?.best.best_score === 500 && sa?.add.votes_given === 2 && sa?.add.points === 15 && sa?.add.masterworks === 3 &&
    sd?.add.games === 1 && sd?.add.points === 0 && sd?.add.votes_given === 3, JSON.stringify([sa, sd]));
  check('Zurück in der Bauhütte, „spielt“ nicht mehr', await waitFor(() => players1.every(inHall), 15000) &&
    ['Anna', 'Bert', 'Dora'].every((n) => callsOf('playing', n).slice(-1)[0]?.playing === false),
  players1.map((b) => b.game.gameMode).join(' '));
  check('Parzellen aufgeräumt (Bau weg, Boden wieder Gras, keine Tiere)', await waitFor(async () =>
    (await status()).info.frei === '0' && await isBlock(ca.x, FLOOR + 1, ca.z + 2, 'air') &&
    await isBlock(pa.minX, FLOOR, pa.minZ, 'grass_block') && await isBlock(center(pc).x, FLOOR + 1, center(pc).z + 1, 'air'),
  30000, 1000) && await countOf('execute if entity @e[type=cow]') === 0, (await status()).info.aufgeraeumt);
  check('Spielerdatei und Bestenliste', /siege: 1/.test(readFile(playerFile('Anna'))) && /beste-wertung: 500/.test(readFile(playerFile('Anna'))));
  clear(bert);
  bert.chat('/top');
  await sleep(700);
  check('/top (englisch): Anna vorn', sees(bert, /1\. Anna – 1 certificates/), bert.chats.slice(-2).join(' | '));

  // ------------------------------------------------------------------ Runde 2: zu zweit
  const emil = await join('Emil');
  for (const b of [anna, bert, carl2, dora, emil]) {
    b.chat('/spielart paare');
  }
  await sleep(800);
  for (const b of [anna, bert, carl2, dora, emil]) {
    b.setQuickBarSlot(1);
    await sleep(150);
    b.activateItem();
  }
  check('Runde 2 startet zu zweit mit fünf Leuten', await waitFor(() => sees(anna, /Spielart: Zu zweit · 5 Leute/), 14000),
    anna.chats.slice(-1)[0]);
  g = await game();
  check('Paare: Anna + Carl (Party), Bert + Dora, Emil allein', g.players.Anna.plot === g.players.Carl.plot &&
    g.players.Bert.plot === g.players.Dora.plot && g.players.Emil.plot !== g.players.Anna.plot &&
    g.players.Emil.plot !== g.players.Bert.plot, JSON.stringify(g.players));
  await cmd('baumeisteradmin thema maibaum');
  check('Thema per Verwaltung: Maibaum, Partner genannt', await waitFor(async () => (await game()).phase === 'BUILD', 3000) &&
    await waitFor(() => sees(anna, /Du baust zusammen mit Carl/) && sees(bert, /building together with Dora/), 3000));
  const fritz = await join('Fritz');
  win = await openWithItem(fritz, 6);
  check('Fernrohr: laufende Runde zum Zuschauen', win && /Zuschauen/.test(plain(win.title)) && win.slots[11]);
  await fritz.clickWindow(11, 0, 0);
  check('Fritz schaut zu (Zuschauermodus)', await waitFor(() => fritz.game.gameMode === 'spectator', 3000) && sees(fritz, /Du schaust zu/));
  g = await game();
  for (const b of [anna, carl2, bert, emil]) {
    const p = g.plots[g.players[b.username].plot];
    const c = center(p);
    await tp(b, c.x + (b === carl2 ? 2.5 : 0.5), FLOOR + 1, c.z + 3.5, 180, 30);
    await give(b, 0, 'spruce_planks', 64);
    await place(b, c.x + (b === carl2 ? 2 : 0), FLOOR, c.z + 2, 'spruce_planks');
  }
  g = await game();
  check('Im Paar bauen beide auf derselben Parzelle', g.players.Anna.blocks >= 1 && g.players.Carl.blocks >= 1);
  mark = calls().length;
  emil.quit();
  check(`Nach ${COMEBACK} s: Emil ist nicht rechtzeitig zurück`, await waitFor(() => sees(anna, /Emil ist nicht rechtzeitig zurückgekommen/),
    (COMEBACK + 15) * 1000, 500), anna.chats.slice(-1)[0]);
  check('Netzwerk: holdSlot 20, dann 0', callsOf('hold-slot', 'Emil', mark).some((c) => c.seconds === COMEBACK) &&
    callsOf('hold-slot', 'Emil', mark).some((c) => c.seconds === 0));
  const emil2 = await join('Emil');
  check('Emil kommt zu spät: Bauhütte und Hinweis', await waitFor(() => inHall(emil2) && sees(emil2, /Leider zu spät/), 6000));
  await cmd('baumeisteradmin vorspulen weiter');
  await waitFor(async () => (await game()).phase === 'RATE', 4000);
  g = await game();
  check('Emils Bau wird nicht bewertet (zu spät)', g.order.length === 2 && !g.order.includes(g.players.Emil.plot), JSON.stringify(g.order));
  await sleep(800);
  const shown = g.gezeigt;
  const partner = shown === g.players.Anna.plot ? carl2 : dora;
  clear(partner);
  partner.chat('/bewerten 5');
  await sleep(500);
  check('Auch der Partner bewertet den eigenen Bau nicht', sees(partner, /bewerten die anderen|others rate/));
  check('Zuschauer sieht die Bewertung mit', Math.hypot(fritz.entity.position.x - (g.plots[shown].minX + g.plots[shown].maxX + 1) / 2,
    fritz.entity.position.z - (g.plots[shown].maxZ + 7.5)) < 4, JSON.stringify(fritz.entity.position));
  await cmd('baumeisteradmin vorspulen weiter');
  await sleep(1500);
  await cmd('baumeisteradmin vorspulen weiter');
  check('Siegerehrung Runde 2', await waitFor(async () => (await game())?.phase === 'AWARD', 6000) &&
    await waitFor(() => sees(anna, /Siegerehrung/), 3000));
  const stats2 = calls().slice(mark).filter((c) => c.t === 'stats');
  check('Statistik Runde 2: fünf Meldungen (Emil zu spät zählt mit), games_paare', stats2.length === 5 &&
    stats2.every((c) => c.add.games_paare === 1), `${stats2.length}`);
  check('Alle zurück in der Bauhütte (auch Fritz)', await waitFor(() => [anna, bert, carl2, dora, fritz].every(inHall), 20000),
    [anna, bert, carl2, dora, fritz].map((b) => `${b.username}:${b.game.gameMode}`).join(' '));

  // ------------------------------------------------------------------ Lobby-Knopf
  anna.setQuickBarSlot(8);
  await sleep(200);
  anna.activateItem();
  check('Bett: zur Lobby – fragt das Netzwerk (in der Bauhütte ohne Nachfrage)', await waitFor(() =>
    calls().some((c) => c.t === 'ask-lobby' && c.name === 'Anna' && c.asked === false), 4000));

  for (const b of [bert, carl2, dora, emil2, fritz]) b.quit();
  await sleep(800);
  const ok = results.filter((r) => r.ok).length;
  console.log(`\n${ok}/${results.length} Prüfungen bestanden`);
  process.exit(ok === results.length ? 0 : 1);
}

main().catch((e) => {
  console.error(e);
  const ok = results.filter((r) => r.ok).length;
  console.log(`\nAbgebrochen – ${ok}/${results.length} Prüfungen bestanden`);
  process.exit(1);
});
