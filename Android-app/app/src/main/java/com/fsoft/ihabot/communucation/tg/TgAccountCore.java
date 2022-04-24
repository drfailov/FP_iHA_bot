package com.fsoft.ihabot.communucation.tg;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.fsoft.ihabot.ApplicationManager;
import com.fsoft.ihabot.communucation.Account;
import com.fsoft.ihabot.communucation.VolleyMultipartRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * Конкретно этот класс отвечает за зеркалирование функций, распределение нагрузки, обработку ошибок.
 *
 *
 * ВСЕ МЕТОДЫ ЭТОГО КЛАССА КРОМЕ МЕТОДОВ CommandModule АСИНХРОННЫ!!!
 * ВЫЗЫВАТЬ ТОЛЬКО ИЗ ВТОРОГО\ТРЕТЬЕГО ПОТОКА!
 * Этот класс должен отвечать только за редирект методов, за распределение нагрузки и
 * решение возникающих проблем. Также за процедуру логина.
 * Он же в идеале должен при вызове ЕГО методов извне обрабатывать ошибки, может, управлять очередью.
 * Если возникают эксепшоны, хэндлить их здесь.
 * Ну, выводить там окно повторного логина, например.
 *
 * Статус аккаунта описывать так:
 * + Аккаунт включен
 * + Токен OK
 * + Аккаунт запущен
 *
 * Общая схема такая:
 * - получаем пустой аккаунт
 * - выводим диалог логина
 * - работаем с ним пока не получим токен
 * - получаем токен
 * - выполняем тестовый запрос (любой)
 * - если тестовый запрос проходит, то token_ok = true
 * - если enabled = true то начинаем startAccount()
 * ...работаем
 * - если возникает ошибка то: token_ok=false; stopAccount();
 * - если ошибка не критическая, то: через 5 минут делаем startAccount() & token_ok=true
 *
 * Created by Dr. Failov on 21.07.2018.
 */

public class TgAccountCore extends Account {
    static public final int RETRIES = 3;//количество повторных попыток в случае ошибки
    //это имя пользователя которому принадлежит этот аккаунт. Оно хранится здесь временно.
    // Когда оно нам нужно, обращаемся к геттеру. если нужно получить имя аккаунта, обращаемся к toString()
    private String userName = null;
    private String telegraphToken = "";//бот использует телеграф для отправки лонгридов


    private long apiCounter = 0; //счётчик доступа к АПИ
    private long errorCounter = 0; //счётчик ошибок при доступе к АПИ
    private String currentUserPhotoUrl = null; //переменная для временного хранения здесь ссылки на фото профиля, чтобы каждый раз не грузить его заново
    private Boolean currentHasPhoto = null; //временный учёт наличия фотографии. Если фото нет то тут False.
    private RequestQueue queue = null;

    public TgAccountCore(ApplicationManager applicationManager, String fileName) {
        super(applicationManager, fileName);
        userName = getFileStorage().getString("userName", userName);
        telegraphToken = getFileStorage().getString("telegraphToken", telegraphToken);
        queue = Volley.newRequestQueue(applicationManager.getContext().getApplicationContext());
        //Proxy proxy = new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved("192.168.1.11", 8118));
    }

    @Override
    protected void checkTokenValidity(final OnTokenValidityCheckedListener listener) {
        super.checkTokenValidity(listener);
        if(getId() == 0) {
            listener.onTokenFail();
            log("В аккаунте " + this + " " + state("некорректный ID"));
            return;
        }
        if(getToken() == null || getToken().isEmpty()) {
            listener.onTokenFail();
            log("В аккаунте " + this + " " + state("некорректный токен"));
            return;
        }
        getMe(new GetMeListener() {
            @Override
            public void gotUser(User user) {
                log("Аккаунт " + this + " " + state("прошёл проверку"));
                listener.onTokenPass();
            }

            @Override
            public void error(Throwable error) {
                log("Аккаунт " + this + " " + state("не прошёл проверку токена: " + error.getClass().getName() + " " + error.getMessage()));
                listener.onTokenFail();
            }
        });
    }

    @Override
    public void startAccount() {
        super.startAccount();
    }

    @Override
    public void stopAccount() {
        super.stopAccount();
    }

    @Override
    public String toString() {
        return getScreenName() + "("+userName+", id="+getId()+")";
    }

