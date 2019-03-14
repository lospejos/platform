package lsfusion.server.physics.admin.service;

import lsfusion.base.SystemUtils;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.base.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.language.ScriptingAction;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;

import java.sql.SQLException;

public class GetVMInfoActionProperty extends ScriptingAction {
    public GetVMInfoActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        String message = SystemUtils.getVMInfo();
        context.delayUserInterfaction(new MessageClientAction(message, ThreadLocalContext.localize("{vm.data}")));
    }
}