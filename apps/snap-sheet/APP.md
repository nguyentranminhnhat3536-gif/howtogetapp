# SnapSheet: Document Scanner

| | |
|---|---|
| Slug | `snap-sheet` |
| Package | `com.ethanstudio.snapsheet` (không bao giờ đổi) |
| Tạo ngày | 2026-10-01 |
| Chính sách quyền riêng tư | https://nguyentranminhnhat3536-gif.github.io/howtogetapp/snap-sheet/privacy-policy.html |
| Bản chạy thử | https://github.com/nguyentranminhnhat3536-gif/howtogetapp/releases/tag/preview-snap-sheet |
| Link Google Play | (chưa có) |

## Trạng thái
Bước 3 · Code MVP xong, CI xanh (build + unit test) ngày 2026-10-01. Chờ Ethan thử bản APK (DUYỆT-2). Chưa ai chạy trên máy thật; thanh toán chỉ thử được sau khi tạo sản phẩm trong Play Console.

## Việc tiếp theo
- [ ] Ethan duyệt kế hoạch đăng ký tài khoản: `PLAN-account.md` (DUYỆT-$)
- [x] CI xanh
- [ ] Ethan thử APK trên điện thoại và góp ý (DUYỆT-2)
- [ ] Ethan tạo sản phẩm trong Play Console (xem mục Kiếm tiền) và chốt giá (DUYỆT-$)
- [ ] Điền studio.md (tên nhà phát triển, email hỗ trợ); đổi `support_email` trong strings.xml
- [ ] Ảnh store, icon 512, bước play-listing và play-policy-check
- [ ] Chốt tên app (có thể đổi trước lần upload đầu; package `com.ethanstudio.snapsheet` thì không đổi được sau đó)

## Ý tưởng
- Dành cho ai: người cần quét giấy tờ, hóa đơn, hợp đồng thành PDF nhanh trên điện thoại, không muốn tạo tài khoản.
- Giải quyết việc gì: chụp giấy tờ, tự cắt viền và làm rõ, lưu thành PDF, chia sẻ; lấy chữ từ ảnh (OCR) mà không gửi ảnh lên mạng.
- Đối thủ chính: nhóm app quét PDF rất đông trên Play (ví dụ Adobe Scan, CamScanner, Microsoft Lens, Google Drive Scan). Chưa tra số lượt tải và điểm cụ thể.
- Điểm khác biệt của mình: chạy hoàn toàn trên máy (quét và OCR không gửi ảnh đi đâu), không tài khoản, không quảng cáo, không đóng dấu logo lên PDF kể cả bản miễn phí, mua một lần vĩnh viễn (ngoài gói tháng và năm). Giao diện song ngữ Anh–Việt, OCR hiểu tiếng Việt có dấu.
- Ghi chú rủi ro: thị trường đông, dễ bị coi là "na ná". Phải giữ khác biệt ở quyền riêng tư, giá mua một lần và độ gọn. Ethan đưa 10 ảnh chụp một app có sẵn làm tham chiếu giao diện; chỉ lấy ngôn ngữ thiết kế (màu, bố cục), không dùng tên, icon, hình minh họa, hay điểm đánh giá của họ.
- DUYỆT-1 (chọn ý tưởng): Ethan tự chọn app quét tài liệu ngày 2026-10-01 nên bỏ qua bước trình 3 ý tưởng.

## Phạm vi MVP
**Bắt buộc:**
1. Quét bằng camera (ML Kit Document Scanner: tự cắt viền, lọc màu, nhiều trang) và nhập ảnh từ thư viện thành PDF
2. Danh sách tài liệu: tìm kiếm, mở PDF, chia sẻ PDF hoặc ảnh, đổi tên, xóa
3. Lấy chữ từ ảnh (OCR, chỉ Pro)
4. Hai bậc: miễn phí (5 trang/tài liệu, 3 lượt xuất/ngày) và Pro (30 trang, xuất không giới hạn, OCR)
5. Ba gói mua: tháng, năm, vĩnh viễn (Play Billing), có Khôi phục giao dịch

**Để sau:** ký tên và đóng dấu lên PDF, thư mục, sao lưu, chỉnh sửa trang, xuất Word.

## Màn hình
| Màn hình | Làm gì |
|---|---|
| Home | Lối tắt (quét một trang, nhiều trang, thẻ căn cước, nhập ảnh) và danh sách gần đây |
| Files | Tìm kiếm và danh sách tài liệu |
| Tools | Thẻ công cụ: một trang, nhiều trang, thẻ căn cước, ảnh sang PDF |
| Account | Thẻ Pro, nâng cấp, khôi phục giao dịch, đánh giá, chia sẻ, liên hệ, chính sách |
| Tài liệu | Xem các trang, mở/chia sẻ PDF và ảnh, lấy chữ, đổi tên, xóa |
| Màn mua | Ba gói, chọn gói, Continue, dòng pháp lý, Restore |
| Bảng Scan | Bấm nút + ở giữa: quét bằng camera hoặc nhập ảnh |

