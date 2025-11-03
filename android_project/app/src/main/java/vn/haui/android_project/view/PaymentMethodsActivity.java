package vn.haui.android_project.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import vn.haui.android_project.R;
import vn.haui.android_project.adapter.CardStackItemDecoration;
import vn.haui.android_project.adapter.PaymentCardAdapter;
import vn.haui.android_project.entity.PaymentCard;
import vn.haui.android_project.services.FirebasePaymentManager;


public class PaymentMethodsActivity extends AppCompatActivity
        implements PaymentCardAdapter.OnCardClickListener {

    private RecyclerView recyclerCards;
    private Button btnAddNewCard;
    private ImageButton btnBack;

    private FirebasePaymentManager paymentManager;
    private PaymentCardAdapter cardAdapter;
    private List<PaymentCard> cardList; // Nên được khởi tạo sớm
    private ListenerRegistration firestoreListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_methods);

        // Khởi tạo Firebase Manager
        paymentManager = FirebasePaymentManager.getInstance();
        // Khởi tạo danh sách thẻ rỗng
        cardList = new ArrayList<>();

        mapViews();
        setupRecyclerView();
        setupListeners();
    }

    private void mapViews() {
        recyclerCards = findViewById(R.id.recycler_cards);
        btnAddNewCard = findViewById(R.id.btn_add_new_card);
        btnBack = findViewById(R.id.btn_back);
    }

    private void setupRecyclerView() {
        // 1. Cài đặt LayoutManager
        recyclerCards.setLayoutManager(new LinearLayoutManager(this));

        // 2. KHỞI TẠO VÀ GÁN ADAPTER (Đã sửa thứ tự đối số)

        // THỨ TỰ ĐÚNG: Context, List, Listener
        cardAdapter = new PaymentCardAdapter(this, cardList, this);
        recyclerCards.setAdapter(cardAdapter);

        // 3. Áp dụng ItemDecoration để tạo hiệu ứng Card Stack
        int overlapDp = 120;
        CardStackItemDecoration itemDecoration = new CardStackItemDecoration(this, overlapDp);

        if (recyclerCards.getItemDecorationCount() == 0) {
            recyclerCards.addItemDecoration(itemDecoration);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnAddNewCard.setOnClickListener(v -> {
            // Mở màn hình Thêm mới thẻ
            Intent intent = new Intent(this, AddCardActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadUserCardsRealtime();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Dừng lắng nghe khi Activity ẩn/dừng để tránh rò rỉ bộ nhớ
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
    }

    private void loadUserCardsRealtime() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem thẻ thanh toán.", Toast.LENGTH_LONG).show();
            return;
        }

        // --- Logic Realtime Update: Sử dụng BiConsumer để nhận List<PaymentCard> ---

        // BiConsumer nhận (isSuccess, cardList đã được ánh xạ từ Service)
        BiConsumer<Boolean, List<PaymentCard>> cardUpdateListener = (isSuccess, receivedCardList) -> {
            if (isSuccess) {
                // Sửa lỗi: Danh sách adapter sẽ được cập nhật
                if (receivedCardList != null) {
                    cardAdapter.setCards(receivedCardList);
                } else {
                    cardAdapter.setCards(new ArrayList<>());
                }
            } else {
                // Xử lý lỗi (Lỗi đã được log trong FirebasePaymentManager)
                Toast.makeText(this, "Lỗi khi tải danh sách thẻ.", Toast.LENGTH_SHORT).show();
                cardAdapter.setCards(new ArrayList<>());
            }
        };

        // Bỏ listener cũ nếu có, sau đó đăng ký listener mới
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
        firestoreListener = paymentManager.getCardsRealtime(cardUpdateListener);
    }


    // -------------------------------------------------------------------
    // II. ADAPTER CLICK LISTENER (Mở màn hình Edit)
    // -------------------------------------------------------------------

    @Override
    public void onCardClick(PaymentCard card) {
        // Khi người dùng click vào một thẻ, mở màn hình EditCardActivity
        Intent intent = new Intent(this, EditCardActivity.class);
        intent.putExtra("card_id", card.getCardId());
        startActivity(intent);
    }
}