package com.fsoft.ihabot.answer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Attachment {
    private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    public final static String TYPE_PHOTO = "photo";
    public final static String TYPE_AUDIO = "audio";
    public final static String TYPE_DOC = "doc";
    public final static String TYPE_VIDEO = "video";

    private String type = "";      //тип вложения из списка выще
    private String filename = "";  //Имя файла если это вложение есть локально в папке attachments (часть базы).
    private File fileToUpload = null; //обьект файла который нужно выгрузить на сервер не из базы (используется отдельно от filename)
    private String tg_file_id = "";  //fileID телеграмовскний сообщающий что что файл уже загружен и существует онлайн
    private String tg_file_id_date = ""; //Дата, когда этот fileId был присвоен

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
    public boolean isOnlineTg(){
        return !tg_file_id.isEmpty();
    }
    public boolean isLocal(){
        return !filename.isEmpty() || fileToUpload != null;
    }
    public Attachment setPhoto(){
        type = TYPE_PHOTO;
        return this;
    }
    public Attachment setDoc(){
        type = TYPE_DOC;
        return this;
    }
    public Attachment setFileToUpload(File file){
        fileToUpload = file;
        return this;
    }

    public File getFileToUpload() {
        return fileToUpload;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        if(type != null)
            jsonObject.put("type", type);
        if(filename != null)
            jsonObject.put("file", filename);
        if(tg_file_id != null)
            jsonObject.put("tg_file_id", tg_file_id);
        if(tg_file_id_date != null)
            jsonObject.put("tg_file_id_date", tg_file_id_date);
        return jsonObject;
    }
    private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
        if(jsonObject.has("type"))
            type = jsonObject.getString("type");
        if(jsonObject.has("file"))
            filename = jsonObject.getString("file");
        if(jsonObject.has("tg_file_id"))
            tg_file_id = jsonObject.getString("tg_file_id");
        if(jsonObject.has("tg_file_id_date"))
            tg_file_id_date = jsonObject.getString("tg_file_id_date");
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFilename() {
        if(filename == null)
            return "";
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getTgFile_id() {
        return tg_file_id;
    }

    public void updateTgFile_id(String file_id) {
        this.tg_file_id = file_id;
        tg_file_id_date = sdf.format(new Date());
    }
}
