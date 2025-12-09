const functions = require("firebase-functions");
const admin = require("firebase-admin");

// Khởi tạo Admin SDK
admin.initializeApp();

/**
 * Trigger: Khi có đơn hàng mới được tạo trong nhánh "orders/{orderId}"
 * Phiên bản: Cloud Functions v1
 */
exports.sendOrderNotification = functions.database.ref("/orders/{orderId}")
  .onCreate((snapshot, context) => {
    // 1. Lấy dữ liệu đơn hàng
    const orderData = snapshot.val();
    const orderId = context.params.orderId;

    if (!orderData) {
      console.log("Dữ liệu đơn hàng rỗng, bỏ qua.");
      return null;
    }

    const totalAmount = orderData.total;

    // Xử lý hiển thị tiền tệ
    let totalStr = "0";
    if (totalAmount) {
      totalStr = totalAmount.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".");
    }

    // 2. Cấu hình thông báo (QUAN TRỌNG NHẤT: payload)
    const payload = {
      notification: {
        title: "📦 Đơn hàng mới!",
        body: `Mã: ${orderId} - Tổng: ${totalStr} VNĐ`,
        sound: "default",
      },
      // Phần data này KHỚP HOÀN TOÀN với code AdminScreenActivity của bạn
      data: {
        open_fragment: "order_management",
        order_id: orderId,
      },
    };

    // 3. Gửi đến topic 'orders'
    return admin.messaging().sendToTopic("orders", payload)
      .then((response) => {
        console.log("✅ Gửi thông báo thành công:", response);
        return null;
      })
      .catch((error) => {
        console.error("❌ Lỗi gửi thông báo:", error);
        return null;
      });
  });
