"""Управление Android-телефоном через adb: дерево интерфейса (uiautomator), тапы по тексту, ввод текста.

Русский текст adb напрямую не вводит, поэтому он набирается тапами по клавиатуре Gboard
в русской раскладке (раскладка ЙЦУКЕН с рядом цифр). Набранное сверяется с полем ввода.
"""
from __future__ import annotations

import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass

os.environ["MSYS_NO_PATHCONV"] = "1"  # Git Bash не должен превращать /sdcard в путь Windows
PACKAGE = os.environ.get("ST_APP_PACKAGE", "org.santayn.testing.mobile.debug")


def adb(*args: str, binary: bool = False, check: bool = False):
    r = subprocess.run(["adb", *args], capture_output=True, check=check)
    return r.stdout if binary else r.stdout.decode("utf-8", "replace")


@dataclass
class Node:
    text: str
    x: int
    y: int
    cls: str
    top: int
    bottom: int


def nodes() -> list[Node]:
    for _ in range(3):
        adb("shell", "uiautomator", "dump", "/sdcard/st-ui.xml")
        xml = adb("shell", "cat", "/sdcard/st-ui.xml")
        start = xml.find("<?xml")
        if start >= 0:
            break
        time.sleep(0.5)
    else:
        return []
    result = []
    for n in ET.fromstring(xml[start:]).iter("node"):
        label = n.get("text") or n.get("content-desc") or ""
        b = re.findall(r"\d+", n.get("bounds", ""))
        if label and len(b) == 4:
            x1, y1, x2, y2 = map(int, b)
            result.append(Node(label.strip(), (x1 + x2) // 2, (y1 + y2) // 2, n.get("class", "").split(".")[-1], y1, y2))
    return result


def texts() -> list[str]:
    return [n.text for n in nodes()]


def find(text: str, index: int = 0, exact_first: bool = True) -> Node | None:
    all_nodes = nodes()
    lowered = text.lower()
    hits = [n for n in all_nodes if n.text.lower() == lowered] if exact_first else []
    hits = hits or [n for n in all_nodes if lowered in n.text.lower()]
    return hits[index] if len(hits) > index else None


def tap_xy(x: int, y: int):
    adb("shell", "input", "tap", str(x), str(y))


def tap(text: str, index: int = 0, wait: float = 10) -> Node:
    node = wait_for(text, wait, index=index)
    tap_xy(node.x, node.y)
    return node


def wait_for(text: str, timeout: float = 10, index: int = 0) -> Node:
    deadline = time.time() + timeout
    while True:
        node = find(text, index)
        if node:
            return node
        if time.time() > deadline:
            raise AssertionError(f"На экране нет «{text}»")
        time.sleep(0.5)


def wait_gone(text: str, timeout: float = 60) -> float:
    started = time.time()
    while find(text, exact_first=False):
        if time.time() - started > timeout:
            raise AssertionError(f"«{text}» не исчезло за {timeout:.0f} с")
        time.sleep(0.7)
    return time.time() - started


def key(*codes: str):
    adb("shell", "input", "keyevent", *codes)


def back():
    key("KEYCODE_BACK")
    time.sleep(0.8)


def keyboard_shown() -> bool:
    return "mInputShown=true" in adb("shell", "dumpsys", "input_method")


def hide_keyboard():
    if keyboard_shown():
        back()


def screen_size() -> tuple[int, int]:
    # «Physical size» и, если задан, «Override size» — действует последний.
    width, height = re.findall(r"(\d+)x(\d+)", adb("shell", "wm", "size"))[-1]
    return int(width), int(height)


def swipe_up():
    w, h = screen_size()
    adb("shell", "input", "swipe", str(w // 2), str(int(h * 0.75)), str(w // 2), str(int(h * 0.3)), "300")
    time.sleep(0.6)


def swipe_down():
    w, h = screen_size()
    adb("shell", "input", "swipe", str(w // 2), str(int(h * 0.3)), str(w // 2), str(int(h * 0.75)), "300")
    time.sleep(0.6)


def scroll_to(text: str, max_swipes: int = 8) -> Node:
    for _ in range(max_swipes):
        node = find(text)
        if node:
            return node
        swipe_up()
    raise AssertionError(f"Не нашёл «{text}», прокрутив экран")


def screenshot(path: str):
    with open(path, "wb") as f:
        f.write(adb("exec-out", "screencap", "-p", binary=True))


def clear_field(max_chars: int = 200):
    """Очищает поле в фокусе: Ctrl+A и удаление, иначе — стирание с конца (многострочные поля тоже)."""
    adb("shell", "input", "keycombination", "KEYCODE_CTRL_LEFT", "KEYCODE_A")
    key("KEYCODE_DEL")
    adb("shell", "input", "keycombination", "KEYCODE_CTRL_LEFT", "KEYCODE_MOVE_END")
    adb("shell", "input", "keyevent", *(["KEYCODE_DEL"] * max_chars))


def type_ascii(text: str):
    adb("shell", "input", "text", text.replace(" ", "%s"))


# Gboard, русская раскладка с рядом цифр. Координаты сняты на экране 1080 px шириной:
# x — от левого края, y — от нижнего края экрана; на другой ширине масштабируются.
_ROWS = [
    (636, "1234567890", [59, 165, 272, 378, 486, 592, 699, 806, 912, 1020]),
    (507, "йцукенгшщзх", [55, 151, 247, 345, 442, 539, 636, 733, 829, 927, 1024]),
    (369, "фывапролджэ", [55, 151, 247, 345, 442, 539, 636, 733, 829, 927, 1024]),
    (230, "ячсмитьбю", [172, 263, 354, 447, 539, 630, 722, 814, 906]),
]
_SPECIAL = {" ": (592, 92), ".": (859, 92), ",": (218, 92)}


def _key_positions() -> dict[str, tuple[int, int]]:
    w, h = screen_size()
    k = w / 1080
    keys = {}
    for from_bottom, chars, xs in _ROWS:
        for c, x in zip(chars, xs):
            keys[c] = (int(x * k), int(h - from_bottom * k))
    for c, (x, from_bottom) in _SPECIAL.items():
        keys[c] = (int(x * k), int(h - from_bottom * k))
    keys["ё"], keys["ъ"] = keys["е"], keys["ь"]
    return keys


def type_russian(text: str):
    """Набирает текст тапами по клавиатуре (строчными буквами, без дефисов и кавычек)."""
    keys = _key_positions()
    commands = []
    for c in text.lower():
        if c not in keys:
            raise ValueError(f"Символ {c!r} не набрать на основной раскладке")
        x, y = keys[c]
        commands.append(f"input tap {x} {y}")
    for i in range(0, len(commands), 40):
        adb("shell", ";".join(commands[i:i + 40]))


def launch_app():
    key("KEYCODE_WAKEUP")
    adb("shell", "monkey", "-p", PACKAGE, "-c", "android.intent.category.LAUNCHER", "1")
    time.sleep(3)


def push_to_downloads(local_path: str, name: str):
    adb("push", local_path, f"/sdcard/Download/{name}")
    adb("shell", "am", "broadcast", "-a", "android.intent.action.MEDIA_SCANNER_SCAN_FILE", "-d", f"file:///sdcard/Download/{name}")
