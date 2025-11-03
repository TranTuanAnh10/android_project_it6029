package vn.haui.android_project.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.PaymentCard;
import vn.haui.android_project.enums.MyConstant;

public class PaymentCardAdapter extends RecyclerView.Adapter<PaymentCardAdapter.CardViewHolder> {

    private final Context context;
    private final List<PaymentCard> cardList;

    public interface OnCardClickListener {
        void onCardClick(PaymentCard card);
    }
    private OnCardClickListener listener;

    public PaymentCardAdapter(Context context, List<PaymentCard> cardList, OnCardClickListener listener) {
        this.context = context;
        this.cardList = cardList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Layout dùng cho từng thẻ
        View view = LayoutInflater.from(context).inflate(R.layout.item_card_preview, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        PaymentCard card = cardList.get(position);

        // Hiển thị thông tin
        // Giả định bạn đã thêm trường last4Digits vào PaymentCardEntity
        holder.tvCardDetail.setText(card.getLast4Digits());
        holder.tvNameOnCard.setText(card.getNameOnCard());

        // --- 1. Đặt Background và Logo theo loại thẻ ---
        setCardDetails(holder, card);

        // Đặt listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCardClick(card);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cardList.size();
    }

    public void setCards(List<PaymentCard> newCards) {
        cardList.clear();
        cardList.addAll(newCards);
        notifyDataSetChanged();
    }

    // -------------------------------------------------------------------
    // PHẦN MỚI: XỬ LÝ BACKGROUND VÀ LOGO
    // -------------------------------------------------------------------

    private void setCardDetails(CardViewHolder holder, PaymentCard card) {
        // Dùng cardType (hoặc một trường khác) để xác định loại thẻ
        String cardType = card.getCardType() != null ? card.getCardType() : "";
        int backgroundResId;
        int logoResId;
        String tvCardType=card.getCardType();
        if (cardType.equals(MyConstant.CARD_MASTERCARD)) {
            backgroundResId = R.drawable.bg_mastercard_gradient;
            logoResId = R.drawable.ic_mastercard_logo;
        } else if (cardType.equals(MyConstant.Card_VISA)) {
            backgroundResId = R.drawable.bg_visa_gradient;
            logoResId = R.drawable.ic_visa_logo;
        } else if (cardType.equals(MyConstant.CARD_JCB)) {
            backgroundResId = R.drawable.bg_jcb_gradient;
            logoResId = R.drawable.ic_jbc_logo;
        } else {
            // Mặc định
            backgroundResId = R.drawable.bg_othercard_gradient;
            logoResId = R.drawable.logo;
        }

        // Đặt background cho CardView/LinearLayout lớn (Giả định ID là card_background_view)
        holder.cardBackgroundView.setBackgroundResource(backgroundResId);

        // Đặt logo
        holder.ivCardLogo.setImageResource(logoResId);

        // Đặt tên loại thẻ
        holder.tvCardType.setText(tvCardType);
    }


    public static class CardViewHolder extends RecyclerView.ViewHolder {
        // View nền lớn nhất (CardView hoặc ConstraintLayout)
        final View cardBackgroundView;

        final ImageView ivCardLogo;
        final TextView tvCardDetail; // Dùng để hiển thị 4 số cuối
        final TextView tvNameOnCard; // Dùng để hiển thị tên chủ thẻ
        final TextView tvCardType;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);

            // Cần cập nhật ID trong item_card_preview.xml để phù hợp với tên biến này
            cardBackgroundView = itemView.findViewById(R.id.card_background_view);

            ivCardLogo = itemView.findViewById(R.id.iv_card_logo);
            tvCardDetail = itemView.findViewById(R.id.tv_card_detail);
            tvNameOnCard = itemView.findViewById(R.id.tv_name_on_card);
            tvCardType=itemView.findViewById(R.id.tv_card_type);
        }
    }
}