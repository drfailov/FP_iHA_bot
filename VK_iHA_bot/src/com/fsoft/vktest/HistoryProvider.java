package com.fsoft.vktest;

import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

/**
 * модуль, который должен хранить истории переписок бота с отдельными пользователями
 * Created by Dr. Failov on 07.08.2014.
 */
public class HistoryProvider implements Command {
    ApplicationManager applicationManager;
    String historyFileName = "";
    HashMap<Long, ArrayList<String>> messages;
    HashMap<Long, ArrayList<Long>> times;

    HistoryProvider(ApplicationManager applicationManager){
        this.applicationManager = applicationManager;
        historyFileName = Environment.getExternalStorageDirectory() + File.separator + "messageHistory";
        messages = new HashMap<>();
        times = new HashMap<>();
    }
    void load(){
        //1234>1|2|3|4
        //1235>1|2|3|4
        //1236>1|2|3|4
        try{
            File file = new File(historyFileName);
            if(!file.exists())
                return;
            String fileText = FileReader.readFromFile(historyFileName);
            String[] lines = fileText.split("\\\n");
            for (int line = 0; line < lines.length; line++) {
                String[] parts = lines[line].split("\\>");
                Long userId = Long.decode(parts[0]);
                ArrayList<String> messages = new ArrayList<>();
                String[] messagesArray = parts[1].split("\\|");
                for (int message = 0; message < messagesArray.length; message++) {
                    messages.add(messagesArray[message]);
                }
                this.messages.put(userId, messages);
            }
        }
        catch (Exception e){
            log("! Ошибка: " + e.toString());
            e.printStackTrace();
        }

    }
    void save(){
        try{
            File file = new File(historyFileName);
            FileWriter fileWriter = new FileWriter(file);
            Set<Map.Entry<Long, ArrayList<String>>> set = messages.entrySet();
            Iterator<Map.Entry<Long, ArrayList<String>>> iterator = set.iterator();
            while (iterator.hasNext()){
                Map.Entry<Long, ArrayList<String>> entry = iterator.next();
                Long userID = entry.getKey();
                fileWriter.write(userID + ">");
                ArrayList<String> messages = entry.getValue();
                for (int mes = 0; mes < messages.size(); mes++) {
                    String message = messages.get(mes);
                    fileWriter.write(message);
                    if (mes < messages.size()-1)
                        fileWriter.write("|");
                }
                if(iterator.hasNext())
                    fileWriter.write("\n");
            }
            fileWriter.close();
        }
        catch (Exception e){
            e.printStackTrace();
            log("Error: " + e.toString());
        }
    }
    String getLastMessage(long userID){
        ArrayList<String> userDB = messages.get(userID);
        if(userDB == null)
            return null;
        else {
            int DbSize = userDB.size();
            if(DbSize == 0)
                return null;
            else
                return userDB.get(DbSize - 1);
        }
    }
    long getLastMessageTime(long userID){
        ArrayList<Long> userDB = times.get(userID);
        if(userDB == null)
            return 0;
        else {
            int DbSize = userDB.size();
            if(DbSize == 0)
                return 0;
            else
                return userDB.get(DbSize - 1);
        }
    }
    String getPreLastMessage(long userID){
        ArrayList<String> userDB = messages.get(userID);
        if(userDB == null)
            return null;
        else {
            int DbSize = userDB.size();
            if(DbSize == 0)
                return null;
            else
                return userDB.get(DbSize - 2);
        }
    }
    //use numbers as 1 .... 10 ... 20...
    String getNLastMessage(long userID, int fromEnd){
        ArrayList<String> userDB = messages.get(userID);
        if(userDB == null)
            return null;
        else {
            int DbSize = userDB.size();
            if(DbSize == 0 || DbSize < fromEnd)
                return null;
            else
                return userDB.get(DbSize - fromEnd);
        }
    }
    String[] getLastMessages (long userId, int numberOfMessages){
        if(!messages.containsKey(userId))
            return null;
        ArrayList<String> messages = this.messages.get(userId);
        int totalUserMessages = messages.size();
        int toReturn = Math.min(numberOfMessages, totalUserMessages);
        String[] result = new String[toReturn];
        int index = messages.size()-1;
        for (int i = 0; i<toReturn; i++){
            result[i] = messages.get(index);
            index --;
        }
        return result;
    }
    public void addMessage(long userID, String message){
        ArrayList<String> userList;
        ArrayList<Long> userListTime;
        if(messages.containsKey(userID)){
            userList = messages.get(userID);
        }
        else {
            userList = new ArrayList<>();
            messages.put(userID, userList);
        }

        if(times.containsKey(userID)){
            userListTime = times.get(userID);
        }
        else {
            userListTime = new ArrayList<>();
            times.put(userID, userListTime);
        }

        userList.add(message);
        userListTime.add(System.currentTimeMillis());
    }
    public @Override String getHelp() {
         return "";
    }
    public @Override String process(String text) {
        if(text.equals("status")) {
            return "Количество собеседников в истории: " + messages.size()+"\n";
        }
        return "";
    }
    private void log(String text){
        ApplicationManager.log(text);
    }
}
