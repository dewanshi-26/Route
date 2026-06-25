package com.example.locsdk;

public class SDKConfig
{

    private float radius = 50f;
    private long interval = 30000;
    private long fastestInterval = 10000;
    private String notificationTitle = "Location Service";
    private String notificationContent = "Logging Active";
    private int notificationIcon = -1;
    private int notificationColor = -1;
    private int notificationImage = -1;

    public float getRadius() {
        return radius;
    }

    public long getInterval() {
        return interval;
    }

    public long getFastestInterval() {
        return fastestInterval;
    }

    public String getNotificationTitle() {
        return notificationTitle;
    }

    public String getNotificationContent() {
        return notificationContent;
    }

    public int getNotificationIcon() {
        return notificationIcon;
    }

    public int getNotificationColor() {
        return notificationColor;
    }

    public int getNotificationImage() {
        return notificationImage;
    }

    public static class Builder {

        private final SDKConfig config = new SDKConfig();

        public Builder setRadius(float radius) {
            config.radius = radius;
            return this;
        }

        public Builder setInterval(long interval) {
            config.interval = interval;
            return this;
        }

        public Builder setFastestInterval(long fastestInterval) {
            config.fastestInterval = fastestInterval;
            return this;
        }

        public Builder setNotificationTitle(String title) {
            config.notificationTitle = title;
            return this;
        }

        public Builder setNotificationContent(String content) {
            config.notificationContent = content;
            return this;
        }

        public Builder setNotificationIcon(int iconResId) {
            config.notificationIcon = iconResId;
            return this;
        }

        public Builder setNotificationColor(int colorInt) {
            config.notificationColor = colorInt;
            return this;
        }

        public Builder setNotificationImage(int imageResId) {
            config.notificationImage = imageResId;
            return this;
        }

        public SDKConfig build() {
            return config;
        }
    }

}
