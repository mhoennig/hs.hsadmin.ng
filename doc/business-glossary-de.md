### hsadminNg fachliches Glossar

<!--
Currently, this business glossary is only available in German because in many cases, 
the German terms are important for comprehensibility for those using this software.
-->

Dieses ist eine Sammlung von Fachbegriffen, die in diesem Projekt benutzt werden.
Ebenfalls aufgenommen sind technische Begriffe, die für Benutzer für das Verständnis der Schnittstellen nötig sind.

Falls etwas fehlt, bitte Bescheid geben.


#### Partner

In diesem System ist ein _Partner_ grundsätzlich jeglicher Geschäftspartner der _Hostsharing eG_.
Dies können grundsätzlich Kunden, siehe [Debitor](#Debitor), wie Lieferanten sein.
Derzeit sind aber nur Debitoren implementiert.

Des Weiteren gibt es für jeden _Partner_ eine fünfstellige Partnernummer mit dem Prefix 'P-' (z.B. `P-123454`)
sowie Zusatzinformationen (z.B. Registergerichtnummer oder Geburtsdatum), die zur genauen Identifikation benötigt werden.

Für einen _Partner_ kann es gleichzeitig mehrere [Debitoren](#Debitor) 
und zeitlich nacheinander mehrere [Mitgliedschaften](#Mitgliedschaft) geben.

Partner sind grundsätzlich als ist [Relation](#Relation) der Vertragsperson mit der Person _Hostsharing eG_ implementiert.


### Debitor

Ein `Debitor` ist quasi ein Rechnungsempfänger für einen [Partner](#Partner).

Für einen _Partner_ kann es gleichzeitig mehrere [Debitoren](#Debitor) geben, 
z.B. für spezielle Projekte des Kunden oder verbundene Organisationen.

Des Weiteren gibt es für jeden _Partner_ eine fünfstellige Partnernummer mit dem Prefix 'P-' (z.B. `P-123454`)
sowie Zusatzinformationen (z.B. Registergerichtsnummer oder Geburtsdatum), die zur genauen Identifikation benötigt werden.

Debitoren sind grundsätzlich als ist [Relation](#Relation) der Vertragsperson mit der Person des Vertragspartners implementiert.


#### Relation

Eine _Relation_ ist eine typisierte und mit Kontaktdaten versehene Beziehung einer (_Holder_)-Person zu einer _Anchor_-Person.

Eine Relation ist eine Art Geschäftsrolle, wir haben hier aber keinen Begriff mit 'Rolle' verwendet,
weil 'Role' (engl.) zu leicht mit der [RBAC-Rolle](#RBAC-Role) verwechselt werden könnte.

Die _Relation_ ist auch ein technisches Konzept und gehört nicht zur Domänensprache.
Dieses Konzept ist jedoch für das Verständnis der ([API](#API)) notwendig.


#### Ex-Partner

Ex-Partner bilden [Personen](#Person) ab, die vormals [Partner](#Partner) waren.
Diese bleiben dadurch informationshalber im System verfügbar.

Implementiert ist der _Ex-Partner_ als eine besondere Form der [Relation](#Relation)
der Person des Ex-Partner (_Holder_) zum neuen Partner (_Anchor_) dargestellt.
Dieses kann zu einer Kettenbildung führen.


#### Representative-Contact (ehemals _contractual_)

Ein _Representative_ ist eine natürliche Person, die für eine nicht-natürliche Person vertretungsberechtigt ist.

Implementiert ist der _Representative_ als eine besondere Form der [Relation](#Relation) 
der Person des Repräsentanten (_Holder_) zur repräsentierten Person (_Anchor_) dargestellt.


### VIP-Contact

Ein _VIP-Contact_ ist eine natürliche Person, die für einen Geschäftspartner eine wichtige Funktion übernimmt, 
nicht aber deren offizieller Repräsentant ist. 

Implementiert ist der _VIP-Contact_ als eine besondere Form der [Relation](#Relation)
der Person des VIP-Contact (_Holder_) zur repräsentierten Person (_Anchor_) dargestellt.


### Operations-Contact

Ein _Operations-_Contact_ ist_ eine natürliche Person, die für einen Geschäftspartner technischer Ansprechpartner ist.

Ein Seiteneffekt ist, dass diese Person im Ticketsystem Znuny direkt dem Geschäftspartner zugeordnet werden kann.

Im Legacy System waren das die Kontakte mit der Rolle `operation` und `silent`.

Implementiert ist der _Operations-Contact_ als eine besondere Form der [Relation](#Relation)
der Person des _Operations-Contact_ (_Holder_) zur repräsentierten Person (_Anchor_) dargestellt.


### OperationsAlert-Contact

Ein _OperationsAlert-_Contact_ ist_ eine natürliche Person, die für einen Geschäftspartner bei technischen Probleme kontaktiert werden soll.

Im Legacy System waren das die Kontakte mit der Rolle `operation`.

Implementiert ist der _OperationsAlert-Contact_ als eine besondere Form der [Relation](#Relation)
der Person des _OperationsAlert-Contact_ (_Holder_) zur repräsentierten Person (_Anchor_) dargestellt.


### Subscriber-Contact

Ein _Subscriber-_Contact_ ist_ eine natürliche Person, die für einen Geschäftspartner eine bestimmte Mailingliste abonniert.

Implementiert ist der _Subscriber-Contact_ als eine besondere Form der [Relation](#Relation)
der Person des _Subscriber-Contact_ (_Holder_) zur repräsentierten Person (_Anchor_) dargestellt.
Zusätzlich wird diese Relation mit dem Kurznamen der abonnierten Mailingliste markiert.  


### Coop-Asset-Transactions (Geschäftsguthabens-Transaktionen)

- positiver Wert => Geschäftsguthaben nehmen zu
- negativer Wert => Geschäftsguthaben nehmen ab

**REVERSAL**: **Korrekturbuchung** einer fehlerhaften Buchung, positiver oder negativer Wert ist möglich 

**DEPOSIT**: **Zahlungseingang** vom Mitglied nach Beteiligung mit Geschäftsanteilen, immer positiver Wert

**DISBURSAL**: **Zahlungsausgang** an Mitglied nach Kündigung von Geschäftsanteilen, immer negativer Wert

**TRANSFER**: **Übertragung** von Geschäftsguthaben an ein anderes Mitglied, immer negativer Wert

**ADOPTION**: **Übernahme** von Geschäftsguthaben von einem anderen Mitglied, immer positiver Wert

**CLEARING**: **Verrechnung** von Geschäftsguthaben mit Schulden des Mitglieds, immer negativer Wert

**LOSS**: **Verlust** von Geschäftsguthaben bei Zuweisung Eigenkapitalverlust nach Kündigung von Geschäftsanteilen, immer negativer Wert 

**LIMITATION**: **Verjährung** von Geschäftsguthaben, wenn Auszahlung innerhalb der Frist nicht möglich war.


### Coop-Share-Transactions (Geschäftsanteil-Transaktionen)

- positiver Wert => Geschäftsanteile nehmen zu
- negativer Wert => Geschäftsanteile nehmen ab
- 
**REVERSAL**: **Korrekturbuchung** einer fehlerhaften Buchung, positiver oder negativer Wert ist möglich

**SUBSCRIPTION**: **Beteiligung** mit Geschäftsanteilen, z.B. durch Beitrittserklärung, immer positiver Wert

**CANCELLATION**: **Kündigung** von Geschäftsanteilen, z.B. durch Austritt, immer negativer Wert


#### Anchor / Relation-Anchor

siehe [Relation](#Relation)


#### Holder / Relation-Holder

siehe [Relation](#Relation)


#### API

Und API (Application-Programming-Interface) verstehen wir eine über HTTPS angesprochene programmatisch bedienbare Schnittstell 
zur Funktionalität des hsAdmin-NG-Systems.
