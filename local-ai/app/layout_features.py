"""Признаки строк распознанной страницы для классификатора разметки. Один код для обучения и сервиса.

Вход — TSV Tesseract (уровни page/block/par/line/word). Строка описывается геометрией относительно
страницы и соседей (высота шрифта, отступ, промежутки), формой текста (маркер списка, номер, точка
в конце) и повтором на других страницах (колонтитулы).
"""
from __future__ import annotations

import math
import re
from dataclasses import dataclass, field

LABELS = ["H1", "H2", "H3", "P_START", "P_CONT", "LI_START", "LI_CONT", "MARGIN"]

_BULLET = re.compile(r"^[•●▪■◦○∙·\-–—*»]\s*")
_NUMBERED = re.compile(r"^(\d{1,2}|[a-zа-я])[.)]\s+", re.IGNORECASE)
_SECTION = re.compile(r"^\d+(\.\d+)+\.?\s")
# Маркер списка, который Tesseract прочитал «мусором»: одиночный символ или одиночная латинская буква
# перед словом («● Текст» → «® Текст», «e Текст»). Одиночные русские буквы — настоящие слова («В», «С»).
JUNK_MARKER = re.compile(r"^(?:[^\w\s]|[A-Za-z])\s+(?=\S)")
_PAGE_NUMBER = re.compile(r"^[-–—\s]*(стр\.?|страница|page)?\s*\d{1,4}\s*[-–—\s]*$", re.IGNORECASE)


