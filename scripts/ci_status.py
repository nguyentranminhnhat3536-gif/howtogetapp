#!/usr/bin/env python3
"""Xem kết quả CI (GitHub Actions) ngay trong phiên AI, không cần mở trình duyệt.

Dùng:
    python3 scripts/ci_status.py                     # kết quả CI của commit hiện tại (HEAD)
    python3 scripts/ci_status.py --wait              # chờ CI chạy xong rồi mới in kết quả
    python3 scripts/ci_status.py --run <id>          # xem một lần chạy cụ thể
    python3 scripts/ci_status.py --dispatch <slug> [--release] [--wait]
                                                     # bấm "Run workflow" (build bằng tay; --release = AAB đã ký)

In ra: kết quả từng job, lỗi build (CI gom sẵn thành annotation), quyền app xin, link bản chạy thử, artifacts.
Mã thoát: 0 = xanh, 1 = đỏ, 2 = không có dữ liệu / hết giờ / lỗi gọi API.
"""
from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import re
import ssl
import subprocess
import sys
import time
import urllib.error
import urllib.request

DEFAULT_REPO = "nguyentranminhnhat3536-gif/howtogetapp"
WORKFLOW_FILE = "build.yml"
API = "https://api.github.com"
POLL_SECONDS = 15
ICON = {"success": "✅", "failure": "❌", "cancelled": "⛔", "skipped": "⏭️", "timed_out": "⌛", None: "⏳"}

_auth_mode: str | None = None  # "proxy" (không gửi token) hoặc "token"


def git(*args: str) -> str:
    try:
        return subprocess.run(["git", *args], capture_output=True, text=True, check=True).stdout.strip()
    except (subprocess.CalledProcessError, FileNotFoundError):
        return ""


def detect_repo() -> str:
    if os.environ.get("XUONG_REPO"):
        return os.environ["XUONG_REPO"]
    url = git("remote", "get-url", "origin")
    m = re.search(r"([\w.-]+)/([\w.-]+?)(?:\.git)?/?$", url)
    if m and m.group(2) == DEFAULT_REPO.split("/")[1]:
        return f"{m.group(1)}/{m.group(2)}"
    return DEFAULT_REPO


def ssl_context() -> ssl.SSLContext:
    for var in ("SSL_CERT_FILE", "REQUESTS_CA_BUNDLE", "CURL_CA_BUNDLE"):
        path = os.environ.get(var)
        if path and os.path.exists(path):
            return ssl.create_default_context(cafile=path)
    return ssl.create_default_context()


def gh(method: str, path: str, body: dict | None = None) -> tuple[int, object]:
    """Gọi GitHub API. Thử không gửi token trước (proxy của phiên tự gắn quyền), sau đó thử GITHUB_TOKEN."""
    global _auth_mode
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    modes = [_auth_mode] if _auth_mode else (["proxy", "token"] if token else ["proxy"])
    data = json.dumps(body).encode() if body is not None else None
    status, payload = 0, None
    for mode in modes:
        headers = {
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "xuong-app-ci-status",
        }
        if data is not None:
            headers["Content-Type"] = "application/json"
        if mode == "token" and token:
            headers["Authorization"] = f"Bearer {token}"
        req = urllib.request.Request(API + path, data=data, method=method, headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=30, context=ssl_context()) as resp:
                raw = resp.read()
                _auth_mode = mode
                return resp.status, (json.loads(raw) if raw else None)
        except urllib.error.HTTPError as e:
            raw = e.read()
            try:
                payload = json.loads(raw) if raw else None
            except json.JSONDecodeError:
                payload = raw.decode(errors="replace")
            status = e.code
            if status in (401, 403, 404) and mode != modes[-1]:
                continue
            return status, payload
        except (urllib.error.URLError, TimeoutError, OSError) as e:
            status, payload = 0, str(e)
    return status, payload


def api_get(path: str) -> dict:
    status, payload = gh("GET", path)
    if status != 200:
        msg = payload.get("message") if isinstance(payload, dict) else payload
        print(f"LỖI gọi GitHub API {path}: {status} {msg}")
        if status in (0, 401, 403, 404):
            print("Có thể phiên này chưa được cấp quyền vào repo, hoặc mạng bị chặn. "
                  "Nhờ Ethan mở tab Actions trên GitHub và chụp màn hình kết quả.")
        sys.exit(2)
    return payload  # type: ignore[return-value]


def is_build_workflow(run: dict) -> bool:
    return run.get("path", "").endswith(WORKFLOW_FILE) or run.get("name") == "Build apps"


def print_run(repo: str, run: dict) -> int:
    conclusion = run.get("conclusion")
    done = run.get("status") == "completed"
    head = f"Lần chạy #{run['run_number']} · {run.get('event')} · nhánh {run.get('head_branch')} · commit {run['head_sha'][:7]}"
    print("=" * len(head))
    print(head)
    print(f"Kết quả: {ICON.get(conclusion if done else None, '•')} {conclusion if done else run.get('status')}")
    print(f"Link: {run['html_url']}")

    jobs = api_get(f"/repos/{repo}/actions/runs/{run['id']}/jobs?per_page=50").get("jobs", [])
    for job in jobs:
        jc = job.get("conclusion") if job.get("status") == "completed" else None
        print(f"\n- {job['name']}: {ICON.get(jc, '•')} {jc or job.get('status')}")
        status, notes = gh("GET", f"/repos/{repo}/check-runs/{job['id']}/annotations?per_page=50")
        if status != 200 or not isinstance(notes, list):
            continue
        for note in notes:
            message = (note.get("message") or "").strip()
            if "Node.js" in message and "deprecated" in message:
                continue  # cảnh báo hạ tầng của GitHub, bỏ qua
            level = note.get("annotation_level", "")
            title = note.get("title") or ""
            print(f"  [{level}] {title}")
            for line in message.splitlines()[:60]:
                print(f"      {line}")

    if done:
        artifacts = api_get(f"/repos/{repo}/actions/runs/{run['id']}/artifacts").get("artifacts", [])
        if artifacts:
            items = ", ".join(f"{a['name']} ({a['size_in_bytes'] / 1_048_576:.1f} MB)" for a in artifacts)
            print(f"\nArtifacts (tải ở mục Artifacts cuối trang Link): {items}")
    return 0 if conclusion == "success" else 1


