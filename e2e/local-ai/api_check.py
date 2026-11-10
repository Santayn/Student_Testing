"""Сценарии своего ИИ через API, без телефона: проверка ответов, подсказка критериев, скан лекции.

Запуск: python api_check.py [grading|criteria|scan ...]   (без аргументов — всё)
Код выхода 1, если хоть одна проверка не прошла. Итог — в out/<сценарий>.json.
"""
import re
import sys
import time
from pathlib import Path

from common import (ADMIN, CASES, HERE, TEACHER, Api, Report, ensure_engines_local,
                    ensure_stand, outcome, student_credentials, wait_reviewed)


def grading(admin: Api, teacher: Api, student: Api) -> int:
    report = Report("api-grading")
    stand = ensure_stand(teacher, student)
    # Каждый ответ из cases.json сдаётся в отдельной попытке; вопросы теста выбираются случайно,
    # поэтому попыток столько, сколько нужно, чтобы каждый ответ попал на свой вопрос.
    todo = {c["question"]: list(c["answers"]) for c in CASES["questions"]}
    submitted = {}  # id ответа → (вопрос, кейс)
    for _ in range(40):
        if not any(todo.values()):
            break
        attempt = student.post(f"/public/learning/test-assignments/{stand.assignment_id}/attempts/start")
        ids, answers, used = [], [], []
        for q in attempt["questions"]:
            text = q.get("question") or q.get("displayText")
            case = todo.get(text, [None])[0] if todo.get(text) else None
            ids.append(q["id"])
            answers.append(case["text"] if case else "не знаю")
            used.append((text, case))
        result = student.post(f"/public/learning/attempts/{attempt['attemptId']}/submit",
                              {"questionIds": ids, "answers": answers, "selectedOptionIds": [[] for _ in ids]})
        responses = [r for r in teacher.get("/results/teacher/review", status="all")["items"]
                     if r["attemptId"] == result["attemptId"]]
        by_question = {r["questionText"]: r["responseId"] for r in responses}
        for text, case in used:
            if case is not None and text in by_question:
                submitted[by_question[text]] = (text, case)
                todo[text].remove(case)
        hidden = student_view_hides_pending(student, stand.test_id, result["attemptId"])
        report.check(hidden, f"попытка {result['attemptId']}: студент не видит предварительных баллов")
    report.check(not any(todo.values()), "все ответы сданы", str({k: len(v) for k, v in todo.items() if v}))

    started = time.time()
    rows = wait_reviewed(teacher, set(submitted))
    print(f"  проверка {len(rows)} ответов заняла {time.time() - started:.1f} с")
    for response_id, (question, case) in submitted.items():
        row = rows[response_id]
        got = outcome(row)
        detail = f"{got}, балл ИИ {row.get('aiScore')}, уверенность {row.get('aiConfidence')}"
        report.check(got in case["allowed"], f"{case['name']}", detail)
        if case.get("expect") and got != case["expect"] and got in case["allowed"]:
            print(f"    (ожидали {case['expect']} — допустимо, но модель могла бы лучше)")
    # Главный инвариант: своя модель сама не снижает балл и не применяет баллы к попытке обмана.
    lowered = [r for r in rows.values() if r["reviewStatus"] == "ai_graded" and (r.get("aiScore") or 0) < 0.995]
    report.check(not lowered, "своя модель сама не ставит балл ниже полного", str([r["responseId"] for r in lowered]))
    tricked = [r for r in rows.values() if r.get("suspicious") and r["reviewStatus"] != "needs_teacher"]
    report.check(not tricked, "попытки обмана уходят преподавателю")
    return report.finish()


def student_view_hides_pending(student: Api, test_id: int, attempt_id: int) -> bool:
    data = student.get("/results/student/data", testId=test_id)
    attempt = next((a for a in data["attempts"] if a["attemptId"] == attempt_id), None)
    if attempt is None:
        return False
    for item in attempt["results"]:
        if item["reviewStatus"] in ("ai_pending", "needs_teacher"):
            if item.get("awardedPoints") is not None or item.get("aiFeedback") or item.get("correct"):
                return False
            if item.get("gradingStatus") != "pending":
                return False
    return True


def criteria(admin: Api, teacher: Api, student: Api) -> int:
    report = Report("api-criteria")
    case = CASES["criteria_suggestion"]
    started = time.time()
    result = teacher.post("/ai/questions/criteria-suggestion",
                          {"question": case["question"], "correctAnswer": case["correctAnswer"], "points": case["points"]})
    seconds = time.time() - started
    # Критерии приходят текстом для поля вопроса: «— пункт — 1 б.» по строке на критерий.
    lines = [line.strip() for line in (result.get("criteria") or "").splitlines() if line.strip()]
    items = []
    for line in lines:
        match = CRITERION.match(line)
        items.append((match.group(1).strip(), float(match.group(2).replace(",", "."))) if match else (line, None))
        print(f"    {line}")
    report.check(2 <= len(items) <= 5, "2–5 критериев", str(len(items)))
    report.check(all(points is not None for _, points in items), "у каждого критерия есть баллы")
    total = round(sum(points or 0 for _, points in items), 2)
    report.check(abs(total - case["points"]) < 0.01, "сумма баллов = баллам вопроса", f"{total} из {case['points']}")
    report.check(all(len(text) >= 5 for text, _ in items), "у всех критериев есть текст")
    report.check(seconds <= case["max_seconds"], "быстро", f"{seconds:.1f} с")
    return report.finish()


CRITERION = re.compile(r"^[—–\-•*\d.)\s]*(.+?)\s*[—–-]\s*(\d+(?:[.,]\d+)?)\s*б(?:алл\w*)?\.?$")


def scan(admin: Api, teacher: Api, student: Api) -> int:
    report = Report("api-scan")
    case = CASES["scan"]
    files = [("files", (Path(p).name, (HERE / p).read_bytes(), "image/jpeg")) for p in case["pages"]]
    started = time.time()
    job = teacher.post("/ai/lecture-scans", files=files, data={"attachOriginals": "false"})
    while True:
        job = teacher.get(f"/ai/jobs/{job['id']}")
        if job["status"] in ("done", "failed") or time.time() - started > 180:
            break
        time.sleep(1)
    seconds = time.time() - started
    report.check(job["status"] == "done", "скан распознан", job.get("error") or "")
    markdown = (job.get("result") or {}).get("markdown") or ""
    for marker in case["expect_markdown"]:
        report.check(any(line.startswith(marker) for line in markdown.splitlines()), f"в разметке есть «{marker}»")
    report.check(seconds <= case["max_seconds"], "быстро", f"{seconds:.1f} с, страниц {len(files)}")
    return report.finish()


SCENARIOS = {"grading": grading, "criteria": criteria, "scan": scan}

if __name__ == "__main__":
    admin, teacher = Api(*ADMIN), Api(*TEACHER)
    student = Api(*student_credentials())
    ensure_engines_local(admin)
    names = sys.argv[1:] or list(SCENARIOS)
    failed = 0
    for name in names:
        print(f"\n== {name} ==")
        failed |= SCENARIOS[name](admin, teacher, student)
    sys.exit(failed)
