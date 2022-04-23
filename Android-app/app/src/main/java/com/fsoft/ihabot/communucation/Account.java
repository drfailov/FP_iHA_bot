package com.fsoft.ihabot.communucation;

import androidx.annotation.NonNull;

import com.fsoft.ihabot.ApplicationManager;
import com.fsoft.ihabot.Utils.CommandModule;
import com.fsoft.ihabot.Utils.FileStorage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Общая хуйня для всех аккаунтов в том числе и ВК. Токен ведь нужен всегда.
 * Я уже просто заебался копаться в горах говна основного аккаунта, поэтому
 * ищу что можно было бы вынести нахуй
 *
 * constructor() -> login() -> startAccount()
 * constructor() -> startAccount()
 * Created by Dr. Failov on 22.02.2017.
 */
public class Account extends CommandModule implements AccountBase {
    private long id = 0L;
    private String token = null;
    //место для хранения данных этого аккаунта и любого наследованного аккаунта
    private FileStorage fileStorage = null;
    private String fileName = null;
    //эту переменную делать true если получилось успешно
    // войти и делать false если возникли серьезные проблемы с аккаунтом
    private boolean token_ok = false;
    //это настройка для пользователя чтобы выключить аккаунт
    private boolean enabled = true;
    //устанавливается true во время запуска аккаунта
    private boolean running = false;
    //это - что-то наподобие временного комментария описывающего, что с этим аккаунтом происходит.
    private String state = "";
    //Имя под которым можно отображать этот аккаунт в программе
    private String screenName = null;
    private Runnable onStateChangedListener = null;


    public Account(ApplicationManager applicationManager, String fileName) {
        super();
        this.fileName = fileName;
        fileStorage = new FileStorage(fileName, applicationManager);
        log(". Создан аккаунт: " + fileName);

        enabled = getFileStorage().getBoolean("enabled", enabled);
        id = getFileStorage().getLong("id", id);
        token = getFileStorage().getString("token", token);
        screenName = getFileStorage().getString("screenName", screenName);

    }
    public boolean remove(){
        //удалить файл аккаунта
        return new File(fileStorage.getFilePath()).delete();
    }
    public void login(){
        //открывает процедуру (пере)логина.
        // По итогу задает значение для token
    }
    public void startAccount(){
        //если токен есть, эта функция его проверяет. Если токен валидный,
        //эта функция запускает работу во всех службах аккаунта, если isEnabled.
        running = true;
        log(". Аккаунт " + toString() + " запускается...");
        setState("Запускается...");
        //token_ok = true; //кажется, это не всегда связано
    }
    public void stopAccount(){
        //эта функция останавливает работу во всех службах аккаунта
        running = false;
        log(". Аккаунт " + toString() + " остановлен.");
        setState("Остановлен.");
        // token_ok = false; ////кажется, это не всегда связано
    }
    protected interface OnTokenValidityCheckedListener{
        void onTokenPass();
        void onTokenFail();
    }
    protected void checkTokenValidity(OnTokenValidityCheckedListener listener){
        //запускать проверку и вызывать лисенер...
        log(". Аккаунт " + toString() + " проверяется...");
        setState("Проверяется...");
    }
    public boolean isMine(String commandTreatment){
        //Эта функция должна отвечать за то, чтобы при обращении в команде
        // можно было понять что обращение именно к этому аккаунту
        //например bcd acc 098309832 enable
        try{
            return Long.parseLong(commandTreatment.trim()) == id;
        }catch (Exception e){
            return false;
        }
    }

    public boolean isToken_ok() {
        return token_ok;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public boolean isRunning() {
        return running;
    }
    public String getState() {
        return state;
    }
    public FileStorage getFileStorage() {
        return fileStorage;
    }
    public long getId() {
        return id;
    }
    public String getToken() {
        return token;
    }
    public String getFileName() {
        //именно это имя аккаунт получает при создании
        //эта функция нужна для того чтобы список аккаунтов можно было сохранить, не только загрузить
        return fileName;
    }
    public String getScreenName() {
        return screenName;
    }
    public void setScreenName(String screenName) {
        this.screenName = screenName;
        getFileStorage().put("screenName", screenName).commit();
    }

    //------------
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        getFileStorage().put("enabled", enabled).commit();
        if(isRunning() && !isEnabled())
            stopAccount();
        if(!isRunning() && isEnabled())
            startAccount();
    }
    public String state(String state) {
        setState(state);
        return state;
    }
    public void setState(String state) {
        String time = new SimpleDateFormat("HH:mm").format(new Date());
        this.state = time + " " + state;
        if(onStateChangedListener != null) {
            try {
                onStateChangedListener.run();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    public void setId(long id) {
        this.id = id;
        getFileStorage().put("id", id).commit();
    }
    public void setToken(String token) {
        this.token = token;
        getFileStorage().put("token", token).commit();
    }
    public void setToken_ok(boolean token_ok) {
        this.token_ok = token_ok;
    }
    public void setOnStateChangedListener(Runnable onStateChangedListener) {
        this.onStateChangedListener = onStateChangedListener;
    }

    @NonNull
    @Override public String toString() {
        return "Аккаунт " + id;
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Account account = (Account) o;

        if (getId() != account.getId()) return false;

        return true;
    }
    @Override public int hashCode() {
        return (int) (getId() ^ (getId() >>> 32));
    }

}
