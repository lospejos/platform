package platform.client.logics;

import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;

public class ClientGrid extends ClientComponent {

    public boolean showFind;
    public boolean showFilter;

    public byte minRowCount;
    public boolean tabVertical = true;
    public boolean autoHide;

    public ClientGroupObject groupObject;

    public ClientGrid() {
        
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);
        outStream.writeBoolean(showFind);
        outStream.writeBoolean(showFilter);

        outStream.writeByte(minRowCount);
        outStream.writeBoolean(tabVertical);
        outStream.writeBoolean(autoHide);

        pool.serializeObject(outStream, groupObject);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, iID, inStream);

        showFind = inStream.readBoolean();
        showFilter = inStream.readBoolean();

        minRowCount = inStream.readByte();
        tabVertical = inStream.readBoolean();
        autoHide = inStream.readBoolean();

        groupObject = pool.deserializeObject(inStream);
    }

    @Override
    public String toString() {
        return "Таблица (" + groupObject.toString() + ")";
    }
}
