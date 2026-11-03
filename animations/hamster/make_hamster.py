#!/usr/bin/env python3
"""
Пример анимации «Хомяк-дегустатор».

Хомяк уже набил щёки едой и добавляет ещё один орех.
Скрипт не трогает код Android-приложения: он генерирует отдельные
файлы GIF / WebP / MP4 и HTML-страницу для просмотра.
"""

from __future__ import annotations

import math
import os
import shutil
import subprocess
from dataclasses import dataclass
from pathlib import Path

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageOps

W = 600
S = 2
C = W * S
FPS = 20
DURATION = 3.0
FRAMES = int(FPS * DURATION)
OUT = Path(__file__).resolve().parent
FRAME_DIR = OUT / "frames"

# Палитра в духе приложения: тёмно-синий + золото.
BG_TOP = (6, 20, 58)
BG_BOTTOM = (3, 11, 34)
SHOCK_BG_TOP = (58, 9, 22)
SHOCK_BG_BOTTOM = (33, 3, 11)
GOLD = (255, 200, 87)
FUR = (246, 196, 92)
FUR_DARK = (170, 108, 38)
FUR_LIGHT = (255, 224, 156)
BELLY = (255, 238, 198)
BELLY_LINE = (224, 170, 98)
CHEEK = (255, 208, 124)
CHEEK_LIGHT = (255, 232, 180)
CHEEK_BUMP = (249, 193, 113)
CHEEK_LINE = (198, 126, 48)
EAR_INNER = (242, 156, 156)
EYE = (43, 26, 15)
NOSE = (214, 104, 118)
NUT = (164, 96, 42)
NUT_DARK = (104, 58, 26)
NUT_LIGHT = (214, 146, 80)
MOUTH = (92, 44, 32)
TONGUE = (240, 140, 150)
WHISKER = (120, 80, 40)


def sc(v: float) -> int:
    return int(round(v * S))


def clamp01(v: float) -> float:
    return max(0.0, min(1.0, v))


def smooth(v: float) -> float:
    v = clamp01(v)
    return v * v * (3.0 - 2.0 * v)


def ease_out_cubic(v: float) -> float:
    v = clamp01(v)
    return 1.0 - (1.0 - v) ** 3


def lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t


def lerp2(a: tuple[float, float], b: tuple[float, float], t: float) -> tuple[float, float]:
    return lerp(a[0], b[0], t), lerp(a[1], b[1], t)


def qbez(
    p0: tuple[float, float],
    p1: tuple[float, float],
    p2: tuple[float, float],
    n: int = 24,
) -> list[tuple[float, float]]:
    pts = []
    for i in range(n + 1):
        t = i / n
        x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0]
        y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]
        pts.append((x, y))
    return pts


def qp(
    p0: tuple[float, float],
    p1: tuple[float, float],
    p2: tuple[float, float],
    t: float,
) -> tuple[float, float]:
    x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0]
    y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]
    return x, y


def E(
    d: ImageDraw.ImageDraw,
    cx: float,
    cy: float,
    rx: float,
    ry: float,
    fill=None,
    outline=None,
    width: float = 2.0,
) -> None:
    d.ellipse(
        [sc(cx - rx), sc(cy - ry), sc(cx + rx), sc(cy + ry)],
        fill=fill,
        outline=outline,
        width=sc(width) if outline is not None else 0,
    )


def L(
    d: ImageDraw.ImageDraw,
    pts: list[tuple[float, float]],
    fill,
    width: float = 2.0,
) -> None:
    d.line([(sc(x), sc(y)) for x, y in pts], fill=fill, width=sc(width), joint="curve")


def A(
    d: ImageDraw.ImageDraw,
    cx: float,
    cy: float,
    rx: float,
    ry: float,
    start: float,
    end: float,
    fill,
    width: float = 2.0,
) -> None:
    d.arc(
        [sc(cx - rx), sc(cy - ry), sc(cx + rx), sc(cy + ry)],
        start,
        end,
        fill=fill,
        width=sc(width),
    )


