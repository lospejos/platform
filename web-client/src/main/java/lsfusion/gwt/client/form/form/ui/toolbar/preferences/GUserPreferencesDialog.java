package lsfusion.gwt.client.form.form.ui.toolbar.preferences;

import com.allen_sauer.gwt.dnd.client.DragHandlerAdapter;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.Callback;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.ui.DialogBoxHelper;
import lsfusion.gwt.client.base.ui.FlexPanel;
import lsfusion.gwt.client.base.ui.GFlexAlignment;
import lsfusion.gwt.client.ErrorHandlingCallback;
import lsfusion.gwt.client.MainFrameMessages;
import lsfusion.gwt.client.form.form.ui.GCaptionPanel;
import lsfusion.gwt.client.form.form.ui.GGridTable;
import lsfusion.gwt.client.form.form.ui.GGroupObjectController;
import lsfusion.gwt.client.form.form.ui.dialog.GResizableModalWindow;
import lsfusion.gwt.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.shared.view.GFont;
import lsfusion.gwt.shared.view.GPropertyDraw;
import lsfusion.gwt.shared.view.changes.GGroupObjectValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.gwt.client.base.GwtClientUtils.createHorizontalStrut;
import static lsfusion.gwt.client.base.GwtClientUtils.createVerticalStrut;
import static lsfusion.gwt.shared.view.GFont.DEFAULT_FONT_FAMILY;
import static lsfusion.gwt.shared.view.GFont.DEFAULT_FONT_SIZE;

@SuppressWarnings("GWTStyleCheck")
public abstract class GUserPreferencesDialog extends GResizableModalWindow {
    private static final MainFrameMessages messages = MainFrameMessages.Instance.get();
    private static final String CSS_USER_PREFERENCES_DUAL_LIST = "userPreferencesDualList";

    private GGroupObjectController groupController;
    private GGridTable grid;

    private FocusPanel focusPanel;

    private ColumnsDualListBox columnsDualListBox;
    private TextBox pageSizeBox;
    private TextBox headerHeightBox;
    private TextBox sizeBox;
    private CheckBox boldBox;
    private CheckBox italicBox;

    private TextBox columnCaptionBox;
    private TextBox columnPatternBox;

