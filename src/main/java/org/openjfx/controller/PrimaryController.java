package org.openjfx.controller;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.openjfx.dto.ElementType;
import org.openjfx.dto.LoadedElement;
import org.openjfx.dto.ScriptType;
import org.openjfx.service.LoadFromCsvService;
import org.openjfx.service.GitBashService;

public class PrimaryController implements Initializable {

    private static final int SPACING_BETWEEN_BUTTONS = 5;

    @FXML
    public VBox primaryPage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        addSectionHeader("Commands to invoke on environment");
        addElementsToScene(ElementType.SERVICE_COMMAND);
        addSectionHeader("Commands to invoke for replacing DapKeys for local tests");
        addElementsToScene(ElementType.UPDATE_DAP_FOR_TESTS_COMMAND);
        addSectionHeader("Links");
        addElementsToScene(ElementType.LINK);
        addAuthorNote("Made with love by Szymon Gross");
    }

    private void addSectionHeader(String headerName) {
        VBox section = new VBox();
        Text text = new Text(headerName);
        text.setStyle("-fx-font: normal bold 20 Langdon");
        text.setFill(Color.ORANGE);
        section.getChildren().add(text);
        primaryPage.getChildren().add(section);
    }

    private void addAuthorNote(String authorName) {
        VBox section = new VBox();
        section.setAlignment(Pos.CENTER);
        Image image = new Image("shiba2.png");
        ImageView imageView = new ImageView(image);
        imageView.setFitHeight(35);
        imageView.setFitWidth(35);
        Tooltip.install(imageView, createTooltip(authorName));
        Group root = new Group(imageView);
        section.getChildren().add(root);
        primaryPage.getChildren().add(section);
    }

    private void addElementsToScene(ElementType type) {
        List<LoadedElement> loadedElements = loadElementsFromCsvFile(type);
        addElementsToScene(loadedElements, type);
    }

    private void addElementsToScene(List<LoadedElement> loadedElements, ElementType type) {
        Set<AbstractMap.SimpleEntry<Integer, String>> uniqueSectionsSet = loadedElements.stream()
                                                                                        .map(elem -> new AbstractMap.SimpleEntry <>(
                                                                                                elem.getSectionDisplayOrder(), elem.getSectionName()
                                                                                        ))
                                                                                        .collect(Collectors.toSet());

        List<AbstractMap.SimpleEntry<Integer, String>> uniqueSections = new ArrayList<>(uniqueSectionsSet);
        uniqueSections.sort(Map.Entry.comparingByKey());

        for (AbstractMap.SimpleEntry<Integer, String> section : uniqueSections) {
            primaryPage.getChildren().add(createHeaderForSection(section.getValue()));
            primaryPage.setBackground(new Background(new BackgroundFill(Color.rgb(33,33,33), CornerRadii.EMPTY, Insets.EMPTY)));

            HBox rows = new HBox();
            rows.setSpacing(SPACING_BETWEEN_BUTTONS);

            List<LoadedElement> sectionElements = loadedElements.stream()
                                                                .filter(elem -> elem.getSectionName().equals(section.getValue()))
                                                                .sorted(Comparator.comparing(LoadedElement::getCommandOrder))
                                                                .collect(Collectors.toList());
            for (LoadedElement loadedElement : sectionElements) {
                rows.getChildren().add(createButton(
                        loadedElement.getButtonName(), loadedElement.getCommand(), loadedElement.isPopupInputDisplayed(),
                        loadedElement.getPopupInputMessage(), loadedElement.getDescription(), type));
            }
            primaryPage.getChildren().add(rows);
        }
    }

    private VBox createHeaderForSection(String sectionName) {
        VBox section = new VBox();
        Text text = new Text(sectionName);
        text.getStyleClass().add("text");
        text.setFill(Color.ORANGE);
        section.getChildren().add(text);
        return section;
    }

    private Button createButton(String buttonName, String command, boolean popupInputDisplayed,
            String popupInputMessage, String description, ElementType type) {
        Button button = new Button(buttonName);
        if (ElementType.SERVICE_COMMAND.equals(type)) {
            addButtonListenerForServiceCommands(button, popupInputDisplayed, popupInputMessage, command, ScriptType.SERVICE_SCRIPT);
        } else if (ElementType.UPDATE_DAP_FOR_TESTS_COMMAND.equals(type)) {
            addButtonListenerForServiceCommands(button, popupInputDisplayed, popupInputMessage, command, ScriptType.UPDATE_DAP_FOR_TESTS);
        } else if (ElementType.LINK.equals(type)) {
            button.setOnMouseClicked(event -> openPageInBrowser(command));
        }
        button.setTooltip(createTooltip(description));
        return button;
    }

    private void addButtonListenerForServiceCommands(Button button, boolean popupInputDisplayed, String popupInputMessage, String command, ScriptType scriptType) {
        button.setOnMouseClicked(event -> {
            if (popupInputDisplayed) {
                Optional<String> result = createTextInputDialog(popupInputMessage);
                result.ifPresent(name -> {
                    GitBashService.runCommand(scriptType, command + " " + name);
                });
            } else {
                GitBashService.runCommand(scriptType, command);
            }
        });
    }

    private Optional<String> createTextInputDialog(String popupInputMessage) {
        TextInputDialog dialog = new TextInputDialog("default_value");

        dialog.setTitle("Information required");
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        dialog.setContentText(popupInputMessage);

        return dialog.showAndWait();
    }

    private void openPageInBrowser(String url) {
        try {
            URI u = new URI(url);
            java.awt.Desktop.getDesktop().browse(u);
        } catch (Exception e) {
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
    }

    private Tooltip createTooltip(String tooltipText) {
        Tooltip tt = new Tooltip();
        tt.setShowDelay(Duration.ONE);
        tt.setShowDuration(Duration.INDEFINITE);
        tt.setText(tooltipText);
        return tt;
    }

    private List<LoadedElement> loadElementsFromCsvFile(ElementType type) {
        try {
            return LoadFromCsvService.load(type);
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

}