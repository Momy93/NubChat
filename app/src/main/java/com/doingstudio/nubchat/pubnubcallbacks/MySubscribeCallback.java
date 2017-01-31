package com.doingstudio.nubchat.pubnubcallbacks;

import com.google.gson.JsonObject;
import com.doingstudio.nubchat.services.BackgroundService;
import com.doingstudio.nubchat.utils.DatabaseHelper;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

public class MySubscribeCallback extends SubscribeCallback{
    private BackgroundService service;
    private  String username;

    public MySubscribeCallback(BackgroundService service,  String username){
        this.service = service;
        this.username = username;
    }

    @Override
    public void status(PubNub pubnub, PNStatus status){}

    @Override
    public void message(PubNub pubnub, PNMessageResult r){
        DatabaseHelper db = DatabaseHelper.getInstance(service);
        JsonObject o = r.getMessage().getAsJsonObject();
        String sender = o.get("sender").getAsString();
        if(sender.equals(username))return;
        String channelName = r.getSubscription();
        if(channelName == null)channelName = sender;
        else channelName = r.getChannel();
        service.newMessage(db.saveMessage(o, channelName, r.getTimetoken()));
    }

    @Override
    public void presence(PubNub pubnub, PNPresenceEventResult presence) {}
}