package shactor;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.DialogVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import cs.qse.common.structure.NS;
import cs.qse.common.structure.PS;
import cs.qse.common.structure.ShaclOrListItem;
import cs.utils.Constants;
import cs.utils.Tuple2;
import de.atextor.turtle.formatter.FormattingStyle;
import de.atextor.turtle.formatter.TurtleFormatter;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.lib.ShLib;
import org.apache.jena.shacl.validation.ReportEntry;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.BindingSet;
import shactor.utils.*;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static shactor.utils.Utils.*;

/**
 * A Designer generated component for the ps-view template.
 * <p>
 * Designer will add and remove fields with @Id mappings but
 * does not overwrite or otherwise change this file.
 */
@Tag("ps-view")
@RouteAlias("propertyShapeDashboard")
@CssImport(value = "./grid.css", themeFor = "vaadin-grid")
@CssImport(value = "./text-field.css", themeFor = "vaadin-text-field")
@JsModule("./ps-view.ts")
@JsModule("./copytoclipboard.js")
@Route("/ps-view")

public class PsView extends LitTemplate {
    @Id("psConstraintsGrid")
    private Grid<PS> psConstraintsGrid;
    @Id("psOrItemsConstraintsGrid")
    private Grid<ShaclOrListItem> psOrItemsConstraintsGrid;
    @Id("psSyntaxTextArea")
    private TextArea psSyntaxTextArea;
    @Id("infoHorizontalLayout")
    private HorizontalLayout infoHorizontalLayout;
    @Id("buttonA")
    private Button buttonA;
    @Id("copySyntaxButton")
    private Button copySyntaxButton;
//    @Id("statusHorizontalLayout")
//    private HorizontalLayout statusHorizontalLayout;

    NS nodeShape;
    PS propertyShape;
    GraphExplorer graphExplorer;
    TextArea descriptionArea;
    Model currNsPsModel;
    @Id("propCoverageInfoParagraph")
    private Paragraph propCoverageInfoParagraph;
    @Id("propCoverageQueryButton")
    private Button propCoverageQueryButton;
    @Id("matchedEntitiesHeading")
    private H4 matchedEntitiesHeading;
    @Id("missingPropertiesHeading")
    private H4 missingPropertiesHeading;
    @Id("entitiesInspectionInfoParagraph")
    private Paragraph entitiesInspectionInfoParagraph;
    @Id("shapeSyntaxHeading")
    private H4 shapeSyntaxHeading;

    public PsView() {
        Tuple2<String, String> urlAndRepoTuple = Utils.getDatasetsEndpointDetails().get(IndexView.selectedDataset);
        if (IndexView.category.equals(IndexView.Category.CONNECT_END_POINT)) {
            System.out.println();
            urlAndRepoTuple = new Tuple2<>(IndexView.graphURL, IndexView.endPointRepo);
        }
        graphExplorer = new GraphExplorer(urlAndRepoTuple._1, urlAndRepoTuple._2);
        nodeShape = ExtractionView.getCurrNS();
        propertyShape = ExtractionView.getCurrPS();

        setupNodeShapeInfo(infoHorizontalLayout);
        //setupPropertyShapeInfo(infoHorizontalLayoutTwo);
        //setupTypesScopeOfPs(infoHorizontalLayoutTwo);
        //setupStatus(statusHorizontalLayout);
        setupTextArea();
        setupGrid();
        extractEntitiesNotHavingFocusProperty();
        setupActionButtons();
        
        // Set dynamic text based on selected format
        updateDynamicText();
    }

    /**
     * Updates dynamic text elements based on the selected format (SHACL or ShEx)
     */
    private void updateDynamicText() {
        String formatName = IndexView.selectedFormat != null ? IndexView.selectedFormat : "SHACL";
        String syntaxHeading = formatName.equals("ShEx") ? "Shape Syntax (ShEx)" : "Shape Syntax (SHACL)";
        
        // Update the shape syntax heading
        if (shapeSyntaxHeading != null) {
            shapeSyntaxHeading.setText(syntaxHeading);
        }
    }

    private void setupNodeShapeInfo(HorizontalLayout hl) {
        //vl.add(new H4("Node Shape (NS) Info:"));
        hl.add(Utils.getReadOnlyTextField("Node Shape: ", nodeShape.getLocalNameFromIri()));
        hl.add(Utils.getReadOnlyTextField("No. of PS of :" + nodeShape.getLocalNameFromIri() + ": ", nodeShape.getCountPropertyShapes().toString()));
        hl.add(Utils.getReadOnlyTextField("Target Class: ", nodeShape.getTargetClass().toString()));
        hl.add(Utils.getReadOnlyTextField("PS PATH: ", propertyShape.getPath()));
        hl.add(Utils.getReadOnlyTextField("PS IRI: ", nodeShape.getIri().toString()));
        //hl.add(Utils.getReadOnlyTextField("Support Threshold: ", ExtractionView.getSupport().toString()));
        //hl.add(Utils.getReadOnlyTextField("Confidence Threshold: ", ExtractionView.getConfidence() * 100 + "%"));
    }

    private void setupStatus(HorizontalLayout hl) {
        if (propertyShape.getPruneFlag()) {
            Span status = new Span(createIcon(VaadinIcon.FILE_REMOVE), new Span("To be Pruned!"));
            status.getElement().getThemeList().add("badge error");
            status.setSizeFull();
            status.setHeightFull();
            hl.add(status);
        } else {
            Span status = new Span(createIcon(VaadinIcon.FLAG), new Span("Not to be Pruned!"));
            status.getElement().getThemeList().add("badge success");
            status.setSizeFull();
            hl.add(status);
        }
    }

