package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.form.client.MainFrameMessages;

public class GCSVType extends GFileType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeCSVFileCaption();
    }
}