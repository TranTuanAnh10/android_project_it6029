package vn.haui.android_project.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList; // Thêm import cho ArrayList
import java.util.List;
import java.util.Objects;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.PaymentCard;
import vn.haui.android_project.enums.MyConstant;

public class ChooseCardAdapter extends RecyclerView.Adapter<ChooseCardAdapter.CardViewHolder> {

    private final Context context;
    private final List<PaymentCard> cardList;
    private final OnCardClickListener listener;

    // 🏆 Thẻ được chọn (để quản lý trạng thái Radio Button)
    private PaymentCard selectedCard;

    public interface OnCardClickListener {
        void onCardClick(PaymentCard card);
    }

    public ChooseCardAdapter(Context context, List<PaymentCard> cardList, OnCardClickListener listener) {
        this.context = context;
        // 💡 Khởi tạo list trống nếu list truyền vào là null (phòng trường hợp lỗi)
        this.cardList = cardList != null ? cardList : new ArrayList<>();
        this.listener = listener;
    }

    // 1. HÀM CẬP NHẬT THẺ ĐƯỢC CHỌN (DÙNG CHO BottomSheet.java)
    /**
     * Đặt thẻ được chọn và cập nhật UI.
     * Có thể truyền {@code null} để bỏ chọn tất cả thẻ.
     */
    public void setSelectedCard(PaymentCard card) {
        this.selectedCard = card;
        notifyDataSetChanged();
    }

    // 2. HÀM CẬP NHẬT DANH SÁCH THẺ (DÙNG CHO PaymentMethodsActivity.java)
    /**
     * Cập nhật toàn bộ danh sách thẻ (Realtime update).
     */
    public void setCards(List<PaymentCard> newCards) {
        cardList.clear();
        if (newCards != null) {
            cardList.addAll(newCards);
        }

        // Đặt lại thẻ được chọn nếu thẻ cũ không còn trong danh sách mới
        if (selectedCard != null && !cardList.contains(selectedCard)) {
            selectedCard = null;
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Dùng item_payment_card_select (nếu bạn muốn dùng giao diện Radio Button)
        // hoặc item_card_preview (nếu bạn muốn dùng giao diện Card Stack)
        // Tôi dùng item_payment_card_select để phù hợp với logic RadioButton/setSelectedCard.
        View view = LayoutInflater.from(context).inflate(R.layout.item_payment_card_select, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        PaymentCard card = cardList.get(position);

        // 1. Quản lý trạng thái chọn
        // So sánh dựa trên ID của thẻ
        boolean isSelected = Objects.equals(card.getCardId(), selectedCard != null ? selectedCard.getCardId() : null);
        if (holder.rbCardSelection != null) {
            holder.rbCardSelection.setChecked(isSelected);
        }
        if (holder.cardItemContainer != null) {
            holder.cardItemContainer.setActivated(isSelected); // Kích hoạt background selector
        }


        // 2. Hiển thị thông tin
        String cardTypeDisplay = getCardTypeDisplayName(card.getCardType()); // VISA, MASTERCARD...
        String cardMethod = getCardMethodForDisplay(card); // Credit, Debit...

        if (holder.tvCardTypeTitle != null) {
            holder.tvCardTypeTitle.setText(cardTypeDisplay + " " + cardMethod);
        }
        if (holder.tvCardLastDigits != null) {
            holder.tvCardLastDigits.setText("*" + card.getLast4Digits());
        }

        // 3. Đặt logo
        int logoResId = getCardLogoResId(card.getCardType());
        if (holder.ivCardLogo != null) {
            holder.ivCardLogo.setImageResource(logoResId);
        }


        // 4. Đặt listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCardClick(card);
            }
            // Tự động set trạng thái chọn khi click (thường cần trong Bottom Sheet)
            setSelectedCard(card);
        });
    }

    @Override
    public int getItemCount() {
        return cardList.size();
    }

    private int getCardLogoResId(String cardType) {
        if (cardType == null) return R.drawable.ic_credit_card;
        if (MyConstant.Card_VISA.equalsIgnoreCase(cardType)) return R.drawable.ic_visa;
        if (MyConstant.CARD_MASTERCARD.equalsIgnoreCase(cardType)) return R.drawable.ic_mastercard;
        if (MyConstant.CARD_JCB.equalsIgnoreCase(cardType)) return R.drawable.ic_jcb;
        return R.drawable.ic_credit_card;
    }
    private String getCardTypeDisplayName(String cardType) {
        if (cardType == null) return "Unknown Card";
        if (MyConstant.Card_VISA.equalsIgnoreCase(cardType)) return "VISA";
        if (MyConstant.CARD_MASTERCARD.equalsIgnoreCase(cardType)) return "MASTERCARD";
        if (MyConstant.CARD_JCB.equalsIgnoreCase(cardType)) return "JCB";
        return "Unknown Card";
    }

    // Giả định cách lấy loại phương thức (Credit/Debit)
    private String getCardMethodForDisplay(PaymentCard card) {
        // !!! BẠN CẦN THAY THẾ BẰNG LOGIC LẤY TỪ Entity PaymentCard (ví dụ: card.getCardMethod()) !!!
        if (MyConstant.Card_VISA.equalsIgnoreCase(card.getCardType())) {
            return "Credit";
        } else if (MyConstant.CARD_MASTERCARD.equalsIgnoreCase(card.getCardType())) {
            return "Debit";
        }
        return "Card";
    }


    public static class CardViewHolder extends RecyclerView.ViewHolder {
        // Các Views từ item_payment_card_select.xml
        final View cardItemContainer;
        final RadioButton rbCardSelection;
        final ImageView ivCardLogo;
        final TextView tvCardTypeTitle;
        final TextView tvCardLastDigits;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            // 💡 Cần đảm bảo các ID này tồn tại trong item_payment_card_select.xml
            cardItemContainer = itemView.findViewById(R.id.card_item_container);
            rbCardSelection = itemView.findViewById(R.id.rb_card_selection);
            ivCardLogo = itemView.findViewById(R.id.iv_card_logo);
            tvCardTypeTitle = itemView.findViewById(R.id.tv_card_type_title);
            tvCardLastDigits = itemView.findViewById(R.id.tv_card_last_digits);
        }
    }
}