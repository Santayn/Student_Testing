"""Общее для сценариев: клиент API, учётные записи, подготовка стенда и проверка исходов."""
from __future__ import annotations

import json
import os
import sys
import time
from dataclasses import dataclass, field
from pathlib import Path

import httpx

HERE = Path(__file__).resolve().parent
OUT = HERE / "out"
CASES = json.loads((HERE / "cases.json").read_text(encoding="utf-8"))

API = os.environ.get("ST_API", "http://127.0.0.1:8080/api/v1")
# Учётные записи из начальных данных стенда (DataLoader). Студент — отдельный, чтобы не тратить попытки сид-студентов.
ADMIN = (os.environ.get("ST_ADMIN_LOGIN", "admin"), os.environ.get("ST_ADMIN_PASSWORD", "admin123"))
TEACHER = (os.environ.get("ST_TEACHER_LOGIN", "teacher"), os.environ.get("ST_TEACHER_PASSWORD", "teacher1"))
STUDENT_FILE = OUT / "student.json"

LECTURE_TITLE = "AI local test"
TEST_TITLE = "AI local test"
TOPIC_ID = 1          # «Seed Informatics Topic»
SUBJECT_ID = 1
SUBJECT_MEMBERSHIP_ID = 1   # преподаватель teacher ↔ предмет Informatics
TEACHING_ASSIGNMENT_ID = 1   # преподаватель teacher ↔ группа PI-201
GROUP_ID = 1

sys.stdout.reconfigure(encoding="utf-8")


class Api:
    def __init__(self, login: str, password: str):
        self.client = httpx.Client(base_url=API, timeout=180)
        r = self.client.post("/auth/login", json={"login": login, "password": password})
        r.raise_for_status()
        tokens = r.json()
        self.client.headers["Authorization"] = "Bearer " + (tokens.get("accessToken") or tokens["token"])

    def _check(self, r: httpx.Response):
        if r.status_code >= 400:
            raise RuntimeError(f"{r.request.method} {r.request.url.path} → {r.status_code}: {r.text[:300]}")
        return r.json() if r.content else None

    def get(self, path, **params):
        return self._check(self.client.get(path, params=params))

    def post(self, path, body=None, **kwargs):
        return self._check(self.client.post(path, json=body, **kwargs))

    def put(self, path, body=None):
        return self._check(self.client.put(path, json=body))


def student_credentials() -> tuple[str, str]:
    login = os.environ.get("ST_AI_STUDENT_LOGIN")
    password = os.environ.get("ST_AI_STUDENT_PASSWORD")
    if login and password:
        return login, password
    if STUDENT_FILE.exists():
        data = json.loads(STUDENT_FILE.read_text(encoding="utf-8"))
        return data["login"], data["password"]
    raise SystemExit(
        "Нет тестового студента. Создайте его: python setup.py "
        "(или задайте ST_AI_STUDENT_LOGIN и ST_AI_STUDENT_PASSWORD)."
    )


@dataclass
class Stand:
    lecture_id: int
    test_id: int
    assignment_id: int
    questions: dict[str, dict] = field(default_factory=dict)  # текст вопроса → вопрос из API


def ensure_engines_local(admin: Api) -> list[dict]:
    """Все три функции — на своих моделях; без готовых моделей сценарии не имеют смысла."""
    overview = admin.get("/admin/ai-settings")
    for feature in overview:
        if not feature["localReady"]:
            raise SystemExit(f"Своя модель для «{feature['feature']}» не готова: {feature}")
        if feature["engine"] != "local":
            admin.put(f"/admin/ai-settings/{feature['feature']}", {"engine": "local"})
    return admin.get("/admin/ai-settings")


