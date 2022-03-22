package com.fsoft.ihabot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class BotService extends Service {
    //static public ApplicationManager applicationManager = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Intent notificationIntent = new Intent(this, BotActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this.getApplicationContext(),
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT // setting the mutability flag
        );

        String channelId = "BotRunning";

        NotificationChannel chan = new NotificationChannel(channelId, getText(R.string.notification_channel_name), NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(chan);

        Notification notification = new Notification.Builder(this, channelId)
                    .setContentTitle(getText(R.string.notification_title))
                    .setContentText(getText(R.string.notification_content))
                    .setSmallIcon(R.drawable.bot_noti)
                    .setContentIntent(pendingIntent)
                    .setTicker(getText(R.string.notification_content))
                    .build();


        // Notification ID cannot be 0.
        int ONGOING_NOTIFICATION_ID = 1;
        startForeground(ONGOING_NOTIFICATION_ID, notification);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {



        } catch (Exception e) {
            Log.d("iHA bot", "Error starting service: " + e.getMessage());
            e.printStackTrace();
        }
//            Log.d("BOT", "Планирование перезапуска...");
//            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
//            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
//            alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 8000, pendingIntent);
//            stopForeground(true);
//            stopSelf();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("BOT", "ON SERVICE Destroy");
//        if (applicationManager != null && applicationManager.isRunning()) {
//            applicationManager.stop();
//        }
    }
}
