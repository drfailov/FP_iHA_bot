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

import com.fsoft.ihabot.R;
import com.fsoft.ihabot.databinding.FragmentGalleryBinding;
import com.fsoft.ihabot.ui.gallery.GalleryViewModel;

public class AccountsFragment extends Fragment {
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_accounts_layout, container, false);
        ListView listView = root.findViewById(R.id.listview_accounts_list);
        listView.setAdapter(new AccountsAdapter(getActivity()));
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
