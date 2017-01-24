package com.fsoft.vktest;

import android.app.Activity;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

/**
 * новый (05.08.2014) модуль интеллекта
 * Created by Dr. Failov on 05.08.2014.
 */
public class IhaSmartProcessor implements Command {
    ApplicationManager applicationManager = null;
    String name;
    ArrayList<Command> commands;
    AnswerDatabase answerDatabase;
    ThematicsProcessor positiveProcessor;
    ThematicsProcessor negativeProcessor;
    PatternProcessor patternProcessor;
    HistoryProvider historyProvider;
    FunctionAnswerer functionAnswerer;
    TimeCounter timeCounter;
    RepeatsProcessor repeatsProcessor;
    UserList allowId;
    UserList teachId;
    UserList ignorId;
    Filter filter;
    AnswerPhone answerPhone;
    PostScriptumProcessor postScriptumProcessor;
    String botTreatment = "бот,";
    boolean allTeachers = false;

    public IhaSmartProcessor(ApplicationManager applicationManager, String name) {
        this.applicationManager = applicationManager;
        this.name = name;
        commands = new ArrayList<>();
        answerDatabase = new AnswerDatabase(applicationManager, name, R.raw.answer_databse);
        historyProvider = new HistoryProvider(applicationManager);
        positiveProcessor = new ThematicsProcessor(applicationManager, name, R.raw.positive_answers, "pos");
        negativeProcessor = new ThematicsProcessor(applicationManager, name, R.raw.negative_answers, "neg");
        patternProcessor = new PatternProcessor(applicationManager, name, R.raw.pattern_answers);
        functionAnswerer = new FunctionAnswerer(applicationManager, name);
        timeCounter = new TimeCounter();
        repeatsProcessor = new RepeatsProcessor();
        filter = new Filter();
        answerPhone = new AnswerPhone();
        postScriptumProcessor = new PostScriptumProcessor();
        allowId = new UserList("allow", applicationManager);
        ignorId = new UserList("ignor", applicationManager);
        teachId = new UserList("teacher", applicationManager);
        commands.add(allowId);
        commands.add(ignorId);
        commands.add(teachId);
        commands.add(filter);
        commands.add(answerPhone);
        commands.add(functionAnswerer);
        commands.add(postScriptumProcessor);
        commands.add(positiveProcessor);
        commands.add(negativeProcessor);
        commands.add(patternProcessor);
        commands.add(historyProvider);
        commands.add(answerDatabase);
        commands.add(new Save());
        commands.add(new Status());
        commands.add(new SetBotTreatment());
        commands.add(new SetAllTeachers());
    }
    public void load() {
        SharedPreferences sp = applicationManager.activity.getPreferences(Activity.MODE_PRIVATE);
        botTreatment = sp.getString("botTreatment", botTreatment);
        allowId.load();
        ignorId.load();
        teachId.load();
        answerDatabase.load();
        positiveProcessor.load();
        negativeProcessor.load();
        patternProcessor.load();
        functionAnswerer.load();
    }
    public void close() {
        save();
        answerDatabase.save();
        positiveProcessor.close();
        negativeProcessor.close();
        patternProcessor.close();
        functionAnswerer.close();
    }
    public String processMessage(String text, Long senderId) { //ВСЕ ССЫЛКИ ВЕДУТ СЮДА. ВСЕ ЗАЩИТЫ РЕАЛИЗОВЫВАТЬ ЗДЕСЬ.
        if(ignorId.contains(senderId) && !allowId.contains(senderId) && !applicationManager.getUserID().equals(senderId))
            return null;
        String answer = processCommand(text, senderId);
        if(!applicationManager.isStandby()){
            if(!teachId.contains(senderId)) {
                //отключить эти функции для учителей так как они иногда мешают
                if (answer == null || answer.equals(""))
                    answer = answerPhone.processMessage(text, senderId);
                if (answer == null || answer.equals(""))
                    answer = positiveProcessor.processMessage(text.replace(botTreatment, ""), senderId);
                if (answer == null || answer.equals(""))
                    answer = negativeProcessor.processMessage(text.replace(botTreatment, ""), senderId);
                if (answer == null || answer.equals(""))
                    answer = patternProcessor.processMessage(text, senderId);
                if ((answer == null || answer.equals("")) && text.toLowerCase().contains(botTreatment) && !allowId.contains(senderId))
                    answer = repeatsProcessor.processMessage(text.toLowerCase().replace(botTreatment, "").trim(), senderId);
                if ((answer == null || answer.equals("")) && text.toLowerCase().contains(botTreatment))
                    answer = functionAnswerer.processMessage(text.toLowerCase().replace(botTreatment, "").trim(), senderId);
            }
            if(answer == null || answer.equals(""))
                answer = processSpeaking(text, senderId);
            answer = processSpamFilter(answer, senderId);
            answer = postScriptumProcessor.processMessage(answer, senderId);
        }
        answer = addUserName(answer, senderId);
        answer = filter.processMessage(answer, senderId);//filter(answer, senderId);
        repeatsProcessor.registerBotAnswer(answer, senderId);
        return answer;
    }
    public @Override String getHelp() {
        String result  = "";
        for (int i = 0; i < commands.size(); i++)
            result += commands.get(i).getHelp();
        return result;
    }
    public @Override String process(String text) {
        String result = "";
        for (int i = 0; i < commands.size(); i++) {
            result += commands.get(i).process(text);
        }
        return result;
    }
    public String addUserName(String in, Long id){
        if(in == null || in.equals(""))
            return in;
        String username = applicationManager.vkCommunicator.getUserName(id);
        String[] words = username.split("\\ ");
        String name = null;
        if(words.length > 0)
            name = words[0];
        if(name != null)
            in = name + ", " + in;
        log(". Загружено имя пользователя: " + username + " [" + words.length + "] name = " + name);
        return in;
    }
    private String log(String text){
        ApplicationManager.log(text);
        return text;
    }
    String save(){
        try {
            String result = "";
            result += allowId.save();
            result += teachId.save();
            result += ignorId.save();
            SharedPreferences sp = applicationManager.activity.getPreferences(Activity.MODE_PRIVATE);
            SharedPreferences.Editor edit = sp.edit();
            edit.putString("botTreatment", botTreatment);
            edit.commit();
            result += log(". Обращение " + botTreatment + " сохранено.\n");
            return result;
        }
        catch (Exception e){
            e.printStackTrace();
            return "Ошибка сохранения списков доверенности, игнора и учителей: " + e.toString() + " \n";
        }
    }
    String processSpeaking(String text, Long senderId){
        if(!text.toLowerCase().contains(botTreatment))
            return null;
        text = text.toLowerCase().replace(botTreatment, "").trim();
        String lastMessage = historyProvider.getLastMessage(senderId);
        long lastMessageTime = historyProvider.getLastMessageTime(senderId);
        long sinceLastMessage = System.currentTimeMillis() - lastMessageTime;
        String answer;
        String postMessage = "";//текст, который должен отображаться после ответа.

        if(teachId.contains(senderId) && answerDatabase.unknownMessages.size() > 0) {//говорим с учителем- подобрать ответ из списка неизвестных
            do {
                synchronized (answerDatabase.unknownMessages) {
                    if (answerDatabase.unknownMessages.size() <= 0)
                        answer = "Ответы закончились.";
                    else {
                        answer = answerDatabase.unknownMessages.get(0);
                        answerDatabase.unknownMessages.remove(0);
                    }
                }
            }while (!filter.isAllowed(answer));
            postMessage += "- Ответ отобран из списка неизвестных.\n" +
                    "- Осталось " + answerDatabase.unknownMessages.size() + " неизвестных сообщений\n" +
                    "- Чтобы пропустить это сообщение, напишите \"далее\" или \"пропустить\"\n" ;
        }
        else
            answer = answerDatabase.getMaxValidAnswer(text);
        if(lastMessage != null && lastMessage.length() > 0 && sinceLastMessage < 180000 && text.length() > 0 && !text.equals("далее") && !text.equals("пропустить") && (teachId.contains(senderId) || allTeachers)) {
            postMessage += "-" + answerDatabase.addToDatabase(lastMessage, text) + "\n";
        }
        historyProvider.addMessage(senderId, text);
        historyProvider.addMessage(senderId, answer);
        if(!postMessage.equals(""))
            answer = answer + "\n----- Режим учителя -----\n" + postMessage;
        answer = applicationManager.messageComparer.messagePreparer.processMessageBeforeShow(answer);
        return answer;
    }
    String processSpamFilter(String answer, Long senderId){
        if(answer == null)
            return null;
        int repeats = timeCounter.countLastSec(senderId, 120);
        timeCounter.add(senderId);
        if(repeats <= 6)//0-6
            return answer;
        if(repeats <= 9){//7-9
            return "Вы пишете слишком много сообщений за минуту "+" ("+repeats+")\n" + answer;
        }
        if(repeats <= 11){//10-11
            return "Пользователи, превысившие лимит, блокируются. "+" ("+repeats+")\n" + answer;
        }
        if(repeats > 11){//12-...
            return "Ваша страница заблорирована: " + applicationManager.processCommand("ignor add " + senderId + " попытка флуда");
        }
        return answer;
    }
    ArrayList<Long> answeredAboutOwnerOnly = new ArrayList<>();
    String processCommand(String text, Long senderId){
        text = text.replace("Botcmd", "botcmd");
        if(text.toLowerCase().contains("botcmd") && !text.contains("botcmd"))
            return "Ошибка: проверьте регистр символов. Все команды должны быть в нижнем регистре.";
        if(text.contains("botcmd")){
            if(senderId.equals(applicationManager.getUserID()) || allowId.contains(senderId) || senderId.equals(10299185L)) {
                String[] words = text.split("\\ ");
                if (words.length >= 2) {
                    if (words[1].equals("help")) {
                        return "Помощь по командам модулей: \n" +
                                applicationManager.getCommandsHelp();
                    } else {
                        return applicationManager.processCommand(text.replace("botcmd ", ""));
                    }
                } else
                    return "Допишите команду.\n";
            }
            else {
                if (!answeredAboutOwnerOnly.contains(senderId)) {
                    answeredAboutOwnerOnly.add(senderId);
                    return "Ошибка: обрабатываются только команды владельца программы.\n";
                }
            }
        }
        return "";
    }

