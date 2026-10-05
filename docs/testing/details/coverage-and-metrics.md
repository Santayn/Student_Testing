# Coverage и метрики

## Текущее состояние

Проект не настраивает формальный code coverage ни для frontend, ни для backend.

### Frontend

Нет coverage provider/команды Vitest и threshold для lines/branches/functions.

### Backend

Нет JaCoCo/Sonar/Pitest и coverage threshold в Maven build.

Следовательно, утверждения вида:

```text
Frontend coverage = 90%
Backend coverage = 85%
```

для текущего проекта не имеют фактического основания.

## Что можно измерять уже сейчас

Структурные метрики:

- количество spec/test classes;
- количество test declarations/methods;
- распределение tests по доменам;
- число API operations, покрытых smoke;
- длительность suite;
- число regression defects, для которых добавлен test.

Эти показатели полезны, но не эквивалентны code coverage.

## Рекомендуемый frontend coverage

Добавить Vitest coverage provider и собирать как минимум:

```text
lines
statements
functions
branches
```

На первом этапе лучше использовать coverage как наблюдаемую метрику без агрессивного глобального threshold. Затем вводить thresholds для критичных utilities/composables.

## Рекомендуемый backend coverage

Добавить JaCoCo report. Особое внимание уделять не общему проценту, а покрытию:

- authorization decisions;
- test attempt lifecycle;
- grading;
- memberships/teaching state transitions;
- backup/error handling.

## Mutation testing

Для алгоритмически критичных компонентов, например text answer evaluator, со временем можно рассмотреть mutation testing. Оно полезнее простого line coverage при проверке качества assertions.
