(this is just a scribbled idea, that's why it's still in German)

Ich habe mal wieder vom RBAC-System geträumt 🙈 Ok, im Halbschlaf darüber nachgedacht trifft es wohl besser. Und jetzt frage ich mich, ob wir viel zu kompliziert gedacht haben.

Bislang gingen wir ja davon aus, dass, wenn komplexe Entitäten (z.B. Partner) erzeugt werden, wir wir über den INSERT-Trigger den Rollen der verknüpften Entitäten (z.B. den Rollen der Personendaten des Partners) auch Rechte an den komplexeren Entitäten und umgekehrt geben müssen.

Da die komplexen Entitäten nur mit gewissen verbundenen Entitäten überhaupt sinnvoll nutzbar sind und diese daher über INNSER JOINs mitladen, könnte sonst auch nur jemand diese Entitäten, der auch die SELECT-Permission an den verküpften Entitäten hat.

Vor einigen Wochen hatten wir schon einmal darüber geredet, ob wir dieses Geflecht wirklich komplett durchplanen müssen, also über mehrere Stufen hinweg, oder ob sehr warscheinlich eh dieselben Leuten an den weiter entfernten Entitäten die nötien Rechte haben, weil dahinter dieselben User stehen. Also z.B. dass gewährleistet ist, dass jemand mit ADMIN-Recht an den Personendaten des Partners auch bis in die SEPA-Mandate eines Debitors hineinsehen kann.

Und nun gehe ich noch einen Schritt weiter: Könnte es nicht auch andersherum sein? Also wenn jemand z.B. SELECT-Recht am Partner hat, dass wir davon ausgehen können, dass derjenige auch die Partner-Personen- und Kontaktdaten sehen darf, und zwar implizit durch seine Partner-SELECT-Permission und ohne dass er explizit Rollen für diese Partner-Personen oder Kontaktdaten inne hat?

Im Halbschlaf kam mir nur die Idee, warum wir nicht einfach die komplexen JPA-Entitäten zwar auf die restricted View setzen, wie bisher, aber für die verknüpften Entitäten auf die direkten (bisher "Raw..." genannt) Entitäten gehen. Dann könnte jemand mit einer Rolle, welche die SELECT-Permission auf die komplexe JPA-Entität (z.B.) Partner inne hat, auch die dazugehörige Relation(ship) ["Relation" wurde vor kurzem auf kurz "Relation" umbenannt] und die wiederum dazu gehörigen Personen- und Kontaktdaten lesen, ohne dass in einem INSERT- und UPDATE-Trigger der Partner-Entität die ganzen Grants mit den verknüpften Entäten aufgebaut und aktualisiert werden müssen.

Beim Debitor ist das nämlich selbst mit Generator die Hölle, zumal eben auch Querverbindungen gegranted werden müssen, z.B. von der Debitor-Person zum Sema-Mandat - jedenfalls wenn man nicht Gefahr laufen wollte, dass jemand mit Admin-Rechten an der Partner-Person (also z.B. ein Repräsentant des Partners) die Sepa-Mandate der Debitoren gar nicht mehr sehen kann. Natürlich bräuchte man immer noch die Agent-Rolle am Partner und Debitor (evtl. repräsentiert durch die jeweils zugehörigen Relation - falls dieser Trick überhaupt noch nötig wäre), sowie ein Grant vom Partner-Agent auf den Debitor-Agent und vom Debitor-Agent auf die Sepa-Mandate-Admins, aber eben ohne filigran die ganzen Neben-Entäten (Personen- und Kontaktdaten von Partner und Debitor sowie Bank-Account) in jedem Trigger berücksichtigen zu müssen. Beim Refund-Bank-Account sogar besonders ätzend, weil der optional ist und dadurch zig "if ...refundBankAccountUuid is not null then ..." im Code enstehen (wenn der auch generiert ist).

Mit anderen Worten, um als Repräsentant eines Geschäftspartners auf den Bank-Account der Sepa-Mandate sehen zu dürfen, wird derzeut folgende Grant-Kette durchlaufen (bzw. eben noch nicht, weil es noch nicht funktioniert):

User -> Partner-Holder-Person:Admin -> Partner-Relation:Agent -> Debitor-Relation:Agent -> Sepa-Mandat:Admin -> BankAccount:Admin -> BankAccount:SELECT

Daraus würde:

User -> Partner-Relation:Agent -> Debitor-Relation:Agent -> Sepa-Mandat:Admin -> Sepa-Mandat:SELECT*

(*mit JOIN auf RawBankAccount, also implizitem Leserecht)

Das klingt zunächst nach nur einer marginalen Vereinfachung, die eigentlich Vereinfachung liegt aber im Erzeugen der Grants in den Triggern, denn da sind zudem noch Partner-Anchor-Person, Debitor-Holder- und Anchor-Person, Partner- und Debitor-Contact sowie der RefundBankAccount zu berücksichtigen. Und genau diese Grants würden großteils wegfallen, und durch implizite Persmissions über die JOINs auf die Raw-Tables ersetzt werden. Den refundBankAccound müssten wir dann, analog zu den Sepa-Mandataten, umgedreht modellieren, da den sonst

Man könnte das Ganze auch als "Entwicklung der Rechtestruktur für Hosting-Entitäten auf der obersten Ebene" (Manged Webspace, Managed Server, Cloud Server etc.) sehen, denn die hängen alle unter dem Mega-komplexen Debitor.  
