package com.fsoft.ihabot.communucation.tg;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Objects;

/*Telegram user according to telegram API*/
public class User {
    private long id = 0;
    private boolean is_bot = false;
    private String first_name = "";
    private String last_name = "";
    private String username = "";
    private String language_code = "";

    public User() {
    }

    public User(long id, String username, String first_name, String last_name) {
        this.id = id;
        this.first_name = first_name;
        this.last_name = last_name;
        this.username = username;
    }

    public User(JSONObject jsonObject) throws JSONException, ParseException {
        fromJson(jsonObject);
    }

    public User(long id, String username) {
        this.id = id;
        this.username = username;
    }

    public User(long id, boolean is_bot, String first_name, String last_name, String username, String language_code) {
        this.id = id;
        this.is_bot = is_bot;
        this.first_name = first_name;
        this.last_name = last_name;
        this.username = username;
        this.language_code = language_code;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("is_bot", is_bot);
        if(first_name != null)
            jsonObject.put("first_name", first_name);
        if(last_name != null)
            jsonObject.put("last_name", last_name);
        if(username != null)
            jsonObject.put("username", username);
        if(language_code != null)
            jsonObject.put("language_code", language_code);
        return jsonObject;
    }
    private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
        try {
            id = jsonObject.getLong("id");
            if (jsonObject.has("username"))
                username = jsonObject.getString("username");
            if (jsonObject.has("is_bot"))
                is_bot = jsonObject.getBoolean("is_bot");
            if (jsonObject.has("name")) //for backward compatibiluty with old format of users (before 2022-04)
                first_name = jsonObject.getString("name");
            if (jsonObject.has("first_name"))
                first_name = jsonObject.getString("first_name");
            if (jsonObject.has("last_name"))
                last_name = jsonObject.getString("last_name");
            if (jsonObject.has("language_code"))
                language_code = jsonObject.getString("language_code");
        }
        catch (Exception e){
            Log.d("USER ERROR " + e.getMessage(), jsonObject.toString());
            throw e;
        }
    }
    public String getName(){
        if(last_name.isEmpty() || first_name.isEmpty())
            return username;
        else
            return first_name + " " + last_name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isIs_bot() {
        return is_bot;
    }

    public void setIs_bot(boolean is_bot) {
        this.is_bot = is_bot;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLanguage_code() {
        return language_code;
    }

    public void setLanguage_code(String language_code) {
        this.language_code = language_code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User userTg = (User) o;
        if(id == userTg.id)
            return true;
        return username != null && userTg.username != null && userTg.username.equals(username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(!first_name.isEmpty())
            sb.append(first_name);
        sb.append(" ");
        if(!last_name.isEmpty())
            sb.append(last_name);
        if(!first_name.isEmpty() || !last_name.isEmpty())
            sb.append(" (");
        if(username != null)
            sb.append("@").append(username);
        else
            sb.append("ID <code>").append(id).append("</code>");
        if(!first_name.isEmpty() || !last_name.isEmpty())
            sb.append(")");
        return sb.toString().trim();
    }
}
