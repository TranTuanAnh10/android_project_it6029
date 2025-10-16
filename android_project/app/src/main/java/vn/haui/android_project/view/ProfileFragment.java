package vn.haui.android_project.view;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import vn.haui.android_project.MainActivity;
import vn.haui.android_project.R;

public class ProfileFragment extends Fragment {

    Button btn_logout;
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
        btn_logout.setOnClickListener(v -> {
            logoutUser();
        });
    }
    public void logoutUser() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        mAuth.signOut();

        Intent intent = new Intent(view.getContext(), SplashScreenActivity.class);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
    private void mapping(){
        btn_logout = view.findViewById(R.id.btn_logout);
    }
}