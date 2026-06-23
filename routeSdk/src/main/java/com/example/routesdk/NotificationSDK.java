package com.example.routesdk;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class NotificationSDK {

    private static final String CHANNEL_ID = "sdk_notification_channel";

    public static void init(Context context) {
        createNotificationChannel(context);
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "SDK Notifications",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Important notifications from SDK");
                channel.enableLights(true);
                channel.setLightColor(Color.BLUE);
                channel.enableVibration(true);
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void showNotification(Context context, String title, String message, Class<?> targetActivity, int smallIconResId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(context, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Custom Layout for Collapsed View
        RemoteViews collapsedView = new RemoteViews(context.getPackageName(), R.layout.sdk_custom_notification);
        collapsedView.setTextViewText(R.id.txtTitle, title);
        collapsedView.setTextViewText(R.id.txtMessage, message);
        collapsedView.setTextViewText(R.id.txtTime, "Just now");
        if (smallIconResId != 0) {
            collapsedView.setImageViewResource(R.id.imgLogo, smallIconResId);
        }

        // Custom Layout for Expanded View
        RemoteViews expandedView = new RemoteViews(context.getPackageName(), R.layout.sdk_custom_notification_expanded);
        expandedView.setTextViewText(R.id.txtTitleExpanded, title);
        expandedView.setTextViewText(R.id.txtMessageExpanded, message);
        expandedView.setTextViewText(R.id.txtTimeExpanded, "Just now");
        if (smallIconResId != 0) {
            expandedView.setImageViewResource(R.id.imgLogoExpanded, smallIconResId);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(smallIconResId != 0 ? smallIconResId : android.R.drawable.ic_dialog_info)
                .setCustomContentView(collapsedView)
                .setCustomBigContentView(expandedView)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setColor(ContextCompat.getColor(context, R.color.sdk_primaryColor))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (notificationManager != null) {
            notificationManager.notify((int) (System.currentTimeMillis() & 0xfffffff), builder.build());
        }
    }
}
