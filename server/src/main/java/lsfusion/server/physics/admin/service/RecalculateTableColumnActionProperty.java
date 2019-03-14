package lsfusion.server.physics.admin.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.language.ScriptingAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.physics.admin.reflection.ReflectionLogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.action.session.DataSession;

import java.sql.SQLException;
import java.util.Iterator;

import static lsfusion.server.base.context.ThreadLocalContext.localize;

public class RecalculateTableColumnActionProperty extends ScriptingAction {

    private final ClassPropertyInterface tableColumnInterface;

    public RecalculateTableColumnActionProperty(ReflectionLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        tableColumnInterface = i.next();
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject tableColumnObject = context.getDataKeyValue(tableColumnInterface);
        final ObjectValue propertyObject = context.getBL().reflectionLM.propertyTableColumn.readClasses(context, tableColumnObject);
        final String propertyCanonicalName = (String) context.getBL().reflectionLM.canonicalNameProperty.read(context, propertyObject);

        try(ExecutionContext.NewSession<ClassPropertyInterface> newContext = context.newSession()) {
            final DataSession dataSession = newContext.getSession();
            ServiceDBActionProperty.run(context, new RunService() {
                public void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                    context.getDbManager().recalculateAggregationTableColumn(dataSession, session, propertyCanonicalName.trim(), isolatedTransaction);
                }
            });
            newContext.apply();
        }

        context.delayUserInterfaction(new MessageClientAction(localize(LocalizedString.createFormatted("{logics.recalculation.completed}", 
                localize("{logics.recalculation.aggregations}"))), localize("{logics.recalculation.aggregations}")));
    }
}