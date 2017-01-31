package com.doingstudio.nubchat.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import com.doingstudio.nubchat.services.BackgroundService;
import com.doingstudio.nubchat.message.Message;
import com.doingstudio.nubchat.R;
import com.doingstudio.nubchat.services.ServiceBinder;
import com.doingstudio.nubchat.utils.DatabaseHelper;

public class SettingsActivity extends AppCompatActivity implements BackgroundService.ServiceCallbacks
{
    private ServiceBinder serviceBinder = new ServiceBinder(this);

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
    public void update(Message message) {serviceBinder.getService().showNotification(message);}

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        TextView getKeys = (TextView)findViewById(R.id.getKeysTextView);
        getKeys.setMovementMethod(LinkMovementMethod.getInstance());

        final EditText usernameEdit = (EditText)findViewById(R.id.usernameEditText);
        final EditText subKeyEdit = (EditText)findViewById(R.id.subKeyEditText);
        final EditText pubKeyEdit = (EditText)findViewById(R.id.pubKeyEditText);

        SharedPreferences sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE);
        String username = sharedPref.getString("username", null);
        String subKey = sharedPref.getString("subKey", null);
        String pubKey = sharedPref.getString("pubKey", null);
        if(username != null)usernameEdit.setText(username);
        if(subKey != null) subKeyEdit.setText(subKey);
        if(pubKey != null)pubKeyEdit.setText(pubKey);
        Button saveButton = (Button)findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(usernameEdit.getText().toString().length() == 0){
                    Snackbar.make(v, getString(R.string.username_error), Snackbar.LENGTH_LONG).show();
                }
                else if(subKeyEdit.getText().toString().length() != 42){
                    Snackbar.make(v, "Subscribe key " + getString(R.string.credentials_error), Snackbar.LENGTH_LONG).show();
                }
                else if(pubKeyEdit.getText().toString().length() != 42){
                    Snackbar.make(v, "Publish key " + getString(R.string.credentials_error), Snackbar.LENGTH_LONG).show();
                }
                else
                {
                    final Switch s = (Switch)findViewById(R.id.cleanGroupSwitch);
                    final String username = usernameEdit.getText().toString().replaceAll(" ", "");
                    final String subKey = subKeyEdit.getText().toString();
                    final String pubKey = pubKeyEdit.getText().toString();
                    final ProgressDialog loadingDialog;
                    loadingDialog = new ProgressDialog(SettingsActivity.this);
                    loadingDialog.setTitle(getString(R.string.loading));
                    loadingDialog.setCancelable(false);
                    loadingDialog.show();

                    new Thread(new Runnable(){
                        @Override
                        public void run(){
                            final boolean result = serviceBinder.getService().setParams(s.isChecked(), username, subKey, pubKey);
                           runOnUiThread(new Runnable(){
                               @Override
                               public void run(){
                                   if(result){

                                       DatabaseHelper.getInstance(SettingsActivity.this).clear();
                                       finish();
                                       startActivity(new Intent(SettingsActivity.this,MainActivity.class));
                                   }
                                   loadingDialog.dismiss();
                               }
                           });
                        }
                    }).start();
                }
            }
        });
    }
}