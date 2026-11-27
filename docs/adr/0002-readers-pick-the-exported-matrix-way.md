# 2. Readers pick the transform matrix way that the exporter keeps

- Status: Accepted
- Date: 2026-11-27
- Deciders: JOSMPlugin maintainers
- Context: IOP-3784
- Decides open item of: [0001](0001-strip-duplicate-transform-matrix-ways.md) (which matrix way the readers pick)

## Context

Since [0001](0001-strip-duplicate-transform-matrix-ways.md), LexxPlussExporter keeps the transform in one
matrix way on save. Files saved before that change can still have two. At LexxConfig 8aa1351b (main on 2026-11-27), 7 of
the 161 `.osm` files in the tree have two ways with the key `transform matrix`; one of them is a backup under
`misc/`. Two readers use the transform:

- PointTransformer (lexxpluss_tools) took the first matrix way of the dataset. The dataset does not keep
  ways in a stable order.
- LexxPlussReader (LexxPlussImporter) took the first matrix way in the file.

When a reader and the exporter use different ways, points placed from CSV data or converted from
`X_image`/`Y_image` use a transform that the next save replaces.

JOSM writes new (negative id) primitives in id order, highest first. On load it keeps a negative id
or renumbers the primitives in file order. In both cases, the highest id in a file that JOSM wrote is
also the highest unique id in the loaded dataset.

Evidence: a headless harness (not in the repository) that loads each file and compares the way that the
exporter keeps with the way each reader uses, run on 2026-11-27 against JOSM r19680+.
- Before this decision, PointTransformer used a different way from the exporter in 6 of the 7 files,
  either in the loaded dataset or in a copy that holds only the matrix ways. LexxPlussReader matched the
  exporter in all 7.
- In a small file where the first matrix way is deleted, both readers used that way and the exporter
  used the other one. In a small file where the first matrix way has nodes, PointTransformer used that
  way; LexxPlussReader did not, because it missed the tags after the nodes.
- With the decision below, both readers use the exporter's way in all 9 files.

## Decision

- A reader uses the same matrix way that the exporter would keep: a way that is not deleted and has
  no nodes, with the highest id.
- If no matrix way is without nodes, a reader uses the live matrix way with the highest id. The exporter
  creates a new way in that case, so no reader can match it. This keeps the transform usable.

## Not decided here

- Re-saving the maps in LexxConfig that still have two matrix ways. This is a separate ticket on the
  LexxConfig side.
- Files with both positive and negative ids, or negative ids that JOSM renumbers only in part.
  A local map file has neither in normal use.

## Consequences

Positive:
- In a file with duplicate matrix ways, CSV capture and the importer use the transform that the
  exporter writes on the next save.
- PointTransformer gives the same result for the same file every time.

Negative / trade-offs:
- The rule is written three times (exporter, PointTransformer, LexxPlussReader). The plugins share no
  source, so a change to the rule must be made in all three.
- The importer's match depends on the order in which JOSM writes and renumbers ids. A file written by
  another tool with matrix ways in a different id order can still be read with another way.
- A file whose only matrix way is deleted now has no transform. Before, both readers used the deleted
  way. PointTransformer shows its "No ways with transform matrix found." notification, and the importer
  leaves nodes that have only `X_image`/`Y_image` without coordinates.

## Confirmation

None in the repository: there are no unit tests. The harness above, and the comments in
PointTransformer and LexxPlussReader that point to this record, are the checks.

## Revisit when

- The exporter's rule in [0001](0001-strip-duplicate-transform-matrix-ways.md) changes.
- The plugins get shared code, so the rule can be written once.

## Alternatives considered

- **Re-save the maps and keep the readers as they were.** Rejected: copies of old files outside
  LexxConfig, and the 7 files until they are re-saved, would still be read with a way chosen by order.
- **Keep the first matrix way in the file (LexxPlussReader's previous rule).** Rejected: it matches the
  exporter only while the first way is live and has no nodes.

## 日本語

読む側（PointTransformer・LexxPlussImporter）は、exporter が残すのと同じ matrix way を使う。
削除されておらずノードの無い way のうち id が最大のもの。該当が無ければ、削除されていない matrix way のうち id が最大のものを使う。
JOSM は負の id を id 降順で書き出し、読み込みでは id を残すかファイル順に振り直す。このため、ファイルで id が最大の way が、dataset でも getUniqueId が最大になる。
代償: 同じ規則が3か所に重複する。importer の一致は JOSM の書き出し順に依存する。
