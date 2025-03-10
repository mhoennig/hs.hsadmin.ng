# Änderung eines Geschäftspartners oder Rechnungsempfängers (Debitor)

**Status:**
- [x] vorgeschlagen von (Michael Hönnig)
- [ ] akzeptiert von (...)
- [ ] abgelehnt von (...)
- [ ] ersetzt durch (ersetzende ADR)

## Kontext und Problemstellung

Im vorgegebenen Datenmodell von Geschäftspartnern und Rechnungsempfängern (Debitoren), das auch fachliche Rollen wie Repräsentant, technische Ansprechpartner oder Mailinglisten-Subscriptions umfasst, stellt sich die Frage, wie eine Änderung der Geschäftspartner-Person effizient und konsistent umgesetzt werden kann.
Diese fachlichen Rollen hängen jeweils an der Partner-Person.

Ein konkretes Beispiel hierfür ist die Änderung von einer natürlichen Person, die verstorben ist, zu deren Erbengemeinschaft.
**Hierbei zeigte sich, dass die API-Bedienung durch die Vielzahl neu zu erstellender Objekte und deren Verknüpfungen komplex und fehleranfällig ist. Zudem lassen sich nicht alle Änderung in einer einzigen Transaktion durchführen, was zu Inkonsistenzen führen kann.“** 

Angepasst werden müssen:

1. alle Relations mit der alten Partner-Person:
- die PARTNER-Relation
- die DEBITOR-Relations (ggf. mehrere)
- die OPERATIONS-Relations (ggf. mehrere)
- die SUBSCRIBER-Relations (ggf. mehrere)
- die REPRESENTATIVE-Relations (ggf. mehrere)
- etc.
2. Die PARTNER-Relation hat die Besonderheit, dass sie vom Partner referenziert wird und daher auch dort ausgetauscht werden muss.
3. Die DEBITOR-Relation hat die Besonderheit, dass sie vom Debitor referenziert wird und daher auch dort ausgetauscht werden muss.

Daher sollen möglichst viele dieser *Neuverdrahtungen* im Backend gemacht werden.
Und dafür braucht es dann eine zentrale Stelle, an der die Kaskade ausgelöst wird. 

Derzeit gibt es drei mögliche Varianten, diese Änderung dynamisch umzusetzen, die jeweils unterschiedliche Auswirkungen auf Aufwände, API und Zugriffsrechte haben.

### Technischer Hintergrund

Zum Zeitpunkt der Erstellung dieses ADR existieren folgende relevante Entitäten:
- **Person**: Natürliche oder juristische Person (Name, Firma, Anrede etc.)
- **Contact**: Kontaktdaten einer fachlichen Rolle
- **Relation**: Mit einem Typ (z.B. PARTNER, DEBITOR, REPRESENTATIVE) und Kontaktdaten versehene Beziehung von einer Person (Holder) zu einer anderen (Anchor)
- **Partner**: Sind quasi Zusatzdaten einer PARTNER-Relation (derzeit nur die Partnernummer), welche eine Partner-Person mit der Hostsharing-Person verknüpft
- **Debitor**: Sind quasi Zusatzdaten einer DEBITOR-Relation, welche eine Debitor-Person mit einer Partner-Person verknüpft

Zugriffsrechte werden über ein hierarchisches, dynamisches RBAC-System gesteuert, bei dem der **OWNER** einer Entitäten-Instanz alle Rechte hat, **ADMIN** definierte Spalten aktualisieren darf, **AGENT** Verknüpfungen anlegen kann, und **TENANT**, **GUEST** sowie **REFERRER** nur Lesezugriff haben.
Partner und Debitor nutzen dabei die RBAC-Rollen der zugehörigen Relations.

## In Betracht gezogene Varianten

