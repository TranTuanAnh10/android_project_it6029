package vn.haui.android_project.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;



import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.Store;

public class BasketStoreAdapter extends RecyclerView.Adapter<BasketStoreAdapter.StoreViewHolder> {

    private Context context;
    private List<Store> list;

    public BasketStoreAdapter(Context context, List<Store> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public StoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.fragment_basket_store, parent, false);
        return new StoreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoreViewHolder holder, int position) {
        Store store = list.get(position);
        holder.tvStoreName.setText(store.getStoreName());
        holder.imgStore.setImageResource(store.getImageRes());

        // Gán trạng thái mở/đóng
        if (store.isExpanded()) {
            holder.rvItems.setVisibility(View.VISIBLE);
            holder.btnPlaceOrder.setVisibility(View.VISIBLE);
            holder.btnDeleteStore.setVisibility(View.GONE);
            holder.btnExpand.setImageResource(R.drawable.ic_arrow_down);
        } else {
            holder.rvItems.setVisibility(View.GONE);
            holder.btnPlaceOrder.setVisibility(View.GONE);
            holder.btnDeleteStore.setVisibility(View.VISIBLE);
            holder.btnExpand.setImageResource(R.drawable.ic_switch);
        }

        // setup recyclerView con
        holder.rvItems.setLayoutManager(new LinearLayoutManager(context));
        FoodAdapter adapter = new FoodAdapter(context, store.getItems());
        holder.rvItems.setAdapter(adapter);

        // Sự kiện click nút expand
        holder.btnExpand.setOnClickListener(v -> {
            store.setExpanded(!store.isExpanded());
            notifyItemChanged(position);
        });

        // Nút xóa cửa hàng
        holder.btnDeleteStore.setOnClickListener(v -> {
            list.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, list.size());
        });
        holder.btnPlaceOrder.setOnClickListener(v ->
                // chỉ demo toast
                android.widget.Toast.makeText(context,
                        "Order placed for " + store.getStoreName(),
                        android.widget.Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class StoreViewHolder extends RecyclerView.ViewHolder {
        ImageView imgStore;
        TextView tvStoreName;
        RecyclerView rvItems;
        Button btnPlaceOrder ;

        ImageButton btnExpand, btnDeleteStore;


        public StoreViewHolder(@NonNull View itemView) {
            super(itemView);
            imgStore = itemView.findViewById(R.id.imgStore);
            tvStoreName = itemView.findViewById(R.id.tvStoreName);
            btnExpand = itemView.findViewById(R.id.btnExpand);
            rvItems = itemView.findViewById(R.id.rvItems);
            btnPlaceOrder = itemView.findViewById(R.id.btnPlaceOrder);
            btnDeleteStore = itemView.findViewById(R.id.btnDeleteStore);
        }
    }
}
