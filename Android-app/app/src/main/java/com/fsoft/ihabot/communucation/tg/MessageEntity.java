package com.fsoft.ihabot.communucation.tg;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

/*
https://core.telegram.org/bots/api#messageentity

drfailov 2018-08-19
* */
public class MessageEntity {
    private String type = "";
    private int offset = 0;
    private int length = 0;
    private String url = "";
    private User user = null;

    public MessageEntity() {
    }
    public MessageEntity(String type, int offset, int length, String url, User user) {
        this.type = type;
        this.offset = offset;
        this.length = length;
        this.url = url;
        this.user = user;
    }
    public MessageEntity(String type, int offset, int length) {
        this.type = type;
        this.offset = offset;
        this.length = length;
    }
    public MessageEntity(String type, int offset, int length, User user) {
        this.type = type;
        this.offset = offset;
        this.length = length;
        this.user = user;
    }
    public MessageEntity(JSONObject jsonObject) throws JSONException, ParseException {
        fromJson(jsonObject);
    }


    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        if(type != null)
            jsonObject.put("type", type);
        if(offset != 0)
            jsonObject.put("offset", offset);
        if(length != 0)
            jsonObject.put("length", length);
        if(url != null)
            jsonObject.put("url", url);
        if(user != null)
            jsonObject.put("user", user.toJson());
        return jsonObject;
    }
    private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
        if(jsonObject.has("type"))
            type = jsonObject.getString("type");
        if(jsonObject.has("offset"))
            offset = jsonObject.getInt("offset");
        if(jsonObject.has("length"))
            length = jsonObject.getInt("length");
        if(jsonObject.has("url"))
            url = jsonObject.getString("url");
        if(jsonObject.has("user"))
            user = new User(jsonObject.getJSONObject("user"));
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public int getOffset() {
        return offset;
    }
    public void setOffset(int offset) {
        this.offset = offset;
    }
    public int getLength() {
        return length;
    }
    public void setLength(int length) {
        this.length = length;
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }
}