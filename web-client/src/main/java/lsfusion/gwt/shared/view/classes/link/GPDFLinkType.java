package lsfusion.gwt.shared.view.classes.link;

import lsfusion.gwt.client.MainFrameMessages;

public class GPDFLinkType extends GLinkType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typePDFFileLinkCaption();
    }
}