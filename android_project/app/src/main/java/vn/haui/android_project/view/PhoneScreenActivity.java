package vn.haui.android_project.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

import vn.haui.android_project.MainActivity;
import vn.haui.android_project.R;

public class PhoneScreenActivity extends AppCompatActivity {

    EditText etPhone;
    Button btnContinue;
    private FirebaseAuth mAuth;
    private String verificationId, phone;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.phone_screen);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.phone_screen), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mapping();
        mAuth = FirebaseAuth.getInstance();
        Intent intent = getIntent();
        currentUser = intent.getParcelableExtra("CURRENT_USER");
        btnContinue.setOnClickListener(v -> {
            phone = etPhone.getText().toString().trim();
            if (!phone.startsWith("+")) {
                // bắt buộc E.164, ví dụ Việt Nam +84...
                Toast.makeText(this, "Nhập số theo định dạng +84xxxxxxxx", Toast.LENGTH_SHORT).show();
                return;
            }
            startPhoneNumberVerification(phone);
        });
    }

    private void mapping() {
        etPhone = findViewById(R.id.phone_otp);
        btnContinue = findViewById(R.id.btn_continue_otp);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(PhoneAuthCredential credential) {
                    signInWithPhoneAuthCredential(credential);
                }

                @Override
                public void onVerificationFailed(FirebaseException e) {
                    Toast.makeText(PhoneScreenActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCodeSent(@NonNull String verId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    verificationId = verId;
                    resendToken = token;
                    Intent intent = new Intent(PhoneScreenActivity.this, PhoneVerifiScreenActivity.class);
                    intent.putExtra("VERIFICATION_ID", verificationId);
                    intent.putExtra("RESEND_TOKEN", resendToken);
                    intent.putExtra("PHONE_NUMBER", phone);
                    startActivity(intent);
                    finish();
                }
            };

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

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = task.getResult().getUser();
                        Toast.makeText(this, "Xác minh thành công: " + user.getUid(), Toast.LENGTH_LONG).show();
                        // Lưu user vào Firestore/Realtime, chuyển màn chính...
                    } else {
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(this, "Mã OTP không đúng", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Xác thực thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
