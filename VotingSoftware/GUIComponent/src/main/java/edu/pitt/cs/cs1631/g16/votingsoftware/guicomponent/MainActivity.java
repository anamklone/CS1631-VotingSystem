package edu.pitt.cs.cs1631.g16.votingsoftware.guicomponent;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import edu.pitt.cs.cs1631.g16.votingsoftware.sisservercommunication.SISServerCommunication;
import edu.pitt.cs.cs1631.g16.votingsoftware.sisservercommunication.SISServerCommunication.MessageType;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static final String TAG = "GUIComponent";

    //UI Elements
    private Button connect, alert;
    private EditText ipAddr;
    private EditText port;
    private static TextView messageText;

    private SISServerCommunication commn;
    private static String Scope = "SIS", Role = "Monitor", Receiver = "PrjRemote", Msg = "Hello!";

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
        }else if(v.getId() == R.id.alertBtn){
            try {
                commn.sendMessage(Receiver, MessageType.ALERT, "Click Alert", null);
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
    }
}
