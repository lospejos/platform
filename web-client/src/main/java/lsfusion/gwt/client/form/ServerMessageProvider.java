package lsfusion.gwt.client.form;

import lsfusion.gwt.shared.result.ListResult;
import lsfusion.gwt.client.ErrorHandlingCallback;
import net.customware.gwt.dispatch.shared.general.StringResult;

public interface ServerMessageProvider {
    void getServerActionMessage(ErrorHandlingCallback<StringResult> callback);
    void getServerActionMessageList(ErrorHandlingCallback<ListResult> callback);
    void interrupt(boolean cancelable);
}