# RFCs

Dieses Verzeichnis enthält **Request for Comments (RFCs)** für größere fachliche und technische Änderungen an hsadmin-NG.

Ein RFC beschreibt die Motivation, das Zielbild, wichtige Entwurfsentscheidungen
sowie – falls sinnvoll – das gewünschte Verhalten anhand von Gherkin-Szenarien.
Während der Implementierung wird das Dokument fortgeschrieben
und nach Abschluss als Referenz für das implementierte Feature beibehalten. 
Die konkrete Umsetzung einzelner Arbeitsschritte erfolgt in den zugehörigen PR-Dokumenten. 

(Bislang sind die Gherkin-Szenarien noch in den einzelnen RFCs zu finden, das soll sich in Zukunft ändern.
Auch der Name 'RFC' sollte überdacht werden, evtl. 'FC' für "Feature Spec".)

## Status

- **Entwurf** – Konzept wird erarbeitet und diskutiert.
- **Umsetzung** – Feature wird implementiert; das RFC kann sich noch ändern.
- **Umgesetzt** – Feature ist implementiert; das RFC beschreibt das gültige Verhalten.
- **Ersetzt** – Inhalt wurde durch ein neueres RFC ersetzt.
- **Zurückgezogen** – Das Vorhaben wird nicht weiter verfolgt.

## Aufbau

Ein RFC sollte in der Regel folgende Abschnitte enthalten:

- Ausgangslage
- Motivation
- Zielbild
- Features mit Gherkin-Szenarien (falls sinnvoll)
- Entwurfsentscheidungen
- Umsetzung / Verweise auf PRs
- Nicht-Ziele / Follow-Ups
- Begriffsabgrenzung (optional)

Ein RFC beschreibt das **Was** und **Warum**, nicht die einzelnen Implementierungsschritte.
