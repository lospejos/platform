package lsfusion.utils.file.open;

import com.google.common.base.Throwables;
import lsfusion.interop.action.OpenUriClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.classes.link.LinkClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;
import org.apache.commons.httpclient.util.URIUtil;

import java.net.URI;
import java.sql.SQLException;
import java.util.Iterator;

public class OpenLinkActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface sourceInterface;

    public OpenLinkActionProperty(BaseLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        sourceInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            ObjectValue sourceObject = context.getDataKeyValue(sourceInterface);
            for (URI file : ((LinkClass) ((DataObject) sourceObject).getType()).getFiles(sourceObject.getValue())) {
                context.delayUserInteraction(new OpenUriClientAction(new URI(URIUtil.decode(file.toString()))));
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}