package lsfusion.client.form.object.table.grid.view;

import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.view.JComponentPanel;
import lsfusion.client.form.object.table.grid.ClientGrid;
import lsfusion.client.form.object.table.grid.controller.GridTableController;
import lsfusion.client.form.object.table.grid.user.design.GridUserPreferences;

import javax.swing.*;
import java.awt.*;

public class GridView extends JComponentPanel {
    final JScrollPane pane;

    private final ClientGrid grid;
    private final GridTable gridTable;
    private final GridTableController gridController;

    @Override
    public Dimension getMaxPreferredSize() { // ради этого вся ветка maxPreferredSize и делалась
        return gridTable.getMaxPreferredSize(getPreferredSize());
    }

    public GridView(GridTableController igridController, ClientFormController form, GridUserPreferences[] iuserPreferences, boolean tabVertical, boolean verticalScroll) {
        gridController = igridController;

        grid = gridController.getGroupController().getGroupObject().grid;

        gridTable = new GridTable(this, form, iuserPreferences);

        gridTable.setTabVertical(tabVertical);

        pane = new JScrollPane(gridTable) {
            @Override
            public void doLayout() {
                // хак, чтобы не изменялся ряд при изменении размеров таблицы,
                // а вместо этого она скроллировалась к видимой строчке
                gridTable.setLayouting(true);
                super.doLayout();
                gridTable.setLayouting(false);
            }

            @Override
            public boolean isValidateRoot() {
                return false;
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension preferredSize = super.getPreferredSize();
                Dimension viewSize = gridTable.getPreferredSize();
                Dimension extentSize = viewport.getPreferredSize();

                // компенсируем добавление к preferredSize размеров скроллбаров, чтобы избежать прыжков размеров таблицы на форме
                // см. ScrollPaneLayout.preferredLayoutSize()
                if (verticalScrollBar != null && verticalScrollBarPolicy == VERTICAL_SCROLLBAR_AS_NEEDED) {
                    boolean canScrollV = !gridTable.getScrollableTracksViewportHeight();
                    if (canScrollV && viewSize.height > extentSize.height) {
                        preferredSize.width -= getVerticalScrollBar().getPreferredSize().width;
                    }
                }

                if (horizontalScrollBar != null && horizontalScrollBarPolicy == HORIZONTAL_SCROLLBAR_AS_NEEDED) {
                    boolean canScrollH = !gridTable.getScrollableTracksViewportWidth();
                    if (canScrollH && viewSize.width > extentSize.width) {
                        preferredSize.height -= getHorizontalScrollBar().getPreferredSize().height;
                    }
                }

                return preferredSize;
            }
        };
        pane.setVerticalScrollBarPolicy(verticalScroll ? ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED : ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        gridTable.setFillsViewportHeight(true);

        gridTable.configureEnclosingScrollPane(pane);

        grid.installMargins(this);

        add(pane, BorderLayout.CENTER);
    }

    public GridTableController getGridController() {
        return gridController;
    }

    public GridTable getTable() {
        return gridTable;
    }

    public int getHeaderHeight() {
        return grid.getHeaderHeight();
    }
}