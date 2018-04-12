package edu.pitt.cs.cs1631.g16.votingsoftware.guicomponent;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
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
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.Hashtable;
import java.util.Timer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import edu.pitt.cs.cs1631.g16.votingsoftware.inputprocessorcomponent.InputProcessorService;
import edu.pitt.cs.cs1631.g16.votingsoftware.sisservercommunication.SISServerCommunication;
import edu.pitt.cs.cs1631.g16.votingsoftware.tablecomponent.TableComponentService;
import tdr.sisprjremote.Util.KeyValueList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static final String TAG = "GUIComponent";

    private static final int SMS_PERMISSION_CODE = 0;

    //UI Elements
    private EditText ipAddr;
    private EditText port;
    private static TableLayout tl;
    private Button connect, start, vote, results, msg, script;
    private Button gui, input, table;
    private static TextView messageText;

    private String phoneNo = "+4432802210", candidateID = "1";
    private String code, list, num;

    private SISServerCommunication commn;
    private static String Scope = "SIS", Role = "Monitor", Msg = "";

    private Intent inputProcessor;
    private Intent tableComponent;

    private static Context context;

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

                    messageText.append(str + "***********\n");

                    KeyValueList parsedMsg = SISServerCommunication.parseMsg(str);
                    if (parsedMsg.getValue(SISServerCommunication.AckMsgId).equals("26")) {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        ipAddr = findViewById(R.id.ipInput);
        port = findViewById(R.id.portInput);

        connect = findViewById(R.id.connect);
        connect.setOnClickListener(this);

        gui = findViewById(R.id.gui);
        gui.setOnClickListener(this);

        input = findViewById(R.id.input);
        input.setOnClickListener(this);

        table = findViewById(R.id.table);
        table.setOnClickListener(this);

        tl = findViewById(R.id.resultsTable);

        start = findViewById(R.id.start);
        start.setOnClickListener(this);
        start.setEnabled(false);

        vote = findViewById(R.id.vote);
        vote.setOnClickListener(this);
        vote.setEnabled(false);

        results = findViewById(R.id.results);
        results.setOnClickListener(this);
        results.setEnabled(false);

        msg = findViewById(R.id.msg);
        msg.setOnClickListener(this);

        script = findViewById(R.id.script);
        script.setOnClickListener(this);

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

    public void onClick(View v) {

        final Context dbContext = this;

        if (v.getId() == R.id.connect) {

            String connectData = ipAddr.getText().toString() + ";" + port.getText().toString();

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

            // Once the GUI is connected we can start the voting process
            start.setEnabled(true);

        } else if (v.getId() == R.id.gui) {

            try {
                commn = new SISServerCommunication(ipAddr.getText().toString(), Integer.parseInt(port.getText().toString()), callbacks, Scope, Role, TAG);

                connect.setEnabled(false);

                commn.register();
                commn.connect();

            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }

            // Once the GUI is connected we can start the voting process
            start.setEnabled(true);

        } else if (v.getId() == R.id.input) {

            String connectData = ipAddr.getText().toString() + ";" + port.getText().toString();

            // Start the InputProcessor
            inputProcessor = new Intent(this, InputProcessorService.class);
            inputProcessor.setData(Uri.parse(connectData));
            startService(inputProcessor);

        } else if (v.getId() == R.id.table) {

            String connectData = ipAddr.getText().toString() + ";" + port.getText().toString();

            // Start the TableComponent
            tableComponent = new Intent(this, TableComponentService.class);
            tableComponent.setData(Uri.parse(connectData));
            startService(tableComponent);

        } else if (v.getId() == R.id.start) {

            // InputProcessor and TableComponent are now running, we can vote and get results
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
                    AlertDialog.Builder candidateInputBox = new AlertDialog.Builder(dbContext);
                    candidateInputBox.setTitle("Enter a list of candidates separated by ;");

                    // Set up the input
                    final EditText candidateInput = new EditText(dbContext);
                    candidateInput.setInputType(InputType.TYPE_CLASS_TEXT);
                    candidateInputBox.setView(candidateInput);

                    // Set up the buttons
                    candidateInputBox.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Hashtable<String, String> attr = new Hashtable<>();
                            list = candidateInput.getText().toString();
                            String Receiver = "TableComponent";
                            try {
                                attr.put(SISServerCommunication.MsgId, "703");
                                attr.put(SISServerCommunication.Passcode, code);
                                attr.put(SISServerCommunication.CandidateList, list);
                                String str = commn.sendMessage(TAG, Receiver, SISServerCommunication.MessageType.ALERT, Msg, attr);
                                messageText.append(str + "***********\n");
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

        } else if (v.getId() == R.id.vote) {

            /* Nested AlertBuilders are ugly but they are the only way to "halt" the UI thread and
             * force it to wait for a response from the Alerts before sending the vote message to the
             * SISserver
             */

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
                    // Builder to get candidate to vote for
                    AlertDialog.Builder builder = new AlertDialog.Builder(dbContext);
                    builder.setTitle("Enter candidate ID");

                    // Set up the input
                    final EditText candidateIDText = new EditText(dbContext);
                    candidateIDText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    builder.setView(candidateIDText);

                    // Set up the buttons
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                candidateID = candidateIDText.getText().toString();
                                Hashtable<String, String> attr = new Hashtable<>();
                                String Receiver = "TableComponent";

                                try {
                                    attr.put(SISServerCommunication.MsgId, "701");
                                    attr.put(SISServerCommunication.VoterPhoneNo, phoneNo);
                                    attr.put(SISServerCommunication.CandidateID, candidateID);
                                    String str = commn.sendMessage(TAG, Receiver, SISServerCommunication.MessageType.ALERT, Msg, attr);
                                    messageText.append(str + "***********\n");
                                } catch (Exception e) {
                                    Log.i(TAG, e.toString());
                                }
                            } catch (NumberFormatException e) {
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

        } else if(v.getId() == R.id.results) {

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
                    AlertDialog.Builder candidateInputBox = new AlertDialog.Builder(dbContext);
                    candidateInputBox.setTitle("Enter the number of candidates returned in the ranked report");

                    // Set up the input
                    final EditText candidateInput = new EditText(dbContext);
                    candidateInput.setInputType(InputType.TYPE_CLASS_TEXT);
                    candidateInputBox.setView(candidateInput);

                    // Set up the buttons
                    candidateInputBox.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Hashtable<String, String> attr = new Hashtable<>();
                            num = candidateInput.getText().toString();
                            String Receiver = "TableComponent";
                            try {
                                attr.put(SISServerCommunication.MsgId, "702");
                                attr.put(SISServerCommunication.Passcode, code);
                                attr.put(SISServerCommunication.N, num);
                                String str = commn.sendMessage(TAG, Receiver, SISServerCommunication.MessageType.ALERT, Msg, attr);
                                messageText.append(str + "***********\n");
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

        } else if(v.getId() == R.id.msg || v.getId() == R.id.script){
            int requestCode = 0;
            if (v.getId() == R.id.msg) {
                requestCode = 1;
            } else if (v.getId() == R.id.script) {
                requestCode = 2;
            }

            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, requestCode);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == 1 || requestCode == 2) && resultCode == Activity.RESULT_OK) {
            Uri uri;
            if (data != null) {
                uri = data.getData();
                Log.i(TAG, "Uri: " + uri.toString());

                if (requestCode == 1) parseXMLMsg(uri);
                else if (requestCode == 2) parseXMLMsgs(uri);
            }
        }
    }

    private void parseXMLMsg(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputStream);

            Element element = doc.getDocumentElement();
            element.normalize();

            NodeList nList = doc.getElementsByTagName("Head");
            Node node = nList.item(0);
            Element element2 = (Element) node;
            String msgId = getValue("MsgID", element2);
            String description = getValue(SISServerCommunication.Description, element2);

            Hashtable<String, String> attr = new Hashtable<>();
            attr.put(SISServerCommunication.MsgId, msgId);
            attr.put(SISServerCommunication.Description, description);

            nList = doc.getElementsByTagName("Item");

            for (int i = 0; i < nList.getLength(); i++) {
                node = nList.item(i);
                element2 = (Element) node;
                String key = getValue("Key", element2);
                String value = getValue("Value", element2);
                attr.put(key,value);
            }

            String receiver = "TableComponent";
            String str = commn.sendMessage(TAG, receiver, SISServerCommunication.MessageType.ALERT, Msg, attr);

            messageText.append(str + "***********\n");

        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private void parseXMLMsgs(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputStream);

            Element element = doc.getDocumentElement();
            element.normalize();

            NodeList nList = doc.getElementsByTagName("Msg");
            Log.d(TAG, "length = " + nList.getLength());

            for (int i = 0; i < nList.getLength(); i++) {
                Node node = nList.item(i);
                Element element2 = (Element) node;
                String msgId = getValue("MsgID", element2);
                String description = getValue(SISServerCommunication.Description, element2);

                Hashtable<String, String> attr = new Hashtable<>();
                attr.put(SISServerCommunication.MsgId, msgId);
                attr.put(SISServerCommunication.Description, description);

                NodeList nList2 = element2.getElementsByTagName("Item");

                for (int j = 0; j < nList2.getLength(); j++) {
                    node = nList2.item(j);
                    element2 = (Element) node;
                    String key = getValue("Key", element2);
                    String value = getValue("Value", element2);
                    attr.put(key, value);
                }

                String receiver = "TableComponent";
                String str = commn.sendMessage(TAG, receiver, SISServerCommunication.MessageType.ALERT, Msg, attr);

                messageText.append(str + "***********\n");
            }

        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private String getValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag).item(0).getChildNodes();
        Node node = nodeList.item(0);
        return node.getNodeValue();
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
