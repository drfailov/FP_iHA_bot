package com.fsoft.ihabot.communucation;

import com.fsoft.ihabot.Utils.ApplicationManager;
import com.fsoft.ihabot.Utils.CommandModule;
import com.fsoft.ihabot.Utils.FileStorage;
import com.fsoft.ihabot.communucation.tg.TgAccount;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * class for communication with VK
 * Created by Dr. Failov on 05.08.2014.
 */
public class Communicator extends CommandModule{
    ApplicationManager applicationManager = null;
    private final ArrayList<TgAccount> tgAccounts = new ArrayList<>();
    private FileStorage file = null;
    private boolean running = false;

    public Communicator(ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
        file = new FileStorage("communicator",applicationManager);

        String[] accountList = file.getStringArray("TGaccounts", new String[0]);
        for (String acc:accountList)
            tgAccounts.add(new TgAccount(applicationManager, acc));
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                startCommunicator();
            }
        }, 1000);
    }
    public void startCommunicator(){
        log("Запуск коммуникатора...");
        running = true;
        for(TgAccount tgAccount:tgAccounts)
            tgAccount.startAccount();
    }
    public void stopCommunicator(){
        running = false;
        for(TgAccount tgAccount:tgAccounts)
            tgAccount.stopAccount();
    }
    public TgAccount getTgAccount(long id){
        for (TgAccount account : tgAccounts)
            if (account.getId() == id)
                return account;
        return null;
    }
    public boolean containsTgAccount(long id){
        return getTgAccount(id) != null;
    }
    public void addAccount(TgAccount tgAccount) throws Exception{
        if(tgAccount == null)
            throw new Exception("Аккаунт не получен");
        if(containsTgAccount(tgAccount.getId()))
            throw new Exception("Такой аккаунт уже есть");
        tgAccounts.add(tgAccount);

        String[] accountList = new String[tgAccounts.size()];
        for (int i = 0; i < tgAccounts.size(); i++)
            accountList[i] = tgAccounts.get(i).getFileName();
        file.put("TGaccounts", accountList).commit();
        log("Аккаунт " + tgAccount.getId() + " добавлен. Список аккаунтов сохранён.");
    }

    @Override
    public void stop() {
        stopCommunicator();
        super.stop();
    }
}
