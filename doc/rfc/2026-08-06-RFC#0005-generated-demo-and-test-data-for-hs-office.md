# RFC#0005: Generierte Demo- und Testdaten für das hs_office-Modul

- Status: Entwurf
- Stand: 2026-08-06
- Author: Michael Hönnig

Der Entwurf hinter PR#270. Das PR-Dokument beschreibt, was dieser eine PR ändert;
dieses RFC hält den Entwurf fest, der die einzelnen PRs überdauert:
Mengengerüst, Archetypen, Invarianten, den festen Personensatz sowie die verworfenen Alternativen.

## Ausgangslage

Für Vorführungen vor Kunden und für realitätsnahe Entwicklungsarbeit fehlt ein Datenbestand in Produktionsgröße.
Die vorhandenen `*-test-data.sql`-Changesets sind bewusst klein und auf Integrationstests ausgelegt;
ihre Personen und Firmen heißen `Susan Firby`, `First GmbH`, `Third OHG`, `Fourth eG`.

Daraus ergeben sich vier Anforderungen:

- Eine Vorführung vor Mitgliedern braucht Daten, die echt aussehen, aber keine realen Personen betreffen.
- Entwicklung und Fehlersuche brauchen realistische Mengen und Verflechtungen.
- Spätere Performance-Messungen an den Restricted Views brauchen einen Grant-Graphen in realistischer Größe.
- Der Datenbestand muss reproduzierbar sein, damit Vorführungen und Messungen wiederholbar bleiben.

## Zielbild

Ein Generator erzeugt deterministisch rund 500 Partner mit dem vollständigen Office-Geflecht.
Er ist als Scenario-Test im `UseCase`-Stil gebaut, erzeugt die Daten über die REST-API statt über SQL,
und das Ergebnis wird als Dump ausgebracht und kann somit auf beliebige Stages ausgeliefert werden.
Die folgenden Abschnitte begründen diese drei Festlegungen einzeln.

Vorgeführt wird nacheinander die Verwaltungssicht auf den Gesamtbestand und die Sicht eines Mitglieds.
Mindestens eine `eG` darf daher kein Zufallsprodukt sein,
sondern trägt eine vollständige Mitgliedschaftshistorie aus Anteilszeichnung, Einzahlung, Debitor und SEPA-Mandat.
Sie entsteht immer, auch im Minimalbestand, und ist zugleich der Anker für den festen Personensatz.

## Mengengerüst bei 500 Partnern

| Entität                                 | Menge              | Bemerkung                                           |
|-----------------------------------------|--------------------|-----------------------------------------------------|
| `hs_office.person`                      | ~2.500             | Mittelwert 5 je Partner, je nach Archetyp 1 bis 12  |
| `hs_office.contact`                     | ~5.000             | Mittelwert 10 je Partner                            |
| `hs_office.relation`                    | ~5.000             | Mittelwert 10 je Partner, je nach Archetyp 3 bis 25 |
| `hs_office.partner` + `partner_details` | 500 + 500          |                                                     |
| `hs_office.debitor`                     | ~750               | 1 bis 2 je Partner                                  |
| `hs_office.bankaccount`                 | ~600               | nicht jeder Debitor hat eine Bankverbindung         |
| `hs_office.sepamandate`                 | ~600               |                                                     |
| `hs_office.membership`                  | ~600               | 0 bis 2 je Partner                                  |
| `hs_office.coopsharetx`                 | ~1.000             |                                                     |
| `hs_office.coopassettx`                 | ~1.000             |                                                     |
| **Summe**                               | **~14.000 Zeilen** |                                                     |

## Generierung statt Anonymisierung einer Produktionskopie

Eine anonymisierte Produktionskopie wurde ernsthaft erwogen, weil sie fachliche Verflechtungen und Verteilungen unverändert erhält.
Sie scheidet für beide Zwecke aus.

Rechtlich bleibt eine solche Kopie pseudonymisiert, nicht anonymisiert.
Quasi-Identifikatoren wie Geburtsjahr, PLZ, `partnerNumber` und der Zeitpunkt einer Anteilszeichnung überstehen jede Maskierung,
die den Realismus erhalten soll.
Vor einem Publikum aus Mitgliedern ist die Wahrscheinlichkeit hoch, dass jemand seinen Datensatz wiedererkennt.

Inhaltlich braucht eine Vorführung das Gegenteil dessen, was eine Produktionskopie auszeichnet.
Deren Wert liegt in den Anomalien: fehlende Postleitzahlen, uneinheitliche Telefonformate, Karteileichen aus der Migration.
Auf einer Leinwand vor Kunden ist das Ablenkung.

## Erzeugung über die REST-API, Ausbringung über einen Dump

Die Erzeugung läuft über die echte REST-API.
Relations, Debitornummern, RBAC-Rollen und Grants entstehen damit exakt wie im Regelbetrieb, und alle Validierungen greifen.
Ein SQL-Seed müsste diese Konsistenz nachbauen und würde still veralten.

Der Einwand, die API sei für diese Menge zu langsam, trägt nicht, weil Erzeugung und Ausbringung getrennt werden:

1. Der Generator läuft einmal und erzeugt den Datenbestand.
2. Das Ergebnis wird als Dump festgehalten.
3. Aufbau und Rücksetzen eines Demo-Systems erfolgen über den Dump und dauern Sekunden.
4. Neu erzeugt wird nur bei Schema-Änderungen — also dann, wenn ohnehin zu prüfen ist, ob die Daten noch passen.

Die Einmal-Laufzeit ist damit nachrangig.

Der Dump hat einen zweiten Nutzen, der über die Vorführung hinausgeht:
**Jeder Aufbau einer Demo-Instanz probt das Zurückspielen eines Produktionsdumps.**
Der Weg, den man im Katastrophenfall genau einmal richtig gehen muss, wird damit regelmäßig gegangen —
einschließlich der Fragen nach Rollen, Eigentümern und globalen Objekten.

Parallelisierung des Generators hilft ohnehin nicht:
`base.tx_journal_trigger` legt je Schreibtransaktion eine Zeile in `base.tx_context` an,
und deren `BEFORE INSERT`-Trigger nimmt `LOCK TABLE base.tx_context IN EXCLUSIVE MODE` bis zum Commit.
Alle schreibenden Transaktionen sind global serialisiert, unabhängig vom Einfügeweg.

