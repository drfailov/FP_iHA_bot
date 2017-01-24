package com.fsoft.vktest;

import java.io.File;
import java.lang.reflect.Array;
import java.util.*;

/**
 * класс для управления аккаунтами
 * Created by Dr. Failov on 13.11.2014.
 */


public class AccountManager extends ArrayList<VkAccount> implements Command {
    private String homeFolder = "";
    private ApplicationManager applicationManager;
    private final Object getActiveSync = new Object();
    private ArrayList<Command> commands = new ArrayList<>();

    public AccountManager(ApplicationManager applicationManager){
        this.applicationManager = applicationManager;
        homeFolder = ApplicationManager.getHomeFolder() + File.separator + "accounts";
        commands.add(new Status());
        commands.add(new AddAccount());
    }
    public void addAccount(final String token, final long id){
        if(get(id) == null) {
            VkAccount account = new VkAccount(applicationManager, getNextAccountFileName(), token, id);
            //account.waitUntilActive();
            add(account);
            if (applicationManager.vkCommunicator.walls.size() == 0)
                log(applicationManager.vkCommunicator.addOwnerID(account.id));
        }
        else
            log("! Повтор аккаунта "+id+" не добавлен.");
    }
    public void addAccount(){
        VkAccount account = new VkAccount(applicationManager, getNextAccountFileName());
        //account.waitUntilActive();
        add(account);
        if(applicationManager.vkCommunicator.walls.size() == 0)
            log(applicationManager.vkCommunicator.addOwnerID(account.id));
    }
    public void addAccount(final VkAccount account){
        //account.waitUntilActive();
        add(account);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(applicationManager.vkCommunicator.walls.size() == 0)
                    log(applicationManager.vkCommunicator.addOwnerID(account.id));
            }
        }, 2000);
    }
    public void removeAccount(final VkAccount account){
        new Thread(new Runnable() {
            @Override
            public void run() {
                account.setActive(false);
                account.removeFile();
                remove(account);
            }
        }).start();
    }
    public void load(){
        File folder = new File(homeFolder);
        log(". Поиск аккаунтов в " + folder.getPath() + " ...");
        File[] files = folder.listFiles();
        if(files != null) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File file, File file2) {
                    return file.getPath().compareTo(file2.getPath());
                }
            });
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if(file.isFile()){
                    log(". Добавление аккаунта " + file.getPath());
                    addAccount(new VkAccount(applicationManager, file.getPath()));
                    sleep(1000);
                }
            }
        }
        else {
            String help = "Для работы бота нужно добавить аккаунт.\n" +
                    "Перейди на вкладку Аккаунты и нажми кнопку Добавить аккаунт.\n" +
                    "Войди в свой или фейковый аккаунт для бота. Жди дальнейших инструкций.";
            log(help);
            applicationManager.messageBox(help);
        }
    }
    public void close(){
        for (int i = 0; i < size(); i++) {
            get(i).close();
        }
    }
    public @Override String getHelp() {
        String result = "";
        for (int i = 0; i < commands.size(); i++) {
            result += commands.get(i).getHelp();
        }
        for (int i = 0; i < size(); i++) {
            result += get(i).getHelp();
        }
        result +="botcmd account add (token) (userId)";
        return result;
    }
    public @Override String process(String text) {
        String result = "";
        for (int i = 0; i < size(); i++) {
            result += get(i).process(text);
        }
        for (int i = 0; i < commands.size(); i++) {
            result += commands.get(i).process(text);
        }
        return result;
    }
    public String getNextAccountFileName(){
        log(". Поиск нового имени файла ...");
        for (int i = 0; true; i++) {
            String path = homeFolder + File.separator + "account" + i;
            File file = new File(path);
            if(!file.exists()){
                log(". Найдено имя: " + path);
                return path;
            }
        }
    }
    public VkAccount getActive(){
        synchronized (getActiveSync) {
            Random random = new Random();
            while (true) {
                ArrayList<VkAccount> activeAccounts = new ArrayList<>();
                for (int i = 0; i < size(); i++) {
                    VkAccount account = get(i);
                    if (account.isReady())
                        activeAccounts.add(account);
                }
                if (activeAccounts.size() > 0) {
                    return activeAccounts.get(random.nextInt(activeAccounts.size()));
                }
                sleep(500);
            }
        }
    }
    public VkAccount get(long userID){
        synchronized (getActiveSync) {
            for (int i = 0; i < size(); i++) {
                VkAccount account = get(i);
                if (account.id == userID)
                    return account;
            }
            return null;
        }
    }

    private void sleep(int mili){
        try{
            Thread.sleep(mili);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    private void log(String text){
        ApplicationManager.log(text);
    }

    private class Status implements Command{
        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("status")){
                return "Количество аккаунтов: " + size() + "\n";
            }
            return "";
        }

        @Override
        public String getHelp() {
            return "";
        }
    }

    private class AddAccount implements Command{
        @Override
        public String process(String input) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("account")){
                if(commandParser.getWord().equals("add")){
                    String token = commandParser.getWord();
                    long id = commandParser.getLong();
                    addAccount(token, id);
                    return "В список добавлен аккаунт с id = " + id +  ", token = " + token;
                }
            }
            return "";
        }

        @Override
        public String getHelp() {
            return "[ Добавить аккаунт в список аккаунтов ]\n" +
                    "---| botcmd account add (token) (userId)\n\n";
        }
    }
}