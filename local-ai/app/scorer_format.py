"""Формат входа оценщика: как из вопроса, эталона и критериев собираются пары текстов.
Единый для обучения (ml/grading импортирует этот файл) и сервиса — модель видит одинаковые входы."""
from __future__ import annotations

import re

MAX_LENGTH = 256

_BULLET = re.compile(r"^\s*(?:[—–\-•*·]+|\d+[.)])\s*")
_POINTS = re.compile(r"\s*[—–\-:(]\s*(\d+(?:[.,]\d+)?)\s*(?:б\.?|балл[а-я]*)\)?\s*$", re.IGNORECASE)


def criterion_pair(question: str, criterion: str, answer: str) -> tuple[str, str]:
    return f"Вопрос: {question.strip()}\nКритерий: {criterion.strip()}", answer.strip()


def reference_pair(question: str, reference: str, answer: str) -> tuple[str, str]:
    return f"Вопрос: {question.strip()}\nЭталон: {reference.strip()}", answer.strip()


def parse_criteria(text: str | None, total_points: float) -> list[tuple[str, float]]:
    """«— пункт — 1 б.» построчно → [(пункт, балл)]. Строки без балла делят остаток поровну."""
    if not text or not text.strip():
        return []
    items: list[tuple[str, float | None]] = []
    for raw in text.splitlines():
        line = _BULLET.sub("", raw).strip()
        if not line:
            continue
        points = None
        match = _POINTS.search(line)
        if match:
            points = float(match.group(1).replace(",", "."))
            line = line[: match.start()].strip(" —–-:;.")
        if line:
            items.append((line, points))
    if not items:
        return []
    known = sum(p for _, p in items if p is not None)
    unknown = [i for i, (_, p) in enumerate(items) if p is None]
    rest = max(total_points - known, 0) if total_points else 0
    share = (rest / len(unknown)) if unknown and rest > 0 else 1.0
    return [(text, p if p is not None else share) for text, p in items]


def reference_variants(reference: str | None) -> list[str]:
    """Эталонов может быть несколько: через «|» или с новой строки."""
    if not reference:
        return []
    parts = re.split(r"\s*\|\s*|\n+", reference)
    return [part.strip() for part in parts if part.strip()]
