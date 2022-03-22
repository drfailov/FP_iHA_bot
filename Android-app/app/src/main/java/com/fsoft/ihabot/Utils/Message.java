package com.fsoft.ihabot.Utils;

import java.util.ArrayList;
import java.util.Date;

public class Message {
    private long source_id = 0L;           //Если это чат, то ID чата, если это стена, то ID стены, и т.д.
    private long message_id = 0L;           //Если это сообщение, то ID сообщения, или же ID коммента на стене, или же...
    private String text = "";               //что в этой хуйне написано
    private UserTg author = null;               //кто эту хуйню написал
    private Date date = null;               //когда мы эту хуйню получили
    protected ArrayList<Attachment> attachments = new ArrayList<>();//что он к этой хуйне приложил
    protected ArrayList<UserTg> mentions = new ArrayList<>();//Кого он в этой хуйне упомянул
//    protected AccountBase botAccount = null;     // кто из ботов эту хуйню обнаружил


    public String getText() {
        return text;
    }
}
