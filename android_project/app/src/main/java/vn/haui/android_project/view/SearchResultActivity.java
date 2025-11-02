package vn.haui.android_project.view;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import vn.haui.android_project.R;

public class SearchResultActivity extends AppCompatActivity {
    private EditText etSearchQuery;
    private LinearLayout mainLayout;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search_result);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });

        // Ánh xạ các view
        etSearchQuery = findViewById(R.id.edtSearch);
        mainLayout = findViewById(R.id.main);
        btnBack = findViewById(R.id.btnBack); // <====== THÊM DÒNG NÀY ĐỂ SỬA LỖI

        // Xóa focus khỏi EditText khi người dùng chạm vào vùng khác (ĐÃ ĐÚNG)
        mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etSearchQuery.clearFocus();
            }
        });

        // Xử lý sự kiện click cho nút back
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
