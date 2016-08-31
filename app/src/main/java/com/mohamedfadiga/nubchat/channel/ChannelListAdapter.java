package com.mohamedfadiga.nubchat.channel;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.mohamedfadiga.nubchat.R;
import com.mohamedfadiga.nubchat.message.Message;

import java.util.ArrayList;


public class ChannelListAdapter extends ArrayAdapter<Pair<Channel,Message>>
{
    public ChannelListAdapter(Context context, int textViewResourceId, ArrayList<Pair<Channel,Message>> channels){
        super(context, textViewResourceId, channels);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        ViewHolder holder;

        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.row_channel, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }
        else holder = (ViewHolder) convertView.getTag();

        Pair<Channel, Message> item = getItem(position);
        holder.channelName.setText(item.first.getName());
        Message message = item.second;
        if(message != null)
        {
            String sender = (item.second.getType() == 1 ?message.getSender()+": " :"");
            if(message.getStatus() == Message.RECEIVED_ME) {
                holder.lastMessage.setTextColor(ContextCompat.getColor(getContext(), R.color.magicBlue));
            }
            else holder.lastMessage.setTextColor(Color.BLACK);
            holder.lastMessage.setText(sender + message.getText());
            holder.lastMsgTime.setText(message.getTimeLabel());
        }
        else
        {
            holder.lastMessage.setText("");
            holder.lastMsgTime.setText("");
        }
        String firstLetter = String.valueOf(item.first.getName().charAt(0));

        ColorGenerator generator = ColorGenerator.MATERIAL;

        int color = generator.getColor(item.first.getName());

        TextDrawable drawable = TextDrawable.builder().buildRound(firstLetter, color);
        holder.channelIcon.setImageDrawable(drawable);
        return convertView;
    }

    private class ViewHolder
    {
        private ImageView channelIcon;
        private TextView channelName, lastMessage, lastMsgTime;

        private ViewHolder(View v)
        {
            channelIcon = (ImageView) v.findViewById(R.id.channelIcon);
            channelName = (TextView) v.findViewById(R.id.channelName);
            lastMessage = (TextView) v.findViewById(R.id.lastMessage);
            lastMsgTime = (TextView) v.findViewById(R.id.lastMsgTime);
        }
    }
}
