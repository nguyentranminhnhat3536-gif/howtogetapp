# howtogetapp — Xưởng App Android của Ethan

Repo này là "bộ não" và "dây chuyền" để AI (Claude) tự nghiên cứu, viết code, build và chuẩn bị phát hành app Android lên Google Play.

- **Luật cho AI:** `CLAUDE.md`
- **Quy trình từng bước:** `.claude/skills/`
- **App mẫu:** `template/`
- **Máy build tự động:** GitHub Actions (`.github/workflows/build.yml`)
- **Bảng theo dõi:** `portfolio/`

---

## Cách dùng hằng ngày

Trong project **Howtogetapp**, mở một task mới rồi nói với AI bằng lời bình thường. Ví dụ:

| Muốn | Gõ |
|---|---|
| Tìm ý tưởng | `Tìm 10 ý tưởng app mới, chấm điểm và đề xuất 3 cái tốt nhất` |
| Làm app | `Làm app <tên ý tưởng> tới bản chạy thử` |
| Sửa app | `App <slug>: nút X bị lỗi Y, sửa giúp` (gửi kèm ảnh chụp màn hình nếu có) |
| Kiếm tiền | `Gắn quảng cáo và gói gỡ quảng cáo cho app <slug>` |
| Chuẩn bị lên store | `Chuẩn bị phát hành app <slug>` |
| Xem tình hình | `Tình hình các app thế nào?` |

AI tự làm hết và chỉ dừng lại hỏi ông ở 4 mốc: **chốt ý tưởng**, **thử bản chạy thử**, **trước khi phát hành**, và **việc liên quan tới tiền hoặc bảo mật**.

## Cài bản chạy thử lên điện thoại

1. Mở link AI gửi (dạng `…/releases/tag/preview-<slug>`) trên điện thoại Android.
2. Bấm vào file `.apk` để tải về, sau đó mở file.
3. Nếu máy hỏi, cho phép trình duyệt "cài ứng dụng không rõ nguồn gốc".

## Việc ông phải tự làm (AI không bấm thay được)

**Một lần duy nhất** (AI sẽ hướng dẫn chi tiết qua skill `factory-setup`):

- Điền `portfolio/studio.md`: tên nhà phát triển, tiền tố package, email hỗ trợ.
- Cất file upload key AI tạo ra vào chỗ an toàn, rồi thêm 4 GitHub Secrets.
- Bật GitHub Pages (để có trang chính sách quyền riêng tư).
- Kiểm tra trạng thái xác minh nhà phát triển trong Play Console. Nếu làm app có mua trong app thì tạo thêm hồ sơ thanh toán.
- Gom một nhóm **ít nhất 15 người dùng Android** sẵn sàng làm tester (Google yêu cầu tối thiểu 12 người trong 14 ngày).
- Tạo tài khoản AdMob nếu định chạy quảng cáo.

**Mỗi lần phát hành:** tải file AAB từ GitHub, tải lên Play Console, dán nội dung store AI đã soạn, rồi bấm gửi duyệt.

## Thời gian thực tế cho một app

| Giai đoạn | Thời gian |
|---|---|
| Ý tưởng → bản chạy thử | 1–3 buổi làm việc với AI |
| Hoàn thiện, gắn kiếm tiền, soạn nội dung store | 1–2 buổi |
| Closed test (bắt buộc) | **tối thiểu 14 ngày** |
| Google duyệt quyền production | khoảng 7 ngày |

Vì vậy nên cho nhiều app chạy song song: app A đang closed test thì làm tiếp app B.

## Lần đầu: đưa hệ thống lên repo

Nếu repo mới chỉ có README này mà chưa có các thư mục kể trên, trong task đầu tiên của project hãy đính kèm file zip hệ thống rồi gõ:

> Giải nén file zip này vào gốc repo howtogetapp (ghi đè README), đọc CLAUDE.md, rồi commit và push. Nếu chỉ push được lên nhánh riêng thì tạo Pull Request vào main và nhắc tôi merge. Sau đó chạy `python3 scripts/ci_status.py --wait` để chờ CI build template; nếu lỗi thì tự sửa tới khi xanh. Xong thì làm theo skill factory-setup.
