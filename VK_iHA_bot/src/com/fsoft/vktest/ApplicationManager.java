package com.fsoft.vktest;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.view.View;

import java.io.*;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * manage components
 * Created by Dr. Failov on 05.08.2014.
 */
public class ApplicationManager {
    static public String programName = "DrFailov_VK_iHA_bot";
    static public String botName = "bot";
    static public String botcmd = "botcmd";
    static public String log(String text){
        if(TabsActivity.consoleView != null)
            TabsActivity.consoleView.log("# " + text);
        return text;
    }
    static public String getHomeFolder(){
        return Environment.getExternalStorageDirectory() + File.separator + programName;
    }
    static public String botMark(){
        if(botName.equals(""))
            return "";
        return "(" + botName + ") ";
    }
    static public String arrayToString(String[] array){
        String result = " ";
        for (int i = 0; i < array.length; i++) {
            result += array[i];
            if(i < array.length-1)
                result += " ";
        }
        return result;
    }

    public Handler handler;
    public boolean running = true; // снимать только при закрытии программы
    public TabsActivity activity = null;
    public VkCommunicator vkCommunicator;
    public IhaSmartProcessor messageProcessor;
    public MessageComparer messageComparer;
    public AccountManager vkAccounts;
    private ArrayList<Command> commands;

    public ApplicationManager(TabsActivity activity, String name){
        this.activity = activity;
        botName = name;
        handler = new Handler();
        vkAccounts = new AccountManager(this);
        vkCommunicator = new VkCommunicator(this);
        messageProcessor = new IhaSmartProcessor(this, name);
        messageComparer = new MessageComparer(this, name);
        commands = new ArrayList<>();
        commands.add(new SetBotName());
        commands.add(new Save());
        commands.add(new Status());
        commands.add(vkAccounts);
        commands.add(vkCommunicator);
        commands.add(activity);
        commands.add(messageProcessor);
        commands.add(messageComparer);
    }
    public void load(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    log(". Заугрузка программы " + botName + " ...");
                    startAutoSaving();
                    messageComparer.load();
                    messageProcessor.load();
                    vkAccounts.load();
                    vkCommunicator.load();
                    SharedPreferences sp = activity.getPreferences(Activity.MODE_PRIVATE);
                    botName = sp.getString("botName", botName);
                    log(". Имя бота " + botName + " загружено.\n");
                    log(". Программа " + botName + " загружена.");
                }
                catch (Exception e){
                    e.printStackTrace();
                    log("! Ошибка загрузки: " + e.toString());
                }
            }
        }).start();
    }
    public void close(){
        try {
            running = false;
            stopAutoSaving();
            processCommand("save");
            vkAccounts.close();
            vkCommunicator.close();
            messageProcessor.close();
            messageComparer.close();
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка завершения: " + e.toString());
        }
    }
    public void messageBox(String text){
        if(activity != null)
            activity.messageBox(text);
    }
    public Long getUserID(){
        return vkCommunicator.getOwnerId();
    }
    public Long getUserID(String name){
        try{
            return Long.parseLong(name);
        }
        catch (Exception e) {
            if(name != null){
                name = name.replace("https://vk.com/", "");
                name = name.replace("http://vk.com/", "");
                name = name.replace("vk.com/", "");
            }
            return vkCommunicator.getOwnerId(name);
        }
    }
    public boolean isStandby(){
        return vkCommunicator.standby;
    }
    public String processMessage(String message, Long senderID){
        try {
            return messageProcessor.processMessage(message, senderID);
        }
        catch (Exception e){
            e.printStackTrace();
            return "Глобальная ошибка обработки сообщения: " + e.toString();
        }
        catch (OutOfMemoryError e){
            e.printStackTrace();
            return "Глобальная нехватка памяти: " + e.toString();
        }
    }
    public String processCommand(String text){
        try {
            String result = "";
            for (int i = 0; i < commands.size(); i++) {
                result += commands.get(i).process(text);
            }
            if (result.equals(""))
                result = "Ошибка обработки команды: такой команды нет.\n";
            return result;
        }
        catch (Exception | Error e){
            e.printStackTrace();
            return "Глобальная ошибка обработки команды: " + e.toString();
        }
    }
    public String getCommandsHelp(){
        try {
            String result = activity.getResources().getString(R.string.name)+", разработанный Dr. Failov.\n" +
                    "Команды можно писать везде, где бот может отвечать, например, на стене, в настройках \"Написать сообщение боту\" , или в ЛС, если их обработка включена.\n\n" +
                    "[ Сохранить все внесенные в базы изменения ]\n" +
                    "---| botcmd save\n\n" +
                    "[ Получить подробный отчёт о состоянии программы ]\n" +
                    "---| botcmd status\n\n";
            for (int i = 0; i < commands.size(); i++) {
                result += commands.get(i).getHelp();
            }
            return result;
        }
        catch (Exception e){
            e.printStackTrace();
            return "! Глобальная ошибка получения справки: " + e.toString();
        }
    }
    public String[] trimArray(String[] in){
        ArrayList<String> tmp = new ArrayList<>();
        for (int i = 0; i < in.length; i++) {
            if(in[i] != null && !in[i].equals(""))
                tmp.add(in[i]);
        }
        String[] result = new String[tmp.size()];
        for (int i = 0; i < tmp.size(); i++) {
            result[i] = tmp.get(i);
        }
        return result;
    }
    public String[] splitText(String text, int size){
        //разделить длинное сообщение на части поменьше
        ArrayList<String> parts = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        for(int i=0; i<text.length(); i++){
            if(i!= 0 && i%size == 0){
                parts.add(stringBuilder.toString());
                stringBuilder = new StringBuilder();
            }
            stringBuilder.append(text.charAt(i));
        }
        if(stringBuilder.length() > 0)
            parts.add(stringBuilder.toString());

        String[] patrsArray = new String[parts.size()];
        for (int i = 0; i < parts.size(); i++) {
            patrsArray[i] = parts.get(i);
        }
        return patrsArray;
    }
    public void sleep(int ms){
        try{
            Thread.sleep(ms);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private Timer autoSaveTimer = null;
    private void startAutoSaving(){
        log(". Запуск автосохранения...");
        if(autoSaveTimer == null){
            autoSaveTimer = new Timer();
            autoSaveTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    log(". Автосохранение...");
                    processCommand("save");
                }
            }, 1800000, 1800000);
        }
    }
    private void stopAutoSaving(){
        log(". Остановка автосохранения...");
        if(autoSaveTimer != null){
            autoSaveTimer.cancel();
            autoSaveTimer= null;
        }
    }

    class SetBotName implements Command{
        @Override
        public String getHelp() {
            return "[ Изменить метку бота (отображается в скобках перед текстом ответа) ]\n" +
                    "---| botcmd setbotname bot \n\n";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("setbotname")) {
                String newName = commandParser.getText();
                if(newName.equals(""))
                    return "Вы не ввели имя.";
                botName = newName;
                return "Новое имя бота задано: " + botName;
            }
            return "";
        }
    }
    class Save implements Command{
        @Override
        public String getHelp() {
            return "";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("save")) {
                SharedPreferences sp = activity.getPreferences(Activity.MODE_PRIVATE);
                SharedPreferences.Editor edit = sp.edit();
                edit.putString("botName", botName);
                edit.commit();
                return log(". Имя бота " + botName + " сохранено.\n");
            }
            return "";
        }
    }
    class Status implements Command{
        @Override
        public String getHelp() {
            return "";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("status")) {
                return "Имя бота: " + botName + "\n";
            }
            return "";
        }
    }
}