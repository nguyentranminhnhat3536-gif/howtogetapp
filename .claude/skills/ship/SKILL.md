---
name: ship
description: Dây chuyền 4 agent (planner → coder → tester → reviewer) làm một tính năng hoặc sửa một lỗi cho app đã có trong apps/ (hoặc template/). Chỉ dùng khi Ethan gõ /ship hoặc nói rõ muốn chạy dây chuyền 4 agent.
argument-hint: "<slug>: <yêu cầu>"
---

# /ship: dây chuyền 4 agent

Chạy dây chuyền cho yêu cầu: $ARGUMENTS

Bạn là **nhạc trưởng**. Bạn không tự viết kế hoạch, code, test hay đánh giá. Bạn giao từng việc cho đúng subagent bằng công cụ Agent (`subagent_type` là `planner`, `coder`, `tester` hoặc `reviewer`, và `run_in_background: false` vì chặng sau cần kết quả chặng trước). Không truyền tham số `model`: cả 4 agent để `model: inherit`, tức dùng đúng model của task đang chạy, nên tự theo khi task chuyển sang model mới. Subagent không biết gì về cuộc trò chuyện, nên mỗi lần giao việc phải nói rõ: slug app, đang ở chặng và vòng nào, file nào trong `.bangiao/` cần đọc. Việc git, push và đọc CI là của bạn.

Bốn agent bàn giao cho nhau bằng file trong `.bangiao/`. Thư mục này nằm trong `.gitignore`, không lên GitHub.

| File | Ai viết |
|---|---|
| `yeu-cau.md` | nhạc trưởng (chặng 0) |
| `ke-hoach.md` | planner |
| `thay-doi.md` | coder |
| `ket-qua-test.md` | tester |
| `ket-qua-ci.md` | nhạc trưởng (kết quả của `ci_status.py`) |
| `danh-gia.md` | nhạc trưởng chép nguyên văn câu trả lời của reviewer (reviewer không có quyền ghi file) |

Sau mỗi chặng, kiểm tra file bàn giao đã có rồi mới sang chặng sau. Không nhảy cóc.

## Chặng 0: Chuẩn bị

1. Xác định app: phần trước dấu `:` trong yêu cầu là slug (`apps/<slug>/`, hoặc `template`). Nếu không ghi và repo chỉ có một app thì dùng app đó; có nhiều app thì hỏi Ethan.
2. Kiểm tra phạm vi. Dây chuyền chỉ dành cho tính năng hoặc sửa lỗi của app đã có. Dừng lại và làm theo quy trình thường trong CLAUDE.md nếu yêu cầu là làm app mới (cần DUYỆT-1), phát hành lên Play (DUYỆT-3), hoặc việc tốn tiền, đụng bảo mật, không hoàn tác được (DUYỆT-$).
3. Chạy `git branch --show-current`. Nếu đang ở `main` hoặc `master` thì dừng và báo Ethan.
4. Nếu còn thay đổi chưa commit của việc trước thì commit và push trước, để phần thay đổi của dây chuyền chỉ chứa việc mới. Sau đó `git pull --no-rebase origin <nhánh>` để lấy commit mới nhất, vì các phiên khác có thể đã push.
5. Dọn sổ bàn giao cũ: `rm -rf .bangiao && mkdir .bangiao`.
6. Ghi `.bangiao/yeu-cau.md`: yêu cầu nguyên văn, slug, nhánh, commit bắt đầu (`git rev-parse HEAD`), ngày.

## Chặng 1: Planner

Giao cho `planner`. Chờ có `.bangiao/ke-hoach.md`.

Nếu mục CÂU HỎI CẦN ETHAN khác `Không có` thì DỪNG. Chuyển câu hỏi cho Ethan bằng lời dễ hiểu, kèm phương án planner đề xuất. Khi Ethan trả lời, ghi câu trả lời vào `.bangiao/yeu-cau.md` rồi chạy lại chặng 1.

Nếu Ethan dặn "dừng sau kế hoạch" thì dừng ở đây và tóm tắt kế hoạch cho Ethan duyệt.

