package com.fsoft.ihabot.Utils;

/*
 * 2018-05-14
 * Этот класс представляет пользователя
 * в тех местах, где надо понять кто сделал что либо
 * например
 *
 * кто создал этот ответ?
 * кто написал это сообщение?
 * кто редактировал ответ?
 *
 *
 * Тут должно быть:
 * string имя соцсети(vk, telegram)
 * string однозначное имя пользователя без всяких там собачек, решеток
 * string Удобочитаемое имя
 *
 *
 * {"network":"vk","id":"drfailov","name":"Роман Папуша"}
 * */

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Objects;

public class UserTg {
    private long id = 0L;        //однозначное ID пользователя
    private String username = "";//текстовое username без собаки. Может быть не всегда.
    private String name = "";    //Удобочитаемое имя шоб его можно отображать на экране


    public UserTg() {
    }

    public UserTg(long id, String username, String name) {
        this.id = id;
        this.username = username;
        this.name = name;
    }

    public UserTg(JSONObject jsonObject) throws JSONException, ParseException {
        fromJson(jsonObject);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("username", username);
        jsonObject.put("name", name);
        return jsonObject;
    }
    public void fromJson(JSONObject jsonObject) throws JSONException, ParseException {
        id = jsonObject.optLong("id", id);
        name = jsonObject.optString("name", name);
        username = jsonObject.optString("username", username);
    }

    /*Считать аккаунты равными если в них совпадает айди либо username
    * */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserTg userTg = (UserTg) o;
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
        if(name != null)
            return name;
        if(username != null)
            return "@"+username;
        return "ID"+id;
    }
}
