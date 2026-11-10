"""Подготовка стенда для сценариев своего ИИ (запускается один раз, повторный запуск ничего не дублирует).

Создаёт тестового студента в группе PI-201, вопросы со свободным ответом, тест «AI local test»
и лекцию с этим тестом. Для создания студента на время запуска нужна открытая регистрация:
APP_PUBLIC_REGISTRATION_ENABLED=true docker compose up -d backend
после — верните как было: docker compose up -d backend
"""
import json
import secrets

from common import (ADMIN, CASES, GROUP_ID, LECTURE_TITLE, OUT, STUDENT_FILE, SUBJECT_ID, SUBJECT_MEMBERSHIP_ID, TEACHER,
                    TEACHING_ASSIGNMENT_ID, TEST_TITLE, TOPIC_ID, Api, ensure_stand)

STUDENT_LOGIN = "aitester"


def ensure_student(admin: Api):
    OUT.mkdir(exist_ok=True)
    users = {u["login"]: u for u in admin.get("/users")}
    if STUDENT_LOGIN not in users:
        password = secrets.token_urlsafe(9)
        r = admin.client.post("/auth/register", json={"login": STUDENT_LOGIN, "password": password})
        if r.status_code == 403:
            raise SystemExit("Регистрация закрыта — откройте её на время setup (см. описание в начале файла).")
        r.raise_for_status()
        STUDENT_FILE.write_text(json.dumps({"login": STUDENT_LOGIN, "password": password}), encoding="utf-8")
        users = {u["login"]: u for u in admin.get("/users")}
    elif not STUDENT_FILE.exists():
        print(f"Студент {STUDENT_LOGIN} уже есть, но пароля нет в {STUDENT_FILE} — задайте ST_AI_STUDENT_PASSWORD.")
    user = users[STUDENT_LOGIN]
    if not user.get("personId"):
        person = admin.post("/users/people", {"firstName": "Тестовый", "lastName": "Студент ИИ",
                                              "email": f"{STUDENT_LOGIN}@example.local"})
        admin.put(f"/users/{user['id']}/person", {"personId": person["id"]})
        user = admin.get(f"/users/{user['id']}")
    roles = {r["name"]: r["id"] for r in admin.get("/roles")}
    admin.put(f"/users/{user['id']}/roles", {"roleIds": [roles["STUDENT"]]})
    members = admin.get(f"/memberships/groups/{GROUP_ID}")
    if not any(m.get("personId") == user["personId"] for m in members):
        admin.post(f"/memberships/groups/{GROUP_ID}", {"personId": user["personId"], "role": 1, "notes": "Сценарии ИИ"})
    print("студент:", STUDENT_LOGIN)


def ensure_test_and_lecture(teacher: Api):
    tests = {t["title"]: t for t in teacher.get("/tests", subjectId=SUBJECT_ID)}
    test = tests.get(TEST_TITLE)
    if test is None:
        count = len(CASES["questions"])
        test = teacher.post("/tests", {
            "title": TEST_TITLE, "attemptsAllowed": 1000, "questionCount": count, "showAiFeedback": True,
            "selectionRules": [{"courseLectureId": None, "topicId": TOPIC_ID, "questionCount": count,
                                "textQuestionCount": count, "singleAnswerQuestionCount": 0,
                                "multipleAnswerQuestionCount": 0, "matchingQuestionCount": 0, "ordinal": 1}],
        })
    lectures = {l["title"]: l for l in teacher.get("/lectures", subjectMembershipId=SUBJECT_MEMBERSHIP_ID)}
    lecture = lectures.get(LECTURE_TITLE)
    if lecture is None:
        any_lecture = next(iter(lectures.values()))
        lecture = teacher.post("/lectures", {
            "subjectId": SUBJECT_ID, "subjectMembershipId": any_lecture["subjectMembershipId"],
            "courseVersionId": any_lecture["courseVersionId"], "ordinal": 900, "title": LECTURE_TITLE,
            "description": "Сценарии собственных моделей ИИ", "contentFolderKey": "ai-local-test",
            "linkedTestId": test["id"], "publicVisible": True,
        })
    links = teacher.get("/teaching/lecture-assignments", teachingAssignmentId=TEACHING_ASSIGNMENT_ID)
    link = next((l for l in links if l["courseLectureId"] == lecture["id"]), None)
    if link is None:
        link = teacher.post(f"/teaching/assignments/{TEACHING_ASSIGNMENT_ID}/lecture-assignments",
                            {"courseLectureId": lecture["id"], "required": False, "minProgressPercent": 0, "status": 1})
    if link["status"] != 1:
        teacher.put(f"/teaching/lecture-assignments/{link['id']}/status", {"status": 1})
    print("тест:", test["id"], "лекция:", lecture["id"])


if __name__ == "__main__":
    admin, teacher = Api(*ADMIN), Api(*TEACHER)
    ensure_student(admin)
    try:
        ensure_stand(teacher)
    except SystemExit:
        pass  # теста/лекции ещё нет — вопросы уже созданы, создаём остальное ниже
    ensure_test_and_lecture(teacher)
    ensure_stand(teacher)
    print("Готово.")
