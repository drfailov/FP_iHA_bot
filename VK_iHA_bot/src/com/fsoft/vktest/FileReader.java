package com.fsoft.vktest;

import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

import java.io.*;

/**
 * класс для работы с файлами
 * Created by Dr. Failov on 21.09.2014.
 */
public class FileReader {
    private Resources resources ;
    private int resourceId;
    private String folderName;
    private String fileName;
    private String fileFolder;
    private String filePath;
    private File file;

    static public String readFromFile(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new java.io.FileReader(fileName));
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
    static public boolean copyFile(String from, String to){
        try{
            File f1 = new File(from);
            File f2 = new File(to);
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
            ApplicationManager.log("Копирование успешно.");
            return true;
        }
        catch(Exception ex){
            ApplicationManager.log("Ошибка копирования: " + ex.toString());
            return false;
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

    public FileReader(Resources resources, int resourceId, String folderName) {
        this.resources = resources;
        this.resourceId = resourceId;
        this.folderName = folderName;
        fileName = resources.getResourceEntryName(resourceId);
        fileFolder = ApplicationManager.getHomeFolder() + File.separator + folderName;
        filePath = fileFolder + File.separator + fileName + ".bin";
        file = new File (filePath);
    }
    public File getFile(){
        return file;
    }
    public String readFile(){
        try {
            log(". чтение "+filePath+"...");
            if (!file.exists()) {
                log(". файла нет. Чтение ресурса...");
                return checkString(readResource(resourceId));
            }
            String fromFile = checkString(readFromFile(filePath));
            if(fromFile == null) {
                log(". файл пуст. Чтение ресурса...");
                return checkString(readResource(resourceId));
            }
            return fromFile;
        }
        catch (Exception e){
            log(". ошибка чтения файла: " + e.toString());
            e.printStackTrace();
            return "";
        }
    }
    public boolean writeFile(String toWrite){
        try{
            boolean folderExists = checkFolder(fileFolder);
            if(folderExists){
                log(". запись "+filePath+"...");
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write(toWrite);
                fileWriter.close();
                return true;
            }
            else {
                log(". ошибка создания папки "+fileFolder);
                return false;
            }
        }
        catch (Exception e){
            e.printStackTrace();
            log(". ошибка записи: "+e.toString());
            return false;
        }
    }
    public String getFilePath(){
        return filePath;
    }
    public String getFileName(){
        return file.getName();
    }

    private boolean checkFolder(String folder){
        File path = new File(folder);
        //если надо - создать папку
        boolean exist = path.isDirectory();
        if(!exist) {
            log(". Создание папки " + path + "...");
            exist = path.mkdirs();
            //если папку созать не удалось
            if(!exist){
                log("! Создать папку не удалось: \n " + path);
            }
        }
        return exist;
    }
    private String checkString(String in){
        if(in == null || in.length() < 3)
            return null;
        return in;
    }
    private void log(String text){
        ApplicationManager.log(text);
    }
    private String readResource(int fileResource){
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(resources.openRawResource(fileResource)));
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
        }catch (Exception e){
            return  e.toString();
        }
    }


}
