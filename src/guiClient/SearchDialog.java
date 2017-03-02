package guiClient;

import java.util.Base64;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class SearchDialog {
    public SearchDialog(Gui gui) {
        JTextField search = new JTextField("");
        final JComponent[] inputs = new JComponent[] { new JLabel("Name"), search };
        String[] options = { "Search", "Cancel" };

        int result = JOptionPane.showOptionDialog(gui, inputs, "Name search", JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, options, "Save");
        if (result == JOptionPane.YES_OPTION) {
            String name = search.getText();
            gui.createTab("search " + Base64.getEncoder().encodeToString(name.getBytes()), "S: " + name, true);
            gui.updateFromNetwork();
        }
    }
}