@dataclass
class State:
    nut_pos: tuple[float, float]
    nut_scale: float
    nut_alpha: float
    nut_behind: bool
    paw: tuple[float, float]
    cheek_l: float
    cheek_r: float
    happy: bool
    blink: bool
    mouth_open: float
    sparkle: float
    breath: float
    bounce: float
    pop: float
    surprised: bool = False
    shock: float = 0.0
    shake_x: float = 0.0
    shake_y: float = 0.0
    sweat: float = 0.0


def state_at(t: float) -> State:
    t = t % 1.0

    # Орех: появляется справа, поднимается к мордочке и уходит прямо в рот.
    if t < 0.14:
        nut_pos = (440.0, 460.0)
    elif t < 0.40:
        nut_pos = qp((440, 460), (392, 388), (300, 338), smooth((t - 0.14) / 0.26))
    else:
        nut_pos = (300.0, 338.0)

    if t < 0.06:
        nut_alpha = 0.0
    elif t < 0.14:
        nut_alpha = smooth((t - 0.06) / 0.08)
    elif t < 0.48:
        nut_alpha = 1.0
    elif t < 0.56:
        nut_alpha = 1.0 - smooth((t - 0.48) / 0.08)
    else:
        nut_alpha = 0.0

    if t < 0.14:
        nut_scale = 0.90
    elif t < 0.40:
        nut_scale = 0.90 + 0.10 * smooth((t - 0.14) / 0.26)
    else:
        # Орех уменьшается, пока проваливается в рот.
        nut_scale = 1.0 - 0.62 * smooth((t - 0.40) / 0.12)

    # В самом конце орех рисуется позади мордочки — он «проваливается» в рот.
    nut_behind = 0.46 <= t < 0.56

    # Лапа держит орех у рта, а после «попа» возвращается на живот.
    if t < 0.02:
        paw = (360.0, 420.0)
    elif t < 0.10:
        paw = lerp2((360, 420), (456, 486), smooth((t - 0.02) / 0.08))
    elif t < 0.14:
        paw = (456.0, 486.0)
    elif t < 0.44:
        paw = (nut_pos[0] + 16.0, nut_pos[1] + 26.0)
    elif t < 0.66:
        paw = qp((316, 364), (352, 408), (360, 420), smooth((t - 0.44) / 0.22))
    else:
        paw = (360.0, 420.0)

    # Обе щеки слегка раздуваются, пока хомяк жуёт.
    if t < 0.50:
        cheek_l = 1.0
    elif t < 0.60:
        cheek_l = 1.0 + 0.15 * ease_out_cubic((t - 0.50) / 0.10)
    elif t < 0.90:
        chew = abs(math.sin((t - 0.60) * math.pi * 6.0))
        cheek_l = 1.15 - 0.03 * chew - 0.05 * smooth((t - 0.60) / 0.30)
    else:
        cheek_l = 1.0
    cheek_r = cheek_l

    happy = 0.56 < t < 0.92
    blink = (0.02 < t < 0.08) or (0.93 < t < 0.98)

    mouth_open = 0.0
    if 0.28 < t < 0.50:
        mouth_open = math.sin(math.pi * clamp01((t - 0.28) / 0.22))
    if 0.50 <= t < 0.92:
        chew = abs(math.sin((t - 0.50) * math.pi * 7.0))
        fade = 1.0 - smooth((t - 0.50) / 0.42)
        mouth_open = max(mouth_open, 0.72 * chew * fade)

    sparkle = 0.0
    if 0.58 < t < 0.94:
        sparkle = math.sin(math.pi * clamp01((t - 0.58) / 0.36))

    pop = 0.0
    if 0.46 < t < 0.68:
        pop = clamp01((t - 0.46) / 0.22)

    breath = 1.2 * math.sin(2 * math.pi * t)

    bounce = 0.0
    if 0.56 < t < 0.78:
        bounce = -9.0 * math.sin(math.pi * clamp01((t - 0.56) / 0.22))

    return State(
        nut_pos=nut_pos,
        nut_scale=nut_scale,
        nut_alpha=nut_alpha,
        nut_behind=nut_behind,
        paw=paw,
        cheek_l=cheek_l,
        cheek_r=cheek_r,
        happy=happy,
        blink=blink,
        mouth_open=mouth_open,
        sparkle=sparkle,
        breath=breath,
        bounce=bounce,
        pop=pop,
    )


