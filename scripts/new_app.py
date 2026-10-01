#!/usr/bin/env python3
"""Tạo app mới từ template/.

Dùng:
    python3 scripts/new_app.py <slug> <package> "<Tên app>"

Ví dụ:
    python3 scripts/new_app.py wire-size-calc com.ethanstudio.wiresizecalc "Wire Size Calculator"

Script sẽ:
  - copy template/ sang apps/<slug>/ và đổi package, tên app, tên dự án
  - tạo apps/<slug>/APP.md và apps/<slug>/store/ (nội dung store)
  - tạo docs/<slug>/privacy-policy.html (GitHub Pages)
  - thêm một dòng vào portfolio/apps.md và docs/index.html
"""
from __future__ import annotations

import datetime as dt
import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TEMPLATE = ROOT / "template"
TEMPLATES = ROOT / "scripts" / "templates"
TEMPLATE_PKG = "com.example.factorytemplate"
TEMPLATE_PROJECT_NAME = "factory-template"
PAGES_BASE = "https://nguyentranminhnhat3536-gif.github.io/howtogetapp"

JAVA_KEYWORDS = {
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
    "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
    "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
    "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
    "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
    "volatile", "while", "true", "false", "null", "fun", "val", "var", "object", "is", "in", "as",
    "typealias", "when",
}
TEXT_SUFFIXES = {".kt", ".kts", ".xml", ".pro", ".toml", ".properties", ".md"}


def die(msg: str) -> None:
    print(f"LỖI: {msg}", file=sys.stderr)
    sys.exit(1)


def android_string(value: str) -> str:
    """Escape một chuỗi để đặt trong <string> của Android."""
    value = value.replace("\\", "\\\\")
    value = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    value = value.replace("'", "\\'").replace('"', '\\"')
    if value.startswith(("@", "?")):
        value = "\\" + value
    return value


def html_text(value: str) -> str:
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace('"', "&quot;")


def studio_value(label: str) -> str | None:
    """Đọc một giá trị trong portfolio/studio.md, dòng dạng: - **<label>…:** giá trị"""
    studio = ROOT / "portfolio" / "studio.md"
    if not studio.exists():
        return None
    for line in studio.read_text(encoding="utf-8").splitlines():
        if label.lower() in line.lower() and ":**" in line:
            value = line.split(":**", 1)[1].strip()
            return value or None
    return None


def set_string(xml: str, name: str, value: str) -> str:
    pattern = re.compile(rf'(<string name="{name}"[^>]*>)(.*?)(</string>)', re.S)
    if not pattern.search(xml):
        die(f"Không thấy <string name=\"{name}\"> trong strings.xml của template")
    return pattern.sub(lambda m: m.group(1) + value + m.group(3), xml, count=1)


def fill(text: str, values: dict[str, str]) -> str:
    for key, val in values.items():
        text = text.replace("{{" + key + "}}", val)
    return text


