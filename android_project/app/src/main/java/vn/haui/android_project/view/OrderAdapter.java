package vn.haui.android_project.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.Order;
import vn.haui.android_project.enums.MyConstant;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Order> orderList;

    public OrderAdapter(List<Order> orderList) {
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        StringBuilder item = new StringBuilder("");
        order.getProductList().stream().limit(2).forEach(itemOrderProduct -> {
            item.append(itemOrderProduct.getName());
            item.append("\n");
        });
        if (order.getProductList().size() > 2)
            item.append("...");
        String sp = (order.getProductList().size() - 1 > 0) ? " (+" + String.valueOf(order.getProductList().size() - 1) + " khác)" : "";
        holder.tvStoreName.setText(order.getProductList().get(0).getName() + sp);
        holder.tvItems.setText(item.toString());
        holder.tvEstimate.setText(order.getCreated_at());

        Context context = holder.itemView.getContext();
        holder.tvStatus.setText(this.mapStatusDesc(order.getStatus()));
        Drawable roundedDrawable = getRoundedBackground(context, order.getStatus());
        holder.tvStatus.setBackground(roundedDrawable);
        holder.tvPrice.setText(formatter.format(order.getTotal()) + "đ");

        String itemName = order.getProductList().get(0).getImage();
        int index = itemName.lastIndexOf('.');
        if (index != -1) {
            itemName = itemName.substring(0, index);
        }
        int drawableId = context.getResources().getIdentifier(
                itemName,
                "drawable",
                context.getPackageName()
        );
        holder.imgStore.setImageResource(drawableId);
        holder.itemView.setOnClickListener(v -> {
            if (MyConstant.DELIVERING.equals(order.getStatus())) {
                Intent intent1 = new Intent(context, OrderTrackingActivity.class);
                intent1.putExtra("ORDER_ID", order.getOrderId());
                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(intent1);
                ;
            } else {
                Intent intent1 = new Intent(context, OrderDetailsActivity.class);
                intent1.putExtra("ORDER_ID", order.getOrderId());
                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(intent1);
                ;
            }
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    DecimalFormat formatter = new DecimalFormat("#,###");

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        ImageView imgStore;
        TextView tvStoreName, tvItems, tvEstimate, tvStatus, tvPrice;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            imgStore = itemView.findViewById(R.id.imgStore);
            tvStoreName = itemView.findViewById(R.id.tvStoreName);
            tvItems = itemView.findViewById(R.id.tvItems);
            tvEstimate = itemView.findViewById(R.id.tvEstimate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPrice = itemView.findViewById(R.id.tvPrice);
        }
    }


    private String mapStatusDesc(String status) {
        switch (status) {
            case MyConstant.PREPARED:
                return "Đã chuẩn bị";
            case MyConstant.PICKINGUP:
                return "Đang lấy hàng";
            case MyConstant.DELIVERING:
                return "Đang giao hàng";
            case MyConstant.FINISH:
                return "Đã giao hàng";
            case MyConstant.REJECT:
                return "Từ chối";
            case MyConstant.CANCEL_ORDER:
                return "Đã hủy";
            default:
                return status;
        }
    }

    private Drawable getRoundedBackground(Context context, String status) {
        int color = getStatusColor(status);
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.bg_status_2).mutate();
        if (drawable instanceof GradientDrawable) {
            GradientDrawable gradientDrawable = (GradientDrawable) drawable;
            gradientDrawable.setColor(color);
            return gradientDrawable;
        }
        return drawable;
    }

    private int getStatusColor(String status) {
        switch (status) {
            case MyConstant.REJECT:
                return Color.parseColor("#F44336"); // Đỏ
            case MyConstant.CANCEL_ORDER:
                return Color.parseColor("#FFDAD6");
            case MyConstant.PREPARED:
                return Color.parseColor("#FAD8FD");
            case MyConstant.PICKINGUP:
                return Color.parseColor("#AAC7FF");
            case MyConstant.DELIVERING:
                return Color.parseColor("#415F91");
            case MyConstant.FINISH:
            default:
                return Color.parseColor("#4CAF50"); // Xanh lá cây
        }
    }


}