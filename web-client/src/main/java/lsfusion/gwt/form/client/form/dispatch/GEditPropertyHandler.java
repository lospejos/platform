package lsfusion.gwt.form.client.form.dispatch;

import lsfusion.gwt.form.shared.view.classes.GType;

public interface GEditPropertyHandler {
    public void requestValue(GType valueType, Object oldValue);

    public void updateEditValue(Object value);

    public void takeFocusAfterEdit();
}
