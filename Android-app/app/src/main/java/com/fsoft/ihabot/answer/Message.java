package com.fsoft.ihabot.answer;

import androidx.annotation.NonNull;

import com.fsoft.ihabot.communucation.tg.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * - Текст сообщения
 * - Вложения в сообщении
 * - кто отправил сообщение
 *
 * - Откуда это сообщение?
 *      - из лички
 *      - из чата
 *      - прямо из программы
 *
 * Created by Dr. Failov on 24.03.2022.
 */
public class Message {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm", Locale.US);
    //все возможные типы того, откуда это сообщение может быть получено
    //В зависимости от источника, мы будет заполнять разные доп. поля
    static public final String SOURCE_DIALOG = "dialog";
    static public final String SOURCE_CHAT = "chat";
    static public final String SOURCE_PROGRAM = "program";
    static public final String SOURCE_HTTP = "http";

    private String source = SOURCE_PROGRAM;//откуда мы эту хуйню получили
    private long message_id = 0L;           //Если это сообщение, то ID сообщения, или же ID коммента на стене, или же...
    private String text = "";               //что в этой хуйне написано
    private User author = null;               //кто эту хуйню написал
    private Date date = null;               //когда мы эту хуйню получили
    private final ArrayList<Attachment> attachments = new ArrayList<>();//что он к этой хуйне приложил

    public Message() {
    }

    public Message(String text) {
        this.text = text;
    }

    public Message(JSONObject jsonObject)throws JSONException, ParseException {
        fromJson(jsonObject);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        if(source != null)
            jsonObject.put("source", source);
        jsonObject.put("message_id", message_id);
        if(text != null)
            jsonObject.put("text", text);
        if(author != null)
            jsonObject.put("author", author.toJson());
        if(date != null)
            jsonObject.put("date", sdf.format(date));
        if(!attachments.isEmpty()){
            JSONArray jsonArray = new JSONArray();
            for(Attachment attachment:attachments)
                jsonArray.put(attachment.toJson());
            jsonObject.put("attachments", jsonArray);
        }
        return jsonObject;
    }

    private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
        if(jsonObject.has("source"))
            source = jsonObject.getString("source");
        if(jsonObject.has("message_id"))
            message_id = jsonObject.getLong("message_id");
        if(jsonObject.has("text"))
            text = jsonObject.getString("text");
        if(jsonObject.has("author"))
            author = new User(jsonObject.getJSONObject("author"));
        if(jsonObject.has("date"))
            date = sdf.parse(jsonObject.getString("date"));
        attachments.clear();
        if(jsonObject.has("attachments")) {
            JSONArray jsonArray = jsonObject.getJSONArray("attachments");
            for (int i = 0; i < jsonArray.length(); i++)
                attachments.add(new Attachment(jsonArray.getJSONObject(i)));
        }
    }


    public boolean isEmpty(){
        return attachments.isEmpty() && text.isEmpty();
    }
    public String getText() {
        return text;
    }

    public void setSourceDialog(){
        source = SOURCE_DIALOG;
    }

    public long getMessage_id() {
        return message_id;
    }

    public void setMessage_id(long message_id) {
        this.message_id = message_id;
    }

    public void setText(String text) {
        this.text = text;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getDate() {
        return date;
    }

    public ArrayList<Attachment> getAttachments() {
        return attachments;
    }
    public boolean hasAttachments(){
        return !attachments.isEmpty();
    }

    public void addAttachment(Attachment attachment){
        attachments.add(attachment);
    }

    /**
     * Выполняет поиск по вложениям в этом сообщении на предмет того содержит ли это сообщение
     * вложение, которое ссылается на файл filename.
     * Эта функция предназначена для использования в базе ответов: когда файл выгружен на сервер,
     * в базу ответов вносятся его FileID
     * @param filename Имя файла из папки вложений (не путь), который мы ищем
     * @return true если это сообщение содержит вложение которое ссылается на файл filename
     */
    public boolean hasAttachmentFilename(String filename){
        for (Attachment attachment:attachments){
            if(attachment.getFilename().equals(filename))
                return true;
        }
        return false;
    }

    /**
     * Находит в этом ответе вложение которое ссылается на filename и вносит в него FileID загруженный конкретным ботом
     * Эта функция предназначена для использования в базе ответов: когда файл выгружен на сервер,
     * в базу ответов вносятся его FileID
     * @param filename Имя файла из папки вложений (не путь), который мы ищем
     * @param fileID Данные которые надо прикрепить к Attachment
     */
    public void addAttachmentFileID(String filename, String fileID, long botId){
        for (Attachment attachment:attachments){
            if(attachment.getFilename().equals(filename))
                attachment.updateTgFile_id(botId, fileID);
        }
    }

    @NonNull
    @Override
    public String toString() {
        int photos = 0;
        int music = 0;
        int videos = 0;
        int documents = 0;
        for (int i = 0; i < attachments.size(); i++) {
            if(attachments.get(i).isPhoto()) photos++;
            if(attachments.get(i).isAudio()) music++;
            if(attachments.get(i).isVideo()) videos++;
            if(attachments.get(i).isDoc()) documents++;
        }
        String result = text;
        String attachments = "";
        if(photos != 0) attachments += " " + photos + " фото, ";
        if(music != 0) attachments += " " + music + " аудио, ";
        if(videos != 0) attachments += " " + videos + " видео, ";
        if(documents != 0) attachments += " " + documents + " файл, ";
        //    " 2 фото, 2 аудио, 2 видео, 2 файл, "
        attachments = attachments.trim();  //    "2 фото, 2 аудио, 2 видео, 2 файл,"
        attachments = attachments.substring(0, Math.max(attachments.length()-1, 0)); //    "2 фото, 2 аудио, 2 видео, 2 файл"
        if(!attachments.isEmpty())
            result = result + " (+" + attachments + ")"; ////    "Ответ (+2 фото, 2 аудио, 2 видео, 2 файл)"
        return result;
    }
}
