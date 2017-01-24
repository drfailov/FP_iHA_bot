package com.fsoft.vktest;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.util.Log;

import java.util.ServiceConfigurationError;

/**
 *
 * Created by Dr. Failov on 28.12.2014.
 */
public class BotService extends Service {
    static public TabsActivity tabsActivity = null;
    static public ApplicationManager applicationManager = null;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("BOT", "ON SERVICE Destroy");
        if(applicationManager!= null && applicationManager.running) {
            applicationManager.close();
            applicationManager.activity.scheduleRestart();//запланируем перезапуск))))
            applicationManager.activity.sleep(1000);
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(tabsActivity != null) {
            if(tabsActivity.applicationManager == null) {
                applicationManager = new ApplicationManager(tabsActivity, "bot");
                tabsActivity.applicationManager = applicationManager;
            }
            applicationManager.load();
            tabsActivity.log("Started.");
            tabsActivity = null;

            Notification notification = new Notification.Builder(getApplicationContext())
                    .setContentTitle(getApplicationContext().getResources().getString(R.string.app_name) + " работает")
                    .setContentText(getApplicationContext().getResources().getString(R.string.name))
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .getNotification();
            notification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 1, new Intent(getApplicationContext(), TabsActivity.class), 0);
            startForeground(1, notification);
        }
        else {
            Log.d("BOT", "Планирование перезапуска...");
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent intent = new Intent(getApplicationContext(), TabsActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
            alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 8000, pendingIntent);
            stopForeground(true);
            stopSelf();
        }
    }
}
