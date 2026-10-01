# SnapSheet: Document Scanner

| | |
|---|---|
| Slug | `snap-sheet` |
| Package | `com.ethanstudio.snapsheet` (không bao giờ đổi) |
| Tạo ngày | 2026-10-01 |
| Chính sách quyền riêng tư | https://nguyentranminhnhat3536-gif.github.io/howtogetapp/snap-sheet/privacy-policy.html |
| Bản chạy thử | https://github.com/nguyentranminhnhat3536-gif/howtogetapp/releases/tag/preview-snap-sheet |
| Bản bấm thử trên web | https://claude.ai/artifact/WdSXixveS1F2cq67mptcnP (toàn bộ app sau nhóm A đợt 1, tiếng Việt, 25 khung từng màn; dựng từ code 2026-10-01) |
| Link Google Play | (chưa có) |

## Trạng thái
Bước 3 · Nhóm A đợt 1 xong (chọn nhiều, gộp PDF cho Pro, sắp/xóa trang, thêm trang, in, thư mục, nén PDF): CI #39 xanh (build + 95 unit test + lint), /ship CHOT 2026-10-01. Dữ liệu lên schema v2 bằng migration, tài liệu cũ giữ nguyên. Chờ Ethan thử bản APK (DUYỆT-2), nhớ cài đè lên bản cũ để thử nâng cấp dữ liệu. Thanh toán, giảm giá và OCR Pro chỉ thử được sau khi tạo sản phẩm trong Play Console.

