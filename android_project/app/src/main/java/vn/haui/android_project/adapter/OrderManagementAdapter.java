package vn.haui.android_project.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.Order;
import vn.haui.android_project.enums.MyConstant;

public class OrderManagementAdapter extends RecyclerView.Adapter<OrderManagementAdapter.OrderViewHolder> {

    private Context context;
    private List<Order> orderList;
    private OnOrderClickListener listener;

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    public OrderManagementAdapter(Context context, List<Order> orderList, OnOrderClickListener listener) {
        this.context = context;
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Đảm bảo bạn có file item_order_management.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_management, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);

        // 1. MÃ ĐƠN (Xử lý null an toàn)
        String orderId = order.getOrderId() != null ? order.getOrderId() : "N/A";
        // Nếu mã quá dài thì cắt bớt cho gọn
        if (orderId.length() > 12) {
            orderId = "..." + orderId.substring(orderId.length() - 8);
        }
        holder.tvOrderId.setText("Mã: " + orderId);

        // 2. ĐỊA CHỈ (Kiểm tra null nhiều lớp)
        String addressStr = "Địa chỉ không xác định";
        if (order.getAddressUser() != null && order.getAddressUser().getAddress() != null) {
            addressStr = order.getAddressUser().getAddress();
        }
        holder.tvAddress.setText("Đ/c: " + addressStr);

        // 3. THỜI GIAN HIỂN THỊ (Cũ)
        holder.tvDate.setText(order.getTimeDisplay() != null ? order.getTimeDisplay() : "");

        // 4. THỜI GIAN ĐẶT HÀNG (Created Date)
        String createdDate = order.getCreated_at();
        if (createdDate != null && !createdDate.isEmpty()) {
            // Tách ngày giờ để hiển thị đẹp hơn (nếu cần), hoặc hiển thị nguyên gốc
            // Ví dụ: "20/11/2025 10:06:44 PM" -> lấy phần giờ "10:06:44 PM"
            try {
                if (createdDate.contains(" ")) {
                    String timeOnly = createdDate.substring(createdDate.indexOf(" ") + 1);
                    holder.tvCreatedDate.setText("Thời gian: " + timeOnly);
                } else {
                    holder.tvCreatedDate.setText(createdDate);
                }
                holder.tvCreatedDate.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                holder.tvCreatedDate.setText(createdDate); // Fallback nếu lỗi tách chuỗi
            }
        } else {
            holder.tvCreatedDate.setVisibility(View.GONE); // Ẩn nếu không có ngày
        }

        // 5. TỔNG TIỀN (SỬA LỖI CRASH: Cannot format given Object as a Number)
        // Lý do: Đôi khi total trên Firebase lưu dạng String ("500000") thay vì Double (500000)
        double totalValue = 0;
        try {
            Object rawTotal = order.getTotal(); // Lấy object gốc ra kiểm tra

            if (rawTotal == null) {
                totalValue = 0;
            } else if (rawTotal instanceof Number) {
                totalValue = ((Number) rawTotal).doubleValue();
            } else if (rawTotal instanceof String) {
                // Nếu là chuỗi thì xóa các ký tự không phải số trước khi parse
                String cleanString = ((String) rawTotal).replace(",", "").replace(".", "").trim();
                if (!cleanString.isEmpty()) {
                    totalValue = Double.parseDouble(cleanString);
                }
            }
        } catch (Exception e) {
            totalValue = 0; // Nếu lỗi format quá nặng thì set về 0 để không crash app
        }

        DecimalFormat formatter = new DecimalFormat("#,###");
        holder.tvTotal.setText(formatter.format(totalValue) + "đ");

        // 6. XỬ LÝ TRẠNG THÁI & MÀU SẮC
        String statusRaw = order.getStatus();
        String statusVN = "Đang cập nhật";
        int colorRes = android.R.color.black;

        if (statusRaw != null) {
            if (statusRaw.equalsIgnoreCase(MyConstant.PREPARED)) {
                statusVN = "Chờ xác nhận";
                colorRes = android.R.color.holo_orange_dark;
            } else if (statusRaw.equalsIgnoreCase(MyConstant.PICKINGUP)) {
                statusVN = "Đang lấy hàng";
                colorRes = android.R.color.holo_purple;
            } else if (statusRaw.equalsIgnoreCase(MyConstant.DELIVERING)) {
                statusVN = "Đang giao hàng";
                colorRes = android.R.color.holo_blue_dark;
            } else if (statusRaw.equalsIgnoreCase(MyConstant.FINISH)) {
                statusVN = "Hoàn thành";
                colorRes = android.R.color.holo_green_dark;
            } else if (statusRaw.equalsIgnoreCase(MyConstant.REJECT)) {
                statusVN = "Từ chối";
                colorRes = android.R.color.holo_red_dark;
            } else if (statusRaw.equalsIgnoreCase(MyConstant.CANCEL_ORDER)) {
                statusVN = "Đã huỷ";
                colorRes = android.R.color.holo_red_dark;
            }
        }

        holder.tvStatus.setText(statusVN);
        // Cần dùng ContextCompat hoặc context.getResources() một cách an toàn
        try {
            holder.tvStatus.setTextColor(context.getResources().getColor(colorRes));
        } catch (Exception e) {
            // Fallback màu đen nếu lỗi resource
            holder.tvStatus.setTextColor(0xFF000000);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOrderClick(order);
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvDate, tvStatus, tvTotal, tvAddress, tvCreatedDate;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            // Đảm bảo các ID này khớp với item_order_management.xml
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvDate = itemView.findViewById(R.id.tv_order_date);
            tvStatus = itemView.findViewById(R.id.tv_order_status);
            tvTotal = itemView.findViewById(R.id.tv_order_total);
            tvAddress = itemView.findViewById(R.id.tv_order_address);
            tvCreatedDate = itemView.findViewById(R.id.tv_created_date);
        }
    }
}
