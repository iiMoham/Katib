"""System prompts and JSON-schema instructions sent to Grok.

The base persona comes straight from the Katib spec. We extend it with an
explicit JSON contract and mode-specific guidance so the model returns data
the keyboard can render without extra parsing.
"""

from __future__ import annotations

from .models import Mode, Task

# Base persona from the spec.
_BASE = (
    "أنت مساعد لغوي متخصص في اللغة العربية. مهمتك تصحيح النصوص العربية وتقديم "
    "مقترحات لغوية بديلة. أجب فقط بـ JSON بالتنسيق المطلوب، بدون أي شرح إضافي "
    "خارج الـ JSON."
)

# What "Gulf mode" means for correction tolerance.
_GULF_NOTE = (
    "الوضع الحالي: خليجي. لا تعامل التعابير العامية الخليجية الشائعة كأخطاء "
    "(مثل: وش، شلون، كذا، يبغى، تو، الحين، زين). صحّح فقط الأخطاء الإملائية "
    "الواضحة والأخطاء التي تكسر المعنى."
)

_MSA_NOTE = (
    "الوضع الحالي: فصحى. صحّح النص ليطابق قواعد اللغة العربية الفصحى الحديثة "
    "(النحو، الصرف، الإملاء)."
)

# JSON contract for the "correct" task.
_CORRECT_SCHEMA = (
    "أعد كائن JSON بهذا الشكل بالضبط:\n"
    '{\n'
    '  "corrections": [\n'
    '    {\n'
    '      "original": "النص الخاطئ كما ورد حرفيًا",\n'
    '      "corrected": "النص المصحَّح",\n'
    '      "type": "spelling | grammar | agreement | conjugation | hamza | taa | yaa | punctuation | other",\n'
    '      "reason": "سبب التصحيح في جملة قصيرة بالعربية"\n'
    '    }\n'
    '  ]\n'
    '}\n'
    "قواعد مهمة:\n"
    "- ضع في \"original\" المقطع الخاطئ كما هو موجود في النص الأصلي حرفًا بحرف، "
    "دون إضافة أو حذف مسافات، حتى نتمكن من تحديد موقعه.\n"
    "- لا تُدرج أي عنصر إذا لم يكن هناك خطأ. إذا كان النص سليمًا تمامًا أعد "
    '"corrections": [].\n'
    "- لا تصحّح أسماء الأعلام أو الكلمات الأجنبية المكتوبة عمدًا.\n"
)

# JSON contract for the "suggest" task.
_SUGGEST_SCHEMA = (
    "أعد كائن JSON بهذا الشكل بالضبط:\n"
    '{\n'
    '  "suggestions": [\n'
    '    {\n'
    '      "target": "الكلمة أو العبارة الأخيرة التي ستُستبدل",\n'
    '      "options": [\n'
    '        {"text": "البديل", "register": "فصحى أو خليجي", "rank": 1}\n'
    '      ]\n'
    '    }\n'
    '  ]\n'
    '}\n'
    "قواعد مهمة:\n"
    "- قدّم حتى 3 بدائل (مرادفات أو عبارات) للكلمة/العبارة الأخيرة في النص، "
    "مرتبة من الأكثر رسمية (rank=1) إلى الأقل رسمية.\n"
    "- ضع \"register\" = \"فصحى\" للبديل الرسمي و \"خليجي\" للبديل العامي.\n"
    "- اجعل \"target\" مطابقًا للنص الأصلي حرفيًا حتى نتمكن من تحديد موقعه.\n"
    "- إن لم تجد بدائل مناسبة أعد \"suggestions\": [].\n"
)


def build_system_prompt(mode: Mode, task: Task) -> str:
    """Assemble the full system prompt for a given mode + task."""
    mode_note = _GULF_NOTE if mode is Mode.gulf else _MSA_NOTE
    schema = _CORRECT_SCHEMA if task is Task.correct else _SUGGEST_SCHEMA
    return f"{_BASE}\n\n{mode_note}\n\n{schema}"


def build_user_prompt(text: str, task: Task) -> str:
    verb = "صحّح النص التالي" if task is Task.correct else "اقترح بدائل للكلمة الأخيرة في النص التالي"
    return f"{verb}:\n\n«{text}»"
