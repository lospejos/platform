package platform.server.logics.property;

import platform.server.classes.ValueClass;
import platform.server.classes.ActionClass;
import platform.server.classes.ConcreteValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.*;
import platform.server.data.where.WhereBuilder;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.view.form.client.RemoteFormView;
import platform.server.view.form.PropertyObjectImplement;
import platform.server.view.form.PropertyObjectInterface;
import platform.interop.action.ClientAction;

import java.util.Map;
import java.util.List;
import java.sql.SQLException;

public abstract class ActionProperty extends UserProperty {

    public ActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }

    // RemoteForm в качестве параметра нужен поскольку действие, как правило, приводит к какой-то реакции со стороны формы
    // RemoteFormView нужен потому, что нужно создавать новый объект RMI, а для этого нужен как минимум порт - правда это в дальнейшем нужно переделать
    public abstract void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteFormView executeForm, Map<ClassPropertyInterface, PropertyObjectInterface> mapObjects) throws SQLException;

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, List<ClientAction> actions, RemoteFormView executeForm, Map<ClassPropertyInterface, PropertyObjectInterface> mapObjects) throws SQLException {
        execute(keys, value, actions, executeForm, mapObjects);
    }

    protected <U extends Changes<U>> U calculateUsedChanges(Modifier<U> modifier) {
        return ClassProperty.getIsClassUsedChanges(interfaces, modifier);
    }
    
    protected Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        return getValueClass().getActionExpr().and(ClassProperty.getIsClassWhere(joinImplement, modifier, changedWhere));
    }

    protected ValueClass getValueClass() {
        return ActionClass.instance;
    }
}
