package platform.server.form.view;

import platform.base.IDGenerator;
import platform.server.form.entity.ObjectEntity;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ObjectView implements ServerIdentitySerializable {

    public ObjectEntity entity;
    private GroupObjectView groupObject;

    public ClassChooserView classChooser;

    public ObjectView() {
        
    }
    
    public ObjectView(IDGenerator idGen, ObjectEntity entity, GroupObjectView groupTo) {

        this.entity = entity;
        this.groupObject = groupTo;

        classChooser = new ClassChooserView(idGen.idShift(), this.entity, this);
    }

    public int getID() {
        return entity.ID;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, groupObject);
        pool.writeString(outStream, entity.caption);

        outStream.writeBoolean(entity.addOnTransaction);

        entity.baseClass.serialize(outStream);
        pool.serializeObject(outStream, classChooser);
    }

    public void customDeserialize(ServerSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        classChooser = pool.deserializeObject(inStream);

        entity = pool.context.form.getObject(iID);
    }
}
