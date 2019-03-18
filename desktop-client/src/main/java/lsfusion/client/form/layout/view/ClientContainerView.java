package lsfusion.client.form.layout.view;

import lsfusion.client.form.layout.ClientComponent;
import lsfusion.client.form.layout.ClientContainer;

import java.awt.*;
import java.util.Map;

public interface ClientContainerView {
    void add(ClientComponent child, JComponentPanel view);
    void remove(ClientComponent child);

    boolean hasChild(ClientComponent child);
    int getChildrenCount();
    ClientComponent getChild(int index);
    Component getChildView(int index);

    JComponentPanel getView();
    
    void updateLayout();

    Dimension getMaxPreferredSize(Map<ClientContainer, ClientContainerView> containerViews);
}