package server.commands;

import common.Individual;
import common.Journals;

public class Invalid extends Command {
    public Invalid(String input) {

    }

    public boolean hasPermission(Individual i, Journals journals) {
        return true;
    }

    public String execute(Individual i, Journals journals) {
        return "Invalid command";
    }
}
