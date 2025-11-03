package vn.haui.android_project.view;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.PaymentCard;
import vn.haui.android_project.enums.MyConstant;
import vn.haui.android_project.services.FirebasePaymentManager;

public class AddCardActivity extends AppCompatActivity {

    // Input Fields
    private EditText etName, etCardNumber, etExpiry, etCvv;
    private Button btnAddCard;
    private ImageButton btnBack;

    // Mock Card Preview Views (Từ layout item_card_preview_mock.xml)
    private TextView tvMockCardNumber, tvMockCardHolder, tvMockExpiryDate;
    private ImageView ivCardTypeLogo;

    private FirebasePaymentManager paymentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_card);

        paymentManager = FirebasePaymentManager.getInstance();

        mapViews();
        setupListener();
        setupTextWatchers();
    }

    private void mapViews() {
        // Input Fields
        etName = findViewById(R.id.et_name_on_card);
        etCardNumber = findViewById(R.id.et_card_number);
        etExpiry = findViewById(R.id.et_expiration_date);
        etCvv = findViewById(R.id.et_cvv);
        btnAddCard = findViewById(R.id.btn_add_card);
        btnBack = findViewById(R.id.btn_back);

        // Mock Card Preview Views (Tìm kiếm trực tiếp)
        tvMockCardNumber = findViewById(R.id.tv_mock_card_number);
        tvMockCardHolder = findViewById(R.id.tv_mock_card_holder);
        tvMockExpiryDate = findViewById(R.id.tv_mock_expiry_date);
        ivCardTypeLogo = findViewById(R.id.iv_card_type_logo);
    }

    private void setupListener() {
        btnBack.setOnClickListener(v -> finish());
        btnAddCard.setOnClickListener(v -> saveCardToFirebase());
    }

    // -------------------------------------------------------------------
    // I. TEXT WATCHER & MOCK PREVIEW UPDATE
    // -------------------------------------------------------------------
    private void setupTextWatchers() {
        // TextWatcher chung để cập nhật Preview (dùng cho Name và Card Number)
        TextWatcher previewUpdater = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCardPreview(); // Cập nhật chung cho tên và số thẻ (định dạng sẵn)
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        etName.addTextChangedListener(previewUpdater);
        etCardNumber.addTextChangedListener(previewUpdater);

        // Thêm TextWatcher riêng cho trường Expiry Date
        etExpiry.addTextChangedListener(new ExpiryDateTextWatcher(etExpiry));
    }

    private void updateCardPreview() {
        // 1. Name on Card
        String name = etName.getText().toString().trim();
        tvMockCardHolder.setText(name.isEmpty() ? "CARD HOLDER" : name.toUpperCase());

        // 2. Card Number
        String number = etCardNumber.getText().toString().trim().replaceAll("\\s+", "");
        String formattedNumber = formatCardNumber(number);
        tvMockCardNumber.setText(formattedNumber);

        // Cập nhật logo
        updateCardLogo(number);

        // 3. Expiration Date
        String expiry = etExpiry.getText().toString().trim();
        tvMockExpiryDate.setText(expiry.isEmpty() ? "MM/YY" : expiry);
    }

    private String formatCardNumber(String number) {
        // Chỉ định dạng 16 số
        StringBuilder formatted = new StringBuilder();
        int maxLength = 16;

        for (int i = 0; i < Math.min(number.length(), maxLength); i++) {
            if (i > 0 && i % 4 == 0) {
                formatted.append("  ");
            }
            formatted.append(number.charAt(i));
        }

        // Thêm placeholder "X" vào cuối
        int currentLength = number.length();
        for (int i = 0; i < maxLength - currentLength; i++) {
            if ((currentLength + i) > 0 && (currentLength + i) % 4 == 0) {
                formatted.append("  ");
            }
            formatted.append("X");
        }

        return formatted.toString();
    }

    private void updateCardLogo(String number) {
        // Cần đảm bảo các drawable ic_visa_logo và ic_mastercard_logo tồn tại
        if (number.startsWith("4")) {
            ivCardTypeLogo.setImageResource(R.drawable.ic_visa_logo);
        } else if (number.startsWith("5")) {
            ivCardTypeLogo.setImageResource(R.drawable.ic_mastercard_logo);
        }  else if (number.startsWith("3")) {
            ivCardTypeLogo.setImageResource(R.drawable.ic_jbc_logo);
        }
    }

    private String determineCardType(String cardNumber) {
        if (cardNumber.startsWith("4")) return MyConstant.Card_VISA;
        if (cardNumber.startsWith("5")) return MyConstant.CARD_MASTERCARD;
        if (cardNumber.startsWith("3")) return MyConstant.CARD_JCB;
        return MyConstant.Card_OTHER;
    }

    // -------------------------------------------------------------------
    // II. SAVE TO FIRESTORE
    // -------------------------------------------------------------------

    private void saveCardToFirebase() {
        String name = etName.getText().toString().trim();
        String number = etCardNumber.getText().toString().trim().replaceAll("\\s+", "");
        String expiry = etExpiry.getText().toString().trim();
        String cvv = etCvv.getText().toString().trim();

        // Validation cơ bản
        if (name.isEmpty() || number.length() != 16 || expiry.length() != 5 || cvv.length() < 3) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ và chính xác thông tin thẻ.", Toast.LENGTH_SHORT).show();
            return;
        }

        PaymentCard newCard = new PaymentCard();
        newCard.setNameOnCard(name);
        newCard.setCardNumber(number);
        newCard.setExpirationDate(expiry);
        newCard.setCvv(cvv);

        // Chuẩn bị dữ liệu cho danh sách hiển thị
        newCard.setCardType(determineCardType(number));
        newCard.setLast4Digits(number.substring(number.length() - 4));

        btnAddCard.setEnabled(false);

        // Lưu vào Firestore qua Service
        paymentManager.addCard(newCard)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Thẻ đã được thêm thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("AddCardActivity", "Lỗi khi lưu thẻ: " + e.getMessage());
                    Toast.makeText(this, "Lỗi khi lưu thẻ: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnAddCard.setEnabled(true);
                });
    }
    private class ExpiryDateTextWatcher implements TextWatcher {
        private final EditText editText;
        private String current = "";
        private final String separator = "/";

        public ExpiryDateTextWatcher(EditText editText) {
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            current = s.toString();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Gọi hàm cập nhật Preview của Activity
            updateCardPreview();
        }

        @Override
        public void afterTextChanged(Editable s) {
            String input = s.toString();

            if (input.equals(current)) {
                return;
            }

            // Xóa mọi ký tự không phải số
            String cleanInput = input.replaceAll("[^\\d]", "");

            if (cleanInput.length() >= 2) {
                String formatted;

                // Trường hợp người dùng xóa dấu /
                if (cleanInput.length() > 2) {
                    formatted = cleanInput.substring(0, 2) + separator + cleanInput.substring(2);
                }
                // Trường hợp chỉ nhập 2 số
                else {
                    formatted = cleanInput.substring(0, 2) + separator;
                }

                // Giới hạn độ dài tối đa là 5 (MM/YY)
                if (formatted.length() > 5) {
                    formatted = formatted.substring(0, 5);
                }

                // Cập nhật EditText và giữ con trỏ ở cuối
                editText.setText(formatted);
                editText.setSelection(formatted.length());
            }
        }
    }
}