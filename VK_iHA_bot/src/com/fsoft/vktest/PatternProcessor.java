package com.fsoft.vktest;

import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Random;

/**
 * будет давать стандартные ответы на стандартные фразы
 * Created by Dr. Failov on 10.08.2014.
 */
public class PatternProcessor implements Command {
    ApplicationManager applicationManager = null;
    String name;
    ArrayList<Pattern> patterns;
    FileReader fileReader;
    ArrayList<Command> commands;

    public PatternProcessor(ApplicationManager applicationManager, String name, int defaultPatterns) {
        this.applicationManager = applicationManager;
        this.name = name;
        this.applicationManager = applicationManager;
        fileReader = new FileReader(applicationManager.activity.getResources(), defaultPatterns, name);
        patterns = new ArrayList<>();
        commands = new ArrayList<>();
        commands.add(new Status());
        commands.add(new Save());
        commands.add(new GetPatternizator());
        commands.add(new AddPatternizator());
        commands.add(new RemPatternizator());
    }
    public String processMessage(String text, Long senderId) {
        ArrayList<String> variants = new ArrayList<>();
        for (int i = 0; i < patterns.size(); i++) {
            String reply = patterns.get(i).process(text);
            if(reply != null)
                variants.add( reply);
        }
        if(variants.size() == 0)
            return null;
        return variants.get(new Random(System.currentTimeMillis()).nextInt(variants.size()));
    }
    public void load() {
        log(". Загрузка шаблонизатора...");
        String fileText = fileReader.readFile();
        if(fileText == null) {
            log("! Ошибка прочтения файла шаблонизатора: " + fileReader.getFilePath());
            return;
        }
        String[] lines = fileText.split("\\\n");
        for (int i = 0; i < lines.length; i++) {
            if(lines[i] != null && !lines[i].equals(""))
                patterns.add(new Pattern(lines[i]));
        }
        log(". Данные шаблонизатора загружены: " + patterns.size() + " шаблонов.");
    }
    public void close() {
        save();
    }
    String save(){
        log(". Сохранение шаблонизатора...");
        if(patterns.size() == 0) {
            log( "! Сохранение шаблонизатора невозможно: база пустая.");
            return "Сохранение шаблонизатора невозможно: база пустая.\n";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < patterns.size(); i++) {
            stringBuilder.append(patterns.get(i).serialize());
            if(i<patterns.size()-1)
                stringBuilder.append("\n");
        }
        boolean result = fileReader.writeFile(stringBuilder.toString());
        if(result) {
            log(". Сохранение данных шаблонизатора (" + patterns.size() + " шаблонов) выполнено в " + fileReader.getFilePath());
            return "Сохранение данных шаблонизатора (" + patterns.size() + " шаблонов) выполнено в " + fileReader.getFilePath() + "\n";
        }
        else{
            log(". Ошибка сохранения данных шаблонизатора в файл " + fileReader.getFilePath());
            return "Ошибка сохранения данных шаблонизатора в файл " + fileReader.getFilePath() + "\n";
        }
    }
    public @Override String getHelp() {
        String result = "";
        for (int i = 0; i < commands.size(); i++) {
            result += commands.get(i).getHelp();
        }
        return result;
    }
    public @Override String process(String text) {
        String result =  "";
        for (int i = 0; i < commands.size(); i++) {
            result += commands.get(i).process(text);
        }
        return result;
    }
    private void log(String text){
        ApplicationManager.log(text);
    }

    private class Status implements Command{
        @Override
        public String getHelp() {
            return "";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("status"))
                return "Количество шаблонов шаблонизатора: "+patterns.size()+"\n" +
                        "Расположение файла базы данных шаблонизатора: "+fileReader.getFilePath()+"\n";
            return "";
        }
    }
    private class Save implements Command{
        @Override
        public String getHelp() {
            return "";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("save"))
                return save();
            return "";
        }
    }
    private class GetPatternizator implements Command{
        @Override
        public String getHelp() {
            return "[ Получить список шаблонов шаблонизатора ]\n" +
                    "---| botcmd getpatternizator\n\n";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("getpatternizator")) {
                String result = "Список шаблонов шаблонизатора: ";
                for (int i = 0; i < patterns.size(); i++) {
                    result += patterns.get(i).serialize() + "\n";
                }
                return result;
            }
            return "";
        }
    }
    private class AddPatternizator implements Command{
        @Override
        public String getHelp() {
            return "[ Добавить шаблон шаблонизатора ]\n" +
                    "---| botcmd addpatternizator текст шаблона*текст ответа\n\n";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("addpatternizator")) {
                String lastText = commandParser.getText();
                if(lastText.equals(""))
                    return "Ошибка внесения шаблона: не получен текст шаблона.";
                String[] part = lastText.split("\\*");
                if(part.length <2)
                    return "Ошибка внесения шаблона: недостаточно аргументов. Вы не звбыли звездочку?!";
                patterns.add(new Pattern(part[0], part[1]));
                return "Шаблон добавлен: " + lastText;
            }
            return "";
        }
    }
    private class RemPatternizator implements Command{
        @Override
        public String getHelp() {
            return "[ Удалить шаблон шаблонизатора ]\n" +
                    "---| botcmd rempatternizator текст шаблона\n\n";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("rempatternizator")) {
                String lastText = commandParser.getText();
                if(lastText.equals(""))
                    return "Ошибка удаления шаблона: не получен текст шаблона.";
                for (int i = 0; i < patterns.size(); i++) {
                    if(patterns.get(i).isIt(lastText)) {
                        patterns.remove(i);
                        return "Шаблон удален: " + lastText;
                    }
                }
                return "Ошибка удаления шаблона. Шаблон не найден в базе: " + lastText;
            }
            return "";
        }
    }

    private class Pattern{
        String answer = "";
        String pattern = "";
        String preparedPattern = null;
        Pattern (String pattern, String answer){
            this.answer = answer;
            this.pattern = pattern;
        }
        Pattern (String serializable){
            String[] part = serializable.split("\\*");
            if(part.length >= 2) {
                pattern = part[0];
                answer = part[1];
            }
        }
        String process(String mes){
            if( isIt(mes))
                return answer;
            return null;
        }
        boolean isIt(String mes){
            String preparedMessage = applicationManager.messageComparer.messagePreparer.processMessageBeforeComparation(mes);
            if(preparedPattern == null)
                preparedPattern = applicationManager.messageComparer.messagePreparer.processMessageBeforeComparation(pattern);
            return preparedMessage.contains(preparedPattern);
        }
        String serialize(){
            return pattern + "*" + answer;
        }
    }
}
