"""Клиент Groq для генерации обучающих данных: ключ берётся из корневого .env (APP_AI_API_KEY)
и никуда не выводится; запросы распределяются по нескольким моделям с учётом их лимитов."""
from __future__ import annotations

import json
import os
import re
import threading
import time
from pathlib import Path

import httpx

REPO_ROOT = Path(__file__).resolve().parents[2]
BASE_URL = "https://api.groq.com/openai/v1"

# Генераторы: у каждой модели свой лимит токенов в минуту, поэтому параллелим по моделям.
# gpt-oss-20b не используется: на пробе писала эталоны с фактическими ошибками.
DEFAULT_MODELS = ["openai/gpt-oss-120b", "qwen/qwen3.8-27b"]

_THINK = re.compile(r"(?s)<think>.*?</think>")


def api_key() -> str:
    key = os.environ.get("APP_AI_API_KEY")
    if key:
        return key
    env = REPO_ROOT / ".env"
    for line in env.read_text(encoding="utf-8").splitlines():
        if line.startswith("APP_AI_API_KEY="):
            return line.split("=", 1)[1].strip()
    raise SystemExit("APP_AI_API_KEY не найден ни в окружении, ни в .env")


class DailyLimitReached(RuntimeError):
    pass


class GroqClient:
    def __init__(self, timeout: float = 180):
        self._http = httpx.Client(
            base_url=BASE_URL,
            timeout=timeout,
            headers={"Authorization": f"Bearer {api_key()}"},
        )
        self._lock = threading.Lock()
        self.usage = {}

    def chat_json(self, model: str, system: str, user: str, max_tokens: int = 4000,
                  temperature: float = 0.8, retries: int = 6) -> dict:
        """Запрос с ответом-JSON. Повторяет при лимите частоты и битом JSON."""
        body = {
            "model": model,
            "messages": [{"role": "system", "content": system}, {"role": "user", "content": user}],
            "temperature": temperature,
            "max_tokens": max_tokens,
            "response_format": {"type": "json_object"},
        }
        # Длинные «рассуждения» съедают лимит токенов; для генерации данных хватает короткого.
        if "gpt-oss" in model:
            body["reasoning_effort"] = "low"
        elif "qwen3" in model:
            body["reasoning_effort"] = "none"
        last_error = None
        attempt = 0
        rate_limited = 0
        while attempt < retries:
            try:
                response = self._http.post("/chat/completions", json=body)
            except httpx.HTTPError as error:
                last_error = error
                attempt += 1
                time.sleep(5 * attempt)
                continue
            if response.status_code == 429:
                message = response.text
                if "per day" in message or "TPD" in message or "RPD" in message:
                    raise DailyLimitReached(f"{model}: дневной лимит исчерпан")
                # Ожидание лимита частоты — не ошибка, попытки не тратит (но и не бесконечно).
                rate_limited += 1
                if rate_limited > 60:
                    last_error = RuntimeError(f"{model}: слишком долго упирается в лимит частоты")
                    break
                wait = _retry_after(response) or 10
                time.sleep(min(wait + 1, 90))
                continue
            attempt += 1
            if response.status_code >= 500 or response.status_code == 413:
                last_error = RuntimeError(f"{model}: HTTP {response.status_code}")
                time.sleep(10 * (attempt + 1))
                if response.status_code == 413:
                    break
                continue
            if response.status_code != 200:
                raise RuntimeError(f"{model}: HTTP {response.status_code}: {response.text[:300]}")
            data = response.json()
            self._count(model, data.get("usage") or {})
            text = _THINK.sub("", data["choices"][0]["message"].get("content") or "").strip()
            try:
                return json.loads(_strip_fences(text))
            except json.JSONDecodeError as error:
                last_error = error
                continue
        raise RuntimeError(f"{model}: не удалось получить ответ ({last_error})")

    def _count(self, model: str, usage: dict):
        with self._lock:
            entry = self.usage.setdefault(model, {"requests": 0, "tokens": 0})
            entry["requests"] += 1
            entry["tokens"] += int(usage.get("total_tokens") or 0)


def _retry_after(response: httpx.Response) -> float | None:
    header = response.headers.get("retry-after")
    if header:
        try:
            return float(header)
        except ValueError:
            pass
    match = re.search(r"try again in ([0-9.]+)(ms|s|m)", response.text)
    if not match:
        return None
    value, unit = float(match.group(1)), match.group(2)
    return value / 1000 if unit == "ms" else value * 60 if unit == "m" else value


def _strip_fences(text: str) -> str:
    text = text.strip()
    if text.startswith("```"):
        text = text.split("\n", 1)[1] if "\n" in text else text
        text = text.rsplit("```", 1)[0]
    return text.strip()


def data_home() -> Path:
    """Где лежат данные и модели (не в git): ST_ML_HOME или D:/ai/student-testing."""
    return Path(os.environ.get("ST_ML_HOME", "D:/ai/student-testing"))
