# Kế hoạch kỹ thuật: SnapSheet cho iPhone (iOS)

Trạng thái: **chờ Ethan duyệt (DUYỆT-$)**. Chưa viết dòng code nào. Ngày 2026-10-01.
Phạm vi: làm bản iOS có cùng tính năng với bản Android hiện tại (quét, tệp, OCR, 3 gói mua). Đăng ký tài khoản làm sau, theo `PLAN-account.md`.

## 1. Cách làm: viết app iOS riêng bằng Swift + SwiftUI (đề nghị)
Bản Android dựa vào công cụ riêng của Google (ML Kit, Play Billing) nên không chạy được trên iPhone. Mỗi phần cốt lõi đều có bản tương đương của Apple, miễn phí, chạy trên máy:

| Phần | Android (đang có) | iOS (sẽ dùng) |
|---|---|---|
| Quét tài liệu, cắt viền | ML Kit Document Scanner | VisionKit `VNDocumentCameraViewController` |
| Đọc chữ (OCR) | ML Kit Text Recognition | Vision `VNRecognizeTextRequest` (cần kiểm tra hỗ trợ tiếng Việt trên iOS đích) |
| Ghép PDF | `PdfDocument` | PDFKit |
| Mua gói | Play Billing | StoreKit 2 |
| Lưu dữ liệu | Room + DataStore | SwiftData + thư mục riêng của app |
| Chọn ảnh | Photo Picker | `PhotosPicker` |
| Giao diện | Jetpack Compose | SwiftUI, theo cùng Scan Design System |

Phương án không chọn:
- **Kotlin/Compose Multiplatform:** dùng chung được ít (quét, OCR, thanh toán đều phải viết riêng), lại thêm độ phức tạp cho app nhỏ.
- **Viết lại cả hai bằng Flutter:** bỏ mất bản Android đã xanh CI, rủi ro cao.

Giữ chung giữa hai bản: thiết kế, chữ (Anh + Việt), tên gói, giới hạn bản miễn phí (5 trang, 3 lượt xuất/ngày, OCR chỉ Pro).

## 2. Gói mua trên App Store
- Một nhóm đăng ký tự gia hạn "SnapSheet Pro" có 2 gói: `com.ethanstudio.snapsheet.pro.monthly`, `com.ethanstudio.snapsheet.pro.yearly`.
- Một sản phẩm mua một lần (non-consumable): `com.ethanstudio.snapsheet.lifetime`.
- Giá lấy từ StoreKit, không gõ cứng. Bắt buộc có nút Restore Purchases, ghi rõ giá, kỳ hạn, cách hủy, kèm link Điều khoản (EULA) và Chính sách quyền riêng tư trên màn mua.
- **Mua trên Android không mở khóa trên iPhone và ngược lại.** Muốn dùng chung thì cần tài khoản + máy chủ ghi nhận quyền lợi (ví dụ dịch vụ như RevenueCat; chưa tra giá). Đó là giai đoạn sau, cần duyệt riêng.