Falls die Einmal-Laufzeit doch stört, ist der Fallback im Repository bereits vorhanden:
`CsvDataImport.persistViaSql(...)` schreibt beim Legacy-Import direkt per SQL an API und JPA vorbei.
Die nächste Stufe wäre, Journal- und RBAC-Trigger während des Ladens abzuschalten
und danach die vorhandenen `*_rebuild_rbac_system()`-Prozeduren einmal mengenbasiert laufen zu lassen.
Gebraucht wurde beides nicht, siehe die Messungen am Ende.

## Umsetzung als Scenario-Test mit konfigurierbarer Kundenzahl

Der Generator wird als Scenario-Test im `UseCase`-Stil gebaut, mit der Kundenzahl als Property.
In der Vorgabe erzeugt er fünf Kunden, für den Datensatz wird er auf 500 gesetzt.

Der ausschlaggebende Vorteil ist die fortlaufende Überprüfung:
Der Generator läuft bei jedem CI-Build mit fünf Kunden mit und kann deshalb nicht verrotten.
Das leistet mehr als eine reine Übersetzungsprüfung gegen die generierten Resource-DTOs —
es fängt auch geänderte Validierungsregeln, neue Pflichtfelder und veränderte Constraints.

Die Konvention „genau ein Top-Level-`UseCase` je Testmethode" bleibt gewahrt:
Eine Testmethode führt einen `UseCase` `GenerateDemoDataset` aus,
der intern je Kunde die fachlichen Use Cases aufruft.

Der Scenario-Report entsteht als Nebenprodukt und beschreibt lesbar, was der Datensatz enthält.
Bei fünf Kunden wird er erzeugt, beim vollen Lauf abgeschaltet — sonst wächst er ins Unbrauchbare.

## Java im Test-Sourceset statt Python

Python ist im Repository für Werkzeuge etabliert (`tools/fixmes`, `tools/howto`, `tools/api`, `tools/lib/apikeys-file.py`)
und wäre für Wortlisten und gewichtete Ziehungen die bequemere Sprache.

Den Ausschlag gibt trotzdem Java, weil die Invarianten Geschäftsregeln sind
und dort leben müssen, wo das Domänenmodell lebt.
In Java stehen die Enums `HsOfficePersonType`, `HsOfficeRelationType` und `HsOfficeMembershipStatus` unmittelbar zur Verfügung,
ebenso `iban4j` und die aus der OpenAPI-Definition generierten Resource-DTOs.
In einer separaten Sprache würden die Invarianten dupliziert und unbemerkt vom Modell wegdriften.

`java.util.Random` mit festem Seed ist reproduzierbar, `ThreadLocalRandom` und parallele Streams sind es nicht.
Der Generator bleibt strikt sequenziell — was wegen der Serialisierung über `base.tx_context` ohnehin gilt.

## Archetypen statt gleichförmigem Zufall

500 strukturell identische Partner sind in einer Listenansicht sofort als synthetisch erkennbar.
Die Mengenangaben des Mengengerüsts sind deshalb Mittelwerte über gewichtete Kundenprofile, keine Konstanten.

| Archetyp                    | Anteil | Merkmale                                                                                     |
|-----------------------------|--------|----------------------------------------------------------------------------------------------|
| Freiberufler / Einzelperson | ~30 %  | `NP`, 1 Person, 1–2 Kontakte, 3–4 Relations, kein Handelsregister, 1 Debitor, Mitgliedschaft |
| Kleine GmbH                 | ~25 %  | `LP` + 1–2 Geschäftsführer als `NP`, Handelsregister, 4–6 Kontakte, 8–12 Relations           |
| Verein e. V.                | ~10 %  | `LP`, mehrköpfiger Vorstand, mehrere `REPRESENTATIVE`, teils historisch abgelöst             |
| Agentur mit Endkunden       | ~10 %  | 2 Debitoren, abweichende Rechnungsanschrift, eigenes SEPA-Mandat je Debitor                  |
| Größeres Unternehmen        | ~8 %   | 15–25 Relations, `VIP_CONTACT`, `OPERATIONS`, `OPERATIONS_ALERT`                             |
| Ex-Partner                  | ~8 %   | `EX_PARTNER`, Mitgliedschaft `CANCELLED`, Anteile gekündigt, Guthaben ausgezahlt             |
| Erbengemeinschaft           | ~4 %   | `UF`, mehrere `NP`, `dateOfDeath` gesetzt, Mitgliedschaft `DECEASED` oder `TRANSFERRED`      |
| Öffentliche Einrichtung     | ~3 %   | `PI`, lange Namen, Behördenadresse, keine Bankverbindung                                     |
| Einzelfälle                 | ~2 %   | `LIQUIDATED`, `EXPULSED`, Rückbuchungen (`REVERSAL`)                                         |

## Der Generator sucht Partner über die Nummer, das Szenario über den Namen

`CreateMembership` suchte den Partner über `GET /api/hs/office/partners?name=…` und erwartete genau einen Treffer.
Diese Abfrage vergleicht präfixweise gegen einzelne Spalten:
`contact.caption`, `tradename`, `givenname` und `familyname`.

Für eine juristische Person genügt das, weil ihr Name vollständig in `tradename` steht.
Für eine natürliche Person steht er in zwei Spalten, und keine davon enthält „Vorname Nachname".
Der Treffer entstand allein über `contact.caption`, weil jede Caption mit dem Namen beginnt —
also über eine Konvention des Generators und nicht über eine Eigenschaft der Fachdaten.

Dazu kommt der Präfixvergleich: ein Name, der Anfang eines längeren ist, liefert zwei Treffer.
In den Namenslisten stecken sechs solche Paare, etwa Lang/Lange und Kraus/Krause.
Bei gleichem Vornamen bricht die Suche dann ab, und zwar zufällig abhängig von Seed und Kundenzahl.

Für den Generator sucht `CreateMembership` deshalb über `GET /api/hs/office/partners/P-{partnerNumber}`.
Diese Nummer ist eindeutig, dem Generator im selben Schritt bekannt, und der Endpunkt liefert genau einen Partner.
Damit können auch natürliche Personen Mitglied sein, was für eine Vorführung vor Mitgliedern nötig ist:
die Personen des festen Satzes sind Privatkunden und wären sonst als einzige Nicht-Mitglieder zu sehen.

Die Szenarien unter `MembershipScenarios` suchen weiterhin über den Namen.
Ein Mensch kennt den Partner unter seinem Namen und müsste die Nummer erst nachschlagen;
ein Bericht, der die Nummer voraussetzt, zeigt damit nicht mehr den Ablauf, den er beschreiben soll.
`CreateMembership` beherrscht daher beide Wege und verlangt genau einen davon:
sind `partnerNumber` und `partnerName` beide oder keiner von beiden gegeben, bricht der Use Case ab,
statt sich stillschweigend für einen zu entscheiden.

