package vn.haui.android_project.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.VoucherEntity;

public class VoucherManagementAdapter extends RecyclerView.Adapter<VoucherManagementAdapter.VoucherViewHolder> {

    private Context context;
    private List<VoucherEntity> voucherList;
    private OnVoucherActionClickListener listener;

    // Interface để xử lý sự kiện click Sửa và Xóa
    public interface OnVoucherActionClickListener {
        void onEditClick(VoucherEntity voucher);
        void onDeleteClick(VoucherEntity voucher);
    }

    public VoucherManagementAdapter(Context context, List<VoucherEntity> voucherList, OnVoucherActionClickListener listener) {
        this.context = context;
        this.voucherList = voucherList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng layout item_voucher.xml mà bạn đã tạo trước đó
        // Hoặc tạo một layout mới nếu muốn custom riêng cho admin (ví dụ: item_voucher_admin.xml)
        // Ở đây mình giả định dùng chung item_voucher.xml nhưng thêm xử lý nút bấm
        View view = LayoutInflater.from(context).inflate(R.layout.item_voucher, parent, false);
        return new VoucherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        VoucherEntity voucher = voucherList.get(position);

        if (voucher == null) return;

        // 1. Hiển thị thông tin cơ bản
        holder.tvCode.setText(voucher.getCode());
        holder.tvName.setText(voucher.getName());
        holder.tvDescription.setText(voucher.getDescription());

        // 2. Hiển thị giá trị giảm giá (Format tiền tệ hoặc %)
        if ("PERCENT".equals(voucher.getDiscountType())) {
            // Nếu là số nguyên (vd: 20.0) thì hiển thị 20%, nếu lẻ hiển thị 20.5%
            if (voucher.getDiscountValue() % 1 == 0) {
                holder.tvDiscountInfo.setText(String.format("Giảm: %.0f%%", voucher.getDiscountValue()));
            } else {
                holder.tvDiscountInfo.setText(String.format("Giảm: %.1f%%", voucher.getDiscountValue()));
            }
        } else {
            // Format tiền Việt Nam: 50,000 đ
            holder.tvDiscountInfo.setText(String.format("Giảm: %,.0f đ", voucher.getDiscountValue()));
        }

        // 3. Hiển thị ngày hết hạn
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        holder.tvExpiry.setText("HSD: " + sdf.format(voucher.getExpiryDate()));

        // 4. Kiểm tra trạng thái và đổi màu
        if (voucher.isValid()) {
            holder.tvStatus.setText("Đang hoạt động");
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // Màu xanh lá
        } else {
            if (!voucher.isActive()) {
                holder.tvStatus.setText("Đã bị khóa");
            } else {
                holder.tvStatus.setText("Đã hết hạn");
            }
            holder.tvStatus.setTextColor(Color.parseColor("#F44336")); // Màu đỏ
        }

        // 5. Load ảnh voucher
        Glide.with(context)
                .load(voucher.getImageUrl())
                .placeholder(R.drawable.ic_launcher_background) // Ảnh chờ
                .error(R.drawable.ic_launcher_background)       // Ảnh lỗi
                .into(holder.imgPreview);

        // 6. Sự kiện Click vào toàn bộ item -> Sửa
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(voucher);
        });

        // Nếu bạn muốn xử lý xóa khi nhấn giữ (Long Click)
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(voucher);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        if (voucherList != null) {
            return voucherList.size();
        }
        return 0;
    }

    public static class VoucherViewHolder extends RecyclerView.ViewHolder {

        ImageView imgPreview;
        TextView tvCode, tvName, tvDescription, tvDiscountInfo, tvExpiry, tvStatus;
        // Nếu layout của bạn có nút xóa riêng, hãy khai báo thêm ở đây (ví dụ ImageButton btnDelete)

        public VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPreview = itemView.findViewById(R.id.img_voucher_preview);
            tvCode = itemView.findViewById(R.id.tv_voucher_code);
            tvName = itemView.findViewById(R.id.tv_voucher_name);
            tvDescription = itemView.findViewById(R.id.tv_voucher_description); // Đảm bảo ID này có trong XML
            tvDiscountInfo = itemView.findViewById(R.id.tv_discount_info);
            tvExpiry = itemView.findViewById(R.id.tv_expiry_date);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }
}
