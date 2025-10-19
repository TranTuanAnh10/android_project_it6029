package vn.haui.android_project.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import vn.haui.android_project.MainActivity;
import vn.haui.android_project.R;

public class ProfileFragment extends Fragment {

    Button btn_logout;
    ImageView imgUser;
    TextView tvUserName,tvMembership;
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
    private void init(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            tvUserName.setText(user.getDisplayName() != null ? user.getDisplayName() : "User");

            Uri photoUrl = user.getPhotoUrl();
            if (photoUrl != null) {
                Glide.with(this)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_user)
                        .into(imgUser);
            }
            tvMembership.setText(user.getEmail() != null ? user.getEmail() : "");
        }
        btn_logout.setOnClickListener(v -> {
            logoutUser();
        });
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
        tvMembership=view.findViewById(R.id.tv_membership);
    }
}