package com.fsoft.ihabot.Utils;


import android.util.Log;

import com.fsoft.ihabot.ApplicationManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * класс для хранения в файлах значений типа ключей
 * Created by Dr. Failov on 31.03.2015.
 */

public class FileStorage {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private ApplicationManager applicationManager = null;
    private String fileName = null;
    private String filePath = null;
    private JSONObject jsonObject = null;

    public static boolean exists(String fileName, ApplicationManager applicationManager){
        return new File(applicationManager.getHomeFolder() + File.separator + fileName).exists();
    }
    public static boolean delete(String fileName, ApplicationManager applicationManager){
        return new File(applicationManager.getHomeFolder() + File.separator + fileName).delete();
    }

    public FileStorage(String fileName, ApplicationManager applicationManager){
        this.fileName = fileName;
        this.applicationManager = applicationManager;
        filePath = applicationManager.getHomeFolder() + File.separator + fileName;
        String fileData = readFromFile(filePath);
        try {
            jsonObject = new JSONObject(fileData);
        }catch (Exception e){
            //e.printStackTrace();
            log("Initializing new file: " + fileName);
            jsonObject = new JSONObject();
        }
    }
    public boolean commit(){
        String data = jsonObject.toString();
        return writeToFile(filePath, data);
    }
    public String getFilePath(){
        return filePath;
    }
    public boolean getBoolean(String key, boolean def) {
        try {
            return jsonObject.getBoolean(key);
        }
        catch (Exception e){
            return def;
        }
    }
    public int getInt(String key, int def) {
        try {
            return jsonObject.getInt(key);
        }
        catch (Exception e){
            return def;
        }
    }
    public float getFloat(String key, double def){
        return (float)getDouble(key, (float)def);
    }
    public double getDouble(String key, double def) {
        try {
            return jsonObject.getDouble(key);
        }
        catch (Exception e){
            return def;
        }
    }
    public long getLong(String key, long def) {
        try {
            return jsonObject.getLong(key);
        }
        catch (Exception e){
            return def;
        }
    }
    public String getString(String key, String def) {
        try {
            return jsonObject.getString(key);
        }
        catch (Exception e){
            return def;
        }
    }
    public Date getDate(String key, Date def){
        try{
            if(has(key))
                return DATE_FORMAT.parse(getString(key, ""));
            return def;
        }
        catch (Exception e){
            return def;
        }
    }
    public double[] getFloatArray(String key, double[] def){
        try{
            if(has(key)){
                JSONArray jsonArray = new JSONArray(getString(key, "[]"));
                double[] result = new double[jsonArray.length()];
                for (int i = 0; i < jsonArray.length(); i++)
                    result[i] = jsonArray.getDouble(i);
                return result;
            }
            return def;
        }
        catch (Exception e){
            return def;
        }
    }
    public long[] getLongArray(String key, long[] def){
        try{
            if(has(key)){
                JSONArray jsonArray = new JSONArray(getString(key, "[]"));
                long[] result = new long[jsonArray.length()];
                for (int i = 0; i < jsonArray.length(); i++)
                    result[i] = jsonArray.getLong(i);
                return result;
            }
            return def;
        }
        catch (Exception e){
            return def;
        }
    }
    public String[] getStringArray(String key, String[] def){
        try{
            if(has(key)){
                JSONArray jsonArray = new JSONArray(getString(key, "[]"));
                String[] result = new String[jsonArray.length()];
                for (int i = 0; i < jsonArray.length(); i++)
                    result[i] = jsonArray.getString(i);
                return result;
            }
            return def;
        }
        catch (Exception e){
            return def;
        }
    }
    public JSONArray getJsonArray(String key, JSONArray def) {
        try {
            return jsonObject.getJSONArray(key);
        }
        catch (Exception e){
            return def;
        }
    }

    public boolean has(String key) {
        return jsonObject.has(key);
    }
    public int length() {
        return jsonObject.length();
    }
    public boolean delete(){
        return FileStorage.delete(fileName, applicationManager);
    }

