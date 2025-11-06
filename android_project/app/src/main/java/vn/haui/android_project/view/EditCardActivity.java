package vn.haui.android_project.view;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher; // Import quan trọng
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.PaymentCard;
import vn.haui.android_project.enums.MyConstant;
import vn.haui.android_project.services.FirebasePaymentManager;

public class EditCardActivity extends AppCompatActivity {

    private static final String TAG = "EditCardActivity";
    private String currentCardId;

    // Views
    private EditText etNameOnCard, etCardNumber, etExpirationDate, etCvv;
    private Button btnEditCard;
    private ImageButton btnBack, btnDelete;

    // Card Preview Views (Để xem trước thay đổi)
    private View cardBackgroundView; // Nền thẻ để đặt gradient
    private TextView tvMockCardNumber, tvMockCardHolder, tvMockExpiryDate;
    private ImageView ivCardTypeLogo; // Logo thẻ

    private FirebasePaymentManager paymentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_card);

        paymentManager = FirebasePaymentManager.getInstance();
        currentCardId = getIntent().getStringExtra("card_id");

        if (currentCardId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID thẻ.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mapViews();
        loadCardData();
        setupListener(); // <-- Sẽ gọi setupTextWatchers() bên trong
    }

    private void mapViews() {
        etNameOnCard = findViewById(R.id.et_name_on_card);
        etCardNumber = findViewById(R.id.et_card_number);
        etExpirationDate = findViewById(R.id.et_expiration_date);
        etCvv = findViewById(R.id.et_cvv);

        btnEditCard = findViewById(R.id.btn_edit_card);
        btnBack = findViewById(R.id.btn_back);
        btnDelete = findViewById(R.id.btn_delete);

        // 💡 Giữ ID của cardBackgroundView chính xác theo layout của bạn
        cardBackgroundView = findViewById(R.id.bg_card_mock);
        tvMockCardNumber = findViewById(R.id.tv_mock_card_number);
        tvMockCardHolder = findViewById(R.id.tv_mock_card_holder);
        tvMockExpiryDate = findViewById(R.id.tv_mock_expiry_date);
        ivCardTypeLogo = findViewById(R.id.iv_card_type_logo);
    }

    /**
     * Tải dữ liệu thẻ hiện tại từ Firestore và điền vào các EditText.
     */
    private void loadCardData() {
        paymentManager.getCardDetails(currentCardId, (card, error) -> {
            if (error != null) {
                Toast.makeText(this, "Không thể tải dữ liệu thẻ.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (card != null) {
                // Điền dữ liệu vào form
                etNameOnCard.setText(card.getNameOnCard());
                // Khi tải, dùng số thẻ đã được định dạng cho form edit
                etCardNumber.setText(formatCardNumberForEdit(card.getCardNumber()));
                etExpirationDate.setText(card.getExpirationDate());
                etCvv.setText(card.getCvv());

                // Cập nhật xem trước thẻ
                updateCardPreview(card);
            }
        });
    }

    /**
     * Cập nhật màu và thông tin trên thẻ xem trước.
     */
    private void updateCardPreview(PaymentCard card) {
        if (card == null || tvMockCardNumber == null) return;

        // Cập nhật text từ dữ liệu tải về
        tvMockCardNumber.setText(formatCardNumberForPreview(card.getCardNumber()));
        tvMockCardHolder.setText(card.getNameOnCard());
        tvMockExpiryDate.setText(card.getExpirationDate());

        // Đặt background và logo lần đầu
        setCardBackgroundAndLogo(card.getCardType());
    }

    private void setCardBackgroundAndLogo(String cardType) {
        // Thêm kiểm tra Null View an toàn
        if (cardBackgroundView == null || ivCardTypeLogo == null) return;

        int backgroundResId;
        int logoResId;

        // 🏆 Fix Lỗi NullPointerException: Gọi equals() trên hằng số
        String type = cardType != null ? cardType : "";

        if (MyConstant.CARD_MASTERCARD.equals(type)) {
            backgroundResId = R.drawable.bg_mastercard_gradient;
            logoResId = R.drawable.ic_mastercard_logo;
        } else if (MyConstant.Card_VISA.equals(type)) {
            backgroundResId = R.drawable.bg_visa_gradient;
            logoResId = R.drawable.ic_visa_logo;
        } else if (MyConstant.CARD_JCB.equals(type)) {
            backgroundResId = R.drawable.bg_jcb_gradient;
            logoResId = R.drawable.ic_jbc_logo;
        } else {
            backgroundResId = R.drawable.bg_othercard_gradient;
            logoResId = R.drawable.logo;
        }

        cardBackgroundView.setBackgroundResource(backgroundResId);
        ivCardTypeLogo.setImageResource(logoResId);
    }

    private void setupListener() {
        btnBack.setOnClickListener(v -> finish());
        btnEditCard.setOnClickListener(v -> updateCard());
        btnDelete.setOnClickListener(v -> deleteCard());

        // 🌟 Thiết lập TextWatchers để cập nhật trực tiếp
        setupTextWatchers();
    }

    /**
     * Thiết lập các TextWatcher cho EditText để cập nhật UI ngay lập tức.
     */
    private void setupTextWatchers() {

        // 1. Name on Card (Tên chủ thẻ)
        etNameOnCard.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(tvMockCardHolder != null) tvMockCardHolder.setText(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // 2. Card Number (Số thẻ) - Cần Format và thay đổi Logo/Màu
        etCardNumber.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Lấy số thẻ thô
                String rawNumber = s.toString().replaceAll("\\s+", "");
                // 1. Cập nhật số thẻ xem trước
                if(tvMockCardNumber != null) tvMockCardNumber.setText(formatCardNumberForPreview(rawNumber));
                // 2. Xác định loại thẻ và cập nhật giao diện
                String type = determineCardType(rawNumber);
                setCardBackgroundAndLogo(type);
            }
            @Override public void afterTextChanged(Editable s) {
                // (Tùy chọn: Thêm logic format số thẻ tự động 4-4-4-4 ở đây)
            }
        });

        // 3. Expiration Date (Ngày hết hạn)
        etExpirationDate.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(tvMockExpiryDate != null) tvMockExpiryDate.setText(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void updateCard() {
        String newName = etNameOnCard.getText().toString().trim();
        String newCardNumber = etCardNumber.getText().toString().replaceAll("\\s+", "");
        String newExpiry = etExpirationDate.getText().toString().trim();
        String newCvv = etCvv.getText().toString().trim();

        if (newName.isEmpty() || newCardNumber.length() < 15 || newExpiry.length() < 4 || newCvv.length() < 3) {
            Toast.makeText(this, "Vui lòng điền đầy đủ và chính xác thông tin.", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("nameOnCard", newName);
        updates.put("cardNumber", newCardNumber);
        updates.put("expirationDate", newExpiry);
        updates.put("cvv", newCvv);
        updates.put("cardType", determineCardType(newCardNumber));
        updates.put("last4Digits", newCardNumber.substring(newCardNumber.length() - 4));


        paymentManager.updateCardByFields(currentCardId, updates, (isSuccess, message) -> {
            if (isSuccess) {
                Toast.makeText(this, "Cập nhật thẻ thành công!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Log.e("EDIT_CARD", "Update failed: " + message);
                Toast.makeText(this, "Lỗi khi cập nhật thẻ: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void deleteCard() {
        // ... (Hàm này giữ nguyên) ...
        paymentManager.deleteCard(currentCardId, (isSuccess, message) -> {
            if (isSuccess) {
                Toast.makeText(this, "Đã xóa thẻ thành công.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Log.e("DELETE_CARD", "Lỗi xóa thẻ: " + message);
                Toast.makeText(this, "Lỗi khi xóa thẻ: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String formatCardNumberForEdit(String rawNumber) {
        return rawNumber;
    }

    private String formatCardNumberForPreview(String rawNumber) {
        if (rawNumber == null || rawNumber.length() < 4) return rawNumber;
        // Hiển thị xxxx xxxx xxxx YYYY
        return "**** **** **** " + rawNumber.substring(rawNumber.length() - 4);
    }

    private String determineCardType(String cardNumber) {
        if (cardNumber.startsWith("4")) return MyConstant.Card_VISA;
        if (cardNumber.startsWith("5")) return MyConstant.CARD_MASTERCARD;
        if (cardNumber.startsWith("3")) return MyConstant.CARD_JCB;
        return MyConstant.Card_OTHER;
    }
}