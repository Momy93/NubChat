package com.mohamedfadiga.nubchat.channel;

import org.json.JSONException;
import org.json.JSONObject;


public class Channel
{
    public static final int PRIVATE = 0 , PUBLIC = 1;
    private String name;
    private int status;
    private int type;
    private long lastReceivedId, lastAddedId;

    public Channel(){
        name = "";
        status = 0;
        type = PRIVATE;
        lastReceivedId = -1;
        lastAddedId = -1;
    }
    public Channel(String name){
        status = 0;
        this.name = name;
        type = PRIVATE;
        lastReceivedId = -1;
        lastAddedId = -1;
    }
    public long getLastReceivedId(){return lastReceivedId;}
    public void setLastReceivedId(long lastReceivedId){this.lastReceivedId = lastReceivedId;}
    public long getLastAddedId(){return lastAddedId;}
    public void setLastAddedId(long lastAddedId){this.lastAddedId = lastAddedId;}
    public void setType(int type) {this.type = type;}
    public int getType() {return type;}
    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }
    public String getName()
    {
        return name;
    }
    public void setName(String name) {this.name = name;}
    public String toString()
    {
        JSONObject result = new JSONObject();
        try
        {
            result.put("name", name);
            result.put("status", status);
            result.put("type",""+type);
            result.put("lastReceivedId", lastReceivedId);
        }
        catch (JSONException e) {e.printStackTrace();}
        return result.toString();
    }
}
