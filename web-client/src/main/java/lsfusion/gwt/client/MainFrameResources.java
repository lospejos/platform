package lsfusion.gwt.client;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

public interface MainFrameResources extends ClientBundle {
    @Source("lsfusion/gwt/public/images/vborder.png")
    ImageResource dragger();

    @CssResource.NotStrict
    @Source("MainFrame.css")
    CssResource css();
}