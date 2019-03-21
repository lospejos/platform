package lsfusion.client.classes;

import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.property.cell.EditBindingMap;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.panel.view.PanelView;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.form.property.Compare;

import java.awt.*;
import java.io.IOException;
import java.text.ParseException;

public interface ClientType {

    PropertyRenderer getRendererComponent(ClientPropertyDraw property);

    PanelView getPanelView(ClientPropertyDraw key, ClientGroupObjectValue columnKey, ClientFormController form);

    PropertyEditor getChangeEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, Object value);

    PropertyEditor getObjectEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, Object value) throws IOException, ClassNotFoundException;

    PropertyEditor getValueEditorComponent(ClientFormController form, ClientPropertyDraw property, Object value);

    Object parseString(String s) throws ParseException;

    String formatString(Object obj) throws ParseException;

    boolean shouldBeDrawn(ClientFormController form);
    
    String getConfirmMessage();

    ClientTypeClass getTypeClass();

    Compare[] getFilterCompares();

    Compare getDefaultCompare();

    EditBindingMap.EditEventFilter getEditEventFilter();

    // добавляет поправку на кнопки и другие элементы 
    int getFullWidthString(String widthString, FontMetrics fontMetrics, ClientPropertyDraw propertyDraw);

    int getDefaultWidth(FontMetrics fontMetrics, ClientPropertyDraw property);

    int getDefaultHeight(FontMetrics fontMetrics, int charHeight);
}
