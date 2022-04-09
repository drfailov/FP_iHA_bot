package com.fsoft.ihabot.answer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class Attachment {
    public final static String TYPE_PHOTO = "photo";
    public final static String TYPE_AUDIO = "audio";
    public final static String TYPE_DOC = "doc";
    public final static String TYPE_VIDEO = "video";

    private String type = "";      //тип вложения из списка выще
    private String filename = "";  //Имя файла если это вложение есть локально в папке attachments (часть базы).
    private File fileToUpload = null; //обьект файла который нужно выгрузить на сервер не из базы (используется отдельно от filename)
    private final ArrayList<OnlineFile> onlineFiles = new ArrayList<>();  //например, fileID телеграмовскний сообщающий что что файл уже загружен и существует онлайн.

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
    /**
     * Проверяет загружен ли был этот ответ на сервер телеграма для конкретного бота (аккаунта)
     * */
    public boolean isOnlineTg(long botId){
        for (OnlineFile onlineFile:onlineFiles)
            if(onlineFile.isTelegram() && onlineFile.getBotAccount() == botId)
                return true;
        return false;
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
        if(onlineFiles != null) {
            JSONArray jsonArray = new JSONArray();
            for(OnlineFile onlineFile:onlineFiles)
                jsonArray.put(onlineFile.toJson());
            jsonObject.put("onlineFiles", jsonArray);
        }
        return jsonObject;
    }
    private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
        if(jsonObject.has("type"))
            type = jsonObject.getString("type");
        if(jsonObject.has("file"))
            filename = jsonObject.getString("file");
        onlineFiles.clear();
        if(jsonObject.has("onlineFiles")) {
            JSONArray jsonArray = jsonObject.getJSONArray("onlineFiles");
            for (int i = 0; i < jsonArray.length(); i++)
                onlineFiles.add(new OnlineFile(jsonArray.getJSONObject(i)));
        }
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

    public String getTgFileID(long botId) {
        for (OnlineFile onlineFile:onlineFiles)
            if(onlineFile.isTelegram() && onlineFile.getBotAccount() == botId)
                return onlineFile.fileID;
        return null;
    }

    public void updateTgFile_id(long botId, String file_id) {
        if(!isOnlineTg(botId)) {
            OnlineFile onlineFile = new OnlineFile(file_id, botId);
            onlineFiles.add(onlineFile);
        }
    }

    public static class OnlineFile{
        /*Every FileID can be used only with that bot who uploaded this file.
        So I need to store fileID as well as bot account ID
        * */
        private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        static private final String NETWORK_TG = "tg";

        private String network = NETWORK_TG;
        private String fileID = "";
        private long botAccount = 0L;
        private Date uploadDate = new Date();

        public OnlineFile(String fileID, long botAccount) {
            this.fileID = fileID;
            this.botAccount = botAccount;
        }

        public OnlineFile(JSONObject jsonObject)throws JSONException, ParseException {
            fromJson(jsonObject);
        }


        public boolean isTelegram() {
            return network.equals(NETWORK_TG);
        }

        public void setTelegram() {
            this.network = NETWORK_TG;
        }

        public String getFileID() {
            return fileID;
        }

        public void setFileID(String fileID) {
            this.fileID = fileID;
        }

        public long getBotAccount() {
            return botAccount;
        }

        public void setBotAccount(long botAccount) {
            this.botAccount = botAccount;
        }

        public Date getUploadDate() {
            return uploadDate;
        }

        public void setUploadDate(Date uploadDate) {
            this.uploadDate = uploadDate;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject jsonObject = new JSONObject();
            if(network != null)
                jsonObject.put("network", network);
            if(fileID != null)
                jsonObject.put("fileID", fileID);
            jsonObject.put("botAccount", botAccount);
            if(uploadDate != null)
                jsonObject.put("uploadDate", sdf.format(uploadDate));
            return jsonObject;
        }
        private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
            if(jsonObject.has("network"))
                network = jsonObject.getString("network");
            if(jsonObject.has("fileID"))
                fileID = jsonObject.getString("fileID");
            if(jsonObject.has("botAccount"))
                botAccount = jsonObject.getLong("botAccount");
            if(jsonObject.has("uploadDate"))
                uploadDate = sdf.parse(jsonObject.getString("uploadDate"));
        }
    }
}
