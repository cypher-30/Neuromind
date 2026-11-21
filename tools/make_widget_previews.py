"""Draws widget-picker preview images for Neuromind's six widgets.

Usage: python -I tools/make_widget_previews.py app/src/main/res   (needs Pillow)
Writes <res>/drawable-nodpi/widget_preview_<name>.png at 3x density.
Colours/fonts mirror ui/widget/WidgetColors.kt (neutral light palette) and res/font.
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

RES = Path(sys.argv[1])
OUT = RES / "drawable-nodpi"
OUT.mkdir(exist_ok=True)
D = 3  # px per dp

BG = (250, 250, 247, 255)


def over_bg(rgb, a):
    """Pre-mixes a translucent colour onto the card so drawing doesn't punch holes."""
    return tuple(round(c * a / 255 + b * (1 - a / 255)) for c, b in zip(rgb, BG[:3])) + (255,)


TEXT = (28, 27, 26, 255)
MUTED = over_bg((28, 27, 26), 153)
ACCENT = (164, 85, 43, 255)
DIVIDER = over_bg((28, 27, 26), 31)
DANGER = (179, 65, 47, 255)


def font(size_sp, bold=False):
    f = ImageFont.truetype(str(RES / "font" / "figtree.ttf"), int(size_sp * D))
    try:
        f.set_variation_by_name("Bold" if bold else "Regular")
    except Exception:
        pass
    return f


def card(w_dp, h_dp):
    im = Image.new("RGBA", (w_dp * D, h_dp * D), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([0, 0, im.width - 1, im.height - 1], radius=20 * D, fill=BG)
    return im, d


class Pen:
    """Writes lines top-down inside a card."""

    def __init__(self, d, x_dp=14, y_dp=12):
        self.d, self.x, self.y = d, x_dp * D, y_dp * D

    def text(self, s, size=14, color=TEXT, bold=False, gap=3, x=None):
        f = font(size, bold)
        self.d.text((x if x is not None else self.x, self.y), s, font=f, fill=color)
        self.y += int(size * D * 1.25) + gap * D

    def heading(self, s):
        self.text(s.upper(), 11, ACCENT, bold=True, gap=4)

    def space(self, dp):
        self.y += dp * D

    def divider(self, w_px):
        self.space(3)
        self.d.line([self.x, self.y, w_px - 14 * D, self.y], fill=DIVIDER, width=D)
        self.space(6)


def save(im, name):
    im.save(OUT / f"widget_preview_{name}.png")
    print("wrote", name, im.size)


# Today (3x2)
im, d = card(250, 140)
p = Pen(d)
p.heading("Priorities")
for title, overdue in [("Sign Assembly attendance", False), ("AI Deliverable 3 notebook", False), ("Go to Parklands", True)]:
    d.ellipse([p.x, p.y + 6 * D, p.x + 6 * D, p.y + 12 * D], fill=DANGER if overdue else ACCENT)
    p.text(title, 13, x=p.x + 14 * D, gap=2)
p.divider(im.width)
p.text("Next: Maisha Program · Mon 10:15 AM", 12, ACCENT)
save(im, "today")

# Next up (3x2)
im, d = card(250, 140)
p = Pen(d)
p.heading("Next up")
p.text("Mobile Application", 16, bold=True, gap=0)
p.text("Development", 16, bold=True)
p.text("Tomorrow · 8:15 AM – 10:15 AM", 13, MUTED)
p.text("Forge F1 04", 13, ACCENT)
save(im, "next_up")

# Daily progress (3x2)
im, d = card(250, 140)
p = Pen(d)
p.heading("Today's progress")
d.text((p.x, p.y), "2", font=font(28, True), fill=TEXT)
d.text((p.x + 20 * D, p.y + 12 * D), "of 5 done", font=font(14), fill=MUTED)
p.space(40)
bar_w = im.width - 28 * D
d.rounded_rectangle([p.x, p.y, p.x + bar_w, p.y + 8 * D], radius=4 * D, fill=DIVIDER)
d.rounded_rectangle([p.x, p.y, p.x + int(bar_w * 0.4), p.y + 8 * D], radius=4 * D, fill=ACCENT)
p.space(16)
p.text("Go to Upperhill · 2:45 PM", 12)
p.text("Meeting with mentor · 3:15 PM", 12)
save(im, "progress")

# Activity heat (3x2)
im, d = card(250, 140)
p = Pen(d)
p.heading("Activity · 4 weeks")
levels = [0, 1, 0, 2, 1, 0, 0, 1, 3, 2, 0, 1, 2, 0, 0, 2, 1, 3, 2, 1, 0, 1, 2, 3, 3, 1, 2, 1]
alpha = {0: 20, 1: 89, 2: 166, 3: 255}
for i, lv in enumerate(levels):
    r, c = divmod(i, 7)
    x = p.x + c * 18 * D
    y = p.y + r * 18 * D
    color = over_bg((28, 27, 26), alpha[0]) if lv == 0 else over_bg((164, 85, 43), alpha[lv])
    d.rounded_rectangle([x, y, x + 14 * D, y + 14 * D], radius=3 * D, fill=color)
p.space(4 * 18 + 2)
p.text("34 tasks done", 12, MUTED)
save(im, "activity")

# Exam countdown (4x2 look, so the big number shows in the picker)
im, d = card(250, 140)
p = Pen(d)
p.heading("Next exam")
top = p.y
d.text((p.x + 4 * D, top), "10", font=font(40, True), fill=ACCENT)
d.text((p.x + 10 * D, top + 52 * D), "days", font=font(12), fill=MUTED)
x2 = p.x + 82 * D
p.text("Mobile App Dev", 15, bold=True, x=x2, gap=1)
p.text("Wed 2 Dec, 4:00 PM", 12, MUTED, x=x2, gap=1)
p.text("Forge F2-04", 12, ACCENT, x=x2)
p.text("Spanish IV · Fri 4 Dec", 12, x=x2, gap=1)
p.text("Server Admin · Mon 7 Dec", 12, x=x2)
save(im, "exams")

# Mini quick log (1x1)
im, d = card(70, 70)
fw = font(16, True)
tw = d.textlength("Log", font=fw)
d.text(((im.width - tw) / 2, 18 * D), "Log", font=fw, fill=ACCENT)
fs = font(11)
tw = d.textlength("voice", font=fs)
d.text(((im.width - tw) / 2, 40 * D), "voice", font=fs, fill=MUTED)
save(im, "quick_log")
