package com.doingstudio.nubchat.message;

import  com.google.gson.*;
import java.util.Calendar;
import java.util.TimeZone;

public class Message
{
    public static final int TEXT = 0, IMAGE = 1, FILE =2 , GMAPS = 3;
    public static final int SENDING = 0, SENT = 1, RECEIVED_RECIPIENT = 2, READ_RECIPIENT = 3, RECEIVED_ME = 4, READ_ME = 5;
    private int type = 0;
    private String text="", sender="Unknown", url, latLon="0,0";
    private long timetoken;
    private long id;
    private int status;
    private String channelName;

    public Message(){
        Calendar calendar =Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        timetoken = calendar.getTimeInMillis()*10000;
        text = "";
        sender = "";
        type = TEXT;
        status = SENDING;
        id = -1;
    }

    @Override
    public boolean equals(Object message){
        return message != null && message instanceof Message && id == ((Message)message).getId();
    }

    private boolean sameDate(Calendar c1, Calendar c2) {
        return (c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH) &&
                c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH));
    }

    public String getTimeLabel(){
        String time = "";
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.setTimeInMillis(timetoken / 10000);
        if(sameDate(calendar, Calendar.getInstance())){
            time += calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            time += ":"+(minute < 10?"0":"")+minute;
        }
        else{
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1;
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            time = "" + day + "/" + month + "/" + year + " " + hour + ":" + (minute < 10?"0":"")+minute;
        }
        return time;
    }

    public JsonObject toJson() {
        JsonObject j = new JsonObject();
        j.addProperty("channelName",channelName);
        j.addProperty("sender",sender);
        j.addProperty("type", type);
        j.addProperty("text",text);
        j.addProperty("timetoken",timetoken);
        if(type == 1 || type == 2)j.addProperty("url",url);
        else if(type == 3)j.addProperty("latlon",latLon);
        return j;
    }

    public String getChannelName() {
        return channelName;
    }
    public void setChannelName(String name) {this.channelName = name;}
    public long getId() {return id;}
    public void setId(long id) {this.id = id;}
    public void setStatus(int status) {this.status = status;}
    public int  getStatus() {return status;}
    public void setTimetoken(long timetoken){this.timetoken = timetoken;}
    public void setSender(String sender) {this.sender = sender;}
    public void setText(String text) {this.text = text;}
    public void setType(int type){this.type = type;}
    public long getTimetoken(){return timetoken;}
    public String getLatLon(){return  latLon;}
    public void  setLatLon(String latLon){this.latLon = latLon;}
    public void setUrl(String url) {this.url = url;}
    public int getType() {return type;}
    public String getUrl() {return url;}
    public String getText()
    {
        return text;
    }
    public String getSender()
    {
        return sender;
    }
}

