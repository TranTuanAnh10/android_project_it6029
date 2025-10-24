package vn.haui.android_project.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.FoodItem;

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.FoodViewHolder> {

    private Context context;
    private List<FoodItem> list;

    public FoodAdapter(Context context, List<FoodItem> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.fragment_items_basket_food, parent, false);
        return new FoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        FoodItem item = list.get(position);
        holder.tvFoodName.setText(item.getName());
        holder.tvFoodDesc.setText(item.getDesc());
        holder.tvFoodPrice.setText(item.getPrice());
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
        holder.imgFood.setImageResource(item.getImageRes());

        // xử lý tăng giảm số lượng
        holder.btnPlus.setOnClickListener(v -> {
            int qty = item.getQuantity() + 1;
            item.setQuantity(qty);
            holder.tvQuantity.setText(String.valueOf(qty));
        });

        holder.btnMinus.setOnClickListener(v -> {
            int qty = item.getQuantity();
            if (qty > 1) {
                qty--;
                item.setQuantity(qty);
                holder.tvQuantity.setText(String.valueOf(qty));
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class FoodViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFood;
        TextView tvFoodName, tvFoodDesc, tvFoodPrice, tvQuantity, btnMinus, btnPlus;

        public FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFood = itemView.findViewById(R.id.imgFood);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvFoodDesc = itemView.findViewById(R.id.tvFoodDesc);
            tvFoodPrice = itemView.findViewById(R.id.tvFoodPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnPlus = itemView.findViewById(R.id.btnPlus);
        }
    }
}
