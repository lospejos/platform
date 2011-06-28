package platform.interop.navigator;

import platform.interop.event.IDaemonTask;
import platform.interop.form.RemoteFormInterface;
import platform.interop.remote.ClientCallBackInterface;
import platform.interop.remote.PendingRemote;

import java.rmi.RemoteException;
import java.util.ArrayList;

public interface RemoteNavigatorInterface extends PendingRemote {

    String getForms(String formSet) throws RemoteException;

    RemoteFormInterface createForm(String formSID, boolean currentSession, boolean interactive) throws RemoteException;

    RemoteFormInterface createForm(byte[] formState) throws RemoteException;

    void saveForm(String formSID, byte[] formState) throws RemoteException;

    void saveVisualSetup(byte[] data) throws RemoteException;

    byte[] getRichDesignByteArray(String formSID) throws RemoteException;

    byte[] getFormEntityByteArray(String formSID) throws RemoteException;

    byte[] getCurrentUserInfoByteArray() throws RemoteException;

    byte[] getElementsByteArray(String groupSID) throws RemoteException;

    void relogin(String login) throws RemoteException;

    void clientExceptionLog(String info) throws RemoteException;

    final static String NAVIGATORGROUP_RELEVANTFORM = "_NAV_RELEVANTFORM_";
    final static String NAVIGATORGROUP_RELEVANTCLASS = "_NAV_RELEVANTCLASS_";

    void close() throws RemoteException;

    ClientCallBackInterface getClientCallBack() throws RemoteException;

    boolean showDefaultForms() throws RemoteException;

    ArrayList<String> getDefaultForms() throws RemoteException;

    byte[] getNavigatorTree() throws RemoteException;
    byte[] getWindows() throws RemoteException;

    ArrayList<IDaemonTask> getDaemonTasks(int compId) throws RemoteException;
}
