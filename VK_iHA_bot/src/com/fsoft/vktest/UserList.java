package com.fsoft.vktest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;

/**
 * Универсальное решение для хранения разных всяких там список пользователей
 * Created by Dr. Failov on 01.01.2015.
 */

class UserList implements Command{
    final Object sync = new Object();
    ApplicationManager applicationManager = null;
    ArrayList<Map.Entry<Long, String>> list = new ArrayList<>();
    String name; //Allowid, teacherid ...
    File file;

    UserList(String name, ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
        this.name = name;
        file = new File(ApplicationManager.getHomeFolder() + File.separator + name);
    }
    @Override public String process(String input) {
        CommandParser commandParser = new CommandParser(input);
        if(!commandParser.getWord().equals(name))
            return "";
        switch (commandParser.getWord()){
            case "add":
                return add(commandParser.getWord(), commandParser.getText());
            case "rem":
                return rem(commandParser.getWord());
            case "get":
                return get();
            case "clr":
                return clr();
        }
        return "";
    }
    @Override public String getHelp() {
        return "[ Добавить страницу в список "+name+" ] \n" +
                "---| " + ApplicationManager.botcmd + " " + name + " add  (id_страницы) (комментарий)\n\n" +
                "[ Удалить страницу из списка "+name+" ] \n" +
                "---| " + ApplicationManager.botcmd + " " + name + " rem  (id_страницы) \n\n" +
                "[ Очистить список "+name+" ] \n" +
                "---| " + ApplicationManager.botcmd + " " + name + " clr \n\n" +
                "[ Получить содержание списка "+name+" ] \n" +
                "---| " + ApplicationManager.botcmd + " " + name + " get \n\n";
    }
    public String add(long userId, String comment){
        log(". ("+name+") Внесение в список страницы " + userId + " ...");
        synchronized (sync){
            try {
                if (userId == -1L || userId == 0)
                    return "Ошибка добавления страницы " + userId + " в список " + name + ". Возможно, вы ввели неправильный ID страницы.";
                if (getIfExists(userId) != null)
                    return "Ошибка добавления страницы " + userId + " в список " + name + ". Страница уже находится в этом списке.";
                if(list.add(new AbstractMap.SimpleEntry<Long, String>(userId, comment)))
                    return "Страница " + userId + " успешно добавлена в список " + name + " с комментарием "+comment+". Сейчас в этом списке " + list.size() + " страниц.";
                else
                    return "Страница " + userId + " почему-то не добавлена в список " + name + ". Сейчас в этом списке " + list.size() + " страниц.";
            }
            catch (Exception e){
                e.printStackTrace();
                return "Ошибка добавления страницы " + userId + " в " + name + ". " + e.toString();
            }
        }
    }
    public String add(String userId, String comment){
        try{
            Long longId = applicationManager.getUserID(userId);
            return add(longId, comment);
        }
        catch (Exception e){
            e.printStackTrace();
            return "Ошибка добавления страницы " + userId + " в " + name + ". " + e.toString();
        }
    }
    public String rem(long userId){
        synchronized (sync){
            try {
                if (userId == -1L)
                    return "Ошибка удаления страницы " + userId + " из списка " + name + ". Возможно, вы ввели неправильный ID страницы.";
                if (getIfExists(userId) == null)
                    return "Ошибка удаления страницы " + userId + " из списка " + name + ". Страница не находится в этом списке.";
                if(list.remove(getIfExists(userId)))
                    return "Страница " + userId + " успешно удалена из списка " + name + ". Сейчас в этом списке " + list.size() + " страниц.";
                else
                    return "Страница " + userId + " почему-то не удалена из списка " + name + ". Сейчас в этом списке " + list.size() + " страниц.";
            }
            catch (Exception e){
                e.printStackTrace();
                return "Ошибка удаления страницы " + userId + " из списка " + name + ". " + e.toString();
            }
        }
    }
    public String rem(String userId){
        try{
            Long longId = applicationManager.getUserID(userId);
            return rem(longId);
        }
        catch (Exception e){
            e.printStackTrace();
            return "Ошибка удаления страницы " + userId + " в " + name + ". " + e.toString();
        }
    }
    public String get(){
        String result = "Список " + name + "  ("+list.size()+"): \n";
        for (int i = 0; i < list.size(); i++) {
            long id = list.get(i).getKey();
            String comment = list.get(i).getValue();
            if(id >= 0)
                result += "http://vk.com/id" + id + "  ("+applicationManager.vkCommunicator.getUserName(id)+", "+comment+")\n";
            else
                result += "http://vk.com/club" + Math.abs(id) + "  ("+applicationManager.vkCommunicator.getUserName(id)+", "+comment+") \n";
        }
        return result;
    }
    public String clr(){
        list.clear();
        return ". Список " + name + " очищен. Размер списка сейчас: " + list.size() + " элементов.\n";
    }
    public boolean contains(long userId){
        return getIfExists(userId) != null;
    }
    public boolean contains(String userId){
        try{
            Long longId = applicationManager.getUserID(userId);
            return contains(longId);
        }
        catch (Exception e){
            return false;
        }
    }
    public int size(){
        return list.size();
    }
    public long get(int i){
        return list.get(i).getKey();
    }
    public String getComment(int i){
        return list.get(i).getValue();
    }

