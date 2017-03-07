package server.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import common.Individual;
import common.Journal;
import common.Journals;
import server.AuditLog;

// Command for listing all journals associated with an individual
// (patient, nurse, or doctor) or for all records associated with a 
// hospital division (only available to doctors).
// Does not show medical data (use Read for this).

public class List extends Command {
    private boolean listDivision;
    private String division;

    public List(String input) {
        // - Possibly to do: extend this to let government agencies specify what
        // to list.
        // Parameter is already division name; use this for government agencies.
        String[] params = input.split(" ");
        listDivision = params.length >= 2 && 0 != params[1].length();
        if (listDivision)
            division = params[1];
    }

    public boolean hasPermission(Individual i, Journals journals) {
        // Government agencies can read all records.
        if (Individual.GOVERNMENT == i.getType())
            return true;

        // Anyone can read records associated with themselves.
        if (!listDivision)
            return true;

        // Doctors and nurses can read records associated with their division.
        return (Individual.NURSE == i.getType() || Individual.DOCTOR == i.getType()) && i.getUnit().equals(division);
    }

    public String execute(Individual i, Journals journals) {
        ArrayList<String> ids;
        if (listDivision) {
            ids = journals.getByDivision(division);
            AuditLog.log(i, "listed division " + division);
            return encodeList(ids, journals);
        } else {
            // Special behaviour: if government, we list divisions.
            if (Individual.GOVERNMENT == i.getType()) {
                ids = journals.getDivisions();
                AuditLog.log(i, "listed divisions");
            } else { // normal individual listing
                ids = journals.get(i);
                AuditLog.log(i, "listed individual");
                return encodeList(ids, journals);
            }
        }
        StringBuilder sb = new StringBuilder();
        if (null != ids) {
            String prefix = "";
            for (String id : ids) {
                sb.append(prefix);
                sb.append(id);
                prefix = ";";
            }
        }
        return sb.toString();
    }

    private static String separator = ";";

    // Utility method for encoding a list of journals (given as a list of IDs).
    // Also includes patient, nurse, doctor and division (for listing).
    public static String encodeList(ArrayList<String> ids, Journals journals) {
        if (null == ids) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String prefix = "";
        for (String id : ids) {
            sb.append(prefix);
            sb.append(id);
            sb.append(',');
            journals.get(id).encode(sb, false);
            prefix = separator;
        }
        return sb.toString();
    }

    public static Map<String, Journal> decodeList(String in) {
        Map<String, Journal> list = new HashMap<String, Journal>();
        String[] items = in.split(separator);
        for (String item : items) {
            String id = item.substring(0, item.indexOf(',')), properties = item.substring(item.indexOf(',') + 1);
            list.put(id, Journal.decode(properties));
        }
        return list;
    }
}