    private void extractEntitiesNotHavingFocusProperty() {
        nodeShape.getTargetClass();
        nodeShape.getSupport();
        propertyShape.getPath();
        String[] propPathSplit = propertyShape.getPath().split("/");
        String propLocalName = propPathSplit[propPathSplit.length - 1];
        List<BindingSet> result = graphExplorer.runSelectQuery(QueryUtil.buildQueryToExtractEntitiesNotHavingFocusProperty(nodeShape.getTargetClass(), propertyShape.getPath()));
        propCoverageInfoParagraph.setText("There are total " + Utils.formatWithCommas(nodeShape.getSupport()) + " entities of " + nodeShape.getTargetClass().getLocalName() + " class out of which " + result.size() + " are missing " + propertyShape.getPath() + " property. SHACTOR allows to add this property to selected entities.");
        entitiesInspectionInfoParagraph.setText("Explore all entities of type " + nodeShape.getTargetClass().getLocalName() + " having property <" + propertyShape.getPath() + ">. SHACTOR allows to generate queries to delete the chosen entities.");

        matchedEntitiesHeading.setText(Utils.formatWithCommas(nodeShape.getSupport() - result.size()) + " entities matched " + propLocalName + " property shape:");
        missingPropertiesHeading.setText(Utils.formatWithCommas(result.size()) + " entities of type " + nodeShape.getTargetClass().getLocalName() + " are missing " + propLocalName + " property:");

        propCoverageQueryButton.addClickListener(buttonClickEvent -> {
            createDialogueToShowEntitiesHavingMissingProperty(result);
        });
    }

    private void setupTypesScopeOfPs(HorizontalLayout hl) {
        HashMap<String, Integer> typeToEntityCount = new HashMap<>();
        //extract types
        List<BindingSet> resultTypes = graphExplorer.runSelectQuery(QueryUtil.buildQueryToExtractTypesOfPs(nodeShape.getTargetClass().stringValue(), propertyShape.getPath()));
        for (BindingSet bindings : resultTypes) { //bindings : ?types
            typeToEntityCount.put(bindings.getValue("types").stringValue(), 0);
        }
        //extract entity count of types
        typeToEntityCount.keySet().forEach(type -> {
            List<BindingSet> resultEntityCount = graphExplorer.runSelectQuery(QueryUtil.buildQueryToComputeEntityCountForTypeOfPs(type, propertyShape.getPath()));
            for (BindingSet bindings : resultEntityCount) { //bindings : ?entityCount
                typeToEntityCount.put(type, Integer.parseInt(bindings.getValue("entityCount").stringValue()));
            }
        });
        Select<String> select = new Select<>();
        select.setLabel("Types Scope (other classes having this property)");
        select.setWidthFull();
        select.setPlaceholder(nodeShape.getTargetClass().stringValue() + " (" + typeToEntityCount.get(nodeShape.getTargetClass().stringValue()) + ") ");
        List<String> values = new ArrayList<>();
        Utils.sortMapDescending(typeToEntityCount).forEach((k, v) -> {
            values.add(k + " (" + v + ")");
        });
        select.setItems(values);
        hl.add(select);
    }

    HashMap<String, Integer> suggestionsWithSupport = new HashMap<>();

