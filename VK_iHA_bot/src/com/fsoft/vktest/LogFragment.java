package com.fsoft.vktest;

/**
 * Dr. Failov on 11.11.2014.
 */
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

public class LogFragment extends Fragment {
    ConsoleView consoleView = null;
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return TabsActivity.consoleView = consoleView = new ConsoleView(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        consoleView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPause() {
        consoleView.setVisibility(View.INVISIBLE);
        super.onPause();
    }
}