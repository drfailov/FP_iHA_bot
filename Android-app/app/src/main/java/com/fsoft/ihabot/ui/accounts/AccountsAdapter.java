package com.fsoft.ihabot.ui.accounts;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.fsoft.ihabot.R;
import com.fsoft.ihabot.Utils.ApplicationManager;
import com.fsoft.ihabot.communucation.Communicator;
import com.fsoft.ihabot.communucation.tg.TgAccount;
import com.fsoft.ihabot.communucation.tg.TgAccountCore;
import com.squareup.picasso.Picasso;

import java.util.Locale;

import jp.wasabeef.picasso.transformations.CropCircleTransformation;

public class AccountsAdapter extends BaseAdapter {
    Activity activity = null;
    ApplicationManager applicationManager = null;
    Communicator communicator = null;

    public AccountsAdapter(Activity activity) {
        this.activity = activity;
        applicationManager = ApplicationManager.getInstance();
        if(applicationManager != null)
            communicator = applicationManager.getCommunicator();
    }

    @Override
    public int getCount() {
        if(communicator == null) return 0;
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
        if(tgAccount == null) {
            return convertView;
        }

        tgAccount.setOnStateChangedListener(new Runnable() { //auto-updating
            @Override
            public void run() {
                notifyDataSetInvalidated();
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
        { //STATE
            TextView textView = convertView.findViewById(R.id.item_account_textView_status);
            if(textView != null) {
                if (tgAccount.getState() != null)
                    textView.setText(tgAccount.getState());
                else
                    textView.setText("Непонятный статус");
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
                        Picasso.get()
                                .load(url)
                                .placeholder(R.mipmap.ic_launcher)
                                .transform(new CropCircleTransformation())
                                .into(imageView);
                    }

                    @Override
                    public void error(Throwable error) {
                        Picasso.get()
                                .load(R.mipmap.ic_launcher)
                                .transform(new CropCircleTransformation())
                                .into(imageView);
                        error.printStackTrace();
                    }
                });
            }
        }
        return convertView;
    }
}
