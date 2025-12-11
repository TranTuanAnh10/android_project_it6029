package vn.haui.android_project.view;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.haui.android_project.R;
import vn.haui.android_project.adapter.ShipperOrderAdapter;
import vn.haui.android_project.model.OrderShiperHistory;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ShipperCalendarFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ShipperCalendarFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private TextView textCurrentMonth;
    private ImageView btnPrevMonth, btnNextMonth;
    private RecyclerView recyclerCalendar, recyclerOrdersToday;
    private LinearLayout layoutEmptyState;

    // Logic
    private Calendar currentCalendar;
    private DatabaseReference mDatabase;
    private String currentUserId;

    // Data
    private List<OrderShiperHistory> allOrders = new ArrayList<>();
    private Map<String, List<OrderShiperHistory>> ordersByDateMap = new HashMap<>();

    private ShipperOrderAdapter detailsAdapter;
    private CalendarAdapter calendarAdapter; // Class Adapter tự viết ở dưới
    private String selectedDateKey = ""; // Ngày đang được chọn

    public ShipperCalendarFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ShipperCalendarFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ShipperCalendarFragment newInstance(String param1, String param2) {
        ShipperCalendarFragment fragment = new ShipperCalendarFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_shipper_calendar, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Ánh xạ View
        textCurrentMonth = view.findViewById(R.id.text_current_month);
        btnPrevMonth = view.findViewById(R.id.btn_prev_month);
        btnNextMonth = view.findViewById(R.id.btn_next_month);
        recyclerCalendar = view.findViewById(R.id.recycler_calendar);
        recyclerOrdersToday = view.findViewById(R.id.recycler_orders_today);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);

        // 2. Khởi tạo
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference("shippers").child(currentUserId);
        currentCalendar = Calendar.getInstance();

        recyclerCalendar.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        calendarAdapter = new CalendarAdapter();
        recyclerCalendar.setAdapter(calendarAdapter);

        recyclerOrdersToday.setLayoutManager(new LinearLayoutManager(requireContext()));
        detailsAdapter = new ShipperOrderAdapter(requireContext(), new ArrayList<>(), order -> {
            // Xử lý click vào đơn hàng chi tiết (giữ nguyên logic cũ của bạn)
        });
        recyclerOrdersToday.setAdapter(detailsAdapter);

        btnPrevMonth.setOnClickListener(v -> changeMonth(-1));
        btnNextMonth.setOnClickListener(v -> changeMonth(1));

        getOrderHistory();
        Date today = new Date();
        SimpleDateFormat sdfKey = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        selectedDateKey = sdfKey.format(today);
        loadOrdersForDate(selectedDateKey);
        updateMonthUI();
    }

    private void changeMonth(int amount) {
        currentCalendar.add(Calendar.MONTH, amount);
        updateMonthUI();
    }

    private void updateMonthUI() {
        SimpleDateFormat sdf = new SimpleDateFormat("'Tháng' MM 'Năm' yyyy", Locale.getDefault());
        textCurrentMonth.setText(sdf.format(currentCalendar.getTime()));

        ArrayList<Date> daysInMonth = new ArrayList<>();
        Calendar calendarIter = (Calendar) currentCalendar.clone();
        calendarIter.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = calendarIter.get(Calendar.DAY_OF_WEEK) - 1;
        calendarIter.add(Calendar.DAY_OF_MONTH, -firstDayOfWeek);

        for (int i = 0; i < 42; i++) {
            daysInMonth.add(calendarIter.getTime());
            calendarIter.add(Calendar.DAY_OF_MONTH, 1);
        }

        calendarAdapter.setData(daysInMonth, ordersByDateMap, currentCalendar.get(Calendar.MONTH));
    }

    private void getOrderHistory() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allOrders.clear();
                ordersByDateMap.clear();

                if (snapshot.hasChild("historys")) {
                    for (DataSnapshot orderSnapshot : snapshot.child("historys").getChildren()) {
                        OrderShiperHistory order = orderSnapshot.getValue(OrderShiperHistory.class);
                        if (order != null) {
                            allOrders.add(order);

                            // *** QUAN TRỌNG: Nhóm đơn hàng theo ngày ***
                            // Giả sử order.getDate() trả về chuỗi "dd/MM/yyyy".
                            // Nếu order lưu timestamp dạng long, bạn cần convert sang String trước.
                            String dateKey = order.getDate(); // Ví dụ: "10/12/2025"

                            if (!ordersByDateMap.containsKey(dateKey)) {
                                ordersByDateMap.put(dateKey, new ArrayList<>());
                            }
                            ordersByDateMap.get(dateKey).add(order);
                        }
                    }
                }
                // Cập nhật lại giao diện lịch sau khi có dữ liệu
                updateMonthUI();

                // Nếu đang chọn ngày nào thì load lại list của ngày đó
                if (!selectedDateKey.isEmpty()) {
                    loadOrdersForDate(selectedDateKey);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadOrdersForDate(String dateKey) {
        selectedDateKey = dateKey;
        List<OrderShiperHistory> orders = ordersByDateMap.get(dateKey);

        if (orders == null || orders.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerOrdersToday.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerOrdersToday.setVisibility(View.VISIBLE);
            detailsAdapter.filterList(orders); // Giả sử adapter cũ có hàm update list
        }
    }

    private class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

        private List<Date> days;
        private Map<String, List<OrderShiperHistory>> dataMap;
        private int displayMonth;
        private SimpleDateFormat sdfKey = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        private SimpleDateFormat sdfDay = new SimpleDateFormat("d", Locale.getDefault());

        public void setData(List<Date> days, Map<String, List<OrderShiperHistory>> map, int month) {
            this.days = days;
            this.dataMap = map;
            this.displayMonth = month;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
            return new DayViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
            Date date = days.get(position);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            String dayText = sdfDay.format(date);
            String fullDateKey = sdfKey.format(date); // Key để map với data (vd: 10/12/2025)

            holder.tvDay.setText(dayText);

            if (cal.get(Calendar.MONTH) != displayMonth) {
                holder.tvDay.setAlpha(0.3f);
                holder.tvCount.setVisibility(View.GONE);
                holder.container.setBackgroundResource(0);
            } else {
                holder.tvDay.setAlpha(1.0f);

                List<OrderShiperHistory> orders = dataMap.get(fullDateKey);
                if (orders != null && !orders.isEmpty()) {
                    holder.tvCount.setVisibility(View.VISIBLE);
                    holder.tvCount.setText(orders.size() + " đơn");
                    holder.container.setBackgroundResource(R.drawable.bg_day_has_order);
                } else {
                    holder.tvCount.setVisibility(View.GONE);
                    holder.container.setBackgroundResource(0);
                }

                if (fullDateKey.equals(selectedDateKey)) {
                    holder.container.setBackgroundResource(R.drawable.bg_day_selected);
                    holder.tvDay.setTextColor(getResources().getColor(android.R.color.white));
                    holder.tvCount.setTextColor(getResources().getColor(android.R.color.white));
                } else {
                    holder.tvDay.setTextColor(getResources().getColor(R.color.md_theme_scrim));
                    // Reset text color nếu không selected
                }
            }

            holder.itemView.setOnClickListener(v -> {
                if (cal.get(Calendar.MONTH) == displayMonth) {
                    loadOrdersForDate(fullDateKey);
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getItemCount() {
            return days == null ? 0 : days.size();
        }

        class DayViewHolder extends RecyclerView.ViewHolder {
            TextView tvDay, tvCount;
            LinearLayout container;

            public DayViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDay = itemView.findViewById(R.id.text_day);
                tvCount = itemView.findViewById(R.id.text_order_count);
                container = itemView.findViewById(R.id.item_day_container);
            }
        }
    }
}