    public String getUserName() {
        return userName;
    }
    public long getApiCounter() {
        return apiCounter;
    }
    public void incrementApiCounter(){
        apiCounter++;
    }
    public void incrementErrorCounter(){
        errorCounter++;
    }
    public long getErrorCounter() {
        return errorCounter;
    }
    public void setUserName(String userName) {
        this.userName = userName;
        getFileStorage().put("userName", userName).commit();
    }
    public void setTelegraphToken(String telegraphToken) {
        this.telegraphToken = telegraphToken;
        getFileStorage().put("telegraphToken", telegraphToken).commit();
    }
    public String getTelegraphToken(){
        return telegraphToken;
    }

    public void publishOnTelegraph(final CreateTelegraphPageListener listener, final String text){
        log(". Publishing text on Telegra.ph...");

        if(getTelegraphToken().isEmpty()){
            log(". Creating Telegra.ph account...");
            createTelegraphAccount(new CreateTelegraphAccountListener() {
                @Override
                public void accountCreated(String token) {
                    log(". Telegra.ph account created!");
                    setTelegraphToken(token);
                    publishOnTelegraph(listener, text);
                }

                @Override
                public void error(Throwable error) {
                    error.printStackTrace();
                    log(". Creating Telegra.ph account error: " + error.toString());
                }
            }, "iHA bot");
            return;
        }
        createTelegraphPage(listener, "iHA bot message", text);
    }


