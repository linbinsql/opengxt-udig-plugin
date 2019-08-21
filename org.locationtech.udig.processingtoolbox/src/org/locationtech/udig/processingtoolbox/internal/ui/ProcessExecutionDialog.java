/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.internal.ui;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.Parameter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.ToolboxView;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget.FileDataType;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.processingtoolbox.styler.ProcessExecutorOperation;
import org.locationtech.udig.processingtoolbox.tools.HtmlWriter;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.ui.internal.LayersView;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Process Execution Dialog
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ProcessExecutionDialog extends TitleAreaDialog {
    protected static final Logger LOGGER = Logging.getLogger(ProcessExecutionDialog.class);

    private IMap map;

    private org.geotools.process.ProcessFactory factory;

    private org.opengis.feature.type.Name processName;

    private String windowTitle;

    private Map<String, Object> inputParams = new HashMap<String, Object>();

    private Map<String, Object> outputParams = new HashMap<String, Object>();

    private Map<Widget, Map<String, Object>> uiParams = new HashMap<Widget, Map<String, Object>>();

    private boolean outputTabRequired = false;

    private Browser browser;

    private CTabItem inputTab, outputTab;

    public ProcessExecutionDialog(Shell parentShell, IMap map, ProcessFactory factory,
            Name processName) {
        super(parentShell);

        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.MODELESS | SWT.RESIZE);

        this.map = map;
        this.factory = factory;
        this.processName = processName;
        this.windowTitle = factory.getTitle(processName).toString();
    }

    @Override
    public void create() {
        super.create();

        setTitle(getShell().getText());
        setMessage(factory.getDescription(processName).toString());
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        IDialogSettings settings = ToolboxPlugin.getDefault().getDialogSettings();
        IDialogSettings section = DialogSettings.getOrCreateSection(settings,
                ToolboxView.PROCESS_DIALOG_SETTINGS);
        return section;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);

        newShell.setText(windowTitle);
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        // 0. Tab Folder
        final CTabFolder parentTabFolder = new CTabFolder(area, SWT.BOTTOM);
        parentTabFolder.setUnselectedCloseVisible(false);
        parentTabFolder.setLayout(new FillLayout());
        parentTabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // 1. Process execution
        inputTab = createInputTab(parentTabFolder);

        // 2. Simple output
        if (outputTabRequired) {
            outputTab = createOutputTab(parentTabFolder);
        }

        // 3. Help
        createHelpTab(parentTabFolder);

        parentTabFolder.setSelection(inputTab);

        // set maximum height
        area.pack();
        parentTabFolder.pack();

        return area;
    }

    private CTabItem createOutputTab(final CTabFolder parentTabFolder) {
        CTabItem tab = new CTabItem(parentTabFolder, SWT.NONE);
        tab.setText(Messages.ProcessExecutionDialog_taboutput);

        try {
            browser = new Browser(parentTabFolder, SWT.NONE);
            GridData layoutData = new GridData(GridData.FILL_BOTH);
            browser.setLayoutData(layoutData);
            tab.setControl(browser);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }

        return tab;
    }

    private CTabItem createHelpTab(final CTabFolder parentTabFolder) {
        CTabItem tab = new CTabItem(parentTabFolder, SWT.NONE);
        tab.setText(Messages.ProcessExecutionDialog_tabhelp);

        try {
            Browser browser = new Browser(parentTabFolder, SWT.NONE);
            GridData layoutData = new GridData(GridData.FILL_BOTH);
            browser.setLayoutData(layoutData);

            HtmlWriter writer = new HtmlWriter(windowTitle);
            writer.writeProcessMetadata(factory, processName);
            browser.setText(writer.getHTML());
            tab.setControl(browser);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }

        
        return tab;
    }

    private CTabItem createInputTab(final CTabFolder parentTabFolder) {
        CTabItem tab = new CTabItem(parentTabFolder, SWT.NONE);
        tab.setText(Messages.ProcessExecutionDialog_tabparameters);

        ScrolledComposite scroller = new ScrolledComposite(parentTabFolder, SWT.NONE | SWT.V_SCROLL
                | SWT.H_SCROLL);
        scroller.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite container = new Composite(scroller, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        // input parameters
        Map<String, Parameter<?>> paramInfo = factory.getParameterInfo(processName);
        for (Entry<String, Parameter<?>> entrySet : paramInfo.entrySet()) {
            if (ToolboxView.getMandatoryParameterOnly() && entrySet.getValue().getMinOccurs() == 0) {
                continue;
            }
            inputParams.put(entrySet.getValue().key, entrySet.getValue().sample);
            insertControl(container, entrySet.getValue());
        }

        // output location
        Map<String, Parameter<?>> resultInfo = factory.getResultInfo(processName, null);
        final int outputCnt = resultInfo.size();
        for (Entry<String, Parameter<?>> entrySet : resultInfo.entrySet()) {
            Class<?> binding = entrySet.getValue().type;
            boolean outputWidgetRequired = false;
            FileDataType fileDataType = FileDataType.SHAPEFILE;
            if (SimpleFeatureCollection.class.isAssignableFrom(binding)) {
                outputWidgetRequired = true;
                fileDataType = FileDataType.SHAPEFILE;
            } else if (Geometry.class.isAssignableFrom(binding)) {
                outputWidgetRequired = true;
                fileDataType = FileDataType.SHAPEFILE;
            } else if (ReferencedEnvelope.class.isAssignableFrom(binding)) {
                outputWidgetRequired = true;
                fileDataType = FileDataType.SHAPEFILE;
            } else if (BoundingBox.class.isAssignableFrom(binding)) {
                outputWidgetRequired = true;
                fileDataType = FileDataType.SHAPEFILE;
            } else if (GridCoverage2D.class.isAssignableFrom(binding)) {
                outputWidgetRequired = true;
                fileDataType = FileDataType.RASTER;
            } else {
                outputTabRequired = true;
            }

            if (outputWidgetRequired) {
                outputParams.put(entrySet.getValue().key, entrySet.getValue().sample);
                OutputLocationWidget view = new OutputLocationWidget(fileDataType, SWT.SAVE);
                view.create(container, SWT.BORDER, outputParams, entrySet.getValue());

                if (outputCnt > 1) {
                    view.setOutputName(entrySet.getKey());
                } else {
                    view.setOutputName(processName.getLocalPart().toLowerCase());
                }
            }
        }

        scroller.setContent(container);
        tab.setControl(scroller);

        scroller.pack();
        container.pack();

        scroller.setMinSize(450, container.getSize().y - 2);
        scroller.setExpandVertical(true);
        scroller.setExpandHorizontal(true);

        return tab;
    }

    private void insertParamTitle(Composite container, final Parameter<?> param) {
        String postfix = ""; //$NON-NLS-1$
        if (param.type.isAssignableFrom(Geometry.class)) {
            postfix = "(WKT)"; //$NON-NLS-1$
        }

        // capitalize title
        String title = param.title.toString();
        title = Character.toUpperCase(title.charAt(0)) + title.substring(1);

        CLabel lblName = new CLabel(container, SWT.NONE);
        lblName.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));

        FontData[] fontData = lblName.getFont().getFontData();
        for (int i = 0; i < fontData.length; ++i) {
            fontData[i].setStyle(SWT.NORMAL);
        }
        lblName.setFont(new Font(container.getDisplay(), fontData));

        if (param.required) {
            Image image = ToolboxPlugin.getImageDescriptor("icons/public_co.gif").createImage(); //$NON-NLS-1$
            lblName.setImage(image);
            lblName.setText(title + postfix);
        } else {
            lblName.setText(title + Messages.ProcessExecutionDialog_optional + postfix);
        }
        lblName.setToolTipText(param.description.toString());
    }

    private void insertControl(Composite container, final Parameter<?> param) {
        // process name
        insertParamTitle(container, param);

        // process value
        if (param.maxOccurs > 1) {
            if (SimpleFeatureCollection.class.isAssignableFrom(param.type)
                    || FeatureCollection.class.isAssignableFrom(param.type)) {
                // vector layers parameters
                FeatureCollectionsDataWidget featuresView = new FeatureCollectionsDataWidget(map);
                featuresView.create(container, SWT.NONE, inputParams, param);
            } else if (GridCoverage2D.class.isAssignableFrom(param.type)) {
                // raster layers parameters
                GridCoveragesDataWidget gridCoverageView = new GridCoveragesDataWidget(map);
                gridCoverageView.create(container, SWT.NONE, inputParams, param);
            } else if (Geometry.class.isAssignableFrom(param.type)) {
                // wkt geometries parameters
                GeometryWidget geometryView = new GeometryWidget(map);
                geometryView.create(container, SWT.NONE, inputParams, param);
            } else {
                Map<String, Object> metadata = param.metadata;
                if (metadata == null || metadata.size() == 0) {
                    // other literal parameters
                    LiteralDataWidget literalView = new LiteralDataWidget(map);
                    literalView.create(container, SWT.NONE, inputParams, param);
                } else {
                    if (metadata.containsKey(Params.FIELD)) {
                        LiteralDataFieldWidget fieldView = new LiteralDataFieldWidget(map);
                        fieldView.create(container, SWT.NONE, inputParams, param, uiParams);
                    } else if (metadata.containsKey(Params.FIELDS)) {
                        LiteralDataFieldsWidget fieldView = new LiteralDataFieldsWidget(map);
                        fieldView.create(container, SWT.NONE, inputParams, param, uiParams);
                    } else {
                        // other literal parameters
                        LiteralDataWidget literalView = new LiteralDataWidget(map);
                        literalView.create(container, SWT.NONE, inputParams, param);
                    }
                }
            }
            return;
        }

        if (SimpleFeatureCollection.class.isAssignableFrom(param.type)
                || FeatureCollection.class.isAssignableFrom(param.type)) {
            // vector layer parameters
            FeatureCollectionDataWidget featuresView = new FeatureCollectionDataWidget(map);
            featuresView.create(container, SWT.NONE, inputParams, param, uiParams);
        } else if (GridCoverage2D.class.isAssignableFrom(param.type)) {
            // raster layer parameters
            GridCoverageDataWidget gridCoverageView = new GridCoverageDataWidget(map);
            gridCoverageView.create(container, SWT.NONE, inputParams, param);
        } else if (Geometry.class.isAssignableFrom(param.type)) {
            // wkt geometry parameters
            GeometryWidget geometryView = new GeometryWidget(map);
            geometryView.create(container, SWT.NONE, inputParams, param);
        } else if (Filter.class.isAssignableFrom(param.type)) {
            // filter parameters
            FilterWidget filterView = new FilterWidget(map);
            filterView.create(container, SWT.NONE, inputParams, param);
        } else if (Expression.class.isAssignableFrom(param.type)) {
            // expression parameters
            ExpressionWidget expressionView = new ExpressionWidget(map);
            expressionView.create(container, SWT.NONE, inputParams, param, uiParams);
        } else if (CoordinateReferenceSystem.class.isAssignableFrom(param.type)) {
            // coordinate reference system parameters
            CrsWidget crsView = new CrsWidget(map);
            crsView.create(container, SWT.NONE, inputParams, param);
        } else if (BoundingBox.class.isAssignableFrom(param.type)) {
            // bounding box parameters
            BoundingBoxWidget boundingBoxView = new BoundingBoxWidget(map);
            boundingBoxView.create(container, SWT.NONE, inputParams, param);
        } else if (param.type.isEnum()) {
            // enumeration parameters
            EnumDataWidget enumView = new EnumDataWidget();
            enumView.create(container, SWT.NONE, inputParams, param);
        } else if (Number.class.isAssignableFrom(param.type)) {
            // number parameters = Byte, Double, Float, Integer, Long, Short
            if (Double.class.isAssignableFrom(param.type)
                    || Float.class.isAssignableFrom(param.type)) {
                NumberDataWidget numberView = new NumberDataWidget(map);
                numberView.create(container, SWT.NONE, inputParams, param);
            } else {
                IntegerDataWidget integerView = new IntegerDataWidget(map);
                integerView.create(container, SWT.NONE, inputParams, param);
            }
        } else if (Boolean.class.isAssignableFrom(param.type)
                || boolean.class.isAssignableFrom(param.type)) {
            // boolean parameters
            BooleanDataWidget booleanView = new BooleanDataWidget();
            booleanView.create(container, SWT.NONE, inputParams, param);
        } else {
            Map<String, Object> metadata = param.metadata;
            if (metadata == null || metadata.size() == 0) {
                // other literal parameters
                LiteralDataWidget literalView = new LiteralDataWidget(map);
                literalView.create(container, SWT.NONE, inputParams, param);
            } else {
                if (metadata.containsKey(Params.FIELD)) {
                    LiteralDataFieldWidget fieldView = new LiteralDataFieldWidget(map);
                    fieldView.create(container, SWT.NONE, inputParams, param, uiParams);
                } else if (metadata.containsKey(Params.FIELDS)) {
                    LiteralDataFieldsWidget fieldView = new LiteralDataFieldsWidget(map);
                    fieldView.create(container, SWT.NONE, inputParams, param, uiParams);
                } else {
                    // other literal parameters
                    LiteralDataWidget literalView = new LiteralDataWidget(map);
                    literalView.create(container, SWT.NONE, inputParams, param);
                }
            }
        }
    }

    private boolean validOutput() {
        for (Entry<String, Object> entrySet : outputParams.entrySet()) {
            File outputFile = new File(outputParams.get(entrySet.getKey()).toString());
            if (!outputFile.exists()) {
                continue;
            }

            String msg = Messages.ProcessExecutionDialog_overwriteconfirm;
            if (MessageDialog.openConfirm(getParentShell(), getShell().getText(), msg)) {
                if (!MapUtils.confirmSpatialFile(outputFile)) {
                    msg = Messages.ProcessExecutionDialog_deletefailed;
                    MessageDialog.openInformation(getParentShell(), getShell().getText(), msg);
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("nls")
    @Override
    protected void okPressed() {
        // check required parameters
        Map<String, Parameter<?>> paramInfo = factory.getParameterInfo(processName);
        for (Entry<String, Parameter<?>> entrySet : paramInfo.entrySet()) {
            String key = entrySet.getValue().key;
            if (entrySet.getValue().required && inputParams.get(key) == null) {
                StringBuffer sb = new StringBuffer(Messages.ProcessExecutionDialog_requiredparam);
                sb.append(System.getProperty("line.separator")).append(" - ");
                sb.append(entrySet.getValue().getTitle());
                MessageDialog.openInformation(getParentShell(), windowTitle, sb.toString());
                return;
            }
        }

        // check output files
        if (!validOutput()) {
            return;
        }

        ProcessExecutorOperation runnable = new ProcessExecutorOperation(map, factory, processName,
                inputParams, outputParams);
        try {
            new ProgressMonitorDialog(getParentShell()).run(true, true, runnable);
        } catch (InvocationTargetException e) {
            ToolboxPlugin.log(e.getMessage());
            MessageDialog.openError(getParentShell(), Messages.General_Error, e.getMessage());
        } catch (InterruptedException e) {
            ToolboxPlugin.log(e.getMessage());
            MessageDialog.openInformation(getParentShell(), Messages.General_Cancelled,
                    e.getMessage());
        }

        if (outputTabRequired) {
            String outputText = runnable.getOutputText();
            if (outputText.length() > 0) {
                outputTab.getParent().setSelection(outputTab);
                browser.setText(outputText);
            }
        } else {
            super.okPressed();
            LayersView.getViewPart()
                    .setCurrentMap((org.locationtech.udig.project.internal.Map) map);
        }
    }
}