    private void setupGrid() {
        // Make column headers dynamic based on selected format
        String formatName = IndexView.selectedFormat != null ? IndexView.selectedFormat : "SHACL";
        String nodeKindHeader = formatName.equals("ShEx") ? "NodeKind" : "sh:NodeKind";
        String classOrDataTypeHeader = formatName.equals("ShEx") ? "Class or dataType" : "sh:Class or sh:dataType";
        
        if (propertyShape.getHasOrList()) {
            psConstraintsGrid.setVisible(false);
            psOrItemsConstraintsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
            psOrItemsConstraintsGrid.addColumn(ShaclOrListItem::getNodeKind).setHeader(Utils.boldHeader(nodeKindHeader)).setResizable(true).setAutoWidth(true);
            psOrItemsConstraintsGrid.addColumn(ShaclOrListItem::getDataTypeOrClass).setHeader(Utils.boldHeader(classOrDataTypeHeader)).setResizable(true).setAutoWidth(true);
            psOrItemsConstraintsGrid.addColumn(ShaclOrListItem::getSupport).setHeader(Utils.boldHeader("Support")).setResizable(true).setAutoWidth(false).setSortable(true);
            psOrItemsConstraintsGrid.addColumn(ShaclOrListItem::getConfidenceInPercentage).setHeader(Utils.boldHeader("Confidence")).setResizable(true).setAutoWidth(false).setComparator(ShaclOrListItem::getConfidence);

            //Declare an undefined flag
            boolean flag = false;
            for (ShaclOrListItem item : propertyShape.getShaclOrListItems()) {
                if (item.getDataTypeOrClass() == null || Objects.equals(item.getDataTypeOrClass(), "http://shaclshapes.org/undefined")) {
                    item.setDataTypeOrClass("Undefined");
                    flag = true;
                }
            }

            for (ShaclOrListItem val : propertyShape.getShaclOrListItems()) {
                if (val.getDataTypeOrClass() != null && !val.getDataTypeOrClass().equals("Undefined")) {
                    suggestionsWithSupport.put(val.getDataTypeOrClass(), val.getSupport());
                }
            }

            Utils.sortMapByValuesDesc(suggestionsWithSupport);

            psOrItemsConstraintsGrid.addColumn(new ComponentRenderer<>(Button::new, (button, shaclOrListItem) -> {
                String objType = shaclOrListItem.getDataTypeOrClass();
                String nodeKind = shaclOrListItem.getNodeKind();
                button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
                if ("IRI".equals(nodeKind)) {
                    button.addClickListener(e -> {
                        List<BindingSet> output;
                        if (objType.equals("Undefined")) {
                            output = graphExplorer.runSelectQuery(QueryUtil.buildQueryToExtractEntitiesHavingUndefinedShClass(nodeShape.getTargetClass().stringValue(), propertyShape.getPath()));
                        } else {
                            output = graphExplorer.runSelectQuery(QueryUtil.buildQueryToExtractEntitiesHavingSpecificShClass(nodeShape.getTargetClass().stringValue(), propertyShape.getPath(), shaclOrListItem.getDataTypeOrClass()));
                        }
                        createDialogueToShowEntitiesWithPropAndObject(output);
                    });

                    button.setIcon(new Icon(VaadinIcon.LIST));
                    button.setText("Inspect Entities");
                }
            })).setHeader(setHeaderWithInfoLogo("Show Entities", "Retrieve list of entities having specified object type."));
            //FIXME: extract entities having specified object type (i.e., value of sh:class constraint)

            if (flag) {
                psOrItemsConstraintsGrid.addColumn(new ComponentRenderer<>(Button::new, (button, shaclOrListItem) -> {
                    String objType = shaclOrListItem.getDataTypeOrClass();
                    String nodeKind = shaclOrListItem.getNodeKind();
                    if (!objType.equals("Undefined")) {
                        button.setVisible(false);
                    }
                    button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
                    if ("IRI".equals(nodeKind)) {
                        button.addClickListener(e -> {
                            if (objType.equals("Undefined")) {
                                List<BindingSet> output = graphExplorer.runSelectQuery(QueryUtil.buildQueryToExtractObjectsHavingUndefinedShClass(nodeShape.getTargetClass().stringValue(), propertyShape.getPath()));
                                createDialogueToShowEntities(output);
                            }
                        });
                        button.setIcon(new Icon(VaadinIcon.LIST));
                        button.setText("Inspect Objects");
                    }
                })).setHeader(setHeaderWithInfoLogo("Show Objects", "Retrieve list of objects (IRIs) having undefined type."));
            }
            psOrItemsConstraintsGrid.setItems(propertyShape.getShaclOrListItems());


        } else {
            psOrItemsConstraintsGrid.setVisible(false);
            if (propertyShape.getDataTypeOrClass() != null) {
                suggestionsWithSupport.put(propertyShape.getDataTypeOrClass(), propertyShape.getSupport());
            }
            //Declare an undefined flag
            boolean flag = false;
            if (propertyShape.getDataTypeOrClass() == null) {
                propertyShape.setDataTypeOrClass("Undefined");
                flag = true;
            }
            String nodeKind = propertyShape.getNodeKind();

            psConstraintsGrid.addColumn(PS::getPath).setHeader(Utils.boldHeader("Property Path")).setResizable(true).setAutoWidth(true);
            psConstraintsGrid.addColumn(PS::getNodeKind).setHeader(Utils.boldHeader(nodeKindHeader)).setResizable(true).setAutoWidth(true);
            psConstraintsGrid.addColumn(PS::getDataTypeOrClass).setHeader(Utils.boldHeader(classOrDataTypeHeader)).setResizable(true).setAutoWidth(true);
            psConstraintsGrid.addColumn(PS::getSupport).setHeader(Utils.boldHeader("Support")).setResizable(true).setAutoWidth(true).setSortable(true);
            psConstraintsGrid.addColumn(PS::getConfidenceInPercentage).setHeader(Utils.boldHeader("Confidence")).setResizable(true).setAutoWidth(true).setComparator(PS::getConfidence);
            if ("IRI".equals(nodeKind)) {
                psConstraintsGrid.addColumn(new ComponentRenderer<>(Button::new, (button, ps) -> {
                    button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
                    button.addClickListener(e -> {
                        List<BindingSet> output;
                        if (ps.getDataTypeOrClass().equals("Undefined")) {
                            output = graphExplorer.runSelectQuery(QueryUtil.buildQueryToExtractEntitiesHavingUndefinedShClass(nodeShape.getTargetClass().stringValue(), propertyShape.getPath()));
                        } else {
                            output = graphExplorer.runSelectQuery(QueryUtil.buildQueryToExtractEntitiesHavingSpecificShClass(nodeShape.getTargetClass().stringValue(), propertyShape.getPath(), ps.getDataTypeOrClass()));
                        }
                        createDialogueToShowEntitiesWithPropAndObject(output);
                    });
                    button.setIcon(new Icon(VaadinIcon.LIST));
                    button.setText("Inspect Entities");
                })).setHeader(setHeaderWithInfoLogo("Show Entities", "Retrieve list of entities having specified object type."));


                if (flag) {
                    psConstraintsGrid.addColumn(new ComponentRenderer<>(Button::new, (button, ps) -> {
                        if (!ps.getDataTypeOrClass().equals("Undefined")) {
                            button.setVisible(false);
                        }
                        button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
                        button.addClickListener(e -> {
                            if (ps.getDataTypeOrClass().equals("Undefined")) {
                                List<BindingSet> output = graphExplorer.runSelectQuery(QueryUtil.buildQueryToExtractObjectsHavingUndefinedShClass(nodeShape.getTargetClass().stringValue(), propertyShape.getPath()));
                                createDialogueToShowEntities(output);
                            }
                        });
                        button.setIcon(new Icon(VaadinIcon.LIST));
                        button.setText("Inspect Objects");
                    })).setHeader(setHeaderWithInfoLogo("Show Objects", "Retrieve list of objects (IRIs) having undefined type."));
                }
            }
            psConstraintsGrid.setItems(propertyShape);
        }
    }