* **1. Relations ersetzen:** Austausch der PARTNER-/DEBITOR-/OPERATIONS-/...-Relations gegen eine neue Relation für die neue Partner-Person (z.B. Erbengemeinschaft) als neuen Holder als PATCH auf /api/hs/office/partners/UUID
* **2. Relations direkt aktualisieren:** Änderung der Holder-Referenz in der bestehenden PARTNER-Relation auf die neue Partner-Person (z.B. Erbengemeinschaft) als PATCH auf /api/hs/office/relations/UUID
* **3. Relations via Partner aktualisieren:** Änderung der Partner-Person in die PARTNER-Relation als PATCH auf /api/hs/office/partners/UUID

### Variante 1: Relations ersetzen

Der Austausch der Partner- (und Debitor-) Person erfolgt über das Erstellen einer neuen PARTNER- bzw. DEBITOR-Relation, im Partner bzw. Debitor wird dann die Referenz auf die alte PARTNER- bzw. DEBITOR-Relation gegen die neue ausgetauscht.

#### Vorteile

- **Beibehaltung der API:** Dieses Verhalten ist bereits implementiert und benötigt keinen großen Umbau an der API, sondern nur eine Erweiterung um das Austauschen weiterer Relations.
- **UPDATE-Permission für AGENT:** Es wäre möglich, der AGENT-Rolle einer Relation UPDATE-Rechte an der Relation zu geben, weil nur die unkritische Contact-Referenz änderbar wäre.
- **Kongruenz von Fachlichkeit+API**: Fachlich handelt es sich um den Austausch der Partner-Person, dazu passend wäre der Endpunkt, allerdings wird in dieser Variante nicht direkt die Partner-Person ausgetauscht, sondern eine neue PARTNER-Relation mit der neuen Partner-Person eingesetzt.

#### Nachteile

- **Verlust expliziter GRANTs:** Gibt es explizite GRANTs an der PARTNER-Relation, gehen diese verloren, da die Relation ausgetauscht wird. Die Übernahme dieser expliziten Grants erfordert also einen zusätzlichen Implementationsaufwand.
- **Divergenz zwischen Fachlichkeit und API:** Fachlich handelt es sich um den Austausch der Partner-Person, würde aber eine neue PARTNER-Relation dieser Person in den Partner eingesetzt werden. Das erfordert ein höheres Verständnis des Datenmodells.
- **Keine Anwendbarkeit auf abhängige Relations:** Beim Aktualisieren der abhängigen Relations (z.B. Representative, Operational- und Billing-Kontakt sowie der Mailinglisten-Subscriptions) stehen wir wieder vor dem Ausgangsproblem und müssten jeweils neue Relations erzeugen und die alten Relations löschen, was dann wieder zum Verlust expliziter GRANTs führt.
- **Performance bei vielen abhängigen Relations:** die abhängigen Relations können nur über Loops, nicht aber durch direkt SQL UPDATEs ausgetauscht werden, was zu einer schlechteren Performance führt

### Variante 2: Relations direkt aktualisieren

Die bestehende PARTNER-Relation bliebe erhalten, und der Holder wird von der verstorbenen Person auf die Erbengemeinschaft geändert.

#### Vorteile

- **Anwendbarkeit auf Partner- und Debitor-Person:** Der Code wäre an einer generischen Stelle, welche dann Partner- und Debitor-Person austauschbar machen würde
- **Einheitlichkeit/Generizität der API:** Die REST-API für Änderungen gehört dann einheitlich zum Relation-Endpunkt, was der bestehenden Handhabung von Contact-Änderungen entspricht.

#### Nachteile

- **UPDATE Permission für Relation-AGENT wäre kritisch:** Der Relation-AGENT darf nicht das Recht bekommen, den Holder auszutauschen. Da es keine Spalten-spezifischen Update-Rechte gibt, könnte dieser auch den Contact nicht mehr austauschen. Derzeit ist das allerdings auch noch nicht so implementiert.
- **Umbau der API:** Der Austausch einer Partner-Person würde vom Partner-Endpunkt (/api/hs/office/partner) zur Relation (/api/hs/office/partner) wandern, was ein größerer Umbau, auch bei den Tests wäre.
- **Divergenz von Fachlichkeit und API**: Fachlich handelt es sich um den Austausch der Partner-Person, aber man würde die Person nicht am Partner selbst austauschen, sondern an der PARTNER-Relation.

