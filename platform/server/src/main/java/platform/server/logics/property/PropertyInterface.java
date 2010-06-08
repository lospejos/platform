package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.expr.PullExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.session.*;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class PropertyInterface<P extends PropertyInterface<P>> implements PropertyInterfaceImplement<P>, Comparable<P> {

    public int ID = 0;

    public PropertyInterface(int ID) {
        this.ID = ID;

        this.changeExpr = new PullExpr("interface " + ID);
    }

    public String toString() {
        return "I/"+ID;
    }

    public Expr mapExpr(Map<P, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        return joinImplement.get((P) this);
    }

    public ObjectValue read(DataSession session, Map<P, DataObject> interfaceValues, Modifier<? extends Changes> modifier) {
        return interfaceValues.get((P) this);
    }

    public void mapFillDepends(Collection<Property> depends) {
    }

    public int compareTo(P o) {
        return ID-o.ID;
    }

    // для того чтобы "попробовать" изменения (на самом деле для кэша)
    public final Expr changeExpr;

    public MapDataChanges<P> mapJoinDataChanges(Map<P, KeyExpr> joinImplement, Expr expr, Where where, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
        return new MapDataChanges<P>();
    }

    public void fill(Set<P> interfaces, Set<PropertyMapImplement<?, P>> properties) {
        interfaces.add((P) this);
    }

    public <K extends PropertyInterface> PropertyInterfaceImplement<K> map(Map<P, K> remap) {
        return remap.get((P)this);
    }
}
