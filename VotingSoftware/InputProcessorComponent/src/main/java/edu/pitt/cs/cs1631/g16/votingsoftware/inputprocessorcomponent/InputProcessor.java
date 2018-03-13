package edu.pitt.cs.cs1631.g16.votingsoftware.inputprocessorcomponent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.Hashtable;

import edu.pitt.cs.cs1631.g16.votingsoftware.sisservercommunication.SISServerCommunication;

public class InputProcessor extends BroadcastReceiver {

    public static final String TAG = "InputProcessor";

    public static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    public InputProcessor() {}

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
                    SISServerCommunication.sendMessage(SISServerCommunication.VotingSoftware, SISServerCommunication.MessageType.ALERT, msg, attr);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
    }
}
