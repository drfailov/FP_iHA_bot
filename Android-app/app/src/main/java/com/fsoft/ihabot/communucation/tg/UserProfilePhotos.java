package com.fsoft.ihabot.communucation.tg;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;

public class UserProfilePhotos {
    private int total_count = 0;
    private ArrayList<ArrayList<PhotoSize>> arrayPhotos = new ArrayList<>();

    public UserProfilePhotos() {
    }

    public UserProfilePhotos(int total_count, ArrayList<ArrayList<PhotoSize>> photos) {
        this.total_count = total_count;
        this.arrayPhotos = photos;
    }
    public UserProfilePhotos(JSONObject jsonObject) throws JSONException, ParseException {
        fromJson(jsonObject);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("total_count", total_count);
        if(arrayPhotos != null && !arrayPhotos.isEmpty()) {
            JSONArray jsonArrayPhotos = new JSONArray();
            for (int i = 0; i < arrayPhotos.size(); i++){
                JSONArray jsonArrayPhoto = new JSONArray();
                ArrayList<PhotoSize> arrayPhoto = arrayPhotos.get(i);
                for (int j = 0; j < arrayPhoto.size(); j++){
                    jsonArrayPhoto.put(arrayPhoto.get(i).toJson());
                }
                jsonArrayPhotos.put(jsonArrayPhoto);
            }
            jsonObject.put("photos", jsonArrayPhotos);
        }
        return jsonObject;
    }
    private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
        if(jsonObject.has("total_count"))
            total_count = jsonObject.getInt("total_count");
        arrayPhotos.clear();
        if(jsonObject.has("photos")){
            JSONArray jsonArrayPhotos = jsonObject.getJSONArray("photos");
            for (int i = 0; i < jsonArrayPhotos.length(); i++) {
                JSONArray jsonArrayPhoto = jsonArrayPhotos.getJSONArray(i);
                ArrayList<PhotoSize> arrayPhoto = new ArrayList<>();
                for (int j = 0; j < jsonArrayPhoto.length(); j++) {
                    JSONObject arrayItem = jsonArrayPhoto.getJSONObject(j);
                    arrayPhoto.add(new PhotoSize(arrayItem));
                }
                arrayPhotos.add(arrayPhoto);
            }
        }
    }

    public int getTotal_count() {
        return total_count;
    }

    public void setTotal_count(int total_count) {
        this.total_count = total_count;
    }

    public ArrayList<ArrayList<PhotoSize>> getArrayPhotos() {
        return arrayPhotos;
    }

    public void setArrayPhotos(ArrayList<ArrayList<PhotoSize>> arrayPhotos) {
        this.arrayPhotos = arrayPhotos;
    }
}
