package com.fsoft.ihabot.communucation.tg;

import com.fsoft.ihabot.communucation.Account;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public class Document {
    private String file_id = "";
    private long file_size = 0;
    private String file_name = "";

    public Document() {
    }
    public Document(String file_id, long file_size, String file_name) {
        this.file_id = file_id;
        this.file_size = file_size;
        this.file_name = file_name;
    }
    public Document(JSONObject jsonObject) throws JSONException, ParseException {
        fromJson(jsonObject);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        if(file_id != null)
            jsonObject.put("file_id", file_id);
        jsonObject.put("file_size", file_size);
        if(file_name != null)
            jsonObject.put("file_name", file_name);
        return jsonObject;
    }
    private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
        if(jsonObject.has("file_id"))
            file_id = jsonObject.getString("file_id");
        if(jsonObject.has("file_size"))
            file_size = jsonObject.getLong("file_size");
        if(jsonObject.has("file_name"))
            file_name = jsonObject.getString("file_name");
    }

    public String getFile_id() {
        return file_id;
    }

    public void setFile_id(String file_id) {
        this.file_id = file_id;
    }

    public long getFile_size() {
        return file_size;
    }

    public void setFile_size(long file_size) {
        this.file_size = file_size;
    }

    public String getFile_name() {
        return file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }
}
