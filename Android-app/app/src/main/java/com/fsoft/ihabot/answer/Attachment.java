package com.fsoft.ihabot.answer;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public class Attachment {
    public static String TYPE_PHOTO = "photo";
    public static String TYPE_AUDIO = "audio";
    public static String TYPE_DOC = "doc";
    public static String TYPE_VIDEO = "video";

    private String type = "";      //тип вложения из списка выще
    private String filename = "";  //Имя файла если это вложение есть локально в папке attachments (часть базы).
    private Object online = null;  //Как-то представить что файл уже загружен и существует онлайн

    public Attachment() {
    }
    public Attachment(JSONObject jsonObject) throws JSONException, ParseException {
        fromJson(jsonObject);
    }


    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        if(type != null)
            jsonObject.put("type", type);
        if(filename != null)
            jsonObject.put("filename", filename);
        return jsonObject;
    }
    private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
        if(jsonObject.has("type"))
            type = jsonObject.getString("type");
        if(jsonObject.has("filename"))
            filename = jsonObject.getString("filename");
    }
}
