# Student Testing UI foundation

`components/ui` is the public UI layer of the frontend.

Business views should import controls from `@/components/ui` instead of importing
`primevue/*` directly. PrimeVue is treated as an implementation detail of this
folder.

## Foundation components

- `UiButton` — primary/secondary/danger/success/ghost actions.
- `UiInput` / `UiTextarea` — text controls with a shared field shell.
- `UiSearchInput` — Academic Navy search control with an internal icon zone.
- `UiSelect` — single select with normalized option mapping.
- `UiCheckbox` / `UiRadio` — choice controls. `UiCheckbox` uses an explicit
  `mode="binary"` (default) or `mode="multiple"`; array mode must not be inferred
  from the current `modelValue`. `UiRadio` requires a shared `name` for every
  option in one group. The complete labelled row is the interaction target.
- `UiTag` — semantic statuses.
- `UiAlert` — inline messages.
- `UiCard` — bordered, low-shadow surface.
- `UiToolbar` — page/table action strip.
- `UiTable` — project DataTable wrapper with project columns and cell slots.
- `UiActionMenu` — ellipsis popup actions.
- `UiDialog` — project modal shell.
- `UiToastHost` + `useUiToast()` — system notifications.

## Design rules

- Academic Navy is the only brand theme.
- Controls use 8px radius, cards 12px, dialogs 16px.
- Default control height is 40px.
- Tables are intentionally denser than student cards.
- Search controls use one outer border, an icon zone on the left and a short
  internal vertical divider.
- Destructive actions use semantic danger styling; do not introduce a second
  brand accent color.
