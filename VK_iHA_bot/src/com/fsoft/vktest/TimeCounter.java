package com.fsoft.vktest;

import java.util.*;

/**
 * класс для учета времени чтобы понять сколько событий sender произошло за последние ... секунд
 * Created by Dr. Failov on 15.09.2014.
 */
public class TimeCounter {
    HashMap<Long, ArrayList<Long>> db = new HashMap<>();
    void add(Long senderId){
        if(!db.containsKey(senderId)) {
            db.put(senderId, new ArrayList<Long>());
        }
        long time = System.currentTimeMillis();
        db.get(senderId).add(time);
        clearOld();
    }
    int countLastSec(Long senderId, long sec){
        if(!db.containsKey(senderId)) {
            return 0;
        }
        int result = 0;
        Long currentTime = System.currentTimeMillis();
        ArrayList<Long> list = db.get(senderId);
        for (int i = 0; i < list.size(); i++) {
            long dif = currentTime - list.get(i);
            if(dif < sec * 1000)
                result ++;
        }
        return result;
    }
    void clearOld(){
        Set<Map.Entry<Long, ArrayList<Long>>> entries = db.entrySet();
        long now = System.currentTimeMillis();
        long oldThreshold = 86400000; //24hours
        Iterator<Map.Entry<Long, ArrayList<Long>>> iterator = entries.iterator();
        while (iterator.hasNext()){
            Map.Entry<Long, ArrayList<Long>> entry = iterator.next();
            long senderId = entry.getKey();
            ArrayList<Long> values= entry.getValue();
            for (int i = 0; i < values.size(); i++) {
                long value = values.get(i);
                long old = now - value;
                if(old > oldThreshold){
                    values.remove(value);
                }
            }
        }
    }
}
