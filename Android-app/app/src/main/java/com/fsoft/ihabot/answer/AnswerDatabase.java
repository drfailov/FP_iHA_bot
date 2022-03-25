package com.fsoft.ihabot.answer;

import android.content.res.Resources;

import com.fsoft.ihabot.R;
import com.fsoft.ihabot.Utils.ApplicationManager;
import com.fsoft.ihabot.Utils.CommandModule;
import com.fsoft.ihabot.Utils.F;

import java.io.File;
/**
 * ----=== Полный пиздец. ===----
 * Приоритеты:
 * - много ответов
 * - большая скорость
 * - много инфы про ответ
 * - мало памяти
 *
 *
 * * Какой формат должна иметь база:
 *  *    Каждая строка - это JSON. Каждый JSON обязательно содержит уникальный ID.
 *  *    Все ответы должны быть строго в порядке возростания ID. Пропуски ID допускаются. (нужно для контроля уникальности ID в базе)
 *  *      Если последовательность будет нарушена, программа выполнит фильтрацию.
 *  *      Фильтрация длится относительно долго. Дольше обычной загрузки программы.
 *  *      Фильтрация происходит в несколько этапов.
 *  *      Фильтрация не требует дополнительных затрат оперативной памяти.
 *  *      Фильтрация заменит ID для всех ответов после места нарушения.
 *  *      Фильтрация также очистит дубликаты ответов.
 *  *    Если строка имеет не JSON формат - эта строка будет пропущена
 *  *      при загрузке и удалена из базы в момент любого изменения или фильтрации базы.
 *  *      При загрузке программы пользователь получит отчёт, если в базе будут выявлены нарущения.
 *  *    Все ответы и вопросы сохраняются в базе в их изначальном виде, без искажений
 *  *    Если в базе есть два одинаковых ответа - они будут уничтожены при фильтрации.
 *
 *
 *
 * */
public class AnswerDatabase  extends CommandModule {
    int defaultDatabaseResourceZip = R.raw.answer_database;
    ApplicationManager applicationManager = null;
    private File fileAnswers = null;
    private File folderAttachments = null;


    public AnswerDatabase(ApplicationManager applicationManager)  throws Exception {
        this.applicationManager = applicationManager;
        if(applicationManager == null)
            return;
        folderAttachments = new File(applicationManager.getHomeFolder(), "attachments");
        fileAnswers = new File(applicationManager.getHomeFolder(), "answer_database.txt");
        if(!fileAnswers.isFile()){
            log(". Файла базы нет. Загрузка файла answer_database.zip из ресурсов...");
            loadDefaultDatabase();
        }
    }

    /*Overwrite(!!!!) database by default from resources */
    private void loadDefaultDatabase() throws Exception {
        if (fileAnswers.isFile()) {
            log(". Удаление старой базы answer_database.txt перед восстановлением стандартной базы...");
            log(". Старый файл базы удалён перед восстановлением: " + fileAnswers.delete());
            log(". Удаление вложений перед восстановлением стандартной базы...");
            File[] attachments = folderAttachments.listFiles();
            if(attachments != null) {
                for (File file : attachments) {
                    log(". Удаление файла "+file.getName()+": " + file.delete());
                }
            }
            log(". Удаление пустой папки вложений перед восстановлением стандартной базы...");
            log(". Удаление папки "+folderAttachments.getName()+": " + folderAttachments.delete());
        }
        //get resources
        Resources resources = null;
        if (applicationManager.getContext() != null)
            resources = applicationManager.getContext().getResources();
        //copy zip
        File tmpZip = new File(applicationManager.getHomeFolder(), "answer_database.zip");
        log(". Копирование файла answer_database.zip из ресурсов...");
        F.copyFile(defaultDatabaseResourceZip, resources, tmpZip);
        //unzip
        log(". Распаковка файла answer_database.zip...");
        try {
            F.unzip(tmpZip, tmpZip.getParentFile());
        } catch (Exception e) {
            log("! Во время распаковки базы произошла ошибка: " + e.getMessage());
            log("Файл базы удалён после неудачной распаковки: " + fileAnswers.delete());
        }
        //check
        if (fileAnswers.isFile()) {
            log(". Распаковка базы успешна, удаление временного архива...");
            log(". Удаление временного архива: " + tmpZip.delete());
        } else {
            throw new Exception(log("! В результате распаковки файл базы не был получен. Проверьте, чтобы в архиве обязательно был файл answer_database.txt. Если такого файла нет, архив повреждён, нужен другой."));
        }
    }




}
