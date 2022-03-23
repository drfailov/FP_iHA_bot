package com.fsoft.ihabot.communucation;

import com.fsoft.ihabot.Utils.FileStorage;

public interface AccountBase
{
    public boolean remove();
    public void login();
    public void startAccount();
    public void stopAccount();
    public boolean isMine(String commandTreatment);
    public boolean isToken_ok();
    public boolean isEnabled();
    public boolean isRunning();
    public String getState();
    public FileStorage getFileStorage();
    public long getId();
    public String getToken();
    public String getFileName();
    public void setEnabled(boolean enabled);
    public String state(String state);
    public void setState(String state);
    public void setId(long id);
    public void setToken(String token);
    public void setToken_ok(boolean token_ok);
    public String getScreenName();
    public void setScreenName(String screenName);
}
