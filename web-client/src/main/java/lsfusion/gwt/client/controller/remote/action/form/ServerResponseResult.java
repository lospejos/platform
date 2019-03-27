package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.action.GAction;
import net.customware.gwt.dispatch.shared.Result;

public class ServerResponseResult implements Result {
    public GAction[] actions;
    public boolean resumeInvocation;

    public ServerResponseResult() {}

    public ServerResponseResult(GAction[] actions, boolean resumeInvocation) {
        this.actions = actions;
        this.resumeInvocation = resumeInvocation;
    }
}