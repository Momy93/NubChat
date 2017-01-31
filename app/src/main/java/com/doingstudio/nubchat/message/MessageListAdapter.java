package com.doingstudio.nubchat.message;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.doingstudio.nubchat.R;
import java.util.ArrayList;

public class MessageListAdapter extends ArrayAdapter<Message>
{
    private String myChannel;

    public MessageListAdapter(Context context, int textViewResourceId, ArrayList<Message> messages, String myChannel)
    {
        super(context, textViewResourceId, messages);
        this.myChannel = myChannel;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView,@NonNull ViewGroup parent){
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.row_message, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }
        else holder = (ViewHolder) convertView.getTag();

        Message message = getItem(position);
        boolean sent = message.getSender().equals(myChannel);
        if(sent){
            holder.layout.setGravity(Gravity.END);
            holder.sender.setVisibility(View.GONE);
            holder.sender.setText("");
        }
        else{
            holder.layout.setGravity(Gravity.START);
            holder.sender.setVisibility(View.VISIBLE);
            holder.sender.setText(message.getSender());
        }
        holder.content.setText(message.getText());
        holder.time.setText(message.getTimeLabel());
        if(message.getStatus() > Message.SENDING) holder.status.setImageResource(R.mipmap.sent_icon);
        else holder.status.setImageResource(R.mipmap.sending_icon);
        return convertView;
    }


    private class ViewHolder{
        TextView sender, content, time;
        LinearLayout layout;
        ImageView status;

        ViewHolder(View v){
            sender = (TextView) v.findViewById(R.id.senderTextView);
            content = (TextView) v.findViewById(R.id.contentTextView);
            time = (TextView) v.findViewById(R.id.timeTextView);
            layout = (LinearLayout) v.findViewById(R.id.linearLayout);
            status = (ImageView)v.findViewById(R.id.statusImageView);
        }
    }
}
