#!/usr/bin/env python3
"""
Generate Android adaptive icon drawables with clean $ sign.
- Background: 432x432 solid #1A73E8
- Foreground: 432x432 transparent with crisp white $ centered (within 72dp safe zone)
- Monochrome: same as foreground but optimized for themed icons (white silhouette)
- Mipmap webps: composite bg+fg and resize to density sizes, with circ mask for round
Best practices verified via websearch 2026-09-01:
 - Adaptive icon layers 108dp = 432px @ xxxhdpi, safe zone 72dp = 288px centered (66%)
 - Bold simple shape, high contrast, no text, centered, monochrome for themed icons Android 13+
 - Material Design $ (attach_money) style: centered vertical stroke, bold S-curve, stroke weight ~12-14% of height
"""
from PIL import Image, ImageDraw, ImageFont
import os

SIZE = 432
BG_COLOR = (0x1A, 0x73, 0xE8, 255)  # ExpressivePrimary
WHITE = (255, 255, 255, 255)
TRANSPARENT = (0, 0, 0, 0)

# Paths
DRAWABLE_DIR = "app/src/main/res/drawable"
MIPMAP_BASE = "app/src/main/res/mipmap"

# Densities for legacy/composite webp
DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

def find_font():
    candidates = [
        "/usr/share/fonts/julietaula-montserrat-fonts/Montserrat-Bold.otf",
        "/usr/share/fonts/google-noto-vf/NotoSans[wght].ttf",
        "/usr/share/fonts/google-droid-sans-fonts/DroidSans-Bold.ttf",
        "/usr/share/fonts/adobe-source-code-pro-fonts/SourceCodePro-Bold.otf",
        "/usr/share/fonts/liberation-sans-fonts/LiberationSans-Bold.ttf",
    ]
    for p in candidates:
        if os.path.exists(p):
            return p
    # fallback find any ttf
    import glob
    for f in glob.glob("/usr/share/fonts/**/*.ttf", recursive=True):
        return f
    return None

FONT_PATH = find_font()
print(f"Using font: {FONT_PATH}")

# Supersample factor for crisp antialiasing
SS = 4
SS_SIZE = SIZE * SS  # 1728

