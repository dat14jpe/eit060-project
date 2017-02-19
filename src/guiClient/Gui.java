package guiClient;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;

import common.Individual;
import common.Journal;

public class Gui extends JPanel {
    private Individual individual;
    private Connection connection;
    private JTabbedPane tpane;
    private ArrayList<Tab> tabs;

    class Tab {
        String division; // empty division string means individual (no division)
        JTable table;
        final boolean divisions; // true if this is the special government-only
                                 // "list divisions" tab

        Tab(String division, JTable table, boolean divisions) {
            this.division = division;
            this.table = table;
            this.divisions = divisions;
        }
    }

    private void initConnection() {
        // Let user choose keystore and enter passwords (for both truststore and
        // keystore).
        // - Should truststore also be... selectable? Might be unnecessary.
        final String certPath = "certificates/";
        final String keystorePath = "certificates/keystores/";

        // List available keystores.
        File[] fileList = new File(keystorePath).listFiles();
        String[] choices = new String[fileList.length];
        for (int i = 0; i < fileList.length; ++i) {
            choices[i] = fileList[i].toString().substring(keystorePath.length());
        }

        // Set up dialog.
        final String defaultPass = "password";
        JPasswordField trustPass = new JPasswordField(defaultPass), keyPass = new JPasswordField(defaultPass);
        JTextField hostField = new JTextField("localhost:9876");
        final JComponent[] inputs = new JComponent[] { new JLabel("Host"), hostField, new JLabel("Truststore password"),
                trustPass, new JLabel("Keystore password"), keyPass, new JLabel("Keystore") };
        // String[] choices = { "clientkeystore", "B", "C" };
        String keystore = (String) JOptionPane.showInputDialog(this, inputs, "Choose keys and enter password",
                JOptionPane.PLAIN_MESSAGE, null, choices, choices[0]);
        if (null == keystore) { // user pressed cancel
            System.exit(0);
        } else {
            // Initialize connection.
            char[] tpass = trustPass.getPassword();
            char[] kpass = keyPass.getPassword();
            String hostName = hostField.getText();
            String truststore = certPath + "clienttruststore";
            keystore = keystorePath + keystore;
            String[] hostValues = hostName.split(":");
            String host = hostValues[0];
            int port = Integer.parseInt(hostValues[1]);
            connection = new Connection(host, port, truststore, keystore, tpass, kpass);
            // - to do: make sure to quit (maybe after showing a nice error
            // message) if connection failed
        }
    }

    private void createDivisionTab(String division) {
        boolean divisions = 0 == division.length() && Individual.GOVERNMENT == individual.getType();
        String[] columns = { "Record ID", "Patient", "Nurse", "Doctor", "Division" };
        String[] divColumns = { "Name" };
        Object[][] data = {};
        JTable table = new JTable(new DefaultTableModel(data, divisions ? divColumns : columns)) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setPreferredScrollableViewportSize(new Dimension(640, 480));
        table.setFillsViewportHeight(true);
        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (2 == e.getClickCount()) { // edit on double click
                    editSelectedRecord();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        String tabName = "D: " + division;
        if (0 == division.length()) {
            tabName = divisions ? "Divisions" : "Individual";
        }
        tpane.addTab(tabName, scrollPane);

        tabs.add(new Tab(division, table, divisions));
    }

    public Gui() {
        super(new BorderLayout());
        initConnection();
        individual = Individual.decode(connection.command("id"));

        // Set up content pane (this).
        tabs = new ArrayList<Tab>();
        tpane = new JTabbedPane();
        tpane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateFromNetwork();
            }
        });
        createDivisionTab("");
        if (!tabs.get(0).divisions && Individual.PATIENT != individual.getType()) {
            createDivisionTab(individual.getUnit());
        }
        add(tpane);

        // Set up button row.
        // - To do: make sure buttons are only visible/active when they can actually be used.
        JPanel buttonPanel = new JPanel();
        Gui gui = this;
        JButton refreshButton = new JButton("Refresh table");
        buttonPanel.add(refreshButton);
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateFromNetwork();
            }
        });

        if (Individual.PATIENT != individual.getType()) {
            JButton editButton = new JButton("Edit record");
            buttonPanel.add(editButton);
            editButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    editSelectedRecord();
                }
            });
        }

        if (Individual.DOCTOR == individual.getType()) {
            JButton createButton = new JButton("Create record");
            buttonPanel.add(createButton);
            createButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    new EditDialog(gui, connection, "");
                }
            });
        }

        if (Individual.GOVERNMENT == individual.getType()) {
            JButton deleteButton = new JButton("Delete record");
            buttonPanel.add(deleteButton);
            deleteButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    deleteSelectedRecord();
                }
            });
        }
        
        add(buttonPanel, BorderLayout.SOUTH);

        // Set up general info (to the left).
        JPanel infoPanel = new IdPanel(individual);
        add(infoPanel, BorderLayout.WEST);

        // Set up general window.
        JFrame frame = new JFrame("GUI client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.setOpaque(true);
        frame.setContentPane(this);

        frame.pack();
        frame.setVisible(true);

        updateFromNetwork();
    }

    // Update table contents via network.
    void updateFromNetwork() {
        int tindex = tpane.getSelectedIndex();
        if (tindex < 0 || tindex > tabs.size() - 1) {
            return;
        }
        Tab tab = tabs.get(tindex);
        JTable table = tab.table;
        DefaultTableModel tmodel = (DefaultTableModel) table.getModel();
        tmodel.setRowCount(0);
        String list = connection.command("list " + tab.division);
        String[] ids = list.split(";");

        if (tab.divisions) { // list divisions
            // - Maybe overkill to blindly update all tabs here; look into it.
            int count = tpane.getTabCount();
            for (int i = 1; i < count; ++i) {
                tpane.remove(1);
                tabs.remove(1);
            }

            for (String name : ids) {
                if (0 == name.length()) {
                    continue;
                }
                // - to do: probably decode from base64
                String[] row = { name };
                tmodel.addRow(row);
                createDivisionTab(name);
            }
        } else { // list records
            for (String id : ids) {
                if (id.trim().length() == 0)
                    continue;
                Journal journal = Journal.decode(connection.command("read " + id));
                String[] row = { id, journal.getPatient(), journal.getNurse(), journal.getDoctor(),
                        journal.getDivision() };
                tmodel.addRow(row);
            }
        }
    }

    // Launch edit dialog for selected (in table) record.
    void editSelectedRecord() {
        Tab tab = tabs.get(tpane.getSelectedIndex());
        JTable table = tab.table;
        int row = table.getSelectedRow();
        if (-1 == row) {
            JOptionPane.showMessageDialog(this, "You need to select a record in order to edit it.",
                    "No record selected", JOptionPane.INFORMATION_MESSAGE);
            return; // no row is selected
        }
        String id = (String) table.getValueAt(row, 0);
        new EditDialog(this, connection, id);
    }

    // Deletes the selected record.
    void deleteSelectedRecord() {
        Tab tab = tabs.get(tpane.getSelectedIndex());
        JTable table = tab.table;
        int row = table.getSelectedRow();
        if (-1 == row) {
            JOptionPane.showMessageDialog(this, "You need to select a record in order to delete it.",
                    "No record selected", JOptionPane.INFORMATION_MESSAGE);
            return; // no row is selected
        }
        String id = (String) table.getValueAt(row, 0);

        // Confirm.
        if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(this, "Are you sure you want to delete record " + id + "?", "Warning", JOptionPane.YES_NO_OPTION)) {
            return;
        }
        
        // Delete.
        connection.command("delete " + id);
        updateFromNetwork();
    }
}
