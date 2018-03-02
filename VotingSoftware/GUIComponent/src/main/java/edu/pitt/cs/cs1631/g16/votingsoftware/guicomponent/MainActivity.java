package edu.pitt.cs.cs1631.g16.votingsoftware.guicomponent;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import edu.pitt.cs.cs1631.g16.votingsoftware.inputprocessorcomponent.InputProcessorService;
import edu.pitt.cs.cs1631.g16.votingsoftware.sisservercommunication.SISServerCommunication;
import edu.pitt.cs.cs1631.g16.votingsoftware.sisservercommunication.SISServerCommunication.MessageType;
import edu.pitt.cs.cs1631.g16.votingsoftware.tablecomponent.TableComponentService;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static final String TAG = "GUIComponent";

    private static final int SMS_PERMISSION_CODE = 0;

    //UI Elements
    private Button connect, alert;
    private EditText ipAddr;
    private EditText port;
    private static TextView messageText;

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
                    messageText.setText(str);
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

        connect = findViewById(R.id.connectBtn);
        connect.setOnClickListener(this);
        alert = findViewById(R.id.alertBtn);
        alert.setOnClickListener(this);
        alert.setEnabled(false);
        ipAddr = findViewById(R.id.ipInput);
        port = findViewById(R.id.portInput);
        messageText = findViewById(R.id.text);

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
        if(v.getId() == R.id.connectBtn) {
            try {
                commn = new SISServerCommunication(ipAddr.getText().toString(), Integer.parseInt(port.getText().toString()), callbacks, Scope, Role, TAG);

                connect.setEnabled(false);

                commn.register();
                commn.connect();

                alert.setEnabled(true);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }

            String connectData = ipAddr.getText().toString() + ";" + port.getText().toString();

            // Start the InputProcessor
            inputProcessor = new Intent(getBaseContext(), InputProcessorService.class);
            inputProcessor.setData(Uri.parse(connectData));
            startService(inputProcessor);

            // Start the TableComponent
            tableComponent = new Intent(getBaseContext(), TableComponentService.class);
            tableComponent.setData(Uri.parse(connectData));
            startService(tableComponent);

        }else if(v.getId() == R.id.alertBtn){
            try {
                commn.sendMessage(TAG, Receiver, MessageType.ALERT, "Click Alert", null);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
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