def state_at_shock(t: float) -> State:
    """Неверный ответ: хомяка бьёт током, орех падает на пол."""
    t = t % 1.0

    if 0.12 < t < 0.66:
        shock = math.sin(math.pi * clamp01((t - 0.12) / 0.54)) ** 0.7
    else:
        shock = 0.0

    # Орех: секунду держится у рта, затем выпадает и катится по полу.
    if t < 0.12:
        nut_pos = (300.0, 352.0)
        nut_alpha = smooth(t / 0.06) if t < 0.06 else 1.0
        nut_scale = 1.0
    elif t < 0.50:
        fall = smooth((t - 0.12) / 0.38)
        nut_pos = (300.0 + 70.0 * fall, 352.0 + 186.0 * fall * fall)
        nut_alpha = 1.0
        nut_scale = 1.0
    else:
        nut_pos = (370.0, 538.0)
        nut_alpha = 1.0
        nut_scale = 1.0

    shake_x = 7.0 * shock * math.sin(t * 2 * math.pi * 13)
    shake_y = 3.0 * shock * math.cos(t * 2 * math.pi * 17)

    sweat = smooth((t - 0.54) / 0.14) if t > 0.54 else 0.0

    return State(
        nut_pos=nut_pos,
        nut_scale=nut_scale,
        nut_alpha=nut_alpha,
        nut_behind=False,
        paw=(360.0, 420.0),
        cheek_l=1.0,
        cheek_r=1.0,
        happy=False,
        blink=False,
        mouth_open=0.0,
        sparkle=0.0,
        breath=1.2 * math.sin(2 * math.pi * t),
        bounce=0.0,
        pop=0.0,
        surprised=True,
        shock=shock,
        shake_x=shake_x,
        shake_y=shake_y,
        sweat=sweat,
    )


def draw_eyes(d: ImageDraw.ImageDraw, st: State) -> None:
    for cx in (250.0, 350.0):
        if st.surprised:
            # Испуганные глаза: белок, огромный зрачок и поднятые брови.
            E(d, cx, 253, 18, 20, (255, 255, 255), FUR_DARK, 2)
            E(d, cx, 255, 11, 12, EYE)
            E(d, cx - 3.5, 250, 4.0, 4.0, (255, 255, 255))
            L(d, [(cx - 24, 224), (cx - 8, 216), (cx + 8, 216), (cx + 24, 224)], FUR_DARK, 4)
        elif st.happy:
            L(d, qbez((cx - 14, 261), (cx, 242), (cx + 14, 261), 18), EYE, 5)
        elif st.blink:
            L(d, qbez((cx - 14, 255), (cx, 263), (cx + 14, 255), 18), EYE, 4)
        else:
            E(d, cx, 255, 14, 14, EYE)
            E(d, cx - 4.5, 250, 4.5, 4.5, (255, 255, 255))
            E(d, cx + 4, 260, 2.0, 2.0, (255, 255, 255))


def draw_cheek(
    img: Image.Image, cx: float, cy: float, scale: float, flip: float = 1.0
) -> None:
    rx = 62.0 * scale
    ry = 52.0 * scale

    ch = Image.new("RGBA", (C, C), (0, 0, 0, 0))
    d = ImageDraw.Draw(ch)
    E(d, cx, cy, rx, ry, CHEEK, CHEEK_LINE, 2.5)

    # Лёгкие бугорки еды внизу щеки.
    for bx, by, br in ((-16, 7, 16), (7, 13, 14), (20, -3, 12)):
        E(
            d,
            cx + flip * bx * scale,
            cy + by * scale,
            br * scale,
            br * 0.80 * scale,
            (252, 198, 116),
        )
    # Плоский блик сверху — в стиле остальной графики.
    E(
        d,
        cx - flip * 22 * scale,
        cy - 23 * scale,
        rx * 0.30,
        ry * 0.17,
        (255, 233, 181),
    )
    E(
        d,
        cx - flip * 30 * scale,
        cy - 20 * scale,
        rx * 0.08,
        ry * 0.06,
        (255, 245, 214),
    )
    img.alpha_composite(ch, (0, 0))


