# Файлы собственных моделей

Модели в git не хранятся (вместе ~550 МБ). Они распространяются архивом `student-testing-models-v1.zip`:

| Папка | Что | Размер |
|---|---|---|
| `grader/` | проверка свободных ответов: `model.onnx`, `tokenizer.json`, `tokenizer_config.json`, `meta.json` | 178 МБ |
| `layout/` | разметка сканов лекций: `model.onnx`, `meta.json` | 60 КБ |
| `criteria/` | подсказка критериев для Ollama: `st-criteria.gguf`, `Modelfile` | 398 МБ |

В архиве есть `SHA256SUMS` — контрольные суммы файлов.

## Где взять

Архив выдаёт руководитель проекта (ссылка — в описании PR «Собственные модели ИИ» или в релизе репозитория `models-v1`, если он опубликован). Модели можно и обучить заново — `ml/README.md`.

## Установка

1. Распакуйте архив сюда, в `local-ai/models/`, чтобы получилось `local-ai/models/grader/model.onnx` и т. д. Или в любую папку и укажите её в `.env`: `LOCAL_AI_MODELS_DIR=D:/путь/к/моделям`.
2. Поднимите стек: `docker compose up -d --build`. Сервис `local-ai` подхватит `grader/` и `layout/` при первом запросе.
3. Подсказка критериев: в `.env` добавьте `APP_AI_LOCAL_LLM_BASE_URL=http://ollama:11434`, затем
   `docker compose --profile local-llm up -d` и
   `docker exec student-test-ollama ollama create st-criteria -f /models/criteria/Modelfile`.
4. Администратор → «Система → Настройки ИИ»: у функций должно быть состояние «модель готова»; там же выбирается движок.
