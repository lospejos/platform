package lsfusion.server.physics.admin.service.action;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.sql.SQLException;

public class SetReupdateModeAction extends InternalAction {

    public SetReupdateModeAction(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        Object value = context.getSingleKeyObject();
        DBManager.PROPERTY_REUPDATE = value!=null;
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }

}