def main() -> None:
    if len(sys.argv) != 4:
        print(__doc__)
        sys.exit(1)
    slug, package, app_name = sys.argv[1].strip(), sys.argv[2].strip(), sys.argv[3].strip()

    # --- Kiểm tra đầu vào ---
    if not re.fullmatch(r"[a-z0-9]+(-[a-z0-9]+)*", slug) or len(slug) > 40:
        die("slug chỉ gồm chữ thường a-z, số và dấu gạch nối, tối đa 40 ký tự (ví dụ: wire-size-calc)")
    if slug == "template":
        die('slug không được là "template"')
    if not re.fullmatch(r"[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+", package):
        die("package phải có dạng com.tenban.tenapp (chữ thường, số, dấu chấm, mỗi phần bắt đầu bằng chữ)")
    if package.startswith(("com.example", "com.android", "android.", "com.google")):
        die("package không được bắt đầu bằng com.example / com.android / android / com.google")
    if any(part in JAVA_KEYWORDS for part in package.split(".")):
        die("package chứa từ khóa của Java/Kotlin (ví dụ: new, class, in, is…), hãy đổi tên")
    if not app_name or len(app_name) > 30:
        die("Tên app phải có từ 1 đến 30 ký tự (giới hạn của Google Play)")

    dest = ROOT / "apps" / slug
    if dest.exists():
        die(f"apps/{slug} đã tồn tại")

    prefix = studio_value("Tiền tố package")
    if prefix and not package.startswith(prefix.rstrip(".") + "."):
        print(f"CẢNH BÁO: package không bắt đầu bằng tiền tố trong studio.md ({prefix})")

    email = studio_value("Email hỗ trợ")
    email_match = re.search(r"[\w.+-]+@[\w-]+(\.[\w-]+)+", email or "")
    email = email_match.group(0) if email_match else None
    developer = studio_value("Tên nhà phát triển") or "(chưa điền trong portfolio/studio.md)"
    today = dt.date.today().isoformat()
    privacy_url = f"{PAGES_BASE}/{slug}/privacy-policy.html"

    # --- 1. Copy template ---
    shutil.copytree(
        TEMPLATE,
        dest,
        ignore=shutil.ignore_patterns("build", ".gradle", ".kotlin", ".idea", "local.properties", "*.iml"),
    )

    # --- 2. Chuyển thư mục mã nguồn sang package mới ---
    old_rel = Path(*TEMPLATE_PKG.split("."))
    new_rel = Path(*package.split("."))
    for source_set in ("main", "test", "androidTest"):
        java_dir = dest / "app" / "src" / source_set / "java"
        old_dir = java_dir / old_rel
        if not old_dir.exists():
            continue
        new_dir = java_dir / new_rel
        new_dir.parent.mkdir(parents=True, exist_ok=True)
        shutil.move(str(old_dir), str(new_dir))
        # Xóa các thư mục rỗng còn lại (com/example)
        parent = old_dir.parent
        while parent != java_dir and parent.exists() and not any(parent.iterdir()):
            parent.rmdir()
            parent = parent.parent

    # --- 3. Thay package và tên dự án trong các file chữ ---
    for path in dest.rglob("*"):
        if path.is_file() and path.suffix in TEXT_SUFFIXES:
            text = path.read_text(encoding="utf-8")
            new_text = text.replace(TEMPLATE_PKG, package)
            if path.name == "settings.gradle.kts":
                new_text = new_text.replace(f'"{TEMPLATE_PROJECT_NAME}"', f'"{slug}"')
            if new_text != text:
                path.write_text(new_text, encoding="utf-8")

    # --- 4. strings.xml: tên app, link chính sách, email hỗ trợ ---
    strings_path = dest / "app" / "src" / "main" / "res" / "values" / "strings.xml"
    strings = strings_path.read_text(encoding="utf-8")
    strings = set_string(strings, "app_name", android_string(app_name))
    strings = set_string(strings, "privacy_policy_url", privacy_url)
    if email:
        strings = set_string(strings, "support_email", email)
    strings_path.write_text(strings, encoding="utf-8")

    values = {
        "APP_NAME": app_name,
        "SLUG": slug,
        "PACKAGE": package,
        "DATE": today,
        "PRIVACY_URL": privacy_url,
    }

    # --- 5. Hồ sơ app và nội dung store ---
    (dest / "APP.md").write_text(fill((TEMPLATES / "APP.md").read_text(encoding="utf-8"), values), encoding="utf-8")
    store = dest / "store"
    (store / "graphics" / "screenshots").mkdir(parents=True, exist_ok=True)
    for name in ("listing.en.md", "listing.vi.md"):
        (store / name).write_text(fill((TEMPLATES / name).read_text(encoding="utf-8"), values), encoding="utf-8")

    # --- 6. Trang chính sách quyền riêng tư ---
    html_values = {k: html_text(v) for k, v in values.items()}
    html_values["DEVELOPER"] = html_text(developer)
    html_values["EMAIL"] = html_text(email or "support@example.com")
    policy_dir = ROOT / "docs" / slug
    policy_dir.mkdir(parents=True, exist_ok=True)
    (policy_dir / "privacy-policy.html").write_text(
        fill((TEMPLATES / "privacy-policy.html").read_text(encoding="utf-8"), html_values), encoding="utf-8"
    )

    # --- 7. Bảng theo dõi và trang chủ GitHub Pages ---
    apps_md = ROOT / "portfolio" / "apps.md"
    if apps_md.exists():
        text = apps_md.read_text(encoding="utf-8").rstrip("\n") + "\n"
        text += f"| {app_name} | `{slug}` | `{package}` | 2 · Đang code | 1.0.0 (1) | — | — | Tạo {today} |\n"
        apps_md.write_text(text, encoding="utf-8")
    index = ROOT / "docs" / "index.html"
    marker = "<!-- APPS -->"
    if index.exists() and marker in index.read_text(encoding="utf-8"):
        item = f'<li><strong>{html_text(app_name)}</strong> · <a href="{slug}/privacy-policy.html">Privacy policy</a></li>\n  {marker}'
        index.write_text(index.read_text(encoding="utf-8").replace(marker, item, 1), encoding="utf-8")

    print(f"Đã tạo apps/{slug}  (package {package})")
    print(f"  - Hồ sơ app:        apps/{slug}/APP.md")
    print(f"  - Nội dung store:   apps/{slug}/store/")
    print(f"  - Chính sách:       docs/{slug}/privacy-policy.html  →  {privacy_url}")
    if not email:
        print("CẢNH BÁO: chưa có email hỗ trợ trong portfolio/studio.md, strings.xml vẫn đang để support@example.com")
    print("Tiếp theo: điền APP.md, làm icon và màu riêng, rồi commit, push và chạy scripts/ci_status.py --wait")


if __name__ == "__main__":
    main()
