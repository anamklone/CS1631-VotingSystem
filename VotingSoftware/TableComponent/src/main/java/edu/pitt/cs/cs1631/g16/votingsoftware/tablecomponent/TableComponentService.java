package edu.pitt.cs.cs1631.g16.votingsoftware.tablecomponent;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.util.Hashtable;
import java.util.Timer;

import edu.pitt.cs.cs1631.g16.votingsoftware.sisservercommunication.SISServerCommunication;
import tdr.sisprjremote.Util.KeyValueList;

public class TableComponentService extends Service {

    public static final String TAG = "TableComponent";

    private static SISServerCommunication commn;
    private static String Scope = "SIS", Role = "Monitor", Receiver = "GUIComponent";

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

                    KeyValueList parsedMsg = SISServerCommunication.parseMsg(str);

                    if (parsedMsg.getValue(SISServerCommunication.MsgId).equals("701")) {
                        castVote(parsedMsg);
                    } else if (parsedMsg.getValue(SISServerCommunication.MsgId).equals("702")) {
                        requestReport(parsedMsg);
                    } else if (parsedMsg.getValue(SISServerCommunication.MsgId).equals("703")) {
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
    }

    private static void initializeTable(KeyValueList msg) {
        Log.d(TAG, "Initializing tables");

        String yesNo = "No";

        if (msg.getValue(SISServerCommunication.Passcode).equals(SISServerCommunication.Passcode_Val)) {
            voterTable = new VoterTable();
            tallyTable = new TallyTable(msg.getValue(SISServerCommunication.CandidateList));
            yesNo = "Yes";
        } else {
            Log.i(TAG, "Incorrect Passcode!!!!");
        }

        try {
            commn.ack(Receiver, "26", yesNo, TAG);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private static void castVote(KeyValueList msg) {
        Log.d(TAG, "Casting vote");
        if(voterTable == null)
            Log.i(TAG, "Voter Table is null");
        if(msg == null)
            Log.i(TAG, "msg is null");
        if(SISServerCommunication.VoterPhoneNo == null)
            Log.i(TAG, "voter phone no is null");
        if(SISServerCommunication.CandidateID == null)
            Log.i(TAG, "candidate ID is null");
        int status = voterTable.recordVoter(msg.getValue(SISServerCommunication.VoterPhoneNo));
        if (status == 0) {
            status = tallyTable.recordVote(msg.getValue(SISServerCommunication.CandidateID));
        }

        Hashtable<String, String> attr = new Hashtable<>();
        attr.put(SISServerCommunication.MsgId, "711");
        attr.put(SISServerCommunication.Status, status + "");

        try {
            commn.sendMessage(TAG, Receiver, SISServerCommunication.MessageType.ALERT, "Acknowledge Vote", attr);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private static void requestReport(KeyValueList msg) {
        Log.d(TAG, "Requesting report");

        if(voterTable == null)
            Log.i(TAG, "Voter Table is null");
        if(msg == null)
            Log.i(TAG, "msg is null");

        String rankedReport = null;

        if (msg.getValue(SISServerCommunication.Passcode).equals(SISServerCommunication.Passcode_Val)) {
            rankedReport = tallyTable.rankedCandidates(Integer.parseInt(msg.getValue(SISServerCommunication.N)));
        }

        Hashtable<String, String> attr = new Hashtable<>();
        attr.put(SISServerCommunication.MsgId, "712");
        attr.put(SISServerCommunication.RankedReport, rankedReport);

        try {
            commn.sendMessage(TAG, Receiver, SISServerCommunication.MessageType.ALERT, "Acknowledge", attr);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        voterTable = null;
        tallyTable = null;
    }
}
