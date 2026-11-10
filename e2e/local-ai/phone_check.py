"""Сценарии своего ИИ на телефоне (adb): студент сдаёт тест, преподаватель просит критерии и распознаёт скан.

Запуск: python phone_check.py [grading|criteria|scan ...] [--attempts N]   (без сценариев — все)
Перед запуском: телефон подключён по adb, приложение установлено и смотрит на стенд,
клавиатура Gboard в русской раскладке. Скриншоты при ошибках — в out/.
"""
import argparse
import re
import sys
import time
import traceback

import phone
from common import (ADMIN, CASES, HERE, LECTURE_TITLE, OUT, TEACHER, Api, Report, ensure_engines_local,
                    ensure_stand, student_credentials)

CHIP = re.compile(r"^(Верно|Частично верно|Неверно)( · .*)?$|^У преподавателя$|^Проверяет ИИ$")


# ---------- вход и навигация ----------

def login(user: str, password: str):
    phone.launch_app()
    if not phone.find("Войдите, чтобы продолжить", exact_first=False):
        logout()
    fields = [n for n in phone.nodes() if n.cls == "EditText"]
    phone.tap_xy(fields[0].x, fields[0].y)
    time.sleep(0.5)
    phone.clear_field(40)
    phone.type_ascii(user)
    phone.key("KEYCODE_TAB")
    time.sleep(0.5)
    phone.type_ascii(password)
    phone.hide_keyboard()
    phone.tap("Войти")
    phone.wait_for("Главная", 20)


def to_tabs():
    """Назад до экрана с нижним меню; несохранённые правки сценария отбрасываются."""
    for _ in range(6):
        if phone.find("Профиль"):
            return
        leave = phone.find("Уйти без сохранения") or phone.find("Закрыть без сохранения")
        if leave:
            phone.tap_xy(leave.x, leave.y)
            time.sleep(1)
            continue
        phone.back()
    raise AssertionError("Не удалось вернуться к нижнему меню")


def logout():
    to_tabs()
    phone.tap("Профиль")
    phone.scroll_to("Выйти")
    phone.tap("Выйти")
    time.sleep(1)
    phone.tap("Выйти")  # подтверждение в диалоге
    phone.wait_for("Войдите, чтобы продолжить", 15)


def open_test_as_student():
    phone.tap("Informatics", wait=15)
    phone.tap("Лекции")
    phone.tap(LECTURE_TITLE, wait=15)
    phone.tap("Пройти тест", wait=15)
    phone.wait_for("Вопрос 1 из", 20)


# ---------- проверка ответов ----------

def grading(report: Report, attempts: int):
    case_by_question = {c["question"]: c for c in CASES["questions"]}
    login(*student_credentials())
    for attempt in range(attempts):
        open_test_as_student()
        typed = {}  # вопрос → кейс ответа
        while True:
            screen = phone.texts()
            question = next((t for t in screen if t in case_by_question), None)
            label = phone.wait_for("Ваш ответ")
            phone.tap_xy(540, label.bottom + 120)
            time.sleep(0.8)
            if question:
                answers = case_by_question[question]["answers"]
                case = answers[attempt % len(answers)]
                phone.type_russian(case["text"])
                typed[question] = case
            else:
                phone.type_ascii("i do not know")  # вопрос не из сценария (например, сид-вопрос темы)
            time.sleep(0.5)
            field = next((n for n in phone.nodes() if n.cls == "EditText"), None)
            if question:
                expected = case["text"].lower()
                ok = field is not None and expected.startswith(field.text.lower()[:60].rstrip("…"))
                report.check(ok, f"ответ набран: {case['name']}", "" if ok else f"в поле: {field.text if field else '—'}")
            phone.hide_keyboard()
            if phone.find("Завершить тест"):
                phone.tap("Завершить тест")
                break
            phone.tap("Следующий вопрос")
            time.sleep(1)

        # Сразу после сдачи — анимация проверки, затем итог без перезахода на экран.
        animated = phone.find("ИИ проверяет свободные ответы", exact_first=False) or phone.find("Проверяет ИИ")
        report.check(bool(animated), f"попытка {attempt + 1}: видна анимация проверки ИИ")
        seconds = phone.wait_gone("ИИ проверяет свободные ответы", 90)
        report.check(not phone.find("Проверяет ИИ"), f"попытка {attempt + 1}: итог обновился сам", f"{seconds:.1f} с")

        for question, case in typed.items():
            chip = chip_for(question)
            allowed = case["allowed"]
            if chip is None:
                report.check(False, f"{case['name']}: статус на экране", "не найден")
                continue
            if chip.startswith("Верно"):
                got = "auto_full"
            elif chip == "У преподавателя":
                got = "teacher or suspicious"
            else:
                got = chip
            ok = ("auto_full" in allowed and got == "auto_full") or (
                got == "teacher or suspicious" and ({"teacher", "suspicious"} & set(allowed)))
            report.check(bool(ok), f"{case['name']}: «{chip}»", f"допустимо: {allowed}")
        phone.tap("К лекции")
        time.sleep(1.5)
        phone.back()
    logout()


def chip_for(question: str) -> str | None:
    """Статус в карточке ответа: чип в строке «Вопрос N» над текстом вопроса."""
    node = phone.scroll_to(question)
    for n in phone.nodes():
        if CHIP.match(n.text) and node.top - 220 < n.y < node.top:
            return n.text
    return None


# ---------- подсказка критериев ----------