### Variante 3: Relations via Partner aktualisieren

Der Austausch der Partner- (bzw. Debitor-) Person würde weiterhin beim Partner bzw. Debitor erfolgen, jedoch würde die Personen-Referenz direkt in der bestehenden Partner- (bzw. Debitor-) Relation umgesetzt werden, statt eine neue Relation mit der neuen Partner- (bzw. Debitor) Person einzusetzen. Die direkt wie auch abhängige Relations könnten also einfach per SQL UPDATE aktualisiert werden.

#### Vorteile

- **Beibehaltung der API:** Der Endpunkt /api/hs/office/partners/UUID bliebe erhalten, wenn auch lokal ein Umbau auf Person-Update statt Relation-Update erfolgen müsste, Anpassungen in Verwendungen dieser API, z.B. in Tests, wären allerdings wenig aufwändig und das Risiko für weitere Aufwände recht gering.
- **UPDATE-Permission für AGENT:** Es wäre möglich, der AGENT-Rolle einer Relation UPDATE-Rechte an der Relation zu geben, aber eine Aktualisierung über die REST-Controller nur an kontrollierten Stellen zuzulassen.
- **Kongruenz von Fachlichkeit+API**: Fachlich handelt es sich um den Austausch der Partner-Person, was auch in dieser Variante technisch abgebildet würde, wenn auch eine Ebene tiefer im JSON, nämlich in der Partner-Relation.

#### Nachteile

Nennenswerte Nachteile wurden nicht identifiziert, allenfalls ist es etwas schräge, dass die RBAC-Rechte an den Relations ein UPDATE zulassen, was aber an der API nur für bestimmte Relations (ggf. kontrolliert) erreichbar wäre.


## Entscheidung und Ergebnis

**Entscheidung:** 3. Relations via Partner aktualisieren 

**Begründung:**
- die Fachlichkeit wird an der API gut abgebildet (PATCH der Partner-Person auf /api/hs/office/partners/UUID)
- der Aufwand ist relativ gering (vieles ist mit SQL UPDATEs machbar)
- die UPDATE Permission dürfte an Relation-AGENT granted werden, ohne damit Schindluder getrieben werden kann (weil das an der API verhindert werden kann)

| Kriterium \ Relations ...                     | 1. ersetzen | 2. direkt aktualisieren | 3. via Partner aktualisieren |
|-----------------------------------------------|------------:|------------------------:|-----------------------------:|
| **Technische und Aufwands-Kriterien**         |             |                         |                              |
| Beibehaltung der API vs. Umbau (inkl. Risiko) |          +2 |                      -2 |                           +1 |
| Anwendbarkeit auf Partner- und Debitor-Person |             |                      +1 |                              |
| Anwendbarkeit auf abhängige Relations         |          -3 |                         |                              |
| Performance bei vielen abhängigen Relations   |          -1 |                         |                              |
| Aufwand für explizite Grants                  |          -1 |                         |                              |
| **Zwischenergebnis**                          |      **-3** |                  **-1** |                       **+1** |
|                                               |             |                         |                              |
| **Fachliche Kriterien**                       |             |                         |                              |
| Kongruenz von Fachlichkeit+API                |          +1 |                      -1 |                           +1 |
| Einheitlichkeit/Generizität der API           |             |                      +1 |                              |
| UPDATE Permission für Relation-AGENT möglich  |          +1 |                         |                           +1 |
| **Zwischenergebnis**                          |      **+2** |                   **0** |                       **+2** |
|                                               |             |                         |                              |
| **Endergebnis**                               |      **-1** |                  **-1** |                       **+3** |
