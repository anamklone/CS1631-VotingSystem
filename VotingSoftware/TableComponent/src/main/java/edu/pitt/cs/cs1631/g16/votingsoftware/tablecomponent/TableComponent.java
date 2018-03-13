package edu.pitt.cs.cs1631.g16.votingsoftware.tablecomponent;

import android.util.Log;

import java.util.Hashtable;

import edu.pitt.cs.cs1631.g16.votingsoftware.sisservercommunication.SISServerCommunication;
import tdr.sisprjremote.Util.KeyValueList;

public class TableComponent {

    public static final String TAG = "TableComponent";

    public TableComponent() { }

    private VoterTable voterTable;
    private TallyTable tallyTable;

    public void initializeTable(KeyValueList msg) {
        Log.d(TAG, "Initializing tables");

        String yesNo = "No";

        if (msg.getValue(SISServerCommunication.Passcode).equals(SISServerCommunication.Passcode_Val)) {
            voterTable = new VoterTable();
            tallyTable = new TallyTable(msg.getValue(SISServerCommunication.CandidateList));
            yesNo = "Yes";
        }

        try {
            SISServerCommunication.ack("26", yesNo, TAG);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void castVote(KeyValueList msg) {
        Log.d(TAG, "Casting vote");

        int status = voterTable.recordVoter(msg.getValue(SISServerCommunication.VoterPhoneNo));
        if (status == 0) {
            status = tallyTable.recordVote(msg.getValue(SISServerCommunication.CandidateID));
        }

        Hashtable<String, String> attr = new Hashtable<>();
        attr.put(SISServerCommunication.MsgId, "711");
        attr.put(SISServerCommunication.Status, status + "");

        try {
            SISServerCommunication.sendMessage(SISServerCommunication.VotingSoftware, SISServerCommunication.MessageType.ALERT, "Acknowledge Vote", attr);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void requestReport(KeyValueList msg) {
        Log.d(TAG, "Requesting report");

        String rankedReport = null;

        if (msg.getValue(SISServerCommunication.Passcode).equals(SISServerCommunication.Passcode_Val)) {
            rankedReport = tallyTable.rankedCandidates(Integer.parseInt(msg.getValue(SISServerCommunication.N)));
        }

        Hashtable<String, String> attr = new Hashtable<>();
        attr.put(SISServerCommunication.MsgId, "712");
        attr.put(SISServerCommunication.RankedReport, rankedReport);

        try {
            SISServerCommunication.sendMessage(SISServerCommunication.VotingSoftware, SISServerCommunication.MessageType.ALERT, "Acknowledge", attr);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        voterTable = null;
        tallyTable = null;
    }
}
