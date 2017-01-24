package com.fsoft.vktest;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

/**
 * console
 * Created by Dr. Failov on 05.08.2014.
 */
public class ConsoleView extends ScrollView {
    private final Object syncObject = new Object();
    TextView textView;
    String log = "Начало лога.";
    Timer scrollDownTimer;
    Handler handler;
    boolean singleMode = false;

    public ConsoleView(Context context) {
        super(context);
        init();
    }
    public ConsoleView(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
        init();
    }
    public ConsoleView(Context context, AttributeSet attributeSet, int style){
        super(context, attributeSet, style);
        init();
    }

    private void init(){
        handler = new Handler();
        textView = new TextView(getContext());
        textView.setTextColor(Color.rgb(30, 255, 100));
        textView.setTextSize(11);
        addView(textView);
    }
    private void sleep(int mili){
        try{
            Thread.sleep(mili);
        }
        catch (Exception e){}
    }
    public void log(String text){
        Log.d("BOT", text);
        synchronized (syncObject) {
            if (log.length() > 11000)
                log = log.substring(log.length() - 9000);
            log = log + "\n" + text;
        }
        if(isShown()) {
            if (!singleMode) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(log);
                        scrollDown();
                    }
                });
            } else {
                final String txt = text;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(txt);
                    }
                });
            }
        }
    }
    public void clearLastLine(){
        if (!singleMode) {
            synchronized (syncObject) {
                if (log.length() > 11000)
                    log = log.substring(log.length() - 9000);
                int cropTo = 0;
                for (int i = log.length() - 1; i >= 0; i--) {
                    if (log.charAt(i) == '\n') {
                        cropTo = i;
                        break;
                    }
                }
                log = log.substring(0, cropTo);
            }
        }
    }
    public  void scrollDown(){
        if(scrollDownTimer != null)
            scrollDownTimer.cancel();
        scrollDownTimer = new Timer();
        scrollDownTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                post(new Runnable() {
                    @Override
                    public void run() {
                        fullScroll(FOCUS_DOWN);
                    }
                });
            }
        }, 50);
    }
    public void changeMode(){
        singleMode = !singleMode;
        changeMode(singleMode);
    }
    public void changeMode(boolean single){
        singleMode = single;
        Toast.makeText(getContext(), "SingleMode = " + singleMode, Toast.LENGTH_SHORT).show();
        if(singleMode){
            textView.setGravity(Gravity.CENTER);
            textView.setPadding(10, 300, 10, 10);
            textView.setTextSize(16);
            textView.setText("single mode");
        }
        else{
            textView.setGravity(Gravity.LEFT);
            textView.setPadding(10, 10, 10, 10);
            textView.setTextSize(10);
            textView.setText("log mode");
        }
    }
    static public String getLoadingBar(int totalLength, int percentage){
        int filled = (int)((double)totalLength * ((double)percentage / 100d));
        int notFilled = totalLength - filled;
        String result = "|";
        for (int i = 0; i < filled; i++)
            result += "█";
        for (int i = 0; i < notFilled; i++)
            result += "─";
        result += "|";
        return result;
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
    }
}
