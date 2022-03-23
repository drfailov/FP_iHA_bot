package com.fsoft.ihabot.ui.accounts;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.fsoft.ihabot.R;
import com.fsoft.ihabot.Utils.ApplicationManager;
import com.fsoft.ihabot.communucation.Communicator;

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
        if(communicator == null) return null;
        return communicator.getTgAccounts().get(i);
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


        //((TextView) convertView.findViewById(R.id.item_account_textView_name)).setText();
        return convertView;
    }
}
