"""Обучение классификатора строк для разметки сканов и экспорт в ONNX.

Модель — небольшая полносвязная сеть (MLP) на нормированных признаках: по точности почти не уступает
градиентному бустингу (96,9% против 97,3% строк), но надёжно экспортируется в ONNX (skl2onnx не справляется
с бустингом новых версий scikit-learn) и в сервисе требует только onnxruntime.

Строки Tesseract сопоставляются с эталонными (нарисованными генератором) по пересечению рамок,
признаки считаются тем же кодом, что в сервисе (local-ai/app/layout_features.py).

Запуск: python -m ml.layout.train_layout [--name layout-v1]
"""
from __future__ import annotations

import argparse
import json
import shutil
import sys
from collections import Counter, defaultdict
from pathlib import Path

import numpy as np
from sklearn.metrics import classification_report, confusion_matrix
from sklearn.neural_network import MLPClassifier
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler

from ml.common.groq_client import data_home

REPO_ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO_ROOT / "local-ai"))
from app.layout_features import FEATURE_COUNT, LABELS, features, parse_tsv  # noqa: E402

LAYOUT_DIR = data_home() / "data" / "layout"
MODELS_DIR = data_home() / "models"


def match_label(line, truth: list[dict]) -> str | None:
    best, best_score = None, 0.0
    for item in truth:
        x0, y0, x1, y1 = item["box"]
        overlap_y = min(line.bottom, y1) - max(line.top, y0)
        overlap_x = min(line.right, x1) - max(line.left, x0)
        if overlap_y <= 0 or overlap_x <= 0:
            continue
        score = overlap_y / max(min(line.height, y1 - y0), 1) * min(1.0, overlap_x / max(line.width, 1))
        if score > best_score:
            best, best_score = item["label"], score
    return best if best_score >= 0.4 else None


def load_documents():
    manifest = json.loads((LAYOUT_DIR / "manifest.json").read_text(encoding="utf-8"))
    by_doc = defaultdict(list)
    for page in manifest:
        by_doc[page["doc"]].append(page)
    documents = []
    for doc, pages in sorted(by_doc.items()):
        parsed, truths = [], []
        for page in sorted(pages, key=lambda p: p["page"]):
            tsv = LAYOUT_DIR / "tsv" / f"{page['name']}.tsv"
            if not tsv.exists():
                break
            lines, width, height = parse_tsv(tsv.read_text(encoding="utf-8"), page["page"])
            parsed.append((lines, width, height))
            truths.append(page["lines"])
        else:
            documents.append((doc, parsed, truths))
    return documents


def build(documents):
    X, y, docs = [], [], []
    unmatched = 0
    for doc, parsed, truths in documents:
        truth_by_page = {page_index: truth for page_index, truth in enumerate(truths)}
        for line, values in features(parsed):
            label = match_label(line, truth_by_page.get(line.page, []))
            if label is None:
                unmatched += 1
                continue
            X.append(values)
            y.append(LABELS.index(label))
            docs.append(doc)
    return np.array(X, dtype=np.float32), np.array(y), np.array(docs), unmatched


def export(model, out_dir: Path):
    from skl2onnx import to_onnx

    onx = to_onnx(model, np.zeros((1, FEATURE_COUNT), dtype=np.float32), target_opset=17,
                  options={id(model.steps[-1][1]): {"zipmap": False}})
    (out_dir / "model.onnx").write_bytes(onx.SerializeToString())


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--name", default="layout-v1")
    args = parser.parse_args()
    sys.stdout.reconfigure(encoding="utf-8")
    documents = load_documents()
    X, y, docs, unmatched = build(documents)
    assert X.shape[1] == FEATURE_COUNT, X.shape
    print(f"Документов {len(documents)}, строк {len(y)}, не сопоставлено {unmatched}", flush=True)
    print("Классы:", {LABELS[k]: v for k, v in sorted(Counter(y).items())}, flush=True)

    test_mask = (docs % 10) == 0
    model = make_pipeline(
        StandardScaler(),
        MLPClassifier(hidden_layer_sizes=(128, 64), alpha=1e-4, max_iter=300, early_stopping=True, random_state=7),
    )
    model.fit(X[~test_mask], y[~test_mask])
    predicted = model.predict(X[test_mask])
    accuracy = float((predicted == y[test_mask]).mean())
    present = sorted(set(y[test_mask]) | set(predicted))
    print(f"ТЕСТ: точность по строкам {accuracy:.4f}")
    print(classification_report(y[test_mask], predicted, labels=present,
                                target_names=[LABELS[i] for i in present], digits=3))
    print("Матрица ошибок:\n", confusion_matrix(y[test_mask], predicted, labels=present))

    report = classification_report(y[test_mask], predicted, labels=present,
                                   target_names=[LABELS[i] for i in present], output_dict=True)
    out_dir = MODELS_DIR / args.name
    if out_dir.exists():
        shutil.rmtree(out_dir)
    out_dir.mkdir(parents=True)
    export(model, out_dir)
    (out_dir / "meta.json").write_text(json.dumps({
        "name": args.name, "labels": LABELS, "features": FEATURE_COUNT,
        "test": {"line_accuracy": round(accuracy, 4),
                 "per_label_f1": {k: round(v["f1-score"], 3) for k, v in report.items() if k in LABELS}},
        "trained_on": {"documents": len(documents), "lines": int(len(y))},
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Модель сохранена: {out_dir}")


if __name__ == "__main__":
    main()
