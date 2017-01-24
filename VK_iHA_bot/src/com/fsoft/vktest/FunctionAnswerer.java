package com.fsoft.vktest;

import android.provider.UserDictionary;
import android.text.format.DateFormat;
import com.perm.kate.api.WallMessage;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * отвечает на конкретные вопросы
 * Created by Dr. Failov on 02.10.2014.
 */
public class FunctionAnswerer implements Command{
    ApplicationManager applicationManager = null;
    String name;
    ArrayList<Function> functions = new ArrayList<>();

    public FunctionAnswerer(ApplicationManager applicationManager, String name) {
        this.applicationManager = applicationManager;
        this.name = name;
        functions.add(new Anonymous());
        functions.add(new Say());
        functions.add(new Cities());
        functions.add(new IliIli());
        functions.add(new Infa());
        functions.add(new When());
        functions.add(new Time());
        functions.add(new BanMe());
        functions.add(new WhichDayOfMonth());
        functions.add(new WhichDayOfWeek());
        functions.add(new Rendom());
        functions.add(new Cities());
        functions.add(new PseudoGraphic());
        functions.add(new MyMessages());
        functions.add(new MathSolver());
    }
    public String processMessage(String text, Long senderId) {
        for (int i = 0; i < functions.size(); i++) {
            String result = functions.get(i).process(text, senderId);
            if(result != null && !result.equals(""))
                return result;
        }
        return null;
    }
    public void load(){}
    public void close(){}
    public @Override  String process(String input) {
        String result =  "";
        for (int i = 0; i < functions.size(); i++) {
            result += functions.get(i).process(input);
        }
        return result;
    }
    public @Override String getHelp() {
        String result = "";
        for (int i = 0; i < functions.size(); i++) {
            result += functions.get(i).getHelp();
        }
        return result;
    }
    private String log(String text){
        ApplicationManager.log(text);
        return text;
    }
    private boolean isPositive(String message){
        return applicationManager.messageProcessor.positiveProcessor.patterns.match(message);
    }
    private boolean isNegative(String message){
        return applicationManager.messageProcessor.negativeProcessor.patterns.match(message);
    }


    class Function implements Command{
        //Получить справку по командам этой функции. Если команд нет, вернуть "".
        @Override public String getHelp() {
            return "";
        }

        //Обработать команду. На вход поступает сразу фрагмент без botcmd, напимер: "wall add drfailov".
        @Override public String process(String input) {
            return "";
        }

        //Обработать входящее сообщение. Сюда поступают не только сообщения на которые этот модуль может
        // дать ответ, а все. Поэтому если этот модуль не должен отвечать на него, вернуть "".
        //Если ответ есть - писать сразу ответ. С маленькой буквы.
         String process (String text, Long senderId){
            return "";
        }
    }
    class MyMessages extends Function{
        private VkCommunicator vkCommunicator;
        private HashMap<Long, Integer> userUsages = new HashMap<>();

        @Override String process(String input, Long senderId) {//функция
            CommandParser commandParser = new CommandParser(input);   //     Бот,      записи пользователя https://vk.com/dogiedog228 на стене  https://vk.com/wall-76425828
            if(commandParser.getWord().equals("записи") && commandParser.getWord().equals("пользователя")) {
                if(senderId <= 0)
                    return "У Вашей страницы неправильный ID.";
                String result = "";
                try {
                    String userLink = commandParser.getWord();
                    result += log(". Получение ссылки на пользователя: " + userLink + "\n");   //https://vk.com/dogiedog228
                    long userId = applicationManager.getUserID(userLink);
                    result += log(". Получен ID пользователя: " + userId + "\n");

                    if(commandParser.getWord().equals("на") && commandParser.getWord().equals("стене")) {
                        String wallLink = commandParser.getWord();
                        result += log(". Получение ссылки на стену: " + wallLink + "\n");       //https://vk.com/wall-76425828
                        wallLink = wallLink.replace("http://vk.com/wall", "");
                        wallLink = wallLink.replace("https://vk.com/wall", "");
                        long wallId = Long.parseLong(wallLink);
                        result += log(". Получен ID стены: " + wallId + "\n");

                        if(userId != -1 && wallId != 0){
                            int usages = 0;
                            int max_usages = 5;
                            if(userUsages.containsKey(senderId))
                                usages = userUsages.get(senderId);
                            result += log(". Получен номер поиска: " + usages + "\n");

                            if(usages < max_usages) {
                                int offset = commandParser.getInt();
                                startFinding(userId, senderId, wallId, offset);
                                result += log(". Запущен поиск записей пользователя "+userId+" на стене "+wallId+" со сдвигом "+offset+". Эта процедура достаточно длительная. " +
                                        "Вы будете получать сообщения от бота по мере обнаружения записей. Когда поиск закончится, Вы также будете проинформированы.\n");
                                if(max_usages - usages < 3)
                                    result += log(". Осталось поисков: "+(max_usages - usages)+".\n");

                                usages++;
                                userUsages.put(senderId, usages);
                            }
                            else {
                                result += log(". Вы превысили лимит поисков в данной сессии.\n");
                            }
                        }
                        else
                            result += log(". Ошибка в ссылках. Проверьте их хорошенько.\n");
                    }
                    else
                        result += log(". Вы допустили ошибку в синтаксисе.");
                }
                catch (Exception e){
                    e.printStackTrace();
                    result += log(". Ошибка выполнения операции: " + e.toString());
                }
                return result;
            }

            return super.process(input, senderId);
        }

