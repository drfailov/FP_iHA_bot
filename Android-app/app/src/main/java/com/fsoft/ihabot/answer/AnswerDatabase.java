package com.fsoft.ihabot.answer;

import android.content.res.Resources;
import android.util.Pair;

import com.fsoft.ihabot.R;
import com.fsoft.ihabot.Utils.ApplicationManager;
import com.fsoft.ihabot.Utils.CommandDesc;
import com.fsoft.ihabot.Utils.CommandModule;
import com.fsoft.ihabot.Utils.F;
import com.fsoft.ihabot.Utils.Triplet;
import com.fsoft.ihabot.communucation.tg.TgAccount;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.Zip4jConfig;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;

/**
 * ----=== Полный пиздец. ===----
 * Приоритеты:
 * - много ответов
 * - большая скорость
 * - много инфы про ответ
 * - мало памяти
 *
 *
 * * Какой формат должна иметь база:
 *  *    Каждая строка - это JSON. Каждый JSON обязательно содержит уникальный ID.
 *  *    Все ответы должны быть строго в порядке возростания ID. Пропуски ID допускаются. (нужно для контроля уникальности ID в базе)
 *  *      Если последовательность будет нарушена, программа выполнит фильтрацию.
 *  *      Фильтрация длится относительно долго. Дольше обычной загрузки программы.
 *  *      Фильтрация происходит в несколько этапов.
 *  *      Фильтрация не требует дополнительных затрат оперативной памяти.
 *  *      Фильтрация заменит ID для всех ответов после места нарушения.
 *  *      Фильтрация также очистит дубликаты ответов.
 *  *    Если строка имеет не JSON формат - эта строка будет пропущена
 *  *      при загрузке и удалена из базы в момент любого изменения или фильтрации базы.
 *  *      При загрузке программы пользователь получит отчёт, если в базе будут выявлены нарущения.
 *  *    Все ответы и вопросы сохраняются в базе в их изначальном виде, без искажений
 *  *    Если в базе есть два одинаковых ответа - они будут уничтожены при фильтрации.
 *
 *
 *
 * */
public class AnswerDatabase  extends CommandModule {
    private static final int defaultDatabaseResourceZip = R.raw.answer_database;
    private final ApplicationManager applicationManager;
    private final JaroWinkler jaroWinkler;
    private final Synonyme synonyme;
    private final File folderAnswerDatabase;
    private final File fileAnswers;
    private final File folderAttachments;


    public AnswerDatabase(ApplicationManager applicationManager)  throws Exception {
        this.applicationManager = applicationManager;
        folderAnswerDatabase = new File(applicationManager.getHomeFolder(), "AnswerDatabase");
        if(!folderAnswerDatabase.isDirectory())
            log("Папки для AnswerDatabase нет. Создание папки: " + folderAnswerDatabase.mkdirs());
        folderAttachments = new File(folderAnswerDatabase, "attachments");
        fileAnswers = new File(folderAnswerDatabase, "answer_database.txt");
        jaroWinkler = new JaroWinkler();
        synonyme = new Synonyme(this);


        if(!fileAnswers.isFile()){
            log("Файла базы нет. Загрузка файла answer_database.zip из ресурсов...");
            loadDefaultDatabase();
        }

        childCommands.add(new DumpCommand());
        childCommands.add(new RememberCommand());
        childCommands.add(new GetAnswersByIdCommand());
        childCommands.add(new GetAnswerByIdCommand());
        childCommands.add(new RemoveAnswerByIdCommand());
        childCommands.add(new GetAnswersByQuestionCommand());
    }


    /**
     * Подбирает ответ на вопрос исходя из базы вопросв. Эта обращается к фукнуии pickAnswers
     * @param question Входящее сообщение типа Message в исходном виде, без никакиз преобразований
     * @return AnswerElement из базы который описывает элемент базы ответов который подходит под этот вопрос
     * @author Dr. Failov
     * @throws Exception Поскольку производится сложная работа с файлом, случиться может что угодно
     */
    public AnswerElement pickAnswer(Message question) throws Exception{
        MessageRating messageRating = pickAnswers(question);


        log("-----------------------------------------------------------");
        log("Вопрос: " + question.getText());
        log("Варианты ответов:");
        for(int i = 0; i<messageRating.getCapacity(); i++){
            double rating = messageRating.getTopRating(i);
            AnswerElement answerElement = messageRating.getTopMessage(i);
            if(answerElement != null) {
                log(String.format(Locale.US, "%d %.2f: %s", i, rating, answerElement));
            }
        }
        log("-----------------------------------------------------------");

        if(messageRating.isEmpty() || messageRating.getTopRating() < 0.40)
            throw new Exception("Нормального ответа найти не получилось");

        ArrayList<AnswerElement> answers = messageRating.getTopMessages();
        AnswerElement answerElement = answers.get(new Random().nextInt(answers.size()));
        updateAnswerUsedTimes(answerElement.getId());
        return answerElement;
    }

