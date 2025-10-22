package shactor;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import cs.Main;
import cs.qse.common.encoders.StringEncoder;
import cs.qse.filebased.Parser;
import cs.qse.querybased.nonsampling.QbParser;
import org.apache.commons.io.FileUtils;
import shactor.utils.Type;
import shactor.utils.Utils;

import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.*;
import java.util.function.Consumer;

@Tag("selection-view")
@JsModule("./selection-view.ts")
@Route("/selection-view")
public class SelectionView extends LitTemplate {
    @Id("completeShapesExtractionButton")
    private static Button completeShapesExtractionButton;
    @Id("contentVerticalLayout")
    private VerticalLayout contentVerticalLayout;
    @Id("graphInfo")
    private static H5 graphInfo;
    @Id("vaadinGrid")
    private static Grid<Type> vaadinGrid;
    @Id("searchField")
    private static TextField searchField;
    @Id("footerLeftImage")
    private Image footerLeftImage;
    @Id("footerRightImage")
    private Image footerRightImage;
    @Id("graphStatsCheckBox")
    private static Checkbox graphStatsCheckBox;
    static Parser parser;
    static QbParser qbParser;
    public static List<String> chosenClasses;
    public static Set<Integer> chosenClassesEncoded;
    public static Boolean isFilteredClasses = false;
    public static HashMap<String, String> defaultShapesModelStats;

    public static String defaultShapesOutputFileAddress = "";
    public static Boolean computeStats = false;

    public static String outputDirectory = "";

    public SelectionView() {
        Utils.setFooterImagesPath(footerLeftImage, footerRightImage);
        graphInfo.setVisible(false);
        completeShapesExtractionButton.setEnabled(false);
        searchField.setVisible(false);
        vaadinGrid.setVisible(false);

        beginParsing();
    }

    private static void beginParsing() {
        setPaths();
        try {
            switch (IndexView.category) {
                case EXISTING_FILE_BASED -> {
                    if (IndexView.graphURL == null || IndexView.graphURL.isBlank()) {
                        Utils.notify("No dataset path configured.", NotificationVariant.LUMO_ERROR,
                                Notification.Position.TOP_CENTER);
                        return;
                    }
                    // Preflight validation to avoid crashing inside external Parser
                    if (isInfoboxPropertiesFile(IndexView.graphURL)) {
                        Utils.notify(
                                "The selected DBpedia file 'infobox_properties_en.nt' contains infobox property triples, not rdf:type class assertions. Class extraction cannot run on this file. Please use 'instance_types_en.nt' (or 'instance_types_en.ttl') instead.",
                                NotificationVariant.LUMO_ERROR, Notification.Position.TOP_CENTER);
                        return;
                    }
                    if (!preflightHasRdfType(IndexView.graphURL)) {
                        Utils.notify(
                                "The selected dataset does not appear to contain rdf:type triples required for class extraction. Please choose a dataset with type assertions (e.g., DBpedia 'instance_types_en.nt').",
                                NotificationVariant.LUMO_ERROR, Notification.Position.TOP_CENTER);
                        return;
                    }
                    parser = new Parser(IndexView.graphURL, 50, 5000,
                            "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>");
                    try {
                        parser.entityExtraction();
                    } catch (ArrayIndexOutOfBoundsException aioobe) {
                        // Provide actionable guidance rather than a raw stacktrace
                        Utils.notify(
                                "Parsing failed: unsupported or malformed dataset content. Make sure the file contains N-Triples/Turtle with rdf:type assertions. For DBpedia, pick 'instance_types_en.nt', not 'infobox_properties_en.nt'.",
                                NotificationVariant.LUMO_ERROR, Notification.Position.TOP_CENTER);
                        return;
                    }
                    setGraphInfo(parser.entityDataHashMap.size(), parser.classEntityCount.size());
                    setupGridInMultiSelectionMode(getClasses(parser.classEntityCount, parser.getStringEncoder()),
                            parser.getStringEncoder(), parser.classEntityCount.size());

                    completeShapesExtractionButton
                            .addClickListener(buttonClickEvent -> completeFileBasedShapesExtraction());
                }
                case CONNECT_END_POINT -> {
                    qbParser = new QbParser(50, "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", IndexView.graphURL,
                            IndexView.endPointRepo);
                    qbParser.getNumberOfInstancesOfEachClass();
                    setGraphInfo(qbParser.getClassEntityCount().size());
                    setupGridInMultiSelectionMode(
                            getClasses(qbParser.getClassEntityCount(), qbParser.getStringEncoder()),
                            qbParser.getStringEncoder(), qbParser.getClassEntityCount().size());

                    completeShapesExtractionButton
                            .addClickListener(buttonClickEvent -> completeQueryBasedShapesExtraction());
                }
            }
            Utils.notify("Graph Parsed Successfully!", NotificationVariant.LUMO_SUCCESS,
                    Notification.Position.TOP_CENTER);
        } catch (Exception ex) {
            ex.printStackTrace();
            Utils.notify("Parsing failed: " + ex.getClass().getSimpleName() + " - " + ex.getMessage(),
                    NotificationVariant.LUMO_ERROR, Notification.Position.TOP_CENTER);
        }
    }

