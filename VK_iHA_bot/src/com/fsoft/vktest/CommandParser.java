package com.fsoft.vktest;

/**
 * Мегаохуительный класс для разбора команд без регистрации и СМС.
 * Created by Dr. Failov on 01.01.2015.
 */

class CommandParser {
    /*
    * класс для разбора команд.
    * */
    private String command = "(empty)";
    private String[] words;
    private int currentWordCounter = 0;

    public CommandParser(String command){
        command = deleteCrap(command);
        this.command = command;
        words = command.split("\\ ");
    }
    private void log(String text){
        ApplicationManager.log(text);
    }
    private String deleteCrap(String in){
        String out = in.replace("botcmd", "");
        out = out.replaceAll(" +", " ");
        out = out.trim();
        return out;
    }
    public String getWord(){
        //получить СЛЕДУЮЩЕЕ ПО ПОРЯДКУ
        int index = currentWordCounter;
        currentWordCounter ++;
        return getWord(index);
    }
    public String getWord(int wordNumber){
        if(wordNumber >= 0 && wordNumber<words.length)
            return words[wordNumber];
        else{
            log("! Ошибка: для команды " + command + " было запрошено слово " + wordNumber + ", хотя их всего " + words.length + ". Нумерация должна быть с нуля.");
            return "";
        }
    }
    public String getText(){
        //получить СЛЕДУЮЩЕЕ ПО ПОРЯДКУ
        return getText(currentWordCounter);
    }
    public String getText(int fromWord){
        String lastText = "";
        if(fromWord < 0){
            log("! Осторожно: получен параметр fromWord = "+fromWord+".");
            fromWord = 0;
        }
        for (int i = fromWord; i < words.length; i++) {
            lastText += words[i];
            if(i<words.length-1)
                lastText += " ";
        }
        return lastText;
    }
    public long getLong(){
        //получить СЛЕДУЮЩЕЕ ПО ПОРЯДКУ
        int index = currentWordCounter;
        currentWordCounter ++;
        return getLong(index);
    }
    public long getLong(int wordNumber){
        String word = getWord(wordNumber);
        try {
            return Long.parseLong(word);
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка: не удалось распознать word = "+word+" как обьект Long.");
            return 0L;
        }
    }
    public int getInt(){
        //получить СЛЕДУЮЩЕЕ ПО ПОРЯДКУ
        int index = currentWordCounter;
        currentWordCounter ++;
        return getInt(index);
    }
    public int getInt(int wordNumber){
        String word = getWord(wordNumber);
        try {
            return Integer.parseInt(word);
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка: не удалось распознать word = "+word+" как обьект Integer.");
            return 0;
        }
    }
    public boolean getBoolean(){
        //получить СЛЕДУЮЩЕЕ ПО ПОРЯДКУ
        int index = currentWordCounter;
        currentWordCounter ++;
        return getBoolean(index);
    }
    public boolean getBoolean(int wordNumber){
        String word = getWord(wordNumber).toLowerCase();
        if(word.equals("false")) return false;
        if(word.equals("off")) return false;
        if(word.equals("no")) return false;
        if(word.equals("disable")) return false;
        if(word.equals("0")) return false;

        if(word.equals("true")) return true;
        if(word.equals("on")) return true;
        if(word.equals("yes")) return true;
        if(word.equals("enable")) return true;
        if(word.equals("1")) return true;

        log("! Внимание: не удалось распознать word = "+word+" как обьект Boolean.");
        return false;
    }
    public double getDouble(){
        //получить СЛЕДУЮЩЕЕ ПО ПОРЯДКУ
        int index = currentWordCounter;
        currentWordCounter ++;
        return getDouble(index);
    }
    public double getDouble(int wordNumber){
        String word = getWord(wordNumber);
        try {
            return Double.parseDouble(word);
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка: не удалось распознать word = "+word+" как обьект Double.");
            return 0;
        }
    }
    public int wordsRemaining(){
        return words.length - currentWordCounter;
    }
}
