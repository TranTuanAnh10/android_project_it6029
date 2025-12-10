package vn.haui.android_project.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.model.OrderShiperHistory;

public class ShipperOrderAdapter extends RecyclerView.Adapter<ShipperOrderAdapter.OrderViewHolder> {

    private List<OrderShiperHistory> orderList;
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(OrderShiperHistory order);
    }
    private final OnItemClickListener listener;


    public ShipperOrderAdapter(Context context, List<OrderShiperHistory> orderList, OnItemClickListener listener) {
        this.context = context;
        this.orderList = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_shipper_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {

        OrderShiperHistory currentOrder = orderList.get(position);

        holder.receiverName.setText(currentOrder.getReceiverName());
        holder.receiverPhone.setText(currentOrder.getReceiverPhone());
        holder.address.setText(currentOrder.getReceiverAddress());
        holder.status.setText(getTextStatus(currentOrder.getStatus()));
        holder.date.setText(currentOrder.getDate());
        if (currentOrder.getStatus().equals("done")) {
            holder.status.setTextColor(context.getResources().getColor(R.color.color_text_home_page_green));
        }
        else if (currentOrder.getStatus().equals("shipping")){
            holder.status.setTextColor(context.getResources().getColor(R.color.color_text_shipping));
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentOrder);
            }
        });
    }

    public String getTextStatus(String text){
        switch (text){
            case "shipping":
                return "Đang giao";
            case "done":
                return "Đã xong";
            default:
                return "Đã xong";
        }
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView receiverName, receiverPhone, address, status, date;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            receiverName = itemView.findViewById(R.id.text_receiver_name);
            receiverPhone = itemView.findViewById(R.id.tv_receiver_phone);
            address = itemView.findViewById(R.id.text_address);
            status = itemView.findViewById(R.id.text_status);
            date = itemView.findViewById(R.id.text_date);
        }
    }
    public void filterList(List<OrderShiperHistory> filteredList) {
        this.orderList.clear();
        this.orderList.addAll(filteredList);
        notifyDataSetChanged();
    }
}