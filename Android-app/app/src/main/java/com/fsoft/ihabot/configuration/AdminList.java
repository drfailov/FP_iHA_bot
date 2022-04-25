package com.fsoft.ihabot.configuration;

import android.service.dreams.DreamService;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fsoft.ihabot.ApplicationManager;
import com.fsoft.ihabot.Utils.CommandDesc;
import com.fsoft.ihabot.Utils.CommandModule;
import com.fsoft.ihabot.Utils.CommandParser;
import com.fsoft.ihabot.Utils.F;
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
import java.util.Objects;

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
                add(userTg, userTg, "Разработчик бота");
            }
            {
                User userTg = new User(914020479, "Polyasha00", "", "");
                add(userTg, userTg, "Подруга разработчика. Так надо");
            }
        }

        childCommands.add(new ShowAdminCommand());
        childCommands.add(new AddAdminCommand());
        childCommands.add(new RemAdminCommand());
        childCommands.add(new EditAdminCommand());

    }

    public AdminListItem add(User userToAdd, User responsible, String comment) throws Exception {
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
        return adminListItem;
    }
    public boolean rem(AdminListItem adminListItem) throws Exception {
        if(adminListItem == null)
            throw new Exception("Не могу удалить пользователя из списка админов: Не получен пользователь чтобы его удалить.");
        boolean result =  userList.remove(adminListItem);
        if(result)
            log(adminListItem + " удален из списка администраторов. Количество администраторов сейчас: " + userList.size());
        else
            log(adminListItem + " НЕ БЫЛ удален из списка администраторов. Причину не знаем.");
        saveArrayToFile();
        return result;
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

    /**
     * Возвращает обьект админа, исходя из описания в видео текстового айдишника либо юзернейма
     * Эта функция очень полезна для команд, где юзер ввёл какую-то херню и нам с этим что-то делать
     * @param usernameOrId текст его ID, либо username. Можно с собакой можно без.
     * @return Если в админах такой персонаж есть, то его админ-аккаунт. Если не найдено, то null
     */
    public AdminListItem getByUsernameOrId(String usernameOrId){
        for (AdminListItem adminListItem:userList){
            if(adminListItem.getUserByUsernameOrId(usernameOrId) != null)
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
        //Можно ли этому администратору пользоваться инфраструктурой обучения
        public static Right LEARNING = new Right("Learning", "Использовать функциональность обучения");
        //Просмотр ответов по ID, по вопросу и т.п.
        public static Right DATABASE_READ = new Right("DatabaseRead", "Просматривать ответы из базы, анализировать их");
        //Добавление, удаление ответов, и т.п.
        public static Right DATABASE_EDIT = new Right("DatabaseEdit", "Добавлять и удалять ответы из базы");
        //выгрузка базы целиком
        public static Right DATABASE_DUMP = new Right("DatabaseDump", "Выгружать и восстанавливать базу ответов");
        //Разрешить или нет просмотр списка администраторов
        public static Right ADMINS_READ = new Right("AdminsRead", "Просматривать список администраторов");
        ////Разрешить или нет добавление администраторов
        public static Right ADMINS_ADD = new Right("AdminsAdd", "Добавлять других администраторов и управлять правами");

        /**
         * Права списком используется для вывода юзеру списка прав в функциях где этот список формируется программно.
         * Null в массиве используется в качестве разделителя в списках для наглядности.
         * @return Массив обьектов Right. Права раздеены на блоки для наглядности. Разделитель - Null.
         */
        public static Right[] getGenericRightsList(){return new Right[]{
                LEARNING, null, DATABASE_READ, DATABASE_EDIT, DATABASE_DUMP, null, ADMINS_READ,  ADMINS_ADD
        };}


        private User user;
        private User responsible;
        private String comment;
        private Date addedDate;
        private final HashMap<String, Boolean> rights = new HashMap<>();


        public AdminListItem(User user, User responsible, String comment) {
            this.user = user;
            this.responsible = responsible;
            this.comment = comment;
            addedDate = new Date();
        }

        public AdminListItem(JSONObject jsonObject) throws Exception{
            fromJson(jsonObject);
        }

        @Nullable
        public User getUserById(long userId){
            if(user != null) {
                if (user.getId() == userId)
                    return user;
            }
            return null;
        }

        /**
         * Вернет обьект юзера если подходит
         * @param usernameOrId текст его ID, либо username. Можно с собакой можно без.
         * @return Если тут такой персонаж есть, то его аккаунт. Если нет, то null
         */
        @Nullable
        public User getUserByUsernameOrId(String usernameOrId){
            if(user != null) {
                if (F.isDigitsOnly(usernameOrId)){
                    try {
                        long result = Long.parseLong(usernameOrId);
                        User found = getUserById(result);
                        if(found != null)
                            return found;
                    }
                    catch (Exception e){
                        //похуй
                    }
                }
                usernameOrId = usernameOrId.replace("@", "").toLowerCase(Locale.ROOT).trim();
                if(user.getUsername() != null)
                    if (user.getUsername().replace("@", "").toLowerCase(Locale.ROOT).trim().equals(usernameOrId))
                        return user;
            }
            return null;
        }

        /**
         * Выясняет разрешено ли этому админу некоторое действие.
         * По умолчанию админам ничего нельзя.
         * @param right Обьект права. Его можно получить из констант этого класса,
         *              либо фунцией getGenericRightsList()
         * @return булевое значение права. В любой непонятной ситуации будет false.
         */
        public boolean isAllowed(Right right){
            if(right == null)
                return false;
            try {
                Boolean b = rights.get(right.getCode());
                if(b == null)
                    return false;
                return b;
            }
            catch (Exception e){
                return false;
            }
        }

        /**
         * Задает значение какого либо права доступа. При этом в файл ничего не сохраняется, нужно
         * после этой команды сохранять в файл. Эта команда только меняет данные в оперативке.
         * @param right Обьект права. Его можно получить из констант этого класса,
         *              либо фунцией getGenericRightsList()
         * @param value Значение права. True если разрешено.
         */
        public void setAllowed(Right right, boolean value){
            rights.put(right.getCode(), value);
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
            for (Right right:getGenericRightsList()) {
                if(right != null) {
                    if (rights.containsKey(right.getCode()))
                        jsonObject.put("allow" + right.getCode(), isAllowed(right));
                }
            }
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
            else
                addedDate = new Date();
            for (Right right:getGenericRightsList()){
                if(right != null) {
                    String key = "allow" + right.code;
                    if (jsonObject.has(key)) {
                        setAllowed(right, jsonObject.getBoolean(key));
                    }
                }
            }
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



        @NonNull
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("\uD83D\uDC64 <b>").append(user).append("</b>\n");
            sb.append("[");

            for (Right right:getGenericRightsList()) {
                if (right != null)
                    sb.append(isAllowed(right)?"✅":"⛔");
                if (right == null)
                    sb.append("--");
            }
            sb.append("]\n");
            sb.append("⚡️ /AdminInfo_").append(user.getId()).append(" - подробнее.");
            return sb.toString();
        }

        @NonNull
        public String toStringFull() {
            StringBuilder sb = new StringBuilder();
            AdminListItem adminListItem = this;

            sb.append("\uD83D\uDC64 <b>Админ:</b> ").append(adminListItem.getUser()).append("\n");
            sb.append("\uD83C\uDFC5 <b>Добавил:</b> ").append(adminListItem.getResponsible()).append("\n");
            if(adminListItem.getAddedDate() != null)
                sb.append("\uD83D\uDCC6 <b>Дата добавления:</b> ")
                        .append(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(adminListItem.getAddedDate()))
                        .append("\n");
            sb.append("\uD83D\uDCA1 <b>Комментарий:</b> ").append(adminListItem.getComment()).append("\n\n");

            sb.append("<b>Разрешения:</b> \n");
            for (Right right:getGenericRightsList()) {
                if (right != null)
                    sb.append(isAllowed(right)?"✅":"⛔").append(" - ").append(right.getDescription()).append("\n");
                if (right == null)
                    sb.append("-\n");
            }
            sb.append("\n");

            sb.append("<b>Действия:</b> \n");

            sb.append("⚡️ /AdminInfo\n"+"<b>Просмотреть</b> список администраторов \n\n");

            sb.append("⚡️ /HelpAdmin\n"+"<b>Просмотреть</b> справку по командам админки \n\n");

            for (Right right:getGenericRightsList()) {
                if (right != null) {
                    if(isAllowed(right)){
                        sb.append("⚡️ /AdminDeny_").append(adminListItem.user.getId()).append("_").append(right.getCode()).append("\n"+"<b>Запретить</b> ").append(right.getDescription()).append("\n\n");
                    }
                    else {
                        sb.append("⚡️ /AdminAllow_").append(adminListItem.user.getId()).append("_").append(right.getCode()).append("\n"+"<b>Разрешить</b> ").append(right.getDescription()).append("\n\n");
                    }
                }
            }


            sb.append("⚡️ /AdminDelete_").append(adminListItem.user.getId()).append("\n"+"<b>Удалить</b> администратора \n\n");
            return sb.toString();
        }

        public static class Right{
            private String code = "";
            private String description = "";

            public Right(String code, String description) {
                this.code = code;
                this.description = description;
            }

            public String getCode() {
                return code;
            }

            public void setCode(String code) {
                this.code = code;
            }

            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = description;
            }

            @NonNull
            @Override
            public String toString() {
                return code;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Right right = (Right) o;
                return code.equals(right.code);
            }

            @Override
            public int hashCode() {
                return Objects.hash(code);
            }
        }
    }

    /**
     * Команда "Админ добавить @username Причина добавления"
     */
    private class AddAdminCommand extends CommandModule{

        @Override
        public ArrayList<Message> processCommand(Message message, TgAccount tgAccount, AdminListItem admin) throws Exception {
            ArrayList<Message> result = super.processCommand(message, tgAccount, admin);

            CommandParser commandParser = new CommandParser(message.getText());
            if(!commandParser.getWord().toLowerCase(Locale.ROOT).equals("админ"))
                return result;
            if(!commandParser.getWord().toLowerCase(Locale.ROOT).equals("добавить"))
                return result;

            String username = commandParser.getWord();
            if(username.isEmpty()){
                result.add(new Message(log(
                        "Ответ на команду \"<b>"+message.getText() + "</b>\"\n" +
                                "\n" +
                                "Чтобы воспользоваться командой добавления администратора, " +
                                "нужно указать username или ID пользователя Telegram.\n" +
                                "\n" +
                                "Пример 1: \n" +
                                "<i>Админ добавить @username Учитель бота</i>\n" +
                                "Пример 2: \n" +
                                "<i>Админ добавить 123456789 Учитель бота</i>\n" +
                                "\n" +
                                "Если ты не знаешь этих данных, прикрепляю тебе список последних " +
                                "диалогов на этом аккаунте, можешь взять его ID из списка.\n" +
                                "Если пользователя, которого ты хочешь добавить администратором, " +
                                "нет в этом списке - пусть он напишет мне. Тогда он здесь появится.\n" +
                                "\n" +
                                getLastDialogList(tgAccount))));
                return result;
            }

            String reason = commandParser.getText();
            if(reason.isEmpty()){
                result.add(new Message(log(
                        "Ответ на команду \"<b>"+message.getText() + "</b>\"\n" +
                                "\n" +
                                "Чтобы воспользоваться командой добавления администратора, " +
                                "необходимо указать причину добавления администратора. Кто это, зачем админ, и т.д.\n" +
                                "\n" +
                                "Пример 1: \n" +
                                "<i>Админ добавить @username Учитель бота</i>\n" +
                                "Пример 2: \n" +
                                "<i>Админ добавить 123456789 Учитель бота</i>\n" +
                                "\n" +
                                "Без причины потом будет легко запутаться в списке администраторов, " +
                                "когда их будет много.")));
                return result;
            }

            User userToAdd = applicationManager.getMessageHistory().getUserByUsernameOrId(username);
            if(userToAdd == null){
                result.add(new Message(log(
                        "Ответ на команду \"<b>"+message.getText() + "</b>\"\n" +
                                "\n" +
                                "Пользователя, которого ты указал в команде я не могу найти в своей базе.\n" +
                                "Я могу добавлять администратором только тех, кто есть в моей базе.\n" +
                                "Попроси пользователя написать мне, и после этого отправь мне эту команду снова.\n" +
                                "Либо можешь проверить правильно ли ты написал его ID или Username.\n" +
                                "Отправляю тебе список тех, кто мне недавно писал:\n" +
                                "\n" +
                                getLastDialogList(tgAccount))));

                return result;
            }

            if(AdminList.this.has(userToAdd)){
                result.add(new Message(log(
                        "Ответ на команду \"<b>"+message.getText() + "</b>\"\n" +
                                "\n" +
                                "Ты пытаешься добавить администратором пользователя, который уже есть в списке администраторов.")));
                return result;
            }


            StringBuilder sb = new StringBuilder("Ответ на команду \"<b>"+message.getText() + "</b>\"\n\n");
            try {
                {
                    AdminListItem adminListItem = add(userToAdd, message.getAuthor(), reason);
                    sb.append("<b>Добавлен админ:</b> ").append(adminListItem.getUser()).append(";\n");
                    sb.append("<b>Ответственный:</b> ").append(adminListItem.getResponsible()).append(";\n");
                    sb.append("<b>Причина:</b> ").append(adminListItem.getComment()).append(";\n");
                    sb.append("Теперь задай этому администратору права с помощью команд:\n");
                    sb.append("<i>админ разрешить ...</i>\n");
                    sb.append("<i>админ запретить ...</i>\n");
                    sb.append("Текущий список администраторов:\n\n");
                }
                for (AdminListItem adminListItem:userList){
                    sb.append(adminListItem.toString());
                    sb.append("\n\n");
                }
                result.add(new Message(sb.toString()));
                return result;
            }
            catch (Exception e){
                e.printStackTrace();
                result.add(new Message(log(
                        "Ответ на команду \"<b>"+message.getText() + "</b>\"\n" +
                                "\n" +
                                e.getLocalizedMessage())));
                return result;
            }
            //never get here
        }

        private String getLastDialogList(TgAccount tgAccount){
            StringBuilder sb = new StringBuilder();
            ArrayList<User> chats =  applicationManager.getMessageHistory().getLastUsersListForAccount(tgAccount);
            for (int i=0; i<chats.size() && i < 10; i++){
                User user = chats.get(i);
                sb.append("<b>ID:</b> <code>").append(user.getId()).append("</code>\n")
                        .append("Username: @").append(user.getUsername()).append("\n")
                        .append("Имя: ").append(user.getFirst_name()).append(" ").append(user.getLast_name()).append("\n\n");
            }
            return sb.toString();
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = super.getHelp();
            result.add(new CommandDesc("Админ добавить @username Причина добавления", "Добавить пользователя в список администраторов. Чтобы получить больше справки, напиши \"Админ добавить\" без аргументов."));
            return result;
        }
    }

    /**
     * Команда "Админ"
     * Команда "Админ @username "
     * /AdminInfo_989898
     */
    private class ShowAdminCommand extends CommandModule{

        @Override
        public ArrayList<Message> processCommand(Message message, TgAccount tgAccount, AdminListItem admin) throws Exception {
            ArrayList<Message> result = super.processCommand(message, tgAccount, admin);

            CommandParser commandParser = new CommandParser(message.getText());
            String word1 = commandParser.getWord().toLowerCase(Locale.ROOT);
            if (!word1.equals("админы") && !word1.equals("admininfo") )
                return result;

            StringBuilder sb = new StringBuilder("Ответ на команду \"<b>"+message.getText() + "</b>\"\n\n");
            String username = commandParser.getWord();
            if(username.isEmpty()) {
                sb.append("<b>Список администраторов сейчас:</b>\n\n");
                for (AdminListItem adminListItem : userList) {
                    sb.append(adminListItem.toString());
                    sb.append("\n\n");
                }
                result.add(new Message(sb.toString()));
                return result;
            }
            AdminListItem adminListItem = getByUsernameOrId(username);
            if(adminListItem == null){
                result.add(new Message(log(
                        "Ответ на команду \"<b>"+message.getText() + "</b>\"\n\n" +
                                "Пользователь " + username + " не найден в списке администраторов.")));
                return result;
            }
            sb.append("Информация об администраторе ").append(username).append(":\n\n");
            sb.append(adminListItem.toStringFull());
            result.add(new Message(sb.toString()));
            return result;
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = super.getHelp();
            result.add(new CommandDesc("Админы или /AdminInfo", "Вывести список администраторов"));
            return result;
        }
    }

    /**
     * Команда "/AdminDelete_472147993"
     */
    private class RemAdminCommand extends CommandModule{
        @Override
        public ArrayList<Message> processCommand(Message message, TgAccount tgAccount, AdminListItem admin) throws Exception {
            ArrayList<Message> result = super.processCommand(message, tgAccount, admin);
            CommandParser commandParser = new CommandParser(message.getText());
            if(!commandParser.getWord().toLowerCase(Locale.ROOT).equals("admindelete"))
                return result;
            String username = commandParser.getWord();
            if(username.isEmpty()){
                result.add(new Message(log(
                        "Ответ на команду \"<b>"+message.getText() + "</b>\"\n\n" +
                                "Имя пользователя для удаления из администраторов не было получено.\n" +
                                "Пример правильной команды:\n" +
                                "/AdminDelete_"+admin.user.getId())));
                return result;
            }
            AdminListItem adminToDelete = getByUsernameOrId(username);
            if(adminToDelete == null){
                result.add(new Message(log(
                        "Ответ на команду \"<b>"+message.getText() + "</b>\"\n\n" +
                                "Пользователь " + username + " не найден в списке администраторов.")));
                return result;
            }

            if(rem(adminToDelete)){
                StringBuilder sb = new StringBuilder("Ответ на команду \"<b>"+message.getText() + "</b>\"\n\n");
                sb.append("Пользователь ").append(username).append(" удалён из списка администраторов.\n");
                sb.append("<b>Список администраторов сейчас:</b>\n\n");
                for (AdminListItem adminListItem:userList){
                    sb.append(adminListItem.toString()).append("\n\n");
                }
                result.add(new Message(sb.toString()));
            }
            else {
                result.add(new Message(log(
                        "Ответ на команду \"<b>"+message.getText() + "</b>\"\n\n" +
                                "Пользователь " + username + " по какой-то причине не был удалён из списка администраторов.")));
            }
            return result;
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = super.getHelp();
            result.add(new CommandDesc("AdminDelete @username", "Удаление администратора. Удобрее всего вызывать из информации об администраторе."));
            return result;
        }
    }

    /**
     * Команда "/AdminAllow_248067313_Learning"
     * Команда "/AdminDeny_248067313_DatabaseEdit"
     */
    private class EditAdminCommand extends CommandModule{
        @Override
        public ArrayList<Message> processCommand(Message message, TgAccount tgAccount, AdminListItem admin) throws Exception {
            ArrayList<Message> result = super.processCommand(message, tgAccount, admin);
            CommandParser commandParser = new CommandParser(message.getText());
            String firstWord = commandParser.getWord().toLowerCase(Locale.ROOT);
            if(!firstWord.equals("adminallow") && !firstWord.equals("admindeny"))
                return result;
            String username = commandParser.getWord();
            if(username.isEmpty()){
                result.add(new Message(log(
                        "Ответ на команду \"<b>"+message.getText() + "</b>\"\n\n" +
                                "Имя пользователя для изменения прав администратора и название права доступа не было получено.\n\n" +
                                "<b>Вот список возможных прав доступа:</b>\n" +
                                "<i>"+rightsAsString() + "</i>\n\n" +
                                "<b>Пример правильной команды:</b>\n" +
                                "/AdminDeny_"+admin.user.getId()+"_AdminsRead"
                )));
                return result;
            }
            AdminListItem adminToEdit = getByUsernameOrId(username);
            if(adminToEdit == null){
                result.add(new Message(log(
                        "Ответ на команду \"<b>"+message.getText() + "</b>\"\n\n" +
                                "Пользователь " + username + " не найден в списке администраторов.\n\n"+
                                "<b>Вот список возможных прав доступа:</b>\n" +
                                "<i>"+rightsAsString() + "</i>\n\n" +
                                "<b>Пример правильной команды:</b>\n" +
                                "/AdminDeny_"+admin.user.getId()+"_AdminsRead"
                )));
                return result;
            }
            String right = commandParser.getText();
            if(right.isEmpty()){
                result.add(new Message(log(
                        "Ответ на команду \"<b>"+message.getText() + "</b>\"\n\n" +
                                "Не было получено название пункта прав доступа, который нужно изменить.\n\n" +
                                "<b>Вот список возможных прав доступа:</b>\n" +
                                "<i>"+rightsAsString() + "</i>\n\n" +
                                "<b>Пример правильной команды:</b>\n" +
                                "/AdminDeny_"+admin.user.getId()+"_AdminsRead"
                )));
                return result;
            }

            for(AdminListItem.Right genericRight:AdminListItem.getGenericRightsList()){
                if(genericRight != null) {
                    if (genericRight.getCode().equals(right)) {
                        boolean newValue = firstWord.equals("adminallow");
                        adminToEdit.setAllowed(genericRight, newValue);
                        saveArrayToFile();
                        result.add(new Message(log(
                                "Ответ на команду \"<b>" + message.getText() + "</b>\"\n\n" +
                                        "Админу " + adminToEdit.user + " теперь " + (newValue ? "разрешено" : "запрещено") + " " + genericRight.getDescription() + ".\n\n" +
                                        "Вот полная информация об администраторе:\n" +
                                        adminToEdit.toStringFull()
                        )));
                        return result;
                    }
                }
            }

            result.add(new Message(log(
                    "Ответ на команду \"<b>"+message.getText() + "</b>\"\n\n" +
                            "Я не могу найти в списке прав доступа \""+right+"\".\n\n" +
                            "<b>Вот список возможных прав доступа:</b>\n" +
                            "<i>"+rightsAsString() + "</i>\n\n" +
                            "<b>Пример правильной команды:</b>\n" +
                            "/AdminDeny_"+admin.user.getId()+"_AdminsRead"
            )));
            return result;
        }

        private String rightsAsString(){
            AdminListItem.Right[] rights = AdminListItem.getGenericRightsList();
            StringBuilder result = new StringBuilder(" ");
            for (int i = 0; i < rights.length; i++) {
                if(rights [i] != null) {
                    result.append(rights[i]);
                    if (i < rights.length - 1)
                        result.append(", ");
                }
            }
            return result.toString().trim();
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = super.getHelp();
            result.add(new CommandDesc("AdminAllow @username", "Управление правами администратора - разрешить что-либо. Удобрее всего вызывать из информации об администраторе."));
            result.add(new CommandDesc("AdminDeny @username", "Управление правами администратора - запретить что-либо. Удобрее всего вызывать из информации об администраторе."));
            return result;
        }
    }
}