    private void setupTextArea() {
        this.psSyntaxTextArea.setValue(prepareModelForNsAndPs());
        this.psSyntaxTextArea.getStyle().set("resize", "vertical"); //https://cookbook.vaadin.com/resizable-components
        this.psSyntaxTextArea.getStyle().set("overflow", "auto");
        this.psSyntaxTextArea.setHeight("150px");
        this.copySyntaxButton.setIcon(VaadinIcon.COPY.create());
        this.copySyntaxButton.getElement().setProperty("title", "Does not work on Safari!");
        this.copySyntaxButton.addClickListener(e -> {
            String value = this.psSyntaxTextArea.getValue();
            System.out.println(value);
            UI.getCurrent().getPage().executeJs("window.copyToClipboard($0)", value);
        });
    }

    private void setupActionButtons() {
        buttonA.addClickListener(e -> this.generateDialogWithQueryForPropertyShape(nodeShape, propertyShape));
        buttonA.addThemeVariants(ButtonVariant.LUMO_ICON);
        buttonA.setIcon(new Icon(VaadinIcon.EDIT));

        propCoverageQueryButton.addThemeVariants(ButtonVariant.LUMO_ICON);
        propCoverageQueryButton.setIcon(new Icon(VaadinIcon.EDIT));
    }

    private void validateEntitiesData(NS ns, PS ps) {
        //here query all the entities and the properties, store the results in a model and then validate against the shape.
        String sparqlQuery = QueryUtil.extractSparqlQueryWithFullEntitiesData(ns, ps);
        List<Triple> tripleList = queryGraph(sparqlQuery);
        Model dataModel = prepareJenaModelForValidation(tripleList);
        Graph dataGraph = dataModel.getGraph();
        Shapes shapes = Shapes.parse(currNsPsModel.getGraph());
        ValidationReport report = ShaclValidator.get().validate(shapes, dataGraph);

        ShLib.printReport(report);
        if (report.conforms()) {
            notifyMessage("Conforms!");
        } else {
            notifyError("Does not conforms");
        }
        for (ReportEntry re : ShaclValidator.get().validate(shapes, dataGraph).getEntries()) {
            String line = re.source() + "|" + re.focusNode() + "|" + re.resultPath() + "|" + re.value() + "|" + re.sourceConstraintComponent() + "|" + re.message();
            System.out.println(line);
        }
    }

    // -------------------------    Methods   -----------------------------

    private void generateDialogWithQueryForPropertyShape(NS ns, PS ps) {
        Dialog dialog = new Dialog();
        dialog.getElement().setAttribute("aria-label", "Query");
        String sparqlQuery = QueryUtil.extractSparqlQuery(ns, ps);

        dialog.getHeader().add(createHeaderLayout(ps.getLocalNameFromIri()));
        createFooter(dialog);
        VerticalLayout dialogLayout = createDialogLayout(ps.getLocalNameFromIri(), sparqlQuery);
        dialog.add(dialogLayout);
        dialog.setModal(false);
        dialog.setDraggable(true);
        dialog.open();
    }

    // -------------------------   Dialogue Methods   -----------------------------


    private H2 createHeaderLayout(String title) {
        H2 headline = new H2("SPARQL Query to Extract Triples for " + title);
        headline.getStyle().set("padding-bottom", "0px");
        headline.addClassName("draggable");
        headline.getStyle().set("margin", "0").set("font-size", "1.5em").set("font-weight", "bold").set("cursor", "move").set("padding", "var(--lumo-space-m) 0").set("flex", "1");
        return headline;
    }

    private VerticalLayout createDialogLayout(String psName, String sparqlQuery) {
        // Make the description format-aware based on user's selection
        String formatName = IndexView.selectedFormat != null ? IndexView.selectedFormat : "SHACL";
        String constraintType = formatName.equals("ShEx") ? "ShEx constraint" : "SHACL constraint";
        
        Paragraph paragraph = new Paragraph("SHACTOR has generated the following SPARQL query to be executed on the provided Knowledge Graph. This query will fetch the triples responsible for extracting the following " + constraintType + ":");
        H6 nsTitle = new H6("NS: " + nodeShape.getLocalNameFromIri());
        H6 psTitle = new H6("PS: " + psName);

        nsTitle.getStyle().set("margin-top", "0px");
        descriptionArea = new TextArea("SPARQL Query");
        descriptionArea.setValue(sparqlQuery);

        VerticalLayout fieldLayout = new VerticalLayout(paragraph, nsTitle, psTitle, descriptionArea);
        fieldLayout.setSpacing(false);
        fieldLayout.setPadding(false);
        fieldLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        fieldLayout.getStyle().set("width", "1000px").set("max-width", "100%");

        return fieldLayout;
    }

    private void createFooter(Dialog dialog) {
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        Button executeQueryButton = new Button("Execute Query", e -> dialog.close());
        executeQueryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(executeQueryButton);

        executeQueryButton.addClickListener(buttonClickEvent -> {
            List<Triple> tripleList = queryGraph(descriptionArea.getValue());
            //simplifyOutput(tripleList);
            createDialogueToShowTriples(tripleList);
        });
    }

