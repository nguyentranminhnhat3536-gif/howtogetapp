# Bài học của xưởng

Mỗi dòng ghi: ngày · app · bài học ngắn, dùng lại được. AI đọc lướt file này đầu mỗi phiên, và thêm dòng mới sau mỗi mốc.

- 2026-09-29 · hệ thống · Phiên cloud bị chặn mạng tới Google Maven và Gradle, nên mọi bản build Android đều chạy trên GitHub Actions. Đọc kết quả bằng `scripts/ci_status.py`.
- 2026-09-29 · hệ thống · Tài khoản Play cá nhân mới: mỗi app phải qua closed test 12 người × 14 ngày. Cho nhiều app chạy song song, dùng chung một Google Group tester.
- 2026-09-29 · hệ thống · Bản hệ thống đưa lên project bị mất thư mục ẩn (`.github/`, `.claude/`). Đã viết lại `.github/workflows/build.yml`; các skill trong `.claude/skills/` vẫn còn thiếu.
- 2026-09-30 · hệ thống · Khi Ethan gửi link web/app của hãng khác và bảo "làm cái này": làm công cụ riêng cùng chức năng, tên và giao diện riêng, không chép thương hiệu. Tính năng cần AI trả phí thì báo chi phí trước (DUYỆT-$).
- 2026-09-30 · hệ thống · Ethan từng dán khóa bí mật (token) vào chat. Không dùng, không lưu, nhắc Ethan hủy khóa. Khóa chỉ được cất trong GitHub Secrets.
