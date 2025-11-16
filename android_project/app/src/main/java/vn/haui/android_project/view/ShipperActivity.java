package vn.haui.android_project.view;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import vn.haui.android_project.R;

public class ShipperActivity extends AppCompatActivity {
    private Spinner spinnerStatus;

    private Button btnDatePicker;
    private Calendar myCalendar;
    private AlertDialog currentOrderDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shipper);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initDropdown();
        myCalendar = Calendar.getInstance();

        btnDatePicker = findViewById(R.id.btn_date_picker);

        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                updateLabel();

                // TODO: Lọc RecyclerView theo ngày
            }
        };
        btnDatePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(
                        ShipperActivity.this,
                        dateSetListener,
                        myCalendar.get(Calendar.YEAR),
                        myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)
                ).show();
            }
        });
        updateLabel();
    }

    private BroadcastReceiver newOrderReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Lấy dữ liệu đơn hàng từ Intent
            String orderId = intent.getStringExtra("orderId");
            String pickup = intent.getStringExtra("pickupAddress");
            String delivery = intent.getStringExtra("deliveryAddress");

            // Hiển thị popup
            showConfirmOrderDialog(orderId, pickup, delivery);
        }
    };
    private void showConfirmOrderDialog(String orderId, String pickup, String delivery) {

        if (currentOrderDialog != null && currentOrderDialog.isShowing()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_confirm_order_shipper, null);

        TextView textOrderId = view.findViewById(R.id.text_order_id);
        TextView textPickup = view.findViewById(R.id.text_pickup);
        TextView textDelivery = view.findViewById(R.id.text_delivery);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);

        textOrderId.setText("Mã đơn: #" + orderId);
        textPickup.setText("Lấy hàng: " + pickup);
        textDelivery.setText("Giao hàng: " + delivery);

        builder.setView(view)
                .setTitle("ĐƠN HÀNG MỚI")
                .setCancelable(false);

        currentOrderDialog = builder.create();

        btnCancel.setOnClickListener(v -> {
            // TODO: Xử lý logic khi Shipper "Hủy"
            // (Gửi thông báo từ chối lên server, v.v.)
            Toast.makeText(this, "Đã hủy đơn " + orderId, Toast.LENGTH_SHORT).show();
            currentOrderDialog.dismiss();
        });

        btnConfirm.setOnClickListener(v -> {
            // TODO: Xử lý logic khi Shipper "Xác nhận"
            // (Cập nhật trạng thái đơn hàng, gửi thông báo cho khách, v.v.)
            Toast.makeText(this, "Đã nhận đơn " + orderId, Toast.LENGTH_SHORT).show();
            currentOrderDialog.dismiss();
        });

        currentOrderDialog.show();
    }
    private void updateLabel() {
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());

        // Gán văn bản mới cho Button
        btnDatePicker.setText(sdf.format(myCalendar.getTime()));
    }

    private void initDropdown(){
        spinnerStatus = findViewById(R.id.spinner_status_filter);

        String[] filterOptions = new String[]{"Tất cả", "Đã giao", "Đang giao", "Không thành công"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                filterOptions
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerStatus.setAdapter(adapter);

        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedStatus = filterOptions[position];

                // TODO: Gọi hàm lọc RecyclerView của bạn tại đây
                // Ví dụ: filterOrderList(selectedStatus);
                Toast.makeText(ShipperActivity.this, "Bạn chọn: " + selectedStatus, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

}