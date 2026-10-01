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
- [ ] Ethan duyệt kế hoạch bản iOS: `PLAN-ios.md` (DUYỆT-$)
- [x] Code đăng ký, đăng nhập (email + Google), quên mật khẩu, xóa tài khoản (CI xanh 2026-10-01)
- [x] Firebase đã cấu hình (project `snapsheet`, gói Spark). Ethan thử trên máy thật 2026-10-01: đăng ký email và đăng nhập Google chạy được
- [ ] Khi lên Play: thêm SHA-1/SHA-256 của khóa Play App Signing vào Firebase (không thì đăng nhập Google hỏng ở bản tải từ Play)
- [ ] Dán link trang xóa tài khoản vào Play Console: https://nguyentranminhnhat3536-gif.github.io/howtogetapp/snap-sheet/delete-account.html
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
| Home | Lời chào + ảnh đại diện, thẻ lớn "Scan document", 4 lối tắt tròn (một trang, nhiều trang, thẻ căn cước, nhập ảnh), danh sách gần đây + "See all" |
| Files | Tìm kiếm, sắp xếp (mới nhất / tên), nhóm "Hôm nay" / "Trước đó", dòng phụ có số trang và dung lượng |
| Tools | Lưới thẻ công cụ: một trang, nhiều trang, thẻ căn cước, ảnh sang PDF, lấy chữ (PRO), chia sẻ nhanh |
| Account | Thẻ Pro, nâng cấp, khôi phục giao dịch, Language (chọn ngôn ngữ của app), đánh giá, chia sẻ, liên hệ, chính sách |
| Tài liệu | Vuốt xem từng trang ("Page x of y"), 4 nút tròn (mở PDF, chia sẻ PDF, chia sẻ ảnh, lấy chữ), thanh lượt xuất miễn phí, đổi tên, xóa |
| Màn mua | Nền "bầu trời", tiêu đề chữ có chân, ba gói (năm, tháng, vĩnh viễn) có nhãn góc, giá năm quy ra mỗi tháng, dòng điều khoản theo gói, Restore, link chính sách |
| Bảng Scan | Bấm nút + ở giữa: quét bằng camera hoặc nhập ảnh |

