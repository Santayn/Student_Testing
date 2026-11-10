"""Своя модель проверки свободных ответов: ONNX-энкодер на процессоре.

Каждый критерий проверяется отдельной парой «вопрос + критерий / ответ»; без критериев ответ сравнивается
с эталонами. Балл — доля баллов выполненных критериев, уверенность — насколько модель далека от 0,5
по самому сомнительному критерию.
"""
from __future__ import annotations

import json
import os
from pathlib import Path

import numpy as np
import onnxruntime as ort
from tokenizers import Tokenizer

from .scorer_format import MAX_LENGTH, criterion_pair, parse_criteria, reference_pair, reference_variants


# Насколько оценка по критериям может расходиться с оценкой по эталону, чтобы ей ещё доверять.
DISAGREEMENT_LIMIT = 0.25


class Grader:
    def __init__(self, model_dir: Path):
        self.meta = json.loads((model_dir / "meta.json").read_text(encoding="utf-8"))
        self.name = self.meta.get("name", model_dir.name)
        self.tokenizer = Tokenizer.from_file(str(model_dir / "tokenizer.json"))
        self.tokenizer.enable_truncation(max_length=int(self.meta.get("max_length", MAX_LENGTH)), strategy="only_first")
        pad_id = self.tokenizer.token_to_id("[PAD]") or 0
        self.tokenizer.enable_padding(pad_id=pad_id, pad_token="[PAD]")
        options = ort.SessionOptions()
        options.intra_op_num_threads = int(os.environ.get("LOCAL_AI_THREADS", "2"))
        options.inter_op_num_threads = 1
        self.session = ort.InferenceSession(str(model_dir / "model.onnx"), options, providers=["CPUExecutionProvider"])
        self.inputs = [i.name for i in self.session.get_inputs()]

    def _probabilities(self, pairs: list[tuple[str, str]]) -> np.ndarray:
        encodings = self.tokenizer.encode_batch(pairs)
        feed = {
            "input_ids": np.array([e.ids for e in encodings], dtype=np.int64),
            "attention_mask": np.array([e.attention_mask for e in encodings], dtype=np.int64),
        }
        if "token_type_ids" in self.inputs:
            feed["token_type_ids"] = np.array([e.type_ids for e in encodings], dtype=np.int64)
        logits = self.session.run(["logits"], feed)[0]
        return 1.0 / (1.0 + np.exp(-logits))

    def grade(self, question: str, reference: str | None, criteria: str | None, points: float, answer: str) -> dict:
        answer = (answer or "").strip()
        parsed = parse_criteria(criteria, points)
        disagreement = None
        if parsed:
            variants = reference_variants(reference)
            pairs = [criterion_pair(question, text, answer) for text, _ in parsed]
            pairs += [reference_pair(question, variant, answer) for variant in variants]
            all_probs = self._probabilities(pairs)
            probs = all_probs[:len(parsed)]
            matched_flags = [bool(p >= 0.5) for p in probs[:, 0]]
            total = sum(weight for _, weight in parsed) or 1.0
            score = sum(weight for (_, weight), ok in zip(parsed, matched_flags) if ok) / total
            confidence = float(np.min(np.abs(2 * probs[:, 0] - 1)))
            if variants:
                # Защита от уверенных ошибок: ответ сравнивается и с эталоном целиком. Если оценка
                # по критериям с ней расходится, решает преподаватель.
                reference_score = float(np.max(all_probs[len(parsed):, 0]))
                disagreement = abs(score - reference_score)
                if disagreement > DISAGREEMENT_LIMIT:
                    confidence = min(confidence, 0.3)
            probs = all_probs
            matched = [text for (text, _), ok in zip(parsed, matched_flags) if ok]
            missing = [text for (text, _), ok in zip(parsed, matched_flags) if not ok]
            criteria_details = [
                {"text": text, "points": weight, "probability": round(float(p), 4)}
                for (text, weight), p in zip(parsed, probs[:, 0])
            ]
        else:
            variants = reference_variants(reference) or [""]
            probs = self._probabilities([reference_pair(question, variant, answer) for variant in variants])
            best = int(np.argmax(probs[:, 0]))
            score = float(probs[best, 0])
            # Без критериев балл непрерывный: уверены в крайних значениях, середина — к преподавателю.
            confidence = float(abs(2 * score - 1))
            score = round(score * 20) / 20
            matched, missing, criteria_details = [], [], []
        suspicious_probability = float(np.max(probs[:, 1]))
        return {
            "score": round(float(score), 4),
            "confidence": round(confidence, 4),
            "matched": matched,
            "missing": missing,
            "feedback": feedback(score, matched, missing, bool(parsed)),
            "suspicious": suspicious_probability >= 0.5,
            "details": {
                "engine": self.name,
                "referenceDisagreement": round(disagreement, 4) if disagreement is not None else None,
                "criteria": criteria_details,
                "suspiciousProbability": round(suspicious_probability, 4),
            },
        }


def feedback(score: float, matched: list[str], missing: list[str], by_criteria: bool) -> str:
    if score >= 0.995:
        return "Ответ засчитан полностью."
    if not by_criteria:
        return "Ответ раскрыт частично." if score > 0 else "Ответ не соответствует эталону."
    if not matched:
        return "Ни один из ключевых пунктов ответа не раскрыт."
    return "Не хватает: " + "; ".join(item.rstrip(".") for item in missing) + "."
