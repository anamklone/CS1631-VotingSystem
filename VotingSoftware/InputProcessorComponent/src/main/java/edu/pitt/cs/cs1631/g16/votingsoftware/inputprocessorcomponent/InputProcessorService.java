package edu.pitt.cs.cs1631.g16.votingsoftware.inputprocessorcomponent;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.Hashtable;

import edu.pitt.cs.cs1631.g16.votingsoftware.sisservercommunication.SISServerCommunication;

public class InputProcessorService extends Service {

    public static final String TAG = "InputProcessorComponent";

    private SISServerCommunication commn;
    private static String Scope = "SIS", Role = "Monitor", Receiver = "TableComponent";

    private static final String MsgId = "MsgId";
    private static final String VoterPhoneNo = "VoterPhoneNo";
    private static final String CandidateID = "CandidateID";

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

    @Override
    public void onCreate() {
        Log.d(TAG, "Service created");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting service");

        String[] connectData = intent.getDataString().split(";");

        try {
            commn = new SISServerCommunication(connectData[0], Integer.parseInt(connectData[1]), callbacks, Scope, Role, TAG);
            commn.register();
            commn.connect();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Ending service");
    }

    public void sendVote(String voterPhoneNo, String candidateID) {
        Hashtable<String, String> attr = new Hashtable<>();
        attr.put(MsgId, "701");
        attr.put(VoterPhoneNo, voterPhoneNo);
        attr.put(CandidateID, candidateID);

        try {
            commn.sendMessage(Receiver, SISServerCommunication.MessageType.ALERT, "Cast Vote", attr);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public static class SmsReceiver extends BroadcastReceiver {

        private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

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
                    String message = sb.toString();

                    //sendVote(sender, message);
                }
            }
        }
    }
}
