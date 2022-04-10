package com.fsoft.ihabot.communucation.tg;

import com.fsoft.ihabot.Utils.ApplicationManager;
import com.fsoft.ihabot.Utils.F;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.concurrent.TimeoutException;

public class TgAccount extends TgAccountCore {
    private MessageProcessor messageProcessor = null;

    public TgAccount(ApplicationManager applicationManager, String fileName) {
        super(applicationManager, fileName);
        messageProcessor = new MessageProcessor(applicationManager, this);
    }

    @Override
    public void startAccount() {
        super.startAccount();
        checkTokenValidity(new OnTokenValidityCheckedListener() {
            @Override
            public void onTokenPass() {
                messageProcessor.startModule();
            }

            @Override
            public void onTokenFail() {

            }
        });
    }

    @Override
    public void stopAccount() {
        super.stopAccount();
        messageProcessor.stopModule();
    }


    public MessageProcessor getMessageProcessor() {
        return messageProcessor;
    }
    public void sendMessage(long chatId, com.fsoft.ihabot.answer.Message message){
        messageProcessor.sendAnswer(chatId, message);
    }
    /**
     * Блокирующая(!) версия функции для выгрузки файла
     */
    public java.io.File downloadPhotoAttachment(String fileId) throws Exception{
        log("Попытка получить ссылку на файл...");
        final ArrayList<java.io.File> files = new ArrayList<>();
        final ArrayList<Exception> exceptions = new ArrayList<>();
        getFile(new GetFileListener() {
            @Override
            public void gotFile(File file) {
                if(file.getFile_path() == null || file.getFile_path().isEmpty()){
                    exceptions.add(new Exception("Телеграм не прислал ссылку на файл для загрузки."));
                    return;
                }
                String directLink = "https://api.telegram.org/file/bot"+getId()+":"+getToken() + "/" + file.getFile_path();
                log("Ссылка на файл получена: " +directLink);
                files.add(new java.io.File(""));
            }

            @Override
            public void error(Throwable error) {

            }
        }, fileId);


        Date started = new Date();
        //ждать пока что-то появится либо пока не будет какая-то ошибка, либо таймаут 30 секунд
        while (files.isEmpty() && exceptions.isEmpty()){
            if(Calendar.getInstance().getTime().getTime() - started.getTime() > 30000) {
                throw new TimeoutException("Таймаут, файл не был получен.");
            }
        }
        if(!files.isEmpty()){
            log("Был получен файл: " + files.get(0).getName());
            return files.get(0);
        }
        if(!exceptions.isEmpty()) {
            log("Была получена ошибка: " + exceptions.get(0));
            throw exceptions.get(0);
        }
        throw new Exception("Ни файл ни ошибка не получены.");
    }
}
