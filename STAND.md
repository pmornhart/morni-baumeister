# Stand – Morni Baumeister

**Was es ist:** Der Bau-Modus des MoinMorni-Netzwerks (Paper-Plugin `MorniBaumeister`, Java 25, Paper 26.x): ein
Bauwettbewerb für 2–16 Leute. Alle bekommen ein Thema, bauen auf Zeit auf ihrer eigenen Parzelle und bewerten danach
die Bauten der anderen (1–5 Sterne); der beste Bau bekommt den Meisterbrief. Keine Währung. Netzwerk-Kennung
`baumeister`, englisch „Master Builder“. Repo `pmornhart/morni-baumeister` (von Philip angelegt).

## Aufbau (`src/main/java/de/moinmorni/baumeister/`)

- `BaumeisterPlugin` (Start, Welt, Takt), `Settings`, `Texts` (de/en), `Ui`, `Items`, `Holo`, `Sidebar`, `Boards`,
  `Commands` (`/spielart`, `/bereit`, `/thema`, `/werkzeug`, `/bewerten`, `/zuschauen`, `/verlassen`, `/stats`, `/top`,
  `/baumeisteradmin`).
- `logic/` (ohne Server, mit Tests): `Theme`, `Themes` (131 Themen, Vorschläge), `ThemeVote`, `Rating`, `Scoring`
  (Durchschnitt, Gleichstand, geteilte Plätze), `Placement`, `PlotGrid` (Bauplätze, Parzellen, Boxen), `Region`
  (Zollstock-Bereich), `WordFilter`, `Throttle` (Redstone-Bremse), `Limits`, `Variant`, `Teams` (Paare), `Groups`,
  `Votes`, `Comeback`.
- `data/`: `PlayerData`, `Store`, `Leaderboard`.
- `world/` (Baupläne als Text): `Plan`, `Blocks`, `Decor`, `Kit`, `HallPlan` (Bauhütte), `ArenaPlan` (Bauplatz mit 16
  Parzellen und Podest), `Construction` (Bau-Version + Parzellen-Größe in `bau.yml`, Aufräumen nur der veränderten
  Bereiche, belegte Bauplätze in `aufraeumen.yml` – nach einem Absturz ganz geräumt), `VoidGenerator`.
- `game/`: `Game` (Ablauf THEME → BUILD → RATE → AWARD, Zurück ins Spiel, Statistik, Meisterbrief), `Games`,
  `Builder`, `PlotState` (Parzelle, veränderter Bereich, Uhrzeit, Wetter, Flüssigkeiten), `Tools` (Boden, Füllen,
  Ersetzen, Leeren, Rückgängig, Zollstock, Umriss), `ToolMenus` (Werkzeugkiste), `ThemeMenu`, `LangBar`.
- `hall/`: `Hall`, `HallMenus`. `menu/`, `listen/` (`PlayerListener`, `BuildGuard` – Schutz und Merken der
  Veränderungen), `net/`.
- `test/`: `run.sh` (Ports 29100/29110), `baumeister-test.cjs`, `rcon.cjs`, `fake-network/`.

## Entscheidungen (17.11.2026, Helfer, Philip schlief)

- Spielarten in der Bauhütte: klassisch (5 Min.), schnell (2 Min.), zu zweit (Partys zuerst, sonst der Reihe nach;
  einer übrig baut allein; ab 4 Leuten, sonst klassisch). Zwei Bauplätze → zwei Runden gleichzeitig.
- Themen-Wahl: drei Vorschläge aus drei Kategorien, die zuletzt gespielten 25 Themen nicht; Mehrheit, Gleichstand Los.
  Abschaltbar (`runde.themenwahl`), dann ausgelost.
- Parzelle 28 × 28 innen, 40 hoch, Boden (Gras) gehört dazu, darunter Erde als Sockel; Rand eine Reihe außerhalb aus
  geschälter Fichte. Wer zu weit fliegt (8 Blöcke über den Rand), kommt in die Mitte zurück.
- Bewertung: Durchschnitt statt Summe (so zählt es nicht, wie viele gerade da sind); Gleichstand → mehr Sterne → mehr
  Meisterwerke → geteilter Platz. Bauten ohne einen Block werden nicht gezeigt (Platz zuletzt, keine Stimmen). Namen
  bleiben bis zur Siegerehrung geheim. Haben alle bewertet, geht es nach 3 s weiter. Sieg nur mit Stimmen und
  mindestens zwei bewerteten Bauten.
- Uhrzeit und Wetter je Parzelle sind Spieler-Zeit/-Wetter: Bauleute sehen sie beim Bauen, alle beim Bewerten.
- Werkzeuge: Bereich mit dem Zollstock (Links-/Rechtsklick), höchstens 4096 Blöcke, 2 s Pause, ein Schritt
  Rückgängig; Blöcke für Füllen/Ersetzen kommen aus der Schnellleiste (Bedrock-tauglich, ohne Chat-Eingabe).
- Schutz: Wortliste wie im Netzwerk-Chat (das Netzwerk liefert keine Vorgabe-Liste, darum eine kurze eigene in der
  config.yml, mit erlaubten Wörtern wie „Barsch“); Redstone: über der Grenze wird der Strom auf aus gehalten, nach
  drei Sekunden in Folge baut der Schutz das Bauteil samt Uhr-Teilen daneben ab. Spender, Geschosse, Portale, TNT aus.
- Statistik zählt auch, wer zu spät zurückkam (Runde gespielt, ohne Wertung); wer selbst geht, zählt nicht.

## Stand (17.11.2026)

- Version 0.1.0 (Branch `claude/erster-stand`, wartet auf Philips Go).
- Unit-Tests: 41 grün (Themen, Themen-Wahl, Wertung mit Gleichstand und geteilten Plätzen, Parzellen-Grenzen und
  Abstände, Zollstock-Bereich, Wortliste, Links, Redstone-Bremse, Grenzen, Paare, Abstimmung, Zurück ins Spiel,
  Spielerdaten, Bestenliste, config.yml = Vorgaben, Texte, Baupläne nur mit 26.x-Blöcken). Übersetzt gegen Paper 26.3
  und 26.1.
- Neubau der Welt ≈ 940 000 Blöcke in 100 s (auf dem ausgelasteten Rechner).

## Offen

- Philip: Go für `main`.
- Netzwerk-Repo: `DEFAULT_MODES`, `MODE_REPOS`, `MODE_JARS`, Lobby-Figur, Webseite, Einstellungs-Katalog (siehe
  Bericht).
- Nicht echt geprüft: Bedrock (Geyser), echte Spieler (Zeiten, Gleichgewicht der Wertung), Paper 26.3 als Server.