## Die Caption sagt, wofür der Kontakt da ist

Ein Partner führt mehrere Kontakte, die sich in Anschrift und Telefonnummer kaum unterscheiden.
Was sie unterscheidet, ist der Zweck, und die Caption ist die einzige Spalte, in der er stehen kann.
Sie lautet deshalb `Name - Zweck (Nummer)`.

| Kontakt für             | Entität bzw. Relation | Beispiel                                            |
|-------------------------|-----------------------|-----------------------------------------------------|
| die Partner-Anschrift   | `PARTNER`             | `Tannenhof Bürotechnik eG - Hauptkontakt (P-90000)` |
| eine vertretende Person | `REPRESENTATIVE`      | `Susanne Weber - Vertretung (P-90000)`              |
| technische Rückfragen   | `OPERATIONS`          | `Claudia Schmitt - Technik (P-90004)`               |
| Störungsmeldungen       | `OPERATIONS_ALERT`    | `Michael Fischer - Alarm (P-90004)`                 |
| die Rechnungsstellung   | Debitor               | `Fischer Pflege GmbH - Rechnungsstelle (D-9000201)` |
| Listen-Abonnements      | `SUBSCRIBER`          | `Jana Schäfer - private Mailinglisten-Adresse`      |

Die Nummer benennt, wozu der Kontakt gehört, und das ist bei der Rechnungsstelle der Debitor und nicht der Partner,
weil ein Partner mehrere Debitoren haben kann und die Captions sich sonst nur in einer Ziffer unterschieden.
Die private Listen-Adresse trägt keine Nummer, weil sie der Person gehört und nicht einer Kundenbeziehung.

Die Caption ist zugleich der Schlüssel, unter dem der Generator einen einmal angelegten Kontakt wiederfindet.
Sie entsteht deshalb an genau einer Stelle, und ein Abonnement übernimmt die Caption der Relation,
statt sie ein zweites Mal aus Name und Nummer zusammenzusetzen.
`contact.caption` ist `varchar(128)`; ein zu langer Name weicht, damit Zweck und Nummer erhalten bleiben.

## Mailinglisten-Abonnements

Ein Abonnement ist eine Relation vom Typ `SUBSCRIBER`, deren Qualifier `mark` die Liste benennt —
ohne Domain, also `generalversammlung` und nicht `generalversammlung@…`.
Anker ist die Partner-Person, wie es die vorhandenen Use-Cases und die Testdaten halten:
das Abonnement gehört zur Kundenbeziehung, nicht zur Genossenschaft.

Was in der Liste landet, ist die Haupt-E-Mail-Adresse des Kontakts, nicht die Person —
so liest es die Sicht `hs_integration.subscription` für mlmmj.
Der Kontakt je Abonnement ist damit das eigentliche Gestaltungsmittel:
drei Viertel der Abonnenten führen eine private Adresse unter einer anderen Domain,
die sie für alle ihre Listen verwenden, der Rest abonniert mit dem Kontakt seiner Relation.

Nicht jede Person entscheidet für sich, sondern **der Kunde nimmt als Ganzes teil und entsendet Leute**:
meist eine, in gut jedem fünften Fall zwei.
Andernfalls stünde ein Mitglied mit fünf Repräsentanten fünfmal auf der Liste seiner Generalversammlung,
was den Verteiler unbrauchbar aufblähte.
Entsandt werden Kunden-Personen, also natürliche Partner-Inhaber und Repräsentanten;
bei den Betriebs-Listen die technischen Ansprechpartner.

| Liste                   | nur Mitglieder | Kunden nehmen teil |
|-------------------------|----------------|--------------------|
| `generalversammlung`    | ja             | 95 %               |
| `members-announce`      | ja             | 90 %               |
| `members-discussion`    | ja             | 30 %               |
| `customers-announce`    | nein           | 85 %               |
| `customers-discuss`     | nein           | 25 %               |
| `operations-announce`   | nein           | 55 %               |
| `operations-discussion` | nein           | 20 %               |

Die Betriebs-Listen erhalten zusätzliche Abonnenten, gerechnet als 35 % der Kundenzahl:
Administratoren, die ihr Kunde in den Office-Daten nie benannt hat.
Sie bekommen die fehlende Relation gleich mit — meist `OPERATIONS`, in einem Fünftel `OPERATIONS_ALERT` —
und abonnieren `operations-announce` fast immer, `operations-discussion` knapp zur Hälfte.
Genau so deckt eine echte Verteilerliste auf, wer alles Zugriff hat, ohne im Office-Datenbestand zu stehen.

Ihre Adresse liegt unter der Domain ihres Kunden, denn eine technische Liste liest man am Arbeitsplatz.
Eine private Listen-Adresse gibt es deshalb nur auf den nicht-technischen Listen.

## Invarianten sind Teil des Generators

Realismus entsteht nicht aus guten Wortlisten, sondern aus stimmigen Kombinationen.
Der Generator muss die folgenden Invarianten aktiv einhalten, statt Felder unabhängig zu würfeln.

| Invariante                                                                            | Begründung                                                                  |
|---------------------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| `salutation` ↔ Geschlecht des `givenName`                                             | fällt sofort auf                                                            |
| inhabergeführte Firma ↔ Vertreter mit demselben Nachnamen                             | verbreitetes Muster, das rein zufällige Namen nie erzeugen                  |
| Telefonvorwahl ↔ PLZ/Ort                                                              | Hamburger Adresse mit Münchner Vorwahl ist verräterisch                     |
| E-Mail-Domain ↔ `tradeName`                                                           | Firma und Mailadresse müssen zusammenpassen                                 |
| `personType = 'NP'` → `tradeName` leer; `'LP'` → Namensfelder leer, außer bei e. K.   | sonst inkonsistente Anzeige in Listen                                       |
| `registrationOffice`/`registrationNumber` nur bei eingetragenen Rechtsformen          | GbR und Freiberufler haben keine                                            |
| `birthday`, `birthName`, `birthPlace` nur bei `NP`                                    |                                                                             |
| `bankaccount.holder` ↔ Partnername, Abweichung nur dosiert                            | abweichende Kontoinhaber gibt es real, aber selten                          |
| `postalAddress.name` ist ein Namenszusatz, keine Wiederholung                         | das Feld trägt Abteilung oder c/o-Zeile und darf leer bleiben               |
| `membership.validity` ↔ `coopsharetx.valueDate`                                       | Anteile werden nicht vor dem Beitritt gezeichnet                            |
| je Debitor meist ein gültiges SEPA-Mandat, wenige ohne, wenige mit abgelaufenem davor | ein abgelöstes Mandat endet am Vortag des neuen, wie nach einem Bankwechsel |
| `status = CANCELLED` → Kündigungsbuchung, Guthabenauszahlung, Enddatum                | häufigster Fehler in synthetischen Genossenschaftsdaten                     |
| `status = DECEASED` → `partner_details.dateOfDeath` gesetzt                           |                                                                             |
| Summe `shareCount` ≥ 0 und Summe `assetValue` ≥ 0 je Mitgliedschaft                   | negative Bilanzen sind fachlich unmöglich                                   |
| `vatId` gesetzt → `vatBusiness = true` und `vatCountryCode` gesetzt                   |                                                                             |
| `debitor.defaultPrefix` eindeutig                                                     | der Constraint erzwingt es; der Generator muss es verwalten                 |