    private List<Triple> queryGraph(String query) {
        return graphExplorer.runQuery(query);
    }

    private Model prepareJenaModelForValidation(List<Triple> tripleList) {
        Model model = ModelFactory.createDefaultModel();
        for (Triple triple : tripleList) {
            Statement s = ResourceFactory.createStatement(
                    ResourceFactory.createResource(wab(triple.getSubject())),
                    ResourceFactory.createProperty(wab(triple.getPredicate())),
                    ResourceFactory.createResource(wab(triple.getObject()))
            );
            model.add(s);
        }
        System.out.println(model.size());
        System.out.println("model");
        return model;
    }

    //Wrap angle brackets
    private String wab(String val) {
        return "<" + val + "> ";
    }


    private String prepareModelForNsAndPs() {
        // Use format-aware approach based on user's selection
        String formatName = IndexView.selectedFormat != null ? IndexView.selectedFormat : "SHACL";
        
        // Create a temporary NodeShape with just the current PropertyShape for display
        // We need to create a Set<NS> to use the format-aware Utils method
        Set<NS> tempNodeShapes = new HashSet<>();
        
        // Create a copy of the current NodeShape with only the current PropertyShape
        NS tempNodeShape = new NS();
        tempNodeShape.setIri(this.nodeShape.getIri());
        tempNodeShape.setTargetClass(this.nodeShape.getTargetClass());
        tempNodeShape.setSupport(this.nodeShape.getSupport());
        
        // Add only the current PropertyShape to show just this specific shape
        List<PS> singlePropertyShape = new ArrayList<>();
        singlePropertyShape.add(this.propertyShape);
        tempNodeShape.setPropertyShapes(singlePropertyShape);
        
        tempNodeShapes.add(tempNodeShape);
        
        // Use the format-aware Utils method to generate the appropriate syntax
        String result = Utils.constructModelForGivenNodeShapesAndTheirPropertyShapes(tempNodeShapes, formatName);
        
        // For backward compatibility, still store the model for SHACL validation if needed
        if ("SHACL".equals(formatName)) {
            // Only create the RDF model if we're using SHACL format
            Model model = ModelFactory.createDefaultModel();
            model.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");
            model.setNsPrefix("qse", "http://shaclshapes.org/");
            
            // Add basic statements for the current NodeShape and PropertyShape
            Statement nsType = ResourceFactory.createStatement(
                ResourceFactory.createResource(this.nodeShape.getIri().toString()), 
                ResourceFactory.createProperty(RDF.type.toString()), 
                ResourceFactory.createResource(SHACL.NODE_SHAPE.toString())
            );
            Statement nsTarget = ResourceFactory.createStatement(
                ResourceFactory.createResource(this.nodeShape.getIri().toString()), 
                ResourceFactory.createProperty(SHACL.TARGET_CLASS.toString()), 
                ResourceFactory.createResource(this.nodeShape.getTargetClass().toString())
            );
            Statement nsPs = ResourceFactory.createStatement(
                ResourceFactory.createResource(this.nodeShape.getIri().toString()), 
                ResourceFactory.createProperty(SHACL.PROPERTY.toString()), 
                ResourceFactory.createResource(this.propertyShape.getIri().toString())
            );
            
            model.add(nsType);
            model.add(nsTarget);
            model.add(nsPs);
            
            this.currNsPsModel = model;
        }
        
        return result;
    }


    private void createDialogueToShowTriples(List<Triple> tripleList) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Triples");

