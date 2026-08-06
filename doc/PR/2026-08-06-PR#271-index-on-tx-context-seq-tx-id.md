# PR#271: Index auf `base.tx_context.seqTxId`

## Das Problem

Der Trigger `set_next_sequential_txid` in `0-base/020-audit-log.sql` vergibt die fortlaufende Transaktionsnummer
per `SELECT COALESCE(MAX(seqTxId)+1, 0) FROM base.tx_context`, unter `LOCK TABLE ... IN EXCLUSIVE MODE`.

Auf `seqTxId` gibt es keinen Index — nur den Primärschlüssel auf `txId` und einen BRIN-Index auf `txTimestamp`.
`MAX(seqTxId)` ist damit ein sequenzieller Scan über die gesamte Tabelle, bei **jeder** schreibenden Transaktion,
und die Kosten je Schreibvorgang wachsen mit der Zahl der bereits geschriebenen Zeilen.

Gemessen mit `pg_stat_statements` bei der Demo-Datenerzeugung aus PR#270:

| Zeilen in `base.tx_context` | Aufrufe | Mittel | Gesamt |
|---|---|---|---|
| 1.482 | 32.495 | 0,175 ms | 5,7 s |
| 2.934 | 61.568 | 0,351 ms | 21,6 s |

Zeilenzahl verdoppelt, mittlere Dauer verdoppelt, Gesamtdauer vervierfacht — quadratisches Wachstum.
Die RBAC-Trigger blieben zum Vergleich konstant und sind damit nicht die Ursache.

Im Betrieb bleibt der Scan klein, weil die Transaktionstabellen aus Datenschutzgründen
per Cronjob auf etwa zwei Wochen gekürzt werden sollen; ein Massen-Import trifft das Problem aber voll.

## Nicht-Ziele

- Der Exclusive-Lock bleibt: ob die lückenlose Reihenfolge fachlich gebraucht wird, ist keine Performance-Frage.
- Keine PostgreSQL-Sequenz statt `MAX(...)`: sie erzeugt Lücken bei Rollbacks und wäre eine fachliche Entscheidung.

## Die Lösung

Ein neues Changeset in `0-base/020-audit-log.sql`, direkt hinter dem für Spalte und Trigger:

```sql
create index if not exists tx_context_seq_tx_id_idx
    on base.tx_context (seqTxId);
```

Das bestehende Changeset bleibt unverändert: es trägt weder `runOnChange:true` noch `--validCheckSum: ANY`
und mischt `alter table` mit `create or replace function`, ist also nach `AGENTS.md` nicht änderbar.

PostgreSQL löst `MAX(...)` über den Index als rückwärts gelesenen Index-Only-Scan mit `LIMIT 1` auf.
An einer Tabelle mit 80.000 Zeilen gemessen:

| | Plan | gelesene Zeilen | Kosten |
|---|---|---|---|
| ohne Index | `Seq Scan` | 80.000 | 1433,01 |
| mit Index | `Index Only Scan Backward` + `Limit` | 1 | 0,33 |

Aus einem Aufwand, der mit der Tabellengröße wächst, wird einer, der praktisch konstant bleibt.
Für die Erzeugung von 5.000 Demo-Kunden aus PR#270 bedeutet das rund eine Stunde statt rund fünf.

## Voraussetzungs-PRs

- keine merge-Abhängigkeit, Messungen und Begründung stammen aber von PR#270 (generierte Demo- und Testdaten)