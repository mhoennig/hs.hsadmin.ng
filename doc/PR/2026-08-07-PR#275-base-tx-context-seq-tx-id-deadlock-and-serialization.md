# PR#275: Deadlocks und Serialisierung durch `base.tx_context.seqTxId`

## Verwandte Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/275

## Das Problem

Der Trigger `set_next_sequential_txid` vergibt `seqTxId` unter `EXCLUSIVE`-Sperre auf `base.tx_context`,
die beim ersten Schreibvorgang genommen und erst beim Commit freigegeben wird.
Das erzeugt Deadlock-Eskalation: `insert` nimmt erst `ROW EXCLUSIVE`, der Trigger verlangt `EXCLUSIVE`;
zwei konkurrierende Transaktionen warten dann verschränkgt gegenseitig aufeinander.

Nachgewiesen mit vier gleichzeitigen Schreibern gegen `demodata500`:
```
ERROR:  deadlock detected
CONTEXT: SQL statement "LOCK TABLE base.tx_context IN EXCLUSIVE MODE"
```

Messung mit je 300 ms Arbeit pro Transaktion: sechs Clients verursachen 50 % Deadlock-Verluste,
einzelne Clients nicht. Heute wenige Nutzer, bald tausende — die Stelle muss jetzt bereinigt werden.

## Anforderung an `seqTxId`

**(1) Aufsteigend in Schreib-Reihenfolge, (2) ohne Serialisierung.** Lückenlosigkeit nicht erforderlich.

Geplant: SaltStack-Synchronisierung nutzt `seqTxId` als Cursor. Konsumenten dürfen nur abgeschlossene
Transaktionen verarbeiten (`txid < pg_snapshot_xmin(pg_current_snapshot())`), da `seqTxId` beim Commit
fällt und Nummern Out-of-Order sichtbar werden.

## Nicht-Ziele

- Lückenlosigkeit der `seqTxId`; die Sequenz hinterlässt bei Rollbacks Lücken, das ist laut Anforderung zulässig.
- Die SaltStack-Synchronisierung selbst; sie folgt in einem eigenen PR und setzt dort die Sichtbarkeitsgrenze durch.
- Deadlock-Behandlung in der Anwendung; sie betrifft andere Sperrquellen und folgt separat.
- `track_commit_timestamp = on`; nur als mögliche Folge notiert, nicht Teil dieses PRs.

## Die Lösung

Eine Sequenz vergibt die Nummer ohne jede Sperre und damit ohne Deadlock und ohne Serialisierung.
Der Trigger wird dabei nicht umgebaut, sondern entfällt: sein einziger Zweck war die Berechnung
der Nummer, und ein Vorgabewert der Spalte leistet dasselbe.

```sql
create sequence base.tx_context_seq_tx_id_seq as bigint;
select setval('base.tx_context_seq_tx_id_seq',
              coalesce((select max(seqTxId) from base.tx_context), 0) + 1, false);
alter table base.tx_context
    alter column seqTxId set default nextval('base.tx_context_seq_tx_id_seq');
drop trigger if exists set_commit_order_trigger on base.tx_context;
drop function if exists public.set_next_sequential_txid();
```

`cache 1` ist erforderlich, da größere Caches der Reihenfolge über den Verbindungspool schaden.
Zwei Changesets nötig: `endDelimiter:--//` schickt den ganzen Block als eine Anweisung;
`alter table` würde mit der Abfrage kollidieren.

Nachgewiesen: `concurrentWritersNeitherDeadlockNorWaitForEachOther` — drei Tx an Barriere starten,
ohne Deadlock und mit kürzerer Gesamtlaufzeit als bei Serialisierung. Mit beiden Changesets besteht
der Test, ohne scheitert er mit Deadlock.

Anmerkung: PR#271 eliminierte den `MAX(seqTxId)`-Index; dieser bleibt, da der Konsument über `seqTxId`
sucht, aber sein Changeset-Kommentar ist jetzt semantisch falsch — nicht änderbar, da veröffentlicht.

## Abhängigkeiten und Folgen

- **Voraus:** PR#271 bereits in `master`
- **Folgen:** SaltStack-Sync (setzt Sichtbarkeitsgrenze durch), Deadlock-Handling in der App,
  optional `track_commit_timestamp = on` (Audit-Commit-Reihenfolge)
