/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platform.client.form;

import platform.base.BaseUtils;
import platform.base.DefaultIDGenerator;
import platform.base.IDGenerator;
import platform.client.Log;
import platform.client.Main;
import platform.client.SwingUtils;
import platform.client.logics.*;
import platform.client.logics.classes.ClientConcreteClass;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.filter.ClientPropertyFilter;
import platform.client.navigator.ClientNavigator;
import platform.interop.CompressingInputStream;
import platform.interop.Order;
import platform.interop.Scroll;
import platform.interop.action.CheckFailed;
import platform.interop.action.ClientAction;
import platform.interop.action.ClientApply;
import platform.interop.form.RemoteChanges;
import platform.interop.form.RemoteFormInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ClientFormController {

    private final ClientForm form;

    public final RemoteFormInterface remoteForm;
    public final ClientNavigator clientNavigator;
    public final ClientFormActionDispatcher actionDispatcher;

    public boolean isDialogMode() {
        return false;
    }

    public boolean isReadOnlyMode() {
        return form.readOnly;
    }

    private static IDGenerator idGenerator = new DefaultIDGenerator();
    private int ID;
    public int getID() {
        return ID;
    }

    public KeyStroke getKeyStroke() {
        return form.keyStroke;
    }

    public String getCaption() {
        return  form.caption;
    }

    public String getFullCaption() {
        return  form.getFullCaption();
    }

    public ClientFormController(RemoteFormInterface remoteForm, ClientNavigator clientNavigator) throws IOException, ClassNotFoundException {

        ID = idGenerator.idShift();

        // Форма нужна, чтобы с ней общаться по поводу данных и прочих
        this.remoteForm = remoteForm;

        // Навигатор нужен, чтобы уведомлять его об изменениях активных объектов, чтобы он мог себя переобновлять
        this.clientNavigator = clientNavigator;

        actionDispatcher = new ClientFormActionDispatcher();

        form = new ClientForm(new DataInputStream(new CompressingInputStream(new ByteArrayInputStream(remoteForm.getRichDesignByteArray()))));

        initializeForm();
    }

    // ------------------------------------------------------------------------------------ //
    // ----------------------------------- Инициализация ---------------------------------- //
    // ------------------------------------------------------------------------------------ //

    private ClientFormLayout formLayout;

    private Map<ClientGroupObject, GroupObjectController> controllers;

    private JButton buttonApply;
    private JButton buttonCancel;

    public ClientFormLayout getComponent() {
        return formLayout;
    }

    void initializeForm() throws IOException {

        formLayout = new ClientFormLayout(form.containers) {

            @Override
            protected void gainedFocus() {

                try {
                    remoteForm.gainedFocus();
                    if (clientNavigator != null) {
                        clientNavigator.currentFormChanged();
                    }

                    // если вдруг изменились данные в сессии
                    ClientExternalScreen.invalidate(getID());
                    ClientExternalScreen.repaintAll(getID());
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при активации формы", e);
                }
            }
        };

//        setContentPane(formLayout.getComponent());
//        setComponent(formLayout.getComponent());

        initializeGroupObjects();

        initializeRegularFilters();

        initializeButtons();

        initializeOrders();

        applyRemoteChanges();
    }

    // здесь хранится список всех GroupObjects плюс при необходимости null
    private List<ClientGroupObject> groupObjects;
    public List<ClientGroupObject> getGroupObjects() {
        return groupObjects;
    }


    private void initializeGroupObjects() throws IOException {

        controllers = new HashMap<ClientGroupObject, GroupObjectController>();
        groupObjects = new ArrayList<ClientGroupObject>();

        for (ClientGroupObject groupObject : form.groupObjects) {
            groupObjects.add(groupObject);
            GroupObjectController controller = new GroupObjectController(groupObject, form, this, formLayout);
            controllers.put(groupObject, controller);
        }

        for (ClientPropertyDraw properties : form.getProperties()) {
            if (properties.groupObject == null) {
                groupObjects.add(null);
                GroupObjectController controller = new GroupObjectController(null, form, this, formLayout);
                controllers.put(null, controller);
                break;
            }
        }
    }

    private void initializeRegularFilters() {

        // Проинициализируем регулярные фильтры

        for (final ClientRegularFilterGroup filterGroup : form.regularFilters) {

            if (filterGroup.filters.size() == 1) {

                final ClientRegularFilter singleFilter = filterGroup.filters.get(0);

                final JCheckBox checkBox = new JCheckBox(singleFilter.toString());
                checkBox.addItemListener(new ItemListener() {

                    public void itemStateChanged(ItemEvent ie) {
                        try {
                            if (ie.getStateChange() == ItemEvent.SELECTED)
                                setRegularFilter(filterGroup, singleFilter);
                            else
                                setRegularFilter(filterGroup, null);
                        } catch (IOException e) {
                            throw new RuntimeException("Ошибка при изменении регулярного фильтра", e);
                        }
                    }
                });
                formLayout.add(filterGroup, checkBox);
                formLayout.addBinding(singleFilter.key, "regularFilter" + filterGroup.ID + singleFilter.ID, new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        checkBox.setSelected(!checkBox.isSelected());
                    }
                });

                if(filterGroup.defaultFilter >= 0) {
                    checkBox.setSelected(true);
                    try {
                        setRegularFilter(filterGroup, singleFilter);
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при инициализации регулярного фильтра", e);
                    }
                }
            } else {

                final JComboBox comboBox = new JComboBox(
                        BaseUtils.mergeList(Collections.singletonList("(Все)"),filterGroup.filters).toArray());
                comboBox.addItemListener(new ItemListener() {

                    public void itemStateChanged(ItemEvent ie) {
                        try {
                            setRegularFilter(filterGroup,
                                    ie.getItem() instanceof ClientRegularFilter ?(ClientRegularFilter)ie.getItem():null);
                        } catch (IOException e) {
                            throw new RuntimeException("Ошибка при изменении регулярного фильтра", e);
                        }
                    }
                });
                formLayout.add(filterGroup, comboBox);

                for (final ClientRegularFilter singleFilter : filterGroup.filters) {
                    formLayout.addBinding(singleFilter.key, "regularFilter" + filterGroup.ID + singleFilter.ID, new AbstractAction() {
                        public void actionPerformed(ActionEvent e) {
                            comboBox.setSelectedItem(singleFilter);
                        }
                    });
                }

                if(filterGroup.defaultFilter >= 0) {
                    ClientRegularFilter defaultFilter = filterGroup.filters.get(filterGroup.defaultFilter);
                    comboBox.setSelectedItem(defaultFilter);
                    try {
                        setRegularFilter(filterGroup, defaultFilter);
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при инициализации регулярного фильтра", e);
                    }
                }
            }

        }
    }

    private void initializeButtons() {

        KeyStroke altP = KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK);
        KeyStroke altX = KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.ALT_DOWN_MASK);
        KeyStroke altDel = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.ALT_DOWN_MASK);
        KeyStroke altR = KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK);
        KeyStroke altEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, (isDialogMode() && isReadOnlyMode()) ? 0 : InputEvent.ALT_DOWN_MASK);
        KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

        // Добавляем стандартные кнопки

        if(Main.module.isFull()) {
            AbstractAction printAction = new AbstractAction("Печать (" + SwingUtils.getKeyStrokeCaption(altP) + ")") {

                public void actionPerformed(ActionEvent ae) {
                    print();
                }
            };
            formLayout.addBinding(altP, "altPPressed", printAction);

            JButton buttonPrint = new JButton(printAction);
            buttonPrint.setFocusable(false);

            AbstractAction xlsAction = new AbstractAction("Excel (" + SwingUtils.getKeyStrokeCaption(altX) + ")") {

                public void actionPerformed(ActionEvent ae) {
                    Main.module.runExcel(remoteForm);
                }
            };
            formLayout.addBinding(altX, "altXPressed", xlsAction);

            JButton buttonXls = new JButton(xlsAction);
            buttonXls.setFocusable(false);

            if (!isDialogMode()) {
                formLayout.add(form.printFunction, buttonPrint);
                formLayout.add(form.xlsFunction, buttonXls);
            }            
        }

        AbstractAction nullAction = new AbstractAction("Сбросить (" + SwingUtils.getKeyStrokeCaption(altDel) + ")") {

            public void actionPerformed(ActionEvent ae) {
                nullPressed();
            }
        };
        JButton buttonNull = new JButton(nullAction);
        buttonNull.setFocusable(false);

        AbstractAction refreshAction = new AbstractAction("Обновить (" + SwingUtils.getKeyStrokeCaption(altR) + ")") {

            public void actionPerformed(ActionEvent ae) {
                refreshData();
            }
        };
        JButton buttonRefresh = new JButton(refreshAction);
        buttonRefresh.setFocusable(false);

        AbstractAction applyAction = new AbstractAction("Применить (" + SwingUtils.getKeyStrokeCaption(altEnter) + ")") {

            public void actionPerformed(ActionEvent ae) {
                applyChanges();
            }
        };
        buttonApply = new JButton(applyAction);
        buttonApply.setFocusable(false);

        AbstractAction cancelAction = new AbstractAction("Отменить (" + SwingUtils.getKeyStrokeCaption(escape) + ")") {

            public void actionPerformed(ActionEvent ae) {
                cancelChanges();
            }
        };
        buttonCancel = new JButton(cancelAction);
        buttonCancel.setFocusable(false);

        AbstractAction okAction = new AbstractAction("OK (" + SwingUtils.getKeyStrokeCaption(altEnter) + ")") {

            public void actionPerformed(ActionEvent ae) {
                okPressed();
            }
        };
        JButton buttonOK = new JButton(okAction);
        buttonOK.setFocusable(false);

        AbstractAction closeAction = new AbstractAction("Закрыть (" + SwingUtils.getKeyStrokeCaption(escape) + ")") {

            public void actionPerformed(ActionEvent ae) {
                closePressed();
            }
        };
        JButton buttonClose = new JButton(closeAction);
        buttonClose.setFocusable(false);

        formLayout.addBinding(altR, "altRPressed", refreshAction);
        formLayout.add(form.refreshFunction, buttonRefresh);
        
        if (!isDialogMode()) {

            formLayout.addBinding(altEnter, "enterPressed", applyAction);
            formLayout.add(form.applyFunction, buttonApply);

            formLayout.addBinding(escape, "escapePressed", cancelAction);
            formLayout.add(form.cancelFunction, buttonCancel);

        } else {

            formLayout.addBinding(altDel, "altDelPressed", nullAction);
            formLayout.add(form.nullFunction, buttonNull);

            formLayout.addBinding(altEnter, "enterPressed", okAction);
            formLayout.add(form.okFunction, buttonOK);

            formLayout.addBinding(escape, "escapePressed", closeAction);
            formLayout.add(form.closeFunction, buttonClose);
        }
    }

    private void initializeOrders() throws IOException {
        // Применяем порядки по умолчанию
        for (Map.Entry<ClientCell, Boolean> entry : form.defaultOrders.entrySet()) {
            controllers.get(entry.getKey().getGroupObject()).changeGridOrder(entry.getKey(), Order.ADD);
            if (!entry.getValue()) {
                controllers.get(entry.getKey().getGroupObject()).changeGridOrder(entry.getKey(), Order.DIR);
            }
        }
    }

    private void applyRemoteChanges() throws IOException {
        RemoteChanges remoteChanges = remoteForm.getRemoteChanges();

        for(ClientAction action : remoteChanges.actions)
            action.dispatch(actionDispatcher);

        Log.incrementBytesReceived(remoteChanges.form.length);
        applyFormChanges(new ClientFormChanges(new DataInputStream(new CompressingInputStream(new ByteArrayInputStream(remoteChanges.form))), form));

        if (clientNavigator != null) {
            clientNavigator.changeCurrentClass(remoteChanges.classID);
        }
    }

    private Color defaultApplyBackground;
    private boolean dataChanged;
    
    private void applyFormChanges(ClientFormChanges formChanges) {

        if(formChanges.dataChanged!=null && buttonApply!=null) {
            if (defaultApplyBackground == null)
                defaultApplyBackground = buttonApply.getBackground();

            dataChanged = formChanges.dataChanged;
            if (dataChanged) {
                buttonApply.setBackground(Color.green);
                buttonApply.setEnabled(true);
                buttonCancel.setEnabled(true);
            } else {
                buttonApply.setBackground(defaultApplyBackground);
                buttonApply.setEnabled(false);
                buttonCancel.setEnabled(false);
            }
        }

        for (GroupObjectController controller : controllers.values()) {
            controller.processFormChanges(formChanges);
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                formLayout.getComponent().revalidate();
                ClientExternalScreen.repaintAll(getID());
            }
        });

        // выдадим сообщение если было от сервера
        if (formChanges.message.length() > 0) {
            Log.printFailedMessage(formChanges.message);
        }
    }

    public void changeGroupObject(ClientGroupObject groupObject, ClientGroupObjectValue objectValue) throws IOException {

        ClientGroupObjectValue curObjectValue = controllers.get(groupObject).getCurrentObject();

        if (!objectValue.equals(curObjectValue)) {
            // приходится вот так возвращать класс, чтобы не было лишних запросов
            remoteForm.changeGroupObject(groupObject.getID(), Serializer.serializeClientGroupObjectValue(objectValue));
            controllers.get(groupObject).setCurrentGroupObject(objectValue, true);

            applyRemoteChanges();
        }
    }

    public void changeGroupObject(ClientGroupObject groupObject, Scroll changeType) throws IOException {

        remoteForm.changeGroupObject(groupObject.getID(), changeType.serialize());

        applyRemoteChanges();
    }


    public void changePropertyDrawWithColumnKeys(ClientPropertyDraw property, Object value, boolean all, ClientGroupObjectValue columnKey) throws IOException {
        remoteForm.changePropertyDrawWithColumnKeys(property.getID(), BaseUtils.serializeObject(value), all, Serializer.serializeClientGroupObjectValue(columnKey));
        applyRemoteChanges();
    }
    
    public void changeProperty(ClientCell property, Object value, boolean all) throws IOException {

        if (property.getGroupObject() != null) // для глобальных свойств пока не может быть отложенных действий
            SwingUtils.stopSingleAction(property.getGroupObject().getActionID(), true);

        if (property instanceof ClientPropertyDraw) {

            remoteForm.changePropertyDraw(property.getID(), BaseUtils.serializeObject(value), all);
            applyRemoteChanges();

        } else {

            if (property instanceof ClientClassCell) {
                changeClass(((ClientClassCell)property).object, (ClientConcreteClass)value);
            } else {

                ClientObject object = ((ClientObjectIDCell)property).object;
                remoteForm.changeObject(object.getID(), value);
                controllers.get(property.getGroupObject()).setCurrentObject(object, value);
                applyRemoteChanges();
            }
        }
    }

    void addObject(ClientObject object, ClientConcreteClass cls) throws IOException {
        
        remoteForm.addObject(object.getID(), cls.ID);
        applyRemoteChanges();
    }

    public void changeClass(ClientObject object, ClientConcreteClass cls) throws IOException {

        SwingUtils.stopSingleAction(object.groupObject.getActionID(), true);

        remoteForm.changeClass(object.getID(), (cls == null) ? -1 : cls.ID);
        applyRemoteChanges();
    }

    public void changeGridClass(ClientObject object, ClientObjectClass cls) throws IOException {

        remoteForm.changeGridClass(object.getID(), cls.ID);
        applyRemoteChanges();
    }

    public void switchClassView(ClientGroupObject groupObject) throws IOException {

        SwingUtils.stopSingleAction(groupObject.getActionID(), true);

        remoteForm.switchClassView(groupObject.getID());
        
        applyRemoteChanges();
    }

    public void changeClassView(ClientGroupObject groupObject, byte show) throws IOException {

        SwingUtils.stopSingleAction(groupObject.getActionID(), true);

        remoteForm.changeClassView(groupObject.getID(), show);

        applyRemoteChanges();
    }

    public void changeOrder(ClientCell property, Order modiType) throws IOException {

        if (property instanceof ClientPropertyDraw) {
            remoteForm.changePropertyOrder(property.getID(), modiType.serialize());
        } else if (property instanceof ClientClassCell) {
            remoteForm.changeObjectClassOrder(property.getID(), modiType.serialize());
        } else {
            remoteForm.changeObjectOrder(property.getID(), modiType.serialize());
        }

        applyRemoteChanges();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void changeFind(List<ClientPropertyFilter> conditions) {
    }

    private final Map<ClientGroupObject, List<ClientPropertyFilter>> currentFilters = new HashMap<ClientGroupObject, List<ClientPropertyFilter>>();
    
    public void changeFilter(ClientGroupObject groupObject, List<ClientPropertyFilter> conditions) throws IOException {

        currentFilters.put(groupObject, conditions);

        remoteForm.clearUserFilters();

        for (List<ClientPropertyFilter> listFilter : currentFilters.values())
            for (ClientPropertyFilter filter : listFilter) {
                remoteForm.addFilter(Serializer.serializeClientFilter(filter));
            }

        applyRemoteChanges();
    }

    private void setRegularFilter(ClientRegularFilterGroup filterGroup, ClientRegularFilter filter) throws IOException {

        remoteForm.setRegularFilter(filterGroup.ID, (filter == null) ? -1 : filter.ID);

        applyRemoteChanges();
    }

    public void changePageSize(ClientGroupObject groupObject, int pageSize) throws IOException {
        remoteForm.changePageSize(groupObject.getID(), pageSize);
    }

    void print() {

        try {
            Main.frame.runReport(remoteForm);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при печати формы", e);
        }
    }

    void refreshData() {

        try {

            remoteForm.refreshData();

            applyRemoteChanges();

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при обновлении формы", e);
        }
    }

    void applyChanges() {

        try {

            if (dataChanged) {

                String okMessage = "";
                for (ClientGroupObject groupObject : form.groupObjects) {
                    okMessage += controllers.get(groupObject).getSaveMessage();
                }

                if (!okMessage.isEmpty()) {
                    if (!(SwingUtils.showConfirmDialog(getComponent(), okMessage, null, JOptionPane.QUESTION_MESSAGE, SwingUtils.YES_BUTTON) == JOptionPane.YES_OPTION)) {
                        return;
                    }
                }

                if(remoteForm.hasClientApply()) {
                    ClientApply clientApply = remoteForm.getClientApply();
                    if(clientApply instanceof CheckFailed) // чтобы не делать лишний RMI вызов
                        Log.printFailedMessage(((CheckFailed)clientApply).message);
                    else {
                        remoteForm.applyClientChanges(((ClientAction)clientApply).dispatch(actionDispatcher));

                        applyRemoteChanges();
                    }
                } else {
                    remoteForm.applyChanges();

                    applyRemoteChanges();
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при применении изменений", e);
        }
    }

    boolean cancelChanges() {

        try {

            if (dataChanged) {

                if (SwingUtils.showConfirmDialog(getComponent(), "Вы действительно хотите отменить сделанные изменения ?", null, JOptionPane.WARNING_MESSAGE, SwingUtils.NO_BUTTON) == JOptionPane.YES_OPTION) {
                    remoteForm.cancelChanges();

                    applyRemoteChanges();
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при отмене изменений", e);
        }

        return true;
    }

    public void okPressed() {
        applyChanges();
    }

    boolean closePressed() {
        return cancelChanges();
    }

    boolean nullPressed() {
        return true;
    }

    public void dropLayoutCaches() {
        formLayout.dropCaches();
    }
}