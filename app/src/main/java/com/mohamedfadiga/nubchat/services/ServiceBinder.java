package com.mohamedfadiga.nubchat.services;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class ServiceBinder
{
    private BackgroundService boundService;
    private boolean bound = false;
    private Activity activity;
    private static int boundActivities = 0;

    private ServiceConnection connection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            BackgroundService.MyBinder binder = (BackgroundService.MyBinder) service;
            boundService = binder.getService();
            bound = true;
            boundService.setCallbacks((BackgroundService.ServiceCallbacks) activity);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };


    public ServiceBinder(Activity activity)
    {
        this.activity = activity;
    }


    public void bind()
    {
        Intent intent = new Intent(activity, BackgroundService.class);
        activity.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        boundActivities++;
    }

    public void unbind()
    {
        if (bound)
        {
            bound = false;
            activity.unbindService(connection);
            boundActivities--;
            if(boundActivities == 0)boundService.clearCallbacks();
        }
    }

    public BackgroundService getService()
    {
        return boundService;
    }
}
