package com.fsoft.vktest;

import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Random;

/**
 * будет давать позитивные ответы
 * Created by Dr. Failov on 10.08.2014.
 */
public class ThematicsProcessor implements Command {
    ApplicationManager applicationManager = null;
    String name;
    String thematics;
    FileReader fileReader;
    AnswersContainer answers;
    PatternsContainer patterns;
    ArrayList<Command> commands;

    public ThematicsProcessor(ApplicationManager applicationManager, String name, int defaultPatterns, String thematics) {
        this.applicationManager = applicationManager;
        this.name = name;
        this.applicationManager = applicationManager;
        this.thematics = thematics;
        fileReader = new FileReader(applicationManager.activity.getResources(), defaultPatterns, name);
        answers = new AnswersContainer();
        patterns = new PatternsContainer();
        commands = new ArrayList<>();
        commands.add(new Status());
        commands.add(new Save());
        commands.add(new GetAnsw());
        commands.add(new GetPatt());
        commands.add(new RemPatt());
        commands.add(new RemAnsw());
        commands.add(new AddAnsw());
        commands.add(new AddPatt());
    }
    public String processMessage(String text, Long senderId) {
        if(patterns.match(text)){
            return answers.getAnswer();
        }
        return null;
    }
    public void load() {
        log(". Загрузка "+thematics+" обработчика...");
        String fileText = fileReader.readFile();
        if(fileText == null) {
            log("! Ошибка прочтения файла "+thematics+" обработчика: " + fileReader.getFilePath());
            return;
        }
        String[] lines = fileText.split("\\\n");
        try{
            String patternsString = lines[0];
            String answersString = lines[1];
            patterns.load(patternsString);
            answers.load(answersString);
        }
        catch (Exception e){
            log("! Error parsing ThematicsProcessor file " + e.toString());
            e.printStackTrace();
        }
        log(". Данные "+thematics+" обработчика загружены.");
    }
    public void close() {
        save();
    }
    String save(){
        boolean result = fileReader.writeFile(patterns.save() + "\n" + answers.save());
        if(result){
            log(". Сохранение данных обработчика ("+patterns.size() + " шаблонов и " + answers.size() + " ответов) выполнено в " + fileReader.getFilePath());
            return "Сохранение данных обработчика ("+patterns.size() + " шаблонов и " + answers.size() + " ответов) выполнено в " + fileReader.getFilePath() + "\n";
        }
        else{
            log("! Ошибка сохранения тематического обработчика в " + fileReader.getFilePath());
            return "Ошибка сохранения тематического обработчика в " + fileReader.getFilePath();
        }
    }
    public @Override String getHelp() {
        String result  = "";
        for (int i = 0; i < commands.size(); i++)
            result += commands.get(i).getHelp();
        return result;
    }
    public @Override String process(String text) {
        String result = "";
        for (int i = 0; i < commands.size(); i++)
            result += commands.get(i).process(text);
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
            if(commandParser.getWord().equals("status")){
                return  "Количество шаблонов тематического процессора "+fileReader.getFileName()+" : "+patterns.size()+"\n" +
                        "Количество ответов тематического процессора "+fileReader.getFileName()+" : "+answers.size()+"\n" +
                        "Расположение файла базы данных тематического процессора "+fileReader.getFileName()+" : "+fileReader.getFilePath()+"\n";
            }
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
            if(commandParser.getWord().equals("save")){
                return  save()+"\n";
            }
            return "";
        }
    }
    private class GetPatt implements Command{
        @Override
        public String getHelp() {
            return "[ Получить список "+thematics+" шаблонов ]\n" +
                    "---| botcmd "+thematics+" getpatt\n\n";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals(thematics))
                if(commandParser.getWord().equals("getpatt")){
                    String result = "Список шаблонов "+thematics+" обработчика: ";
                    for (int i = 0; i < patterns.size(); i++) {
                        result += patterns.get(i) + "\n";
                    }
                    return result;
                }
            return "";
        }
    }
    private class GetAnsw implements Command{
        @Override
        public String getHelp() {
            return "[ Получить список "+thematics+" ответов ]\n" +
                    "---| botcmd "+thematics+" getansw\n\n";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals(thematics))
                if(commandParser.getWord().equals("getansw")){
                    String result = "Список ответов "+thematics+" обработчика: ";
                    for (int i = 0; i < answers.size(); i++) {
                        result += answers.get(i) + "\n";
                    }
                    return result;
                }
            return "";
        }
    }
    private class RemPatt implements Command{
        @Override
        public String getHelp() {
            return "[ Удалить "+thematics+" шаблон ]\n" +
                    "---| botcmd "+thematics+" rempatt (текст)\n\n";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals(thematics))
                if(commandParser.getWord().equals("rempatt")){
                    String lastText = commandParser.getText();
                    if(lastText.equals(""))
                        return "Ошибка удаления шаблона: не получен текст шаблона.";
                    for (int i = 0; i < patterns.size(); i++) {
                        if(patterns.get(i).equals(lastText)) {
                            patterns.remove(i);
                            return "Шаблон удален: " + lastText;
                        }
                    }
                    return "Ошибка удаления шаблона. Шаблон не найден в базе: " + lastText;
                }
            return "";
        }
    }
    private class RemAnsw implements Command{
        @Override
        public String getHelp() {
            return "[ Удалить "+thematics+" ответ ]\n" +
                    "---| botcmd "+thematics+" remansw (текст)\n\n";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals(thematics))
                if(commandParser.getWord().equals("remansw")){
                    String lastText = commandParser.getText();
                    if(lastText.equals(""))
                        return "Ошибка удаления ответа: не получен текст ответа.";
                    for (int i = 0; i < answers.size(); i++) {
                        if(answers.get(i).equals(lastText)) {
                            answers.remove(i);
                            return "Ответ удален: " + lastText;
                        }
                    }
                    return "Ошибка удаления ответа. Ответ не найден в базе: " + lastText;
                }
            return "";
        }
    }
    private class AddPatt implements Command{
        @Override
        public String getHelp() {
            return "[ Добавить "+thematics+" шаблон ]\n" +
                    "---| botcmd "+thematics+" addpatt (текст)\n\n";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals(thematics))
                if(commandParser.getWord().equals("addpatt")){
                    String lastText = commandParser.getText();
                    if(lastText.equals(""))
                        return "Ошибка внесения шаблона: не получен текст шаблона.";
                    patterns.add(lastText);
                    return "Шаблон добавлен: " + lastText;
                }
            return "";
        }
    }
    private class AddAnsw implements Command{
        @Override
        public String getHelp() {
            return "[ Добавить "+thematics+" ответ ]\n" +
                    "---| botcmd "+thematics+" addansw (текст)\n\n";
        }

        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals(thematics))
                if(commandParser.getWord().equals("addansw")){
                    String lastText = commandParser.getText();
                    if(lastText.equals(""))
                        return "Ошибка внесения ответа: не получен текст ответа.";
                    answers.add(lastText);
                    return "Ответ добавлен: " + lastText;
                }
            return "";
        }
    }

    class PatternsContainer extends ArrayList<String>{
        @Override
        public boolean add(String object) {
            return super.add(object);
        }

        void load(String delimited){
            try {
                String[] elements = delimited.split("\\\\");
                for (int i = 0; i < elements.length; i++) {
                    add(elements[i]);
                }
            }catch (Exception e){
                log("Error while adding data to ThematicsProcessor " + delimited);
                e.printStackTrace();
            }
        }
        String save(){
            String result = "";
            for (int i = 0; i < size(); i++) {
                result += get(i);
                if(i < size()-1)
                    result += "\\";
            }
            return result;
        }
        boolean match(String message){
            for (int i = 0; i < size(); i++) {
                if(applicationManager.messageComparer.isEquals(get(i), message))
                    return true;
            }
            return false;
        }
    }
    class AnswersContainer extends ArrayList<String>{
        Random random = new Random();
        void load(String delimited){
            try {
                String[] elements = delimited.split("\\\\");
                for (int i = 0; i < elements.length; i++) {
                    add(elements[i]);
                }
            }catch (Exception e){
                log("Error while adding data to AnswersContainer");
                e.printStackTrace();
            }
        }
        String save(){
            String result = "";
            for (int i = 0; i < size(); i++) {
                result += get(i);
                if(i < size()-1)
                    result += "\\";
            }
            return result;
        }
        String getAnswer(){
            return get(random.nextInt(size()));
        }
    }
}
