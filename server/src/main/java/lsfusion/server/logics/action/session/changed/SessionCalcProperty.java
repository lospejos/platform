package lsfusion.server.logics.action.session.changed;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.event.PrevScope;
import lsfusion.server.logics.property.AggregateProperty;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.SimpleIncrementProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class SessionCalcProperty<T extends PropertyInterface> extends SimpleIncrementProperty<T> {

    public final CalcProperty<T> property;
    public final PrevScope scope;

    public SessionCalcProperty(LocalizedString caption, CalcProperty<T> property, PrevScope scope) {
        super(caption, property.getFriendlyOrderInterfaces());
        this.property = property;
        this.scope = scope;
    }

    public abstract OldProperty<T> getOldProperty();

    public abstract ChangedProperty<T> getChangedProperty();

    @Override
    public ImSet<OldProperty> getParseOldDepends() {
        return SetFact.singleton((OldProperty)getOldProperty());
    }

    @Override
    public ImSet<SessionCalcProperty> getSessionCalcDepends(boolean events) {
        return super.getSessionCalcDepends(events).addExcl(this); // вызываем super так как могут быть вычисляемые события внутри (желательно для SetOrDropped оптимизации)
    }

    @Override
    public boolean aspectDebugHasAlotKeys() {
        return property instanceof AggregateProperty && ((AggregateProperty) property).hasAlotKeys();
    }
}
