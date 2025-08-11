package shactor.utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import cs.qse.common.structure.NS;
import cs.qse.common.structure.PS;
import cs.qse.common.structure.ShaclOrListItem;
import cs.utils.Tuple2;
import shactor.config.ConfigurationManager;
import shactor.utils.formatters.ShapeFormatterFactory;
import shactor.utils.formatters.ShaclFormatter;
import shactor.utils.formatters.ShExFormatter;
import de.atextor.turtle.formatter.FormattingStyle;
import de.atextor.turtle.formatter.TurtleFormatter;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Utils {
    public static VerticalLayout getVerticalLayout() {
        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSizeFull();
        verticalLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        return verticalLayout;
    }

    public static TextField getTextField(String label) {
        TextField textField = new TextField();
        textField.setWidth("50%");
        textField.setLabel(label);
        return textField;
    }

    public static Paragraph getParagraph(String text) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.setClassName("bold-paragraph");
        return paragraph;
    }

    public static TextField getReadOnlyTextField(String label, String value) {
        TextField textField = new TextField();
        textField.setReadOnly(true);
        textField.setLabel(label);
        textField.setValue(value);
        textField.setWidth("50%");
        textField.addThemeName("label-design");
        return textField;
    }


    public static Button getPrimaryButton(String label) {
        Button primaryButton = new Button(label);
        primaryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return primaryButton;
    }

    public static Button getSecondaryButton(String label) {
        Button primaryButton = new Button(label);
        primaryButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        return primaryButton;
    }

    public static RadioButtonGroup<String> getRadioButtonGroup(String label, List<String> items) {
        RadioButtonGroup<String> radioGroup = new RadioButtonGroup<>();
        radioGroup.setWidth("50%");
        //radioGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        radioGroup.setLabel(label);
        radioGroup.setItems(items);
        radioGroup.setValue(items.get(0));
        return radioGroup;
    }

    public static void notify(String message, NotificationVariant notificationVariant, Notification.Position position) {
        Notification notification = Notification.show(message);
        notification.addThemeVariants(notificationVariant);
        notification.setPosition(position);
    }


    public static void notifyError(String message) {
        Notification notification = Notification.show(message);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.setPosition(Notification.Position.MIDDLE);
    }

    public static void notifyMessage(String message) {
        Notification notification = Notification.show(message);
        notification.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
        notification.setPosition(Notification.Position.MIDDLE);
    }

    public static void setFooterImagesPath(Image footerLeftImage, Image footerRightImage) {
        footerLeftImage.setSrc("./images/DKW-Logo.png");
        footerRightImage.setSrc("./images/aau.png");
    }

    public static void setIconForButtonWithToolTip(Button button, VaadinIcon icon, String toolTip) {
        button.setIcon(new Icon(icon));
        button.setText("");
        button.setTooltipText(toolTip);
    }


    public static Component setHeaderWithInfoLogo(String headerTitle, String headerDetails) {
        Span span = new Span(Utils.boldHeader(headerTitle));
        Icon icon = VaadinIcon.INFO_CIRCLE.create();
        icon.getElement().setAttribute("title", headerDetails);
        icon.getStyle().set("height", "var(--lumo-font-size-m)").set("color", "var(--lumo-contrast-70pct)").set("margin-right", "10px");

        HorizontalLayout layout = new HorizontalLayout(span, icon);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(false);

        return layout;
    }

    private Icon createStatusIcon(String status) {
        boolean isAvailable = "true".equals(status);
        Icon icon;
        if (isAvailable) {
            icon = VaadinIcon.CHECK.create();
            icon.getElement().getThemeList().add("badge success");
        } else {
            icon = VaadinIcon.CLOSE_SMALL.create();
            icon.getElement().getThemeList().add("badge error");
        }
        icon.getStyle().set("padding", "var(--lumo-space-xs");
        return icon;
    }


    public static LinkedHashMap<String, Integer> sortMapDescending(HashMap<String, Integer> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    public static Html boldHeader(String label) {
        return new Html("<div style='font-weight: bold;'>" + label + "</div>");
    }

    public static boolean matchesTerm(String value, String searchTerm) {
        return value.toLowerCase().contains(searchTerm.toLowerCase());
    }

    public static Select<String> configureAndGetSelectField() {
        Select<String> selectField = new Select<>();
        selectField.setWidth("50%");
        selectField.setLabel("Select from existing datasets");
        selectField.setItems(getDatasetsAddresses().keySet());
        selectField.setValue("DBpedia");
        return selectField;
    }

    public static HashMap<String, String> getDatasetsAddresses() {
        ConfigurationManager config = ConfigurationManager.getInstance();
        return new HashMap<>(config.getDatasetPaths());
    }

    public static HashMap<String, Tuple2<String, String>> getDatasetsEndpointDetails() {
        ConfigurationManager config = ConfigurationManager.getInstance();
        HashMap<String, Tuple2<String, String>> map = new HashMap<>();
        
        // Get all dataset names and create endpoint details for each
        for (String datasetName : config.getDatasetPaths().keySet()) {
            ConfigurationManager.EndpointDetails details = config.getEndpointDetails(datasetName);
            map.put(datasetName, new Tuple2<>(details.getUrl(), details.getRepository()));
        }
        
        return map;
    }

    public static Icon createIcon(VaadinIcon vaadinIcon) {
        Icon icon = vaadinIcon.create();
        icon.getStyle().set("padding", "var(--lumo-space-xs");
        return icon;
    }


    public static String readKey() {
        String key = null;
        try {
            key = new String(Files.readAllBytes(Paths.get("google_api_key.txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return key;
    }

    public static List<String> sortMapByValuesDesc(HashMap<String, Integer> map) {
        // Create a list of entries from the map
        List<Map.Entry<String, Integer>> list = new ArrayList<>(map.entrySet());

        // Sort the list in descending order based on the values
        list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        // Create a list of keys sorted by their corresponding values
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : list) {
            keys.add(entry.getKey());
        }

        return keys;
    }


    public static String formatWithCommas(int number) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(number);
    }

    public static List<String> getTopKeysFromMap(Set<String> keyset, int thershold) {
        List<String> firstNKeys = new ArrayList<>();
        int i = 0;
        for (String key : keyset) {
            firstNKeys.add(key);
            i++;
            if (i == thershold) {
                break;
            }
        }
        return firstNKeys;
    }

    /**
     * Constructs a formatted model for given NodeShapes and their PropertyShapes.
     * 
     * This method maintains backward compatibility by defaulting to SHACL format.
     * For new code, consider using the overloaded method with explicit format parameter.
     * 
     * @param nodeShapes A set of NodeShape objects containing the shapes to format
     * @return A string representation of the shapes in SHACL Turtle format
     * @throws IllegalArgumentException if nodeShapes is null
     * @deprecated Use {@link #constructModelForGivenNodeShapesAndTheirPropertyShapes(Set, String)} instead
     */
    public static String constructModelForGivenNodeShapesAndTheirPropertyShapes(Set<NS> nodeShapes) {
        // Maintain backward compatibility by defaulting to SHACL format
        return constructModelForGivenNodeShapesAndTheirPropertyShapes(nodeShapes, "SHACL");
    }

    /**
     * Constructs a formatted model for given NodeShapes and their PropertyShapes in the specified format.
     * 
     * This method uses the ShapeFormatterFactory to delegate formatting to the appropriate
     * formatter implementation based on the specified format. It supports both SHACL and ShEx
     * formats and can be easily extended to support additional formats in the future.
     * 
     * Supported formats:
     * - "SHACL" - Generates SHACL shapes in Turtle syntax (.ttl)
     * - "ShEx" - Generates ShEx shapes in ShEx syntax (.shex)
     * 
     * The method creates a factory instance with both SHACL and ShEx formatters and uses
     * the factory's convenience method to format the shapes. This approach ensures
     * consistent formatting behavior and proper error handling.
     * 
     * @param nodeShapes A set of NodeShape objects containing the shapes to format.
     *                   Each NodeShape includes its IRI, target class, and associated
     *                   PropertyShapes with their constraints.
     * @param format The desired output format ("SHACL" or "ShEx", case-insensitive)
     * @return A string representation of the shapes in the specified format
     * @throws IllegalArgumentException if nodeShapes is null or format is unsupported
     * @throws RuntimeException if formatting fails due to internal errors
     * 
     * @since Phase 4.2 - ShEx Integration Implementation
     */
    public static String constructModelForGivenNodeShapesAndTheirPropertyShapes(Set<NS> nodeShapes, String format) {
        // Input validation
        if (nodeShapes == null) {
            throw new IllegalArgumentException("NodeShapes cannot be null");
        }
        
        if (format == null || format.trim().isEmpty()) {
            throw new IllegalArgumentException("Format cannot be null or empty");
        }
        
        try {
            // Create factory with both formatter implementations
            // Note: In a full Spring application, this would be injected via @Autowired
            ShaclFormatter shaclFormatter = new ShaclFormatter();
            ShExFormatter shexFormatter = new ShExFormatter();
            ShapeFormatterFactory factory = new ShapeFormatterFactory(shaclFormatter, shexFormatter);
            
            // Use factory to format shapes in the specified format
            return factory.formatShapes(nodeShapes, format);
            
        } catch (IllegalArgumentException e) {
            // Re-throw validation errors with context
            throw new IllegalArgumentException("Failed to format shapes: " + e.getMessage(), e);
        } catch (Exception e) {
            // Wrap unexpected errors
            throw new RuntimeException("Unexpected error during shape formatting: " + e.getMessage(), e);
        }
    }

}
