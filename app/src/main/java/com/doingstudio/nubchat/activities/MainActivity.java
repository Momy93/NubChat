package com.doingstudio.nubchat.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import com.doingstudio.nubchat.services.BackgroundService;
import com.doingstudio.nubchat.channel.Channel;
import com.doingstudio.nubchat.channel.ChannelListAdapter;
import com.doingstudio.nubchat.utils.DatabaseHelper;
import com.doingstudio.nubchat.message.Message;
import com.doingstudio.nubchat.R;
import com.doingstudio.nubchat.services.ServiceBinder;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements BackgroundService.ServiceCallbacks{

    private ListView listView;
    private DatabaseHelper db;
    private ArrayList<Pair<Channel, Message>> channels;
    private ChannelListAdapter channelListAdapter;
    private ServiceBinder serviceBinder = new ServiceBinder(this);
    private String username;

    @Override
    protected void onResume(){
        super.onResume();
        db = DatabaseHelper.getInstance(MainActivity.this);
        new Thread(new Runnable(){
            @Override
            public void run(){
                update(null);
            }
        }).start();
    }

    @Override
    protected void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        Intent i= new Intent(this, BackgroundService.class);
        startService(i);
        SharedPreferences sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE);
        username = sharedPref.getString("username", null);
        String subKey = sharedPref.getString("subKey", null);
        String pubKey = sharedPref.getString("pubKey", null);
        if(username == null || subKey == null || pubKey == null)
        {
            Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        listView = (ListView)findViewById(R.id.channelsListView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Pair<Channel,Message> p = ((ChannelListAdapter) listView.getAdapter()).getItem(position);
                if(p != null){
                    Channel channel = p.first;
                    Intent intent  = new Intent(getApplicationContext(), ChatActivity.class);
                    intent.putExtra("channel", channel.getName());
                    startActivity(intent);
                }

            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                final View dialogView = LayoutInflater.from(builder.getContext()).inflate(R.layout.fragment_new_channel, null);
                builder.setView(dialogView)
                        .setTitle(getString(R.string.new_channel))
                        .setCancelable(false).setPositiveButton(getText(R.string.add), new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                EditText name = (EditText) dialogView.findViewById(R.id.nameEditText);
                                RadioButton publicRadio = (RadioButton) dialogView.findViewById(R.id.radioButton);
                                final Channel channel = new Channel(name.getText().toString());
                                channel.setType(publicRadio.isChecked() ? Channel.PUBLIC : Channel.PRIVATE);
                                String query = "SELECT * FROM channels WHERE name = '" +channel.getName() +"'";
                                if (db.getChannel(query) == null){
                                    dialog.dismiss();
                                    final ProgressDialog loadingDialog;
                                    loadingDialog = new ProgressDialog(MainActivity.this);
                                    loadingDialog.setTitle(getString(R.string.loading));
                                    loadingDialog.setCancelable(false);
                                    loadingDialog.show();
                                    new Thread(new Runnable()
                                        {
                                            @Override
                                            public void run()
                                            {
                                                if(serviceBinder.getService().registerChannel(channel, username)){
                                                    db.addChannel(channel);
                                                    update(null);
                                                }
                                                loadingDialog.dismiss();
                                            }
                                        }).start();
                                }
                                else{
                                    Intent intent  = new Intent(getApplicationContext(), ChatActivity.class);
                                    intent.putExtra("channel", channel.getName());
                                    startActivity(intent);
                                }
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                builder.create().show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        Intent intent;
        switch (id){
            case R.id.settings:
                intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        serviceBinder.bind();
    }

    @Override
    protected void onStop() {
        super.onStop();
        serviceBinder.unbind();
    }

    @Override
    public void update(Message message) {
        channels = db.getLastMessages();
        channelListAdapter =  new ChannelListAdapter(this, R.layout.row_channel, channels);
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                listView.setAdapter(channelListAdapter);
            }
        });
       if(message != null && message.getStatus() == Message.RECEIVED_ME){
           Vibrator v = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
           v.vibrate(1000);
       }
    }
}