package vn.haui.android_project.view;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import vn.haui.android_project.R;

public class CustomLocationDialog extends DialogFragment {

    public static final String TAG = "CustomLocationDialog";

    // --- 1. INTERFACE: Giao tiếp ngược với Activity/Fragment ---
    public interface LocationDialogListener {
        /**
         * Được gọi khi người dùng chọn một tùy chọn trong dialog.
         * @param isOkSelected: true nếu chọn 'OK', false nếu chọn 'No, thanks'.
         */
        void onLocationOptionChosen(boolean isOkSelected);
    }

    private LocationDialogListener listener;

    // --- 2. GẮN LISTENER ---
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            // Gán Activity (hoặc Fragment nếu bạn dùng getParentFragment()) làm listener
            listener = (LocationDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement LocationDialogListener");
        }
    }

    // --- 3. NHÚNG LAYOUT XML ---
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Ánh xạ layout tùy chỉnh
        return inflater.inflate(R.layout.dialog_custom_location, container, false);
    }

    // --- 4. XỬ LÝ SỰ KIỆN NÚT ---
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Loại bỏ viền và tiêu đề mặc định
        if (getDialog() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Button btnNegative = view.findViewById(R.id.btn_negative);
        btnNegative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onLocationOptionChosen(false); // No, thanks
                }
                dismiss(); // Đóng dialog
            }
        });

        Button btnPositive = view.findViewById(R.id.btn_positive);
        btnPositive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onLocationOptionChosen(true); // OK
                }
                dismiss(); // Đóng dialog
            }
        });
    }

    // --- 5. HỦY GÁN LISTENER (để tránh rò rỉ bộ nhớ) ---
    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}