def draw_dollar_image(size_px, supersample=4):
    """Create transparent image with white $ centered. Returns PIL Image size_px."""
    ss = supersample
    ss_size = size_px * ss
    img = Image.new("RGBA", (ss_size, ss_size), TRANSPARENT)
    draw = ImageDraw.Draw(img)

    # Use font rendering for material-like $; size chosen to fit within safe zone.
    # Safe zone at xxxhdpi = 288px; $ should be ~ 60-66% of that ~ 180-200px tall at 432px.
    # At supersampled size, height ~ 800px.
    # Try font size 1100 ss pixels => height ~ 750-800.
    font_size = int(800) * 1  # tuned for SS=4, will be ~800 at 1728 canvas -> ~200px at 432 after downscale; keep in safe zone 288
    # Actually ss_size is 1728, so font_size 800 gives ~800px glyph height at ss, ~200px final
    font_size = int(font_size * ss // 1) if False else 820  # final 820 at SS canvas after testing
    # Correct: for ss=4, canvas 1728, we want final glyph ~190px -> ss glyph 760px -> font_size ~760
    font_size = 760
    # For variable font, weight axis not handled by PIL; use Bold font file (Montserrat Bold) which is ideal
    # Monserrat Bold gives clean geometric $ with vertical bar centered, good for icons.
    try:
        font = ImageFont.truetype(FONT_PATH, font_size)
    except Exception as e:
        print(f"font load failed {e}, fallback default")
        font = ImageFont.load_default()

    text = "$"
    # Get bbox
    bbox = draw.textbbox((0,0), text, font=font)
    tw = bbox[2]-bbox[0]
    th = bbox[3]-bbox[1]
    print(f"text bbox {bbox} tw={tw} th={th} ss_size={ss_size}")
    # Center: anchor mm ensures centered at given xy
    # Use anchor="mm" for middle-middle
    x = ss_size // 2
    y = ss_size // 2
    # Draw white $
    # Slight vertical offset: typographic "$" often slightly above center; adjust + 0.02*ss_size down
    # Use anchor mm
    try:
        draw.text((x, y), text, font=font, fill=WHITE, anchor="mm")
    except TypeError:
        # older Pillow without anchor: manual offset
        draw.text((x - tw//2 - bbox[0], y - th//2 - bbox[1]), text, font=font, fill=WHITE)

    # Downscale with high quality
    img_small = img.resize((size_px, size_px), Image.LANCZOS)
    return img_small

def create_background():
    img = Image.new("RGBA", (SIZE, SIZE), BG_COLOR)
    img.save(os.path.join(DRAWABLE_DIR, "ic_launcher_background.png"), "PNG")
    print("background saved")

def create_foreground():
    fg = draw_dollar_image(SIZE, SS)
    fg.save(os.path.join(DRAWABLE_DIR, "ic_launcher_foreground.png"), "PNG")
    print("foreground saved")

def create_monochrome():
    # For themed icons: single-color white silhouette on transparent - same as foreground but ensure pure white alpha
    # We reuse same draw but ensure no extra effects; could be identical to foreground per spec
    # To be strictly monochrome, we make sure only white with alpha, no other colors.
    mono = draw_dollar_image(SIZE, SS)
    mono.save(os.path.join(DRAWABLE_DIR, "ic_launcher_monochrome.png"), "PNG")
    print("monochrome saved")

def create_mipmaps():
    bg = Image.open(os.path.join(DRAWABLE_DIR, "ic_launcher_background.png")).convert("RGBA")
    fg = Image.open(os.path.join(DRAWABLE_DIR, "ic_launcher_foreground.png")).convert("RGBA")
    # Ensure both 432
    assert bg.size == (SIZE, SIZE)
    assert fg.size == (SIZE, SIZE)
    # Composite at full res
    composite_full = Image.alpha_composite(bg, fg)  # 432
    for density, px in DENSITIES.items():
        dir_path = os.path.join(MIPMAP_BASE + f"-{density}")
        os.makedirs(dir_path, exist_ok=True)
        # Resize composite to px
        resized = composite_full.resize((px, px), Image.LANCZOS)
        # Save webp
        resized.save(os.path.join(dir_path, "ic_launcher.webp"), "WEBP", quality=95, method=6)
        # Round variant: circular mask
        # Create circular mask with antialiasing
        # For round, we clip to circle; background already solid, so just mask
        # High-res mask then downscale is already done; apply circle mask at px size with supersampled edge
        # Create mask at 4x px then downscale for smooth edge
        m_ss = px * 4
        mask_ss = Image.new("L", (m_ss, m_ss), 0)
        d = ImageDraw.Draw(mask_ss)
        d.ellipse((0,0,m_ss,m_ss), fill=255)
        mask = mask_ss.resize((px, px), Image.LANCZOS)
        # Apply mask
        round_img = resized.copy()
        # Use mask as alpha: multiply existing alpha with mask
        # Since resized has alpha 255, we can set alpha = mask
        r, g, b, a = round_img.split()
        # Blend alpha with mask (a * mask /255)
        # Simple: set alpha to mask (since a is 255)
        # For pixels with alpha already 255, new alpha = mask
        # For more correctness: Image.composite with mask
        round_img.putalpha(mask)
        # But need to keep RGB where mask 0 transparent
        round_img.save(os.path.join(dir_path, "ic_launcher_round.webp"), "WEBP", quality=95, method=6)
        print(f"{density} {px}px webps saved")

if __name__ == "__main__":
    create_background()
    create_foreground()
    create_monochrome()
    create_mipmaps()
    print("All done")
