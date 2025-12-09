package vn.haui.android_project.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;


import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.VoucherEntity;

/**
 * Adapter cho RecyclerView để hiển thị danh sách Voucher.
 * SỬ DỤNG layout item_voucher_row.xml ĐƯỢC CUNG CẤP.
 */
public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder> {

    private final Context context;
    private final List<VoucherEntity> voucherList;
    private final VoucherClickListener voucherClickListener;

    // Interface để xử lý sự kiện click từ Activity/Fragment
    public interface VoucherClickListener {
        void onVoucherClicked(VoucherEntity voucher);
    }

    public VoucherAdapter(Context context, List<VoucherEntity> voucherList, VoucherClickListener listener) {
        this.context = context;
        this.voucherList = voucherList;
        this.voucherClickListener = listener;
    }

    // 1. ViewHolder: Giữ các tham chiếu đến các view trong item_voucher_row.
    public class VoucherViewHolder extends RecyclerView.ViewHolder {
        // Ánh xạ các View dựa trên file item_voucher_row.xml
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvDescription;
        TextView tvApply; // TextView đóng vai trò là nút "Áp dụng"

        public VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ ID thực tế từ layout item_voucher_row.xml
            ivIcon = itemView.findViewById(R.id.iv_payment_icon);
            tvTitle = itemView.findViewById(R.id.tv_voucher_title);
            tvDescription = itemView.findViewById(R.id.tv_voucher_description);
            tvApply = itemView.findViewById(R.id.tv_use_voucher);

            // Thiết lập sự kiện click cho TextView "Áp dụng/Sử dụng"
            tvApply.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    VoucherEntity voucher = voucherList.get(position);
                    // Chỉ gọi callback nếu voucher có thể áp dụng được
                    if (voucher.isActive()) {
                        voucherClickListener.onVoucherClicked(voucher);
                    }
                }
            });
        }
    }

    // 2. onCreateViewHolder: Tạo ViewHolder bằng cách inflate layout item_voucher_row.
    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_voucher_row, parent, false);
        return new VoucherViewHolder(view);
    }

    // 3. onBindViewHolder: Đổ dữ liệu từ VoucherEntity vào các View của ViewHolder.
    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        VoucherEntity voucher = voucherList.get(position);

        // Đổ dữ liệu
        holder.tvTitle.setText(voucher.getName());
        holder.tvDescription.setText(voucher.getDescription());

        // Cập nhật trạng thái và giao diện dựa trên isApplicable
        if (voucher.isActive()) {
            // Trạng thái Áp dụng được
            holder.tvTitle.setTextColor(Color.BLACK);
            holder.tvDescription.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));

            // Text áp dụng
            // Đảm bảo R.string.use_voucher tồn tại trong strings.xml của bạn
            holder.tvApply.setText(R.string.use_voucher);
            holder.tvApply.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_light));
            holder.tvApply.setAlpha(1.0f); // Đảm bảo không bị mờ

            // Icon (Đặt màu đỏ/chính)
            // Lọc màu cho icon để trùng với màu chủ đạo
            loadPreviewImage(voucher.getImageUrl(), holder.ivIcon);
        } else {
            // Trạng thái KHÔNG Áp dụng được (Mờ đi)
            int greyColor = Color.parseColor("#C0C0C0"); // Light Gray
            holder.tvTitle.setTextColor(greyColor);
            holder.tvDescription.setTextColor(greyColor);

            // Text thông báo không áp dụng được
            // Cần tạo string R.string.not_applicable: "Không áp dụng"
            holder.tvApply.setText(R.string.not_applicable);
            holder.tvApply.setTextColor(greyColor);
            holder.tvApply.setAlpha(0.5f); // Làm mờ text/nút

            loadPreviewImage(voucher.getImageUrl(), holder.ivIcon);
        }
    }

    // 4. getItemCount: Trả về tổng số lượng item.
    @Override
    public int getItemCount() {
        return voucherList.size();
    }

    /**
     * Hàm helper để cập nhật danh sách voucher và làm mới RecyclerView.
     */
    public void updateVouchers(List<VoucherEntity> newVouchers) {
        voucherList.clear();
        voucherList.addAll(newVouchers);
        notifyDataSetChanged();
    }


    private static void loadPreviewImage(String url, ImageView imageView) {
        if (url == null || url.isEmpty()) return;

        GlideUrl glideUrl = new GlideUrl(url, new LazyHeaders.Builder()
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
                .build());

        Glide.with(imageView.getContext())
                .load(glideUrl)
                .override(600, 600)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_delete)
                .into(imageView);
    }
}