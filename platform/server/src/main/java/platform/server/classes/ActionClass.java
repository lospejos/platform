package platform.server.classes;

import platform.interop.Data;
import platform.server.data.sql.SQLSyntax;
import platform.server.form.view.report.ReportDrawField;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;

// по умолчанию будем считать, что у ActionClass'а данные как у LogicalClass
public class ActionClass extends DataClass<Object> {

    public static final ActionClass instance = new ActionClass();
    private final static String sid = "ActionClass";

    public ActionClass() {
        DataClass.storeClass(sid, instance);
    }

    @Override
    public String toString() {
        return "Action";
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof ActionClass ? this : null;
    }

    public Object getDefaultValue() {
        return true;
//        throw new RuntimeException("Неправильный вызов интерфейса");
    }

    @Override
    public byte getTypeID() {
        return Data.ACTION;
    }

    protected Class getJavaClass() {
        return Boolean.class;
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getBitType();
    }

    public boolean isSafeString(Object value) {
        return true;
    }

    public String getString(Object value, SQLSyntax syntax) {
        assert (Boolean)value;
        return syntax.getBitString(true);
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        assert (Boolean)value;
        statement.setByte(num, (byte)1);
    }

    public Format getDefaultFormat() {
        return null;
    }

    public Object read(Object value) {
        if(value!=null) return true;
        return null;
    }

    @Override
    public boolean fillReportDrawField(ReportDrawField reportField) {
        return false;
    }

    public String getSID() {
        return sid;
    }
}
