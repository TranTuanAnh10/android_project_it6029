package vn.haui.android_project.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import vn.haui.android_project.R;
import vn.haui.android_project.service.FirebaseUserManager;

public class ProfileFragment extends Fragment {

    Button btn_logout;
    ImageView imgUser;
    TextView tvUserName, tvUserEmail;
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

        btn_logout.setOnClickListener(v -> logoutUser());
    }
    public void logoutUser() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signOut();

        Intent intent = new Intent(view.getContext(), LoginScreenActivity.class);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
    private void mapping(){
        btn_logout = view.findViewById(R.id.btn_logout);
        imgUser = view.findViewById(R.id.iv_avatar);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserEmail =view.findViewById(R.id.tv_user_email);
    }
}