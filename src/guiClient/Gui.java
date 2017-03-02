package guiClient;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import javax.swing.BoxLayout;
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
import common.Journals;
import server.commands.List;

public class Gui extends JPanel {
    Individual individual;
    private Connection connection;
    private JTabbedPane tpane;
    private ArrayList<Tab> tabs;

    // Local partial cache of journals database.
    Journals journals;

    class Tab {
        String command; // command used to list this tab's contents
        String name;
        JTable table;
        final boolean divisions; // true if this is the special government-only
                                 // "list divisions" tab

        Tab(String command, String name, JTable table, boolean divisions) {
            this.command = command;
            this.name = name;
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
        Arrays.sort(choices);

        // Set up dialog.
        final String defaultPass = "password";
        JPasswordField trustPass = new JPasswordField(defaultPass), keyPass = new JPasswordField(defaultPass);
        JTextField hostField = new JTextField("localhost:9876");
        final JComponent[] inputs = new JComponent[] { new JLabel("Host"), hostField, new JLabel("Truststore password"),
                trustPass, new JLabel("Keystore password"), keyPass, new JLabel("Keystore") };
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

    void createTab(String command, String name, boolean closeable) {
        // boolean divisions = 0 == division.length() && Individual.GOVERNMENT
        // == individual.getType();
        // - Ugly solution; make it better.
        boolean divisions = name.equals("Divisions");// name.indexOf(':') != 1;
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
        tpane.addTab(name, scrollPane);
        if (closeable) {
            tpane.setTabComponentAt(tpane.getTabCount() - 1, createTabHead(name));
        }

        tabs.add(new Tab(command, name, table, divisions));
    }
    
    // - Table head experiment (to allow the closing of tabs).
    private JPanel createTabHead(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.setOpaque(false);
        JButton close = new JButton("x");
        JLabel label = new JLabel(title + "  ");
        close.setBorderPainted(false);
        close.setOpaque(false);
        
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tpane.removeTabAt(tpane.indexOfTab(title));
            }
        });
        
        panel.add(label);
        panel.add(close);
        return panel;
    }

    public Gui(Journals journals) {
        super(new BorderLayout());
        this.journals = journals;
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

        createTab("list", Individual.GOVERNMENT == individual.getType() ? "Divisions" : "Individual", false);
        if (!tabs.get(0).divisions && Individual.PATIENT != individual.getType()) {
            createTab("list " + individual.getUnit(), "D: " + individual.getUnit(), false);
        }
        add(tpane);

        // Set up button row.
        // - To do: make sure buttons are only visible/active when they can
        // actually be used.
        JPanel buttonPanel = new JPanel();
        Gui gui = this;
        JButton refreshButton = new JButton("Refresh");
        buttonPanel.add(refreshButton);
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateFromNetwork();
            }
        });

        if (Individual.PATIENT != individual.getType() && Individual.GOVERNMENT != individual.getType()) {
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
                    // - to do: don't create edit dialog for list of divisions
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

        if (Individual.PATIENT != individual.getType()) {
            JButton searchButton = new JButton("Search");
            buttonPanel.add(searchButton);
            searchButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    new SearchDialog(gui);
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
        String list = connection.command(tab.command);

        if (tab.divisions) { // list divisions
            String[] names = list.split(";");
            
            // - Maybe overkill to blindly update all tabs here; look into it.
            int count = tpane.getTabCount();
            for (int i = 1; i < count; ++i) {
                tpane.remove(1);
                tabs.remove(1);
            }

            for (String name : names) {
                if (0 == name.length()) {
                    continue;
                }
                // - to do: probably decode from base64
                String[] row = { name };
                tmodel.addRow(row);
                createTab("list " + name, "D: " + name, false);
            }
        } else { // list records
            Map<String, Journal> journals = List.decodeList(list); // - don't do this if it's divisions
            for (Map.Entry<String, Journal> entry : journals.entrySet()) {
                String id = entry.getKey();
                Journal journal = entry.getValue();
                if (id.trim().length() == 0)
                    continue;
                //Journal journal = Journal.decode(connection.command("read " + id));
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
        if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete record " + id + "?", "Warning", JOptionPane.YES_NO_OPTION)) {
            return;
        }

        // Delete.
        connection.command("delete " + id);
        updateFromNetwork();
    }
}
