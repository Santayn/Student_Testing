"""Своя разметка сканов лекций без LLM: Tesseract → классификатор строк (ONNX) → Markdown по правилам."""
from __future__ import annotations

import json
import os
import re
import subprocess
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

import numpy as np
import onnxruntime as ort

from .layout_features import FEATURE_COUNT, JUNK_MARKER, LABELS, Line, features, parse_tsv

# Те же параметры, что при обучении (ml/layout/ocr_pages.py).
TESSERACT_ARGS = ["-l", "rus+eng", "--psm", "3"]
PAGE_TIMEOUT_SECONDS = 120

_BULLET = re.compile(r"^[•●▪■◦○∙·\-–—*»]\s*")
_NUMBERED = re.compile(r"^(\d{1,2})[.)]\s+")
_LETTERED = re.compile(r"^([a-zа-я])\)\s+", re.IGNORECASE)


class LayoutModel:
    def __init__(self, model_dir: Path):
        self.meta = json.loads((model_dir / "meta.json").read_text(encoding="utf-8"))
        self.name = self.meta.get("name", model_dir.name)
        if self.meta.get("features") != FEATURE_COUNT or self.meta.get("labels") != LABELS:
            raise ValueError("Модель обучена на другом наборе признаков — переобучите её.")
        options = ort.SessionOptions()
        options.intra_op_num_threads = int(os.environ.get("LOCAL_AI_THREADS", "2"))
        self.session = ort.InferenceSession(str(model_dir / "model.onnx"), options, providers=["CPUExecutionProvider"])
        self.input = self.session.get_inputs()[0].name

    def classify(self, values: list[list[float]]) -> list[str]:
        if not values:
            return []
        outputs = self.session.run(None, {self.input: np.array(values, dtype=np.float32)})
        return [LABELS[int(index)] for index in outputs[0]]

    def scan(self, images: list[bytes]) -> dict:
        workers = max(1, int(os.environ.get("LOCAL_AI_THREADS", "2")))
        with ThreadPoolExecutor(max_workers=workers) as pool:
            tsvs = list(pool.map(ocr_page, images))
        pages = [parse_tsv(tsv, index) for index, tsv in enumerate(tsvs)]
        rows = features(pages)
        labels = self.classify([values for _, values in rows])
        lines = [(line, label) for (line, _), label in zip(rows, labels)]
        return {
            "markdown": assemble(lines),
            "pages": len(images),
            "lines": len(lines),
            "engine": self.name,
        }


def ocr_page(image: bytes) -> str:
    result = subprocess.run(
        ["tesseract", "stdin", "stdout", *TESSERACT_ARGS, "tsv"],
        input=image, capture_output=True, timeout=PAGE_TIMEOUT_SECONDS, check=False,
    )
    if result.returncode != 0:
        raise RuntimeError("tesseract не смог распознать страницу")
    return result.stdout.decode("utf-8", errors="replace")


def _join(text: str, addition: str) -> str:
    """Склеивает строки абзаца, убирая перенос слова по слогам."""
    if text.endswith("-") and addition[:1].islower():
        return text[:-1] + addition
    return f"{text} {addition}" if text else addition


def _list_item(text: str) -> str:
    numbered = _NUMBERED.match(text)
    if not numbered and not _LETTERED.match(text):
        text = JUNK_MARKER.sub("", text, count=1)
    if numbered:
        return f"{numbered.group(1)}. {text[numbered.end():].strip()}"
    lettered = _LETTERED.match(text)
    if lettered:
        return f"- {lettered.group(1)}) {text[lettered.end():].strip()}"
    return "- " + _BULLET.sub("", text, count=1).strip()


def assemble(lines: list[tuple[Line, str]]) -> str:
    blocks: list[list[str]] = []  # [вид, текст]
    previous: Line | None = None
    for line, label in lines:
        text = line.text.strip()
        if not text or label == "MARGIN":
            continue
        last = blocks[-1] if blocks else None
        if label in ("H1", "H2", "H3"):
            # Строки заголовка, идущие вплотную, — один заголовок, даже если модель дала им разный уровень.
            close = previous is not None and previous.page == line.page and                 line.top - previous.bottom < 0.8 * max(line.font_height, previous.font_height)
            if last and last[0] in ("H1", "H2", "H3") and (last[0] == label or close):
                last[0] = min(last[0], label)  # «H1» < «H2»: берём более крупный уровень
                last[1] = _join(last[1], text)
            else:
                blocks.append([label, text])
        elif label == "LI_START":
            blocks.append(["LI", _list_item(text)])
        elif label == "LI_CONT" and last and last[0] == "LI":
            last[1] = _join(last[1], text)
        elif label == "P_CONT" and last and last[0] in ("P", "LI"):
            # Продолжение абзаца — в том числе на следующей странице.
            last[1] = _join(last[1], text)
        else:
            blocks.append(["P", text])
        previous = line

    prefix = {"H1": "# ", "H2": "## ", "H3": "### "}
    out: list[str] = []
    for index, (kind, text) in enumerate(blocks):
        if kind == "LI":
            # Пункты одного списка идут подряд без пустых строк.
            if out and index > 0 and blocks[index - 1][0] == "LI":
                out.append(text)
            else:
                out.extend(["", text] if out else [text])
            continue
        if out:
            out.append("")
        out.append(prefix.get(kind, "") + text)
    return "\n".join(out).strip()
