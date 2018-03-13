package edu.pitt.cs.cs1631.g16.votingsoftware.guicomponent;

import android.Manifest;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import edu.pitt.cs.cs1631.g16.votingsoftware.sisservercommunication.SISServerCommunication;
import edu.pitt.cs.cs1631.g16.votingsoftware.inputprocessorcomponent.InputProcessor;
import edu.pitt.cs.cs1631.g16.votingsoftware.tablecomponent.TableComponent;
import tdr.sisprjremote.Util.KeyValueList;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "GUIComponent";

    private static final int SMS_PERMISSION_CODE = 0;

    private static TableLayout tl;

    private SISServerCommunication commn;
    private static String Scope = "SIS", Role = "Monitor";

    private static InputProcessor inputProcessor;
    private static TableComponent tableComponent;

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

                    KeyValueList parsedMsg = SISServerCommunication.parseMsg(str);

                    if (parsedMsg.getValue(SISServerCommunication.MsgId).equals("701")) {
                        tableComponent.castVote(parsedMsg);
                    } else if (parsedMsg.getValue(SISServerCommunication.MsgId).equals("702")) {
                        tableComponent.requestReport(parsedMsg);
                    } else if (parsedMsg.getValue(SISServerCommunication.MsgId).equals("703")) {
                        tableComponent.initializeTable(parsedMsg);
                    } else if (parsedMsg.getValue(SISServerCommunication.AckMsgId).equals("26")) {
                        ackInitialize(parsedMsg);
                    } else if (parsedMsg.getValue(SISServerCommunication.MsgId).equals("711")) {
                        ackVote(parsedMsg);
                    } else if (parsedMsg.getValue(SISServerCommunication.MsgId).equals("712")) {
                        ackRequestReport(parsedMsg);
                    }

                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    private static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getBaseContext();

        tl = findViewById(R.id.resultsTable);

        if (!isSmsPermissionGranted()) {
            Log.d(TAG, "Request permission");
            requestSmsPermission();
        } else {
            Log.d(TAG, "Already have permission");
        }

        try {
            commn = new SISServerCommunication(callbacks, Scope, Role);
            commn.register();
            commn.connect();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        inputProcessor = new InputProcessor();
        IntentFilter filter = new IntentFilter(inputProcessor.SMS_RECEIVED);
        this.registerReceiver(inputProcessor, filter);

        tableComponent = new TableComponent();
    }

    // Check if we have SMS permission
    private  boolean isSmsPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    // Request runtime SMS permission
    private void requestSmsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECEIVE_SMS)) {
            // You may display a non-blocking explanation here, read more in the documentation:
            // https://developer.android.com/training/permissions/requesting.html
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, SMS_PERMISSION_CODE);
    }

    private static void ackInitialize(KeyValueList msg) {
        if (msg.getValue(SISServerCommunication.YesNo).equals("Yes")) {
            Toast.makeText(context, "Tables initialized", Toast.LENGTH_LONG).show();
        } else if (msg.getValue(SISServerCommunication.YesNo).equals("No")) {
            Toast.makeText(context, "Tables not initialized", Toast.LENGTH_LONG).show();
        }
    }

    private static void ackVote(KeyValueList msg) {
        if (msg.getValue(SISServerCommunication.Status).equals("1")) {
            Toast.makeText(context, "Duplicate vote", Toast.LENGTH_LONG).show();
        } else if (msg.getValue(SISServerCommunication.Status).equals("2")) {
            Toast.makeText(context, "Invalid vote", Toast.LENGTH_LONG).show();
        } else if (msg.getValue(SISServerCommunication.Status).equals("3")) {
            Toast.makeText(context, "Valid vote", Toast.LENGTH_LONG).show();
        }
    }

    private static void ackRequestReport(KeyValueList msg) {
        String[] results = msg.getValue(SISServerCommunication.RankedReport).split(";");

        TableRow tr = new TableRow(context);
        TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);

        TextView ranking = new TextView(context);
        ranking.setText("Ranking");
        ranking.setTypeface(null, Typeface.BOLD);

        TextView candidateID = new TextView(context);
        candidateID.setText("Candidate ID");
        candidateID.setTypeface(null, Typeface.BOLD);

        TextView numVotes = new TextView(context);
        numVotes.setText("# Votes");
        numVotes.setTypeface(null, Typeface.BOLD);

        tr.addView(ranking);
        tr.addView(candidateID);
        tr.addView(numVotes);

        tl.addView(tr, 0);

        for (int i = 0; i < results.length; i++) {
            String[] winner = results[i].split(",");

            tr =  new TableRow(context);
            lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);

            ranking = new TextView(context);
            ranking.setText("" + (i + 1));

            candidateID = new TextView(context);
            candidateID.setText(winner[0]);

            numVotes = new TextView(context);
            numVotes.setText(winner[1]);

            tr.addView(ranking);
            tr.addView(candidateID);
            tr.addView(numVotes);

            tl.addView(tr, i + 1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (commn != null){
            commn.disconnect();
        }
        unregisterReceiver(inputProcessor);
    }
}
