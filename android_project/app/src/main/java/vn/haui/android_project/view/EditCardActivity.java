package vn.haui.android_project.view;

import android.os.Bundle;
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

        // Khởi tạo Manager
        paymentManager = FirebasePaymentManager.getInstance();

        // 1. Lấy ID thẻ từ Intent
        currentCardId = getIntent().getStringExtra("card_id");

        if (currentCardId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID thẻ.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mapViews();
        loadCardData();
        setupListener();
    }

    private void mapViews() {
        etNameOnCard = findViewById(R.id.et_name_on_card);
        etCardNumber = findViewById(R.id.et_card_number);
        etExpirationDate = findViewById(R.id.et_expiration_date);
        etCvv = findViewById(R.id.et_cvv);

        btnEditCard = findViewById(R.id.btn_edit_card);
        btnBack = findViewById(R.id.btn_back);
        btnDelete = findViewById(R.id.btn_delete);
        cardBackgroundView = findViewById(R.id.card_background_view);
        tvMockCardNumber = findViewById(R.id.tv_mock_card_number);
        tvMockCardHolder = findViewById(R.id.tv_mock_card_holder);
        tvMockExpiryDate = findViewById(R.id.tv_mock_expiry_date);
        ivCardTypeLogo = findViewById(R.id.iv_card_type_logo);

    }

    /**
     * Tải dữ liệu thẻ hiện tại từ Firestore và điền vào các EditText.
     */
    private void loadCardData() {
        // Sử dụng Manager để lấy dữ liệu chi tiết của một thẻ
        paymentManager.getCardDetails(currentCardId, (card, error) -> {
            if (error != null) {
                Toast.makeText(this, "Không thể tải dữ liệu thẻ.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (card != null) {
                // Điền dữ liệu vào form
                etNameOnCard.setText(card.getNameOnCard());
                etCardNumber.setText(formatCardNumberForEdit(card.getCardNumber()));
                etExpirationDate.setText(card.getExpirationDate());
                etCvv.setText(card.getCvv());

                // Cập nhật xem trước thẻ (tương tự như AddCardActivity)
                updateCardPreview(card);
            }
        });
    }

    /**
     * Cập nhật màu và thông tin trên thẻ xem trước.
     */
    private void updateCardPreview(PaymentCard card) {
        if (card == null || tvMockCardNumber == null) return;

        // Chỉ hiển thị 4 số cuối (hoặc toàn bộ số thẻ)
        tvMockCardNumber.setText(formatCardNumberForPreview(card.getCardNumber()));
        tvMockCardHolder.setText(card.getNameOnCard());
        tvMockExpiryDate.setText(card.getExpirationDate());

        // Đặt background và logo (Hàm này cần được định nghĩa)
        setCardBackgroundAndLogo(card.getCardType());
    }

    private void setCardBackgroundAndLogo(String cardType) {
//        if (cardBackgroundView == null || ivCardTypeLogo == null) return;
        int backgroundResId;
        int logoResId;
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

    }

    private void updateCard() {
        // 1. Thu thập dữ liệu và validate (Giữ nguyên)
        String newName = etNameOnCard.getText().toString().trim();
        String newCardNumber = etCardNumber.getText().toString().replaceAll("\\s+", "");
        String newExpiry = etExpirationDate.getText().toString().trim();
        String newCvv = etCvv.getText().toString().trim();

        if (newName.isEmpty() || newCardNumber.length() < 15 || newExpiry.length() < 4 || newCvv.length() < 3) {
            Toast.makeText(this, "Vui lòng điền đầy đủ và chính xác thông tin.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Tạo đối tượng Map chứa các trường cần cập nhật (Giữ nguyên)
        Map<String, Object> updates = new HashMap<>();
        updates.put("nameOnCard", newName);
        updates.put("cardNumber", newCardNumber);
        updates.put("expirationDate", newExpiry);
        updates.put("cvv", newCvv);

        // 3. GỌI MANAGER SỬ DỤNG CALLBACK (Đã sửa)
        // Gọi hàm mới trong Manager và xử lý kết quả bằng callback
        paymentManager.updateCardByFields(currentCardId, updates, (isSuccess, message) -> {
            if (isSuccess) {
                Toast.makeText(this, "Cập nhật thẻ thành công!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                // Hiển thị message lỗi chi tiết từ Manager
                Log.e("EDIT_CARD", "Update failed: " + message);
                Toast.makeText(this, "Lỗi khi cập nhật thẻ: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void deleteCard() {
        // Hiển thị hộp thoại xác nhận trước khi xóa (nên làm trong ứng dụng thật)
        // new AlertDialog.Builder(this) ... .show();

        // GỌI MANAGER VỚI CALLBACK
        paymentManager.deleteCard(currentCardId, (isSuccess, message) -> {
            if (isSuccess) {
                Toast.makeText(this, "Đã xóa thẻ thành công.", Toast.LENGTH_SHORT).show();
                finish(); // Đóng Activity sau khi xóa thành công
            } else {
                // Hiển thị thông báo lỗi chi tiết từ Manager
                Log.e("DELETE_CARD", "Lỗi xóa thẻ: " + message);
                Toast.makeText(this, "Lỗi khi xóa thẻ: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Hàm hỗ trợ format (Giả định: Không lưu đầy đủ số thẻ trong DB)
    private String formatCardNumberForEdit(String rawNumber) {
        // Tùy thuộc vào cách bạn lưu trữ. Nếu bạn lưu full, trả về full number.
        // Đây chỉ là một placeholder. Nếu bạn chỉ lưu 4 số cuối, bạn sẽ cần ẩn phần còn lại.
        return rawNumber;
    }

    private String formatCardNumberForPreview(String rawNumber) {
        // Định dạng hiển thị thẻ (ví dụ: **** **** **** 1234)
        if (rawNumber == null || rawNumber.length() < 4) return rawNumber;
        return "**** **** **** " + rawNumber.substring(rawNumber.length() - 4);
    }

    // Cần hàm này trong thực tế để xác định loại thẻ (Visa/MC) từ số thẻ
    private String detectCardType(String cardNumber) {
        // Placeholder
        if (cardNumber.startsWith("4")) return "VISA";
        if (cardNumber.startsWith("5")) return "MASTERCARD";
        return "UNKNOWN";
    }
}