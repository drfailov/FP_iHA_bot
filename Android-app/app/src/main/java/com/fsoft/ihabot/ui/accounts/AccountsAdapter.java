package com.fsoft.ihabot.ui.accounts;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.fsoft.ihabot.R;
import com.fsoft.ihabot.ApplicationManager;
import com.fsoft.ihabot.Utils.F;
import com.fsoft.ihabot.communucation.Communicator;
import com.fsoft.ihabot.communucation.tg.TgAccount;
import com.fsoft.ihabot.communucation.tg.TgAccountCore;
import com.squareup.picasso.Picasso;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import jp.wasabeef.picasso.transformations.CropCircleTransformation;

public class AccountsAdapter extends BaseAdapter {
    Activity activity = null;
    ApplicationManager applicationManager = null;
    Communicator communicator = null;
    /**
     * Дело в том, что когда создаётся этот адаптер, данных в сервисе ещё не существует.
     * Поэтому нужно попробовать повторно их достать когда сервис уже работает.
     * Задача этого таймера состоит в том чтобы обновлять данные о ApplicationManager до тех пор, пока он не будет создан.
     * ПОсле того как успешно получен Communictor (с которого и надо показывать данные), это таймер ликвидируется.
     */
    Timer timerToUpdateView = null;

    public AccountsAdapter(Activity activity) {
        this.activity = activity;
        applicationManager = ApplicationManager.getInstance();
        if(applicationManager != null)
            communicator = applicationManager.getCommunicator();
        timerToUpdateView = new Timer();
        timerToUpdateView.schedule(new TimerTask() {
            @Override
            public void run() {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }
        }, 1000, 1000);
    }

    @Override
    public void notifyDataSetChanged() {
        if(applicationManager == null)
            applicationManager = ApplicationManager.getInstance();
        if(applicationManager != null && communicator == null)
            communicator = applicationManager.getCommunicator();
        if(communicator != null)
            timerToUpdateView.cancel();
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        if(applicationManager == null)
            applicationManager = ApplicationManager.getInstance();
        if(applicationManager != null && communicator == null)
            communicator = applicationManager.getCommunicator();
        if(communicator != null)
            timerToUpdateView.cancel();
        super.notifyDataSetInvalidated();
    }

    @Override
    public int getCount() {
        if(communicator == null) {
            return 0;
        }
        return communicator.getTgAccounts().size();
    }

    @Override
    public Object getItem(int i) {
        return null;
//        if(communicator == null) return null;
//        return communicator.getTgAccounts().get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        if (convertView == null) {
            convertView = activity.getLayoutInflater().inflate(R.layout.fragment_accounts_list_item, container, false);
        }

        TgAccount tgAccount = communicator.getTgAccounts().get(position);
        if(tgAccount == null) { //если юзер пустой, надо загрузить по новой, чтобы там не висели подвисшие данные с проглого элемента
            return activity.getLayoutInflater().inflate(R.layout.fragment_accounts_list_item, container, false);
        }

        tgAccount.setOnStateChangedListener(new Runnable() { //auto-updating
            @Override
            public void run() {
                if(activity != null) {
                    activity.runOnUiThread(() -> notifyDataSetInvalidated());
                }
            }
        });
        { //NAME
            TextView textView = convertView.findViewById(R.id.item_account_textView_name);
            if(textView != null) {
                if (tgAccount.getScreenName() != null)
                    textView.setText(tgAccount.getScreenName());
                else
                    textView.setText("Имя неизвестно");
            }
        }
        { //USERNAME
            TextView textView = convertView.findViewById(R.id.item_account_textView_userhame);
            if(textView != null) {
                textView.setText(String.format("@%s", tgAccount.getUserName()));
            }
        }
        { //STATE
            TextView textView = convertView.findViewById(R.id.item_account_textView_status);
            if(textView != null) {
                if(tgAccount.isEnabled()) {
                    if (tgAccount.getState() != null)
                        textView.setText(tgAccount.getState());
                    else
                        textView.setText("Непонятный статус");
                }
                else
                    textView.setText("Аккаунт приостановлен пользователем");
            }
        }
        { //SENT MESSAGES
            TextView textView = convertView.findViewById(R.id.item_account_textView_messages_sent);
            if(textView != null) {
                if (tgAccount.getMessageProcessor() != null)
                    textView.setText(String.format(Locale.getDefault(),"%d шт", tgAccount.getMessageProcessor().getMessagesSentCounter()));
                else
                    textView.setText("Непонятно");
            }
        }
        { //RECEIVED MESSAGES
            TextView textView = convertView.findViewById(R.id.item_account_textView_messages_received);
            if(textView != null) {
                if (tgAccount.getMessageProcessor() != null)
                    textView.setText(String.format(Locale.getDefault(),"%d шт", tgAccount.getMessageProcessor().getMessagesReceivedCounter()));
                else
                    textView.setText("Непонятно");
            }
        }
        { //API COUNTER
            TextView textView = convertView.findViewById(R.id.item_account_textView_api_counter);
            if(textView != null) {
                textView.setText(String.format(Locale.getDefault(),"%d шт", tgAccount.getApiCounter()));
            }
        }
        { //API ERRORS
            TextView textView = convertView.findViewById(R.id.item_account_textView_api_errors);
            if(textView != null) {
                textView.setText(String.format(Locale.getDefault(),"%d шт", tgAccount.getErrorCounter()));
            }
        }
        {//photo
            ImageView imageView = convertView.findViewById(R.id.item_account_imageview_avatar);
            if(imageView != null) {
                tgAccount.getMyPhotoUrl(new TgAccountCore.GetUserPhotoListener() {
                    @Override
                    public void gotPhoto(String url) {
                        imageView.post(new Runnable() {
                            @Override
                            public void run() {
                                Picasso.get()
                                        .load(url)
                                        .placeholder(R.drawable.tg_account_placeholder)
                                        .transform(new CropCircleTransformation())
                                        .into(imageView);
                            }
                        });
                    }

                    @Override
                    public void noPhoto() {
                        imageView.post(new Runnable() {
                            @Override
                            public void run() {
                                Picasso.get()
                                        .load(R.drawable.tg_account_placeholder)
                                        .transform(new CropCircleTransformation())
                                        .into(imageView);
                            }
                        });

                    }

                    @Override
                    public void error(Throwable error) {
                        imageView.post(new Runnable() {
                            @Override
                            public void run() {
                                Picasso.get()
                                        .load(R.drawable.tg_account_placeholder)
                                        .transform(new CropCircleTransformation())
                                        .into(imageView);
                            }
                        });

                        error.printStackTrace();
                    }
                });
            }
            else {
                Log.d(F.TAG, "R.id.item_account_imageview_avatar is null =(");
            }
        }
        {//pause
            ImageView imageView = convertView.findViewById(R.id.item_account_imageview_pause);
            if(imageView != null) {
                if(tgAccount.isEnabled())
                    imageView.setVisibility(View.GONE);
                else
                    imageView.setVisibility(View.VISIBLE);
            }
        }

        return convertView;
    }
}
