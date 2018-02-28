package edu.pitt.cs.cs1631.g16.votingsoftware.tablecomponent;

import java.util.HashSet;

public class VoterTable {

    private HashSet<String> table;

    public VoterTable() {
        table = new HashSet();
    }

    public int recordVoter(String voterPhoneNo) {
        if (table.contains(voterPhoneNo)) {
            return 1;
        }
        table.add(voterPhoneNo);
        return 0;
    }

}
