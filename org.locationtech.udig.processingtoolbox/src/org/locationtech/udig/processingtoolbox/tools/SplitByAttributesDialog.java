/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.tools;

import java.lang.reflect.InvocationTargetException;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PlatformUI;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.process.spatialstatistics.storage.NamePolicy;
import org.geotools.process.spatialstatistics.storage.ShapeExportOperation;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.ToolboxView;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget.FileDataType;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.FieldType;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.VectorLayerType;
import org.locationtech.udig.project.IMap;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;

/**
 * Splits a layer according to attributes within the selected field producing a separate shapefile
 * for common attributes.
 * 
 * @author MapPlus
 */
public class SplitByAttributesDialog extends AbstractGeoProcessingDialog implements
        IRunnableWithProgress {
    protected static final Logger LOGGER = Logging.getLogger(SplitByAttributesDialog.class);

    private Combo cboInput, cboSplitField, cboNamePolicy;

    private Button chkPrefix;

    private Table uniqueTable;

    private NamePolicy namePolicy = NamePolicy.NORMAL;

    private SimpleFeatureCollection inputFeatures;

    public SplitByAttributesDialog(Shell parentShell, IMap map) {
        super(parentShell, map);

        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL
                | SWT.RESIZE);

        this.windowTitle = Messages.SplitByAttributesDialog_title;
        this.windowDesc = Messages.SplitByAttributesDialog_description;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(area, SWT.BORDER);
        container.setLayout(new GridLayout(2, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        // input features
        uiBuilder.createLabel(container, Messages.SplitByAttributesDialog_InputFeatures, null, 2);
        cboInput = uiBuilder.createCombo(container, 2);
        cboInput.addModifyListener(modifyListener);

        // split field
        uiBuilder.createLabel(container, Messages.SplitByAttributesDialog_SplitField, null, 1);
        cboSplitField = uiBuilder.createCombo(container, 1);
        cboSplitField.addModifyListener(modifyListener);

        chkPrefix = uiBuilder.createCheckbox(container, Messages.SplitByAttributesDialog_UsePrefix,
                null, 2);
        chkPrefix.setSelection(true);
        chkPrefix.addSelectionListener(selectionListener);

        // output layers
        Group grpTable = uiBuilder.createGroup(container,
                Messages.SplitByAttributesDialog_OutputLayers, false, 2);
        grpTable.setLayout(new GridLayout(4, false));

        String[] columns = new String[] { Messages.SplitByAttributesDialog_OutputName,
                Messages.SplitByAttributesDialog_FieldValue, Messages.SplitByAttributesDialog_Count };
        uniqueTable = uiBuilder.createEditableTable(grpTable, columns, 4, 1);

        // 4. name policy
        uiBuilder.createLabel(container, Messages.SplitByAttributesDialog_NamePolicy, null, 1);
        cboNamePolicy = uiBuilder.createCombo(container, 2);
        cboNamePolicy.addModifyListener(modifyListener);
        setComboItems(cboNamePolicy, NamePolicy.class);
        cboNamePolicy.select(0);

        locationView = new OutputDataWidget(FileDataType.FOLDER, SWT.OPEN);
        locationView.create(container, SWT.BORDER, 2, 1);
        locationView.setFolder(ToolboxView.getWorkspace()); // default location

        fillLayers(map, cboInput, VectorLayerType.ALL);

        area.pack(true);
        return area;
    }

    SelectionListener selectionListener = new SelectionAdapter() {
        @SuppressWarnings("unchecked")
        @Override
        public void widgetSelected(SelectionEvent event) {
            Widget widget = event.widget;
            if (widget.equals(chkPrefix)) {
                for (TableItem item : uniqueTable.getItems()) {
                    Entry<Object, Integer> entrySet = (Entry<Object, Integer>) item.getData();
                    String fieldName = entrySet.getKey().toString();
                    if (chkPrefix.getSelection()) {
                        fieldName = cboInput.getText() + "_" + entrySet.getKey().toString(); //$NON-NLS-1$
                    }
                    item.setText(new String[] { fieldName, entrySet.getKey().toString(),
                            entrySet.getValue().toString() });
                }
            }
        }
    };

    ModifyListener modifyListener = new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {
            Widget widget = e.widget;
            if (invalidWidgetValue(widget)) {
                return;
            }

            if (widget.equals(cboInput)) {
                inputFeatures = MapUtils.getFeatures(map, cboInput.getText());
                if (inputFeatures != null) {
                    fillFields(cboSplitField, inputFeatures.getSchema(), FieldType.ALL);
                    uniqueTable.removeAll();
                }
            } else if (widget.equals(cboSplitField)) {
                BusyIndicator.showWhile(PlatformUI.getWorkbench().getDisplay(), new Runnable() {
                    @Override
                    public void run() {
                        buildTables(cboSplitField.getText());
                    }
                });
            } else if (widget.equals(cboNamePolicy)) {
                namePolicy = NamePolicy.valueOf(cboNamePolicy.getText());
            }
        }
    };

    private void buildTables(String attributeName) {
        SortedMap<Object, Integer> valueCountsMap = new TreeMap<Object, Integer>();
        SimpleFeatureIterator featureIter = inputFeatures.features();
        try {
            while (featureIter.hasNext()) {
                final SimpleFeature feature = featureIter.next();
                Object value = feature.getAttribute(attributeName);
                if (value == null) {
                    value = "Null_Value"; //$NON-NLS-1$
                }

                if (valueCountsMap.containsKey(value)) {
                    final int cnt = valueCountsMap.get(value);
                    valueCountsMap.put(value, Integer.valueOf(cnt + 1));
                } else {
                    valueCountsMap.put(value, Integer.valueOf(1));
                }
            }
        } finally {
            featureIter.close();
        }

        uniqueTable.removeAll();
        for (Entry<Object, Integer> entrySet : valueCountsMap.entrySet()) {
            TableItem item = new TableItem(uniqueTable, SWT.NONE);
            String fieldName = entrySet.getKey().toString();
            if (this.chkPrefix.getSelection()) {
                fieldName = cboInput.getText() + "_" + entrySet.getKey().toString(); //$NON-NLS-1$
            }

            item.setText(new String[] { fieldName, entrySet.getKey().toString(),
                    entrySet.getValue().toString() });
            item.setData(entrySet);
            item.setChecked(true);
        }
    }

    @Override
    protected void okPressed() {
        if (inputFeatures == null || !existCheckedItem(uniqueTable)) {
            openInformation(getShell(), Messages.SplitByAttributesDialog_Warning);
            return;
        }

        try {
            PlatformUI.getWorkbench().getProgressService().run(false, true, this);
            openInformation(getShell(), Messages.General_Completed);
            super.okPressed();
        } catch (InvocationTargetException e) {
            MessageDialog.openError(getShell(), Messages.General_Error, e.getMessage());
        } catch (InterruptedException e) {
            MessageDialog.openInformation(getShell(), Messages.General_Cancelled, e.getMessage());
        }
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
        monitor.beginTask(String.format(Messages.Task_Executing, windowTitle),
                uniqueTable.getItems().length + 1);
        try {
            monitor.worked(increment);

            final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
            PropertyName uniqueField = ff.property(cboSplitField.getText());

            ShapeExportOperation process = new ShapeExportOperation();
            process.setOutputDataStore(locationView.getDataStore());
            process.setNamePolicy(namePolicy);

            for (TableItem item : uniqueTable.getItems()) {
                monitor.subTask(String.format(Messages.Task_Executing, item.getText()));
                if (monitor.isCanceled()) {
                    break;
                }

                if (item.getChecked()) {
                    String layerName = item.getText();
                    switch (namePolicy) {
                    case LOWERCASE:
                        layerName = layerName.toLowerCase();
                        break;
                    case UPPERCASE:
                        layerName = layerName.toUpperCase();
                        break;
                    default:
                        break;
                    }

                    final String value = item.getText(1);
                    Filter filter = ff.equal(uniqueField, ff.literal(value), true);
                    if (value.equals("Null_Value")) { //$NON-NLS-1$
                        filter = ff.isNull(uniqueField);
                    }

                    process.setOutputTypeName(layerName);
                    SimpleFeatureSource outputSfs = process.execute(inputFeatures
                            .subCollection(filter));
                    if (outputSfs == null) {
                        ToolboxPlugin.log(windowTitle + " : Failed to export : " + layerName); //$NON-NLS-1$
                    }
                }

                monitor.worked(increment);
                Thread.sleep(100);
            }
        } catch (Exception e) {
            ToolboxPlugin.log(e.getMessage());
            throw new InvocationTargetException(e.getCause(), e.getMessage());
        } finally {
            monitor.done();
        }
    }
}