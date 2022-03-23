package com.fsoft.ihabot.communucation.tg;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public class PhotoSize {
    private String file_id = "";
    private long file_size = 0;
    private int height = 0;
    private int width = 0;

    public PhotoSize() {
    }

    public PhotoSize(String file_id, long file_size, int height, int width) {
        this.file_id = file_id;
        this.file_size = file_size;
        this.height = height;
        this.width = width;
    }

    public PhotoSize(JSONObject jsonObject) throws JSONException, ParseException {
        fromJson(jsonObject);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        if(file_id != null)
            jsonObject.put("file_id", file_id);
        jsonObject.put("file_size", file_size);
        jsonObject.put("height", height);
        jsonObject.put("width", width);
        return jsonObject;
    }
    private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
        if(jsonObject.has("file_id"))
            file_id = jsonObject.getString("file_id");
        if(jsonObject.has("file_size"))
            file_size = jsonObject.getLong("file_size");
        if(jsonObject.has("height"))
            height = jsonObject.getInt("height");
        if(jsonObject.has("width"))
            width = jsonObject.getInt("width");
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

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}