        @Override public String process(String input) {//команды
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("status")){
                String result = "Статистика использования функции \"Сообщения на стене пользователя\":\n";
                Iterator<Map.Entry<Long, Integer>> iterator = userUsages.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Long, Integer> cur = iterator.next();
                    result += "- Пользователь vk.com/id" + cur.getKey() + " выполнил " + cur.getValue() + " поисков.\n";
                }
                return result;
            }
            return super.process(input);
        }

        void startFinding(final long targetUserId, final long senderId, final long targetWallId, final int offset_in){
            vkCommunicator = applicationManager.vkCommunicator;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int offset = offset_in;
                    int count = 90;
                    int foundCount = 0;
                    ArrayList<WallMessage> foundMessages = new ArrayList<>();
                    try {
                        while(true) {
                            log(". Поиск записей " +
                                    " targetWallId = " + targetWallId +
                                    " targetUserId = " + targetUserId +
                                    " count = " + count +
                                    " offset = " + offset);
                            ArrayList<WallMessage> wallMessages = vkCommunicator.getWallMessages(targetWallId, count, offset, "");
                            if(wallMessages.size() == 0){
                                if(foundMessages.size() != 0)
                                    informate(ApplicationManager.botMark() + "Найдены записи:" +
                                        "\noffset = " + offset + "\n\n" + getLinks(foundMessages, targetWallId), senderId);
                                informate(log(". Всего найдено "+foundCount+" записей. Поиск завершен. "), senderId);
                                break;
                            }
                            for (int i = 0; i < wallMessages.size(); i++) {
                                WallMessage wallMessage = wallMessages.get(i);
                                if(wallMessage.from_id == targetUserId){
                                    foundCount ++;
                                    foundMessages.add(wallMessage);
                                    if(foundMessages.size() > 29){
                                        informate("Найдены записи:" +
                                                "\noffset = " + offset + "\n\n" + getLinks(foundMessages, targetWallId), senderId);
                                        foundMessages.clear();
                                    }
                                }
                            }
                            Thread.sleep(1000);
                            offset += count;
                        }
                    }
                    catch (Exception e){
                        if(foundMessages.size() != 0)
                            informate("Найдены записи:" +
                                "\noffset = " + offset + "\n\n" + getLinks(foundMessages, targetWallId), senderId);
                        informate(log(". Всего найдено "+foundCount+" записей. Поиск завершен по причине " + e.toString()), senderId);
                    }
                }
            }).start();
        }
        String getLinks(ArrayList<WallMessage> foundMessages, long targetWallId){
            String result = "";
            for (int i = 0; i < foundMessages.size(); i++) {
                result += "http://vk.com/wall" + targetWallId + "_" + foundMessages.get(i).id + "\n";
            }
            return result;
        }
        void informate(String text, long senderId){
            vkCommunicator.sendMessage(senderId, text);
        }
    }
    class Anonymous extends Function{
        HashMap<Long, Integer> messagesSent = new HashMap<>();
        int sentMaximum = 20;

        @Override
        String process(String text, Long senderId) {
            String[] words = text.split("\\ ");
            if(words.length > 2 && words[0].toLowerCase().equals("анонимно")){//Бот, анонимно drfailov Бот охуенен!
                String result = "";
                result += log(". Отправка анонимного сообщения...\n");
                String name = words[1];
                result += log(". Получено имя пользователя: "+name+"\n");
                long id = applicationManager.getUserID(name);
                result += log(". Получено id пользователя: "+id+"\n");
                if(id < 2)
                    result += log("! id некорректен.\n");
                else if(id == 100 || id == 101 || id == 333)
                    result += log("! В пизду себе отправь такое сообщение, а не агенту поддержки!.\n");
                else {
                    String message = text.replace(words[0], "").replace(words[1], "").trim();
                    result += log(". Получен текст сообщения: " + message + "\n");
                    message = applicationManager.messageProcessor.filter.processMessage(message, senderId);
                    result += log(". Текст сообщения после фильтрации: " + message + "\n");
                    if (!message.contains("Вы получаете предупреждение")) {//если сообщение прошло проверку
                        int sent = 0;
                        if (messagesSent.containsKey(senderId))
                            sent = messagesSent.get(senderId);
                        if (sent < sentMaximum) {
                            message = ApplicationManager.botMark() + "Вы получили анонимное сообщение: " + message + "\n" +
                                    "--------------------\n" +
                                    "Не нужно отвечать на это сообщение, т.к. отправитель не сможет прочитать Ваш ответ.";
                            result += applicationManager.vkCommunicator.sendMessage(id, message);
                            sent++;
                            messagesSent.put(senderId, sent);
                            int remaining = (sentMaximum - sent);
                            if (remaining < 5)
                                result += log(". Осталось сообщений: " + remaining + "\n");
                        } else
                            result += log(". Вы отправили уже слишком много сообщений за эту сессию.\n" +
                                    "! Сообщение не отправлено.\n");
                    } else {
                        result = message + "\n " + log("! Сообщение не отправлено.\n");
                    }
                }
                return result;
            }
            return super.process(text, senderId);
        }

        @Override public String process(String input) {//команды
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("status")){
                String result = "Статистика отправки анонимных сообщений:\n";
                Iterator<Map.Entry<Long, Integer>> iterator = messagesSent.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Long, Integer> cur = iterator.next();
                    result += "- Пользователь vk.com/id" + cur.getKey() + " отправил " + cur.getValue() + " сообщений.\n";
                }
                return result;
            }
            return super.process(input);
        }
    }
    class Infa extends Function{
        @Override
        String process(String text, Long senderId) {
            String[] words = text.split("\\ ");
            if(words.length > 1 && words[0].toLowerCase().equals("инфа")){
                int infa = (int)(System.currentTimeMillis()%101);
                text = text.replace("инфа", "");
                if(isNegative(text))
                    infa = 0;
                else if(isPositive(text))
                    infa = 100;
                return "Инфа \"" + text + "\" : " + infa + "%.";
            }
            return super.process(text, senderId);
        }
    }
    class IliIli extends Function{
        @Override
        String process(String text, Long senderId) {
            if(text.contains(" или ")){
                text = text.replace("!", "");
                text = text.replace("?", "");
                text = text.replace("что лучше", "");
                text = text.replace("что круче", "");
                text = text.replace("что купить", "");
                text = text.replace("что выбрать", "");
                text = text.replace("выбери", "");
                text = text.replace(":", "");
                text = text.replace(",", "");
                MessageComparer comparer = applicationManager.messageComparer;
                Random random = new Random(System.currentTimeMillis());
                String[] vars = text.split("или");
                log("vars = " + vars.length);
                if(vars.length < 2){
                    return "или что?";
                }
                //проверка на говно
                String[] shit = new String[]{
                        "айфон",
                        "айпад",
                        "айтюнс",
                        "макбук",
                        "айос",
                        "айпод",
                        "эппл",
                        "говно",
                        "консоль",
                        "xbox",
                        "playstation",
                        "аниме",
                        "путин"
                };
                String shitDetected = null;
                for (int i = 0; i < vars.length; i++) {
                    for (int j = 0; j < shit.length; j++) {
                        if(comparer.isEquals(vars[i], shit[j]))
                            shitDetected = vars[i];
                    }
                }
                if(shitDetected != null)
                    return "Однозначно, " + shitDetected + " - говно.";
                //--------------случайный выбор
                int var = random.nextInt(vars.length);
                return "Мой выбор: " + vars[var];
            }
            return super.process(text, senderId);
        }
    }
    class Say extends Function{
        @Override
        String process(String text, Long senderId) {
            String[] words = text.split("\\ ");
            if(words.length > 1 && words[0].toLowerCase().equals("скажи")){
                text = text.replace("скажи ", "");
                text = text.replace("?", "");
                if(isNegative(text))
                    return "Я не буду этого говорить!";
                return text;
            }
            return super.process(text, senderId);
        }
    }
    class When extends Function{
        @Override
        String process(String text, Long senderId) {
            String[] words = text.split("\\ ");
            text = text.replace("?", "").replace("!", "");

            if(words.length > 1 && words[0].toLowerCase().equals("когда")){
                text = text.replace("когда ", "");
                Random random = new Random();
                switch (random.nextInt(4)) {
                    case 0:
                        return "Через " + random.nextInt(35) +" дней " + text + ".";
                    case 1:
                        return "Через " + random.nextInt(10) +" месяцев " + text + ".";
                    case 2:
                        return (random.nextInt(28)+1) + "." + (random.nextInt(12)+1) + "." + (random.nextInt(90) + Calendar.getInstance().getTime().getYear() + 1900) + " случится " + text + ".";
                    case 3:
                        return "\"" + text + "\" не случится никогда.";
                }
                return text;
            }
            return super.process(text, senderId);
        }
    }
    class Time extends Function{
        @Override
        String process(String text, Long senderId) {
            MessageComparer comparer = applicationManager.messageComparer;
            if(comparer.isEquals(text, "который час")
                    || comparer.isEquals(text, "сколько времени")
                    || comparer.isEquals(text, "какой час")
                    || comparer.isEquals(text, "какое время")
                    || comparer.isEquals(text, "текущее время")
                    || comparer.isEquals(text, "время")
                    || comparer.isEquals(text, "сколько сейчас")
                    || comparer.isEquals(text, "точное время") ){
                return "По моим данным, текущее время: " + new SimpleDateFormat("HH:mm:ss").format(new Date()) + ".";
            }
            return super.process(text, senderId);
        }
    }
    class BanMe extends Function{
        @Override
        String process(String text, Long senderId) {
            if(text.contains("забань меня")){
                String result = applicationManager.processCommand("ignor add " + senderId + " по собственному желанию");
                return "Ок, " + result;
            }
            return super.process(text, senderId);
        }
    }
    class WhichDayOfWeek extends Function{
        @Override
        String process(String text, Long senderId) {
            MessageComparer comparer = applicationManager.messageComparer;
            if(comparer.isEquals(text, "какой день")
                    || comparer.isEquals(text, "какой день недели")
                    || comparer.isEquals(text, "день")
                    ){
                Calendar calendar = Calendar.getInstance();
                return "По поим данным, текущий день недели: " + calendar.get(Calendar.DAY_OF_WEEK) + ".";
            }
            return super.process(text, senderId);
        }
    }
    class WhichDayOfMonth extends Function{
        @Override
        String process(String text, Long senderId) {
            MessageComparer comparer = applicationManager.messageComparer;
            if(comparer.isEquals(text, "какое сейчас число")
                    || comparer.isEquals(text, "какое сегодня число")
                    || comparer.isEquals(text, "число")
                    ){
                Calendar calendar = Calendar.getInstance();
                return "По поим данным, сейчас " + calendar.get(Calendar.DAY_OF_MONTH) + " число.";
            }
            return super.process(text, senderId);
        }
    }
    class Rendom extends Function{
        @Override
        String process(String text, Long senderId) {
            CommandParser commandParser = new CommandParser(text);
            if(commandParser.getWord().equals("рандом")){
                int min=0;
                int max = 2000000;
                java.util.Random random = new java.util.Random();
                switch (commandParser.wordsRemaining()){
                    case 1:
                        max = commandParser.getInt();
                        break;
                    case 2:
                        min = commandParser.getInt();
                        max = commandParser.getInt();
                        break;
                }
                int num = Math.min(min, max) + (random.nextInt(Math.abs(max - min)));
                return "Случайное число: "+num+".";
            }
            return super.process(text, senderId);
        }
    }
    class Cities extends Function{
        FileReader fileReader = new FileReader(applicationManager.activity.getResources(), R.raw.cities_database, name);
        String[] cities = null;
        HashMap<Long, ArrayList<String>> gameHistory = new HashMap<>();

        @Override String process(String textIn, Long senderId) {
            if(gameHistory.containsKey(senderId)){
                String text = prepareCityName(textIn);
                if(text.toLowerCase().trim().equals("стоп игра") ||
                        text.toLowerCase().contains("конец игры")||
                        text.toLowerCase().contains("закончить игру")||
                        text.toLowerCase().contains("завершить игру")||
                        text.toLowerCase().contains("я не играю")||
                        text.toLowerCase().contains("хватит играть")||
                        text.toLowerCase().contains("иди нахуй")||
                        text.toLowerCase().contains("пошел нахуй")||
                        text.toLowerCase().contains("иди в жопу")||
                        text.toLowerCase().contains("пошел в жопу")){
                    ArrayList<String> historyList = gameHistory.get(senderId);
                    int played = historyList.size();
                    gameHistory.remove(senderId);
                    return "игра закончена! С тобой приятно было играть. Мы сыграли " + played + " городов.";
                }
                if(cities == null)
                    initCities();
                if(isCity(text)){
                    ArrayList<String> historyList = gameHistory.get(senderId);
                    if(historyList.contains(text))
                        return "было уже.";
                    else {
                        if(historyList.size() > 0) {
                            char lastWordLastLetter = getLastLetter(historyList.get(historyList.size() - 1));
                            if (lastWordLastLetter != getFirstLetter(text))
                                return "неправильно. Тебе сейчас нужно говорить город на букву \"" + lastWordLastLetter + "\".";
                        }
                        historyList.add(text);
                        char lastLetter = getLastLetter(text);
                        String result = getNextCityOnLetter(lastLetter, senderId);
                        historyList.add(result);
                        result = applicationManager.messageComparer.messagePreparer.makeBeginWithUpper(result.trim());
                        return result;
                    }
                }
                else
                    return "я не знаю такого города. Попробуй какой-нибудь другой.";
            }
            if(textIn.toLowerCase().contains("города")){
                ArrayList<String> historyList = new ArrayList<>();
                gameHistory.put(senderId, historyList);
                return "ну давай сыграем в \"Города\"! Правила простые: нужно назвать город на последнюю букву предыдущего названого города." +
                        " Начинай ты! Называй город.\n" +
                        " Чтобы закончить игру, набери \"стоп игра\" или \"конец игры\" или \"закончить игру\".";
            }
            return super.process(textIn, senderId);
        }
        private void initCities(){
            cities = fileReader.readFile().split("\\\n");
            for (int i = 0; i < cities.length; i++) {
                cities[i] = prepareCityName(cities[i]);
            }
        }
        private String prepareCityName(String in){
            return (" " + in + " ").toLowerCase().replace("!", " ").replace("?", " ").replace("-", " ").replace(",", " ").replace(".", " ").replace("_", " ").replaceAll(" +", " ");
        }
        private boolean isCity(String in){
            if(in.length() < 4)
                return false;
            for (int i = 0; i < cities.length; i++) {
                if(cities[i].contains(in))
                    return true;
            }
            return false;
        }
        private char getLastLetter(String in){
            for (int i = in.length()-1; i >= 0; i--) {
                if(in.charAt(i) != ' ' && in.charAt(i) != '.' && in.charAt(i) != 'ы' && in.charAt(i) != 'ь' && in.charAt(i) != 'ё' && in.charAt(i) != 'ъ')
                    return in.charAt(i);
            }
            return 'a';
        }
        private char getFirstLetter(String in){
            for (int i = 0; i < in.length(); i++) {
                if(in.charAt(i) != ' ')
                    return in.charAt(i);
            }
            return 'a';
        }
        private String getNextCityOnLetter(char letter, long sender){
            //загрузить говрода на букву
            ArrayList<String> correctCities = new ArrayList<>();
            for (int i = 0; i < cities.length; i++) {
                if(getFirstLetter(cities[i]) == letter)
                    correctCities.add(cities[i]);
            }
            //удалить города которые были уже
            ArrayList<String> historyList = gameHistory.get(sender);
            for (int i = 0; i < historyList.size(); i++) {
                if(correctCities.contains(historyList.get(i)))
                    correctCities.remove(historyList.get(i));
            }
            if(correctCities.size() == 0)
                return "На эту букву больше городов нет. Потому давай что нидудь на букву н.";
            //выбрать случайным образом
            return correctCities.get(new Random().nextInt(correctCities.size()));
        }
    }
    class PseudoGraphic extends Function{
        ArrayList<Symbol> symbols = null;
        char filled;
        char empty;

        private String[] getLetter(char c){
            if(symbols == null)
                loadLetters();

            for (int i = 0; i < symbols.size(); i++)
                if(symbols.get(i).isIt(c))
                    return symbols.get(i).getLines();

            return new String[]{
                    "     ",
                    "     ",
                    "     ",
                    "     ",
                    "     "
            };
        }
        private String getText(String text){
            //init mas
            String[] fullSizeText = new String[5];
            for (int j = 0; j < fullSizeText.length; j++)
                fullSizeText[j] = "";

            //build 5-height text
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                String[] letter = getLetter(c);
                for (int j = 0; j < fullSizeText.length; j++)
                    fullSizeText[j] += " " + letter[j];
            }

            //build half-size text



            for (int j = 0; j < fullSizeText.length; j++)
                fullSizeText[j] = fullSizeText[j].replace(' ', empty).replace('#', filled);
            String out = "\n";
            for (int i = 0; i < fullSizeText.length; i++) {
                out += fullSizeText[i] + "\n";
            }
            return out;
        }
        private void loadLetters(){
            symbols = new ArrayList<>();
            FileReader fileReader = new FileReader(applicationManager.activity.getResources(), R.raw.symbols_database, name);
            String text = fileReader.readFile();
            String[] letters = text.split("NS:");
            for (int i = 0; i < letters.length; i++) {
                String[] lines = letters[i].split("\\\n");
                if(lines.length >= 6){
                    symbols.add(new Symbol(lines[0], new String[]{
                            lines[1],
                            lines[2],
                            lines[3],
                            lines[4],
                            lines[5],
                    }));
                }
            }
        }
        @Override String process(String in, Long senderId) {
            CommandParser commandParser = new CommandParser(in);
            if(commandParser.getWord().toLowerCase().equals("напиши")){
                filled = '█';
                empty = '─';
                String text = commandParser.getText().toLowerCase();
                Pattern pattern = Pattern.compile("(символы(..) )");
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String found = matcher.group(1);
                    String symbols = matcher.group(2);
                    text = text.replace(found, "");
                    filled = symbols.charAt(0);
                    empty = symbols.charAt(1);
                }
                if(!applicationManager.messageProcessor.filter.isAllowed(text))
                    return "Сообщение " + text + " не может быть показано.";
                if(text.length() < 1)
                    return "Вы забыли написать сообщение после слова \"напиши\".";
                if(text.length() > 5000)
                    return "Слишком длинное сообщение";
                if(isNegative(text))
                    return "Я не буду этого писать!";
                String[] line = text.split("\\\\n");
                String result = "Текст " + text.replace("\\n", " ") + " : ";
                for (int i = 0; i < line.length; i++) {
                    result +=getText(line[i]) + "\n";
                }
                return result;
            }
            return super.process(in, senderId);
        }

        private class Symbol{
            private String letters = "";
            private String[] lines = new String[5];

            public Symbol(String letters, String[] lines){
                this.lines = normalize(lines);
                this.letters = letters;
            }
            private String[] normalize(String[] lines){
                int max = 0;
                for (int i = 0; i < lines.length; i++) {
                    max = Math.max(max, lines[i].length());
                }
                for (int i = 0; i < lines.length; i++) {
                    while(lines[i].length() < max)
                        lines[i] = lines[i] + " ";
                }
                return lines;
            }
            public int getHeight(){
                return lines.length;
            }
            public boolean isIt(char c){
                for (int i = 0; i < letters.length(); i++) {
                    if(letters.charAt(i) == c)
                        return true;
                }
                return false;
            }
            public String[] getLines(){
                return lines;
            }

        }
    }
    class MathSolver extends Function{
        @Override
        String process(String text, Long senderId) {
            if(!text.contains("+") && !text.contains("-") && !text.contains("*") && !text.contains("/") && !text.contains("(") && !text.contains(")") && !text.contains("^"))
                return "";
            try {
                String ex = text.toLowerCase();
                ex = ex.replace("сколько будет", "");
                ex = ex.replace("реши мне", "");
                ex = ex.replace("реши", "");
                ex = ex.replace("сколько", "");
                ex = ex.replace("можешь решить", "");
                ex = ex.replace("посчитай", "");
                ex = ex.replace("вычисли", "");
                ex = ex.replace("помоги", "");
                ex = ex.replace(":", "");
                ex = ex.replace("?", "");
                Expression expression = new ExpressionBuilder(ex).build();
                String result = "Результат: " + String.valueOf(expression.evaluate());
                result = result.replace('.', ',');
                result = result + ".";
                result = result.replace(",0.", ".");
                return result;
            }
            catch (Exception e){
                return "";
            }
        }
    }
}
