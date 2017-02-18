package server.commands;

import common.Individual;
import common.Journal;
import common.Journals;
import server.AuditLog;

// Show a specific medical record.

public class Read extends Command {
    private String id; // record ID

    public Read(String input) throws Exception {
        // Format: "read id", where "id" is the record's unique ID.
        String[] params = input.split(" ");
        if (params.length < 2) {
            throw new Exception("Too few parameters.");
        }
        id = params[1];
    }

    public boolean hasPermission(Individual i, Journals journals) {
        Journal journal = journals.get(id);
        if (null == journal) {
            return false;
        }
        switch (i.getType()) {
        case Individual.PATIENT: {
            // A patient is allowed to read his/her own list of records.
            return journal.getPatient().equals(i.getName());
        }
        case Individual.NURSE:
        case Individual.DOCTOR: {
            // A nurse may read all records associated with him/her,
            // and also all records associated with their division.
            return journal.getNurse().equals(i.getName()) || journal.getDivision().equals(i.getUnit());
        }
        case Individual.GOVERNMENT: {
            // A government agency is allowed to read all records.
            return true;
        }
        }

        return false;
    }

    public String execute(Individual i, Journals journals) {
        Journal journal = journals.get(id);
        if (null == journal) {
            return ""; // - should this even be possible?
        }
        AuditLog.log(i, "read record " + id);
        return journal.encode();
    }
}
