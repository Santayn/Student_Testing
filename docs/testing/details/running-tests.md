# Запуск тестов

## Frontend: prerequisites

Требуется Node согласно `frontend/package.json`:

```text
^22.18.0 || >=24.12.0
```

Установка зависимостей:

```bash
cd frontend
npm ci
```

## Полный frontend quality gate

```bash
npm run quality
```

Эквивалентно:

```bash
npm run lint
npm run quality:static
npm run test:unit -- --run
npm run build
```

## Только Vitest

Однократный прогон:

```bash
npm run test:unit -- --run
```

Watch mode:

```bash
npm run test:unit
```

Отдельный spec:

```bash
npx vitest run src/__tests__/student/learning/TestAttemptReload.spec.js
```

## Backend: Windows

Из `backend/`:

```powershell
.\mvnw.cmd test
```

или при установленном Maven:

```powershell
mvn test
```

## Backend: Linux/macOS

Целевая команда:

```bash
./mvnw test
```

Но текущий audited archive содержит проблему Unix wrapper: `backend/mvnw` сохранён с CRLF и не имеет корректного executable bit после ZIP packaging. До исправления можно использовать установленный Maven:

```bash
mvn test
```

либо нормализовать wrapper локально:

```bash
sed -i 's/\r$//' mvnw
chmod +x mvnw
./mvnw test
```

Нормализация wrapper должна быть исправлена в репозитории, а не рассматриваться как постоянный test workflow.

## API/system smoke

Требуется уже запущенный backend/demo environment:

```powershell
cd backend
.\scripts\full-system-smoke.ps1
```

С другим backend URL:

```powershell
.\scripts\full-system-smoke.ps1 -BaseUrl http://localhost:8080
```

Текущий smoke требует ревизии покрытия; см. [api-system-smoke.md](./api-system-smoke.md).