## IBAN und BIC werden gerechnet, nicht gesucht

Es gibt keine Knappheit an gültigen IBANs.
Gültigkeit bedeutet eine nach ISO 7064 Mod 97-10 passende Prüfziffer, und die wird berechnet.
Knapp sind allein dokumentierte Beispiel-IBANs aus Tutorials, die hier nicht gebraucht werden.

Die Bibliothek `org.iban4j:iban4j` ist bereits Abhängigkeit und wird in
`HsOfficeBankAccountController` für `IbanUtil.validate` und `BicUtil.validate` verwendet.
Derselbe Baustein erzeugt im Generator die Werte, wodurch sie die Prüfung der API garantiert bestehen.
`IbanUtil.validate` prüft Ländercode, Länge und Prüfziffer, nicht die Existenz der Bankleitzahl.

Das eigentliche Risiko ist nicht Ungültigkeit, sondern die Kollision mit einer realen Kontoverbindung.
Deshalb beginnen die Bankleitzahlen mit einer 9.
Das Merkblatt Bankleitzahlendatei nummeriert die Clearing-Gebiete an Stelle 1 von 1 bis 8,
eine 9 ist dort also nicht vergebbar, und die Bankleitzahlendatei bestätigt es:
keiner ihrer 13.806 Datensätze beginnt mit 9.
Der Bereich ist damit nicht nur heute frei, sondern strukturell unvergebbar, was auch künftige Vergaben ausschließt.
Die IBAN ist prüfziffernkorrekt, kann aber nirgendwohin routen und gehört niemandem.

Für den BIC wird die Konvention nach ISO 9362 genutzt:
Eine `0` an Position 8 kennzeichnet einen Test-BIC.
Aus `DEUTDEFF` wird `DEUTDEF0` — strukturell gültig, nicht routingfähig, für Fachleute erkennbar.

Bankleitzahl, BIC und Bankname werden stets als Tripel aus einer Liste gezogen, nie unabhängig gewürfelt.

## Kollisionen mit realen Personen werden aktiv vermieden

Je echter Namen und Adressen wirken, desto wahrscheinlicher treffen sie eine reale Person.
Eine plausible Kombination aus Name, Straße und Ort steht sonst als angebliches Hostsharing-Mitglied auf der Leinwand.

- E-Mail-Domains aus den nach RFC 2606 reservierten Bereichen oder einer eigenen Demo-Domain.
- Telefonnummern aus den von der Bundesnetzagentur für Medienproduktionen reservierten Blöcken, siehe unten.
- Reale Straßennamen mit hohen Hausnummern, sodass die Kombination unwahrscheinlich existiert.
- Häufige Vor- und Nachnamen, aber keine prominenten und keine seltenen Namen.
- IBANs mit nicht belegter Bankleitzahl, siehe oben.

Einzige Ausnahme ist der feste Personensatz weiter unten,
dessen Namen mit Zustimmung der Betroffenen echt sind.

## Telefonnummern nur aus den reservierten Blöcken

Die Bundesnetzagentur hält Rufnummern bereit, die dauerhaft keinem Teilnehmer zugeteilt sind
und deshalb in Medien genehmigungsfrei gezeigt werden dürfen (Mitteilung 148/2021).
Das sind je 1000 Nummern in fünf Ortsnetzen sowie Mobilfunknummern ohne Ortsbezug:

| Ortsnetz          | Vorwahl | Block             |
|-------------------|---------|-------------------|
| Berlin            | 30      | 23125 000 bis 999 |
| Hamburg           | 40      | 66969 000 bis 999 |
| Frankfurt am Main | 69      | 90009 000 bis 999 |
| München           | 89      | 99998 000 bis 999 |
| Köln              | 221     | 4710 000 bis 999  |

Eine Festnetznummer bekommt nur, wessen Ort eines dieser fünf Ortsnetze ist.
Alle übrigen Orte bekommen eine Mobilfunknummer.
Eine Vorwahl, die nicht zum Ort passt, wäre genau der Widerspruch, den die Adressdaten vermeiden sollen;
eine Mobilfunknummer trägt keinen Ortsbezug und kann deshalb auch keinem widersprechen.
Aus der Ortsverteilung folgt ein Verhältnis von etwa 30 zu 70 zwischen Festnetz und Mobilfunk.

Der Vorrat ist klein: 5000 Festnetznummern, 200 Nummern aus zwei Mobilfunkblöcken und 10 einzelne Mobilfunknummern.
Bei 500 Kunden wiederholt sich damit jede Mobilfunknummer etwa siebenmal.
Eindeutige Nummern sind aus einem reservierten Vorrat nicht zu haben,
und keine Prüfung der Anwendung verlangt sie.
Der Test vergleicht jede erzeugte Nummer gegen die aus der Mitteilung abgeschriebene Liste,
statt gegen die Konstanten des Generators, die er prüfen soll.

## Ein Generator, zwei Ausprägungen

Der Anomalie-Schalter unterscheidet die Zwecke, ohne die Erzeugungslogik zu duplizieren.

|                 | Demo-Datensatz                    | Entwickler-Datensatz                                            |
|-----------------|-----------------------------------|-----------------------------------------------------------------|
| Vollständigkeit | alle Felder gefüllt und plausibel | fehlende PLZ, leere statt `NULL`-Werte, unvollständige Kontakte |
| Formate         | einheitlich                       | Telefonnummern in gemischten und fehlerhaften Formaten          |
| Feldlängen      | mittlere Längen                   | Werte an der Feldlängengrenze                                   |
| Randfälle       | wenige, kuratiert                 | vollständige Bandbreite                                         |

