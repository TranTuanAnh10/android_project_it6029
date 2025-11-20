package vn.haui.android_project.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.adapter.UserAdapter;
import vn.haui.android_project.entity.UserEntity;
import vn.haui.android_project.view.UserEditActivity; // Activity để chỉnh sửa user

public class UserManagementFragment extends Fragment {

    private RecyclerView recyclerViewUsers;
    private UserAdapter userAdapter;
    private List<UserEntity> userList;
    private FirebaseFirestore db;
    private SearchView searchView;

    // Xóa bỏ các biến và phương thức không cần thiết như newInstance, mParam1, mParam2...

    public UserManagementFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_user_management, container, false);

        // Ánh xạ các view từ layout
        recyclerViewUsers = view.findViewById(R.id.recycler_view_users);
        searchView = view.findViewById(R.id.search_view_users);
        db = FirebaseFirestore.getInstance();

        // Gọi các hàm để thiết lập
        setupRecyclerView();
        loadUsers();
        setupSearchView();

        return view;
    }

    /**
     * Thiết lập RecyclerView, Adapter và xử lý sự kiện click
     */
    private void setupRecyclerView() {
        userList = new ArrayList<>();
        // Khởi tạo Adapter với danh sách rỗng và định nghĩa sự kiện click
        userAdapter = new UserAdapter(getContext(), userList, user -> {
            // Khi một user được click, mở màn hình UserEditActivity
            Intent intent = new Intent(getActivity(), UserEditActivity.class);
            // Truyền ID của user qua Intent để màn hình sau biết cần sửa user nào
            intent.putExtra("USER_ID", user.getUid());
            startActivity(intent);
        });

        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewUsers.setAdapter(userAdapter);
    }

    /**
     * Tải toàn bộ danh sách người dùng từ Firestore
     */
    private void loadUsers() {
        // Sắp xếp theo tên để danh sách ổn định hơn
        db.collection("users").orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<UserEntity> newUserList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            UserEntity user = document.toObject(UserEntity.class);
                            // Rất quan trọng: Gán ID của document vào đối tượng UserEntity
                            // để sau này có thể truy vấn lại đúng document đó
                            user.setUid(document.getId());
                            newUserList.add(user);
                        }
                        // Cập nhật dữ liệu cho Adapter
                        userAdapter.updateData(newUserList);
                    } else {
                        // Xử lý lỗi khi không tải được dữ liệu
                        Toast.makeText(getContext(), "Lỗi tải danh sách người dùng.", Toast.LENGTH_SHORT).show();
                        Log.e("UserManagement", "Error loading users: ", task.getException());
                    }
                });
    }

    /**
     * Thiết lập chức năng tìm kiếm cho SearchView
     */
    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            // Được gọi khi người dùng bấm nút search trên bàn phím
            @Override
            public boolean onQueryTextSubmit(String query) {
                userAdapter.getFilter().filter(query);
                return false;
            }

            // Được gọi mỗi khi văn bản trong SearchView thay đổi
            @Override
            public boolean onQueryTextChange(String newText) {
                // Lọc danh sách ngay lập tức khi người dùng gõ
                userAdapter.getFilter().filter(newText);
                return false;
            }
        });
    }

    // (Optional) Thêm hàm onResume để cập nhật lại danh sách khi quay lại từ màn hình chỉnh sửa
    @Override
    public void onResume() {
        super.onResume();
        // Tải lại danh sách để cập nhật các thay đổi (nếu có) từ màn hình chỉnh sửa
        loadUsers();
    }
}
