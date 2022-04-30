package com.fsoft.ihabot.Utils;

import android.util.Log;

import com.fsoft.ihabot.ApplicationManager;

/**
 * Мегаохуительный класс для разбора команд без регистрации и СМС.
 * Created by Dr. Failov on 01.01.2015.
 */

public class CommandParser {
    /*
     * класс для разбора команд.
     * */
    public static ApplicationManager applicationManager = null;
    private String command = "(empty)";
    private String[] words;
    private int currentWordCounter = 0;

    public CommandParser(String command){
        command = deleteCrap(command);
        this.command = command;
        words = command.split("\\ ");
        if(words.length == 1)
            words = command.split("_");
    }
    private void log(String text){
        Log.d(F.TAG, text);
    }
    private String deleteCrap(String in){
        String out = in.replace("botcmd", "");
        out = out.replace("/", " ");
        out = out.replaceAll(" +", " ");
        out = out.trim();
        return out;
    }
    public String tryWord(){
        //получить СЛЕДУЮЩЕЕ ПО ПОРЯДКУ НО НЕ МЕНЯТЬ СЧЁТЧИК. Т.е. вызов tryWord не повлияет на результат вызова getWord
        int index = currentWordCounter;
        return getWord(index);
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
            //e.printStackTrace();
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
        if(word.toLowerCase().equals("false")) return false;
        if(word.toLowerCase().equals("off")) return false;
        if(word.toLowerCase().equals("no")) return false;
        if(word.toLowerCase().equals("disable")) return false;
        if(word.toLowerCase().equals("disabled")) return false;
        if(word.toLowerCase().equals("выкл")) return false;
        if(word.toLowerCase().equals("выключить")) return false;
        if(word.toLowerCase().equals("выключен")) return false;
        if(word.toLowerCase().equals("нет")) return false;
        if(word.equals("0")) return false;
        if(word.equals("-")) return false;

        if(word.toLowerCase().equals("true")) return true;
        if(word.toLowerCase().equals("on")) return true;
        if(word.toLowerCase().equals("yes")) return true;
        if(word.toLowerCase().equals("enable")) return true;
        if(word.toLowerCase().equals("enabled")) return true;
        if(word.toLowerCase().equals("вкл")) return true;
        if(word.toLowerCase().equals("включить")) return true;
        if(word.toLowerCase().equals("включен")) return true;
        if(word.toLowerCase().equals("да")) return true;
        if(word.equals("1")) return true;
        if(word.equals("+")) return true;

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
