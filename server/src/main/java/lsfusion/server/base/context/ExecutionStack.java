package lsfusion.server.base.context;

import lsfusion.base.Pair;
import lsfusion.base.lambda.Processor;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.language.linear.LP;
import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.logics.action.session.classes.UpdateCurrentClassesSession;

import java.sql.SQLException;
import java.util.List;

public interface ExecutionStack {

    ImMap<String, String> getAllParamsWithClassesInStack();

    ImMap<String, ObjectValue> getAllParamsWithValuesInStack();

    ImSet<Pair<LP, List<ResolveClassSet>>> getAllLocalsInStack();

    Processor<ImMap<String, ObjectValue>> getWatcher();

    boolean hasNewDebugStack();

    void updateCurrentClasses(UpdateCurrentClassesSession session) throws SQLException, SQLHandledException;

    boolean sameSession(UpdateCurrentClassesSession session);
}
