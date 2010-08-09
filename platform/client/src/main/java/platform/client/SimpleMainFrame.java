package platform.client;

import platform.client.form.ClientForm;
import platform.client.navigator.ClientNavigator;
import platform.client.navigator.ClientNavigatorForm;
import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.StringTokenizer;

public class SimpleMainFrame extends MainFrame {
    public SimpleMainFrame(RemoteNavigatorInterface remoteNavigator, String forms) throws ClassNotFoundException, IOException {
        super(remoteNavigator);

        ClientNavigator navigator = new ClientNavigator(remoteNavigator) {

            @Override
            public void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException {
                // пока ничего не делаем, так как не должны вообще формы вызываться
            }
        };

        final JTabbedPane mainPane = new JTabbedPane(JTabbedPane.BOTTOM);

        StringTokenizer st = new StringTokenizer(forms, ",");
        while (st.hasMoreTokens()) {
            Integer formID = Integer.parseInt(st.nextToken());
            final ClientForm form = new ClientForm(navigator.remoteNavigator.createForm(formID, false), navigator);
            mainPane.addTab(form.getFullCaption(), form.getComponent());

            KeyStroke keyStroke = form.getKeyStroke();
            if (keyStroke != null) {
                mainPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, formID);
                mainPane.getActionMap().put(formID, new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        mainPane.setSelectedComponent(form.getComponent());
                    }
                });
            }
        }
        setContentPane(mainPane);
        mainPane.setFocusable(false);
    }

    @Override
    public void runReport(ClientNavigator clientNavigator, RemoteFormInterface remoteForm) throws ClassNotFoundException, IOException {
        // надо здесь подумать, что вызывать
    }

    @Override
    public void runForm(ClientNavigator clientNavigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {
        // надо здесь подумать, что вызывать
    }
}
