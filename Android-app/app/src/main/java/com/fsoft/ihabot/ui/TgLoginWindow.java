package com.fsoft.ihabot.ui;


import android.app.Activity;
import android.app.Dialog;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.fsoft.ihabot.R;
import com.fsoft.ihabot.Utils.ApplicationManager;
import com.fsoft.ihabot.Utils.CommandModule;
import com.fsoft.ihabot.communucation.tg.TgAccount;
import com.fsoft.ihabot.communucation.tg.TgAccountCore;
import com.fsoft.ihabot.communucation.tg.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Это окно должно заниматься процедурой логина в аккаунт.
 * В его задачи входит:
 * - получить обьект аккаунта который нужно залогинить
 * - открыть окно логина и ждать пока пользователь залогинится
 * - проверить токен на валидность
 * - как только токен получен, закрыться и сообщить об успешном логине
 * - самостоятельно задать токен в обьекте vkAccount и сделать Start Account
 *
 *
 * Created by Dr. Failov on 22.07.2018.
 */

public class TgLoginWindow extends CommandModule {
    private Handler handler = new Handler();
    private TgAccount tgAccount = null;
    private Dialog loginDialog = null;
    private Activity activity = null;
    private OnSuccessfulLoginListener onSuccessfulLoginListener = null;

    private EditText tokenField;
    private TextView saveButton;
    private View closeButton;

    public TgLoginWindow(Activity activity, OnSuccessfulLoginListener onSuccessfulLoginListener) {
        super();
        this.activity = activity;
        this.tgAccount = new TgAccount(ApplicationManager.getInstance(), "acc" + new SimpleDateFormat("_yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".json");
        this.onSuccessfulLoginListener = onSuccessfulLoginListener;
        showLoginWindow();
    }

    private void showLoginWindow(){
        if(loginDialog == null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        log("Войди в аккаунт");
                        loginDialog = new Dialog(activity);
                        loginDialog.setCancelable(false);
                        loginDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        loginDialog.setContentView(R.layout.dialog_add_telegram_account);
                        loginDialog.getWindow().getAttributes().width = WindowManager.LayoutParams.MATCH_PARENT;
                        tokenField = loginDialog.findViewById(R.id.dialog_add_telegram_account_field_token);
                        saveButton = loginDialog.findViewById(R.id.dialogAdd_telegram_accountButtonSave);
                        closeButton = loginDialog.findViewById(R.id.dialogadd_telegram_accountButtonCancel);
                        if(saveButton != null)
                            saveButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    saveButton();
                                }
                            });
                        if(closeButton != null)
                            closeButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    closeLoginWindow();
                                    //String resultOfDeletion = applicationManager.getCommunicator().remTgAccount(tgAccount);
                                    //Toast.makeText(activity, resultOfDeletion, Toast.LENGTH_SHORT).show();
//                                    if(howToRefresh != null)
//                                        howToRefresh.run();
                                }
                            });
                        loginDialog.show();
                    } catch (Throwable e) {
                        e.printStackTrace();
                        log("! Ошибка показа окна логина: " + e.toString());
                    }
                }
            });
        }
    }
    private void closeLoginWindow(){
        if(loginDialog != null) {
            loginDialog.dismiss();
            loginDialog = null;
        }
    }
    private void saveButton() {
        //проверить и если валидно сохранить
        if(tokenField == null)
            return;
        String tokenString = tokenField.getText().toString();
        String[] parts = tokenString.split(":");
        if(parts.length != 2) {
            Toast.makeText(activity, "Токен введён неверно.", Toast.LENGTH_SHORT).show();
            return;
        }
        String idString = parts[0];
        String token = parts[1];
        long id = 0;
        try {
            id = Long.parseLong(idString);
        }
        catch (Exception e){
            Toast.makeText(activity, "Токен введён неверно: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        tgAccount.setId(id);
        tgAccount.setToken(token);
        tgAccount.getMe(new TgAccountCore.GetMeListener() {
            @Override
            public void gotUser(User user) {
                Toast.makeText(activity, "Вход выполнен!", Toast.LENGTH_SHORT).show();
                closeLoginWindow();
                //tgAccount.startAccount();
                if(onSuccessfulLoginListener != null)
                    onSuccessfulLoginListener.onSuccessfulLogin(tgAccount);
            }

            @Override
            public void error(Throwable error) {
                saveButton.setEnabled(true);
                saveButton.setText("Сохранить");
                tgAccount.setId(0);
                tgAccount.setToken("");
                Toast.makeText(activity, "Токен не сработал: " + error.getClass().getName() + " " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        saveButton.setEnabled(false);
        saveButton.setText("Проверка...");
    }

    public interface OnSuccessfulLoginListener{
        void onSuccessfulLogin(TgAccount tgAccount);
    }
}
