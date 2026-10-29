# SolidCraft Mobile – Funktionsparität

Diese Inventur wird aus [`docs/features.md`](docs/features.md) abgeleitet, dem vollständigen Befehlsregister des Desktop-Quellcodes. Jede registrierte Aktion ist einzeln aufgeführt. „Gemeinsam vorhanden“ bedeutet, dass Android denselben Rust-Befehl und dieselbe egui-Ansicht verwendet; es ist ausdrücklich **kein** Nachweis der Bedienbarkeit auf einem Touchgerät. Android-spezifische Verifikation und notwendige Portierungsarbeit sind separat markiert.

- Registrierte Originalbefehle: **405**
- Als geplant markiert (nicht Teil des vorhandenen Originals): **1**
- Android-Einstiegspunkt: `apps/solidcraft-android/src/lib.rs` (gemeinsames `SolveApp` + `Session`).
- Android-Host: `app/src/main/java/org/solidcraft/mobile/MainActivity.kt` (GameActivity).
- SAF filter maps registered CAD MIME types and disables incomplete filters when private formats are accepted. glTF/GLB external buffer references are percent-decoded, path-checked, and copied from a user-selected folder into the staged design workspace. GLB v2 fields are parsed as little-endian; truncated headers and missing JSON chunks are rejected. Embedded URI handling matches the Rust importer's lowercase `data:` scheme. Kotlin unit tests cover these cases as well as nested provider-tree traversal, missing resources, aggregate size limits, and unsafe provider names. An API-36 Android emulator opened an external-buffer glTF via DocumentsUI, granted a project subfolder, staged the 42-byte buffer, and imported one body; the Downloads root itself is blocked by Android's folder picker, so the test used a user-selectable project subfolder.
- Touch selection expands screen-space hit slop for sketch points/curves, vertices, edges and axes only for viewport touch streams; touches outside the viewport do not widen CAD targets, and mouse thresholds stay unchanged. Rust host tests cover one-finger event ownership, the expanded-vs-precise tolerance, and the outside-viewport boundary. On API-36 AVD `emulator-5556`, single-finger taps selected the XY plane, placed both ends of a sketch line, and finished the sketch with the segment visible. This verifies one complete 2D sketch workflow, not precision across all geometry targets or multi-touch gestures on a device.
- API-36 emulator runs verified SAF open/save, recent-file restore, and asynchronous DXF export. One later emulator attempt hit a DocumentsUI focus ANR on File → Open; this is a failed picker attempt, not a general SAF pass. Separate Android 16 physical-device tests later opened a Downloads design, saved it, force-stopped the app, and reopened the same provider file; other providers, picker cancellation, and failure recovery still need device coverage. A fresh isolated API-36.1 x86_64 Android 16 AVD crashed during startup in its translated Vulkan `vkQueueSubmit` path for both the exact GitHub arm64 APK and a fresh local arm64 rebuild; it cannot validate native ARM64 emulator behavior. Recent entries use eframe persistence and working copies use URI-stable paths in app-private storage. Host integration tests cover two-finger navigation, but all 405 commands and full physical CAD workflows are not verified.
- UI-Verifikation: Die Browserkarte startet auf schmalen Ansichten eingeklappt, lässt sich antippen, um die Objektliste zu zeigen, und ändert dabei nicht die Desktop-Einklapppräferenz. Alle 12 Browserkarten-Tests bestehen am 2026-10-29. API-36-Porträt- und Querformat-Screenshots sowie Start auf einem physischen Android-16-Gerät wurden geprüft. Der Test `browser::card_tests::narrow_floating_browser_starts_compact_and_expands_on_touch` prüft eine 48-Punkte-Phone-Zeile, die Body-Auswahl nahe der unteren Kante und die Rückkehr zu 26 Punkten bei Desktop-Breite. Das ist keine vollständige mobile Funktionsparität.

## Statusdefinition

- **Gemeinsam vorhanden, mobil ungeprüft**: Engine/UI ist im gemeinsamen Rust-Code enthalten; Android-Touch, Lifecycle und Bedienpfad wurden für diesen Befehl noch nicht einzeln verifiziert.
- **Android-Portierung fehlt**: Plattformintegration oder Android-Bedienpfad ist explizit nicht vorhanden, etwa externe Slicer-Integration.
- **Im Original geplant**: Die Desktop-Funktionsliste kennzeichnet den Befehl als geplant; er wird nicht als bestehende Originalfunktion gezählt.

## Befehlsinventur

