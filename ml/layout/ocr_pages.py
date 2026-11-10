"""Распознаёт синтетические страницы тем же Tesseract, что работает в сервисе local-ai (образ
student-test-local-ai), и складывает TSV рядом. Уже распознанные страницы пропускаются.

Запуск: python -m ml.layout.ocr_pages [--workers 6]
"""
from __future__ import annotations

import argparse
import subprocess
import sys

from ml.common.groq_client import data_home

LAYOUT_DIR = data_home() / "data" / "layout"
IMAGE = "student-test-local-ai:latest"
# Те же параметры, что в local-ai/app/layout.py.
TESSERACT_ARGS = "-l rus+eng --psm 3"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--workers", type=int, default=6)
    args = parser.parse_args()
    sys.stdout.reconfigure(encoding="utf-8")
    pages = LAYOUT_DIR / "pages"
    tsv = LAYOUT_DIR / "tsv"
    tsv.mkdir(exist_ok=True)
    todo = [p.stem for p in sorted(pages.glob("*.png")) if not (tsv / f"{p.stem}.tsv").exists()]
    print(f"Распознать: {len(todo)} страниц", flush=True)
    if not todo:
        return
    # newline="\n": на Windows иначе получится CRLF, и xargs в контейнере не найдёт файлы.
    (LAYOUT_DIR / "todo.txt").write_text("\n".join(todo) + "\n", encoding="utf-8", newline="\n")
    script = (
        f"cd /data && xargs -P {args.workers} -I{{}} sh -c "
        f"'tesseract pages/{{}}.png tsv/{{}} {TESSERACT_ARGS} tsv >/dev/null 2>&1' < todo.txt"
    )
    subprocess.run(
        ["docker", "run", "--rm", "-v", f"{LAYOUT_DIR.as_posix()}:/data", "--user", "0",
         "--entrypoint", "sh", IMAGE, "-c", script],
        check=True,
    )
    done = sum(1 for name in todo if (tsv / f"{name}.tsv").exists())
    print(f"Готово: {done} из {len(todo)}")


if __name__ == "__main__":
    main()
