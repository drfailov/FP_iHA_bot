package com.fsoft.ihabot.ui.accounts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fsoft.ihabot.R;
import com.fsoft.ihabot.databinding.FragmentGalleryBinding;
import com.fsoft.ihabot.ui.gallery.GalleryViewModel;

public class AccountsFragment extends Fragment {
    ListView listView;
    SwipeRefreshLayout swipeRefreshLayout;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_accounts_layout, container, false);
        listView = root.findViewById(R.id.listview_accounts_list);
        swipeRefreshLayout = root.findViewById(R.id.swiperefreshlayout_accounts_list);
        listView.setAdapter(new AccountsAdapter(getActivity()));
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
