package lsfusion.server.logics.property.classes.data;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.FormulaUnionExpr;
import lsfusion.server.data.expr.formula.FormulaUnionImpl;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.UnionProperty;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class FormulaUnionProperty extends UnionProperty {

    protected FormulaUnionProperty(LocalizedString caption, ImOrderSet<Interface> interfaces) {
        super(caption, interfaces);
    }

    @Override
    protected Expr calculateExpr(final ImMap<Interface, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges propChanges, final WhereBuilder changedWhere) {
        ImCol<Expr> exprs = getOperands().mapColValues(value -> value.mapExpr(joinImplement, calcType, propChanges, changedWhere));
        return FormulaUnionExpr.create(getFormula(), exprs.toList());
    }

    protected boolean useSimpleIncrement() {
        return true;
    }

    protected abstract FormulaUnionImpl getFormula();

    @Override
    public ExClassSet calcInferValueClass(ImMap<Interface, ExClassSet> inferred, InferType inferType) {
        return FormulaImplProperty.inferValueClass(getOrderInterfaces(), getFormula(), inferred);
    }
}