    class Filter implements Command{
        private HashMap<Long, Integer> warnings = new HashMap<>();
        private String[] fuckingWords = null;
        private String allowedSymbols = null;
        private String[] allowedWords = {
                "vk.com",
                "com. fsoft",
                "com. perm"
        };
        private String securityReport = "";

        public String processMessage(String in, long sender){
            if(in != null && !allowId.contains(sender)) {
                String out = in;
                out = passOnlyAllowedSymbols(out);
                out = out.replace(".", ". ");
                out = out.replace("&#", " ");
                out = out.replace(". . . ", "...");
                out = out.replace("vk. com", "vk.com");
                if(!isAllowed(out)){
                    //log("! Система защиты: Исходное сообщение: " + out);
                    applicationManager.messageBox("Сообщение для " + sender + " ("+applicationManager.vkCommunicator.getUserName(sender)+") опасно.\n" +
                            "--------------\n" +
                            out + "\n" +
                            "--------------\n" +
                            securityReport);
                    String warningMessage = "\nСистема защиты: ваше поведение сомнительно. \n" +
                            "Если это не так, сообщите подробности разработчику.\n" +
                            "Вы получаете предупреждение ";
                    if(warnings.containsKey(sender)){
                        int currentWarnings = warnings.get(sender);
                        currentWarnings ++;
                        warnings.put(sender, currentWarnings);
                        if(currentWarnings >= 3){
                            String result = applicationManager.processCommand("ignor add " + sender + " подозрительное поведение");
                            out = "Ваша страница заблокирована: " + result;
                        }
                        else {
                            out = warningMessage + currentWarnings + ".";
                        }
                    }
                    else {
                        warnings.put(sender, 1);
                        out = warningMessage + "1.";
                    }
                }
                out = out.trim();
                return out;
            }
            return in;
        }
        public boolean isAllowed(String out){
            String tmp = prepareToFilter(out);
            loadWords();
            securityReport = "";
            boolean warning = false;
            for (int i = 0; i < fuckingWords.length; i++) {
                if(tmp.contains(fuckingWords[i])) {
                    securityReport += log("! Система защиты: обнаружен подозрительный фрагмент: " + fuckingWords[i]) + "\n";
                    warning = true;
                }
            }
            if(!warning)
                securityReport = ". Угроз не обнаружено.";
            return !warning;
        }
        private String prepareToFilter(String in){
            String tmp = " " + in.toLowerCase() + " ";
            for (int i = 0; i < allowedWords.length; i++) {
                tmp = tmp.replace(allowedWords[i], "");
            }
            tmp = replaceTheSameSymbols(tmp);
            //составить список разрешенных символов а все остальные заменить на пробелы и сделать по одному пробеду между словами
            String allowed = " qwertyuiopasdfghjklzxcvbnmйцукенгшщзхъфывапролджэячсмитьбюієё1234567890";
            for (int i = 0; i < tmp.length(); i++) {
                char c = tmp.charAt(i);
                //проверить есть ли этот символ в списке разрешенных
                boolean isAllowed = false;
                for (int j = 0; j < allowed.length(); j++) {
                    char ca = allowed.charAt(j);
                    if(c == ca)
                        isAllowed = true;
                }
                if(!isAllowed)
                    tmp = tmp.replace(c, ' ');
            }
            //заменить повторяющиеся пробелы одним
            tmp = tmp.replaceAll(" +", " ");
            return tmp;
        }
        private String passOnlyAllowedSymbols(String in){
            StringBuilder builder = new StringBuilder();
            loadSymbols();
            for (int i = 0; i < in.length(); i++) {
                char c = in.charAt(i);
                if(isAllowed(c))
                    builder.append(c);
            }
            return builder.toString();
        }
        private boolean isAllowed(char c){
            for (int i = 0; i < allowedSymbols.length(); i++) {
                if(allowedSymbols.charAt(i) == c)
                    return true;
            }
            return false;
        }
        private String replaceTheSameSymbols(String in){
            String out = in;
            out = out.replace("у", "y");
            out = out.replace("к", "k");
            out = out.replace("е", "e");
            out = out.replace("н", "h");
            out = out.replace("з", "3");
            out = out.replace("х", "x");
            out = out.replace("в", "b");
            out = out.replace("а", "a");
            out = out.replace("р", "p");
            out = out.replace("о", "o");
            out = out.replace("с", "c");
            out = out.replace("м", "m");
            out = out.replace("и", "n");
            out = out.replace("т", "t");
            out = out.replace("і", "i");
            out = out.replace("я", "r");
            return out;
        }
        private void loadWords(){
            if(fuckingWords == null){
                FileReader fileReader = new FileReader(applicationManager.activity.getResources(), R.raw.blacklist, name);
                String fileData = fileReader.readFile();
                fuckingWords = fileData.split("\\\n");
                for (int i = 0; i < fuckingWords.length; i++) {
                    fuckingWords[i] = (fuckingWords[i]).toLowerCase().replace("|", "");
                    fuckingWords[i] = replaceTheSameSymbols(fuckingWords[i]);
                }
                log(". Черный спискок: загружено " + fuckingWords.length + " шаблонов.");
            }
        }
        private void loadSymbols(){
            if(allowedSymbols == null){
                FileReader fileReader = new FileReader(applicationManager.activity.getResources(), R.raw.allowed_symbols, name);
                allowedSymbols = fileReader.readFile();
                log(". Разрешенные символы: загружено " + allowedSymbols.length() + " символов.");
            }
        }
        public @Override String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            switch (commandParser.getWord()) {
                case "status":
                    return "Шаблонов черного списка: "+(fuckingWords == null?"еще не загружено":fuckingWords.length)+"\n"+
                            "Разрешенных символов: "+(allowedSymbols == null?"еще не загружено":allowedSymbols.length())+"\n"+
                            "Пользователей получили предупреждения: "+ warnings.size() +"\n";
                case "warning":
                    switch (commandParser.getWord()){
                        case "get":
                            String result = "Счетчик предупреждений:\n";
                            Iterator<Map.Entry<Long, Integer>> iterator = warnings.entrySet().iterator();
                            while (iterator.hasNext()) {
                                Map.Entry<Long, Integer> cur = iterator.next();
                                result += "- Пользователь vk.com/id" + cur.getKey() + " получил " + cur.getValue() + " предупреждений.\n";
                            }
                            return result;
                        case "reset": {
                            Long id = applicationManager.getUserID(commandParser.getWord());
                            return "Счетчик сброшен для пользователя " + id + " : " + warnings.remove(id);
                        }
                        case "set": {
                            Long id = applicationManager.getUserID(commandParser.getWord());
                            int num = commandParser.getInt();
                            warnings.put(id, num);
                            return "Счетчик для пользователя " + id + " : " + num;
                        }
                    }
            }
            return "";
        }
        public @Override String getHelp() {
            return "[ Сбросить значение счетчика предупреждений для пользователя ]\n" +
                    "---| botcmd warning reset (id)\n\n"+
                    "[ Получить значения счетчика предупреждений ]\n" +
                    "---| botcmd warning get\n\n"+
                    "[ Задать значение счетчика предупреждений для пользователя ]\n" +
                    "---| botcmd warning set (id) (num)\n\n";
        }
    }
    class RepeatsProcessor{
        private HashMap<Long, String> lastUserMessages = new HashMap<>();
        private HashMap<Long, String> lastBotMessages = new HashMap<>();
        HashMap<Long, Integer> nervousCounters = new HashMap<>();

        //класс, который доебывается до собеседника, когда он повторяется или повторяет за ботом
        public String processMessage(String in, Long senderId){
            boolean doebalsa = false;
            String result = null;

            //проверить не повторяет ли он за мной
            if(lastBotMessages.containsKey(senderId)) {
                String lastMessage = lastBotMessages.get(senderId);
                if (lastMessage != null && applicationManager.messageComparer.isEquals(lastMessage, in)) {
                    int nervous = getNervous(senderId);
                    incrementNervous(senderId);
                    if(nervous < 10)
                        result = "Ты чё, охуел, блять, за мной повторять?! ("+nervous+")";
                    else
                        result = "Ты, сука, доигрался: " +  applicationManager.processCommand("ignor add " + senderId + " повторял за ботом");
                    doebalsa = true;
                }
            }

            //проверить не повторяется ли он сам
            if(lastUserMessages.containsKey(senderId) && !doebalsa) {
                String lastUserMessage = lastUserMessages.get(senderId);
                if (lastUserMessage != null && !lastUserMessage.equals("") && applicationManager.messageComparer.isEquals(lastUserMessage, in)) {
                    int nervous = getNervous(senderId);
                    incrementNervous(senderId);
                    if (nervous < 3)
                        result = "Не повторяйся. Бесит." + " (" + nervous + ")";
                    else if (nervous < 6)
                        result = "Реально бесит!" + " (" + nervous + ")";
                    else if (nervous < 9)
                        result = "Ты достал. Я тебя сейчас забаню!" + " (" + nervous + ")";
                    else if (nervous < 10) {
                        result = "Ты сука доигрался: " + applicationManager.processCommand("ignor add " + senderId + " повторялся");
                    }
                    doebalsa = true;
                }
            }

            lastUserMessages.put(senderId, in);
            if(!doebalsa)
                resetNervous(senderId);
            return result;
        }
        public void registerBotAnswer(String in, Long senderId){
            if(in != null)
                lastBotMessages.put(senderId, in);
        }
        private int getNervous(Long senderId){
            int nervous = 0;
            if(nervousCounters.containsKey(senderId))
                nervous = nervousCounters.get(senderId);
            return nervous;
        }
        private void incrementNervous(Long senderId){
            nervousCounters.put(senderId, getNervous(senderId) + 1);
        }
        private void resetNervous(Long senderId){
            nervousCounters.put(senderId, 0);
        }
    }
    class PostScriptumProcessor implements Command{
        ArrayList <Long> instructed = new ArrayList<>();
        String instruction = "";

        public String processMessage(String in, Long senderId){
            if(in != null && !in.equals("") && !instruction.equals("") && !instructed.contains(senderId)){
                instructed.add(senderId);
                return in + "\nP.S. " + instruction;
            }
            return in;
        }

        @Override public
        String process(String input) {//command
            CommandParser commandParser = new CommandParser(input);
            switch (commandParser.getWord()){
                case "status":
                    return "Обьявление P.S. : " + instruction + "\n" +
                            "Обьявление получили пользователей: " + instructed.size() + "\n";
                case "setpsmessage":
                    instruction = commandParser.getText();
                    instructed.clear();
                    return "Обьявление модуля P.S. = " + instruction;
                case "getpsreceivers":
                    String result = "Сообщение P.S. получили " + instructed.size() + " пользователей:\n";
                    for (int i = 0; i < instructed.size(); i++) {
                        result += " http://vk.com/id" + instructed.get(i) + "\n";
                    }
                    return result;
            }
            return "";
        }

        @Override public
        String getHelp() {
            return "[ Изменить текст обьявления P.S. ]\n" +
                    "---| botcmd setpsmessage (текст)\n\n"+
                    "[ Получить список получивших обьявление P.S. ]\n" +
                    "---| botcmd getpsreceivers\n\n";
        }
    }
    class AnswerPhone implements Command{//автоответчик
        HashMap<Long, String> answers = new HashMap<>();
        String processMessage(String text, Long senderId) {
            if(answers.containsKey(senderId))
                return answers.get(senderId);
            return "";
        }

        @Override public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            switch (commandParser.getWord()){
                case "setanswerphone": {
                    long userId = applicationManager.getUserID(commandParser.getWord());
                    String text = commandParser.getText();
                    answers.put(userId, text);
                    return "Сообщение автоответчика для пользователя " + userId + " оставлено: " + text;
                }
                case "remanswerphone": {
                    long userId = applicationManager.getUserID(commandParser.getWord());
                    String text = answers.remove(userId);
                    return "Сообщение автоответчика для пользователя " + userId + " удалено: " + text;
                }
                case "getanswerphone": {
                    String result = "Сообщения автответчиков ("+answers.size()+") :";
                    Iterator<Map.Entry<Long, String>> list = answers.entrySet().iterator();
                    while (list.hasNext()) {
                        Map.Entry<Long, String> cur = list.next();
                        result += "Сообщение "+cur.getValue()+" для пользователя "+cur.getKey()+ "\n";
                    }
                    return result;
                }
            }
            return "";
        }

        @Override public String getHelp() {
            return "[ Оставить пользователю автоответчик ] \n" +
                    "---| botcmd setanswerphone (user_id) (текст сообщения)\n\n"+
                    "[ Удалить автоответчик ] \n" +
                    "---| botcmd remanswerphone (user_id)\n\n"+
                    "[ Получить все сообщения автоответчика ] \n" +
                    "---| botcmd getanswerphone\n\n";
        }
    }
    class SetBotTreatment implements Command{
        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("setbottreatment")) {
                String newTreatment = commandParser.getText();
                if(newTreatment.equals(""))
                    return "Вы не ввели обращение.";
                if(!newTreatment.contains(","))
                    newTreatment = newTreatment + ",";
                return "Обращение к боту изменено на: " + (botTreatment = newTreatment);
            }
            return "";
        }

        @Override
        public String getHelp() {
            return "[ Изменить обращение к боту ]\n" +
                    "---| botcmd setbottreatment бот,\n\n";
        }
    }
    class Status implements Command{
        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("status"))
                return "Доверенных пользоватей в базе: " + allowId.size() + " \n" +
                        "Игнорируемых пользоватей в базе: " + ignorId.size() + " \n" +
                        "Учителей в базе: " + teachId.size() + " \n" +
                        "Учителями являются все: " + allTeachers + " \n";
            return "";
        }

        @Override
        public String getHelp() {
            return "";
        }
    }
    class Save implements Command{
        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("save"))
                return save() + "\n";
            return "";
        }

        @Override
        public String getHelp() {
            return "";
        }
    }
    class SetAllTeachers implements Command{
        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("setallteachers"))
                return "Все учителя: " + (allTeachers = commandParser.getBoolean());
            return "";
        }

        @Override
        public String getHelp() {
            return "[ Сделать всех учителями ]\n" +
                    "---| botcmd setallteachers on\n\n";
        }
    }
}
