"""Endpoint tests with a stubbed Grok client (no network / no API key needed)."""

import json

import pytest
from fastapi.testclient import TestClient

import main
from app.grok_client import GrokClient
from app.models import Mode, Task


class _FakeCompletionMessage:
    def __init__(self, content: str):
        self.content = content


class _FakeChoice:
    def __init__(self, content: str):
        self.message = _FakeCompletionMessage(content)


class _FakeCompletion:
    def __init__(self, content: str):
        self.choices = [_FakeChoice(content)]


def _install_fake_grok(app, response_payload: dict):
    """Patch the app's GrokClient so .process uses canned JSON instead of network."""
    grok: GrokClient = app.state.grok

    async def fake_create(*args, **kwargs):
        return _FakeCompletion(json.dumps(response_payload, ensure_ascii=False))

    grok._client.chat.completions.create = fake_create  # type: ignore[attr-defined]


@pytest.fixture
def client():
    with TestClient(main.app) as c:
        yield c


def test_health(client):
    r = client.get("/health")
    assert r.status_code == 200
    assert r.json()["status"] == "ok"


def test_correct_locates_offsets(client):
    _install_fake_grok(
        client.app,
        {
            "corrections": [
                {
                    "original": "انشاء",
                    "corrected": "إن شاء",
                    "type": "hamza",
                    "reason": "الصواب فصل (إن شاء) لا (انشاء).",
                }
            ]
        },
    )
    text = "انشاء الله بكرة"
    r = client.post("/correct", json={"text": text, "mode": "msa", "task": "correct"})
    assert r.status_code == 200
    body = r.json()
    assert len(body["corrections"]) == 1
    corr = body["corrections"][0]
    assert corr["corrected"] == "إن شاء"
    assert corr["type"] == "hamza"
    assert text[corr["start"] : corr["start"] + corr["length"]] == "انشاء"


def test_correct_drops_noop(client):
    # original == corrected should be filtered out.
    _install_fake_grok(
        client.app,
        {"corrections": [{"original": "بيت", "corrected": "بيت", "type": "other", "reason": ""}]},
    )
    r = client.post("/correct", json={"text": "بيت كبير", "task": "correct"})
    assert r.json()["corrections"] == []


def test_suggest_returns_ranked_options(client):
    _install_fake_grok(
        client.app,
        {
            "suggestions": [
                {
                    "target": "زين",
                    "options": [
                        {"text": "جيد", "register": "فصحى", "rank": 1},
                        {"text": "ممتاز", "register": "فصحى", "rank": 2},
                        {"text": "زين", "register": "خليجي", "rank": 3},
                    ],
                }
            ]
        },
    )
    r = client.post("/correct", json={"text": "الاكل زين", "mode": "gulf", "task": "suggest"})
    assert r.status_code == 200
    groups = r.json()["suggestions"]
    assert len(groups) == 1
    assert len(groups[0]["options"]) == 3
    assert groups[0]["options"][0]["text"] == "جيد"


def test_cache_marks_second_call(client):
    _install_fake_grok(client.app, {"corrections": []})
    payload = {"text": "نص للتخزين المؤقت", "task": "correct"}
    first = client.post("/correct", json=payload).json()
    second = client.post("/correct", json=payload).json()
    assert first["cached"] is False
    assert second["cached"] is True


def test_empty_text_short_circuits(client):
    r = client.post("/correct", json={"text": "   ", "task": "correct"})
    assert r.status_code == 200
    assert r.json()["corrections"] == []


def test_too_long_text_rejected(client):
    long_text = "ا" * 5000
    r = client.post("/correct", json={"text": long_text, "task": "correct"})
    assert r.status_code == 413
