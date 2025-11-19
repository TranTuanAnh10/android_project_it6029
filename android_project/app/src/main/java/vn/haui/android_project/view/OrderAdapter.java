package vn.haui.android_project.view;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.List;

import vn.haui.android_project.MainActivity;
import vn.haui.android_project.R;
import vn.haui.android_project.entity.Order;

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
        String sp = (order.getProductList().size()-1 > 0) ? " (+" + String.valueOf(order.getProductList().size()-1)+" khác)": "" ;
        holder.tvStoreName.setText(order.getProductList().get(0).getName()+sp);
        holder.tvItems.setText(order.getProductList().toString());
        holder.tvEstimate.setText(order.getTimeDisplay());
        holder.tvStatus.setText(order.getStatus());
        holder.tvPrice.setText(String.valueOf(order.getTotal()));
        holder.imgStore.setImageResource(R.drawable.banh_mi_op_la);
        holder.itemView.setOnClickListener(v -> {
            // Xử lý khi item được nhấn
            Intent intentOrderTrackAc = new Intent(holder.itemView.getContext(), OrderTrackingActivity.class);
            intentOrderTrackAc.putExtra("ORDER_ID", order.getOrderId());

            holder.itemView.getContext().startActivity(intentOrderTrackAc);
            //", "Hello from Activity A!");
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

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
}
