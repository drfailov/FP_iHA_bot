package com.fsoft.ihabot.communucation.tg;

import com.fsoft.ihabot.communucation.Account;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public class File {
    private String file_id = "";
    private long file_size = 0;
    private String file_path = "";

    public File() {
    }
    public File(String file_id, long file_size, String file_path) {
        this.file_id = file_id;
        this.file_size = file_size;
        this.file_path = file_path;
    }
    public File(JSONObject jsonObject) throws JSONException, ParseException {
        fromJson(jsonObject);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        if(file_id != null)
            jsonObject.put("file_id", file_id);
        jsonObject.put("file_size", file_size);
        if(file_path != null)
            jsonObject.put("file_path", file_path);
        return jsonObject;
    }
    private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
        if(jsonObject.has("file_id"))
            file_id = jsonObject.getString("file_id");
        if(jsonObject.has("file_size"))
            file_size = jsonObject.getLong("file_size");
        if(jsonObject.has("file_path"))
            file_path = jsonObject.getString("file_path");
    }

    public String getUrl(Account tgAccount){
        //https://api.telegram.org/file/bot<token>/<file_path>
        return "https://api.telegram.org/file/bot" + tgAccount.getId() + ":"+tgAccount.getToken()+"/"+file_path;
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

    public String getFile_path() {
        return file_path;
    }

    public void setFile_path(String file_path) {
        this.file_path = file_path;
    }
}