def wait_for_runs(repo: str, find_runs, first_wait: int = 150, timeout: int = 45 * 60) -> list[dict]:
    start = time.time()
    runs: list[dict] = []
    while time.time() - start < timeout:
        runs = find_runs()
        if not runs and time.time() - start > first_wait:
            return []
        if runs and all(r.get("status") == "completed" for r in runs):
            return runs
        elapsed = int(time.time() - start)
        state = ", ".join(f"#{r['run_number']} {r.get('status')}" for r in runs) or "chưa thấy lần chạy nào"
        print(f"… {elapsed // 60}:{elapsed % 60:02d} · {state}", flush=True)
        time.sleep(POLL_SECONDS)
    print("Hết giờ chờ (45 phút). Kết quả hiện tại:")
    return runs


def runs_for_sha(repo: str, sha: str) -> list[dict]:
    data = api_get(f"/repos/{repo}/actions/runs?head_sha={sha}&per_page=20")
    return [r for r in data.get("workflow_runs", []) if is_build_workflow(r)]


def main() -> None:
    parser = argparse.ArgumentParser(description="Xem kết quả GitHub Actions của Xưởng App")
    parser.add_argument("--wait", action="store_true", help="chờ tới khi CI chạy xong")
    parser.add_argument("--run", type=int, help="ID của một lần chạy cụ thể")
    parser.add_argument("--dispatch", metavar="SLUG", help='build bằng tay cho apps/<SLUG> (hoặc "template")')
    parser.add_argument("--release", action="store_true", help="dùng với --dispatch: build AAB đã ký")
    args = parser.parse_args()
    repo = detect_repo()

    if args.run:
        sys.exit(print_run(repo, api_get(f"/repos/{repo}/actions/runs/{args.run}")))

    branch = git("rev-parse", "--abbrev-ref", "HEAD") or "main"

    if args.dispatch:
        if not re.fullmatch(r"[a-z0-9]+(-[a-z0-9]+)*", args.dispatch):
            sys.exit("LỖI: slug không hợp lệ")
        since = dt.datetime.now(dt.timezone.utc) - dt.timedelta(seconds=10)
        body = {"ref": branch, "inputs": {"app": args.dispatch, "release": "true" if args.release else "false"}}
        status, payload = gh("POST", f"/repos/{repo}/actions/workflows/{WORKFLOW_FILE}/dispatches", body)
        if status not in (200, 204):
            msg = payload.get("message") if isinstance(payload, dict) else payload
            print(f"Không bấm được Run workflow qua API ({status} {msg}).")
            print(f"Nhờ Ethan: https://github.com/{repo}/actions/workflows/{WORKFLOW_FILE} → Run workflow → "
                  f"app = {args.dispatch}{', tick release' if args.release else ''}")
            sys.exit(2)
        print(f"Đã yêu cầu build {args.dispatch} ({'release' if args.release else 'debug'}) trên nhánh {branch}.")

        def find_dispatched() -> list[dict]:
            data = api_get(f"/repos/{repo}/actions/workflows/{WORKFLOW_FILE}/runs"
                           f"?event=workflow_dispatch&branch={branch}&per_page=10")
            fresh = [r for r in data.get("workflow_runs", [])
                     if dt.datetime.fromisoformat(r["created_at"].replace("Z", "+00:00")) >= since]
            return fresh[:1]

        runs = wait_for_runs(repo, find_dispatched, first_wait=90) if args.wait else []
        if not args.wait:
            time.sleep(8)
            runs = find_dispatched()
        if not runs:
            print(f"Chưa thấy lần chạy mới. Xem: https://github.com/{repo}/actions")
            sys.exit(2)
        sys.exit(max(print_run(repo, r) for r in runs))

    sha = git("rev-parse", "HEAD")
    if not sha:
        sys.exit("LỖI: không đọc được commit hiện tại (đang không ở trong repo git?)")
    ahead = git("rev-list", "--count", "@{u}..HEAD")
    if ahead and ahead != "0":
        print(f"CẢNH BÁO: còn {ahead} commit chưa push. CI chỉ chạy sau khi push.")
    elif not ahead:
        print("CẢNH BÁO: nhánh này chưa có upstream. Nhớ push trước (git push -u origin <nhánh>).")

    if args.wait:
        runs = wait_for_runs(repo, lambda: runs_for_sha(repo, sha))
    else:
        runs = runs_for_sha(repo, sha)
    if not runs:
        print(f"Không có lần chạy CI nào cho commit {sha[:7]}. Có thể commit này không sửa gì trong apps/ hay "
              f"template/, hoặc chưa push. Xem: https://github.com/{repo}/actions")
        sys.exit(2)
    sys.exit(max(print_run(repo, r) for r in runs))


if __name__ == "__main__":
    main()
