# ADR-0002: Branches push one CSV file per day to head office over SFTP

Status: Accepted
Date: 2026-10-19 · Deciders: <member 1>, <member 2>

## Context
Each of the three branches exports one file per day, about 33 000 rows or a
few MB (`BRANCH-yyyy-MM-dd.csv`). Files are due by 23:00 over the branch's
internet link, and links fail now and then (QA-3: a branch file that is 2 h
late must not delay the other branches' reports beyond 07:00). The monthly
report starts at 06:00 on day 2 of M+1 (QA-1). Branch systems are managed by
the branches, behind their own firewalls, and must not gain access to other
branches' data (QA-5). Our batch (ADR-0001) is file-based.

## Decision
Each branch **pushes** its daily file by SFTP to a landing zone at head
office, using one account and key per branch that can write only to its own
folder. A file is uploaded as `*.part` and renamed to `BRANCH-yyyy-MM-dd.csv`
when complete, so the batch never reads a half-written file.

**Failure policy (QA-3):**
- *How long we wait:* the batch starts at 06:00 and polls for missing files
  until 06:30; it does not wait longer.
- *Partial report:* at 06:30 a report is published for every branch whose
  files are complete. The chain total is labelled `INCOMPLETE: REP
  2026-09-30 missing`, listed under anomalies and in the e-mail to the
  finance director, so nobody mistakes it for the final figure.
- *Late file:* when the missing file arrives, the whole month is recomputed
  (about 5 s, cheap) and a new report version replaces the partial one; the
  finance director gets a "complete" e-mail. No incremental merge logic.
- *Idempotent re-run:* the key of a file is its name plus a SHA-256
  checksum. Same name and same checksum: ignored. Same name and different
  checksum: the new file replaces the old one (old kept in `archive/`). It
  never appends, so a repeated upload cannot duplicate rows, and two
  runs over the same inputs produce identical reports.

## Alternatives considered
- **Head office pulls from each branch** (SFTP or HTTPS poll): central
  control of retries, but needs inbound access to three branch networks
  (NAT, firewalls, IP changes) and head office must know every branch's
  availability; the branch can retry itself at no cost to us.
- **POS publishes one event per receipt to a message broker**: near real
  time and buffers outages by design, but needs POS changes the branches
  do not control, an operated broker, exactly-once handling, and about
  1 M events per month to reconcile against the daily files the brief
  already defines (QA-1, QA-5 surface).
- **HTTPS upload instead of SFTP**: no SFTP server, but we would have to
  write and secure an upload service (authentication, size limits) that
  SFTP gives us ready-made.

## Consequences
+ QA-3: a failed link only delays that branch's files; the others are
  processed and published on time.
+ QA-3: re-runs are safe and cheap because they are idempotent.
+ QA-5: per-branch accounts limit what a compromised branch can write.
+ QA-6: tests need only a folder of files, no network.
- QA-3: the chain total is wrong (labelled incomplete) while any file is
  missing; the finance director may act on a partial figure.
- No intraday numbers: data is at best one day old.
- We must operate an SFTP server and manage three branch keys (rotation,
  revocation).
- A fixed 30-minute grace period is a guess; it may be too short for a
  slow link or wasteful if files are normally on time.

Revisit when: any branch misses the 06:30 cutoff on more than 2 days in a
month, head office asks for intraday reports, or the POS vendor can publish
events reliably.
