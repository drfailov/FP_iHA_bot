package com.fsoft.ihabot.communucation.tg;

import com.fsoft.ihabot.ApplicationManager;
import com.fsoft.ihabot.Utils.F;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
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
        log("Попытка получить ссылку для скачивания файла фото вложения...");
        final ArrayList<java.io.File> files = new ArrayList<>();
        final ArrayList<Exception> exceptions = new ArrayList<>();
        getFile(new GetFileListener() {
            @Override
            public void gotFile(File file) {
                if(file.getFile_path() == null || file.getFile_path().isEmpty()){
                    exceptions.add(new Exception("Телеграм не прислал ссылку на файл для скачивания фото."));
                    return;
                }
                String directLink = "https://api.telegram.org/file/bot"+getId()+":"+getToken() + "/" + file.getFile_path();
                log("Ссылка на файл получена. Попытка скачать файл: " +directLink);
                String fileRes = F.getFileExtension(file.getFile_path());
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSS", Locale.US);
                String filename = simpleDateFormat.format(new Date());
                if(!fileRes.isEmpty())
                    filename += fileRes;
                java.io.File placeToSave = new java.io.File(ApplicationManager.getInstance().getTempFolder(), filename);
                log("Скачивать файл будем сюда: " + placeToSave.getAbsolutePath());

                try  {
                    URL u = new URL(directLink);
                    try(InputStream is = u.openStream()) {
                        DataInputStream dis = new DataInputStream(is);
                        byte[] buffer = new byte[1024];
                        int length;
                        try(FileOutputStream fos = new FileOutputStream(placeToSave)) {
                            while ((length = dis.read(buffer)) > 0) {
                                fos.write(buffer, 0, length);
                            }
                        }
                    }
                    log("Загрузка файла прошла без ошибок. Загружено " + placeToSave.length() + " байт.");
                } catch (MalformedURLException mue) {
                    exceptions.add(new Exception(log("Ошибка MalformedURLException скачивания файла фото: " + mue)));
                    mue.printStackTrace();
                    return;
                } catch (IOException ioe) {
                    exceptions.add(new Exception(log("Ошибка IOException скачивания файла фото: " + ioe.getLocalizedMessage())));
                    ioe.printStackTrace();
                    return;
                } catch (SecurityException se) {
                    exceptions.add(new Exception(log("Ошибка SecurityException скачивания файла фото: " + se.getLocalizedMessage())));
                    se.printStackTrace();
                    return;
                }
                if(placeToSave.isFile())
                    files.add(placeToSave);
                else
                    exceptions.add(new Exception(log("Всё вроде прошло норм, но файл фото не был скачан.")));
            }

            @Override
            public void error(Throwable error) {

            }
        }, fileId);


        Date started = new Date();
        //ждать пока что-то появится либо пока не будет какая-то ошибка, либо таймаут 30 секунд
        while (files.isEmpty() && exceptions.isEmpty()){
            if(Calendar.getInstance().getTime().getTime() - started.getTime() > 60000) {
                throw new TimeoutException("Таймаут, файл фото не получилось скачать.");
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
        throw new Exception("Во время скачивания файла фотографии ни файл ни ошибка не получены. Это странная ситуация.");
    }
}
