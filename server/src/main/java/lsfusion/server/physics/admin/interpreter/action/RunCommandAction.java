package lsfusion.server.physics.admin.interpreter.action;

import com.google.common.base.Throwables;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.BufferedInputStream;
import java.sql.SQLException;
import java.util.Iterator;

public class RunCommandAction extends InternalAction {
    private final ClassPropertyInterface commandInterface;
    private final ClassPropertyInterface isClientInterface;

    public RunCommandAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        commandInterface = i.next();
        isClientInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        String command = (String) context.getKeyValue(commandInterface).getValue();
        boolean isClient = context.getKeyValue(isClientInterface).getValue() != null;
        if(command != null) {
            if (isClient) {
                String result = (String) context.requestUserInteraction(new RunCommandClientAction(command));
                if (result != null) {
                    context.requestUserInteraction(new MessageClientAction(result, "Ошибка"));
                }
            } else {
                try {
                    Process p = Runtime.getRuntime().exec(command);
                    BufferedInputStream err = new BufferedInputStream(p.getErrorStream());
                    StringBuilder errS = new StringBuilder();
                    byte[] b = new byte[1024];
                    while (err.read(b) != -1) {
                        errS.append(new String(b, "cp866").trim()).append("\n");
                    }
                    err.close();
                    String result = errS.toString();
                    if (!result.isEmpty()) {
                        throw new RuntimeException(result);
                    }
                } catch (Exception e) {
                    throw Throwables.propagate(e);
                }
            }
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}