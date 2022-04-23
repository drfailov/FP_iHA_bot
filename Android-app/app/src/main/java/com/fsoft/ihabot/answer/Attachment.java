package com.fsoft.ihabot.answer;

import com.fsoft.ihabot.ApplicationManager;

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
    private String attachmentsFilename = "";  //Имя файла, только если это вложение есть локально в папке attachments! (часть базы).
    private File fileToUpload = null; //обьект файла который нужно выгрузить на сервер не из базы (используется отдельно от filename). Не вносится в JSON.
    private String receivedFilename = ""; //если вложение было получено от собеседника, здесь мы сохраняем как он его назвал. Никакой связи с файловой системой это поле не несёт. Не вносится в JSON.
    private long size = 0; //размер файла в байтах
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
     * Поскольку использование FileID допускается только в пределах одного аккаунта,
     * для проверки требуется знать  ID бота, с аккаунта которого нужно использовать файл
     * @author Dr. Failov
     * */
    public boolean isOnlineTg(long botId){
        for (OnlineFile onlineFile:onlineFiles)
            if(onlineFile.isTelegram() && onlineFile.getBotAccount() == botId)
                return true;
        return false;
    }

    /**
     * Проверяет содержится ли это вложение в папке Attachments
     * Это показывает, подходит ли оно для добавления в базу ответов.
     * @param applicationManager Требуется для получения адреса папки вложений
     * @return  true если файл содержится во вложениях и его можно смело добавлять с базу
     * @author Dr. Failov
     */
    public boolean isLocalInAttachmentFolder(ApplicationManager applicationManager){
        if(attachmentsFilename == null)
            return false;
        if(attachmentsFilename.isEmpty())
            return false;
        return new File(applicationManager.getAnswerDatabase().getFolderAttachments(), attachmentsFilename).isFile();
    }

    /**
     * Проверяет существует ли этот файл локально, и соответственно,
     * подходит ли он для загрузки на сервер (есть ли откуда взять файл)
     * @return true если файл есть откуда взять.
     * @author Dr. Failov
     */
    public boolean isLocal(){
        return !attachmentsFilename.isEmpty() || fileToUpload != null;
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
        if(attachmentsFilename != null)
            jsonObject.put("attachmentsFilename", attachmentsFilename);
        jsonObject.put("size", size);
        if(!onlineFiles.isEmpty()) {
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
        if(jsonObject.has("file")) //its old name
            attachmentsFilename = jsonObject.getString("file");
        if(jsonObject.has("attachmentsFilename"))
            attachmentsFilename = jsonObject.getString("attachmentsFilename");
        if(jsonObject.has("size"))
            size = jsonObject.getLong("size");
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

    public String getAttachmentsFilename() {
        if(attachmentsFilename == null)
            return "";
        return attachmentsFilename;
    }

    public void setAttachmentsFilename(String attachmentsFilename) {
        this.attachmentsFilename = attachmentsFilename;
    }

    public String getReceivedFilename() {
        return receivedFilename;
    }

    public void setReceivedFilename(String receivedFilename) {
        this.receivedFilename = receivedFilename;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
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
