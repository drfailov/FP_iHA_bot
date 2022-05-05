package com.fsoft.ihabot.ui.admins;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
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

import com.fsoft.ihabot.ApplicationManager;
import com.fsoft.ihabot.R;
import com.fsoft.ihabot.communucation.tg.TgAccount;
import com.fsoft.ihabot.ui.accounts.AccountsAdapter;
import com.google.android.material.snackbar.Snackbar;

public class AdminsFragment  extends Fragment {
    ListView listView;
    SwipeRefreshLayout swipeRefreshLayout;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_admins_layout, container, false);
        listView = root.findViewById(R.id.listview_admins_list);
        swipeRefreshLayout = root.findViewById(R.id.swiperefreshlayout_admins_list);
        listView.setAdapter(new AdminsAdapter(getActivity()));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            }
        });
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (listView != null)
                    listView.invalidateViews();
                if (swipeRefreshLayout != null)
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
