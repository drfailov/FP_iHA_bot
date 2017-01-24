package com.fsoft.vktest;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import com.perm.kate.api.Api;
import com.perm.kate.api.Auth;
import com.perm.kate.api.Message;
import com.perm.kate.api.User;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Dr. Failov on 11.11.20
 * 14.
 */
public class VkAccount implements Command {
    public String userName = null;
    public long id = 0L;
    private VkAccount thisAccount = this;
    private ApplicationManager applicationManager;
    private ArrayList<Command> commands = new ArrayList<>();
    private final String API_ID = "4485671";
    private File file;
    private String token = null;
    private int apiCounter = 0;
    private Api api;
    private Dialog loginDialog = null;
    private long lastError = 0;
    private int errorRate = 0;
    private int messageCounter = 0;
    private int messageSent = 0;

    public VkAccount(ApplicationManager applicationManager, String fileAddress) {
        this.applicationManager = applicationManager;
        file = new File(fileAddress);
        init();
    }
    public VkAccount(ApplicationManager applicationManager, String fileAddress, String token, long id) {
        this.applicationManager = applicationManager;
        this.token = token;
        this.id = id;
        file = new File(fileAddress);
        init();
    }
    public @Override String toString() {
        String result = "";
        if(userName == null)
            result +=  "Аккаунт " + id;
        else
            result += userName;
        result = "("+result+") act="+active + " rdy="+isReady() + " api="+apiCounter;
        return result;
    }
    private void init(){
        commands.add(new Status());
        commands.add(new Save());
        commands.add(new Active());
        commands.add(new ReplyAnyMessage());
        commands.add(new AcceptAnyRequest());
        commands.add(new StatusBroadcasting());
        commands.add(new GetToken());
        commands.add(new MessageProcessing());
        commands.add(new SetInstruction());
        commands.add(new SetStatusText());
        commands.add(new GetChatCounter());
        commands.add(new ExitFromOfftopChats());
        commands.add(new MessageScanInterval());
        log(". Создан аккаунт: " + file.getPath());
        readFromFile();
        if (token == null)
            showLoginWindow();
        else {
            createApi();
        }
    }
    private void log(String text){
        if(applicationManager != null)
            ApplicationManager.log(text);
    }
    public Api api(){
        //пусть пока будет закомментировано. Это приводит к сильной потере производительности.
//        if(accountView != null)
//            accountView.refresh();
        waitUntilActive();
        apiCounter ++;
        markRead();
        return api;
    }
    public boolean isActive(){
        return active;
    }
    public int getApiCounter(){
        return apiCounter;
    }
    public void reportError(Exception e){
        if (e.toString().contains("Captcha needed")) {
            deactivateFor10Minutes();
        }
        if (e.toString().contains("invalid access_token")) {
            setActive(false);
        }
        if (e.toString().contains("Validation required")) {
            setActive(false);
        }
        if (e.toString().contains("invalid session")) {
            setActive(false);
        }

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
            //setActive(false);
        lastError = System.currentTimeMillis();
        markRead();
    }
    public @Override String process(String text){
        String result = "";
        for (int i = 0; i < commands.size(); i++) {
            result += commands.get(i).process(text);
        }
        return result;
    }
    public @Override String getHelp(){
        String result = "";
        for (int i = 0; i < commands.size(); i++) {
            result += commands.get(i).getHelp();
        }
        return result;
    }
    public View getView(Context context){
        return new AccountView(context);
    }
    public void close(){
        writeToFile();
        clearStatus();
    }

