package com.fsoft.ihabot.communucation.tg;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public class Chat {
    private long id = 0;
    private String type = "";
    private String title = "";
    private String username = "";
    private String first_name = "";
    private String last_name = "";
    private String description = "";
    private boolean all_members_are_administrators = false;

    public Chat() {
    }

    public Chat(JSONObject jsonObject) throws JSONException, ParseException {
        fromJson(jsonObject);
    }

    @Override
    public String toString() {
        if(title == null || title.isEmpty())
            return first_name + " " + last_name;
        else
            return title;
    }
    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("all_members_are_administrators", all_members_are_administrators);
        if(type != null)
            jsonObject.put("type", type);
        if(title != null)
            jsonObject.put("title", title);
        if(description != null)
            jsonObject.put("description", description);
        if(first_name != null)
            jsonObject.put("first_name", first_name);
        if(last_name != null)
            jsonObject.put("last_name", last_name);
        if(username != null)
            jsonObject.put("username", username);
        return jsonObject;
    }
    private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
        id = jsonObject.getLong("id");
        if(jsonObject.has("all_members_are_administrators"))
            all_members_are_administrators = jsonObject.getBoolean("all_members_are_administrators");
        if(jsonObject.has("first_name"))
            first_name = jsonObject.getString("first_name");
        if(jsonObject.has("last_name"))
            last_name = jsonObject.getString("last_name");
        if(jsonObject.has("username"))
            username = jsonObject.getString("username");
        if(jsonObject.has("description"))
            description = jsonObject.getString("description");
        if(jsonObject.has("title"))
            title = jsonObject.getString("title");
        if(jsonObject.has("type"))
            type = jsonObject.getString("type");
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAll_members_are_administrators() {
        return all_members_are_administrators;
    }

    public void setAll_members_are_administrators(boolean all_members_are_administrators) {
        this.all_members_are_administrators = all_members_are_administrators;
    }
}

