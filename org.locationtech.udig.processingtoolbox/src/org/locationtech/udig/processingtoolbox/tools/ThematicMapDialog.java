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

import java.awt.Color;
import java.util.logging.Logger;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Spinner;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.FeatureTypes.SimpleShapeType;
import org.geotools.process.spatialstatistics.styler.GraduatedColorStyleBuilder;
import org.geotools.process.spatialstatistics.styler.GraduatedSymbolStyleBuilder;
import org.geotools.process.spatialstatistics.styler.SSStyleBuilder;
import org.geotools.styling.Fill;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.FieldType;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.VectorLayerType;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.style.sld.SLDContent;
import org.locationtech.udig.style.sld.editor.BorderColorComboListener.Outline;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Histogram Dialog
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ThematicMapDialog extends AbstractThematicMapDialog {
    protected static final Logger LOGGER = Logging.getLogger(ThematicMapDialog.class);

    private Button chkSymbol;

    private Combo cboLayer, cboField, cboMethod, cboNormal, cboOutline;

    private Spinner spnClass, spnTransparency, spnLineWidth, spnMin, spnMax;

    private Slider sldTransparency;

    public ThematicMapDialog(Shell parentShell, IMap map) {
        super(parentShell, map);

        this.windowTitle = Messages.ThematicMapDialog_title;
        this.windowDesc = Messages.ThematicMapDialog_description;
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new GridLayout(6, false));
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Image image = ToolboxPlugin.getImageDescriptor("icons/public_co.gif").createImage(); //$NON-NLS-1$

        // Layer
        uiBuilder.createLabel(container, Messages.ThematicMapDialog_InputLayer, EMPTY, image, 1);
        cboLayer = uiBuilder.createCombo(container, 5, true);
        fillLayers(map, cboLayer, VectorLayerType.ALL);

        // Field
        uiBuilder.createLabel(container, Messages.ThematicMapDialog_InputField, EMPTY, image, 1);
        cboField = uiBuilder.createCombo(container, 2, true);

        uiBuilder.createLabel(container, Messages.ThematicMapDialog_Normalization, EMPTY, image, 1);
        cboNormal = uiBuilder.createCombo(container, 2, true);

        // Class
        uiBuilder.createLabel(container, Messages.ThematicMapDialog_Mode, EMPTY, image, 1);
        cboMethod = uiBuilder.createCombo(container, 2, true);

        uiBuilder.createLabel(container, Messages.ThematicMapDialog_Classes, EMPTY, image, 1);
        spnClass = uiBuilder.createSpinner(container, 5, 2, 12, 0, 1, 1, 2);

        // Ramp
        uiBuilder.createLabel(container, Messages.ThematicMapDialog_ColorRamp, EMPTY, image, 1);
        cboColorRamp = uiBuilder.createCombo(container, 5, true);

        // Preview ramp
        uiBuilder.createLabel(container, EMPTY, EMPTY, null, 1);
        lblPreview = new Label(container, SWT.NONE);
        lblPreview.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 5, 1));

        // Reverse color ramp
        uiBuilder.createLabel(container, EMPTY, EMPTY, null, 1);
        chkReverse = uiBuilder.createCheckbox(container, Messages.ThematicMapDialog_Reverse, EMPTY,
                5);

        // Graduated Symbol
        uiBuilder.createLabel(container, EMPTY, EMPTY, null, 1);
        chkSymbol = uiBuilder
                .createCheckbox(container, Messages.ThematicMapDialog_Symbol, EMPTY, 1);
        uiBuilder.createLabel(container, Messages.ThematicMapDialog_Minimum, EMPTY, null, 1);
        spnMin = uiBuilder.createSpinner(container, 1, 0, 5, 0, 1, 1, 1);
        uiBuilder.createLabel(container, Messages.ThematicMapDialog_Maximum, EMPTY, null, 1);
        spnMax = uiBuilder.createSpinner(container, 0, 5, 200, 0, 1, 1, 1);

        // transparency
        uiBuilder.createLabel(container, Messages.ThematicMapDialog_Transparency, EMPTY, image, 1);
        Composite subCon = new Composite(container, SWT.NONE);
        subCon.setLayout(uiBuilder.createGridLayout(5, false, 0, 5));
        subCon.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1));

        sldTransparency = uiBuilder.createSlider(subCon, 10, 0, 100, 0, 1, 10, 4);

        spnTransparency = new Spinner(subCon, SWT.RIGHT_TO_LEFT | SWT.BORDER);
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
        gridData.widthHint = 20;
        spnTransparency.setLayoutData(gridData);
        spnTransparency.setValues(10, 0, 100, 0, 1, 10);

        // outline
        uiBuilder.createLabel(container, Messages.ThematicMapDialog_OutlineColor, EMPTY, image, 1);
        cboOutline = uiBuilder.createCombo(container, 2, true);

        uiBuilder.createLabel(container, Messages.ThematicMapDialog_OutlineWidth, EMPTY, image, 1);
        spnLineWidth = uiBuilder.createSpinner(container, 5, 0, 100, 1, 1, 10, 2);

        cboOutline.setItems(Outline.labels());
        cboOutline.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                int selection = cboOutline.getSelectionIndex();
                if (selection == 0) { // None
                    cboOutline.setData(java.awt.Color.GRAY);
                } else if (selection == 1) { // Black
                    cboOutline.setData(java.awt.Color.BLACK);
                } else if (selection == 2) { // White
                    cboOutline.setData(java.awt.Color.WHITE);
                } else if (selection == 3) { // Custom
                    ColorDialog cd = new ColorDialog(parent.getShell());
                    cd.setText("Select Custom Color"); //$NON-NLS-1$
                    if (cboOutline.getData() != null) {
                        java.awt.Color c = (java.awt.Color) cboOutline.getData();
                        cd.setRGB(new RGB(c.getRed(), c.getGreen(), c.getBlue()));
                    }
                    RGB rgb = cd.open();
                    if (rgb != null) {
                        cboOutline.setData(new java.awt.Color(rgb.red, rgb.green, rgb.blue));
                    }
                }
            }
        });
        cboOutline.select(2);

        cboLayer.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (cboLayer.getSelectionIndex() == -1) {
                    return;
                }
                activeLayer = MapUtils.getLayer(map, cboLayer.getText());
                if (activeLayer != null) {
                    fillFields(cboField, activeLayer.getSchema(), FieldType.Number);
                    fillFields(cboNormal, activeLayer.getSchema(), FieldType.Number);
                    cboNormal.add(EMPTY);
                    cboNormal.select(cboNormal.getItemCount() - 1);
                    cboMethod.removeAll();

                    if (FeatureTypes.getSimpleShapeType(activeLayer.getSchema()) == SimpleShapeType.LINESTRING) {
                        spnMax.setSelection(5);
                    } else {
                        spnMax.setSelection(50);
                    }
                }
            }
        });

        cboField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (cboField.getSelectionIndex() == -1) {
                    return;
                }
                boolean numeric = FeatureTypes.isNumeric(activeLayer.getSchema(),
                        cboField.getText());
                cboNormal.setEnabled(numeric);
                if (numeric) {
                    cboMethod.setItems(NUMERIC_METHOD);
                } else {
                    cboMethod.setItems(CATEGORY_METHOD);
                }
                cboMethod.select(0);
            }
        });

        chkReverse.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                updatePreview();
            }
        });

        sldTransparency.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                spnTransparency.setSelection(sldTransparency.getSelection());
            }
        });

        spnTransparency.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                sldTransparency.setSelection(spnTransparency.getSelection());
            }
        });

        area.pack(true);

        cboColorRamp.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                updatePreview();
            }
        });

        updateColorRamp(0, 5);
        
        return area;
    }

    @Override
    protected void okPressed() {
        if (invalidWidgetValue(cboLayer, cboField)) {
            openInformation(getShell(), Messages.Task_ParameterRequired);
            return;
        }

        Runnable runnable = new Runnable() {
            @Override
            @SuppressWarnings({})
            public void run() {

                float opacity = (100 - spnTransparency.getSelection()) / 100f;
                String fieldName = cboField.getText();
                String normalProperty = cboNormal.getText();
                String palette = cboColorRamp.getItem(cboColorRamp.getSelectionIndex());
                String paletteName = palette.split("\\(")[0]; //$NON-NLS-1$
                boolean reverse = chkReverse.getSelection();

                java.awt.Color outlineColor = (java.awt.Color) cboOutline.getData();
                float outlineWidth = spnLineWidth.getSelection() / (10f * spnLineWidth.getDigits());
                if (cboOutline.getSelectionIndex() == 0) {
                    outlineWidth = 0f;
                }

                int numClasses = spnClass.getSelection();
                String styleName = cboMethod.getText().toUpperCase();
                String functionName = getFunctionName(styleName);

                SimpleFeatureCollection features = MapUtils.getFeatures(activeLayer);
                SimpleFeatureType schema = activeLayer.getSchema();

                // create default style
                SSStyleBuilder ssBuilder = new SSStyleBuilder(schema);
                ssBuilder.setOpacity(opacity);
                Style style = ssBuilder.getDefaultFeatureStyle();

                // crate thematic style
                if (chkSymbol.getSelection()) {
                    double minSize = uiBuilder.fromSpinnerValue(spnMin, spnMin.getSelection());
                    double maxSize = uiBuilder.fromSpinnerValue(spnMax, spnMax.getSelection());

                    GraduatedSymbolStyleBuilder builder = new GraduatedSymbolStyleBuilder();
                    builder.setMinSize((float) minSize);
                    builder.setMaxSize((float) maxSize);
                    builder.setFillOpacity(opacity);
                    builder.setOutlineColor(outlineColor);
                    builder.setOutlineWidth(outlineWidth);
                    if (outlineWidth == 0) {
                        builder.setLineOpacity(0.0f);
                    }
                    builder.setNormalProperty(normalProperty);

                    // background symbol
                    if (FeatureTypes.getSimpleShapeType(schema) == SimpleShapeType.POLYGON) {
                        String geom = schema.getGeometryDescriptor().getLocalName();
                        Stroke stroke = sf.createStroke(ff.literal(outlineColor),
                                ff.literal(opacity));
                        Fill fill = sf.createFill(ff.literal(new Color(234, 234, 234)),
                                ff.literal(0.5f));
                        PolygonSymbolizer backgroundSymbol = sf.createPolygonSymbolizer(stroke,
                                fill, geom);
                        builder.setBackgroundSymbol(backgroundSymbol);
                    }

                    style = builder.createStyle(features, fieldName, functionName, numClasses,
                            paletteName, reverse);
                } else {
                    GraduatedColorStyleBuilder builder = new GraduatedColorStyleBuilder();
                    builder.setFillOpacity(opacity);
                    builder.setOutlineColor(outlineColor);
                    builder.setOutlineWidth(outlineWidth);
                    if (outlineWidth == 0) {
                        builder.setLineOpacity(0.0f);
                    }
                    builder.setNormalProperty(normalProperty);

                    style = builder.createStyle(features, fieldName, functionName, numClasses,
                            paletteName, reverse);
                }

                if (style != null) {
                    // put the style on the blackboard
                    activeLayer.getStyleBlackboard().clear();
                    activeLayer.getStyleBlackboard().put(SLDContent.ID, style);
                    activeLayer.getStyleBlackboard().flush();
                    activeLayer.refresh(activeLayer.getBounds(new NullProgressMonitor(), null));
                }
            }
        };

        try {
            BusyIndicator.showWhile(Display.getCurrent(), runnable);
        } catch (Exception e) {
            ToolboxPlugin.log(e);
            MessageDialog.openError(getParentShell(), Messages.General_Error, e.getMessage());
        }
    }
}
