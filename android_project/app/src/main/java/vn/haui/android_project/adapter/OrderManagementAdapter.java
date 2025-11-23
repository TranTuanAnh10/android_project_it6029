package vn.haui.android_project.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;import androidx.recyclerview.widget.RecyclerView;

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

        // 1. Mã đơn
        String orderId = order.getOrderId() != null ? order.getOrderId() : "N/A";
        if (orderId.length() > 10) orderId = "..." + orderId.substring(orderId.length() - 8);
        holder.tvOrderId.setText("Mã: " + orderId);

        // 2. Địa chỉ
        String addressStr = "Địa chỉ không xác định";
        if (order.getAddressUser() != null && order.getAddressUser().getAddress() != null) {
            addressStr = order.getAddressUser().getAddress();
        }
        holder.tvAddress.setText("Đ/c: " + addressStr);

        // 3. Thời gian
        holder.tvDate.setText(order.getTimeDisplay() != null ? order.getTimeDisplay() : "");

        // 4. Tổng tiền
        DecimalFormat formatter = new DecimalFormat("#,###");
        holder.tvTotal.setText(formatter.format(order.getTotal()) + "đ");

        // 5. XỬ LÝ TRẠNG THÁI & MÀU SẮC
        String statusRaw = order.getStatus();
        String statusVN = "Đang cập nhật";
        int colorRes = android.R.color.black;

        if (statusRaw != null) {
            if (statusRaw.equalsIgnoreCase(MyConstant.PREPARED)) {
                statusVN = "Chờ xác nhận";
                colorRes = android.R.color.holo_orange_dark;
            }
            else if (statusRaw.equalsIgnoreCase(MyConstant.PICKINGUP)) {
                statusVN = "Đang lấy hàng";
                colorRes = android.R.color.holo_purple; // Hoặc màu tím
            }
            else if (statusRaw.equalsIgnoreCase(MyConstant.DELIVERING)) {
                statusVN = "Đang giao hàng";
                colorRes = android.R.color.holo_blue_dark;
            }
            else if (statusRaw.equalsIgnoreCase(MyConstant.FINISH)) {
                statusVN = "Hoàn thành";
                colorRes = android.R.color.holo_green_dark;
            }
            // --- THÊM ĐOẠN NÀY ---
            else if (statusRaw.equalsIgnoreCase(MyConstant.REJECT)) {
                statusVN = "Đã hủy";
                colorRes = android.R.color.holo_red_dark; // Màu đỏ báo hiệu đơn hủy
            }
        }

        holder.tvStatus.setText(statusVN);
        holder.tvStatus.setTextColor(context.getResources().getColor(colorRes));

        holder.itemView.setOnClickListener(v -> listener.onOrderClick(order));
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvDate, tvStatus, tvTotal, tvAddress;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            // Đảm bảo các ID này khớp với item_order_management.xml
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvDate = itemView.findViewById(R.id.tv_order_date);
            tvStatus = itemView.findViewById(R.id.tv_order_status);
            tvTotal = itemView.findViewById(R.id.tv_order_total);
            tvAddress = itemView.findViewById(R.id.tv_order_address);
        }
    }
}
