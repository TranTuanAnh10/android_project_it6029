const express = require('express');
const bodyParser = require('body-parser');
const admin = require('firebase-admin');

// --- CẤU HÌNH ---
const PORT = 5000; // Cổng để server lắng nghe
const SERVICE_ACCOUNT_FILE = './service-account.json'; // Đường dẫn tới file key bạn vừa tải

// 1. Khởi tạo Firebase Admin SDK với file key
const serviceAccount = require(SERVICE_ACCOUNT_FILE);
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

// 2. Tạo server Express
const app = express();
app.use(bodyParser.json()); // Để server đọc được dữ liệu JSON từ app Android

// 3. Tạo một API để App User gọi vào
app.post('/send-notification', (req, res) => {
  console.log('Đã nhận được request gửi thông báo:', req.body);

  // Lấy dữ liệu từ App User gửi lên
  const orderId = req.body.orderId;
  const totalAmount = req.body.total;

  // Format tiền tệ
  const totalStr = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(totalAmount);

  // Chuẩn bị nội dung thông báo
  const message = {
    notification: {
      title: '📦 Đơn hàng mới (Từ Server Local)!',
      body: `Mã: ${orderId} - Tổng: ${totalStr}`
    },
    data: {
      open_fragment: 'order_management' // Giữ nguyên để khớp với code AdminScreenActivity của bạn
    },
    topic: 'orders' // Gửi đến topic mà App Admin đã đăng ký
  };

  // 4. Dùng Admin SDK để gửi thông báo
  admin.messaging().send(message)
    .then(response => {
      console.log('✅ Gửi thông báo thành công:', response);
      res.status(200).send({ success: true, message: 'Notification sent successfully' });
    })
    .catch(error => {
      console.error('❌ Lỗi gửi thông báo:', error);
      res.status(500).send({ success: false, error: error.message });
    });
});

// 5. Khởi động server
app.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 Server thông báo đang chạy tại cổng ${PORT}`);
  console.log(`Hãy đảm bảo App User gọi đến đúng địa chỉ IP của máy tính này!`);
});
