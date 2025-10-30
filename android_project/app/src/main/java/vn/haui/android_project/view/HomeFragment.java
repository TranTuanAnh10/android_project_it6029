package vn.haui.android_project.view;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.CategoryItem;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    public HomeFragment() { }

    private static final String COLLECTION_CATEGORYS = "categorys";
    View view;
    private FirebaseFirestore db;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        this.view = view;
        db = FirebaseFirestore.getInstance();
        loadAllHomeData();
        return view;
    }
    private void loadAllHomeData() {

        db.collection(COLLECTION_CATEGORYS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<CategoryItem> topPicksList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            CategoryItem item = document.toObject(CategoryItem.class);
                            item.setId(document.getId());
                        }
                        populateCuisineLayout(topPicksList);
                        Log.d(TAG, "Tải Top Picks thành công: " + topPicksList.size() + " mục");

                    } else {
                        Log.w(TAG, "Lỗi khi tải Top Picks", task.getException());
                    }
                });
    }

    public void populateCuisineLayout(List<CategoryItem> items) {
        LinearLayout parentLayout = view.findViewById(R.id.layoutCuisines);
        parentLayout.removeAllViews();

        Context context = view.getContext();
        parentLayout.setWeightSum(items.size());

        int imageSizePx = dpToPx(context, 48);
        int textMarginTopPx = dpToPx(context, 8);

        for (CategoryItem item : items) {
            LinearLayout itemLayout = new LinearLayout(context);

            LinearLayout.LayoutParams itemLayoutParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
            );
            itemLayout.setLayoutParams(itemLayoutParams);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setGravity(Gravity.CENTER);

            ShapeableImageView imageView = new ShapeableImageView(context);
            LinearLayout.LayoutParams imageLayoutParams = new LinearLayout.LayoutParams(
                    imageSizePx,
                    imageSizePx
            );
            imageView.setLayoutParams(imageLayoutParams);
            String drawableName = item.getImage();
            int drawableId = context.getResources().getIdentifier(
                    drawableName,
                    "drawable",
                    context.getPackageName()
            );
            imageView.setImageResource(drawableId);

            MaterialTextView textView = new MaterialTextView(context);
            LinearLayout.LayoutParams textLayoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            textLayoutParams.topMargin = textMarginTopPx;
            textView.setLayoutParams(textLayoutParams);
            textView.setText(item.getName());

            // Thiết lập giao diện văn bản và màu sắc
            // Lấy từ theme (cách tốt nhất)
//            textView.setTextAppearance(R..attr.textAppearanceBodyMedium); // Sử dụng style chuẩn
            // Hoặc textView.setTextAppearance(R.attr.textAppearanceBodyMedium); // Nếu dùng Material Theme

            // Lấy màu từ theme
            // textView.setTextColor(MaterialColors.getColor(context, R.attr.colorOnSurfaceVariant, Color.GRAY));

            // Hoặc set cứng (không khuyến khích)
            // textView.setTextColor(Color.parseColor("#..."));

            itemLayout.addView(imageView);
            itemLayout.addView(textView);
            final CategoryItem currentItem = item;
            itemLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onCuisineItemClicked(currentItem);
                }
            });
            parentLayout.addView(itemLayout);
        }
    }
    private void onCuisineItemClicked(CategoryItem item) {
        Toast.makeText(requireContext(), "Bạn đã chọn: " + item.getName(), Toast.LENGTH_SHORT).show();

        // Hoặc: điều hướng sang fragment/activity khác
        // ví dụ: findNavController().navigate(R.id.action_to_detailsFragment);
    }
    private static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }
}