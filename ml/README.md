# Собственные модели ИИ

Три функции ИИ могут работать не через внешний API, а на своих моделях — на сервере без видеокарты
(8 ГБ ОЗУ, процессор). Обучение — на ПК с видеокартой NVIDIA (проверено на RTX 3050 8 ГБ).
Какой движок использовать, администратор выбирает в «Система → Настройки ИИ».

| Функция | Модель | Как работает на сервере |
|---|---|---|
| Проверка свободных ответов | своя модель-оценщик: энкодер `rubert-tiny2` + две головы (критерий выполнен / попытка обмана), обучена на синтетике | `local-ai`, ONNX int8 ≈ 30 МБ, десятки мс на ответ |
| Разметка сканов лекций | Tesseract + свой классификатор строк (градиентный бустинг по геометрии строк) + сборка Markdown по правилам | `local-ai`, ONNX, миллисекунды на страницу (OCR — секунды) |
| Подсказка критериев | Qwen2.5-0.5B/1.5B-Instruct, дообученная LoRA | Ollama, GGUF q4 ≈ 0,4–1 ГБ, 2–15 с на ответ |

Данные и модели в git не хранятся: по умолчанию лежат в `D:/ai/student-testing` (переменная `ST_ML_HOME`).

## Окружение обучения

```bash
py -3.13 -m venv D:/ai/venv-st
D:/ai/venv-st/Scripts/pip install torch==2.14.1 --index-url https://download.pytorch.org/whl/cu126
D:/ai/venv-st/Scripts/pip install -r ml/requirements.txt pillow skl2onnx
```

Команды ниже запускаются из корня репозитория (`python` — интерпретатор из `D:/ai/venv-st`),
`HF_HOME=D:/ai/hf` — кэш базовых моделей. Генерации данных нужен ключ Groq в корневом `.env` (`APP_AI_API_KEY`).

## 1. Проверка свободных ответов

```bash
python -m ml.grading.generate_dataset topics      # подтемы по 26 дисциплинам
python -m ml.grading.generate_dataset generate    # вопрос, эталон, критерии и ~12 ответов разного качества на подтему
python -m ml.grading.generate_dataset verify      # другая модель перепроверяет разметку; расхождения выбрасываются
python -m ml.grading.train_scorer --name grader-v1
python -m ml.grading.compare_with_api --model grader-v1   # своя модель против API на отложенных ответах
```

Генераторы — `gpt-oss-120b` и `qwen3.8-27b` на Groq (`gpt-oss-20b` писал эталоны с ошибками). Ответы студентов
намеренно разные: верные разными словами, частичные, «похожие, но неверные» (отрицание, перепутанные понятия),
набор ключевых слов, ответ не на тот вопрос, «не знаю», попытки обмануть проверку, разговорные с опечатками.
Тест — вопросы, которых модель не видела (разбиение по подтемам).

Формат входа модели — `local-ai/app/scorer_format.py` (один файл для обучения и сервиса).

## 2. Подсказка критериев

```bash
python -m ml.criteria.train_criteria --base Qwen/Qwen2.5-0.5B-Instruct --name criteria-0.5b
python -m ml.criteria.export_ollama --name criteria-0.5b --tag st-criteria
```

Примеры — вопросы, эталоны и критерии из набора п. 1. Промпт совпадает с тем, что бэкенд отправляет в API.
Экспорт кладёт в `models/<name>/ollama/` файл `st-criteria.gguf` и `Modelfile` — их копируют на сервер в
`local-ai/models/criteria/`. Сумму баллов бэкенд в любом случае приводит к баллу вопроса.

## 3. Разметка сканов

```bash
docker build -t student-test-local-ai:latest local-ai   # тот же Tesseract, что в сервисе
python -m ml.layout.generate_pages --docs 600   # синтетические страницы лекций: шрифты, кегли, списки, колонтитулы, шум
python -m ml.layout.ocr_pages --workers 8       # распознавание в контейнере local-ai
python -m ml.layout.train_layout --name layout-v1
```

Разметка не нужна: генератор знает роль каждой нарисованной строки (заголовок 1–3 уровня, начало/продолжение абзаца,
пункт списка и его продолжение, колонтитул). Признаки строки — `local-ai/app/layout_features.py`, сборка Markdown —
`local-ai/app/layout.py`.

## Развёртывание

1. Скопировать модели в `local-ai/models/` (или указать каталог в `LOCAL_AI_MODELS_DIR`):
   `grader/` ← `models/grader-v1/*`, `layout/` ← `models/layout-v1/*`, `criteria/` ← `models/criteria-0.5b/ollama/*`.
2. `docker compose up -d --build` — сервис `local-ai` поднимается вместе со всем стеком.
3. Для подсказки критериев: `docker compose --profile local-llm up -d`, затем
   `docker exec student-test-ollama ollama create st-criteria -f /models/criteria/Modelfile`
   и `APP_AI_LOCAL_LLM_BASE_URL=http://ollama:11434` в `.env`.
4. В «Система → Настройки ИИ» выбрать «Собственная модель» для нужных функций.