def ensure_stand(teacher: Api, student: Api | None = None) -> Stand:
    """Вопросы, тест и лекция для сценариев; создаёт недостающее, повторный запуск ничего не дублирует."""
    existing = {q["question"]: q for q in teacher.get("/questions", topicId=TOPIC_ID)}
    for index, case in enumerate(CASES["questions"]):
        if case["question"] not in existing:
            created = teacher.post("/questions", {
                "topicId": TOPIC_ID, "type": 4, "question": case["question"], "points": case["points"],
                "ordinal": 200 + index, "correctAnswer": case["reference"],
                "gradingCriteria": "\n".join(f"— {c}" for c in case.get("criteria", [])) or None,
            })
            existing[created["question"]] = created
    questions = {c["question"]: existing[c["question"]] for c in CASES["questions"]}

    tests = {t["title"]: t for t in teacher.get("/tests", subjectId=SUBJECT_ID)}
    test = tests.get(TEST_TITLE)
    if test is None:
        raise SystemExit(f"Нет теста «{TEST_TITLE}». Запустите python setup.py.")
    if test["attemptsAllowed"] < 1000:
        # Сценарии сдают тест много раз.
        teacher.put(f"/tests/{test['id']}", {
            "title": test["title"], "description": test.get("description"), "duration": test.get("duration"),
            "attemptsAllowed": 1000, "questionCount": test["questionCount"], "showAiFeedback": True,
        })

    lectures = {l["title"]: l for l in teacher.get("/lectures", subjectMembershipId=SUBJECT_MEMBERSHIP_ID)}
    lecture = lectures.get(LECTURE_TITLE)
    if lecture is None:
        raise SystemExit(f"Нет лекции «{LECTURE_TITLE}». Запустите python setup.py.")

    assignment_id = 0
    if student is not None:
        tests_of_lecture = student.get(f"/public/learning/lectures/{lecture['id']}/tests")
        assignment_id = next(t["assignmentId"] for t in tests_of_lecture if t["id"] == test["id"])
    return Stand(lecture["id"], test["id"], assignment_id, questions)


# Исходы проверки одного ответа своей моделью.
AUTO_FULL = "auto_full"        # ИИ уверенно поставил полный балл сам
TO_TEACHER = "teacher"         # ответ ушёл преподавателю
SUSPICIOUS = "suspicious"      # попытка повлиять на проверку: к преподавателю, баллы ИИ не применены


def outcome(item: dict) -> str:
    """Исход по строке очереди «Проверка ответов» (/results/teacher/review)."""
    if item.get("suspicious"):
        return SUSPICIOUS
    if item["reviewStatus"] == "ai_graded" and (item.get("aiScore") or 0) >= 0.995:
        return AUTO_FULL
    if item["reviewStatus"] == "needs_teacher":
        return TO_TEACHER
    return f"{item['reviewStatus']} (балл {item.get('aiScore')})"


def wait_reviewed(teacher: Api, response_ids: set[int], timeout: float = 120) -> dict[int, dict]:
    """Ждёт, пока ИИ проверит ответы; возвращает строки очереди проверки по id ответа."""
    deadline = time.time() + timeout
    while True:
        rows = {r["responseId"]: r for r in teacher.get("/results/teacher/review", status="all")["items"]
                if r["responseId"] in response_ids}
        if len(rows) == len(response_ids) and all(r["reviewStatus"] != "ai_pending" for r in rows.values()):
            return rows
        if time.time() > deadline:
            raise TimeoutError(f"ИИ не проверил ответы за {timeout:.0f} с: {sorted(response_ids - rows.keys())}")
        time.sleep(1)


class Report:
    def __init__(self, title: str):
        self.title = title
        self.rows: list[tuple[bool, str, str]] = []

    def check(self, ok: bool, name: str, detail: str = ""):
        self.rows.append((ok, name, detail))
        print(("  ✔ " if ok else "  ✘ ") + name + (f" — {detail}" if detail else ""), flush=True)

    def finish(self) -> int:
        failed = [r for r in self.rows if not r[0]]
        print(f"\n{self.title}: {len(self.rows) - len(failed)} из {len(self.rows)} проверок прошли", flush=True)
        OUT.mkdir(exist_ok=True)
        (OUT / f"{self.title}.json").write_text(json.dumps(
            [{"ok": ok, "name": n, "detail": d} for ok, n, d in self.rows], ensure_ascii=False, indent=1),
            encoding="utf-8")
        return 1 if failed else 0
