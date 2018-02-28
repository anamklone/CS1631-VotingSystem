package tdr.sisprjremote.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class KeyValueList {
    // delimiter for encoding the message
    static final String delim = "$$$";
    // regex pattern for decoding the message
    static final String pattern = "\\$+";
    // interal map for the message <property name, property value>, key and
    // value are both in String format
    private Map<String, String> map;

    /*
     * Constructor
     */
    public KeyValueList() {
        map = new HashMap<>();
    }

    /*
     * decode a message in String format into a corresponding KeyValueList
     */
    public static KeyValueList decodedKV(String message) {
        KeyValueList kvList = new KeyValueList();

        String[] parts = message.split(pattern);
        int validLen = parts.length;
        if (validLen % 2 != 0) {
            --validLen;
        }
        if (validLen < 1) {
            return kvList;
        }

        for (int i = 0; i < validLen; i += 2) {
            kvList.putPair(parts[i], parts[i + 1]);
        }
        return kvList;
    }

    /*
     * Add one property to the map
     */
    public boolean putPair(String key, String value) {
        key = key.trim();
        value = value.trim();
        if (key == null || key.length() == 0 || value == null
                || value.length() == 0) {
            return false;
        }
        map.put(key, value);
        return true;
    }

    // /*
    // * extract a List containing all the input message IDs in Integer format
    // * (specifically designed for message 20)
    // */
    // public List<Integer> InputMessages() {
    // int i = 1;
    // List<Integer> list = new ArrayList<>();
    // String m = map.get("InputMsgID" + i);
    // while (m != null) {
    // list.add(Integer.parseInt(m));
    // ++i;
    // m = map.get("InputMsgID" + i);
    // }
    // return list;
    // }
    //
    // /*
    // * extract a List containing all the output message IDs in Integer format
    // * (specifically designed for message 20)
    // */
    // public List<Integer> OutputMessages() {
    // int i = 1;
    // List<Integer> list = new ArrayList<>();
    // String m = map.get("OutputMsgID" + i);
    // while (m != null) {
    // list.add(Integer.parseInt(m));
    // ++i;
    // m = map.get("OutputMsgID" + i);
    // }
    // return list;
    // }

    public String removePair(String key) {
        return map.remove(key);
    }

    /*
     * encode the KeyValueList into a String
     */
    /*
     * encode the KeyValueList into a String
	 */
    public String encodedString() {

        StringBuilder builder = new StringBuilder();
        builder.append("(");
        for (Entry<String, String> entry : map.entrySet()) {
            builder.append(entry.getKey() + delim + entry.getValue() + delim);
        }
        // X$$$Y$$$, minimum
        builder.append(")");
        return builder.toString();
    }

    /*
     * get the property value based on property name
     */
    public String getValue(String key) {
        String value = map.get(key);
        if (value != null) {
            return value;
        } else {
            return "";
        }
    }

    /*
     * get the number of properties
     */
    public int size() {
        return map.size();
    }

    /*
     * toString for printing
     */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        for (Entry<String, String> entry : map.entrySet()) {
            builder.append(entry.getKey() + " : " + entry.getValue() + "\n");
        }
        return builder.toString();
    }
}
