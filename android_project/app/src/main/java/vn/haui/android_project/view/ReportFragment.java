package vn.haui.android_project.view;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import vn.haui.android_project.R;
import vn.haui.android_project.enums.MyConstant;

public class ReportFragment extends Fragment {

    // Views
    private TextView tvStartDate, tvEndDate;
    private LinearLayout layoutStartDate, layoutEndDate;
    private Button btnFilter;

    private TextView tvTotalRevenue;
    private TextView tvCountPending, tvCountShipping, tvCountDelivered, tvCountCancelled;

    // Logic Data
    private Calendar calendarStart, calendarEnd;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        initViews(view);
        setupDatePickers();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        // Mặc định load dữ liệu tháng hiện tại
        loadReportData();

        return view;
    }

    private void initViews(View view) {
        tvStartDate = view.findViewById(R.id.tv_start_date);
        tvEndDate = view.findViewById(R.id.tv_end_date);
        layoutStartDate = view.findViewById(R.id.layout_start_date);
        layoutEndDate = view.findViewById(R.id.layout_end_date);
        btnFilter = view.findViewById(R.id.btn_filter_report);

        tvTotalRevenue = view.findViewById(R.id.tv_total_revenue);
        tvCountPending = view.findViewById(R.id.tv_count_pending);
        tvCountShipping = view.findViewById(R.id.tv_count_shipping);
        tvCountDelivered = view.findViewById(R.id.tv_count_delivered);
        tvCountCancelled = view.findViewById(R.id.tv_count_cancelled);

        // Khởi tạo ngày mặc định: Start là ngày 1, End là ngày hiện tại
        calendarStart = Calendar.getInstance();
        calendarStart.set(Calendar.DAY_OF_MONTH, 1); // Ngày đầu tháng

        calendarEnd = Calendar.getInstance(); // Ngày hôm nay

        updateDateTexts();
    }

    private void updateDateTexts() {
        tvStartDate.setText(dateFormat.format(calendarStart.getTime()));
        tvEndDate.setText(dateFormat.format(calendarEnd.getTime()));
    }

    private void setupDatePickers() {
        // Chọn ngày bắt đầu
        layoutStartDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
                calendarStart.set(year, month, dayOfMonth);
                // Reset giờ về 00:00:00 để so sánh chính xác
                calendarStart.set(Calendar.HOUR_OF_DAY, 0);
                calendarStart.set(Calendar.MINUTE, 0);
                calendarStart.set(Calendar.SECOND, 0);
                updateDateTexts();
            }, calendarStart.get(Calendar.YEAR), calendarStart.get(Calendar.MONTH), calendarStart.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        // Chọn ngày kết thúc
        layoutEndDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
                calendarEnd.set(year, month, dayOfMonth);
                // Set giờ về 23:59:59 để bao gồm hết ngày cuối
                calendarEnd.set(Calendar.HOUR_OF_DAY, 23);
                calendarEnd.set(Calendar.MINUTE, 59);
                calendarEnd.set(Calendar.SECOND, 59);
                updateDateTexts();
            }, calendarEnd.get(Calendar.YEAR), calendarEnd.get(Calendar.MONTH), calendarEnd.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        // Nút lọc
        btnFilter.setOnClickListener(v -> loadReportData());
    }

    private void loadReportData() {
        // Reset UI
        tvTotalRevenue.setText("Đang tính...");

        long startDateMillis = calendarStart.getTimeInMillis();
        long endDateMillis = calendarEnd.getTimeInMillis();

        if (startDateMillis > endDateMillis) {
            Toast.makeText(getContext(), "Ngày bắt đầu không được lớn hơn ngày kết thúc", Toast.LENGTH_SHORT).show();
            return;
        }

        // Truy cập vào node "orders" trên Realtime Database
        mDatabase.child("orders").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double totalRevenue = 0;
                int countPending = 0;
                int countShipping = 0;
                int countDelivered = 0;
                int countCancelled = 0;

                // Định dạng ngày khớp với: "20/11/2025 10:06:44 PM"
                // Dùng Locale.US để hiểu được chữ "PM" hoặc "AM"
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.US);

                for (DataSnapshot data : snapshot.getChildren()) {
                    try {
                        // 1. LẤY NGÀY TẠO (created_at)
                        String dateStr = data.child("created_at").getValue(String.class);
                        Date orderDate = null;

                        if (dateStr != null && !dateStr.isEmpty()) {
                            try {
                                orderDate = sdf.parse(dateStr);
                            } catch (Exception e) {
                                // Fallback: Nếu lỡ có đơn nào định dạng khác hoặc thiếu PM/AM
                                Log.e("ReportDate", "Lỗi parse ngày: " + dateStr);
                            }
                        }

                        if (orderDate == null) continue; // Bỏ qua nếu không có ngày

                        // 2. SO SÁNH KHOẢNG THỜI GIAN
                        if (orderDate.getTime() >= startDateMillis && orderDate.getTime() <= endDateMillis) {

                            // 3. LẤY TỔNG TIỀN (total - kiểu Double trong Order.java)
                            Double total = data.child("total").getValue(Double.class);
                            if (total == null) total = 0.0;

                            // 4. LẤY TRẠNG THÁI (status)
                            String status = data.child("status").getValue(String.class);

                            if (status != null) {
                                status = status.trim(); // Xóa khoảng trắng thừa

                                // -- LOGIC PHÂN LOẠI TRẠNG THÁI (Dùng MyConstant) --
                                if (status.equals(MyConstant.PREPARED)) {
                                    countPending++;
                                } else if (status.equals(MyConstant.PICKINGUP) || status.equals(MyConstant.DELIVERING)) {
                                    countShipping++;
                                } else if (status.equals(MyConstant.FINISH)) {
                                    countDelivered++;
                                    // CHỈ CỘNG DOANH THU KHI ĐƠN ĐÃ HOÀN THÀNH
                                    totalRevenue += total;
                                } else if (status.equals(MyConstant.CANCEL_ORDER) || status.equals(MyConstant.REJECT)) {
                                    countCancelled++;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("ReportFragment", "Lỗi xử lý đơn ID: " + data.getKey() + " - " + e.getMessage());
                    }
                }

                // Cập nhật giao diện
                updateUI(totalRevenue, countPending, countShipping, countDelivered, countCancelled);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ReportFragment", "Lỗi tải DB: " + error.getMessage());
                tvTotalRevenue.setText("Lỗi tải");
            }
        });
    }


    private void updateUI(double revenue, int pending, int shipping, int delivered, int cancelled) {
        // Format tiền tệ
        String formattedRevenue = NumberFormat.getInstance(Locale.US).format(revenue);
        tvTotalRevenue.setText(formattedRevenue + " đ");

        tvCountPending.setText(String.valueOf(pending));
        tvCountShipping.setText(String.valueOf(shipping));
        tvCountDelivered.setText(String.valueOf(delivered));
        tvCountCancelled.setText(String.valueOf(cancelled));
    }
}
