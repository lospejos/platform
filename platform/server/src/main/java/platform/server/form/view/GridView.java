package platform.server.form.view;

import java.io.DataOutputStream;
import java.io.IOException;

public class GridView extends ComponentView implements ClientSerialize {

    public boolean showFind = false;
    public boolean showFilter = true;

    public byte minRowCount = 0;
    public boolean tabVertical = false;
    public boolean autoHide = false;

    public GridView(int ID) {
        super(ID);
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeBoolean(showFind);
        outStream.writeBoolean(showFilter);

        outStream.writeByte(minRowCount);
        outStream.writeBoolean(tabVertical);

        outStream.writeBoolean(autoHide);
    }
}
