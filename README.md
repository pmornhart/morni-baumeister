# Morni Baumeister

Der Bau-Modus des Minecraft-Netzwerks **MoinMorni** (moinmorni.net): **Ein Thema, eine Parzelle – die Zeit läuft!**
Alle bekommen dasselbe Thema („Almhütte“, „Maibaum“, „Brotzeit“, „Drache“ …), bauen auf ihrer eigenen Parzelle auf
Zeit und bewerten danach die Bauten der anderen. Der beste Bau bekommt den **Meisterbrief**. 2 bis 16 Leute, allein
oder zu zweit. Ein Paper-Plugin für Paper 26.x (Java 25), im Netzwerk und auch ganz allein.

Englischer Name: **Master Builder**.

## So wird gespielt

1. In der **Bauhütte** (einer Zimmerei auf einer Alm-Insel) stimmt man mit dem **Kompass** über die Spielart ab, liest
   im **Buch**, wie es geht, schaut mit dem **Fernrohr** einer laufenden Runde zu und sagt mit dem **grünen
   Farbstoff** „bereit“. Ab 3 Leuten (einstellbar) läuft ein Countdown; sind alle bereit, geht es nach 5 Sekunden los.
2. **Spielarten:** *Klassisch* (Vorgabe 5 Minuten Bauzeit), *Schnell* (2 Minuten), *Zu zweit* (Paare bauen auf einer
   Parzelle – Partys zuerst zusammen, ab 4 Leuten).
3. **Thema:** Drei Vorschläge aus verschiedenen Bereichen erscheinen in einem Fenster und in der Schnellleiste; ein
   Klick ist eine Stimme, die Mehrheit gewinnt (Gleichstand: das Los). Zuletzt gespielte Themen kommen eine Weile nicht
   wieder. Die Themen-Wahl lässt sich abschalten – dann wird ausgelost.
4. **Bauen:** Jede Person (bzw. jedes Paar) hat eine eigene Parzelle mit Holzrand (Vorgabe 28 × 28 Blöcke, 40 hoch)
   und baut im Kreativmodus, fliegen geht. Nichts kommt aus der Parzelle hinaus, und wer zu weit fliegt, landet wieder
   in ihrer Mitte. Die Zeit läuft oben in der Leiste, Warnungen bei 60, 30, 10 … 1 Sekunden.
5. **Bau-Hilfen** (alles per Klick, auch auf Bedrock): die **Werkzeugkiste** (Stern) mit Boden wechseln (27 Böden),
   Uhrzeit (Morgen, Mittag, Abend, Nacht) und Wetter der eigenen Parzelle – so sehen es auch die anderen beim Bewerten
   –, Köpfe (von Wesen und allen Bauleuten), fertige Banner mit eigenen Mustern (weiß-blaue Rauten, Smaragd, Gipfel,
   Tanne …), **Füllen**, **Ersetzen**, **Leeren** und **Rückgängig**. Den Bereich dafür setzt man mit dem **Zollstock**
   (Linksklick auf einen Block = erste Ecke, Rechtsklick = zweite Ecke); höchstens 4096 Blöcke auf einmal.
6. **Bewerten:** Danach werden alle der Reihe nach zu jedem Bau gebracht (Bauten ohne einen Block werden übersprungen)
   und geben mit der Wolle in der Schnellleiste – oder `/bewerten 1–5` – **1 bis 5 Sterne**: Naja, Geht so, Gut,
   Super, Meisterwerk. Umentscheiden geht, den eigenen Bau (im Paar auch den des Partners) bewerten nur die anderen.
   Wer was gebaut hat, bleibt geheim bis zur Siegerehrung. Haben alle bewertet, geht es nach 3 Sekunden weiter.
