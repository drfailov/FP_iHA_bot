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
import com.fsoft.ihabot.communucation.tg.TgAccount;
import com.fsoft.ihabot.communucation.tg.TgAccountCore;
import com.fsoft.ihabot.communucation.tg.User;
import com.fsoft.ihabot.configuration.AdminList;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import jp.wasabeef.picasso.transformations.CropCircleTransformation;

public class AddAdminUsersAdapter extends BaseAdapter {
    Activity activity = null;
    ApplicationManager applicationManager = null;
    ArrayList<User> users = null;


    public AddAdminUsersAdapter(Activity activity) {
        this.activity = activity;
        applicationManager = ApplicationManager.getInstance();
        users = applicationManager.getMessageHistory().getLastUsersList();
        //Удалить из списка тех кто уже админ
        ArrayList<AdminList.AdminListItem> admins = applicationManager.getAdminList().getUserList();
        for (AdminList.AdminListItem admin:admins){
            users.remove(admin.getUser());
        }
    }

    @Override
    public void notifyDataSetChanged() {
        if(applicationManager == null)
            applicationManager = ApplicationManager.getInstance();
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        if(applicationManager == null)
            applicationManager = ApplicationManager.getInstance();
        super.notifyDataSetInvalidated();
    }

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public Object getItem(int i) {
        return users.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        if (convertView == null) {
            convertView = activity.getLayoutInflater().inflate(R.layout.dialog_add_admin_list_item, container, false);
        }

        User user = users.get(position);
        if(user == null) { //если юзер пустой, надо загрузить по новой, чтобы там не висели подвисшие данные с проглого элемента
            return activity.getLayoutInflater().inflate(R.layout.dialog_add_admin_list_item, container, false);
        }

        { //NAME
            TextView textView = convertView.findViewById(R.id.item_user_textView_name);
            if(textView != null) {
                textView.setText(user.getName());
            }
        }
        { //USERNAME
            TextView textView = convertView.findViewById(R.id.item_user_textView_username);
            if(textView != null) {
                if(user.getUsername().isEmpty())
                    textView.setText(String.format(Locale.US, "%d", user.getId()));
                else
                    textView.setText(String.format("@%s", user.getUsername()));
            }
        }

        TgAccount tgAccount = applicationManager.getCommunicator().getWorkingTgAccount();
        if(tgAccount != null){//photo
            ImageView imageView = convertView.findViewById(R.id.item_user_imageview_avatar);
            if(imageView != null) {
                Picasso.get()
                        .load(R.drawable.tg_account_placeholder)
                        .transform(new CropCircleTransformation())
                        .into(imageView);
//                {
//                    tgAccount.getUserPhotoUrl(new TgAccountCore.GetUserPhotoListener() {
//                        @Override
//                        public void gotPhoto(String url) {
//                            imageView.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    Picasso.get()
//                                            .load(url)
//                                            .placeholder(R.drawable.tg_account_placeholder)
//                                            .transform(new CropCircleTransformation())
//                                            .into(imageView);
//                                }
//                            });
//                        }
//
//                        @Override
//                        public void noPhoto() {
//                            imageView.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    Picasso.get()
//                                            .load(R.drawable.tg_account_placeholder)
//                                            .transform(new CropCircleTransformation())
//                                            .into(imageView);
//                                }
//                            });
//
//                        }
//
//                        @Override
//                        public void error(Throwable error) {
//                            imageView.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    Picasso.get()
//                                            .load(R.drawable.tg_account_placeholder)
//                                            .transform(new CropCircleTransformation())
//                                            .into(imageView);
//                                }
//                            });
//
//                            error.printStackTrace();
//                        }
//                    }, user.getId());
//                }
            }
            else {
                Log.d(F.TAG, "R.id.item_account_imageview_avatar is null =(");
            }
        }


        return convertView;
    }
}
