"""Обучение своей модели-оценщика свободных ответов и экспорт в ONNX для работы на процессоре.

Модель: русский энкодер + две головы на [CLS]:
  match      — критерий выполнен (пара «вопрос+критерий / ответ») или доля балла
               (пара «вопрос+эталон / ответ», мягкая метка);
  suspicious — ответ пытается повлиять на проверку.

Запуск: python -m ml.grading.train_scorer [--base cointegrated/rubert-tiny2] [--epochs 4] [--name grader-v1]
"""
from __future__ import annotations

import argparse
import hashlib
import json
import random
import shutil
import sys
import time
from dataclasses import dataclass
from pathlib import Path

import numpy as np
import torch
from torch import nn
from torch.utils.data import DataLoader, Dataset
from transformers import AutoModel, AutoTokenizer, get_linear_schedule_with_warmup

from ml.common.groq_client import data_home
from ml.grading.scorer_format import MAX_LENGTH, criterion_pair, reference_pair

DATA_FILE = data_home() / "data" / "grading" / "verified.jsonl"
MODELS_DIR = data_home() / "models"


def split_of(topic_id: str) -> str:
    bucket = int(hashlib.md5(topic_id.encode("utf-8")).hexdigest(), 16) % 10
    return "test" if bucket == 0 else "val" if bucket == 1 else "train"


@dataclass
class Example:
    text_a: str
    text_b: str
    match: float
    suspicious: float
    kind: str  # criterion | reference


def build_examples(items: list[dict]) -> list[Example]:
    examples = []
    for item in items:
        criteria = item["criteria"]
        total = sum(float(c.get("points") or 1) for c in criteria) or 1.0
        for answer in item["answers"]:
            text = answer["text"]
            suspicious = 1.0 if answer.get("injection") else 0.0
            for criterion, matched in zip(criteria, answer["matched"]):
                a, b = criterion_pair(item["question"], criterion["text"], text)
                examples.append(Example(a, b, 1.0 if matched else 0.0, suspicious, "criterion"))
            ratio = sum(float(c.get("points") or 1) for c, m in zip(criteria, answer["matched"]) if m) / total
            a, b = reference_pair(item["question"], item["reference"], text)
            examples.append(Example(a, b, ratio, suspicious, "reference"))
    return examples


def cross_negatives(items: list[dict], per_item: int, seed: int = 3) -> list[Example]:
    """Бесплатные отрицательные примеры: верный ответ на ДРУГОЙ вопрос не выполняет критерии этого."""
    rng = random.Random(seed)
    correct = [(item, a) for item in items for a in item["answers"] if a.get("kind") in ("correct", "verbose", "sloppy")]
    examples = []
    for item in items:
        for _ in range(per_item):
            other_item, other = rng.choice(correct)
            if other_item is item:
                continue
            criterion = rng.choice(item["criteria"])
            a, b = criterion_pair(item["question"], criterion["text"], other["text"])
            examples.append(Example(a, b, 0.0, 0.0, "criterion"))
            a, b = reference_pair(item["question"], item["reference"], other["text"])
            examples.append(Example(a, b, 0.0, 0.0, "reference"))
    return examples


class PairDataset(Dataset):
    def __init__(self, examples: list[Example]):
        self.examples = examples

    def __len__(self):
        return len(self.examples)

    def __getitem__(self, index):
        return self.examples[index]


class Scorer(nn.Module):
    def __init__(self, base: str):
        super().__init__()
        self.encoder = AutoModel.from_pretrained(base)
        hidden = self.encoder.config.hidden_size
        self.dropout = nn.Dropout(0.1)
        self.head = nn.Linear(hidden, 2)

    def forward(self, input_ids, attention_mask, token_type_ids=None):
        kwargs = {"input_ids": input_ids, "attention_mask": attention_mask}
        if token_type_ids is not None:
            kwargs["token_type_ids"] = token_type_ids
        cls = self.encoder(**kwargs).last_hidden_state[:, 0]
        return self.head(self.dropout(cls))  # [:, 0] — match, [:, 1] — suspicious