Ein fester Seed sorgt in beiden Fällen für Determinismus,
damit sich Datensätze zwischen Läufen nicht verschieben und Vorführungsabläufe stabil bleiben.

## Optionaler Satz fest definierter Personen für die Anmeldung

Die generierten Partner sind namenlose Masse.
Für eine Vorführung müssen sich einzelne Personen tatsächlich anmelden können.

Der Generator legt deshalb optional einen kleinen Satz natürlicher Personen an, deren Namen vorgegeben sind statt gezogen.
Für diese Personen lassen sich nach einem Keycloak-Sync Accounts erzeugen.
Die Werkzeugkette dafür ist vorhanden:
`tools/get-keycloak-users` → `tools/create-accounts-from-csv`.

Dieselbe Datei ist Eingabe des Generators und Eingabe der Account-Erzeugung, sodass kein zweites Format entsteht.
Sie füllt sich in drei Schritten:

1. `tools/get-keycloak-users` exportiert aus dem Realm `subjectName`, `subjectUuid`, `givenName` und `familyName`.
2. Der Generator legt die Personen mit genau diesen Namen an
   und schreibt die Datei mit gefüllter `personUuid` daneben:
   aus `keycloak-users.csv` wird `keycloak-users-with-person-uuids.csv`.
3. `tools/create-accounts-from-csv` erzeugt daraus nach dem Restore die Accounts;
   `globalUid` und `globalGid` stammen nicht aus der Datei, sondern aus dem Startwert-Argument des Werkzeugs.

Für einen erzeugten Datenbestand entfällt damit `tools/enrich-keycloak-users-with-existing-natural-person-uuid`.
Dieses Werkzeug sucht die Personen nach dem Restore über die API,
während der Generator die UUIDs bereits kennt, weil er die Personen selbst angelegt hat.
Für gewachsene Datenbestände bleibt es die einzige Möglichkeit und damit erhalten.

### Ein Kunde je Anmeldenamen-Präfix

Ein Präfix bezeichnet bei Hostsharing einen Kunden, nicht eine Person.
Der Generator erzeugt deshalb je Präfix genau einen Kunden:
ein dreibuchstabiges Präfix wird eine Privatperson, jedes längere Präfix eine GmbH.
Nur ein dreibuchstabiges Präfix taugt zugleich als Rechnungsstellen-Präfix,
das dem Muster `^[a-z0-9]{3}$` genügen muss;
diese Präfixe sind daher von der erzeugten Folge ausgenommen.

Alle Personen eines Präfixes werden Vertreter dieses Kunden.
Bei einer Privatperson ist die erste von ihnen der Inhaber selbst und keine zusätzliche Vertretung.

Anmeldenamen mit dem Präfix `hostsharing-` erhalten keinen eigenen Kunden.
Sie gehören zu einem Menschen, der bereits einen Mitglieds-Anmeldenamen hat,
und beide Anmeldenamen erhalten dieselbe `personUuid`.
Die Zuordnung erfolgt über den Nachnamen und den ersten Teil des Vornamens,
weil die Vornamen nicht immer gleich geschrieben sind.

### Reale Namen, erfundene Daten

Die Datei enthält bewusst reale Personen.
Echt sind ihre RBAC-Subject-Namen sowie Vor- und Nachnamen,
weil sie den Mitgliedern ohnehin namentlich bekannt sind und die Betroffenen zugestimmt haben.
Alle übrigen Daten dieser Personen sind erfunden und werden wie bei allen anderen erzeugt:
Anschrift, Telefon, E-Mail, Bankverbindung, Geburtsdaten
sowie die juristische Person, die sie vertreten.

Weil die Datei reale Namen trägt, bleibt sie unter `.private/` und gehört nicht in die Versionsverwaltung.
Genau dafür ist ihr Pfad ein Parameter des Generators: dieser bleibt frei von Personendaten,
und ohne Angabe entfällt der feste Personensatz.
Die Personen werden über ihren Namen zugeordnet und nicht über die Keycloak-UUID,
weil die UUIDs bei einer Neuinstallation des Realms andere sind.

Daraus folgen zwei Anforderungen an den Generator:

- Die festen Personen sind zusätzlich Vertreter einer erzeugten Genossenschaft (`eG`), nicht der Hostsharing eG selbst.
  Technisch bedeutet das eine Relation vom Typ `REPRESENTATIVE` mit der juristischen Person als Anker und der festen Person als Inhaber.
  Über die `PARTNER`-Relation wäre eine solche Person nicht auffindbar,
  weil dort die juristische Person selbst der Inhaber ist
  und `tools/natural-partner-person-by-name` ausschließlich natürliche Inhaber liefert.
  Ist ein fester Personensatz vorgegeben, muss der Generator daher mindestens eine `eG` erzeugen.
  Es ist dieselbe Person wie beim eigenen Kunden und keine zweite gleichen Namens:
  `AddDemoRelation` legt eine Person nur an, wenn unter ihrem Namen noch keine bekannt ist.
- Ihre Vor- und Nachnamen müssen im gesamten Datenbestand eindeutig sein,
  denn ein mehrdeutiger Treffer liefert keine `personUuid`.
  Bei 500 aus frequenzgewichteten Listen gezogenen Partnern ist eine Kollision wahrscheinlich,
  deshalb werden die Namen der festen Personen aus dem Ziehungspool ausgeschlossen.

Der Dump enthält die Personen, aber weder Subjects noch Accounts:
Diese hängen vom Zustand des Keycloak-Realms ab und werden nach dem Restore über die Werkzeugkette erzeugt.
Damit bleibt das Artefakt unabhängig von der Realm-Konfiguration.

Die Spalte `subjectUuid` wäre der Hebel, das zu ändern:
sie trägt die aus Keycloak bekannte UUID, über die sich Subject und Account gleich miterzeugen ließen.
Selbst erfundene UUIDs kämen dafür nicht in Frage, denn das Subject wird über die Keycloak-UUID identifiziert,
und ein späterer Sync träfe dann auf denselben Namen unter anderer UUID.

## Verworfen: reiner SQL-Seed als weiterer Liquibase-Context

Ein zusätzlicher Context neben `!without-test-data` wäre schneller gebaut.
Er umgeht jedoch die API, muss die RBAC-Konsistenz selbst herstellen und veraltet still, wenn sich das Modell ändert.

## Verworfen: Erzeugung in der laufenden Anwendung über ein Profil `demo-data`

