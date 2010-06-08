package platform.server.logics.property;

import platform.server.logics.DataObject;
import platform.server.data.SQLSession;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.CompareWhere;
import platform.server.session.*;
import platform.server.classes.CustomClass;
import platform.server.view.form.client.RemoteFormView;
import platform.server.view.form.PropertyObjectInterface;
import platform.interop.action.ClientAction;

import java.util.Map;
import java.util.List;
import java.sql.SQLException;

public class PropertyValueImplement<P extends PropertyInterface> extends PropertyImplement<DataObject,P> {

    public PropertyValueImplement(Property<P> property, Map<P, DataObject> mapping) {
        super(property, mapping);
    }

    public Object read(SQLSession session, Modifier<? extends Changes> modifier) throws SQLException {
        return property.read(session, mapping, modifier);
    }

    @Override
    public String toString() {
        return property.toString();
    }

    public int getID() {
        return property.ID;
    }

    public CustomClass getDialogClass(DataSession session) {
        return property.getDialogClass(mapping, session.getCurrentClasses(mapping));
    }

    public PropertyChange<P> getPropertyChange(Expr expr) {
        Map<P, KeyExpr> mapKeys = property.getMapKeys();
        return new PropertyChange<P>(mapKeys, expr, CompareWhere.compareValues(mapKeys, mapping));
    }

    public boolean canBeChanged(Modifier<? extends Changes> modifier) {
        return property.getDataChanges(getPropertyChange(property.changeExpr), null, modifier).changes.hasChanges();
    }

    public List<ClientAction> execute(DataSession session, Object value, Modifier<? extends Changes> modifier, RemoteFormView executeForm, Map<P, PropertyObjectInterface> mapObjects) throws SQLException {
        return session.execute(property, getPropertyChange(session.getObjectValue(value, property.getType()).getExpr()), modifier, executeForm, mapObjects);
    }
}