    public FileStorage put(String key, long value) {
        try {
            jsonObject.put(key, value);
        }
        catch (Exception e){
            log("! Не могу сохранить значение " + key + " в " + fileName + "!");
            e.printStackTrace();
        }
        return this;
    }
    public FileStorage put(String key, double value) {
        try {
            jsonObject.put(key, value);
        }
        catch (Exception e){
            log("! Не могу сохранить значение " + key + " в " + fileName + "!");
            e.printStackTrace();
        }
        return this;
    }
    public FileStorage put(String key, int value) {
        try {
            jsonObject.put(key, value);
        }
        catch (Exception e){
            log("! Не могу сохранить значение " + key + " в " + fileName + "!");
            e.printStackTrace();
        }
        return this;
    }
    public FileStorage put(String key, boolean value) {
        try {
            jsonObject.put(key, value);
        }
        catch (Exception e){
            log("! Не могу сохранить значение " + key + " в " + fileName + "!");
            e.printStackTrace();
        }
        return this;
    }
    public FileStorage put(String key, String value) {
        try {
            jsonObject.put(key, value);
        }
        catch (Exception e){
            log("! Не могу сохранить значение " + key + " в " + fileName + "!");
            e.printStackTrace();
        }
        return this;
    }
    public FileStorage put(String key, Date value) {
        try {
            jsonObject.put(key, DATE_FORMAT.format(value));
        }
        catch (Exception e){
            log("! Не могу сохранить значение " + key + " в " + fileName + "!");
            e.printStackTrace();
        }
        return this;
    }
    public FileStorage put(String key, String[] values) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < values.length; i++)
                jsonArray.put(values[i]);
            put(key, jsonArray);
        }
        catch (Exception e){
            log("! Не могу сохранить значение " + key + " в " + fileName + "!");
            e.printStackTrace();
        }
        return this;
    }
    public FileStorage put(String key, double[] values) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < values.length; i++)
                jsonArray.put(values[i]);
            put(key, jsonArray);
        }
        catch (Exception e){
            log("! Не могу сохранить значение " + key + " в " + fileName + "!");
            e.printStackTrace();
        }
        return this;
    }
    public FileStorage put(String key, long[] values) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < values.length; i++)
                jsonArray.put(values[i]);
            put(key, jsonArray);
        }
        catch (Exception e){
            log("! Не могу сохранить значение " + key + " в " + fileName + "!");
            e.printStackTrace();
        }
        return this;
    }
    public FileStorage put(String key, JSONArray value) {
        try {
            jsonObject.put(key, value);
        }
        catch (Exception e){
            log("! Не могу сохранить значение " + key + " в " + fileName + "!");
            e.printStackTrace();
        }
        return this;
    }

    private String log(String string){
        Log.d(F.TAG, string);
        return string;
    }
    private String readFromFile(String fileName) {
        try {
            File file = new File(fileName);
            if(!file.isFile()) {
                //log(". Файл отсутствует: " + fileName);
                return "";
            }
            BufferedReader br = new BufferedReader(new java.io.FileReader(file));
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    sb.append("\n");
                    line = br.readLine();
                }
                log(". Файл загружен: " + fileName);
                return sb.toString();
            } finally {
                br.close();
            }
        }
        catch (Exception e){
            log("! Ошибка загрузки: " + e.toString() + " - " + fileName);
            e.printStackTrace();
            return "";
        }
    }
    private boolean writeToFile(String filePath, String  data){
        try{
            File folder = new File(filePath).getParentFile();
            if(!folder.exists())
                folder.mkdirs();
            FileWriter fileWriter = new FileWriter(filePath);
            fileWriter.write(data);
            fileWriter.close();
            log(". Файл сохранен: " + filePath);
            return true;
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка сохранения файла: " + filePath);
            return false;
        }
    }

}
