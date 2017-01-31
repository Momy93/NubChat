
## Introduction
Nubchat is an Adroid app that let you communicate with other android smartphones, or dev boards like Arduino, RaspberryPi and all the devices that support PubNub API's.<br>
To use this app you'll need a subcribe key and publish key from  https://pubnub.com, and enable "STORAGE & PLAYBACK" AND "STREAM CONTROLLER".<br>
It's available on Google play store https://play.google.com/store/apps/details?id=com.mohamedfadiga.nubchat


## Usage

There are many things you can do with it. The main purpose is that you have some devices that will communicate, and do actions or give you informations about themeselves when you write to them.
You can find an example here  https://www.hackster.io/Momy93/nubchat-chat-app-for-devices-and-humans-with-pubnub-bad66c

<br>
![description](https://raw.githubusercontent.com/pluralsight/guides/master/images/f40d498e-9a95-410e-b4ab-d82c2c38f4d5.jpg)


![description](https://raw.githubusercontent.com/pluralsight/guides/master/images/1d94a6ce-b693-4248-a9e3-4f413cbac804.jpg)


![description](https://raw.githubusercontent.com/pluralsight/guides/master/images/79d625a5-3520-4677-a8bb-f0dd0b23ed49.jpg)





## How it works
For the first usage, you have to chose a username and insert your pubnub keys.<br>
Of course these keys are personal, and should not share them with other people.<br>

<i>"So, what if i want to chat with my friend Mark?"</i>

This app is designed for communicating with your devices, and an Android smartphone is a device, but used by another person.<br>
So if you want to chat with <i>Mark</i> you'll have to provide him your keys.<br>
Otherwise if you want to use NubChat as a normal chat app for smartphones, hard code your keys in the app souce.<br>

The username you chose, is  not your pubnub email or username, it's just a name you chose to receive  messages on the current device.<br>
Once you make this first setup, you can start using tha app.<br>
You can edit your username and PubNub keys when you want on Menu->Seetings.<br>

With the "+" Button you can add new channel. You'll be prompt to chose the channel name and set if it's a public or private. Public and private channel are not functions from Pubnub. This logic is created inside the app using pubnub API's.
When you add a new channel, its informations are store on a local database on your smartphone.<br>
To explain you well how the app works, let say you installed it and you chose "Bob" as username.<br>
Let's add a private channel called RaspberryPi.<br> 



![description](https://raw.githubusercontent.com/pluralsight/guides/master/images/af6f8f58-7ea1-43a5-a6bf-91d6d0f714fb.jpg)



As already said data about this new channel are stored in the local database. The databasse's channels table structure is something like this:<br>

<table >
  <tr>
    <th>name</th>
    <th>channel_type</th> 
    <th>channel_status</th>
    <th>last_rec_id</th>
    <th>last_add_id</th>
  </tr>
  <tr>
    <td>RaspberryPi</td>
    <td>0</td> 
    <td>0</td>
    <td>-1</td>
    <td>-1</td>
  </tr>
</table>

name: the name of the channel. <br>
channel_type: 0 for private channel and 1 for a public one. <br>
channel_status: not used in this first release of the app.<br>
last_rec_id: the id of the last received message (from our Raspeberry)<br>
last_add_id: the id of the last message added to this channel, either sent or received.<br>

last_rec_id and last_add_id are -1 because the channel is empty yet.
when we send a message the app use pubnub publish function.

A  message sent by the app has the following structure:

```{
  "channelName": "RaspberryPi",
  "sender": "Bob",
  "type": 0,
  "text": "Dattebayo!",
  "timetoken": 14858572070330000
}```

Type for now is always 0 (pure text message).
in the Message class you can see future types that will be available:

```public static final int TEXT = 0, IMAGE = 1, FILE =2 , GMAPS = 3;```

When sending a message to the app from your device, you just need 3 field:

```{
  "sender": "RaspberryPi",
  "type": 0,
  "text": "Dattebane!"
}```


When RaspberyPi wants to send me a message, it will send it to "Bob" channel, and as I am a subscriber, I will receive it.
Even if I didn't added the private Channel, if someone publish a message direclty on my channel, I receive it.

For public channel, it is not efficient to subscribe to all the channels. Pubnub guidlines says:

> Currently, we recommend you use channel multiplexing for up to 10 (ten) channels. If your app requires a single client connection to subscribe to more than 10 channels, we recommend using channel groups. But you can subscribe to 50 channels from a single client.

So I'm using a channel group. 
Lets say we added two new public channels: Group1 and Group 2:

<table >
  <tr>
    <th>name</th>
    <th>channel_type</th> 
    <th>channel_status</th>
    <th>last_rec_id</th>
    <th>last_add_id</th>
  </tr>
    <tr>
    <td>RaspberryPi</td>
    <td>0</td> 
    <td>0</td>
    <td>7</td>
    <td>5</td>
  </tr>
  <tr>
    <td>Group1</td>
    <td>1</td> 
    <td>0</td>
    <td>-1</td>
    <td>-1</td>
  </tr>
    <tr>
    <td>Group2</td>
    <td>1</td> 
    <td>0</td>
    <td>-1</td>
    <td>-1</td>
  </tr>
</table>

Now what I do is to add Group1 and Group2 to a channel group, so I just need to subscribe to it, in order to receive messages from the these channels.
Adding channels to a channel group doesn't mean that they are logically inserted in the group. You can still subscribe and publish to this channel.
What the channel group does is something like subscribe to all the channel you added, and send you back the messages he receive from them.
In my case I add public channels to a channel group that has the same name of my channel (Bob).

    pubNub.subscribe()
                .channelGroups(Collections.singletonList(username))
                .channels(Collections.singletonList(username))
                .execute();
 
As you can see subscribing to channels and to channel group has two separated methods, and I'm giving them always "Bob" as a parameter.
When the subscribe method calls my calback, it tells me if someone publish directly on channel "Bob", or someone published on a channel added to the channel group called "Bob".
This is the callback called when there is a new message:

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
  

If a message has been sent to a public channel, r.getSubscription()) value could be for example "Group1" or "Group2".
So, when r.getSubscription() give me a null pointer, I know that the message has been published directly on "Bob" channel and not on a public channel.

When your device goes online after being offline, the app uses history funcition to get messages you missed while you were offline.  
This time I have to call the history function for all the public channels because channel group hasn't history capability.
Maybe there is a better way to do this anyway but I don't get by now.
For private channels instead, I just need to call history on my own channel (Bob).
