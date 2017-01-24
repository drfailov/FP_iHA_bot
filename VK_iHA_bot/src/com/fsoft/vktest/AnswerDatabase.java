package com.fsoft.vktest;


import org.json.JSONArray;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * база данных ключевых слов и ответов на них
 * Created by Dr. Failov on 06.08.2014.
 */
public class AnswerDatabase implements Command {
    ApplicationManager  applicationManager;
    FileReader fileReaderAnswerDatabase;
    File fileUnknownMessages;
    ArrayList<AnswerElement> answers = new ArrayList<>();
    ArrayList<String> unknownMessages = new ArrayList<>();
    ArrayList<Command> commands = new ArrayList<>();


    public AnswerDatabase(ApplicationManager applicationManager, String name, int databaseDefaultResource) {
        this.applicationManager = applicationManager;
        fileReaderAnswerDatabase = new FileReader(applicationManager.activity.getResources(), databaseDefaultResource, name);
        fileUnknownMessages = new File(ApplicationManager.getHomeFolder() + File.separator + "unknown_messages");
        commands.add(new Status());
        commands.add(new AddSpkPatt());
        commands.add(new Save());
        commands.add(new RemSpkPatt());
        commands.add(new DumpDatabase());
        commands.add(new ImportDatabase());
        commands.add(new DumpUnknownMessages());
    }
    public String addToDatabase(String message, String reply){
        AnswerElement ae = getNew(message, reply);
        return addToDatabase(ae);
    }
    public void load(){
        String fileText = fileReaderAnswerDatabase.readFile();
        if(fileText != null) {
            String[] lines = fileText.split("\\\n");
            log(". Загрузка ответов с " + fileReaderAnswerDatabase.getFilePath() + "...");
            log(". Количество строк в файле " + lines.length + "...");
            int cnt = 0;
            for (int i = 0; i < lines.length; i++) {
                try {
                    AnswerElement ae = new AnswerElement(lines[i]);
                    if (ae.keywords.size() < 50 && calculateWords(ae.text) < 50) {
                        answers.add(ae);
                        cnt ++;
                    }
                }
                catch (Exception e){
                    log("Ошибка обработки строки " + i + " | " + lines[i]);
                }
            }
            log(". Записано шаблонов в базу: " + cnt + "...");
            log(". Загружено "+answers.size()+" ответов.");
        }
        else{
            log("! Ошибка чтения из файла базы данных ответов: " + fileReaderAnswerDatabase.getFilePath());
        }
        clearDuplicates();
        clearDisallowedAnswers();

        try {
            log(". Загрузка неизвестных сообщений с " + fileUnknownMessages.getPath() + "...");
            BufferedReader fileReader = new BufferedReader(new java.io.FileReader(fileUnknownMessages));
            StringBuilder sb = new StringBuilder();
            String line = fileReader.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = fileReader.readLine();
            }
            JSONArray jsonArray = new JSONArray(sb.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                addUnknown(jsonArray.getString(i));
            }
            log(". Загружено "+unknownMessages.size()+" неизвестных сообщений.");
        }
        catch (Exception e){
            e.printStackTrace();
            log(". Ошибка чтения неизвестных сообщений: " + e.toString() + "\n");
        }
    }
    public String save(){
        String result = "";
        if(answers.size() > 1){
            result += log(". Сохранение ответов в " + fileReaderAnswerDatabase.getFilePath() + "...\n");
            if(fileReaderAnswerDatabase.writeFile(getDatabaseString()))
                result += log(". Сохранено "+answers.size()+" ответов.\n");
            else
                result += log("! Сохранить базу ответов в " + fileReaderAnswerDatabase.getFilePath() + " не удалось.\n");
        }
        else
            result += log("! База ответов пуста. Не сохранять.\n");

        if(unknownMessages.size() > 0){
            try {
                result += log(". Сохранение неизвестных сообщений ("+unknownMessages.size()+") в " + fileUnknownMessages.getPath() + "...\n");
                JSONArray jsonArray = new JSONArray();
                for (int i = 0; i < unknownMessages.size(); i++) {
                    jsonArray.put(unknownMessages.get(i));
                }
                FileWriter fileWriter = new FileWriter(fileUnknownMessages);
                fileWriter.write(jsonArray.toString());
                fileWriter.close();
                result += log(". Неизвестные сообщения ("+unknownMessages.size()+") сохранены в " + fileUnknownMessages.getPath() + ".\n");
            }
            catch (Exception e){
                e.printStackTrace();
                result += log(". Ошибка сохранения неизвестных сообщений: " + e.toString() + "\n");
            }
        }
        return result;
    }
    public void clearDuplicates(){
        log(". Очистка дубликатов ответов...");
        try {
            int cnt = 0;
            log("|──────────────────────────────|");
            int percent = 0;
            for (int i = 0; i < answers.size() - 1; i++) {
                int curPercent = (i * 100)/answers.size();
                if(curPercent != percent){
                    TabsActivity.consoleView.clearLastLine();
                    log(ConsoleView.getLoadingBar(30, curPercent));
                    percent = curPercent;
                }
                AnswerElement cur = answers.get(i);
                for (int j = i + 1; j < answers.size(); j++) {
                    AnswerElement tst = answers.get(j);
                    boolean textEquals = tst.text.equals(cur.text);
                    boolean keywordsEquals = tst.keywords.size() == cur.keywords.size();
                    if (keywordsEquals)
                        for (int k = 0; k < tst.keywords.size(); k++)
                            if (!tst.keywords.get(k).equals(cur.keywords.get(k)))
                                keywordsEquals = false;
                    if (textEquals && keywordsEquals) {
                        cnt++;
                        answers.remove(j);
                        j--;
                    }
                }
            }
            TabsActivity.consoleView.clearLastLine();
            log(". Удалено дубликатов: " + cnt);
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка удаления дубликатов: " + e.toString());

        }
    }
    public void clearDisallowedAnswers(){
        log(". Очистка запрещенных ответов...");
        //убрать из базы ответы, которые содержатся в реестре запрещенных слов и фраз
        try {
            int cnt = 0;
            log("|──────────────────────────────|");
            int percent = 0;
            for (int i = 0; i < answers.size(); i++) {
                int curPercent = (i * 100)/answers.size();
                if(curPercent != percent){
                    TabsActivity.consoleView.clearLastLine();
                    log(ConsoleView.getLoadingBar(30, curPercent));
                    percent = curPercent;
                }
                AnswerElement cur = answers.get(i);
                if(!applicationManager.messageProcessor.filter.isAllowed(cur.text)){
                    log(". Ответ запрещен: " + cur.text);
                    answers.remove(i);
                    i--;
                    cnt ++;
                }
            }
            TabsActivity.consoleView.clearLastLine();
            log(". Удалено запрещенных ответов: " + cnt);
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка удаления запрещенных ответов: " + e.toString());

        }
    }
    public String getDatabaseString(){
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < answers.size(); i++) {
            stringBuilder.append(answers.get(i).toParcelable());
            if(i < answers.size() -1)
                stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }
    public String getMaxValidAnswer(String message){
        if(message.length() == 0)
            return "Что?";
        float max = 0;
        //проведем эту процедуру всего один раз - так будет быстрее
        String messagePrepared = applicationManager.messageComparer.messagePreparer.processMessageBeforeComparation(message);
        ArrayList<AnswerElement> maxes = new ArrayList<>();
        for (int i = 0; i < answers.size(); i++) {
            float validity = applicationManager.messageComparer.compareMessages(answers.get(i).getPreparedKeywords(), true, messagePrepared, true);
            if(validity > max){
                max = validity;
                maxes.clear();
            }
            if(validity == max){
                maxes.add(answers.get(i));
            }
        }
        log(". Отобрано " + maxes.size() + " ответов со степенью схожести " + max*100f + "%");
        if(max < 0.7f)
            addUnknown(message);
        if(maxes.size() == 0)
            return "Да у меня, блин, база пустая! Удали файл "+ fileReaderAnswerDatabase.getFilePath()+", а я его заново перезапишу. Вот тогда и поговорим.";
        if(max < 0.2){
            return "Что?";
        }
        Random random = new Random();
        int index = random.nextInt(maxes.size());
        return maxes.get(index).text;
    }
    public @Override String getHelp(){
        String result = "";
        for (int i = 0; i < commands.size(); i++) {
            result += commands.get(i).getHelp();
        }
        return result;
    }
    public @Override String process(String text){
        String result = "";
        for (int i = 0; i < commands.size(); i++) {
            result += commands.get(i).process(text);
        }
        return result;
    }
    public void addUnknown(String message){
        if(message.split("\\ ").length < 7 && message.length() > 2 && message.length() < 30 && !unknownMessages.contains(message)){
            log(". Сообщение "+message+" внесено в список неизвестных.");
            unknownMessages.add(message);
        }
    }

    private String removeAnswer(String text){
        int cnt = 0;
        text = text.toLowerCase();
        for (int i = 0; i < answers.size(); i++) {
            AnswerElement ae = answers.get(i);
            if(ae.text.toLowerCase().equals(text)) {
                answers.remove(ae);
                i--;
                if(i<0)
                    i=0;
                cnt++;
            }
        }
        if(cnt == 0)
            return "Удаление шаблона не было выполнено: такого шаблона в базе нет.\n";
        else
            return "Удаление шаблона выполено. Удалено " + cnt + " ответов.\n";
    }
    private int calculateWords (String text){
        int words = 0;
        try{
            String[] wordsArray = text.split("\\ ");
            words = wordsArray.length;
        }
        catch (Exception e){}
        return words;
    }
    private String makeBeginWithUpper(String in){
        String first = String.valueOf(in.charAt(0));
        return first.toUpperCase() + in.substring(1);
    }
    private AnswerElement findTheSame(AnswerElement toFind){
        for (int i = 0; i < answers.size(); i++) {
            if(answers.get(i).equals(toFind))
                return answers.get(i);
        }
        return null;
    }
    private String log(String text){
        ApplicationManager.log(text);
        return text;
    }
    private String addToDatabase(AnswerElement toAdd){
        if(toAdd.keywords.size() >= 30 && calculateWords(toAdd.text) >= 30) {//не заносить в базу длинные ответы или шаблоны
            log("! pattern " + toAdd.text + " is too long. Skipping.");
            return "Ошибка добавления шаблона: строки слишком длинные.\n";
        }
        //добавить в базу ответ. Если он уже есть в базе - его число повторов увеличится. Если в базе его нет - он добавится.
        toAdd.text = makeBeginWithUpper(toAdd.text);
        AnswerElement same = findTheSame(toAdd);
        if(same == null) {
            answers.add(toAdd);
            return "Шаблон " + toAdd.toString() + " успешно добавлен в базу. Ответов в базе = "+answers.size()+"\n";
        }
        else {
            same.repeats++;
            if(same.repeats > 10)
                same.repeats = 10;
            return "Шаблон " + toAdd.toString() + " уже есть в базе. Число его повроторов уведичится до "+same.repeats+".\n";
        }
    }
    private AnswerElement getNew(String message, String reply){
        return new AnswerElement(message, reply);
    }

    class AnswerElement{
        String text;
        ArrayList<String> keywords;
        int repeats;
        //используется для оптимизации. Каждый раз при сравнении проводится подготовка сообщения. чтобы этого избежать,
        //будем оптимизировать внутри этого класса и хранить подготовленный ответ, сразу выдавая готой результат
        // функцией getPreparedKeywords
        String preraredKeywords = null;

        AnswerElement(String text, String[] keywords, int repeats){
            text = text.replace("\n", " ").replace("\\", "");
            this.text = text;
            this.keywords = new ArrayList<>();
            for (int i = 0; i < keywords.length; i++) {
                this.keywords.add((keywords[i]).replace(" ", ""));
            }
            this.repeats = repeats;
            if(this.repeats > 10)
                this.repeats = 10;
        }
        AnswerElement(String message, String reply){
            //сформировать шаблон ответа из сообщения и известного ответа на него
            //message = applicationManager.getAllMain(message);
            String[] keywords = applicationManager.trimArray(message.split("\\ "));
            reply = reply.replace("\n", " ").replace("\\", "");
            this.repeats = 0;
            this.text = reply;
            this.keywords = new ArrayList<>();
            for (int i = 0; i < keywords.length; i++) {
                this.keywords.add(keywords[i]);
            }
        }
        AnswerElement(String parcelable){
            //     key1 key2 key3 key4\answer\repeats
            //         привет как дела\привет\3
            String[] parts = parcelable.split("\\\\");
            text = parts[1];
            this.keywords = new ArrayList<>();
            String[] keywords = parts[0].split("\\ ");
            for (int i = 0; i < keywords.length; i++) {
                this.keywords.add(/*applicationManager.getAllMain*/(keywords[i])/*.replace(" ", "")*/);
            }
            this.repeats = Integer.parseInt(parts[2]);
        }

        //COPPARISON
        float compareWords(String word1, String word2){
            float sum = 0;
            int minLength = Math.min(word1.length(), word2.length());
            int maxLength = Math.max(word1.length(), word2.length());
            for (int i = 0; i < minLength; i++) {
                if(word1.charAt(i) == word2.charAt(i))
                    sum++;
            }
            return sum/maxLength;
        }
        float comparePattern(String[] message, String[] pattern){
            //make matrix
            float[][] matr = new float[message.length][pattern.length];
            for (int messageWord = 0; messageWord < message.length; messageWord++) {
                for (int patternWord = 0; patternWord < pattern.length; patternWord++) {
                    matr[messageWord][patternWord] = compareWords(message[messageWord], pattern[patternWord]);
                }
            }
            //calculate MAXes for pattern and message words
            float max = message.length + pattern.length;
            float sum = 0;
            //pattern
            for (int patternWord = 0; patternWord < pattern.length; patternWord++) {
                float patternMax = 0;
                for (int messageWord = 0; messageWord < message.length; messageWord++) {
                    if(matr[messageWord][patternWord] > patternMax)
                        patternMax = matr[messageWord][patternWord];
                }
                sum += patternMax;
            }
            //message
            for (int messageWord = 0; messageWord < message.length; messageWord++) {
                float messageMax = 0;
                for (int patternWord = 0; patternWord < pattern.length; patternWord++){
                    if(matr[messageWord][patternWord] > messageMax)
                        messageMax = matr[messageWord][patternWord];
                }
                sum += messageMax;
            }
            return sum / max;
        }
        public float calculateIndex(String text){
            //text = applicationManager.getAllMain(text);
            String[] messageKeywords = text.split("\\ ");
            messageKeywords = applicationManager.trimArray(messageKeywords);
            String[] patternKeywords = new String[keywords.size()];
            for (int i = 0; i < keywords.size(); i++) {
                patternKeywords[i] = keywords.get(i);
            }
            float index = comparePattern(messageKeywords, patternKeywords);
            index += 0.01f * (float)repeats;
            return index;
        }
        public boolean equals(AnswerElement o) {
            if(!o.text.equals(text))
                return false;
            if(! (o.keywords.size() == keywords.size()))
                return false;
            for (int i = 0; i < keywords.size(); i++) {
                if(!keywords.get(i).equals(o.keywords.get(i)))
                    return false;
            }
            return true;
        }
        public String toParcelable(){
            String result = "";
            for (int i = 0; i < keywords.size(); i++){
                result += keywords.get(i);
                if(i < keywords.size()-1)
                    result += " ";
            }
            result += "\\" + text;
            result += "\\" + repeats;
            return result;
        }
        public String toString(){
            String result = "\"" + text + "\" в ответ на \"";
            for (int i = 0; i < keywords.size(); i++){
                result += keywords.get(i) + " ";
            }
            result += "\" (приоритет " + repeats + ")";
            return result;
        }
        public String getKeywordsAsText(){
            String result = "";
            for (int i = 0; i < keywords.size(); i++){
                result += keywords.get(i);
                if(i < keywords.size()-1)
                    result += " ";
            }
            return result;
        }
        public String getPreparedKeywords(){
            if(preraredKeywords == null) {
                String result = "";
                for (int i = 0; i < keywords.size(); i++) {
                    result += keywords.get(i);
                    if (i < keywords.size() - 1)
                        result += " ";
                }
                result = preraredKeywords = applicationManager.messageComparer.messagePreparer.processMessageBeforeComparation(result);
                return result;
            }
            else
                return preraredKeywords;
        }
    }
    class Status implements Command{
        @Override
        public String getHelp() {
            return "";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("status"))
                return "Размер базы ответов: " + answers.size() + "\n" +
                        "Файл базы ответов: " + fileReaderAnswerDatabase.getFilePath() + "\n" +
                        "Количество неизвестных сообщений: " + unknownMessages.size() + "\n";
            return "";
        }
    }
    class Save implements Command{
        @Override
        public String getHelp() {
            return "";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("save"))
                return save() + "\n";
            return "";
        }
    }
    class AddSpkPatt implements Command{
        @Override
        public String getHelp() {
            return "[ Добавить ответ на сообщение в базу ]\n" +
                    "---| botcmd addspkpatt сообщение*ответ\n\n";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("addspkpatt")){

                String lastText = commandParser.getText();
                if (lastText.equals(""))
                    return "Ошибка добавления шаблона: не получены сообщения.\n";
                String[] messages = lastText.split("\\*");
                if (messages.length < 2)
                    return "Ошибка сравнения: не получено второе сообщение. Вы точно не забыли поставить * между сообщениями?\n";
                return addToDatabase(messages[0], messages[1]);
            }
            return "";
        }
    }
    class RemSpkPatt implements Command{
        @Override
        public String getHelp() {
            return "[ Удалить ответ на сообщение из базы ]\n" +
                    "---| botcmd remspkpatt ответ\n\n";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("remspkpatt")){
                String lastText = commandParser.getText();
                if (lastText.equals(""))
                    return "Ошибка удаления шаблона: не получены сообщения.\n";
                return removeAnswer(lastText);
            }
            return "";
        }
    }
    class DumpDatabase implements Command{
        @Override
        public String getHelp() {
            return "[ Выгрузить дамп базы данных ]\n" +
                    "---| botcmd dumpdatabase\n\n";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("dumpdatabase")){
                save();
                String result = applicationManager.vkCommunicator.uploadDocument(fileReaderAnswerDatabase.getFile());
                if(result == null)
                    return "Не удалось выгрузить документ на сервер.";
                else
                    return "База ответов: \n" + result;
            }
            return "";
        }
    }
    class ImportDatabase implements Command{
        @Override
        public String getHelp() {
            return "[ Импортировать записи базы данных ]\n" +
                    "---| botcmd importdatabase https://vk.com/doc10299185_358837359\n\n";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("importdatabase")){
                String result = log(". Импорт базы данных...\n");
                int recordsBefore = answers.size();
                String link = commandParser.getWord().split("\\?")[0];
                result += log(". Получена ссылка на документ: " + link + "\n");
                if(!applicationManager.vkCommunicator.isDocumentLink(link)){
                    result += log("! Ошибка. Ссылка не указывает на документ во Вконтакте.\n");
                    return result;
                }

                result += log(". Загрузка файла...\n");
                File downloadedFile = applicationManager.vkCommunicator.downloadDocument(link);
                if(downloadedFile == null){
                    result += log("! Ошибка. Документ не загружен.\n");
                    return result;
                }
                result += log(". Документ загружен: " + downloadedFile.getPath() + "\n");

                result += log(". Чтение файла...\n");
                String downloadedFileText = null;
                try {
                    downloadedFileText = FileReader.readFromFile(downloadedFile.getPath());
                }
                catch (Exception e){
                    result += log("! Ошибка. Текст из файла не может быть прочитан.\n");
                    return result;
                }
                result += log(". Текст из файла прочитан.\n");
                log(downloadedFileText);

                result += log(". Поиск сигнатуры...\n");
                String[] downloadedFileTextLines = downloadedFileText.split("\\\n");
                result += log(". Всего строк: " + downloadedFileTextLines.length + "\n");
                Pattern pattern = Pattern.compile(".+?var src = '([^']+)';");
                String directFileLink = null;
                for (int i = 0; i < downloadedFileTextLines.length; i++) {
                    Matcher matcher = pattern.matcher(downloadedFileTextLines[i]);
                    if (matcher.find())
                    {
                        result += log(". Сигнатура найдена: " + downloadedFileTextLines[i] + "\n");
                        directFileLink = (matcher.group(1));
                        result += log(". Ссылка получена: " + directFileLink + "\n");
                        break;
                    }
                }
                if(directFileLink == null) {
                    result += log("! Ошибка: ссылка на загрузку не получена.\n");
                    return result;
                }

                applicationManager.sleep(1000);

                result += log(". Загрузка файла...\n");
                File downloadedDirectFile = applicationManager.vkCommunicator.downloadDocument(directFileLink);
                if(downloadedDirectFile == null){
                    result += log("! Ошибка. Документ не загружен.\n");
                    return result;
                }
                result += log(". Документ загружен: " + downloadedDirectFile.getPath() + "\n");

                result += log(". Сохранение текущей БД...\n");
                result += save();

                result += log(". Создание резервной копии...\n");
                String databaseBackupFilePath = fileReaderAnswerDatabase.getFile().getPath() + "_backup" + new SimpleDateFormat("yyyy_MM_dd_HH-mm-ss").format(new Date());
                if(!FileReader.copyFile(fileReaderAnswerDatabase.getFile().getPath(),  databaseBackupFilePath)) {
                    result += log("! Ошибка записи резервной копии! \n");
                    return result;
                }
                result += log(". Резервная копия сохранена здесь: " + databaseBackupFilePath + "\n");

                result += log(". Записей в базе сейчас: " + FileReader.countLines(fileReaderAnswerDatabase.getFile().getPath()) + "\n");
                result += log(". Дополнение файла БД...\n");
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(fileReaderAnswerDatabase.getFile(), true);
                    FileInputStream fileInputStream = new FileInputStream(downloadedDirectFile);
                    fileOutputStream.write('\n');
                    int totalTransfered = 0;
                    byte[] buffer = new byte[128];
                    while (fileInputStream.available() > 0){
                        int r = fileInputStream.read(buffer);
                        totalTransfered += r;
                        fileOutputStream.write(buffer, 0, r);
                    }
                    fileOutputStream.close();
                    fileInputStream.close();
                    result += log(". Перенесено байт: " + totalTransfered + "\n");
                }
                catch (Exception e){
                    result += "! Ошибка дополнения файла: " + e.toString();
                    return result;
                }

                result += log(". Загрузка обновленной БД...\n");
                load();
                int recordsAfter = answers.size();//FileReader.countLines(fileReaderAnswerDatabase.getFile().getPath());
                result += log(". Записей в базе загружено: " + recordsAfter + "\n");
                result += log(". Новых записей в базу добавлено: " + (recordsAfter - recordsBefore) + "\n");

                return result;
            }
            return "";
        }
    }
    class DumpUnknownMessages implements Command{
        @Override
        public String getHelp() {
            return "[ Выгрузить дамп базы неизвестных слов ]\n" +
                    "---| botcmd dumpunknownmessages\n\n";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("dumpunknownmessages")){
                save();
                String result = applicationManager.vkCommunicator.uploadDocument(fileUnknownMessages);
                if (result == null)
                    return "Не удалось выгрузить документ на сервер.";
                else
                    return "База неизвестных сообщений: \n" + result;
            }
            return "";
        }
    }
}
