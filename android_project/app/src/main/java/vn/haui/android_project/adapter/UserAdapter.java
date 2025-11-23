package vn.haui.android_project.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.UserEntity;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> implements Filterable {

    private Context mContext;
    private List<UserEntity> mListUser;
    private List<UserEntity> mListUserOld;
    private IClickItemUserListener iClickItemUserListener;

    public interface IClickItemUserListener {
        void onClickItemUser(UserEntity user);
    }

    public UserAdapter(Context mContext, List<UserEntity> mListUser, IClickItemUserListener listener) {
        this.mContext = mContext;
        this.mListUser = mListUser;
        this.mListUserOld = mListUser;
        this.iClickItemUserListener = listener;
    }

    public void updateData(List<UserEntity> newUserList) {
        this.mListUser = new ArrayList<>(newUserList);
        this.mListUserOld = new ArrayList<>(newUserList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserEntity user = mListUser.get(position);
        if (user == null) return;

        // 1. Tên
        if (user.getName() != null && !user.getName().isEmpty()) {
            holder.tvName.setText(user.getName());
        } else if (user.getName() != null) {
            holder.tvName.setText(user.getName());
        } else {
            holder.tvName.setText("Chưa có tên");
        }

        // 2. Số điện thoại
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
            holder.tvPhone.setText(user.getPhoneNumber());
        } else {
            holder.tvPhone.setText("Chưa có SĐT");
        }

        // 3. Email
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            holder.tvEmail.setText(user.getEmail());
            holder.tvEmail.setVisibility(View.VISIBLE);
        } else {
            holder.tvEmail.setVisibility(View.GONE);
        }

        // 4. Địa chỉ (Giữ ẩn như code cũ)
        holder.layoutAddress.setVisibility(View.GONE);

        // 5. XỬ LÝ ROLE (Admin, Employee, Shipper, User/Customer)
        // ---------------------------------------------------------
        String role = user.getRole(); // Lấy chuỗi trực tiếp từ DB
        if (role == null) role = "";  // Tránh null

        // Hiển thị role viết hoa (VD: EMPLOYEE)
        holder.tvRole.setText(role.toUpperCase());

        // Logic đổi màu nền dựa trên các role bạn cung cấp
        String colorHex = "#9E9E9E"; // Màu mặc định (Xám)
        String roleLower = role.toLowerCase(); // Chuyển thường để so sánh chính xác

        if (roleLower.contains("admin")) {
            colorHex = "#D32F2F"; // ĐỎ (Admin)
        }
        else if (roleLower.contains("employee")) {
            colorHex = "#FF9800"; // CAM (Employee/Nhân viên)
        }
        else if (roleLower.contains("shipper")) {
            colorHex = "#1976D2"; // XANH DƯƠNG ĐẬM (Shipper)
        }
        else if (roleLower.contains("user") || roleLower.contains("customer")) {
            colorHex = "#4CAF50"; // XANH LÁ (Khách hàng)
        }

        // Tô màu nền cho badge Role
        Drawable background = holder.tvRole.getBackground();
        if (background != null) {
            background.setColorFilter(Color.parseColor(colorHex), PorterDuff.Mode.SRC_IN);
        }
        // ---------------------------------------------------------

        // Sự kiện click
        holder.itemView.setOnClickListener(v -> iClickItemUserListener.onClickItemUser(user));
    }

    @Override
    public int getItemCount() {
        if (mListUser != null) return mListUser.size();
        return 0;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String strSearch = constraint.toString();List<UserEntity> listResult = new ArrayList<>();

                if (strSearch.isEmpty()) {
                    listResult = mListUserOld;
                } else {
                    String keyword = strSearch.toLowerCase().trim(); // Chuẩn hóa từ khóa

                    for (UserEntity user : mListUserOld) {
                        // Kiểm tra null cho các trường dữ liệu
                        String name = (user.getName() != null) ? user.getName().toLowerCase() : "";
                        String phone = (user.getPhoneNumber() != null) ? user.getPhoneNumber() : "";
                        String email = (user.getEmail() != null) ? user.getEmail().toLowerCase() : ""; // THÊM DÒNG NÀY

                        // Logic tìm kiếm: Chứa từ khóa trong Tên HOẶC SĐT HOẶC Email
                        if (name.contains(keyword) || phone.contains(keyword) || email.contains(keyword)) {
                            listResult.add(user);
                        }
                    }
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = listResult;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mListUser = (List<UserEntity>) results.values;
                notifyDataSetChanged();
            }
        };
    }


    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvEmail, tvAddress, tvRole;
        LinearLayout layoutAddress;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_user_name);
            tvPhone = itemView.findViewById(R.id.tv_user_phone);
            tvEmail = itemView.findViewById(R.id.tv_user_email);
            tvAddress = itemView.findViewById(R.id.tv_user_address);
            tvRole = itemView.findViewById(R.id.tv_user_role);
            layoutAddress = itemView.findViewById(R.id.layout_address);
        }
    }
}
