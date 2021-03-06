package lsfusion.server.physics.admin.profiler.action;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.profiler.Profiler;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.HashSet;

import static lsfusion.server.physics.admin.profiler.Profiler.profileForms;
import static lsfusion.server.physics.admin.profiler.Profiler.profileUsers;

public class StartProfilerAction extends InternalAction {
    public StartProfilerAction(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            KeyExpr userKey = new KeyExpr("User");
            QueryBuilder<Object, Object> queryU = new QueryBuilder<>(MapFact.singletonRev("User", userKey));
            queryU.and(findProperty("overFilter[User]").getExpr(context.getModifier(), userKey).getWhere());
            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> resultU = queryU.execute(context.getSession());

            profileUsers = new HashSet<>();
            for (ImMap<Object, Object> imMap : resultU.keys()) {
                profileUsers.add((Long) imMap.get("User"));
            }

            KeyExpr formKey = new KeyExpr("ProfileForm");
            QueryBuilder<Object, Object> queryF = new QueryBuilder<>(MapFact.singletonRev("ProfileForm", formKey));
            queryF.addProperty("formCN", findProperty("canonicalName[Form]").getExpr(context.getModifier(), formKey));
            queryF.and(findProperty("dataFilter[Form]").getExpr(context.getModifier(), formKey).getWhere());
            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> resultF = queryF.execute(context.getSession());

            profileForms = new HashSet<>();
            for (ImMap<Object, Object> imMap : resultF.values()) {
                profileForms.add(BaseUtils.nullToString(imMap.get("formCN")));
            }

            Profiler.PROFILER_ENABLED = true;
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
    }
}