    private static void setPaths() {
        try {
            // Check for Docker environment variables first
            String resourcesPath = System.getenv("QSE_RESOURCES_PATH");
            String configPath = System.getenv("QSE_CONFIG_PATH");
            String outputPath = System.getenv("QSE_OUTPUT_PATH");

            // Fallback to JAR directory detection if not in Docker
            if (resourcesPath == null || configPath == null || outputPath == null) {
                CodeSource codeSource = Parser.class.getProtectionDomain().getCodeSource();
                File jarFile = new File(codeSource.getLocation().toURI().getPath());
                String jarDir = jarFile.getParentFile().getPath();

                if (resourcesPath == null)
                    resourcesPath = jarDir + "/resources/";
                if (configPath == null)
                    configPath = jarDir + "/config/";
                if (outputPath == null)
                    outputPath = jarDir + "/Output/";
            }

            Main.setDataSetNameForJar(buildDatasetName(IndexView.category));
            outputDirectory = outputPath;
            Main.setOutputFilePathForJar(outputPath);
            Main.setConfigDirPathForJar(configPath);
            Main.setResourcesPathForJar(resourcesPath);
            Main.qseFromSpecificClasses = false;

            System.out.println("[SHACTOR] Resources path: " + resourcesPath);
            System.out.println("[SHACTOR] Config path: " + configPath);
            System.out.println("[SHACTOR] Output path: " + outputPath);

            // Ensure Output directory exists
            File outDir = new File(outputDirectory);
            if (!outDir.exists()) {
                boolean created = outDir.mkdirs();
                if (!created) {
                    System.err.println("Warning: Could not create Output directory at " + outputDirectory);
                }
            }

            // Clean output directory (if it exists)
            File[] filesInOutputDir = outDir.listFiles();
            if (filesInOutputDir != null) {
                for (File file : filesInOutputDir) {
                    try {
                        if (file.isDirectory()) {
                            FileUtils.forceDelete(file);
                        } else if (!file.getName().equals(".keep")) {
                            boolean deleted = file.delete();
                            if (deleted) {
                                System.out.println("Deleted already existing file: " + file.getPath());
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("Warning: Could not delete file in Output directory: " + file.getPath());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setupGridInMultiSelectionMode(List<Type> classes, StringEncoder encoder,
            Integer classEntityCountSize) {
        vaadinGrid.setVisible(true);
        vaadinGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        vaadinGrid.addColumn(Type::getName).setHeader(Utils.boldHeader("Class IRI")).setSortable(true);
        vaadinGrid.addColumn(Type::getInstanceCount).setHeader(Utils.boldHeader("Class Instance Count"))
                .setSortable(true);

        vaadinGrid.setItems(classes);
        GridListDataView<Type> dataView = vaadinGrid.setItems(classes);
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> dataView.refreshAll());

        dataView.addFilter(type -> {
            String searchTerm = searchField.getValue().trim();
            if (searchTerm.isEmpty())
                return true;
            return matchesTerm(type.getName(), searchTerm);
        });

        vaadinGrid.addSelectionListener(selection -> {
            // System.out.printf("Number of selected classes: %s%n",
            // selection.getAllSelectedItems().size());
            if (selection.getAllSelectedItems().size() == classEntityCountSize) {
                System.out.println("Extract Shapes for All Classes");
                chosenClasses = new ArrayList<>();
                chosenClassesEncoded = new HashSet<>();
            } else {
                System.out.println("Extract Shapes for Chosen Classes");
                chosenClasses = new ArrayList<>();
                chosenClassesEncoded = new HashSet<>();
                selection.getAllSelectedItems().forEach(item -> {
                    chosenClasses.add(item.getName());
                    chosenClassesEncoded.add(encoder.encode(item.getName()));
                });
            }
            completeShapesExtractionButton.setEnabled(selection.getAllSelectedItems().size() > 0);
        });
        searchField.setVisible(true);
    }

    // Transform extracted classes to Type
    private static List<Type> getClasses(Map<Integer, Integer> classEntityCountMap, StringEncoder stringEncoder) {
        List<Type> types = new ArrayList<>();
        classEntityCountMap.forEach((k, v) -> {
            Type t = new Type();
            t.setName(stringEncoder.decode(k));
            t.setEncodedKey(v);
            t.setInstanceCount(v);
            types.add(t);
        });
        types.sort((d1, d2) -> d2.getInstanceCount() - d1.getInstanceCount());
        return types;
    }

    private static void setGraphInfo(int entityCount, int classCount) {
        graphInfo.setVisible(true);
        String info = "No. of entities: " + Utils.formatWithCommas(entityCount) + " ; " + "No. of classes: "
                + Utils.formatWithCommas(classCount)
                + ". Please select the classes from the table below for which you want to extract shapes.";
        graphInfo.setText(info);
    }

    private static void setGraphInfo(int classCount) {
        graphInfo.setVisible(true);
        String info = "No. of classes: " + classCount
                + ". Please select the classes from the table below for which you want to extract shapes.";
        graphInfo.setText(info);
    }

    private static void completeFileBasedShapesExtraction() {
        parser.entityConstraintsExtraction();
        parser.computeSupportConfidence();

        if (chosenClasses.size() > 0) {
            isFilteredClasses = true;
            System.out.println(chosenClasses);
            defaultShapesOutputFileAddress = parser.extractSHACLShapes(true, chosenClasses);
        } else {
            isFilteredClasses = false;
            defaultShapesOutputFileAddress = parser.extractSHACLShapes(false, chosenClasses);
        }
        defaultShapesModelStats = parser.shapesExtractor.getCurrentShapesModelStats();
        // Utils.notifyMessage(graphStatsCheckBox.getValue().toString());
        computeStats = graphStatsCheckBox.getValue();
        completeShapesExtractionButton.getUI().ifPresent(ui -> ui.navigate("extraction-view"));
    }

    private static void completeQueryBasedShapesExtraction() {
        if (chosenClassesEncoded.size() > 0) {
            isFilteredClasses = true;
            qbParser.setClasses(chosenClassesEncoded);
        } else {
            isFilteredClasses = false;
            qbParser.getDistinctClasses();
        }
        qbParser.getShapesInfoAndComputeSupport();
        defaultShapesOutputFileAddress = qbParser.extractSHACLShapes();
        qbParser.writeSupportToFile();
        defaultShapesModelStats = qbParser.shapesExtractor.getCurrentShapesModelStats();
        // Utils.notifyMessage(graphStatsCheckBox.getValue().toString());
        computeStats = graphStatsCheckBox.getValue();
        completeShapesExtractionButton.getUI().ifPresent(ui -> ui.navigate("extraction-view"));
    }

    public static Parser getParser() {
        return parser;
    }

    public static QbParser getQbParser() {
        return qbParser;
    }

    public static String getDefaultShapesOutputFileAddress() {
        return defaultShapesOutputFileAddress;
    }

    public static String buildDatasetName(IndexView.Category category) {
        String name = "file";
        switch (category) {
            case EXISTING_FILE_BASED -> {
                String[] parts = IndexView.graphURL.split("/");
                String filename = parts[parts.length - 1];
                // Remove file extension to get clean dataset name
                if (filename.contains(".")) {
                    name = filename.substring(0, filename.lastIndexOf("."));
                } else {
                    name = filename;
                }
            }
            case CONNECT_END_POINT -> name = IndexView.endPointRepo;
            case ANALYZE_SHAPES -> System.out.println("High level");
        }
        return name;
    }

    private static boolean matchesTerm(String value, String searchTerm) {
        return value.toLowerCase().contains(searchTerm.toLowerCase());
    }

    // Not used for now, but will be useful if you have to create filter over
    // columns of grid
    private static class TypeFilter {
        private final GridListDataView<Type> dataView;

        private String name;
        private Integer instanceCount;

        public TypeFilter(GridListDataView<Type> dataView) {
            this.dataView = dataView;
            this.dataView.addFilter(this::test);
        }

        public void setName(String fullName) {
            this.name = fullName;
            this.dataView.refreshAll();
        }

        public boolean test(Type type) {
            return matches(type.getName(), name);
        }

        private boolean matches(String value, String searchTerm) {
            return searchTerm == null || searchTerm.isEmpty() || value.toLowerCase().contains(searchTerm.toLowerCase());
        }

    }

    private static Component createFilterHeader(String labelText, Consumer<String> filterChangeConsumer) {
        Label label = new Label(labelText);
        label.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        TextField textField = new TextField();
        textField.setValueChangeMode(ValueChangeMode.EAGER);
        textField.setClearButtonVisible(true);
        textField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        textField.setWidthFull();
        textField.getStyle().set("max-width", "100%");
        textField.addValueChangeListener(e -> filterChangeConsumer.accept(e.getValue()));
        VerticalLayout layout = new VerticalLayout(label, textField);
        layout.getThemeList().clear();
        layout.getThemeList().add("spacing-xs");

        return layout;
    }

    private static boolean isInfoboxPropertiesFile(String filePath) {
        if (filePath == null)
            return false;
        String lower = filePath.toLowerCase(Locale.ROOT);
        return lower.contains("infobox_properties") || lower.endsWith("infobox_properties_en.nt")
                || lower.endsWith("infobox_properties_en.ttl");
    }

    private static boolean preflightHasRdfType(String filePath) {
        try {
            Path p = Path.of(filePath);
            if (!Files.exists(p)) {
                System.err.println("Preflight failed: file does not exist: " + filePath);
                return false;
            }
            if (Files.size(p) == 0) {
                System.err.println("Preflight failed: file is empty: " + filePath);
                return false;
            }
            int maxLines = 500; // quick scan
            int read = 0;
            try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                String line;
                while ((line = br.readLine()) != null && read < maxLines) {
                    read++;
                    if (line.isEmpty() || line.startsWith("#"))
                        continue;
                    // simple checks for rdf:type presence in common encodings
                    if (line.contains("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")
                            || line.contains(" rdf:type ")) {
                        return true;
                    }
                }
            }
            System.err.println(
                    "Preflight warning: no rdf:type predicate found in first " + read + " lines of " + filePath);
            return false;
        } catch (IOException ioe) {
            System.err.println("Preflight failed due to IO error: " + ioe.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Preflight failed: " + e.getMessage());
            return false;
        }
    }
}
