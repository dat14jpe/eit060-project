package server.commands;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;

import common.Individual;
import common.Journal;
import common.Journals;

public class Search {
    private String name;

    // Search command: lists all records associated with a given name
    // (or rather, those records also viewable by the current user).
    public Search(String input) throws Exception {
        // Format: "search location name";
        // location is either all or division,
        // and name is... self-evident.
        String[] params = input.split(" ");
        if (params.length < 3) {
            throw new Exception("Too few parameters.");
        }

        String slocation = params[1];
        // - to do: use location to restrict government searches as desired
        name = new String(Base64.getDecoder().decode(params[2].getBytes()), "UTF-8");
    }

    public boolean hasPermission(Individual i, Journals journals) {
        // No need for patients to search (they can only view their own records,
        // which are retrieved by the list command).
        return Individual.PATIENT != i.getType();
    }

    public String execute(Individual i, Journals journals) {
        // This command is special: what it returns depends on user details
        // when the command is executed.
        ArrayList<String> ids = journals.get(Individual.onlyName(name));
        
        // Remove IDs this individual isn't allowed to see.
        Iterator<String> iter = ids.iterator();
        while (iter.hasNext()) {
            String id = iter.next();
            Journal journal = journals.get(id);
            if (null == journal) { // no need to include nothings
                iter.remove();
                continue;
            }
            // Code reuse: create a read command and see what it says.
            Read read;
            try {
                read = new Read("read " + id);
                if (!read.hasPermission(i, journals)) {
                    iter.remove();
                }
            } catch (Exception e) {
                e.printStackTrace(); // - should we let this propagate?
            }
        }
        
        // - to do: generate list, and return it

        // - to do
        return i.encode();
    }
}
