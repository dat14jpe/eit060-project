package guiClient;

import common.Journals;

public class GuiClient {
    static public void main(String[] argss) {
        Journals journals = new Journals();
        new Gui(journals);
    }
}
