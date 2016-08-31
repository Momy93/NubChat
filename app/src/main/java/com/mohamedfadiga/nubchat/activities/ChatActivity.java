package com.mohamedfadiga.nubchat.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import com.mohamedfadiga.nubchat.channel.Channel;
import com.mohamedfadiga.nubchat.services.BackgroundService;
import com.mohamedfadiga.nubchat.utils.DatabaseHelper;
import com.mohamedfadiga.nubchat.message.Message;
import com.mohamedfadiga.nubchat.message.MessageListAdapter;
import com.mohamedfadiga.nubchat.R;
import com.mohamedfadiga.nubchat.services.ServiceBinder;
import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity implements BackgroundService.ServiceCallbacks
{
    private MessageListAdapter messageListAdapter;
    private ListView listView;
    private String channel;
    private String username;
    private ServiceBinder serviceBinder = new ServiceBinder(this);
    private ArrayList<Message> messages;
    private DatabaseHelper db;

    @Override
    public void onResume()
    {
        super.onResume();
        db = DatabaseHelper.getInstance(ChatActivity.this);
        messages = db.getChannelMessages(channel);
        messageListAdapter =  new MessageListAdapter(this, R.layout.row_message, messages, username);
        listView = (ListView)findViewById(R.id.messagesListView);
        listView.setAdapter(messageListAdapter);
        listView.setSelection(messageListAdapter.getCount() - 1);
        for(Message message: messages)
        {
            if(!message.getSender().equals(username) && message.getStatus()==Message.RECEIVED_ME) {
                message.setStatus(Message.READ_ME);
                db.saveMessage(message);
            }
            messageListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle b)
    {
        super.onCreate(b);
        setContentView(R.layout.activity_chat);

        final EditText eT = (EditText) findViewById(R.id.messageEditText);
        Bundle extras = getIntent().getExtras();
        channel = extras.getString("channel");
        SharedPreferences sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE);
        username = sharedPref.getString("username", null);
        setTitle(channel);
        Button sendButton = (Button)findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (eT.getText().toString().length() > 0) {
                    Message message = new Message();
                    message.setText(eT.getText().toString());
                    eT.setText("");
                    message.setChannelName(channel);
                    message.setSender(username);
                    db.saveMessage(message);
                    update(message);
                    serviceBinder.getService().sendMessage(message);
                }
            }
        });
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
    public void update(final Message message)
    {
        if(!message.getChannelName().equals(channel))
            serviceBinder.getService().showNotification(message);
        else runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final int index = messages.indexOf(message);
                if (index != -1) messages.set(index, message);
                else{
                    messages.add(message);
                    if(message.getStatus() == Message.RECEIVED_ME)message.setStatus(Message.READ_ME);
                    db.saveMessage(message);
                }
                messageListAdapter.notifyDataSetChanged();
                listView.setSelection(messageListAdapter.getCount() - 1);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_channel, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        switch (id)
        {
            case R.id.delete:
                final ProgressDialog loadingDialog;
                loadingDialog = new ProgressDialog(ChatActivity.this);
                loadingDialog.setTitle(getString(R.string.loading));
                loadingDialog.setCancelable(true);
                loadingDialog.show();
                new Thread(new Runnable(){
                    @Override
                    public void run(){
                        String query = "SELECT * FROM channels WHERE name = '" +channel+"'";
                        if(db.getChannel(query).getType() == Channel.PRIVATE || serviceBinder.getService().deleteChannel(channel)){
                            db.deleteChannel(channel);
                            finish();
                        }
                        runOnUiThread(new Runnable(){
                            @Override
                            public void run(){loadingDialog.dismiss();}
                        });
                    }
                }).start();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}