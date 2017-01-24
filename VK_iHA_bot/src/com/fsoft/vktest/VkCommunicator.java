package com.fsoft.vktest;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.perm.kate.api.*;
import com.perm.utils.Utils;
import com.perm.utils.WrongResponseCodeException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.io.FileReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * class for communication with VK
 * Created by Dr. Failov on 05.08.2014.
 */
public class VkCommunicator implements Command {
    public boolean standby = false;
    public ArrayList<Wall> walls = new ArrayList<>();
    private final Object wallSync = new Object();
    private ApplicationManager applicationManager = null;
    private boolean cont = true;
    private ArrayList<Command> commands = new ArrayList<>();
    private int scanMessages = 20;
    private long startupTime = 0;
    private int retryThreshold = 5;
    private Thread detectorTask = null;
    private AccountManager vkAccounts;
    private File file;
    private int scanInterval = 5;

    public VkCommunicator(ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
        vkAccounts = applicationManager.vkAccounts;
        file = new File(ApplicationManager.getHomeFolder() + File.separator + "communicator");
        startupTime = System.currentTimeMillis();
        beginListening();
        commands.add(new AddOwnerId());
        commands.add(new AddLikes());
        commands.add(new RemOwnerId());
        commands.add(new GetOwnerId());
        commands.add(new ClrOwnerId());
        commands.add(new SetScanInterval());
        commands.add(new SetScanMessages());
        commands.add(new Status());
        commands.add(new Standby());
        commands.add(new SendPost());
        commands.add(new Save());
        commands.add(new DeletePost());
    }
    public void load() {
        log(". Загрузка коммуникатора из файла " + file.getPath() + " ...");
        if(file.isFile()){
            try {
                java.io.FileReader fileReader = new java.io.FileReader(file);
                StringBuilder stringBuilder = new StringBuilder();
                while(fileReader.ready())
                    stringBuilder.append((char)fileReader.read());
                fileReader.close();
                String text = stringBuilder.toString();
                log(". Прочитано: " + text);
                load(text);
            }
            catch (Exception e){
                e.printStackTrace();
                log("! Ошибка чтения коммуникатора "+file.getPath()+" : " + e.toString());
            }
        }
        else
            log(". файла коммуникатора нет: " + file.getPath() + ".");
    }
    private void load(String text){
        try{
            log(". Разбор текста коммуникатора: " + text + " ...");
            JSONObject jsonObject = new JSONObject(text);
            JSONArray jsonArray = jsonObject.getJSONArray("walls");
            for (int i = 0; i < jsonArray.length(); i++) {
                String parcelable = jsonArray.getString(i);
                addOwnerID(parcelable);
            }
            log(". Коммуникатор разобран. Прочитано " + walls + " стен.");
        }
        catch (Exception e){
            e.printStackTrace();
            log(". Ошибка разбора коммуникатора: " + e.toString());
        }
    }
    public void close() {
        stopListening();
        save();
        applicationManager = null;
    }
    public Long getOwnerId() {
        if(applicationManager.vkAccounts.size() == 0)
            return 0L;
        else
            return applicationManager.vkAccounts.get(0).id;
    }
    public void setStandby(boolean standby){
        this.standby = standby;
    }
    public Long getOwnerId(String text){
        return getIdByName(text);
    }
    public String messageReceived(String message, Long senderId) {
        if(message.contains(ApplicationManager.botMark()))
            return null;
        try {
            String result = applicationManager.processMessage(message, senderId);
            if (result != null && !result.equals(""))
                log("! REPL: " + result);
            return result;
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка обработки сообщения: " + e.toString());
            return null;
        }
    }
    public String getWorkingTime(){
        String result = "";
        long workingTimeSec = (System.currentTimeMillis() - startupTime)/1000;
        long threshold = 60*60*24;
        if(workingTimeSec > threshold){
            long num = workingTimeSec/threshold;
            workingTimeSec -= threshold * num;
            result += num + " дн. ";
        }

        threshold = 60*60;
        if(workingTimeSec > threshold){
            long num = workingTimeSec/threshold;
            workingTimeSec -= threshold * num;
            result += num + " час. ";
        }

        threshold = 60;
        if(workingTimeSec > threshold){
            long num = workingTimeSec/threshold;
            workingTimeSec -= threshold * num;
            result += num + " мин. ";
        }

        result += workingTimeSec + " сек. ";
//        String timeUnits = " сек.";
//        if(workingTime > 60){
//            workingTime /= 60;
//            timeUnits = " мин.";
//            if(workingTime > 60){
//                workingTime /= 60;
//                timeUnits = " час.";
//                if(workingTime > 24){
//                    workingTime /= 24;
//                    timeUnits = " дн.";
//                }
//            }
//        }
        return result;
    }
    public @Override String getHelp() {
        String result = "";
        for (int i = 0; i < commands.size(); i++)
            result += commands.get(i).getHelp();
        for (int i = 0; i < walls.size(); i++)
            result += walls.get(i).getCommandHelp();
        return result;
    }
    public @Override String process(String text) {
        String result = "";
        for (int i = 0; i < commands.size(); i++)
            result += commands.get(i).process(text);
        for (int i = 0; i < walls.size(); i++)
            result += walls.get(i).processCommand(text);
        return result;
    }
    //others
    public String addOwnerID(long id){
        synchronized (wallSync) {
            if (getWallExists(id) == null && id != 0) {
                walls.add(new Wall(id));
                return log(". Cтена внесена " + id + " в список обрабатываемых. ");
            } else
                return log("! Cтена не внесена " + id + " в список обрабатываемых, т.к. уже там присутствует.");
        }
    }
    public String addOwnerID(String parcelable){
        synchronized (wallSync) {
            long ID = getIdFromParcelable(parcelable);
            if (getWallExists(ID) == null && ID != 0) {
                Wall wall = new Wall(parcelable);
                walls.add(wall);
                return log(". Cтена внесена " + wall.id + " в список обрабатываемых. ");
            } else
                return log("! Cтена не внесена " + ID + " в список обрабатываемых, т.к. уже там присутствует.");
        }
    }
    private long getIdFromParcelable(String parcelable){
        try{
            log(". Загрузка ID из строки: "+parcelable+"...");
            JSONObject jsonObject = new JSONObject(parcelable);
            return jsonObject.getLong("id");
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка загрузки ID: " + e.toString());
            return 0;
        }
    }
    private Wall getWallExists(long id){
        for (int i=0; i<walls.size(); i++)
            if(walls.get(i).id == id)
                return walls.get(i);
        return null;
    }
    public String save(){
        String result = "";
        result += log(". Запись коммуникатора в файл " + file.getPath() + " ...");
        try {
            File parentFolder = file.getParentFile();
            if(!parentFolder.exists())
                parentFolder.mkdirs();
            FileWriter fileWriter = new FileWriter(file);
            String text = getParcelable();
            result += log(". Запись: " + text);
            fileWriter.write(text);
            fileWriter.close();
            result += log(". Записано.");
        }
        catch (Exception e){
            e.printStackTrace();
            result += log("! Ошибка записи коммуникатора "+file.getPath()+" : " + e.toString());
        }
        return result;
    }
    private String getParcelable(){
        log(". Серилизация коммуникатора ...");
        try {
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < walls.size(); i++) {
                jsonArray.put(walls.get(i).toString());
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("walls", jsonArray);
            String result = jsonObject.toString();
            log(". Серилизация коммуникатора успешна: " + result);
            return result;
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка серилизации коммуникатора: " + e.toString());
            return "";
        }
    }
    public void beginListening() {
        if(detectorTask == null) {
            log("- Запуск мониторинга...");
            detectorTask = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        log("- Жду новых сообщений...");
                        while (cont && applicationManager.running) {
                            for (int i = 0; i < walls.size(); i++) {
                                walls.get(i).findNewPosts();
                            }
                            sleep(scanInterval * 1000);
                            if(standby)
                                sleep(scanInterval * 1000 * 10);
                        }
                    }catch (Exception e){
                        log("! Ошибка: "  + e.toString());
                        e.printStackTrace();
                    }
                    return;
                }
            });
            detectorTask.start();
        }
    }
    public void stopListening(){
        log(". Остановка мониторинга...");
        cont = false;
        if(detectorTask != null) {
            detectorTask.interrupt();
            detectorTask = null;
        }
    }
    private String log(String text){
        if(applicationManager != null)
            ApplicationManager.log(text);
        return text;
    }
    private void sleep(int mili){
        try{
            Thread.sleep(mili);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public void reportCommunicatorError(Exception e){
        if (e.toString().contains("Unable to resolve host")) {
            log("Деактивация чтения стен  на 10 минут...");
            stopListening();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    beginListening();
                }
            }, 600000);
            return;
        }
    }
    public boolean isDocumentLink(String link){
        //https://vk.com/doc10299185_358837359
        return link.matches("https?:\\/\\/vk\\.com\\/doc[0-9]+_[0-9]+");//   \?[^ ]+

    }

    /////////API functions
    public long createWallComment(long ownerID, long message_id, String text, Long replyTo){
        long result = 0;
        String[] parts = applicationManager.splitText(text, 5000);
        for (int i = 0; i < parts.length; i++) {
            result = 0;
            for (int attempt = 0; attempt < retryThreshold; attempt++) {
                VkAccount account = vkAccounts.getActive();
                try{
                    result = account.api().createWallComment(ownerID, message_id, ApplicationManager.botMark() + (i != 0 ? " (часть " + i + ") " : " ") + parts[i], replyTo, null, false, "", "");
                    account.markSend();
                    break;
                }
                catch (Exception e){
                    e.printStackTrace();
                    log("! Ошибка отправки комментария: " + e.toString());
                    account.reportError(e);
                    sleep(1000);
                }
            }
        }
        return result;
    }
    public long getIdByName(String name){
        for (int attempt = 0; attempt < retryThreshold; attempt++) {
            VkAccount account = vkAccounts.getActive();
            try{
                long result = account.api().resolveScreenName(name);
                account.markSend();
                return result;
            }
            catch (Exception e){
                e.printStackTrace();
                account.reportError(e);
                log("! Error resolving name: " + e.toString());
                sleep(1000);
            }
        }
        return -1;
    }
    public ArrayList<WallMessage> getWallMessages(Long owner_id, int count, int offset, String filter) throws KException{
        for (int attempt = 0; attempt < retryThreshold; attempt++) {
            VkAccount account = vkAccounts.getActive();
            try{
                ArrayList<WallMessage> messages = account.api().getWallMessages(owner_id, count, offset, filter);
                account.markRead();
                return messages;
            }
            catch (Exception e){
                if(e.toString().contains("FileNotFound"))
                    continue;
                e.printStackTrace();
                log("! Error reading wall "+owner_id+": " + e.toString());
                account.reportError(e);
                Wall wall = getWallExists(owner_id);
                if(wall != null)
                    wall.reportError(e);
                sleep(1000);
            }
        }
        return new ArrayList<WallMessage>();
    }
    public ArrayList<Comment> getWallComments(Long owner_id, Long post_id, int offset, int count, boolean reverse_order){
        for (int attempt = 0; attempt < retryThreshold; attempt++) {
            VkAccount account = vkAccounts.getActive();
            try{
                ArrayList<Comment> messages = account.api().getWallComments(owner_id, post_id, offset, count, reverse_order).comments;
                account.markRead();
                return messages;
            }
            catch (Exception e){
                e.printStackTrace();
                log("! Error reading comments: " + e.toString());
                account.reportError(e);
                Wall wall = getWallExists(owner_id);
                if(wall != null)
                    wall.reportError(e);
                sleep(1000);
            }
        }
        return new ArrayList<Comment>();
    }
    public String createWallMessage(long ownerID, String text){
        for (int attempt = 0; attempt < retryThreshold; attempt++) {
            VkAccount account = vkAccounts.getActive();
            try{
                account.api().createWallPost(ownerID, text, null, null, false, false, false, null, null, null, 0L, null, null);
                account.markSend();
                return "Cooбщение " + text + " успешно отправлено на стену " + ownerID + ".";
            }
            catch (Exception e){
                e.printStackTrace();
                log("! Error createWallMessage: " + e.toString());
                account.reportError(e);
                Wall wall = getWallExists(ownerID);
                if(wall != null)
                    wall.reportError(e);
                sleep(1000);
            }
        }
        return "Ошибка отправки сообщения";
    }
    public String deletePost(long postId, long ownerId){
        String result = "";
        for (int i = 0; i < vkAccounts.size(); i++) {
            VkAccount account = vkAccounts.get(i);
            result += log(". Попытка удалить запись аккаунтом "+account.userName+" ...\n");
            try{
                if(account.isActive()) {
                    account.api().removeWallPost(postId, ownerId);
                    account.markSend();
                    result += log(". Запись " + postId + " удалена со стены " + ownerId + " успешно.\n");
                    return result;
                }
                else {
                    result += log(". Аккаунт неактивен.\n");
                }
            }
            catch (Exception e){
                result += log(". Не получилось ("+account.userName+") : " + e.toString() + "\n");
                e.printStackTrace();
            }
        }
        result += log(". Ничего не получилось.\n");
        return result;
    }
    public String deleteComment(long commentId, long ownerId){
        String result = "";
        for (int i = 0; i < vkAccounts.size(); i++) {
            VkAccount account = vkAccounts.get(i);
            result += log(". Попытка удалить запись аккаунтом "+account.userName+" ...\n");
            try{
                if(account.isActive()) {
                    account.api().deleteWallComment(ownerId, commentId);
                    account.markSend();
                    result += log(". Запись " + commentId + " удалена со стены " + ownerId + " успешно.\n");
                    return result;
                }
                else {
                    result += log(". Аккаунт неактивен.\n");
                }
            }
            catch (Exception e){
                result += log(". Не получилось ("+account.userName+") : " + e.toString() + "\n");
                e.printStackTrace();
            }
        }
        result += log(". Ничего не получилось.\n");
        return result;
    }
    private HashMap<Long, String> userNamesCache = new HashMap<>();
    public String getUserName(long id){
        if(id < 0)
            return getGroupName(id);
        if(userNamesCache.containsKey(id))
            return userNamesCache.get(id);
        for (int attempt = 0; attempt < retryThreshold; attempt++) {
            VkAccount account = vkAccounts.getActive();
            try{
                ArrayList<Long> uid = new ArrayList<>();
                uid.add(id);
                ArrayList<User> users = account.api().getProfiles(uid, null, null, "Nom", null, null);
                account.markRead();
                if(users.size() > 0){
                    User me = users.get(0);
                    String result = me.first_name + " " + me.last_name;
                    log(". Имя пользователя загружено: " + result);
                    userNamesCache.put(id, result);
                    return( result);
                }
            }
            catch (Exception e){
                if(e.toString().contains("NetworkOnMainThreadException"))
                    return "User";
                e.printStackTrace();
                account.reportError(e);
                Wall wall = getWallExists(id);
                if(wall != null)
                    wall.reportError(e);
                log("! Ошибка загрузки имени пользователя: " + e.toString());
            }
        }
        return "";
    }
    public String getGroupName(long id){
        if(id >= 0)
            getUserName(id);
        if(userNamesCache.containsKey(id))
            return userNamesCache.get(id);
        for (int attempt = 0; attempt < retryThreshold; attempt++) {
            VkAccount account = vkAccounts.getActive();
            try{
                ArrayList<Long> uid = new ArrayList<>();
                uid.add(Math.abs(id));
                ArrayList<Group> users = account.api().getGroups(uid, null, null);
                account.markRead();
                if(users.size() > 0){
                    Group me = users.get(0);
                    String result = me.name;
                    log(". Имя сообщества загружено: " + result);
                    userNamesCache.put(id, result);
                    return( result);
                }
            }
            catch (Exception e){
                e.printStackTrace();
                account.reportError(e);
                Wall wall = getWallExists(id);
                if(wall != null)
                    wall.reportError(e);
                log("! Ошибка загрузки имени сообщества: " + e.toString());
            }
        }
        return "";
    }
    public String sendMessage(long id, String text){
        String result = "";
        for (int attempt = 0; attempt < retryThreshold; attempt++) {
            VkAccount account = vkAccounts.getActive();
            result += log(". Получен аккаунт: " + account.userName + " \n");
            try{
                result += log(". Отправка сообщения ("+text+", "+id+") ... \n");
                account.api().sendMessage(id, 0, text, null, null, null, null, null, null, null, null);
                result += log(". Сообщение отправлено! \n");
                return result;
            }
            catch (Exception e){
                e.printStackTrace();
                account.reportError(e);
                result += log("! Ошибка отправки сообщения: " + e.toString() + "\n");
            }
        }
        return result;
    }
    public void addLike(Long owner_id, Long item_id, String type){
        for (int attempt = 0; attempt < retryThreshold; attempt++) {
            VkAccount account = vkAccounts.getActive();
            try{
                account.api().addLike(owner_id, item_id, type, null, null, null);
                account.markSend();
            }
            catch (Exception e){
                e.printStackTrace();
                account.reportError(e);
                log("! Error adding like: " + e.toString());
            }
        }
    }
    public String addLikeByAllAccounts(Long owner_id, Long item_id, String type){
        String result = "";
        String postAddress = "vk.com/wall"+owner_id+"_"+item_id;
        for (int acc = 0; acc < vkAccounts.size(); acc++) {
            VkAccount account = vkAccounts.get(acc);
            if(account.isActive()) {
                for (int attempt = 0; attempt < retryThreshold; attempt++) {
                    try {
                        account.api().addLike(owner_id, item_id, type, null, null, null);
                        account.markSend();
                        result += log(". Добавлен лайк к " + postAddress + " пользователем " + account.userName + " \n");
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                        account.reportError(e);
                        result += log("! Error adding like to " + postAddress + " by " + account.userName + " : " + e.toString() + "\n");
                    }
                }
            }
            sleep(1000);
        }
        return result;
    }
    public String uploadDocument(File file){
        for (int i = 0; i < retryThreshold; i++) {
            try {
                log("Reading file...");
                log("file to upload =" + file.getPath());
                FileInputStream fileInputStream = new FileInputStream(file);
                int total = (int)file.length();
                log("file size =" + total);
                byte[] fileArray = new byte[total];
                RandomAccessFile f = new RandomAccessFile(file, "r");
                f.read(fileArray);

                log("Getting server...");
                VkAccount account = vkAccounts.getActive();
                Api api = account.api();
                String server = api.docsGetUploadServer();
                log("server =" + server);

                log("Connecting...");
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost postRequest = new HttpPost(server);
                ByteArrayBody bab = new ByteArrayBody(fileArray, file.getName());
                MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                reqEntity.addPart("file", bab);
                postRequest.setEntity(reqEntity);
                httpClient.getParams().setParameter("http.protocol.content-charset", "UTF-8");

                log("Sending...");
                HttpResponse httpResponse = httpClient.execute(postRequest);

                log("Reading...");
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent(), "UTF-8"));
                String sResponse;
                StringBuilder s = new StringBuilder();
                while ((sResponse = reader.readLine()) != null)
                    s = s.append(sResponse);
                String response = s.toString();
                log("received =" + total);

                log("Parsing...");
                JSONObject jsonObject = new JSONObject(response);
                if(!jsonObject.has("file"))
                    throw new Exception(response);
                String fileField = jsonObject.getString("file");
                log("file =" + fileField);

                log("Saving...");
                String fileURL = api.saveDoc(fileField).url;
                log("fileURL=" + fileURL);
                return fileURL;
            }
            catch (Exception e){
                e.printStackTrace();
                log("Ошибка загрузки документа: " + e.toString());
            }
        }
        return null;
    }
    public File downloadDocument(String link){
        try{
            log("Connecting...");
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(link);
            HttpResponse httpResponse = httpClient.execute(httpGet);

            log("CreatingFile...");
            File fileToSave = new File(ApplicationManager.getHomeFolder() + File.separator + "download_" + new SimpleDateFormat("yyyy_MM_dd_HH-mm-ss").format(new Date()));
            FileOutputStream fileOutputStream = new FileOutputStream(fileToSave);

            log("Reading...");
            byte[] content = EntityUtils.toByteArray(httpResponse.getEntity());
            log("reseived " + content.length + " bytes");
            fileOutputStream.write(content);
            fileOutputStream.close();
            log("writed " + fileToSave.length() + " bytes");
            log("saved to " + fileToSave.getPath() + ".");
            return fileToSave;
        }
        catch (Exception e){
            e.printStackTrace();
            log("Ошибка загрузки документа: " + e.toString());
        }
        return null;
    }

    public class Wall{
        public long id;
        private boolean active = true;
        private String name = null;
        private boolean initialized = false;
        private int messagesDetected = 0;
        private int commentsDetected = 0;
        private int messagesReplied  = 0;
        private long lastError = 0;
        private int errorRate = 0;
        private long messageMaxId = 0; //хранит последний прочитанный ID записи на этой стене. ID всегда растут.
        private ArrayList<Command> commands = new ArrayList<>();
        /*
        * ----------------- Как читать стену
        * - Получить список всех сообщений
        * - Всем в массиве обработанных записей сделать счетчик пропусков +1
        * - Каждое запись попробовать найти в массиве обработанных.
        * - Те что найдены: счетчик пропусков = 0
        *   - сравнить количество комментариев.
        *   - если оно изменилось
        *       - сделать всем в массиве обработанных комментариев этой записи +1
         *      - загрузить список комментариев
         *      - те что найдены комментарии - счетчик пропусков = 0
        * - Тех что нет - то новые сообщения
        * - Внести их в список обработанных
        * - Очистить список обработанных, если пропусков больше 10ти
        * */

        public Wall(long id) {
            this.id = id;
            init();
        }
        public Wall(String parcelable){
            try{
                log(". Загрузка стены из строки: "+parcelable+"...");
                JSONObject jsonObject = new JSONObject(parcelable);
                id = jsonObject.getLong("id");
                init();
            }
            catch (Exception e){
                e.printStackTrace();
                active = false;
                log("! Ошибка загрузки стены: " + e.toString());
            }
        }
        public void findNewPosts(){
            if(isActive()) {
                try {
                    incrementPostSkipCounters();
                    ArrayList<WallMessage> messages = getWallMessages(id, scanMessages, 0, "");
                    long lastPostId = messageMaxId;
                    for (int message = 0; message < messages.size(); message++) {
                        WallMessage post = messages.get(message);
                        lastPostId = Math.max(lastPostId, post.id);
                        if (isNew(post)) {//если это сообщение ранее не было обработано
                            log(". POST ("+getName()+"): " + post.text);
                            processNewPost(id, messages.get(message));
                        } else {
                            PostProcessed postProcessed = getEquals(postsProcessed, new PostProcessed(post.id, post.text, 0));
                            if (postProcessed.commentCount != post.comment_count) {//количество комментариев ищменилось
                                postProcessed.commentCount = post.comment_count;
                                incrementCommentSkipCounters(post.id);
                                ArrayList<Comment> comments = getWallComments(id, post.id, (int) messages.get(message).comment_count - scanMessages, scanMessages, false);
                                for (int comment = 0; comment < comments.size(); comment++) {
                                    Comment comm = comments.get(comment);
                                    lastPostId = Math.max(lastPostId, comm.cid);
                                    if (!comm.message.contains(ApplicationManager.botMark()) && isNew(post, comm)) {
                                        log(". COMM ("+getName()+"): " + comm.message);
                                        processNewComment(id, comm, post);
                                    }
                                }
                            }
                        }
                    }
                    messageMaxId = lastPostId;
                    errorRate = 0;
                } catch (Exception e) {
                    e.printStackTrace();
                    reportError(e);
                    reportCommunicatorError(e);
                    log("! Ошибка: " + e.toString());
                }
            }
        }
        public @Override String toString(){
            try {
                log(". Сериализация данных стены "+getName()+"...");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", id);
                String result = jsonObject.toString();
                log(". Сериализация успешна: " + result);
                return result;
            }
            catch (Exception e){
                e.printStackTrace();
                log("! Ошибка сериализации данных: " + e.toString());
                return null;
            }
        }
        public @Override boolean equals(Object object){
            if(object == null)
                return false;
            if(getClass() == object.getClass()){
                return id == ((Wall)object).id;
            }
            return object.equals(id);
        }
        public String getName(){
            if(name == null || name.equals(""))
                return String.valueOf(id);
            else
                return name;
        }
        public boolean isActive(){
            return active && initialized;
        }
        public void reportError(Exception e){
            if (e.toString().contains("User was deleted or banned")) {
                deactivateFor10Minutes();//active = false;
                log(". Стена пользователя "+getName()+" замороена. Стена дективирована.");
                return;
            }
//            if (e.toString().contains("Access to post comments denied")) { //Могут забанить не всех ботов, а лишь одного. Если забанят всех - сработает другая защита.
//                active = false;
//                log(". Стена пользователя "+getName()+" закрыта. Стена дективирована.");
//                return;
//            }
//            if (e.toString().contains("Unable to resolve host")) {//могут быть временные сбои с инетом.
//                log(". Стена пользователя "+getName()+" недоступна из-за проблем с Интернет-соединением. Деактивация стены на 10 минут.");
//                deactivateFor10Minutes();
//            }

            long now = System.currentTimeMillis();
            long sinceLastError = now-lastError;
            if(sinceLastError < 60000){
                errorRate ++;
            }
            else {
                errorRate = 0;
            }
            if(errorRate > 20)
                deactivateFor10Minutes();
            lastError = System.currentTimeMillis();
        }
        public String processCommand(String command){
            String result = "";
            for (int i = 0; i < commands.size(); i++) {
                result += commands.get(i).process(command);
            }
            return result;
        }
        public String getCommandHelp(){
            String result = "";
            for (int i = 0; i < commands.size(); i++) {
                result += log(commands.get(i).getHelp());
            }
            return result;
        }
        public View getView(Context context){
            return new WallView(context);
        }

        private void init(){
            commands.add(new SetActive());
            commands.add(new Status());
            getWallName();
            initProcessed();
        }
        private void initProcessed(){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int trying = 0;
                    while(cont) {
                        try {
                            ArrayList<WallMessage> messages = getWallMessages(id, scanMessages, 0, "");
                            for (int message = 0; message < messages.size() && cont && active; message++) {
                                log(". Чтение "+getName()+", попытка " + trying + " ... " + Math.round(((double)message/(double)messages.size()) * 100) + "%");
                                addProcessed(messages.get(message));
                                ArrayList<Comment> comments = getWallComments(id, messages.get(message).id, (int) messages.get(message).comment_count - scanMessages, scanMessages, false);
                                for (int comment = 0; comment < comments.size() && cont; comment++)
                                    addProcessed(messages.get(message), comments.get(comment));
                            }
                            initialized = true;
                            log(". Стена " + getName() + " инициализирована.");
                            break;
                        } catch (Exception e) {
                            log("! Ошибка: " + e.toString());
                            e.printStackTrace();
                            trying ++;
                            if(trying > 10) {
                                log("! Инициализация стены " + getName() + " отменена из-за большого количества ошибок.");
                                break;
                            }
                        }
                    }
                }
            }).start();
        }
        void deactivateFor10Minutes(){
            log("Деактивация стены "+name+" на 10 минут...");
            active = (false);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    active = (true);
                }
            }, 600000);
        }
        private void getWallName(){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(id < 0)
                        name = getGroupName(id);
                    else
                        name = getUserName(id);
                }
            }).start();
        }
        private void processNewPost(Long ownerID, WallMessage message){
            messagesDetected ++;
            String postText = message.text.replaceAll("\\s+", " ");
            for (int i = 0; i < message.attachments.size(); i++) {
                Attachment attachment = message.attachments.get(i);
                if(attachment.type.equals("doc"))
                    postText += " " + attachment.document.url;
            }
            String reply = messageReceived(postText, message.from_id);
            if (reply != null  && !reply.equals("")) {
                messagesReplied++;
                createWallComment(ownerID, message.id, reply, null);
//                addLike(ownerID, message.id, "post");
            }
        }
        private void processNewComment(Long ownerID, Comment comment, WallMessage post){
            commentsDetected ++;
            String postText = comment.message.replaceAll("\\s+", " ");
            for (int i = 0; i < comment.attachments.size(); i++) {
                Attachment attachment = comment.attachments.get(i);
                if(attachment.type.equals("doc"))
                    postText += " " + attachment.document.url;
            }
            String reply = messageReceived(postText, comment.from_id);
            if (reply != null && !reply.equals("")) {
                messagesReplied++;
                createWallComment(ownerID, post.id, reply, comment.cid);
//                addLike(ownerID, comment.cid, "comment");
            }
        }

        //Processed counters
        private ArrayList<PostProcessed> postsProcessed = new ArrayList<>();
        private ArrayList<CommentProcessed> commentsProcessed = new ArrayList<>();
        private void incrementPostSkipCounters(){
            for (int i = 0; i < postsProcessed.size(); i++)
                postsProcessed.get(i).skipIterations ++;
        }
        private void incrementCommentSkipCounters(long post_id){
            for (int i = 0; i < commentsProcessed.size(); i++)
                if(commentsProcessed.get(i).postID == post_id)
                    commentsProcessed.get(i).skipIterations ++;
        }
        private void addProcessed(WallMessage wallMessage){
            PostProcessed postProcessed = new PostProcessed(wallMessage.id, wallMessage.text, wallMessage.comment_count);
            if(!postsProcessed.contains(postProcessed))
                postsProcessed.add(postProcessed);
            cleanProcessed();
        }
        private void addProcessed(WallMessage wallMessage, Comment comment){
            CommentProcessed commentProcessed = new CommentProcessed(wallMessage.id, comment.cid, comment.message);
            if(!commentsProcessed.contains(commentProcessed))
                commentsProcessed.add(commentProcessed);
            cleanProcessed();
        }
        private void cleanProcessed(){
            for (int i = 0; i < postsProcessed.size(); i++) {
                if(postsProcessed.get(i).skipIterations > 50) {
                    postsProcessed.remove(i);
                    i--;
                }
            }
            for (int i = 0; i < commentsProcessed.size(); i++) {
                if(commentsProcessed.get(i).skipIterations > 50) {
                    commentsProcessed.remove(i);
                    i--;
                }
            }
        }
        private boolean isNew(WallMessage wallMessage){
            PostProcessed postProcessed = new PostProcessed(wallMessage.id, wallMessage.text, 0);
            boolean contains = postsProcessed.contains(postProcessed);
            if(contains)
                getEquals(postsProcessed, postProcessed).skipIterations=0;
            else
                addProcessed(wallMessage);
            if(wallMessage.id < messageMaxId)
                return false;
            return !contains;
        }
        private boolean isNew(WallMessage wallMessage, Comment comment){
            CommentProcessed commentProcessed = new CommentProcessed(wallMessage.id, comment.cid, comment.message);
            boolean contains = commentsProcessed.contains(commentProcessed);
            if(contains)
                getEquals(commentsProcessed, commentProcessed).skipIterations=0;
            else
                addProcessed(wallMessage, comment);
            if(comment.cid < messageMaxId)
                return false;
            return !contains;
        }
        private PostProcessed getEquals(ArrayList<PostProcessed> array, PostProcessed object){
            for (int i = 0; i < array.size(); i++) {
                PostProcessed fromArray = array.get(i);
                if(fromArray.equals(object))
                    return fromArray;
            }
            return new PostProcessed(0,"", 0);
        }
        private CommentProcessed getEquals(ArrayList<CommentProcessed> array, CommentProcessed object){
            for (int i = 0; i < array.size(); i++) {
                CommentProcessed fromArray = array.get(i);
                if(fromArray.equals(object))
                    return fromArray;
            }
            return new CommentProcessed(0,0,"");
        }
        private class PostProcessed{
            long postID;
            String postText;
            long commentCount = 0;
            //важно! Это - количество итераций ПОДРЯД, на которых это сообщение НЕ было получено. Используется для того, чтобы удалить его из базы когда оно не будет нужно.
            int skipIterations = 0;

            PostProcessed(long postID, String postText, long commentCount) {
                this.postID = postID;
                this.postText = postText;
                this.commentCount = commentCount;
            }

            @Override
            public boolean equals(Object o) {
                if(o.getClass() != getClass())
                    return false;
                PostProcessed object = (PostProcessed) o;
                return object.postText.equals(postText) && object.postID == postID;
            }
        }
        private class CommentProcessed{
            long postID;
            long commentID;
            String postText;
            //важно! Это - количество итераций ПОДРЯД, на которых это сообщение НЕ было получено. Используется для того, чтобы удалить его из базы когда оно не будет нужно.
            int skipIterations = 0;

            CommentProcessed(long postID, long commentID, String postText) {
                this.postID = postID;
                this.commentID = commentID;
                this.postText = postText;
            }

            @Override
            public boolean equals(Object o) {
                if(o.getClass() != getClass())
                    return false;
                CommentProcessed object = (CommentProcessed) o;
                return object.postText.equals(postText) && object.postID == postID && object.commentID == commentID;
            }
        }

        class WallView extends LinearLayout {
            Handler handler;
            TextView textViewName;
            TextView textViewActive;
            TextView textViewPostTotal;
            TextView textViewCommentTotal;
            TextView textViewRepliedTotal;
            Context context = null;
            AlertDialog alertDialog = null;

            WallView(Context context) {
                super(context);
                this.context = context;
                handler = new Handler();
                setOrientation(LinearLayout.VERTICAL);
                setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showDialog();
                    }
                });
                addView(getDelimiter(context));

                int color = active ? (initialized ? Color.GREEN : Color.RED) : Color.GRAY;

                {
                    TextView textView = textViewName = new TextView(context);
                    textView.setPadding(20, 0, 0, 0);
                    textView.setTextColor(color);
                    textView.setText(getName());
                    textView.setTextSize(18);
                    addView(textView);
                }
                {
                    TextView textView = textViewActive = new TextView(context);
                    textView.setTextColor(color);
                    textView.setText("активен = " + isActive());
                    textView.setTextSize(10);
                    addView(textView);
                }
                {
                    TextView textView = textViewPostTotal = new TextView(context);
                    textView.setTextColor(color);
                    textView.setText("новых записей = " + messagesDetected);
                    textView.setTextSize(10);
                    addView(textView);
                }
                {
                    TextView textView = textViewCommentTotal = new TextView(context);
                    textView.setTextColor(color);
                    textView.setText("новых комментариев = " + commentsDetected);
                    textView.setTextSize(10);
                    addView(textView);
                }
                {
                    TextView textView = textViewRepliedTotal = new TextView(context);
                    textView.setTextColor(color);
                    textView.setText("опубликовано ответов = " + messagesReplied);
                    textView.setTextSize(10);
                    addView(textView);
                }
                addView(getDelimiter(context));
            }
            private View getDelimiter(Context context){
                View delimiter = new View(context);
                delimiter.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 1));
                delimiter.setBackgroundColor(Color.DKGRAY);
                return delimiter;
            }
            public void showDialog(){
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                LinearLayout linearLayout = new LinearLayout(context);
                linearLayout.setOrientation(VERTICAL);
                ScrollView scrollView = new ScrollView(context);
                scrollView.addView(linearLayout);
                builder.setView(scrollView);
                {
                    TextView textView = new TextView(context);
                    textView.setText("Стена " + getName());
                    textView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    textView.setGravity(Gravity.CENTER);
                    textView.setTextSize(20);
                    textView.setTextColor(Color.WHITE);
                    linearLayout.addView(textView);
                    linearLayout.addView(getDelimiter(context));
                }

                linearLayout.addView(getOnOffRow("Активный ("+active+")",new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        active = (true);
                        closeDialog();
                    }
                }, new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        active = (false);
                        closeDialog();
                    }
                }));

                {
                    Button button = new Button(context);
                    button.setText("Удалить");
                    button.setTextColor(Color.RED);
                    button.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            messageBox(applicationManager.processCommand("wall rem " + id));
                            closeDialog();
                        }
                    });
                    linearLayout.addView(button);
                }
                alertDialog = builder.show();
            }
            public void closeDialog(){
                if(alertDialog != null)
                    alertDialog.dismiss();
            }
            private LinearLayout getOnOffRow(String text, OnClickListener onClickListener, OnClickListener offClickListener){
                TextView textView = new TextView(context);
                textView.setText(text);
                textView.setPadding(10, 0, 0, 0);
                textView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                Button buttonOn = new Button(context);
                buttonOn.setText("вкл");
                buttonOn.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
                buttonOn.setTextColor(Color.GREEN);
                buttonOn.setOnClickListener(onClickListener);

                Button buttonOff = new Button(context);
                buttonOff.setText("выкл");
                buttonOff.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
                buttonOff.setTextColor(Color.YELLOW);
                buttonOff.setOnClickListener(offClickListener);

                LinearLayout horizontalLayout = new LinearLayout(context);
                horizontalLayout.setOrientation(HORIZONTAL);
                horizontalLayout.addView(textView);
                horizontalLayout.addView(buttonOn);
                horizontalLayout.addView(buttonOff);
                return horizontalLayout;
            }

            void messageBox(final String text){
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setPositiveButton("OK", null);
                        builder.setMessage(text);
                        builder.setTitle("Результат");
                        builder.show();
                    }
                });
            }
        }
        class Status implements Command{
            @Override public
            String process(String input) {
                CommandParser commandParser = new CommandParser(input);
                if(commandParser.getWord().equals("status"))
                    return "ID стены "+getName()+": "+id+"\n" +
                            "Стена "+getName()+" активна: "+active+"\n" +
                            "Стена "+getName()+" инициализирована: "+initialized+"\n" +
                            "Обнаружено записей на стене "+getName()+": "+messagesDetected+"\n" +
                            "Обнаружено комментариев на стене "+getName()+": "+commentsDetected+"\n" +
                            "Опубликовано ответов на стене "+getName()+": "+messagesReplied+"\n";
                return "";
            }

            @Override
            public String getHelp() {
                return "";
            }
        }
        class SetActive implements Command{
            @Override public
            String process(String input) {
                CommandParser commandParser = new CommandParser(input);
                if(commandParser.getWord().equals("wall"))
                    if(commandParser.getLong() == id)
                        if(commandParser.getWord().equals("setactive"))
                            return "active = " + (active = commandParser.getBoolean());
                return "";
            }

            @Override public
            String getHelp() {
                return "[ Активировать/деактивировать стену "+getName()+" ]\n" +
                        "---| botcmd wall "+id+" setactive on\n\n";
            }
        }
    }
    private class Status implements Command{
        @Override public
        String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("status"))
                return "Состояние программы: " + (standby?"ожидание":"работает") + "\n" +
                        "Количество анализируемых стен: "+walls.size()+"\n" +
                        "Интервал сканирования: "+scanInterval+" сек. \n" +
                        "Время работы: "+getWorkingTime()+" \n";
            return "";
        }

        @Override
        public String getHelp() {
            return "";
        }
    }
    private class Save implements Command{
        @Override public
        String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("save"))
                return save();
            return "";
        }

        @Override
        public String getHelp() {
            return "";
        }
    }
    private class Standby implements Command{
        @Override public
        String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("standby")) {
                setStandby(commandParser.getBoolean());
                return "standby = " + standby;
            }
            return "";
        }

        @Override public
        String getHelp() {
            return "[ Включить\\выключить режим ожидания ]\n" +
                    "---| botcmd standby on\n\n";
        }
    }
    private class SendPost implements Command{
        @Override public
        String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("sendpost")) {
                String result = log(". Отправка сообщения на стену...\n");
                try {
                    String ownerIDstring = commandParser.getWord();
                    result += log( ". Получен адрес стены: "+ownerIDstring+".\n");
                    Long ownerID = applicationManager.getUserID(ownerIDstring);
                    result += log( ". Получен ID стены: "+ownerID+".\n");
                    if(ownerID.equals(-1L))
                        result += log( ". Ошибка представления введеного идентификатора пользователя.\n");
                    else {
                        String lastText = commandParser.getText();
                        result += log(". Получен текст сообщения: " + lastText + ".\n");
                        if (lastText.equals(""))
                            result += log(". Ошибка отправки сообщения: не получен текст.\n");
                        else {
                            if(applicationManager.messageProcessor.filter.isAllowed(lastText))
                                result += log(createWallMessage(ownerID, lastText) + "\n");
                            else
                                result += log(". Ваше сообщение не может быть отправлено.");
                        }
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                    result += log(". Ошибка отправки сообщения: " + e.toString() + "\n");
                }
                return result;
            }
            return "";
        }

        @Override public
        String getHelp() {
            return "[ Отправить сообщение на стену ]\n" +
                    "---| botcmd sendpost vk.com/drfailov Привет!\n\n";
        }
    }
    private class SetScanMessages implements Command{
        @Override public
        String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("setscanmessages")) {
                int newValue = commandParser.getInt();
                if(newValue > 0 && newValue < 200) {
                    scanMessages = newValue;
                    return "Теперь система сканирует "+scanMessages+" записей.";
                }
            }
            return "";
        }

        @Override public
        String getHelp() {
            return "[ Задать количество сканируемых на стене записей ]\n" +
                    "---| botcmd setscanmessages 40\n\n";
        }
    }
    private class SetScanInterval implements Command{
        @Override public
        String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("setscaninterval")) {
                int newValue = commandParser.getInt();
                if(newValue > 0) {
                    scanInterval = newValue;
                    return "Теперь система сканирует записи каждые "+scanInterval+" секунд.";
                }
            }
            return "";
        }

        @Override public
        String getHelp() {
            return "[ Задать интервал сканирования стен ]\n" +
                    "---| botcmd setscaninterval 10\n\n";
        }
    }
    private class DeletePost implements Command{
        @Override public
        String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("deletepost")) {
                String result = "";
                String link = commandParser.getWord();
                result += log(". Получение ссылки: " + link + "\n");       //https://vk.com/wall10299185_13439?reply=19248
                link = link.replace("http://vk.com/wall", "");      //10299185_13439?reply=19248
                link = link.replace("https://vk.com/wall", "");     //10299185_13439?reply=19248
                link = link.replace("?reply=", "_");                //10299185_13439_19248
                result += log(". Преобразование ссылки: " + link + "\n");
                try {
                    String[] parts = link.split("\\_");             //[10299185, 13439, 19248]
                    if (parts.length < 2)
                        result += log(". Ошибка разбора ссылки: обнаружено всего " + parts.length + " номеров, а надо минимум 2." + "\n");
                    if (parts.length == 2) {
                        long ownerId = Long.parseLong(parts[0]);
                        result += log(". Разбор ссылки: ownerId = " + ownerId + "\n");
                        long postId = Long.parseLong(parts[1]);
                        result += log(". Разбор ссылки: postId = " + postId + "\n");
                        String type = "post";
                        result += log(". Разбор ссылки: type = " + type + "\n");
                        result += log(deletePost(postId, ownerId));
                    }
                    if (parts.length == 3) {
                        long ownerId = Long.parseLong(parts[0]);
                        result += log(". Разбор ссылки: ownerId = " + ownerId + "\n");
                        long commentID = Long.parseLong(parts[2]);
                        result += log(". Разбор ссылки: commentID = " + commentID + "\n");
                        String type = "comment";
                        result += log(". Разбор ссылки: type = " + type + "\n");
                        result += log(deleteComment(commentID, ownerId));
                    }
                    if (parts.length > 3) {
                        result += log(". Ошибка разбора ссылки: обнаружено " + parts.length + " номеров, не удалось определить тип записи." + "\n");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    result += log(". Ошибка разбора ссылки: " + e.toString());

                }
                return result;
            }
            return "";
        }

        @Override public
        String getHelp() {
            return "[ Удалить запись или комментарий со стены ]\n" +
                    "---| botcmd deletepost postID ownerID\n\n";
        }
    }
    private class AddOwnerId implements Command{
        @Override public
        String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("wall"))
                if(commandParser.getWord().equals("add"))
                    return add(commandParser.getWord());

            return "";
        }
        public String add(long userId){
            log(". Внесение в список стен страницы " + userId + " ...");
            try {
                if (userId == -1L)
                    return "Ошибка добавления страницы " + userId + " в список стен. Возможно, вы ввели неправильный ID страницы.";
                if (getWallExists(userId) != null)
                    return "Ошибка добавления страницы " + userId + " в список стен. Страница уже находится в этом списке.";
                if(walls.add(new Wall(userId)))
                    return "Страница " + userId + " успешно добавлена в список стен. Сейчас в этом списке " + walls.size() + " страниц.";
                else
                    return "Страница " + userId + " почему-то не добавлена в список стен. Сейчас в этом списке " + walls.size() + " страниц.";
            }
            catch (Exception e){
                e.printStackTrace();
                return "Ошибка добавления страницы " + userId + " в список стен. " + e.toString();
            }
        }
        public String add(String userId){
            try{
                Long longId = applicationManager.getUserID(userId);
                return add(longId);
            }
            catch (Exception e){
                e.printStackTrace();
                return "Ошибка добавления страницы " + userId + " в список стен. " + e.toString();
            }
        }

        @Override public
        String getHelp() {
            return "[ Добавить стену в список обрабатываемых ]\n" +
                    "---| botcmd wall add (ID страницы)\n\n";
        }
    }
    private class RemOwnerId implements Command{
        @Override public
        String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("wall"))
                if(commandParser.getWord().equals("rem"))
                    return rem(commandParser.getWord());

            return "";
        }
        public String rem(long userId){
            log(". Удаления из списка стен страницы " + userId + " ...");
            try {
                if (userId == -1L)
                    return "Ошибка удаления страницы " + userId + " из списка стен. Возможно, вы ввели неправильный ID страницы.";
                if (getWallExists(userId) == null)
                    return "Ошибка удаления страницы " + userId + " из списка стен. Страница отсутствует в этом списке.";
                if(walls.remove(new Wall(userId)))
                    return "Страница " + userId + " успешно удалена из списка стен. Сейчас в этом списке " + walls.size() + " страниц.";
                else
                    return "Страница " + userId + " почему-то не удалена из списка стен. Сейчас в этом списке " + walls.size() + " страниц.";
            }
            catch (Exception e){
                e.printStackTrace();
                return "Ошибка удаления страницы " + userId + " из списка стен. " + e.toString();
            }
        }
        public String rem(String userId){
            try{
                Long longId = applicationManager.getUserID(userId);
                return rem(longId);
            }
            catch (Exception e){
                e.printStackTrace();
                return "Ошибка удаления страницы " + userId + " из списка стен. " + e.toString();
            }
        }

        @Override public
        String getHelp() {
            return "[ Удалить стену из списка обрабатываемых ]\n" +
                    "---| botcmd wall rem (ID страницы)\n\n";
        }
    }
    private class GetOwnerId implements Command{
        @Override public
        String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("wall"))
                if(commandParser.getWord().equals("get"))
                {
                    String resut = "";
                    for (int i = 0; i < walls.size(); i++) {
                        Wall wall = walls.get(i);
                        long id = wall.id;
                        if(id >= 0)
                            resut += wall.getName() + " ( http://vk.com/id" + id + " ) \n";
                        else
                            resut += wall.getName() + " ( http://vk.com/club" + Math.abs(id) + " ) \n";
                    }
                    return resut;
                }

            return "";
        }

        @Override public
        String getHelp() {
            return "[ Получить список обрабатываемых стен ]\n" +
                    "---| botcmd wall get\n\n";
        }
    }
    private class ClrOwnerId implements Command{
        @Override public
        String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("wall"))
                if(commandParser.getWord().equals("clr"))
                {
                    walls.clear();
                    return "Стены очищены. Сейчас обрабатывается "+walls.size()+" стен.";
                }

            return "";
        }

        @Override public
        String getHelp() {
            return "[ Очистить список обрабатываемых стен ]\n" +
                    "---| botcmd wall clr\n\n";
        }
    }
    private class AddLikes implements Command{
        @Override public
        String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("like")) {
                String result = "";
                String link = commandParser.getWord();
                result += log(". Получение ссылки: " + link + "\n");       //https://vk.com/wall10299185_13439?reply=19248
                link = link.replace("http://vk.com/wall", "");      //10299185_13439?reply=19248
                link = link.replace("https://vk.com/wall", "");     //10299185_13439?reply=19248
                link = link.replace("?reply=", "_");                //10299185_13439_19248
                result += log(". Преобразование ссылки: " + link + "\n");
                try {
                    String[] parts = link.split("\\_");             //[10299185, 13439, 19248]
                    if(parts.length < 2)
                        result += log(". Ошибка разбора ссылки: обнаружено всего " + parts.length + " номеров, а надо минимум 2." + "\n");
                    if(parts.length == 2){
                        long ownerId = Long.parseLong(parts[0]);
                        result += log(". Разбор ссылки: ownerId = " + ownerId + "\n");
                        long postId = Long.parseLong(parts[1]);
                        result += log(". Разбор ссылки: postId = " + postId + "\n");
                        String type = "post";
                        result += log(". Разбор ссылки: type = " + type + "\n");
                        result += log(addLikeByAllAccounts(ownerId, postId, type));
                    }
                    if(parts.length == 3){
                        long ownerId = Long.parseLong(parts[0]);
                        result += log(". Разбор ссылки: ownerId = " + ownerId + "\n");
                        long commentID = Long.parseLong(parts[2]);
                        result += log(". Разбор ссылки: commentID = " + commentID + "\n");
                        String type = "comment";
                        result += log(". Разбор ссылки: type = " + type + "\n");
                        result += log(addLikeByAllAccounts(ownerId, commentID, type));
                    }
                    if(parts.length > 3){
                        result += log(". Ошибка разбора ссылки: обнаружено " + parts.length + " номеров, не удалось определить тип записи." + "\n");
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                    result += log(". Ошибка разбора ссылки: " + e.toString());
                }
                return result;
            }

            return "";
        }

        @Override public
        String getHelp() {
            return "[ добавить лайк на комментарий ]\n" +
                    "---| botcmd like https://vk.com/wall10299185_13439?reply=19248 \n\n"+
                    "[ добавить лайк на запись ]\n" +
                    "---| botcmd like https://vk.com/wall10299185_13439 \n\n";
        }
    }
}