    //read and save
    void readFromFile(){
        if(!file.exists())
            return;
        log(". Чтение из файла " + file.getPath() + " ...");
        try {
            java.io.FileReader fileReader = new FileReader(file);
            StringBuilder stringBuilder = new StringBuilder();
            while(fileReader.ready())
                stringBuilder.append((char)fileReader.read());
            fileReader.close();
            String text = stringBuilder.toString();
            log(". прочитано: " + text);
            parseString(text);
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка чтения аккаунта "+file.getPath()+" : " + e.toString());
        }
    }
    void parseString(String text){
        try {
            JSONObject jsonObject = new JSONObject(text);
            if(jsonObject.has("id"))
                id = jsonObject.getLong("id");
            if(jsonObject.has("token"))
                token = jsonObject.getString("token");
            if(jsonObject.has("replyAnyMessage"))
                replyAnyMessage = jsonObject.getBoolean("replyAnyMessage");
            if(jsonObject.has("statusBroadcasting"))
                setStatusBroadcasting(jsonObject.getBoolean("statusBroadcasting"));
            if(jsonObject.has("acceptAnyRequest"))
                setAcceptAnyRequest(jsonObject.getBoolean("acceptAnyRequest"));
            if(jsonObject.has("instruction"))
                instruction = jsonObject.getString("instruction");
            if(jsonObject.has("messageProcessing"))
                setMessageProcessing(jsonObject.getBoolean("messageProcessing"));
            if(jsonObject.has("statusBroadcastingText"))
                statusBroadcastingText = jsonObject.getString("statusBroadcastingText");
            if(jsonObject.has("exitFromOfftopChats"))
                exitFromOfftopChats = jsonObject.getBoolean("exitFromOfftopChats");
            if(jsonObject.has("messageScanInterval"))
                messageScanInterval = jsonObject.getInt("messageScanInterval");
        }
        catch (Exception e){
            log("Error parsing " + toString() + " : " + e.toString());
            e.printStackTrace();
        }
    }
    String createParceable(){
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", id);
            jsonObject.put("token", token);
            jsonObject.put("replyAnyMessage", replyAnyMessage);
            jsonObject.put("statusBroadcasting", statusBroadcasting);
            jsonObject.put("acceptAnyRequest", acceptAnyRequest);
            jsonObject.put("messageProcessing", messageProcessing);
            jsonObject.put("instruction", instruction);
            jsonObject.put("statusBroadcastingText", statusBroadcastingText);
            jsonObject.put("exitFromOfftopChats", exitFromOfftopChats);
            jsonObject.put("messageScanInterval", messageScanInterval);
            return jsonObject.toString();
        }
        catch (Exception e){
            log("Error creating parceable " + toString() + " : " + e.toString());
            e.printStackTrace();
            return null;
        }
    }
    void writeToFile(){
        log(". Запись в файл " + file.getPath() + " ...");
        try {
            File parentFolder = file.getParentFile();
            if(!parentFolder.exists())
                parentFolder.mkdirs();
            FileWriter fileWriter = new FileWriter(file);
            String text = createParceable();
            log(". Запись: " + text);
            fileWriter.write(text);
            fileWriter.close();
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка записи аккаунта "+file.getPath()+" : " + e.toString());
        }
    }
    void removeFile(){
        file.delete();
    }


    //login process
    void showLoginWindow(){
        applicationManager.handler.post(new Runnable() {
            @Override
            public void run() {
                log("Login, please.");
                Context context = applicationManager.activity;
                final LoginView loginView = new LoginView(context);
                loginView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));

                LinearLayout frameLayout = new LinearLayout(context);
                frameLayout.setOrientation(LinearLayout.VERTICAL);
                frameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                frameLayout.addView(loginView);

                TextView textView = new TextView(context);
                textView.setText("Это окно закроется автоматически, если вход будет успешным.");
                frameLayout.addView(textView);

                Button button = new Button(context);
                button.setText("Назад");
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        loginView.goBack();
                    }
                });
                frameLayout.addView(button);

                loginDialog = new Dialog(context);
                loginDialog.setTitle("Войдите в аккаунт");
                loginDialog.setContentView(frameLayout);
                //loginDialog.setCancelable(false);
                loginDialog.getWindow().setLayout(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
                loginDialog.show();
            }
        });
    }
    void closeLoginWindow(){
        if(loginDialog != null) {
            loginDialog.dismiss();
            loginDialog = null;
        }
    }
    void createApi(){
        api = new Api(token, API_ID);
        setActive(true);
        writeToFile();
    }
    void getUserName(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                userName = applicationManager.vkCommunicator.getUserName(id);
            }
        }).start();
    }

    //auto-allow friends
    private boolean acceptAnyRequest = false;
    private Timer acceptAnyRequestTimer= null;
    private int acceptedRequests = 0;
    private void setAcceptAnyRequest(boolean in){
        log(". Принятие всех заявок в друзья для аккаунта "+userName+"  " + (in?"запускатеся...":"отключается..."));
        acceptAnyRequest = in;
        if(acceptAnyRequest)
            startAcceptAnyRequest();
        if(!acceptAnyRequest)
            stopAcceptAnyRequest();
    }
    private void startAcceptAnyRequest(){
        if(acceptAnyRequestTimer == null) {
            if (acceptAnyRequest) {
                log(". Запуск принятия всех заявок в друзья...");
                acceptAnyRequestTimer = new Timer();
                acceptAnyRequestTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        acceptAnyRequest();
                    }
                }, 1000, 600000);//10 minutes
            }
            else
                log(". Принятие всех заявок в друзья отключено.");
        }
        else
            log(". Принятие всех заявок в друзья уже активно.");
    }
    private void stopAcceptAnyRequest(){
        if(acceptAnyRequestTimer != null) {
            log(". Остановка принятия всех заявок в друзья...");
            acceptAnyRequestTimer.cancel();
            acceptAnyRequestTimer= null;
        }
        else
            log(". Принятие всех заявок в друзья уже отключено.");
    }
    private void acceptAnyRequest(){
        if(!applicationManager.running){
            stopAcceptAnyRequest();
            return;
        }
        if(acceptAnyRequest){
            acceptFriends();
            rejectFollowers();
        }
    }
    private void rejectFollowers(){
        try {
            log(". ("+userName+") Отклонение подписок...");
            ArrayList<Long> users = new ArrayList<>();
            ArrayList<Object[]> friends = api().getRequestsFriends(1);
            for (int i = 0; i < friends.size(); i++) {
                users.add((Long)friends.get(i)[0]);
            }

            for (int i = 0; i < users.size(); i++) {
                long id = users.get(i);
                long result = api().deleteFriend(id);
                if(result == 1)
                    log(". ("+userName+") пользователь удален из списка друзей: " + id);
                else if(result == 2)
                    log(". ("+userName+") заявка на добавление в друзья от данного пользователя отклонена: " + id);
                else if(result == 3)
                    log(". ("+userName+") рекомендация добавить в друзья данного пользователя удалена: " + id);
                else
                    log(". ("+userName+") Ошибка принятия заявки от " + id + " : " + result);
                sleep(2000);
            }
            log(". ("+userName+") отклонено "+users.size()+" подписок.");

        }
        catch (Exception e){
            e.printStackTrace();
            reportError(e);
            log("! ("+userName+")Ошибка принятия заявок в друзья: " + e.toString());
        }
    }
    private void acceptFriends(){
        try {
            log(". ("+userName+") Принятие новых заявок в друзья...");
            ArrayList<Long> users = new ArrayList<>();
            ArrayList<Object[]> friends = api().getRequestsFriends(0);
            for (int i = 0; i < friends.size(); i++) {
                users.add((Long)friends.get(i)[0]);
            }
            ArrayList<User> followers = api().getFollowers(id, 0, 50, null, null);
            for (int i = 0; i < followers.size(); i++) {
                users.add(followers.get(i).uid);
            }

            for (int i = 0; i < users.size(); i++) {
                long id = users.get(i);
                long result = api().addFriend(id, "", null, null);
                if(result == 2) {
                    log(". ("+userName+") Заявка одобрена: " + id);
                    acceptedRequests++;
                }
                else if(result == 1) {
                    log(". ("+userName+") Заявка отправлена: " + id);
                    acceptedRequests++;
                }
                else if(result == 4) {
                    log(". ("+userName+") Повторная отправка заявки: " + id);
                    acceptedRequests++;
                }
                else {
                    log(". ("+userName+") Ошибка принятия заявки от " + id + " : " + result);
                }
                sleep(2000);
            }
            log(". ("+userName+") принято "+users.size()+" заявок в друзья.");

        }
        catch (Exception e){
            e.printStackTrace();
            reportError(e);
            log("! ("+userName+")Ошибка принятия заявок в друзья: " + e.toString());
        }
    }

    //status proadcasting
    private boolean statusBroadcasting = false;
    private boolean statusBroadcastingCont = true;
    private Timer statusBroadcastingTimer = null;
    private String statusBroadcastingText = "TIME | работает NAME. Время с перезагрузки: WORKING";
    private void setStatusBroadcasting(boolean statusBroadcasting){
        this.statusBroadcasting = statusBroadcasting;
        log(". Трансляция статуса для аккаунта " + userName + " = " + statusBroadcasting);
        if(statusBroadcasting && statusBroadcastingTimer == null)
            startStatusBroadcasting();
        else if(!statusBroadcasting && statusBroadcastingTimer != null)
            stopStatusBroadcasting();
    }
    private void startStatusBroadcasting(){
        if(statusBroadcastingTimer == null && statusBroadcasting) {
            statusBroadcastingCont = true;
            statusBroadcastingTimer = new Timer();
            statusBroadcastingTimer.schedule(new TimerTask(){
                @Override
                public void run() {
                    broadcastStatus();
                }
            }, 10000, 90000);
        }
    }//---------------------------------------------------------------
    private void stopStatusBroadcasting(){
        statusBroadcastingCont = false;
        if(statusBroadcastingTimer != null){
            statusBroadcastingTimer.cancel();
            statusBroadcastingTimer = null;
        }
    }
    private void broadcastStatus(){
        if(!applicationManager.running){
            stopStatusBroadcasting();
            return;
        }
        if(!statusBroadcasting || !statusBroadcastingCont)
            return;
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            api().setOnline(null, null);
            markRead();
            String newStatus = statusBroadcastingText;
            newStatus = newStatus.replace("TIME", sdf.format(calendar.getTime()));
            newStatus = newStatus.replace("NAME", applicationManager.activity.getResources().getString(R.string.name));
            newStatus = newStatus.replace("WORKING", applicationManager.vkCommunicator.getWorkingTime());
            api().setStatus(newStatus);
            markSend();
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка установки статуса: " + e.toString());
            reportError(e);
        }
    }
    private void clearStatus(){
        if(isActive()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        api().setStatus("");
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        log("! Ошибка очистки статуса: "+ e.toString());
                    }
                }
            }).start();
        }
    }

    //message processing
    private boolean messageProcessing = false;
    private boolean exitFromOfftopChats = true;
    private long lastMessageProcessed = 0;
    private long lastTimeReadMessage = 0;
    private Timer messageProcessingTimer = null;
    private boolean messageProcessingCont = false;
    private HashMap <Long, Integer> chatOfftopCounter = new HashMap<>();
    private ArrayList<Long> instructed = new ArrayList<>();
    private boolean replyAnyMessage = false;
    private int messageScanInterval = 8;
    private boolean messagesBusy = false;//используется для предотвращения конфликта потоков.
    private String instruction = "Если ты хочешь со мной поговорить, начни своё сообщение с текста \"Бот, \", и тогда я тебе отвечу.";
    private void initProcessedMessages(){
        if(messageProcessingTimer == null && messageProcessing)
            new Thread(new Runnable() {
            @Override
            public void run() {
                sleep(10000);
                int trying = 0;
                while(true) {
                    try {
                        log(". Чтение личных сообщений "+userName+", попытка " + trying + " ...");
                        ArrayList<Message> messages = api().getMessages(0, false, 20);
                        markRead();
                        long lastMessage = 1;
                        for (int i = 0; i < messages.size(); i++)
                            lastMessage = Math.max(messages.get(i).mid, lastMessage);
                        setLastMessage(lastMessage);
                        startMessageProcessing();
                        break;
                    } catch (Exception e) {
                        log("! Ошибка Чтение личных сообщений "+userName + " :" + e.toString());
                        e.printStackTrace();
                        reportError(e);
                        trying ++;
                        if(trying > 5)
                            break;
                    }
                }
            }
        }).start();
    }
    private void startMessageProcessing(){
        if(messageProcessingTimer == null && messageProcessing) {
            messageProcessingCont = true;
            messageProcessingTimer = new Timer();
            messageProcessingTimer.schedule(new TimerTask() {
                @Override public void run() {
                    try {
                        if(messageProcessingCont)
                            findNewMessages();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        log("Ошибка обработки сообщений: " + e.toString());
                    }
                }
            }, 1000 * messageScanInterval, 1000 * messageScanInterval);
            messagesBusy = false;
        }
    }
    private void stopMessageProcessing(){
        messageProcessingCont = false;
        if(messageProcessingTimer != null){
            messageProcessingTimer.cancel();
            messageProcessingTimer = null;
            messagesBusy = false;
        }
    }
    private void setMessageProcessing(boolean messageProcessing){
        this.messageProcessing = messageProcessing;
        log(". Обработка сообщений для аккаунта " + userName + " = " + messageProcessing);
        if(messageProcessing)
            initProcessedMessages();
        else
            stopMessageProcessing();
    }
    private boolean isNewMessage(long messageID){
        return messageID > lastMessageProcessed;
    }
    private void setLastMessage(long maxMessageId){
        lastMessageProcessed = Math.max(maxMessageId, lastMessageProcessed);
    }
    private void findNewMessages(){
        if(!applicationManager.running){
            //значит программа уже завершена
            stopMessageProcessing();
            return;
        }
        if(lastMessageProcessed == 0)
            //значит программа не инициализирована
            return;
        if(messagesBusy)
            //значит где-то работает еще один поток
            return;
        messagesBusy = true;
        if(exitFromOfftopChats)
            excludeFromOffTopDialogs();
        long maxMessageId = 0;
        long timeOffset = (System.currentTimeMillis() - lastTimeReadMessage) / 1000;
        try {
            lastTimeReadMessage = System.currentTimeMillis();
            ArrayList<Message>  messages = api().getMessages(timeOffset + 10, false, 50);
            markRead();
            for (int i = 0; i < messages.size(); i++) {
                Message message = messages.get(i);
                registerOfftopMessage(message.chat_id);
                maxMessageId = Math.max(maxMessageId, message.mid);
                if(!message.body.contains(ApplicationManager.botMark()) && isNewMessage(message.mid)) {
                    processNewMessage(messages.get(i));
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
            log("! ошибка чтения сообщений: " + e.toString());
            reportError(e);
        }
        setLastMessage(maxMessageId);
        if (applicationManager.isStandby())
            sleep(60000);
        messagesBusy = false;
    }
    private void excludeFromOffTopDialogs(){
        try {
            Iterator<Map.Entry<Long, Integer>> iterator = chatOfftopCounter.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<Long, Integer> cur = iterator.next();
                if(cur.getValue() == 800){
                    sendMessage(0L, cur.getKey(), "Вы забыли про меня? =(");
                    markSend();
                }
                if(cur.getValue() == 900){
                    sendMessage(0L, cur.getKey(), "Все общаются... А обо мне все забыли.");
                    markSend();
                }
                if(cur.getValue() > 1000){
                    sendMessage(0L, cur.getKey(), "Скучно с вами! Никто со мной не общается=(");
                    markSend();
                    api().removeUserFromChat(cur.getKey(), id);
                    markRead();
                    chatOfftopCounter.remove(cur.getKey());
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
            log("! ошибка выхода из диалогов: " + e.toString());
            reportError(e);
        }
    }
    private void registerOfftopMessage(Long chat_id){
        if(chat_id == null)
            return;
        if(chatOfftopCounter.containsKey(chat_id)){
            int current = chatOfftopCounter.get(chat_id);
            chatOfftopCounter.put(chat_id, current + 1);
        }
        else {
            chatOfftopCounter.put(chat_id, 1);
        }
    }
    private void registerChatAnswer(Long chat_id) {
        if (chat_id == null)
            return;
        chatOfftopCounter.put(chat_id, 0);
    }
    private void processNewMessage(Message message){
        messageCounter ++;
        log(". MESS ("+userName+"): " + message.body);
        String reply = messageReceived(message.body.replaceAll("\\s+", " "), message.uid);
        if (reply != null  && !reply.equals("")) {
            log("! REPL ("+userName+"): " + reply);
            markAsRead(message.mid);
            messageSent ++;
            if(message.chat_id == null)
                sendMessage(message.uid, 0L, reply);
            else {
                registerChatAnswer(message.chat_id);
                sendMessage(message.uid, message.chat_id, reply);//, message.mid);
            }
            //sendMessage(message.uid, message.chat_id == null? 0 : message.chat_id, reply, message.mid);
        }
        else if(!applicationManager.isStandby()
                && replyAnyMessage
                && !instructed.contains(message.uid)
                && !message.body.contains(ApplicationManager.botMark())
                && !applicationManager.messageProcessor.ignorId.contains(message.uid)
                && message.chat_members == null){
            markAsRead(message.mid);
            log("! Инструкция ("+userName+"): " + instruction);
            if(message.chat_id == null)
                sendMessage(message.uid, 0L, instruction);
            else
                sendMessage(message.uid, message.chat_id, instruction, message.mid);
            instructed.add(message.uid);
        }
    }
    private String messageReceived(String message, Long senderId) {
        if(message.contains(ApplicationManager.botMark()))
            return null;
        return applicationManager.processMessage(message, senderId);
    }
    private String sendMessage(Long userId, Long chatId, String text){
        return sendMessage(userId, chatId, text, new ArrayList<Long>());
    }
    private String sendMessage(Long userId, Long chatId, String text, Long forward){
        ArrayList<Long> forwardList = new ArrayList<>();
        forwardList.add(forward);
        return sendMessage(userId, chatId, text, forwardList);
    }
    private String sendMessage(Long userId, Long chatId, String text, ArrayList<Long> forward){
        try {
            String result = null;
            String[] parts = applicationManager.splitText(text, 4000);
            for (int i = 0; i < parts.length; i++) {
                result = api().sendMessage(userId, chatId, ApplicationManager.botMark() + (i != 0 ? " (часть " + i + ") " : " ") + parts[i], null, null, null, forward, null, null, null, null);
                markSend();
            }
            return result;
        }
        catch (Exception e){
            e.printStackTrace();
            log("! ошибка: " + e.toString());
            reportError(e);
            return e.toString();
        }
    }
    private void markAsRead(long mid){
        ArrayList<Long> mids = new ArrayList<>();
        mids.add(mid);
        try {
            api().markAsNewOrAsRead(mids, true);
            markRead();
        }
        catch (Exception e){
            log("! Error while mark as read: " + e.toString());
            e.printStackTrace();
            reportError(e);
        }
    }
    //longpoll not used
//    private void initProcessedMessages1(){
//        messageProcessingThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                messageProcessingThreadCounter ++;
//                sleep(10000);
//                try {
//                    log(". Загрузка longpoll сервера...");
//                    Object[] obj = api().getLongPollServer(null, null);
//                    String key = (String)obj[0];
//                    String server = (String)obj[1];
//                    Long ts = (Long)obj[2];
//                    while(true){
//                        String response = api().sendLongPollRequest(server, key, ts);
//                        if(response.contains("failed"))
//                        {
//                            //если failed - прекратить текущую сессию и начать новую
//                            initProcessedMessages();
//                            messageProcessingThreadCounter --;
//                            break;
//                        }
//                        ts = processResponse(response);
//                        sleep(1000);
//                    }
//                }
//                catch (Exception e){
//                    e.printStackTrace();
//                    log("! Longpoll error: " + e.toString());
//                    initProcessedMessages();
//                    messageProcessingThreadCounter --;
//                    reportError(e);
//                }
//            }
//        });
//        messageProcessingThread.start();
//    }
//    private Long processResponse(String resp) throws JSONException{
//        JSONObject response = new JSONObject(resp);
//        Long ts = response.getLong("ts");
//        JSONArray updates = response.getJSONArray("updates");
//        ArrayList<Long> messageIDs = new ArrayList<>();
//        for (int i = 0; i < updates.length(); i++) {
//            JSONArray event = updates.getJSONArray(i);
//            int eventType = event.getInt(0);
//            if(eventType == 4){//НОВОЕ СООБЩЕНИЕ
//                long message_id = event.getLong(1);
//                messageIDs.add(message_id);
//            }
//        }
//        if(messageIDs.size() > 0) {
//            ArrayList<Message> messages = getMessageById(messageIDs);
//            if (messages != null) {
//                for (int i = 0; i < messages.size(); i++) {
//                    Message message = messages.get(i);
//                    if(!message.is_out)
//                        processNewMessage(message);
//                }
//            } else
//                log("! Было пропущено " + messageIDs.size() + "сообщений, т.к. их отправителей получить не удалось!");
//        }
//        return ts;
//    }
//    private ArrayList<Message> getMessageById(ArrayList<Long> uid){
//        for (int attempt = 0; attempt < 5; attempt++) {
//            try{
//                ArrayList<Message> messages= api().getMessagesById(uid);
//                markRead();
//                return messages;
//            }
//            catch (Exception e){
//                e.printStackTrace();
//                reportError(e);
//            }
//        }
//        return null;
//    }

    //overload save
    private boolean active = false;
    long lastRequestTime = 0;
    long requestGap = 0;
    void waitUntilActive(){
        while (!isReady())
            sleep(500);
    }
    boolean isReady(){
        if(!active)
            return false;
        long now = System.currentTimeMillis();
        return (now - lastRequestTime) > requestGap;
    }
    void deactivateFor10Minutes(){
        log("Деактивация аккаунта "+userName+" на 10 минут...");
        setActive(false);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                setActive(true);
            }
        }, 600000);
    }
    void sleep(int mili){
        try{
            Thread.sleep(mili);
        }
        catch (Exception e){}
    }
    void markRead(){
        //по замыслу, данная функция предусматривает ручную установку типа выполняемой с аккаунтом операции
        // Операции чтения с сервера должны проводиться с интервалом, указанным ниже
        // операции записи имеют больший интервал.
        // Поэтому, тут две функции. Если их вызвать, ближайшее время этот аккаунт будет не готов для выполнения операций.
        lastRequestTime = System.currentTimeMillis();
        requestGap = 1000;
    }
    void markSend(){
        lastRequestTime = System.currentTimeMillis();
        requestGap = 10000;
    }
    void setActive(boolean act){
        if(act && !active && applicationManager.running){ //только сейчас сделали активным
            getUserName();
            startStatusBroadcasting();
            startAcceptAnyRequest();
            initProcessedMessages();
            //startMessageProcessing(); //перенесено в initProcessedMessages
        }
        if((!act && active) || !applicationManager.running){ //сделали неактивным
            stopStatusBroadcasting();
            stopMessageProcessing();
            stopAcceptAnyRequest();
        }
        active = act;
        log(". Аккаунт " + toString() + " " + (active ? "активирован" : "деактивирован"));
    }

    class LoginView extends WebView{
        public LoginView(Context context) {
            super(context);
            setWebViewClient(new VkontakteWebViewClient());
            loadUrl(Auth.getUrl(API_ID, Auth.getSettings()));
        }
        class VkontakteWebViewClient extends WebViewClient {
            @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                parseUrl(url);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
            private void parseUrl(String url) {
                try {
                    if(url==null)
                        return;
                    log(". url=" + url);
                    if(url.startsWith(Auth.redirect_url)) {
                        if(!url.contains("error=")){
                            String[] auth=Auth.parseRedirectUrl(url);
                            token = auth[0];
                            id = Long.parseLong(auth[1]);
                            if(token != null && id != 0) {
                                closeLoginWindow();
                                createApi();
                                if(applicationManager.vkCommunicator.walls.size() == 0)
                                    applicationManager.vkCommunicator.addOwnerID(id);
                                if(applicationManager.vkAccounts.size() == 1){
                                    String help = "Поздравляю! Ты успешно вошел в аккаунт. Не торопись закрывать это сообщение. Пока ты читаешь это, программа готовится работать с твоей страницей.\n" +
                                            "Через секунд 30 можно проверять, работает ли программа. \n" +
                                            "- Убедись, что комментарии на стене аккаунта включены, без них программа работать не будет.\n" +
                                            "- Напиши на стене текст \"botcmd help\" от имени владельца. Бот должен ответить в комментарии полной инструкцией со всеми командами.\n" +
                                            "- Чтобы просто поговорить с ботом, каждое своё сообщение на стене начинай с обращения \"Бот,\", например: \"Бот, как дела?\".\n" +
                                            "Дальше разбирайся сам:) Удачи!";
                                    log(help);
                                    applicationManager.messageBox(help);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log("! " + e.toString());
                    e.printStackTrace();
                }
            }
        }
    }
    class AccountView extends LinearLayout{
        Handler handler;
        TextView textViewName;
        TextView textViewActive;
        TextView textViewReady;
        TextView textViewApi;
        TextView textViewStatusTranslation;
        TextView textViewMessageProcessing;
        TextView textViewReplyAnyMessage;
        TextView textViewMessages;
        TextView textViewMessagesSent;
        TextView textViewAcceptAnyRequest;
        boolean lastState = false;
        Context context = null;
        AlertDialog alertDialog = null;

        AccountView(Context context) {
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

            int color = isActive() ? (isReady() ? Color.GREEN : Color.RED) : Color.GRAY;

            {
                TextView textView = textViewName = new TextView(context);
                textView.setPadding(20, 0, 0, 0);
                textView.setTextColor(color);
                textView.setText(userName);
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
                TextView textView = textViewReady = new TextView(context);
                textView.setTextColor(color);
                textView.setText("готов = " + isReady());
                textView.setTextSize(10);
                addView(textView);
            }
            {
                TextView textView = textViewApi = new TextView(context);
                textView.setTextColor(color);
                textView.setText("обращений к API = " + getApiCounter());
                textView.setTextSize(10);
                addView(textView);
            }
            {
                TextView textView = textViewMessageProcessing = new TextView(context);
                textView.setTextColor(color);
                textView.setText("обработка сообщений = " + messageProcessing);
                textView.setTextSize(10);
                addView(textView);
            }
            {
                TextView textView = textViewReplyAnyMessage = new TextView(context);
                textView.setTextColor(color);
                textView.setText("отвечать инструкцией = " + replyAnyMessage);
                textView.setTextSize(10);
                addView(textView);
            }
            {
                TextView textView = textViewMessages = new TextView(context);
                textView.setTextColor(color);
                textView.setText("новых сообщений = " + messageCounter);
                textView.setTextSize(10);
                addView(textView);
            }
            {
                TextView textView = textViewMessagesSent = new TextView(context);
                textView.setTextColor(color);
                textView.setText("отправлено сообщений = " + messageSent);
                textView.setTextSize(10);
                addView(textView);
            }
            {
                TextView textView = textViewStatusTranslation = new TextView(context);
                textView.setTextColor(color);
                textView.setText("трансляция статуса = " + statusBroadcasting);
                textView.setTextSize(10);
                addView(textView);
            }
            {
                TextView textView = textViewAcceptAnyRequest = new TextView(context);
                textView.setTextColor(color);
                textView.setText("принятие всех заявок = " + acceptAnyRequest);
                textView.setTextSize(10);
                addView(textView);
            }
            addView(getDelimiter(context));
        }
        public void refresh(){
            boolean active = isActive();
            if(active == lastState)
                return;
            lastState = active;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    int color = isActive() ? (isReady() ? Color.GREEN : Color.RED) : Color.GRAY;

                    textViewName.setTextColor(color);
                    textViewName.setText(userName);
                    textViewActive.setTextColor(color);
                    textViewActive.setText("активен = " + isActive());
                    textViewReady.setTextColor(color);
                    textViewReady.setText("готов = " + isReady());
                    textViewApi.setTextColor(color);
                    textViewApi.setText("обращений к API = " + getApiCounter());
                    textViewMessages.setTextColor(color);
                    textViewMessages.setText("новых сообщений = " + messageCounter);
                    textViewStatusTranslation.setTextColor(color);
                    textViewStatusTranslation.setText("трансляция статуса = " + statusBroadcasting);
                    textViewAcceptAnyRequest.setTextColor(color);
                    textViewAcceptAnyRequest.setText("принятие всех заявок = " + acceptAnyRequest);
                    textViewMessageProcessing.setTextColor(color);
                    textViewMessageProcessing.setText("обработка сообщений = " + messageProcessing);
                    textViewReplyAnyMessage.setTextColor(color);
                    textViewReplyAnyMessage.setText("отвечать инструкцией = " + replyAnyMessage);
                }
            });
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
                textView.setText("Аккаунт " + userName);
                textView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                textView.setGravity(Gravity.CENTER);
                textView.setTextSize(20);
                textView.setTextColor(Color.WHITE);
                linearLayout.addView(textView);
                linearLayout.addView(getDelimiter(context));
            }

            linearLayout.addView(getOnOffRow("Активный ("+isActive()+")",new OnClickListener() {
                @Override
                public void onClick(View view) {
                    setActive(true);
                    closeDialog();
                }
            }, new OnClickListener() {
                @Override
                public void onClick(View view) {
                    setActive(false);
                    closeDialog();
                }
            }));


            linearLayout.addView(getOnOffRow("Трансляция статуса ("+statusBroadcasting+")",new OnClickListener() {
                @Override
                public void onClick(View view) {
                    setStatusBroadcasting(true);
                    closeDialog();
                }
            }, new OnClickListener() {
                @Override
                public void onClick(View view) {
                    setStatusBroadcasting(false);
                    closeDialog();
                }
            }));


            linearLayout.addView(getOnOffRow("Принимать все заявки ("+acceptAnyRequest+")",new OnClickListener() {
                @Override
                public void onClick(View view) {
                    setAcceptAnyRequest(true);
                    closeDialog();
                }
            }, new OnClickListener() {
                @Override
                public void onClick(View view) {
                    setAcceptAnyRequest(false);
                    closeDialog();
                }
            }));


            linearLayout.addView(getOnOffRow("Обрабатывать сообщения ("+messageProcessing+")",new OnClickListener() {
                @Override
                public void onClick(View view) {
                    setMessageProcessing(true);
                    closeDialog();
                }
            }, new OnClickListener() {
                @Override
                public void onClick(View view) {
                    setMessageProcessing(false);
                    closeDialog();
                }
            }));


            linearLayout.addView(getOnOffRow("Отвечать инструкцией ("+replyAnyMessage+")",new OnClickListener() {
                @Override
                public void onClick(View view) {
                    replyAnyMessage = (true);
                    closeDialog();
                }
            }, new OnClickListener() {
                @Override
                public void onClick(View view) {
                    replyAnyMessage = (false);
                    closeDialog();
                }
            }));


            {
                Button button = new Button(context);
                button.setText("Перезайти в аккаунт");
                button.setTextColor(Color.YELLOW);
                button.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showLoginWindow();
                        closeDialog();
                    }
                });
                linearLayout.addView(button);
            }
            {
                Button button = new Button(context);
                button.setText("Удалить");
                button.setTextColor(Color.RED);
                button.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        applicationManager.vkAccounts.removeAccount(thisAccount);
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
    }

    //commands
    class Status implements Command{
        public @Override String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("status"))
                return "Аккаунт " + userName + " id="+id + "\n" +
                        "Аккаунт " + userName + " файл="+file + "\n" +
                        "Аккаунт " + userName + " счетник обращений к API="+apiCounter + "\n" +
                        "Аккаунт " + userName + " активен="+active + "\n" +
                        "Аккаунт " + userName + " готов="+isReady() + "\n" +
                        "Аккаунт " + userName + " обработка сообщений="+messageProcessing + "\n" +
                        "Аккаунт " + userName + " сообщения обрабатываются="+(messageProcessingCont && messageProcessingTimer != null) + "\n" +
                        "Аккаунт " + userName + " текст статуса="+statusBroadcastingText + "\n" +
                        "Аккаунт " + userName + " обновлять статус="+statusBroadcasting + "\n" +
                        "Аккаунт " + userName + " принимать все заявки в друзья="+acceptAnyRequest + "\n" +
                        "Аккаунт " + userName + " принято заявок в друзья="+acceptedRequests + "\n" +
                        "Аккаунт " + userName + " новых сообщений="+ messageCounter + "\n" +
                        "Аккаунт " + userName + " отправлено сообщений="+ messageSent + "\n" +
                        "Аккаунт " + userName + " отвечать на все сообщения="+ replyAnyMessage + "\n" +
                        "Аккаунт " + userName + " готов="+isReady() + "\n" +
                        "Аккаунт " + userName + " инструкция="+instruction + "\n";
            return "";
        }

        public @Override String getHelp() {
            return "";
        }
    }
    class Save implements Command{
        public @Override String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("save")) {
                writeToFile();
                return "Запись аккаунта " + userName + " в файл " + file + " ...\n";
            }
            return "";
        }

        public @Override String getHelp() {
            return "";
        }
    }
    class Active implements Command{
        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("account"))
                if(commandParser.getLong() == id)
                    if(commandParser.getWord().equals("active")) {
                        setActive(commandParser.getBoolean());
                        return "("+userName+") включен = " + active;
                    }
            return "";
        }

        @Override
        public String getHelp() {
            return "[ Включить или выключить аккаунт "+userName+" ]\n" +
                    "---| botcmd account " + id + " active on   \n\n";
        }
    }
    class StatusBroadcasting implements Command{
        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("account"))
                if(commandParser.getLong() == id)
                    if(commandParser.getWord().equals("statusbroadcasting")) {
                        setStatusBroadcasting(commandParser.getBoolean());
                        return "("+userName+") Трансляция статуса = " + statusBroadcasting;
                    }
            return "";
        }

        @Override
        public String getHelp() {
            return "[ Включить или выключить трансляцию статуса для аккаунта "+userName+" ]\n" +
                    "---| botcmd account " + id + " statusbroadcasting on   \n\n";
        }
    }
    class ReplyAnyMessage implements Command{
        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("account"))
                if(commandParser.getLong() == id)
                    if(commandParser.getWord().equals("replyanymessage")) {
                        replyAnyMessage = (commandParser.getBoolean());
                        return "("+userName+") Ответ инструкцией = " + replyAnyMessage;
                    }
            return "";
        }

        @Override
        public String getHelp() {
            return "[ Включить или выключить ответ инструкцией на любое сообщение для аккаунта "+userName+" ]\n" +
                    "---| botcmd account " + id + " replyanymessage on   \n\n";
        }
    }
    class SetInstruction implements Command{
        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("account"))
                if(commandParser.getLong() == id)
                    if(commandParser.getWord().equals("setinstruction")) {
                        instruction = commandParser.getText();
                        return "("+userName+") instruction = " + instruction;
                    }
            return "";
        }

        @Override
        public String getHelp() {
            return "[ Изменить инструкцию для аккаунта "+userName+" ]\n" +
                    "---| botcmd account " + id + " setinstruction (текст инструкции)   \n\n";
        }
    }
    class SetStatusText implements Command{
        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("account"))
                if(commandParser.getLong() == id)
                    if(commandParser.getWord().equals("setstatustext")) {
                        statusBroadcastingText = commandParser.getText();
                        return "("+userName+") statusBroadcastingText = " + statusBroadcastingText;
                    }
            return "";
        }

        @Override
        public String getHelp() {
            return "[ Изменить текст статуса для аккаунта "+userName+" ]\n" +
                    "[   фрагмент TIME заменяется на время обновления статуса ]\n" +
                    "[   фрагмент NAME заменяется на название и версию бота ]\n" +
                    "[   фрагмент WORKING заменяется на время работы с момента перезагрузки ]\n" +
                    "---| botcmd account " + id + " setstatustext (текст инструкции)   \n\n";
        }
    }
    class GetToken implements Command{
        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("account"))
                if(commandParser.getLong() == id)
                    if(commandParser.getWord().equals("gettoken"))
                        return "("+userName+") id = " + id +
                                "\n("+userName+") token = " + token +
                                "\n\nДанные для вставки в другого бота:\n" +
                                "botcmd account add "+token+" "+id+" ";
            return "";
        }

        @Override
        public String getHelp() {
            return "[ Получить токен (секретный(!) код) для аккаунта "+userName+" ]\n" +
                    "---| botcmd account " + id + " gettoken   \n\n";
        }
    }
    class AcceptAnyRequest implements Command{
        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("account"))
                if(commandParser.getLong() == id)
                    if(commandParser.getWord().equals("acceptanyrequest")) {
                        setAcceptAnyRequest(commandParser.getBoolean());
                        return "("+userName+") Приниятие всех в друзья = " + acceptAnyRequest;
                    }
            return "";
        }

        @Override
        public String getHelp() {
            return "[ Включить или выключить принятие всех заявок в друзья для аккаунта "+userName+" ]\n" +
                    "---| botcmd account " + id + " acceptanyrequest on   \n\n";
        }
    }
    class MessageProcessing implements Command{
        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("account"))
                if(commandParser.getLong() == id)
                    if(commandParser.getWord().equals("messageprocessing")) {
                        setMessageProcessing(commandParser.getBoolean());
                        return "("+userName+") Обработка личных сообений = " + messageProcessing;
                    }
            return "";
        }

        @Override
        public String getHelp() {
            return "[ Включить или выключить обработку личных сообщений для аккаунта "+userName+" ]\n" +
                    "---| botcmd account " + id + " messageprocessing on   \n\n";
        }
    }
    class GetChatCounter implements Command{
        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("account"))
                if(commandParser.getLong() == id)
                    if(commandParser.getWord().equals("getchatcounter")) {
                        String result = "Счетчики оффтопа по диалогам: \n";
                        Iterator<Map.Entry<Long, Integer>> iterator = chatOfftopCounter.entrySet().iterator();
                        while (iterator.hasNext()) {
                            Map.Entry<Long, Integer> cur = iterator.next();
                            result += "Диалог: " + cur.getKey() + ", сообщений оффтопа: " + cur.getValue() + " \n";
                        }
                        return result;
                    }
            return "";
        }

        @Override
        public String getHelp() {
            return "[ Получить счетчики чатов для аккаунта "+userName+" ]\n" +
                    "---| botcmd account " + id + " getchatcounter  \n\n";
        }
    }
    class ExitFromOfftopChats implements Command{
        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("account"))
                if(commandParser.getLong() == id)
                    if(commandParser.getWord().equals("exitfromofftopchats")) {
                        setMessageProcessing(commandParser.getBoolean());
                        return "("+userName+") Выход из оффтопных бесед = " + exitFromOfftopChats;
                    }
            return "";
        }

        @Override
        public String getHelp() {
            return "[ Включить или выключить автоматический выход из оффтопных чатов для аккаунта для аккаунта "+userName+" ]\n" +
                    "---| botcmd account " + id + " exitfromofftopchats on   \n\n";
        }
    }
    class MessageScanInterval implements Command{
        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("account"))
                if(commandParser.getLong() == id)
                    if(commandParser.getWord().equals("messagescaninterval"))
                        return "("+userName+") Интервал сканирования личных сообщений = " + (messageScanInterval = commandParser.getInt()) + " секунд. Изменения вступят в силу после перезапуска программы.";
            return "";
        }

        @Override
        public String getHelp() {
            return "[ Задать интервал чтения (в секундах) личных сообщений для аккаунта "+userName+" ]\n" +
                    "---| botcmd account " + id + " messagescaninterval 8   \n\n";
        }
    }
}
