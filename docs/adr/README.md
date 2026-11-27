# Architecture Decision Records

Decisions that shape this repository and would otherwise live only in Jira tickets or PR
descriptions are recorded here, one file per decision, numbered in order
(`NNNN-short-title.md`). A record is never edited to say something different — supersede it
with a new one and link both.

| # | Title | Status |
|---|-------|--------|
| [0001](0001-strip-duplicate-transform-matrix-ways.md) | Remove duplicate transform matrix ways from the saved file, not from the dataset | Accepted; which matrix way the readers pick decided by [0002](0002-readers-pick-the-exported-matrix-way.md) |
| [0002](0002-readers-pick-the-exported-matrix-way.md) | Readers pick the transform matrix way that the exporter keeps | Accepted |