    //filename, fileId, botId
    ArrayList<Triplet<String, String, Long>> updateAnswerPhotoIdQueue = new ArrayList<>();
    /**
     * В очередь добавляется информация о том что файлу был присвоен ID конкретным ботом.
     * Когда в очереди накапливается некоторое количество элемнетов, очередь записывается в файл
     * Производится полный проход по базе, по всем вложениям. И везде где в ответе во вложении используется файл filename,
     * вписывается fileId для дальнейшего использования при отправке.
     * @param filename Имя файла из папки вложений, который был отправлен
     * @param fileId ID файла на сервере телеграм, который следует внести в базу
     * @author Dr. Failov
     * @throws Exception Поскольку производится сложная работа с файлом, случиться может что угодно
     */
    public void updateAnswerAttachmentFileId(String filename, String fileId, long botId) throws Exception{
        if(filename == null || filename.isEmpty())
            throw new Exception(log("Проблема внесения в базу ID фотографии: filename = null или пустой!"));
        if(fileId == null || fileId.isEmpty())
            throw new Exception(log("Проблема внесения в базу ID фотографии: fileId = null или пустой!"));
        log("Вношу в очередь на прикрепление в базу к файлу " + filename + " айдишник " + fileId + " ...");
        for(Triplet<String, String, Long> pair:updateAnswerPhotoIdQueue)
            if(pair.getFirst().equals(filename) && pair.getThird() == botId)
                throw new Exception(log("Попытка внести в очередь несколько ID для файла " + filename));
        updateAnswerPhotoIdQueue.add(new Triplet<>(filename, fileId, botId));
        log("IDшников в очереди: " + updateAnswerPhotoIdQueue.size());

        if(updateAnswerPhotoIdQueue.size() >= 5){
            log("Накопилось достаточно элементов в очереди чтобы внести данные в базу...");

            File fileTmp = new File(applicationManager.getTempFolder(), "Answer_database.tmp");
            PrintWriter fileTmpWriter = new PrintWriter(fileTmp);
            BufferedReader bufferedReader = new BufferedReader(new FileReader(fileAnswers));
            String line;
            int lineNumber = 0;
            int errors = 0;
            int changed = 0;
            synchronized (fileAnswers) {
                try {
                    while ((line = bufferedReader.readLine()) != null) {
                        lineNumber++;
                        if (lineNumber % 984 == 0)
                            log("Прикрепление FileID в базе (" + lineNumber + " уже пройдено)");
                        try {
                            JSONObject jsonObject = new JSONObject(line);
                            AnswerElement answerElement = new AnswerElement(jsonObject);
                            if (answerElement.hasAnswer()){
                                for (Triplet<String, String, Long> pair:updateAnswerPhotoIdQueue) {
                                    if(answerElement.getAnswerMessage().hasAttachmentFilename(pair.getFirst())){
                                        answerElement.getAnswerMessage().addAttachmentFileID(pair.getFirst(), pair.getSecond(), pair.getThird());
                                        changed++;
                                        log(answerElement.toJson().toString());
                                    }
                                }
                            }
                            fileTmpWriter.println(answerElement.toJson().toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                            errors++;
                            log("! Ошибка разбора строки " + lineNumber + " как ответа из базы.\n" + e.getMessage());
                        }
                    }
                }
                finally {
                    bufferedReader.close();
                    fileTmpWriter.close();
                }
                if (errors != 0)
                    log("При внесении FileID в базу возникло ошибок: " + errors + ".");
                log("В базе изменено " + changed + " FileID.");
                File databaseTmpStorage = new File(applicationManager.getTempFolder(), "Answer_database_"+new Date().getTime()+".tmp");
                log("Убираем старую базу: " + fileAnswers.renameTo(databaseTmpStorage));
                boolean databaseReplaced = fileTmp.renameTo(fileAnswers);
                log("Замена старой базы новой: " + databaseReplaced);
                if(!databaseReplaced){
                    log("!!! Возникла проблема с заменой базы данных! Восстановление резервной копии: " + databaseTmpStorage.getName());
                    log("Замена базы из файла резервной кории: " + databaseTmpStorage.renameTo(fileAnswers));
                }
            }
            updateAnswerPhotoIdQueue.clear();
            log("Готово, FileID успешно внесены в базу. Очередь сброшена.");
        }
    }

    /**
     * @return ApplicationManager который хранится в этом модуле.
     * Используется например для передачи его дочерним модулям.
     */
    public ApplicationManager getApplicationManager() {
        return applicationManager;
    }

    /**
     * Выдает папку где хранятся вложения к ответам в базе
     * Чтобы, к примеру, коммуникатор знал откуда брать файлы для звгрузки
     * @return File который папке где лежат аттачи
     */
    public File getFolderAttachments() {
        return folderAttachments;
    }

    /**
     * Выдает папку где хранится всё что связано с базой ответов. Ответы, аттачи, синонимы
     * Чтобы модули знали где брать файлы, к примеру провайдер синонимов.
     * @return File который папка где лежит база
     */
    public File getFolderAnswerDatabase() {
        return folderAnswerDatabase;
    }



    ArrayList<Long> updateAnswerUsedTimesQueue = new ArrayList<>();

    /**
     * Вносит в базу информацию о том, что какой-то ответ был использован при подборе ответа.
     * Для статистики.
     * Внесение в файл происходит раз в некоторое количество ответв, чтобы не дёргать файл по каждой мелочи.
     *
     * @param answerID ID ответа в который надо внести информацию о статистике использования
     * @author Dr. Failov
     * @throws Exception Поскольку производится сложная работа с файлом, случиться может что угодно
     */
    public void updateAnswerUsedTimes(long answerID) throws Exception{
        updateAnswerUsedTimesQueue.add(answerID);
        log("Ответ " + answerID + " добавлен в очередь("+updateAnswerUsedTimesQueue.size()+") для обновления количества его использований...");
        if(updateAnswerUsedTimesQueue.size() > 2){
            //записать в файл инфу
            log("Внесение в файл базы ответов информации о количестве использований ответов " + F.text(updateAnswerUsedTimesQueue) + "...");
            File fileTmp = new File(applicationManager.getTempFolder(), "Answer_database.tmp");
            PrintWriter fileTmpWriter = new PrintWriter(fileTmp);
            BufferedReader bufferedReader = new BufferedReader(new FileReader(fileAnswers));
            String line;
            int lineNumber = 0;
            int errors = 0;
            int changed = 0;
            synchronized (fileAnswers) {
                try {
                    while ((line = bufferedReader.readLine()) != null) {
                        lineNumber++;
                        if (lineNumber % 1884 == 0)
                            log("Внесение "+F.text(updateAnswerUsedTimesQueue)+" использований ответов в базу (" + lineNumber + " уже пройдено)");
                        try {
                            JSONObject jsonObject = new JSONObject(line);
                            AnswerElement answerElement = new AnswerElement(jsonObject);
                            boolean isChanged = false;
                            for(Iterator<Long> i = updateAnswerUsedTimesQueue.iterator(); i.hasNext();) {
                                Long current = i.next();
                                if(current.equals(answerElement.getId())) {
                                    isChanged = true;
                                    answerElement.incrementTimesUsed();
                                    i.remove();
                                }
                            }
                            if(isChanged)
                                changed ++;
                            fileTmpWriter.println(answerElement.toJson().toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                            errors++;
                            log("Ошибка разбора строки " + lineNumber + " как ответа из базы.\n" + e.getMessage());
                        }
                    }
                }
                finally {
                    bufferedReader.close();
                    fileTmpWriter.close();
                }
                if (errors != 0)
                    log("При внесении в базу информации о количестве использований ответов возникло ошибок: " + errors + ".");
                log("В файл базы ответов внесено " + changed + " изменений.");
                File databaseTmpStorage = new File(applicationManager.getTempFolder(), "Answer_database_"+new Date().getTime()+".tmp");
                log("Убираем старую базу: " + fileAnswers.renameTo(databaseTmpStorage));
                boolean databaseReplaced = fileTmp.renameTo(fileAnswers);
                log("Замена старой базы новой: " + databaseReplaced);
                if(!databaseReplaced){
                    log("!!! Возникла проблема с заменой базы данных! Восстановление резервной копии: " + databaseTmpStorage.getName());
                    log("Замена базы из файла резервной кории: " + databaseTmpStorage.renameTo(fileAnswers));
                }
            }
            updateAnswerUsedTimesQueue.clear();
        }
    }

    /**
     * Подбирает ответ на вопрос исходя из базы вопросв. Эта функция просматривает файл полностью в поисках ответа.
     * @param question Входящее сообщение типа Message в исходном виде, без никакиз преобразований
     * @return MessageRating описывающий что подходит в качестве ответа
     * @author Dr. Failov
     * @throws Exception Поскольку производится сложная работа с файлом, случиться может что угодно
     */
    private MessageRating pickAnswers(Message question) throws Exception{
        if(question.getText().split(" +").length > 7)
            throw new Exception("Я на сообщение с таким количеством слов не смогу подобрать ответ.");
        if(question.getText().length() > 40)
            throw new Exception("Я на такое длинное сообщение не смогу подобрать ответ.");

        MessageRating messageRating = new MessageRating(50);

        String line;
        int lineNumber = 0;
        int errors = 0;
        synchronized (fileAnswers) {
            try(BufferedReader bufferedReader = new BufferedReader(new FileReader(fileAnswers))) {
                while ((line = bufferedReader.readLine()) != null) {
                    if (lineNumber % 1289 == 0)
                        log(". Поиск ответа в базе (" + lineNumber + " уже проверено) ...");
                    try {
                        JSONObject jsonObject = new JSONObject(line);
                        AnswerElement currentAnswerElement = new AnswerElement(jsonObject);
                        String currentQuestion = currentAnswerElement.getQuestionMessage().getText();
                        String neededQuestion = question.getText();

                        double similarity = compareMessages(neededQuestion, currentQuestion);

                        messageRating.addAnswer(currentAnswerElement, similarity);
                    } catch (Exception e) {
                        e.printStackTrace();
                        errors++;
                        log("! Ошибка разбора строки " + lineNumber + " как ответа из базы.\n" + e.getMessage());
                    }
                    lineNumber++;
                }
            }
        }
        if (errors != 0)
            log("! При загрузке базы ответов возникло ошибок: " + errors + ".");
        System.gc();

        return messageRating;

    }

    /**
     * Сохранить два сообщения в базу, вопрос-ответ.
     * Если в ответе присутствуют вложения, которые ссылаются на файлы на серверах телеграм, функция попытается их
     * скачать используя tgAccount. При этом это должен быть тот аккаунт, который получил те FileID которые во вложениях.
     * (FileID действителен только для одного аккаунта)
     * Если tgAccount не передать, загрузка с сервера не будет произведена и файлы должны уже присутствовать в папке attachments
     * @author Dr. Failov
     * @throws Exception Поскольку производится сложная работа с файлом, случиться может что угодно
     */
    private AnswerElement addAnswerToDatabase(Message question, Message answer, TgAccount tgAccount) throws Exception{
        //сохранить все вложения в ответе локально в папку Attachments
        if(tgAccount != null) {
            answer = saveAttachmentsToLocal(answer, tgAccount);
        }
        //предотвратить добавление в базу не локальных вложений
        for(Attachment attachment : answer.getAttachments()) {
            if (!attachment.isLocalInAttachmentFolder(applicationManager))
                throw new Exception("В сообщении-ответе не все вложения в сохранены локально в папку attachments. Я не могу добавить в базу вложения без локальных файлов.");
        }

        if(question.getText().isEmpty())
            throw new Exception("Поскольку бот не умеет учитывать в ответе вложения, вопросы без текста не допускаются.");
        if(question.getText().length() > 80)
            throw new Exception("Бот крайне плохо отвечает на длинные сообщения, поэтому нет смысла их добавлять в базу в качестве вопроса.");
        if(answer.getText().isEmpty() && answer.getAttachments().isEmpty())
            throw new Exception("Нельзя добавлять в базу ответ, если в ответе не содержится ни вложений ни текста.");
        AnswerElement answerElement = new AnswerElement();
        answerElement.setAnswerMessage(answer);
        answerElement.setQuestionMessage(question);
        log("Добавляю в базу ответ: " + answerElement);

        log("Прошерстим базу для подбора максимального ID для текущего ответа...");
        {
            try {
                String line;
                int lineNumber = 0;
                synchronized (fileAnswers) {
                    try(BufferedReader bufferedReader = new BufferedReader(new FileReader(fileAnswers))) {
                        while ((line = bufferedReader.readLine()) != null) {
                            if (lineNumber % 3289 == 0)
                                log("Шерстим базу для подбора ID... (" + lineNumber + " уже проверено) ...");
                            try {
                                JSONObject jsonObject = new JSONObject(line);
                                AnswerElement currentAnswerElement = new AnswerElement(jsonObject);
                                answerElement.setIdBiggerThan(currentAnswerElement.getId());
                            } catch (Exception e) {
                                e.printStackTrace();
                                log("! Ошибка разбора строки " + lineNumber + " как ответа из базы.\n" + e.getMessage());
                            }
                            lineNumber++;
                        }
                    }
                }
            }
            catch (Exception e){
                e.printStackTrace();
                throw new Exception("Ошибка прочтения базы данных для подбора ID нового ответа: " + e.getLocalizedMessage());
            }
            System.gc();
        }

        log("Вписываем новый ответ в базу...");
        {
            synchronized (fileAnswers) {
                try (FileWriter fw = new FileWriter(fileAnswers, true);
                     BufferedWriter bw = new BufferedWriter(fw);
                     PrintWriter out = new PrintWriter(bw)) {
                    out.println(answerElement.toJson());
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new Exception("Все этапы перед этим прошли нормально, но не могу дописать ответ в файл базы ответов.");
                }
            }
        }


        return answerElement;
    }

    /**
     * Получить из базы заданное количество ответов, которые находятся после некоторого ID включительно.
     * Если конкретного ID в базе не будет, будут отправлены ответы которые следуют за ним.
     * Если в заданном диапазоне ответов будут пропуски ID, в выборку будут отобраны следующие
     * за ним ответы, будет отсчитано нужное количество.
     * @param startingId Идентификатор ответа, начиная с коротого начинаем собирать ответы в массив
     * @param count Количество, которое надо собрать в массив
     * @return Массив, содержащий ответы в заданном диапазоне значений
     * @author Dr. Failov
     * @throws Exception Поскольку производится сложная работа с файлом, случиться может что угодно
     */
    private ArrayList<AnswerElement> getAnswers(long startingId, long count) throws Exception{
        log("Поиск в базе "+count+" ответов после ID"+startingId+"...");
        ArrayList<AnswerElement> result = new ArrayList<>();
        {
            try {
                String line;
                int lineNumber = 0;
                synchronized (fileAnswers) {
                    try(BufferedReader bufferedReader = new BufferedReader(new FileReader(fileAnswers))) {
                        while ((line = bufferedReader.readLine()) != null) {
                            if (lineNumber % 3289 == 0)
                                log("Ищу в базе нужные IDшники... (" + lineNumber + " уже проверено) ...");
                            try {
                                JSONObject jsonObject = new JSONObject(line);
                                AnswerElement currentAnswerElement = new AnswerElement(jsonObject);
                                if (currentAnswerElement.getId() >= startingId && result.size() < count)
                                    result.add(currentAnswerElement);
                                if(result.size() >= count) {
                                    log("Готово (успешно собрано " + result.size() + " ответов) ...");
                                    break;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                log("! Ошибка разбора строки " + lineNumber + " как ответа из базы.\n" + e.getMessage());
                            }
                            lineNumber++;
                        }
                    }
                }
            }
            catch (Exception e){
                e.printStackTrace();
                throw new Exception("Ошибка прочтения базы данных: " + e.getLocalizedMessage());
            }
            System.gc();
        }
        return result;
    }

    /**
     * Производится полный проход по базе, и удаление ответа с конкретным ID из базы.
     * База при этом копируется во временную папку, а оттуда перезаписывает текущую
     * @param answersID список ID которые надо удалить
     * @return количество записей, которые были удалены
     * @author Dr. Failov
     * @throws Exception Поскольку производится сложная работа с файлом, случиться может что угодно
     */
    private int removeAnswersFromDatabase(ArrayList<Long> answersID) throws Exception{
        log("Удаляю из базы ответов ответы с ID "+F.text(answersID)+"...");
        File fileTmp = new File(applicationManager.getTempFolder(), "Answer_database.tmp");
        PrintWriter fileTmpWriter = new PrintWriter(fileTmp);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileAnswers));
        String line;
        int lineNumber = 0;
        int errors = 0;
        int changed = 0;
        synchronized (fileAnswers) {
            try {
                while ((line = bufferedReader.readLine()) != null) {
                    lineNumber++;
                    if (lineNumber % 1484 == 0)
                        log("Удаление ответов с ID "+F.text(answersID)+" в базе (" + lineNumber + " уже пройдено)");
                    try {
                        JSONObject jsonObject = new JSONObject(line);
                        AnswerElement answerElement = new AnswerElement(jsonObject);
                        if(answersID.contains(answerElement.getId())){
                            changed ++;
                        }
                        else {
                            fileTmpWriter.println(answerElement.toJson().toString());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        errors++;
                        log("Ошибка разбора строки " + lineNumber + " как ответа из базы.\n" + e.getMessage());
                    }
                }
            }
            finally {
                bufferedReader.close();
                fileTmpWriter.close();
            }
            if (errors != 0)
                log("При удалении ответа по ID из базы возникло ошибок: " + errors + ".");
            log("В базе удалено " + changed + " ответов.");
            File databaseTmpStorage = new File(applicationManager.getTempFolder(), "Answer_database_"+new Date().getTime()+".tmp");
            log("Убираем старую базу: " + fileAnswers.renameTo(databaseTmpStorage));
            boolean databaseReplaced = fileTmp.renameTo(fileAnswers);
            log("Замена старой базы новой: " + databaseReplaced);
            if(!databaseReplaced){
                log("!!! Возникла проблема с заменой базы данных! Восстановление резервной копии: " + databaseTmpStorage.getName());
                log("Замена базы из файла резервной кории: " + databaseTmpStorage.renameTo(fileAnswers));
            }
        }
        return changed;
    }

    /**
     * Выполняется поиск неиспользуемых вложений, с просмотром всей базы.
     * Если находятся неиспользованные вложения, они удаляются из папки.
     * @return количество файлов которые были удалены.
     * @author Dr. Failov
     * @throws Exception Поскольку производится сложная работа с файлом, случиться может что угодно
     */
    private int cleanUnusedAttachments() throws Exception{
        log("Удаляю из базы неиспользуемые вложения...");
        if(folderAttachments == null)
            throw new Exception("Невозможно выполнить очистку несипользуемых вложений, потому что папка с вложениями не задана.");
        if(!folderAttachments.isDirectory())
            throw new Exception("Невозможно выполнить очистку несипользуемых вложений, потому что папка с вложениями не создана.");
        File[] attachmentsToDeleteArray = folderAttachments.listFiles();
        if(attachmentsToDeleteArray == null)
            throw new Exception("Невозможно выполнить очистку несипользуемых вложений, потому что папка с вложениями пуста, или к ней нет доступа.");
        ArrayList<File> attachmentsToDelete = new ArrayList<>(Arrays.asList(attachmentsToDeleteArray));
        log("Всего вложений до удаления: " + attachmentsToDelete.size());
        {
            try {
                String line;
                int lineNumber = 0;
                synchronized (fileAnswers) {
                    try(BufferedReader bufferedReader = new BufferedReader(new FileReader(fileAnswers))) {
                        while ((line = bufferedReader.readLine()) != null) {
                            if (lineNumber % 1289 == 0)
                                log("Шерстим базу для анализа используемых вложений... (" + lineNumber + " уже проверено) ...");
                            try {
                                JSONObject jsonObject = new JSONObject(line);
                                AnswerElement currentAnswerElement = new AnswerElement(jsonObject);
                                if(currentAnswerElement.hasAnswer()){
                                    for(Attachment attachment:currentAnswerElement.getAnswerMessage().getAttachments()){
                                        String filename = attachment.getFilename();
                                        if(filename != null && !filename.isEmpty()){
                                            //тут происходит работа с каждым файлом каждого вложения каждого ответа из базы
                                            attachmentsToDelete.removeIf(file -> file.getName().equals(filename));
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                log("! Ошибка разбора строки " + lineNumber + " как ответа из базы.\n" + e.getMessage());
                            }
                            lineNumber++;
                        }
                    }
                }
            }
            catch (Exception e){
                e.printStackTrace();
                throw new Exception("Ошибка прочтения базы данных для анализа использования вложений: " + e.getLocalizedMessage());
            }
            System.gc();
        }
        log("Вложений к удалению: " + attachmentsToDelete.size());
        int changed = 0;
        for (File file:attachmentsToDelete) {
            if (file.delete()) {
                log("Успешно удалён файл: " + file + ".");
                changed ++;
            }
        }
        return changed;
    }

    /**
     *  Допустим, мы получили Message от собеседника в телеграме.
     *  При этом все вложения которые в нём есть, они представлены FileID, которые ссылаются на файл на сервере.
     *  Но если мы хотим добавить ответ в базу, нам нужны будут ответы локально, не на сервере. Чтобы из базы их можно было отправлять.
     *  Эта функция находит в обьекте Message вложения которых нет локально и пытается их скачать, используя TgAccount.
     *  Файлы этой командой сохраняются сразу в папку attachments и становятся частью базы.
     *  @param  answer сообщение, вложения в коротом требуется сохранить локально.
     *  @param  tgAccount аккаунт телеграм, который получил те FileID которые во вложениях. (FileID действителен только для одного аккаунта)
     *  @return Message, тот же который и был принят, только после процедуры обновления вложений в нём.
     *  Использовать возвращаемое значение функции не обязательно, поскольку изменения производятся с тем обьектом, который был прислан на входе.
     *  @author Dr. Failov
     *  @throws Exception Поскольку производится сложная работа с файлом, случиться может что угодно
     *
     * */
    private Message saveAttachmentsToLocal(Message answer, TgAccount tgAccount) throws Exception{
        ArrayList<Attachment> attachments = answer.getAttachments();
        for (Attachment attachment:attachments){
            if(attachment.isPhoto() && attachment.isOnlineTg(tgAccount.getId()) && !attachment.isLocalInAttachmentFolder(applicationManager)){
                log("Сохраняю фото из сообщения в локальную папку...");
                String fileId = attachment.getTgFileID(tgAccount.getId());
                File tmpFile = tgAccount.downloadPhotoAttachment(fileId);
                if(tmpFile != null && tmpFile.isFile()) {
                    File destFile = new File(folderAttachments, tmpFile.getName());
                    log("Перемещение файла " + tmpFile.getName() + " из " + tmpFile.getParentFile() + " в " + destFile.getParentFile() + "...");
                    if(tmpFile.renameTo(destFile)) {
                        log("Файл перемещен. Прикрепляю файл к ответу.");
                        attachment.setFilename(destFile.getName());
                    }
                    else {
                        throw new Exception("Ошибка скачивания вложений: Не получается сохранить файл в папке вложений.");
                    }
                }
            }
        }
        return answer;
    }

    private String compareMessagesLastS1Text = ""; //данные для оптимизации compareMessages. Хранит строку с прошлого вызова, чтобы понять актуальны ли следующие поля
    private boolean compareMessagesLastS1Empty = false; //данные для оптимизации compareMessages. Хранит информацию о том оказалась ли строка с прошлого вызова пустой
    private boolean compareMessagesLastS1Question = false; //данные для оптимизации compareMessages. Хранит информацию о том содержала ли строка с прошлого вызова вопрос
    private ArrayList<String> compareMessagesLastS1Words = new ArrayList<>(); //данные для оптимизации compareMessages. Хранит преобразованную строку с прошлого вызова готовую к сравнению
    private String compareMessagesLastS2Text = ""; //данные для оптимизации compareMessages. Хранит строку с прошлого вызова, чтобы понять актуальны ли следующие поля
    private boolean compareMessagesLastS2Empty = false; //данные для оптимизации compareMessages. Хранит информацию о том оказалась ли строка с прошлого вызова пустой
    private boolean compareMessagesLastS2Question = false; //данные для оптимизации compareMessages. Хранит информацию о том содержала ли строка с прошлого вызова вопрос
    private ArrayList<String> compareMessagesLastS2Words = new ArrayList<>(); //данные для оптимизации compareMessages. Хранит преобразованную строку с прошлого вызова готовую к сравнению

    /**
     * Сравнивает две фразы.
     * Если на вход одна из фраз многократно подряд повторяется,
     * результат её обработки сохраняется во временную переменную и её обработка
     * не пересчитывается каждый раз заново (оптимизация, мать его).
     *
     * === Процедура такая: ===
     - привести текст входящего сообшения к нижнему регистру
     - убрать обращение бот
     - оставить только последнее предложение
     - Сохранить информацию о том есть ли знак вопроса
     - Убрать все символы и знаки, оставить только текст
     - если после этого всего строка оказывается пустая - результат сравнения точно 0
     - Заменить символы которые часто забивают писать (ё ъ щ)
     - устранить любые символы повторяющиеся несколько раз
     - заменить синонимы (полнотекстовым образом)
     - Тримануть
     - разложить на слова
     - сравнить все слова со всеми  по алгоритму Жаро-Винклера
     - При сравнении между вариантами учитывать если есть слова которые не участвовали в сравнении
     - ПРи сравнении учитывать наличие знака вопроса в тексте*.
     - учитывать длину сообщения

     * @param s1 Входная строка1 для сравнения
     * @param s2 Входная строка2 для сравнения
     * @return Коэффициент похожести фраз. На данный момент 0.6 = полное сходство.
     */
    private double compareMessages(String s1, String s2){
        if(compareMessagesLastS1Text.equals(s1) && compareMessagesLastS1Empty)
            return 0;
        if(compareMessagesLastS2Text.equals(s2) && compareMessagesLastS2Empty)
            return 0;
        if(!compareMessagesLastS1Text.equals(s1)){
            compareMessagesLastS1Text = s1;
            log("IN        :" + s1);//Пример: "бОт , ОпАчКи, ты бот. ПривЁЁёётик как ДелииишкИ ?!?!?! ??! )))"
            s1 = s1.toLowerCase(Locale.ROOT); //привести текст входящего сообшения к нижнему регистру
            log("LOWCASE   :" + s1); //Пример: "бот , опачки, ты бот. привёёёётик как делииишки ?!?!?! ??! )))"
            s1 = removeTreatment(s1); //убрать обращение бот
            log("TREATMENT :" + s1); //Пример: " опачки, ты бот. привёёёётик как делииишки ?!?!?! ??! )))"
            s1 = passOnlyLastSentence(s1); //оставить только последнее предложение
            log("LAST SENT :" + s1); //Пример: " привёёёётик как делииишки ?!?!?! ??! )))"
            compareMessagesLastS1Question = isQuestion(s1); //Сохранить информацию о том есть ли знак вопроса или слово вопроса
            s1 = filterSymbols(s1); //Убрать все символы и знаки, оставить только текст
            log("FILTER    :" + s1); //Пример: " привёёёётик как делииишки   "
            compareMessagesLastS1Empty = s1.isEmpty();  //проверить не оказывается ли у нас пустая строка
            if(compareMessagesLastS1Empty) return 0; //если после этого всего строка оказывается пустая - результат сравнения точно 0
            s1 = replacePhoneticallySimilarLetters(s1); //Заменить символы которые часто забивают писать (ё ъ щ)
            log("PHONETIC  :" + s1); //Пример: " привеееетик как делииишки   "
            s1 = removeRepeatingSymbols(s1); //устранить любые символы повторяющиеся несколько раз
            log("REPEATING :" + s1); //Пример: " приветик как делишки   "
            s1 = synonyme.replaceSynonyms(s1); //заменить синонимы (полнотекстовым образом в нижнем регистре)
            log("SYNONYMS  :" + s1); //Пример: " привет как дела   "
            s1 = s1.trim(); //Тримануть
            log("TRIM      :" + s1); //Пример: "привет как дела"
            compareMessagesLastS1Words = new ArrayList<>(Arrays.asList(s1.split(" ")));//разложить на слова
            log("ARRAY     :" + compareMessagesLastS1Words); //Пример: ["привет","как","дела"]
        }
        if(!compareMessagesLastS2Text.equals(s2)){
            compareMessagesLastS2Text = s2;
            //Пример: "бОт , ОпАчКи, ты бот. ПривЁЁёётик как ДелииишкИ ?!?!?! ??! )))"
            s2 = s2.toLowerCase(Locale.ROOT); //привести текст входящего сообшения к нижнему регистру
            //Пример: "бот , опачки, ты бот. привёёёётик как делииишки ?!?!?! ??! )))"
            s2 = removeTreatment(s2); //убрать обращение бот
            //Пример: " опачки, ты бот. привёёёётик как делииишки ?!?!?! ??! )))"
            s2 = passOnlyLastSentence(s2); //оставить только последнее предложение
            compareMessagesLastS2Question = isQuestion(s2); //Сохранить информацию о том есть ли знак вопроса или слово вопроса
            //Пример: " привёёёётик как делииишки ?!?!?! ??! )))"
            s2 = filterSymbols(s2); //Убрать все символы и знаки, оставить только текст
            compareMessagesLastS2Empty = s2.isEmpty();  //проверить не оказывается ли у нас пустая строка
            if(compareMessagesLastS2Empty) return 0; //если после этого всего строка оказывается пустая - результат сравнения точно 0
            //Пример: " привёёёётик как делииишки   "
            s2 = replacePhoneticallySimilarLetters(s2); //Заменить символы которые часто забивают писать (ё ъ щ)
            //Пример: " привеееетик как делииишки   "
            s2 = removeRepeatingSymbols(s2); //устранить любые символы повторяющиеся несколько раз
            //Пример: " приветик как делишки   "
            s2 = synonyme.replaceSynonyms(s2); //заменить синонимы (полнотекстовым образом в нижнем регистре)
            //Пример: " привет как дела   "
            s2 = s2.trim(); //Тримануть
            //Пример: "привет как дела"
            compareMessagesLastS2Words = new ArrayList<>(Arrays.asList(s2.split(" ")));//разложить на слова
            //Пример: ["привет","как","дела"]
        }


        //сравнить все слова со всеми  по алгоритму Жаро-Винклера
        //
        //1 записать общее количество слов для обоих фраз
        //2 Найти между двумя строками пару самых похожих слов (максимальный коэффициент)
        //3 Их похожесть добавить к сумме по этой паре фраз
        //4 Эту пару слов удалить из списка
        //5 Если в каждой фразе ещё остались слова, вернуться к п.2
        //6 Результат вычислить по формуле: сумма похожести / общее количество слов обоих фраз

        //работает с копиями потому что массив будет редактироваться
        ArrayList<String> s1words = new ArrayList<>(compareMessagesLastS1Words);
        ArrayList<String> s2words = new ArrayList<>(compareMessagesLastS2Words);
        //записать общее количество слов для обоих фраз
        double wordsSum = s1words.size() + s2words.size();
        double similaritySum = 0;
        //Если в каждой фразе ещё остались слова, вернуться
        while(!s1words.isEmpty() && !s2words.isEmpty()){
            //Найти между двумя строками пару самых похожих слов (максимальный коэффициент)
            String s1wordMax = null;
            String s2wordMax = null;
            double maxSimilarity = 0;
            for (int i=0; i<s1words.size(); i++){
                for (int j=0; j<s2words.size(); j++){
                    String s1word = s1words.get(i);
                    String s2word = s2words.get(j);
                    double similarity = jaroWinkler.similarity(s1word, s2word);
                    if(similarity >= maxSimilarity){
                        s1wordMax = s1word;
                        s2wordMax = s2word;
                        maxSimilarity = similarity;
                    }
                }
            }
            //Их похожесть добавить к сумме по этой паре фраз
            similaritySum += maxSimilarity;
            //Эту пару слов удалить из списка
            s1words.remove(s1wordMax);
            s2words.remove(s2wordMax);
        }
        //Результат вычислить по формуле: сумма похожести / общее количество слов обоих фраз
        double result = similaritySum / wordsSum;

        //учесть знак вопроса
        if(compareMessagesLastS1Question == compareMessagesLastS2Question)
            result += 0.1;

        //учесть длину строк. Чем больше это число тем больше значат отличия в длине строк
        result -= Math.abs(s1.length()-s2.length()) * 0.004;

        return result;
    }

    /**
     * Удаляет из фразы обращение "бот,", если такое есть. Если вся фраза состоит из слова "бот", фраза не изменится.
     * Пример: "бот, ты бот" -> "ты бот".
     * Пример: "бот ,ты бот" -> "ты бот".
     * Пример: "бот ты бот" -> "ты бот".
     * Пример: "бот!!" -> "бот!!".
     * Пример: "трава зелёная" -> "трава зелёная".
     * @param s1 Входная строка на русском в нижнем регистре
     * @return Текст без изменений, либо без обращения бот.
     */
    private static String removeTreatment(String s1){
        //бот ,     бот,      бот
        if(filterSymbols(s1).trim().equals("бот")) return s1; //если бот - это единственное что есть в фразе
        if (s1.startsWith("бот ,")) s1 = s1.substring(5);
        else if (s1.startsWith("бот,")) s1 = s1.substring(4);
        else if (s1.startsWith("бот")) s1 = s1.substring(3);
        return s1;
    }

    /**
     * Определяет есть ли в предложении вопрос. Определяет по наличию знака вопроса или словам типа "как", "когда" и т.п.
     * Пример: "Ёпта, бот. Как ты?" -> true.
     * Пример: "Как ты" -> true.
     * Пример: "Трава зелёная" -> false.
     * @param s1 Входная строка на русском в нижнем регистре
     * @return true если это вопрос, false если это не вопрос.
     */
    private static boolean isQuestion(String s1){
        return  (" "+s1).contains("?")
                || (" "+s1).contains(" где ")
                || (" "+s1).contains(" ли ")
                || (" "+s1).contains(" сколько ")
                || (" "+s1).contains(" когда ")
                || (" "+s1).contains(" как ")
                || (" "+s1).contains(" какие ")
                || (" "+s1).contains(" кем ")
                || (" "+s1).contains(" каким ")
                || (" "+s1).contains(" с кем ")
                || (" "+s1).contains(" в каком ")
                || (" "+s1).contains(" какой ")
                || (" "+s1).contains(" какая ")
                || (" "+s1).contains(" какими ")
                || (" "+s1).contains(" что ");
    }

    private static boolean isNumber(String s){
        if(s.isEmpty())
            return false;
        final String numbers = "0123456789";
        for (char c: s.toCharArray()){
            if(numbers.indexOf(c) == -1)
                return false;
        }
        return true;
    }

    /**
     * Оставить только последнее предложение, если принято несколько. Разделяет по точкам.
     * Пример: "Ёпта, бот. Как ты?" -> " Как ты?".
     * @param in Входная строка
     * @return Строка в которой осталось только последнее предложение.
     */
    private static String passOnlyLastSentence(String in){
        if(!in.contains("."))
            return in;
        String[] sentences = in.split("\\.");
        for(int i=sentences.length-1; i>=0; i--)
            if(sentences[i].length() > 3)
                return sentences[i];
        //если не было найдено ни одного нормального предложения
        return in;
    }

    /**
     * Убрать в строке все символы кроме букв в нижнем регистре
     * Пример: "о_О їбать ты лох!!(((" -> "їбать ты лох".
     * @param input Входная строка в нижнем регистре
     * @return Строка в которой остались только буквы в нижнем регистре и пробелы.
     */
    private static String filterSymbols(String input){
        //String allowedSymbols = "qwertyuiopasdfghjklzxcvbnm їіёйцукенгшщзхъфывапролджэячсмитьбю 1234567890";
        String allowedSymbols = "qwertyuiopasdfghjklzxcvbnm їіёйцукенгшщзхъфывапролджэячсмитьбю";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if(allowedSymbols.indexOf(c) >= 0)
                builder.append(c);
        }
        return builder.toString();
    }

    /**
     * Убрать в строке повторы символов
     * Пример: "Привеееет ебанааат!!!" -> "Привет ебанат!".
     * @param in Входная строка
     * @return Строка без повторов символов.
     */
    public static String removeRepeatingSymbols(String in){
        Character last = null;
        String result = "";
        for (Character c : in.toCharArray()) {
            if (c.equals(last)) {
                continue;
            }
            result = result.concat(c.toString());
            last = c;
        }
        return result;
    }

    /**
     * Заменить фонетически похожие буквы, которые часто пишут или игнорируют. Пример: й,ё,ъ.
     * Пример: "съешь ещё" -> "сьешь еше".
     * @param in Входная строка на русском в нижнем регистре
     * @return Строка с заменёнными символами.
     */
    public static String replacePhoneticallySimilarLetters(String in) {
        String result = in;
        result = result.replace('ё', 'е');
        result = result.replace('й', 'и');
        result = result.replace('ї', 'і');
        result = result.replace('щ', 'ш');
        result = result.replace('ъ', 'ь');
        return result;
    }

    /**
     * Overwrite(!!!!) database by default from resources
     * */
    private void loadDefaultDatabase() throws Exception {
        if (fileAnswers.isFile()) {
            log(". Удаление старой базы answer_database.txt перед восстановлением стандартной базы...");
            log(". Старый файл базы удалён перед восстановлением: " + fileAnswers.delete());
            log(". Удаление вложений перед восстановлением стандартной базы...");
            File[] attachments = folderAttachments.listFiles();
            if(attachments != null) {
                for (File file : attachments) {
                    log(". Удаление файла "+file.getName()+": " + file.delete());
                }
            }
            log(". Удаление пустой папки вложений перед восстановлением стандартной базы...");
            log(". Удаление папки "+folderAttachments.getName()+": " + folderAttachments.delete());
        }
        //get resources
        Resources resources = null;
        if (applicationManager.getContext() != null)
            resources = applicationManager.getContext().getResources();
        //copy zip
        File tmpZip = new File(folderAnswerDatabase, "answer_database.zip");
        log(". Копирование файла answer_database.zip из ресурсов...");
        F.copyFile(defaultDatabaseResourceZip, resources, tmpZip);
        //unzip
        log(". Распаковка файла answer_database.zip...");
        try {
            F.unzip(tmpZip, tmpZip.getParentFile());
        } catch (Exception e) {
            log("! Во время распаковки базы произошла ошибка: " + e.getMessage());
            log("Файл базы удалён после неудачной распаковки: " + fileAnswers.delete());
        }
        //check
        if (fileAnswers.isFile()) {
            log(". Распаковка базы успешна, удаление временного архива...");
            log(". Удаление временного архива: " + tmpZip.delete());
        } else {
            throw new Exception(log("! В результате распаковки файл базы не был получен. Проверьте, чтобы в архиве обязательно был файл answer_database.txt. Если такого файла нет, архив повреждён, нужен другой."));
        }
    }



    /**
     *
     * The Jaro–Winkler distance metric is designed and best suited for short
     * strings such as person names, and to detect typos; it is (roughly) a
     * variation of Damerau-Levenshtein, where the substitution of 2 close
     * characters is considered less important then the substitution of 2 characters
     * that a far from each other.
     * Jaro-Winkler was developed in the area of record linkage (duplicate
     * detection) (Winkler, 1990). It returns a value in the interval [0.0, 1.0].
     * The distance is computed as 1 - Jaro-Winkler similarity.
     *
     * https://en.wikipedia.org/wiki/Jaro%E2%80%93Winkler_distance
     * https://github.com/tdebatty/java-string-similarity/blob/master/src/main/java/info/debatty/java/stringsimilarity/JaroWinkler.java
     *
     * @author Thibault Debatty
     */
    static class JaroWinkler{

        private static final double DEFAULT_THRESHOLD = 0.7;
        private static final int THREE = 3;
        private static final double JW_COEF = 0.1;
        private final double threshold;

        /**
         * Instantiate with default threshold (0.7).
         *
         */
        public JaroWinkler() {
            this.threshold = DEFAULT_THRESHOLD;
        }

        /**
         * Instantiate with given threshold to determine when Winkler bonus should
         * be used.
         * Set threshold to a negative value to get the Jaro distance.
         * @param threshold Threshold
         */
        public JaroWinkler(final double threshold) {
            this.threshold = threshold;
        }

        /**
         * Returns the current value of the threshold used for adding the Winkler
         * bonus. The default value is 0.7.
         *
         * @return the current value of the threshold
         */
        public final double getThreshold() {
            return threshold;
        }

        /**
         * Compute Jaro-Winkler similarity.
         * @param s1 The first string to compare.
         * @param s2 The second string to compare.
         * @return The Jaro-Winkler similarity in the range [0, 1]
         * @throws NullPointerException if s1 or s2 is null.
         */
        public final double similarity(final String s1, final String s2) {
            if (s1 == null) {
                throw new NullPointerException("s1 must not be null");
            }

            if (s2 == null) {
                throw new NullPointerException("s2 must not be null");
            }

            if (s1.equals(s2)) {
                return 1;
            }

            int[] mtp = matches(s1, s2);
            float m = mtp[0];
            if (m == 0) {
                return 0f;
            }
            double j = ((m / s1.length() + m / s2.length() + (m - mtp[1]) / m))
                    / THREE;
            double jw = j;

            if (j > getThreshold()) {
                jw = j + Math.min(JW_COEF, 1.0 / mtp[THREE]) * mtp[2] * (1 - j);
            }
            return jw;
        }


        /**
         * Return 1 - similarity.
         * @param s1 The first string to compare.
         * @param s2 The second string to compare.
         * @return 1 - similarity.
         * @throws NullPointerException if s1 or s2 is null.
         */
        public final double distance(final String s1, final String s2) {
            return 1.0 - similarity(s1, s2);
        }

        private int[] matches(final String s1, final String s2) {
            String max, min;
            if (s1.length() > s2.length()) {
                max = s1;
                min = s2;
            } else {
                max = s2;
                min = s1;
            }
            int range = Math.max(max.length() / 2 - 1, 0);
            int[] matchIndexes = new int[min.length()];
            Arrays.fill(matchIndexes, -1);
            boolean[] matchFlags = new boolean[max.length()];
            int matches = 0;
            for (int mi = 0; mi < min.length(); mi++) {
                char c1 = min.charAt(mi);
                for (int xi = Math.max(mi - range, 0),
                     xn = Math.min(mi + range + 1, max.length()); xi < xn; xi++) {
                    if (!matchFlags[xi] && c1 == max.charAt(xi)) {
                        matchIndexes[mi] = xi;
                        matchFlags[xi] = true;
                        matches++;
                        break;
                    }
                }
            }
            char[] ms1 = new char[matches];
            char[] ms2 = new char[matches];
            for (int i = 0, si = 0; i < min.length(); i++) {
                if (matchIndexes[i] != -1) {
                    ms1[si] = min.charAt(i);
                    si++;
                }
            }
            for (int i = 0, si = 0; i < max.length(); i++) {
                if (matchFlags[i]) {
                    ms2[si] = max.charAt(i);
                    si++;
                }
            }
            int transpositions = 0;
            for (int mi = 0; mi < ms1.length; mi++) {
                if (ms1[mi] != ms2[mi]) {
                    transpositions++;
                }
            }
            int prefix = 0;
            for (int mi = 0; mi < min.length(); mi++) {
                if (s1.charAt(mi) == s2.charAt(mi)) {
                    prefix++;
                } else {
                    break;
                }
            }
            return new int[]{matches, transpositions / 2, prefix, max.length()};
        }
    }

    /**
     *
     * You put here a lot of messages and get [capacity] ones with biggest rating
     *
     * @author Dr. Failov
     */
    private static class MessageRating{
        private final int capacity;
        private final AnswerElement[] messages; //0 - top element (max rating)
        private final double[] ratings;

        /**
         * Instantiate with default capacity of 10
         */
        public MessageRating() {
            this.capacity = 10;
            messages = new AnswerElement[capacity];
            ratings = new double[capacity];
        }
        /**
         * Instantiate with given capacity
         * @param capacity Number of elements stored in rating
         */
        public MessageRating(int capacity) {
            this.capacity = capacity;
            messages = new AnswerElement[capacity];
            ratings = new double[capacity];
        }

        /**
         * Get Array of messages which have equal and biggest rating from ever received.
         * Several AnswerElements will be only in case if its ratings equal.
         * @return Message with biggest rating or NULL.
         */
        public ArrayList<AnswerElement> getTopMessages(){
            ArrayList<AnswerElement> result = new ArrayList<>();
            if(messages[0] != null) {
                result.add(messages[0]);
                for (int i=1; i<capacity; i++){
                    if(ratings[0] == ratings[i])
                        result.add(messages[i]);
                    else
                        break;
                }
            }
            return result;
        }

        /**
         * Return Message with one of the biggest rating ever received.
         * @return Message with one of the biggest rating or NULL.
         */
        public AnswerElement getTopMessage(){
            return messages[0];
        }

        /**
         * Return Message with biggest rating ever received.
         * @param index 0 is first place, 1 is second place in rating... Max is capacity-1
         * @return Message with taken place in rating or NULL.
         */
        public AnswerElement getTopMessage(int index){
            if(index > capacity-1)
                return null;
            return messages[index];
        }

        /**
         * Return rating value of Message with biggest rating ever received.
         * @return Message with biggest rating or NULL.
         */
        public double getTopRating(){
            return ratings[0];
        }

        /**
         * Return rating value of Message with biggest rating ever received.
         * @param index 0 is first place, 1 is second place in rating... Max is capacity-1
         * @return Message with taken place in rating or NULL.
         */
        public double getTopRating(int index){
            if(index > capacity-1)
                return 0;
            return ratings[index];
        }

        /**
         * @return Number of elements stored in rating
         */
        public int getCapacity() {
            return capacity;
        }

        /**
         * Check if rating is empty
         * @return true if no any elements ever received
         */
        public boolean isEmpty(){
            return messages[0] == null;
        }

        /**
         * Return rating value of Message with biggest rating ever received.
         * @param message Message to store in rating.
         * @param rating rating of Message to store in rating.
         */
        public void addAnswer(AnswerElement message, double rating){
//            if(rating > 0)
//                log("add " + rating + " \t" + message.getText().replace("\n", " ").substring(0, 100));
            for(int i=0; i<capacity; i++){
                if(ratings[i] == 0 || ratings[i] < rating){
                    //shift all next (from i+1 to capacity-1)
                    for(int j=capacity-1; j>=i+1; j--) {
                        messages[j] = messages[j-1];
                        ratings[j] = ratings[j-1];
                    }
                    messages[i] = message;
                    ratings[i] = rating;
                    return;
                }
            }
        }
    }

    /**
     * Команда "выгрузить базу"
     */
    private class DumpCommand extends CommandModule{
        @Override
        public ArrayList<Message> processCommand(Message message, TgAccount tgAccount) throws Exception {
            ArrayList<Message> result = super.processCommand(message, tgAccount);
            if(message.getText().toLowerCase(Locale.ROOT).trim().equals("выгрузить базу")) {
                log("Выполнение команды выгрузки дампа базы. Выбор имени для архива...");
                //Выбрать имя для нового файла
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                File tmpZipFile = new File(applicationManager.getTempFolder(), sdf.format(new Date())+"_DatabaseDump.zip");
                //если файл сегодня уже создавался, ему придумается новое имя. И так до тех пор, пока имя не будет уникальным.
                for(int i=2; tmpZipFile.isFile(); i++) {
                    tmpZipFile = new File(applicationManager.getTempFolder(), sdf.format(new Date()) + "_DatabaseDump" + i + ".zip");
                }
                log("Создание архива "+tmpZipFile.getName()+"...");
                try {
                    ZipFile zipFile = new ZipFile(tmpZipFile);
                    ZipParameters parameters = new ZipParameters();
                    parameters.setCompressionMethod(CompressionMethod.DEFLATE);
                    parameters.setCompressionLevel(CompressionLevel.NORMAL);
                    zipFile.createSplitZipFileFromFolder(folderAnswerDatabase, parameters, true, 45485760);
                    log("Архив создан без ошибок.");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                log("Вот какие файлы теперь валяются во временной папке: ");
                File[] tmpFiles = applicationManager.getTempFolder().listFiles();
                if(tmpFiles != null) {
                    for (File file : tmpFiles) {
                        log("- " + file.getName() + " : " + file.length() + " байт.");
                    }
                }

                String archiveName = tmpZipFile.getName().split("\\.")[0];
                log("Имя текущего архива: " + archiveName);
                if(tmpZipFile.isFile()) {
                    log("Файлы к отправке: ");
                    if(tmpFiles != null) {
                        for (File file : tmpFiles) {
                            if(file.getName().contains(archiveName)) {
                                Message answer = new Message("Дамп базы прикрепляю файлом.");
                                log("- " + file.getName() + " : " + file.length() + " байт.");
                                answer.addAttachment(new Attachment().setDoc().setFileToUpload(file));
                                result.add(answer);
                            }
                        }
                    }
                }
            }
            return result;
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = super.getHelp();
            result.add(new CommandDesc("выгрузить базу", "Отправить во вложении архив с текущей базой и вложениями."));
            return result;
        }
    }
    /**
     * Команда "Запомни"
     */
    private class RememberCommand extends CommandModule{
        private final HashMap<Long, RememberCommandSession> sessions = new HashMap<>();

        @Override
        public ArrayList<Message> processCommand(Message message, TgAccount tgAccount) throws Exception {
            //через сколько минут после получения команды сбрасываться до IDLE
            long commandTimeoutMs = 5 * 60 * 1000; //5 мин
            ArrayList<Message> result = super.processCommand(message, tgAccount);
            Long userId = message.getAuthor().getId();

            synchronized (sessions) {
                RememberCommandSession session = sessions.get(userId);

                if (session == null) { //начать новую сессию
                    if (message.getText().toLowerCase(Locale.ROOT).trim().equals("запомни")) {
                        log("Команда \"запомни\" получена. Ожидаю поступления сообщений.");
                        sessions.put(userId, new RememberCommandSession());
                        result.add(new Message("Ответ на команду \"<b>"+message.getText() + "</b>\"\n\n"+
                                "Это команда для добавления ответа в базу ответов.\n" +
                                "Теперь пришли 2 сообщения, вопрос и ответ.\n" +
                                "Если передумал, напиши <code>отмена</code>."));
                    }
                }
                else { //сессия уже есть и активна

                    {//Проверить не слишком ли дофига времени собеседник тупил и актуальна ли ещё вообще его команда
                        long difference = new Date().getTime() - session.sessionStarted.getTime();
                        if (difference > commandTimeoutMs) {
                            sessions.remove(userId);
                            log("Команда \"запомни\" отклонена поскольку вопроса пришлось ждать слишком долго.");
                            return result;
                        }
                    }
                    if (message.getText().toLowerCase(Locale.ROOT).trim().equals("отмена")) {
                        sessions.remove(userId);
                        result.add(new Message(log(
                                "Ответ на команду \"<b>"+message.getText() + "</b>\"\n\n" +
                                "Команда \"запомни\" отклонена по команде пользователя.")));
                        return result;
                    }
                    session.messages.add(message);
                    log("Команда \"запомни\" получила сообщение "+session.messages.size()+".");
                    if (session.messages.size() == 1) {
                        result .add(new Message("")); //чтобы пропустить ответ из базы
                        return result;
                    }
                    if (session.messages.size() == 2) {
                        Message question = null;
                        Message answer = null;
                        if(session.messages.get(0).getMessage_id() < session.messages.get(1).getMessage_id()) {
                            question = session.messages.get(0);
                            answer = session.messages.get(1);
                        }
                        else {
                            question = session.messages.get(1);
                            answer = session.messages.get(0);
                        }
                        log("Команда \"запомни\" присвоила сообщениям такую последовательность: \n" +
                                question + " -> " + answer);
                        //добавить в базу используя полученные данные
                        log("Команда \"запомни\" добавляет вопрос и ответ в базу данных...");
                        try {
                            AnswerElement answerElement = addAnswerToDatabase(question, answer, tgAccount);
                            log("Команда \"запомни\" завершена.");
                            sessions.remove(userId);
                            result .add(new Message(
                                    "Ответ на команду \"<b>Запомни</b>\"\n\n"+
                                    "<b>Добавлено в базу:</b> " + answerElement +
                                            "\n<b>ID:</b> <code>" + answerElement.getId() + "</code>"));
                        }
                        catch (Exception e){
                            log("Команда \"запомни\" не смогла добавить ответ в базу, вот почему: " + e.getLocalizedMessage());
                            e.printStackTrace();
                            sessions.remove(userId);
                            result .add(new Message(
                                    "Ответ на команду \"<b>Запомни</b>\"\n\n"+
                                            "<b>Ошибка:</b> " + e.getLocalizedMessage()));
                        }
                    }
                    if (session.messages.size() >= 3){
                        //если в списке более 2 ответов, значит что-то уже пошло не так и эту сессию следует закрывать
                        sessions.remove(userId);
                    }
                }
            }

            return result;
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = super.getHelp();
            result.add(new CommandDesc("запомни", "После этого сообщения пришли 2 сообщения: вопрос и ответ. Сохранит в базу такую пару вопрос-ответ."));
            return result;
        }

        private class RememberCommandSession{
            ArrayList<Message> messages;
            Date sessionStarted;

            public RememberCommandSession() {
                sessionStarted = new Date();
                messages = new ArrayList<>();
            }
        }
    }
    /**
     * Команда "Ответы 15032"
     */
    private class GetAnswersByIdCommand extends CommandModule{
        final int numberOfAnswers = 20;
        @Override
        public ArrayList<Message> processCommand(Message message, TgAccount tgAccount) throws Exception {
            ArrayList<Message> result = super.processCommand(message, tgAccount);
            String[] words = message.getText().toLowerCase(Locale.ROOT).trim().split(" ");
            if (words.length == 2 && words[0].equals("ответы") && isNumber(words[1])) {
                long neededIndex = Long.parseLong(words[1]);
                long startedIndex = neededIndex - (numberOfAnswers / 2);
                ArrayList<AnswerElement> answerElements = getAnswers(startedIndex, numberOfAnswers);
                StringBuilder stringBuilder = new StringBuilder("Ответ на команду \"<b>"+message.getText() + "</b>\"\n\n");
                stringBuilder.append("Вот список ответов в базе, находящихся рядом с заданным  ID:\n");
                if(answerElements.isEmpty()){
                    stringBuilder.append("В заданном диапазоне ID ответов не найдено.");
                }
                for (AnswerElement answerElement:answerElements) {
                    stringBuilder.append("<code>").append(answerElement.getId()).append("</code> : ");
                    if(answerElement.getId() == neededIndex)
                        stringBuilder.append("<b>");
                    stringBuilder.append(answerElement);
                    if(answerElement.getId() == neededIndex)
                        stringBuilder.append("</b>");
                    stringBuilder.append("\n");
                    if(stringBuilder.length() > 3800)
                        break;
                }
                result.add(new Message(stringBuilder.toString()));
            }
            return result;
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = super.getHelp();
            result.add(new CommandDesc("ответы 15032", "Выведет список из "+numberOfAnswers/2+" сообщений до указанного ответа и "+numberOfAnswers/2+" сообщений после указанного ответа."));
            return result;
        }
    }
    /**
     * Команда "Ответы на Иди нахуй!"
     */
    private class GetAnswersByQuestionCommand extends CommandModule{
        final int numberOfAnswers = 20;
        @Override
        public ArrayList<Message> processCommand(Message message, TgAccount tgAccount) throws Exception {
            ArrayList<Message> result = super.processCommand(message, tgAccount);
            if (message.getText().toLowerCase(Locale.ROOT).trim().startsWith("ответы на")) {
                String questionText = message.getText().toLowerCase(Locale.ROOT).trim().replace("ответы на", "");
                Message question = new Message(questionText);
                question.setAuthor(message.getAuthor());
                for (Attachment attachment:message.getAttachments())
                    question.addAttachment(attachment);

                MessageRating messageRating = pickAnswers(question);

                StringBuilder stringBuilder = new StringBuilder("Ответ на команду \"<b>"+message.getText() + "</b>\"\n\n");
                stringBuilder.append("Вот список ответов в базе, подобранных на вопрос:\n");

                if(messageRating.isEmpty()){
                    stringBuilder.append("Нет ни одного ответа. Возможно, база пустая?");
                }

                for(int i = 0; i<messageRating.getCapacity() && i < numberOfAnswers; i++){
                    double rating = messageRating.getTopRating(i);
                    AnswerElement answerElement = messageRating.getTopMessage(i);
                    if(answerElement != null) {
                        stringBuilder.append("<code>").append(answerElement.getId()).append("</code> \t");
                        stringBuilder.append("<b>").append(String.format(Locale.US,"%.3f", rating)).append("</b>:\t");
                        stringBuilder.append(answerElement).append("\n");
                        if(stringBuilder.length() > 3800)
                            break;
                    }
                }

                result.add(new Message(stringBuilder.toString()));
            }
            return result;
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = super.getHelp();
            result.add(new CommandDesc("ответы на Привет!", "Выведет список из "+numberOfAnswers+" ответов на заданный вопрос с указанием их рейтинга."));
            return result;
        }
    }
    /**
     * Команда "Ответ 15032"
     */
    private class GetAnswerByIdCommand extends CommandModule{
        @Override
        public ArrayList<Message> processCommand(Message message, TgAccount tgAccount) throws Exception {
            ArrayList<Message> result = super.processCommand(message, tgAccount);
            String[] words = message.getText().toLowerCase(Locale.ROOT).trim().split(" ");
            if (words.length == 2 && words[0].equals("ответ") && isNumber(words[1])) {
                long neededIndex = Long.parseLong(words[1]);
                long startedIndex = neededIndex - 2;
                ArrayList<AnswerElement> answerElements = getAnswers(startedIndex, 5);


                for (AnswerElement answerElement:answerElements) {
                    if(answerElement.getId() == neededIndex){
                        Message answer = answerElement.getAnswerMessage();
                        StringBuilder sb = new StringBuilder("Ответ на команду \"<b>"+message.getText() + "</b>\"\n\n");
                        sb.append("Полная информация об ответе:\n");
                        sb.append("<b>ID: </b><code>").append(answerElement.getId()).append("</code>\n");
                        sb.append("<b>Был использован: </b>").append(answerElement.getTimesUsed()).append(" раз\n");
                        sb.append("\n");
                        sb.append("<b>Вопрос: </b>").append(answerElement.getQuestionMessage()).append("\n");
                        sb.append("<b>Автор вопроса: </b>").append(answerElement.getQuestionMessage().getAuthor()).append("\n");
                        sb.append("<b>Дата вопроса: </b>").append(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(answerElement.getQuestionMessage().getDate())).append("\n");
                        sb.append("\n");
                        sb.append("<b>Ответ: </b>").append(answer).append("\n");
                        sb.append("<b>Автор ответа: </b>").append(answerElement.getQuestionMessage().getAuthor()).append("\n");
                        sb.append("<b>Дата ответа: </b>").append(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(answerElement.getQuestionMessage().getDate())).append("\n");
                        if(answer.hasAttachments())
                            sb.append("<i>Вложения прикреплены к этому сообщению</i>\n");

                        answer.setText(sb.toString());
                        result.add(answer);
                        return result;
                    }
                }
                result.add(new Message("Ответа с ID <code>" + neededIndex + "</code> не найдено."));
            }
            return result;
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = super.getHelp();
            result.add(new CommandDesc("ответ 15032", "Отправит ответ с заданным ID"));
            return result;
        }
    }
    /**
     * Команда "Забудь 15032"
     */
    private class RemoveAnswerByIdCommand extends CommandModule{
        @Override
        public ArrayList<Message> processCommand(Message message, TgAccount tgAccount) throws Exception {
            ArrayList<Message> result = super.processCommand(message, tgAccount);
            String[] words = message.getText().toLowerCase(Locale.ROOT).trim().split(" ");
            if (words.length == 2 && words[0].equals("забудь") && isNumber(words[1])) {
                long neededIndex = Long.parseLong(words[1]);
                ArrayList<Long> toDelete = new ArrayList<>();
                toDelete.add(neededIndex);
                int deletedAnswers = removeAnswersFromDatabase(toDelete);
                int deletedAttachments = cleanUnusedAttachments();
                result.add(new Message("" +
                        "Ответ на команду \"<b>"+message.getText() + "</b>\"\n\n" +
                        "Удаление из базы ответа с ID <code>" + neededIndex + "</code>...\n" +
                        "<b>Удалено ответов:</b> " + deletedAnswers + ";\n"+
                        "<b>Удалено вложений:</b> " + deletedAttachments + ";\n"));
            }
            return result;
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = super.getHelp();
            result.add(new CommandDesc("забудь 15032", "удалит ответ с заданным ID из базы и почистит вложения"));
            return result;
        }
    }
}