def make_collate(tokenizer):
    def collate(batch: list[Example]):
        encoded = tokenizer(
            [e.text_a for e in batch], [e.text_b for e in batch],
            truncation="only_first", max_length=MAX_LENGTH, padding=True, return_tensors="pt",
        )
        encoded["labels"] = torch.tensor([[e.match, e.suspicious] for e in batch], dtype=torch.float32)
        return encoded
    return collate


@torch.no_grad()
def predict(model, loader, device) -> np.ndarray:
    model.eval()
    outputs = []
    for batch in loader:
        labels = batch.pop("labels")
        batch = {k: v.to(device) for k, v in batch.items()}
        with torch.autocast(device_type=device.type, dtype=torch.float16, enabled=device.type == "cuda"):
            logits = model(**batch)
        outputs.append(torch.sigmoid(logits.float()).cpu().numpy())
    return np.concatenate(outputs) if outputs else np.zeros((0, 2))


def evaluate_items(items: list[dict], examples: list[Example], probs: np.ndarray) -> dict:
    """Метрики на уровне ответа: балл по критериям против разметки."""
    cursor = 0
    abs_errors, exact, within, crit_correct, crit_total = [], 0, 0, 0, 0
    susp_tp = susp_fp = susp_fn = 0
    answers_total = 0
    for item in items:
        criteria = item["criteria"]
        weights = [float(c.get("points") or 1) for c in criteria]
        total = sum(weights) or 1.0
        for answer in item["answers"]:
            n = len(criteria)
            p_crit = probs[cursor:cursor + n, 0]
            p_susp = probs[cursor:cursor + n + 1, 1].max()
            cursor += n + 1  # критерии + пара с эталоном
            predicted = [p >= 0.5 for p in p_crit]
            truth = [bool(m) for m in answer["matched"]]
            crit_correct += sum(int(a == b) for a, b in zip(predicted, truth))
            crit_total += n
            pred_score = sum(w for w, m in zip(weights, predicted) if m) / total
            true_score = sum(w for w, m in zip(weights, truth) if m) / total
            abs_errors.append(abs(pred_score - true_score))
            exact += int(predicted == truth)
            within += int(abs(pred_score - true_score) <= 0.25 + 1e-9)
            answers_total += 1
            is_susp, true_susp = p_susp >= 0.5, bool(answer.get("injection"))
            susp_tp += int(is_susp and true_susp)
            susp_fp += int(is_susp and not true_susp)
            susp_fn += int(not is_susp and true_susp)
    assert cursor == len(examples)
    precision = susp_tp / max(susp_tp + susp_fp, 1)
    recall = susp_tp / max(susp_tp + susp_fn, 1)
    return {
        "answers": answers_total,
        "criterion_accuracy": round(crit_correct / max(crit_total, 1), 4),
        "score_mae": round(float(np.mean(abs_errors)), 4) if abs_errors else None,
        "answer_exact": round(exact / max(answers_total, 1), 4),
        "score_within_0_25": round(within / max(answers_total, 1), 4),
        "suspicious_precision": round(precision, 4),
        "suspicious_recall": round(recall, 4),
    }


