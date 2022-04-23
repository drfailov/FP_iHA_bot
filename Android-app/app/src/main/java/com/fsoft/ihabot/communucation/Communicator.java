package com.fsoft.ihabot.communucation;

import com.fsoft.ihabot.ApplicationManager;
import com.fsoft.ihabot.Utils.CommandModule;
import com.fsoft.ihabot.Utils.FileStorage;
import com.fsoft.ihabot.communucation.tg.TgAccount;

import java.util.ArrayList;

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
    }
    public void startCommunicator(){
        log("Запуск коммуникатора...");
        running = true;
        for(TgAccount tgAccount:tgAccounts) {
            if(tgAccount.isEnabled())
                tgAccount.startAccount();
        }
    }
    public void stopCommunicator(){
        running = false;
        for(TgAccount tgAccount:tgAccounts) {
            tgAccount.stopAccount();
        }
    }
    public TgAccount getTgAccount(long id){
        for (TgAccount account : tgAccounts)
            if (account.getId() == id)
                return account;
        return null;
    }
    public ArrayList<TgAccount> getTgAccounts() {
        return tgAccounts;
    }

    public boolean containsTgAccount(long id){
        return getTgAccount(id) != null;
    }
    public void addAccount(TgAccount tgAccount) throws Exception{
        if(tgAccount == null)
            throw new Exception("Аккаунт не получен");
        if(containsTgAccount(tgAccount.getId())) {
            tgAccount.stopAccount();
            throw new Exception("Такой аккаунт уже есть");
        }
        tgAccounts.add(tgAccount);

        String[] accountList = new String[tgAccounts.size()];
        for (int i = 0; i < tgAccounts.size(); i++)
            accountList[i] = tgAccounts.get(i).getFileName();
        file.put("TGaccounts", accountList).commit();
        log("Аккаунт " + tgAccount.getId() + " добавлен. Список аккаунтов сохранён.");
    }
    public void remAccount(TgAccount accountToRemove) throws Exception{
        if(accountToRemove == null)
            throw new Exception("Функция удаления TG аккаунта вызвана с аргументом null");
        if(running)
            accountToRemove.stopAccount();
        tgAccounts.remove(accountToRemove);

        String[] accountList = new String[tgAccounts.size()];
        for (int i = 0; i < tgAccounts.size(); i++)
            accountList[i] = tgAccounts.get(i).getFileName();
        file.put("TGaccounts", accountList).commit();

        if(accountToRemove.remove())
            log("Аккаунт TG " + accountToRemove + " успешно удалён.");
        else
            throw new Exception("Аккаунт TG " + accountToRemove + " удалён из списка, но его файл удалить не получается.");
    }

    @Override
    public void stop() {
        stopCommunicator();
        super.stop();
    }
}
