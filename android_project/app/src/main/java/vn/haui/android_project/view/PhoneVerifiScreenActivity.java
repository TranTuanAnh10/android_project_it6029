package vn.haui.android_project.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

import vn.haui.android_project.MainActivity;
import vn.haui.android_project.R;
import vn.haui.android_project.service.FirebaseUserManager;

public class PhoneVerifiScreenActivity extends AppCompatActivity {

    EditText otp1, otp2, otp3, otp4, otp5, otp6;
    Button btnConfirm;
    String phoneNumber, verificationId, uid;
    TextView txPhone, tvResend;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.phone_verifi_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.phone_verifi_screen), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mapping();
        mAuth = FirebaseAuth.getInstance();
        Intent intent = getIntent();
        if (intent != null) {
            phoneNumber = intent.getStringExtra("PHONE_NUMBER");
            resendToken = intent.getParcelableExtra("RESEND_TOKEN");
            verificationId = intent.getStringExtra("VERIFICATION_ID");
            uid = intent.getStringExtra("USER_ID");
            txPhone.setText(phoneNumber);
        }
        tvResend.setOnClickListener(v -> {
            startPhoneNumberVerification(phoneNumber);
        });

        btnConfirm.setOnClickListener(v -> {
            String code = otp1.getText().toString().trim() +
                    otp2.getText().toString().trim() +
                    otp3.getText().toString().trim() +
                    otp4.getText().toString().trim() +
                    otp5.getText().toString().trim() +
                    otp6.getText().toString().trim();
            if (verificationId == null) {
                Toast.makeText(this, "Chưa gửi mã OTP", Toast.LENGTH_SHORT).show();
                return;
            }
            verifyOtpAndLinkPhone(verificationId, code);
        });
    }

    private void mapping() {
        otp1 = findViewById(R.id.otp_1);
        otp2 = findViewById(R.id.otp_2);
        otp3 = findViewById(R.id.otp_3);
        otp4 = findViewById(R.id.otp_4);
        otp5 = findViewById(R.id.otp_5);
        otp6 = findViewById(R.id.otp_6);
        btnConfirm = findViewById(R.id.btn_continue_otp);
        txPhone = findViewById(R.id.tv_phone_number);
        tvResend = findViewById(R.id.tv_resend);
        setupOtpInputs();
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(callbacks)
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    Log.d("OTP", "onVerificationCompleted: Tự động xác minh OTP thành công.");
                    linkPhoneCredentialToCurrentUser(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Log.e("OTP", "onVerificationFailed", e);
                    if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        Toast.makeText(PhoneVerifiScreenActivity.this, "Số điện thoại không hợp lệ.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(PhoneVerifiScreenActivity.this, "Xác minh thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCodeSent(@NonNull String verId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    verificationId = verId;
                    resendToken = token;
                    Toast.makeText(PhoneVerifiScreenActivity.this, "Mã OTP đã được gửi đến số điện thoại: " + phoneNumber, Toast.LENGTH_LONG).show();
                }
            };

    private void verifyOtpAndLinkPhone(String verificationId, String code) {
        if (code.length() < 6) {
            Toast.makeText(this, "Vui lòng nhập đủ 6 số OTP.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Tạo credential từ mã OTP người dùng nhập
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        // Gọi hàm chung để liên kết
        linkPhoneCredentialToCurrentUser(credential);
    }

    private void linkPhoneCredentialToCurrentUser(PhoneAuthCredential credential) {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Người dùng chưa đăng nhập. Không thể liên kết.", Toast.LENGTH_SHORT).show();
            // Có thể chuyển về màn hình đăng nhập ở đây
            return;
        }

        btnConfirm.setEnabled(false); // Vô hiệu hóa nút để tránh click nhiều lần
        Toast.makeText(this, "Đang xác minh...", Toast.LENGTH_SHORT).show();

        currentUser.linkWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Xác minh số điện thoại thành công!", Toast.LENGTH_SHORT).show();
                        updatePhoneNumberInFirestore(currentUser.getUid(), phoneNumber);
                        // Chuyển về màn hình chính hoặc màn hình profile
                        Intent intent = new Intent(PhoneVerifiScreenActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Exception e = task.getException();
                        Log.e("OTP", "Link failed", e);
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(this, "Số điện thoại này đã được sử dụng bởi một tài khoản khác.", Toast.LENGTH_LONG).show();
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(this, "Mã OTP không đúng.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Xác minh thất bại!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    btnConfirm.setEnabled(true);
                });
    }

    private void updatePhoneNumberInFirestore(String uid, String phone) {
        FirebaseUserManager userManager = new FirebaseUserManager();
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("phoneNumber", phone);

        userManager.updateUser(uid, updates, aVoid -> {
            Log.d("Firestore", "✅ Đã cập nhật số điện thoại vào Firestore thành công.");
        }, e -> {
            Log.e("Firestore", "❌ Lỗi khi cập nhật số điện thoại vào Firestore.", e);
            // Có thể thêm Toast ở đây để thông báo lỗi cho người dùng nếu cần
        });
    }

    private void setupOtpInputs() {
        EditText[] otpFields = {otp1, otp2, otp3, otp4, otp5, otp6};

        for (int i = 0; i < otpFields.length; i++) {
            final int index = i;
            EditText editText = otpFields[i];

            // Khi focus vào ô
            editText.setOnFocusChangeListener((v, hasFocus) -> {
                if (editText.getText().toString().isEmpty()) {
                    editText.setBackgroundResource(R.drawable.bg_otp); // ô trống
                }
            });
            // Khi nhập ký tự
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < otpFields.length - 1) {
                        editText.setBackgroundResource(R.drawable.bg_otp_filled); // đã nhập
                        otpFields[index + 1].requestFocus();
                    } else if (s.length() == 0 && index > 0) {
                        editText.setBackgroundResource(R.drawable.bg_otp);
                        otpFields[index - 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
        // Focus vào ô đầu tiên khi mở Activity
        otpFields[0].requestFocus();
    }
}
