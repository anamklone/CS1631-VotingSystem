package tdr.sisprjremote.Util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**************************************************
 * Class MsgEncoder:
 * Serialize the KeyValue List and Send it out to a Stream.
 ***************************************************/
public class MsgEncoder {

    // used for writing Strings
    private PrintStream writer;

    /*
     * Constructor
     */
    public MsgEncoder(OutputStream out) throws IOException {
        writer = new PrintStream(out);
    }

    /*
     * encode the KeyValueList that represents a message into a String and send
     */
    public void sendMsg(KeyValueList kvList) throws IOException {
        if (kvList == null || kvList.size() < 1) {
            return;
        }

        writer.println(kvList.encodedString());
        writer.flush();
    }
}
