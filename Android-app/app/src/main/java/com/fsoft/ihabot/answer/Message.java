package com.fsoft.ihabot.answer;

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

    public String getText() {
        return text;
    }

    public ArrayList<Attachment> getAttachments() {
        return attachments;
    }
}
