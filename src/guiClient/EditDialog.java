package guiClient;

import java.util.Base64;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import common.Journal;

public class EditDialog {
    private Connection connection;

    // If id is empty (zero length), a new record will be created.
    public EditDialog(Gui gui, Connection connection, String id) {
        this.connection = connection;
        boolean editing = 0 != id.length();

        // - Start of being able to edit existing records:
        Journal journal;
        if (!editing) {
            journal = new Journal("", "", "", "", "");
        } else {
            journal = Journal.decode(connection.command("read " + id));
        }

        // Set up dialog.
        JTextField patient = new JTextField(journal.getPatient());
        JTextField nurse = new JTextField(journal.getNurse());
        JTextArea medicalData = new JTextArea(journal.getMedicalData());
        final JComponent[] inputs = new JComponent[] { new JLabel("Patient"), patient, new JLabel("Nurse"), nurse,
                new JLabel("Medical data"), medicalData };
        String[] options = { "Save", "Cancel" };
        String title = editing ? "Editing record" : "New record";
        int result = JOptionPane.showOptionDialog(gui, inputs, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, options, "Save");
        if (result == JOptionPane.YES_OPTION) {
            if (editing) { // editing an existing record
                connection.command("edit " + id + " " + encode(medicalData.getText()));
            } else { // creating a new record
                connection.command("create " + encode(patient.getText()) + " " + encode(nurse.getText()) + " "
                        + encode(medicalData.getText()));
            }
            gui.updateFromNetwork();
        }
    }

    private String encode(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes());
    }
}