@dataclass
class Line:
    page: int
    text: str
    left: int
    top: int
    width: int
    height: int
    conf: float
    word_heights: list[int] = field(default_factory=list)

    @property
    def right(self):
        return self.left + self.width

    @property
    def bottom(self):
        return self.top + self.height

    @property
    def font_height(self):
        # Высота строки зависит от выносных элементов; медиана высот слов устойчивее.
        if self.word_heights:
            values = sorted(self.word_heights)
            return values[len(values) // 2]
        return self.height


def parse_tsv(tsv: str, page: int) -> tuple[list[Line], int, int]:
    """TSV Tesseract → строки страницы (в порядке чтения) и размер страницы."""
    rows = [row.split("\t") for row in tsv.splitlines()]
    if not rows:
        return [], 0, 0
    header = rows[0]
    index = {name: i for i, name in enumerate(header)}
    page_w = page_h = 0
    lines: dict[tuple, dict] = {}
    order: list[tuple] = []
    for row in rows[1:]:
        if len(row) < len(header):
            continue
        level = int(row[index["level"]])
        if level == 1:
            page_w, page_h = int(row[index["width"]]), int(row[index["height"]])
            continue
        if level != 5:
            continue
        text = row[index["text"]].strip()
        if not text:
            continue
        key = (row[index["block_num"]], row[index["par_num"]], row[index["line_num"]])
        left, top = int(row[index["left"]]), int(row[index["top"]])
        width, height = int(row[index["width"]]), int(row[index["height"]])
        conf = float(row[index["conf"]])
        entry = lines.get(key)
        if entry is None:
            entry = {"words": [], "left": left, "top": top, "right": left + width, "bottom": top + height,
                     "confs": [], "heights": []}
            lines[key] = entry
            order.append(key)
        entry["words"].append(text)
        entry["left"] = min(entry["left"], left)
        entry["top"] = min(entry["top"], top)
        entry["right"] = max(entry["right"], left + width)
        entry["bottom"] = max(entry["bottom"], top + height)
        if conf >= 0:
            entry["confs"].append(conf)
        if any(ch.isalnum() for ch in text):
            entry["heights"].append(height)
    result = [
        Line(page, " ".join(e["words"]), e["left"], e["top"], e["right"] - e["left"], e["bottom"] - e["top"],
             (sum(e["confs"]) / len(e["confs"])) if e["confs"] else 0.0, e["heights"])
        for e in (lines[key] for key in order)
    ]
    result.sort(key=lambda line: (line.top, line.left))
    return result, page_w, page_h


def _median(values: list[float], default: float = 1.0) -> float:
    values = sorted(v for v in values if v > 0)
    return values[len(values) // 2] if values else default


def _norm_margin_text(text: str) -> str:
    return re.sub(r"\d+", "#", text.lower()).strip(" -–—.")


def features(pages: list[tuple[list[Line], int, int]]) -> list[tuple[Line, list[float]]]:
    """Признаки всех строк документа (страницы по порядку)."""
    all_lines = [line for lines, _, _ in pages for line in lines]
    doc_height = _median([line.font_height for line in all_lines])
    # Тексты у верхнего/нижнего края, повторяющиеся на разных страницах, — колонтитулы.
    margin_counts: dict[str, set[int]] = {}
    for lines, _, page_h in pages:
        for line in lines:
            if page_h and (line.top < page_h * 0.12 or line.bottom > page_h * 0.88):
                margin_counts.setdefault(_norm_margin_text(line.text), set()).add(line.page)
    page_count = max(len(pages), 1)

    result = []
    for lines, page_w, page_h in pages:
        if not lines:
            continue
        page_w = page_w or max(line.right for line in lines)
        page_h = page_h or max(line.bottom for line in lines)
        heights = [line.font_height for line in lines]
        body = _median(heights, doc_height)
        text_left = _median([line.left for line in lines], 0)
        text_left = min(text_left, min(line.left for line in lines if line.font_height <= body * 1.15) if lines else text_left)
        text_right = max(sorted(line.right for line in lines)[int(len(lines) * 0.9)], text_left + 1)
        text_width = max(text_right - text_left, 1)
        spacing = _median([b.top - a.bottom for a, b in zip(lines, lines[1:]) if 0 < b.top - a.bottom < body * 3],
                          body * 0.5)
        center = (text_left + text_right) / 2
        for i, line in enumerate(lines):
            prev = lines[i - 1] if i > 0 else None
            nxt = lines[i + 1] if i + 1 < len(lines) else None
            text = line.text.strip()
            words = text.split()
            letters = [ch for ch in text if ch.isalpha()]
            margin_repeat = len(margin_counts.get(_norm_margin_text(text), ())) if page_count > 1 else 0
            gap_above = ((line.top - prev.bottom) / body) if prev else 3.0
            gap_below = ((nxt.top - line.bottom) / body) if nxt else 3.0

            def h_rel(other):
                return other.font_height / body if other else 1.0

            def left_rel(other):
                return (other.left - text_left) / text_width if other else 0.0

            def ends_sentence(other):
                return float(bool(other) and other.text.rstrip()[-1:] in ".!?:;")

            values = [
                line.font_height / body,
                line.font_height / doc_height,
                (line.left - text_left) / text_width,
                (text_right - line.right) / text_width,
                line.width / text_width,
                abs((line.left + line.right) / 2 - center) / text_width,
                min(gap_above, 6.0),
                min(gap_below, 6.0),
                (gap_above - spacing / body),
                line.top / page_h,
                line.bottom / page_h,
                math.log1p(len(words)),
                math.log1p(len(text)),
                float(bool(_BULLET.match(text))),
                float(bool(_NUMBERED.match(text))),
                float(bool(_SECTION.match(text))),
                float(bool(_PAGE_NUMBER.match(text))),
                float(text[-1:] in ".!?"),
                float(text[-1:] == ":"),
                float(text[-1:] in "-‐"),
                (sum(ch.isupper() for ch in letters) / len(letters)) if letters else 0.0,
                float(bool(text[:1]) and text[:1].isupper()),
                float(bool(text[:1]) and text[:1].islower()),
                min(margin_repeat, 5) / 5.0,
                float(margin_repeat >= 2),
                line.conf / 100.0,
                h_rel(prev), h_rel(nxt),
                left_rel(prev), left_rel(nxt),
                (line.left - prev.left) / text_width if prev else 0.0,
                ends_sentence(prev),
                float(bool(prev) and bool(_BULLET.match(prev.text) or _NUMBERED.match(prev.text))),
                float(prev is None),
                float(nxt is None),
                float(line.page == 0),
                float(bool(JUNK_MARKER.match(text))),
            ]
            result.append((line, values))
    return result


FEATURE_COUNT = 37
