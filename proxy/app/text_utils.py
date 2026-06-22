"""Helpers for locating substrings in the user's text.

We never trust the model to report character offsets — Arabic combining marks
and the model's tendency to normalise whitespace make that unreliable. Instead
the model returns the literal erroneous substring, and we locate it here using
diacritic-insensitive matching.
"""

from __future__ import annotations

import unicodedata

# Arabic diacritics (harakat / tashkeel) we ignore when matching.
_TASHKEEL = {
    "ً", "ٌ", "ٍ", "َ", "ُ", "ِ",
    "ّ", "ْ", "ٓ", "ٔ", "ٕ", "ٰ",
    "ـ",  # tatweel / kashida
}


def _strip_tashkeel(s: str) -> str:
    return "".join(ch for ch in s if ch not in _TASHKEEL)


def _normalize(s: str) -> str:
    """NFC-normalise and drop tashkeel for tolerant matching."""
    return _strip_tashkeel(unicodedata.normalize("NFC", s))


def locate(text: str, needle: str) -> tuple[int, int]:
    """Return (start, length) of `needle` within `text`.

    Tries an exact match first, then a diacritic-insensitive match. Returns
    (-1, 0) if the substring cannot be located. `length` is measured against
    the original text so the keyboard can underline the right span.
    """
    if not needle:
        return -1, 0

    idx = text.find(needle)
    if idx != -1:
        return idx, len(needle)

    # Fall back to tolerant matching on a normalised copy. We build an index
    # map from normalised positions back to original positions.
    norm_chars: list[str] = []
    orig_index: list[int] = []
    for i, ch in enumerate(unicodedata.normalize("NFC", text)):
        if ch in _TASHKEEL:
            continue
        norm_chars.append(ch)
        orig_index.append(i)

    norm_text = "".join(norm_chars)
    norm_needle = _normalize(needle)
    if not norm_needle:
        return -1, 0

    j = norm_text.find(norm_needle)
    if j == -1:
        return -1, 0

    start = orig_index[j]
    end_norm = j + len(norm_needle) - 1
    end = orig_index[end_norm] if end_norm < len(orig_index) else len(text) - 1
    return start, end - start + 1


def last_token(text: str) -> str:
    """Best-effort: the final whitespace-delimited token of the text."""
    stripped = text.rstrip()
    if not stripped:
        return ""
    return stripped.split()[-1]
