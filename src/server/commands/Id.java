package server.commands;

import common.Individual;
import common.Journals;

// Return basic user information, as parsed by the server.

public class Id extends Command {
    public Id(String input) {

    }

    public boolean hasPermission(Individual i, Journals journals) {
        return true;
    }

    public String execute(Individual i, Journals journals) {
        return i.encode();
    }
}
