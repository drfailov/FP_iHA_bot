package com.fsoft.ihabot.Utils;

import android.content.Context;

import com.fsoft.ihabot.BotService;
import com.fsoft.ihabot.answer.AnswerDatabase;
import com.fsoft.ihabot.answer.Attachment;
import com.fsoft.ihabot.answer.Message;
import com.fsoft.ihabot.communucation.Communicator;
import com.fsoft.ihabot.communucation.tg.TgAccount;
import com.fsoft.ihabot.configuration.AdminList;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Это центральный элемент всей цепочки команд.
 * Главное хранилище обьединяющее программую.
 *
 *
 * 2015 Этот класс начал формироваться
 * 2017 Спустя два года самое время всё нахуй переписать.
 * 2022 Спустя ещё 5 лет я переписываю всё снова.
 *
 * Менеджер занимается хранением связи между модулями, является контейнером для команд общего назначения (обслуживание бота,
 * файловой системы, учёт времени, работа с бэкамами, например)
 *
 * Какой модуль чем занимается:
 *
 * Service. Менеджер должен создаваться исключительно из сервиса. Ссылка на сервис нужна для контекста.
 *
 * Activity обслуживает все окна. Экраны с командами, экраны с аккаунтами, логом, сообщениями,
 * Активити получает доступ к сервису при помощи статической функции GetInstance,
 * а далее обращается к ответственным модулям.
 * \\\ что делать с командами работы с элементами на экране (на активити, которое не всегда есть)
 * Когда модулю нужно отобразить на экране сообщение, он обращается к активити.
 *
 * BotBrain занимается подбором ответов. Он является контейнером для функций, для базы. Там содержатся и синонимы, и базы,
 * и все(!) модули которые отвечают НЕ на команды.
 * Текст переходит между модулями в его изначальном виде, т.е. с обращением.
 * Этот же модуль отвечает за отображение сообщений на активити.
 * Этот модуль содержит функции проверки наличия обращения, убирания обращения из Message
 *
 * Communicator занимается работой с сетью. С аккаунтами. Именно этот модуль хранит аккаунты, запускает их. Аккаунты
 * инициируют вызовы других модулей.
 * /// как должны идти сообщения написанные боту изнутри программы?
 *
 * Created by Dr. Failov on 14.08.2018.
 */
public class ApplicationManager extends CommandModule {
    private static ApplicationManager applicationManagerInstance = null;
    public static ApplicationManager getInstance(){
        return applicationManagerInstance;
    }

    private final BotService service;//это в общем то наш сервис. Он должен быть по любому
    private final Communicator communicator;
    private final AnswerDatabase answerDatabase;
    private final AdminList adminList; //кто админ, кто не админ, кому что можна


    public ApplicationManager(BotService service) throws Exception {
        super();
        applicationManagerInstance = this;
        this.service = service;

        //Здесь можно подчистить временнную папку
        if(!getTempFolder().isDirectory())
            log("Временной папки нет, создание временной папки: " + getTempFolder().mkdirs());
        File[] tempFiles = getTempFolder().listFiles();
        if(tempFiles != null && tempFiles.length != 0){
            log("Во временной папке есть старые файлы. Очистка временной папки...");
            for (File file:tempFiles)
                log("- Удаление файла " + file.getName() + ": " + file.delete());
        }

        //инициализация основных модулей
        adminList = new AdminList(this);
        answerDatabase = new AnswerDatabase(this);
        communicator = new Communicator(this);

        //построение вертикали управления
        childCommands.add(adminList);
        childCommands.add(answerDatabase);
        childCommands.add(communicator);
        childCommands.add(new HelpCommand());

        //Запусе коммуникатора с задержкой, на всякий случай, чтобы успели прогрзиться остальные модули
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                communicator.startCommunicator();
            }
        }, 1000);
    }
    /**
     * Возвращает адрес где у программы есть полный доступ хранить сфои файлы
     * @return File который указывает туда
     * @author Dr. Failov
     */
    public File getHomeFolder(){
        return service.getFilesDir();
    }
    /**
     * Возвращает адрес где у программы есть возможность хранить временные файлы.
     * Эта папка очищается при каждом запуске.
     * @return File который указывает во времменнуэ папку.
     * @author Dr. Failov
     */
    public File getTempFolder(){
        return new File(getHomeFolder(), "Temp");
    }
    public Context getContext() {
        return service;
    }
    public Communicator getCommunicator() {
        return communicator;
    }
    public AnswerDatabase getAnswerDatabase() {
        return answerDatabase;
    }
    public AdminList getAdminList() {
        return adminList;
    }



    private class HelpCommand extends CommandModule{
        @Override
        public ArrayList<Message> processCommand(Message message, TgAccount tgAccount) throws Exception {
            ArrayList<Message> result = super.processCommand(message, tgAccount);
            if(message.getText().toLowerCase(Locale.ROOT).trim().equals("помощь")) {
                ArrayList<CommandDesc> commands = ApplicationManager.this.getHelp();
                StringBuilder stringBuilder = new StringBuilder();
                for (CommandDesc commandDesc:commands)
                    stringBuilder.append("<b>").append(commandDesc.getExample()).append("</b> - ").append(commandDesc.getHelpText()).append("\n\n");
                Message answer = new Message(stringBuilder.toString());
                result.add(answer);
            }
            return result;
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = super.getHelp();
            result.add(new CommandDesc("помощь", "Выводит полный список доступных команд"));
            return result;
        }
    }
}
