package com.fsoft.ihabot.communucation.tg;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

/**
 * https://core.telegram.org/bots/api#sticker
 * @author Dr. Failov
 */
public class Sticker {
    private String file_id = "";
    private int height = 0;
    private int width = 0;
    private long file_size = 0;
    private boolean is_animated = false;
    private boolean is_video = false;


    public Sticker(String file_id, int height, int width, boolean is_animated, boolean is_video) {
        this.file_id = file_id;
        this.height = height;
        this.width = width;
        this.is_animated = is_animated;
        this.is_video = is_video;
    }
    public Sticker(JSONObject jsonObject) throws JSONException, ParseException {
        fromJson(jsonObject);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        if(file_id != null)
            jsonObject.put("file_id", file_id);
        jsonObject.put("height", height);
        jsonObject.put("width", width);
        jsonObject.put("file_size", file_size);
        jsonObject.put("is_animated", is_animated);
        jsonObject.put("is_video", is_video);
        return jsonObject;
    }
    private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
        if(jsonObject.has("file_id"))
            file_id = jsonObject.getString("file_id");
        if(jsonObject.has("height"))
            height = jsonObject.getInt("height");
        if(jsonObject.has("width"))
            width = jsonObject.getInt("width");
        if(jsonObject.has("file_size"))
            file_size = jsonObject.getLong("file_size");
        if(jsonObject.has("is_animated"))
            is_animated = jsonObject.getBoolean("is_animated");
        if(jsonObject.has("is_video"))
            is_video = jsonObject.getBoolean("is_video");
    }

    public String getFile_id() {
        return file_id;
    }

    public void setFile_id(String file_id) {
        this.file_id = file_id;
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

    public boolean isIs_animated() {
        return is_animated;
    }

    public void setIs_animated(boolean is_animated) {
        this.is_animated = is_animated;
    }

    public boolean isIs_video() {
        return is_video;
    }

    public void setIs_video(boolean is_video) {
        this.is_video = is_video;
    }

    public long getFile_size() {
        return file_size;
    }

    public void setFile_size(long file_size) {
        this.file_size = file_size;
    }
}
