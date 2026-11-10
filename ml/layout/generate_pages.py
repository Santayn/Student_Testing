"""Синтетические страницы лекций для модели разметки сканов.

Документ собирается из случайной структуры (заголовки трёх уровней, абзацы, маркированные и нумерованные
списки, колонтитул, номер страницы) с текстом из набора вопросов/эталонов. Страницы рисуются разными
шрифтами и размерами, с лёгким наклоном, размытием и шумом; для каждой нарисованной строки записывается
её роль — это и есть разметка, руками ничего размечать не нужно.

Запуск: python -m ml.layout.generate_pages --docs 400
"""
from __future__ import annotations

import argparse
import json
import random
import re
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

from ml.common.groq_client import data_home

LAYOUT_DIR = data_home() / "data" / "layout"
FONTS_DIR = Path("C:/Windows/Fonts")
FONT_PAIRS = [
    ("times.ttf", "timesbd.ttf"), ("arial.ttf", "arialbd.ttf"), ("calibri.ttf", "calibrib.ttf"),
    ("georgia.ttf", "georgiab.ttf"), ("verdana.ttf", "verdanab.ttf"), ("tahoma.ttf", "tahomabd.ttf"),
    ("pala.ttf", "palab.ttf"), ("BOOKOS.TTF", "BOOKOSB.TTF"), ("GARA.TTF", "GARABD.TTF"),
    ("segoeui.ttf", "segoeuib.ttf"),
]
BULLETS = ["•", "–", "-", "●", "▪"]


def load_corpus() -> dict:
    raw = data_home() / "data" / "grading" / "raw.jsonl"
    sentences, headings, disciplines = [], [], set()
    for line in raw.read_text(encoding="utf-8").splitlines():
        item = json.loads(line)
        disciplines.add(item["discipline"])
        headings.append(item["topic"])
        for text in [item["reference"]] + [a["text"] for a in item["answers"] if a.get("kind") in ("correct", "verbose")]:
            for sentence in re.split(r"(?<=[.!?])\s+", text):
                sentence = sentence.strip()
                if 20 <= len(sentence) <= 260 and not re.search(r"[A-Za-z]{12,}", sentence):
                    sentences.append(sentence)
        for criterion in item["criteria"]:
            text = criterion["text"].strip().rstrip(".")
            if 8 <= len(text) <= 120:
                headings.append(text)
    return {"sentences": sentences, "headings": headings, "disciplines": sorted(disciplines)}


def build_document(rng: random.Random, corpus: dict) -> list[tuple[str, str]]:
    """[(тип блока, текст)]: H1/H2/H3/P/UL/OL (элементы списка — отдельными блоками LI с номером)."""
    blocks = [("H1", rng.choice(corpus["headings"]))]
    numbered_headings = rng.random() < 0.4

    def paragraph():
        return " ".join(rng.choice(corpus["sentences"]) for _ in range(rng.randint(1, 5)))

    for section in range(1, rng.randint(2, 5) + 1):
        title = rng.choice(corpus["headings"])
        blocks.append(("H2", f"{section}. {title}" if numbered_headings else title))
        for sub in range(1, rng.randint(0, 3) + 1):
            for _ in range(rng.randint(1, 3)):
                kind = rng.random()
                if kind < 0.6:
                    blocks.append(("P", paragraph()))
                else:
                    ordered = kind > 0.82
                    style = rng.choice(["1.", "1)", "а)"]) if ordered else rng.choice(BULLETS)
                    for index in range(rng.randint(2, 6)):
                        text = rng.choice(corpus["sentences"] if rng.random() < 0.7 else corpus["headings"])
                        if ordered:
                            marker = (f"{index + 1}." if style == "1." else f"{index + 1})" if style == "1)"
                                      else f"{'абвгдежзик'[index]})")
                        else:
                            marker = style
                        blocks.append(("LI", f"{marker} {text}"))
            if rng.random() < 0.55:
                sub_title = rng.choice(corpus["headings"])
                blocks.append(("H3", f"{section}.{sub} {sub_title}" if numbered_headings else sub_title))
        blocks.append(("P", paragraph()))
    return blocks


