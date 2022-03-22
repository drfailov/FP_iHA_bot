package com.fsoft.ihabot.communucation.tg;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

/*
 * https://core.telegram.org/bots/api#message
 *
 * drfailov 2018-07-01
 *
 * */
public class Message {
    private long message_id = 0;
    private User from = null;
    private Date date = null;
    private Chat chat = null;
    private String text = "";
    private Message reply_to_message = null;

    private ArrayList<MessageEntity> entities = new ArrayList<>();

    public Message() {
    }

    public Message(JSONObject jsonObject) throws JSONException, ParseException {
        fromJson(jsonObject);
    }

    public Message(long message_id, User from, Date date, Chat chat, String text) {
        this.message_id = message_id;
        this.from = from;
        this.date = date;
        this.chat = chat;
        this.text = text;
    }

    @Override
    public String toString() {
        return from + ": " + text;
    }
    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message_id", message_id);
        if(from != null)
            jsonObject.put("from", from.toJson());
        if(date != null)
            jsonObject.put("date", date.getTime()/1000L);
        if(chat != null)
            jsonObject.put("chat", chat.toJson());
        if(reply_to_message != null)
            jsonObject.put("reply_to_message", reply_to_message.toJson());
        if(text != null)
            jsonObject.put("text", text);
        if(entities != null && !entities.isEmpty()) {
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < entities.size(); i++)
                jsonArray.put(entities.get(i).toJson());
            jsonObject.put("entities", jsonArray);
        }
        return jsonObject;
    }
    private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
        message_id = jsonObject.getLong("message_id");
        if(jsonObject.has("from"))
            from = new User(jsonObject.getJSONObject("from"));
        if(jsonObject.has("date"))
            date = new Date(jsonObject.getLong("date") * 1000L);
        if(jsonObject.has("chat"))
            chat = new Chat(jsonObject.getJSONObject("chat"));
        if(jsonObject.has("reply_to_message"))
            reply_to_message = new Message(jsonObject.getJSONObject("reply_to_message"));
        if(jsonObject.has("text"))
            text = jsonObject.getString("text");
        entities.clear();
        if(jsonObject.has("entities")){
            JSONArray jsonArray = jsonObject.getJSONArray("entities");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject arrayItem = jsonArray.getJSONObject(i);
                entities.add(new MessageEntity(arrayItem));
            }
        }
    }

    public long getMessage_id() {
        return message_id;
    }
    public void setMessage_id(long message_id) {
        this.message_id = message_id;
    }
    public User getFrom() {
        return from;
    }
    public void setFrom(User from) {
        this.from = from;
    }
    public Date getDate() {
        return date;
    }
    public void setDate(Date date) {
        this.date = date;
    }
    public Chat getChat() {
        return chat;
    }
    public void setChat(Chat chat) {
        this.chat = chat;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public ArrayList<MessageEntity> getEntities() {
        return entities;
    }
    public void setEntities(ArrayList<MessageEntity> entities) {
        this.entities = entities;
    }
    public Message getReply_to_message() {
        return reply_to_message;
    }
    public void setReply_to_message(Message reply_to_message) {
        this.reply_to_message = reply_to_message;
    }
}
