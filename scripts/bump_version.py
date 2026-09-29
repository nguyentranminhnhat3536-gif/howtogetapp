#!/usr/bin/env python3
"""Tăng version của một app trước khi phát hành.

Dùng:
    python3 scripts/bump_version.py <slug>            # 1.2.3 -> 1.2.4 (sửa lỗi nhỏ)
    python3 scripts/bump_version.py <slug> minor      # 1.2.3 -> 1.3.0 (thêm tính năng)
    python3 scripts/bump_version.py <slug> major      # 1.2.3 -> 2.0.0 (thay đổi lớn)

versionCode luôn tăng thêm 1 (Google Play bắt buộc mỗi bản tải lên phải có versionCode lớn hơn bản trước).
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def main() -> None:
    if len(sys.argv) not in (2, 3):
        print(__doc__)
        sys.exit(1)
    slug = sys.argv[1]
    part = sys.argv[2] if len(sys.argv) == 3 else "patch"
    if part not in ("patch", "minor", "major"):
        sys.exit("LỖI: loại tăng phải là patch, minor hoặc major")

    path = ROOT / "apps" / slug / "app" / "build.gradle.kts"
    if not path.exists():
        sys.exit(f"LỖI: không thấy {path.relative_to(ROOT)}")
    text = path.read_text(encoding="utf-8")

    code_match = re.search(r"versionCode\s*=\s*(\d+)", text)
    name_match = re.search(r'versionName\s*=\s*"(\d+)\.(\d+)\.(\d+)"', text)
    if not code_match or not name_match:
        sys.exit('LỖI: không đọc được versionCode / versionName (cần dạng versionName = "1.2.3")')

    old_code = int(code_match.group(1))
    major, minor, patch = (int(x) for x in name_match.groups())
    if part == "major":
        major, minor, patch = major + 1, 0, 0
    elif part == "minor":
        minor, patch = minor + 1, 0
    else:
        patch += 1
    new_code = old_code + 1
    new_name = f"{major}.{minor}.{patch}"
    old_name = ".".join(name_match.groups())

    text = text[: code_match.start(1)] + str(new_code) + text[code_match.end(1):]
    text = re.sub(r'versionName\s*=\s*"\d+\.\d+\.\d+"', f'versionName = "{new_name}"', text, count=1)
    path.write_text(text, encoding="utf-8")
    print(f"{slug}: {old_name} ({old_code}) -> {new_name} ({new_code})")


if __name__ == "__main__":
    main()
