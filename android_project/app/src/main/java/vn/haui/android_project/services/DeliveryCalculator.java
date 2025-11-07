package vn.haui.android_project.services;

import android.location.Location;
import java.lang.Math;

public class DeliveryCalculator {

    private static final double ROAD_FACTOR = 1.3;
    private static final double AVERAGE_SPEED_KMH = 30.0;

    public static double calculateEstimatedTime(
            double startLat,
            double startLon,
            double endLat,
            double endLon
    ) {
        float[] results = new float[1];
        Location.distanceBetween(startLat, startLon, endLat, endLon, results);
        double distanceStraightKm = results[0] / 1000.0;
        double estimatedDistanceKm = distanceStraightKm * ROAD_FACTOR;
        double timeHours = estimatedDistanceKm / AVERAGE_SPEED_KMH;
        double timeMinutes = timeHours * 60.0;
        return timeMinutes;
    }

    public static String formatTime(double timeMinutes) {
        int totalMinutes = (int) Math.round(timeMinutes);
        if (totalMinutes < 1) return "Ngay lập tức";
        if (totalMinutes < 60) return totalMinutes + " phút";
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        if (minutes == 0) {
            return hours + " giờ";
        } else {
            return hours + " giờ " + minutes + " phút";
        }
    }
}
