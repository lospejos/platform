package lsfusion.server.data.expr.formula;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.classes.IsClassType;
import lsfusion.server.data.expr.classes.VariableClassExpr;
import lsfusion.server.data.expr.classes.VariableSingleClassExpr;
import lsfusion.server.data.expr.join.base.FormulaJoin;
import lsfusion.server.data.expr.join.classes.ObjectClassField;
import lsfusion.server.data.expr.join.inner.InnerBaseJoin;
import lsfusion.server.data.expr.key.KeyType;
import lsfusion.server.data.expr.where.pull.ExprPullWheres;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.query.compile.FJData;
import lsfusion.server.data.stat.KeyStat;
import lsfusion.server.data.stat.PropStat;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.stat.StatType;
import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.logics.classes.ValueClassSet;
import lsfusion.server.logics.classes.struct.ConcatenateClassSet;
import lsfusion.server.logics.classes.user.set.AndClassSet;

public class ConcatenateExpr extends VariableClassExpr {

    private final ImList<BaseExpr> exprs;

    public BaseExpr deconcatenate(int part) {
        return exprs.get(part);
    }

    public ConcatenateExpr(ImList<BaseExpr> exprs) {
        this.exprs = exprs;
    }

    public static Expr create(ImList<? extends Expr> exprs) {
        return new ExprPullWheres<Integer>() {
            @Override
            protected Expr proceedBase(ImMap<Integer, BaseExpr> map) {
                return BaseExpr.create(new ConcatenateExpr(ListFact.fromIndexedMap(map)));
            }
        }.proceed(exprs.toIndexedMap());
    }

    protected ConcatenateExpr translate(MapTranslate translator) {
        return new ConcatenateExpr(translator.translateDirect(exprs));
    }

    public void fillAndJoinWheres(MMap<FJData, Where> joins, Where andWhere) {
        for(BaseExpr param : exprs)
            param.fillJoinWheres(joins, andWhere);
    }

    protected AndClassSet getPartClass(AndClassSet classes, int i) {
        return ((ConcatenateClassSet)classes).get(i);
    }
    protected AndClassSet getConcatenateClass(AndClassSet[] classes) {
        return new ConcatenateClassSet(classes);
    }
    public ClassExprWhere getClassWhere(AndClassSet classes) {
        ClassExprWhere result = ClassExprWhere.TRUE;
        for(int i=0;i<exprs.size();i++)
            result = result.and(exprs.get(i).getClassWhere(getPartClass(classes, i)));
        return result;
    }

    public AndClassSet getAndClassSet(ImMap<VariableSingleClassExpr, AndClassSet> and) {
        AndClassSet[] andClasses = new AndClassSet[exprs.size()];
        for(int i=0;i<exprs.size();i++) {
            AndClassSet classSet = exprs.get(i).getAndClassSet(and);
            if(classSet==null)
                return null;
            andClasses[i] = classSet;
        }
        return getConcatenateClass(andClasses);
    }

    public boolean addAndClassSet(MMap<VariableSingleClassExpr, AndClassSet> and, AndClassSet add) {
        for(int i=0;i<exprs.size();i++)
            if(!(exprs.get(i).addAndClassSet(and,getPartClass(add, i))))
                return false;
        return true;
    }

    public Expr classExpr(ImSet<ObjectClassField> classes, IsClassType type) {
        throw new RuntimeException("not supported");
    }

    public Where isClass(ValueClassSet set, IsClassType type) {
        Where where = Where.TRUE();
        for(int i=0;i<exprs.size();i++)
            where = where.and(exprs.get(i).isClass((ValueClassSet)getPartClass(set, i), type), type.isInconsistent());
        return where;
    }

    public Expr translate(ExprTranslator translator) {
        return create(translator.translate(exprs));
    }

    public boolean calcTwins(TwinImmutableObject obj) {
        return exprs.equals(((ConcatenateExpr)obj).exprs);
    }

    protected boolean isComplex() {
        return true;
    }
    public int hash(HashContext hashContext) {
        return hashOuter(exprs, hashContext);
    }

    public Type getType(KeyType keyType) {
        Type[] types = new Type[exprs.size()];
        for(int i=0;i<exprs.size();i++)
            types[i] = exprs.get(i).getType(keyType);
        return ConcatenateType.get(types);
    }

    public Stat getTypeStat(KeyStat keyStat, boolean forJoin) {
        Stat result = Stat.ONE;
        for (BaseExpr expr : exprs) result = result.mult(expr.getTypeStat(keyStat, forJoin));
        return result;
    }

    public String getSource(final CompileSource compile, final boolean needValue) {
        ImList<String> sources = exprs.mapListValues((BaseExpr value) -> value.getSource(compile, needValue));
        return ((ConcatenateType)getType(compile.keyType)).getConcatenateSource(sources,compile.syntax,compile.env);
    }

    @Override
    public PropStat getStatValue(KeyStat keyStat, StatType type) {
        return FormulaExpr.getStatValue(this, keyStat);
    }

    public InnerBaseJoin<?> getBaseJoin() {
        return new FormulaJoin<>(exprs.toIndexedMap(), true);
    }
}
