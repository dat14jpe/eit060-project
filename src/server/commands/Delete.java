package server.commands;

import common.Individual;
import common.Journals;
import server.AuditLog;

public class Delete extends Command {
    private String id;

    public Delete(String input) throws Exception {
        // Format: "delete id", where "id" is the record's unique ID.
        String[] params = input.split(" ");
        if (params.length < 2) {
            throw new Exception("Too few parameters.");
        }
        id = params[1];
    }

    public boolean hasPermission(Individual i, Journals journals) {
        // Government agencies are allowed to delete any records.
        return Individual.GOVERNMENT == i.getType();
    }

    public String execute(Individual i, Journals journals) {
        AuditLog.log(i, "deleted record " + id);
        journals.remove(id);
        return ""; // - possibly return true/false on whether a journal was actually deleted?
    }
}
