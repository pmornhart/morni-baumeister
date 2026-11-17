# Morni Baumeister

@STAND.md

## Regeln für dieses Projekt

- Repo ist **öffentlich**: keine Geheimnisse, Tokens oder privaten Daten committen (Geheimes nur in `.env`, nie im Repo).
  Auch keine Test-Server-Ordner, Welten, Spielerdaten oder RCON-Passwörter.
- Der Modus heißt **Baumeister** (englisch „Master Builder“), das Repo `pmornhart/morni-baumeister`, die Jar
  `Morni-Baumeister.jar`, das Plugin `MorniBaumeister`, die Kennung im Netzwerk `baumeister`.
- Texte im Spiel auf Deutsch (Standard) und Englisch, freundlich, keine Fachbegriffe ohne Erklärung. Alle Texte stehen
  in `Texts.java` (MiniMessage, beide Sprachen); `TextsTest` prüft Platzhalter, Zeichen, dass jeder Schlüssel aus dem
  Quelltext einen Text hat, dass jede Spielart, Wertung, Kategorie, Uhrzeit, jedes Kapitel und Banner einen Namen hat
  und dass kein Platzhalter wie ein Standard-Tag heißt. Eigene Farb-Tags: `<sky>` Himmelblau, `<au>` Gold,
  `<em>` Smaragd.
- Themen stehen in `logic/Themes.java` (eigene Liste, Deutsch und Englisch, Kennung klein ohne Leerzeichen);
  `ThemesTest` verlangt mindestens 120 Themen und je Kategorie mindestens 10, keine doppelten Namen.
- **„Verfasst mit Claude 🤖“** als letzte Zeile in Commit-Messages (durch eine Leerzeile getrennt), in PRs, Issues und
  Release-Notes; einmal als Fußzeile im README.
- Commits auf Deutsch mit Stichpunkten `- Neu:` / `- Behoben:` / `- Verbessert:` / `- Entfernt:`.
- Keine fremden Marken, Figuren, Namen, Texte oder Grafiken (nichts von Mojang, Nintendo, Hypixel oder anderen Servern
  und Spielen): nur die Spielidee „Bauwettbewerb auf Zeit mit Bewertung“, eigene Namen (Bauhütte, Zollstock,
  Meisterbrief), eigene Bauten, eigene Themen.
- **Nichts für echtes Geld**, kein Pay-to-win: es gibt keine Währung, nur Statistik und den Meisterbrief.
- Die Netzwerk-Schnittstelle `src/api/java/de/moinmorni/api/MoinMorni.java` ist eine Kopie aus `pmornhart/moinmorni.net`
  (`plugins/moinmorni-network/src/main/java/de/moinmorni/api/MoinMorni.java`) und muss **genau gleich** bleiben. Sie wird
  nur mitübersetzt und kommt **nicht** in die Jar. Nur `net/Connected` darf sie benutzen.
- Baumeister muss auch ohne Netzwerk laufen (`softdepend: [MoinMorni]`; Sprache dann aus dem Client).
- Paper 26.x, Java 25. Der Code muss auch gegen Paper 26.1 übersetzen (`mvn -Ppaper-26.1 compile`, läuft in der CI).
- Regeln ohne Server (Themen und Vorschläge, Themen-Wahl, Wertung mit Gleichstand, Parzellen-Grenzen, Bereich der
  Werkzeuge, Wortliste, Redstone-Bremse, Grenzen, Paare, Abstimmung, Zurück ins Spiel) gehören in `logic/`, Daten in
  `data/` – ohne Bukkit, mit Tests. Werte dort nur zusammen mit den Tests ändern.
- Gebaut wird nur auf der eigenen Parzelle während der Bauzeit; alles, was Blöcke verändert, muss in `BuildGuard`
  geprüft werden (innerhalb der Baufläche bleiben) und die Parzelle als verändert merken (`PlotState.touch`) – nur das
  wird nach der Runde geräumt.
- Baupläne (`world/`) arbeiten nur mit Blocknamen als Text – `PlanTest` prüft ohne Server, dass es nur Blöcke von 26.x
  gibt (z. B. `iron_chain`, nicht `chain`), dass die Parzellen leer sind und Gras als Boden haben, Rand und Sockel
  stimmen und Podest und Ankunft begehbar sind.
- Ändern sich Bauhütte oder Bauplätze: `Construction.VERSION` erhöhen – dann baut der Server beim nächsten Start neu
  (auch bei geänderter Parzellen-Größe).
- Neue Version: `<version>` in `pom.xml` erhöhen und `docs/releases/v<version>.md` anlegen; ein Push auf `main` baut das
  Release mit `Morni-Baumeister.jar` – das Netzwerk holt es sich von dort.
- Vor jeder Veröffentlichung (Merge nach `main`) Philips Go abwarten.
- Nach jeder sichtbaren Änderung Bilder wie im echten Minecraft (echter Client aus `moinmorni.net/tools/mc-client`,
  Texturen und Bilder nur lokal, nie ins Repo).
- „Alles im Panel“: jede Einstellung, die per Befehl geht, muss auch im Panel gehen. Darum hat Baumeister nur die
  `config.yml` und Verwaltungs-Befehle zum Testen (`/baumeisteradmin`); die Anbindung an den Einstellungs-Katalog
  gehört ins Netzwerk-Repo.
- Bedrock-Spieler (Geyser) spielen mit: alle Menüs sind Truhen-Fenster, Themen-Wahl und Wertung auch per Rechtsklick in
  der Schnellleiste, Werkzeuge per Klick in der Werkzeugkiste.
- Echter Test nach jeder Änderung am Spielablauf: `test/run.sh` (siehe README), Ports 29100/29110.
- `STAND.md` nach jedem größeren Schritt aktualisieren.
