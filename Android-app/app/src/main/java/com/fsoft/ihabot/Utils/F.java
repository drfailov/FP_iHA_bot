
/*
 * В этом классе собраны все методы, которые могут быть полезны по всей программе и ни с чем не связаны
 * Created by Dr. Failov on 10.02.2017.
 */

package com.fsoft.ihabot.Utils;

import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class F {
    static public String TAG = "iHA bot";

        public static void sleep(int ms){
            try{
                Thread.sleep(ms);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        public static int dp(int dp){
            return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
        }
        public static String getMD5(String data){
            MessageDigest messageDigest = null;
            byte[] digest = new byte[0];

            try {
                messageDigest = MessageDigest.getInstance("MD5");
                messageDigest.reset();
                messageDigest.update(data.getBytes());
                digest = messageDigest.digest();
            } catch (NoSuchAlgorithmException e) {
                // тут можно обработать ошибку
                // возникает она если в передаваемый алгоритм в getInstance(,,,) не существует
                e.printStackTrace();
            }

            BigInteger bigInt = new BigInteger(1, digest);
            String md5Hex = bigInt.toString(16);

            while( md5Hex.length() < 32 )
                md5Hex = "0" + md5Hex;

            return md5Hex;
        }
        public static String getFileExtension(String name) {
            //String name = file.getName();
            int lastIndexOf = name.lastIndexOf(".");
            if (lastIndexOf == -1) {
                return ""; // empty extension
            }
            return name.substring(lastIndexOf);
        }
        public static String[] splitText(String text, int size){
            //разделить длинное сообщение на части поменьше
            ArrayList<String> parts = new ArrayList<>();
            StringBuilder stringBuilder = new StringBuilder();
            for(int i=0; i<text.length(); i++){
                if(i!= 0 && i%size == 0){
                    parts.add(stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                }
                stringBuilder.append(text.charAt(i));
            }
            if(stringBuilder.length() > 0)
                parts.add(stringBuilder.toString());

            String[] patrsArray = new String[parts.size()];
            for (int i = 0; i < parts.size(); i++) {
                patrsArray[i] = parts.get(i);
            }
            return patrsArray;
        }
        public static String[] trimArray(String[] in){
            //удаляет из массива пустые и null элементы в любых местах массива
            ArrayList<String> tmp = new ArrayList<>();
            for (int i = 0; i < in.length; i++) {
                if(in[i] != null && !in[i].equals(""))
                    tmp.add(in[i]);
            }
            String[] result = new String[tmp.size()];
            for (int i = 0; i < tmp.size(); i++) {
                result[i] = tmp.get(i);
            }
            return result;
        }
        public static boolean isArrayContains(String[] in, String value){
            for (int i = 0; i < in.length; i++)
                if(in[i].toLowerCase().trim().equals(value.toLowerCase().trim()))
                    return true;
            return false;
        }
        public static int getRamUsagePercent(){
            long maxMemory = Runtime.getRuntime().maxMemory();
            long curMemory = Runtime.getRuntime().totalMemory();
            return (int)((curMemory * 100L) / maxMemory);
        }
        public static String arrayToString(String[] array){
            String result = " ";
            for (int i = 0; i < array.length; i++) {
                result += array[i];
                if(i < array.length-1)
                    result += " ";
            }
            return result;
        }
        public static String arrayToStringMultipleLines(String[] array){
            String result = "";
            for (int i = 0; i < array.length; i++) {
                result += array[i];
                if(i < array.length-1)
                    result += "\n";
            }
            return result;
        }
        public static String getTimeText(long ms){
            String result = "";
            long workingTimeSec = ms/1000;

            long threshold = 100L*365L*60L*60L*24L;
            if(workingTimeSec > threshold){
                long num = workingTimeSec/threshold;
                workingTimeSec -= threshold * num;
                result += num + " веков ";
            }

            threshold = 365*60*60*24;
            if(workingTimeSec > threshold){
                long num = workingTimeSec/threshold;
                workingTimeSec -= threshold * num;
                result += num + " лет ";
            }

            threshold = 30*60*60*24;
            if(workingTimeSec > threshold){
                long num = workingTimeSec/threshold;
                workingTimeSec -= threshold * num;
                result += num + " мес. ";
            }

            threshold = 60*60*24;
            if(workingTimeSec > threshold){
                long num = workingTimeSec/threshold;
                workingTimeSec -= threshold * num;
                result += num + " дн. ";
            }

            threshold = 60*60;
            if(workingTimeSec > threshold){
                long num = workingTimeSec/threshold;
                workingTimeSec -= threshold * num;
                result += num + " час. ";
            }

            threshold = 60;
            if(workingTimeSec > threshold){
                long num = workingTimeSec/threshold;
                workingTimeSec -= threshold * num;
                result += num + " мин. ";
            }

            result += workingTimeSec + " сек. ";
            return result;
        }
        public static String readFromResource(int resourceId, Resources resources) {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(resources.openRawResource(resourceId)));
                try {
                    StringBuilder sb = new StringBuilder();
                    String line = br.readLine();

                    while (line != null) {
                        sb.append(line);
                        sb.append("\n");
                        line = br.readLine();
                    }
                    return sb.toString();
                } finally {
                    br.close();
                }
            }
            catch (Exception e){
                e.printStackTrace();
                return "";
            }
        }
        public static String readFromFile(File file) {
            try {
                BufferedReader br = new BufferedReader(new java.io.FileReader(file));
                try {
                    StringBuilder sb = new StringBuilder();
                    String line = br.readLine();

                    while (line != null) {
                        sb.append(line);
                        sb.append("\n");
                        line = br.readLine();
                    }
                    return sb.toString();
                } finally {
                    br.close();
                }
            }
            catch (Exception e){
                e.printStackTrace();
                return "";
            }
        }
        public static boolean copyFile(String from, String to) {
            File f1 = new File(from);
            File f2 = new File(to);
            return copyFile(f1, f2);
        }
        public static boolean copyFile(File f1, File f2){
            try{
                InputStream in = new FileInputStream(f1);

                //For Append the file.
                //OutputStream out = new FileOutputStream(f2,true);

                //For Overwrite the file.
                OutputStream out = new FileOutputStream(f2);

                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0){
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
                //ApplicationManager.log("Копирование успешно.");
                return true;
            }
            catch(Exception ex){
                //ApplicationManager.log("Ошибка копирования: " + ex.toString());
                return false;
            }
        }
        public static void copyFile(int fromResourceId, Resources resources, File to)throws Exception{
            if(!to.getParentFile().isDirectory()){
                to.getParentFile().mkdir();
            }
            OutputStream out = new FileOutputStream(to, false);
            InputStream in = resources.openRawResource(fromResourceId);
            try {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0){
                    out.write(buf, 0, len);
                }
            } finally {
                in.close();
                out.close();
            }
        }
        @RequiresApi(api = Build.VERSION_CODES.N)
        public static void unzip(File zipFile, File targetDirectory) throws IOException {
            try{
                unzipWithEncoding(zipFile, targetDirectory, "UTF-8");
            }
            catch (IllegalArgumentException e){
                try{
                    unzipWithEncoding(zipFile, targetDirectory, "CP866");
                }
                catch (IllegalArgumentException e1){
                    try{
                        unzipWithEncoding(zipFile, targetDirectory, "CP437");
                    }
                    catch (IllegalArgumentException e2){
                        throw  e2;
                    }
                }
            }
        }
        @RequiresApi(api = Build.VERSION_CODES.N)
        public static void unzipWithEncoding(File zipFile, File targetDirectory, String encoding) throws IOException {
            ZipInputStream zis = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(zipFile)), Charset.forName(encoding));
            try {
                ZipEntry ze;
                int count;
                byte[] buffer = new byte[8192];
                while ((ze = zis.getNextEntry()) != null) {
                    File file = new File(targetDirectory, ze.getName());
                    File dir = ze.isDirectory() ? file : file.getParentFile();
                    if (!dir.isDirectory() && !dir.mkdirs())
                        throw new FileNotFoundException("Failed to ensure directory: " +
                                dir.getAbsolutePath());
                    if (ze.isDirectory())
                        continue;
                    FileOutputStream fout = new FileOutputStream(file);
                    try {
                        while ((count = zis.read(buffer)) != -1)
                            fout.write(buffer, 0, count);
                    } finally {
                        fout.close();
                    }
            /* if time should be restored as well
            long time = ze.getTime();
            if (time > 0)
                file.setLastModified(time);
            */
                }
            } finally {
                zis.close();
            }
        }
        public static void zip(String[] _files, String parentPath, String zipFileName) throws Exception {
            ZipOutputStream out = null;
            try {
                int BUFFER = 1024;
                BufferedInputStream origin = null;
                FileOutputStream dest = new FileOutputStream(zipFileName);
                out = new ZipOutputStream(new BufferedOutputStream(dest));
                byte[] data = new byte[BUFFER];

                for (int i = 0; i < _files.length; i++) {
                    Log.d("Compress", "Adding: " + _files[i].replace(parentPath, ""));
                    FileInputStream fi = new FileInputStream(_files[i]);
                    origin = new BufferedInputStream(fi, BUFFER);

                    ZipEntry entry = new ZipEntry(_files[i].replace(parentPath, ""));
                    out.putNextEntry(entry);
                    int count;

                    while ((count = origin.read(data, 0, BUFFER)) != -1) {
                        out.write(data, 0, count);
                    }
                    origin.close();
                }
            } finally {
                if(out != null)
                    out.close();
            }
        }
        public static int countLines(String filename) {
            try {
                InputStream is = new BufferedInputStream(new FileInputStream(filename));
                byte[] c = new byte[1024];
                int count = 0;
                int readChars = 0;
                boolean empty = true;
                while ((readChars = is.read(c)) != -1) {
                    empty = false;
                    for (int i = 0; i < readChars; ++i) {
                        if (c[i] == '\n') {
                            ++count;
                        }
                    }
                }
                is.close();
                return (count == 0 && !empty) ? 1 : count;
            }
            catch (Exception e){
                return 0;
            }
        }
        public static String filterSymbols(String input, String allowedSymbols){
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if(allowedSymbols.indexOf(c) >= 0)
                    builder.append(c);
            }
            return builder.toString();
        }
        public static ArrayList<Integer> parseRange(String input, int total, int maximum) throws Exception{
            //эта функция принимает строку и возвращает массив который соответствует диапазону
            // total - общее количество обьектов среди которых выбирается диапазон
            // maximum - максимальный размер диапазона, который нельзя превышать
            //
            // . 12
            // . 23,34
            // . 23-45
            // . 23-
            // . -34

            // Можно использовать любые диапазоны чисел. Примеры:
            // 1 : Только первый объект;
            // 12-20 : Объекты с 12-го по 20-й;
            // 15- : С 20-го и до конца;
            // -22 : От начала до 22-го;
            // 1, 5, 7 : первый, пятый и седьмой;
            // 1, 5, 12-15 : первый, пятый и с 12-го по 15-й;
            // -10, 86, 90 : первые 10, 86-й и 90-й;

        /*
        *
        *           "Для просмотра можно использовать любые диапазоны чисел. Например:\n" +
                    "1 ::: Только первый объект;\n" +
                    "12-20 ::: Объекты с 12-го по 20-й;\n" +
                    "15- ::: С 20-го и до конца;\n" +
                    "-22 ::: От начала до 22-го;\n" +
                    "1,5,7 ::: первый, пятый и седьмой;\n" +
                    "1,5,12-15 ::: первый, пятый и с 12-го по 15-й;\n" +
                    "-10,86,90 ::: первые 10, 86-й и 90-й;",
        * */

            input = input.replace(" ", "").trim();
            ArrayList<Integer> result = new ArrayList<>();

            if(total < 1)
                throw new Exception("В базе нет объектов.");
            if(maximum < 1)
                throw new Exception("Получен некорректный параметр maximum.");
            if(input.length() > 200){
                throw new Exception("Слишком сложный диапазон. Его разбор может привести к поломке программы.");
            }
            if(input.equals("")) {
                //если не указан диапазон, выбрать максимальный сначала
                for(int i=0; i<Math.min(total, maximum); i++)
                    result.add(i);
                return result;
            }
            if(!input.matches("[0-9,\\-]+")){
                //если есть что-то кроме 1234567890-,
                throw new Exception("Диапазон содержит недопустимые символы. " +
                        "Можно использовать только числа, тире и запятую для задания диапазона.");
            }
            String[] ranges = input.split(",");
            for (String range:ranges){
                //если нет тире
                if(!range.contains("-")){
                    try{
                        result.add(Integer.parseInt(range));
                    }
                    catch (Exception e){
                        throw new Exception("Не могу распознать \"" + range + "\" как число.");
                    }
                }
                //если более одного тире
                int numberOfTires = range.length() - range.replace("-", "").length();
                if(numberOfTires > 1) {
                    throw new Exception("Диапазон \"" + range + "\" содержит более одного тире. " +
                            "Я не понимаю что это должно значить. " +
                            "Чтобы использовать диапазон, надо писать числа: ОТ-ДО.");
                }
                //если ТОЛЬКО тире
                if(range.equals("-")) {
                    throw new Exception("Диапазон \"" + range + "\" содержит только тире. " +
                            "Я не понимаю что это должно значить. " +
                            "Чтобы использовать диапазон, надо писать числа: ОТ-ДО.");
                }
                //если указан только конец диапазона (-12)
                if(range.startsWith("-")){
                    try{
                        int max = Integer.parseInt(range.replace("-", ""));
                        max = Math.min(max, Math.min(total, maximum));
                        for(int i=0; i<max; i++)
                            result.add(i);
                    }
                    catch (Exception e){
                        throw new Exception("Не могу распознать \"" + range + "\" как диапазон от начала.");
                    }
                }
                //если указано только начало диапазона (12-)
                else if(range.endsWith("-")){
                    try{
                        int n1 = Integer.parseInt(range.replace("-", ""));
                        int min = Math.min(n1, total);
                        int max = Math.max(n1, total);
                        if(max > total)
                            max = total;
                        if(max - min > maximum)
                            max = min + maximum;
                        for(int i=min; i<max; i++)
                            result.add(i);
                    }
                    catch (Exception e){
                        throw new Exception("Не могу распознать \"" + range + "\" как диапазон до конца.");
                    }
                }
                //если указаны оба числа диапазона (10-20)
                else{
                    try{
                        String[] numbers = range.split("\\-");
                        int n1 = Integer.parseInt(numbers[0]);
                        int n2 = Integer.parseInt(numbers[0]);
                        int min = Math.min(n1, n2);
                        int max = Math.max(n1, n2);
                        if(max > total)
                            max = total;
                        if(max - min > maximum)
                            max = min + maximum;
                        for(int i=min; i<max; i++)
                            result.add(i);
                    }
                    catch (Exception e){
                        throw new Exception("Не могу распознать \"" + range + "\" как диапазон между двумя числами.");
                    }
                }
            }
            if(result.size() == 0)
                throw new Exception("Диапазон не соответствует ни одной ячейке.");
            return result;
        }
        public static String makeBeginWithLower(String in){
            if(in.length() < 1)
                return in;
            //24,11 Исправлено подставление малой буквы в слова написанные капсом
            String firstWord = in.split(" ")[0];
            boolean firstWordHasLowercase = !firstWord.equals(firstWord.toUpperCase());
            if(firstWordHasLowercase) {
                String first = String.valueOf(in.charAt(0));
                return first.toLowerCase() + in.substring(1);
            }
            return in;
        }
        public static String makeBeginWithUpper(String in){
            String first = String.valueOf(in.charAt(0));
            return first.toUpperCase() + in.substring(1);
        }