def draw_nut(d: ImageDraw.ImageDraw, x: float, y: float, scale: float, alpha: float) -> None:
    if alpha <= 0.01 or scale <= 0.02:
        return
    a = int(255 * clamp01(alpha))
    body = (*NUT, a)
    dark = (*NUT_DARK, a)
    light = (*NUT_LIGHT, a)
    line = (128, 72, 30, a)

    # Орех крупнее лица-мордочки, чтобы жест читался.
    E(d, x, y + 2 * scale, 24 * scale, 29 * scale, body, dark, 2)
    # Шапочка и хвостик.
    E(d, x, y - 21 * scale, 21 * scale, 10 * scale, dark)
    E(d, x, y - 27 * scale, 11 * scale, 5 * scale, dark)
    L(d, [(x, y - 31 * scale), (x + 4 * scale, y - 38 * scale)], dark, 3)
    # Рельеф скорлупы.
    for dx, dy, r in ((-6, 2, 13), (7, 9, 10), (2, -8, 11)):
        A(d, x + dx * scale, y + dy * scale, r * scale, r * 1.25 * scale, 200, 340, line, 1.8)
    A(d, x - 3 * scale, y + 3 * scale, 20 * scale, 24 * scale, 130, 210, line, 1.5)
    # Блик.
    E(d, x - 8 * scale, y - 2 * scale, 5 * scale, 9 * scale, light)
    E(d, x + 7 * scale, y + 9 * scale, 2.5 * scale, 4 * scale, light)


def draw_sparkles(d: ImageDraw.ImageDraw, intensity: float) -> None:
    if intensity <= 0.01:
        return
    positions = [
        (185, 250, 1.00),
        (215, 232, 0.72),
        (160, 305, 0.58),
        (222, 365, 0.82),
        (398, 252, 0.62),
        (382, 366, 0.55),
    ]
    for x, y, k in positions:
        a = int(255 * clamp01(intensity * k))
        if a <= 4:
            continue
        size = 11 * k * (0.75 + 0.25 * intensity)
        pts = [
            (x, y - size),
            (x + size * 0.28, y - size * 0.28),
            (x + size, y),
            (x + size * 0.28, y + size * 0.28),
            (x, y + size),
            (x - size * 0.28, y + size * 0.28),
            (x - size, y),
            (x - size * 0.28, y - size * 0.28),
        ]
        d.polygon([(sc(px), sc(py)) for px, py in pts], fill=(255, 220, 130, a))
        E(d, x, y, size * 0.22, size * 0.22, (255, 255, 235, a))


def draw_electric(d: ImageDraw.ImageDraw, intensity: float) -> None:
    """Молнии вокруг хомяка при разряде тока."""
    a = int(235 * clamp01(intensity))
    if a < 5:
        return
    bolts = [
        [(70, 170), (108, 214), (88, 222), (128, 282)],
        [(530, 160), (492, 206), (512, 214), (472, 276)],
        [(120, 450), (156, 408), (138, 398), (176, 360)],
        [(480, 448), (444, 404), (462, 396), (424, 356)],
        [(300, 92), (278, 132), (296, 138), (272, 176)],
    ]
    for bolt in bolts:
        pts = [(sc(x), sc(y)) for x, y in bolt]
        d.line(pts, fill=(150, 225, 255, int(a * 0.30)), width=sc(11), joint="curve")
        d.line(pts, fill=(205, 243, 255, a), width=sc(3.4), joint="curve")


def draw_sweat(d: ImageDraw.ImageDraw, x: float, y: float, alpha: float) -> None:
    a = int(225 * clamp01(alpha))
    if a < 5:
        return
    pts = [
        (x, y - 18),
        (x + 13, y + 2),
        (x + 8, y + 13),
        (x, y + 19),
        (x - 8, y + 13),
        (x - 13, y + 2),
    ]
    d.polygon([(sc(px), sc(py)) for px, py in pts], fill=(143, 216, 255, a))
    E(d, x - 4, y + 1, 3.2, 5.4, (238, 251, 255, a))


