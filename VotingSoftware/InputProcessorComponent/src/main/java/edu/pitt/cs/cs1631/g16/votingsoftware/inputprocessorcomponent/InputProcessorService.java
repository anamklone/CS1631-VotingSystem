package edu.pitt.cs.cs1631.g16.votingsoftware.inputprocessorcomponent;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.Hashtable;
import java.util.Timer;

import edu.pitt.cs.cs1631.g16.votingsoftware.sisservercommunication.SISServerCommunication;

public class InputProcessorService extends Service {

    public static final String TAG = "InputProcessorComponent";

    private SISServerCommunication commn;
    private static String Scope = "SIS", Role = "Monitor", Receiver = "TableComponent";

    static Handler callbacks = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            String str;
            Log.d(TAG, msg.what + "");
            switch (msg.what) {
                case SISServerCommunication.CONNECTED:
                    Log.d(TAG, "Connected");
                    break;
                case SISServerCommunication.MESSAGE_RECEIVED:
                    Log.d(TAG, "Message Received");
                    str = (String)msg.obj;
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    private SmsReceiver smsReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting service");

        if(intent == null)
            Log.e(TAG, "Intent is null");
        else
            Log.e(TAG, "Intent is not null");
        String[] connectData = intent.getDataString().split(";");

        try {
            commn = new SISServerCommunication(connectData[0], Integer.parseInt(connectData[1]), callbacks, Scope, Role, TAG);
            commn.register();
            Timer time = new Timer();
            try {
                time.wait(200);
            }catch (Exception e){
                Log.e(TAG, e.toString());
            }
            commn.connect();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        smsReceiver = new SmsReceiver();
        registerReceiver(smsReceiver, filter);

        return START_REDELIVER_INTENT;
    }


    @Override
    public IBinder onBind(Intent intent) {
        // Don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Ending service");
        unregisterReceiver(smsReceiver);
    }

    public class SmsReceiver extends BroadcastReceiver {

        private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

        public SmsReceiver(){
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SMS_RECEIVED)) {
                Log.d(TAG, "SMS received");

                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    // get sms objects
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    if (pdus.length == 0) {
                        return;
                    }
                    // large message might be broken into many
                    SmsMessage[] messages = new SmsMessage[pdus.length];
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < pdus.length; i++) {
                        messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        sb.append(messages[i].getMessageBody());
                    }
                    String sender = messages[0].getOriginatingAddress();
                    Log.e(TAG, sender);
                    String message = sb.toString();

                    String msg = "";
                    Hashtable<String, String> attr = new Hashtable<>();

                    String[] splitMsg = message.split(" ");

                    for (int i = 0; i < splitMsg.length; i++) {
                        Log.d(TAG, "splitMsg[" + i + "] = " + splitMsg[i]);
                    }

                    if (splitMsg[0].toLowerCase().equals("start")) {
                        try {
                            attr.put(SISServerCommunication.MsgId, "703");
                            attr.put(SISServerCommunication.Passcode, splitMsg[1]);
                            attr.put(SISServerCommunication.CandidateList, splitMsg[2]);
                        } catch (Exception e) {
                            Log.e(TAG, "Incorrect message format");
                        }
                    } else if (splitMsg[0] .toLowerCase().equals("stop")) {
                        try {
                            attr.put(SISServerCommunication.MsgId, "702");
                            attr.put(SISServerCommunication.Passcode, splitMsg[1]);
                            attr.put(SISServerCommunication.N, splitMsg[2]);
                        } catch (Exception e) {
                            Log.e(TAG, "Incorrect message format");
                        }
                    } else {
                        attr.put(SISServerCommunication.MsgId, "701");
                        attr.put(SISServerCommunication.VoterPhoneNo, sender);
                        attr.put(SISServerCommunication.CandidateID, message);
                    }

                    try {
                        commn.sendMessage(TAG, Receiver, SISServerCommunication.MessageType.ALERT, msg, attr);
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
        }
    }
}
