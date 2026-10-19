# Design review: cohesion and coupling of sales-report 0.1.0

Scope: `src/main/java/edu/itc/salesreport` after Task 4. Numbers come from the
`import edu.itc.salesreport...` lines (tests are not counted).

## Cohesion: one responsibility per package

| Package | Responsibility (one sentence) |
|---------|-------------------------------|
| `model` | Immutable canonical business types (transaction, summaries, report) that enforce their own invariants and do the money arithmetic. |
| `ingest` | Turns POS CSV text into validated `SaleTransaction`s and tells observers how loading progresses. |
| `render` | Turns a finished `MonthlyReport` into bytes of one output format (Strategy). |
| root (`App`) | Composition root: the only place that creates concrete objects and wires observers. |

## Coupling measured from the imports

Definitions: **Ce** (efferent) = distinct classes outside the package that the
package imports; **Ca** (afferent) = distinct classes inside the package that
other packages import; **I = Ce / (Ca + Ce)** (0 = stable, 1 = unstable).

| Package | Ce (classes imported) | Ca (classes imported by others) | I = Ce / (Ca + Ce) |
|---------|-----------------------|---------------------------------|--------------------|
| `model` | 0 | 4: `PaymentMethod`, `SaleTransaction` (by ingest); `BranchSummary`, `MonthlyReport` (by render) | 0 / (4 + 0) = **0.00** |
| `ingest` | 2: `PaymentMethod`, `SaleTransaction` | 3: `ConsoleProgress`, `CsvTransactionParser`, `MonthLoader` (by App) | 2 / (3 + 2) = **0.40** |
| `render` | 2: `BranchSummary`, `MonthlyReport` | 0 (nobody in `main` uses it yet) | 2 / (0 + 2) = **1.00** |
| root `App` | 3: `ConsoleProgress`, `CsvTransactionParser`, `MonthLoader` | 0 | 3 / (0 + 3) = **1.00** |

Reading: dependencies point one way, `App` to `ingest`/`render` to `model`, with
no cycle. The most depended-upon package (`model`) is the most stable
(I = 0), which follows the Stable Dependencies Principle. `render` has I = 1.0
only because its caller (the engine, Lab-02, and the UI, Lab-03) does not
exist yet; it will drop once those packages import it.
`DependencyRulesTest` enforces the two rules that matter: `model` imports no
other project package, and `ingest` never imports `render`.

## One coupling I accept

`ingest` and `render` both import `model`. This is deliberate: `model` is the
shared vocabulary of the system, it is immutable and has no outgoing
dependencies, so a change there is a conscious, compiler-checked event. The
alternative (each package with its own DTOs plus mappers) would add copying
and mapping code with no benefit at this size.

## Sealed hierarchy or open interface for QA-4 (new export format in 2 days or less)?

QA-4 is about our own team adding PDF and XLSX (Lab-08) fast and safely, so I
keep the **sealed** `ReportRenderer`. A new format costs 1 new class, 1 name
in `permits` and 1 `case` in `forExtension`, about 3 touched places, well
inside 2 person-days. In return the compiler flags every `switch` that forgot
the new type, so no format can be half-added. An open interface would let
third parties plug in renderers without touching `render`, but nobody
outside the team needs that in release 1 (YAGNI), and an open interface loses
the exhaustiveness check. Revisit if customers must supply their own formats
(then switch to `ServiceLoader` with an open interface).
