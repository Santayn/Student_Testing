"""Сервис собственных моделей ИИ (работает на процессоре, без доступа в интернет).

Модели лежат в LOCAL_AI_MODELS_DIR (по умолчанию /models):
  grader/   — проверка свободных ответов (ONNX + tokenizer.json + meta.json);
  layout/   — разметка сканов лекций (ONNX + meta.json), распознавание — Tesseract в этом же образе.
Сервис стартует и без моделей: /health показывает, какие готовы.
"""
from __future__ import annotations

import base64
import binascii
import logging
import os
import threading
from pathlib import Path

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from .grader import Grader
from .layout import LayoutModel

LOGGER = logging.getLogger("local-ai")
MODELS_DIR = Path(os.environ.get("LOCAL_AI_MODELS_DIR", "/models"))
MAX_PAGES = 60

app = FastAPI(title="Student Testing — собственные модели ИИ")


class _Lazy:
    """Модель загружается при первом обращении; ошибка загрузки запоминается и видна в /health."""

    def __init__(self, folder: str, factory):
        self.folder = folder
        self.factory = factory
        self.model = None
        self.error: str | None = None
        self.lock = threading.Lock()

    def get(self):
        with self.lock:
            if self.model is None and self.error is None:
                directory = MODELS_DIR / self.folder
                try:
                    self.model = self.factory(directory)
                    LOGGER.info("Модель %s загружена: %s", self.folder, self.model.name)
                except FileNotFoundError:
                    self.error = f"Модель не найдена: {directory}"
                except Exception as error:  # noqa: BLE001
                    self.error = f"Модель не загрузилась: {error}"
                    LOGGER.exception("%s load failed", self.folder)
            return self.model

    def status(self):
        model = self.get()
        return {
            "ready": model is not None,
            "name": model.name if model else None,
            "error": self.error,
            "metrics": model.meta.get("test") if model else None,
        }


_grader = _Lazy("grader", Grader)
_layout = _Lazy("layout", LayoutModel)


class GradeRequest(BaseModel):
    question: str = Field(max_length=4000)
    reference: str | None = Field(default=None, max_length=8000)
    criteria: str | None = Field(default=None, max_length=8000)
    points: float = 1.0
    answer: str = Field(default="", max_length=8000)


class ScanRequest(BaseModel):
    """Страницы — JPEG/PNG в base64, по порядку."""
    pages: list[str] = Field(min_length=1, max_length=MAX_PAGES)


@app.get("/health")
def health():
    return {"grader": _grader.status(), "layout": _layout.status()}


@app.post("/v1/grade")
def grade(request: GradeRequest):
    model = _grader.get()
    if model is None:
        raise HTTPException(status_code=503, detail=_grader.error or "Модель проверки ответов не загружена.")
    return model.grade(request.question, request.reference, request.criteria, request.points, request.answer)


@app.post("/v1/scan")
def scan(request: ScanRequest):
    model = _layout.get()
    if model is None:
        raise HTTPException(status_code=503, detail=_layout.error or "Модель разметки сканов не загружена.")
    try:
        images = [base64.b64decode(page, validate=True) for page in request.pages]
    except (binascii.Error, ValueError) as error:
        raise HTTPException(status_code=400, detail="Страница передана не в base64.") from error
    try:
        return model.scan(images)
    except RuntimeError as error:
        raise HTTPException(status_code=422, detail=str(error)) from error
