package vn.haui.android_project.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.UserEntity;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> implements Filterable {

    private Context context;
    private List<UserEntity> userList;
    private List<UserEntity> userListFiltered; // Danh sách chỉ dùng để lọc
    private OnUserClickListener listener;

    // Interface để gửi sự kiện click về cho Fragment
    public interface OnUserClickListener {
        void onUserClick(UserEntity user);
    }

    public UserAdapter(Context context, List<UserEntity> userList, OnUserClickListener listener) {
        this.context = context;
        this.userList = userList;
        this.userListFiltered = new ArrayList<>(userList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng layout item_user đã được đơn giản hóa (không có ImageView)
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        // Lấy người dùng từ danh sách đã được lọc
        UserEntity user = userListFiltered.get(position);
        holder.bind(user, listener);
    }

    @Override
    public int getItemCount() {
        return userListFiltered.size();
    }

    // Lớp ViewHolder: Giữ các view của một item, đã bỏ đi ImageView
    class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvRole;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ các TextView từ layout
            tvName = itemView.findViewById(R.id.tv_user_name);
            tvEmail = itemView.findViewById(R.id.tv_user_email);
            tvRole = itemView.findViewById(R.id.tv_user_role);
        }

        // Gán dữ liệu vào các view
        public void bind(final UserEntity user, final OnUserClickListener listener) {
            tvName.setText(user.getName());
            tvEmail.setText(user.getEmail());

            // --- PHẦN VAI TRÒ ĐÃ ĐƯỢC ĐƠN GIẢN HÓA ---
            // Chỉ hiển thị văn bản, không set background màu mè
            String role = user.getRole() != null ? user.getRole().toUpperCase() : "CUSTOMER";
            tvRole.setText("Vai trò: " + role);
            tvRole.setBackground(null); // Xóa mọi background đã set trước đó trong XML

            // Thiết lập sự kiện click cho toàn bộ item
            itemView.setOnClickListener(v -> listener.onUserClick(user));
        }
    }

    // --- Phần xử lý bộ lọc (search), không thay đổi ---
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String charString = constraint.toString().toLowerCase(Locale.getDefault()).trim();
                if (charString.isEmpty()) {
                    userListFiltered = new ArrayList<>(userList);
                } else {
                    List<UserEntity> filteredList = new ArrayList<>();
                    for (UserEntity user : userList) {
                        // Tìm kiếm theo tên hoặc email
                        if (user.getName().toLowerCase(Locale.getDefault()).contains(charString) ||
                                user.getEmail().toLowerCase(Locale.getDefault()).contains(charString)) {
                            filteredList.add(user);
                        }
                    }
                    userListFiltered = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = userListFiltered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                userListFiltered = (ArrayList<UserEntity>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    // Hàm để Fragment cập nhật dữ liệu mới cho Adapter
    public void updateData(List<UserEntity> newUserList) {
        this.userList.clear();
        this.userList.addAll(newUserList);
        this.userListFiltered = new ArrayList<>(newUserList);
        notifyDataSetChanged();
    }
}
