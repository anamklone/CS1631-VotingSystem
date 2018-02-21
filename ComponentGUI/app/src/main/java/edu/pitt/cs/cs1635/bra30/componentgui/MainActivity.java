package edu.pitt.cs.cs1635.bra30.componentgui;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static final int CONNECTED = 1;
    public static final int DISCONNECTED = 2;
    public static final int MESSAGE_RECEIVED = 3;
    public static final String TAG = "GUI";

    //UI Elements
    private Button connect;
    private EditText ipAddr;
    private EditText port;
    private static TextView messageText;

    //Socket Elements
    static ComponentSocket client;
    static Handler callbacks = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            String str;
            String[] strs;
            switch (msg.what) {
                case MESSAGE_RECEIVED:
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
        ipAddr = findViewById(R.id.ipInput);
        port = findViewById(R.id.portInput);
        messageText = findViewById(R.id.text);
    }

    public void onClick(View v){
        client = new ComponentSocket(ipAddr.getText().toString(), Integer.parseInt(port.getText().toString()), callbacks);
        client.start();
        connect.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(client!=null){
            client.killThread();
        }
    }
}
