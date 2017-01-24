package com.fsoft.vktest;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * this class is comparing messages
 * Created by Dr. Failov on 25.08.2014.
 */
public class MessageComparer implements Command{
    ApplicationManager applicationManager = null;
    String name;
    MessagePreparer messagePreparer;
    long comparedCounter = 0;
    float compareThreshold = 0.9f;
    private ArrayList<Command> commands;

    public MessageComparer(ApplicationManager applicationManager, String name) {
        this.applicationManager = applicationManager;
        this.name = name;
        messagePreparer = new MessagePreparer(applicationManager, name);
        commands = new ArrayList<>();
        commands.add(new Status());
        commands.add(new Save());
        commands.add(new Compare());
        commands.add(messagePreparer);
    }
    public void load() {
        compareThreshold = applicationManager.activity.getPreferences(Context.MODE_PRIVATE).getFloat("compareThreshold_" + name, compareThreshold);
        messagePreparer.load();
    }
    public void close() {
        save();
        messagePreparer.close();
    }
    @Override public String process(String text) {
        String result = "";
        for (int i = 0; i < commands.size(); i++)
            result += commands.get(i).process(text);
        return result;
    }
    @Override public String getHelp() {
        String result  = "";
        for (int i = 0; i < commands.size(); i++)
            result += commands.get(i).getHelp();
        return result;
    }
    public boolean isEquals(String text1, String text2){
        float result = compareMessages(text1, text2);
        return result > 0.9f;
    }
    public float compareMessages(String message1, String message2){
        return compareMessages(message1, false, message2, false);
    }
    public float compareMessages(String message1, boolean message1prepared, String message2, boolean message2prepared){
        //будевые переменные показывают, нужно ли проводить подготовку сообщений, которая является достаточно сложной процкдурой
        comparedCounter ++;
        if(!message1prepared)
            message1 = messagePreparer.processMessageBeforeComparation(message1);
        if(!message2prepared)
            message2 = messagePreparer.processMessageBeforeComparation(message2);
        String[] message1Keywords = message1.split("\\ ");
        message1Keywords = trimArray(message1Keywords);
        String[] message2Keywords = message2.split("\\ ");
        message2Keywords = trimArray(message2Keywords);
        return comparePattern(message1Keywords, message2Keywords);
    }
    private String save(){
        try {
            SharedPreferences.Editor edit = applicationManager.activity.getPreferences(Context.MODE_PRIVATE).edit();
            edit.putFloat("compareThreshold_" + name, compareThreshold);
            edit.commit();
            return "Порог сравнения сохранен: " + compareThreshold + "\n";
        }
        catch (Exception e){
            return "Ошибка сохранения параметров модуля сравнения: " + e.toString() + "\n";
        }
    }
    private float compareWords(String word1, String word2){
        float sum = 0;
        int minLength = Math.min(word1.length(), word2.length());
        int maxLength = Math.max(word1.length(), word2.length());
        for (int i = 0; i < minLength; i++) {
            if(word1.charAt(i) == word2.charAt(i))
                sum++;
        }
        return sum/maxLength;
    }
    private float comparePattern(String[] message, String[] pattern){
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
    private int calculateWords (String text){
        int words = 0;
        try{
            String[] wordsArray = text.split("\\ ");
            words = wordsArray.length;
        }
        catch (Exception e){}
        return words;
    }
    private String[] trimArray(String[] in){
        ArrayList<String> tmp = new ArrayList<>();
        for (int i = 0; i < in.length; i++) {
            if(in[i] != null && !in[i].equals(""))
                tmp.add(in[i]);
        }
        String[] result = new String[tmp.size()];
        for (int i = 0; i < tmp.size(); i++) {
            result[i] = tmp.get(i);
        }
        return result;
    }
    private void log(String text){
        ApplicationManager.log(text);
    }

    class Status implements Command{
        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("status"))
                 return "Всего проведено сравнений: "+comparedCounter+"\n" +
                        "Порог сравнительного анализа: "+compareThreshold+"\n";
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
                return save();
            return "";
        }

