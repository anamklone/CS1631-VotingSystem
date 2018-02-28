package tdr.sisprjremote.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**************************************
 * Class MsgDecoder:
 * Get String from input Stream and reconstruct it to
 * a Key Value List.
 ***************************************/

public class MsgDecoder {
    // used for reading Strings
    private BufferedReader reader;

    /*
     * Constructor
     */
    public MsgDecoder(InputStream in) throws IOException {
        reader = new BufferedReader(new InputStreamReader(in));
    }

    /*
     * read and decode the message into KeyValueList
     */
    public KeyValueList getMsg() throws Exception {
        KeyValueList kvList = new KeyValueList();
        StringBuilder builder = new StringBuilder();

        String message = reader.readLine();

        if (message != null && message.length() > 2) {

            builder.append(message);

            while (message != null && !message.endsWith(")")) {
                message = reader.readLine();
                builder.append("\n" + message);
            }

            kvList = KeyValueList
                    .decodedKV(builder.substring(1, builder.length() - 1));
        }
        return kvList;
    }
}
