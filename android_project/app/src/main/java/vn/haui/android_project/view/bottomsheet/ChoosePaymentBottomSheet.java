package vn.haui.android_project.view.bottomsheet;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.adapter.ChooseCardAdapter; // Đã sử dụng
import vn.haui.android_project.entity.PaymentCard;
import vn.haui.android_project.services.FirebasePaymentManager;
import vn.haui.android_project.view.AddCardActivity;

public class ChoosePaymentBottomSheet extends BottomSheetDialogFragment
        // 🏆 ĐÃ SỬA LỖI: Triển khai interface của ChooseCardAdapter
        implements ChooseCardAdapter.OnCardClickListener {

    private static final String TAG = "PaymentBottomSheet";

    public interface PaymentSelectionListener {
        void onCardSelected(PaymentCard selectedCard);
        void onCashSelected();
    }

    private PaymentSelectionListener listener;

    // Header Views
    private ImageView btnCloseBottomSheet;

    // Credit Card Section Views
    private ConstraintLayout headerCreditCardSection;
    private LinearLayout containerCreditCardSection;
    private ImageView ivCardExpandCollapse;
    private RecyclerView recyclerViewCards;
    private TextView tvAddNewCard;

    // Cash on Delivery Section Views
    private ConstraintLayout containerCash;
    private Switch switchCashOnDelivery;

    private ChooseCardAdapter cardAdapter;
    private final List<PaymentCard> cardList = new ArrayList<>(); // Vẫn giữ list này
    private ListenerRegistration cardListenerRegistration;

    private PaymentCard selectedPaymentCard = null;

    public static ChoosePaymentBottomSheet newInstance(PaymentSelectionListener listener) {
        ChoosePaymentBottomSheet fragment = new ChoosePaymentBottomSheet();
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_choose_payment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ Views
        btnCloseBottomSheet = view.findViewById(R.id.btn_close_bottom_sheet);
        headerCreditCardSection = view.findViewById(R.id.header_credit_card_section);
        containerCreditCardSection = view.findViewById(R.id.container_credit_card_section);
        ivCardExpandCollapse = view.findViewById(R.id.iv_card_expand_collapse);
        recyclerViewCards = view.findViewById(R.id.recycler_view_cards);
        tvAddNewCard = view.findViewById(R.id.tv_add_new_card);
        containerCash = view.findViewById(R.id.container_cash);
        switchCashOnDelivery = view.findViewById(R.id.switch_cash_on_delivery);

        // Cài đặt Adapter
        cardAdapter = new ChooseCardAdapter(requireContext(), cardList, this);
        recyclerViewCards.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewCards.setAdapter(cardAdapter);

        // Cài đặt Listener
        setupListeners();

        // Tải dữ liệu
        loadUserCards();
    }

    private void setupListeners() {
        btnCloseBottomSheet.setOnClickListener(v -> dismiss());

        headerCreditCardSection.setOnClickListener(v -> toggleCardListVisibility());

        tvAddNewCard.setOnClickListener(v -> {
            // Mở màn hình Thêm mới thẻ
            Intent intent = new Intent(requireContext(), AddCardActivity.class);
            startActivity(intent);

            // Tùy chọn: Đóng Bottom Sheet sau khi chuyển sang màn hình thêm thẻ
            // dismiss();
        });

        switchCashOnDelivery.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Nếu chọn COD, bỏ chọn thẻ
                if (selectedPaymentCard != null) {
                    selectedPaymentCard = null;
                    cardAdapter.setSelectedCard(null);
                }
                if (listener != null) {
                    listener.onCashSelected();
                }
            }
        });

        containerCash.setOnClickListener(v -> {
            switchCashOnDelivery.setChecked(!switchCashOnDelivery.isChecked());
        });
    }


    private void toggleCardListVisibility() {
        if (recyclerViewCards.getVisibility() == View.VISIBLE) {
            recyclerViewCards.setVisibility(View.GONE);
            tvAddNewCard.setVisibility(View.GONE);
            ivCardExpandCollapse.setImageResource(R.drawable.ic_arrow_drop_down);
        } else {
            recyclerViewCards.setVisibility(View.VISIBLE);
            tvAddNewCard.setVisibility(View.VISIBLE);
            ivCardExpandCollapse.setImageResource(R.drawable.ic_arrow_drop_up);
        }
    }

    @Override
    public void onCardClick(PaymentCard card) {
        selectedPaymentCard = card;
        cardAdapter.setSelectedCard(card);
        if (switchCashOnDelivery.isChecked()) {
            switchCashOnDelivery.setChecked(false);
        }
        if (listener != null) {
            listener.onCardSelected(card);
        }
    }

    private void loadUserCards() {
        cardListenerRegistration = FirebasePaymentManager.getInstance().getCardsRealtime(
                (isSuccess, cards) -> {
                    if (isSuccess) {
                        // 🏆 TỐI ƯU: Sử dụng setCards() của Adapter để cập nhật dữ liệu
                        cardAdapter.setCards(cards);

                        // Tự động chọn thẻ đầu tiên nếu có và chưa có thẻ nào được chọn
                        if (!cardList.isEmpty() && selectedPaymentCard == null) {
                            // Gọi onCardClick để tự động chọn và kích hoạt listener
                            onCardClick(cardList.get(0));
                        }
                    } else {
                        Log.e(TAG, "Lỗi tải danh sách thẻ từ Firebase.");
                        Toast.makeText(getContext(), "Không thể tải thẻ thanh toán.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cardListenerRegistration != null) {
            cardListenerRegistration.remove();
        }
    }
}