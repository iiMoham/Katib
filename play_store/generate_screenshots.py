"""Turn raw emulator screenshots into framed Play Store marketing screenshots.

Each output is 1080x1920 with a teal gradient background, an Arabic caption band,
and the device screenshot inset with rounded corners + a soft shadow.
"""

from __future__ import annotations

import os

import arabic_reshaper
from bidi.algorithm import get_display
from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "..", "android")

TEAL = (10, 123, 94)
TEAL_DARK = (6, 70, 53)
GOLD = (200, 150, 12)
WHITE = (255, 255, 255)
AMIRI_BOLD = r"C:\Windows\Fonts\Amiri-Bold.ttf"

SHOTS = [
    ("screenshot_onboarding.png", "فعّل لوحة المفاتيح في خطوات بسيطة"),
    ("screenshot_keyboard.png", "لوحة مفاتيح عربية ذكية في كل تطبيق"),
    ("screenshot_dashboard.png", "صحّح نصوصك وتابع إحصاءاتك"),
    ("screenshot_modegate.png", "فصحى وخليجي بضغطة زر"),
]


def ar(t: str) -> str:
    return get_display(arabic_reshaper.reshape(t))


def gradient(w, h, top, bottom):
    base = Image.new("RGB", (1, h))
    for y in range(h):
        f = y / max(1, h - 1)
        base.putpixel((0, y), tuple(int(top[i] + (bottom[i] - top[i]) * f) for i in range(3)))
    return base.resize((w, h))


def rounded(img, radius):
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, img.size[0], img.size[1]], radius=radius, fill=255)
    out = img.convert("RGBA")
    out.putalpha(mask)
    return out


def frame(src_name, caption, idx):
    W, H = 1080, 1920
    canvas = gradient(W, H, TEAL, TEAL_DARK).convert("RGBA")
    d = ImageDraw.Draw(canvas)

    # Caption
    fnt = ImageFont.truetype(AMIRI_BOLD, 60)
    txt = ar(caption)
    l, t, r, b = d.textbbox((0, 0), txt, font=fnt)
    d.text(((W - (r - l)) / 2 - l, 95 - t), txt, font=fnt, fill=WHITE)
    # gold accent under caption
    d.rounded_rectangle([(W - 180) / 2, 210, (W + 180) / 2, 220], radius=5, fill=GOLD)

    # Device screenshot
    shot = Image.open(os.path.join(SRC, src_name)).convert("RGB")
    target_w = 880
    target_h = int(shot.height * target_w / shot.width)
    shot = shot.resize((target_w, target_h))
    shot = rounded(shot, 36)

    x = (W - target_w) // 2
    y = 290

    # Soft shadow
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    sd.rounded_rectangle([x + 8, y + 16, x + target_w + 8, y + target_h + 16], radius=36, fill=(0, 0, 0, 120))
    shadow = shadow.filter(ImageFilter.GaussianBlur(18))
    canvas = Image.alpha_composite(canvas, shadow)

    canvas.alpha_composite(shot, (x, y))

    out = os.path.join(HERE, f"screenshot-{idx}.png")
    canvas.convert("RGB").save(out)
    return out


if __name__ == "__main__":
    for i, (name, cap) in enumerate(SHOTS, start=1):
        print(frame(name, cap, i))
