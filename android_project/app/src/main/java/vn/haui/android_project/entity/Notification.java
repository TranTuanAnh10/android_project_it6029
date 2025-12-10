package vn.haui.android_project.entity;

public class Notification {
    private String title;
    private String subtitle;
    private String time;
    private boolean isUnread;
    private int iconResId;
    private int iconColorResId;
    private boolean isGroupHeader;
    private String headerTitle;
    public Notification(String headerTitle) {
        this.headerTitle = headerTitle;
        this.isGroupHeader = true;
    }

    // Constructor cho Item Thông báo
    public Notification(String title, String subtitle, String time, boolean isUnread, int iconResId, int iconColorResId) {
        this.title = title;
        this.subtitle = subtitle;
        this.time = time;
        this.isUnread = isUnread;
        this.iconResId = iconResId;
        this.iconColorResId = iconColorResId;
        this.isGroupHeader = false;
    }

    // Getters
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getTime() { return time; }
    public boolean isUnread() { return isUnread; }
    public int getIconResId() { return iconResId; }
    public int getIconColorResId() { return iconColorResId; }
    public boolean isGroupHeader() { return isGroupHeader; }
    public String getHeaderTitle() { return headerTitle; }

}
