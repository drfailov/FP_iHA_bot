package com.fsoft.ihabot.configuration;

import androidx.annotation.NonNull;

import com.fsoft.ihabot.Utils.ApplicationManager;
import com.fsoft.ihabot.Utils.CommandModule;
import com.fsoft.ihabot.answer.AnswerElement;
import com.fsoft.ihabot.communucation.tg.User;

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
            {
                User userTg = new User(248067313, "DrFailov", "Dr.", "Failov");
                add(userTg, userTg, "Разработчик бота.", true,
                        true, true, true,
                        true, true);
            }
        }
//        {
//            User userTg = new User(914020479, "Polyasha00", "", "");
//            add(userTg, userTg, "Подруга разработчика. Так надо.", true,
//                    true, true, true,
//                    true, true);
//        }
    }

    public void add(User userToAdd, User responsible, String comment, boolean allowDatabaseDump,
                    boolean allowDatabaseRead, boolean allowDatabaseEdit, boolean allowLearning,
                    boolean allowAdminsRead, boolean allowAdminsAdd) throws Exception {
        if(userToAdd == null)
            throw new Exception("Не могу добавить пользователя в список: Не получен пользователь чтобы его добавить.");
        if(responsible == null)
            throw new Exception("Не могу добавить пользователя в список: Не получен ответственный за этого пользователя.");
        if(comment == null || comment.isEmpty())
            throw new Exception("Не могу добавить пользователя в список: Не получен комментарий по поводу этого пользователя.");
        if(has(userToAdd))
            throw new Exception("Не могу добавить пользователя в список: Пользователь уже содержится в списке.");
        AdminListItem adminListItem = new AdminListItem(userToAdd, responsible, comment,
                allowDatabaseDump, allowDatabaseRead, allowDatabaseEdit,
                allowLearning, allowAdminsRead, allowAdminsAdd);
        userList.add(adminListItem);
        log(adminListItem + " добавлен в список администраторов. Количество администраторов сейчас: " + userList.size());
        saveArrayToFile();
    }

    /**
     * Возвращает true если пользователь есть в списке
     * @param userTg Пользователь, которого мы ищем. Тот самый, который используется у нас везде в Message
     * @return Возвращает true если пользователь есть в списке
     */
    public boolean has(User userTg){
        return get(userTg) != null;
    }

    /**
    * Возвращает этого пользователя в списке со всеми деталями
    * @param userTg Пользователь, которого мы ищем. Тот самый, который используется у нас везде в Message
    * @return Возвращает обьект AdminListItem который содержит инфу про пользователя и ответственного за него пользователя
    */
    public AdminListItem get(User userTg){
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
        private User user;
        private User responsible;
        private String comment;

        private boolean allowDatabaseDump = false; //выгрузка базы целиком
        private boolean allowDatabaseRead = false; //просмотр ответов по ID, по вопросу и т.п.
        private boolean allowDatabaseEdit = false; //Добавление, удаление ответов, и т.п.
        private boolean allowLearning = false;     //Ещё пока не реализованная функциональность обучения
        private boolean allowAdminsRead = false;    //Разрешить или нет просмотр списка администраторов
        private boolean allowAdminsAdd = false;    //Разрешить или нет добавление администраторов

        public AdminListItem(User user, User responsible, String comment, boolean allowDatabaseDump, boolean allowDatabaseRead, boolean allowDatabaseEdit, boolean allowLearning, boolean allowAdminsRead, boolean allowAdminsAdd) {
            this.user = user;
            this.responsible = responsible;
            this.comment = comment;
            this.allowDatabaseDump = allowDatabaseDump;
            this.allowDatabaseRead = allowDatabaseRead;
            this.allowDatabaseEdit = allowDatabaseEdit;
            this.allowLearning = allowLearning;
            this.allowAdminsRead = allowAdminsRead;
            this.allowAdminsAdd = allowAdminsAdd;
        }

        public AdminListItem(User user, User responsible, String comment) {
            this.user = user;
            this.responsible = responsible;
            this.comment = comment;
        }

        public AdminListItem(JSONObject jsonObject) throws Exception{
            fromJson(jsonObject);
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
            jsonObject.put("allowDatabaseRead", allowDatabaseRead);
            jsonObject.put("allowDatabaseEdit", allowDatabaseEdit);
            jsonObject.put("allowLearning", allowLearning);
            jsonObject.put("allowAdminsRead", allowAdminsRead);
            jsonObject.put("allowAdminsAdd", allowAdminsAdd);
            return jsonObject;
        }
        private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
            if(jsonObject.has("user"))
                user = new User(jsonObject.getJSONObject("user"));
            if(jsonObject.has("responsible"))
                responsible = new User(jsonObject.getJSONObject("responsible"));
            if(jsonObject.has("comment"))
                comment = jsonObject.getString("comment");
            if(jsonObject.has("allowDatabaseDump"))
                allowDatabaseDump = jsonObject.getBoolean("allowDatabaseDump");
            if(jsonObject.has("allowDatabaseRead"))
                allowDatabaseRead = jsonObject.getBoolean("allowDatabaseRead");
            if(jsonObject.has("allowDatabaseEdit"))
                allowDatabaseEdit = jsonObject.getBoolean("allowDatabaseEdit");
            if(jsonObject.has("allowLearning"))
                allowLearning = jsonObject.getBoolean("allowLearning");
            if(jsonObject.has("allowAdminsRead"))
                allowAdminsRead = jsonObject.getBoolean("allowAdminsRead");
            if(jsonObject.has("allowAdminsAdd"))
                allowAdminsAdd = jsonObject.getBoolean("allowAdminsAdd");
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public User getResponsible() {
            return responsible;
        }

        public void setResponsible(User responsible) {
            this.responsible = responsible;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        /**
         * @return Можно ли этому администратору выгружать базу целиком
         */
        public boolean isAllowDatabaseDump() {
            return allowDatabaseDump;
        }

        public AdminListItem setAllowDatabaseDump(boolean allowDatabaseDump) {
            this.allowDatabaseDump = allowDatabaseDump;
            return this;
        }

        /**
         * @return Можно ли этому администратору просматривать ответы из базы, анализировать их
         */
        public boolean isAllowDatabaseRead() {
            return allowDatabaseRead;
        }

        public AdminListItem setAllowDatabaseRead(boolean allowDatabaseRead) {
            this.allowDatabaseRead = allowDatabaseRead;
            return this;
        }

        /**
         * @return Можно ли этому администратору добавлять и удалять ответы из базы
         */
        public boolean isAllowDatabaseEdit() {
            return allowDatabaseEdit;
        }

        public AdminListItem setAllowDatabaseEdit(boolean allowDatabaseEdit) {
            this.allowDatabaseEdit = allowDatabaseEdit;
            return this;
        }

        /**
         * @return Можно ли этому администратору пользоваться инфраструктурой обучения
         */
        public boolean isAllowLearning() {
            return allowLearning;
        }

        public AdminListItem setAllowLearning(boolean allowLearning) {
            this.allowLearning = allowLearning;
            return this;
        }

        /**
         * @return Можно ли этому администратору просматривать список администраторов
         */
        public boolean isAllowAdminsRead() {
            return allowAdminsRead;
        }

        public void setAllowAdminsRead(boolean allowAdminsRead) {
            this.allowAdminsRead = allowAdminsRead;
        }

        /**
         * @return Можно ли этому администратору добавлять других администраторов
         */
        public boolean isAllowAdminsAdd() {
            return allowAdminsAdd;
        }

        public AdminListItem setAllowAdminsAdd(boolean allowAdminsAdd) {
            this.allowAdminsAdd = allowAdminsAdd;
            return this;
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
