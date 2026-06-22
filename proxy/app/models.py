"""Pydantic request/response schemas for the corrections API.

These mirror the contract from the Katib spec:
    request : { text, mode, task }
    response: { corrections: [...], suggestions: [...] }
"""

from __future__ import annotations

from enum import Enum

from pydantic import BaseModel, Field


class Mode(str, Enum):
    msa = "msa"      # Modern Standard Arabic — الفصحى
    gulf = "gulf"    # Gulf dialect — الخليجي


class Task(str, Enum):
    correct = "correct"
    suggest = "suggest"


class CorrectionType(str, Enum):
    spelling = "spelling"        # إملائي
    grammar = "grammar"          # نحوي
    agreement = "agreement"      # تطابق (جمع/مفرد، مذكر/مؤنث)
    conjugation = "conjugation"  # تصريف الأفعال
    hamza = "hamza"              # همزات: أ / إ / ا
    taa = "taa"                  # تاء مربوطة/مفتوحة: ة / ه
    yaa = "yaa"                  # ألف مقصورة/ياء: ى / ي
    punctuation = "punctuation"  # علامات الترقيم
    other = "other"


# ---------- Request ----------

class CorrectRequest(BaseModel):
    text: str = Field(..., description="Raw Arabic text the user has typed.")
    mode: Mode = Mode.msa
    task: Task = Task.correct


# ---------- Response objects ----------

class Correction(BaseModel):
    original: str = Field(..., description="The erroneous substring as it appears in the text.")
    corrected: str = Field(..., description="The fixed substring.")
    start: int = Field(-1, description="Character offset of `original` in the text, or -1 if not located.")
    length: int = Field(0, description="Character length of `original`.")
    type: CorrectionType = CorrectionType.other
    reason: str = Field("", description="One-line explanation in Arabic.")


class SuggestionOption(BaseModel):
    text: str = Field(..., description="The alternative word or phrase.")
    register: str = Field("فصحى", description="Register label: فصحى (formal) or خليجي (casual).")
    rank: int = Field(1, description="1 = most formal/recommended, higher = less formal.")


class SuggestionGroup(BaseModel):
    target: str = Field("", description="The word/phrase the options would replace (usually the last token).")
    start: int = Field(-1, description="Character offset of `target` in the text, or -1 if not located.")
    length: int = Field(0)
    options: list[SuggestionOption] = Field(default_factory=list)


class CorrectResponse(BaseModel):
    corrections: list[Correction] = Field(default_factory=list)
    suggestions: list[SuggestionGroup] = Field(default_factory=list)
    mode: Mode = Mode.msa
    task: Task = Task.correct
    cached: bool = False
