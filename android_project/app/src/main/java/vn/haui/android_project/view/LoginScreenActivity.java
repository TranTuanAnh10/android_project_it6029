package vn.haui.android_project.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;

import vn.haui.android_project.MainActivity;
import vn.haui.android_project.R;
import vn.haui.android_project.services.FirebaseUserManager;

public class LoginScreenActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin, btnGoogle;
    FirebaseAuth mAuth;
    GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.login_screen);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_screen), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        // ✅ Cấu hình Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Lấy từ google-services.json
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mapping();

        // Login bằng email
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin.", Toast.LENGTH_SHORT).show();
                return;
            }

            signIn(email, password);
        });

        // ✅ Login bằng Google
        btnGoogle.setOnClickListener(v -> signInWithGoogle());
    }

    private void mapping() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_continue);
        btnGoogle = findViewById(R.id.btnGoogle);
    }

    private void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            FirebaseUserManager userManager = new FirebaseUserManager();
                            userManager.saveOrUpdateUser(user);
                            gotoMain(user);
                        }
                    } else {
                        Toast.makeText(LoginScreenActivity.this,
                                "Đăng nhập thất bại: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ✅ Hàm đăng nhập Google
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, "Đăng nhập Google thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            FirebaseUserManager userManager = new FirebaseUserManager();
                            userManager.saveOrUpdateUser(user);
                            gotoMain(user);
                        }
                    } else {
                        Toast.makeText(this, "Xác thực thất bại.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void gotoMain(@NonNull FirebaseUser user) {
        FirebaseUserManager userManager = new FirebaseUserManager();
        userManager.getUserByUid(user.getUid(), userData -> {
            // Lấy dữ liệu từ Firestore hoặc từ Auth nếu Firestore không có
            String phone = (String) userData.getOrDefault("phoneNumber", "");
            if (phone.isBlank()){
                // chua co std thi day den nhap std
                Intent intent = new Intent(LoginScreenActivity.this, PhoneScreenActivity.class);
                startActivity(intent);
                finish();
            }else {
                // da co std thi day den main
                Intent intent = new Intent(LoginScreenActivity.this, MainActivity.class);
                intent.putExtra("USER_ID", user.getUid());
                intent.putExtra("USER_EMAIL", user.getEmail());
                intent.putExtra("USER_NAME", user.getDisplayName());
                if (user.getPhotoUrl() != null)
                    intent.putExtra("USER_PHOTO", user.getPhotoUrl().toString());
                startActivity(intent);
                finish();
            }
        }, error -> {
            Toast.makeText(this, "Không tìm thấy thông tin tài khoản", Toast.LENGTH_SHORT).show();
        });
    }
}
