package com.fsoft.vktest;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.*;
import android.widget.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * Created by Dr. Failov on 11.11.2014.
 */
public class SettingsFragment  extends Fragment {
    TextView textViewStatus = null;
    Button buttonCloseProgram = null;
    Button buttonChangelog = null;
    Button buttonSendMessageToBot = null;
    Button buttonLearnBot = null;
    Button buttonShowHelp = null;
    Button buttonSave = null;
    TabsActivity tabsActivity = null;
    ApplicationManager applicationManager = null;
    Handler handler = null;
    Timer timer = null;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context context = inflater.getContext();
        handler = new Handler();
        if(context.getClass().equals(TabsActivity.class))
            tabsActivity = (TabsActivity)context;
        if(tabsActivity != null)
            applicationManager = tabsActivity.applicationManager;
        return inflater.inflate(R.layout.settings_layout, container, false);
    }
    @Override public void onResume() {
        super.onResume();
        ApplicationManager.log("settings resume");
        textViewStatus = (TextView)getActivity().findViewById(R.id.textViewStatus);
        textViewStatus.setTextSize(9);
        writeStatus("Received applicationManager = " + applicationManager);
        buttonCloseProgram = (Button)getActivity().findViewById(R.id.buttonClose);
        if(buttonCloseProgram != null){
            buttonCloseProgram.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    applicationManager.activity.turnoff();
                }
            });
        }
        buttonChangelog = (Button)getActivity().findViewById(R.id.buttonChangelog);
        if(buttonChangelog != null){
            buttonChangelog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showChangelog();
                }
            });
        }
        buttonShowHelp = (Button)getActivity().findViewById(R.id.buttonShowHelp);
        if(buttonShowHelp != null){
            buttonShowHelp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sendMessageToBot("botcmd help");
                }
            });
        }
        buttonSendMessageToBot = (Button)getActivity().findViewById(R.id.buttonSendMessageToBot);
        if(buttonSendMessageToBot != null){
            buttonSendMessageToBot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sendMessageToBot("");
                }
            });
        }
        buttonLearnBot = (Button)getActivity().findViewById(R.id.buttonLearnBot);
        if(buttonLearnBot != null){
            buttonLearnBot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    learnBot();
                }
            });
        }
        buttonSave = (Button)getActivity().findViewById(R.id.buttonSave);
        if(buttonSave != null){
            buttonSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    saveSettings();
                }
            });
        }
        if(applicationManager != null && timer == null) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    refreshData();
                }
            }, 1000, 10000);
        }

    }
    @Override public void onPause() {
        super.onPause();
        ApplicationManager.log("settings pause");
        if(timer != null) {
            timer.cancel();
            timer = null;
        }
    }
    void refreshData(){
        writeStatus(applicationManager.processCommand("status"));
    }
    void showChangelog(){
        String changelog = "";
        Resources resources = applicationManager.activity.getResources();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(resources.openRawResource(R.raw.chandelog)));
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    sb.append("\n");
                    line = br.readLine();
                }
                changelog = sb.toString();
            } finally {
                br.close();
            }
        }catch (Exception e){
            e.printStackTrace();
            changelog = "Ошибка чтения файла: " + e.toString();
        }
        new AlertDialog.Builder(applicationManager.activity).setTitle("История изменений").setMessage(changelog).setPositiveButton("OK", null).show();
    }
    void sendMessageToBot(String in){
        Context context = getActivity();

        final TextView textViewResult = new TextView(context);
        if(in.equals(""))
            textViewResult.setText("Введите сообщение и нажмите \"Отправить\"");
        else {
            String reply;
            if(applicationManager.vkAccounts.size() == 0)
                reply = "Ошибка: нет аккаунтов.";
            else
                reply = applicationManager.processMessage(in, applicationManager.vkAccounts.get(0).id);
            textViewResult.setText(reply);
        }
        final EditText editTextMessage = new EditText(context);
        editTextMessage.setHint("Введите сообщение");
        editTextMessage.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button buttonSend = new Button(context);
        buttonSend.setText("Отправить");
        buttonSend.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = editTextMessage.getText().toString();
                String reply;
                if(applicationManager.vkAccounts.size() == 0)
                    reply = "Ошибка: нет аккаунтов.";
                else
                    reply = applicationManager.processMessage(message, applicationManager.vkAccounts.get(0).id);
                ApplicationManager.log("message = {"+message+"}");
                ApplicationManager.log("reply = {"+reply+"}");
                textViewResult.setText(reply);
            }
        });

        Button buttonClose = new Button(context);
        buttonClose.setText("Закрыть");
        buttonClose.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));

        LinearLayout linearLayoutBottom = new LinearLayout(context);
        linearLayoutBottom.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayoutBottom.setOrientation(LinearLayout.HORIZONTAL);
        linearLayoutBottom.addView(editTextMessage);
        linearLayoutBottom.addView(buttonSend);

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        linearLayout.addView(textViewResult);
        linearLayout.addView(linearLayoutBottom);
        linearLayout.addView(buttonClose);

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(linearLayout);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(scrollView);
        final Dialog dialogWithBot = builder.show();
        buttonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogWithBot.dismiss();
            }
        });
    }
    void learnBot(){
        new LearnBotWindow(getActivity()).show();
    }
    void saveSettings(){
        String result = applicationManager.processCommand("save");
        messageBox(result);
    }
    void messageBox(String text){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setPositiveButton("OK", null);
        builder.setMessage(text);
        builder.setTitle("Результат");
        builder.show();
    }

    private void writeStatus(String text){
        final String txt = text;
        if(textViewStatus != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    textViewStatus.setText(txt);
                }
            });
        //Log.d("BOT", "WriteStatus = " + text);
    }

    class LearnBotWindow extends Dialog{
        AnswerDatabase answerDatabase;
        Context context;
        Button buttonNext;
        Button buttonClose;
        TextView textViewRemaining;
        TextView textViewInstruction;
        TextView textViewMessage;
        EditText editTextAnswer;


        LearnBotWindow(Context context) {
            super(context);
            this.context = context;
            answerDatabase = applicationManager.messageProcessor.answerDatabase;
            getWindow().requestFeature(Window.FEATURE_NO_TITLE);


            buttonNext = new Button(context);
            buttonNext.setText("Дальше");
            buttonNext.setTextSize(12);
            buttonNext.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            buttonNext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    next();
                }
            });


            buttonClose = new Button(context);
            buttonClose.setText("Закрыть");
            buttonClose.setTextSize(12);
            buttonClose.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            buttonClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    close();
                }
            });


            textViewInstruction = new TextView(context);
            textViewInstruction.setText("Все сообщения, на которые бот не знает ответа он вносит в специальный спискок неизвестных фраз. " +
                    "Ваша задача сейчас - последовательно отвечать на сообщения, потображаемые в текстовом поле ниже и нажимать кнопку Далее.");
            textViewInstruction.setTextColor(Color.GRAY);
            textViewInstruction.setPadding(5, 2, 5, 0);


            textViewRemaining = new TextView(context);
            textViewRemaining.setText("Осталось ... сообщений.");
            textViewRemaining.setTextColor(Color.GRAY);
            textViewRemaining.setPadding(5, 0, 5, 10);


            textViewMessage = new TextView(context);
            textViewMessage.setText("NOTHING");
            textViewMessage.setTextColor(Color.WHITE);
            textViewMessage.setPadding(15, 10, 5, 10);


            editTextAnswer = new EditText(context);
            editTextAnswer.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            editTextAnswer.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View view, int i, KeyEvent keyEvent) {
                    if(keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER){
                        next();
                        return true;
                    }
                    return false;
                }
            });


            LinearLayout linearLayoutBottom = new LinearLayout(context);
            linearLayoutBottom.setOrientation(LinearLayout.HORIZONTAL);
            linearLayoutBottom.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            linearLayoutBottom.addView(buttonClose);
            linearLayoutBottom.addView(buttonNext);


            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            linearLayout.addView(textViewInstruction);
            linearLayout.addView(textViewRemaining);
            linearLayout.addView(textViewMessage);
            linearLayout.addView(editTextAnswer);
            linearLayout.addView(linearLayoutBottom);


            ScrollView scrollView = new ScrollView(context);
            scrollView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            scrollView.addView(linearLayout);
            setContentView(scrollView);
        }

        @Override
        public void show() {
            super.show();
            next();
        }

        void next(){
            String message = textViewMessage.getText().toString();
            String reply = editTextAnswer.getText().toString();
            if(!message.equals("NOTHING")) {
                if (!reply.equals("")) {
                    String res = answerDatabase.addToDatabase(message, reply);
                    ApplicationManager.log(res);
                    textViewInstruction.setText(res);
                }
                if(answerDatabase.unknownMessages.size() > 0)
                    answerDatabase.unknownMessages.remove(0);
            }
            editTextAnswer.setText("");
            if(answerDatabase.unknownMessages.size() > 0){
                message = answerDatabase.unknownMessages.get(0);
                textViewMessage.setText(message);
                textViewRemaining.setText("Осталось "+answerDatabase.unknownMessages.size()+" сообщений.");
            }
            else {
                textViewRemaining.setText("Больше нет сообщений.");
                 textViewMessage.setText("NOTHING");
            }

        }
        void close(){
            dismiss();
        }
        void text(String text){

        }
    }
}