## 3. Build và thử khi không có máy Mac
- Phiên làm việc của tôi không build iOS được. Build chạy trên **GitHub Actions bằng máy macOS**, giống cách bản Android đang build.
- Dự án Xcode tạo bằng XcodeGen (`project.yml`) để không phải sửa tay file `.pbxproj`. Thư mục: `apps/snap-sheet-ios/`. Workflow riêng: `.github/workflows/ios.yml`.
- Mỗi lần push: build cho máy ảo và chạy unit test (không cần ký).
- Bản cho Ethan thử: build đã ký, tự đẩy lên **TestFlight**. Ethan cài app TestFlight trên iPhone rồi thử. Cần chứng chỉ ký và khóa App Store Connect API, cất trong GitHub Secrets.
- Yêu cầu của Apple từ 28/4/2026: phải build bằng Xcode 26 và SDK iOS 26 trở lên ([Apple](https://www.developer.apple.com/news/upcoming-requirements/)). Đề nghị hỗ trợ từ iOS 17 (để dùng SwiftData); chưa tra tỉ lệ máy theo phiên bản.
- Rủi ro: viết Swift mà không có máy ảo để xem nên vòng sửa lỗi qua CI và TestFlight sẽ chậm hơn Android. Phần giao diện phụ thuộc vào việc Ethan thử trên iPhone thật.

## 4. Luật App Store cần đáp ứng
- Nhãn quyền riêng tư (App Privacy) trong App Store Connect và file khai báo quyền riêng tư `PrivacyInfo.xcprivacy` trong app.
- Câu xin quyền camera và ảnh, viết rõ lý do (bắt buộc trên iOS, khác Android).
- Nếu sau này có đăng nhập Google thì phải có thêm **Sign in with Apple**, và phải xóa được tài khoản ngay trong app.
- Trong app iOS không nhắc tới Android hay Google Play.
- Không có yêu cầu closed test 12 người × 14 ngày như Google Play. Thử nội bộ qua TestFlight; mời người ngoài thì Apple duyệt bản beta trước.

## 5. Việc Ethan phải tự làm (từng bước)
1. Có Apple ID bật xác minh hai bước. Vào https://developer.apple.com/programs/enroll, đăng ký **cá nhân**, trả **99 USD/năm**.
2. Trong App Store Connect › Business: ký Paid Apps Agreement, điền thuế và tài khoản ngân hàng (bắt buộc mới bán được gói).
3. Đăng ký **App Store Small Business Program** để phí Apple thu là 15% thay vì 30%.
4. Tạo App ID `com.ethanstudio.snapsheet` và tạo app trong App Store Connect.
5. Tạo khóa App Store Connect API (Users and Access › Integrations): lưu Issuer ID, Key ID và file `.p8` vào **GitHub Secrets** (tôi sẽ đưa tên từng secret). Không dán vào chat.
6. Cài TestFlight trên iPhone.
Bước 1–3 tốn tiền hoặc liên quan tài khoản và ngân hàng (DUYỆT-$).

## 6. Chi phí
| Khoản | Số tiền | Ghi chú |
|---|---|---|
| Apple Developer Program | 99 USD/năm | Ngừng gia hạn thì app bị gỡ khỏi App Store ([nguồn](https://studio.adalo.com/blog/apple-developer-program-guide)) |
| Phí Apple trên doanh thu | 15% | Khi tham gia Small Business Program, doanh thu dưới 1 triệu USD/năm ([Apple](https://developer-mdn.apple.com/app-store/small-business-program/)); không tham gia thì 30% |
| Build trên GitHub Actions (macOS) | 0 đồng hiện tại | Repo đang **công khai** nên máy build chuẩn miễn phí. Nếu chuyển sang riêng tư: macOS tính gấp 10 lần phút miễn phí, vượt thì khoảng 0,062 USD/phút ([nguồn](https://www.thepricer.org/how-much-does-github-actions-cost/)) |
| Máy Mac | Không bắt buộc | Có Mac thì sửa lỗi nhanh hơn; chưa ước giá |
| iPhone để thử | Ethan đã có thì 0 | Cần iPhone chạy được iOS 17 trở lên |
| Công của tôi | Không tính tiền | Ước lượng 3–5 phiên làm việc, nhiều hơn bản Android vì sửa lỗi qua CI |
Số liệu trên lấy từ các trang tổng hợp và trang Apple, cần Ethan xem lại trước khi trả tiền.

Lưu ý: repo công khai nghĩa là ai cũng đọc được code. Bí mật (chứng chỉ, khóa) vẫn chỉ nằm trong GitHub Secrets.

## 7. Các mốc
1. Ethan duyệt kế hoạch và làm mục 5.
2. Tôi dựng dự án iOS + CI, rồi làm quét, tệp, PDF, chia sẻ. Gửi bản TestFlight cho Ethan thử (DUYỆT-2).
3. Thêm OCR và 3 gói mua (StoreKit 2), thử mua bằng tài khoản Sandbox.
4. Nội dung App Store (Anh + Việt), ảnh chụp màn hình, nhãn quyền riêng tư, nộp duyệt (DUYỆT-3).

## 8. Đề nghị về thứ tự
Đưa bản Android lên Google Play trước (đang chờ Ethan thử APK và tạo gói trong Play Console). Có thể bắt đầu iOS khi bản Android đang chạy closed test 14 ngày, để tận dụng thời gian chờ.

## 9. Câu hỏi Ethan cần trả lời
- Duyệt chi 99 USD/năm cho tài khoản Apple không?
- Bắt đầu iOS ngay, hay chờ bản Android vào closed test?
- Có iPhone để thử không, chạy iOS bản nào?
- Chấp nhận mua Pro trên iPhone và Android là riêng nhau (giai đoạn đầu) không?
