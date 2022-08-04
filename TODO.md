
## TODO

This document is just for tracking the initial development project and thus only in German.

### Kommunikative Aufwände

| ID  | Beschreibung                                                |  Budget | Aufwand | Leistung |
|:----|:------------------------------------------------------------|--------:|--------:|---------:|
| PRE | Vorbesprechungen bis inkl. KickOff-Meeting                  |       8 |       4 |        4 |
| PRJ | Zweiwöchige Projektbesprechungen                            |      12 |       2 |        2 |
| RET | Vierwöchige Retrospektiven (optional)                       |       6 |         |          |
| TRA | Einarbeitung von Mitarbeitern des Auftraggebers (Training)  |      12 |         |          |
| E2E | Unterstützung beim Aufbau der E2E-System-Integrations-Tests |      40 |         |          |
| MIG | Unterstützung bei der Datenmigration                        |      16 |         |          |
| INS | Unterstützung ei der Inbetriebnahme / Einführung            |      16 |         |          |
| APP | Abnahme (Approval)                                          |       4 |         |          |
| SUP | Unterstützung nach der Abnahme (Support)                    |      12 |         |          |
|     |                                                             |         |         |          |


### Allgemeine Leistungen

| ID  | Beschreibung                                               |   Budget | Aufwand | Leistung |
|:----|:-----------------------------------------------------------|---------:|--------:|---------:|
| DEV | Aufbau der Entwicklungsumgebung (bis inkl. Unit-Tests)     |       16 |      12 |       16 |
| ATN | Entwurf des Authorisierungs-Systems                        |       40 |      68 |       36 |
| ATZ | Auswahl und Implementierung des Authentifizierungs-Systems |       20 |         |          |
| ITS | Aufbau einer Umgebung für Integrationstests (*1)           |        4 |       4 |        4 |
| ATS | Aufbau einer Umgebung für Akzeptanztests (*1)              |       16 |       3 |          |
| PIP | Aufbau einer Build- und Testpipeline                       |       20 |         |          |
| ARC | Aufbau einer Architekturkontrolle                          |        8 |       2 |        2 |
|     |                                                            |          |         |          |

(*1: ITS+ATS sind aufgesplittet aus TST mit 20 geplanten Stunden entstanden)

### Leistungen bezogen auf fachliche Objekte

| ID  | fachliches Objekt      | Persona        | Ops          | Budget | Aufwand | Leistung |
|:----|:-----------------------|:---------------|:-------------|-------:|--------:|---------:|
| ROL | Rollen                 | Hostmaster     | Scrulojtx    |     26 |      10 |        5 |
| USR | LDAP-User              | Hostmaster     | Scrufojtex   |     29 |      10 |        5 |
| USR | LDAP-User              | LDAP-User      | rufojex      |     20 |      10 |          |
| GRP | Gruppen                | Hostmaster     | scrulojtx    |     26 |         |          |
| CBD | Customer Base          | Sachbearbeiter | ScruLojia    |     20 |      10 |        4 |
| CBD | Customer Base          | Kunde          | sr           |      5 |       1 |          |
| MSV | Managed Virtual Server | Hostmaster     | crudfoj      |     20 |         |          |
| MSV | Managed Virtual Server | Owner          | rulojt       |     15 |         |          |
| MWS | Managed Webspace       | Hostmaster     | scrudfojte   |     26 |       2 |          |
| MWS | Managed Webspace       | Owner          | srulojte     |     18 |       1 |          |
| MWS | Managed Websppace      | Admin          | srulojte     |      6 |       1 |          |
| ACC | Unix-Account           | Owner          | scrudfojte   |     26 |       2 |          |
| ACC | Unix-Account           | Admin          | sruloje      |     15 |       1 |          |
| DOM | Domain                 | Owner          | (scrudfojte) |      9 |       1 |          |
| DOM | Domain                 | Admin          | (srle)       |      3 |       1 |          |
| EMA | E-Mail-Address         | Owner          | (scrudfojte) |      9 |       1 |          |
| MAL | E-Mail-Alias           | Owner          | -            |      0 |       1 |          |
| DBP | Database Postgres      | Owner          | (scrudlojte) |      9 |         |          |
| DBP | Database Postgres      | Admin          | (srle)       |      3 |         |          |
| DUP | Database-User Postgres | Admin          | (scrudlojte) |      9 |         |          |
| DUP | Database-User Postgres | Admin          | -            |      0 |         |          |
| DBM | Database MariaDB       | Owner          | -            |      0 |         |          |
| DBM | Database MariaDB       | Admin          | -            |      0 |         |          |
| DUM | Database-User MariaDB  | Admin          | -            |      0 |         |          |
| DUM | Database-User MariaDB  | Admin          | -            |      9 |         |          |
|     |                        |                |              |        |         |          |

**Ops Agenda**: **S**: Schema, **V**: View, **C**: Create, **R**: Read, **U**: Update, **D**: Delete, **L**: List, **F**: Filter, **O**: Optimistic Locking, **J**: Journal (Audit), **H**: Historization, **U**: Undo, **I**: Inactivate, **T**: Tombstone, **A**: Archive, **E**: Event, **X**: Extraordinary


### Wöchentlicher Status

<!-- file not committed to git, please run `tools/todo-progress` to generate -->
![hsadmin-ng Projektfortschritt](TODO-progress.png)

In der folgenden Tabelle sind Aufwand und Leistung akkumulierte Werte.

<!-- generated todo-progress begin: -->
| Datum      | Budget | Aufwand | Leistung | Verbleibend |
|------------|-------:|--------:|---------:|------------:|
| 2022-07-17 |    553 |      44 |        0 |         553 |
| 2022-07-24 |    553 |       8 |        0 |         553 |
| 2022-07-31 |    553 |     143 |       40 |         513 |
| 2022-08-04 |    553 |     147 |       78 |         475 |
<!-- generated todo-progress end. -->