    public GUserPreferencesDialog(GGridTable grid, GGroupObjectController groupController, boolean canBeSaved) {
        super(messages.formGridPreferences());

        this.groupController = groupController;

        this.grid = grid;

        // columns
        columnsDualListBox = new ColumnsDualListBox() {
            @Override
            public void setColumnCaptionBoxText(String text) {
                columnCaptionBox.setText(text);
            }
            @Override
            public void setColumnPatternBoxText(String pattern) {
                columnPatternBox.setText(pattern);
            }
        };
        columnsDualListBox.getDragController().addDragHandler(new DragHandlerAdapter());
        columnsDualListBox.addStyleName(CSS_USER_PREFERENCES_DUAL_LIST);

        // column caption settings        
        columnCaptionBox = new TextBox();
        columnCaptionBox.addStyleName("userPreferencesColumnTextBox");
        columnCaptionBox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                columnsDualListBox.columnCaptionBoxTextChanged(columnCaptionBox.getText());
            }
        });

        FlexPanel columnCaptionPanel = new FlexPanel();
        columnCaptionPanel.add(new Label(messages.formGridPreferencesColumnCaption() + ":"), GFlexAlignment.CENTER);
        columnCaptionPanel.add(createHorizontalStrut(2));
        columnCaptionPanel.add(columnCaptionBox);

        // column pattern settings
        columnPatternBox = new TextBox();
        columnPatternBox.addStyleName("userPreferencesColumnTextBox");
        columnPatternBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                columnsDualListBox.columnPatternBoxTextChanged(columnPatternBox.getText());
            }
        });

        FlexPanel columnPatternPanel = new FlexPanel();
        columnPatternPanel.add(new Label(messages.formGridPreferencesColumnPattern() + ":"), GFlexAlignment.CENTER);
        columnPatternPanel.add(createHorizontalStrut(2));
        columnPatternPanel.add(columnPatternBox);

        VerticalPanel columnSettingsPanel = new VerticalPanel();
        columnSettingsPanel.setSpacing(2);
        columnSettingsPanel.setWidth("100%");
        columnSettingsPanel.add(columnCaptionPanel);
        columnSettingsPanel.add(columnPatternPanel);

        //page size settings
        pageSizeBox = new TextBox();
        pageSizeBox.addStyleName("userPreferencesIntegralTextBox");
        FlexPanel pageSizePanel = new FlexPanel();
        pageSizePanel.add(new Label(messages.formGridPreferencesPageSize() + ":"), GFlexAlignment.CENTER);
        pageSizePanel.add(createHorizontalStrut(2));
        pageSizePanel.add(pageSizeBox);

        //header height
        headerHeightBox = new TextBox();
        headerHeightBox.addStyleName("userPreferencesIntegralTextBox");
        FlexPanel headerHeightPanel = new FlexPanel();
        headerHeightPanel.add(new Label(messages.formGridPreferencesHeaderHeight() + ":"), GFlexAlignment.CENTER);
        headerHeightPanel.add(createHorizontalStrut(2));
        headerHeightPanel.add(headerHeightBox);
        
        // font settings
        sizeBox = new TextBox();
        sizeBox.addStyleName("userPreferencesIntegralTextBox");
        boldBox = new CheckBox(messages.formGridPreferencesFontStyleBold());
        boldBox.addStyleName("userPreferencesCheckBox");
        italicBox = new CheckBox(messages.formGridPreferencesFontStyleItalic());
        italicBox.addStyleName("userPreferencesCheckBox");
        FlexPanel fontPanel = new FlexPanel();
        Label fontLabel = new Label(messages.formGridPreferencesFontSize() + ":");
        fontLabel.addStyleName("userPreferencesFontLabel");
        fontPanel.add(fontLabel, GFlexAlignment.CENTER);
        fontPanel.add(createHorizontalStrut(2));
        fontPanel.add(sizeBox);
        fontPanel.add(createHorizontalStrut(6));
        fontPanel.add(boldBox);
        fontPanel.add(createHorizontalStrut(6));
        fontPanel.add(italicBox);
        fontPanel.add(createHorizontalStrut(2));

        VerticalPanel gridSettingsPanel = new VerticalPanel();
        gridSettingsPanel.setSpacing(2);
        gridSettingsPanel.add(pageSizePanel);
        gridSettingsPanel.add(headerHeightPanel);
        gridSettingsPanel.add(new GCaptionPanel(messages.formGridPreferencesFont(), fontPanel));

        Button saveButton = null;
        Button resetButton = null; 
        if (canBeSaved) {
            saveButton = new Button(messages.formGridPreferencesSave());
            saveButton.addStyleName("userPreferencesSaveResetButton");
            saveButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    savePressed();
                }
            });

            resetButton = new Button(messages.formGridPreferencesReset());
            resetButton.addStyleName("userPreferencesSaveResetButton");
            resetButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    resetPressed();
                }
            });
        }

        // ok/cancel buttons
        Button okButton = new Button(messages.ok());
        okButton.setWidth("6em");
        okButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                okPressed();
            }
        });

        Button cancelButton = new Button(messages.cancel());
        cancelButton.setWidth("6em");
        cancelButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                hide();
            }
        });

        HorizontalPanel okCancelButtons = new HorizontalPanel();
        okCancelButtons.add(okButton);
        okCancelButtons.add(createHorizontalStrut(3));
        okCancelButtons.add(cancelButton);
        okCancelButtons.addStyleName("floatRight");

        
        VerticalPanel preferencesPanel = new VerticalPanel();
        preferencesPanel.setSpacing(3);
        preferencesPanel.setSize("100%", "100%");
        preferencesPanel.add(columnsDualListBox);
        preferencesPanel.setCellHeight(columnsDualListBox, "100%");
        preferencesPanel.add(GwtClientUtils.createVerticalStrut(3));
        preferencesPanel.add(new GCaptionPanel(messages.formGridPreferencesSelectedColumnSettings(), columnSettingsPanel));
        preferencesPanel.add(createVerticalStrut(3));
        preferencesPanel.add(new GCaptionPanel(messages.formGridPreferencesGridSettings(), gridSettingsPanel));
        preferencesPanel.add(createVerticalStrut(5));
        if (canBeSaved) {
            preferencesPanel.add(saveButton);
            preferencesPanel.add(resetButton);
        }
        preferencesPanel.add(okCancelButtons);

        focusPanel = new FocusPanel(preferencesPanel);
        focusPanel.addStyleName("noOutline");
        focusPanel.setSize("100%", "100%");
        focusPanel.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE) {
                    hide();
                }
            }
        });

        VerticalPanel mainContainer = new VerticalPanel();
        mainContainer.add(focusPanel);
        mainContainer.setCellHeight(focusPanel, "100%");
        mainContainer.setSize("430px", "450px");

        setContentWidget(mainContainer);

        refreshValues(mergeFont());
    }

    public void showDialog() {
        center();
        focusPanel.setFocus(true);
    }

    private void okPressed() {
        for (Widget label : columnsDualListBox.getVisibleWidgets()) {
            PropertyListItem property = ((PropertyLabel) label).getPropertyItem();
            grid.setColumnSettings(property.property, property.getUserCaption(true), property.getUserPattern(true),
                    columnsDualListBox.getVisibleIndex(label), false);
        }

        String[] hiddenPropSids = new String[columnsDualListBox.getInvisibleWidgets().size()];
        for (int i = 0; i < columnsDualListBox.getInvisibleWidgets().size(); i++) {
            Widget label = columnsDualListBox.getInvisibleWidgets().get(i);
            PropertyListItem property = ((PropertyLabel) label).getPropertyItem();
            grid.setColumnSettings(property.property, property.getUserCaption(true), property.getUserPattern(true),
                    columnsDualListBox.getVisibleCount() + i, true);
            if (property.inGrid == null || property.inGrid) {
                hiddenPropSids[i] = property.property.propertyFormName;
            }
        }

        GFont userFont = getUserFont();
        grid.setUserFont(userFont);
        grid.font = userFont;
        
        Integer userPageSize = getUserPageSize();
        grid.setUserPageSize(userPageSize);

        grid.setUserHeaderHeight(getUserHeaderHeight());
        grid.refreshColumnsAndRedraw();

        grid.setHasUserPreferences(true);

        grid.columnsPreferencesChanged();
        
        grid.refreshUPHiddenProps(hiddenPropSids);

        hide();
    }

    private GFont getUserFont() {
        GFont initialFont = getInitialFont();
        Integer size;
        try {
            size = Integer.parseInt(sizeBox.getValue());
        } catch(NumberFormatException e) {
            size = initialFont.size;
        }

        return new GFont(initialFont.family, size != 0 ? size : initialFont.size, boldBox.getValue(), italicBox.getValue());
    }

    private Integer getUserPageSize() {
        Integer pageSize;
        try {
            pageSize = Integer.parseInt(pageSizeBox.getValue());
        } catch(NumberFormatException e) {
            return null;
        }
        return pageSize != 0 ? pageSize : null;
    }

    private Integer getUserHeaderHeight() {
        Integer headerHeight;
        try {
            headerHeight = Integer.parseInt(headerHeightBox.getValue());
        } catch(NumberFormatException e) {
            return null;
        }
        return headerHeight >= 0 ? headerHeight : null;
    }

    private GFont getInitialFont() {
        GFont designFont = grid.getDesignFont();
        return designFont == null ? new GFont(grid.font != null ? grid.font.family : DEFAULT_FONT_FAMILY, GFont.DEFAULT_FONT_SIZE, false, false) : designFont;
    }

    private void resetPressed() {
        final GSaveResetConfirmDialog confirmDialog = new GSaveResetConfirmDialog(false);
        confirmDialog.show(new Callback() {
            @Override
            public void onSuccess() {
                columnCaptionBox.setText(null);
                columnPatternBox.setText(null);
                grid.resetPreferences(confirmDialog.forAll, confirmDialog.complete, createChangeCallback(false));
            }

            @Override
            public void onFailure() {
                focusPanel.setFocus(true);
            }
        });
    }

    private void savePressed() {
        final GSaveResetConfirmDialog confirmDialog = new GSaveResetConfirmDialog(true);
        confirmDialog.show(new Callback() {
            @Override
            public void onSuccess() {
                Map<GPropertyDraw, Map<Boolean, Integer>> userSortDirections = new HashMap<>();
                int i = 0;
                for (Map.Entry<Map<GPropertyDraw, GGroupObjectValue>, Boolean> entry : grid.getOrderDirections().entrySet()) {
                    HashMap<Boolean, Integer> dirs = new HashMap<>();
                    dirs.put(entry.getValue(), i);
                    userSortDirections.put(entry.getKey().keySet().iterator().next(), dirs);
                    i++;
                }
        
                for (Widget w : columnsDualListBox.getVisibleWidgets()) {
                    PropertyListItem property = ((PropertyLabel) w).getPropertyItem();
                    refreshPropertyUserPreferences(property, false, columnsDualListBox.getVisibleIndex(w), userSortDirections.get(property.property));
                }
        
                for (Widget w : columnsDualListBox.getInvisibleWidgets()) {
                    PropertyListItem property = ((PropertyLabel) w).getPropertyItem();
                    int propertyOrder = columnsDualListBox.getVisibleCount() + columnsDualListBox.getInvisibleIndex(w);
                    refreshPropertyUserPreferences(property, true, propertyOrder, userSortDirections.get(property.property));
                }
        
                GFont userFont = getUserFont();
                grid.setUserFont(userFont);
                
                Integer userPageSize = getUserPageSize();
                grid.setUserPageSize(userPageSize);
        
                grid.setUserHeaderHeight(getUserHeaderHeight());
                grid.refreshColumnsAndRedraw();
                
                grid.saveCurrentPreferences(confirmDialog.forAll, createChangeCallback(true));
            }

            @Override
            public void onFailure() {
                focusPanel.setFocus(true);
            }
        });
    }

    private void refreshPropertyUserPreferences(PropertyListItem property, boolean hide, int propertyOrder, Map<Boolean, Integer> userSortDirections) {
        Boolean sortDirection = userSortDirections != null ? userSortDirections.keySet().iterator().next() : null;
        Integer sortIndex = userSortDirections != null ? userSortDirections.values().iterator().next() : null;
        grid.setColumnSettings(property.property, property.getUserCaption(true), property.getUserPattern(true), propertyOrder, hide);
        grid.setUserSort(property.property, sortDirection != null ? sortIndex : null);
        grid.setUserAscendingSort(property.property, sortDirection);
    }

    private Boolean getPropertyState(GPropertyDraw property) {
        if (groupController.isPropertyInGrid(property)) {
            return true;
        } else if (groupController.isPropertyInPanel(property)) {
            return false;
        }
        return null;
    }

    private void refreshValues(GFont font) {
        List<GPropertyDraw> orderedVisibleProperties = grid.getOrderedVisibleProperties(groupController.getGroupObjectProperties());
        GGridUserPreferences currentPreferences = grid.getCurrentPreferences();
        columnsDualListBox.clearLists();

        for (GPropertyDraw property : orderedVisibleProperties) {
            columnsDualListBox.addVisible(new PropertyListItem(property, currentPreferences.getUserCaption(property),
                    currentPreferences.getUserPattern(property), getPropertyState(property)));
        }
        for (GPropertyDraw property : groupController.getGroupObjectProperties()) {
            if (!orderedVisibleProperties.contains(property)) {
                columnsDualListBox.addInvisible(new PropertyListItem(property, currentPreferences.getUserCaption(property),
                        currentPreferences.getUserPattern(property), getPropertyState(property)));
            }
        }

        sizeBox.setValue((font == null || font.size == null) ? DEFAULT_FONT_SIZE.toString() : font.size.toString());
        boldBox.setValue(font != null && font.bold);
        italicBox.setValue(font != null && font.italic);

        Integer currentPageSize = currentPreferences.pageSize;
        pageSizeBox.setValue(currentPageSize == null ? "" : String.valueOf(currentPageSize));

        Integer currentHeaderHeight = currentPreferences.headerHeight;
        headerHeightBox.setValue(currentHeaderHeight == null ? "" : String.valueOf(currentHeaderHeight));
    }
    
    private GFont mergeFont() {
        GGridUserPreferences prefs = grid.getCurrentPreferences();

        GFont font = getInitialFont();
        if (prefs.hasUserPreferences()) {
            font = prefs.font;
        }
        return font;
    } 

    private ErrorHandlingCallback<ServerResponseResult> createChangeCallback(final boolean save) {
        return new ErrorHandlingCallback<ServerResponseResult>() {
            @Override
            public void success(ServerResponseResult result) {
                GFont font = mergeFont();
                refreshValues(font);
                grid.font = font;
                grid.columnsPreferencesChanged();
                grid.setUserHeaderHeight(getUserHeaderHeight());
                grid.refreshColumnsAndRedraw();
                preferencesChanged();
                String caption = save ? messages.formGridPreferencesSaving() : messages.formGridPreferencesResetting();
                String message = save ? messages.formGridPreferencesSaveSuccess() : messages.formGridPreferencesResetSuccess();
                DialogBoxHelper.showMessageBox(false, caption, message, new DialogBoxHelper.CloseCallback() {
                    @Override
                    public void closed(DialogBoxHelper.OptionType chosenOption) {
                        focusPanel.setFocus(true);
                    }
                });
            }

            @Override
            public void failure(Throwable caught) {
                GFont font = mergeFont();
                refreshValues(font);
                grid.font = font;
                grid.columnsPreferencesChanged();
                focusPanel.setFocus(true);
            }
        };
    }
    
    public abstract void preferencesChanged();
}
