package lsfusion.server.logics.navigator.controller.context;

import lsfusion.base.Result;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.form.ModalityType;
import lsfusion.server.base.controller.remote.ui.RemoteUIContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.ManageSessionType;
import lsfusion.server.logics.form.interactive.controller.remote.RemoteForm;
import lsfusion.server.logics.form.interactive.dialogedit.DialogRequest;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.listener.CustomClassListener;
import lsfusion.server.logics.form.interactive.listener.FocusListener;
import lsfusion.server.logics.form.interactive.listener.RemoteFormListener;
import lsfusion.server.logics.form.interactive.property.checked.PullChangeProperty;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilter;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.navigator.controller.remote.RemoteNavigator;
import lsfusion.server.physics.admin.authentication.controller.remote.RemoteConnection;
import lsfusion.server.physics.admin.authentication.controller.remote.RemoteConnectionContext;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;
import lsfusion.server.physics.admin.log.LogInfo;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Stack;

// multiple inheritance - RemoteConnection + RemoteUI
public class RemoteNavigatorContext extends RemoteConnectionContext {
    private final RemoteNavigator navigator;
    
    private final RemoteUIContext uiContext; // multiple inheritance
    
    public RemoteNavigatorContext(RemoteNavigator remoteNavigator) {
        navigator = remoteNavigator;
        
        uiContext = new RemoteUIContext() {
            @Override
            protected ImSet<SecurityPolicy> getSecurityPolicies() {
                return SetFact.singleton(navigator.securityPolicy);
            }

            @Override
            protected int getExportPort() {
                return navigator.getExportPort();
            }

            @Override
            protected RemoteFormListener getFormListener() {
                return navigator;
            }

            @Override
            public LogicsInstance getLogicsInstance() {
                return RemoteNavigatorContext.this.getLogicsInstance();
            }

            @Override
            public void aspectDelayUserInteraction(ClientAction action, String message) {
                RemoteNavigatorContext.this.aspectDelayUserInteraction(action, message);
            }

            @Override
            public Object[] aspectRequestUserInteraction(ClientAction[] actions, String[] messages) {
                return RemoteNavigatorContext.this.aspectRequestUserInteraction(actions, messages);
            }

            @Override
            public FocusListener getFocusListener() {
                return RemoteNavigatorContext.this.getFocusListener();
            }

            @Override
            public CustomClassListener getClassListener() {
                return RemoteNavigatorContext.this.getClassListener();
            }

            @Override
            public Long getCurrentComputer() {
                return RemoteNavigatorContext.this.getCurrentComputer();
            }

            @Override
            public Long getCurrentUser() {
                return RemoteNavigatorContext.this.getCurrentUser();
            }

            @Override
            public Long getCurrentUserRole() {
                return RemoteNavigatorContext.this.getCurrentUserRole();
            }

            @Override
            public LogInfo getLogInfo() {
                return RemoteNavigatorContext.this.getLogInfo();
            }

            @Override
            public Locale getLocale() {
                return RemoteNavigatorContext.this.getLocale();
            }

            @Override
            protected boolean isExternal() {
                return getForm.get() != null;
            }

            @Override
            protected void requestFormUserInteraction(RemoteForm remoteForm, ModalityType modalityType, boolean forbidDuplicate, ExecutionStack stack) throws SQLException, SQLHandledException {
                Stack<Result<RemoteForm>> getForms = getForm.get();
                if(getForms != null)
                    getForms.peek().set(remoteForm);
                else
                    super.requestFormUserInteraction(remoteForm, modalityType, forbidDuplicate, stack);
            }
        };
    }

    private ThreadLocal<Stack<Result<RemoteForm>>> getForm = new ThreadLocal<>();

    public void pushGetForm() {
        Stack<Result<RemoteForm>> getForms = getForm.get();
        if(getForms == null) {
            getForms = new Stack<>();
            getForm.set(getForms);
        }
        getForms.push(new Result<>());
    }

    public RemoteForm popGetForm() {
        Stack<Result<RemoteForm>> getForms = getForm.get();
        Result<RemoteForm> result = getForms.pop();
        if(getForms.isEmpty())
            getForm.remove();
        return result.result;
    }

    @Override
    protected RemoteConnection getConnectionObject() {
        return navigator;
    }

    public void aspectDelayUserInteraction(ClientAction action, String message) {
        navigator.delayUserInteraction(action);
    }

    @Override
    public Object[] aspectRequestUserInteraction(ClientAction[] actions, String[] messages) {
        return navigator.requestUserInteraction(actions);
    }

    public FocusListener getFocusListener() {
        return navigator;
    }

    public CustomClassListener getClassListener() {
        return navigator;
    }

    // UI interfaces, multiple inheritance
    
    @Override
    public void requestFormUserInteraction(FormInstance formInstance, ModalityType modalityType, boolean forbidDuplicate, ExecutionStack stack) throws SQLException, SQLHandledException {
        uiContext.requestFormUserInteraction(formInstance, modalityType, forbidDuplicate, stack);
    }

    public ObjectValue requestUserObject(DialogRequest dialog, ExecutionStack stack) throws SQLException, SQLHandledException { // null если canceled
        return uiContext.requestUserObject(dialog, stack);
    }

    public ObjectValue requestUserData(DataClass dataClass, Object oldValue) {
        return uiContext.requestUserData(dataClass, oldValue);
    }

    public ObjectValue requestUserClass(CustomClass baseClass, CustomClass defaultValue, boolean concrete) {
        return uiContext.requestUserClass(baseClass, defaultValue, concrete);
    }

    public FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, Boolean noCancel, ManageSessionType manageSession, ExecutionStack stack, boolean checkOnOk, boolean showDrop, boolean interactive, boolean isFloat, ImSet<ContextFilter> contextFilters, ImSet<PullChangeProperty> pullProps, boolean readonly) throws SQLException, SQLHandledException {
        return uiContext.createFormInstance(formEntity, mapObjects, session, isModal, noCancel, manageSession, stack, checkOnOk, showDrop, interactive, isFloat, contextFilters, pullProps, readonly);
    }

    public RemoteForm createRemoteForm(FormInstance formInstance, ExecutionStack stack) {
        return uiContext.createRemoteForm(formInstance, stack);
    }

}
