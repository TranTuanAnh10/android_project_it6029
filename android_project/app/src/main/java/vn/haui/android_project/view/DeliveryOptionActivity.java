package vn.haui.android_project.view;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import vn.haui.android_project.R;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class DeliveryOptionActivity extends AppCompatActivity {

    CheckBox cbStandard, cbSchedule, cbPickup;
    LinearLayout optionStandard, optionSchedule, optionPickup;
    LinearLayout detailStandard, layoutScheduleOptions, layoutPickupOptions, detailPickup;
    ImageView icArrowSchedule, icArrowPickup;
    TextView tvTime, tvYumPoints;

    // Schedule buttons
    Button btn15min, btn30min, btn45min, btn1hour;
    // Pickup buttons
    Button btn1000, btn1030, btn1100, btn1130;

    String currentOption = "standard"; // lưu trạng thái hiện tại


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_payment); // layout bạn vừa gửi


        tvYumPoints = findViewById(R.id.tvYumPoints);
        tvYumPoints.setText(android.text.Html.fromHtml("You'll get <font color='#E74C3C'>115 Yum Points</font> for this Order!"));

        // Ánh xạ view
        cbStandard = findViewById(R.id.cbStandard);
        cbSchedule = findViewById(R.id.cbSchedule);
        cbPickup = findViewById(R.id.cbPickup);

        optionStandard = findViewById(R.id.optionStandard);
        optionSchedule = findViewById(R.id.optionSchedule);
        optionPickup = findViewById(R.id.optionPickup);

        detailStandard = findViewById(R.id.detailStandard);
        layoutScheduleOptions = findViewById(R.id.layoutDeliveryOptions);
        layoutPickupOptions = findViewById(R.id.layoutPickupOptions);
        detailPickup = findViewById(R.id.detailPickup);

        icArrowSchedule = findViewById(R.id.icArrowSchedule);
        icArrowPickup = findViewById(R.id.icArrowPickup);
        tvTime = findViewById(R.id.tvTime);


        // Schedule buttons
        btn15min = findViewById(R.id.btn15min);
        btn30min = findViewById(R.id.btn30min);
        btn45min = findViewById(R.id.btn45min);
        btn1hour = findViewById(R.id.btn1hour);

        // Pickup buttons
        btn1000 = findViewById(R.id.btn1000);
        btn1030 = findViewById(R.id.btn1030);
        btn1100 = findViewById(R.id.btn1100);
        btn1130 = findViewById(R.id.btn1130);

        // Mặc định: Standard
        showOption("standard");

        // Xử lý chọn từng option
        optionStandard.setOnClickListener(v -> showOption("standard"));
        optionSchedule.setOnClickListener(v -> showOption("schedule"));
        optionPickup.setOnClickListener(v -> showOption("pickup"));

        // Mũi tên toggle hiển thị chi tiết mà không đổi lựa chọn
        icArrowSchedule.setOnClickListener(v -> toggleDetail("schedule"));
        icArrowPickup.setOnClickListener(v -> toggleDetail("pickup"));

        // Nút thời gian Schedule
        btn15min.setOnClickListener(v -> selectScheduleTime(btn15min));
        btn30min.setOnClickListener(v -> selectScheduleTime(btn30min));
        btn45min.setOnClickListener(v -> selectScheduleTime(btn45min));
        btn1hour.setOnClickListener(v -> selectScheduleTime(btn1hour));

        // Nút thời gian Pickup
        btn1000.setOnClickListener(v -> selectPickupTime(btn1000));
        btn1030.setOnClickListener(v -> selectPickupTime(btn1030));
        btn1100.setOnClickListener(v -> selectPickupTime(btn1100));
        btn1130.setOnClickListener(v -> selectPickupTime(btn1130));
    }

    private void showOption(String selected) {
        currentOption = selected;

        cbStandard.setChecked(selected.equals("standard"));
        cbSchedule.setChecked(selected.equals("schedule"));
        cbPickup.setChecked(selected.equals("pickup"));

        // Hiện/ẩn chi tiết
        detailStandard.setVisibility(selected.equals("standard") ? View.VISIBLE : View.GONE);
        layoutScheduleOptions.setVisibility(selected.equals("schedule") ? View.VISIBLE : View.GONE);
        layoutPickupOptions.setVisibility(selected.equals("pickup") ? View.VISIBLE : View.GONE);
        detailPickup.setVisibility(selected.equals("pickup") ? View.VISIBLE : View.GONE);

        // Đổi màu nền
        highlightOption(optionStandard, selected.equals("standard"));
        highlightOption(optionSchedule, selected.equals("schedule"));
        highlightOption(optionPickup, selected.equals("pickup"));

        // Xử lý icon / text ở Standard
        if (selected.equals("standard")) {
            tvTime.setText("10:00");
        } else {

        }

        // Reset mũi tên về hướng ban đầu
        icArrowSchedule.setRotation(0f);
        icArrowPickup.setRotation(0f);
    }

    private void toggleDetail(String option) {
        if (!currentOption.equals(option)) {
            return; // không đổi lựa chọn
        }

        if (option.equals("schedule")) {
            boolean visible = layoutScheduleOptions.getVisibility() == View.VISIBLE;
            layoutScheduleOptions.setVisibility(visible ? View.GONE : View.VISIBLE);
            icArrowSchedule.setRotation(visible ? 0f : 180f);
        } else if (option.equals("pickup")) {
            boolean visible = layoutPickupOptions.getVisibility() == View.VISIBLE;
            layoutPickupOptions.setVisibility(visible ? View.GONE : View.VISIBLE);
            icArrowPickup.setRotation(visible ? 0f : 180f);
        }
    }

    private void highlightOption(LinearLayout layout, boolean isSelected) {
        int color = isSelected
                ? ContextCompat.getColor(this, R.color.bg_red)
                : ContextCompat.getColor(this, android.R.color.white);
        layout.setBackgroundColor(color);
    }

    private void selectScheduleTime(Button selectedBtn) {
        Button[] buttons = {btn15min, btn30min, btn45min, btn1hour};
        for (Button b : buttons) {
            b.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.white));
            b.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        }

        selectedBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.bg_red));
        selectedBtn.setTextColor(ContextCompat.getColor(this, android.R.color.white));
    }

    private void selectPickupTime(Button selectedBtn) {
        Button[] buttons = {btn1000, btn1030, btn1100, btn1130};
        for (Button b : buttons) {
            b.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.white));
            b.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        }

        selectedBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.bg_red));
        selectedBtn.setTextColor(ContextCompat.getColor(this, android.R.color.white));
    }
}