Erwogen wurde, die Anwendung mit einem Profil `demo-data` zu starten und die Kunden im Hintergrund nach und nach anlegen zu lassen.
Das ist im Betrieb bequem, scheitert aber an drei Punkten.

**Angriffsfläche.**
Ein profilgesteuerter Massen-Datenerzeuger läge im Produktions-Jar —
Code, der Partner, Personen und Mitgliedschaften am Fachprozess vorbei anlegt.
Nach der Härtungsrunde aus `13df8751` wäre das ein Rückschritt,
und im Audit-Log wären so erzeugte Daten nicht von regulären Anlagen zu unterscheiden.

**Der globale Schreib-Lock.**
Jede Schreibtransaktion hält `LOCK TABLE base.tx_context IN EXCLUSIVE MODE` bis zum Commit.
Während der Generator im Hintergrund läuft, wartet jeder Schreibzugriff der Vorführung hinter ihm.
Gerade die schrittweise Erzeugung im laufenden Betrieb ist damit die ungünstigste Variante.

**Reproduzierbarkeit.**
Inkrementelles Befüllen macht das Rücksetzen auf einen definierten Stand schwerer als ein Dump-Restore.
Ein Vorführungsskript braucht aber, dass ein bestimmter Datensatz beim nächsten Mal derselbe ist.

Soll ein Demo-Server sich später doch selbst befüllen,
wäre der saubere Weg ein eigenes Gradle-Subprojekt, das von der Anwendung abhängt, aber nicht Teil des Deployment-Jars ist.
Das ist erst zu rechtfertigen, wenn sich der Dump-Restore als unzureichend erweist.

## Generator-Inventar: Primitive

Die Wortlisten sind selbst kuratiert und liegen als Ressourcen im Test-Sourceset.
Das vermeidet eine zusätzliche Abhängigkeit und jede Lizenzfrage,
und es hält die Gewichtung sowie die Ausschlussliste für die festen Personen steuerbar.

Damit die Listen auch für große Bestände tragen, werden Namen zusammengesetzt, wie es im Deutschen üblich ist:
`Schwarz` + `müller`, `Alster` + `blick`.
Aus 31 Präfixen und 18 Stämmen entstehen so 580 Firmennamen-Bausteine und mit 24 Branchenwörtern 13.920 Kerne;
aus 26 Präfixen und 25 Stämmen werden 730 Nachnamen und mit 70 Vornamen 51.100 Personennamen.
Die kuratierten Listen bleiben die häufige Mehrheit, die Zusammensetzungen bilden den Long Tail —
bei fünf Kunden im CI-Lauf erscheinen also weiterhin überwiegend die handverlesenen Namen.

Rund ein Drittel der Firmen ist inhabergeführt und trägt den Nachnamen des Inhabers,
der dann als Vertreter mit demselben Nachnamen auftritt: `Hochhof GmbH & Co. KG` mit Herrn Hochhof.
Das ist ein Muster, das in synthetischen Daten sonst nie vorkommt und das jeder Betrachter aus dem Alltag kennt.
Vereine, öffentliche Einrichtungen und Erbengemeinschaften sind davon ausgenommen.

| Generator       | Quelle / Verfahren                                          | Realismus                                                      |
|-----------------|-------------------------------------------------------------|----------------------------------------------------------------|
| `Vorname`       | frequenzgewichtete Liste, mit Geschlecht und Jahrgangsbezug | Namensmoden je Jahrgang                                        |
| `Nachname`      | frequenzgewichtete Liste                                    | Long-Tail erhalten, nicht nur die häufigsten                   |
| `Titel`         | `Dr.`, `Prof. Dr.`, `Dipl.-Ing.`                            | ~5 %, überproportional bei älteren Jahrgängen                  |
| `Anrede`        | abgeleitet aus dem Geschlecht des Vornamens                 | nie unabhängig gezogen                                         |
| `Firmenname`    | Baukasten aus Namensteil, Branchenwort und Rechtsform       | Rechtsformverteilung GmbH ≫ UG > e. K. > GbR > e. V. > eG > AG |
| `PLZ + Ort`     | reale Paare, bevölkerungsgewichtet                          | erzeugt realistische Ballung                                   |
| `Straße`        | reale Straßennamen                                          | hohe Hausnummern zur Kollisionsvermeidung                      |
| `Telefonnummer` | reservierte Blöcke, Festnetz nur bei passendem Ortsnetz      | Formatvielfalt im Entwickler-Datensatz                         |
| `E-Mail`        | abgeleitet aus Person oder Firma, reservierte Domain        | passt zum Firmennamen                                          |
| `Bank-Tripel`   | Liste aus nicht belegter BLZ, Test-BIC und Bankname         | immer als Tripel                                               |
| `IBAN`          | `iban4j` mit BLZ aus dem Tripel                             | besteht die API-Validierung                                    |
| `Datum`         | zweckabhängige Verteilungen                                 | Beitritte häufen sich zum Jahresanfang                         |

## Generator-Inventar: Entitäten in Erzeugungsreihenfolge

Die Entitäts-Generatoren sind die fachlichen Use Cases, die `GenerateDemoDataset` je Kunde aufruft.
Unter `src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/` bestehen bereits Use Cases
für Person, Contact, Partner, Debitor, Membership und Subscription; diese werden wiederverwendet, statt sie nachzubauen.

| #  | Generator              | Baut auf                          | Kernentscheidung                                                               |
|----|------------------------|-----------------------------------|--------------------------------------------------------------------------------|
| 1  | `PersonGenerator`      | Namens-Primitive                  | `personType` bestimmt, welche Felder gesetzt und welche leer sind              |
| 2  | `ContactGenerator`     | Adress-, Telefon-, Mail-Primitive | die jsonb-Key-Sets variieren zwischen Kontakten                                |
| 3  | `RelationGenerator`    | Person, Contact                   | zuerst `PARTNER`, dann die übrigen Typen                                       |
| 4  | `PartnerGenerator`     | Relation                          | `partnerNumber` fortlaufend, Handelsregisterdaten nur bei eingetragenen Formen |
| 5  | `BankAccountGenerator` | IBAN- und Bank-Primitive          | `holder` meist gleich dem Partnernamen                                         |
| 6  | `DebitorGenerator`     | Relation `DEBITOR`, BankAccount   | `defaultPrefix` aus dem Firmennamen ableiten, Eindeutigkeit erzwingen          |
| 7  | `SepaMandateGenerator` | Debitor, BankAccount              | `validity` teils offen, teils beendet                                          |
| 8  | `MembershipGenerator`  | Partner                           | `status` und `validity` bestimmen alles Nachfolgende                           |
| 9  | `CoopShareTxGenerator` | Membership                        | Zeichnung beim Beitritt, Kündigung beim Austritt                               |
| 10 | `CoopAssetTxGenerator` | Membership, CoopShareTx           | Einzahlung folgt der Zeichnung, Auszahlung der Kündigung                       |