class Renderer:
    def __init__(self, rng: random.Random, corpus: dict):
        self.rng = rng
        dpi = rng.choice([150, 170, 200, 220])
        self.scale = dpi / 72.0
        self.width, self.height = int(8.27 * dpi), int(11.69 * dpi)
        regular, bold = rng.choice(FONT_PAIRS)
        self.regular, self.bold = str(FONTS_DIR / regular), str(FONTS_DIR / bold)
        self.body_pt = rng.choice([10, 11, 12, 12, 13, 14])
        # Иногда заголовки того же кегля, что и текст, — только жирные (сложный случай).
        same_size = rng.random() < 0.2
        self.sizes = {
            "H1": self.body_pt if same_size else self.body_pt + rng.choice([4, 6, 8]),
            "H2": self.body_pt if same_size else self.body_pt + rng.choice([2, 3, 4]),
            "H3": self.body_pt if same_size else self.body_pt + rng.choice([0, 1, 2]),
        }
        self.margin_x = int(self.width * rng.uniform(0.08, 0.14))
        self.margin_top = int(self.height * rng.uniform(0.07, 0.1))
        self.margin_bottom = int(self.height * rng.uniform(0.07, 0.1))
        self.first_indent = rng.random() < 0.5
        self.para_gap = rng.uniform(0.2, 0.9)
        self.line_spacing = rng.uniform(1.15, 1.6)
        self.center_h1 = rng.random() < 0.5
        self.list_indent = rng.uniform(0.0, 1.8)
        self.header_text = rng.choice(corpus["disciplines"]) + rng.choice(["", ". Курс лекций", ". Конспект лекций", ""])
        self.has_header = rng.random() < 0.6
        self.page_number_style = rng.choice(["center", "right", "dash", "none"])
        self.pages: list[tuple[Image.Image, list[dict]]] = []
        self._new_page()

    def font(self, kind: str):
        size = self.sizes.get(kind, self.body_pt)
        path = self.bold if kind in ("H1", "H2", "H3") else self.regular
        return ImageFont.truetype(path, max(8, int(size * self.scale)))

    def _new_page(self):
        image = Image.new("L", (self.width, self.height), 255)
        self.draw = ImageDraw.Draw(image)
        self.lines: list[dict] = []
        self.pages.append((image, self.lines))
        self.y = self.margin_top
        number = len(self.pages)
        small = ImageFont.truetype(self.regular, max(8, int((self.body_pt - 2) * self.scale)))
        if self.has_header and number > 1:
            self._text_line(self.header_text, small, self.margin_x, int(self.margin_top * 0.45), "MARGIN")
        if self.page_number_style != "none":
            label = f"– {number} –" if self.page_number_style == "dash" else str(number)
            box = self.draw.textbbox((0, 0), label, font=small)
            w = box[2] - box[0]
            x = (self.width - w) // 2 if self.page_number_style != "right" else self.width - self.margin_x - w
            self._text_line(label, small, x, self.height - int(self.margin_bottom * 0.6), "MARGIN")

    def _text_line(self, text, font, x, y, label):
        self.draw.text((x, y), text, font=font, fill=0)
        box = self.draw.textbbox((x, y), text, font=font)
        self.lines.append({"label": label, "text": text, "box": list(box)})

    def _wrap(self, text, font, width, first_indent=0):
        words, lines, current = text.split(), [], ""
        limit = width - first_indent
        for word in words:
            candidate = (current + " " + word).strip()
            if self.draw.textlength(candidate, font=font) <= limit or not current:
                current = candidate
            else:
                lines.append(current)
                current, limit = word, width
        if current:
            lines.append(current)
        return lines

    def block(self, kind: str, text: str):
        font = self.font(kind)
        line_h = int(font.size * self.line_spacing)
        width = self.width - 2 * self.margin_x
        indent_list = int(font.size * self.list_indent)
        if kind in ("H1", "H2", "H3"):
            self.y += int(line_h * self.rng.uniform(0.6, 1.4))
            lines = self._wrap(text, font, width)
            labels = [kind] * len(lines)
            xs = []
            for line in lines:
                if kind == "H1" and self.center_h1:
                    xs.append(self.margin_x + int((width - self.draw.textlength(line, font=font)) / 2))
                else:
                    xs.append(self.margin_x)
        elif kind == "LI":
            marker, _, rest = text.partition(" ")
            hanging = int(self.draw.textlength(marker + " ", font=font))
            lines = self._wrap(text, font, width - indent_list - hanging)
            labels = ["LI_START"] + ["LI_CONT"] * (len(lines) - 1)
            xs = [self.margin_x + indent_list] + [self.margin_x + indent_list + hanging] * (len(lines) - 1)
        else:
            indent = int(font.size * 1.5) if self.first_indent else 0
            lines = self._wrap(text, font, width, indent)
            labels = ["P_START"] + ["P_CONT"] * (len(lines) - 1)
            xs = [self.margin_x + indent] + [self.margin_x] * (len(lines) - 1)
            self.y += int(line_h * self.para_gap)
        for line, label, x in zip(lines, labels, xs):
            if self.y + line_h > self.height - self.margin_bottom:
                self._new_page()
                if label in ("H1", "H2", "H3"):
                    pass
            self._text_line(line, font, x, self.y, label)
            self.y += line_h
        if kind in ("H1", "H2", "H3"):
            self.y += int(line_h * self.rng.uniform(0.1, 0.5))


def degrade(image: Image.Image, rng: random.Random) -> Image.Image:
    if rng.random() < 0.6:
        image = image.rotate(rng.uniform(-0.6, 0.6), resample=Image.BICUBIC, fillcolor=255)
    if rng.random() < 0.5:
        image = image.filter(ImageFilter.GaussianBlur(rng.uniform(0.3, 0.9)))
    if rng.random() < 0.5:
        noise = Image.effect_noise(image.size, rng.uniform(8, 25))
        image = Image.blend(image, noise, rng.uniform(0.04, 0.12))
    return image


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--docs", type=int, default=400)
    parser.add_argument("--seed", type=int, default=1)
    args = parser.parse_args()
    sys.stdout.reconfigure(encoding="utf-8")
    pages_dir = LAYOUT_DIR / "pages"
    pages_dir.mkdir(parents=True, exist_ok=True)
    corpus = load_corpus()
    print(f"Корпус: {len(corpus['sentences'])} предложений, {len(corpus['headings'])} заголовков", flush=True)
    manifest = []
    for doc in range(args.docs):
        rng = random.Random(args.seed * 100000 + doc)
        renderer = Renderer(rng, corpus)
        for kind, text in build_document(rng, corpus):
            renderer.block(kind, text)
        for page_index, (image, lines) in enumerate(renderer.pages):
            if not any(line["label"] != "MARGIN" for line in lines):
                continue
            name = f"d{doc:04d}_p{page_index:02d}"
            degrade(image, rng).convert("L").save(pages_dir / f"{name}.png", optimize=True)
            manifest.append({"name": name, "doc": doc, "page": page_index, "lines": lines})
        if (doc + 1) % 50 == 0:
            print(f"документов {doc + 1}, страниц {len(manifest)}", flush=True)
    (LAYOUT_DIR / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False), encoding="utf-8")
    print(f"Готово: {len(manifest)} страниц")


if __name__ == "__main__":
    main()
