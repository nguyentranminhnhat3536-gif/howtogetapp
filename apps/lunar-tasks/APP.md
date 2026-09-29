# Tasks & Lunar Reminders

| | |
|---|---|
| Slug | `lunar-tasks` |
| Package | `com.ethanstudio.lunartasks` (không bao giờ đổi) |
| Tạo ngày | 2026-09-29 |
| Chính sách quyền riêng tư | https://nguyentranminhnhat3536-gif.github.io/howtogetapp/lunar-tasks/privacy-policy.html |
| Bản chạy thử | https://github.com/nguyentranminhnhat3536-gif/howtogetapp/releases/tag/preview-lunar-tasks |
| Link Google Play | (chưa có) |

## Trạng thái
Bước 3 · Đã code MVP, đang build trên CI để gửi Ethan thử (DUYỆT-2).

## Việc tiếp theo
- [x] Điền mục Ý tưởng và Phạm vi MVP
- [x] Icon riêng và màu thương hiệu
- [x] Code MVP
- [ ] CI xanh, gửi link APK cho Ethan thử (DUYỆT-2)
- [ ] Điền studio.md (tên nhà phát triển, email hỗ trợ), chốt package trước lần upload đầu

## Ý tưởng
- Dành cho ai: người Việt cần một danh sách việc đơn giản, đặc biệt là người hay phải nhớ việc theo âm lịch (thắp hương mùng 1 và rằm, ngày giỗ, lễ tết).
- Giải quyết việc gì: ghi việc cần làm, nhắc đúng giờ, và lặp lại theo lịch âm, điều mà các app việc cần làm phổ biến không làm.
- Đối thủ chính: Google Keep, Todoist, Microsoft To Do (mạnh nhưng không có âm lịch); các app lịch vạn niên (có âm lịch nhưng quản lý việc yếu). Chưa tra số lượt tải cụ thể.
- Điểm khác biệt của mình: việc lặp theo tháng âm / năm âm, chọn nhanh "Mùng 1" / "Rằm", nhập ngày giỗ bằng ngày âm, mỗi việc hiện kèm ngày âm. Chạy offline, không cần tài khoản.

## Phạm vi MVP
**Bắt buộc (3–5):**
1. Thêm, sửa, xóa việc; đánh dấu xong có Hoàn tác; đánh dấu quan trọng
2. Nhóm việc: Quá hạn / Hôm nay / Ngày mai / Sắp tới / Không có ngày / Đã xong
3. Lặp lại: ngày, tuần, tháng, năm, tháng âm lịch, năm âm lịch
4. Nhắc việc bằng thông báo (mặc định 7:00 nếu việc không có giờ)
5. Thẻ đầu trang: hôm nay, ngày âm, mùng 1 và rằm sắp tới
6. Nút "Xong" trên thông báo; tùy chọn nhắc trước 1 ngày lúc 19:00
7. Dễ dùng cho người lớn tuổi: nút "Aa" mở Cài đặt (chữ to ×1.3, tương phản cao), nhãn TalkBack cho ô tick
8. Nhắc uống thuốc: tên thuốc, cách uống, nhiều cữ mỗi ngày (mỗi cữ là một việc lặp hằng ngày có nhắc)
9. Gọi nhanh người thân: chọn từ danh bạ (không cần quyền READ_CONTACTS) hoặc nhập số; nút Gọi to ở màn hình chính mở trình quay số (không cần quyền CALL_PHONE)

**Để sau:**
- Widget màn hình chính
- Danh mục / nhãn, tìm kiếm
- Sao lưu và xuất dữ liệu (gói Pro)

## Màn hình
| Màn hình | Làm gì |
|---|---|
| Danh sách việc | Thẻ ngày dương/âm, các nhóm việc, tick xong, sao quan trọng, nút Thêm việc |
| Thêm / sửa việc | Tên, ghi chú, ngày (kèm ngày âm), giờ, chọn theo ngày âm, lặp lại, nhắc, quan trọng, xóa |

## Dữ liệu
- DataStore `settings`: chữ to, tương phản cao, tên và số người thân (chỉ lưu trên máy).
- Room (`tasks.db`, bảng `tasks`, schema v3, có migration 1→2→3), chỉ nằm trên máy. Không có mạng, không có tài khoản.

## Kiếm tiền
- Mô hình:
- AdMob App ID / ad unit ID:
- Sản phẩm Play Billing:

## Quyền (permissions) và lý do
| Quyền | Lý do |
|---|---|
| POST_NOTIFICATIONS | Hiện thông báo nhắc việc (Android 13+ hỏi người dùng khi bật Nhắc tôi) |
| RECEIVE_BOOT_COMPLETED | Hẹn lại giờ nhắc sau khi khởi động lại máy |

Không dùng SCHEDULE_EXACT_ALARM (Play hạn chế quyền này); nhắc bằng `setAndAllowWhileIdle` nên có thể trễ vài phút.

## Tài nguyên bên thứ ba và giấy phép
| Tài nguyên (hình, font, âm thanh, dữ liệu…) | Nguồn | Giấy phép |
|---|---|---|
| Icon trong app và icon launcher (vẽ lại từ Material icons) | https://fonts.google.com/icons | Apache 2.0 |
| Thuật toán âm lịch (viết lại bằng Kotlin) | Hồ Ngọc Đức, https://www.informatik.uni-leipzig.de/~duc/amlich/ | Cần kiểm tra điều kiện dùng lại trước khi phát hành |

## Soạn sẵn cho Play Console
- **Data safety:** Không thu thập, không chia sẻ dữ liệu (mọi thứ nằm trên máy, kể cả tên thuốc và số người thân).
- **Health apps declaration:** app chỉ nhắc giờ uống thuốc, không phải thiết bị y tế; có câu miễn trừ trong màn hình thuốc. Cần khai đúng ở mục Health apps nếu Play Console hỏi.
- **Content rating (IARC):**
- **Target audience:** 18+
- **App access:** Toàn bộ tính năng dùng được mà không cần đăng nhập.
- **Ads:** Không

## Closed test
- Ngày tester thứ 12 tham gia:
- Ngày đủ 14 ngày:
- Góp ý nhận được → đã sửa:

## Lịch sử phát hành
| Ngày | Version | Track | Nội dung chính |
|---|---|---|---|
| | | | |

## Nhật ký quyết định
- 2026-09-29: Tạo app từ template.
- 2026-09-29: Ethan chọn "app quản lý công việc". Chọn điểm khác biệt là lặp và nhắc theo âm lịch để không bị na ná Keep/Todoist.
- 2026-09-29: Package tạm `com.ethanstudio.lunartasks` (studio.md chưa có tiền tố). Phải chốt trước lần upload đầu vì package không đổi được.
- 2026-09-29: Dùng navigation-compose (route dạng chuỗi) và Room + KSP có sẵn trong catalog của template, không thêm thư viện ngoài.
- 2026-09-29: Thêm nút "Xong" trên thông báo và nhắc trước 1 ngày (19:00 hôm trước). DB lên v2 bằng migration, không xóa dữ liệu người dùng.
- 2026-09-29: Ethan muốn phục vụ người khuyết tật và người lớn tuổi. Làm trước chữ to và tương phản cao trong app này. Không dùng AccessibilityService và SMS vì Play kiểm tra gắt.
- 2026-09-29: Ethan chọn gộp tính năng người lớn tuổi vào app này. Thêm nhắc uống thuốc và gọi nhanh người thân.
