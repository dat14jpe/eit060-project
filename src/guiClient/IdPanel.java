package guiClient;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import common.Individual;

public class IdPanel extends JPanel {
    public IdPanel(Individual id) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(new JLabel(id.getName())); // - might get a little... long, in the side panel
        add(new JLabel(Individual.typeString(id.getType())));
        add(new JLabel("Division: " + id.getUnit()));
    }
}
