package lsfusion.gwt.shared.view.classes;

import lsfusion.gwt.client.MainFrameMessages;

public class GHTMLType extends GFileType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeHTMLFileCaption();
    }
}
