const admin = require("firebase-admin");

// 1. Setup Firebase (Dùng file key bạn đã tải)
var serviceAccount = require("./service-account.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "xx" // <-- Nhớ thay đúng link DB của bạn
});

const db = admin.database();
const messaging = admin.messaging();

console.log("👀 Đang rình rập đơn hàng mới trên Database...");

// 2. Lắng nghe nhánh 'orders'
// 'child_added' sẽ chạy mỗi khi có đơn mới (hoặc khi chạy lại server nó sẽ quét lại đơn cũ)
// Để tránh quét lại đơn cũ, ta có thể giới hạn bằng query, nhưng để test đơn giản thì cứ nghe hết.
db.ref("orders").limitToLast(1).on("child_added", (snapshot) => {
    const orderData = snapshot.val();
    const orderId = snapshot.key;

    // Kiểm tra logic để không gửi lại thông báo cho đơn cũ (Ví dụ check thời gian tạo)
    // Ở đây mình làm đơn giản: Cứ thấy data mới nhảy vào là bắn (có thể bị lặp khi restart server)
    
    if (!orderData) return;

    console.log(`📦 Phát hiện đơn mới: ${orderId}`);

    const totalAmount = orderData.total || 0;
    const totalStr = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(totalAmount);

    const message = {
        notification: {
            title: "📦 Đơn hàng mới!",
            body: `Mã: ${orderId} - Tổng: ${totalStr}`
        },
        data: {
            open_fragment: "order_management",
            order_id: String(orderId)
        },
        topic: "orders"
    };

    // Gửi thông báo
    messaging.send(message)
        .then((response) => {
            console.log("✅ Đã gửi thông báo:", response);
        })
        .catch((error) => {
            console.log("❌ Lỗi gửi:", error);
        });
});