def criteria(report: Report):
    case = CASES["questions"][0]
    login(*TEACHER)
    phone.tap("Вопросы", wait=15)
    phone.tap(phone.scroll_to(case["question"][:40]).text)
    phone.scroll_to("Предложить критерии")
    phone.swipe_up()  # кнопка не у самого края экрана
    # Очищаем поле: модель на тот же вопрос может вернуть тот же текст, а сравнивать надо с пустым.
    field = criteria_field()
    if field is not None and criteria_text():
        phone.tap_xy(field.x, field.y)
        time.sleep(0.5)
        phone.clear_field(600)
        phone.hide_keyboard()
    report.check(criteria_text() == "", "поле критериев очищено")
    started = time.time()
    phone.tap("Предложить критерии")
    after = ""
    while time.time() - started < 60 and not after:
        time.sleep(0.7)
        after = criteria_text()
    seconds = time.time() - started
    report.check(bool(after), "критерии подставлены в поле", f"{seconds:.1f} с")
    report.check(seconds <= CASES["criteria_suggestion"]["max_seconds"], "быстро", f"{seconds:.1f} с")
    points = [float(p.replace(",", ".")) for p in re.findall(r"(\d+(?:[.,]\d+)?)\s*б\.?\s*(?:\n|$)", after)]
    report.check(abs(sum(points) - case["points"]) < 0.01, "сумма баллов = баллам вопроса", f"{sum(points)} из {case['points']}")
    print("    " + after.replace("\n", "\n    "))
    # Без сохранения: сценарий не меняет вопросы стенда.
    phone.tap("Закрыть")
    phone.tap("Закрыть без сохранения", wait=5)
    phone.back()
    logout()


def criteria_field():
    # Поле критериев — последнее с лимитом 4000 (у эталонного ответа такой же лимит).
    screen = phone.nodes()
    counters = [n for n in screen if n.text.endswith("/4000")]
    if not counters:
        return None
    # Счётчик — подпись внутри границ поля.
    fields = [n for n in screen if n.cls == "EditText" and n.top < counters[-1].top <= n.bottom]
    if not fields:
        return None
    fields[-1].empty = counters[-1].text.startswith("0/")
    return fields[-1]


def criteria_text() -> str:
    field = criteria_field()
    # Пустое поле показывает подсказку, а не текст — по счётчику видно, что символов 0.
    return "" if field is None or field.empty else field.text


# ---------- скан лекции ----------

def scan(report: Report):
    pages = CASES["scan"]["pages"]
    names = [f"st-e2e-{i + 1}.jpg" for i in range(len(pages))]
    for path, name in zip(pages, names):
        phone.push_to_downloads(str(HERE / path), name)
    login(*TEACHER)
    phone.tap("Лекции", wait=15)
    phone.tap(LECTURE_TITLE, wait=15)
    phone.scroll_to("Распознать скан")
    phone.tap("Распознать скан")
    phone.tap("Файлы")
    first = phone.wait_for(names[0], 15)
    phone.adb("shell", "input", "swipe", str(first.x), str(first.y), str(first.x), str(first.y), "900")
    for name in names[1:]:
        phone.tap(name)
    phone.tap("Выбрать")
    phone.wait_for(f"Страница {len(names)}", 15)
    attach = phone.find("Прикрепить сканы к материалам лекции")
    if attach:
        phone.tap_xy(attach.x, attach.y)  # не прикреплять к материалам
    button = phone.scroll_to("Распознать")
    started = time.time()
    phone.tap_xy(button.x, button.y)
    # Пока идёт распознавание, на кнопке — ход работы; закончилось — снова «Распознать».
    time.sleep(1)
    while time.time() - started < CASES["scan"]["max_seconds"] + 30:
        label = next((n.text for n in phone.nodes() if abs(n.y - button.y) < 60 and abs(n.x - button.x) < 300), "")
        if label == "Распознать":
            break
        time.sleep(0.5)
    seconds = time.time() - started
    done = phone.scroll_to("Вставить в текст лекции", 4) if not phone.find("Заменить текст") else True
    report.check(bool(done), "скан распознан", f"{seconds:.1f} с")
    report.check(seconds <= CASES["scan"]["max_seconds"], "быстро", f"{seconds:.1f} с")
    phone.tap("Закрыть")
    leave = phone.find("Закрыть без сохранения")
    if leave:
        phone.tap_xy(leave.x, leave.y)
    phone.back()
    logout()


SCENARIOS = {"grading": grading, "criteria": criteria, "scan": scan}

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("scenarios", nargs="*", default=list(SCENARIOS))
    parser.add_argument("--attempts", type=int, default=2)
    args = parser.parse_args()
    if "device" not in phone.adb("devices").split("\n", 1)[-1]:
        sys.exit("Телефон не подключён (adb devices).")
    admin, teacher = Api(*ADMIN), Api(*TEACHER)
    ensure_engines_local(admin)
    ensure_stand(teacher, Api(*student_credentials()))
    OUT.mkdir(exist_ok=True)
    failed = 0
    for name in args.scenarios:
        print(f"\n== {name} ==", flush=True)
        report = Report(f"phone-{name}")
        try:
            SCENARIOS[name](report, args.attempts) if name == "grading" else SCENARIOS[name](report)
        except Exception as error:  # noqa: BLE001 — сценарий падает целиком, остальные идут дальше
            phone.screenshot(str(OUT / f"phone-{name}-error.png"))
            report.check(False, "сценарий прошёл до конца", f"{error} (скриншот out/phone-{name}-error.png)")
            traceback.print_exc()
        failed |= report.finish()
    sys.exit(failed)
