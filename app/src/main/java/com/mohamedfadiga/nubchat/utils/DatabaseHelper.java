package com.mohamedfadiga.nubchat.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;
import com.google.gson.JsonObject;
import com.mohamedfadiga.nubchat.channel.Channel;
import com.mohamedfadiga.nubchat.message.Message;
import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper
{
    // Database Info
    private static final String DATABASE_NAME = "nubChatDatabase";
    private static final int DATABASE_VERSION = 1;

    // Table Names
    private static final String TABLE_CHANNELS = "channels";
    private static final String TABLE_MESSAGES = "messages";

    // Channels table columns
    private static final String KEY_CHANNEL_NAME = "name";
    private static final String KEY_CHANNEL_TYPE  = "channel_type";
    private static final String KEY_CHANNEL_STATUS  = "channel_status";
    private static final String KEY_CHANNEL_LAST_REC_ID  = "last_rec_id";
    private static final String KEY_CHANNEL_LAST_ADD_ID  = "last_add_id";

    // Messages table columns
    private final static String KEY_MESSAGE_ID = "id";
    private final static String KEY_MESSAGE_CHANNEL = "channel";
    private final static String KEY_MESSAGE_SENDER = "sender";
    private final static String KEY_MESSAGE_TIMETOKEN = "timetoken";
    private final static String KEY_MESSAGE_TEXT = "text";
    private final static String KEY_MESSAGE_TYPE = "message_type";
    private final static String KEY_MESSAGE_URL = "url";
    private final static String KEY_MESSAGE_LAT_LON = "latlon";
    private final static String KEY_MESSAGE_STATUS = "message_status";

    private static DatabaseHelper sInstance;
    private static String username;

    public static synchronized DatabaseHelper getInstance(Context context)
    {
        if (sInstance == null){
            sInstance = new DatabaseHelper(context.getApplicationContext());
            SharedPreferences sharedPref = context.getSharedPreferences("settings", Context
                .MODE_PRIVATE);
            username = sharedPref.getString("username", null);
        }
        return sInstance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db)
    {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        String CREATE_CHANNELS_TABLE = "CREATE TABLE " + TABLE_CHANNELS +
            "(" +
            KEY_CHANNEL_NAME + " TEXT," +
            KEY_CHANNEL_TYPE + " INTEGER," +
            KEY_CHANNEL_STATUS + " INTEGER," +
            KEY_CHANNEL_LAST_REC_ID + " INTEGER," +
            KEY_CHANNEL_LAST_ADD_ID + " INTEGER" +
            ")";

        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES +
            "(" +
            KEY_MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            KEY_MESSAGE_CHANNEL + " TEXT," +
            KEY_MESSAGE_SENDER + " TEXT," +
            KEY_MESSAGE_TIMETOKEN + " INTEGER," +
            KEY_MESSAGE_TEXT + " TEXT," +
            KEY_MESSAGE_TYPE + " INTEGER," +
            KEY_MESSAGE_URL + " TEXT," +
            KEY_MESSAGE_LAT_LON + " TEXT," +
            KEY_MESSAGE_STATUS + " INTEGER" +
            ")";
        db.execSQL(CREATE_CHANNELS_TABLE);
        db.execSQL(CREATE_MESSAGES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {

        if (oldVersion != newVersion)
        {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHANNELS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
            onCreate(db);
        }
    }


    public void clear(){
        SQLiteDatabase db = getReadableDatabase();
        db.execSQL("DELETE FROM " + TABLE_CHANNELS);
        db.execSQL("DELETE FROM " + TABLE_MESSAGES);
    }


    public ArrayList<Pair<Channel, Message>> getLastMessages()
    {

        ArrayList<Pair<Channel, Message>> pairs = new ArrayList<>();
        String query =  String.format("SELECT * FROM %s LEFT JOIN %s ON %s = %s ORDER BY %s DESC",
            TABLE_CHANNELS,
            TABLE_MESSAGES,
            KEY_CHANNEL_LAST_ADD_ID,
            KEY_MESSAGE_ID,
            KEY_MESSAGE_TIMETOKEN);

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        try
        {
            if(cursor.moveToFirst())
            {
                do
                {
                    Channel channel = getChannel(cursor);
                    long lastMgId = channel.getLastAddedId();
                    Message message = null;
                    if(lastMgId != -1)message = getMessage(cursor);
                    pairs.add(new Pair<>(channel,message));
                } while(cursor.moveToNext());
            }
        }
        catch (Exception e) {e.printStackTrace();}
        finally{
            if (cursor != null && !cursor.isClosed()) {cursor.close();}
        }
        return pairs;
    }


    private Channel getChannel(Cursor cursor)
    {
        Channel channel = new Channel();
        channel.setName(cursor.getString(cursor.getColumnIndex(KEY_CHANNEL_NAME)));
        channel.setType(cursor.getInt(cursor.getColumnIndex(KEY_CHANNEL_TYPE)));
        channel.setStatus(cursor.getInt(cursor.getColumnIndex(KEY_CHANNEL_STATUS)));
        channel.setLastReceivedId(cursor.getLong(cursor.getColumnIndex(KEY_CHANNEL_LAST_REC_ID)));
        channel.setLastAddedId(cursor.getLong(cursor.getColumnIndex(KEY_CHANNEL_LAST_ADD_ID)));
       return channel;
    }



    public ArrayList<Channel> getChannels()
    {
        ArrayList<Channel> channels = new ArrayList<>();
        String query =  "SELECT * FROM " +TABLE_CHANNELS;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        try{
            if(cursor.moveToFirst()){
                do{
                    channels.add(getChannel(cursor));
               } while(cursor.moveToNext());
            }
        }
        catch (Exception e) {e.printStackTrace();}
        finally{
            if (cursor != null && !cursor.isClosed()) {cursor.close();}
        }
        return channels;
    }


    public void deleteChannel(String channelName)
    {
        SQLiteDatabase db = getReadableDatabase();
        db.delete(TABLE_CHANNELS,KEY_CHANNEL_NAME + "= ? ",new String[]{channelName});
        db.delete(TABLE_MESSAGES,KEY_MESSAGE_CHANNEL + "= ? ",new String[]{channelName});
    }


    private Message getMessage(String query)
    {
        Message message = null;

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        try{
            if (cursor.moveToFirst())message = getMessage(cursor);
        }
        catch (Exception e) {e.printStackTrace();}
        finally{
            if (cursor != null && !cursor.isClosed()) {cursor.close();}
        }
        return message;

    }

    public boolean addChannel(Channel channel)
    {
        boolean result = false;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        ContentValues values = new ContentValues();
        values.put(KEY_CHANNEL_NAME, channel.getName());
        values.put(KEY_CHANNEL_TYPE, channel.getType());
        values.put(KEY_CHANNEL_STATUS, channel.getStatus());
        values.put(KEY_CHANNEL_LAST_REC_ID, channel.getLastReceivedId());
        values.put(KEY_CHANNEL_LAST_ADD_ID, channel.getLastAddedId());
        int rows = db.update(TABLE_CHANNELS, values, KEY_CHANNEL_NAME + "= ?", new
            String[]{channel.getName()});
        if (rows == 0)
        {
            db.insertOrThrow(TABLE_CHANNELS, null, values);
            result = true;
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        return result;
    }


    public ArrayList<Message> getChannelMessages(String channel)
    {
        ArrayList<Message> messages = new ArrayList<>();
        String query = String.format("SELECT * FROM %s WHERE %s = '%s' OR (%s = '%s' AND %s = " +
            "'%s') ORDER BY %s ASC", TABLE_MESSAGES, KEY_MESSAGE_CHANNEL, channel, KEY_MESSAGE_SENDER, username,
            KEY_MESSAGE_CHANNEL, channel, KEY_MESSAGE_TIMETOKEN);
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        try{
            if (cursor.moveToFirst()){
                do{
                    Message message = getMessage(cursor);
                    messages.add(message);
                } while(cursor.moveToNext());
            }
        }
        catch (Exception e) {e.printStackTrace();}
        finally{
            if (cursor != null && !cursor.isClosed()) {cursor.close();}
        }
        return messages;
    }

    private Message getMessage(Cursor cursor)
    {
        Message message = new Message();
        message.setId(cursor.getLong(cursor.getColumnIndex(KEY_MESSAGE_ID)));
        message.setSender(cursor.getString(cursor.getColumnIndex(KEY_MESSAGE_SENDER)));
        message.setTimetoken(cursor.getLong(cursor.getColumnIndex(KEY_MESSAGE_TIMETOKEN)));
        message.setText(cursor.getString(cursor.getColumnIndex(KEY_MESSAGE_TEXT)));
        message.setType(cursor.getInt(cursor.getColumnIndex(KEY_MESSAGE_TYPE)));
        message.setUrl(cursor.getString(cursor.getColumnIndex(KEY_MESSAGE_URL)));
        message.setLatLon(cursor.getString(cursor.getColumnIndex(KEY_MESSAGE_LAT_LON)));
        message.setStatus(cursor.getInt(cursor.getColumnIndex(KEY_MESSAGE_STATUS)));
        message.setChannelName(cursor.getString(cursor.getColumnIndex(KEY_MESSAGE_CHANNEL)));
        return message;
    }

    public void saveMessage(Message message)
    {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        ContentValues values = new ContentValues();
        values.put(KEY_MESSAGE_CHANNEL, message.getChannelName());
        values.put(KEY_MESSAGE_SENDER, message.getSender());
        values.put(KEY_MESSAGE_TIMETOKEN, message.getTimetoken());
        values.put(KEY_MESSAGE_TEXT, message.getText());
        values.put(KEY_MESSAGE_TYPE, message.getType());
        values.put(KEY_MESSAGE_URL, message.getUrl());
        values.put(KEY_MESSAGE_LAT_LON, message.getLatLon());
        values.put(KEY_MESSAGE_STATUS, message.getStatus());
        int rows = db.update(TABLE_MESSAGES, values,   KEY_MESSAGE_ID + " = ?", new String[]{"" + message.getId()});
        if (rows == 0){
            message.setId(db.insertOrThrow(TABLE_MESSAGES, null, values));
            db.setTransactionSuccessful();
            db.endTransaction();
            Channel channel = getChannel("SELECT * FROM "+ TABLE_CHANNELS +" WHERE " + KEY_CHANNEL_NAME + " = '" + message.getChannelName()+"'");
            channel.setLastAddedId(message.getId());
            Message last = getMessage(channel.getLastReceivedId());

            if(message.getStatus() > Message.READ_RECIPIENT ){
                if(last == null || message.getTimetoken() > last.getTimetoken()) channel.setLastReceivedId(message.getId());
            }
            addChannel(channel);
        }
        else{
            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }

    public Message saveMessage(JsonObject n, String channelName, long timetoken)
    {
        Message message = new Message();
        Channel channel = getChannel("SELECT * FROM "+ TABLE_CHANNELS +" WHERE " + KEY_CHANNEL_NAME + " = '" +channelName+"'");
        if (channel == null){
            channel = new Channel(channelName);
            addChannel(channel);
        }
        message.setChannelName(channelName);
        message.setSender(n.get("sender").getAsString());
        message.setStatus(Message.RECEIVED_ME);
        int type = n.get("type").getAsInt();
        message.setType(type);
        message.setText(n.get("text").getAsString());
        message.setTimetoken(timetoken);
        if(type==Message.IMAGE || type == Message.FILE)message.setUrl(n.get("url").getAsString());
        else if (type == Message.GMAPS)message.setLatLon(n.get("latlon").getAsString());
        saveMessage(message);
        return message;
    }

    public Message getMessage(long id)
    {
        String query = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + KEY_MESSAGE_ID + " = '" +id +"'";
        return getMessage(query);
    }

    public Channel getChannel(String query)
    {
        Channel channel = null;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        try{
            if (cursor.moveToFirst())channel = getChannel(cursor);
        }
        catch (Exception e) {e.printStackTrace();}
        finally{
            if (cursor != null && !cursor.isClosed()) {cursor.close();}
        }
        return channel;
    }


    public long getLastTimetoken()
    {
        String query = String.format("SELECT * FROM %s LEFT JOIN %s ON %s = %s WHERE  %s == 0 " +
            "ORDER BY %S DESC", TABLE_MESSAGES, TABLE_CHANNELS, KEY_MESSAGE_ID,
            KEY_CHANNEL_LAST_REC_ID, KEY_CHANNEL_TYPE, KEY_MESSAGE_TIMETOKEN);
        Channel channel = getChannel(query);
        return channel == null?0:getMessage(channel.getLastReceivedId()).getTimetoken();
    }


    public ArrayList<Message> getUnsentMessages()
    {
        ArrayList<Message> messages = new ArrayList<>();
        String query = String.format("SELECT * FROM %s WHERE  %s = 0", TABLE_MESSAGES, KEY_MESSAGE_STATUS);
        //Log.d(TAG, query);
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        try
        {
            if(cursor.moveToFirst()){
                do{
                    messages.add(getMessage(cursor));
                } while(cursor.moveToNext());
            }
        }
        catch(Exception e){e.printStackTrace();}
        finally{if(cursor != null && !cursor.isClosed())cursor.close();}
        return messages;
    }
}