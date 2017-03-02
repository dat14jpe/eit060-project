package server;

import server.commands.*;

public class CommandParser {
    public CommandParser() {

    }

    Command parse(String input) {
        int spacePos = input.indexOf(' ');
        String name = spacePos == -1 ? input : input.substring(0, spacePos);
        Command cmd;
        try {
            switch (name) {
            case "id":
                cmd = new Id(input);
                break;
            case "create":
                cmd = new Create(input);
                break;
            case "delete":
                cmd = new Delete(input);
                break;
            case "edit":
                cmd = new Edit(input);
                break;
            case "list":
                cmd = new List(input);
                break;
            case "read":
                cmd = new Read(input);
                break;
            case "search":
                cmd = new Search(input);
                break;
            default:
                cmd = new Invalid(input);
            }
        } catch (Exception e) { // - probably to do: only catch "formatting" exceptions
            cmd = new Invalid(input);
        }
        return cmd;
    }
}