def draw_hamster(t: float, variant: str = "eat") -> tuple[Image.Image, State]:
    st = state_at_shock(t) if variant == "shock" else state_at(t)
    layer = Image.new("RGBA", (C, C), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)

    # Уши (за головой).
    for ex in (195.0, 405.0):
        E(d, ex, 180, 43, 43, FUR, FUR_DARK, 3)
        E(d, ex, 180, 24, 24, EAR_INNER)
        E(d, ex - 6, 171, 7, 9, (255, 190, 190))

    # Тело.
    body_cy = 410 + st.breath
    E(d, 300, body_cy, 155, 135, FUR, FUR_DARK, 3)

    # Живот.
    E(d, 300, body_cy + 30, 100, 85, BELLY, BELLY_LINE, 2)
    E(d, 274, body_cy + 8, 45, 30, (255, 248, 220))

    # Лапки-ножки.
    for fx in (225.0, 375.0):
        E(d, fx, body_cy + 125, 46, 21, FUR, FUR_DARK, 3)
        for k in (-12, 0, 12):
            L(d, [(fx + k, body_cy + 118), (fx + k, body_cy + 135)], FUR_DARK, 2)

    # Голова.
    if st.shock > 0.02:
        # Вздыбленная шёрстка от разряда.
        for i in range(20):
            ang = 2 * math.pi * i / 20
            x1 = 300 + 145 * math.cos(ang)
            y1 = 285 + 125 * math.sin(ang)
            length = (8 + 9 * st.shock) * (0.72 + 0.28 * abs(math.sin(i * 2.3)))
            x2 = 300 + (145 + length) * math.cos(ang)
            y2 = 285 + (125 + length) * math.sin(ang)
            L(d, [(x1, y1), (x2, y2)], FUR_DARK, 2.6)

    E(d, 300, 285, 145, 125, FUR, FUR_DARK, 3)

    # Хохолок.
    L(d, [(288, 165), (280, 148)], FUR_DARK, 3)
    L(d, [(300, 162), (300, 144)], FUR_DARK, 3)
    L(d, [(312, 165), (320, 148)], FUR_DARK, 3)

    # Орех, который уже входит в щёку, рисуем под щекой.
    if st.nut_behind and st.nut_alpha > 0.01:
        draw_nut(d, st.nut_pos[0], st.nut_pos[1], st.nut_scale, st.nut_alpha)

    # Усы — выглядывают из-под щёк.
    for side in (-1, 1):
        bx = 300 + side * 110
        for dy, tilt in ((-16, -9), (0, 0), (16, 9)):
            L(
                d,
                [(bx, 322 + dy), (bx + side * 72, 318 + dy + tilt)],
                WHISKER,
                2.2,
            )

    # Щёки.
    draw_cheek(layer, 210, 322, st.cheek_l, flip=1.0)
    draw_cheek(layer, 390, 322, st.cheek_r, flip=-1.0)
    d = ImageDraw.Draw(layer)

    # Глаза, нос, рот.
    draw_eyes(d, st)
    E(d, 300, 308, 9, 7, NOSE, (150, 60, 80), 1.5)
    E(d, 297, 306, 3, 2, (255, 190, 200))

    if st.surprised:
        E(d, 300, 338, 13, 17, MOUTH)
        E(d, 300, 345, 7, 5, TONGUE)
    elif st.mouth_open > 0.05:
        h = 6 + 12 * st.mouth_open
        E(d, 300, 335, 11, h, MOUTH)
        E(d, 300, 335 + h * 0.35, 6, h * 0.35, TONGUE)
    else:
        L(d, [(288, 332), (300, 338), (312, 332)], MOUTH, 3)

    # Левая лапа на животе.
    E(d, 222, 425, 30, 21, FUR, FUR_DARK, 3)
    for k in (-10, 0, 10):
        L(d, [(222 + k, 418), (222 + k, 433)], FUR_DARK, 2)

    # Орех и эффекты — на отдельном слое, чтобы аккуратно смешивать прозрачность.
    fx = Image.new("RGBA", (C, C), (0, 0, 0, 0))
    fd = ImageDraw.Draw(fx)

    px, py = st.paw
    if st.nut_alpha > 0.01 and not st.nut_behind:
        draw_nut(fd, st.nut_pos[0], st.nut_pos[1], st.nut_scale, st.nut_alpha)
    # Правая лапа поверх ореха — так видно, что хомяк его держит.
    E(fd, px, py, 27, 18, FUR, FUR_DARK, 3)
    for k in (-9, 0, 9):
        L(fd, [(px + k, py - 6), (px + k, py + 8)], FUR_DARK, 2)

    # Кольцо «поп» вокруг рта.
    if st.pop > 0.01:
        radius = 48 + 34 * st.pop
        alpha = int(190 * math.sin(math.pi * st.pop))
        fd.ellipse(
            [sc(300 - radius), sc(338 - radius), sc(300 + radius), sc(338 + radius)],
            outline=(255, 224, 138, alpha),
            width=sc(5),
        )

    draw_sparkles(fd, st.sparkle)
    draw_electric(fd, st.shock)
    if st.sweat > 0.01:
        draw_sweat(fd, 424, 250, st.sweat)
    layer = Image.alpha_composite(layer, fx)

    return layer, st


