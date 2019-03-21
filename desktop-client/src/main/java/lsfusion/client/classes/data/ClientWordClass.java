package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.cell.classes.controller.FilePropertyEditor;
import lsfusion.client.form.property.cell.classes.view.WordPropertyRenderer;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.classes.DataType;

public class ClientWordClass extends ClientStaticFormatFileClass {

    public final static ClientWordClass instance = new ClientWordClass(false, false);

    public ClientWordClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    @Override
    public String[] getExtensions() {
        return new String[] {"doc", "docx"};
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new WordPropertyRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return obj.toString();
    }

    public byte getTypeId() {
        return DataType.WORD;
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new FilePropertyEditor(multiple, storeName, ClientResourceBundle.getString("logics.classes.word"), getExtensions());
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.word.file");
    }
}