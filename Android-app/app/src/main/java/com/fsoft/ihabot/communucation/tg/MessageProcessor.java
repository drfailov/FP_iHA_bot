package com.fsoft.ihabot.communucation.tg;
//todo определять чаты и игнорить их, заполнять поля message

import com.fsoft.ihabot.ApplicationManager;
import com.fsoft.ihabot.Utils.CommandModule;
import com.fsoft.ihabot.Utils.F;
import com.fsoft.ihabot.answer.AnswerElement;
import com.fsoft.ihabot.answer.Attachment;
import com.fsoft.ihabot.configuration.AdminList;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Этот класс принимает все сообщения в телеге и тут в целом решается судьба каждого сообщения
 */
public class MessageProcessor extends CommandModule {
    private TgAccount tgAccount = null;
    private ApplicationManager applicationManager = null;
    private long lastUpdateId = 0;
    private boolean isChatsEnabled = true;
    private int errors = 0;
    private int messagesReceivedCounter = 0;
    private int messagesSentCounter = 0;


    public MessageProcessor(ApplicationManager applicationManager, TgAccount tgAccount) {
        this.applicationManager = applicationManager;
        this.tgAccount = tgAccount;
        isChatsEnabled = tgAccount.getFileStorage().getBoolean("chatsEnabled", isChatsEnabled);
    }

    @Override public void stop() {
        super.stop();
        stopModule();
    }

    public void startModule(){
        log("Обработка сообщений для аккаунта "+tgAccount+" запускается...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                update();
            }
        }).start();
    }
    public void stopModule(){
        log("Обработка сообщений для аккаунта "+tgAccount+" останавливается...");
    }

    public int getMessagesReceivedCounter() {
        return messagesReceivedCounter;
    }
    public void incrementMessagesReceivedCounter(){
        messagesReceivedCounter++;
    }
    public int getMessagesSentCounter() {
        return messagesSentCounter;
    }
    public void incrementMessagesSentCounter(){
        messagesSentCounter++;
    }
    public boolean isChatsEnabled() {
        return isChatsEnabled;
    }
    public void setChatsEnabled(boolean chatsEnabled) {
        isChatsEnabled = chatsEnabled;
        tgAccount.getFileStorage().put("chatsEnabled", isChatsEnabled).commit();
    }

    public void update(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                updateAsync();
            }
        }).start();
    }
    public void updateAsync(){
        log(". Sending request for "+tgAccount+" update...");
        tgAccount.getUpdates(new TgAccountCore.GetUpdatesListener() {
            @Override
            public void gotUpdates(final ArrayList<Update> updates) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            log(". Received " + updates.size() + " " + tgAccount + " updates.");
                            errors = 0;
                            if (isRunning()) {
                                processUpdates(updates);
                                update();
                            }
                        }
                        catch (Exception e){
                            e.printStackTrace();
                            log("! Ошибка "+e.getMessage()+" обработки апдейтов аккаунта " + tgAccount);
                        }
                    }
                }).start();
            }

            @Override
            public void error(final Throwable error) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        log(". Error getting "+tgAccount+" updates: " + error.getClass().getName() + " " +error.getMessage()+". Retry...");
                        errors ++;
