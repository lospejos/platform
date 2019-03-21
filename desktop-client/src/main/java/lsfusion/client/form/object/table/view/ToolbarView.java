package lsfusion.client.form.object.table.view;

import lsfusion.client.form.design.view.JComponentPanel;
import lsfusion.client.form.object.table.ClientToolbar;
import lsfusion.interop.form.design.Alignment;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.base.view.FlexConstraints;
import lsfusion.interop.base.view.FlexLayout;

import javax.swing.*;
import java.awt.*;

public class ToolbarView extends JComponentPanel {
    private JPanel mainPanel;

    public ToolbarView(ClientToolbar toolbar) {
        setLayout(new FlexLayout(this, false, Alignment.START));
        initBottomContainer();
        toolbar.installMargins(this);
    }

    private void initBottomContainer() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
        mainPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        add(mainPanel, new FlexConstraints(FlexAlignment.CENTER, 0));
    }

    public void addComponent(Component component) {
        mainPanel.add(component);
    }
}