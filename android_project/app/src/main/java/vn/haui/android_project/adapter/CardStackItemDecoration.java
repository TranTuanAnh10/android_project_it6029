package vn.haui.android_project.adapter;

// File: CardStackItemDecoration.java

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class CardStackItemDecoration extends RecyclerView.ItemDecoration {

    // Chiều cao chồng lấp (chênh lệch giữa các thẻ) tính bằng pixel
    private final int overlapHeightPx;

    public CardStackItemDecoration(Context context, int overlapDp) {
        // Chuyển đổi DP sang PX (ví dụ: 100dp)
        float density = context.getResources().getDisplayMetrics().density;
        overlapHeightPx = (int) (overlapDp * density);
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {

        super.getItemOffsets(outRect, view, parent, state);

        int position = parent.getChildAdapterPosition(view);
        int itemCount = parent.getAdapter().getItemCount();

        if (position > 0) {
            // Đặt offset âm ở trên để thẻ hiện tại dịch chuyển lên che thẻ trước đó
            // Ví dụ: -100px
            outRect.top = -overlapHeightPx;
        }

        // Chỉ thêm khoảng trống lớn phía dưới cho thẻ cuối cùng để thẻ đầu tiên
        // có đủ không gian để hiển thị phần bị chồng lên.
        if (position == itemCount - 1 && itemCount > 1) {
            // Thêm khoảng trống bằng tổng chiều cao chồng lấp của các thẻ trước đó
            outRect.bottom = (itemCount - 1) * overlapHeightPx;
        }
    }
}