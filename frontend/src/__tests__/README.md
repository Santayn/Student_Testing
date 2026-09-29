# Frontend test structure

Tests are grouped by the product/domain they protect, not by implementation type.

- `admin/` — administrator workspaces and their composables/components.
- `teacher/` — teacher workflows (`lectures`, `questions`, `tests`, `topics`, `workload`, etc.).
- `student/` — student learning and attempt lifecycle.
- `results/` — shared/student/teacher results behavior.
- `auth/`, `profile/` — authentication and profile flows.
- `ui/` — UI foundation, accessibility, responsive and theme contracts.
- `navigation/` — application navigation and route contracts.
- `architecture/` — cross-domain architectural/security/stale-context contracts.
- `core/` — generic request/batch/app-level helpers.

When a test reads production source text, prefer paths resolved from `process.cwd()/src` rather than paths relative to the spec file. This keeps tests stable when the test folders are reorganized.
