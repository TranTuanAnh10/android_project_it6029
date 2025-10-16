package vn.haui.android_project.view;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.haui.android_project.R;

public class HomeFragment extends Fragment {

    public HomeFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

//        Button btnShop = view.findViewById(R.id.btn_open_shop);
//        btnShop.setOnClickListener(v -> {
//            Intent intent = new Intent(getActivity(), ShopDetailActivity.class);
//            startActivity(intent);
//        });

        return view;
    }
}