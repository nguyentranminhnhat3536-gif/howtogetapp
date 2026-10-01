# Kế hoạch kỹ thuật: đăng ký và đăng nhập tài khoản (SnapSheet)

Trạng thái: **chờ Ethan duyệt (DUYỆT-$)**. Chưa viết dòng code nào. Ngày 2026-10-01.
Màn hình đã thiết kế: https://claude.ai/artifact/Nug9pRAZzY5ejELXK2TNZz

## 1. Nguyên tắc
- Tài khoản là **tùy chọn**. App vẫn dùng đủ mọi tính năng hiện có khi không đăng nhập ("Continue without an account"). Việc mua Pro vẫn gắn với tài khoản Google Play, không gắn với tài khoản này.
- Giai đoạn 1 chỉ làm **nhận diện người dùng** (đăng ký, đăng nhập, quên mật khẩu, xóa tài khoản). Không đưa tài liệu lên mạng.
- Lưu ý thẳng: ở giai đoạn 1 đăng nhập chưa mang lại lợi ích gì cho người dùng. Chỉ nên làm khi có tính năng cần nó (xem mục 7).

## 2. Chọn dịch vụ: Firebase Authentication
- Hỗ trợ sẵn email + mật khẩu, Google, gửi email xác nhận, đặt lại mật khẩu, xóa tài khoản; không phải tự dựng máy chủ.
- Chi phí: xem mục 6.
- Phương án khác (tự dựng máy chủ, Supabase Auth) tốn công hơn mà không rẻ hơn ở quy mô nhỏ. Chưa chọn.

## 3. Việc Ethan phải tự làm (từng bước)
1. Vào https://console.firebase.google.com, tạo project mới (tên gợi ý `snapsheet`). Giữ gói **Spark (miễn phí)**. Không bật Google Analytics nếu không cần.
2. Thêm app Android: package `com.ethanstudio.snapsheet`. Dán dấu vân tay SHA-1 và SHA-256 (tôi sẽ đưa của bản debug trên CI; của bản phát hành lấy trong Play Console › Setup › App signing).
3. Authentication › Sign-in method: bật **Email/Password** và **Google**. Điền email hỗ trợ cho Google.
4. Tải file `google-services.json`. **Không dán nội dung vào chat.** Vào GitHub › repo › Settings › Secrets and variables › Actions › New secret, tên `GOOGLE_SERVICES_JSON_SNAP_SHEET`, dán nội dung vào đó.
5. Điền `portfolio/studio.md` (tên nhà phát triển, email hỗ trợ) vì màn đăng ký và chính sách sẽ hiện email này.

## 4. Việc phía code (sau khi Ethan duyệt)
- Thư viện: Firebase BoM, `firebase-auth`, Credential Manager + `googleid` cho "Continue with Google", plugin `com.google.gms.google-services`. Phiên bản phải tra chính thức trước khi thêm (trang Google bị chặn trong phiên này), CI sẽ báo nếu sai.
- Plugin google-services chỉ áp dụng khi có file cấu hình; CI ghi file từ secret trước khi build, bản build thiếu secret vẫn chạy được (không có đăng nhập).
- Mới: `auth/AuthRepository` (đăng ký, đăng nhập, Google, gửi lại email, đặt lại mật khẩu, đăng xuất, xóa tài khoản), `auth/AuthValidation` (kiểm tra email, mật khẩu ≥ 8 ký tự, trùng khớp), `auth/AuthErrors` (đổi lỗi Firebase thành câu dễ hiểu, song ngữ).
- Giao diện: 4 màn theo bản thiết kế (Welcome, Sign In, Sign Up, Check your inbox), thêm Forgot password; tab Account hiện email, Sign Out, Delete Account.
- Xóa tài khoản: bấm Delete Account › xác nhận › đăng nhập lại (Firebase đòi đăng nhập gần đây) › xóa. Xóa hẳn tài khoản trên Firebase; tài liệu trên máy vẫn thuộc người dùng, hỏi có xóa luôn không.
- Test: kiểm tra email, mật khẩu, bảng đổi lỗi (không cần mạng).
- Bảo mật: không ghi email, mật khẩu vào log; mật khẩu chỉ gửi cho Firebase; `google-services.json` không commit; tắt tự động sao lưu Android cho dữ liệu đăng nhập.

