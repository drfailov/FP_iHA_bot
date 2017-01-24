package com.fsoft.vktest;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * Created by Dr. Failov on 11.11.2014.
 */
public class AllowListFragment extends Fragment {
    Button addAccountButton = null;
    LinearLayout linearLayoutItems = null;
    TabsActivity tabsActivity = null;
    ApplicationManager applicationManager = null;
    Handler handler = null;
    Timer timer = null;
    UserList userList;
    int lastListSize = 0;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context context = inflater.getContext();
        handler = new Handler();
        if(context.getClass().equals(TabsActivity.class))
            tabsActivity = (TabsActivity)context;
        if(tabsActivity != null)
            applicationManager = tabsActivity.applicationManager;

        userList = getUserList();

        ScrollView scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        TextView textView = new TextView(context);
        textView.setText(getFragmentName());
        textView.setTextSize(20);
        linearLayout.addView(textView);
        TextView textView1 = new TextView(context);
        textView1.setText(getListDescription());
        linearLayout.addView(textView1);
        linearLayout.addView(getDelimiter(context));

        linearLayoutItems = new LinearLayout(context);
        linearLayoutItems.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayoutItems.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(linearLayoutItems);
        addAccountButton = new Button(context);
        addAccountButton.setText("Добавить пользователя");
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
    UserList getUserList(){
        return applicationManager.messageProcessor.allowId;
    }
    String getFragmentName(){
        return "Доверенные пользователи";
    }
    String getListDescription(){
        return "Только доверенные пользователи могут управлять программой. Если недоверенный пользователь попытается написать боту команду, он получит отказ.";
    }
    void showAddWallWindow(){
        Context context = getActivity();

        final EditText editText = new EditText(context);
        editText.setHint("Введите ID пользователя которого добавить");

        final EditText editText1 = new EditText(context);
        editText1.setHint("Введите комментарий");

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(editText);
        linearLayout.addView(editText1);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Добавление стены");
        builder.setPositiveButton("Добавить", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        messageBox(userList.add(editText.getText().toString(), editText1.getText().toString()));
                    }
                }).start();
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.setView(linearLayout);
        builder.show();
    }
    void rebuildList(){
        final ArrayList<String> names = new ArrayList<>();
        for (int i = 0; i < userList.size(); i++){
            names.add(applicationManager.vkCommunicator.getUserName(userList.get(i)));
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    linearLayoutItems.removeAllViews();
                    if(userList.size() == 0){
                        TextView textView = new TextView(tabsActivity);
                        textView.setGravity(Gravity.CENTER);
                        textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                        textView.setTextSize(30);
                        textView.setTextColor(Color.argb(255, 40,40,40));
                        textView.setText("Список " + userList.name + " пуст.");
                        linearLayoutItems.addView(textView);

                        return;
                    }
                    for (int i = 0; i < userList.size(); i++) {
                        TextView textViewName = new TextView(tabsActivity);
                        textViewName.setText(names.get(i));
                        textViewName.setTextSize(20);
                        textViewName.setTextColor(Color.WHITE);

                        TextView textViewID = new TextView(tabsActivity);
                        textViewID.setText("ID = " + userList.get(i));

                        TextView textViewComment = new TextView(tabsActivity);
                        textViewComment.setText(userList.getComment(i));

                        LinearLayout linearLayout = new LinearLayout(tabsActivity);
                        linearLayout.setOrientation(LinearLayout.VERTICAL);
                        linearLayout.setPadding(10, 10, 10, 10);
                        linearLayout.addView(textViewName);
                        linearLayout.addView(textViewID);
                        linearLayout.addView(textViewComment);
                        linearLayout.addView(getDelimiter(tabsActivity));
                        linearLayout.setTag(userList.get(i));
                        linearLayout.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                showDialog((Long)view.getTag());
                            }
                        });

                        linearLayoutItems.addView(linearLayout);
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
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
    private View getDelimiter(Context context){
        View delimiter = new View(context);
        delimiter.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 1));
        delimiter.setBackgroundColor(Color.DKGRAY);
        return delimiter;
    }
    AlertDialog alertDialog;
    private void showDialog(final Long id){
        AlertDialog.Builder builder = new AlertDialog.Builder(tabsActivity);
        builder.setTitle("Меню пользователя " + id);
        LinearLayout linearLayout = new LinearLayout(tabsActivity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        ScrollView scrollView = new ScrollView(tabsActivity);
        scrollView.addView(linearLayout);
        builder.setView(scrollView);
        {
            Button button = new Button(tabsActivity);
            button.setText("Удалить");
            button.setTextColor(Color.RED);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    userList.rem(id);
                    if(alertDialog != null)
                        alertDialog.cancel();
                    Toast.makeText(tabsActivity, "Пользователь "+id+" удален", Toast.LENGTH_SHORT).show();
                }
            });
            linearLayout.addView(button);
        }
        alertDialog = builder.show();
    }
}