        //dialog.getFooter().add(createFilterButton(dialog));
        VerticalLayout dialogLayout = createDialogContentForShowingTriples(tripleList);
        dialog.add(dialogLayout);
        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.addThemeVariants(DialogVariant.LUMO_NO_PADDING);
        dialog.open();
    }

    private void createDialogueToShowEntitiesWithPropAndObject(List<BindingSet> tripleList) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Entities Inspection");
        VerticalLayout dialogLayout = createDialogContentForShowingEntitiesWithPropAndObject(tripleList);
        dialog.add(dialogLayout);
        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.addThemeVariants(DialogVariant.LUMO_NO_PADDING);
        dialog.open();
    }

    private void createDialogueToShowEntities(List<BindingSet> tripleList) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Entities Inspection");
        VerticalLayout dialogLayout = createDialogContentForShowingEntities(tripleList);
        dialog.add(dialogLayout);
        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.addThemeVariants(DialogVariant.LUMO_NO_PADDING);
        dialog.open();
    }

    private void createDialogueToShowEntitiesHavingMissingProperty(List<BindingSet> tripleList) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Entities Inspection");
        VerticalLayout dialogLayout = createDialogContentForShowingEntitiesHavingMissingProperty(tripleList);
        dialog.add(dialogLayout);
        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.addThemeVariants(DialogVariant.LUMO_NO_PADDING);
        dialog.open();
    }

    private VerticalLayout createDialogContentForShowingEntitiesWithPropAndObject(List<BindingSet> result) {
        List<Triple> tripleList = new ArrayList<>();
        result.forEach(bindings -> {
            tripleList.add(new Triple(bindings.getBinding("subject").getValue().stringValue(),
                    bindings.getBinding("predicate").getValue().stringValue(),
                    bindings.getBinding("object").getValue().stringValue()));
        });

        Grid<Triple> grid = new Grid<>(Triple.class, false);

        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addColumn(Triple::getSubject).setHeader("Subject (Entity IRI)").setResizable(true).setSortable(true);
        grid.addColumn(Triple::getPredicate).setHeader("Property").setResizable(true).setSortable(true);
        grid.addColumn(Triple::getObject).setHeader("Object").setResizable(true).setSortable(true);

        grid.addColumn(new ComponentRenderer<>(Button::new, (button, triple) -> {
            button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
            button.addClickListener(e -> this.generateQueryToUpdateTriple(triple));
            button.setIcon(new Icon(VaadinIcon.EDIT));
        })).setHeader(setHeaderWithInfoLogo("Edit", "A SPARQL Query to edit this triple will be constructed."));


        grid.addColumn(new ComponentRenderer<>(Button::new, (button, triple) -> {
            button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
            button.addClickListener(e -> this.generateQueryToRemoveTriple(triple));
            button.setIcon(new Icon(VaadinIcon.TRASH));
        })).setHeader(setHeaderWithInfoLogo("Delete", "A SPARQL Query to remove this triple will be constructed."));

        grid.getStyle().set("width", "1500px").set("max-width", "100%");
        grid.setItems(tripleList);

        Button generateDeleteQueryButton = Utils.getPrimaryButton("Generate Query to Delete the selected Triples");
        generateDeleteQueryButton.setAutofocus(true);
        generateDeleteQueryButton.setVisible(false);
        generateDeleteQueryButton.setId("alignButtonCenter");
        AtomicReference<Set<Triple>> selectedItems = new AtomicReference<>();
        grid.addSelectionListener(selection -> {
            selectedItems.set(selection.getAllSelectedItems());
            generateDeleteQueryButton.setVisible(selection.getAllSelectedItems().size() > 0);
        });

        VerticalLayout dialogLayout = new VerticalLayout(grid, generateDeleteQueryButton);
        dialogLayout.setPadding(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("min-width", "1500px").set("max-width", "100%").set("height", "100%");

        generateDeleteQueryButton.addClickListener(buttonClickEvent -> {
            generateDeleteQueryForSelectedData(selectedItems.get());
        });

        return dialogLayout;
    }

    private VerticalLayout createDialogContentForShowingEntities(List<BindingSet> tripleList) {
        List<Triple> subjectList = new ArrayList<>();
        tripleList.forEach(bindings -> {
            subjectList.add(new Triple(bindings.getBinding("val").getValue().stringValue(), "", ""));
        });

        Grid<Triple> grid = new Grid<>(Triple.class, false);

        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addColumn(Triple::getSubject).setHeader("IRI or Literal value").setResizable(true).setSortable(true);

        grid.addColumn(new ComponentRenderer<>(Button::new, (button, triple) -> {
            button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
            button.addClickListener(e -> this.generateQueryToAddType(triple));
            button.setIcon(new Icon(VaadinIcon.PLUS));
        })).setHeader(setHeaderWithInfoLogo("Generate Insert Query", "A SPARQL Query to insert type of this entity will be constructed."));


        grid.getStyle().set("width", "1500px").set("max-width", "100%");
        grid.setItems(subjectList);
        String title = "IRIs having undefined type";
        if (!suggestionsWithSupport.isEmpty()) {
            title = "The table below shows the entities missing type information. SHACTOR suggests the following types/classes for these entities. Select entities using checkbox and select the one of the suggested type:";
        }
        Paragraph paragraph = new Paragraph(title);

        CheckboxGroup<String> checkboxGroup = new CheckboxGroup<>();
        checkboxGroup.setItems(Utils.getTopKeysFromMap(suggestionsWithSupport.keySet(), 5));

        Button generateInsertQueryButton = Utils.getPrimaryButton("Generate Query to Insert Missing values for Selected Entities");
        //generateInsertQueryButton.setWidth("300px");
        generateInsertQueryButton.setAutofocus(true);
        generateInsertQueryButton.setId("alignButtonCenter");
        generateInsertQueryButton.setVisible(false);

        //VerticalLayout vlForCheckBoxItems = new VerticalLayout(checkboxGroup);
        Div divForCheckBoxItems = new Div(checkboxGroup);
        divForCheckBoxItems.setId("divForCheckBoxes");
        divForCheckBoxItems.setMinWidth("1500px");

        //vlForCheckBoxItems.setMaxHeight("15%");
        VerticalLayout dialogLayout = new VerticalLayout(paragraph, divForCheckBoxItems, grid, generateInsertQueryButton);
        dialogLayout.setPadding(true);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("min-width", "1500px").set("max-width", "100%").set("height", "100%");


        AtomicReference<Set<Triple>> selectedItems = new AtomicReference<>();
        grid.addSelectionListener(selection -> {
            selectedItems.set(selection.getAllSelectedItems());
            generateInsertQueryButton.setVisible(checkboxGroup.getSelectedItems().size() > 0 && selection.getAllSelectedItems().size() > 0);
        });

        checkboxGroup.addSelectionListener(listener -> {
            if (selectedItems.get() != null) {
                generateInsertQueryButton.setVisible(checkboxGroup.getSelectedItems().size() > 0 && selectedItems.get().size() > 0);
            }
        });
        generateInsertQueryButton.addClickListener(buttonClickEvent -> {
            generateInsertQueryForSelectedData(selectedItems.get(), checkboxGroup.getSelectedItems());
        });
        return dialogLayout;
    }

    private VerticalLayout createDialogContentForShowingEntitiesHavingMissingProperty(List<BindingSet> tripleList) {
        List<Triple> subjectList = new ArrayList<>();
        tripleList.forEach(bindings -> {
            subjectList.add(new Triple(bindings.getBinding("entity").getValue().stringValue(), "", ""));
        });

        Grid<Triple> grid = new Grid<>(Triple.class, false);

        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addColumn(Triple::getSubject).setHeader("Entity IRI").setResizable(true).setSortable(true);

        grid.addColumn(new ComponentRenderer<>(Button::new, (button, triple) -> {
            button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
            button.addClickListener(e -> this.generateQueryToAddProperty(triple));
            button.setIcon(new Icon(VaadinIcon.PLUS));
        })).setHeader(setHeaderWithInfoLogo("Generate query to add property", "A SPARQL Query to insert property for this entity will be constructed."));


        grid.getStyle().set("width", "1500px").set("max-width", "100%");
        grid.setItems(subjectList);
        Paragraph paragraph = new Paragraph("Entities not having property ' " + propertyShape.getPath() + "' are shown below. " +
                "If you want to insert this property for a given entity, click on the + icon to generate the INSERT Query. ");
        VerticalLayout dialogLayout = new VerticalLayout(paragraph, grid);
        dialogLayout.setPadding(true);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("min-width", "1500px").set("max-width", "100%").set("height", "100%");

        return dialogLayout;
    }

    private VerticalLayout createDialogContentForShowingTriples(List<Triple> tripleList) {
        Grid<Triple> grid = new Grid<>(Triple.class, false);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addColumn(Triple::getSubject).setHeader("Subject").setResizable(true).setSortable(true);
        grid.addColumn(Triple::getPredicate).setHeader("Predicate").setResizable(true).setSortable(true);
        grid.addColumn(Triple::getObject).setHeader("Object").setResizable(true).setSortable(true);
        grid.addColumn(new ComponentRenderer<>(Button::new, (button, triple) -> {
            button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
            button.addClickListener(e -> this.generateQueryToRemoveTriple(triple));
            button.setIcon(new Icon(VaadinIcon.TRASH));
        })).setHeader(setHeaderWithInfoLogo("Delete", "A SPARQL Query to remove this triple will be executed."));
        grid.getStyle().set("width", "2000px").set("max-width", "100%");
        grid.setItems(tripleList);

        Button generateDeleteQueryButton = Utils.getPrimaryButton("Generate Query to Delete the selected Triples");
        generateDeleteQueryButton.setAutofocus(true);
        generateDeleteQueryButton.setId("alignButtonCenter");
        generateDeleteQueryButton.setVisible(false);


        AtomicReference<Set<Triple>> selectedItems = new AtomicReference<>();
        grid.addSelectionListener(selection -> {
            selectedItems.set(selection.getAllSelectedItems());
            generateDeleteQueryButton.setVisible(selection.getAllSelectedItems().size() > 0);
        });

        VerticalLayout dialogLayout = new VerticalLayout(grid, generateDeleteQueryButton);
        dialogLayout.setPadding(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("min-width", "1500px").set("max-width", "100%").set("height", "100%");

        generateDeleteQueryButton.addClickListener(buttonClickEvent -> {
            generateDeleteQueryForSelectedData(selectedItems.get());
        });

        return dialogLayout;
    }

    private void generateDeleteQueryForSelectedData(Set<Triple> triples) {
        StringBuilder deleteQuery = new StringBuilder();
        deleteQuery.append("DELETE \nWHERE { \n");
        for (Triple triple : triples) {
            deleteQuery.append("\t<").append(triple.getSubject()).append(">  <").append(triple.getPredicate()).append(">  <").append(triple.getObject()).append("> . \n");
        }
        deleteQuery.append("}\n");
        String title = "SPARQL Query to delete selected triples ";
        String description = "Here is the generated delete query for the selected triples. Please execute it on your graph to make the changes. ";
        DialogUtil.getDialogWithHeaderAndFooter(title, deleteQuery.toString(), description);
    }

    private void generateInsertQueryForSelectedData(Set<Triple> triples, Set<String> entityTypes) {
        StringBuilder insertQuery = new StringBuilder();
        insertQuery.append("INSERT { \n ");
        for (Triple triple : triples) {
            for (String type : entityTypes) {
                insertQuery.append("\t<").append(triple.getSubject()).append(">  ").append(Constants.RDF_TYPE).append("  <").append(type).append("> . \n");
            }
        }
        insertQuery.append("}\nWHERE { } \n");
        String title = "SPARQL Query to add *Missing* type of the IRI ";
        String description = "Here is the generated insert query for the selected entities and types. Please execute it on your graph to make the changes. ";
        DialogUtil.getDialogWithHeaderAndFooter(title, insertQuery.toString(), description);
    }

    private void generateQueryToAddProperty(Triple triple) {
        String insertPart = "INSERT { \n <" + triple.getSubject() + ">  <" + propertyShape.getPath() + ">  <VALUE_TO_ADD> \n } \n";
        String tripleClause = "WHERE { <" + triple.getSubject() + ">  a  <" + nodeShape.getTargetClass().stringValue() + ">  } \n";
        String updateQuery = "\n" + insertPart + tripleClause + "\n\n";
        String title = "SPARQL Query to add *Missing* property for entity";
        String description = "Here is the insert query, you can update the desired value by replacing 'VALUE_TO_ADD' and copy this query to execute it on your graph!";
        DialogUtil.getDialogWithHeaderAndFooter(title, updateQuery, description);
    }

    private void generateQueryToAddType(Triple triple) {
        String insertPart = "INSERT { \n <" + triple.getSubject() + ">  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>  <VALUE_TO_ADD> \n } \n";
        String tripleClause = "WHERE {} ";
        String updateQuery = "\n" + insertPart + tripleClause + "\n\n";
        String title = "SPARQL Query to add *Missing* type of the IRI ";
        String description = "Here is the insert query, you can update the desired value by replacing 'VALUE_TO_ADD' and copy this query to execute it on your graph!";
        if (!suggestionsWithSupport.isEmpty()) {
            DialogUtil.getDialogWithHeaderAndFooterWithSuggestion(title, triple, updateQuery, description + " We suggest the following to help you find the appropriate missing value for entity " + triple.getSubject() + ": ", new ArrayList<>(Utils.getTopKeysFromMap(suggestionsWithSupport.keySet(), 5)));
        } else {
            DialogUtil.getDialogWithHeaderAndFooter(title, updateQuery, description);
        }
    }

    private void generateQueryToRemoveTriple(Triple triple) {
        String deleteQuery = "\nDELETE \nWHERE { \n\t  <" + triple.getSubject() + ">  <" + triple.getPredicate() + ">  <" + triple.getObject() + "> .\n } ";
        DialogUtil.getDialogWithHeaderAndFooter("SPARQL Query to Delete Triple ", deleteQuery, "Here is the delete query, you can copy this query to execute it on your graph! ");
    }

    private void generateQueryToUpdateTriple(Triple triple) {
        String delPart = "DELETE { \n <" + triple.getSubject() + ">  <" + triple.getPredicate() + ">  <" + triple.getObject() + ">  \n} \n";
        String insertPart = "INSERT { \n <" + triple.getSubject() + ">  <" + triple.getPredicate() + ">  <VALUE_TO_REPLACE> \n } \n";
        String tripleClause = " WHERE { \n  <" + triple.getSubject() + ">  <" + triple.getPredicate() + ">  <" + triple.getObject() + "> \n } ";
        String updateQuery = "\n\n" + delPart + insertPart + tripleClause + "\n\n";

        String title = "SPARQL Query to Update Entity Information ";
        String description = "Here is the update query, you can update the desired value by replacing 'VALUE_TO_REPLACE' and copy this query to execute it on your graph!";
        DialogUtil.getDialogWithHeaderAndFooter(title, updateQuery, description + "SHACTOR is unable to suggest more information for this particular IRI. Please look over the internet. Thanks.");
    }

    private String buildSyntaxForNsAndPs() {
        String nsType = wab(this.nodeShape.getIri().toString()) + " " + wab(RDF.type.toString()) + " " + wab(SHACL.NODE_SHAPE.toString());
        String nsTarget = wab(this.nodeShape.getIri().toString()) + " " + wab(SHACL.TARGET_CLASS.toString()) + " " + wab(this.nodeShape.getTargetClass().toString());
        String nsPs = wab(this.nodeShape.getIri().toString()) + " " + wab(SHACL.PROPERTY.toString()) + " " + wab(this.propertyShape.getIri().toString());

        String psPath = wab(this.propertyShape.getIri().toString()) + " " + wab(SHACL.PATH.toString()) + " " + wab(this.propertyShape.getPath());
        String psNodeKind = wab(this.propertyShape.getIri().toString()) + " " + wab(SHACL.NODE_KIND.toString()) + " " + wab(this.propertyShape.getNodeKind());
        String psNodeType = wab(this.propertyShape.getIri().toString()) + " " + wab(SHACL.DATATYPE.toString()) + " " + wab(this.propertyShape.getDataTypeOrClass());
        String psSupport = wab(this.propertyShape.getIri().toString()) + " " + wab(Constants.SUPPORT) + " " + this.propertyShape.getSupport();
        String psConfidence = wab(this.propertyShape.getIri().toString()) + " " + wab(Constants.CONFIDENCE) + " " + this.propertyShape.getConfidence();
        return nsType + " . \n" + nsTarget + " . \n" + nsPs + " . \n" + psPath + " .\n" + psNodeKind + " .\n" + psNodeType + " .\n" + psSupport + " .\n" + psConfidence + " .\n";
    }

    private static void simplifyOutput(List<Triple> tripleList) {
        for (Triple triple : tripleList) {
            if (triple.getPredicate().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                triple.setPredicate("rdf:type");
            }
            if (triple.getPredicate().contains("http://dbpedia.org/ontology/")) {
                triple.setPredicate(triple.getPredicate().replace("http://dbpedia.org/ontology/", "dbo:"));
            }

            if (triple.getObject().contains("http://dbpedia.org/ontology/")) {
                triple.setObject(triple.getObject().replace("http://dbpedia.org/ontology/", "dbo:"));
            }

            if (triple.getSubject().contains("http://dbpedia.org/resource/")) {
                triple.setSubject(triple.getSubject().replace("http://dbpedia.org/resource/", "dbr:"));
            }
            if (triple.getObject().contains("http://dbpedia.org/resource/")) {
                triple.setObject(triple.getObject().replace("http://dbpedia.org/resource/", "dbr:"));
            }

            if (triple.getObject().contains("http://www.w3.org/2002/07/owl#")) {
                triple.setObject(triple.getObject().replace("http://www.w3.org/2002/07/owl#", "owl:"));
            }
        }
    }

    public Icon createIcon(VaadinIcon vaadinIcon) {
        Icon icon = vaadinIcon.create();
        icon.getStyle().set("padding", "var(--lumo-space-xs");
        return icon;
    }
}
