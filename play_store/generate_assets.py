"""Generate Google Play store graphics for Katib using Pillow + Amiri (Arabic).

Outputs (in this folder):
  - icon-512.png                512x512 app icon
  - feature-graphic-1024x500.png  Play feature graphic

Arabic is shaped with arabic_reshaper + python-bidi so letters connect correctly.
"""

from __future__ import annotations

import math
import os

import arabic_reshaper
from bidi.algorithm import get_display
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))

TEAL = (10, 123, 94)
TEAL_DARK = (6, 70, 53)
GOLD = (200, 150, 12)
WHITE = (255, 255, 255)

AMIRI_BOLD = r"C:\Windows\Fonts\Amiri-Bold.ttf"
AMIRI_REG = r"C:\Windows\Fonts\Amiri-Regular.ttf"


def ar(text: str) -> str:
    """Shape Arabic for correct connected, right-to-left rendering."""
    return get_display(arabic_reshaper.reshape(text))


def font(path: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(path, size)


def draw_centered(draw, cx, cy, text, fnt, fill):
    l, t, r, b = draw.textbbox((0, 0), text, font=fnt)
    w, h = r - l, b - t
    draw.text((cx - w / 2 - l, cy - h / 2 - t), text, font=fnt, fill=fill)
    return w, h


def star(draw, cx, cy, r, fill):
    """A 4-point sparkle (✦) centred at (cx, cy)."""
    pts = []
    for i in range(8):
        ang = math.pi / 2 + i * math.pi / 4
        rad = r if i % 2 == 0 else r * 0.32
        pts.append((cx + rad * math.cos(ang), cy - rad * math.sin(ang)))
    draw.polygon(pts, fill=fill)


def vertical_gradient(size, top, bottom):
    w, h = size
    base = Image.new("RGB", (1, h))
    for y in range(h):
        f = y / max(1, h - 1)
        base.putpixel((0, y), tuple(int(top[i] + (bottom[i] - top[i]) * f) for i in range(3)))
    return base.resize((w, h))


def make_icon():
    S = 512
    img = Image.new("RGB", (S, S), TEAL)
    d = ImageDraw.Draw(img)

    # subtle darker teal vignette via a centred rounded panel
    star(d, S / 2, 150, 46, GOLD)

    # Wordmark
    draw_centered(d, S / 2, S / 2 + 20, ar("كاتب"), font(AMIRI_BOLD, 210), WHITE)

    # Gold pen-stroke underline
    y = S - 120
    d.rounded_rectangle([120, y, S - 120, y + 16], radius=8, fill=GOLD)
    # little nib triangle on the right end (RTL "start")
    d.polygon([(S - 120, y - 6), (S - 120, y + 22), (S - 92, y + 8)], fill=GOLD)

    out = os.path.join(HERE, "icon-512.png")
    img.save(out)
    return out


def make_feature():
    W, H = 1024, 500
    img = vertical_gradient((W, H), TEAL, TEAL_DARK).convert("RGB")
    d = ImageDraw.Draw(img)

    # Decorative faint sparkles on the left
    star(d, 210, 250, 120, (255, 255, 255, 0) and (18, 140, 108))
    star(d, 330, 120, 46, (16, 110, 84))
    star(d, 150, 380, 36, (16, 110, 84))

    # Right-aligned brand block (RTL). Anchor near the right margin.
    right = W - 80
    # Title
    title = ar("كاتب")
    tf = font(AMIRI_BOLD, 170)
    l, t, r, b = d.textbbox((0, 0), title, font=tf)
    d.text((right - (r - l) - l, 120 - t), title, font=tf, fill=WHITE)
    # gold sparkle before the title
    star(d, right - (r - l) - 70, 200, 34, GOLD)

    # Tagline
    tag = ar("مساعدك الذكي للكتابة بالعربية")
    gf = font(AMIRI_REG, 56)
    l2, t2, r2, b2 = d.textbbox((0, 0), tag, font=gf)
    d.text((right - (r2 - l2) - l2, 330 - t2), tag, font=gf, fill=(240, 224, 170))

    # gold underline accent under tagline
    d.rounded_rectangle([right - (r2 - l2), 420, right, 430], radius=5, fill=GOLD)

    out = os.path.join(HERE, "feature-graphic-1024x500.png")
    img.save(out)
    return out


if __name__ == "__main__":
    print("icon:", make_icon())
    print("feature:", make_feature())