    public void load(){
        log(". Загрузка " + name + " из файла " + file.getPath() + " ...");
        if(file.isFile()){
            try {
                java.io.FileReader fileReader = new java.io.FileReader(file);
                StringBuilder stringBuilder = new StringBuilder();
                while(fileReader.ready())
                    stringBuilder.append((char)fileReader.read());
                fileReader.close();
                String text = stringBuilder.toString();
                log(". Прочитано: " + text);
                setParcelable(text);
            }
            catch (Exception e){
                e.printStackTrace();
                log("! Ошибка чтения аккаунта "+file.getPath()+" : " + e.toString());
            }
        }
        else
            log(". (" + name + ") файла нет: " + file.getPath() + ".");
    }
    public String save(){
        String result = "";
        result += log(". (" + name + ")Запись в файл " + file.getPath() + " ...\n");
        try {
            File parentFolder = file.getParentFile();
            if(!parentFolder.exists())
                parentFolder.mkdirs();
            FileWriter fileWriter = new FileWriter(file);
            String text = getParcelable();
            result += log(". (" + name + ")Запись: " + text + "\n");
            fileWriter.write(text);
            fileWriter.close();
            result += log(". (" + name + ")Записано.\n");
        }
        catch (Exception e){
            e.printStackTrace();
            result += log("! (" + name + ")Ошибка записи "+file.getPath()+" : " + e.toString() + "\n");
        }
        return result;
    }
    private String getParcelable(){
        log(". ("+name+") Создание строки для записи...");
        //старая версия
//        String result = "";
//        for (int i = 0; i < list.size(); i++) {
//            result += String.valueOf(list.get(i));
//            if(i < list.size()-1)
//                result += "|";
//        }
        try {
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < list.size(); i++) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", list.get(i).getKey());
                jsonObject.put("comment", list.get(i).getValue());
                jsonArray.put(jsonObject);
            }
            String result = jsonArray.toString();
            log(". ("+name+")Строка для записи создана: " + result);
            return result;
        }
        catch (Exception e){
            log("! ("+name+")Ошибка создания строки для записи: " + e.toString());
        }
        return "";
    }
    private void setParcelable(String parcelable){
        log(". ("+name+") Разбор строки:" + parcelable);
        if(parcelable != null && !parcelable.equals("")){
            if(parcelable.contains("|")) {
                String[] splitted = parcelable.split("\\|");
                for (int i = 0; i < splitted.length; i++)
                    add(splitted[i], "нет описания");
                log(". (" + name + ") Строка разобрана. Получено " + list.size() + " страниц.");
            }
            else {
                try {
                    JSONArray jsonArray = new JSONArray(parcelable);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        long id = jsonObject.getLong("id");
                        String comment = jsonObject.getString("comment");
                        add(id, comment);
                    }
                    log(". (" + name + ") Строка разобрана. Получено " + list.size() + " страниц.");
                }
                catch (Exception e){
                    e.printStackTrace();
                    log("! Ошибка разбора строки " + name + " : " + e.toString());
                }
            }
        }
        else
            log(". ("+name+") Строка пуста. Список пуст.");
    }
    private Map.Entry<Long, String> getIfExists(Long number){
        for (int i = 0; i < list.size(); i++) {
            if(list.get(i).getKey().equals(number))
                return list.get(i);
        }
        return null;
    }
    private String log(String text){
        ApplicationManager.log(text);
        return text;
    }
}