        @Override
        public String getHelp() {
            return "";
        }
    }
    class Compare implements Command{
        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("compare")){
                String lastText = commandParser.getText();
                if(lastText.equals(""))
                    return "Ошибка сравнения сообщений: не получены сообщения.";
                String[] messages = lastText.split("\\*");
                if(messages.length < 2)
                    return "Ошибка сравнения: не получено второе сообщение. Вы точно не забыли поставить * между сообщениями?";
                float floatresult = compareMessages(messages[0], messages[1]);
                return "Результат сравнения {"+messages[0]+"} и {"+messages[1]+"}: сходство " +floatresult*100f+ "%.\n";
            }
            return "";
        }

        @Override
        public String getHelp() {
            return "[ Сравнить 2 сообщения ]\n" +
                    "---| botcmd compare сообщение*сообщение\n\n";
        }
    }
    class MessagePreparer implements Command {
        /**
         * This class is preparing messages for comparation
         */
        ApplicationManager applicationManager = null;
        String name;
        SynonimousProvider synonimousProvider;
        int processedCounter = 0;
        private ArrayList<Command> commands;

        public MessagePreparer(ApplicationManager applicationManager, String name) {
            this.applicationManager = applicationManager;
            this.name = name;
            synonimousProvider = new SynonimousProvider(applicationManager, name, R.raw.synonimous);
            commands = new ArrayList<>();
            commands.add(new Status());
            commands.add(new Prepare());
            commands.add(synonimousProvider);
        }
        @Override public  String getHelp() {
            String result  = "";
            for (int i = 0; i < commands.size(); i++)
                result += commands.get(i).getHelp();
            return result;
        }
        @Override public  String process(String text) {
            String result = "";
            for (int i = 0; i < commands.size(); i++)
                result += commands.get(i).process(text);
            return result;
        }
        public void load() {
            synonimousProvider.load();
        }
        public void close() {
            synonimousProvider.close();
        }
        public String processWordBeforeComparation(String in){
            processedCounter ++;
            String out = in;
            out = toLowerCase(out);
            out = replaceLatin(out);
            out = replaceLetters(out);
            out = removeBrackets(out);
            out = deleteBadSymbols(out);
            return out;
        }
        public String processMessageBeforeComparation(String in){
            processedCounter ++;
            //CACHE HERE
            String out = in;
            out = toLowerCase(out);
            out = replaceLatin(out);
            out = replaceLetters(out);
            out = removeBrackets(out);
            out = deleteBadSymbols(out);
            out = replaceSynonimous(out);
            return out;
        }
        public String processMessageBeforeShow(String in){
            processedCounter ++;
            String out = in;
            out = breakLinks(out);
            out = removeBrackets(out);
            out = trimMessage(out);
            out = makeBeginWithLower(out);
            return out;
        }
        private String toLowerCase(String in){
            return in.toLowerCase();
        }
        private String breakLinks(String in){
            String result = in;
            result = result.replace(".com", "****");
            result = result.replace(".ru", "****");
            result = result.replace(".ua", "****");
            result = result.replace(".net", "****");
            result = result.replace(".org", "****");
            result = result.replace(".in", "****");
            result = result.replace(".narod", "****");
            result = result.replace(".at", "****");
            return result;
        }
        private String replaceLatin(String in){
            String result = in;
            result = result.replace('a', 'а');
            result = result.replace('b', 'б');
            result = result.replace('c', 'с');
            result = result.replace('d', 'д');
            result = result.replace('e', 'е');
            result = result.replace('f', 'ф');
            result = result.replace('g', 'г');
            result = result.replace('h', 'х');
            result = result.replace('i', 'и');
            result = result.replace('j', 'й');
            result = result.replace('k', 'к');
            result = result.replace('l', 'л');
            result = result.replace('m', 'м');
            result = result.replace('n', 'н');
            result = result.replace('o', 'о');
            result = result.replace('p', 'п');
            result = result.replace('q', 'к');
            result = result.replace('r', 'р');
            result = result.replace('s', 'с');
            result = result.replace('t', 'т');
            result = result.replace('u', 'у');
            result = result.replace('v', 'в');
            result = result.replace('w', 'в');
            result = result.replace('x', 'х');
            result = result.replace('y', 'у');
            result = result.replace('z', 'з');
            return result;
        }
        private String replaceLetters(String in){
            String result = in;
            result = result.replace('о', 'а');
            result = result.replace('й', 'и');
            result = result.replace('е', 'и');
            result = result.replace('ё', 'и');
            result = result.replace('ы', 'и');
            result = result.replace('і', 'и');
            result = result.replace('ї', 'и');
            result = result.replace('э', 'и');
            result = result.replace('т', 'д');
            result = result.replace('з', 'с');
            result = result.replace('ц', 'с');
            result = result.replace('ф', 'в');
            result = result.replace('щ', 'ш');
            result = result.replace('б', 'п');
            result = result.replace('г', 'х');
            result = result.replace('ъ', 'ь');
            return result;
        }
        private String replaceSynonimous(String in){
            String out = in;
            out = synonimousProvider.getAllMain(out);
            return out;
        }
        private String removeBrackets(String in){
            int max = 0;
            int cnt = 0;
            StringBuilder stringBuffer = new StringBuilder("");
            for(int i=0; i<in.length(); i++){
                char c = in.charAt(i);
                if(c == '[') cnt++;
                if(cnt == 0)
                    stringBuffer.append(c);
                if(c == ']') cnt --;
                if(cnt > max)
                    max = cnt;
            }
            if(max > 0){
                String result = "";
                try {
                    result = stringBuffer.toString().substring(2);
                }
                catch (Exception e){}
                return result;
            }
            return stringBuffer.toString();
        }
        private String deleteBadSymbols(String text){
            //pass only allowed symbols
            StringBuilder stringBuilder = new StringBuilder();
            String allowed = "ёйцукенгшщзхъфывапролджэячсмитьбю123456789";
            for (int i = 0; i < text.length(); i++) {
                String c = String.valueOf(text.charAt(i));
                if(allowed.contains(c))
                    stringBuilder.append(c);
                else
                    stringBuilder.append(" ");
            }
            String result = " " + stringBuilder.toString() + " ";
            result = result.replaceAll(" +", " ");
            return result;
        }
        String makeBeginWithLower(String in){
            String first = String.valueOf(in.charAt(0));
            return first.toLowerCase() + in.substring(1);
        }
        String makeBeginWithUpper(String in){
            String first = String.valueOf(in.charAt(0));
            return first.toUpperCase() + in.substring(1);
        }
        String trimMessage(String in){
            String out = in.replaceAll(" +", " ");
            //убрать пробкл в начале сообщения если он есть
            if(out.charAt(0) == ' ')
                out = out.substring(1);
            return out;
        }


        class Status implements Command{
            @Override
            public String process(String input) {
                CommandParser commandParser = new CommandParser(input);
                if(commandParser.getWord().equals("status"))
                    return "Преобразовано сообщений: " + processedCounter + "\n";
                return "";
            }

            @Override
            public String getHelp() {
                return "";
            }
        }
        class Prepare implements Command{
            @Override
            public String process(String input) {
                CommandParser commandParser = new CommandParser(input);
                if(commandParser.getWord().equals("preparemessage")){
                    String lastText = commandParser.getText();
                    if(lastText.equals(""))
                        return "Ошибка преобразования текста: не получен текст.\n";
                    String result = processMessageBeforeComparation(lastText);
                    return "Результат преобразования текста: \nОригинальный текст: " + lastText + "\n Преобразованный текст: " + result + "\n";
                }
                return "";
            }

            @Override
            public String getHelp() {
                return "[ Преобразовать сообщение для сравнения в программе ]\n" +
                        "---| botcmd preparemessage сообщение\n\n";
            }
        }
        class SynonimousProvider implements Command{
            ApplicationManager applicationManager = null;
            String name;
            ArrayList<Scope> scopes;
            FileReader fileReader;
            private ArrayList<Command> commands;

            public SynonimousProvider(ApplicationManager applicationManager, String name, int sinDefaultResource) {
                this.applicationManager = applicationManager;
                this.name = name;
                scopes = new ArrayList<>();
                this.applicationManager = applicationManager;
                fileReader = new FileReader(applicationManager.activity.getResources(), sinDefaultResource, name);
                commands = new ArrayList<>();
                commands.add(new Save());
                commands.add(new Status());
                commands.add(new AddSyn());
            }
            public void load() {
                log(". Загрузка базы синонимов...");
                String file = fileReader.readFile();
                if(file == null){
                    log("! Ошибка чтения синонимов из файла " + fileReader.getFilePath());
                    return;
                }
                String[] lines = file.split("\\\n");
                log(". Загружено "+lines.length+" строк. Группировка...");
                log("|──────────────────────────────|");
                int percent = 0;
                for (int i = 0; i < lines.length; i++) {
                    int curPercent = (i * 100)/lines.length;
                    if(curPercent != percent){
                        TabsActivity.consoleView.clearLastLine();
                        log(ConsoleView.getLoadingBar(30, curPercent));
                        percent = curPercent;
                    }
                    Scope scope = new Scope(lines[i]);
                    addScope(scope);
                    if(lines.length < 300)
                        try{
                            Thread.sleep(50);
                        }
                        catch (Exception e){}
                }
                log(". База синонимов загружена: "+scopes.size()+" синонимических рядов.");
            }
            public void close() {
                save();
            }
            @Override public String getHelp() {
                String result  = "";
                for (int i = 0; i < commands.size(); i++)
                    result += commands.get(i).getHelp();
                return result;
            }
            @Override public String process(String text) {
                String result = "";
                for (int i = 0; i < commands.size(); i++)
                    result += commands.get(i).process(text);
                return result;
            }
            public String processMessage(String text, Long senderId) {
                return getAllMain(text);
            }
            public String getAllMain(String text){
                if(mainPhrases == null)
                    mainPhrases = new HashMap<>();
                if(mainPhrases.containsKey(text))
                    return mainPhrases.get(text);
                String in = new String(text);

                for(int i=0; i<scopes.size(); i++){
                    text = scopes.get(i).replaceSynsByMain(text);
                }
                mainPhrases.put(in, text);
                return text;
            }
            public String getMainSyn(String word){
                for (int i = 0; i < scopes.size(); i++) {
                    if(scopes.get(i).doesSynExists(word))
                        return scopes.get(i).getMainSyn();
                }
                return word;
            }
            public String addSyn(String baseSyn, String newSyn){
                try {
                    if(baseSyn == null || newSyn == null || baseSyn.equals("") || newSyn.equals(""))
                        return "Ошибка добавления синонима: аргументы пусты.";
                    newSyn = processWordBeforeComparation(newSyn);
                    baseSyn = processWordBeforeComparation(baseSyn);
                    if(baseSyn.equals(newSyn))
                        return "Ошибка добавления синонима: аргументы равны. "+"("+baseSyn +" и "+newSyn+")";
                    for (int scope = 0; scope < scopes.size(); scope++) {
                        boolean containsBase = scopes.get(scope).doesSynExists(baseSyn);
                        if (containsBase) {
                            boolean containsNew = scopes.get(scope).doesSynExists(newSyn);
                            if (!containsNew) {
                                scopes.get(scope).words.add(newSyn);
                                return "Синоним " + newSyn + " успешно добавлен в ряд к синониму " + baseSyn + ". Сейчас в этом ряду " + scopes.get(scope).words.size() + " синонимов.";
                            } else {
                                return "Синоним " + newSyn + " не может быть добавлен к синониму " + baseSyn + ", поскольку среди его " + scopes.get(scope).words.size() + " синонимов такой уже есть.";
                            }
                        }
                    }
                    scopes.add(new Scope(baseSyn + "\\" + newSyn));
                    return "Добавлен новый синонимический ряд: "+baseSyn +" и "+newSyn+".";
                }
                catch (Exception e){
                    e.printStackTrace();
                    return "Произошел сбой во время добавления синонима: " + e.toString();
                }
            }
            private void addScope(Scope scope){
                for (int i = 0; i < scopes.size(); i++) {
                    if(scopes.get(i).isSimilar(scope)){
                        scopes.get(i).complete(scope);
                        return;
                    }
                }
                scopes.add(scope);
            }
            private String save(){
                log(". Сохранение базы синонимов...");
                if(scopes.size() == 0) {
                    log( "! Сохранение синонимов невозможно: база пустая.");
                    return "Сохранение синонимов невозможно: база пустая.\n";
                }
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < scopes.size(); i++) {
                    stringBuilder.append(scopes.get(i).save());
                    if(i<scopes.size()-1)
                        stringBuilder.append("\n");
                }
                boolean result = fileReader.writeFile(stringBuilder.toString());
                if(result) {
                    log(". База синонимов сохранена: "+scopes.size()+" синонимических рядов.");
                    return "- Сохранение синонимов в " + fileReader.getFilePath() + " прошло успешно. Всего сохранено " + scopes.size() + " синонимических рядов.";
                }
                else {
                    log("! Ошибка сохранения синонимов в " + fileReader.getFilePath() + " Всего " + scopes.size() + " синонимических рядов.");
                    return "! Ошибка сохранения синонимов в " + fileReader.getFilePath() + " Всего " + scopes.size() + " синонимических рядов.";
                }
            }
            private HashMap<String, String> mainPhrases;


            class Status implements Command{
                @Override
                public String process(String input) {
                    CommandParser commandParser = new CommandParser(input);
                    if(commandParser.getWord().equals("status"))
                        return "Всего синонимических рядов: " + scopes.size() + "\n" +
                                "Адрес хранилища синонимов: " + fileReader.getFilePath() + "\n";
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
                        return save();
                    return "";
                }

                @Override
                public String getHelp() {
                    return "";
                }
            }
            class AddSyn implements Command{
                @Override
                public String process(String input) {
                    CommandParser commandParser = new CommandParser(input);
                    if(commandParser.getWord().equals("addsyn")){
                        if(commandParser.wordsRemaining() < 2)
                            return "Ошибка добавления синонима: недостаточно аргументов.\n";
                        String base = commandParser.getWord();
                        String nw = commandParser.getWord();
                        return addSyn(base, nw);
                    }
                    return "";
                }

                @Override
                public String getHelp() {
                    return "[ Добавить новый синоним в базу ]\n" +
                            "---| botcmd addsyn базовый_синоним новый_синоним\n\n";
                }
            }
            private class Scope{
                private ArrayList<String> words;
                private String separator = "\\";

                public Scope(String scope){
                    words = new ArrayList<>();
                    String[] splitted = scope.split("\\" + separator);
                    for(int i=0; i<splitted.length; i++)
                        words.add( processWordBeforeComparation(splitted[i]));
                }
                public String save(){
                    String result = "";
                    for (int i = 0; i < words.size(); i++) {
                        result += words.get(i);
                        if(i<words.size()-1)
                            result += separator;
                    }
                    return result;
                }
                public boolean doesSynExists(String word){
                    for (int i = 0; i < words.size(); i++)
                        if((words.get(i)).equals(word))
                            return true;
                    return false;
                }
                public boolean isSimilar(Scope scope){
                    for (int i = 0; i < words.size(); i++) {
                        for (int j = 0; j < scope.words.size(); j++) {
                            if(words.get(i).equals(scope.words.get(j)))
                                return true;
                        }
                    }
                    return false;
                }
                public void complete(Scope scope){
                    //дополнить текущий ряд другим рядом, если известно что они похожи
                    for (int i = 0; i < scope.words.size(); i++) {
                        if(!words.contains(scope.words.get(i)))
                            words.add(scope.words.get(i));
                    }
                }
                public String getMainSyn(){
                    String result = words.get(0);
                    return result;
                }
                public String replaceSynsByMain(String text){
                    for (int i = 1; i < words.size(); i++)
                        text = text.replace(words.get(i), getMainSyn());
                    return text;
                }
                public String whatSynExists(String message){
                    for(int i=0; i<words.size(); i++)
                        if(message.contains((words.get(i))))
                            return (words.get(i));
                    return null;
                }
                public ArrayList<String> provideVariants(String message){
                    String exists = whatSynExists(message);
                    ArrayList<String> result = new ArrayList<>();
                    if(exists != null)
                        for(int i=0; i<words.size(); i++)
                            result.add(message.replace(exists, (words.get(i))));
                    else
                        result.add(message);
                    return result;
                }
                public ArrayList<String> provideVariants(ArrayList<String> in){
                    ArrayList<String> out = new ArrayList<>();
                    for (int i = 0; i < in.size(); i++)
                        out.addAll(provideVariants(in.get(i)));
                    return out;
                }
            }
        }
    }
}