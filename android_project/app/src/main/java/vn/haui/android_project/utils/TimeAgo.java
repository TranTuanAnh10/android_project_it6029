package vn.haui.android_project.utils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Lớp tiện ích để chuyển đổi một đối tượng Date thành một chuỗi thời gian tương đối
 * ví dụ: "5 phút trước", "1 giờ trước", "Hôm qua".
 */
public class TimeAgo {

    /**
     * Tính toán và trả về chuỗi thời gian tương đối so với thời điểm hiện tại.
     *
     * @param date Đối tượng Date cần chuyển đổi.
     * @return Một chuỗi đại diện cho khoảng thời gian đã trôi qua (ví dụ: "vừa xong", "10 phút trước").
     */
    public static String getTimeAgo(Date date) {
        if (date == null) {
            return ""; // Trả về chuỗi rỗng nếu ngày tháng không hợp lệ
        }

        // Lấy thời gian hiện tại
        long currentTime = System.currentTimeMillis();
        // Lấy thời gian của thông báo
        long pastTime = date.getTime();

        // Tính toán khoảng thời gian đã trôi qua (tính bằng mili giây)
        long timeDifference = currentTime - pastTime;

        // Chuyển đổi sang các đơn vị khác nhau
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeDifference);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeDifference);
        long hours = TimeUnit.MILLISECONDS.toHours(timeDifference);
        long days = TimeUnit.MILLISECONDS.toDays(timeDifference);

        // Trả về chuỗi phù hợp
        if (seconds < 60) {
            return "vừa xong";
        } else if (minutes < 60) {
            return minutes + " phút trước";
        } else if (hours < 24) {
            return hours + " giờ trước";
        } else if (days < 7) {
            return days + " ngày trước";
        } else {
            // Nếu lâu hơn một tuần, hiển thị ngày tháng đầy đủ cho rõ ràng
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            return sdf.format(date);
        }
    }
}
