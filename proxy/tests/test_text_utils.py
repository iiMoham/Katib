"""Unit tests for substring location (RTL / diacritic handling)."""

from app.text_utils import last_token, locate


def test_locate_exact():
    text = "انا احب القراءة"
    start, length = locate(text, "احب")
    assert text[start : start + length] == "احب"


def test_locate_at_start():
    text = "انا ذاهب"
    start, length = locate(text, "انا")
    assert (start, length) == (0, 3)


def test_locate_diacritic_insensitive():
    # Model returns a vowelled form; text has none.
    text = "ذهب الولد الى المدرسة"
    start, length = locate(text, "الْمَدْرَسَة")
    assert text[start : start + length] == "المدرسة"


def test_locate_missing_returns_minus_one():
    assert locate("نص بسيط", "غير موجود") == (-1, 0)


def test_last_token():
    assert last_token("اريد ان اكتب جمل") == "جمل"
    assert last_token("كلمة   ") == "كلمة"
    assert last_token("   ") == ""
