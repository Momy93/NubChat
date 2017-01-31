package com.doingstudio.nubchat.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;
import com.doingstudio.nubchat.utils.DatabaseHelper;
import com.doingstudio.nubchat.R;
import com.doingstudio.nubchat.activities.ChatActivity;
import com.doingstudio.nubchat.channel.Channel;
import com.doingstudio.nubchat.message.Message;
import com.doingstudio.nubchat.pubnubcallbacks.HistoryCallback;
import com.doingstudio.nubchat.pubnubcallbacks.MySubscribeCallback;
import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.PubNubException;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.endpoints.History;
import com.pubnub.api.endpoints.channel_groups.AddChannelChannelGroup;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.channel_group.PNChannelGroupsAllChannelsResult;
import java.util.ArrayList;
import java.util.Collections;
import  android.os.Handler;

public class BackgroundService extends Service
{
    private ServiceCallbacks serviceCallbacks;
    /*private static final String ON_NUBCHAT = "onNubChat";*/
    private String username ;
    private String pubKey;
    private String subKey;
    private PubNub pubNub;
    private DatabaseHelper db;
    private boolean online;
    private Handler handler;

    private BroadcastReceiver receiver =  new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (isNetworkAvailable()){
                if(!online){
                    online = true;
                    update();
                }
            }
            else online = false;
        }
    };

    public boolean deleteChannel(String channelName){
        try{
            pubNub.removeChannelsFromChannelGroup().channelGroup(username).channels(Collections.singletonList(channelName)).sync();
            return true;
        } catch(PubNubException e){
            makeToast(e.getErrormsg());
            return false;
        }
    }

    public boolean registerChannel(Channel channel, String name){
        if(channel.getType() == Channel.PRIVATE){
            db.addChannel(channel);
            pubNub.subscribe()
                .channelGroups(Collections.singletonList(username))
                .channels(Collections.singletonList(username))
                .execute();
            return true;
        }

        AddChannelChannelGroup g;
        g = pubNub.addChannelsToChannelGroup().channelGroup(name).channels(Collections.singletonList(channel.getName()));
        try
        {
            g.sync();
            pubNub.subscribe()
                .channelGroups(Collections.singletonList(username))
                .channels(Collections.singletonList(username))
                .execute();
            return true;
        }
        catch(PubNubException e){
            makeToast(e.getErrormsg());
            return false;
        }
    }

    public class MyBinder extends Binder {
        BackgroundService getService() {
            return BackgroundService.this;
        }
    }

    @Override
    public void onCreate(){
        handler =new Handler(Looper.getMainLooper());
        online = (isNetworkAvailable() && hasInternetConnection());
        db = DatabaseHelper.getInstance(this);
        SharedPreferences sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE);
        username = sharedPref.getString("username", null);
        subKey = sharedPref.getString("subKey", null);
        pubKey = sharedPref.getString("pubKey", null);
        if (!(username == null || subKey == null || pubKey == null)){
            PNConfiguration pnc = new PNConfiguration();
            pnc.setSubscribeKey(subKey);
            pnc.setPublishKey(pubKey);
            pnc.setUuid(username);
            pubNub = new PubNub(pnc);
            pubNub.addListener(new MySubscribeCallback(this, username));
            this.registerReceiver(receiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
            update();
        }
    }

    public void  sendMessage(final Message message){
        pubNub.publish()
            .message(message.toJson())
            .channel(message.getChannelName()).shouldStore(true).async(new PNCallback<PNPublishResult>(){
                @Override
                public void onResponse(PNPublishResult result, PNStatus status){
                    if(!status.isError()){
                        message.setStatus(Message.SENT);
                        db.saveMessage(message);
                        if(serviceCallbacks != null) serviceCallbacks.update(message);
                    }
                }
            });
    }

    /*public class SerializableMessage{
        private int type;
        private String text, sender, url, latLon;

        SerializableMessage(Message message){
            this.sender = message.getSender();
            this.text = message.getText();
            this.url = message.getUrl();
            this.type = message.getType();
            this.latLon = message.getLatLon();
        }

        public int getType() {return type;}
        public String getLatLon() {return latLon;}
        public String getSender() {return sender;}
        public String getText() {return text;}
        public String getUrl() {return url;}
    }*/

    private void update(){
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                ArrayList<Channel> channels = db.getChannels();
                ArrayList<Message> messages = db.getUnsentMessages();
                for(Message message: messages)sendMessage(message);
                try{
                    for(Channel channel : channels){
                        if(channel.getType() == 1){

                            Message message = db.getMessage(channel.getLastReceivedId());
                            History history = pubNub.history();
                            history.channel(channel.getName()).start(message == null ? 0 : message.getTimetoken() + 1).reverse(true).includeTimetoken(true).async(new HistoryCallback(history, BackgroundService.this, username, channel.getName()));
                        }
                    }

                    History history = pubNub.history().channel(username).start(db.getLastTimetoken() + 1).reverse(true).includeTimetoken(true);
                    history.async(new HistoryCallback(history, BackgroundService.this, username, username));
                }
                catch(Exception e){
                    makeToast("Please enable history in your PubNub account");
                }
                pubNub.subscribe()
                    .channelGroups(Collections.singletonList(username))
                    .channels(Collections.singletonList(username))
                    .execute();


            }
        }).start();
    }

  /*  public boolean usernameAvailable(String name)
    {
        try{
            AllChannelsChannelGroup g = pubNub.listChannelsForChannelGroup().channelGroup(name);
            PNChannelGroupsAllChannelsResult r = g.sync();
            return r.getChannels().indexOf(ON_NUBCHAT) == -1;
        }
        catch(PubNubException e){
            makeToast(e.getErrormsg());
            return false;
        }
    }*/

    public void makeToast(final  String msg){
        handler.post(new Runnable(){
            @Override
            public void run(){
                Toast.makeText(BackgroundService.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

   /* public boolean registerUsername(String name){return registerChannel(new Channel(ON_NUBCHAT), name);}*/

    public boolean setParams(boolean cleanGroup, String...params){
        SharedPreferences sharedPref = getSharedPreferences("settings",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        boolean result = true;

        if(username != null){
            if((cleanGroup)&&(!cleanGroup())){
                    result = false;
                    makeToast("Cannot clean group");
                }
            else pubNub.disconnect();
        }

        if(result){
            PNConfiguration pnc = new PNConfiguration();
            pnc.setSubscribeKey(params[1]);
            pnc.setPublishKey(params[2]);
            pnc.setUuid(params[0]);
            pubNub = new PubNub(pnc);
            subKey = params[1];
            pubKey = params[2];
            username = params[0];
            editor.putString("username", username);
            editor.putString("subKey", subKey);
            editor.putString("pubKey", pubKey);
            editor.apply();
            update();
        }

        return result;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return START_STICKY;
    }

    private boolean cleanGroup(){
        try{
            PNChannelGroupsAllChannelsResult r = pubNub.listChannelsForChannelGroup().channelGroup(username).sync();
            if(r.getChannels().size() == 0)return true;
            pubNub.removeChannelsFromChannelGroup().channelGroup(username).channels(r.getChannels()).sync();
            return true;
        }catch(PubNubException e){e.printStackTrace();return false;}
    }

    public boolean isNetworkAvailable(){
        ConnectivityManager cm =  (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    @Override
    public IBinder onBind(Intent intent){
        return new MyBinder();
    }

    @Override
    public void onDestroy(){
        pubNub.disconnect();
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    public static boolean hasInternetConnection(){
        Runtime runtime = Runtime.getRuntime();
        try{
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int     exitValue = ipProcess.waitFor();
            return (exitValue == 0);

        } catch (Exception e){e.printStackTrace();}
        return false;
    }

    public void clearCallbacks(){
        serviceCallbacks = null;
    }

    public void newMessage(Message message){
        if (serviceCallbacks != null)serviceCallbacks.update(message);
        else showNotification(message);
    }

    public interface ServiceCallbacks{
        void update(Message message);
    }

    public void showNotification(Message message){
        String channel = message.getChannelName();
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.icon)
                        .setContentTitle(channel)
                        .setContentText(message.getSender() + ": " + message.getText())
                        .setAutoCancel(true).setDefaults(Notification.DEFAULT_SOUND)
                        .setLights(ContextCompat.getColor(this, R.color.magicBlue), 1000, 5000)
                        .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.icon));
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("channel", channel);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(ChatActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent =  stackBuilder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, mBuilder.build());
    }

    public void setCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }
}