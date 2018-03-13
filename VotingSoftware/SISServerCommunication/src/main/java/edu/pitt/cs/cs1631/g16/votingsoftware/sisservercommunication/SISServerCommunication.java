package edu.pitt.cs.cs1631.g16.votingsoftware.sisservercommunication;

import android.os.Handler;

import java.util.Hashtable;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import tdr.sisprjremote.ComponentSocket;
import tdr.sisprjremote.Util.KeyValueList;
import tdr.sisprjremote.Util.NetTool;

public class SISServerCommunication {

    public static String TAG = "SISServerCommunication";

    static ComponentSocket client;

    public static final int CONNECTED = 1;
    public static final int DISCONNECTED = 2;
    public static final int MESSAGE_RECEIVED = 3;

    private static final int PORT = 8000;

    private static String SCOPE;
    private static String ROLE;

    public SISServerCommunication(Handler callbacks, String scope, String role) throws Exception {
        if (client != null) {
            client.killThread();
        }
        client = new ComponentSocket(NetTool.getPublicAddress(), PORT, callbacks);
        client.start();

        if (scope == null || scope.isEmpty()) {
            throw new Exception("Invalid scope");
        }
        this.SCOPE = scope;

        if (role == null || role.isEmpty()) {
            throw new Exception("Invalid role");
        }
        this.ROLE = role;
    }

    public void register() throws Exception {
        sendMessage(SISServer, MessageType.REGISTER, "Register", null);
    }

    public void connect() throws Exception {
        sendMessage(SISServer, MessageType.CONNECT, "Connect", null);
    }

    public static void ack(String ackMsgID, String yesNo, String name) throws Exception {
        Hashtable<String, String> attr = new Hashtable<>();
        attr.put("AckMsgID", ackMsgID);
        attr.put("YesNo", yesNo);
        attr.put("Name", name);
        sendMessage(SISServerCommunication.VotingSoftware, MessageType.ALERT, "Acknowledgement", attr);
    }

    public void disconnect() {
        if (client != null) {
            client.killThread();
        }
    }

    public static void sendMessage(String receiver, String messageType, String message,
                            Hashtable<String, String> attributes) throws Exception {

        if (client == null || (!client.isSocketAlive()
                && messageType != MessageType.REGISTER && messageType != MessageType.CONNECT)) {
            throw new Exception("Not connected to the server");
        }

        KeyValueList messageInfo = new KeyValueList();
        messageInfo.putPair("Scope", SCOPE);
        messageInfo.putPair("Role", ROLE);
        messageInfo.putPair("Sender", VotingSoftware);

        if (receiver != null && !receiver.isEmpty()) {
            messageInfo.putPair("Receiver", receiver);
        }

        if (messageType == null || messageType.isEmpty()) {
            throw new Exception("Invalid messageType");
        }
        messageInfo.putPair("MessageType", messageType);

        if (message != null && !message.isEmpty()) {
            messageInfo.putPair("Message", message);
        }

        if (attributes != null && attributes.size() != 0) {
            for (Map.Entry<String, String> attr : attributes.entrySet()) {
                messageInfo.putPair(attr.getKey(), attr.getValue());
            }
        }

        sendMessage(messageInfo);
    }

    private static void sendMessage(final KeyValueList messageInfo) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                client.setMessage(messageInfo);
            }
        }, 100);
    }

    public void ack() throws Exception {
        if (client == null || !client.isSocketAlive()) {
            throw new Exception("Not connected to the server");
        }
        client.ack();
    }

    public static KeyValueList parseMsg(String msg) {
        KeyValueList kvList = new KeyValueList();

        String[] lines = msg.split("\\r?\\n");

        for (int i = 0; i < lines.length; i++) {
            String[] split = lines[i].split(" : ");
            for (int j = 1; j < split.length; j += 2) {
                kvList.putPair(split[j - 1], split[j]);
            }
        }

        return kvList;
    }

    public static final String SISServer = "SISServer";
    public static final String VotingSoftware = "VotingSoftware";
    public static final String MsgId = "MsgId";
    public static final String AckMsgId = "AckMsgId";
    public static final String Passcode = "Passcode", Passcode_Val = "*****";
    public static final String CandidateList = "CandidateList";
    public static final String VoterPhoneNo = "VoterPhoneNo";
    public static final String CandidateID = "CandidateID";
    public static final String N = "N";
    public static final String Status = "Status";
    public static final String RankedReport = "RankedReport";
    public static final String YesNo = "YesNo";

    public class Role {
        public static final String BASIC = "Basic";
        public static final String CONTROLLER = "Controller";
        public static final String MONITOR = "Monitor";
        public static final String ADVERTISER = "Advertiser";
        public static final String DEBUGGER = "Debugger";
    }

    public class MessageType {
        public static final String READING = "Reading";
        public static final String ALERT = "Alert";
        public static final String SETTING = "Setting";
        public static final String REGISTER = "Register";
        public static final String CONFIRM = "Confirm";
        public static final String CONNECT = "Connect";
        public static final String EMERGENCY = "Emergency";
    }

}
