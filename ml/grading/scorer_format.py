"""Формат входа оценщика — единый с сервисом: код живёт в local-ai/app/scorer_format.py."""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "local-ai"))

from app.scorer_format import (  # noqa: E402,F401
    MAX_LENGTH,
    criterion_pair,
    parse_criteria,
    reference_pair,
    reference_variants,
)
