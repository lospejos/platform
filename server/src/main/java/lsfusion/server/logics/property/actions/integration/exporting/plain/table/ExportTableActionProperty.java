package lsfusion.server.logics.property.actions.integration.exporting.plain.table;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.logics.property.actions.integration.FormIntegrationType;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.FormSelector;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.ObjectSelector;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.integration.exporting.plain.ExportPlainActionProperty;
import lsfusion.server.logics.property.actions.integration.exporting.plain.ExportPlainWriter;

import java.io.IOException;

public class ExportTableActionProperty<O extends ObjectSelector> extends ExportPlainActionProperty<O> {

    public ExportTableActionProperty(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, FormIntegrationType staticType, LCP exportFile, ImMap<GroupObjectEntity, LCP> exportFiles, String charset) {
        super(caption, form, objectsToSet, nulls, staticType, exportFile, exportFiles, charset);
    }

    @Override
    protected ExportPlainWriter getWriter(ImOrderMap<String, Type> fieldTypes, boolean singleRow) throws IOException {
        return new ExportTableWriter(fieldTypes, singleRow);
    }
}