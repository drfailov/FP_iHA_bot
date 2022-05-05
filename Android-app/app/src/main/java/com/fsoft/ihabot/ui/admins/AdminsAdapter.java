package com.fsoft.ihabot.ui.admins;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.fsoft.ihabot.ApplicationManager;
import com.fsoft.ihabot.R;
import com.fsoft.ihabot.Utils.F;
import com.fsoft.ihabot.communucation.Communicator;
import com.fsoft.ihabot.communucation.tg.TgAccount;
import com.fsoft.ihabot.communucation.tg.TgAccountCore;
import com.fsoft.ihabot.communucation.tg.User;
import com.fsoft.ihabot.configuration.AdminList;
import com.squareup.picasso.Picasso;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import jp.wasabeef.picasso.transformations.CropCircleTransformation;

public class AdminsAdapter extends BaseAdapter {
    Activity activity = null;
    ApplicationManager applicationManager = null;
    AdminList adminList = null;
    /**
     * Дело в том, что когда создаётся этот адаптер, данных в сервисе ещё не существует.
     * Поэтому нужно попробовать повторно их достать когда сервис уже работает.
     * Задача этого таймера состоит в том чтобы обновлять данные о ApplicationManager до тех пор, пока он не будет создан.
     * ПОсле того как успешно получен Communictor (с которого и надо показывать данные), это таймер ликвидируется.
     */
    Timer timerToUpdateView = null;

    public AdminsAdapter(Activity activity) {
        this.activity = activity;
        applicationManager = ApplicationManager.getInstance();
        if(applicationManager != null)
            adminList = applicationManager.getAdminList();
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
        if(applicationManager != null && adminList == null)
            adminList = applicationManager.getAdminList();
        if(adminList != null)
            timerToUpdateView.cancel();
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        if(applicationManager == null)
            applicationManager = ApplicationManager.getInstance();
        if(applicationManager != null && adminList == null)
            adminList = applicationManager.getAdminList();
        if(adminList != null)
            timerToUpdateView.cancel();
        super.notifyDataSetInvalidated();
    }

    @Override
    public int getCount() {
        if(adminList == null) {
            return 0;
        }
        return adminList.getUserList().size();
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
            convertView = activity.getLayoutInflater().inflate(R.layout.fragment_admins_list_item, container, false);
        }

        AdminList.AdminListItem adminListItem =  adminList.getUserList().get(position);
        if(adminListItem == null) {
            return activity.getLayoutInflater().inflate(R.layout.fragment_admins_list_item, container, false);
        }
        User user = adminListItem.getUser();
        if(user == null) {
            return activity.getLayoutInflater().inflate(R.layout.fragment_admins_list_item, container, false);
        }

        { //NAME
            TextView textView = convertView.findViewById(R.id.item_admin_textView_name);
            if(textView != null) {
                textView.setText(user.getName());
            }
        }
//        { //USERNAME
//            TextView textView = convertView.findViewById(R.id.item_account_textView_userhame);
//            if(textView != null) {
//                textView.setText(String.format("@%s", tgAccount.getUserName()));
//            }
//        }
//        { //STATE
//            TextView textView = convertView.findViewById(R.id.item_account_textView_status);
//            if(textView != null) {
//                if(tgAccount.isEnabled()) {
//                    if (tgAccount.getState() != null)
//                        textView.setText(tgAccount.getState());
//                    else
//                        textView.setText("Непонятный статус");
//                }
//                else
//                    textView.setText("Аккаунт приостановлен пользователем");
//            }
//        }
//        { //SENT MESSAGES
//            TextView textView = convertView.findViewById(R.id.item_account_textView_messages_sent);
//            if(textView != null) {
//                if (tgAccount.getMessageProcessor() != null)
//                    textView.setText(String.format(Locale.getDefault(),"%d шт", tgAccount.getMessageProcessor().getMessagesSentCounter()));
//                else
//                    textView.setText("Непонятно");
//            }
//        }
//        { //RECEIVED MESSAGES
//            TextView textView = convertView.findViewById(R.id.item_account_textView_messages_received);
//            if(textView != null) {
//                if (tgAccount.getMessageProcessor() != null)
//                    textView.setText(String.format(Locale.getDefault(),"%d шт", tgAccount.getMessageProcessor().getMessagesReceivedCounter()));
//                else
//                    textView.setText("Непонятно");
//            }
//        }
//        { //API COUNTER
//            TextView textView = convertView.findViewById(R.id.item_account_textView_api_counter);
//            if(textView != null) {
//                textView.setText(String.format(Locale.getDefault(),"%d шт", tgAccount.getApiCounter()));
//            }
//        }
//        { //API ERRORS
//            TextView textView = convertView.findViewById(R.id.item_account_textView_api_errors);
//            if(textView != null) {
//                textView.setText(String.format(Locale.getDefault(),"%d шт", tgAccount.getErrorCounter()));
//            }
//        }
//        {//photo
//            ImageView imageView = convertView.findViewById(R.id.item_account_imageview_avatar);
//            if(imageView != null) {
//                tgAccount.getMyPhotoUrl(new TgAccountCore.GetUserPhotoListener() {
//                    @Override
//                    public void gotPhoto(String url) {
//                        imageView.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                Picasso.get()
//                                        .load(url)
//                                        .placeholder(R.drawable.tg_account_placeholder)
//                                        .transform(new CropCircleTransformation())
//                                        .into(imageView);
//                            }
//                        });
//                    }
//
//                    @Override
//                    public void noPhoto() {
//                        imageView.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                Picasso.get()
//                                        .load(R.drawable.tg_account_placeholder)
//                                        .transform(new CropCircleTransformation())
//                                        .into(imageView);
//                            }
//                        });
//
//                    }
//
//                    @Override
//                    public void error(Throwable error) {
//                        imageView.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                Picasso.get()
//                                        .load(R.drawable.tg_account_placeholder)
//                                        .transform(new CropCircleTransformation())
//                                        .into(imageView);
//                            }
//                        });
//
//                        error.printStackTrace();
//                    }
//                });
//            }
//            else {
//                Log.d(F.TAG, "R.id.item_account_imageview_avatar is null =(");
//            }
//        }
//        {//pause
//            ImageView imageView = convertView.findViewById(R.id.item_account_imageview_pause);
//            if(imageView != null) {
//                if(tgAccount.isEnabled())
//                    imageView.setVisibility(View.GONE);
//                else
//                    imageView.setVisibility(View.VISIBLE);
//            }
//        }

        return convertView;
    }
}
