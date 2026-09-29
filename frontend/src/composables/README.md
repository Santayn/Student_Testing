# Composables structure

Composables are grouped by the screen/domain that owns their behavior.

- `questions/` — teacher question bank and question editor flows.
- `lectures/` — lecture list, editor, materials and delete/save flows.
- `course-templates/` — course template data, editors and mutations.
- `results/` — results filters, data and contextual option cascade.
- `profile/` — profile loading and password change flows.
- `tests/` — test attempt and test editor flows.
- `teacher/` — shared teacher workspace context and teacher workload/topic data.
- `admin/` — admin screen-specific composables, further grouped by workspace.
- `shared/` — reusable UI/application composables that are not owned by one screen.

Rule of thumb: if a composable is only meaningful for one feature/screen, keep it in that feature folder. Put it in `shared/` only when multiple unrelated features genuinely reuse it.
