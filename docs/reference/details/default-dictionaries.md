# Начальные справочники DataLoader

Эти значения создаются текущим `DataLoader` при включённом seed. Они являются начальными данными, а не обязательно закрытыми enum системы.

## Роли

```text
USER
STUDENT
TEACHER
ADMIN
```

## Permissions

```text
users.read
users.write
roles.manage
people.read
people.write
academic.manage
courses.manage
teaching.manage
tests.manage
questions.manage
tests.take
lectures.read
```

## Teaching load types

```text
Lecture
Practice
Laboratory
```

## Demo accounts

При локальном demo seed создаются известные тестовые аккаунты, включая `student`, `teacher`, `admin` и дополнительные student/teacher users. Эти данные предназначены для local/demo environment и не должны считаться production defaults.

Полный lifecycle DataLoader описан в [Deployment: first start/bootstrap](../../deployment/details/first-start-and-bootstrap.md).