## 5. Chính sách Google Play
- **Xóa tài khoản (bắt buộc):** nếu app cho tạo tài khoản thì phải có đường xóa **trong app** và một **trang web** để yêu cầu xóa tài khoản và dữ liệu. ([Google Play](https://support.google.com/googleplay/android-developer/answer/13327111)). Tôi sẽ làm trang `docs/snap-sheet/delete-account.html` (cách xóa trong app, hoặc gửi email yêu cầu từ địa chỉ đã đăng ký; xử lý trong 30 ngày) và điền link vào Play Console. Xóa thủ công trên Firebase Console, đủ cho quy mô nhỏ.
- **Chính sách quyền riêng tư:** thêm mục tài khoản (email, mã người dùng), dùng Firebase Authentication, thời gian giữ, cách xóa. Sửa cả bản Anh và Việt.
- **Data safety khai lại:** Personal info › Email address và User IDs: có thu thập; mục đích App functionality và Account management; không bán, không dùng cho quảng cáo; mã hóa khi truyền; có cho yêu cầu xóa. Giữ nguyên khai về Purchase history và Diagnostics. Khai đúng những gì SDK thật sự thu thập, kiểm tra lại phần "SDK" trong Play Console.
- Nút "Continue with Google" phải dùng nút/biểu tượng chính thức theo hướng dẫn thương hiệu Google (bản thiết kế đang dùng chữ G tạm).
- Không nhắm trẻ em dưới 13 tuổi (đã khai 18+).

## 6. Chi phí (DUYỆT-$)
Số liệu lấy từ các bài tổng hợp, **chưa đối chiếu trang giá chính thức của Firebase**; Ethan cần xem lại trên trang giá trước khi duyệt.
| Khoản | Giai đoạn 1 |
|---|---|
| Firebase Authentication (email + Google) | 0 đồng. Các bài tổng hợp ghi miễn phí đến 50.000 người dùng hoạt động mỗi tháng; vượt thì khoảng 0,0055 USD mỗi người ([nguồn](https://www.rapidnative.com/blogs/firebase-authentication-pricing)) |
| Email xác nhận và đặt lại mật khẩu | 0 đồng (người gửi mặc định của Firebase; đổi sang tên miền riêng thì mất phí tên miền) |
| Trang xóa tài khoản | 0 đồng (GitHub Pages) |
| Máy chủ riêng | Không dùng |
| Công của tôi | Không tính tiền. Ước lượng 1 đến 2 phiên làm việc, thêm vài vòng sửa CI |
Rủi ro phát sinh tiền: nếu sau này nâng lên gói trả theo mức dùng (Blaze) hoặc làm sao lưu đám mây (mục 7).

## 7. Giai đoạn 2 (chưa làm, cần duyệt riêng)
Sao lưu tài liệu lên đám mây để có lý do đăng nhập. Hệ quả: cần lưu trữ trả phí theo dung lượng, tài liệu rời khỏi máy nên **phải đổi lại lời hứa "chạy hoàn toàn trên máy"**, chính sách và Data safety phải khai thêm (nội dung người dùng), cần mã hóa và xóa theo tài khoản. Chưa ước được chi phí vì chưa biết dung lượng bình quân; sẽ báo số cụ thể trước khi làm.

## 8. Các bước và mốc
1. Ethan duyệt kế hoạch và làm mục 3.
2. Tôi code, CI xanh, gửi APK (DUYỆT-2).
3. Tôi viết trang xóa tài khoản, sửa chính sách, soạn sẵn nội dung Data safety (DUYỆT-3 trước khi nộp Play).
4. Closed test chung với app, ghi vào `APP.md`.

## 9. Câu hỏi Ethan cần trả lời
- Duyệt dùng Firebase gói Spark miễn phí cho giai đoạn 1 không?
- Chấp nhận giai đoạn 1 chỉ có đăng nhập (chưa có sao lưu) không, hay muốn làm luôn sao lưu (tốn tiền, đổi lời hứa riêng tư)?
- Email hỗ trợ dùng địa chỉ nào?
