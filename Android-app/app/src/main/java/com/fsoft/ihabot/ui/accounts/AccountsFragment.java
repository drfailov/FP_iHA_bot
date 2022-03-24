package com.fsoft.ihabot.ui.accounts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fsoft.ihabot.R;
import com.fsoft.ihabot.Utils.ApplicationManager;
import com.fsoft.ihabot.communucation.tg.TgAccount;

public class AccountsFragment extends Fragment {
    ListView listView;
    SwipeRefreshLayout swipeRefreshLayout;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_accounts_layout, container, false);
        listView = root.findViewById(R.id.listview_accounts_list);
        swipeRefreshLayout = root.findViewById(R.id.swiperefreshlayout_accounts_list);
        listView.setAdapter(new AccountsAdapter(getActivity()));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                try {
                    final TgAccount tgAccount = ApplicationManager.getInstance().getCommunicator().getTgAccounts().get(i);
                    PopupMenu popupMenu = new PopupMenu(getActivity(), view);
                    popupMenu.inflate(R.menu.account_popup_menu);
                    if(tgAccount.isEnabled())
                        popupMenu.getMenu().findItem(R.id.action_enable_account).setVisible(false);
                    else
                        popupMenu.getMenu().findItem(R.id.action_disable_account).setVisible(false);
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (item.getItemId() == R.id.action_disable_account) {
                                tgAccount.setEnabled(false);
                            }
                            if (item.getItemId() == R.id.action_enable_account) {
                                tgAccount.setEnabled(true);
                            }
                            return false;
                        }
                    });
                    popupMenu.show();
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(listView != null)
                    listView.invalidateViews();
                if(swipeRefreshLayout != null)
                    swipeRefreshLayout.setRefreshing(false);
            }
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
