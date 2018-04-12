package edu.pitt.cs.cs1631.g16.votingsoftware.guicomponent;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import java.util.Hashtable;
import java.util.Timer;

import edu.pitt.cs.cs1631.g16.votingsoftware.inputprocessorcomponent.InputProcessorService;
import edu.pitt.cs.cs1631.g16.votingsoftware.sisservercommunication.SISServerCommunication;
import edu.pitt.cs.cs1631.g16.votingsoftware.sisservercommunication.SISServerCommunication.MessageType;
import edu.pitt.cs.cs1631.g16.votingsoftware.tablecomponent.TableComponentService;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static final String TAG = "GUIComponent";

    private static final int SMS_PERMISSION_CODE = 0;

    //UI Elements
    private Button connect, start, vote, results, script;
    private EditText ipAddr;
    private EditText port;
    private static TextView messageText;

    //Keys for XML
    private static final String MsgId = "MsgId";
    private static final String Passcode = "Passcode";
    private static final String CandidateList = "CandidateList";
    private static final String N = "N";
    private static final String VoterPhoneNo = "VoterPhoneNo";
    private static final String CandidateID = "CandidateID";

    private String connectData;
    private String phoneNo = "+4432802210", candidateID = "1";
    private String code, list;


    private SISServerCommunication commn;
    private static String Scope = "SIS", Role = "Monitor", Receiver = "SISServer", Msg = "Hello!";

    static Handler callbacks = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            String str;
            Log.d(TAG, msg.what + "");
            switch (msg.what) {
                case SISServerCommunication.MESSAGE_RECEIVED:
                    Log.d(TAG, "Message Received");
                    str = (String)msg.obj;
                    messageText.append(str + "***********\n");
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    private Intent inputProcessor;
    private Intent tableComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Set up buttons
        connect = findViewById(R.id.connectBtn);
        connect.setOnClickListener(this);

        start = findViewById(R.id.start);
        start.setOnClickListener(this);
        start.setEnabled(false);

        vote = findViewById(R.id.vote);
        vote.setOnClickListener(this);
        vote.setEnabled(false);

        results = findViewById(R.id.request);
        results.setOnClickListener(this);
        results.setEnabled(false);

        script = findViewById(R.id.script);
        script.setOnClickListener(this);

        ipAddr = findViewById(R.id.ipInput);
        port = findViewById(R.id.portInput);
        messageText = findViewById(R.id.messageReceivedList);

        if (!isSmsPermissionGranted()) {
            Log.d(TAG, "Request permission" );
            requestSmsPermission();
        } else {
            Log.d(TAG, "Already have permission");
        }
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

    public void onClick(View v){
        final  Context context = this;

        if(v.getId() == R.id.connectBtn) {

            connectData = ipAddr.getText().toString() + ";" + port.getText().toString();

            // Start the InputProcessor
            inputProcessor = new Intent(this, InputProcessorService.class);
            inputProcessor.setData(Uri.parse(connectData));
            startService(inputProcessor);

            Timer time = new Timer();
            try {
                time.wait(200);
            }catch (Exception e){
                Log.e(TAG, e.toString());
            }

            // Start the TableComponent
            tableComponent = new Intent(this, TableComponentService.class);
            tableComponent.setData(Uri.parse(connectData));
            startService(tableComponent);

            try {
                time.wait(200);
            }catch (Exception e){
                Log.e(TAG, e.toString());
            }
            try {
                commn = new SISServerCommunication(ipAddr.getText().toString(), Integer.parseInt(port.getText().toString()), callbacks, Scope, Role, TAG);

                connect.setEnabled(false);

                commn.register();
                commn.connect();

            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }

            //Once the GUI is connected we can start the voting process
            start.setEnabled(true);

        }else if(v.getId() == R.id.start){
            //Input processor and tablecomponent are now running, we can vote and get results
            vote.setEnabled(true);
            results.setEnabled(true);
            start.setEnabled(false);

            //Builder to get password
            AlertDialog.Builder userPasscodeDialog = new AlertDialog.Builder(this);
            userPasscodeDialog.setTitle("Enter PIN");

            // Set up the input
            final EditText passcodeInput = new EditText(this);
            passcodeInput.setInputType(InputType.TYPE_CLASS_TEXT);
            userPasscodeDialog.setView(passcodeInput);

            // Set up the buttons
            userPasscodeDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    code = passcodeInput.getText().toString();
                    //Builder to get list of candidates
                    AlertDialog.Builder candidateInputBox = new AlertDialog.Builder(context);
                    candidateInputBox.setTitle("Enter a list of candidates separated by ;");

                    // Set up the input
                    final EditText candidateInput = new EditText(context);
                    candidateInput.setInputType(InputType.TYPE_CLASS_TEXT);
                    candidateInputBox.setView(candidateInput);

                    // Set up the buttons
                    candidateInputBox.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String msg = "";
                            Hashtable<String, String> attr = new Hashtable<>();
                            list = candidateInput.getText().toString();
                            String Receiver = "TableComponent";
                            try {
                                attr.put(MsgId, "703");
                                attr.put(Passcode, code);
                                attr.put(CandidateList, list);
                                commn.sendMessage(TAG, Receiver, SISServerCommunication.MessageType.ALERT, msg, attr);
                            } catch (Exception e) {
                                Log.e(TAG, e.toString());
                            }
                        }
                    });
                    candidateInputBox.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    candidateInputBox.show();

                }
            });
            userPasscodeDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            userPasscodeDialog.show();


        }else if(v.getId() == R.id.vote){

            //Nested AlertBuilders are ugly but they are the only way to "halt" the UI thread and
            //force it to wait for a response from the Alerts before sending the vote message to the
            //SISserver

            //Builder to get phone number
            AlertDialog.Builder phoneNoBuilder = new AlertDialog.Builder(this);
            phoneNoBuilder.setTitle("Enter Phone Number (no dashes) \"+4432802210\"");

            // Set up the input
            final EditText phoneNoInput = new EditText(this);
            phoneNoInput.setInputType(InputType.TYPE_CLASS_PHONE);
            phoneNoBuilder.setView(phoneNoInput);

            // Set up the buttons
            phoneNoBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    phoneNo = phoneNoInput.getText().toString();
                    //Builder to get candidate to vote for
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Enter candidate ID");

                    // Set up the input
                    final EditText candidateIDText = new EditText(context);
                    candidateIDText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    builder.setView(candidateIDText);

                    // Set up the buttons
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                candidateID = candidateIDText.getText().toString();
                                String msg = "";
                                Hashtable<String, String> attr = new Hashtable<>();
                                String Receiver = "TableComponent";

                                try {
                                    attr.put(MsgId, "701");
                                    attr.put(VoterPhoneNo, phoneNo);
                                    attr.put(CandidateID, candidateID);
                                    commn.sendMessage(TAG, Receiver, SISServerCommunication.MessageType.ALERT, msg, attr);
                                }catch (Exception e) {
                                    Log.i(TAG, e.toString());
                                }
                            }catch(NumberFormatException e){
                                Log.e(TAG, "Not a number");
                            }
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();
                }
            });
            phoneNoBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            phoneNoBuilder.show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(commn!=null){
            commn.disconnect();
        }
        if (inputProcessor != null) {
            stopService(inputProcessor);
        }
        if (tableComponent != null) {
            stopService(tableComponent);
        }
    }
}
