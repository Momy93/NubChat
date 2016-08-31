package com.mohamedfadiga.nubchat.pubnubcallbacks;

import com.fasterxml.jackson.databind.JsonNode;
import com.mohamedfadiga.nubchat.services.BackgroundService;
import com.mohamedfadiga.nubchat.utils.DatabaseHelper;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

public class MySubscribeCallback extends SubscribeCallback
{
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
        String sender = r.getMessage().get("sender").asText();
        if(sender.equals(username))return;
        JsonNode n = r.getMessage();
        String channelName = r.getActualChannel();
        if(channelName == null)channelName = n.get("sender").asText();
        service.newMessage(db.saveMessage(n, channelName, r.getTimetoken()));
    }

    @Override
    public void presence(PubNub pubnub, PNPresenceEventResult presence) {}
}