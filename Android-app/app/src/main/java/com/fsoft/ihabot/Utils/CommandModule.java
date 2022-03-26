package com.fsoft.ihabot.Utils;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * этот класс будет собирать в себе весь общий функционал необходимый для работы модулей внутри самой программы
 * Created by Dr. Failov on 12.02.2017.
 */
public class CommandModule implements Command {
    protected SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    protected ArrayList<CommandModule> childCommands = new ArrayList<>();


    public CommandModule() {
    }

    protected static String log(String string){
        Log.d(F.TAG, string);
        return string;
    }

    @Override
    public String processCommand(Message message) {
        String result = "";
        for (CommandModule child : childCommands) {
            try {
                result += child.processCommand(message);
            }
            catch (Exception e){
                e.printStackTrace();
                return "! Ошибка обрабоки команды дочерним модулем:\n"+
                                "Модуль: "+this.getClass().getName()+"\n" +
                                "Ошибка: " + e.toString();
            }
        }
        return result;
    }
    @Override
    public ArrayList<CommandDesc> getHelp() {
        ArrayList<CommandDesc> result=new ArrayList<>();
        for (CommandModule child : childCommands)
            result.addAll(child.getHelp());
        return result;
    }
    public void stop() {
        //при закрытии программы останавливает процессы
        //НЕ ИСПОЛЬЗОВАТЬ ДЛЯ СОХРАНЕНИЯ!!!!!!!!!!!!!!! Сохранять всё на лету, так надежнее
        for (CommandModule child : childCommands)
            child.stop();

    }
}
