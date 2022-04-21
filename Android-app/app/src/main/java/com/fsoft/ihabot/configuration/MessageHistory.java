package com.fsoft.ihabot.configuration;

import android.util.JsonReader;
import android.util.Log;

import com.fsoft.ihabot.Utils.ApplicationManager;
import com.fsoft.ihabot.Utils.F;
import com.fsoft.ihabot.answer.Message;
import com.fsoft.ihabot.communucation.tg.Chat;
import com.fsoft.ihabot.communucation.tg.MessageEntity;
import com.fsoft.ihabot.communucation.tg.TgAccount;
import com.fsoft.ihabot.communucation.tg.TgAccountCore;

import org.json.JSONArray;
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
public class MessageHistory {
    private final File messageHistoryFile;
    private ArrayList<MessageHistoryTgAccount> messageHistoryTgAccounts = new ArrayList<>();

    public MessageHistory(ApplicationManager applicationManager) {
        messageHistoryFile = new File(applicationManager.getHomeFolder(), "MessageHistory.json");
        Log.d(F.TAG, "Восстановление истории чатов из : " + messageHistoryFile.getName());
        if(messageHistoryFile.isFile()) {
            try {
                JSONArray jsonArray = new JSONArray(F.readFromFile(messageHistoryFile));
                for (int i = 0; i < jsonArray.length(); i++)
                    messageHistoryTgAccounts.add(new MessageHistoryTgAccount(jsonArray.getJSONObject(i)));
                Log.d(F.TAG, "Восстановлена история аккаунтов: " + messageHistoryTgAccounts.size());
            } catch (Exception e) {
                Log.d(F.TAG, "Ошибка восстановления истории чатов : " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }

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
                messageHistoryWriteToFile();
                return;
            }
        }
        //Если аккаунта нет, создать и добавить
        Log.d(F.TAG, "Регистрация аккаунта в истории: " + tgAccount.getScreenName());
        MessageHistoryTgAccount messageHistoryTgAccount = new MessageHistoryTgAccount(tgAccount.getId(), tgAccount.getScreenName());
        messageHistoryTgAccounts.add(messageHistoryTgAccount);
        messageHistoryTgAccount.registerTelegramMessage(chat, message);
        messageHistoryWriteToFile();
    }

    private void messageHistoryWriteToFile(){
        Log.d(F.TAG, "Сохранение истории чатов в файл " + messageHistoryFile.getName() + " ...");
        try{
            try(PrintWriter fileTmpWriter = new PrintWriter(messageHistoryFile)) {
                JSONArray jsonArray = new JSONArray();
                for (MessageHistoryTgAccount account:messageHistoryTgAccounts)
                    jsonArray.put(account.toJson());
                fileTmpWriter.println(jsonArray);
            }
            Log.d(F.TAG, "Сохранена история аккаунтов: " + messageHistoryTgAccounts.size());
        }
        catch (Exception e){
            Log.d(F.TAG, "Ошибка сохранения истории чатов в файл: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public ArrayList<MessageHistoryTgAccount> getMessageHistoryTgAccounts() {
        return messageHistoryTgAccounts;
    }

    public void setMessageHistoryTgAccounts(ArrayList<MessageHistoryTgAccount> messageHistoryTgAccounts) {
        this.messageHistoryTgAccounts = messageHistoryTgAccounts;
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
            MessageHistoryChat messageHistoryChat = new MessageHistoryChat(chat.getId(), chatmame, chat.getType());
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
        private int totalMessageCounter = 0;
        private Date firstRegistered = null;
        private ArrayList<Message> messageHistory = new ArrayList<>();

        public MessageHistoryChat() {
            firstRegistered = new Date();
        }

        public MessageHistoryChat(long chatId, String chatName, String chatType) {
            this.chatId = chatId;
            this.chatName = chatName;
            this.chatType = chatType;
            firstRegistered = new Date();
        }

        public MessageHistoryChat(JSONObject jsonObject) throws JSONException, ParseException{
            fromJson(jsonObject);
        }

        public void registerTelegramMessage(Message message){
            Log.d(F.TAG, "Регистрация сообщения в истории: " + message);
            totalMessageCounter ++;
            messageHistory.add(0, message);
            while (messageHistory.size() > MESSAGES_LIMIT)
                messageHistory.remove(MESSAGES_LIMIT);
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

}
