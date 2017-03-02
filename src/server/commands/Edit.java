package server.commands;

import java.util.Base64;

import common.Individual;
import common.Journal;
import common.Journals;
import server.AuditLog;

// Used for editing journal data; the edit will only be allowed
// if the journal contents are the same as they were before the
// edit, to avoid unintentional data corruption.
// (maybe just send old timestamp instead of data?)

public class Edit extends Command {
    private String id, medicalData;

    public Edit(String input) throws Exception {
        // Format: "edit id old new", where "old" is used to guard against
        // corruption.
        // - For now, we're not using this "corruption guard". Change that.
        // New is the new medical data (should other fields be editable?
        // Unsure).
        String[] params = input.split(" ");
        if (params.length < 3) {
            throw new Exception("Too few parameters.");
        }

        id = params[1];
        medicalData = new String(Base64.getDecoder().decode(params[2].getBytes()), "UTF-8");
    }

    public boolean hasPermission(Individual i, Journals journals) {
        Journal journal = journals.get(id);
        if (null == journal) {
            return false; // - technically, shouldn't this be an outright error?
        }

        switch (i.getType()) {
        case Individual.NURSE: {
            // A nurse may write to all records associated with him/her.
            return journal.getNurse().equals(i.getName());
        }
        case Individual.DOCTOR: {
            // A nurse may write to all records associated with him/her.
            return journal.getDoctor().equals(i.getName());
        }
        default:
            return false;
        }
    }

    public String execute(Individual i, Journals journals) {
        AuditLog.log(i, "edited record " + id);
        journals.get(id).setMedicalData(medicalData);
        return "Edited"; // - to do: return result (can this fail?)
    }
}
