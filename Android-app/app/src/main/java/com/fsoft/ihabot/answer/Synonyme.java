package com.fsoft.ihabot.answer;

import static java.util.Arrays.asList;

import android.content.res.Resources;

import com.fsoft.ihabot.R;
import com.fsoft.ihabot.Utils.ApplicationManager;
import com.fsoft.ihabot.Utils.CommandModule;
import com.fsoft.ihabot.Utils.F;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * Synonyme database replaces all words to its base synonym
 *
 * @author Dr. Failov
 * date 2022-03-28
 */
public class Synonyme extends CommandModule {
    private final ApplicationManager applicationManager;
    private static final int defaultDatabaseResource = R.raw.synonyme;
    private File fileSynonyme = null;
    private final ArrayList<ArrayList<String>> synonymeRows = new ArrayList<>();

    public Synonyme(AnswerDatabase answerDatabase)  throws Exception {
        this.applicationManager = answerDatabase.getApplicationManager();
        if(applicationManager == null)
            return;
        fileSynonyme = new File(answerDatabase.getFolderAnswerDatabase(), "synonyme.txt");
        if(!fileSynonyme.isFile()){
            log(". Файла синонимов нет. Загрузка файла synonyme.zip из ресурсов...");
            loadDefaultDatabase();
        }
        loadDefaultDatabase(); //todo это для отладки!

        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileSynonyme));
        String line;
        int lineNumber = 0;

        while ((line = bufferedReader.readLine()) != null) {
            lineNumber++;
            //- Заменить символы которые часто забивают писать (ё ъ щ)
            line = AnswerDatabase.replacePhoneticallySimilarLetters(line);
            // - устранить любые символы повторяющиеся несколько раз
            line = AnswerDatabase.removeRepeatingSymbols(line);
            if (lineNumber % 18 == 0)
                log(". Загрузка синонимов (" + lineNumber + " рядов загружено) ...");
            try {
                ArrayList<String> row = new ArrayList<>(Arrays.asList(line.split(",")));
                if(row.size() > 1){
                    synonymeRows.add(row);
                }
                else {
                    log("! Строка " + lineNumber + " в базе синонимов содержит слишком мало синонимов");
                }
            } catch (Exception e) {
                e.printStackTrace();
                log("! Ошибка разбора строки " + lineNumber + " как ряда синонимов.\n" + e.getMessage());
            }
        }
        //завешить сессию
        bufferedReader.close();
        System.gc();
        log(". Загрузка синонимов из synonyme.txt прошла без ошибок. Загружено " + synonymeRows.size() + " рядов.");
    }

    /*текст на входе:
        - привести текст входящего сообшения к нижнему регистру
        - убрать обращение бот
        - Убрать все символы и знаки, оставить только текст
        - Заменить символы которые часто забивают писать (ё ъ щ)
        - устранить любые символы повторяющиеся несколько раз
     */

    public String replaceSynonyms(String in){
        in = " " + in + " ";
        for (ArrayList<String> row:synonymeRows){
            if(row.size() < 2)
                continue;
            String baseSynonyme = " " + row.get(0) + " ";
            for(int i=1;i<row.size(); i++){
                in = in.replace(" " + row.get(i) + " ", baseSynonyme);
            }
        }
        return in.trim();
    }


    /*Overwrite(!!!!) database by default from resources */
    private void loadDefaultDatabase() throws Exception {
        if (fileSynonyme.isFile()) {
            log(". Удаление старых синонимов synonyme.txt перед восстановлением стандартной базы...");
            log(". Старый файл синонимов удалён перед восстановлением: " + fileSynonyme.delete());
        }
        //get resources
        Resources resources = null;
        if (applicationManager.getContext() != null)
            resources = applicationManager.getContext().getResources();
        //copy file
        log(". Копирование файла synonyme.txt из ресурсов...");
        F.copyFile(defaultDatabaseResource, resources, fileSynonyme);
        log(". Загрузка synonyme.txt из ресурсов прошла без ошибок.");
    }


}