## Ein eigener Datenbestand statt eines gemeinsamen

`DemoDataScenarioTest` fährt den Generator allein, in einer eigenen Datenbank und mit dem Liquibase-Context
`without-test-data`. Damit enthält der Datenbestand weder die Liquibase-Testdaten noch die Fixtures der
übrigen Szenarien; „Test AG" und „Michelle Matthieu" tauchen in keiner Partnerliste mehr auf.
Der Test prüft diese Voraussetzung zu Beginn selbst und bricht ab, sobald die Datenbank nicht leer ist.

Ohne die Testdaten-Changesets gibt es kein USER-Subject mit globaler ADMIN-Rolle;
den Platz nimmt beim Erzeugen das Provisioning-API-Key-Subject ein.
Für die Vorführung selbst reicht das nicht, weil das Frontend keinen API-Key kennt;
der Generator legt deshalb `hsh-demo_admin` als USER-Subject mit globaler ADMIN-Rolle an.
Das ist der einzige Anmelde-Zugang im Datenbestand, er ist als solcher benannt,
und er ersetzt die 59 unabsichtlich mitgeschleppten Identitäten durch eine gewollte.
Da 182 Aufrufe in 41 Dateien `asGlobalAgent()` verwenden, entscheidet nicht jeder Aufrufer über die Identität,
sondern `FakeLoginUser.useAsGlobalAgent(...)` schaltet sie an einer Stelle um.
Der Mandant „Hostsharing eG" wird dann vom Generator selbst angelegt statt aus den Testdaten übernommen.

Der Liquibase-Context gilt nicht nur beim Erzeugen, sondern auch beim Betrieb:
Ein Anwendungsstart ohne `without-test-data` spielt die Testdaten-Changesets nach —
lokal beobachtet als 5 Testpartner und 53 fremde Subjects in einem zuvor sauberen Datenbestand.
Eine Stage, die den Dump erhält, muss also ebenso konfiguriert sein, sonst ist die Trennung nach dem ersten Start dahin.

Der Dump entsteht mit `--exclude-table-data=rbac.api_key --exclude-table-data=rbac.api_key_scope`.
Der Testlauf provisioniert den API-Key aus `ScenarioTest`, dessen Klartext als Konstante im Repository steht;
er trägt die globale ADMIN-Rolle, ohne Ablauf und ohne Scope.
Der Ausschluss verliert nichts: kein Changeset legt API-Keys an,
und `ProvisioningApiKeyBootstrap` sucht nach *gespeicherten Schlüsseln*, nicht nach dem Subject.
Das Subject bleibt also ohne Berechtigungsnachweis im Dump,
und der nächste Anwendungsstart trägt für genau dieses Subject den eigenen Schlüssel der Zielumgebung ein.
`ProvisioningApiKeyBootstrapAcceptanceTest` weist das nach.

Ebenso ausgeschlossen wird `base.tx_journal`.
Das Audit-Journal protokolliert die Erzeugung selbst und ist mit Abstand die größte Tabelle;
bei 500 Kunden sinkt der Dump dadurch von 22 auf 13 MB.
`base.tx_context` bleibt erhalten, weil die Historisierungstabellen von Booking, Hosting und Accounts darauf verweisen —
nur die Kindtabelle lässt sich ohne hängende Referenz weglassen.

Die Alternative, die Journal-Trigger während der Erzeugung abzuschalten, wäre schneller, wurde aber verworfen:
`alter table ... disable trigger` ist dauerhaftes DDL, das `pg_dump` mit überträgt,
sodass ein vergessenes Wiedereinschalten eine Demo-Instanz ohne Audit ausliefern würde.
Außerdem liefe die Erzeugung dann nicht mehr über den echten Schreibpfad,
womit die Messungen aus PR#271 ihre Aussagekraft verlören.
Als Teil eines Skalierungsmodus, der Realismus gegen Menge tauscht, bleibt die Idee erwägenswert.

Geprüft wird beim Erzeugen, nicht beim Einspielen:
das Einspielen liegt beim Betrieb, das Erzeugen des Artefakts liegt hier.
Der Dump-Schritt von `tools/demo-data` durchsucht deshalb die Inhaltsliste des Dumps
nach `rbac.api_key` und bricht bei einem Treffer ab.

Für Produktionssicherungen gilt das Gegenteil: dort gehören die API-Keys in den Dump.
Wandert eine solche Sicherung auf eine andere Stage, lassen sie sich beim Zurückspielen ausschließen,
indem die Inhaltsliste des Dumps gefiltert wird (`pg_restore --list` → `grep -v` → `--use-list`),
siehe [doc/demo-datasets.md](../demo-datasets.md).

Das Format wählt, wer den Dump erzeugt:
bei rund 1 GB für 5.000 Kunden ist das COPY- oder das Custom-Format dem zeilenweisen `--column-inserts` deutlich überlegen.
Dass beim Einspielen die Datenbankrollen anzupassen sind, ist gelebte Praxis und keine Besonderheit dieses Dumps.

| Aspekt                         | Festlegung                                                                                              |
|--------------------------------|---------------------------------------------------------------------------------------------------------|
| Ort                            | `src/test`, ein Top-Level-`UseCase` `GenerateDemoDataset`, gefahren von `DemoDataScenarioTest`          |
| Kundenzahl                     | Property, Vorgabe 5, für den Datensatz lokal auf 500 gesetzt                                            |
| Aufruf                         | Tag `scenarioTest`, läuft im CI mit; eigene Datenbank und eigener Liquibase-Context `without-test-data` |
| Zieldatenbank                  | über `spring.datasource.url`, wie in `CsvDataImport`                                                    |
| Anomalie-Schalter              | Property, Vorgabe für den Demo-Datensatz: aus                                                           |
| Scenario-Report                | bei 5 Kunden erzeugen, beim vollen Lauf abschalten                                                      |
| `partnerNumber`                | reservierter Block ab `P-90000`, getrennt von Produktion und von den vorhandenen Scenario-Testdaten     |
| `globalUid`/`globalGid`        | reservierter Block ab 900000, weit oberhalb real vergebener Unix-UIDs                                   |
| Zieldatenbank des vollen Laufs | über `HSADMINNG_POSTGRES_JDBC_URL` auf eine dauerhafte Datenbank statt auf den Testcontainer            |
| Ergebnis                       | der Dump entsteht anschließend mit `pg_dump` als Betriebsschritt, datiert und mit Kundenzahl im Namen   |

