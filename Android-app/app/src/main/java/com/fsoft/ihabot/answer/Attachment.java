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
    private String file_id = "";  //Как-то представить что файл уже загружен и существует онлайн

    public Attachment() {
    }
    public Attachment(JSONObject jsonObject) throws JSONException, ParseException {
        fromJson(jsonObject);
    }

    public boolean isPhoto(){
        return type.equals(TYPE_PHOTO);
    }
    public boolean isAudio(){
        return type.equals(TYPE_AUDIO);
    }
    public boolean isDoc(){
        return type.equals(TYPE_DOC);
    }
    public boolean isVideo(){
        return type.equals(TYPE_VIDEO);
    }
    public boolean isOnline(){
        return !file_id.isEmpty();
    }
    public boolean isLocal(){
        return !filename.isEmpty();
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        if(type != null)
            jsonObject.put("type", type);
        if(filename != null)
            jsonObject.put("file", filename);
        return jsonObject;
    }
    private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
        if(jsonObject.has("type"))
            type = jsonObject.getString("type");
        if(jsonObject.has("file"))
            filename = jsonObject.getString("file");
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFile_id() {
        return file_id;
    }

    public void setFile_id(String file_id) {
        this.file_id = file_id;
    }
}
