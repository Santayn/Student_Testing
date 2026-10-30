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
- `UiFilterBar` — workspace search/filter/action strip with a responsive mobile stack.
- `UiTable` — project DataTable wrapper with project columns and cell slots.
- `UiActionMenu` — ellipsis popup actions.
- `UiDialog` — compact CRUD modal shell backed by PrimeVue Dialog.
- `UiDrawer` — side editor backed by PrimeVue Drawer; becomes full-screen on phones.
- `UiUnsavedChangesConfirm` — shared confirmation for closing a dirty overlay form.
- `UiToastHost` + `useUiToast()` — system notifications.
- `useOverlayForm()` — entity-agnostic create/edit, dirty, saving and close lifecycle for overlay forms.

## Design rules

- Academic Navy is the only brand theme.
- Controls use 8px radius, cards 12px, dialogs 16px.
- Default control height is 40px.
- Tables are intentionally denser than student cards.
- Search controls use one outer border, an icon zone on the left and a short
  internal vertical divider.
- Destructive actions use semantic danger styling; do not introduce a second
  brand accent color.

## Overlay policy

- Keep list/workspace pages visible for browsing, searching and filtering.
- Use `UiDialog` for short CRUD forms.
- Use `UiDrawer` for medium editors that need more vertical space and context.
- Keep long, multi-step workflows on dedicated routes.
- Keep only one working overlay open at a time; a small confirmation dialog may sit above it.
- Route changes are not required for ordinary create/edit overlays.
- PrimeVue owns portal, focus trap, keyboard and overlay mechanics; Tailwind and
  `--st-*` tokens are used for content layout and project styling.

## Motion API (opt-in)

Motion is configured on individual components. Existing components preserve their
current appearance and transition behaviour unless a `motion` prop is supplied.
No application-wide animation setting, Pinia state, or new runtime dependency is required.

| Component | Allowed `motion` values |
| --- | --- |
| `UiButton` (button, link, disabled link) | `default`, `none`, `press`, `lift`, `pulse`, `hover`, `focused` |
| `UiCard` | `default`, `none`, `lift`, `hover`, `focused` |
| `UiAlert` | `default`, `none`, `fade`, `fade-slide`, `pulse` |
| `UiDialog` | `default`, `none`, `fade`, `fade-slide`, `scale-fade` |
| `UiSelect` | `default`, `none`, `focus`, `hover`, `focused` |
| `UiTable` | `default`, `none`, `row-highlight`, `focused` |
| `UiField` / `UiInput` / `UiTextarea` | `default`, `none`, `error-reveal`, `focused` |
| `UiLoadingState` | `default`, `none`, `fade`, `soft-pulse` |
| `UiToastHost` | `default`, `none`, `fade`, `fade-slide` |
| `UiDisclosure` | `default`, `none`, `expand` |

Examples:

```vue
<UiButton label="Сохранить" motion="press" />
<UiCard title="Предмет" motion="lift">...</UiCard>
<UiAlert message="Сохранено" motion="fade-slide" />
<UiDialog v-model="open" title="Редактирование" motion="scale-fade">...</UiDialog>
<UiSelect v-model="value" :options="options" motion="focus" />
```

For complex elements, prefer a local and intentional effect:

```vue
<UiTable :columns="columns" :rows="rows" motion="row-highlight" />
<UiInput v-model="name" :error="error" motion="error-reveal" />
<UiLoadingState motion="soft-pulse" />
<UiToastHost motion="fade-slide" />
<UiDisclosure title="Фильтры" motion="expand">
  <!-- Fields remain mounted while the panel is closed. -->
</UiDisclosure>
```

`UiTable` never animates the whole row on sort, pagination or loading, only
background color on hover/focus. `UiField` animates the error message, not
input positioning. `UiDisclosure` uses `v-show` to preserve nested form state;
the trigger uses `aria-expanded` / `aria-controls`. Toast animation is applied
to individual messages, not the whole notification host. The spinner of
`UiLoadingState` continues to communicate activity even with `motion="none"`.

For an **enter/leave lifecycle**, use `UiMotion` instead of an interaction prop:

```vue
<UiMotion :show="visible" preset="fade-slide">
  <UiAlert variant="success" message="Сохранено" motion="none" />
</UiMotion>
```

`UiMotion` provides `show`, `preset` (`none`, `fade`, `fade-slide`,
`scale-fade`, `expand`), `appear` (default `false`), and `tag` (default `div`).
It renders a DOM wrapper; `expand` additionally renders an inner overflow
container. Use `show` rather than unmounting the `UiMotion` wrapper itself.

`UiAlert.motion` animates **initial appearance only**; `UiDialog.motion` customizes
the existing PrimeVue dialog transition, without touching its modal mask or focus
management. Buttons animate their own native/RouterLink DOM element. The `lift`
preset requires a real hover pointer. All effects use `--st-motion-*` tokens and
respect `prefers-reduced-motion`; `useMotionPreferences()` is available for cases
that also need a reactive JS signal.

Never use a decorative animation as the only indicator of an error, focus or success.
The legacy overlay, form-validation and navigation animations are unchanged.

Developer preview (dev server only): `/motion-preview`.

## Adaptive Glass / Shine API (opt-in)

`surface` and `shine` are independent from the existing `motion` prop:

```vue
<UiCard surface="glass" shine="sweep" shine-intensity="subtle" motion="lift">
  Предмет
</UiCard>
<UiButton surface="frosted" shine="sweep" shine-intensity="strong" motion="press" label="Сохранить" />
<UiDialog v-model="showDialog" surface="frosted" title="Окно">...</UiDialog>
<UiSelect v-model="value" :options="options" surface="glass" />
```

| Prop | Supported on | Values | Default |
| --- | --- | --- | --- |
| `surface` | Card, Button, Dialog, Select | `default`, `glass`, `frosted` | `default` |
| `shine` | Card, Button | `none`, `sweep` | `none` |
| `shineIntensity` | Card, Button | `subtle`, `normal`, `strong` | `normal` |

The sweep is a **single fixed diagonal stripe moving left to right** (760 ms),
restarted by a new hover (or button keyboard focus). It does not depend on
pointer position, scroll position, or pointermove events. `shine` without a
non-default `surface` intentionally has no effect. Dark mode uses a cool lunar
reflection; light mode uses a warm solar reflection. Theme values live under
`--st-glass-*` in `theme/tokens.css`. Dialogs/Selects use only the material,
not animated glare, to avoid distracting form interactions.

For reduced motion the stripe does not move; for reduced transparency or browsers
without backdrop filters the background becomes opaque. Keep glass opt-in to
avoid expensive blur on dense mobile lists. The effect is demonstrated at
`/motion-preview` in development mode.