def make_bg_base(shock: bool = False) -> Image.Image:
    y = np.linspace(0, 1, C, dtype=np.float32).reshape(-1, 1, 1)
    top_color = SHOCK_BG_TOP if shock else BG_TOP
    bottom_color = SHOCK_BG_BOTTOM if shock else BG_BOTTOM
    top = np.array(top_color, dtype=np.float32).reshape(1, 1, 3)
    bottom = np.array(bottom_color, dtype=np.float32).reshape(1, 1, 3)
    arr = (top * (1 - y) + bottom * y).astype(np.uint8)
    arr = np.repeat(arr, C, axis=1)
    bg = Image.fromarray(arr, "RGB").convert("RGBA")

    # Мягкое свечение в центре: золотое или тревожно-красное.
    mask = ImageOps.invert(Image.radial_gradient("L").resize((C, C)))
    glow_color = (255, 120, 110, 0) if shock else (255, 200, 87, 0)
    glow = Image.new("RGBA", (C, C), glow_color)
    glow.putalpha(mask.point(lambda v: int(v * (0.20 if shock else 0.16))))
    bg = Image.alpha_composite(bg, glow)

    deco = Image.new("RGBA", (C, C), (0, 0, 0, 0))
    dd = ImageDraw.Draw(deco)
    dd.ellipse(
        [sc(24), sc(24), sc(576), sc(576)],
        outline=(255, 200, 87, 180),
        width=sc(3),
    )
    dd.ellipse(
        [sc(34), sc(34), sc(566), sc(566)],
        outline=(255, 200, 87, 55),
        width=sc(1.5),
    )
    stars = [
        (78, 96, 2.0, 70), (135, 55, 1.4, 45), (196, 72, 1.2, 40),
        (515, 92, 2.0, 65), (470, 50, 1.3, 40), (555, 175, 1.2, 38),
        (70, 480, 1.6, 45), (110, 535, 1.2, 35), (530, 500, 1.5, 45),
        (575, 420, 1.2, 35), (60, 260, 1.3, 35), (548, 285, 1.2, 35),
    ]
    for x, y, r, a in stars:
        dd.ellipse([sc(x - r), sc(y - r), sc(x + r), sc(y + r)], fill=(255, 220, 130, a))
    bg = Image.alpha_composite(bg, deco)
    return bg


def draw_shadow(bg: Image.Image, bounce: float) -> None:
    strength = 1.0 - min(1.0, abs(bounce) / 18.0)
    alpha = int(95 * strength)
    if alpha <= 0:
        return
    shadow = Image.new("RGBA", (C, C), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    rx = 172 * (0.92 + 0.08 * strength)
    sd.ellipse(
        [sc(300 - rx), sc(548 - 24), sc(300 + rx), sc(548 + 24)],
        fill=(0, 0, 0, alpha),
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=12 * S))
    bg.alpha_composite(shadow)


def compose_frame(
    t: float,
    bg_base: Image.Image,
    alpha_only: bool = False,
    variant: str = "eat",
) -> Image.Image:
    hamster, st = draw_hamster(t, variant)
    offset = (
        int(round(st.shake_x * S)),
        int(round((st.bounce + st.shake_y) * S)),
    )
    if alpha_only:
        out = Image.new("RGBA", (C, C), (0, 0, 0, 0))
        out.paste(hamster, offset, hamster)
        return out

    bg = bg_base.copy()
    draw_shadow(bg, st.bounce)
    layer = Image.new("RGBA", (C, C), (0, 0, 0, 0))
    layer.paste(hamster, offset, hamster)
    return Image.alpha_composite(bg, layer)


