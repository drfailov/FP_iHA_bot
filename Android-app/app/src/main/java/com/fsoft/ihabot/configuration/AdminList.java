package com.fsoft.ihabot.configuration;

import androidx.annotation.NonNull;

import com.fsoft.ihabot.Utils.ApplicationManager;
import com.fsoft.ihabot.Utils.CommandModule;
import com.fsoft.ihabot.Utils.UserTg;
import com.fsoft.ihabot.answer.AnswerElement;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;

public class AdminList  extends CommandModule {
    private final ArrayList<AdminListItem> userList = new ArrayList<>();
    private final File userListFile;

    public AdminList(ApplicationManager applicationManager)  throws Exception{
        userListFile = new File(applicationManager.getHomeFolder(), "adminList.txt");
        log("Загрузка списка администраторов из файла "+userListFile.getName()+"...");
        if(!userListFile.isFile())
            log("Это видимо первый запуск, создам пустой файл: "+userListFile.createNewFile());

        String line;
        synchronized (userListFile) {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(userListFile));
            while ((line = bufferedReader.readLine()) != null) {
                try {
                    JSONObject jsonObject = new JSONObject(line);
                    userList.add(new AdminListItem(jsonObject));
                } catch (Exception e) {
                    e.printStackTrace();
                    log("Ошибка разбора строки как AdminListItem: " + e.getMessage());
                }
            }
            //завешить сессию
            bufferedReader.close();
        }
        log("Загружено список: " + userList.size() + " администраторов");
        if(userList.isEmpty()){
            log("Поскольку список администраторов пустой, добавляю тестовую запись.");
            UserTg userTg = new UserTg(248067313, "DrFailov", "Dr. Failov");
            add(userTg, userTg, "Разработчик бота.");
        }
    }

    public void add(UserTg userToAdd, UserTg responsible, String comment) throws Exception {
        if(userToAdd == null)
            throw new Exception("Не могу добавить пользователя в список: Не получен пользователь чтобы его добавить.");
        if(responsible == null)
            throw new Exception("Не могу добавить пользователя в список: Не получен ответственный за этого пользователя.");
        if(comment == null || comment.isEmpty())
            throw new Exception("Не могу добавить пользователя в список: Не получен комментарий по поводу этого пользователя.");
        if(has(userToAdd))
            throw new Exception("Не могу добавить пользователя в список: Пользователь уже содержится в списке.");
        AdminListItem adminListItem = new AdminListItem(userToAdd, responsible, comment);
        userList.add(adminListItem);
        log(adminListItem + " добавлен в список администраторов. Количество администраторов сейчас: " + userList.size());
        saveArrayToFile();
    }

    /**
     * Возвращает true если пользователь есть в списке
     * @param userTg Пользователь, которого мы ищем. Тот самый, который используется у нас везде в Message
     * @return Возвращает true если пользователь есть в списке
     */
    public boolean has(UserTg userTg){
        return get(userTg) != null;
    }

    /**
    * Возвращает этого пользователя в списке со всеми деталями
    * @param userTg Пользователь, которого мы ищем. Тот самый, который используется у нас везде в Message
    * @return Возвращает обьект AdminListItem который содержит инфу про пользователя и ответственного за него пользователя
    */
    public AdminListItem get(UserTg userTg){
        for (AdminListItem adminListItem:userList){
            if(adminListItem.getUser() != null)
                if(adminListItem.getUser().equals(userTg))
                    return adminListItem;
        }
        return null;
    }


    private void saveArrayToFile() throws Exception{
        try (PrintWriter fileTmpWriter = new PrintWriter(userListFile)) {
            for (AdminListItem adminListItem : userList)
                fileTmpWriter.println(adminListItem.toJson().toString());
            log("Сохранено в файл "+userListFile.getName()+": " + userList.size() + " администраторов");
        }
    }



    public static class AdminListItem{
        private UserTg user;
        private UserTg responsible;
        private String comment;
        private boolean allowDatabaseDump = false;

        public AdminListItem(){

        }

        public AdminListItem(UserTg user, UserTg responsible, String comment) {
            this.user = user;
            this.responsible = responsible;
            this.comment = comment;
        }

        public AdminListItem(JSONObject jsonObject) throws Exception{
            fromJson(jsonObject);
        }

        public UserTg getUser() {
            return user;
        }

        public void setUser(UserTg user) {
            this.user = user;
        }

        public UserTg getResponsible() {
            return responsible;
        }

        public void setResponsible(UserTg responsible) {
            this.responsible = responsible;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public boolean isAllowDatabaseDump() {
            return allowDatabaseDump;
        }

        public void setAllowDatabaseDump(boolean allowDatabaseDump) {
            this.allowDatabaseDump = allowDatabaseDump;
        }


        public JSONObject toJson() throws JSONException {
            JSONObject jsonObject = new JSONObject();
            if(user != null)
                jsonObject.put("user", user.toJson());
            if(responsible != null)
                jsonObject.put("responsible", responsible.toJson());
            if(comment != null)
                jsonObject.put("comment", comment);
            jsonObject.put("allowDatabaseDump", allowDatabaseDump);
            return jsonObject;
        }
        private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
            if(jsonObject.has("user"))
                user = new UserTg(jsonObject.getJSONObject("user"));
            if(jsonObject.has("responsible"))
                responsible = new UserTg(jsonObject.getJSONObject("responsible"));
            if(jsonObject.has("comment"))
                comment = jsonObject.getString("comment");
            if(jsonObject.has("allowDatabaseDump"))
                allowDatabaseDump = jsonObject.getBoolean("allowDatabaseDump");
        }

        @NonNull
        @Override
        public String toString() {
            String result = "";
            if(user != null){
                result += "Польльзователь: " + user;
            }
            if(responsible != null){
                if(!result.isEmpty())
                    result += ", ";
                result += "Ответственный: " + responsible;
            }
            if(comment != null){
                if(!result.isEmpty())
                    result += ", ";
                result += "Основание: " + comment;
            }
            return result;
        }
    }
}
