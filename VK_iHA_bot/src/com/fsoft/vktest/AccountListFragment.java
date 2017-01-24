package com.fsoft.vktest;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * Created by Dr. Failov on 11.11.2014.
 */
public class AccountListFragment extends Fragment {
    Button addAccountButton = null;
    LinearLayout linearLayoutAccounts = null;
    TabsActivity tabsActivity = null;
    ApplicationManager applicationManager = null;
    Handler handler = null;
    Timer timer = null;
    int lastListSize = 0;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context context = inflater.getContext();
        handler = new Handler();
        if(context.getClass().equals(TabsActivity.class))
            tabsActivity = (TabsActivity)context;
        if(tabsActivity != null)
            applicationManager = tabsActivity.applicationManager;

        ScrollView scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        TextView textView = new TextView(context);
        textView.setText("Активные аккаунты");
        textView.setTextSize(20);
        linearLayout.addView(textView);
        TextView textView1 = new TextView(context);
        textView1.setText("Внимание! Если добавить несколько дублирующихся аккаунтов, программа будет работать некорректно. Следите за отсутствием повторов!");
        linearLayout.addView(textView1);

        linearLayoutAccounts = new LinearLayout(context);
        linearLayoutAccounts.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayoutAccounts.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(linearLayoutAccounts);
        addAccountButton = new Button(context);
        addAccountButton.setText("Добавить аккаунт");
        addAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applicationManager.vkAccounts.addAccount();
            }
        });
        linearLayout.addView(addAccountButton);
        Button buttonRefresh = new Button(context);
        buttonRefresh.setText("Обновить список");
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rebuildList();
            }
        });
        linearLayout.addView(buttonRefresh);
        scrollView.addView(linearLayout);
        return scrollView;
        //return inflater.inflate(R.layout.settings_layout, container, false);
    }
    @Override public void onResume() {
        super.onResume();
        if(applicationManager != null && timer == null) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    rebuildList();
                }
            }, 1000, 10000);
        }
    }
    @Override public void onPause() {
        super.onPause();
        if(timer != null) {
            timer.cancel();
            timer = null;
        }
    }
    void checkList(){
        final int listSize = applicationManager.vkAccounts.size();
        if(listSize != lastListSize) {
            rebuildList();
            lastListSize = listSize;
        }
    }
    void rebuildList(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                linearLayoutAccounts.removeAllViews();
                for (int i = 0; i < applicationManager.vkAccounts.size(); i++) {
                    VkAccount vkAccount = applicationManager.vkAccounts.get(i);
                    linearLayoutAccounts.addView(vkAccount.getView(tabsActivity));
                }
            }
        });
    }
    void log(String text){
        ApplicationManager.log(text);
    }
}
