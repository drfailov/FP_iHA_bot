package com.fsoft.vktest;

import android.app.*;
import android.app.ActionBar.Tab;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.*;
import android.os.Process;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * Created by Dr. Failov on 11.11.2014.
 */
public class TabsActivity extends Activity implements Command {
    static public ConsoleView consoleView = null;
    private TabsActivity context = this;
    private Handler handler = new Handler();
    private boolean running = false;
    private ActionBar actionBar = null;
    private Tab logTab = null;
    private int messageBoxes = 0;
    public ApplicationManager applicationManager = null;
    public ArrayList<Command> commands = new ArrayList<>();
    private Timer memoryStateRefreshTimer = null;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        actionBar = getActionBar();
        if (actionBar != null)
            prepareActionBarTabs(actionBar);
        commands.add(new GetLog());
        commands.add(new ShowMessage());
        commands.add(new PowerOff());
        commands.add(new Restart());
        commands.add(new Error());
        commands.add(new OutOfMemory());
    }
    @Override protected void onStart() {
        super.onStart();
        running = true;
        if(BotService.applicationManager == null) {
            log("Запуск сервиса...");
            BotService.tabsActivity = this;
            startService(new Intent(context, BotService.class));
        }
        else {
            log("Сервис уже работает. Подключение к сервису...");
            applicationManager = BotService.applicationManager;
            applicationManager.activity = this;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    log("Подключено. Программа работает.");
                }
            }, 1000);
        }
    }
    @Override protected void onResume() {
        super.onResume();
        if (actionBar != null && logTab != null)
            actionBar.selectTab(logTab);
        if(memoryStateRefreshTimer == null){
            memoryStateRefreshTimer = new Timer();
            memoryStateRefreshTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    refreshMemoryState();
                }
            }, 1000, 1000);
        }
    }
    @Override    protected void onPause() {
        if(memoryStateRefreshTimer != null) {
            memoryStateRefreshTimer.cancel();
            memoryStateRefreshTimer = null;
        }
        super.onPause();
    }
    @Override protected void onStop() {
        super.onStop();
        running = false;
    }
    @Override protected void onDestroy() {
        Log.d("BOT", "ON Destroy");
//        if(applicationManager != null && applicationManager.running) {
//            //Этот флаг сейчас может быть true только в случае когда программа завершилась с ошибкой
//            applicationManager.close();
//            scheduleRestart();//запланируем перезапуск))))
//            sleep(1000);
//            android.os.Process.killProcess(Process.myPid());
//        }
        super.onDestroy();
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if(event.getKeyCode() == KeyEvent.KEYCODE_BACK)
            return true;
        return super.dispatchKeyEvent(event);
    }
    @Override public String process(String text){
        String result = "";
        for (int i = 0; i < commands.size(); i++)
            result += commands.get(i).process(text);
        return result;
    }
    @Override public String getHelp(){
        String result  = "";
        for (int i = 0; i < commands.size(); i++)
            result += commands.get(i).getHelp();
        return result;
    }
    private void prepareActionBarTabs(ActionBar actionBar){
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        {
            String label1 = "Лог";
            logTab = actionBar.newTab();
            logTab.setText(label1);
            TabListener<LogFragment> tl = new TabListener<>(this, label1, LogFragment.class);
            logTab.setTabListener(tl);
            actionBar.addTab(logTab);
        }
        {
            String label2 = "Настройки";
            Tab tab = actionBar.newTab();
            tab.setText(label2);
            TabListener<SettingsFragment> tl2 = new TabListener<>(this, label2, SettingsFragment.class);
            tab.setTabListener(tl2);
            actionBar.addTab(tab);
        }
        {
            String label2 = "Аккаунты";
            Tab tab = actionBar.newTab();
            tab.setText(label2);
            TabListener<AccountListFragment> tl2 = new TabListener<>(this, label2, AccountListFragment.class);
            tab.setTabListener(tl2);
            actionBar.addTab(tab);
        }
        {
            String label2 = "Стены";
            Tab tab = actionBar.newTab();
            tab.setText(label2);
            TabListener<WallListFragment> tl2 = new TabListener<>(this, label2, WallListFragment.class);
            tab.setTabListener(tl2);
            actionBar.addTab(tab);
        }
        {
            String label2 = "Доверенные";
            Tab tab = actionBar.newTab();
            tab.setText(label2);
            TabListener<AllowListFragment> tl2 = new TabListener<>(this, label2, AllowListFragment.class);
            tab.setTabListener(tl2);
            actionBar.addTab(tab);
        }
        {
            String label2 = "Игнорируемые";
            Tab tab = actionBar.newTab();
            tab.setText(label2);
            TabListener<IgnorListFragment> tl2 = new TabListener<>(this, label2, IgnorListFragment.class);
            tab.setTabListener(tl2);
            actionBar.addTab(tab);
        }
        {
            String label2 = "Учителя";
            Tab tab = actionBar.newTab();
            tab.setText(label2);
            TabListener<TeacherListFragment> tl2 = new TabListener<>(this, label2, TeacherListFragment.class);
            tab.setTabListener(tl2);
            actionBar.addTab(tab);
        }
        actionBar.selectTab(logTab);
    }
    private void refreshMemoryState(){
        String name = getResources().getString(R.string.app_name);
        long maxMemory = Runtime.getRuntime().maxMemory();
        long curMemory = Runtime.getRuntime().totalMemory();
        final String title = name + " (RAM " + ((curMemory*100L)/maxMemory) + "%)";
        handler.post(new Runnable() {
            @Override
            public void run() {
                setTitle(title);
            }
        });
    }
    public boolean isRunning(){
        return running;
    }
    public  void sleep(int ms){
        try {
            Thread.sleep(ms);
        }
        catch (Exception e){}
    }
    public void scheduleRestart(){
        Log.d("BOT", "Планирование перезапуска...");
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(context, TabsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 15000, pendingIntent);
    }
    public void log(String text){
        if(consoleView != null)
            consoleView.log(text);
    }
    public void messageBox(final String text){
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (context.isRunning() && messageBoxes < 5){
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            messageBoxes --;
                        }
                    });
                    builder.setMessage(text);
                    AlertDialog alertDialog = builder.show();
                    Timer timer = new Timer();
                    TimerTask timerTask = new TimerTask() {
                        AlertDialog alertDialog = null;
                        @Override
                        public void run() {
                            if(alertDialog != null)
                                alertDialog.dismiss();
                        }

                        @Override
                        public boolean equals(Object o) {
                            if(o.getClass() == AlertDialog.class)
                                alertDialog = (AlertDialog) o;
                            return super.equals(o);
                        }
                    };
                    timerTask.equals(alertDialog);
                    timer.schedule(timerTask, 60000);
                    messageBoxes ++;
                }
            }
        });
    }
    public void turnoff(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                applicationManager.close();
                sleep(1000);
                stopService(new Intent(context, BotService.class));
                sleep(1000);
                android.os.Process.killProcess(Process.myPid());
            }
        }).start();
    }
    public void restart(){
        scheduleRestart();
        turnoff();
    }

    private class GetLog implements Command{
        @Override
        public String getHelp() {
            return "[ Получить весь лог (Осторожно, может сработать система защиты!) ]\n" +
                    "---| botcmd getlog \n\n";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("getlog")){
                return consoleView.log;
            }
            return "";
        }
    }
    private class ShowMessage implements Command{
        @Override
        public String getHelp() {
            return "[ Показать на экране устройства сообщение ]\n" +
                    "---| botcmd messagebox (text) \n\n";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("messagebox")){
                String text = commandParser.getText();
                messageBox(text);
                return "Сообщение "+text+" показано.";
            }
            return "";
        }
    }
    private class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private Fragment mFragment;
        private final Activity mActivity;
        private final String mTag;
        private final Class<T> mClass;

        /**
         * Constructor used each time a new tab is created.
         *
         * @param activity
         *            The host Activity, used to instantiate the fragment
         * @param tag
         *            The identifier tag for the fragment
         * @param clz
         *            The fragment's Class, used to instantiate the fragment
         */
        public TabListener(Activity activity, String tag, Class<T> clz) {
            mActivity = activity;
            mTag = tag;
            mClass = clz;
        }
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            // Check if the fragment is already initialized
            if (mFragment == null) {
                // If not, instantiate and add it to the activity
                mFragment = Fragment.instantiate(mActivity, mClass.getName());
                ft.add(android.R.id.content, mFragment, mTag);
            } else {
                // If it exists, simply attach it in order to show it
                ft.show(mFragment);
                mFragment.onResume();
            }
        }
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
                // Detach the fragment, because another one is being attached
                mFragment.onPause();
                ft.hide(mFragment);
            }
        }
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            // User selected the already selected tab. Usually do nothing.
        }
    }
    private class PowerOff implements Command{
        @Override
        public String getHelp() {
            return "[ Завершить работу программы ]\n" +
                    "---| botcmd turnoff \n\n";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("turnoff")) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        turnoff();
                    }
                }, 5000);
                return "Полное завершение программы через 5 секунд.";
            }
            return "";
        }
    }
    private class Restart implements Command{
        @Override
        public String getHelp() {
            return "[ Перезапустить программу ]\n" +
                    "---| botcmd restart \n\n";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("restart")) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        restart();
                    }
                }, 5000);
                return "Перезапуск программы через 5 секунд. Повторная загрузка программы займет около минуты.";
            }
            return "";
        }
    }
    private class Error implements Command{
        @Override
        public String getHelp() {
            return "[ Вызвать ошибку в программе (используется разработчиком для отладки) ]\n" +
                    "---| botcmd error \n\n";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("error")) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        //переполнение стека гарантировано
                        run();
                    }
                }, 5000);
                return "Вылет программы из-за переполнения стека через 5 секунд.";
            }
            return "";
        }
    }
    private class OutOfMemory implements Command{
        @Override
        public String getHelp() {
            return "[ Вызвать переполнение памяти в программе (используется разработчиком для отладки) ]\n" +
                    "---| botcmd outofmemory \n\n";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("outofmemory")) {
                new Timer().schedule(new TimerTask() {
                    ArrayList<Bitmap> test = new ArrayList<>();
                    @Override
                    public void run() {
                        int filled = 0;
                        while (true){
                            filled += 3;
                            log("Занято "+filled+" MB");
                            for (int i = 0; i < 3; i++) {
                                test.add(Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888));
                            }
                            sleep(500);
                        }
                    }
                }, 5000);
                return "Вылет программы из-за переполнения памяти через 5 секунд.";
            }
            return "";
        }
    }
}
