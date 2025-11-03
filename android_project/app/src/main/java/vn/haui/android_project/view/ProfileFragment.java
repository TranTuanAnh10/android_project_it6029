package vn.haui.android_project.view;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.List;

import vn.haui.android_project.MainActivity;
import vn.haui.android_project.R;
import vn.haui.android_project.entity.DeviceToken;
import vn.haui.android_project.entity.UserEntity;
import vn.haui.android_project.enums.DatabaseTable;
import vn.haui.android_project.services.FirebaseUserManager;

public class ProfileFragment extends Fragment {

    Button btn_logout;
    ImageView imgUser;
    ImageButton editProfile;
    TextView tvUserName, tvUserEmail,pointYumyard,saveRecipients,paymentMethods;
    View view;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_profile, container, false);
        mapping();
        init();
        return view;
    }
    private void init() {
        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (authUser == null) return;

        FirebaseUserManager userManager = new FirebaseUserManager();
        userManager.getUserByUid(authUser.getUid(), userData -> {
            // ✅ Lấy dữ liệu Firestore
            String name = (String) userData.getOrDefault("name", authUser.getDisplayName());
            String email = (String) userData.getOrDefault("email", authUser.getEmail());
            String avatarUrl = (String) userData.getOrDefault("avatarUrl", authUser.getPhotoUrl() != null ? authUser.getPhotoUrl().toString() : null);

            tvUserName.setText(name != null ? name : "User");
            tvUserEmail.setText(email != null ? email : "");

            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(this)
                        .load(avatarUrl)
                        .placeholder(R.drawable.ic_user)
                        .into(imgUser);
            } else {
                imgUser.setImageResource(R.drawable.ic_user);
            }

        }, error -> {
            // ⚠️ Nếu Firestore chưa có dữ liệu (lần đầu đăng nhập), fallback sang FirebaseAuth
            tvUserName.setText(authUser.getDisplayName() != null ? authUser.getDisplayName() : "User");
            tvUserEmail.setText(authUser.getEmail() != null ? authUser.getEmail() : "");

            Uri photoUrl = authUser.getPhotoUrl();
            if (photoUrl != null) {
                Glide.with(this)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_user)
                        .into(imgUser);
            } else {
                imgUser.setImageResource(R.drawable.ic_user);
            }
        });
        editProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileFragment.this.getContext(), EditProfileScreenActivity.class);
            startActivity(intent);
        });
        saveRecipients.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileFragment.this.getContext(), ChooseRecipientActivity.class);
            startActivity(intent);
        });
        btn_logout.setOnClickListener(v -> logoutUser(authUser.getUid()));
        pointYumyard.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileFragment.this.getContext(), OrderTrackingActivity.class);
            startActivity(intent);
        });
        paymentMethods.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileFragment.this.getContext(), PaymentMethodsActivity.class);
            startActivity(intent);
        });
    }
    public void logoutUser(String userId) {
        logoutAndRemoveToken(userId);
        Intent intent = new Intent(view.getContext(), LoginScreenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
    private void mapping(){
        btn_logout = view.findViewById(R.id.btn_logout);
        imgUser = view.findViewById(R.id.iv_avatar);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserEmail =view.findViewById(R.id.tv_user_email);
        editProfile=view.findViewById(R.id.iv_edit_profile);
        pointYumyard=view.findViewById(R.id.point_yumyard);
        saveRecipients=view.findViewById(R.id.save_recipients);
        paymentMethods=view.findViewById(R.id.payment_methods);
    }
    private void logoutAndRemoveToken(String userId) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(currentToken -> {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    DocumentReference userRef = db.collection(DatabaseTable.USERS.getValue()).document(userId);

                    userRef.get().addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            UserEntity user = snapshot.toObject(UserEntity.class);
                            if (user != null && user.getTokens() != null) {
                                // lọc ra danh sách token còn lại (xoá token hiện tại)
                                List<DeviceToken> updatedTokens = new ArrayList<>();
                                for (DeviceToken t : user.getTokens()) {
                                    if (!t.getToken().equals(currentToken)) {
                                        updatedTokens.add(t);
                                    }
                                }

                                // cập nhật lại danh sách token
                                userRef.update("tokens", updatedTokens)
                                        .addOnSuccessListener(a -> Log.d("Logout", "Đã xoá token thành công"))
                                        .addOnFailureListener(e -> Log.e("Logout", "Lỗi khi xoá token", e));
                            }
                        }
                    });

                    // Xoá session Firebase (nếu bạn dùng Firebase Auth)
                    FirebaseAuth.getInstance().signOut();
                });
    }

}