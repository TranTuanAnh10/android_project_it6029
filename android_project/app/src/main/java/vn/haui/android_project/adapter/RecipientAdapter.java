package vn.haui.android_project.adapter;

import android.content.Context; // Cần context nếu bạn muốn tạo Intent
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast; // Dùng tạm để test click

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.UserLocationEntity;
import vn.haui.android_project.view.EditRecipientActivity;

public class RecipientAdapter extends RecyclerView.Adapter<RecipientAdapter.RecipientViewHolder> {

    private final List<UserLocationEntity> locationList;
    private final Context context; // Thêm Context để có thể tạo Intent/Toast

    public RecipientAdapter(List<UserLocationEntity> locationList, Context context) {
        this.locationList = locationList;
        this.context = context;
    }

    // --- 1. onCreateViewHolder ---
    @NonNull
    @Override
    public RecipientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Ánh xạ layout item_recipient_address.xml
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipient_address, parent, false);
        // Chuyển Context và List cho ViewHolder để xử lý click
        return new RecipientViewHolder(view, locationList, context);
    }

    // --- 2. onBindViewHolder (Phần quan trọng để gán dữ liệu và style) ---
    @Override
    public void onBindViewHolder(@NonNull RecipientViewHolder holder, int position) {
        UserLocationEntity location = locationList.get(position);

        // Gán dữ liệu vào TextViews
        holder.tvLocationType.setText(location.getLocationType());
        holder.tvFullAddress.setText(location.getAddress());
        // Hiển thị Tên người nhận và SĐT (Giả sử getRecipientName() có sẵn)
        String recipientInfo = location.getPhoneNumber();
        holder.tvRecipientInfo.setText(recipientInfo);

        // --- LOGIC GIAO DIỆN MẶC ĐỊNH (HIGHLIGHT) ---
        if (location.isDefaultLocation()) {
            holder.cardContainer.setBackgroundResource(R.drawable.bg_selector_address_card);
            // Ẩn icon chỉnh sửa cho địa chỉ mặc định/vị trí hiện tại
            holder.imgEditIcon.setVisibility(View.GONE);
        } else {
            holder.cardContainer.setBackgroundResource(R.drawable.bg_normal_address_card);
            // Hiện icon chỉnh sửa cho các địa chỉ khác
            holder.imgEditIcon.setVisibility(View.VISIBLE);
        }
    }

    // --- 3. getItemCount ---
    @Override
    public int getItemCount() {
        return locationList.size();
    }

    // --- 4. ViewHolder (Đã sửa lỗi và thêm logic) ---
    public static class RecipientViewHolder extends RecyclerView.ViewHolder {

        public final TextView tvLocationType;
        public final TextView tvFullAddress;
        public final LinearLayout cardContainer;
        public final TextView tvRecipientInfo;
        public final ImageView imgEditIcon;

        public RecipientViewHolder(View itemView, List<UserLocationEntity> locationList, Context context) {
            super(itemView);

            // Ánh xạ các Views
            tvLocationType = itemView.findViewById(R.id.tv_location_type);
            tvFullAddress = itemView.findViewById(R.id.tv_full_address);
            cardContainer = itemView.findViewById(R.id.card_container);
            tvRecipientInfo = itemView.findViewById(R.id.tv_recipient_info);
            imgEditIcon = itemView.findViewById(R.id.img_edit_icon);
            imgEditIcon.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    UserLocationEntity location = locationList.get(position);
                    Intent intent = new Intent(context, EditRecipientActivity.class);
                    intent.putExtra("location_id", location.getId());
                    intent.putExtra("address", location.getAddress());
                    intent.putExtra("phoneNumber", location.getPhoneNumber());
                    intent.putExtra("defaultLocation", location.getDefault());
                    context.startActivity(intent);
                }
            });

            // Xử lý sự kiện click chọn toàn bộ item (Chọn địa chỉ)
            cardContainer.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    // TODO: Gửi kết quả (địa chỉ được chọn) về Activity gọi
                    Toast.makeText(context, "Đã chọn: " + locationList.get(position).getLocationType(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}