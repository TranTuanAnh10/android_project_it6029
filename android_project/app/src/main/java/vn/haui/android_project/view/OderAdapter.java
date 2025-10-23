package vn.haui.android_project.view;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import vn.haui.android_project.R;

public class OderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {


    private List<Order> orderList;

    public OrderAdapter(List<Order> orderList) {
        this.orderList = orderList;
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
