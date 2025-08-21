package shactor;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.progressbar.ProgressBarVariant;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.StreamResource;
import cs.qse.common.structure.NS;
import cs.qse.common.structure.PS;
import cs.qse.common.structure.ShaclOrListItem;
import cs.qse.filebased.Parser;
import cs.qse.querybased.nonsampling.QbParser;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.vaadin.olli.FileDownloadWrapper;
import shactor.utils.ChartsUtil;
import shactor.utils.DialogUtil;
import shactor.utils.PruningUtil;
import shactor.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static shactor.utils.ChartsUtil.*;
import static shactor.utils.Utils.*;


@Tag("extraction-view")
@JsModule("./extraction-view.ts")
@CssImport(value = "./grid.css", themeFor = "vaadin-grid")
@Route("/extraction-view")
public class ExtractionView extends LitTemplate {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractionView.class);
    private static final String FORMAT_SHACL = "SHACL";
    private static final String FORMAT_SHEX = "ShEx";

    @Id("contentVerticalLayout")
    private VerticalLayout contentVerticalLayout;
    
    @Id("stepCounter")
    private H4 stepCounter;
    
    @Id("extractedShapesText")
    private Paragraph extractedShapesText;

    @Id("supportTextField")
    private TextField supportTextField;
    @Id("confidenceTextField")
    private TextField confidenceTextField;
    @Id("startPruningButton")
    private Button startPruningButton;
    @Id("shapesGrid")
    private Grid<NS> shapesGrid;
    @Id("propertyShapesGrid")
    private Grid<PS> propertyShapesGrid;
    @Id("propertyShapesGridInfo")
    private H5 propertyShapesGridInfo;

    @Id("downloadSelectedShapesButton")
    private Button downloadSelectedShapesButton;

    private final PruningUtil pruningUtil = new PruningUtil();

    // PostProcessing Configuration
    @Value("${shactor.postprocessing.enabled:true}")
    private boolean postProcessingEnabled;

    String currNodeShape;
    String prunedFileAddress = "";
    List<NS> prunedNodeShapes = null; // Store pruned NodeShapes for format-aware download

    @Id("headingPieCharts")
    private H2 headingPieCharts;
    @Id("headingNodeShapesAnalysis")
    private H2 headingNodeShapesAnalysis;
    @Id("vaadinRadioGroup")
    private RadioButtonGroup<String> vaadinRadioGroup;
    @Id("psVaadinRadioGroup")
    private RadioButtonGroup<String> psVaadinRadioGroup;
    @Id("psGridRadioButtonInfo")
    private Paragraph psGridRadioButtonInfo;
    @Id("nsGridRadioButtonInfo")
    private Paragraph nsGridRadioButtonInfo;
    static PS currPS;
    static NS currNS;
    static Integer support;
    static Double confidence;
    @Id("actionButtonsHorizontalLayout")
    private HorizontalLayout actionButtonsHorizontalLayout;
    @Id("nsSearchField")
    private TextField nsSearchField;
    @Id("psSearchField")
    private TextField psSearchField;
    @Id("chartsContainerHorizontalLayout")
    private HorizontalLayout chartsContainerHorizontalLayout;
    @Id("soChartsContainerHorizontalLayout")
    private HorizontalLayout soChartsContainerHorizontalLayout;
    @Id("vl1")
    private VerticalLayout vl1;
    @Id("vl2")
    private VerticalLayout vl2;
    @Id("vl3")
    private VerticalLayout vl3;
    @Id("vl4")
    private VerticalLayout vl4;
    @Id("graphStatsVerticalLayout")
    private VerticalLayout graphStatsVerticalLayout;
    @Id("splitLayout")
    private SplitLayout splitLayout;
    @Id("graphStatsHeading")
    private Paragraph graphStatsHeading;
    @Id("pruningParamsHorizontalLayout")
    private HorizontalLayout pruningParamsHorizontalLayout;

    public ExtractionView() {
        chartsContainerHorizontalLayout.removeAll();
        soChartsContainerHorizontalLayout.setVisible(false);
        
        // Set dynamic text based on selected format
        updateDynamicText();

        if (SelectionView.computeStats) {
            if (IndexView.category.equals(IndexView.Category.CONNECT_END_POINT)) {
                //Utils.notifyMessage("Computer Stats over Endpoint (TODO)");
                graphStatsVerticalLayout.setVisible(false);
                splitLayout.setSplitterPosition(100);
            } else {
                splitLayout.setSplitterPosition(60);
                graphStatsVerticalLayout.add(buildBarChartUsingDatasetsStats(IndexView.selectedDataset));
            }
        } else {
            graphStatsVerticalLayout.setVisible(false);
            splitLayout.setSplitterPosition(100);
        }

        //Utils.setFooterImagesPath(footerLeftImage, footerRightImage);
        nsSearchField.setVisible(false);
        psSearchField.setVisible(false);
        psGridRadioButtonInfo.setVisible(false);
        nsGridRadioButtonInfo.setVisible(false);
        downloadSelectedShapesButton.setVisible(false);
        vaadinRadioGroup.setVisible(false);
        psVaadinRadioGroup.setVisible(false);
        headingPieCharts.setVisible(false);
        headingNodeShapesAnalysis.setVisible(false);
        shapesGrid.setVisible(false);
        propertyShapesGrid.setVisible(false);
        propertyShapesGridInfo.setVisible(false);

        // Restrict inputs to digits only (no whitespace or other characters)
        supportTextField.setPattern("\\d+");
        supportTextField.setPreventInvalidInput(true);
        supportTextField.setAllowedCharPattern("[0-9]");
        confidenceTextField.setPattern("\\d+");
        confidenceTextField.setPreventInvalidInput(true);
        confidenceTextField.setAllowedCharPattern("[0-9]");

        configureButtonWithFileWrapper(VaadinIcon.BAR_CHART, "Download Shapes Statistics", SelectionView.outputDirectory + SelectionView.buildDatasetName(IndexView.category) + ".csv");
        configureButtonWithFileWrapper(VaadinIcon.TIMER, "Download SHACTOR extraction logs", SelectionView.outputDirectory + SelectionView.buildDatasetName(IndexView.category) + "_RUNTIME_LOGS.csv");
        // Download Shapes button will be configured after parser is available in beginPruning()
        //Utils.setIconForButtonWithToolTip(readShapesStatsButton, VaadinIcon.BAR_CHART, "Download Shapes Statistics");
        //Utils.setIconForButtonWithToolTip(readShactorLogsButton, VaadinIcon.TIMER, "Download SHACTOR extraction logs");
        //Utils.setIconForButtonWithToolTip(taxonomyVisualizationButton, VaadinIcon.FILE_TREE, "Visualize Shapes Taxonomy");

        /*
        taxonomyVisualizationButton.setVisible(false);
        taxonomyVisualizationButton.addClickListener(buttonClickEvent -> {
            RouterLink link = new RouterLink("taxonomy-view", TaxonomyView.class);
            taxonomyVisualizationButton.getUI().ifPresent(ui -> ui.getPage().open(link.getHref()));
        });*/

        startPruningButton.addClickListener(buttonClickEvent -> {
            if (supportTextField.getValue().isEmpty() || confidenceTextField.getValue().isEmpty()) {
                Utils.notify("Please enter valid values!", NotificationVariant.LUMO_ERROR, Notification.Position.TOP_CENTER);
            } else {
                beginPruning();
                // Download Reliable Shapes button will be configured after prunedNodeShapes is set in beginPruning()
            }
        });
    }

    /**
     * Updates dynamic text elements based on the selected format (SHACL or ShEx)
     */
    private void updateDynamicText() {
        String formatName = IndexView.selectedFormat != null ? IndexView.selectedFormat : FORMAT_SHACL;
        String shapesType = formatName.equals(FORMAT_SHEX) ? "ShExC shapes" : "SHACL shapes";
        
        // Update the extracted shapes text
        if (extractedShapesText != null) {
            extractedShapesText.setText("SHACTOR has extracted " + shapesType + " for the chosen classes. You have the following options:");
        }
    }

    private void configureButtonWithFileWrapper(VaadinIcon vaadinIcon, String label, String fileAddress) {
        LOG.debug("Downloading file wrapper for: {}", fileAddress);
        Button button = new Button();
        Utils.setIconForButtonWithToolTip(button, vaadinIcon, label);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        FileDownloadWrapper buttonWrapper;
        try {
            File file = new File(fileAddress);
            ByteArrayInputStream stream = new ByteArrayInputStream(FileUtils.readFileToByteArray(file));
            buttonWrapper = new FileDownloadWrapper(new StreamResource(file.getName(), () -> stream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        buttonWrapper.wrapComponent(button);
        actionButtonsHorizontalLayout.add(buttonWrapper);
    }

    /**
     * Configures the "Download Shapes" button with lazy content generation.
     * 
     * This method implements a pure lazy-generation approach without legacy file fallbacks.
     * Content is generated on-demand when the download button is clicked, ensuring
     * always fresh and format-conformant output (Turtle/SHACL compliant).
     * 
     * Key features:
     * - Button is only enabled when parser and shapesExtractor are available
     * - Content is generated lazily via StreamResource supplier (no pre-generation)
     * - Format-aware filename and content generation (SHACL .ttl vs ShEx .shex)
     * - No fallbacks to legacy files - eliminates old formatting issues
     */
    private void configureFormatAwareDownloadShapesButton() {
        Button button = new Button();
        Utils.setIconForButtonWithToolTip(button, VaadinIcon.DOWNLOAD, "Download Shapes");
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Determine format-aware filename based on selected format
        String formatName = IndexView.selectedFormat != null ? IndexView.selectedFormat : FORMAT_SHACL;
        String fileExtension = formatName.equals(FORMAT_SHEX) ? "shex" : "ttl";
        String fileName = "shapes." + fileExtension;

        // Enable button only when shapes generator (parser + shapesExtractor) is available
        // This prevents downloads with stale or unavailable data
        boolean ready = (parser != null && parser.shapesExtractor != null);
        button.setEnabled(ready);
        
        // Add tooltip to explain when button is disabled
        if (!ready) {
            button.getElement().setAttribute("title", "Please run extraction first to enable shapes download");
        }

        // Create StreamResource with lazy content generation
        // Content is generated only when download is requested, ensuring fresh output
        StreamResource resource = new StreamResource(fileName, () -> {
            // Double-check availability at download time (defensive programming)
            if (parser == null || parser.shapesExtractor == null) {
                throw new RuntimeException("Shapes generator is not initialized yet. Please run extraction first.");
            }
            
            // Generate format-aware content using current node shapes from extractor
            // This eliminates legacy issues: correct sh:nodeKind casing, typed numeric literals, etc.
            String syntax = Utils.constructModelForGivenNodeShapesAndTheirPropertyShapes(
                new HashSet<>(parser.shapesExtractor.getNodeShapes()), 
                formatName
            );
            
            // Conditionally apply post-processing fix for remaining issues (only for SHACL/Turtle)
            if (postProcessingEnabled && !FORMAT_SHEX.equals(formatName)) {
                syntax = postProcessTurtleContent(syntax);
            }
            
            // Return content as UTF-8 byte stream for download
            return new ByteArrayInputStream(syntax.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        });

        // Wrap button with FileDownloadWrapper for proper Vaadin download handling
        FileDownloadWrapper buttonWrapper = new FileDownloadWrapper(resource);
        buttonWrapper.wrapComponent(button);
        actionButtonsHorizontalLayout.add(buttonWrapper);
    }

    /**
     * Configures the "Download Reliable Shapes" button with lazy content generation.
     * 
     * This method implements pure lazy-generation for pruned (reliable) shapes without 
     * any legacy file fallbacks. Content is generated on-demand when the download button 
     * is clicked, ensuring always fresh and format-conformant output.
     * 
     * Key features:
     * - Button is only enabled when prunedNodeShapes are available (post-pruning)
     * - Content is generated lazily via StreamResource supplier (no pre-generation)
     * - Format-aware filename and content generation (reliable_shapes.ttl vs .shex)
     * - No fallbacks to prunedFileAddress - eliminates legacy formatting issues
     * - Proper error handling with clear user feedback
     */
    private void configurePrunedShapesDownloadButton() {
        Button button = new Button();
        button.setText("Download Reliable Shapes");
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Determine format-aware filename based on selected format
        String formatName = IndexView.selectedFormat != null ? IndexView.selectedFormat : "SHACL";
        String fileExtension = formatName.equals("ShEx") ? "shex" : "ttl";
        String fileName = "reliable_shapes." + fileExtension;

        // Enable button only when pruned shapes data is available
        // This ensures downloads contain meaningful pruned content
        boolean ready = (this.prunedNodeShapes != null && !this.prunedNodeShapes.isEmpty());
        button.setEnabled(ready);
        
        // Add helpful tooltip explaining what this download contains and when it is enabled
        String reliableHint = "Downloads only shapes that satisfy the configured Support and Confidence thresholds.";
        String tooltipText = ready ? reliableHint : reliableHint + " Please run pruning first to enable reliable shapes download.";
        button.getElement().setAttribute("title", tooltipText);

        // Create StreamResource with lazy content generation
        // Content is generated only when download is requested, ensuring fresh output
        StreamResource resource = new StreamResource(fileName, () -> {
            // Double-check availability at download time (defensive programming)
            if (this.prunedNodeShapes == null || this.prunedNodeShapes.isEmpty()) {
                throw new RuntimeException("Pruned shapes are not available yet. Please run pruning first.");
            }
            
            // Generate format-aware content using current pruned node shapes
            // This eliminates legacy issues: correct sh:nodeKind casing, typed numeric literals, etc.
            String syntax = Utils.constructModelForGivenNodeShapesAndTheirPropertyShapes(
                new HashSet<>(this.prunedNodeShapes), 
                formatName
            );
            
            // Conditionally apply post-processing fix for remaining issues (only for SHACL/Turtle)
            if (postProcessingEnabled && !FORMAT_SHEX.equals(formatName)) {
                syntax = postProcessTurtleContent(syntax);
            }
            
            // Return content as UTF-8 byte stream for download
            return new ByteArrayInputStream(syntax.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        });

        // Wrap button with FileDownloadWrapper for proper Vaadin download handling
        FileDownloadWrapper buttonWrapper = new FileDownloadWrapper(resource);
        buttonWrapper.getStyle().set("align-self", "end");
        // Mirror the same tooltip on the wrapper to ensure the hint appears regardless of hover target
        buttonWrapper.getElement().setAttribute("title", tooltipText);
        buttonWrapper.wrapComponent(button);
        pruningParamsHorizontalLayout.add(buttonWrapper);
    }

    /**
     * Builds a filtered list of NodeShapes that satisfy the configured thresholds.
     * Behavior matches the previous inline logic (no semantic change).
     */
    private List<NS> buildPrunedNodeShapes(List<NS> nodeShapes, Integer support, Double confidence) {
        List<NS> filteredNodeShapes = new ArrayList<>();
        for (NS ns : nodeShapes) {
            // Exclude NodeShapes that fail the support threshold
            if (ns.getPruneFlag()) {
                continue;
            }

            // Keep only PropertyShapes that meet BOTH support and confidence thresholds
            List<PS> keptPropertyShapes = new ArrayList<>();
            for (PS ps : ns.getPropertyShapes()) {
                boolean keep = false;
                try {
                    Integer psSupport = ps.getSupport();
                    Double psConfidence = ps.getConfidence();

                    if (psSupport != null && psConfidence != null) {
                        // Use PS-level metrics when available
                        keep = (psSupport >= support) && (psConfidence >= confidence);
                    } else if (ps.getShaclOrListItems() != null) {
                        // Fallback: evaluate OR-list items; keep PS if any item satisfies BOTH thresholds
                        for (ShaclOrListItem item : ps.getShaclOrListItems()) {
                            try {
                                boolean itemBelowSupport = item.getSupportPruneFlag();
                                boolean itemBelowConfidence = item.getConfidencePruneFlag();
                                if (!itemBelowSupport && !itemBelowConfidence) {
                                    keep = true;
                                    break;
                                }
                            } catch (Throwable ignoredInner) { /* ignore and continue */ }
                        }
                    } else {
                        // If no information available, default to exclude from reliable set
                        keep = false;
                    }

                    if (keep) {
                        keptPropertyShapes.add(ps);
                    }
                } catch (Throwable ignored) {
                    // If any unexpected issue occurs, default to exclude from reliable set
                }
            }

            // Only include NodeShape if it still has at least one reliable PropertyShape
            if (!keptPropertyShapes.isEmpty()) {
                NS filtered = new NS();
                filtered.setIri(ns.getIri());
                filtered.setTargetClass(ns.getTargetClass());
                filtered.setSupport(ns.getSupport());
                filtered.setPropertyShapes(keptPropertyShapes);
                filteredNodeShapes.add(filtered);
            }
        }
        return filteredNodeShapes;
    }

    /**
     * Post-processes turtle content to fix remaining formatting issues.
     * 
     * This is a quick and dirty solution to handle:
     * 1. Replace "NodeKind" with "nodeKind" (case-sensitive)
     * 2. Replace commas with dots in qse:confidence values (e.g., "1,2E-1" → "1.2E-1")
     * 3. Add ^^xsd:double datatype to all qse:confidence values
     * 4. Fix untyped scientific notation values like "1E0" → "1.0E0"^^xsd:double
     * 
     * @param content The original turtle content
     * @return The post-processed turtle content
     */
    private String postProcessTurtleContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        
        // 1. Fix NodeKind casing: replace "NodeKind" with "nodeKind" 
        content = content.replace("NodeKind", "nodeKind");
        
        // 2. Fix decimal comma in confidence values and add xsd:double datatype
        // Pattern matches: qse:confidence followed by whitespace, then number with comma, then optional whitespace and semicolon
        // Example: "qse:confidence 1,2E-1 ;" becomes "qse:confidence \"1.2E-1\"^^xsd:double ;"
        String confPred = "(?:qse:confidence|<http://shaclshapes.org/confidence>)";
        
        content = content.replaceAll(
            "(" + confPred + "\\s+)([0-9]+,[0-9]+(?:E[+-]?[0-9]+)?)\\s*;",
            "$1\"" + "$2" + "\"^^xsd:double ;"
        );
        
        // 3. Replace commas with dots in the matched confidence values
        // This needs to be done after the datatype annotation to avoid affecting other commas
        content = content.replaceAll(
            "(" + confPred + "\\s+\"[^\"]*),([^\"]*\"\\^\\^xsd:double)",
            "$1.$2"
        );
        
        // 4. Fix untyped scientific notation values like "1E0" → "1.0E0"^^xsd:double
        // Pattern matches: confidence predicate followed by whitespace, then scientific notation without decimal point
        content = content.replaceAll(
            "(" + confPred + "\\s+)([0-9]+E[+-]?[0-9]+)\\s*;",
            "$1\"$2\"^^xsd:double ;"
        );
        
        // 5. Add decimal point to scientific notation values that lack it (inside quotes)
        // This converts "1E0" to "1.0E0" within the already quoted and typed values
        content = content.replaceAll(
            "(" + confPred + "\\s+\")([0-9]+)(E[+-]?[0-9]+)(\"\\^\\^xsd:double)",
            "$1$2.0$3$4"
        );
        
        return content;
    }

    Parser parser;

    private void beginPruning() {
        // Update step counter to show Step 4/4 when analysis begins
        stepCounter.setText("SHACTOR (Step 4/4)");
        
        soChartsContainerHorizontalLayout.setVisible(true);
        shapesGrid.removeAllColumns();
        vl1.removeAll();
        vl2.removeAll();
        vl3.removeAll();
        vl4.removeAll();

        String supportStr = supportTextField.getValue() != null ? supportTextField.getValue().trim() : "";
        String confStr = confidenceTextField.getValue() != null ? confidenceTextField.getValue().trim() : "";
        try {
            support = Integer.parseInt(supportStr);
            confidence = (Double.parseDouble(confStr)) / 100;
        } catch (NumberFormatException e) {
            Utils.notify("Please enter numeric values only (no spaces).", NotificationVariant.LUMO_ERROR, Notification.Position.TOP_CENTER);
            return;
        }
        List<NS> nodeShapes = null;

        switch (IndexView.category) {
            case EXISTING_FILE_BASED -> {
                parser = SelectionView.getParser();
                this.prunedFileAddress = parser.extractSHACLShapesWithPruning(SelectionView.isFilteredClasses, confidence, support, SelectionView.chosenClasses); // extract shapes with pruning
                nodeShapes = parser.shapesExtractor.getNodeShapes();
            }
            case CONNECT_END_POINT -> {
                QbParser qbParser = SelectionView.getQbParser();
                this.prunedFileAddress = qbParser.extractSHACLShapesWithPruning(confidence, support); // extract shapes with pruning
                nodeShapes = qbParser.shapesExtractor.getNodeShapes();
            }
        }
        
        // Configure Download Shapes button now that parser is available
        // This ensures the button is properly enabled with correct data availability
        configureFormatAwareDownloadShapesButton();
        
        //if you want to compute stats by querying the model, uncomment the following line
        //pruningUtil.computeStats(parser.shapesExtractor, support, confidence);

        assert nodeShapes != null;
        pruningUtil.applyPruningFlags(nodeShapes, support, confidence);
        pruningUtil.getDefaultStats(nodeShapes);
        pruningUtil.getStatsBySupport(nodeShapes);
        pruningUtil.getStatsByConfidence(nodeShapes);
        pruningUtil.getStatsByBoth(nodeShapes);

        headingPieCharts.setVisible(true);

        headingNodeShapesAnalysis.setVisible(true);
        vl1.add(getParagraph("Default Shapes Analysis"));
        vl2.add(getParagraph("Shapes Analysis by Support"));
        vl3.add(getParagraph("Shapes Analysis by Confidence"));
        vl4.add(getParagraph("By Support and Confidence"));

        vl1.add(ChartsUtil.buildPieChart(preparePieChartsDataWithDefaultStats(pruningUtil.getStatsDefault(), pruningUtil)));
        vl2.add(ChartsUtil.buildPieChart(preparePieChartDataForSupportAnalysis(pruningUtil.getStatsBySupport(), support, pruningUtil)));
        vl3.add(ChartsUtil.buildPieChart(preparePieChartDataForConfidenceAnalysis(pruningUtil.getStatsByConfidence(), confidence, pruningUtil)));
        vl4.add(ChartsUtil.buildPieChart(preparePieChartDataForSupportAndConfidenceAnalysis(pruningUtil.getStatsByBoth(), support, confidence, pruningUtil)));

        setupNodeShapesGrid(nodeShapes, support, confidence);
        setupFilterRadioGroup(vaadinRadioGroup);
        vaadinRadioGroup.setVisible(true);
        List<NS> finalNodeShapes = nodeShapes;
        
        // Store pruned NodeShapes for format-aware download using extracted method
        this.prunedNodeShapes = buildPrunedNodeShapes(nodeShapes, support, confidence);
        
        // Configure Download Reliable Shapes button now that prunedNodeShapes is available
        // This ensures the button is properly enabled with correct pruned data
        configurePrunedShapesDownloadButton();
        
        vaadinRadioGroup.addValueChangeListener(listener -> {
            if (listener.getValue() != null) {
                if (vaadinRadioGroup.getValue().equals("Above")) {
                    shapesGrid.setItems(positive(finalNodeShapes));
                    shapesGrid.getDataProvider().refreshAll();
                }

                if (vaadinRadioGroup.getValue().equals("Below")) {
                    shapesGrid.setItems(negative(finalNodeShapes));
                    shapesGrid.getDataProvider().refreshAll();
                }

                if (vaadinRadioGroup.getValue().equals("All")) {
                    LOG.debug("Default");
                    shapesGrid.setItems(finalNodeShapes);
                    shapesGrid.getDataProvider().refreshAll();
                }
            }
        });
    }


    // -------------------------   Setup Grids   -----------------------------
    private void setupNodeShapesGrid(List<NS> nodeShapes, Integer support, Double confidence) {
        shapesGrid.setVisible(true);
        nsGridRadioButtonInfo.setVisible(true);
        shapesGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        shapesGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        shapesGrid.addColumn(NS::getLocalNameFromIri).setHeader(Utils.boldHeader("Node Shape")).setResizable(true).setAutoWidth(true).setComparator(NS::getPruneFlag);
        shapesGrid.addColumn(NS::getTargetClass).setHeader(Utils.boldHeader("Target Class")).setResizable(true).setResizable(true).setAutoWidth(true);
        shapesGrid.addColumn(NS::getSupport).setHeader(Utils.boldHeader("Support")).setResizable(true).setAutoWidth(true).setSortable(true);
        shapesGrid.addColumn(NS::getCountPropertyShapes).setHeader(Utils.boldHeader("Count PS")).setResizable(true).setAutoWidth(true);
        shapesGrid.addColumn(new ComponentRenderer<>(ProgressBar::new, (progressBar, ns) -> {
            progressBar.addThemeVariants(ProgressBarVariant.LUMO_SUCCESS);
            progressBar.setId("quality-indicator-progress-bar");
            double psCountGreen = ns.getCountPropertyShapes() - ns.getCountPsWithSupportPruneFlag();
            progressBar.setValue(psCountGreen / ns.getCountPropertyShapes());
        })).setHeader((setHeaderWithInfoLogo(
                "PS Quality (by Support)",
                "This shows quality of NS in terms of PS left after pruning (green) and removed by pruning (red) provided user's support and confidence thresholds."))).setResizable(true).setAutoWidth(true);

        shapesGrid.addColumn(new ComponentRenderer<>(ProgressBar::new, (progressBar, ns) -> {
            progressBar.addThemeVariants(ProgressBarVariant.LUMO_SUCCESS);
            progressBar.setId("quality-indicator-progress-bar");
            double psCountGreen = ns.getCountPropertyShapes() - ns.getCountPsWithConfidencePruneFlag();
            progressBar.setValue(psCountGreen / ns.getCountPropertyShapes());
        })).setHeader(setHeaderWithInfoLogo(
                "PS Quality (by Confidence)",
                "This shows quality of NS in terms of PS left after pruning (green) and removed by pruning (red) provided user's support and confidence thresholds.")).setResizable(true).setAutoWidth(true);

        shapesGrid.addColumn(new ComponentRenderer<>(Button::new, (button, ns) -> {
            button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
            button.addClickListener(e -> {
                currNS = ns;
                propertyShapesGrid.removeAllColumns();
                this.setupPropertyShapesGrid(ns);
            });
            button.setIcon(new Icon(VaadinIcon.LIST_UL));
            button.setText("PS List");
        })).setHeader(setHeaderWithInfoLogo("Show PS", "See PS of current NS"));

        //setClassNameToHighlightNodeShapesInRed(shapesGrid);
        nodeShapes.sort((d1, d2) -> d2.getSupport() - d1.getSupport());
        GridListDataView<NS> dataView = shapesGrid.setItems(nodeShapes);
        shapesGrid.addSelectionListener(selection -> {
            LOG.debug("Number of selected classes: {}", selection.getAllSelectedItems().size());
            downloadSelectedShapesButton.setVisible(true);

            downloadSelectedShapesButton.addClickListener(listener -> {
                // Use the selected format from IndexView for shape generation
                LOG.debug("[DEBUG] Selected format: {}", IndexView.selectedFormat);
                LOG.debug("[DEBUG] Number of selected items: {}", selection.getAllSelectedItems().size());
                
                String shapes = Utils.constructModelForGivenNodeShapesAndTheirPropertyShapes(
                    selection.getAllSelectedItems(), 
                    IndexView.selectedFormat
                );
                
                LOG.debug("[DEBUG] Generated shapes format: {}", IndexView.selectedFormat);
                LOG.debug("[DEBUG] Generated shapes length: {}", shapes.length());
                
                // Use format-aware dialog method to show correct header and file extension
                DialogUtil.getDialogWithHeaderAndFooterForShowingShapeSyntax(shapes, IndexView.selectedFormat);
                //System.out.println(shapes);
            });
        });
        nsSearchField.setVisible(true);
        nsSearchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        nsSearchField.setValueChangeMode(ValueChangeMode.EAGER);
        nsSearchField.addValueChangeListener(e -> dataView.refreshAll());

        dataView.addFilter(type -> {
            psSearchField.clear();
            String searchTerm = nsSearchField.getValue().trim();
            if (searchTerm.isEmpty())
                return true;
            return matchesTerm(type.getLocalNameFromIri(), searchTerm);
        });
    }

    private void setupPropertyShapesGrid(NS ns) {
        if (ns == null) return;
        psGridRadioButtonInfo.setVisible(true);
        currNodeShape = ns.getLocalNameFromIri();
        propertyShapesGridInfo.setVisible(true);
        propertyShapesGridInfo.setText("Property Shapes Analysis for " + currNodeShape);

        //propertyShapesGrid.removeAllColumns();
        propertyShapesGrid.setVisible(true);
        propertyShapesGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        propertyShapesGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        propertyShapesGrid.addColumn(PS::getLocalNameFromIri).setHeader(Utils.boldHeader("Property Shape")).setResizable(true).setAutoWidth(true).setComparator(PS::getPruneFlag);
        propertyShapesGrid.addColumn(PS::getPath).setHeader(Utils.boldHeader("Property Path")).setResizable(true).setAutoWidth(true);
        propertyShapesGrid.addColumn(PS::getSupport).setHeader(Utils.boldHeader("Support")).setResizable(true).setAutoWidth(true).setSortable(true);
        propertyShapesGrid.addColumn(PS::getConfidenceInPercentage).setHeader(Utils.boldHeader("Confidence")).setResizable(true).setAutoWidth(true).setComparator(PS::getConfidence);
        propertyShapesGrid.addColumn(new ComponentRenderer<>(ProgressBar::new, (progressBar, ps) -> {
            progressBar.addThemeVariants(ProgressBarVariant.LUMO_SUCCESS);
            progressBar.setId("quality-indicator-progress-bar");
            if (ps.getConfidence() != null) {
                progressBar.setValue(ps.getConfidence());
            } else {
                ShaclOrListItem item = null;
                for (ShaclOrListItem currItem : ps.getShaclOrListItems()) {
                    if (item == null) {
                        item = currItem;
                    }
                    if (currItem.getConfidence() > item.getConfidence()) {
                        item = currItem;
                    }
                }
                assert item != null;
                progressBar.setValue(item.getConfidence());
            }
        })).setHeader(setHeaderWithInfoLogo("PSc Quality (by Confidence)", " This shows")).setResizable(true).setAutoWidth(true);
        propertyShapesGrid.addColumn(new ComponentRenderer<>(Button::new, (button, ps) -> {
            button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
            //button.addClickListener(e -> this.generateQueryForPropertyShape(ns, ps));
            RouterLink link = new RouterLink("ps-view", PsView.class);
            button.addClickListener(e -> {
                currPS = ps;
                button.getUI().ifPresent(ui -> ui.getPage().open(link.getHref()));
            });

            button.setIcon(new Icon(VaadinIcon.EXTERNAL_LINK));
            button.setText("Analyze");
        })).setHeader(setHeaderWithInfoLogo("Action", "The generated SPARQL query will fetch the triples responsible for having chosen PS as part of NS"));

        setClassNameToHighlightPropertyShapesInRed(propertyShapesGrid);

        for (PS ps : ns.getPropertyShapes()) {
            if (ps.getConfidence() == null) {
                ShaclOrListItem item = null;
                for (ShaclOrListItem currItem : ps.getShaclOrListItems()) {
                    if (item == null) {
                        item = currItem;
                    }
                    if (currItem.getConfidence() > item.getConfidence()) {
                        item = currItem;
                    }
                }
                assert item != null;
                ps.setConfidence(item.getConfidence());
                ps.setSupport(item.getSupport());
            }
        }
        ns.getPropertyShapes().sort((d1, d2) -> d2.getSupport() - d1.getSupport());

        psVaadinRadioGroup.setVisible(true);
        setupFilterRadioGroup(psVaadinRadioGroup);
        psVaadinRadioGroup.addValueChangeListener(listener -> {
            if (listener.getValue() != null) {
                if (listener.getValue().equals("Above")) {
                    propertyShapesGrid.setItems(positivePs(ns.getPropertyShapes()));
                    propertyShapesGrid.getDataProvider().refreshAll();
                }

                if (listener.getValue().equals("Below")) {
                    propertyShapesGrid.setItems(negativePs(ns.getPropertyShapes()));
                    propertyShapesGrid.getDataProvider().refreshAll();
                }

                if (listener.getValue().equals("All")) {
                    LOG.debug("Default");
                    propertyShapesGrid.setItems(ns.getPropertyShapes());
                    propertyShapesGrid.getDataProvider().refreshAll();
                }
            }
        });

        GridListDataView<PS> dataView = propertyShapesGrid.setItems(ns.getPropertyShapes());

        psSearchField.setVisible(true);
        psSearchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        psSearchField.setValueChangeMode(ValueChangeMode.EAGER);
        psSearchField.addValueChangeListener(e -> dataView.refreshAll());

        dataView.addFilter(type -> {
            String searchTerm = psSearchField.getValue().trim();
            if (searchTerm.isEmpty())
                return true;
            return matchesTerm(type.getLocalNameFromIri(), searchTerm);
        });
    }

    private void setupFilterRadioGroup(RadioButtonGroup<String> vaadinRadioGroup) {
        vaadinRadioGroup.setItems("All", "Above", "Below");
        vaadinRadioGroup.setValue("All");
    }
    // -------------------------   Grids Helper Methods   -----------------------------


    private List<NS> negative(List<NS> ns) {
        List<NS> list = new ArrayList<>();
        for (NS nodeShape : ns) {
            if (nodeShape.getPruneFlag()) {
                list.add(nodeShape);
            }
        }
        ns = list;
        ns.sort((d1, d2) -> d2.getSupport() - d1.getSupport());
        return ns;
    }

    private List<NS> positive(List<NS> ns) {
        List<NS> list = new ArrayList<>();
        for (NS nodeShape : ns) {
            if (!nodeShape.getPruneFlag()) {
                list.add(nodeShape);
            }
        }
        ns = list;
        ns.sort((d1, d2) -> d2.getSupport() - d1.getSupport());
        return ns;
    }

    private List<PS> negativePs(List<PS> ps) {
        List<PS> list = new ArrayList<>();
        for (PS nodeShape : ps) {
            if (nodeShape.getPruneFlag()) {
                list.add(nodeShape);
            }
        }
        ps = list;
        ps.sort((d1, d2) -> d2.getSupport() - d1.getSupport());
        return ps;
    }

    private List<PS> positivePs(List<PS> ps) {
        List<PS> list = new ArrayList<>();
        for (PS nodeShape : ps) {
            if (!nodeShape.getPruneFlag()) {
                list.add(nodeShape);
            }
        }
        ps = list;
        ps.sort((d1, d2) -> d2.getSupport() - d1.getSupport());
        return ps;
    }


    private static void setClassNameToHighlightNodeShapesInRed(Grid<NS> shapesGrid) {
        shapesGrid.setClassNameGenerator(ns -> {
            if (ns.getPruneFlag()) {
                return "prune";
            } else {
                return "no-prune";
            }
        });
    }

    private void setClassNameToHighlightPropertyShapesInRed(Grid<PS> propertyShapesGrid) {
        propertyShapesGrid.setClassNameGenerator(ps -> {
            if (ps.getPruneFlag()) {
                return "prune";
            } else {
                return "no-prune";
            }
        });
    }


    // -------------------------   Getter Methods   -----------------------------
    public static PS getCurrPS() {
        return currPS;
    }

    public static NS getCurrNS() {
        return currNS;
    }

    public static Integer getSupport() {
        return support;
    }

    public static Double getConfidence() {
        return confidence;
    }

}
