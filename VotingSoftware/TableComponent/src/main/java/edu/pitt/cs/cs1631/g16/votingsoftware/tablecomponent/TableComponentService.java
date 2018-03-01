package edu.pitt.cs.cs1631.g16.votingsoftware.tablecomponent;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.util.Hashtable;

import edu.pitt.cs.cs1631.g16.votingsoftware.sisservercommunication.SISServerCommunication;
import tdr.sisprjremote.Util.KeyValueList;

public class TableComponentService extends Service {

    public static final String TAG = "TableComponent";

    private static SISServerCommunication commn;
    private static String Scope = "SIS", Role = "Monitor", Receiver = "GUIComponent";

    private static final String MsgId = "MsgId";
    private static final String Passcode = "Passcode", Passcode_Val = "*****";
    private static final String CandidateList = "CandidateList";
    private static final String VoterPhoneNo = "VoterPhoneNo";
    private static final String CandidateID = "CandidateID";
    private static final String N = "N";
    private static final String Status = "Status";
    private static final String RankedReport = "RankedReport";

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
                    Log.d(TAG, str);

                    KeyValueList parsedMsg = KeyValueList.decodedKV(str);

                    if (parsedMsg.getValue(MsgId).equals("701")) {
                        castVote(parsedMsg);
                    } else if (parsedMsg.getValue(MsgId).equals("702")) {
                        requestReport(parsedMsg);
                    } else if (parsedMsg.getValue(MsgId).equals("703")) {
                        initializeTable(parsedMsg);
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    private static VoterTable voterTable;
    private static TallyTable tallyTable;

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

    private static void initializeTable(KeyValueList msg) {
        String yesNo = "No";

        if (msg.getValue(Passcode).equals(Passcode_Val)) {
            voterTable = new VoterTable();
            tallyTable = new TallyTable(msg.getValue(CandidateList));
            yesNo = "Yes";
        }

        try {
            commn.ack("701", yesNo, TAG);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private static void castVote(KeyValueList msg) {
        int status = voterTable.recordVoter(msg.getValue(VoterPhoneNo));
        if (status == 0) {
            status = tallyTable.recordVote(msg.getValue(CandidateID));
        }

        Hashtable<String, String> attr = new Hashtable<>();
        attr.put(MsgId, "711");
        attr.put(Status, status + "");

        try {
            commn.sendMessage(Receiver, SISServerCommunication.MessageType.ALERT, "Acknowledge Vote", attr);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private static void requestReport(KeyValueList msg) {
        String rankedReport = null;

        if (msg.getValue(Passcode).equals(Passcode_Val)) {
            rankedReport = tallyTable.rankedCandidates(Integer.parseInt(msg.getValue(N)));
        }

        Hashtable<String, String> attr = new Hashtable<>();
        attr.put(MsgId, "712");
        attr.put(RankedReport, rankedReport);

        try {
            commn.sendMessage(Receiver, SISServerCommunication.MessageType.ALERT, "Acknowledge", attr);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        voterTable = null;
        tallyTable = null;
    }
}