//                        if(errors > 20){
//                            tgAccount.stopAccount();
//                            tgAccount.state("Слишком много ошибок " + error.getClass().getName() + " " +error.getMessage());
//                        }
                        if(isRunning()) {
                            if(errors != 0) {
                                int wait = errors;
                                if(wait > 60)
                                    wait = 60;
                                log(tgAccount.state("Ожидание " + wait + " секунд после ошибки..."));
                                F.sleep(wait * 1000);
                            }
                            update();
                        }
                    }
                }).start();
            }
        }, lastUpdateId+1, 20);
    }

    public void processUpdates(ArrayList<Update> updates) throws Exception{
        for (Update update:updates){
            if(update.getUpdate_id() > lastUpdateId)
                lastUpdateId = update.getUpdate_id();
            if(update.getMessage() != null){
                processMessage(update.getMessage());
            }
            else if(update.getEdited_message() != null){
                processMessage(update.getEdited_message());
            }
        }
    }
    public void processMessage(final Message message) throws Exception{
        new Thread(new Runnable() {
            @Override
            public void run() {
                processMessageAsync(message);
            }
        }).start();
        //Эта задержка поможет обработать сообщения в правильной последовательности в случае,
        // если было переслано сразу несколько сообщений
        Thread.sleep(500);
    }

    public void processMessageAsync(final Message message){
        if(applicationManager == null) return;
        if(applicationManager.getAnswerDatabase() == null) return;

        //Это выполняется в отдельном потоке (можно делать что хочешь и сколько хочешь)
        /*
        - Когда получено сообщение:
        - Проверить не является ли пользователь админинистратором. Если является, обработать как команду

        - привести текст входящего сообшения к нижнему регистру
        - убрать обращение бот
        - оставить только последнее предложение
        - Сохранить информацию о том есть ли знак вопроса
        - Убрать все символы и знаки, оставить только текст
        - если после этого всего строка оказывается пустая - результат сравнения точно 0
        - заменить синонимы (полнотекстовым образом)
        - Заменить символы которые часто забивают писать (ё ъ щ)
        - устранить любые символы повторяющиеся несколько раз
        - Тримануть
        - разложить на слова
        - сравнить все слова со всеми  по алгоритму Жаро-Винклера
        - при сравнении также учитывать: длину слова
        - При сравнении между вариантами учитывать если есть слова которые не участвовали в сравнении
        - ПРи сравнении учитывать наличие знака вопроса в тексте*.

        - Выбрать максимальный.
        - Если есть незагруженные на сервер вложения, внести их в очередь для загрузки на сервер
        - Инициировать загрузку вложений из очереди на сервер

        - Когда загрузка вложений завершена:
        - Удалить вложение из очереди на загрузку на сервер
        - Обновить в объекте сообщения информацию о онлайн загруженных вложениях
        - Обновить в файле базы данных информацию о том что вложения загружены
        - внести сообщение в очередь для отправки (при этом заменить в тексте сообщения константы на данные пользователя)
        - Инициировать отправку сообщений из очереди на сервер

        - Когда сообщение отправлено:
        - Удалить сообщение из очереди отправки на сервер
        - обновить счётчик отправленных сообщений в аккаунте
        - Обновить в объекте ответа из базы информацию о количестве использований
        - Обновить в файле базы данных информацию о количестве использований
        * */

        // Вывести логи, обновить счётчики
        log(". ПОЛУЧЕНО СООБЩЕНИЕ: " + message);
        incrementMessagesReceivedCounter();
        log("\n.");
        log("\nТы: " + message.getFrom());
        log("\nТы написал: " + message.getText());
        log("\nПодпись: " + message.getCaption());
        log("\nПринято сообщений: " + messagesReceivedCounter);
        log("\nОтправлено сообщений: " + messagesSentCounter);
        log("\nВыполнено запросов к API: " + tgAccount.getApiCounter());
        log("\nОшибок при доступе к API: " + tgAccount.getErrorCounter());

        if (message.getChat().getId() != message.getFrom().getId()){
            if(!message.getText()
                    .toLowerCase(Locale.ROOT)
                    .replace(",", " ")
                    .replaceAll(" +", " ")
                    .trim()
                    .startsWith("бот ")){
                log("Получено сообщение с чата " + message.getChat().getTitle() + ", " +
                        "и в нём нет обращения. " +
                        "Игнорируем.");
                return;
            }
        }

        //сформировать обьект вопроса
        //Этот код потенциально можно засунуть как один из конструкторов Message
        com.fsoft.ihabot.answer.Message question = new com.fsoft.ihabot.answer.Message();
        question.setMessage_id(message.getMessage_id());
        question.setText(message.getText());
        if(question.isEmpty())
            question.setText(message.getCaption());
        question.setAuthor(message.getFrom());
        if(message.getForward_from() != null)
            question.setForwardedFrom(message.getForward_from());
        question.setSourceDialog();
        question.setDate(message.getDate());
        { //в сообщениие добавить фото
            String photoId = message.getPhotoId();
            if (photoId != null && !photoId.isEmpty()){
                Attachment attachment = new Attachment().setPhoto();
                attachment.updateTgFile_id(tgAccount.getId(), photoId);
                attachment.setSize(message.getPhotoFileSize());
                question.addAttachment(attachment);
            }
        }
        { //в сообщениие добавить стикер
            if(message.getSticker() != null) {
                String photoId = message.getSticker().getFile_id();
                if (photoId != null && !photoId.isEmpty() && !message.getSticker().isIs_animated() && !message.getSticker().isIs_video()) {
                    Attachment attachment = new Attachment().setPhoto();
                    attachment.updateTgFile_id(tgAccount.getId(), photoId);
                    attachment.setSize(message.getSticker().getFile_size());
                    question.addAttachment(attachment);
                }
            }
        }
        { //в сообщениие добавить файл
            if(message.getDocument() != null) {
                String file_id = message.getDocument().getFile_id();
                if (file_id != null && !file_id.isEmpty()) {
                    Attachment attachment = new Attachment().setDoc();
                    attachment.updateTgFile_id(tgAccount.getId(), file_id);
                    attachment.setSize(message.getDocument().getFile_size());
                    attachment.setReceivedFilename(message.getDocument().getFile_name());
                    question.addAttachment(attachment);
                }
            }
        }

        //учесть сообщение в статистике
        applicationManager.getMessageHistory().registerTelegramMessage(message.getChat(), question, tgAccount);

        //посчитать как много юзер пишет и если что, его игнорить
        int messagesLastMinute = applicationManager.getMessageHistory().countMessagesLastMinute(question.getAuthor());
        log("За послежнюю минуту этот пользователь писал: " + messagesLastMinute + " раз. \n");
        if(messagesLastMinute > 15){
            log("Такое количество сообщений расценено как спам. Игнорируем. \n");
            applicationManager.getMessageHistory().registerTelegramSpam(message.getChat(), question, tgAccount);
            return;
        }


        //Отправить пользователю что бот уже начал готовить ответ для него
        tgAccount.sendChatTyping(message.getChat().getId());



        //Проверить не является ли пользователь админинистратором. Если является, обработать как команду
        AdminList.AdminListItem admin = applicationManager.getAdminList().get(question.getAuthor());
        if(admin != null){
            try {
                ArrayList<com.fsoft.ihabot.answer.Message> results = applicationManager.processCommand(question, tgAccount, admin);
                /*
                Модули команд получают ВСЕ сообщения если пользователь администратор, и если хотят,
                присылают от себя ответы.

                Если по результатам обработки команды есть ответы от модулей команд,
                * отправить ответы в виде отдельных сообщений и не пустить сообщение на обработку базой.
                *
                * Если есть ответы и они пустые - не отправлять ответы и не пустить
                * сообщение на обработку базой.
                *
                * Если ответов нет, пустить сообщение на обработку базой.*/

                for (com.fsoft.ihabot.answer.Message item:results) {
                    if (!item.isEmpty()) {
                        item.setReplyToMessage(question);
                        sendAnswer(message.getChat().getId(), item);
                    }
                }
                if(!results.isEmpty())
                    return;
            }
            catch (Exception e){
                e.printStackTrace();
                sendAnswer(
                        message.getChat().getId(),
                        "Ответ на команду <b>" + question.getText() + "</b>\n\n" +
                                "Ошибка обработки команды: "+e.getLocalizedMessage()
                );
                return;
            }
        }

        //Подобрать ответ из базы и отправить
        try {
            AnswerElement answerElement = applicationManager.getAnswerDatabase().pickAnswer(question);
            com.fsoft.ihabot.answer.Message answer = answerElement.getAnswerMessage();
            answer.setReplyToMessage(question);
            sendAnswer(message.getChat().getId(), answer);
        }
        catch (Exception e){
            e.printStackTrace();
            sendAnswer(
                    message.getChat().getId(),
                    e.getLocalizedMessage()
            );
        }





        //заполняем юзера
//        com.fsoft.vktest.Utils.User brainUser = new User();
//        brainUser.setName(message.getFrom().getName());
//        brainUser.setNetwork(User.NETWORK_TELEGRAM);
//        brainUser.setId(message.getFrom().getId());

        //функция отправки ответа юзеру
        //com.fsoft.vktest.AnswerInfrastructure.Message.OnAnswerReady onAnswerReady;

//        onAnswerReady = new com.fsoft.vktest.AnswerInfrastructure.Message.OnAnswerReady() {
//            @Override
//            public void sendAnswer(com.fsoft.vktest.AnswerInfrastructure.Message answer) {
//                if(!answer.hasAnswer())
//                    return;


                //int attachmentsSent = 0;

//                final TgAccountCore.SendMessageListener listener = new TgAccountCore.SendMessageListener() {
//                    @Override
//                    public void sentMessage(Message message) {
//                        log(". Отправлено сообщение: " + message);
//                        inctementMessagesSentCounter();
//                    }
//
//                    @Override
//                    public void error(Throwable error) {
//                        log(error.getClass().getName() + " while sending message");
//                    }
//                };

                //если в сообщении есть вложения, отправим их
//                for(Attachment attachment:answer.getAnswer().attachments){
//                    try {
//                        java.io.File file = attachment.getFile();
//                        if(attachment.isPhoto()) {
//                            if(attachment.hasTgCache())
//                                tgAccount.sendPhoto(listener, message.getChat().getId(), replyText, attachment.getTgCache().getId());
//                            else
//                                tgAccount.sendPhoto(listener, message.getChat().getId(), replyText, file);
//                            attachmentsSent++;
//                        }
//                        if(attachment.isDoc()) {
//                            if(attachment.hasTgCache())
//                                tgAccount.sendDocument(listener, message.getChat().getId(), replyText, attachment.getTgCache().getId());
//                            else
//                                tgAccount.sendDocument(listener, message.getChat().getId(), replyText, file);
//                            attachmentsSent++;
//                        }
//                        if(attachment.isAudio()) {
//                            if(attachment.hasTgCache())
//                                tgAccount.sendAudio(listener, message.getChat().getId(), replyText, attachment.getTgCache().getId());
//                            else
//                                tgAccount.sendAudio(listener, message.getChat().getId(), replyText, file);
//                            attachmentsSent++;
//                        }
//                    }
//                    catch (Exception e){
//                        e.printStackTrace();
//                        log("! Ошибка получения файла вложения: " + e.getMessage());
//                    }
//                }


            //}
        };

    public void sendAnswer(long chatId, String answer){
        sendAnswer(chatId, new com.fsoft.ihabot.answer.Message(answer));
    }
    public void sendAnswer(long chatId, com.fsoft.ihabot.answer.Message answer){
        if(answer.getAttachments().isEmpty()) {
            tgAccount.sendMessage(new TgAccountCore.SendMessageListener() {
                @Override
                public void sentMessage(Message message) {
                    log(". Отправлено сообщение: " + message);
                    incrementMessagesSentCounter();
                }

                @Override
                public void error(Throwable error) {
                    log(error.getClass().getName() + " while sending message");
                }
            }, chatId, answer);
        }
        else {
            for (Attachment attachment : answer.getAttachments()) {
                //send photo
                if(attachment.isPhoto()){
                    long reply_to_message_id_n = 0;
                    if(answer.getReplyToMessage() != null)
                        reply_to_message_id_n = answer.getReplyToMessage().getMessage_id();
                    final long reply_to_message_id = reply_to_message_id_n;
                    if(attachment.isOnlineTg(tgAccount.getId())){
                        String fileId = attachment.getTgFileID(tgAccount.getId());
                        tgAccount.sendPhoto(new TgAccountCore.SendMessageListener() {
                            @Override
                            public void sentMessage(Message message) {
                                log("Отправлено фото по FileID: " + message.toString());
                                incrementMessagesSentCounter();
                            }

                            @Override
                            public void error(Throwable error) {
                                log(error.getClass().getName() + " while sending photo by ID");
                                if(attachment.isLocal()) {
                                    log("Rejecting photo ID...");
                                    File file = new File(applicationManager.getAnswerDatabase().getFolderAttachments(), attachment.getAttachmentsFilename());
                                    try {
                                        applicationManager.getAnswerDatabase().updateAnswerAttachmentFileId(file.getName(), null, tgAccount.getId());
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        log("Не могу записать в базу айдишник фотографии: " + e.getLocalizedMessage());
                                    }

                                    log("Send photo again as file...");
                                    tgAccount.sendPhoto(new TgAccountCore.SendMessageListener() {
                                        @Override
                                        public void sentMessage(Message message) {
                                            log("Отправлено фото: " + message.toString());
                                            incrementMessagesSentCounter();
                                        }

                                        @Override
                                        public void error(Throwable error) {
                                            log(error.getClass().getName() + " while sending photo");
                                        }
                                    }, chatId, answer.getText(), file, reply_to_message_id);
                                }
                            }
                        }, chatId, answer.getText(), fileId, reply_to_message_id);
                    }
                    else if(attachment.isLocal()) {
                        File file = new File(applicationManager.getAnswerDatabase().getFolderAttachments(), attachment.getAttachmentsFilename());
                        tgAccount.sendPhoto(new TgAccountCore.SendMessageListener() {
                            @Override
                            public void sentMessage(Message message) {
                                log("Отправлено фото: " + message.toString());
                                String photoId = message.getPhotoId();
                                if (photoId != null) {
                                    log("ID загруженной фотографии: " + photoId);
                                    try {
                                        applicationManager.getAnswerDatabase().updateAnswerAttachmentFileId(file.getName(), photoId, tgAccount.getId());
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        log("Не могу записать в базу айдишник фотографии: " + e.getLocalizedMessage());
                                    }
                                }
                                incrementMessagesSentCounter();
                            }

                            @Override
                            public void error(Throwable error) {
                                log(error.getClass().getName() + " while sending photo");
                            }
                        }, chatId, answer.getText(), file, reply_to_message_id);
                    }
                }


                //send document, from attachment or from file
                if(attachment.isDoc() && attachment.isLocal()){
                    File file = attachment.getFileToUpload();
                    if(file == null)
                        file = new File(applicationManager.getAnswerDatabase().getFolderAttachments(), attachment.getAttachmentsFilename());
                    long reply_to_message_id = 0;
                    if(answer.getReplyToMessage() != null)
                        reply_to_message_id = answer.getReplyToMessage().getMessage_id();
                    tgAccount.sendDocument(new TgAccountCore.SendMessageListener() {
                        @Override
                        public void sentMessage(Message message) {
                            log("Отправлено документ: " + message.toString());
                            String fileId = message.getPhotoId();
                            if(fileId != null){
                                log("ID загруженного документа: " + fileId);
                                try {
                                    if(!attachment.getAttachmentsFilename().isEmpty())
                                        applicationManager.getAnswerDatabase().updateAnswerAttachmentFileId(attachment.getAttachmentsFilename(), fileId, tgAccount.getId());
                                }
                                catch (Exception e){
                                    e.printStackTrace();
                                    log("Не могу записать в базу айдишник документа: " + e.getLocalizedMessage());
                                }
                            }
                            incrementMessagesSentCounter();
                        }

                        @Override
                        public void error(Throwable error) {
                            error.printStackTrace();
                            log(error.getLocalizedMessage() + " while sending document");
                        }
                    }, chatId, answer.getText(), file, reply_to_message_id);
                }


            }
        }
    }

    private boolean isRunning(){
        return tgAccount.isRunning();
    }
        //формирует объект и вызываем систему
        //com.fsoft.vktest.AnswerInfrastructure.Message brainMessage;
//        brainMessage = new com.fsoft.vktest.AnswerInfrastructure.Message(
//                MessageBase.SOURCE_CHAT,
//                message.getText(),
//                brainUser,
//                tgAccount,
//                onAnswerReady
//        );
//        brainMessage.setMessage_id(message.getMessage_id());
//        brainMessage.setChat_id(message.getChat().getId());
//        brainMessage.setChat_title(message.getChat().getTitle());
//        if(message.getReply_to_message() != null && message.getReply_to_message().getFrom() != null) {
//            User mentioned = new User().tg(message.getReply_to_message().getFrom().getId());
//            mentioned.setUsername(message.getReply_to_message().getFrom().getUsername());
//            mentioned.setName(message.getReply_to_message().getFrom().getName());
//            brainMessage.getMentions().add(mentioned);
//        }

        //applicationManager.getBrain().processMessage(brainMessage);
    //}
}
