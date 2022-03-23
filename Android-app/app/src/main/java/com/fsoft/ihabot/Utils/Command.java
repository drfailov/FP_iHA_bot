package com.fsoft.ihabot.Utils;

import java.util.ArrayList;

/**
 * Базовый класс для всех обработчиков команд, общий для всех модулей. Это наследование нужно чтобы хранить их все одном массиве
 * На вход принимается текст без botcmd.
 * !!!!! Если команда НЕ обрабатывает текст, возфращать "" (пустую стрингу) ни в коем случае не null!
 * ! Каждый возвращаемый результат ДОЛЖЕН заканчиваться переходом на новую строку
 *
 * Формат справки:
 * [ описание команды ] (до 50 символов)
 * ---| botcmmd hui push <pizda_id>
 * Created by Dr. Failov on 28.11.2014.
 */
public interface Command {
    String processCommand(Message message); //На вход принимается текст без botcmd
    ArrayList<CommandDesc> getHelp();
}
