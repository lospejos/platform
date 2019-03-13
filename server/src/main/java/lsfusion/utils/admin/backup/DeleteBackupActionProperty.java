package lsfusion.utils.admin.backup;

import com.google.common.base.Throwables;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;
import lsfusion.server.language.ScriptingLogicsModule;

import java.io.File;
import java.sql.SQLException;
import java.util.Iterator;

public class DeleteBackupActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface backupInterface;

    public DeleteBackupActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        backupInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try (ExecutionContext.NewSession<ClassPropertyInterface> newContext = context.newSession()) {
            DataObject backupObject = newContext.getDataKeyValue(backupInterface);

            String backupFilePath = (String) findProperty("file[Backup]").read(newContext, backupObject);
            String backupLogFilePath = (String) findProperty("fileLog[Backup]").read(newContext, backupObject);
            File f = new File(backupFilePath);
            File fLog = new File(backupLogFilePath);
            if (fLog.exists() && !fLog.delete()) {
                fLog.deleteOnExit();
            }
            if (!f.exists() || f.delete()) {
                ServerLoggers.systemLogger.info("Deleted backup " + f.getName());
                findProperty("fileDeleted[Backup]").change(true, newContext, backupObject);
                context.delayUserInteraction(new MessageClientAction("Deleted backup " + f.getName(), "Deleted backup"));
            }
            newContext.apply();

        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }
}
