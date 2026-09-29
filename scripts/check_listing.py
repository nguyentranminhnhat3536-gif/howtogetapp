#!/usr/bin/env python3
"""Kiểm tra nội dung trang store (độ dài, từ cấm, chữ giữ chỗ) theo giới hạn của Google Play.

Dùng:
    python3 scripts/check_listing.py <slug>

Đọc apps/<slug>/store/listing.*.md. Mỗi file có các mục "## Title", "## Short description",
"## Full description", "## Release notes". Dòng chú thích <!-- ... --> được bỏ qua.
Thoát với mã 1 nếu có LỖI.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

LIMITS = {
    "title": 30,
    "short description": 80,
    "full description": 4000,
    "release notes": 500,
}
# Chính sách metadata của Google Play: không dùng các từ này trong tiêu đề
TITLE_BANNED = [
    r"\bbest\b", r"#\s*1\b", r"\bnumber\s+one\b", r"\btop\b", r"\bfree\b", r"\bno\s+ads?\b",
    r"\bdownload\b", r"\bsale\b", r"\bdiscount\b",
    r"miễn phí", r"tốt nhất", r"số\s*1", r"không quảng cáo",
]
# Có thể bị coi là quảng cáo/khuyến mãi tùy ngữ cảnh -> chỉ cảnh báo
TITLE_RISKY = [r"\bnew\b", r"\bhot\b", r"\bmới\b"]
PLACEHOLDERS = [r"lorem ipsum", r"\bTODO\b", r"\bTBD\b", r"\{\{", r"\}\}", r"xxx"]
EMOJI = re.compile(
    "[\U0001F000-\U0001FAFF\U00002600-\U000027BF\U0001F900-\U0001F9FF⭐⬆↔-⇿️]"
)


def parse(path: Path) -> dict:
    text = re.sub(r"<!--.*?-->", "", path.read_text(encoding="utf-8"), flags=re.S)
    sections, current = {}, None
    for line in text.splitlines():
        heading = re.match(r"^##\s+(.+?)\s*$", line)
        if heading:
            current = heading.group(1).strip().lower()
            sections[current] = []
        elif current is not None:
            sections[current].append(line)
    return {k: "\n".join(v).strip() for k, v in sections.items()}


def check(path: Path) -> tuple[list, list]:
    errors, warnings = [], []
    sections = parse(path)
    for key, limit in LIMITS.items():
        value = sections.get(key)
        if value is None:
            errors.append(f'Thiếu mục "## {key.capitalize()}"')
            continue
        if not value:
            errors.append(f"{key.capitalize()}: đang trống")
            continue
        n = len(value)
        if n > limit:
            errors.append(f"{key.capitalize()}: {n}/{limit} ký tự, dài quá {n - limit}")
        for pattern in PLACEHOLDERS:
            if re.search(pattern, value, re.I):
                errors.append(f'{key.capitalize()}: còn chữ giữ chỗ ("{pattern}")')
    title = sections.get("title", "")
    if title:
        if "\n" in title:
            errors.append("Title: chỉ được một dòng")
        for pattern in TITLE_BANNED:
            if re.search(pattern, title, re.I):
                errors.append(f'Title: có từ bị Google cấm trong tiêu đề ("{pattern}")')
        for pattern in TITLE_RISKY:
            if re.search(pattern, title, re.I):
                warnings.append(f'Title: từ "{pattern}" có thể bị coi là khuyến mãi, cân nhắc bỏ')
        if EMOJI.search(title):
            errors.append("Title: có emoji")
        if re.search(r"\b[A-Z]{5,}\b", title):
            warnings.append("Title: có từ viết HOA toàn bộ (chỉ được phép nếu đó là tên thương hiệu)")
        if re.search(r"([!?.*~])\1", title):
            errors.append("Title: lặp ký tự đặc biệt")
    short = sections.get("short description", "")
    if short:
        if EMOJI.search(short):
            warnings.append("Short description: có emoji (nên bỏ)")
        if re.search(r"\bbest\b|#\s*1\b|\btop\b|tốt nhất|số\s*1", short, re.I):
            warnings.append('Short description: có "best / #1 / top", dễ bị coi là tự nhận xếp hạng')
    full = sections.get("full description", "")
    if full and re.search(r"★|⭐|\bstars?\b.*\breview", full, re.I):
        warnings.append("Full description: tránh trích đánh giá hay số sao (dễ vi phạm chính sách)")
    return errors, warnings


def main() -> None:
    if len(sys.argv) != 2:
        print(__doc__)
        sys.exit(1)
    slug = sys.argv[1]
    files = sorted((ROOT / "apps" / slug / "store").glob("listing.*.md"))
    if not files:
        sys.exit(f"LỖI: không thấy apps/{slug}/store/listing.*.md")
    total_errors = 0
    for path in files:
        errors, warnings = check(path)
        total_errors += len(errors)
        status = "ĐẠT" if not errors else f"{len(errors)} LỖI"
        print(f"\n== {path.relative_to(ROOT)}: {status}")
        for e in errors:
            print(f"  LỖI:      {e}")
        for w in warnings:
            print(f"  Lưu ý:    {w}")
    sys.exit(1 if total_errors else 0)


if __name__ == "__main__":
    main()
