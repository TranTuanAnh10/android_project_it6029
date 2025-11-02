package vn.haui.android_project.view;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.textfield.TextInputEditText;

import vn.haui.android_project.R;

public class HomeFragment extends Fragment {
    // Khai báo biến ở đây để có thể truy cập trong onResume
    private TextInputEditText edtSearch;

    public HomeFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

//        Button btnShop = view.findViewById(R.id.btn_open_shop);
//        btnShop.setOnClickListener(v -> {
//            Intent intent = new Intent(getActivity(), ShopDetailActivity.class);
//            startActivity(intent);
//        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Ánh xạ ô TextInputEditText từ layout
        // Gán vào biến thành viên đã khai báo ở trên
        edtSearch = view.findViewById(R.id.edtSearch);

        // 2. Gắn sự kiện click cho ô tìm kiếm
        edtSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 3. Tạo Intent để mở màn hình tìm kiếm mới
                Intent intent = new Intent(getActivity(), SearchResultActivity.class);

                // 4. Bắt đầu Activity mới
                startActivity(intent);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Luôn kiểm tra xem edtSearch đã được khởi tạo chưa trước khi sử dụng
        if (edtSearch != null) {
            edtSearch.clearFocus();
        }
    }
}