package com.example.bmc_dian;

import androidx.annotation.NonNull;

import com.example.routesdk.NotificationSDK;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        String title = "Notification App";
        String message = "";

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            message = remoteMessage.getNotification().getBody();
        }

        if (remoteMessage.getData().size() > 0) {
            if (remoteMessage.getData().get("title") != null) {
                title = remoteMessage.getData().get("title");
            }
            if (remoteMessage.getData().get("message") != null) {
                message = remoteMessage.getData().get("message");
            }
        }

        if (message != null && !message.isEmpty()) {
            NotificationSDK.showNotification(this, title, message, MainActivity.class, R.drawable.ringing);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
    }
}
