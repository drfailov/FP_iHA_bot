package com.fsoft.ihabot.Utils;

import com.fsoft.ihabot.answer.Message;
import com.fsoft.ihabot.communucation.tg.TgAccount;
import com.fsoft.ihabot.configuration.AdminList;

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

    /**
     * На входе и на выходе обьект com.fsoft.ihabot.answer.Message
     * Обработка команд происзодит раньше чем обработка базой ответов
     * Если по итогу модули прислали ответы на команду - Будут отправлены только сообщения с ответами на команду
     * Если модули прислали пустые ответы на команду - ничего не будет отправлено. Базой ответов это сообщение не будет обработано.
     * Если модули не прислали ни одного ответа на команду - будет выполнена обработка сообщения как обычного ответа
    * */
    ArrayList<Message> processCommand(Message message, TgAccount tgAccount, AdminList.AdminListItem admin)  throws Exception; //На вход принимается текст без botcmd

    ArrayList<CommandDesc> getHelp();
}
