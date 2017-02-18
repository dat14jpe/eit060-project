package server.commands;

import common.Individual;
import common.Journals;

abstract public class Command {
    // Check if the given individual has permission to 
    // perform this action on the given journals database.
    public abstract boolean hasPermission(Individual i, Journals journals);

    // Execute this command as the given individual on the
    // given journals database.
    public abstract String execute(Individual i, Journals journals);
}
