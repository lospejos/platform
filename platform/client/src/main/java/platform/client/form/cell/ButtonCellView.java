package platform.client.form.cell;

import platform.client.SwingUtils;
import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.ClientGroupObjectValue;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class ButtonCellView extends JButton implements CellView {
    private ClientPropertyDraw key;
    private ClientGroupObjectValue columnKey; // чисто для кэширования

    @Override
    public int hashCode() {
        return key.getID() * 31 + columnKey.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ButtonCellView && ((ButtonCellView) o).key.equals(key) && ((ButtonCellView) o).columnKey.equals(columnKey);
    }

    public ButtonCellView(final ClientPropertyDraw key, ClientGroupObjectValue columnKey, final ClientFormController form) {
        super(key.getFullCaption());
        this.key = key;
        this.columnKey = columnKey;

        if (key.readOnly) {
            setEnabled(false);
        }

        key.design.designComponent(this);

        addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {

                try {

                    PropertyEditorComponent editor = key.getEditorComponent(form, null);
                    if (editor != null) {
                        editor.getComponent(SwingUtils.computeAbsoluteLocation(ButtonCellView.this), getBounds(), null);
                        if (editor.valueChanged())
                            listener.cellValueChanged(editor.getCellEditorValue());
                    }

                } catch (Exception e) {
                    throw new RuntimeException("Ошибка при выполнении действия", e);
                }
            }
        });
    }

    public JComponent getComponent() {
        return this;
    }

    private CellViewListener listener;
    public void addListener(CellViewListener listener) {
        this.listener = listener;
    }

    public void setValue(Object ivalue) {
        // собственно, а как в Button нужно устанавливать value
    }

    public void startEditing(KeyEvent e) {
        doClick(20);
    }

    public void setCaption(String caption) {
        setText(caption);
    }

    public void setHighlight(Object highlight) {
        // пока не highlight'им
    }
}