Der Generator gehört in die Versionsverwaltung, der Dump nicht.
Der Dump ist ein Build-Artefakt und wird auf dem Testserver abgelegt.

Ein `tools/`-Kommando als dünner Aufrufwrapper um den Gradle-Task ist möglich,
aber erst sinnvoll, wenn der Generator regelmäßig von Hand angestoßen wird.

## Begriffsabgrenzung

- **Demo-Datensatz** — kuratiert und vollständig, für Vorführungen vor Kunden.
- **Entwickler-Datensatz** — derselbe Generator mit aktiviertem Anomalie-Schalter, für Entwicklung und Tests.
- **Testdaten** — die bestehenden, bewusst kleinen `*-test-data.sql`-Changesets; von diesem Konzept unberührt.
- **Anonymisierung** — Entfernung des Personenbezugs, sodass eine Zuordnung nicht mehr möglich ist.
- **Pseudonymisierung** — Ersetzung der Bezeichner bei fortbestehendem Personenbezug über Quasi-Identifikatoren.
  Eine maskierte Produktionskopie ist pseudonymisiert, nicht anonymisiert.

## Performance-Abschätzungen

Die Werte gelten für die Anwendung, nicht für den Generator:
wie lange ein einzelner Anlegevorgang dauert, wenn bereits 500, 1.000 oder 5.000 Kunden im Bestand sind.
Sie setzen voraus, dass der Index aus PR#271 gesetzt ist.

Ohne ihn wächst die Laufzeit quadratisch — 100 Kunden in 58 Sekunden, 500 in 536 Sekunden.
Die Ursache ist gemessen und liegt weder bei den Abfragen noch bei den RBAC-Triggern:
deren Kosten je Aufruf bleiben konstant (`hs_office.relation_build_rbac_system` 25,5 ms bei 100 und 25,6 ms bei 200 Kunden).
Verantwortlich ist `set_next_sequential_txid`, das je Schreibtransaktion `MAX(seqTxId)` über die gesamte
`base.tx_context` ermittelt, ohne Index also einen Seq-Scan unter `LOCK TABLE ... IN EXCLUSIVE MODE`.
Die mittlere Dauer verdoppelt sich mit der Tabellengröße, die Gesamtdauer vervierfacht sich;
auf dieses eine Statement entfielen bei 5.000 Kunden rund vier der fünf Stunden.

Mit dem Index aus PR#271 fällt dieser Anteil weg.
Die daraufhin geschätzte eine Stunde für 5.000 Kunden war jedoch zu optimistisch.
Gemessen wurden inzwischen ganze Läufe: 100 Kunden in 3 Minuten, 500 in 14:43 Minuten.
Das sind rund 1,7 Sekunden je Kunde, und der Wert steigt mit der Bestandsgröße weiter an;
für 5.000 Kunden sind daher eher zweieinhalb bis drei Stunden zu erwarten.

Alle Messungen liefen als Global Admin; für Lesezugriffe greift dann der Kurzschluss
in `rbac.select_*_rv` (`1058-rbac-generators.sql:203`).
Ein normaler Benutzer läuft stattdessen über `rbac.queryAccessibleObjectUuidsOfSubjectIds` durch den Grant-Graphen,
wofür es noch keine Messungen gibt.

| Vorgang                              | 500     | 1.000   | 5.000       |
|--------------------------------------|---------|---------|-------------|
| `POST /partners` allein              | ~90 ms  | ~90 ms  | ~90–130 ms  |
| Grunddatensatz eines neuen Mitglieds | ~0,35 s | ~0,35 s | ~0,35–0,5 s |

Der Grunddatensatz umfasst zwei Personen, einen Kontakt, eine Relation, den Partner,
einen Debitor, die Mitgliedschaft und eine Anteilsbuchung — also sieben bis acht Requests.
Die Einzelwerte dahinter:

| Request                        | Dauer  |
|--------------------------------|--------|
| Person anlegen                 | ~20 ms |
| Kontakt anlegen                | ~25 ms |
| Bankverbindung anlegen         | ~20 ms |
| Relation anlegen               | ~70 ms |
| Partner anlegen                | ~90 ms |
| Debitor anlegen                | ~55 ms |
| Mitgliedschaft anlegen         | ~35 ms |
| Anteilsbuchung anlegen         | ~30 ms |
| Lesender Zugriff zur Auflösung | ~15 ms |

Partner, Relation und Debitor sind die teuren Vorgänge, weil hinter jedem eine RBAC-Erzeugung steht;
`POST /partners` legt intern Partner, Details und die `PARTNER`-Relation an.

**Woher die Werte stammen.**
Grundlage sind Messungen mit `pg_stat_statements` bei 100 und 200 erzeugten Kunden.
Der Generator ist ein synchroner Client, deshalb lässt sich aus seiner Wanduhrzeit die Dauer je Request ableiten:
bei 200 Kunden 156 Sekunden abzüglich des quadratischen Anteils, verteilt auf rund 4.800 Requests,
also etwa 33 ms im Mittel — einschließlich Commit, Hibernate und Spring.
Die Einzelwerte oben summieren sich auf rund 0,70 s je erzeugtem Kunden gegenüber gemessenen 0,78 s;
die Größenordnung ist damit gegengeprüft.

**Dass die Werte kaum mit der Datenmenge wachsen, ist gemessen, nicht angenommen.**
`hs_office.relation_build_rbac_system` lag bei 100 wie bei 200 Kunden bei rund 25,5 ms,
ebenso blieben `grantRoleToRole` und `isGranted` flach.
Diese Trigger arbeiten je Objekt, nicht über den gesamten Graphen.
Die Spannen in der 5.000er-Spalte sind Extrapolation: die Messpunkte liegen bei 100 und 200 Kunden,
und Indexpflege sowie Pufferdruck wachsen langsam mit.

**Was die Werte nicht enthalten.**
Gemessen wurde auf `localhost` mit dem Profil `fake-jwt`, also ohne Netzwerk, ohne TLS und ohne echte Tokenprüfung,
gegen eine Datenbank im lokalen Container.
Die Zahlen sind reine Serverzeit vom Eintreffen des Requests bis zur Antwort.
Für Lesezugriffe gelten sie nur für Global Admins, die den Kurzschluss nehmen;
für die Schreibvorgänge oben gibt es keine solche Abkürzung, sie sind daher allgemein gültig.
