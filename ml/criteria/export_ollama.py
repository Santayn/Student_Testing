"""Экспорт дообученной модели критериев в Ollama и в переносимый GGUF для сервера.

Подводные камни, из-за которых модель «пишет мусор»:
  * transformers 5 сохраняет конфиг и токенизатор в новом формате — импорт Ollama их не понимает,
    поэтому конфиг и файлы токенизатора берутся из исходной модели (веса — дообученные);
  * Ollama не всегда узнаёт шаблон чата Qwen — задаём ChatML явно.

Запуск: python -m ml.criteria.export_ollama --name criteria-0.5b [--tag st-criteria]
Результат: models/<name>/ollama/{st-criteria.gguf, Modelfile} — кладётся на сервер в local-ai/models/criteria/.
"""
from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import sys
from pathlib import Path

from huggingface_hub import snapshot_download

from ml.common.groq_client import data_home

MODELS_DIR = data_home() / "models"
TOKENIZER_FILES = ["config.json", "generation_config.json", "tokenizer.json", "tokenizer_config.json",
                   "vocab.json", "merges.txt"]
TEMPLATE = '''TEMPLATE """{{- range .Messages }}<|im_start|>{{ .Role }}
{{ .Content }}<|im_end|>
{{ end }}<|im_start|>assistant
"""
PARAMETER stop <|im_end|>
PARAMETER stop <|endoftext|>
PARAMETER temperature 0.2
PARAMETER num_ctx 2048
'''


def ollama(*args: str) -> str:
    result = subprocess.run(["ollama", *args], capture_output=True, text=True, encoding="utf-8", errors="replace")
    if result.returncode != 0:
        raise SystemExit(f"ollama {' '.join(args)}: {result.stderr.strip()[-500:]}")
    return result.stdout


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--name", default="criteria-0.5b")
    parser.add_argument("--tag", default="st-criteria")
    parser.add_argument("--quantize", default="q4_K_M")
    args = parser.parse_args()
    sys.stdout.reconfigure(encoding="utf-8")

    model_dir = MODELS_DIR / args.name
    meta = json.loads((model_dir / "meta.json").read_text(encoding="utf-8"))
    base = Path(snapshot_download(meta["base"], allow_patterns=TOKENIZER_FILES))

    staged = model_dir / "hf-ollama"
    if staged.exists():
        shutil.rmtree(staged)
    staged.mkdir()
    for weights in (model_dir / "hf").glob("*.safetensors"):
        shutil.copy2(weights, staged / weights.name)
    for name in TOKENIZER_FILES:
        if (base / name).exists():
            shutil.copy2(base / name, staged / name)

    build_file = model_dir / "Modelfile.build"
    build_file.write_text(f"FROM {staged.as_posix()}\n{TEMPLATE}", encoding="utf-8")
    print(ollama("create", args.tag, "--quantize", args.quantize, "-f", str(build_file))[-200:])

    # Сжатые веса — блоб Ollama; копируем его как переносимый GGUF для сервера.
    modelfile = ollama("show", args.tag, "--modelfile")
    blob = next((Path(m.group(1).strip()) for m in re.finditer(r"^FROM (.+)$", modelfile, re.MULTILINE)
                 if "sha256" in m.group(1)), None)
    if blob is None or not blob.exists():
        raise SystemExit("Не найден файл весов в Ollama")
    out_dir = model_dir / "ollama"
    out_dir.mkdir(exist_ok=True)
    shutil.copy2(blob, out_dir / f"{args.tag}.gguf")
    (out_dir / "Modelfile").write_text(f"FROM ./{args.tag}.gguf\n{TEMPLATE}", encoding="utf-8")
    shutil.rmtree(staged)
    size = (out_dir / f"{args.tag}.gguf").stat().st_size / 1e6
    print(f"Готово: {out_dir} ({size:.0f} МБ). Модель «{args.tag}» доступна в локальной Ollama.")


if __name__ == "__main__":
    main()
