package com.fsoft.ihabot.answer;

import android.content.res.Resources;
import android.util.Pair;

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
import java.util.Locale;

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
    private static final int defaultDatabaseResourceZip = R.raw.answer_database;
    private ApplicationManager applicationManager = null;
    private JaroWinkler jaroWinkler = new JaroWinkler();
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

    /*Медленная функция для отладки*/
    public AnswerElement pickAnswer(Message question) throws Exception{
        MessageRating messageRating = new MessageRating();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileAnswers));
        String line;
        int lineNumber = 0;
        int errors = 0;

        while ((line = bufferedReader.readLine()) != null) {
            lineNumber++;
            if (lineNumber % 1000 == 0)
                log(". Поиск ответа в базе (" + lineNumber + " уже проверено) ...");
            try {
                JSONObject jsonObject = new JSONObject(line);
                AnswerElement currentAnswerElement = new AnswerElement(jsonObject);
                String currentQuestion = currentAnswerElement.getQuestionMessage().getText();
                String neededQuestion = question.getText();

                double similarity = compareMessages(neededQuestion, currentQuestion);

                messageRating.addAnswer(currentAnswerElement, similarity);
            } catch (Exception e) {
                e.printStackTrace();
                errors++;
                log("! Ошибка разбора строки " + lineNumber + " как ответа из базы.\n" + e.getMessage());
            }
        }
        if (errors != 0)
            log("! При загрузке базы ответов возникло ошибок: " + errors + ".");
        //завешить сессию
        bufferedReader.close();
        System.gc();

        log("-----------------------------------------------------------");
        log("Вопрос: " + question.getText());
        log("Варианты ответов:");
        for(int i = 0; i<messageRating.getCapacity(); i++){
            double rating = messageRating.getTopRating(i);
            AnswerElement answerElement = messageRating.getTopMessage(i);
            if(answerElement != null) {
                log(String.format(Locale.US, "%d %2f: ID%d %s -> %s",
                        i,
                        rating,
                        answerElement.getId(),
                        answerElement.getQuestionMessage().toString().replace("\n", ""),
                        answerElement.getAnswerMessage().toString().replace("\n", "")));
            }
        }
        log("-----------------------------------------------------------");

        if(messageRating.isEmpty() || messageRating.getTopRating() < 0.48)
            throw new Exception("Нормального ответа найти не получилось");
        else
            return messageRating.getTopMessage();
    }
    private double compareMessages(String s1, String s2){
        /*- привести текст входящего сообшения к нижнему регистру
        - оставить только последнее предложение
        - Сохранить информацию о том есть ли знак вопроса
        - Убрать все символы и знаки, оставить только текст
        если после этого всего строка оказывается пустая - результат сравнения точно 0
        - заменить синонимы (полнотекстовым образом)
        - Заменить символы которые часто забивают писать (ё ъ щ)
        - устранить любые символы повторяющиеся несколько раз
        - Тримануть
        - разложить на слова
        - сравнить все слова со всеми  по алгоритму Жаро-Винклера
        - При сравнении между вариантами учитывать если есть слова которые не участвовали в сравнении
        - ПРи сравнении учитывать наличие знака вопроса в тексте*.
        - учитывать длину сообщения
         */
        //привести текст входящего сообшения к нижнему регистру
        s1 = s1.toLowerCase(Locale.ROOT);
        s2 = s2.toLowerCase(Locale.ROOT);

        //оставить только последнее предложение
        s1 = passOnlyLastSentence(s1);
        s2 = passOnlyLastSentence(s2);

        //Сохранить информацию о том есть ли знак вопроса
        boolean s1question = s1.contains("?");
        boolean s2question = s2.contains("?");

        //Убрать все символы и знаки, оставить только текст
        s1 = filterSymbols(s1);
        s2 = filterSymbols(s2);

        //если после этого всего строка оказывается пустая - результат сравнения точно 0
        if(s1.isEmpty() || s2.isEmpty())
            return 0;

        //заменить синонимы (полнотекстовым образом в нижнем регистре)
        //todo

        //Заменить символы которые часто забивают писать (ё ъ щ)
        s1 = replacePhoneticallySimilarLetters(s1);
        s2 = replacePhoneticallySimilarLetters(s2);

        //устранить любые символы повторяющиеся несколько раз
        s1 = removeRepeatingSymbols(s1);
        s2 = removeRepeatingSymbols(s2);

        //Тримануть
        s1 = s1.trim();
        s2 = s2.trim();

        //разложить на слова
        ArrayList<String> s1words = new ArrayList<>(Arrays.asList(s1.split(" ")));
        ArrayList<String> s2words = new ArrayList<>(Arrays.asList(s2.split(" ")));

        //сравнить все слова со всеми  по алгоритму Жаро-Винклера
        //
        //1 записать общее количество слов для обоих фраз
        //2 Найти между двумя строками пару самых похожих слов (максимальный коэффициент)
        //3 Их похожесть добавить к сумме по этой паре фраз
        //4 Эту пару слов удалить из списка
        //5 Если в каждой фразе ещё остались слова, вернуться к п.2
        //6 Результат вычислить по формуле: сумма похожести / общее количество слов обоих фраз

        //записать общее количество слов для обоих фраз
        double wordsSum = s1words.size() + s2words.size();
        double similaritySum = 0;
        //Если в каждой фразе ещё остались слова, вернуться
        while(!s1words.isEmpty() && !s2words.isEmpty()){
            //Найти между двумя строками пару самых похожих слов (максимальный коэффициент)
            String s1wordMax = null;
            String s2wordMax = null;
            double maxSimilarity = 0;
            for (int i=0; i<s1words.size(); i++){
                for (int j=0; j<s2words.size(); j++){
                    String s1word = s1words.get(i);
                    String s2word = s2words.get(j);
                    double similarity = jaroWinkler.similarity(s1word, s2word);
                    if(similarity >= maxSimilarity){
                        s1wordMax = s1word;
                        s2wordMax = s2word;
                        maxSimilarity = similarity;
                    }
                }
            }
            //Их похожесть добавить к сумме по этой паре фраз
            similaritySum += maxSimilarity;
            //Эту пару слов удалить из списка
            s1words.remove(s1wordMax);
            s2words.remove(s2wordMax);
        }
        //Результат вычислить по формуле: сумма похожести / общее количество слов обоих фраз
        double result = similaritySum / wordsSum;

        //учесть знак вопроса
        if(s1question == s2question)
            result += 0.1;

        //учесть длину строк
        result -= Math.abs(s1.length()-s2.length()) * 0.01;

        return result;
    }
    private static String passOnlyLastSentence(String in){
        if(!in.contains("."))
            return in;
        String[] sentences = in.split("\\.");
        for(int i=sentences.length-1; i>=0; i--)
            if(sentences[i].length() > 3)
                return sentences[i];
        //если не было найдено ни одного нормального предложения
        return in;
    }
    private static String filterSymbols(String input){
        String allowedSymbols = "qwertyuiopasdfghjklzxcvbnm їіёйцукенгшщзхъфывапролджэячсмитьбю 1234567890";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if(allowedSymbols.indexOf(c) >= 0)
                builder.append(c);
        }
        return builder.toString();
    }
    private static String removeRepeatingSymbols(String in){
        Character last = null;
        String result = "";
        for (Character c : in.toCharArray()) {
            if (c.equals(last)) {
                continue;
            }
            result = result.concat(c.toString());
            last = c;
        }
        return result;
    }
    private static String replacePhoneticallySimilarLetters(String in) {
        String result = in;
        result = result.replace('ё', 'е');
        result = result.replace('й', 'и');
        result = result.replace('е', 'и');
        result = result.replace('і', 'и');
        result = result.replace('ї', 'и');
        result = result.replace('э', 'и');
        result = result.replace('щ', 'ш');
        result = result.replace('ъ', 'ь');
        return result;
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



    /**
     *
     * The Jaro–Winkler distance metric is designed and best suited for short
     * strings such as person names, and to detect typos; it is (roughly) a
     * variation of Damerau-Levenshtein, where the substitution of 2 close
     * characters is considered less important then the substitution of 2 characters
     * that a far from each other.
     * Jaro-Winkler was developed in the area of record linkage (duplicate
     * detection) (Winkler, 1990). It returns a value in the interval [0.0, 1.0].
     * The distance is computed as 1 - Jaro-Winkler similarity.
     *
     * https://en.wikipedia.org/wiki/Jaro%E2%80%93Winkler_distance
     * https://github.com/tdebatty/java-string-similarity/blob/master/src/main/java/info/debatty/java/stringsimilarity/JaroWinkler.java
     *
     * @author Thibault Debatty
     */
    static class JaroWinkler{

        private static final double DEFAULT_THRESHOLD = 0.7;
        private static final int THREE = 3;
        private static final double JW_COEF = 0.1;
        private final double threshold;

        /**
         * Instantiate with default threshold (0.7).
         *
         */
        public JaroWinkler() {
            this.threshold = DEFAULT_THRESHOLD;
        }

        /**
         * Instantiate with given threshold to determine when Winkler bonus should
         * be used.
         * Set threshold to a negative value to get the Jaro distance.
         * @param threshold Threshold
         */
        public JaroWinkler(final double threshold) {
            this.threshold = threshold;
        }

        /**
         * Returns the current value of the threshold used for adding the Winkler
         * bonus. The default value is 0.7.
         *
         * @return the current value of the threshold
         */
        public final double getThreshold() {
            return threshold;
        }

        /**
         * Compute Jaro-Winkler similarity.
         * @param s1 The first string to compare.
         * @param s2 The second string to compare.
         * @return The Jaro-Winkler similarity in the range [0, 1]
         * @throws NullPointerException if s1 or s2 is null.
         */
        public final double similarity(final String s1, final String s2) {
            if (s1 == null) {
                throw new NullPointerException("s1 must not be null");
            }

            if (s2 == null) {
                throw new NullPointerException("s2 must not be null");
            }

            if (s1.equals(s2)) {
                return 1;
            }

            int[] mtp = matches(s1, s2);
            float m = mtp[0];
            if (m == 0) {
                return 0f;
            }
            double j = ((m / s1.length() + m / s2.length() + (m - mtp[1]) / m))
                    / THREE;
            double jw = j;

            if (j > getThreshold()) {
                jw = j + Math.min(JW_COEF, 1.0 / mtp[THREE]) * mtp[2] * (1 - j);
            }
            return jw;
        }


        /**
         * Return 1 - similarity.
         * @param s1 The first string to compare.
         * @param s2 The second string to compare.
         * @return 1 - similarity.
         * @throws NullPointerException if s1 or s2 is null.
         */
        public final double distance(final String s1, final String s2) {
            return 1.0 - similarity(s1, s2);
        }

        private int[] matches(final String s1, final String s2) {
            String max, min;
            if (s1.length() > s2.length()) {
                max = s1;
                min = s2;
            } else {
                max = s2;
                min = s1;
            }
            int range = Math.max(max.length() / 2 - 1, 0);
            int[] matchIndexes = new int[min.length()];
            Arrays.fill(matchIndexes, -1);
            boolean[] matchFlags = new boolean[max.length()];
            int matches = 0;
            for (int mi = 0; mi < min.length(); mi++) {
                char c1 = min.charAt(mi);
                for (int xi = Math.max(mi - range, 0),
                     xn = Math.min(mi + range + 1, max.length()); xi < xn; xi++) {
                    if (!matchFlags[xi] && c1 == max.charAt(xi)) {
                        matchIndexes[mi] = xi;
                        matchFlags[xi] = true;
                        matches++;
                        break;
                    }
                }
            }
            char[] ms1 = new char[matches];
            char[] ms2 = new char[matches];
            for (int i = 0, si = 0; i < min.length(); i++) {
                if (matchIndexes[i] != -1) {
                    ms1[si] = min.charAt(i);
                    si++;
                }
            }
            for (int i = 0, si = 0; i < max.length(); i++) {
                if (matchFlags[i]) {
                    ms2[si] = max.charAt(i);
                    si++;
                }
            }
            int transpositions = 0;
            for (int mi = 0; mi < ms1.length; mi++) {
                if (ms1[mi] != ms2[mi]) {
                    transpositions++;
                }
            }
            int prefix = 0;
            for (int mi = 0; mi < min.length(); mi++) {
                if (s1.charAt(mi) == s2.charAt(mi)) {
                    prefix++;
                } else {
                    break;
                }
            }
            return new int[]{matches, transpositions / 2, prefix, max.length()};
        }
    }

    /**
     *
     * You put here a lot of messages and get [capacity] ones with biggest rating
     *
     * @author Dr. Failov
     */
    private static class MessageRating{
        private final int capacity;
        private final AnswerElement[] messages; //0 - top element (max rating)
        private final double[] ratings;

        /**
         * Instantiate with default capacity of 10
         */
        public MessageRating() {
            this.capacity = 10;
            messages = new AnswerElement[capacity];
            ratings = new double[capacity];
        }
        /**
         * Instantiate with given capacity
         * @param capacity Number of elements stored in rating
         */
        public MessageRating(int capacity) {
            this.capacity = capacity;
            messages = new AnswerElement[capacity];
            ratings = new double[capacity];
        }

        /**
         * Return Message with biggest rating ever received.
         * @return Message with biggest rating or NULL.
         */
        public AnswerElement getTopMessage(){
            return messages[0];
        }

        /**
         * Return Message with biggest rating ever received.
         * @param index 0 is first place, 1 is second place in rating... Max is capacity-1
         * @return Message with taken place in rating or NULL.
         */
        public AnswerElement getTopMessage(int index){
            if(index > capacity-1)
                return null;
            return messages[index];
        }

        /**
         * Return rating value of Message with biggest rating ever received.
         * @return Message with biggest rating or NULL.
         */
        public double getTopRating(){
            return ratings[0];
        }

        /**
         * Return rating value of Message with biggest rating ever received.
         * @param index 0 is first place, 1 is second place in rating... Max is capacity-1
         * @return Message with taken place in rating or NULL.
         */
        public double getTopRating(int index){
            if(index > capacity-1)
                return 0;
            return ratings[index];
        }

        /**
         * @return Number of elements stored in rating
         */
        public int getCapacity() {
            return capacity;
        }

        /**
         * Check if rating is empty
         * @return true if no any elements ever received
         */
        public boolean isEmpty(){
            return messages[0] == null;
        }

        /**
         * Return rating value of Message with biggest rating ever received.
         * @param message Message to store in rating.
         * @param rating rating of Message to store in rating.
         */
        public void addAnswer(AnswerElement message, double rating){
//            if(rating > 0)
//                log("add " + rating + " \t" + message.getText().replace("\n", " ").substring(0, 100));
            for(int i=0; i<capacity; i++){
                if(ratings[i] == 0 || ratings[i] < rating){
                    //shift all next (from i+1 to capacity-1)
                    for(int j=capacity-1; j>=i+1; j--) {
                        messages[j] = messages[j-1];
                        ratings[j] = ratings[j-1];
                    }
                    messages[i] = message;
                    ratings[i] = rating;
                    return;
                }
            }
        }
    }
}
