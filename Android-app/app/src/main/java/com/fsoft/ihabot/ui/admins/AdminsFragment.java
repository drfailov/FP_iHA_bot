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
import com.fsoft.ihabot.configuration.AdminList;
import com.fsoft.ihabot.ui.accounts.AccountsAdapter;
import com.google.android.material.snackbar.Snackbar;

public class AdminsFragment  extends Fragment {
    ListView listView;
    SwipeRefreshLayout swipeRefreshLayout;
    AdminsAdapter adminsAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_admins_layout, container, false);
        listView = root.findViewById(R.id.listview_admins_list);
        swipeRefreshLayout = root.findViewById(R.id.swiperefreshlayout_admins_list);
        adminsAdapter = new AdminsAdapter(getActivity());
        listView.setAdapter(adminsAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                try {
                    final AdminList.AdminListItem adminListItem = ApplicationManager.getInstance().getAdminList().getUserList().get(i);
                    if(adminListItem == null)
                        return;
                    PopupMenu popupMenu = new PopupMenu(getActivity(), view);
                    popupMenu.inflate(R.menu.admin_popup_menu);
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (item.getItemId() == R.id.action_copy_account_username) {
                                if(getActivity() != null) {
                                    ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                                    String text = "@" + adminListItem.getUser().getUsername();
                                    clipboardManager.setPrimaryClip(ClipData.newPlainText("username", text));
                                }
                            }
                            if (item.getItemId() == R.id.action_remove_rights) {
                                try {
                                    for (AdminList.AdminListItem.Right right : AdminList.AdminListItem.getGenericRightsList()) {
                                        if (right != null)
                                            adminListItem.setAllowed(right, false);
                                    }
                                    ApplicationManager.getInstance().getAdminList().saveArrayToFile();
                                    adminsAdapter.notifyDataSetInvalidated();
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                    Snackbar.make(view, "Ошибка: "+e.getLocalizedMessage(), Snackbar.LENGTH_SHORT).show();
                                }
                            }
                            if (item.getItemId() == R.id.action_delete_admin) {
                                new AlertDialog.Builder(getActivity())
                                        .setTitle("Удаление аккаунта")
                                        .setMessage("Вы действительно хотите удалить администратора " + adminListItem.getUser() + "?")
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                try{
                                                    ApplicationManager.getInstance().getAdminList().rem(adminListItem);
                                                    Snackbar.make(view, "Аккаунт "+adminListItem.getUser()+" успешно удалён", Snackbar.LENGTH_SHORT).show();
                                                    adminsAdapter.notifyDataSetInvalidated();
                                                }
                                                catch (Exception e){
                                                    e.printStackTrace();
                                                    Snackbar.make(view, "Ошибка: "+e.getLocalizedMessage(), Snackbar.LENGTH_SHORT).show();
                                                }
                                            }})
                                        .setNegativeButton(android.R.string.no, null).show();
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