7. **Gewinnen:** Es zählt der Durchschnitt der Sterne (egal, wie viele bewertet haben), bei Gleichstand mehr Sterne
   insgesamt, dann mehr „Meisterwerke“; ist alles gleich, teilen sich die Bauten den Platz. Bei der **Siegerehrung**
   stehen die ersten drei auf dem Podest (Gold, Eisen, Kupfer), es gibt Feuerwerk, jeder erfährt seinen Platz, und
   der Sieger bekommt den **Meisterbrief** (ein Buch mit Name, Thema, Wertung und Datum). Danach geht es zurück in die
   Bauhütte, und die Parzellen werden geräumt.

**Fair bauen:** Schilder, Namensschilder, Bücher und umbenannte Dinge werden geprüft – gesperrte Wörter (Liste in der
`config.yml`, wie im Netzwerk-Chat; „4rsch“ oder „a r s c h“ zählen auch) und Links gehen nicht. Gegen Lag: höchstens
12 Wesen (Tiere stehen still, keine Endermänner, Wither oder Drachen) und 24 gesetzte Flüssigkeiten je Parzelle,
Redstone-Uhren werden gebremst und nach drei Sekunden abgebaut, Kolben schieben nichts hinaus, kein TNT, keine
Portale, keine Geschosse, keine Spender, nichts brennt oder wächst über den Rand.

**Zurück ins Spiel:** Verliert jemand mitten in der Runde die Verbindung (zum Beispiel, weil das Spiel abstürzt), bleibt
seine Parzelle eine Weile frei (`rueckkehr-sekunden`, Vorgabe 90). Kommt er rechtzeitig wieder – direkt oder über
„Zurück ins Spiel“ in der Lobby –, baut er dort weiter, wo er war (beim Bewerten: weiter bewerten). Kommt er zu spät,
ist er raus: sein Bau wird nicht bewertet (im Paar baut der Partner weiter), die Runde zählt als gespielt.

## Welt (eigene Entwürfe, aus dem Code gebaut)

- **Bauhütte:** schwebende Alm-Insel mit Zimmerei (Bestenliste davor), Werkbänken, Holzstapeln, Baugerüst mit Kran,
  halb fertigem Musterhaus, Tafeln „So geht's“ und „Spielarten“.
- **Zwei Bauplätze** auf einer Bergwiese mit je 16 Parzellen (4 × 4, Kieswege dazwischen, Laternen an den Ecken) und
  einem Siegerpodest mit Fahnen – zwei Runden können gleichzeitig laufen.

Alles entsteht beim ersten Start in einer eigenen leeren Welt (`baumeister`, ≈ 940 000 Blöcke). Ändert man die
Parzellen-Größe, werden die Bauplätze beim nächsten Start neu gebaut.

## Befehle

| Befehl | Was er tut |
|---|---|
| `/spielart [klassisch\|schnell\|paare]` | über die Spielart abstimmen (ohne Angabe: Menü) – auch `/art` |
| `/bereit` | bereit melden – auch `/ready` |
| `/thema [1\|2\|3]` | Themen-Wahl öffnen oder abstimmen – auch `/theme` |
| `/werkzeug` | Werkzeugkiste öffnen – auch `/tools` |
| `/bewerten <1–5>` | den gezeigten Bau bewerten – auch `/rate` |
| `/zuschauen [spieler\|nummer]` | einer Runde zuschauen – auch `/spectate` |
| `/verlassen` | Runde oder Zuschauen verlassen – auch `/leave` |
| `/stats [spieler]` · `/top` | Statistik und die besten Baumeister |
| `/baumeisteradmin info\|status\|start [spielart] [thema]\|thema <kennung>\|vorspulen <weiter\|sekunden>\|stopp\|neubauen\|neuladen` | Verwaltung und Tests (`moinmorni.baumeister.admin`) – auch `/bmadmin` |

