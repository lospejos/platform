package platform.gwt.login.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.handlers.SimpleActionHandlerEx;
import platform.gwt.base.shared.actions.VoidResult;
import platform.gwt.login.server.LoginServiceImpl;
import platform.gwt.login.shared.actions.RemindPassword;

import java.io.IOException;

public class RemindPasswordHandler extends SimpleActionHandlerEx<RemindPassword, VoidResult> {
    protected final LoginServiceImpl servlet;

    public RemindPasswordHandler(LoginServiceImpl servlet) {
        this.servlet = servlet;
    }

    @Override
    public VoidResult executeEx(RemindPassword action, ExecutionContext context) throws DispatchException, IOException {
        servlet.getLogics().remindPassword(action.email);
        return new VoidResult();
    }
}
