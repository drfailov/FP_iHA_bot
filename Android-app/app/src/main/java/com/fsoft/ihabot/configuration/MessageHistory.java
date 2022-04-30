package com.fsoft.ihabot.configuration;

import android.util.Log;

import androidx.annotation.Nullable;

import com.fsoft.ihabot.ApplicationManager;
import com.fsoft.ihabot.Utils.CommandDesc;
import com.fsoft.ihabot.Utils.CommandModule;
import com.fsoft.ihabot.Utils.CommandParser;
import com.fsoft.ihabot.Utils.F;
import com.fsoft.ihabot.answer.Message;
import com.fsoft.ihabot.communucation.tg.Chat;
import com.fsoft.ihabot.communucation.tg.TgAccount;
import com.fsoft.ihabot.communucation.tg.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * Через этот модуль будут проходить все входящие сообщения.
 * Он будет проводить статистический анализ.
 * Будет получать аккаунт телеграма откуда получено сообщение и само сообщение.
 *
 * Статистику будем вести раздельно по аккаунтам телеги.
 * Для каждого аккаунта будем вести список чатов и информацию про них.
 */
public class MessageHistory extends CommandModule {
    private final File messageHistoryFile;
    private final ArrayList<MessageHistoryTgAccount> messageHistoryTgAccounts = new ArrayList<>();

