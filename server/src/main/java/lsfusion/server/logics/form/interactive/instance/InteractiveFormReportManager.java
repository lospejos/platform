package lsfusion.server.logics.form.interactive.instance;

import lsfusion.interop.form.stat.report.FormPrintType;
import lsfusion.interop.form.user.FormUserPreferences;
import lsfusion.interop.form.stat.report.ReportGenerationData;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.form.stat.report.FormReportManager;
import lsfusion.server.logics.form.stat.report.InteractiveFormReportInterface;

import java.sql.SQLException;

public class InteractiveFormReportManager extends FormReportManager {
    
    public InteractiveFormReportManager(FormInstance form) {
        this(form, null, null);
    }
    public InteractiveFormReportManager(FormInstance form, Integer groupId, FormUserPreferences preferences) {
        super(new InteractiveFormReportInterface(form, groupId, preferences));
    }

    // backward compatibility
    public ReportGenerationData getReportData(Integer groupId, boolean toExcel, FormUserPreferences preferences) throws SQLException, SQLHandledException {
        return new InteractiveFormReportManager(((InteractiveFormReportInterface)reportInterface).getForm(), groupId, preferences).getReportData(toExcel ? FormPrintType.XLS : FormPrintType.PRINT);
    }

}