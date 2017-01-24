package com.fsoft.vktest;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * Created by Dr. Failov on 11.11.2014.
 */
public class WallListFragment extends Fragment {
    Button addAccountButton = null;
    LinearLayout linearLayoutWalls = null;
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
        textView.setText("Активные стены");
        textView.setTextSize(20);
        linearLayout.addView(textView);

        linearLayoutWalls = new LinearLayout(context);
        linearLayoutWalls.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayoutWalls.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(linearLayoutWalls);
        addAccountButton = new Button(context);
        addAccountButton.setText("Добавить стену");
        addAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               showAddWallWindow();
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
    void showAddWallWindow(){
        Context context = getActivity();

        final EditText editText = new EditText(context);
        editText.setHint("Введите ID стены которую добавить");

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Добавление стены");
        builder.setPositiveButton("Добавить", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        messageBox(applicationManager.processCommand("wall add " + editText.getText().toString()));
                    }
                }).start();
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.setView(editText);
        builder.show();
    }
    void rebuildList(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                linearLayoutWalls.removeAllViews();
                for (int i = 0; i < applicationManager.vkCommunicator.walls.size(); i++) {
                    linearLayoutWalls.addView(applicationManager.vkCommunicator.walls.get(i).getView(tabsActivity));
                }
            }
        });
    }
    void log(String text){
        ApplicationManager.log(text);
    }

    void messageBox(final String text){
        handler.post(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setPositiveButton("OK", null);
                builder.setMessage(text);
                builder.setTitle("Результат");
                builder.show();
            }
        });
    }
}