    public void getMe(final GetMeListener listener){
        final String url ="https://api.telegram.org/bot"+getId()+":"+getToken()+"/getMe";
        log("GetMe: " + url);
        incrementApiCounter();
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            log(". Got response: " + response);
                            JSONObject jsonObject = new JSONObject(response);
                            if(!jsonObject.has("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception("No OK in response!"));
                                return;
                            }
                            if(!jsonObject.getBoolean("ok")){
                                incrementErrorCounter();
                                listener.error(new Exception(jsonObject.optString("description", "No description")));
                                return;
                            }
                            JSONObject result = jsonObject.getJSONObject("result");
                            User user = new User(result);
                            setScreenName(user.getFirst_name() + " " + user.getLast_name());
                            setUserName(user.getUsername());
                            listener.gotUser(user);
                        }
                        catch (Exception e){
                            listener.error(e);
                            e.printStackTrace();
                            incrementErrorCounter();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sending request: " + url);
                listener.error(error);
                error.printStackTrace();
                incrementErrorCounter();
            }
        });
        // Add the request to the RequestQueue.
        stringRequest.setRetryPolicy(new DefaultRetryPolicy( 30000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(stringRequest);
    }
    public void getUpdates(final GetUpdatesListener listener, long offset, int timeout){
        final String url ="https://api.telegram.org/bot"+getId()+":"+getToken()+"/getUpdates?offset="+offset+"&timeout="+timeout;
        log("Getting updates: " + url);
        incrementApiCounter();
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            log(". Got response: " + response);
                            JSONObject jsonObject = new JSONObject(response);
                            if(!jsonObject.has("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception("No OK in response!"));
                                return;
                            }
                            if(!jsonObject.getBoolean("ok")){
                                incrementErrorCounter();
                                listener.error(new Exception(jsonObject.optString("description", "No description")));
                                return;
                            }
                            JSONArray result = jsonObject.getJSONArray("result");
                            ArrayList<Update> updates = new ArrayList<>();
                            for (int i = 0; i < result.length(); i++) {
                                JSONObject updateJson = result.getJSONObject(i);
                                Update update = new Update(updateJson);
                                updates.add(update);
                            }
                            listener.gotUpdates(updates);
                            state("Информация успешно обновлена");
                        }
                        catch (Exception e){
                            listener.error(e);
                            e.printStackTrace();
                            incrementErrorCounter();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sending request: " + url);
                listener.error(error);
                error.printStackTrace();
                incrementErrorCounter();
            }
        });
        stringRequest.setRetryPolicy(new DefaultRetryPolicy( 30000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
    public void sendMessage(final SendMessageListener listener, final long chat_id, com.fsoft.ihabot.answer.Message message){
        if(message.getText().length() > 3990)
            message.setText(message.getText().substring(0, 3990));
        JSONObject jsonObject = new JSONObject();
        try {
            //text = URLEncoder.encode(text, "UTF-8");
            jsonObject.put("chat_id", chat_id);
            jsonObject.put("parse_mode", "HTML");
            jsonObject.put("text", message.getText());
            if(message.getReplyToMessage() != null){
                jsonObject.put("reply_to_message_id", message.getReplyToMessage().getMessage_id());
            }
        }
        catch (Exception e){
            log("! Error building JSON: " + e.toString());
            e.printStackTrace();
        }
        final String url ="https://api.telegram.org/bot"+getId()+":"+getToken()+"/sendMessage";//?chat_id="+chat_id+"&text="+text;
        log("Sending message: " + url);
        log(jsonObject.toString());
        incrementApiCounter();
        // Request a string response from the provided URL.
        JsonObjectRequest stringRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        try{
                            log("Sending message response: " + jsonObject.toString());
                            if(!jsonObject.has("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception("No OK in response!"));
                                return;
                            }
                            if(!jsonObject.getBoolean("ok")){
                                incrementErrorCounter();
                                listener.error(new Exception(jsonObject.optString("description", "No description")));
                                return;
                            }
                            JSONObject result = jsonObject.getJSONObject("result");
                            Message message = new Message(result);
                            listener.sentMessage(message);
                            state("Успешно отправлено текстовое сообщение");
                        }
                        catch (Exception e){
                            listener.error(e);
                            e.printStackTrace();
                            incrementErrorCounter();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Caused by: java.net.UnknownHostException: Unable to resolve host "api.telegram.org": No address associated with hostname
                log(error.getClass().getName() + " while sending request: " + url);
                error.printStackTrace();
                listener.error(error);
                incrementErrorCounter();
            }
        });
        stringRequest.setRetryPolicy(new DefaultRetryPolicy( 50000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
    public void getMyPhotoUrl(final GetUserPhotoListener listener){
        if(currentHasPhoto != null && !currentHasPhoto)
            listener.noPhoto();
        if(currentHasPhoto != null && currentHasPhoto)
            listener.gotPhoto(currentUserPhotoUrl);
        if(currentHasPhoto == null) {
            if (currentUserPhotoUrl != null) {
                listener.gotPhoto(currentUserPhotoUrl);
            } else {
                getUserPhotoUrl(new GetUserPhotoListener() {
                    @Override
                    public void gotPhoto(String url) {
                        currentUserPhotoUrl = url;
                        currentHasPhoto = true;
                        listener.gotPhoto(url);
                    }

                    @Override
                    public void noPhoto() {
                        currentHasPhoto = false;
                        listener.noPhoto();
                    }

                    @Override
                    public void error(Throwable error) {
                        listener.error(error);
                    }
                }, getId());
            }
        }
    }
    public void getUserPhotoUrl(final GetUserPhotoListener listener, final long user_id){
        log(". Получение фотографии пользователя " + user_id + "...");
        getUserProfilePhotos(new GetUserProfilePhotosListener() {
            @Override
            public void gotPhotos(UserProfilePhotos photos) {
                log(". Запрос на архив фотографий " + user_id + " выполнен.");
                if(photos.getArrayPhotos().isEmpty()) {
                    log("! У юзера нет фотографий!");
                    listener.noPhoto();
                    return;
                }
                if(photos.getArrayPhotos().get(0).isEmpty()) {
                    log("! У фото нет размера!");
                    listener.noPhoto();
                    return;
                }
                PhotoSize photoSize = photos.getArrayPhotos().get(0).get(1);
                final String file_id = photoSize.getFile_id();
                getFile(new GetFileListener() {
                    @Override
                    public void gotFile(File file) {
                        log(". Запрос на файл фотографии " + user_id + " выполнен.");
                        if(file.getFile_path().isEmpty())
                            listener.error(new Exception(log("! Почему-то не получен адрес файла")));
                        String url = file.getUrl(TgAccountCore.this);
                        log(". Ссылка на фотографию:   " + url);
                        listener.gotPhoto(url);
                    }

                    @Override
                    public void error(Throwable error) {
                        log("! Ошибка выполнения запроса на файл фотографи " + user_id + ": "+error.getMessage()+".");
                        listener.error(error);
                    }
                }, file_id);
            }

            @Override
            public void error(Throwable error) {
                log("! Ошибка выполнения запроса на архив фотографий " + user_id + ": "+error.getMessage()+".");
                listener.error(error);
            }
        }, user_id, 0, 1);
    }
    public void getUserProfilePhotos(final GetUserProfilePhotosListener listener, final long user_id, int offset, int limit){
        final String url ="https://api.telegram.org/bot"+getId()+":"+getToken()+"/getUserProfilePhotos?user_id="+user_id+"&offset="+offset+"&limit="+limit;
        log(". Sending request: " + url);
        incrementApiCounter();
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            log(". Got response: " + response);
                            JSONObject jsonObject = new JSONObject(response);
                            if(!jsonObject.has("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception("No OK in response!"));
                                return;
                            }
                            if(!jsonObject.getBoolean("ok")){
                                incrementErrorCounter();
                                listener.error(new Exception(jsonObject.optString("description", "No description")));
                                return;
                            }
                            JSONObject result = jsonObject.getJSONObject("result");
                            UserProfilePhotos userProfilePhotos = new UserProfilePhotos(result);
                            listener.gotPhotos(userProfilePhotos);
                            state("Успешно загружен список фотографий в аккануте");
                        }
                        catch (Exception e){
                            listener.error(e);
                            e.printStackTrace();
                            incrementErrorCounter();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sending request: " + url);
                listener.error(error);
                error.printStackTrace();
                incrementErrorCounter();
            }
        });
        stringRequest.setRetryPolicy(new DefaultRetryPolicy( 50000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
    public void getFile(final GetFileListener listener, String file_id){
        final String url ="https://api.telegram.org/bot"+getId()+":"+getToken()+"/getFile?file_id="+file_id;
        log(". Sending request: " + url);
        incrementApiCounter();
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            log(". Got response: " + response);
                            JSONObject jsonObject = new JSONObject(response);
                            if(!jsonObject.has("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception("No OK in response!"));
                                return;
                            }
                            if(!jsonObject.getBoolean("ok")){
                                incrementErrorCounter();
                                listener.error(new Exception(jsonObject.optString("description", "No description")));
                                return;
                            }
                            JSONObject result = jsonObject.getJSONObject("result");
                            File file = new File(result);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    listener.gotFile(file);
                                }
                            }).start();
                            state("Успешно загружена информация о файле по ID");
                        }
                        catch (Exception e){
                            listener.error(e);
                            e.printStackTrace();
                            incrementErrorCounter();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sending request: " + url);
                listener.error(error);
                error.printStackTrace();
                incrementErrorCounter();
            }
        });
        stringRequest.setRetryPolicy(new DefaultRetryPolicy( 30000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
    public void sendChatTyping(final long chat_id){
        sendChatAction(chat_id, "typing");
    }
    public void sendChatAction(final long chat_id, final String action){
        JSONObject jsonObject = new JSONObject();
        try {
            //text = URLEncoder.encode(text, "UTF-8");
            jsonObject.put("chat_id", chat_id);
            jsonObject.put("action", action);
        }
        catch (Exception e){
            log("! Error building JSON: " + e.toString());
            e.printStackTrace();
        }

        final String url ="https://api.telegram.org/bot"+getId()+":"+getToken()+"/sendChatAction";
        log("sendChatAction: " + chat_id + ", " + action);
        incrementApiCounter();
        // Request a string response from the provided URL.
        JsonObjectRequest stringRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try{
                            log("sendChatAction response: " + response);
                        }
                        catch (Exception e){
                            e.printStackTrace();
                            incrementErrorCounter();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sendChatAction: " + url);
                error.printStackTrace();
                incrementErrorCounter();
            }
        });
        // Add the request to the RequestQueue.
        //stringRequest.setRetryPolicy(new DefaultRetryPolicy( 30000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(stringRequest);
    }
    public void sendPhoto(final SendMessageListener listener, final long chat_id, final String text, final java.io.File f, final long reply_to_message_id){
        try {
            if(!f.isFile())
                throw new Exception("Попытка отправить под видом фотографии файл которого не существует.");
            if(f.length() > 9000000)
                throw new Exception("Попытка отправить под видом фотографии файл размер которого больше 9мб.");
            final String url ="https://api.telegram.org/bot"+getId()+":"+getToken()+"/sendPhoto";
            log("Uploading photo file: " + url);
            incrementApiCounter();
            VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, url, new Response.Listener<NetworkResponse>() {
                @Override
                public void onResponse(NetworkResponse response) {
                    String resultResponse = new String(response.data);
                    log("Upload photo result: " + resultResponse);

                    try{
                        JSONObject jsonObject = new JSONObject(resultResponse);
                        log("Sending message response: " + jsonObject.toString());
                        if(!jsonObject.has("ok")) {
                            incrementErrorCounter();
                            listener.error(new Exception("No OK in response!"));
                            return;
                        }
                        if(!jsonObject.getBoolean("ok")){
                            incrementErrorCounter();
                            listener.error(new Exception(jsonObject.optString("description", "No description")));
                            return;
                        }
                        JSONObject result = jsonObject.getJSONObject("result");
                        Message message = new Message(result);
                        listener.sentMessage(message);
                        state("Успешно выгружен файл фото");
                    }
                    catch (Exception e){
                        listener.error(e);
                        e.printStackTrace();
                        incrementErrorCounter();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    log("Upload photo error: " + error.networkResponse);
                    error.printStackTrace();
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("chat_id", String.valueOf(chat_id));
                    params.put("parse_mode", "HTML");
                    if(text.length() > 1020)
                        params.put("caption", text.substring(0, 1020));
                    else
                        params.put("caption", text);
                    if(reply_to_message_id != 0){
                        params.put("reply_to_message_id", String.valueOf(reply_to_message_id));
                    }
                    return params;
                }

                @Override
                protected Map<String, DataPart> getByteData() {
                    Map<String, DataPart> params = new HashMap<>();
                    params.put("photo", new DataPart(f));
                    return params;
                }
            };

            queue.add(multipartRequest);
        }
        catch (Exception e){
            if(listener != null)
                listener.error(e);
        }
    }

    /**
     * Отправить этим аккаунтом телеграма файл вложением. Ограничение составляет 45 мегабайт.
     * @param listener Что делать при успешной загрузке либо при ошибке
     * @param chat_id Айди чата к кому надо отправлять.
     * @param text Подпись к документу
     * @param f Собственно файл. У программы должен быть полный к нему доступ.
     * @param reply_to_message_id Если нужно отправить сообщение как ответ, использовать айди сообшения в чате. Если нет, отправить 0
     */
    public void sendDocument(final SendMessageListener listener, final long chat_id, final String text, final java.io.File f, final long reply_to_message_id){
        try {
            if(!f.isFile())
                throw new Exception("Попытка отправить под видом документа файл которого не существует.");
            if(f.length() > 47000000)
                throw new Exception("Попытка отправить под видом документа файл размер которого больше 45мб.");
            final String url ="https://api.telegram.org/bot"+getId()+":"+getToken()+"/sendDocument";
            log("Uploading document file: " + url);
            incrementApiCounter();
            VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, url, new Response.Listener<NetworkResponse>() {
                @Override
                public void onResponse(NetworkResponse response) {
                    String resultResponse = new String(response.data);
                    log("Upload document result: " + resultResponse);

                    try{
                        JSONObject jsonObject = new JSONObject(resultResponse);
                        log("Sending document response: " + jsonObject.toString());
                        if(!jsonObject.has("ok")) {
                            incrementErrorCounter();
                            listener.error(new Exception("No OK in response!"));
                            return;
                        }
                        if(!jsonObject.getBoolean("ok")){
                            incrementErrorCounter();
                            listener.error(new Exception(jsonObject.optString("description", "No description")));
                            return;
                        }
                        JSONObject result = jsonObject.getJSONObject("result");
                        Message message = new Message(result);
                        listener.sentMessage(message);
                        state("Успешно выгружен файл документа");
                    }
                    catch (Exception e){
                        listener.error(e);
                        e.printStackTrace();
                        incrementErrorCounter();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    log("Upload document error: " + error.networkResponse);
                    error.printStackTrace();
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("chat_id", String.valueOf(chat_id));
                    params.put("parse_mode", "HTML");
                    if(text.length() > 1020)
                        params.put("caption", text.substring(0, 1020));
                    else
                        params.put("caption", text);
                    if(reply_to_message_id != 0){
                        params.put("reply_to_message_id", String.valueOf(reply_to_message_id));
                    }
                    return params;
                }

                @Override
                protected Map<String, DataPart> getByteData() {
                    Map<String, DataPart> params = new HashMap<>();
                    params.put("document", new DataPart(f));
                    return params;
                }
            };

            queue.add(multipartRequest);
        }
        catch (Exception e){
            if(listener != null)
                listener.error(e);
        }
    }







    //TELEGRAPH
    public void createTelegraphAccount(final CreateTelegraphAccountListener listener, String name){
        try {
            name = URLEncoder.encode(name, "UTF-8");
        }
        catch (Exception e){
            log("! Unsopported encoding for UEREncoder");
            e.printStackTrace();
        }
        final String url =   "https://api.telegra.ph/createAccount?short_name="+name+"&author_name="+name;
        log(". Sending request: " + url);
        incrementApiCounter();
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            log(". Got response: " + response);
                            JSONObject jsonObject = new JSONObject(response);
                            if(!jsonObject.has("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception("No OK in response!"));
                                return;
                            }
                            if(!jsonObject.getBoolean("ok")){
                                incrementErrorCounter();
                                listener.error(new Exception(jsonObject.optString("description", "No description")));
                                return;
                            }
                            JSONObject result = jsonObject.getJSONObject("result");
                            String token = result.getString("access_token");
                            listener.accountCreated(token);
                        }
                        catch (Exception e){
                            listener.error(e);
                            e.printStackTrace();
                            incrementErrorCounter();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sending request: " + url);
                error.printStackTrace();
                listener.error(error);
                incrementErrorCounter();
            }
        });
        // Add the request to the RequestQueue.
        stringRequest.setRetryPolicy(new DefaultRetryPolicy( 30000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(stringRequest);
    }
    public void createTelegraphPage(final CreateTelegraphPageListener listener, final String name, final String text){
        //Example: https://api.telegra.ph/createPage?access_token=430d287a4199d7e31a530bae39ecf1dd841a87317091370758a4aabf01f6&title=iHA%20bot%20test&content=[{%22tag%22:%22p%22,%22children%22:[%22LOLKA%22]}]
        //Example: https://api.telegra.ph/createPage?access_token=430d287a4199d7e31a530bae39ecf1dd841a87317091370758a4aabf01f6&title=iHA%20bot%20test&content=[%22wwewewewe%22]
        final String url = "https://api.telegra.ph/createPage";//?access_token="+telegraphToken;//+"&title="+name;//+"&content=[%22"+text+"%22]";
        final Map<String,String> params = new HashMap<String, String>();
        params.put("access_token", telegraphToken);
        params.put("title", name);
        if(text.length() > 31000)
            params.put("content", "[\""+text.substring(0, 31000).replace("\"", " ")+"\"]");
        else
            params.put("content", "[\""+text+"\"]");
        log(". Sending request: " + url);
        log(". # access_token=" + telegraphToken);
        log(". # title=" + name);
        log(". # content=text[" + text.length() + "]");
        log(text);
        incrementApiCounter();

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            JSONObject jsonObject = new JSONObject(response);
                            log(". Got response: " + response);
                            if(!jsonObject.has("ok")) {
                                incrementErrorCounter();
                                listener.error(new Exception("No OK in response!"));
                                return;
                            }
                            if(!jsonObject.getBoolean("ok")){
                                incrementErrorCounter();
                                listener.error(new Exception(jsonObject.optString("description", "No description")));
                                return;
                            }
                            JSONObject result = jsonObject.getJSONObject("result");
                            String url = result.getString("url");
                            listener.pageCreated(url);
                        }
                        catch (Exception e){
                            listener.error(e);
                            e.printStackTrace();
                            incrementErrorCounter();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                log(error.getClass().getName() + " while sending request: " + url);
                error.printStackTrace();
                listener.error(error);
                incrementErrorCounter();
            }
        })
        {
            @Override
            protected Map<String,String> getParams(){
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type","application/x-www-form-urlencoded");
                return params;
            }
        };


        // Add the request to the RequestQueue.
        stringRequest.setRetryPolicy(new DefaultRetryPolicy( 30000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(stringRequest);
    }

    public interface SendMessageListener{
        void sentMessage(Message message);
        void error(Throwable error);
    }
    public interface GetMeListener{
        void gotUser(User user);
        void error(Throwable error);
    }
    public interface GetUpdatesListener{
        void gotUpdates(ArrayList<Update> updates);
        void error(Throwable error);
    }
    public interface GetUserPhotoListener{
        void gotPhoto(String url);
        void noPhoto();
        void error(Throwable error);
    }
    public interface GetUserProfilePhotosListener{
        void gotPhotos(UserProfilePhotos photos);
        void error(Throwable error);
    }
    public interface GetFileListener{
        void gotFile(File file);
        void error(Throwable error);
    }
    public interface CreateTelegraphAccountListener{
        void accountCreated(String token);
        void error(Throwable error);
    }
    public interface CreateTelegraphPageListener{
        void pageCreated(String link);
        void error(Throwable error);
    }
}