def save_gif(frames: list[Image.Image], name: str = "hamster.gif") -> None:
    gif_frames = [f.convert("RGB").resize((480, 480), Image.Resampling.LANCZOS) for f in frames]
    # Единая палитра, чтобы фон не «мигал».
    master = Image.new("RGB", (480 * 3, 480))
    for i, idx in enumerate((0, len(gif_frames) // 3, 2 * len(gif_frames) // 3)):
        master.paste(gif_frames[idx], (480 * i, 0))
    palette = master.quantize(colors=128, method=Image.Quantize.MEDIANCUT)
    quantized = [
        f.quantize(palette=palette, dither=Image.Dither.NONE) for f in gif_frames
    ]
    quantized[0].save(
        OUT / name,
        save_all=True,
        append_images=quantized[1:],
        duration=int(1000 / FPS),
        loop=0,
        optimize=True,
        disposal=2,
    )


def save_webp(frames: list[Image.Image], name: str, alpha: bool = False) -> None:
    frames[0].save(
        OUT / name,
        save_all=True,
        append_images=frames[1:],
        duration=int(1000 / FPS),
        loop=0,
        lossless=False,
        quality=92,
        method=6,
        background=(0, 0, 0, 0) if alpha else (6, 20, 58, 255),
    )


def save_mp4(frames: list[Image.Image], name: str = "hamster.mp4") -> None:
    if FRAME_DIR.exists():
        shutil.rmtree(FRAME_DIR)
    FRAME_DIR.mkdir(parents=True)
    for i, frame in enumerate(frames):
        frame.convert("RGB").resize((W, W), Image.Resampling.LANCZOS).save(
            FRAME_DIR / f"frame_{i:03d}.png"
        )
    subprocess.run(
        [
            "ffmpeg", "-y", "-loglevel", "error",
            "-framerate", str(FPS),
            "-i", str(FRAME_DIR / "frame_%03d.png"),
            "-c:v", "libx264",
            "-pix_fmt", "yuv420p",
            "-movflags", "+faststart",
            str(OUT / name),
        ],
        check=True,
    )
    shutil.rmtree(FRAME_DIR)


def render_variant(variant: str) -> tuple[list[Image.Image], list[Image.Image]]:
    bg_base = make_bg_base(shock=variant == "shock")
    frames: list[Image.Image] = []
    alpha_frames: list[Image.Image] = []
    for i in range(FRAMES):
        t = i / FRAMES
        frame = compose_frame(t, bg_base, variant=variant)
        alpha_frame = compose_frame(t, bg_base, alpha_only=True, variant=variant)
        frames.append(frame.resize((W, W), Image.Resampling.LANCZOS))
        alpha_frames.append(alpha_frame.resize((W, W), Image.Resampling.LANCZOS))
    return frames, alpha_frames


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)

    eat_frames, eat_alpha = render_variant("eat")
    save_gif(eat_frames, "hamster.gif")
    save_webp(eat_frames, "hamster.webp")
    save_webp(eat_alpha, "hamster-alpha.webp", alpha=True)
    save_mp4(eat_frames, "hamster.mp4")
    eat_frames[int(0.72 * FRAMES)].save(OUT / "poster.png")

    shock_frames, shock_alpha = render_variant("shock")
    save_gif(shock_frames, "hamster-shock.gif")
    save_webp(shock_frames, "hamster-shock.webp")
    save_webp(shock_alpha, "hamster-shock-alpha.webp", alpha=True)
    save_mp4(shock_frames, "hamster-shock.mp4")
    shock_frames[int(0.42 * FRAMES)].save(OUT / "poster-shock.png")

    print(f"Готово: 2 анимации по {FRAMES} кадров, {FPS} fps")
    for name in (
        "hamster.gif", "hamster.webp", "hamster-alpha.webp", "hamster.mp4", "poster.png",
        "hamster-shock.gif", "hamster-shock.webp", "hamster-shock-alpha.webp",
        "hamster-shock.mp4", "poster-shock.png",
    ):
        path = OUT / name
        print(f"  {name}: {path.stat().st_size / 1024:.0f} КБ")


if __name__ == "__main__":
    main()
