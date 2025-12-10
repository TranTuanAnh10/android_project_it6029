package vn.haui.android_project.adapter;

import android.content.Context;
import android.graphics.Color;import android.view.LayoutInflater;
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
import java.util.Objects;

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
    private final double totalBill; // Biến để lưu tổng tiền đơn hàng

    // Hằng số cho loại voucher để tránh lỗi chính tả
    private static final String VOUCHER_TYPE_PERCENTAGE = "PERCENT";

    // Interface để trả về cả số tiền được giảm
    public interface VoucherClickListener {
        void onVoucherClicked(VoucherEntity voucher, double discountAmount);
    }

    // Constructor nhận vào tổng tiền
    public VoucherAdapter(Context context, List<VoucherEntity> voucherList, double totalBill, VoucherClickListener listener) {
        this.context = context;
        this.voucherList = voucherList;
        this.totalBill = totalBill; // Gán tổng tiền
        this.voucherClickListener = listener;
    }

    public class VoucherViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvDescription;
        TextView tvApply;

        public VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_payment_icon);
            tvTitle = itemView.findViewById(R.id.tv_voucher_title);
            tvDescription = itemView.findViewById(R.id.tv_voucher_description);
            tvApply = itemView.findViewById(R.id.tv_use_voucher);

            // Sửa sự kiện click
            tvApply.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    VoucherEntity voucher = voucherList.get(position);
                    boolean isApplicable = isVoucherApplicable(voucher, totalBill);

                    if (isApplicable) {
                        double discountAmount = calculateDiscount(voucher, totalBill);
                        voucherClickListener.onVoucherClicked(voucher, discountAmount);
                    }
                }
            });
        }
    }

    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_voucher_row, parent, false);
        return new VoucherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        VoucherEntity voucher = voucherList.get(position);

        holder.tvTitle.setText(voucher.getName());
        holder.tvDescription.setText(voucher.getDescription());

        // Kiểm tra xem voucher có áp dụng được không
        boolean isApplicable = isVoucherApplicable(voucher, totalBill);

        if (isApplicable) {
            // Trạng thái Áp dụng được
            holder.tvTitle.setTextColor(Color.BLACK);
            holder.tvDescription.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
            holder.tvApply.setText(R.string.use_voucher);
            holder.tvApply.setTextColor(ContextCompat.getColor(context, R.color.color_text_home_page_black));
            holder.tvApply.setAlpha(1.0f);
        } else {
            // Trạng thái KHÔNG Áp dụng được (Mờ đi)
            int greyColor = ContextCompat.getColor(context, R.color.color_text_home_page_text_black);
            holder.tvTitle.setTextColor(greyColor);
            holder.tvDescription.setTextColor(greyColor);
            holder.tvApply.setText(R.string.not_applicable);
            holder.tvApply.setTextColor(greyColor);
            holder.tvApply.setAlpha(0.5f);
        }
        // Luôn load ảnh
        loadPreviewImage(voucher.getImageUrl(), holder.ivIcon);
    }

    @Override
    public int getItemCount() {
        return voucherList != null ? voucherList.size() : 0;
    }

    /**
     * Kiểm tra xem một voucher có thể áp dụng cho đơn hàng không.
     */
    private boolean isVoucherApplicable(VoucherEntity voucher, double bill) {
        if (voucher == null || !voucher.isActive()) {
            return false;
        }
        // Kiểm tra điều kiện giá trị đơn hàng tối thiểu
        return bill >= voucher.getMinOrderValue();
    }

    /**
     * Tính toán số tiền thực tế được giảm.
     */
    private double calculateDiscount(VoucherEntity voucher, double bill) {
        if (!isVoucherApplicable(voucher, bill)) {
            return 0.0;
        }

        double discount = 0.0;
        // ========================================================
        // SỬA LỖI Ở ĐÂY: Dùng đúng tên phương thức và hằng số
        // ========================================================
        if (VOUCHER_TYPE_PERCENTAGE.equalsIgnoreCase(voucher.getDiscountType())) {
            // Giảm theo phần trăm
            discount = bill * (voucher.getDiscountValue() / 100.0);
            // Kiểm tra giới hạn giảm giá tối đa
            if (voucher.getMaxOrderValue() > 0 && discount > voucher.getMaxOrderValue()) {
                discount = voucher.getMaxOrderValue();
            }
        } else {
            // Giảm số tiền cố định
            discount = voucher.getDiscountValue();
        }
        return discount;
    }

    /**
     * Hàm helper để cập nhật danh sách voucher và làm mới RecyclerView.
     */
    public void updateVouchers(List<VoucherEntity> newVouchers) {
        voucherList.clear();
        if (newVouchers != null) {
            voucherList.addAll(newVouchers);
        }
        notifyDataSetChanged();
    }

    private static void loadPreviewImage(String url, ImageView imageView) {
        if (url == null || url.isEmpty()) {
            // Bạn có thể đặt một ảnh mặc định ở đây nếu muốn
            // imageView.setImageResource(R.drawable.ic_default_voucher);
            return;
        }

        GlideUrl glideUrl = new GlideUrl(url, new LazyHeaders.Builder()
                .addHeader("User-Agent", "Mozilla/5.0")
                .build());

        Glide.with(imageView.getContext())
                .load(glideUrl)
                .placeholder(R.drawable.ic_abount_yumyard) // Thay bằng placeholder của bạn
                .error(R.drawable.ic_delete) // Thay bằng icon lỗi của bạn
                .into(imageView);
    }
}
