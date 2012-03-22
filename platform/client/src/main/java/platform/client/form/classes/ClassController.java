package platform.client.form.classes;

import platform.client.ClientResourceBundle;
import platform.client.form.ClientFormController;
import platform.client.form.ClientFormLayout;
import platform.client.logics.ClientObject;
import platform.client.logics.classes.ClientObjectClass;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.IOException;

public class ClassController {

    // компоненты для отображения
    private ClassContainer classContainer;
    private ClassTree view;

    // данные по объекту, класс которого обрабатывается
    private final ClientObject object;

    // объект, при помощи которого будет происходить общение с внешним миром
    private final ClientFormController form;

    public ClassController(ClientObject iobject, ClientFormController iform) throws IOException {

        this.object = iobject;
        this.form = iform;
    }

    public ClassContainer getClassContainer() {
        return classContainer;
    }

    public void addView(ClientFormLayout formLayout) {

        // создаем дерево для отображения классов
        view = new ClassTree(object.getID(), object.baseClass) {

            protected void currentClassChanged() {
                try {
                    form.changeGridClass(object, view.getSelectionClass());
                } catch (IOException e) {
                    throw new RuntimeException(ClientResourceBundle.getString("errors.error.changing.current.class"), e);
                }
            }
        };

        classContainer = new ClassContainer(view) {

            protected void needToBeRevalidated() {
                form.dropLayoutCaches();
            }

            protected void widthDecreased() {
                object.classChooser.constraints.fillHorizontal *= 0.95 ;
            }

            protected void widthIncreased() {
                object.classChooser.constraints.fillHorizontal = 0.95 * object.classChooser.constraints.fillHorizontal + 0.05;
            }
        };

        formLayout.add(object.classChooser, classContainer);
    }

    public void showViews() {

        if (object.classChooser.show) {

            if (classContainer != null)
                classContainer.setVisible(true);
        }
    }

    public void hideViews() {

        if (classContainer != null)
            classContainer.setVisible(false);
    }

    // нужно для того, что если объект типа дата, то для него не будет возможностей добавлять объекты
    public boolean allowedEditObjects() { return object.baseClass instanceof ClientObjectClass; }

    private DefaultMutableTreeNode getSelectedNode() {

        TreePath path = view.getSelectionModel().getLeadSelectionPath();
        if (path == null) return null;

        return (DefaultMutableTreeNode) path.getLastPathComponent();
    }

    public ClientObjectClass getDerivedClass() {

        DefaultMutableTreeNode selNode = getSelectedNode();
        if (selNode == null || !view.getCurrentNode().isNodeChild(selNode)) return (ClientObjectClass) view.getCurrentClass();

        return (ClientObjectClass) selNode.getUserObject();
    }
}