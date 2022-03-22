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

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public class UserTg {
    private long id = 0L;        //однозначное имя пользователя без всяких там собачек, решеток
    private String username = "";//текстовое обращение. Может быть не всегда.
    private String name = "";    //Удобочитаемое имя


    public UserTg() {
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

}