Giao diện theo Scan Design System (https://claude.ai/artifact/UzxZzBwaCYBTAcHrdgpB6w).

## Dữ liệu
- Room `snapsheet.db`, bảng `docs` (tên, ngày, số trang). File ảnh và PDF trong `filesDir/docs/<id>/`, chỉ app đọc được; chia sẻ qua FileProvider.
- DataStore `pro`: đã mua Pro chưa (lưu để dùng khi mất mạng; mỗi lần mở app hỏi lại Google Play) và số lượt xuất trong ngày.
- Không có mạng của riêng app, không có tài khoản.

## Kiếm tiền
- Mô hình: freemium. Không quảng cáo (không AdMob, không cần UMP).
- Bản miễn phí: quét không giới hạn số tài liệu, 5 trang mỗi tài liệu, 3 lượt xuất (mở/chia sẻ PDF hoặc ảnh) mỗi ngày, không OCR. Không đóng dấu logo.
- Sản phẩm Play Billing (ID phải khớp từng chữ):
  - Đăng ký (Subscription) `snapsheet_pro`, hai base plan: `monthly` (gia hạn mỗi tháng) và `yearly` (mỗi năm). Có thể thêm ưu đãi dùng thử miễn phí; app tự nhận và hiện.
  - Sản phẩm trong app (One-time product) `snapsheet_lifetime`: mua một lần, không hoàn trả tự động.
- Giá: do Ethan chốt trong Play Console (DUYỆT-$). App lấy giá thật từ Google Play, không gõ cứng. Gợi ý khởi điểm (ước lượng, chưa tra giá đối thủ): tháng khoảng 2,99 USD, năm khoảng 14,99 USD, vĩnh viễn khoảng 24,99 USD; gói năm rẻ hơn 12 tháng lẻ để màn mua hiện "Save %".
- Mua xong: acknowledge trong vòng 3 ngày (app tự làm), nếu không Google hoàn tiền.

## Quyền (permissions) và lý do
| Quyền | Lý do |
|---|---|
| (không có) | Quét dùng màn hình của Google Play services (không cần CAMERA), ảnh chọn bằng trình chọn ảnh hệ thống (không cần đọc thư viện). BILLING, INTERNET, ACCESS_NETWORK_STATE do thư viện Play Billing và ML Kit tự thêm (app không tự gọi mạng). |

## Tài nguyên bên thứ ba và giấy phép
| Tài nguyên (hình, font, âm thanh, dữ liệu…) | Nguồn | Giấy phép |
|---|---|---|
| Icon giao diện và icon app | Tự vẽ (vector) theo Scan Design System | Của Ethan |
| Font | Roboto mặc định của Android | Apache 2.0 |
| ML Kit Document Scanner, ML Kit Text Recognition | Google | Điều khoản ML Kit |
| Play Billing Library 9.1.0 | Google | Apache 2.0 |

## Soạn sẵn cho Play Console
- **Data safety:** Không thu thập dữ liệu cá nhân để gửi đi. Khai: lịch sử mua hàng được Google Play xử lý (Purchase history, không dùng cho quảng cáo). ML Kit và Google Play services có thể thu thập chẩn đoán (Diagnostics) theo chính sách Google; kiểm tra lại mục "SDK thu thập" trong Play Console trước khi nộp.
- **Content rating (IARC):**
- **Target audience:** 18+
- **App access:** Toàn bộ tính năng dùng được mà không cần đăng nhập.
- **Ads:** Không
- **Subscriptions:** Phải hiện giá, kỳ hạn, cách hủy ngay trên màn mua (đã làm).

## Closed test
- Ngày tester thứ 12 tham gia:
- Ngày đủ 14 ngày:
- Góp ý nhận được → đã sửa:

## Lịch sử phát hành
| Ngày | Version | Track | Nội dung chính |
|---|---|---|---|
| | | | |

## Nhật ký quyết định
- 2026-10-01: Tạo app từ template.
- 2026-10-01: Ethan yêu cầu "code hoàn thiện hệ thống scan" có 3 gói (tháng, năm, vĩnh viễn). Chọn ML Kit Document Scanner (quét, cắt viền, lọc, không cần quyền camera) và ML Kit Text Recognition (OCR trên máy) để app chạy offline.
- 2026-10-01: Tháng và năm là hai base plan trong cùng một đăng ký `snapsheet_pro` (cách Google khuyên); vĩnh viễn là sản phẩm mua một lần riêng.
- 2026-10-01: Không quảng cáo ở bản đầu: đơn giản hơn, không cần UMP, hợp với điểm bán "riêng tư".
- 2026-10-01: Tạm bỏ ký tên và đóng dấu (có trong ảnh tham chiếu) vì cần trình sửa PDF riêng; đưa vào "Để sau".
- 2026-10-01: Chưa có phiên bản ML Kit đã tra chính thức (trang Google bị chặn trong phiên). Dùng document-scanner 16.0.0 và text-recognition 16.0.1; nếu CI báo không tìm thấy thì tra lại.
- 2026-10-01: Ethan muốn thêm đăng ký tài khoản. Đã thiết kế 4 màn (https://claude.ai/artifact/Nug9pRAZzY5ejELXK2TNZz) và viết kế hoạch kỹ thuật + chi phí trong `PLAN-account.md`. Chưa code, chờ duyệt. Tài khoản là tùy chọn, vẫn dùng được không cần đăng nhập.
