package com.fsoft.ihabot;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.fsoft.ihabot.Utils.F;
import com.fsoft.ihabot.communucation.tg.TgAccount;
import com.fsoft.ihabot.communucation.tg.User;
import com.fsoft.ihabot.configuration.AdminList;
import com.fsoft.ihabot.ui.TgLoginWindow;
import com.fsoft.ihabot.ui.admins.AddAdminUsersAdapter;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.fsoft.ihabot.databinding.ActivityBotBinding;

public class BotActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityBotBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityBotBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setSupportActionBar(binding.appBarBot.toolbar);
        binding.appBarBot.fab.setOnClickListener(getAddAccountListener());
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_messages, R.id.nav_accounts, R.id.nav_admins)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_bot);
        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController navController, @NonNull NavDestination navDestination, @Nullable Bundle bundle) {
                if(navDestination.getId() == R.id.nav_accounts){
                    binding.appBarBot.fab.setVisibility(View.VISIBLE);
                    binding.appBarBot.fab.setImageResource(R.drawable.ic_add_tg_account);
                    binding.appBarBot.fab.setOnClickListener(getAddAccountListener());
                }
                else if(navDestination.getId() == R.id.nav_admins){
                    binding.appBarBot.fab.setVisibility(View.VISIBLE);
                    binding.appBarBot.fab.setImageResource(R.drawable.ic_add_user);
                    binding.appBarBot.fab.setOnClickListener(getAddAdminListener());
                }
                else {
                    binding.appBarBot.fab.setVisibility(View.INVISIBLE);
                }

                Log.d("NAV",navDestination.toString());
            }
        });
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);



        //проверить запущен ли сервис. если нет - запустить.
        Log.d(F.TAG, "Starting service...");
        Intent intent = new Intent(getApplicationContext(), BotService.class);
        startService(intent);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bot, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // process actionbar menu selected
        if(item.getItemId() == R.id.action_turnoff){
            getApplicationContext().stopService(new Intent(this, BotService.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_bot);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private View.OnClickListener getAddAccountListener(){
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                new TgLoginWindow(BotActivity.this, new TgLoginWindow.OnSuccessfulLoginListener() {
                    @Override
                    public void onSuccessfulLogin(TgAccount tgAccount) {
                        try {
                            ApplicationManager.getInstance().getCommunicator().addAccount(tgAccount);
                            tgAccount.startAccount();
                            Snackbar.make(view, "Добавлено: " + tgAccount.toString(), Snackbar.LENGTH_LONG).show();
                        }
                        catch (Exception e){
                            e.printStackTrace();
                            Snackbar.make(view, "Ошибка: " + e.getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
            }
        };
    }

    private View.OnClickListener getAddAdminListener(){
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Dialog dialog = new Dialog(BotActivity.this);
                dialog.setContentView(R.layout.dialog_add_admin);
                ListView listView = dialog.findViewById(R.id.lv_assignment_users);
                AddAdminUsersAdapter adapter = new AddAdminUsersAdapter(BotActivity.this);
                listView.setAdapter(adapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        if(adapter.getItem(i).getClass() == User.class) {
                            User user = (User) adapter.getItem(i);
                            ApplicationManager applicationManager = ApplicationManager.getInstance();
                            if(applicationManager != null) {
                                try {
                                    AdminList.AdminListItem adminListItem = applicationManager.getAdminList().add(user, user, "Добавлено из интерфейса программы.");
                                    for (AdminList.AdminListItem.Right right: AdminList.AdminListItem.getGenericRightsList()){
                                        if(right != null)
                                            adminListItem.setAllowed(right, true);
                                    }
                                    applicationManager.getAdminList().saveArrayToFile();
                                    Snackbar.make(binding.appBarBot.fab, "Добавлен администратор: " + user.toString(), Snackbar.LENGTH_LONG).show();
                                }
                                catch (Exception e){
                                    Snackbar.make(binding.appBarBot.fab, "Не удалось добавить администратора: " + e.getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
                                    e.printStackTrace();
                                }
                            }
                            dialog.dismiss();
                        }
                    }
                });
                dialog.show();


            }
        };
    }
}