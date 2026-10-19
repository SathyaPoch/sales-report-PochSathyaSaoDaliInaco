# ADR-0001: Build release 1 as a modular monolith batch job

Status: Accepted
Date: 2026-10-19 · Deciders: <member 1>, <member 2>

## Context
The report for month M is due on the morning of day 2 of M+1 and covers about
3 M rows (3 branches x 30 days x 33 000 rows). Our scenarios (Task 1) give three
architectural drivers:
- **QA-1 (H,H)**: a month of 3 M rows is reported in 60 s or less on an
  8-core server.
- **QA-3 (H,M)**: if one branch file is 2 h late, the other branches' reports
  are still out by 07:00.
- **QA-5 (H,M)**: a branch manager sees only their own branch.

Constraints: a team of two, about 8 weeks, Java 25 and Maven, one Maven
project until Lab-09. Data arrives once per day as files, so nothing needs to
be answered in real time. QA-4 (new format in 2 days or less) and QA-6 (engine
tests in 30 s or less) are important but already cheap to meet.

## Decision
We will build release 1 as one deployable Java program (a modular monolith)
that runs as a scheduled batch job: ingest, engine and render are separate
packages with enforced dependency direction (`DependencyRulesTest`) but run
in one JVM and communicate by in-process calls. The two user interfaces are
separate programs that read the finished reports from the report store.

## Alternatives considered
- **B, process pipeline** (coordinator plus worker JVMs over local sockets):
  isolates crashes and uses more than one machine, but adds process
  start-up, a wire format and process management for a load one JVM handles
  with a large margin (about 5 s estimated against the 60 s limit of QA-1).
- **C, event-driven microservices** (ingest, aggregation, report and
  notification services around a message broker): buffers late files nicely
  (QA-3) and scales best, but at 1 row per event the broker alone needs
  60 s for 3 M messages (QA-1 fails), it needs an operated broker plus
  service-to-service authentication, and two people cannot run it well.
- **D, database-centred ELT** (load into a SQL database, report with SQL):
  attractive for ad-hoc queries, but it adds a database to operate and moves
  the BigDecimal rounding rules out of tested Java code.

The detailed comparison is in `lab01/atam.md`.

## Consequences
+ QA-1: one JVM with parallel parsing is estimated at about 5 s for 3 M rows,
  a 12x margin.
+ QA-5: no network between components, so the only access check is in the
  viewer or the web page; the attack surface stays small.
+ QA-6: engine tests need no broker, container or other process and run in
  seconds.
+ Lowest cost and risk for a two-person team; modules can be split out later
  because the package boundaries are tested.
- QA-3: availability depends on a policy, not on the architecture: the
  single job must handle missing files itself (see ADR-0002), and a crash of
  the JVM stops the whole run (mitigated by an idempotent re-run).
- QA-2: scaling is mainly vertical (cores and heap); the whole month is held
  in one process, so heap size becomes the limit as the chain grows.
- One deployable means a change in any package needs a full rebuild and
  redeploy of the batch.

Revisit when: a month exceeds 10 M rows, the QA-1 median passes 30 s (half
the budget), the chain has more than 6 branches, or head office asks for
intraday (near real-time) reports.
