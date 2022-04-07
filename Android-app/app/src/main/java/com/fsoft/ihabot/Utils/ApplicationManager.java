package com.fsoft.ihabot.Utils;

import android.content.Context;

import com.fsoft.ihabot.BotService;
import com.fsoft.ihabot.answer.AnswerDatabase;
import com.fsoft.ihabot.communucation.Communicator;
import com.fsoft.ihabot.configuration.AdminList;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Спустя два года самое время всё нахуй переписать.
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
 *
 *
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
    private final AdminList adminList;


    public ApplicationManager(BotService service) throws Exception {
        super();
        applicationManagerInstance = this;
        this.service = service;

        adminList = new AdminList(this);

        answerDatabase = new AnswerDatabase(this);

        communicator = new Communicator(this);

        childCommands.add(adminList);
        childCommands.add(answerDatabase);
        childCommands.add(communicator);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                communicator.startCommunicator();
            }
        }, 1000);
    }
    public File getHomeFolder(){
        return service.getFilesDir();
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
}