    public MessageHistory(ApplicationManager applicationManager) {
        messageHistoryFile = new File(applicationManager.getHomeFolder(), "MessageHistory.json");
        Log.d(F.TAG, "Восстановление истории чатов из : " + messageHistoryFile.getName());
        if(messageHistoryFile.isFile()) {
            try {
                String data = F.readFromFile(messageHistoryFile);
                Log.d(F.TAG, "Восстановление " + data.length() + " символов.");
                JSONArray jsonArray = new JSONArray(data);
                for (int i = 0; i < jsonArray.length(); i++)
                    messageHistoryTgAccounts.add(new MessageHistoryTgAccount(jsonArray.getJSONObject(i)));
                Log.d(F.TAG, "Восстановлена история аккаунтов: " + messageHistoryTgAccounts.size());
            } catch (Exception e) {
                Log.d(F.TAG, "Ошибка восстановления истории чатов : " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
        childCommands.add(new UserHistoryCommand());
    }

    /**
     * Выполняет поиск по всей истории сообщений с целью найти данные о пользователе по его ID в истории сообщений
     * @param userId какой телеграмовский userId ищем.
     * @return Если в истории такой персонаж есть, то его аккаунт. Если не найдено, то null
     */
    @Nullable
    public User getUserById(long userId){
        for (MessageHistoryTgAccount account:messageHistoryTgAccounts){
            User user = account.getUserById(userId);
            if(user != null)
                return user;
        }
        return null;
    }

    /**
     * Выполняет поиск по всей истории сообщений с целью найти данные о пользователе по его ID в текстовом виде или юзернейму
     * Нечувствительно к регистру.
     * @param userIdOrUsername текст его ID, либо username. Можно с собакой можно без.
     * @return Если в истории такой персонаж есть, то его аккаунт. Если не найдено, то null
     */
    @Nullable
    public User getUserByUsernameOrId(String userIdOrUsername){
        for (MessageHistoryTgAccount account:messageHistoryTgAccounts){
            User user = account.getUserByUsernameOrId(userIdOrUsername);
            if(user != null)
                return user;
        }
        return null;
    }

    //количество зарегистрированных сообщений с момента перезагрузки
    private int registeredMessages = 0;
    /**
     * Регистрирует сообщение в чате.
     * @param message сообщение которое было в чате. Входящее.
     * @param tgAccount аккаунт с которого это сообщение было принято
     * @param chat чат, откуда было получено сообщение
     */
    public void registerTelegramMessage(Chat chat, Message message, TgAccount tgAccount){
        if(tgAccount == null){
            Log.d(F.TAG, "Была вызвана функция registerTelegramMessage, " +
                    "но передан NULL TgAccount. Так нельзя. Фукнция не будет выполнена.");
            return;
        }
        if(message == null){
            Log.d(F.TAG, "Была вызвана функция registerTelegramMessage, " +
                    "но передан NULL message. Так нельзя. Фукнция не будет выполнена.");
            return;
        }
        if(chat == null){
            Log.d(F.TAG, "Была вызвана функция registerTelegramMessage, " +
                    "но передан NULL chat. Так нельзя. Фукнция не будет выполнена.");
            return;
        }
        if(message.getAuthor() == null) {
            Log.d(F.TAG, "Была вызвана функция registerTelegramMessage, " +
                    "но в сообщении не указан автор: " + message);
            return;
        }
        //если аккаунт уже есть, добавить
        for (MessageHistoryTgAccount messageHistoryTgAccount:messageHistoryTgAccounts){
            if(messageHistoryTgAccount.tgAccountId == tgAccount.getId()){
                messageHistoryTgAccount.registerTelegramMessage(chat, message);
                Log.d(F.TAG, "Сообщений с момента перезагрузки: " + registeredMessages);
                if(registeredMessages % 10 == 0)
                    messageHistoryWriteToFile();
                registeredMessages ++;
                return;
            }
        }
        //Если аккаунта нет, создать и добавить
        Log.d(F.TAG, "Регистрация аккаунта в истории: " + tgAccount.getScreenName());
        MessageHistoryTgAccount messageHistoryTgAccount = new MessageHistoryTgAccount(tgAccount.getId(), tgAccount.getScreenName());
        messageHistoryTgAccounts.add(messageHistoryTgAccount);
        messageHistoryTgAccount.registerTelegramMessage(chat, message);
        Log.d(F.TAG, "Сообщений с момента перезагрузки: " + registeredMessages);
        if(registeredMessages % 10 == 0)
            messageHistoryWriteToFile();
        registeredMessages ++;
    }

    /**
     * Получить список последних чатов на аккаунте.
     * Возвращает массив User
     * @param tgAccount Аккаунт для которого загружаем юзеров
     * @return Возвращает обьекты пользователей в истории чатов в порядке от самых новых к старым
     */
    public ArrayList<User> getLastUsersListForAccount(TgAccount tgAccount) {
        ArrayList<User> users = new ArrayList<>();
        for (MessageHistoryChat chat:getLastChatsListForAccount(tgAccount))
            if(chat.chatUser != null)
                users.add(chat.chatUser);
        return users;
    }

    /**
     * Считает количество сообщений от некоторого пользовалетя за последнюю минуту.
     * Используется для фильтрации спама, например.
     * @param user обьект юзера телеграма который мы ищем. В нём должен быть либо username либо ID
     * @return Количество сообщений от юзера. Если он не писал, соответственно, 0.
     */
    public int countMessagesLastMinute(User user){
        if(user == null)
            return 0;
        int result = 0;
        long minuteMs = 60 * 1000;
        long timeThreshold = Calendar.getInstance().getTime().getTime() - minuteMs;
        for(MessageHistoryTgAccount account:messageHistoryTgAccounts){
            for(MessageHistoryChat chat:account.chats){
                if(chat.chatUser != null) {
                    if (chat.chatUser.equals(user)) {
                        for (Message message : chat.messageHistory) {
                            if (message.getDate() != null) {
                                if (message.getDate().getTime() > timeThreshold) {
                                    result++;
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * отправляет массив сообщений которые писал этот пользователь, начиная с самых новых
     * @param usernameOrId текст его ID, либо username. Можно с собакой можно без.
     * @return массив сообщений которые писал этот пользователь, начиная с самых новых
     */
    public ArrayList<Message> getUserHistory(String usernameOrId){
        ArrayList<Message> result = new ArrayList<>();

        for (MessageHistoryTgAccount account:messageHistoryTgAccounts){
            for (MessageHistoryChat chat:account.chats){
                for (Message message:chat.messageHistory){
                    if(message.getAuthor().isIt(usernameOrId)){
                        result.add(message);
                    }
                }
            }
        }
        {//sort
            result.sort(new Comparator<Message>() {
                @Override
                public int compare(Message message, Message m1) {
                    return Long.compare(m1.getDate().getTime(), message.getDate().getTime());
                }
            });
        }
        return result;
    }

    /**
     * Возвращает список истории всех чатов начиная с самых новых
     * @return список истории всех чатов начиная с самых новых
     */
    public ArrayList<MessageHistoryChat> getLastChats(){
        ArrayList<MessageHistoryChat> result = new ArrayList<>();
        for (MessageHistoryTgAccount account:messageHistoryTgAccounts){
            result.addAll(account.chats);
        }
        {//sort
            result.sort(new Comparator<MessageHistoryChat>() {
                @Override
                public int compare(MessageHistoryChat message, MessageHistoryChat m1) {
                    return Long.compare(m1.getLastMessageDate().getTime(), message.getLastMessageDate().getTime());
                }
            });
        }
        return result;
    }



    /**
     * берёт весь массив из оперативки и прячет в файл
     */
    private void messageHistoryWriteToFile(){
        Log.d(F.TAG, "Сохранение истории чатов в файл " + messageHistoryFile.getName() + " ...");
        try{
            try(PrintWriter fileTmpWriter = new PrintWriter(messageHistoryFile)) {
                JSONArray jsonArray = new JSONArray();
                for (MessageHistoryTgAccount account:messageHistoryTgAccounts)
                    jsonArray.put(account.toJson());
                fileTmpWriter.println(jsonArray);
            }
            Log.d(F.TAG, "Сохранена история сообщений: " + messageHistoryFile.length() + " байт");
        }
        catch (Exception e){
            Log.d(F.TAG, "Ошибка сохранения истории чатов в файл: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    /**
     * Получить список последних чатов на аккаунте.
     * Возвращает массив "Внутренних" обьектов класса
     * @param tgAccount Аккаунт для которого загружаем чаты
     * @return Возвращает обьекты чатов в истории в порядке от самых новых к старым
     */
    private ArrayList<MessageHistoryChat> getLastChatsListForAccount(TgAccount tgAccount) {
        for (MessageHistoryTgAccount messageHistoryTgAccount:messageHistoryTgAccounts) {
            if (messageHistoryTgAccount.tgAccountId == tgAccount.getId()) {
                return messageHistoryTgAccount.getLastChatsList();
            }
        }
        Log.d(F.TAG, "Был запрошен список чатов для аккаунта, которого нет в истории: " + tgAccount.getScreenName() + " ("+tgAccount.getId()+")");
        return new ArrayList<>();
    }




    /**
     * Представляет в истории все данные о конкретном аккаунте телеграма.
     */
    private static class MessageHistoryTgAccount{
        private long tgAccountId = 0;
        private String tgAccountName = "";
        private ArrayList<MessageHistoryChat> chats = new ArrayList<>();

        public MessageHistoryTgAccount(long tgAccountId, String tgAccountName) {
            this.tgAccountId = tgAccountId;
            this.tgAccountName = tgAccountName;
        }

        public MessageHistoryTgAccount(JSONObject jsonObject) throws JSONException, ParseException{
            fromJson(jsonObject);
        }

        @Nullable
        public User getUserById(long userId){
            for (MessageHistoryChat chat:chats){
                User user = chat.getUserById(userId);
                if(user != null)
                    return user;
            }
            return null;
        }

        @Nullable
        public User getUserByUsernameOrId(String userIdOrUsername){
            for (MessageHistoryChat chat:chats){
                User user = chat.getUserByUsernameOrId(userIdOrUsername);
                if(user != null)
                    return user;
            }
            return null;
        }

        public void registerTelegramMessage(Chat chat, Message message){
            if(message == null){
                Log.d(F.TAG, "Была вызвана функция registerTelegramMessage, " +
                        "но передан NULL message. Так нельзя. Фукнция не будет выполнена.");
                return;
            }
            if(chat == null){
                Log.d(F.TAG, "Была вызвана функция registerTelegramMessage, " +
                        "но передан NULL chat. Так нельзя. Фукнция не будет выполнена.");
                return;
            }
            //если чат есть, добавить в него
            for (MessageHistoryChat messageHistoryChat:chats){
                if(messageHistoryChat.chatId == chat.getId()){
                    messageHistoryChat.registerTelegramMessage(message);
                    return;
                }
            }
            //если нет, создать и добавить
            String chatmame = (chat.getFirst_name() + " " + chat.getLast_name() + " " + chat.getUsername() + " " + chat.getTitle()).trim();
            Log.d(F.TAG, "Регистрация чата в истории: " + chatmame);
            User chatUser = null;
            if(chat.getType().equals("private") && message.getAuthor() != null)
                chatUser = message.getAuthor();
            MessageHistoryChat messageHistoryChat = new MessageHistoryChat(chat.getId(), chatmame, chat.getType(), chatUser);
            messageHistoryChat.registerTelegramMessage(message);
            chats.add(messageHistoryChat);
        }

        public JSONObject toJson() throws JSONException {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("tgAccountId", tgAccountId);
            if(tgAccountName != null)
                jsonObject.put("tgAccountName", tgAccountName);
            if(!chats.isEmpty()) {
                JSONArray jsonArray = new JSONArray();
                for (int i = 0; i < chats.size(); i++)
                    jsonArray.put(chats.get(i).toJson());
                jsonObject.put("chats", jsonArray);
            }
            return jsonObject;
        }

        private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
            if(jsonObject.has("tgAccountId"))
                tgAccountId = jsonObject.getLong("tgAccountId");
            if(jsonObject.has("tgAccountName"))
                tgAccountName = jsonObject.getString("tgAccountName");
            chats.clear();
            if(jsonObject.has("chats")){
                JSONArray jsonArray = jsonObject.getJSONArray("chats");
                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        JSONObject arrayItem = jsonArray.getJSONObject(i);
                        chats.add(new MessageHistoryChat(arrayItem));
                    }
                    catch (Exception e){
                        Log.d(F.TAG, "Ошибка восстановления аккаунта из истории: " + e.getLocalizedMessage() + ". Ну и х*й с ним.");
                    }
                }
            }
        }

        public ArrayList<MessageHistoryChat> getLastChatsList() {
            ArrayList<MessageHistoryChat> resultLast24h = new ArrayList<>();
            {//fill array with only last 24h chats
                long now = new Date().getTime();
                long time24h = 24 * 60 * 60 * 1000;
                for (MessageHistoryChat chat : chats) {
                    if(now - chat.getLastMessageDate().getTime() < time24h)
                        resultLast24h.add(chat);
                }
            }
            {//sort
                resultLast24h.sort(new Comparator<MessageHistoryChat>() {
                    @Override
                    public int compare(MessageHistoryChat messageHistoryChat, MessageHistoryChat t1) {
                        return Long.compare(t1.getLastMessageDate().getTime(), messageHistoryChat.getLastMessageDate().getTime());
                    }
                });
            }
            return resultLast24h;
        }

        public ArrayList<MessageHistoryChat> getChats() {
            return chats;
        }

        public void setChats(ArrayList<MessageHistoryChat> chats) {
            this.chats = chats;
        }

        public long getTgAccountId() {
            return tgAccountId;
        }

        public void setTgAccountId(long tgAccountId) {
            this.tgAccountId = tgAccountId;
        }

        public String getTgAccountName() {
            return tgAccountName;
        }

        public void setTgAccountName(String tgAccountName) {
            this.tgAccountName = tgAccountName;
        }

        /**
         * Сравнивает обьект между собой и с другими классами.
         * Такая странная штука нужна для того, чтобы можно было написать я Equals к массиву
         * @param o Обьект для сравнения. Поддерживает типы: MessageHistoryTgAccount, Long, TgAccount
         * @return true, если обьект на входе имеет тот же айди
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null ) return false;
            if(o.getClass() == Long.class)
                return tgAccountId == ((Long)o);
            if(o.getClass() == TgAccount.class)
                return tgAccountId == ((TgAccount)o).getId();
            if(getClass() != o.getClass()) return false;
            MessageHistoryTgAccount that = (MessageHistoryTgAccount) o;
            return tgAccountId == that.tgAccountId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(tgAccountId);
        }
    }

    /**
     * Представляет статистику по чатам.
     */
    private static class MessageHistoryChat{
        private static final int MESSAGES_LIMIT=20;
        private long chatId = 0;
        private String chatName = "";
        private String chatType = "";
        private User chatUser = null;
        private int totalMessageCounter = 0;
        private Date firstRegistered = null;
        private ArrayList<Message> messageHistory = new ArrayList<>();

        public MessageHistoryChat() {
            firstRegistered = new Date();
        }

        /**
         * @param chatId ID чата. для юзеров он совпадает с ID юзера
         * @param chatName Имя чата которое будет отображатся в отчётах
         * @param chatType тип чата который присылает телега
         * @param chatUser обьет телеговского юзера в случае если это чат приват
         */
        public MessageHistoryChat(long chatId, String chatName, String chatType, User chatUser) {
            this.chatId = chatId;
            this.chatName = chatName;
            this.chatType = chatType;
            this.chatUser = chatUser;
            firstRegistered = new Date();
        }

        public MessageHistoryChat(JSONObject jsonObject) throws JSONException, ParseException{
            fromJson(jsonObject);
        }

        @Nullable
        public User getUserByUsernameOrId(String usernameOrId){
            if(chatUser != null) {
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
                if(chatUser.getUsername() != null)
                    if (chatUser.getUsername().replace("@", "").toLowerCase(Locale.ROOT).trim().equals(usernameOrId))
                        return chatUser;
            }
            return null;
        }

        @Nullable
        public User getUserById(long userId){
            if(chatUser != null) {
                if (chatUser.getId() == userId)
                    return chatUser;
            }
            return null;
        }

        public void registerTelegramMessage(Message message){
            Log.d(F.TAG, "Регистрация сообщения в истории: " + message);
            totalMessageCounter ++;
            messageHistory.add(0, message);
            while (messageHistory.size() > MESSAGES_LIMIT)
                messageHistory.remove(MESSAGES_LIMIT);
        }

        public Date getLastMessageDate(){
            Date result = new Date(0);
            for (Message message:messageHistory){
                if(message.getDate() != null)
                    if(message.getDate().after(result))
                        result = message.getDate();
            }
            return result;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("chatId", chatId);
            if(chatName != null)
                jsonObject.put("chatName", chatName);
            if(chatType != null)
                jsonObject.put("chatType", chatType);
            jsonObject.put("totalMessageCounter", totalMessageCounter);
            if(firstRegistered != null)
                jsonObject.put("firstRegistered", new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(firstRegistered));
            if(chatUser != null)
                jsonObject.put("chatUser", chatUser.toJson());
            if(!messageHistory.isEmpty()) {
                JSONArray jsonArray = new JSONArray();
                for (int i = 0; i < messageHistory.size(); i++)
                    jsonArray.put(messageHistory.get(i).toJson());
                jsonObject.put("messageHistory", jsonArray);
            }
            return jsonObject;
        }

        private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
            if(jsonObject.has("chatId"))
                chatId = jsonObject.getLong("chatId");
            if(jsonObject.has("chatName"))
                chatName = jsonObject.getString("chatName");
            if(jsonObject.has("chatType"))
                chatType = jsonObject.getString("chatType");
            if(jsonObject.has("totalMessageCounter"))
                totalMessageCounter = jsonObject.getInt("totalMessageCounter");
            if(jsonObject.has("firstRegistered"))
                firstRegistered = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(jsonObject.getString("firstRegistered"));
            if(jsonObject.has("chatUser"))
                chatUser = new User(jsonObject.getJSONObject("chatUser"));
            messageHistory.clear();
            if(jsonObject.has("messageHistory")){
                JSONArray jsonArray = jsonObject.getJSONArray("messageHistory");
                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        JSONObject arrayItem = jsonArray.getJSONObject(i);
                        messageHistory.add(new Message(arrayItem));
                    }
                    catch (Exception e){
                        Log.d(F.TAG, "Ошибка восстановления сообщения из истории: " + e.getLocalizedMessage() + ". Ну и х*й с ним.");
                    }
                }
            }
        }

        public int getTotalMessageCounter() {
            return totalMessageCounter;
        }

        public void setTotalMessageCounter(int totalMessageCounter) {
            this.totalMessageCounter = totalMessageCounter;
        }

        public Date getFirstRegistered() {
            return firstRegistered;
        }

        public void setFirstRegistered(Date firstRegistered) {
            this.firstRegistered = firstRegistered;
        }

        public ArrayList<Message> getMessageHistory() {
            return messageHistory;
        }

        public void setMessageHistory(ArrayList<Message> messageHistory) {
            this.messageHistory = messageHistory;
        }

        public long getChatId() {
            return chatId;
        }

        public void setChatId(long chatId) {
            this.chatId = chatId;
        }

        public String getChatName() {
            return chatName;
        }

        public void setChatName(String chatName) {
            this.chatName = chatName;
        }

        public String getChatType() {
            return chatType;
        }

        public void setChatType(String chatType) {
            this.chatType = chatType;
        }
    }


    /**
     * Команда "история @username"
     * /History_@username
     */
    private class UserHistoryCommand extends CommandModule {

        @Override
        public ArrayList<Message> processCommand(Message message, TgAccount tgAccount, AdminList.AdminListItem admin) throws Exception {
            ArrayList<Message> result = super.processCommand(message, tgAccount, admin);

            CommandParser commandParser = new CommandParser(message.getText());
            String word1 = commandParser.getWord().toLowerCase(Locale.ROOT);
            if (!word1.equals("история") && !word1.equals("history") )
                return result;

            if(!admin.isAllowed(AdminList.AdminListItem.HISTORY_VIEW)){
                result.add(new Message("Нет доступа к команде."));
                return result;
            }

            StringBuilder sb = new StringBuilder("Ответ на команду \"<b>"+message.getText() + "</b>\"\n\n");
            String username = commandParser.getWord();
            if(username.isEmpty()) {
                sb.append("<b>Список чатов сейчас:</b>\n\n");
                ArrayList<MessageHistoryChat> chats = getLastChats();
                for (MessageHistoryChat chat : chats) {
                    sb.append("<b>");
                    sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(chat.getLastMessageDate()));
                    sb.append("</b>, ");
                    sb.append(chat.chatUser);
                    sb.append(";\n");
                    sb.append("⚡️ /History_");
                    sb.append(chat.chatUser.getId());
                    sb.append("\n\n");
                    if(sb.length() > 3800)
                        break;
                }
                result.add(new Message(sb.toString()));
                return result;
            }

            ArrayList<Message> historyMessages = getUserHistory(username);
            if(historyMessages.isEmpty()){
                sb.append("<b>История сообщений</b> c ").append(username).append(" пуста.");
            }
            if(!historyMessages.isEmpty()){
                Message lastMessage = historyMessages.get(0);
                sb.append("<b>История сообщений</b> c пользователем ").append(lastMessage.getAuthor()).append(":\n");
                sb.append("<i>(сохраняются только последние "+MessageHistoryChat.MESSAGES_LIMIT+" сообщений в чате)</i>\n\n");
            }
            for (Message historyMessage:historyMessages){
                sb.append("<b>");
                sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(historyMessage.getDate()));
                sb.append("</b>, ");
                sb.append(historyMessage);
                sb.append(";\n");
                if(sb.length() > 3800)
                    break;
            }

            result.add(new Message(sb.toString()));
            return result;
        }

        @Override
        public ArrayList<CommandDesc> getHelp(AdminList.AdminListItem requester) {
            ArrayList<CommandDesc> result = super.getHelp(requester);
            if(requester.isAllowed(AdminList.AdminListItem.HISTORY_VIEW))
                result.add(new CommandDesc("История @username", "Вывести последние сообщения пользователя боту"));
            return result;
        }
    }
}
