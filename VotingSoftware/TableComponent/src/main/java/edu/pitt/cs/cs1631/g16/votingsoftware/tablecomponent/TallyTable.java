package edu.pitt.cs.cs1631.g16.votingsoftware.tablecomponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;

public class TallyTable {

    private Hashtable<String, Integer> table;

    public TallyTable() {
        table = new Hashtable();
    }

    public TallyTable(String candidateList) {
        String candidateIDs[] = candidateList.split(";");
        table = new Hashtable(candidateIDs.length);
        for (int i = 0; i < candidateIDs.length; i++) {
            table.put(candidateIDs[i], 0);
        }
    }

    public int recordVote(String candidateID) {
        if (table.containsKey(candidateID)) {
            table.put(candidateID, table.get(candidateID) + 1);
            return 3;
        }
        return 2;
    }

    public String rankedCandidates(int numWinners) {
        ArrayList<Map.Entry<String, Integer>> sortedTable = new ArrayList(table.entrySet());
        Collections.sort(sortedTable, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> c1, Map.Entry<String, Integer> c2) {
                return c1.getValue().compareTo(c2.getValue());
            }
        });

        StringBuilder rankedReport = new StringBuilder();
        for (int i = 0; i < numWinners; i++) {
            rankedReport.append(sortedTable.get(i).getKey()).append(",")
                        .append(sortedTable.get(i).getValue()).append(";");
        }

        return rankedReport.toString();
    }

}
