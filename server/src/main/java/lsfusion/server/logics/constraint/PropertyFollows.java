package lsfusion.server.logics.constraint;

import lsfusion.server.logics.property.oraction.PropertyInterface;

public class PropertyFollows<T extends PropertyInterface, L extends PropertyInterface> {

    public final static int RESOLVE_TRUE = 1;
    public final static int RESOLVE_FALSE = 2;
    public final static int RESOLVE_ALL = RESOLVE_TRUE | RESOLVE_FALSE;
    public final static int RESOLVE_NOTHING = 0;

/*    public PropertyFollows(Property<T> property, PropertyMapImplement<L, T> implement, int options) {
        this.property = property;
        this.implement = implement;
        this.options = options;
    }

    private final Property<T> property;
    private final PropertyMapImplement<L, T> implement;
    private int options;

    public void resolveTrue(DataSession session) throws SQLException {
        if((options & RESOLVE_TRUE) == 0) // для оптимизации в общем то
            return;
        assert property.interfaces.size() == implement.mapping.size(); // assert что количество
        Map<T,KeyExpr> mapKeys = property.getMapKeys();
        implement.mapNotNull(mapKeys, property.getExpr(mapKeys, session.modifier).getWhere(), session, session.modifier);
    }

    public void resolveFalse(DataSession session) throws SQLException {
        if((options & RESOLVE_FALSE) == 0) // для оптимизации в общем то
            return;
        Map<T,KeyExpr> mapKeys = property.getMapKeys();
        property.setNull(mapKeys, implement.mapExpr(mapKeys, session.modifier).getWhere().not(), session, session.modifier);
    }*/
}
