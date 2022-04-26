package com.fsoft.ihabot.Utils;

import com.fsoft.ihabot.answer.Message;
import com.fsoft.ihabot.communucation.tg.TgAccount;
import com.fsoft.ihabot.configuration.AdminList;

import java.util.ArrayList;

/**
 * Базовый класс для всех обработчиков команд, общий для всех модулей. Это наследование нужно чтобы хранить их все одном массиве
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
     * @param message сообщение на входе от администратора в неизменном виде
     * @param tgAccount Аккаунт телеграм бота, который получил команду. Нужен для обращения в сеть и деланья всякого важного разного командами
     * @param admin Какой из админов прислал команду. Мы тут можем проверить есть ли у него права, и т.д.
     * @return Список сообщений ответов на команду.
     * @throws Exception Мало ли что может случиться при обработке. Команды многое могут делать.
    * */
    ArrayList<Message> processCommand(Message message, TgAccount tgAccount, AdminList.AdminListItem admin)  throws Exception; //На вход принимается текст без botcmd

    /**
     * Эта команда участвует в формировании справки для админа со списком доступных команд
     * @param requester Какой админ запрашивает справку. Не будем выводить ему в справку команд, к которым у него нет доступа.
     * @return Список обьектов CommandDesc, где каждый содердит достаточно информации о команде
     */
    ArrayList<CommandDesc> getHelp(AdminList.AdminListItem requester);
}