Giao diện theo Scan Design System (https://claude.ai/artifact/UzxZzBwaCYBTAcHrdgpB6w).

## Dữ liệu
- Room `snapsheet.db`, bảng `docs` (tên, ngày, số trang). File ảnh và PDF trong `filesDir/docs/<id>/`, chỉ app đọc được; chia sẻ qua FileProvider.
- DataStore `pro`: đã mua Pro chưa (lưu để dùng khi mất mạng; mỗi lần mở app hỏi lại Google Play) và số lượt xuất trong ngày.
- Không có mạng của riêng app, không có tài khoản.

## Kiếm tiền
- Mô hình: freemium. Không quảng cáo (không AdMob, không cần UMP).
- Gói tháng có **ưu đãi dùng thử miễn phí 7 ngày** (tạo trong Play Console: base plan `monthly` › Add offer › Free trial 7 ngày, điều kiện "New customer acquisition"). Trong 7 ngày: dùng đủ Pro. Hết 7 ngày: tự trừ 0,99 USD/tháng. Nếu hủy trong 7 ngày: không mất tiền, về bản miễn phí (5 trang/tài liệu, 3 lượt xuất/ngày, không OCR; tài liệu đã tạo vẫn giữ). Mỗi tài khoản Google chỉ được dùng thử một lần (Play tự kiểm tra).
- Bản miễn phí: quét không giới hạn số tài liệu, 5 trang mỗi tài liệu, 3 lượt xuất (mở/chia sẻ PDF hoặc ảnh) mỗi ngày, không OCR. Không đóng dấu logo.
- Sản phẩm Play Billing (ID phải khớp từng chữ):
  - Đăng ký (Subscription) `snapsheet_pro`, hai base plan: `monthly` (gia hạn mỗi tháng) và `yearly` (mỗi năm). Có thể thêm ưu đãi dùng thử miễn phí; app tự nhận và hiện.
  - Sản phẩm trong app (One-time product) `snapsheet_lifetime`: mua một lần, không hoàn trả tự động.
- Giá Ethan chốt ngày 2026-10-01: tháng **0,99 USD**, năm **9,99 USD** (rẻ hơn 12 tháng lẻ 16%, màn mua tự hiện "Save 15%"), vĩnh viễn **19,97 USD** (Ethan chốt lại). Đặt trong Play Console bằng USD, Play tự quy đổi giá từng nước. App lấy giá thật từ Google Play, không gõ cứng.
- Mua xong: acknowledge trong vòng 3 ngày (app tự làm), nếu không Google hoàn tiền.

## Quyền (permissions) và lý do
| Quyền | Lý do |
|---|---|
| (không có) | Quét dùng màn hình của Google Play services (không cần CAMERA), ảnh chọn bằng trình chọn ảnh hệ thống (không cần đọc thư viện). BILLING, INTERNET, ACCESS_NETWORK_STATE, READ_GSERVICES do thư viện Play Billing, ML Kit và Firebase tự thêm. Mạng chỉ dùng khi đăng nhập. USE_BIOMETRIC và USE_FINGERPRINT (Credential Manager tự thêm) đã gỡ vì app không dùng. |

## Tài nguyên bên thứ ba và giấy phép
| Tài nguyên (hình, font, âm thanh, dữ liệu…) | Nguồn | Giấy phép |
|---|---|---|
| Icon giao diện và icon app | Tự vẽ (vector) theo Scan Design System | Của Ethan |
| Font | Roboto mặc định của Android | Apache 2.0 |
| Font tiêu đề DM Serif Display (`res/font/dm_serif_display.ttf`, chỉ dùng cho en/es/pt/fr/de/id/tr; ngôn ngữ khác dùng font có chân của hệ thống) | https://github.com/google/fonts/tree/main/ofl/dmserifdisplay | SIL OFL 1.1 (văn bản kèm theo ở `assets/licenses/DMSerifDisplay-OFL.txt`) |
| ML Kit Document Scanner, ML Kit Text Recognition | Google | Điều khoản ML Kit |
| Play Billing Library 9.1.0 | Google | Apache 2.0 |

## Soạn sẵn cho Play Console
- **Data safety:** Personal info › Email address và User IDs: có thu thập (chỉ khi người dùng tự tạo tài khoản, không bắt buộc), mục đích App functionality + Account management, không chia sẻ, không bán, mã hóa khi truyền, có cho yêu cầu xóa (link trang xóa tài khoản). Tài liệu quét không thu thập. Khai: lịch sử mua hàng được Google Play xử lý (Purchase history, không dùng cho quảng cáo). ML Kit và Google Play services có thể thu thập chẩn đoán (Diagnostics) theo chính sách Google; kiểm tra lại mục "SDK thu thập" trong Play Console trước khi nộp.
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
- 2026-10-01: Ethan hỏi bản iOS. Viết kế hoạch kỹ thuật và chi phí trong `PLAN-ios.md`: app riêng bằng Swift + SwiftUI (VisionKit, Vision, PDFKit, StoreKit 2), build trên GitHub Actions macOS, thử qua TestFlight. Chưa code, chờ duyệt.
- 2026-10-01: Ethan chọn tối ưu bản Android trước rồi mới làm iOS. Đợt tối ưu 1: tắt sao lưu tự động của Android (đúng lời hứa tài liệu chỉ nằm trên máy, chính sách đã ghi); hỏi lại Google Play mỗi lần quay lại app (bắt kịp gia hạn, hủy, hoàn tiền, giao dịch chờ); bộ nhớ đệm ảnh thu nhỏ để cuộn mượt; không mở/chia sẻ file đã mất; màn tài liệu hiện số lượt xuất miễn phí còn lại; đổi chiều cao cố định thành tối thiểu để chữ to (cỡ chữ hệ thống 200%) không bị cắt.
- 2026-10-01: Ethan yêu cầu có đăng ký và đăng nhập ở đầu app. Đã code: màn chào (Đăng nhập / Đăng ký / Dùng không cần tài khoản) hiện ở lần mở đầu tiên; đăng ký email + mật khẩu có email xác nhận; đăng nhập email và Google (Credential Manager); quên mật khẩu; tab Tài khoản có Đăng xuất và Xóa tài khoản. Dùng Firebase Authentication (BoM 34.17.0, plugin google-services 4.5.0; credentials 1.6.0; googleid 1.2.0). Cấu hình Firebase nằm trong GitHub Secret, không commit. Nút Google chưa có logo chính thức của Google (phải thêm đúng hướng dẫn thương hiệu trước khi phát hành).
- 2026-10-01: Đăng nhập Google cần SHA cố định. Tạo khóa debug cố định (không commit; Ethan cất ở GitHub Secret `DEBUG_KEYSTORE_BASE64`, CI ghi vào ~/.android/debug.keystore). SHA-1 bản debug: 43:0A:7C:C9:B1:48:5A:0B:BC:F5:CF:54:AD:12:D4:3F:DB:58:46:C2. Khi lên Play phải thêm SHA-1/SHA-256 của khóa Play App Signing vào Firebase.
- 2026-10-01: Lỗi đầu tiên khi đăng ký là do chưa bấm "Get started" và chưa bật Email/Password trong Firebase Authentication. App giờ báo rõ lỗi này và hiện mã lỗi cho các lỗi lạ. Ethan xác nhận đăng ký và đăng nhập (email + Google) chạy được trên máy thật.
- 2026-10-01: Ethan muốn app quốc tế. Thêm 10 ngôn ngữ (es, pt-BR, fr, de, id, ru, tr, ja, ko, hi) ngoài en và vi; Android tự chọn theo ngôn ngữ máy, Android 13+ chọn riêng cho app được (generateLocaleConfig). Bản dịch do AI làm, nên nhờ người bản xứ đọc lại trước khi quảng bá ở nước đó. OCR hiện chỉ đọc chữ Latin (Anh, Việt, Tây Ban Nha, Pháp, Đức, Indonesia, Thổ Nhĩ Kỳ...); chữ Nhật, Hàn, Hindi, Nga cần thêm mô hình ML Kit riêng (để sau, app nặng thêm).
- 2026-10-01: Đổi giao diện theo yêu cầu Ethan: nền trắng chủ đạo, xanh là màu phụ, trình bày bằng gradient (nút, nút +, ảnh đại diện, dải đầu trang, thẻ Pro, vòng icon). Màn đăng nhập chuyển từ nền tối sang nền trắng.
- 2026-10-01: Ethan giao mình quyết giới hạn sau dùng thử: giữ nguyên bản miễn phí như cũ. Màn mua hiện "Dùng thử miễn phí 7 ngày", nút đổi thành "Dùng thử miễn phí 7 ngày", có dòng nói rõ giá sau dùng thử và cách hủy (chính sách Google Play bắt buộc).
- 2026-10-01: Ethan thấy màn mua vẫn nền tối (máy bật chế độ tối). Đổi app luôn dùng giao diện trắng bất kể cài đặt máy (bộ màu tối vẫn giữ trong code, bật lại bằng AppTheme(darkTheme = true)); thanh trạng thái luôn icon tối. Màn mua thêm "Đang tải giá…" và thôi chờ Google Play sau 10 giây để hiện thông báo rõ thay vì trống.
- 2026-10-01: Làm lại giao diện theo bản thiết kế "SnapSheet Redesign" Ethan đã duyệt (Home, Files, Tools, Tài liệu, bảng Scan, màn mua kiểu bầu trời, Account, màn chào) và vẽ lại bộ icon nét 2px. Màn mua bỏ dòng "Terms" vì app chưa có trang điều khoản (chỉ giữ link chính sách); không hiện số sao hay đánh giá. Màn chào vẫn cho dùng không cần tài khoản, thêm nút "Continue with Google" (chỉ chữ, chưa có logo Google).
- 2026-10-01: Thêm Account › Language: chọn "System default" hoặc 1 trong 12 ngôn ngữ, đổi ngay. Không thêm thư viện appcompat: Android 13+ dùng LocaleManager của hệ thống (khớp với Cài đặt › Ứng dụng › Ngôn ngữ), Android 8–12 lưu mã ngôn ngữ trong SharedPreferences `app_locale` (chỉ trên máy) rồi tạo lại màn hình.
- 2026-10-01: Nhúng font DM Serif Display (SIL OFL 1.1) cho tiêu đề lớn. Font chỉ có chữ Latin nên tiếng Việt, Nga, Nhật, Hàn, Hindi dùng font có chân của hệ thống để không vỡ dấu.