| Bereich | Originalfunktion | ID | Originalquelle | Android-Status |
|---|---|---|---|---|
| Sketch | Center Point Arc | `sketch.arc.center_point` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Tangent Arc | `sketch.arc.tangent` | `crates/engine/src/cmd/sketch_create.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | 3-Point Arc | `sketch.arc.three_point` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | AutoConstrain | `sketch.auto_constrain` | `crates/engine/src/cmd/sketch_constraints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | AutoConstrain from datum | `sketch.auto_constrain_from_datum` | `crates/engine/src/cmd/sketch_constraints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Auto-Project Edges on Reference | `sketch.auto_project` | `crates/engine/src/cmd/sketch_project.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Blend Curve | `sketch.blend_curve` | `crates/engine/src/cmd/sketch_create.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Break | `sketch.break` | `crates/engine/src/cmd/sketch_modify.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Break Link | `sketch.break_link` | `crates/engine/src/cmd/sketch_project.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Centerline | `sketch.centerline` | `crates/engine/src/cmd/sketch_create.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Chain | `sketch.chain` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Distance and Angle Chamfer | `sketch.chamfer.distance_angle` | `crates/engine/src/cmd/sketch_modify.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Equal Distance Chamfer | `sketch.chamfer.equal_distance` | `crates/engine/src/cmd/sketch_modify.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Two Distance Chamfer | `sketch.chamfer.two_distance` | `crates/engine/src/cmd/sketch_modify.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Center Diameter Circle (C) | `sketch.circle.center` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | 3-Point Circle | `sketch.circle.three_point` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | 3-Tangent Circle | `sketch.circle.three_tangent` | `crates/engine/src/cmd/sketch_create.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | 2-Point Circle | `sketch.circle.two_point` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | 2-Tangent Circle | `sketch.circle.two_tangent` | `crates/engine/src/cmd/sketch_create.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Conic Curve | `sketch.conic` | `crates/engine/src/cmd/sketch_create.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Sketch Constrainer | `sketch.constrainer` | `crates/engine/src/cmd/sketch_constraints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Coincident | `sketch.constraint.coincident` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Collinear | `sketch.constraint.collinear` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Concentric | `sketch.constraint.concentric` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Curvature | `sketch.constraint.curvature` | `crates/engine/src/cmd/sketch_constraints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Equal | `sketch.constraint.equal` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Fix/UnFix | `sketch.constraint.fix` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Horizontal/Vertical | `sketch.constraint.horizontal_vertical` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | MidPoint | `sketch.constraint.midpoint` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Parallel | `sketch.constraint.parallel` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Perpendicular | `sketch.constraint.perpendicular` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Polygon | `sketch.constraint.polygon` | `crates/engine/src/cmd/sketch_constraints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Symmetry | `sketch.constraint.symmetry` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Tangent | `sketch.constraint.tangent` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Normal/Construction (X) | `sketch.construction` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Copy Sketch Entities | `sketch.copy` | `crates/engine/src/cmd/sketch_clipboard.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Create Sketch | `sketch.create` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Cut Sketch Entities | `sketch.cut` | `crates/engine/src/cmd/sketch_clipboard.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Delete Sketch Entities | `sketch.delete` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Sketch Dimension (D) | `sketch.dimension` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Move Dimension Text | `sketch.dimension_text` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Drag Sketch Geometry | `sketch.drag` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | End Sketch Drag | `sketch.drag_end` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Draw Dimensioned Sketch Geometry | `sketch.draw.dimensioned` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Edit Sketch | `sketch.edit` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Edit Text | `sketch.edit_text` | `crates/engine/src/cmd/sketch_create.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Ellipse | `sketch.ellipse` | `crates/engine/src/cmd/sketch_create.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Save As DXF | `sketch.export_dxf` | `crates/engine/src/cmd/sketch_import.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Extend | `sketch.extend` | `crates/engine/src/cmd/sketch_modify.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Fillet | `sketch.fillet` | `crates/engine/src/cmd/sketch_modify.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Finish Sketch | `sketch.finish` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Finish with AutoConstrain | `sketch.finish_auto_constrain` | `crates/engine/src/cmd/sketch_constraints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Fit Curves to Mesh Section | `sketch.fit_curves_to_section` | `crates/engine/src/cmd/sketch_project.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Constraint Glyphs | `sketch.glyphs` | `crates/engine/src/cmd/sketch_constraints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Include 3D Geometry | `sketch.include_3d` | `crates/engine/src/cmd/sketch_project.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Infinite Line | `sketch.infinite` | `crates/engine/src/cmd/sketch_create.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Insert DXF | `sketch.insert_dxf` | `crates/engine/src/cmd/sketch_import.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Insert SVG | `sketch.insert_svg` | `crates/engine/src/cmd/sketch_import.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Inspect Sketch | `sketch.inspect` | `crates/engine/src/cmd/inspect.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Intersect | `sketch.intersect` | `crates/engine/src/cmd/sketch_project.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Intersection Curve | `sketch.intersection_curve` | `crates/engine/src/cmd/sketch_project.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Isoparametric Curve | `sketch.isoparametric_curve` | `crates/engine/src/cmd/sketch_project.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Line (L) | `sketch.line` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Midpoint Line | `sketch.line.midpoint` | `crates/engine/src/cmd/sketch_create.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | 3D Line | `sketch.line3d` | `crates/engine/src/cmd/sketch_3d.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Loose Ends | `sketch.loose_ends` | `crates/engine/src/cmd/sketch_ends.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Tie Loose Ends | `sketch.loose_ends.tie` | `crates/engine/src/cmd/sketch_ends.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Mirror | `sketch.mirror` | `crates/engine/src/cmd/sketch_modify.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Move/Copy Sketch Entities | `sketch.move` | `crates/engine/src/cmd/sketch_modify.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Move 3D Point | `sketch.move3d` | `crates/engine/src/cmd/sketch_3d.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Drag Sketch Point | `sketch.move_point` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Offset (O) | `sketch.offset` | `crates/engine/src/cmd/sketch_modify.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Break Offset Association | `sketch.offset.break` | `crates/engine/src/cmd/sketch_modify.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Sketch Palette Options | `sketch.options` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Paste Sketch Entities | `sketch.paste` | `crates/engine/src/cmd/sketch_clipboard.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Circular Pattern | `sketch.pattern.circular` | `crates/engine/src/cmd/sketch_modify.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Rectangular Pattern | `sketch.pattern.rectangular` | `crates/engine/src/cmd/sketch_modify.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Point | `sketch.point` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | 3D Point | `sketch.point3d` | `crates/engine/src/cmd/sketch_3d.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Circumscribed Polygon | `sketch.polygon.circumscribed` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Edge Polygon | `sketch.polygon.edge` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Inscribed Polygon | `sketch.polygon.inscribed` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Project (P) | `sketch.project` | `crates/engine/src/cmd/sketch_project.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Project To Surface | `sketch.project_to_surface` | `crates/engine/src/cmd/sketch_project.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Center Rectangle | `sketch.rectangle.center` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Rounded Rectangle | `sketch.rectangle.rounded` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | 3-Point Rectangle | `sketch.rectangle.three_point` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | 2-Point Rectangle (R) | `sketch.rectangle.two_point` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Redefine Sketch Plane | `sketch.redefine` | `crates/engine/src/cmd/browser.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Sketch Scale | `sketch.scale` | `crates/engine/src/cmd/sketch_modify.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Center Point Arc Slot | `sketch.slot.arc_center` | `crates/engine/src/cmd/sketch_create.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Three Point Arc Slot | `sketch.slot.arc_three_point` | `crates/engine/src/cmd/sketch_create.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Center Point Slot | `sketch.slot.center_point` | `crates/engine/src/cmd/sketch_create.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Center to Center Slot | `sketch.slot.center_to_center` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Overall Slot | `sketch.slot.overall` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Snap Inference | `sketch.snap` | `crates/engine/src/cmd/sketch_constraints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Solve Sketch | `sketch.solve` | `crates/engine/src/cmd/sketch.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Control Point Spline | `sketch.spline.control_point` | `crates/engine/src/cmd/sketch_create.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Control Point Spline (Degree 5) | `sketch.spline.control_point_5` | `crates/engine/src/cmd/sketch_create.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Fit Point Spline | `sketch.spline.fit_point` | `crates/engine/src/cmd/sketch_create.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | 3D Spline | `sketch.spline3d` | `crates/engine/src/cmd/sketch_3d.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Spun Profile | `sketch.spun_profile` | `crates/engine/src/cmd/sketch_project.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Text | `sketch.text` | `crates/engine/src/cmd/sketch_create.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Toggle Driven Dimension | `sketch.toggle_driven` | `crates/engine/src/cmd/sketch_constraints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Trim (T) | `sketch.trim` | `crates/engine/src/cmd/sketch_modify.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sketch | Trim Along Path | `sketch.trim.path` | `crates/engine/src/cmd/sketch_modify.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Align | `solid.align` | `crates/engine/src/cmd/features_more.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Boundary Fill | `solid.boundary_fill` | `crates/engine/src/cmd/features_more.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Bounding Solid | `solid.bounding_solid` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Box | `solid.box` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Chamfer | `solid.chamfer` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Coil | `solid.coil` | `crates/engine/src/cmd/features_more.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Combine | `solid.combine` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Cylinder | `solid.cylinder` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Draft | `solid.draft` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Emboss | `solid.emboss` | `crates/engine/src/cmd/features_more.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Extrude (E) | `solid.extrude` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Fillet (F) | `solid.fillet` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Hole (H) | `solid.hole` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Loft | `solid.loft` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Mirror | `solid.mirror` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Move/Copy (M) | `solid.move` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Offset Face | `solid.offset_face` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Circular Pattern | `solid.pattern.circular` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Pattern on Path | `solid.pattern.path` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Rectangular Pattern | `solid.pattern.rectangular` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Pipe | `solid.pipe` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Press Pull (Q) | `solid.press_pull` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Remove | `solid.remove` | `crates/engine/src/cmd/features_more.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Replace Face | `solid.replace_face` | `crates/engine/src/cmd/features_more.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Revolve | `solid.revolve` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Rib | `solid.rib` | `crates/engine/src/cmd/features_more.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Scale | `solid.scale` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Shell | `solid.shell` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Sphere | `solid.sphere` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Split Body | `solid.split_body` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Split Face | `solid.split_face` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Sweep | `solid.sweep` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Thread | `solid.thread` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Torus | `solid.torus` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Solid modelling | Web | `solid.web` | `crates/engine/src/cmd/features_more.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Surfaces | Blend Surface | `surface.blend` | `crates/engine/src/cmd/surface.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Surfaces | Boundary Surface | `surface.boundary` | `crates/engine/src/cmd/surface_boundary.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Surfaces | Extend | `surface.extend` | `crates/engine/src/cmd/surface.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Surfaces | Surface Extrude | `surface.extrude` | `crates/engine/src/cmd/surface.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Surfaces | Fair Surface | `surface.fair` | `crates/engine/src/cmd/surface.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Surfaces | Surface Fillet)* | `surface.fillet` | `crates/engine/src/cmd/surface.rs` (planned registry entry) | Im Original geplant |
| Surfaces | Surface Loft | `surface.loft` | `crates/engine/src/cmd/surface.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Surfaces | Match Surface | `surface.match` | `crates/engine/src/cmd/surface_match.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Surfaces | Offset Surface | `surface.offset` | `crates/engine/src/cmd/surface.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Surfaces | Patch | `surface.patch` | `crates/engine/src/cmd/surface.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Surfaces | Reverse Normal | `surface.reverse_normal` | `crates/engine/src/cmd/surface.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Surfaces | Surface Revolve | `surface.revolve` | `crates/engine/src/cmd/surface.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Surfaces | Ruled Surface | `surface.ruled` | `crates/engine/src/cmd/surface.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Surfaces | Stitch | `surface.stitch` | `crates/engine/src/cmd/surface.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Surfaces | Surface Sweep | `surface.sweep` | `crates/engine/src/cmd/surface.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Surfaces | Thicken | `surface.thicken` | `crates/engine/src/cmd/surface.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Surfaces | Trim | `surface.trim` | `crates/engine/src/cmd/surface.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Surfaces | Unstitch | `surface.unstitch` | `crates/engine/src/cmd/surface.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Surfaces | Untrim | `surface.untrim` | `crates/engine/src/cmd/surface.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Construction geometry | Axis Through Edge | `construct.axis.edge` | `crates/engine/src/cmd/construct.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Construction geometry | Axis Perpendicular To Face | `construct.axis.normal_to_face` | `crates/engine/src/cmd/construct.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Construction geometry | Axis Through Cylinder/Cone/Torus | `construct.axis.through_cylinder` | `crates/engine/src/cmd/construct.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Construction geometry | Axis Through Two Planes | `construct.axis.two_planes` | `crates/engine/src/cmd/construct.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Construction geometry | Axis Through Two Points | `construct.axis.two_points` | `crates/engine/src/cmd/construct.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Construction geometry | Plane Along Path | `construct.plane.along_path` | `crates/engine/src/cmd/construct.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Construction geometry | Plane at Angle | `construct.plane.at_angle` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Construction geometry | Midplane | `construct.plane.midplane` | `crates/engine/src/cmd/construct.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Construction geometry | Offset Plane | `construct.plane.offset` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Construction geometry | Perpendicular Plane | `construct.plane.perpendicular` | `crates/engine/src/cmd/construct.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Construction geometry | Tangent Plane | `construct.plane.tangent` | `crates/engine/src/cmd/construct.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Construction geometry | Plane Through Three Points | `construct.plane.three_points` | `crates/engine/src/cmd/construct.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Construction geometry | Plane Through Two Edges | `construct.plane.two_edges` | `crates/engine/src/cmd/construct.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Construction geometry | Point Along Path | `construct.point.along_path` | `crates/engine/src/cmd/construct.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Construction geometry | Point At Center Of Circle/Sphere/Torus | `construct.point.center` | `crates/engine/src/cmd/construct.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Construction geometry | Point At Edge And Plane | `construct.point.edge_plane` | `crates/engine/src/cmd/construct.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Construction geometry | Point Through Three Planes | `construct.point.three_planes` | `crates/engine/src/cmd/construct.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Construction geometry | Point Through Two Edges | `construct.point.two_edges` | `crates/engine/src/cmd/construct.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Construction geometry | Point At Vertex | `construct.point.vertex` | `crates/engine/src/cmd/construct.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sheet metal | Convert to Sheet Metal | `sheet.convert` | `crates/engine/src/cmd/sheet.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sheet metal | Export Bend Table | `sheet.export_bend_table` | `crates/engine/src/cmd/sheet.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sheet metal | Export Flat Pattern as DXF | `sheet.export_dxf` | `crates/engine/src/cmd/sheet.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sheet metal | Flange | `sheet.flange` | `crates/engine/src/cmd/sheet.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sheet metal | Create Flat Pattern | `sheet.flat_pattern` | `crates/engine/src/cmd/sheet.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sheet metal | Fold | `sheet.fold` | `crates/engine/src/cmd/sheet.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sheet metal | Hem | `sheet.hem` | `crates/engine/src/cmd/sheet.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sheet metal | Sheet Metal Rules | `sheet.manage_rules` | `crates/engine/src/cmd/sheet.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sheet metal | Perforate | `sheet.perforate` | `crates/engine/src/cmd/sheet.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sheet metal | Refold | `sheet.refold` | `crates/engine/src/cmd/sheet.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sheet metal | List Sheet Metal Rules | `sheet.rules` | `crates/engine/src/cmd/sheet.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Sheet metal | Unfold | `sheet.unfold` | `crates/engine/src/cmd/sheet.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Plastic parts | Assign Plastic Rule | `plastic.assign_rule` | `crates/engine/src/cmd/plastic.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Plastic parts | Boss | `plastic.boss` | `crates/engine/src/cmd/plastic.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Plastic parts | Lip | `plastic.lip` | `crates/engine/src/cmd/plastic.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Plastic parts | Manage Plastic Rules | `plastic.manage_rules` | `crates/engine/src/cmd/plastic.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Plastic parts | Rest | `plastic.rest` | `crates/engine/src/cmd/plastic.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Plastic parts | List Plastic Rules | `plastic.rules` | `crates/engine/src/cmd/plastic.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Plastic parts | Snap Fit | `plastic.snap_fit` | `crates/engine/src/cmd/plastic.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Components | Activate Component | `component.activate` | `crates/engine/src/cmd/component.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Components | Break Link | `component.break_link` | `crates/engine/src/cmd/component_link.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Components | Capture Position | `component.capture_position` | `crates/engine/src/cmd/component.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Components | New Component | `component.create` | `crates/engine/src/cmd/component.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Components | Delete Component | `component.delete` | `crates/engine/src/cmd/component.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Components | Create Components from Bodies | `component.from_bodies` | `crates/engine/src/cmd/component.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Components | Insert Design | `component.insert_design` | `crates/engine/src/cmd/component_link.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Components | List Links | `component.links` | `crates/engine/src/cmd/component_link.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Components | List Components | `component.list` | `crates/engine/src/cmd/component.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Components | Part Properties | `component.metadata` | `crates/engine/src/cmd/bom.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Components | Change Part Properties | `component.metadata.set` | `crates/engine/src/cmd/bom.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Components | Move Bodies to Component | `component.move_bodies` | `crates/engine/src/cmd/component.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Components | Move Sketches to Component | `component.move_sketches` | `crates/engine/src/cmd/component.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Components | Paste New | `component.paste_new` | `crates/engine/src/cmd/component.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Components | Component Physical Properties | `component.properties` | `crates/engine/src/cmd/component.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Components | Relink Source | `component.relink` | `crates/engine/src/cmd/component_link.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Components | Rename Component | `component.rename` | `crates/engine/src/cmd/component.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Components | Revert Position | `component.revert_position` | `crates/engine/src/cmd/component.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Components | Unlock Contents | `component.unlock` | `crates/engine/src/cmd/component.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Components | Update Links | `component.update_links` | `crates/engine/src/cmd/component_link.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Assembly joints | As-Built Joint | `joint.as_built` | `crates/engine/src/cmd/joints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Assembly joints | Cam | `joint.cam` | `crates/engine/src/cmd/joints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Assembly joints | Joint (J) | `joint.create` | `crates/engine/src/cmd/joints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Assembly joints | Delete Joint | `joint.delete` | `crates/engine/src/cmd/joints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Assembly joints | Drive Joints | `joint.drive` | `crates/engine/src/cmd/joints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Assembly joints | Edit Joint | `joint.edit` | `crates/engine/src/cmd/joints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Assembly joints | Gear | `joint.gear` | `crates/engine/src/cmd/joints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Assembly joints | Inspect Joint | `joint.inspect` | `crates/engine/src/cmd/joints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Assembly joints | Joint Limits | `joint.limits` | `crates/engine/src/cmd/joints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Assembly joints | List Joints | `joint.list` | `crates/engine/src/cmd/joints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Assembly joints | Motion Link | `joint.motion_link` | `crates/engine/src/cmd/joints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Assembly joints | Joint Origin | `joint.origin` | `crates/engine/src/cmd/joints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Assembly joints | Rack and Pinion | `joint.rack_pinion` | `crates/engine/src/cmd/joints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Assembly joints | Reach Point | `joint.reach` | `crates/engine/src/cmd/joints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Assembly joints | Rigid Group | `joint.rigid_group` | `crates/engine/src/cmd/joints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Assembly joints | Screw | `joint.screw` | `crates/engine/src/cmd/joints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Assembly joints | Solve Joints | `joint.solve` | `crates/engine/src/cmd/joints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Assembly joints | Sweep Joint | `joint.sweep` | `crates/engine/src/cmd/motion.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Configurations | Activate Configuration | `config.activate` | `crates/engine/src/cmd/config.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Configurations | Configuration Column | `config.column` | `crates/engine/src/cmd/config.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Configurations | Configure | `config.configure` | `crates/engine/src/cmd/config.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Configurations | Preview Configuration | `config.preview` | `crates/engine/src/cmd/config.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Configurations | Configuration Row | `config.row` | `crates/engine/src/cmd/config.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Configurations | Configuration Rule | `config.rule` | `crates/engine/src/cmd/config.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Configurations | Configuration Rules | `config.rules` | `crates/engine/src/cmd/config.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Configurations | Display Configuration Table | `config.table` | `crates/engine/src/cmd/config.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Timeline | Add Recorded Feature | `timeline.add` | `crates/engine/src/cmd/script.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Timeline | Compute All | `timeline.compute_all` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Timeline | Copy Features | `timeline.copy` | `crates/engine/src/cmd/clipboard.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Timeline | Delete (Delete) | `timeline.delete` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Timeline | Feature Dependents | `timeline.dependents` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Timeline | Edit Feature | `timeline.edit` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Timeline | Export Command Script | `timeline.export_script` | `crates/engine/src/cmd/script.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Timeline | New Feature Folder | `timeline.group` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Timeline | Inspect Feature | `timeline.inspect` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Timeline | Move Timeline Items | `timeline.move_many` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Timeline | Move Features to Folder | `timeline.move_to_group` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Timeline | Paste Features | `timeline.paste` | `crates/engine/src/cmd/clipboard.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Timeline | Edit Feature | `timeline.redefine` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Timeline | Rename | `timeline.rename` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Timeline | Rename Feature Folder | `timeline.rename_group` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Timeline | Reorder Feature | `timeline.reorder` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Timeline | Reorder Features | `timeline.reorder_many` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Timeline | Roll History Marker | `timeline.roll_to` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Timeline | Move Timeline Marker | `timeline.rollback` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Timeline | Suppress Feature | `timeline.suppress` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Timeline | Remove Feature Folders | `timeline.ungroup` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Inspect | Accessibility Analysis | `inspect.accessibility` | `crates/engine/src/cmd/surface_analysis.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Inspect | Center of Mass | `inspect.center_of_mass` | `crates/engine/src/cmd/sketch_inspect.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Inspect | Clearance | `inspect.clearance` | `crates/engine/src/cmd/clearance.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Inspect | Compare Bodies | `inspect.compare` | `crates/engine/src/cmd/compare.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Inspect | Continuity Analysis | `inspect.continuity` | `crates/engine/src/cmd/surface_analysis.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Inspect | Curvature Comb Analysis | `inspect.curvature_comb` | `crates/engine/src/cmd/sketch_inspect.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Inspect | Curvature Map Analysis | `inspect.curvature_map` | `crates/engine/src/cmd/surface_analysis.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Inspect | Draft Analysis | `inspect.draft` | `crates/engine/src/cmd/surface_analysis.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Inspect | Environment Map Analysis | `inspect.environment_map` | `crates/engine/src/cmd/surface_analysis.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Inspect | Interference | `inspect.interference` | `crates/engine/src/cmd/joints.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Inspect | Isocurve Analysis | `inspect.isocurve` | `crates/engine/src/cmd/sketch_inspect.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Inspect | Measure (I) | `inspect.measure` | `crates/engine/src/cmd/inspect.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Inspect | Minimum Radius Analysis | `inspect.minimum_radius` | `crates/engine/src/cmd/sketch_inspect.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Inspect | Section Analysis | `inspect.section` | `crates/engine/src/cmd/section.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Inspect | Silhouette | `inspect.silhouette` | `crates/engine/src/cmd/silhouette.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Inspect | Zebra Analysis | `inspect.zebra` | `crates/engine/src/cmd/surface_analysis.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Files | Export | `file.export` | `crates/engine/src/cmd/file.rs` | SAF-Brücke implementiert, Exportpfad auf Android ungeprüft |
| Files | Export Current View as DXF | `file.export_view_dxf` | `crates/engine/src/cmd/view_dxf.rs` | API-36-Emulator: DXF asynchron erzeugt und über SAF zurückgeschrieben; Datei 19.265 Byte, Header AC1015 verifiziert |
| Files | Insert Mesh | `file.insert_mesh` | `crates/engine/src/cmd/file.rs` | SAF-Brücke implementiert, Importpfad auf Android ungeprüft |
| Files | Insert STEP | `file.insert_step` | `crates/engine/src/cmd/file.rs` | SAF-Brücke implementiert, Importpfad auf Android ungeprüft |
| Files | New Design (Ctrl+N) | `file.new` | `crates/engine/src/cmd/file.rs` | Gemeinsam vorhanden, mobil ungeprüft; Android-Dateizugriff fehlt |
| Files | Save (Ctrl+S) | `file.save` | `crates/engine/src/cmd/file.rs` | Physisches Android 16: SAF-Datei überschrieben, nach Prozessneustart wieder geöffnet; Save-As und Providerfehler separat prüfen |
| Files | Save As | `file.save_as` | `crates/engine/src/cmd/file.rs` | API-36-Emulator: SAF-Datei geschrieben (8.919 Byte) und nach Prozessneustart erneut geöffnet |
| Files | Save As Mesh | `file.save_mesh` | `crates/engine/src/cmd/file.rs` | SAF-Brücke implementiert, Exportpfad auf Android ungeprüft |
| Files | Installed Slicers | `file.slicers` | `crates/engine/src/cmd/file.rs` | Gemeinsam vorhanden, mobil ungeprüft; Android-Dateizugriff fehlt |
| Documents | Open (Ctrl+O) | `crates/engine/src/cmd/file.rs` | API-36-Emulator und Android 16: SolidCraft-Datei aus Downloads geöffnet und nach Prozessneustart erneut geöffnet; SAF-Einlesen tauscht die Arbeitskopie nun erst nach vollständig erfolgreichem Kopieren atomar aus |
| Documents | Recover Design | `doc.recover` | `crates/engine/src/cmd/file.rs` | Gemeinsam vorhanden, mobil ungeprüft; Android-Dateizugriff fehlt |
| Documents | Discard Recovered Design | `doc.recovery_discard` | `crates/engine/src/cmd/file.rs` | Gemeinsam vorhanden, mobil ungeprüft; Android-Dateizugriff fehlt |
| Documents | Recoverable Designs | `doc.recovery_list` | `crates/engine/src/cmd/file.rs` | Gemeinsam vorhanden, mobil ungeprüft; Android-Dateizugriff fehlt |
| Edit | Checkpoint | `edit.checkpoint` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Edit | Redo (Ctrl+Y) | `edit.redo` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Edit | Restore Checkpoint | `edit.restore_checkpoint` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| Edit | Undo (Ctrl+Z) | `edit.undo` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `appearance.*` | Appearance (A) | `appearance.assign` | `crates/engine/src/cmd/inspect.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `appearance.*` | Appearance Library | `appearance.library` | `crates/engine/src/cmd/inspect.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `appearance.*` | List Appearances | `appearance.list` | `crates/engine/src/cmd/inspect.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `assembly.*` | Bill of Materials | `assembly.bom` | `crates/engine/src/cmd/bom.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `body.*` | Rename Body | `body.rename` | `crates/engine/src/cmd/browser.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `browser.*` | New Group | `browser.group` | `crates/engine/src/cmd/browser.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `browser.*` | Move Browser Items | `browser.move` | `crates/engine/src/cmd/browser.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `browser.*` | Order Browser Items | `browser.order` | `crates/engine/src/cmd/browser.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `browser.*` | Rename Group | `browser.rename_group` | `crates/engine/src/cmd/browser.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `browser.*` | Ungroup | `browser.ungroup` | `crates/engine/src/cmd/browser.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `browser.*` | Show/Hide | `browser.visibility` | `crates/engine/src/cmd/browser.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `canvas.*` | Calibrate Canvas | `canvas.calibrate` | `crates/engine/src/cmd/sketch_canvas.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `canvas.*` | Decal | `canvas.decal` | `crates/engine/src/cmd/sketch_canvas.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `canvas.*` | Delete Canvas | `canvas.delete` | `crates/engine/src/cmd/sketch_canvas.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `canvas.*` | Edit Canvas | `canvas.edit` | `crates/engine/src/cmd/sketch_canvas.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `canvas.*` | Canvas | `canvas.insert` | `crates/engine/src/cmd/sketch_canvas.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `contact.*` | Check Contact | `contact.check` | `crates/engine/src/cmd/motion.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `contact.*` | New Contact Set | `contact.create` | `crates/engine/src/cmd/motion.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `contact.*` | Disable Contact | `contact.disable_all` | `crates/engine/src/cmd/motion.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `contact.*` | Edit Contact Set | `contact.edit` | `crates/engine/src/cmd/motion.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `contact.*` | Enable All Contact | `contact.enable_all` | `crates/engine/src/cmd/motion.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `contact.*` | Enable Contact Sets | `contact.enable_sets` | `crates/engine/src/cmd/motion.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `contact.*` | List Contact Sets | `contact.list` | `crates/engine/src/cmd/motion.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `diagnostics.*` | Crash Report | `diagnostics.crash_report` | `crates/engine/src/cmd/diagnostics.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `diagnostics.*` | Crash Reports | `diagnostics.crash_reports` | `crates/engine/src/cmd/diagnostics.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `diagnostics.*` | Delete Crash Reports | `diagnostics.crash_reports.clear` | `crates/engine/src/cmd/diagnostics.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `diagnostics.*` | Crash Test | `diagnostics.crash_test` | `crates/engine/src/cmd/diagnostics.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `document.*` | Inspect Design | `document.inspect` | `crates/engine/src/cmd/inspect.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `document.*` | Design Properties | `document.metadata` | `crates/engine/src/cmd/metadata.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `document.*` | Remove Design Property | `document.metadata.remove` | `crates/engine/src/cmd/metadata.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `document.*` | Change Design Properties | `document.metadata.set` | `crates/engine/src/cmd/metadata.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `document.*` | Change Design Units | `document.units` | `crates/engine/src/cmd/browser.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `engine.*` | List Commands | `engine.commands` | `crates/engine/src/cmd/inspect.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `engine.*` | Describe Command | `engine.describe` | `crates/engine/src/cmd/inspect.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `explode.*` | Exploded View | `explode.create` | `crates/engine/src/cmd/motion.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `explode.*` | Delete Exploded View | `explode.delete` | `crates/engine/src/cmd/motion.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `explode.*` | List Exploded Views | `explode.list` | `crates/engine/src/cmd/motion.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `explode.*` | Show Exploded View | `explode.show` | `crates/engine/src/cmd/motion.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `expr.*` | Evaluate Expression | `expr.evaluate` | `crates/engine/src/cmd/parameters.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `face.*` | Delete Face | `face.delete` | `crates/engine/src/cmd/features.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `library.*` | Apply Local Library Entry | `library.apply` | `crates/engine/src/cmd/libraries.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `library.*` | Export Design Library | `library.export` | `crates/engine/src/cmd/libraries.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `library.*` | Inspect Local Library | `library.inspect` | `crates/engine/src/cmd/libraries.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `material.*` | Physical Material | `material.assign` | `crates/engine/src/cmd/inspect.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `material.*` | New Physical Material | `material.create` | `crates/engine/src/cmd/materials.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `material.*` | Delete Physical Material | `material.delete` | `crates/engine/src/cmd/materials.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `material.*` | Edit Physical Material | `material.edit` | `crates/engine/src/cmd/materials.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `material.*` | List Materials | `material.list` | `crates/engine/src/cmd/inspect.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `material.*` | Directional Elastic Modulus | `material.properties.directional` | `crates/engine/src/cmd/material_properties.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `material.*` | Validate Physical Material Data | `material.properties.validate` | `crates/engine/src/cmd/material_properties.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `mesh.*` | Convert Mesh | `mesh.convert` | `crates/engine/src/cmd/mesh.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `model.*` | List Edges | `model.edges` | `crates/engine/src/cmd/inspect.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `model.*` | List Faces | `model.faces` | `crates/engine/src/cmd/inspect.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `model.*` | List Threads | `model.threads` | `crates/engine/src/cmd/inspect.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `motion.*` | Delete Motion Study | `motion.delete` | `crates/engine/src/cmd/motion.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `motion.*` | Export Motion Study | `motion.export` | `crates/engine/src/cmd/motion.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `motion.*` | Export Motion Animation | `motion.export_animation` | `crates/engine/src/cmd/motion.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `motion.*` | Motion Interference | `motion.interference` | `crates/engine/src/cmd/motion.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `motion.*` | List Motion Studies | `motion.list` | `crates/engine/src/cmd/motion.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `motion.*` | Play Motion Study | `motion.play` | `crates/engine/src/cmd/motion.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `motion.*` | Plot Motion Study | `motion.plot` | `crates/engine/src/cmd/motion.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `motion.*` | Motion Study | `motion.study` | `crates/engine/src/cmd/motion.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `motion.*` | Motion Trace | `motion.trace` | `crates/engine/src/cmd/motion.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `motion.*` | Keep Motion Trace | `motion.trace.keep` | `crates/engine/src/cmd/motion.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `occurrence.*` | Paste (Instance) | `occurrence.copy` | `crates/engine/src/cmd/component.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `occurrence.*` | Ground | `occurrence.ground` | `crates/engine/src/cmd/component.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `occurrence.*` | Move Occurrence | `occurrence.move` | `crates/engine/src/cmd/component.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `occurrence.*` | Occurrence Physical Properties | `occurrence.properties` | `crates/engine/src/cmd/component.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `parameters.*` | Add User Parameter | `parameters.add` | `crates/engine/src/cmd/parameters.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `parameters.*` | Change Parameters | `parameters.change` | `crates/engine/src/cmd/parameters.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `parameters.*` | Comment Parameter | `parameters.comment` | `crates/engine/src/cmd/parameters.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `parameters.*` | Complete Expression | `parameters.complete` | `crates/engine/src/cmd/parameters.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `parameters.*` | Delete Parameter | `parameters.delete` | `crates/engine/src/cmd/parameters.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `parameters.*` | Export Parameters | `parameters.export` | `crates/engine/src/cmd/parameters.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `parameters.*` | Favourite Parameter | `parameters.favorite` | `crates/engine/src/cmd/parameters.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `parameters.*` | Parameter Graph | `parameters.graph` | `crates/engine/src/cmd/parameters.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `parameters.*` | Import Parameters | `parameters.import` | `crates/engine/src/cmd/parameters.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `parameters.*` | List Parameters | `parameters.list` | `crates/engine/src/cmd/parameters.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `parameters.*` | Move Parameter | `parameters.move` | `crates/engine/src/cmd/parameters.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `parameters.*` | Rename Parameter | `parameters.rename` | `crates/engine/src/cmd/parameters.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `parameters.*` | Parameter Users | `parameters.users` | `crates/engine/src/cmd/parameters.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `parts.*` | Insert Fastener | `parts.fastener` | `crates/engine/src/cmd/parts.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `parts.*` | Insert Part | `parts.insert` | `crates/engine/src/cmd/parts.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `parts.*` | Standard Parts | `parts.library` | `crates/engine/src/cmd/parts.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `parts.*` | Change Part Size | `parts.resize` | `crates/engine/src/cmd/parts.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `render.*` | Render Backends | `render.backends` | `crates/engine/src/cmd/render.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `render.*` | Cancel Render | `render.cancel` | `crates/engine/src/cmd/render.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `render.*` | Export Render Image | `render.export` | `crates/engine/src/cmd/render.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `render.*` | Check Render Request | `render.preflight` | `crates/engine/src/cmd/render.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `render.*` | Inspect Render Scene | `render.scene` | `crates/engine/src/cmd/render.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `render.*` | Apply Render Camera | `render.settings.apply` | `crates/engine/src/cmd/render.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `render.*` | Render Settings | `render.settings.get` | `crates/engine/src/cmd/render.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `render.*` | Save Render Settings | `render.settings.set` | `crates/engine/src/cmd/render.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `render.*` | Render Image | `render.start` | `crates/engine/src/cmd/render.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `render.*` | Render Progress | `render.status` | `crates/engine/src/cmd/render.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `select.*` | Clear Selection | `select.clear` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `select.*` | Select | `select.set` | `crates/engine/src/cmd/edit.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `selection.*` | Delete | `selection.delete` | `crates/engine/src/cmd/delete.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `view.*` | Ambient Occlusion | `view.ambient_occlusion` | `crates/engine/src/cmd/view_navigation.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `view.*` | Restore Named View | `view.apply` | `crates/engine/src/cmd/browser.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `view.*` | Delete Named View | `view.delete` | `crates/engine/src/cmd/browser.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `view.*` | Graphics Preset | `view.graphics_preset` | `crates/engine/src/cmd/view_navigation.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `view.*` | History layout | `view.history_layout` | `crates/engine/src/cmd/view_navigation.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `view.*` | Workspace layout | `view.layout` | `crates/engine/src/cmd/view_navigation.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `view.*` | List Named Views | `view.list` | `crates/engine/src/cmd/browser.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `view.*` | Navigate View | `view.navigate` | `crates/engine/src/cmd/view_navigation.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `view.*` | Normal to Selection | `view.normal_to` | `crates/engine/src/cmd/view_navigation.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `view.*` | Rename Named View | `view.rename` | `crates/engine/src/cmd/browser.rs` | Gemeinsam vorhanden, mobil ungeprüft |
| `view.*` | New Named View | `view.save` | `crates/engine/src/cmd/browser.rs` | Gemeinsam vorhanden, mobil ungeprüft |

