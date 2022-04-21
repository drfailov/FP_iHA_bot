package com.fsoft.ihabot.configuration;

import androidx.annotation.NonNull;

import com.fsoft.ihabot.Utils.ApplicationManager;
import com.fsoft.ihabot.Utils.CommandDesc;
import com.fsoft.ihabot.Utils.CommandModule;
import com.fsoft.ihabot.Utils.CommandParser;
import com.fsoft.ihabot.answer.AnswerElement;
import com.fsoft.ihabot.answer.Message;
import com.fsoft.ihabot.communucation.tg.TgAccount;
import com.fsoft.ihabot.communucation.tg.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class AdminList  extends CommandModule {
    ApplicationManager applicationManager;
    private final ArrayList<AdminListItem> userList = new ArrayList<>();
    private final File userListFile;

    public AdminList(ApplicationManager applicationManager)  throws Exception{
        this.applicationManager = applicationManager;
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
            {
                User userTg = new User(914020479, "Polyasha00", "", "");
                add(userTg, userTg, "Подруга разработчика. Так надо.", true,
                        true, true, true,
                        true, true);
            }
        }

        childCommands.add(new AddAdminCommand());

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
        private Date addedDate;

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
            addedDate = new Date();
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
            addedDate = new Date();
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
            if(addedDate != null)
                jsonObject.put("addedDate", new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(addedDate));
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
            if(jsonObject.has("addedDate"))
                addedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(jsonObject.getString("addedDate"));
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

        public Date getAddedDate() {
            return addedDate;
        }

        public void setAddedDate(Date addedDate) {
            this.addedDate = addedDate;
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


    private class AddAdminCommand extends CommandModule{
        //1) Прислать команду на добавление админа "админ добавить usern".
        // В ответ бот присылает список своих диалогов с пользователями,
        // у которых в именах, никнеймах или ID содержится фрагмент "usern".
        // В списке будет содержаться имя пользователя и ID пользователя.
        // Чтобы на этом этапе отменить команду, надо прислать "отмена".
        // На ответ пользователю даётся 5 минут.
        // В этот момент создаётся сессия AddAdminCommandSession,
        // в ней сохраняется список пользователей, которые были отправлены в списке.
        //2) Прислать боту ID пользователя из списка и причину добавления:
        // "248067313 Друг бота, будет обучать его ответам."
        // На этом месте у нас уже есть данные о пользователе, причина добавления, дата, ответственный.
        // Пользователь добавляется в список админов.
        // Сессия AddAdminCommandSession удаляется.



        @Override
        public ArrayList<Message> processCommand(Message message, TgAccount tgAccount) throws Exception {
            ArrayList<Message> result = super.processCommand(message, tgAccount);

            CommandParser commandParser = new CommandParser(message.getText());
            if(!commandParser.getWord().toLowerCase(Locale.ROOT).equals("админ"))
                return result;
            if(!commandParser.getWord().toLowerCase(Locale.ROOT).equals("добавить"))
                return result;


            StringBuilder sb = new StringBuilder("Ответ на команду \"<b>"+message.getText() + "</b>\"\n\n");
            sb.append("Команда имеет два варианта вызова.\n");
            sb.append("Формат команды с username пользователя: \n<i>админ добавить @username причина</i>.\n");
            sb.append("Формат команды с ID пользователя: \n<i>админ добавить 123456789 причина</i>.\n\n");
            sb.append("Чтобы можно было добавить админа, он должен быть в списке ниже.\n");
            sb.append("Отображаю список последних написавших пользователей в этот аккаунт:\n\n");

            ArrayList<User> chats =  applicationManager.getMessageHistory().getLastUsersListForAccount(tgAccount);
            for (int i=0; i<chats.size() && i < 10; i++){
                User user = chats.get(i);
                sb.append("<b>ID:</b> <code>").append(user.getId()).append("</code>\n")
                        .append("Username: @").append(user.getUsername()).append("\n")
                        .append("Имя: ").append(user.getFirst_name()).append(" ").append(user.getLast_name()).append("\n\n");
            }
//            String reason = commandParser.getText();
//            if(reason.isEmpty()){
//                result.add(new Message(log(
//                        "Ответ на команду \"<b>"+message.getText() + "</b>\"\n\n" +
//                                "Чтобы воспользоваться командой добавления администратора, " +
//                                "необходимо указать причину и описание. Кто это, зачем админ, и т.д.\n" +
//                                "Без этого потом будет легко запутаться в списке администраторов.")));
//                return result;
//            }



            result.add(new Message(sb.toString()));
            return result;
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = super.getHelp();
            result.add(new CommandDesc("Админ добавить Причина добавления", "" +
                    "Добавить пользователя из следующего пересланного сообщения в список администраторов с указанной причиной добавления. " +
                    "После отправки этой команды, перешли любое сообщение от пользователя, которого надо добавить в администраторы."));
            return result;
        }
    }
    private class RemAdminCommand extends CommandModule{
        @Override
        public ArrayList<Message> processCommand(Message message, TgAccount tgAccount) throws Exception {
            ArrayList<Message> result = super.processCommand(message, tgAccount);
            CommandParser commandParser = new CommandParser(message.getText());
            if(!commandParser.getWord().toLowerCase(Locale.ROOT).equals("админ"))
                return result;
            if(!commandParser.getWord().toLowerCase(Locale.ROOT).equals("добавить"))
                return result;
            String username = commandParser.getWord();
            if(username.isEmpty()){
                result.add(new Message(log(
                        "Ответ на команду \"<b>"+message.getText() + "</b>\"\n\n" +
                                "Чтобы воспользоваться командой добавления администратора, " +
                                "нужно указать username пользователя Telegram.\n" +
                                "Если у пользователя нет username, пусть создаст в настройках своего аккаунта.")));
                return result;
            }
            String reason = commandParser.getText();
            if(reason.isEmpty()){
                result.add(new Message(log(
                        "Ответ на команду \"<b>"+message.getText() + "</b>\"\n\n" +
                                "Чтобы воспользоваться командой добавления администратора, " +
                                "необходимо указать причину и описание. Кто это, зачем админ, и т.д.\n" +
                                "Без этого потом будет легко запутаться в списке администраторов.")));
                return result;
            }

            result.add(new Message("админа добавление но пока ничего не написано " + username));
            return result;
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = super.getHelp();
            result.add(new CommandDesc("Админ добавить @username Зачем он нужен", "Добавить пользователя в список администраторов. Аргументы: username пользователя, основание для добавления."));
            return result;
        }
    }
}
