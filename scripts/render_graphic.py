#!/usr/bin/env python3
"""Xuất file HTML hoặc SVG ra ảnh PNG đúng kích thước (dùng Chromium của Playwright có sẵn trong phiên).

Dùng:
    python3 scripts/render_graphic.py <vào.html|vào.svg> <ra.png> <rộng> <cao> [--opaque]

    --opaque   bỏ kênh trong suốt (bắt buộc với feature graphic và ảnh chụp màn hình: PNG 24-bit)

Ví dụ:
    python3 scripts/render_graphic.py apps/x/store/graphics/icon.svg apps/x/store/graphics/icon-512.png 512 512
    python3 scripts/render_graphic.py feature.html apps/x/store/graphics/feature-1024x500.png 1024 500 --opaque

Mẹo: dùng font có sẵn trên máy (sans-serif) vì mạng tới Google Fonts có thể bị chặn. Luôn mở ảnh ra xem lại.
"""
import sys
from pathlib import Path


def main() -> None:
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    opaque = "--opaque" in sys.argv
    if len(args) != 4:
        print(__doc__)
        sys.exit(1)
    src, out = Path(args[0]).resolve(), Path(args[1]).resolve()
    width, height = int(args[2]), int(args[3])
    if not src.exists():
        sys.exit(f"LỖI: không thấy {src}")
    out.parent.mkdir(parents=True, exist_ok=True)

    from playwright.sync_api import sync_playwright  # có sẵn trong phiên cloud

    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": width, "height": height}, device_scale_factor=1)
        if src.suffix.lower() == ".svg":
            svg = src.read_text(encoding="utf-8")
            page.set_content(
                "<!doctype html><html><head><style>"
                "html,body{margin:0;padding:0;background:transparent}"
                f"#c{{width:{width}px;height:{height}px}}#c svg{{width:100%;height:100%;display:block}}"
                f"</style></head><body><div id='c'>{svg}</div></body></html>"
            )
        else:
            page.goto(src.as_uri())
        page.wait_for_load_state("networkidle")
        page.screenshot(
            path=str(out),
            clip={"x": 0, "y": 0, "width": width, "height": height},
            omit_background=not opaque,
        )
        browser.close()

    from PIL import Image

    img = Image.open(out)
    if opaque:
        # PNG 24-bit (không kênh alpha) cho feature graphic và ảnh chụp màn hình
        if img.mode != "RGB":
            background = Image.new("RGB", img.size, (255, 255, 255))
            background.paste(img, mask=img.split()[-1] if img.mode in ("RGBA", "LA") else None)
            background.save(out, "PNG")
    elif img.mode != "RGBA":
        # Icon 512 của Google Play phải là PNG 32-bit (có kênh alpha)
        img.convert("RGBA").save(out, "PNG")

    print(f"Đã xuất {out} ({width}x{height}{', không trong suốt' if opaque else ''})")


if __name__ == "__main__":
    main()
