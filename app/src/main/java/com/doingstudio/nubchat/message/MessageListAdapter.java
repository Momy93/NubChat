package com.doingstudio.nubchat.message;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.doingstudio.nubchat.R;
import java.util.ArrayList;

public class MessageListAdapter extends ArrayAdapter<Message>{
    private String myChannel;
    private Activity context;
    private ArrayList<Message> messages;

    public MessageListAdapter(Activity context, int textViewResourceId, ArrayList<Message> messages, String myChannel){
        super(context, textViewResourceId, messages);
        this.myChannel = myChannel;
        this.context = context;
        this.messages = messages;
    }

    @Override
    public int getViewTypeCount() {return 4;}

    @Override
    public int getItemViewType(int position) {return messages.get(position).getType();}

    @NonNull
    @Override
    public Message getItem(int position){return messages.get(position);}

    @NonNull
    @Override
    public View getView(int position, View convertView,@NonNull ViewGroup parent){

        LayoutInflater inflater = context.getLayoutInflater();
        final Message message = getItem(position);
        ViewHolder holder = new ViewHolder();

        if(convertView == null){
            switch(getItemViewType(position)){
                case 3:{
                    convertView = inflater.inflate(R.layout.row_image_message, null);
                    convertView.setTag(holder);
                    break;
                }

                default:{
                    convertView = inflater.inflate(R.layout.row_text_message, null);
                    convertView.setTag(holder);
                }
                break;
            }

        }
        else holder = (ViewHolder)convertView.getTag();

        if(getItemViewType(position)==3){
            final String[] coordinates;
            String latLon = message.getLatLon();
            if(latLon == null)latLon="0,0";
            coordinates = latLon.split(",");
            holder.image = (ImageView)convertView.findViewById(R.id.messageImageView);
            final ImageView image = holder.image;
            Glide.with(context).load("https://maps.googleapis.com/maps/api/staticmap?center=lat,lon&size=300x150&zoom=13&key=AIzaSyB2LFr92PSIehkCRTzybYostc9TuEOVnls&markers=size:mid%7Ccolor:0x0080ff%7Clabel:%7Clat,lon".replace("lat", coordinates[0]).replace("lon", coordinates[1]))
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        image.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Uri gmmIntentUri = Uri.parse("geo:"+coordinates[0]+","+coordinates[1]+"?q="+coordinates[0]+","+coordinates[1]/*+"("+Uri.encode(message.getSender())+")"*/);
                                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                                mapIntent.setPackage("com.google.android.apps.maps");
                                context.startActivity(mapIntent);
                            }
                        });
                        return false;
                    }
                })
                .placeholder(R.mipmap.gmaps_icon)
                .into(holder.image);

        }

        holder.sender = (TextView)convertView.findViewById(R.id.senderTextView);
        holder.content = (TextView)convertView.findViewById(R.id.contentTextView);
        holder.time = (TextView)convertView.findViewById(R.id.timeTextView);
        holder.layout = (LinearLayout)convertView.findViewById(R.id.linearLayout);
        holder.status = (ImageView)convertView.findViewById(R.id.statusImageView);

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
        ImageView image;
    }
}