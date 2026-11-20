# Peachy (Android) – Hướng dẫn build

## Cách A – Android Studio (trên laptop)
1. Cài Android Studio (developer.android.com/studio).
2. Giải nén zip → File > Open → chọn thư mục `Peachy`. Chờ Gradle sync xong (lần đầu tải khá lâu).
3. Build > Build App Bundle(s) / APK(s) > Build APK(s).
4. File APK ở `app/build/outputs/apk/debug/app-debug.apk` → gửi sang điện thoại → mở để cài
   (Xiaomi sẽ hỏi cho phép "Cài ứng dụng không rõ nguồn gốc" → Cho phép).

## Cách B – GitHub (không cần cài gì)
1. Tạo repo mới trên github.com → "uploading an existing file" → kéo toàn bộ nội dung thư mục `Peachy` vào → Commit.
2. Tab **Actions** → workflow "Build APK" tự chạy (~5 phút).
3. Mở lần chạy → mục **Artifacts** → tải `Peachy-apk` → giải nén → cài APK lên điện thoại.

## Thêm widget
Giữ lâu chỗ trống trên màn hình chính → Widget → "Peachy" → kéo ra → chọn ngày.
Hoặc trong app: mở một ngày → "Lưu & ghim widget ra màn hình".
Chạm vào widget để mở phần chỉnh ngày đó. Giữ lâu widget → kéo giãn để đổi cỡ (nhỏ: chỉ số; rộng: đủ thông tin).

## Xiaomi (MIUI/HyperOS)
Để widget nhảy số đúng lúc nửa đêm: Cài đặt → Ứng dụng → Peachy → Tiết kiệm pin → "Không giới hạn", và bật "Tự khởi chạy".