## Plattformfunktionen außerhalb des Befehlsregisters

| Funktion | Originalimplementierung | Android-Stand | Nachweis / nächster Schritt |
|---|---|---|---|
| Engine, B-rep, Skizzenlöser, parametrische Historie | `crates/engine`, `crates/kernel`, `crates/sketch`, `crates/geom` | Gemeinsam eingebunden; Rust-Code läuft als Android `cdylib` | Host-Unit-Tests und Android-APK-Build vorhanden; featureweise Android-Engine-Integrationsabdeckung fehlt.
| Hauptoberfläche und Befehlsdialoge | `crates/ui-egui` | Gemeinsame egui-Oberfläche; Ribbon und Browser sind für schmale Breiten angepasst | Kompakte Darstellung hat Host- und API-36-Viewport-Prüfung; alle Dialoge/Bedienwege auf Telefon und Tablet durchtesten.
| 3D-Navigation und Selektion | `crates/ui-egui/src/navigation.rs`, `crates/ui-egui/src/viewport.rs` | Mehrfinger-Pan, Pinch-Zoom, Orbit, Touch-Auswahl mit vergrößertem Trefferbereich und Langdruck-Menü implementiert | Host-Integrationstests und vollständige egui-Suite bestanden; Android-Gerätegenauigkeit für Pan, Pinch, Orbit und kleine CAD-Elemente fehlt.
| Datei öffnen, speichern und exportieren | `crates/ui-egui/src/file_picker.rs`, `apps/solidcraft-android/src/storage.rs`, `app/src/main/java/org/solidcraft/mobile/MainActivity.kt` | SAF öffnet Dateien in dauerhaftem app-internem Arbeitsbereich; Save/Save As schreibt über URI zurück; asynchroner DXF-Export nutzt denselben Pfad | API-36-Downloads-Workflow für Öffnen, Speichern, Wiederöffnen nach Prozessneustart und DXF-Export bestanden; andere Provider und Fehler-/Abbruchfälle testen.
| Import externer glTF-Puffer | `app/src/main/java/org/solidcraft/mobile/GltfExternalResources.kt`, `app/src/main/java/org/solidcraft/mobile/ExternalResourceStaging.kt`, `crates/io/src/gltf_in.rs` | glTF/GLB-Pufferpfade werden validiert; die App fordert bei externen Ressourcen einen Ordnerzugriff an und kopiert nur referenzierte Dateien unter einem Gesamtgrößenlimit in den Arbeitsordner. Eingebettete `data:`-URIs folgen der Rust-Importer-Semantik, defekte GLB-Dateien mit abgeschnittenem Header oder ohne JSON-Chunk werden zurückgewiesen | Kotlin-Unit-Tests decken Pfadsicherheit, GLB little-endian Chunk sowie abgeschnittenen Header und fehlenden JSON-Chunk, URI-Schemagehäuse, verschachtelte Ordnersuche, fehlende Ressourcen, Gesamtgrößenlimit und manipulierte Providernamen ab. API-36-Gerätetest öffnete ein externes glTF, erteilte SAF-Zugriff auf den Projektordner, prüfte den 42-Byte-Puffer im privaten Stage und importierte einen Body; Android sperrt die Freigabe des Downloads-Stammordners.
| Autosave/Wiederherstellung | `apps/solidcraft-android/src/lib.rs`, `crates/ui-egui/src/recovery_ui.rs`, `crates/engine/src/recovery.rs` | Recovery-Service läuft mit app-privatem Ordner und SolidCraft-Autosaveintervall | Gemeinsame Recovery-Tests und Verzeichnisstart auf Emulator/Gerät; Wiederherstellung nach erzwungenem Android-Prozessabbruch fehlt.
| Einstellungen und zuletzt verwendete Projekte | `crates/ui-egui`, `apps/solidcraft-android/src/lib.rs` | eframe speichert Einstellungen und Recent-Liste im app-internen Speicher | API-36-Emulator hat Recent-Datei nach Force-Stop wieder geöffnet; Rotation und Lifecycle-Neustart zusätzlich testen.
| Tastatur/Maus/Stift | `crates/ui-egui/src/navigation.rs`, Input-Werkzeuge, GameActivity | Android-Texteingabe und gemeinsame egui-Ereignispfade vorhanden | Externe Tastatur, Maus, Stylus, Zurück-Navigation und Konflikte mit Touch-Gesten separat auf Geräten prüfen.
| Rendering und Lifecycle | `apps/solidcraft-android/src/lib.rs`, eframe/wgpu | Start und Viewport auf API-36-Emulator und Android-16-Gerät nachgewiesen | Rotation, Pause/Resume, Surface-Neuerstellung, GPU-Kontextverlust und Speicherknappheit testen.

## Erfassungsmethode

Die Befehlszeilen stammen aus der generierten Originalübersicht `docs/features.md`; Implementierungsdateien werden über `CommandSpec::new` in `crates/engine/src/cmd/` ermittelt. UI-Pfad und Android-Verhalten benötigen zusätzliche Ende-zu-Ende-Tests und werden daher nicht aus dem bloßen Vorhandensein des Rust-Befehls abgeleitet.

