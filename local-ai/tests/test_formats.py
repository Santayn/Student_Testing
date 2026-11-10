"""Юнит-тесты сервиса без моделей: разбор критериев, защита оценщика и сборка Markdown.

Запуск из каталога local-ai: python -m unittest discover tests
"""
import unittest

from app.layout import assemble
from app.layout_features import Line, features, parse_tsv
from app.scorer_format import parse_criteria, reference_variants


def line(text, top, page=0, height=20, left=100):
    return Line(page, text, left, top, 400, height, 90.0, [height])


class CriteriaParsingTests(unittest.TestCase):
    def test_points_and_bullets(self):
        parsed = parse_criteria("— Определение — 1 б.\n2) Пример (2 балла)\n• Вывод", 4)
        self.assertEqual(parsed, [("Определение", 1.0), ("Пример", 2.0), ("Вывод", 1.0)])

    def test_empty_and_variants(self):
        self.assertEqual(parse_criteria("  ", 2), [])
        self.assertEqual(reference_variants("ОЗУ | оперативная память\nRAM"), ["ОЗУ", "оперативная память", "RAM"])


class AssembleTests(unittest.TestCase):
    def test_blocks_lists_and_margins(self):
        markdown = assemble([
            (line("Курс лекций", 10), "MARGIN"),
            (line("Индексы", 60, height=30), "H1"),
            (line("Индекс ускоряет поиск строк в таб-", 120), "P_START"),
            (line("лице и занимает место.", 150), "P_CONT"),
            (line("® B-дерево — диапазоны;", 190), "LI_START"),
            (line("e хеш — только равенство.", 220), "LI_START"),
            (line("1) Создать индекс.", 250), "LI_START"),
            (line("12", 900), "MARGIN"),
        ])
        self.assertEqual(markdown, "# Индексы\n\nИндекс ускоряет поиск строк в таблице и занимает место.\n\n"
                                   "- B-дерево — диапазоны;\n- хеш — только равенство.\n1. Создать индекс.")

    def test_heading_split_across_lines_is_one_heading(self):
        markdown = assemble([
            (line("Усталостные нагрузки вызывают", 100, height=24), "H3"),
            (line("разрушение материала", 128, height=24), "H2"),
            (line("Текст.", 200), "P_START"),
        ])
        self.assertEqual(markdown.splitlines()[0], "## Усталостные нагрузки вызывают разрушение материала")

    def test_russian_single_letter_word_is_kept(self):
        self.assertEqual(assemble([(line("В активном режиме сервер ждёт", 100), "LI_START")]),
                         "- В активном режиме сервер ждёт")

    def test_paragraph_continues_on_next_page(self):
        markdown = assemble([
            (line("Абзац начинается", 1500, page=0), "P_START"),
            (line("и продолжается.", 100, page=1), "P_CONT"),
        ])
        self.assertEqual(markdown, "Абзац начинается и продолжается.")


class FeatureTests(unittest.TestCase):
    TSV = ("level\tpage_num\tblock_num\tpar_num\tline_num\tword_num\tleft\ttop\twidth\theight\tconf\ttext\n"
           "1\t1\t0\t0\t0\t0\t0\t0\t1000\t1400\t-1\t\n"
           "5\t1\t1\t1\t1\t1\t100\t100\t200\t40\t95\tЗаголовок\n"
           "5\t1\t1\t2\t1\t1\t100\t200\t300\t20\t95\tПервая\n"
           "5\t1\t1\t2\t1\t2\t420\t200\t200\t20\t95\tстрока.\n")

    def test_tsv_lines_and_feature_vector(self):
        lines, width, height = parse_tsv(self.TSV, 0)
        self.assertEqual((width, height), (1000, 1400))
        self.assertEqual([l.text for l in lines], ["Заголовок", "Первая строка."])
        rows = features([(lines, width, height)])
        from app.layout_features import FEATURE_COUNT
        self.assertTrue(all(len(values) == FEATURE_COUNT for _, values in rows))
        self.assertGreater(rows[0][1][0], rows[1][1][0])  # заголовок крупнее текста


if __name__ == "__main__":
    unittest.main()


class FakeGrader:
    """Grader без модели: вероятности задаются тестом."""

    def __init__(self, criteria_probs, reference_prob):
        from app.grader import Grader
        self.grade = Grader.grade.__get__(self)
        self.name = "fake"
        self._rows = [[p, 0.0] for p in criteria_probs] + [[reference_prob, 0.0]]

    def _probabilities(self, pairs):
        import numpy as np
        return np.array(self._rows[:len(pairs)])


class GraderGuardTests(unittest.TestCase):
    def test_disagreement_with_reference_sends_answer_to_teacher(self):
        verdict = FakeGrader([0.99, 0.99], 0.05).grade("В?", "Эталон.", "Пункт 1\nПункт 2", 2, "ответ")
        self.assertEqual(verdict["score"], 1.0)
        self.assertLessEqual(verdict["confidence"], 0.3)

    def test_agreement_keeps_confidence(self):
        verdict = FakeGrader([0.99, 0.01], 0.55).grade("В?", "Эталон.", "Пункт 1\nПункт 2", 2, "ответ")
        self.assertEqual(verdict["score"], 0.5)
        self.assertGreater(verdict["confidence"], 0.9)
