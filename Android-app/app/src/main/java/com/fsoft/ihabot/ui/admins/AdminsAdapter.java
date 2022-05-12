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
        if(adminListItem == null) { //если юзер пустой, надо загрузить по новой, чтобы там не висели подвисшие данные с проглого элемента
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
        { //USERNAME
            TextView textView = convertView.findViewById(R.id.item_admin_textView_userhame);
            if(textView != null) {
                if(user.getUsername().isEmpty())
                    textView.setText(String.format(Locale.US, "%d", user.getId()));
                else
                    textView.setText(String.format("@%s", user.getUsername()));
            }
        }

        { //DESCRIPTION
            TextView textView = convertView.findViewById(R.id.item_admins_textView_description);
            if(textView != null) {
                textView.setText(adminListItem.getComment());
            }
        }

        { //RIGHTS
            TextView textView = convertView.findViewById(R.id.item_admins_textView_rights);
            if(textView != null) {
                textView.setText(adminListItem.getRightsAsString());
            }
        }

        TgAccount tgAccount = applicationManager.getCommunicator().getWorkingTgAccount();
        if(tgAccount != null){//photo
            ImageView imageView = convertView.findViewById(R.id.item_admin_imageview_avatar);

            if(imageView != null) {
                imageView.setImageResource(R.drawable.ic_tg_logo);
                tgAccount.getUserPhotoUrl(new TgAccountCore.GetUserPhotoListener() {
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
                }, user.getId());
            }
            else {
                Log.d(F.TAG, "R.id.item_account_imageview_avatar is null =(");
            }
        }


        return convertView;
    }
}
