package guiClient;

import java.util.Base64;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import common.Journal;
import server.commands.Edit;

public class EditDialog {
    private Connection connection;

    // If id is empty (zero length), a new record will be created.
    // If this user doesn't have permission to edit the record, we're
    // only viewing.
    public EditDialog(Gui gui, Connection connection, String id) {
        this.connection = connection;
        boolean editing = 0 != id.length();
        boolean viewing = false;

        // - Start of being able to edit existing records:
        Journal journal;
        if (!editing) {
            journal = new Journal("", "", gui.individual.getName(), gui.individual.getUnit(), "");
        } else {
            journal = Journal.decode(connection.command("read " + id));

            // - New: check permission to determine whether we're only viewing.
            gui.journals.add(journal, id);
            Edit edit;
            try {
                edit = new Edit("edit " + id + " invalid");
                viewing = !edit.hasPermission(gui.individual, gui.journals);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Set up dialog.
        JTextField patient = new JTextField(journal.getPatient());
        JTextField nurse = new JTextField(journal.getNurse());
        JTextField doctor = new JTextField(journal.getDoctor());
        JTextField division = new JTextField(journal.getDivision());
        JTextArea medicalData = new JTextArea(journal.getMedicalData());
        final JComponent[] inputs = new JComponent[] { new JLabel("Patient"), patient, new JLabel("Nurse"), nurse,
                new JLabel("Doctor"), doctor, new JLabel("Division"), division, new JLabel("Medical data"),
                medicalData };
        String[] options = { "Save", "Cancel" };
        String title = editing ? "Editing record" : "New record";

        doctor.setEditable(false);
        division.setEditable(false);
        if (viewing) {
            title = "Viewing record";
            medicalData.setEditable(false);
            options = new String[] { "Close" };
        }
        if (editing) {
            title += " " + id;
            patient.setEditable(false);
            nurse.setEditable(false);
        }

        int result = JOptionPane.showOptionDialog(gui, inputs, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, options, "Save");
        if (viewing)
            return;
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