//        public static String commandDescsToText(ArrayList<CommandDesc> commandDescs){
//            StringBuilder stringBuilder = new StringBuilder();
//            for(CommandDesc commandDesc:commandDescs)
//                stringBuilder.append(commandDescToText(commandDesc));
//            return stringBuilder.toString();
//        }
//        public static String commandDescToText(CommandDesc commandDesc) {
//            return "[[ " + commandDesc.getName() + "]]\n" +
//                    commandDesc.getHelpText() + "\n" +
//                    "---| " + commandDesc.getExample() + "\n\n";
//        }
        public static ArrayList<Long> integerArrayToLongArray(ArrayList<Integer> array){
            ArrayList<Long> toShowLong = new ArrayList<>();
            for (Integer integer:array)
                toShowLong.add((long)integer);
            return toShowLong;
        }
        public static String getDateTimeString(Date date) {
            Calendar now = Calendar.getInstance();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(date.getTime());

            // Device and server clock can be a bit not synchronized
            // Most likely the difference is few seconds
            // Show 'just now' if < 30 secs elapsed
            if (now.getTimeInMillis() - calendar.getTimeInMillis() < 30000) {
                return "Только что";
            }

            // Past one hour
            long oneHour = 60*60*1000;
            if (now.getTimeInMillis() - calendar.getTimeInMillis() < oneHour) {
                int minutes = (int) ((now.getTimeInMillis() - calendar.getTimeInMillis()) / 1000 / 60);
                if (minutes == 0) minutes = 1;
                return "Час назад";
            }

            int nowDay = now.get(Calendar.DAY_OF_YEAR);
            int cDay = calendar.get(Calendar.DAY_OF_YEAR);
            int nowYear = now.get(Calendar.YEAR);
            int cYear = calendar.get(Calendar.YEAR);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            // Today
            if (nowDay == cDay && nowYear == cYear) {
                return "Сегодня в " + hour + ":" + minute;
            }

            // Yesterday
            if (nowDay - 1 == cDay && nowYear == cYear || nowDay == 1 && nowYear - 1 == cYear) {
                return "Вчера в " + hour + minute;
            }

            int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
            int month = calendar.get(Calendar.MONTH) + 1;
            // Same year
            if (nowYear == cYear) {
                return dayOfMonth + "-" + month + " в " + hour + ":" + minute;
            }
            // Default
            else {
                return dayOfMonth + "-" + month + "-" + cYear + " в " + hour + ":" + minute;
            }
        }
        public static Date timeMillisToDate(long timeMillis){
            long now = System.currentTimeMillis();
            long difference = now - timeMillis;
            Date date = new Date();
            date.setTime(date.getTime() - difference);
            return date;
        }
        public static long randomLong(long min, long max){
            try
            {
                Random random  = new Random();
                return min + (long) (random.nextDouble() * (max - min));
            }
            catch (Throwable t) {t.printStackTrace();}
            return 0L;
        }
        public static String replaceCaseInsensitive(String text, String pattern, String toReplace){
            //Hello %USERnAme%
            //0123456789012345678901234567890
            pattern = pattern.toLowerCase();
            for(int index = text.toLowerCase().indexOf(pattern); index != -1; index = text.toLowerCase().indexOf(pattern)){
                text = text.substring(0, index) + toReplace + text.substring(index + toReplace.length(), text.length());
            }
            return text;
        }
        public static ArrayList<Object> intersect(ArrayList<Object> array1, ArrayList<Object> array2){
            //выводит в общий массив только те объекты, которые есть в обоих массивах
            ArrayList<Object> result = new ArrayList<>();
            for (Object object:array1)
                if(array2.contains(object))
                    result.add(object);
            return result;
        }
        public static String[] arrayListToArray(ArrayList<String> input){
            String[] result = new String[input.size()];
            for (int i = 0; i < input.size(); i++) {
                result[i] = input.get(i);
            }
            return result;
        }
        public static ArrayList<String> arrayToArrayList(String[] input){
            ArrayList<String> result = new ArrayList<>();
            for (String s:input) {
                result.add(s);
            }
            return result;
        }
        public static float avgValue(float[] data){
            int totalDays = data.length;
            if(totalDays == 0)
                return 0;
            float sum = 0;
            for (int i = 0; i < totalDays; i++)
                sum += data[i];
            float avg = sum / totalDays;
            return avg;
        }
        public static float avgValue(ArrayList<Float> data){
            int totalDays = data.size();
            if(totalDays == 0)
                return 0;
            float sum = 0;
            for (int i = 0; i < totalDays; i++)
                sum += data.get(i);
            float avg = sum / totalDays;
            return avg;
        }
        public static float maxValue(float[] data){
            int totalDays = data.length;
            if(totalDays == 0)
                return 0;
            float max = totalDays == 0?0:data[0];
            for (int i = 0; i < totalDays; i++)
                if(data[i] > max)
                    max = data[i];
            return max;
        }
        public static float minValue(float[] data){
            int totalDays = data.length;
            if(totalDays == 0)
                return 0;
            float min = totalDays == 0?0:data[0];
            for (int i = 0; i < totalDays; i++)
                if(data[i] < min)
                    min = data[i];
            return min;
        }
        public static String getRandomString(int len ){
            SecureRandom rnd = new SecureRandom();
            final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
            StringBuilder sb = new StringBuilder( len );
            for( int i = 0; i < len; i++ )
                sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
            return sb.toString();
        }
        public static boolean isSameDay(Date date1, Date date2){
            if(date1 == null && date2 == null)
                return true;
            if(date1 != null && date2 == null)
                return false;
            if(date1 == null && date2 != null)
                return false;
            Calendar cal1 = Calendar.getInstance();
            Calendar cal2 = Calendar.getInstance();
            cal1.setTime(date1);
            cal2.setTime(date2);
            boolean sameDay = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
            return sameDay;
        }
        public static boolean isToday(Date date){
            return isSameDay(date, new Date());
        }
        public static boolean isSameMinute(Date date1, Date date2){
            if(date1 == null && date2 == null)
                return true;
            if(date1 != null && date2 == null)
                return false;
            if(date1 == null && date2 != null)
                return false;
            Calendar cal1 = Calendar.getInstance();
            Calendar cal2 = Calendar.getInstance();
            cal1.setTime(date1);
            cal2.setTime(date2);
            boolean sameDay = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
                    cal1.get(Calendar.HOUR_OF_DAY) == cal2.get(Calendar.HOUR_OF_DAY) &&
                    cal1.get(Calendar.MINUTE) == cal2.get(Calendar.MINUTE);
            return sameDay;
        }
        public static String howMuchTimeAgo(Date date){
            Date now = new Date();
            Map<TimeUnit,Long> diffMap = computeDiffBetweenDates(date, now);
            //Log.d("FeedElement", "DateDiff = " + diffMap.toString());
            String diffStr = "";
            if(diffMap.containsKey(TimeUnit.DAYS) && diffMap.get(TimeUnit.DAYS) > 365)
                diffStr += (diffMap.get(TimeUnit.DAYS)/365)+"y";
            else if(diffMap.containsKey(TimeUnit.DAYS) && diffMap.get(TimeUnit.DAYS) > 30)
                diffStr += (diffMap.get(TimeUnit.DAYS)/30)+"m";
            else if(diffMap.containsKey(TimeUnit.DAYS) && diffMap.get(TimeUnit.DAYS) > 0)
                diffStr += diffMap.get(TimeUnit.DAYS)+"d";
            else if(diffMap.containsKey(TimeUnit.HOURS) && diffMap.get(TimeUnit.HOURS) > 0)
                diffStr += diffMap.get(TimeUnit.HOURS)+"h";
            else if(diffMap.containsKey(TimeUnit.MINUTES) && diffMap.get(TimeUnit.MINUTES) > 0)
                diffStr += diffMap.get(TimeUnit.MINUTES)+"min";
            else if(diffMap.containsKey(TimeUnit.SECONDS) && diffMap.get(TimeUnit.SECONDS) > 0)
                diffStr += diffMap.get(TimeUnit.SECONDS)+"s";
            else
                diffStr += new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).format(date);
            return diffStr;
        }
        public static Map<TimeUnit,Long> computeDiffBetweenDates(Date date1, Date date2) {
            long diffInMillies = date2.getTime() - date1.getTime();
            List<TimeUnit> units = new ArrayList<TimeUnit>(EnumSet.allOf(TimeUnit.class));
            Collections.reverse(units);
            Map<TimeUnit,Long> result = new LinkedHashMap<TimeUnit,Long>();
            long milliesRest = diffInMillies;
            for ( TimeUnit unit : units ) {
                long diff = unit.convert(milliesRest, TimeUnit.MILLISECONDS);
                long diffInMilliesForUnit = unit.toMillis(diff);
                milliesRest = milliesRest - diffInMilliesForUnit;
                result.put(unit,diff);
            }
            return result;
        }
        public static boolean isDigitsOnly(String str) {
            final int len = str.length();
            for (int i = 0; i < len; i++) {
                if (!Character.isDigit(str.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
        public static JSONArray hashMapToJsonArray(HashMap<Long, Integer> hashMap) throws Exception{
            JSONArray jsonArray = new JSONArray();
            Set<Map.Entry<Long, Integer>> set = hashMap.entrySet();
            Iterator<Map.Entry<Long, Integer>> iterator = set.iterator();
            while (iterator.hasNext()){
                Map.Entry<Long, Integer> entry = iterator.next();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("key", entry.getKey());
                jsonObject.put("value", entry.getValue());
                jsonArray.put(jsonObject);
            }
            return jsonArray;
        }
        public static HashMap<Long, Integer> hashMapFromJsonArray(JSONArray jsonArray) throws Exception{
            HashMap<Long, Integer> result = new HashMap<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                long key = jsonObject.getLong("key");
                int value = jsonObject.getInt("value");
                result.put(key, value);
            }
            return result;
        }


}
