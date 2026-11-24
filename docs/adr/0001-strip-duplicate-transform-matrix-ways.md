# 1. Remove duplicate transform matrix ways from the saved file, not from the dataset

- Status: Accepted
- Date: 2026-11-24
- Deciders: JOSMPlugin maintainers
- Context: IOP-3335

## Context

LexxPlussExporter writes the PicLayer transform into the tags of one way that has the key `transform matrix`.
Copying and pasting that way, or merging layers, adds a second one. The exporter updated only the first way
it found, so the other kept an old transform. At LexxConfig 58402dc5, 5 of 101 maps had two such ways
(2 of them in production), and tools that read the file cannot tell which one is current.

The duplicates must go when the map is saved. A save is not an undoable command: commands on JOSM's
undo/redo stack may still point at a duplicate way, and they check the way's state when they run.

Evidence: a headless harness (not in the repository) that loads the 5 maps and runs the save logic
against JOSM r19680+, run on 2026-11-24.
- Removing the way from the dataset made a later undo or redo of a delete throw AssertionError.
- Marking it deleted made a later redo of a delete throw IllegalArgumentException.
- With the decision below, all 5 maps are written with one matrix way and no empty way,
  lat/lon of every node is unchanged, and the undo/redo cases pass.

## Decision

- On save, exactly one matrix way keeps the transform tags: the oldest live way without nodes.
  If there is none, a new way is created.
- Every other matrix way loses only the transform tags. It stays in the dataset, and its state is not
  changed, so commands on the undo/redo stack keep working.
- When the file is written, a way that lost its tags this way is left out while it has no tags, no nodes
  and no relation that will be written lists it. A way that gets tags or nodes back is written again.

## Not decided here

- Preventing duplicates when a matrix way is pasted or a layer is merged.
- Which matrix way the readers (PointTransformer in lexxpluss_tools, LexxPlussImporter) pick in files
  saved before this change.
- Other write paths: autosave and session files still contain the empty ways.

## Consequences

Positive:
- A map saved through LexxPlussExporter has a single matrix way with the transform of that save.
- Undo and redo across a save do not throw.

Negative / trade-offs:
- The dataset keeps an empty way per duplicate until the layer is closed. The Validator or a selection
  can show it.
- The exporter writes the file with its own OsmWriter subclass instead of OsmWriterFactory, so a factory
  installed by another plugin is not used for these saves.
- A deleted way with a positive id that lost its tags is left out of the file instead of being written
  with action=delete. The maps are local files that are not uploaded to OSM.
- Copies of a stripped way made by merging layers are not known as stripped and are written as empty ways.

## Confirmation

None in the repository: there are no unit tests. The harness above and a manual save in JOSM with
PicLayer (count the ways that have the key `transform matrix`) are the checks.

## Revisit when

- Duplicates are prevented at paste or merge time, so a save no longer meets them.
- JOSM lets a plugin remove a primitive in a way the undo/redo stack accepts.

## Alternatives considered

- **Remove the duplicates from the dataset (`DataSet.removePrimitive`).** Rejected: an undo or redo of
  a delete command that points at the way throws AssertionError ("Primitive is of wrong data set for
  this command"), reproduced in the harness.
- **Mark the duplicates deleted (`setDeleted(true)`).** Rejected: a later redo of a delete throws
  IllegalArgumentException ("is already deleted"), reproduced in the harness. A deleted way with a
  positive id is also still written with action=delete.
- **Strip the tags and write the empty ways.** Rejected: each duplicate leaves a way with no tags and
  no nodes in the file.

## 日本語

保存時に `transform matrix` way が複数あれば、最も古いノード無しの way だけに変換タグを書き、残りはタグだけ消して dataset に残す。
dataset から外す案と削除フラグを立てる案は、undo/redo のコマンドが例外を投げることをハーネスで再現して却下した。
タグを消した way は、タグ・ノード・書き出される relation からの参照がどれも無い間だけ、ファイルの書き出しから除く。
代償として、dataset には空 way がレイヤーを閉じるまで残り、保存は OsmWriterFactory を通らなくなる。
