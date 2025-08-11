package shactor.utils;

import com.storedobject.chart.SOChart;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.server.StreamResource;
import cs.utils.Constants;
import org.apache.commons.io.FileUtils;
import org.vaadin.olli.FileDownloadWrapper;
import shactor.SelectionView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class DialogUtil {
    public static Button actionButton;

    public static void getDialogWithHeaderAndFooter(String title, String textAreaText, String infoParagraphText) {
        Dialog dialog = new Dialog();
        dialog.getElement().setAttribute("aria-label", "Dialog");
        dialog.getHeader().add(getHeaderTitle(title));
        createFooter(dialog, "Execute");
        VerticalLayout dialogLayout = createDialogLayout(textAreaText, infoParagraphText);
        dialog.add(dialogLayout);
        dialog.setModal(false);
        dialog.setDraggable(true);
        dialog.open();
    }

    public static void getDialogWithHeaderAndFooterForShowingShapeSyntax(String shapesSyntax) {
        // Maintain backward compatibility by defaulting to SHACL format
        getDialogWithHeaderAndFooterForShowingShapeSyntax(shapesSyntax, "SHACL");
    }

    /**
     * Shows a dialog with shape syntax and format-aware download functionality.
     * 
     * This method creates a dialog that displays the generated shapes syntax and provides
     * a download button with the correct file extension based on the selected format.
     * 
     * @param shapesSyntax The generated shapes syntax to display
     * @param format The format of the shapes ("SHACL" or "ShEx")
     */
    public static void getDialogWithHeaderAndFooterForShowingShapeSyntax(String shapesSyntax, String format) {
        Dialog dialog = new Dialog();
        dialog.getElement().setAttribute("aria-label", "Dialog");
        
        // Dynamic header title based on format
        String headerTitle = format.equals("ShEx") ? "ShEx Shapes" : "SHACL Shapes";
        dialog.getHeader().add(getHeaderTitle(headerTitle));
        
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        Button button = new Button();
        Utils.setIconForButtonWithToolTip(button, VaadinIcon.DOWNLOAD, "Download");
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        FileDownloadWrapper buttonWrapper;
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(shapesSyntax.getBytes());
            
            // Dynamic filename and extension based on format
            String filename = format.equals("ShEx") ? "selectedShapes.shex" : "selectedShapes.ttl";
            buttonWrapper = new FileDownloadWrapper(new StreamResource(filename, () -> stream));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        buttonWrapper.wrapComponent(button);
        dialog.getFooter().add(buttonWrapper, cancelButton);

        TextArea syntaxArea = new TextArea();
        syntaxArea.setValue(shapesSyntax);


        syntaxArea.getStyle().set("resize", "vertical"); //https://cookbook.vaadin.com/resizable-components
        syntaxArea.getStyle().set("overflow", "auto");
        syntaxArea.setHeight("300px");

        VerticalLayout fieldLayout = new VerticalLayout(syntaxArea);
        fieldLayout.setSpacing(false);
        fieldLayout.setPadding(false);
        fieldLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        fieldLayout.getStyle().set("width", "1200px").set("max-width", "100%");

        dialog.add(fieldLayout);
        dialog.setModal(true);
        dialog.setDraggable(false);
        dialog.open();
    }

    public static void getDialogWithHeaderAndFooterWithSuggestion(String title, Triple triple, String textAreaText, String infoParagraphText, ArrayList<String> suggestions) {
        Dialog dialog = new Dialog();
        dialog.getElement().setAttribute("aria-label", "Dialog");
        dialog.getHeader().add(getHeaderTitle(title));
        createFooter(dialog, "Execute");
        VerticalLayout dialogLayout = createDialogLayoutWithSuggestion(textAreaText, triple, infoParagraphText, suggestions);
        dialog.add(dialogLayout);
        dialog.setModal(false);
        dialog.setDraggable(true);
        dialog.open();
    }

    public static Dialog getDialogToDisplayChartWithHeaderAndFooter(String title, SOChart chart) {
        Dialog dialog = new Dialog();
        dialog.getElement().setAttribute("aria-label", "Dialog");
        dialog.getHeader().add(getHeaderTitle(title));
        createFooter(dialog, "Execute");
        VerticalLayout dialogLayout = createDialogLayoutForChart("Chart", chart);
        dialog.add(dialogLayout);
        dialog.setModal(false);
        dialog.setDraggable(true);
        dialog.setResizable(true);
        //dialog.open();
        return dialog;
    }

    private static H2 getHeaderTitle(String title) {
        H2 headline = new H2(title);
        headline.getStyle().set("padding-bottom", "0px");
        headline.addClassName("draggable");
        headline.getStyle().set("margin", "0").set("font-size", "1.5em").set("font-weight", "bold").set("cursor", "move").set("padding", "var(--lumo-space-m) 0").set("flex", "1");
        return headline;
    }

    private static VerticalLayout createDialogLayout(String textAreaText, String paragraphText) {
        Paragraph paragraph = new Paragraph(paragraphText);
        TextArea descriptionArea = new TextArea();
        descriptionArea.setValue(textAreaText);
        VerticalLayout fieldLayout = new VerticalLayout(paragraph, descriptionArea);
        fieldLayout.setSpacing(false);
        fieldLayout.setPadding(false);
        fieldLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        fieldLayout.getStyle().set("width", "1200px").set("max-width", "100%");
        return fieldLayout;
    }


    private static VerticalLayout createDialogLayoutWithSuggestion(String textAreaText, Triple triple, String paragraphText, ArrayList<String> suggestions) {
        TextArea descriptionArea = new TextArea();
        descriptionArea.setValue(textAreaText);
        VerticalLayout fieldLayout = new VerticalLayout(descriptionArea);

        CheckboxGroup<String> checkboxGroup = new CheckboxGroup<>();
        checkboxGroup.setItems(suggestions);

        checkboxGroup.addSelectionListener(listener -> {
            if (listener.getAllSelectedItems().size() > 0) {
                StringBuilder insertQuery = new StringBuilder();
                insertQuery.append("INSERT { \n ");
                listener.getAllSelectedItems().forEach(type -> {
                    insertQuery.append("\t<").append(triple.getSubject()).append(">  ").append(Constants.RDF_TYPE).append("  <").append(type).append("> . \n");
                });
                insertQuery.append("}\nWHERE { } \n");
                descriptionArea.setValue(insertQuery.toString());
            } else {
                descriptionArea.setValue(textAreaText);
            }
        });
        Div divForCheckBoxItems = new Div(checkboxGroup);
        divForCheckBoxItems.setId("divForCheckBoxes");
        divForCheckBoxItems.setMinWidth("1200px");
        fieldLayout.add(divForCheckBoxItems);
        fieldLayout.add(new Paragraph("Please select one of the above suggested type or edit the 'VALUE_TO_ADD'."));
        fieldLayout.add(descriptionArea);
        fieldLayout.setSpacing(false);
        fieldLayout.setPadding(false);
        fieldLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        fieldLayout.getStyle().set("width", "1200px").set("max-width", "100%");
        return fieldLayout;
    }

    private static VerticalLayout createDialogLayoutForChart(String label, SOChart chart) {
        Paragraph paragraph = new Paragraph(label);
        VerticalLayout fieldLayout = new VerticalLayout(paragraph, chart);
        fieldLayout.setSpacing(false);
        fieldLayout.setPadding(false);
        fieldLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        fieldLayout.getStyle().set("width", "1000px").set("max-width", "100%");
        return fieldLayout;
    }

    private static void createFooter(Dialog dialog, String buttonLabel) {
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        actionButton = new Button(buttonLabel, e -> dialog.close());
        actionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(cancelButton);
        //dialog.getFooter().add(actionButton);
        //actionButton.addClickListener(buttonClickEvent -> {});
    }

}