def export_onnx(model: Scorer, tokenizer, out_dir: Path):
    from onnxruntime.quantization import QuantType, quantize_dynamic

    model = model.cpu().eval()
    sample = tokenizer(["Вопрос: тест\nКритерий: пункт"], ["ответ"], return_tensors="pt")
    names = ["input_ids", "attention_mask"] + (["token_type_ids"] if "token_type_ids" in sample else [])
    inputs = tuple(sample[n] for n in names)
    fp32 = out_dir / "model-fp32.onnx"
    torch.onnx.export(
        model, inputs, str(fp32), input_names=names, output_names=["logits"],
        dynamic_axes={n: {0: "batch", 1: "seq"} for n in names} | {"logits": {0: "batch"}},
        opset_version=17, dynamo=False,
    )
    quantize_dynamic(str(fp32), str(out_dir / "model.onnx"), weight_type=QuantType.QInt8)
    fp32.unlink()
    return names


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", default="cointegrated/rubert-tiny2")
    parser.add_argument("--epochs", type=int, default=4)
    parser.add_argument("--batch", type=int, default=32)
    parser.add_argument("--lr", type=float, default=5e-5)
    parser.add_argument("--name", default="grader-v1")
    parser.add_argument("--cross-negatives", type=int, default=2)
    args = parser.parse_args()
    sys.stdout.reconfigure(encoding="utf-8")
    random.seed(13)
    torch.manual_seed(13)

    items = [json.loads(line) for line in DATA_FILE.read_text(encoding="utf-8").splitlines() if line.strip()]
    items = [item for item in items if item["answers"]]
    parts = {"train": [], "val": [], "test": []}
    for item in items:
        parts[split_of(item["topic_id"])].append(item)
    examples = {name: build_examples(part) for name, part in parts.items()}
    examples["train"] += cross_negatives(parts["train"], args.cross_negatives)
    print({name: (len(parts[name]), len(examples[name])) for name in parts}, flush=True)

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    tokenizer = AutoTokenizer.from_pretrained(args.base)
    model = Scorer(args.base).to(device)
    collate = make_collate(tokenizer)
    train_loader = DataLoader(PairDataset(examples["train"]), batch_size=args.batch, shuffle=True, collate_fn=collate)
    val_loader = DataLoader(PairDataset(examples["val"]), batch_size=128, collate_fn=collate)
    test_loader = DataLoader(PairDataset(examples["test"]), batch_size=128, collate_fn=collate)

    optimizer = torch.optim.AdamW(model.parameters(), lr=args.lr, weight_decay=0.01)
    steps = len(train_loader) * args.epochs
    scheduler = get_linear_schedule_with_warmup(optimizer, int(steps * 0.06), steps)
    scaler = torch.amp.GradScaler(enabled=device.type == "cuda")
    loss_fn = nn.BCEWithLogitsLoss(reduction="none")
    # Попыток обмана мало — поднимаем их вес в потере.
    susp_weight = torch.tensor([1.0, 3.0], device=device)

    best, best_state = None, None
    for epoch in range(args.epochs):
        model.train()
        started, total_loss = time.time(), 0.0
        for step, batch in enumerate(train_loader, 1):
            labels = batch.pop("labels").to(device)
            batch = {k: v.to(device) for k, v in batch.items()}
            with torch.autocast(device_type=device.type, dtype=torch.float16, enabled=device.type == "cuda"):
                logits = model(**batch)
            loss = (loss_fn(logits.float(), labels) * susp_weight).mean()
            optimizer.zero_grad()
            scaler.scale(loss).backward()
            scaler.unscale_(optimizer)
            nn.utils.clip_grad_norm_(model.parameters(), 1.0)
            scaler.step(optimizer)
            scaler.update()
            scheduler.step()
            total_loss += loss.item()
        metrics = evaluate_items(parts["val"], examples["val"], predict(model, val_loader, device))
        print(f"эпоха {epoch + 1}: loss {total_loss / len(train_loader):.4f}, val {metrics}, "
              f"{time.time() - started:.0f} c", flush=True)
        if best is None or metrics["score_mae"] < best["score_mae"]:
            best = metrics
            best_state = {k: v.detach().cpu().clone() for k, v in model.state_dict().items()}

    model.load_state_dict(best_state)
    test_metrics = evaluate_items(parts["test"], examples["test"], predict(model, test_loader, device))
    print("ТЕСТ:", test_metrics, flush=True)

    out_dir = MODELS_DIR / args.name
    if out_dir.exists():
        shutil.rmtree(out_dir)
    out_dir.mkdir(parents=True)
    names = export_onnx(model, tokenizer, out_dir)
    tokenizer.save_pretrained(out_dir)
    (out_dir / "meta.json").write_text(json.dumps({
        "name": args.name, "base": args.base, "max_length": MAX_LENGTH, "inputs": names,
        "outputs": ["match", "suspicious"], "val": best, "test": test_metrics,
        "trained_on": {k: len(v) for k, v in parts.items()},
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Модель сохранена: {out_dir}")


if __name__ == "__main__":
    main()