## Chặng 2: Coder

Giao cho `coder`. Chờ có `.bangiao/thay-doi.md`. Nếu coder trả về câu hỏi thì xử lý như ở chặng 1.

## Chặng 3: Tester viết test

Giao cho `tester` (lần gọi đầu). Chờ có `.bangiao/ket-qua-test.md`.

## Chặng 4: Build và chạy test trên CI

Phiên này không build Android được (CLAUDE.md mục 5), nên test chạy trên GitHub Actions.

1. `git add -A && git commit -m "<slug>: <việc đã làm>"`, rồi `git push -u origin <nhánh>`. Nếu bị từ chối vì GitHub có commit mới: `git pull --no-rebase origin <nhánh>` rồi push lại.
2. Nếu thay đổi không đụng `apps/` hay `template/` thì CI không chạy. Khi đó ghi `Không có CI (không đổi code app)` vào `.bangiao/ket-qua-ci.md` rồi sang chặng 5.
3. Chạy `python3 scripts/ci_status.py --wait > .bangiao/ket-qua-ci.md 2>&1; echo "exit=$?"`. Mã thoát: 0 là xanh, 1 là đỏ, 2 là chưa có dữ liệu. Build Android mất vài phút; nếu lệnh hết giờ trước khi CI xong thì chạy lại, script sẽ chờ tiếp đúng commit đó.
4. Giao cho `tester` (gọi lại sau CI) để đọc kết quả.
5. Nếu đỏ:
   - Tester đã sửa test sai: quay lại bước 1 của chặng này.
   - Tester ghi `LỖI Ở CODE` hoặc `LỖI BUILD`: giao `coder` sửa (dặn đọc `ket-qua-ci.md` và `ket-qua-test.md`), rồi quay lại bước 1.
   - Tối đa **3 vòng đỏ**. Vòng thứ 3 vẫn đỏ thì DỪNG và báo Ethan: lỗi gì, đã thử những gì, đề xuất đổi cách làm (CLAUDE.md mục 5).

## Chặng 5: Reviewer

Giao cho `reviewer`. Chép nguyên văn câu trả lời vào `.bangiao/danh-gia.md`.

- `PHAN QUYET: CHOT`: sang chặng 6.
- `PHAN QUYET: CAN SUA`: giao `coder` sửa theo `danh-gia.md`. Nếu có mục liên quan tới test thì giao thêm `tester`. Sau đó chạy lại chặng 4 và 5. Tối đa **2 vòng CAN SUA**; hết 2 vòng vẫn CAN SUA thì dừng và báo Ethan các mục còn lại.
- `PHAN QUYET: CHAN`: DỪNG, không tự sửa. Báo Ethan lý do bằng lời dễ hiểu, kèm đề xuất (thường là làm lại kế hoạch với ràng buộc còn thiếu).

## Chặng 6: Kết thúc

1. Cập nhật `apps/<slug>/APP.md` (mục Trạng thái và Việc tiếp theo) và dòng của app trong `portfolio/apps.md`. Có bài học dùng lại được thì thêm một dòng vào `portfolio/lessons.md`. Commit, push, rồi chạy `python3 scripts/ci_status.py --wait` một lần nữa để chắc bản chạy thử mới nhất đã build xanh.
2. Đảm bảo có Pull Request từ nhánh hiện tại vào `main` (CLAUDE.md mục 5). **Không tự merge** và không phát hành. Merge là việc của Ethan.
3. Báo Ethan theo mẫu ở CLAUDE.md mục 8:
   - **Phán quyết:** CHOT / CAN SUA / CHAN, kèm mục "Tóm tắt cho Ethan" của reviewer.
   - **Đã làm:** 2–4 gạch đầu dòng.
   - **Kết quả:** link bản chạy thử `https://github.com/nguyentranminhnhat3536-gif/howtogetapp/releases/tag/preview-<slug>` và link Pull Request.
   - **Ethan cần làm (DUYỆT-2):** các bước trong mục "Việc Ethan thử trên điện thoại" của `ket-qua-test.md`.
   - **Tiếp theo.**