Einstellungen stehen nur in der `config.yml` (mit Erklärungen): Zurück-ins-Spiel-Zeit, Countdown, Mindestzahl,
Spielarten an/aus, Höchstzahl, Themen-Wahl, Bauzeiten, Bewertungs- und Siegerehrungszeit, Parzellen-Größe und -Höhe,
Werkzeug-Grenze und -Pause, Grenzen für Wesen, Flüssigkeiten und Redstone, Wortliste und Links. Es gibt keine
Einstellungs-Befehle – im Netzwerk kommen sie über den Einstellungs-Katalog des Panels.

## Im Netzwerk

Das Netzwerk-Plugin („MoinMorni“) stellt die Schnittstelle `de.moinmorni.api.MoinMorni` bereit; Baumeister nutzt sie,
wenn sie da ist (`softdepend`): Sprache, Party (zu zweit zusammen), Statistik, Zustand, zurück zur Lobby. Ohne
Netzwerk kommt die Sprache aus dem Client.

Ab Version 2 der Schnittstelle meldet Baumeister, wer gerade **spielt** (`playing`: vom Rundenstart bis zum Ende oder
Verlassen), schickt das Bett in der Bauhütte über `askToLobby` in die Lobby und hält nach einem Verbindungsabbruch die
Parzelle frei (`holdSlot(uuid, sekunden)`, nach der Rückkehr, bei Rundenende oder zu spät `holdSlot(uuid, 0)`). Ein
älteres Netzwerk-Plugin kennt diese Methoden nicht – dann geht es ohne Nachfrage und ohne Angebot weiter wie bisher.

Statistik nach jeder Runde (`stats("baumeister", …)`, höher ist besser):

- hochgezählt: `games`, `wins` (Meisterbriefe), `podiums` (Platz 1–3), `points` (bekommene Sterne), `votes_given`,
  `masterworks` (bekommene „Meisterwerk“-Stimmen), `blocks` (gesetzte Blöcke), `games_klassisch`, `games_schnell`,
  `games_paare`
- Bestwerte: `best_score` (beste Durchschnittswertung in Hundertsteln, 450 = 4,50 Sterne), `best_points`

Zustand: `waiting` oder `running`, dazu die Spielerzahl. Lokal: `spieler/<uuid>.yml` und `bestenliste.yml`.

Bei jedem Push auf `main` baut GitHub ein Release mit `Morni-Baumeister.jar` (Version aus `pom.xml`).

## Bauen und testen

```bash
mvn package                      # Tests + target/Morni-Baumeister.jar (Java 25)
mvn -Ppaper-26.1 compile         # passt der Code auch zu Paper 26.1?
test/run.sh                      # echter Test: Paper-Server + Test-Spieler (mineflayer)
FRESH=1 test/run.sh              # dasselbe, Welt vorher neu bauen
KEEP=1 SKIP_TEST=1 test/run.sh   # nur den Test-Server starten (z. B. für Bilder)
```

`test/run.sh` baut das Plugin und ein Test-Netzwerk (`test/fake-network`: Bert spricht Englisch, Anna und Carl sind
eine Party, alle Meldungen landen in `calls.log`), richtet einen Paper-26.1.2-Server ein (offline, Port 29100, RCON
29110, kleine Grenzen, 20 s Zurück ins Spiel), wartet auf genug freien Arbeitsspeicher und spielt zwei Runden durch
(`test/baumeister-test.cjs`): klassisch zu viert (Bauhütte, Abstimmung, Themen-Wahl per Fenster, Schnellleiste und
Befehl, Bauen, Rand, Zurückholen, Zollstock, Werkzeugkiste, Schutz, Verbindung weg und zurück, Bewertung, Siegerehrung,
Meisterbrief, Statistik, Aufräumen) und zu zweit zu fünft mit Zuschauer (Paare, Thema per Verwaltung, zu spät zurück,
Partner bewertet nicht, Lobby-Knopf). Mit `EXTRA_PLUGINS="…/ViaVersion.jar …/ViaBackwards.jar"` kommen ViaVersion
und ViaBackwards dazu.

---

Verfasst mit Claude 🤖