## Việc tiếp theo
- [ ] Ethan duyệt kế hoạch bản iOS: `PLAN-ios.md` (DUYỆT-$)
- [x] Code đăng ký, đăng nhập (email + Google), quên mật khẩu, xóa tài khoản (CI xanh 2026-10-01)
- [x] Firebase đã cấu hình (project `snapsheet`, gói Spark). Ethan thử trên máy thật 2026-10-01: đăng ký email và đăng nhập Google chạy được
- [ ] Khi lên Play: thêm SHA-1/SHA-256 của khóa Play App Signing vào Firebase (không thì đăng nhập Google hỏng ở bản tải từ Play)
- [ ] Dán link trang xóa tài khoản vào Play Console: https://nguyentranminhnhat3536-gif.github.io/howtogetapp/snap-sheet/delete-account.html
- [x] CI xanh
- [x] Làm lại giao diện theo thiết kế đã duyệt, thêm Account › Language (CI #32 xanh, /ship CHOT 2026-10-01)
- [ ] Ethan thử APK trên điện thoại và góp ý (DUYỆT-2)
- [x] Nhóm A đợt 1: chọn nhiều, gộp PDF (Pro), sắp/xóa trang, thêm trang, in, thư mục, nén (CI #39 xanh, /ship CHOT 2026-10-01)
- [ ] Nhóm A đợt 2: ký tên (Pro), chèn chữ mờ (Pro), nhập file PDF có sẵn, quét mã QR/mã vạch (đã code qua /ship 2026-10-01; đánh dấu xong khi CI xanh)
- [ ] Đợt sau: khóa PDF bằng mật khẩu (Pro)
- [ ] Gia cố sau đợt 1 (góp ý không bắt buộc của reviewer): PagesViewModel chỉ trả `saving = false` khi lỗi (chặn bấm Lưu 2 lần); luồng in trong Print.kt bắt mọi `Exception`; FilesScreen tắt các hộp thoại khi thoát chế độ chọn; báo rõ khi thêm trang bị cắt vì giới hạn; chữ `limit_pages_free` không ghi cứng "5 trang"; `rename` dùng `UPDATE docs SET name`; dấu tích cho "No folder"; dọn tài liệu gộp dở khi app bị tắt giữa chừng; dùng chung `Mutex` cho `recoverPendingEdits` và `rewritePages`
- [x] Xóa tài liệu lỗi thì báo bằng snackbar, không crash (DocViewModel.delete, /ship 2026-10-01)
- [ ] Vòng gia cố: rà hết bộ nhớ khi lưu scan và khi OCR (OutOfMemoryError trong TextOcr); OcrViewModel.retry() kiểm lại isPro; ocr-locked chỉ mở bảng chọn khi đang đứng ở ROUTE_OCR_LOCKED
- [ ] Ethan tạo offer giảm 10% cho `snapsheet_lifetime` trong Play Console (DUYỆT-$)
- [ ] Gửi Ethan danh sách đề xuất tính năng mới để chọn
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

**Để sau:** sao lưu, chỉnh sửa trang (xoay, cắt lại, kéo thả), xuất Word.

## Màn hình
| Màn hình | Làm gì |
|---|---|
| Home | Lời chào + ảnh đại diện, thẻ lớn "Scan document", 4 lối tắt tròn (một trang, nhiều trang, thẻ căn cước, nhập ảnh), danh sách gần đây + "See all" |
| Files | Tìm kiếm, sắp xếp (mới nhất / tên), nhóm "Hôm nay" / "Trước đó", dòng phụ có số trang và dung lượng. Hàng chip thư mục ("All", từng thư mục, "New folder"); đang lọc một thư mục thì có Đổi tên / Xóa thư mục (xóa thư mục không xóa tài liệu). Chọn nhiều (nút Select hoặc nhấn giữ): vòng chọn có số thứ tự, thanh hành động Chia sẻ / Chuyển / Gộp (PRO) / Xóa |
| Tools | Lưới 11 thẻ công cụ: một trang, nhiều trang, thẻ căn cước, ảnh sang PDF, nhập file PDF, quét mã QR, lấy chữ (PRO), gộp PDF (PRO, chuyển sang Files ở chế độ chọn), ký tên (PRO, bảng chọn tài liệu), chèn chữ mờ (PRO, bảng chọn tài liệu), chia sẻ nhanh |
| Account | Thẻ Pro, nâng cấp, khôi phục giao dịch, Language (chọn ngôn ngữ của app), đánh giá, chia sẻ, liên hệ, chính sách |
| Lấy chữ (OCR, Pro) | Chọn tài liệu (bảng dưới từ Tools), đọc từng trang có tiến độ và nút Hủy, kết quả: chọn trang, tìm có tô vàng, đếm từ/ký tự, nối dòng, Copy/Share/Lưu .txt; không thấy chữ: 3 mẹo; bản miễn phí: bảng PRO |
| Tài liệu | Vuốt xem từng trang ("Page x of y"), 4 nút tròn (mở PDF, chia sẻ PDF, chia sẻ ảnh, lấy chữ), hàng nút thứ hai (thêm trang, sửa trang, in, nén), hàng nút thứ ba: Ký tên, Chèn chữ mờ (PRO), thanh lượt xuất miễn phí, chuyển vào thư mục, đổi tên, xóa |
| Sửa trang | Mỗi hàng một trang: ảnh nhỏ, nút Lên / Xuống / Xóa; bấm Lưu mới ghi vào tài liệu; quay lại khi chưa lưu thì hỏi bỏ thay đổi |
| Bảng Nén | Chọn mức Nhỏ / Vừa / Gốc rồi chia sẻ bản nén (tài liệu đã lưu không đổi) |
| Ký tên (Pro) | Khung vẽ chữ ký 2:1 (xóa nét, xóa chữ ký đã lưu, dùng chữ ký này); đặt chữ ký lên một hoặc nhiều trang: chuyển trang, kéo để đổi chỗ, thanh trượt đổi cỡ, bỏ khỏi trang; Lưu tạo bản sao "(signed)", bản gốc giữ nguyên |
| Chèn chữ mờ (Pro) | Xem trước trang đầu, nhập chữ (tối đa 40 ký tự) hoặc chọn gợi ý, 3 màu, 3 độ đậm; Lưu tạo bản sao "(watermark)" có chữ chéo trên mọi trang, bản gốc giữ nguyên |
| Kết quả quét mã | Bảng dưới hiện nội dung mã (chọn được), Sao chép, Chia sẻ; link http/https có thêm nút Mở liên kết; Quét lại, Đóng. Không lưu kết quả |
| Màn mua | Nền "bầu trời", tiêu đề chữ có chân, ba gói (năm, tháng, vĩnh viễn) có nhãn góc, giá năm quy ra mỗi tháng, dòng điều khoản theo gói, Restore, link chính sách |
| Bảng Scan | Bấm nút + ở giữa: quét bằng camera hoặc nhập ảnh |

Giao diện theo Scan Design System (https://claude.ai/artifact/UzxZzBwaCYBTAcHrdgpB6w).

## Dữ liệu
- Room `snapsheet.db`, **schema v2**: bảng `docs` (tên, ngày, số trang, `folderId` cho phép null) và bảng `folders` (tên, ngày tạo). Nâng từ v1 bằng `MIGRATION_1_2` (chỉ `CREATE TABLE` + `ADD COLUMN`, không xóa gì): tài liệu cũ còn nguyên, `folderId = NULL` (nằm ở "All"). Không dùng `fallbackToDestructiveMigration`.
- File ảnh và PDF trong `filesDir/docs/<id>/`, chỉ app đọc được; chia sẻ qua FileProvider. Bản nén để chia sẻ ghi đè vào `docs/<id>/compressed.pdf`. Sửa/thêm trang dựng bản mới trong `docs/<id>.new` rồi mới thay bản cũ; mở app thì tự dọn hoặc hoàn tất lần ghi bị ngắt.
- Chữ ký: một file PNG nền trong suốt ở `filesDir/signature/signature.png` (chỉ app đọc được, sao lưu Android đang tắt). Xóa được trong màn Ký tên. Ký tên và chèn chữ mờ tạo tài liệu mới (bản sao), không sửa bản gốc.
- Nhập PDF: file được chép tạm vào `cacheDir/import` rồi xóa ngay sau khi dựng xong (và mỗi lần mở app).
- DataStore `pro`: đã mua Pro chưa (lưu để dùng khi mất mạng; mỗi lần mở app hỏi lại Google Play) và số lượt xuất trong ngày.
- Không có mạng của riêng app, không có tài khoản.

## Kiếm tiền
- Mô hình: freemium. Không quảng cáo (không AdMob, không cần UMP).
- Gói tháng có **ưu đãi dùng thử miễn phí 7 ngày** (tạo trong Play Console: base plan `monthly` › Add offer › Free trial 7 ngày, điều kiện "New customer acquisition"). Trong 7 ngày: dùng đủ Pro. Hết 7 ngày: tự trừ 0,99 USD/tháng. Nếu hủy trong 7 ngày: không mất tiền, về bản miễn phí (5 trang/tài liệu, 3 lượt xuất/ngày, không OCR; tài liệu đã tạo vẫn giữ). Mỗi tài khoản Google chỉ được dùng thử một lần (Play tự kiểm tra).
- Bản miễn phí: quét không giới hạn số tài liệu, 5 trang mỗi tài liệu, 3 lượt xuất (mở/chia sẻ PDF hoặc ảnh) mỗi ngày, không OCR. Không đóng dấu logo.
- Sản phẩm Play Billing (ID phải khớp từng chữ):
  - Đăng ký (Subscription) `snapsheet_pro`, hai base plan: `monthly` (gia hạn mỗi tháng) và `yearly` (mỗi năm). Có thể thêm ưu đãi dùng thử miễn phí; app tự nhận và hiện.
  - Sản phẩm trong app (One-time product) `snapsheet_lifetime`: mua một lần, không hoàn trả tự động.
- Giá Ethan chốt ngày 2026-10-01: tháng **0,99 USD**, năm **9,99 USD** (rẻ hơn 12 tháng lẻ 16%, màn mua tự hiện "Save 15%"), vĩnh viễn **100 USD**, đang ưu đãi **giảm 10% còn 90 USD** (Ethan chốt 2026-10-01). Đặt trong Play Console bằng USD, Play tự quy đổi giá từng nước. App lấy giá thật từ Google Play, không gõ cứng.
- Gói vĩnh viễn: app lấy giá gốc, giá giảm và % từ Google Play. Chỉ hiện giá gạch và nhãn khi Play trả về ưu đãi thật; hết ưu đãi thì tự về 100 USD. Các bước Ethan làm trong Play Console:
  1. Monetize with Play › Products › One-time products › `snapsheet_lifetime`.
  2. Ở purchase option "Buy" đặt giá 100 USD (Set prices › USD 100.00 › Update exchange rates nếu muốn tự quy đổi).
  3. Trong purchase option đó › Add offer › **Discounted offer** › giảm **10%** (giá 90 USD), chọn thời gian bắt đầu và kết thúc, chọn nước áp dụng › Activate.
  4. Mở app (bản tải từ Play hoặc tài khoản tester) › Account › Get Pro: thẻ Lifetime phải hiện ~~$100.00~~ $90.00 kèm nhãn "10% OFF".
- Giá gốc 100 USD nên là giá thật; ở EU giá trước giảm phải là giá thấp nhất trong 30 ngày, nên đặt thời hạn cho đợt giảm.
- Mua xong: acknowledge trong vòng 3 ngày (app tự làm), nếu không Google hoàn tiền.

## Quyền (permissions) và lý do
| Quyền | Lý do |
|---|---|
| (không có) | Quét dùng màn hình của Google Play services (không cần CAMERA), ảnh chọn bằng trình chọn ảnh hệ thống (không cần đọc thư viện). BILLING, INTERNET, ACCESS_NETWORK_STATE, READ_GSERVICES do thư viện Play Billing, ML Kit và Firebase tự thêm. Mạng chỉ dùng khi đăng nhập. USE_BIOMETRIC và USE_FINGERPRINT (Credential Manager tự thêm) đã gỡ vì app không dùng. Quét mã dùng màn quét của Google Play services, không cần CAMERA. Nhập PDF dùng trình chọn tệp của hệ thống, không cần quyền đọc bộ nhớ. |

## Tài nguyên bên thứ ba và giấy phép
| Tài nguyên (hình, font, âm thanh, dữ liệu…) | Nguồn | Giấy phép |
|---|---|---|
| Icon giao diện và icon app | Tự vẽ (vector) theo Scan Design System | Của Ethan |
| Font | Roboto mặc định của Android | Apache 2.0 |
| Font tiêu đề DM Serif Display (`res/font/dm_serif_display.ttf`, chỉ dùng cho en/es/pt/fr/de/id/tr; ngôn ngữ khác dùng font có chân của hệ thống) | https://github.com/google/fonts/tree/main/ofl/dmserifdisplay | SIL OFL 1.1 (văn bản kèm theo ở `assets/licenses/DMSerifDisplay-OFL.txt`) |
| ML Kit Document Scanner, ML Kit Text Recognition | Google | Điều khoản ML Kit |
| Play Billing Library 9.1.0 | Google | Apache 2.0 |
| Google code scanner (play-services-code-scanner 16.1.0) | Google | Điều khoản ML Kit |

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
- 2026-10-01: Extract text thành màn riêng theo thiết kế đã duyệt. Bản miễn phí không chạy OCR mà chỉ thấy bảng PRO. Không lưu kết quả OCR (không đổi dữ liệu). Lưu .txt qua trình chọn nơi lưu của hệ thống (không cần quyền).
- 2026-10-01: Gói vĩnh viễn đọc offer qua `getOneTimePurchaseOfferDetailsList` (Billing 8+) và mua đúng offerToken.
- 2026-10-01: Xóa tài liệu lỗi thì báo, không crash.
- 2026-10-01: Dựng lại toàn bộ app thành bản bấm thử trên web để Ethan kiểm tra. Màu lấy từ code (`Color.kt`, `Gradients.kt`), không lấy từ artifact Scan Design System vì token ở đó là bản cũ (#3880F8) khác màu app đang dùng (#2A6FE8).
- 2026-10-01: Nhóm A đợt 1 (chọn nhiều, gộp PDF, sắp/xóa/thêm trang, in, thư mục, nén). Gộp là Pro, còn lại miễn phí, giới hạn bản miễn phí giữ nguyên. Lượt xuất: In = 1 lượt; chia sẻ bản nén (kể cả mức Gốc) = 1 lượt; chia sẻ nhiều tài liệu cùng lúc = mỗi tài liệu 1 lượt, không đủ thì chặn cả lần (để không lách được giới hạn 3 lượt/ngày). Gộp, sắp/xóa/thêm trang, chuyển thư mục không tính lượt.
- 2026-10-01: Nén và dựng lại PDF không dùng `PdfDocument` của Android (nhúng ảnh không nén nên file to hơn ảnh JPEG gốc nhiều lần). Viết bộ ghi PDF nhỏ bằng Kotlin (`JpegPdfWriter`) nhúng thẳng byte JPEG, không thêm thư viện. `PdfBuilder` cũ giữ cho luồng quét/nhập ảnh.
- 2026-10-01: Sắp trang bằng nút Lên/Xuống, không kéo thả (kéo thả dễ lỗi, khó dùng với chữ to và TalkBack). Thư mục chỉ một cấp; mỗi tài liệu thuộc 0 hoặc 1 thư mục.
- 2026-10-01: Nhóm A đợt 2 (Ethan muốn thêm công cụ để Pro đáng tiền hơn). Làm 4 công cụ: Ký tên (Pro), Chèn chữ mờ (Pro), Nhập file PDF (miễn phí), Quét mã QR/mã vạch (miễn phí). Cả bốn chạy offline, không xin quyền mới. Khóa PDF bằng mật khẩu để đợt sau: phải tự viết phần mã hóa PDF, không kiểm được trên JVM, sai thì người nhận không mở được file.
- 2026-10-01: Ký tên là Pro (giá trị cao nhất: hợp đồng, đơn từ; app cùng loại cũng thu phí). Chèn chữ mờ là Pro (bảo vệ bản sao CCCD/giấy tờ). Nhập PDF miễn phí (cửa đưa file vào app; giới hạn 5/30 trang có sẵn tự gợi ý nâng cấp). Quét mã miễn phí (tiện ích nhỏ, giúp mở app thường hơn). Ô Pro có nhãn PRO; người miễn phí bấm thì báo rồi mở màn mua. Giới hạn bản miễn phí giữ nguyên.
- 2026-10-01: Bốn công cụ mới không tính lượt xuất. Lượt chỉ tính khi file rời khỏi app (mở, chia sẻ, in, chia sẻ bản nén); chia sẻ tài liệu đã ký hoặc có chữ mờ vẫn tính lượt như thường.
- 2026-10-01: Ký tên và chèn chữ mờ tạo BẢN SAO mới ("<tên> (signed)" / "<tên> (watermark)" theo ngôn ngữ, tối đa 80 ký tự, cùng thư mục với bản gốc), không sửa bản gốc: chữ ký đã in vào ảnh thì không gỡ được, và người dùng thường cần giữ bản chưa ký.
- 2026-10-01: Ký tên: chỉ lưu một chữ ký (PNG trong suốt, mực xanh đen #0B1A3A), vẽ bằng ngón tay trong khung 2:1. Một lần ký đặt được lên nhiều trang. Kéo để đổi chỗ, đổi cỡ bằng thanh trượt (dễ dùng hơn chụm 2 ngón, TalkBack đọc được). Vị trí mặc định góc dưới phải, rộng 35% trang.
- 2026-10-01: Chữ mờ: một dòng chữ chéo giữa trang (dưới trái lên trên phải) trên mọi trang; 3 màu (Xám, Đỏ, Xanh), 3 độ đậm (15%, 30%, 45%), tối đa 40 ký tự; font đậm của hệ thống nên tiếng Việt có dấu, Hindi, Nhật hiện đúng. Ảnh xem trước dùng đúng hàm vẽ của bản lưu.
- 2026-10-01: Nhập PDF: chọn file bằng trình chọn tệp hệ thống (không cần quyền). Mỗi trang dựng thành ảnh JPEG 200 dpi (cạnh dài ≤ 2000 px) như một trang quét, rồi ghép lại PDF bằng `JpegPdfWriter`, nên chữ trong PDF gốc không còn bôi chọn được. Chỉ nhập 5 (miễn phí) / 30 (Pro) trang đầu và báo số trang nếu bị cắt. File trên 100 MB thì từ chối; PDF có mật khẩu thì báo rõ.
- 2026-10-01: Quét mã dùng Google code scanner `play-services-code-scanner` 16.1.0 (màn quét do Google Play services cung cấp, không cần quyền CAMERA); meta-data `barcode_ui` để Play tải sẵn mô-đun khi cài, dùng được offline. Kết quả không lưu; chỉ link http/https mới có nút Mở, và luôn hiện địa chỉ trước khi mở.
