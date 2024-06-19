package org.codefromheaven.controller;

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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.codefromheaven.dto.LoadedElementDTO;
import org.codefromheaven.dto.FileType;
import org.codefromheaven.dto.Setting;
import org.codefromheaven.dto.settings.SettingsDTO;
import org.codefromheaven.service.LoadFromJsonService;
import org.codefromheaven.service.animal.AnimalService;
import org.codefromheaven.service.command.GitBashService;
import org.codefromheaven.service.command.PowerShellService;
import org.codefromheaven.service.settings.InternalVisibilitySettingsService;
import org.codefromheaven.service.settings.SettingsServiceBase;

import static javafx.stage.Modality.APPLICATION_MODAL;

public class MainWindowController implements Initializable {

    private static final int SPACING_BETWEEN_BUTTONS = 5;
    private static final int SIZE_OF_AUTHOR_IMAGE_IN_PIXELS = 50;

    private static final String COMMON_BUTTON_STYLES = "-fx-text-fill: #000000; -fx-font-size: 12px;";

    private static final String BUTTON_STYLES = "-fx-background-color: #E39100FF; " + COMMON_BUTTON_STYLES;
    private static final String BUTTON_SELECTED_STYLES = "-fx-background-color: #00b6ae; " + COMMON_BUTTON_STYLES;
    private static final String TOOLTIP_STYLES = "-fx-text-fill: red;";
    private static final Color BACKGROUND_COLOR = Color.rgb(33,33,33);

    @FXML
    private MenuItem changeVisibleElements;

    @FXML
    private ScrollPane mainScrollPane;

    @FXML
    public VBox primaryPage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.loadContent();
    }

    private void loadContent() {
        primaryPage.getChildren().clear();
        setupScrollPane();

        SettingsDTO visibilitySettings = InternalVisibilitySettingsService.loadVisibilitySettings();

        if (isAnyElementInSectionEnabled(FileType.SERVICE_COMMANDS, visibilitySettings)) {
            addSectionHeader("Commands to invoke on environment");
            addElementsToScene(FileType.SERVICE_COMMANDS, visibilitySettings);
        }

        if (isAnyElementInSectionEnabled(FileType.UPDATE_DAP_FOR_TEST_COMMANDS, visibilitySettings)) {
            addSectionHeader("Commands to invoke for replacing DapKeys for local tests");
            addElementsToScene(FileType.UPDATE_DAP_FOR_TEST_COMMANDS, visibilitySettings);
        }

        if (isAnyElementInSectionEnabled(FileType.LINKS, visibilitySettings)) {
            addSectionHeader("Links");
            addElementsToScene(FileType.LINKS, visibilitySettings);
        }

        if (isAnyElementInSectionEnabled(FileType.OPEN_REMOTE_APPS, visibilitySettings)) {
            addSectionHeader("Remote apps");
            addElementsToScene(FileType.OPEN_REMOTE_APPS, visibilitySettings);
        }

        if (isAnyElementInSectionEnabled(FileType.SKAT_VPN, visibilitySettings)) {
            addSectionHeader("SKAT VPN");
            addElementsToScene(FileType.SKAT_VPN, visibilitySettings);
        }

        addAuthorNote("Made with love by Szymon Gross");
    }

    private void setupScrollPane() {
        String maxWindowHeightString = SettingsServiceBase.getValue(Setting.MAX_WINDOW_HEIGHT);
        int maxWindowHeight = Integer.parseInt(maxWindowHeightString);
        mainScrollPane.setMaxHeight(maxWindowHeight);
        mainScrollPane.setFitToHeight(true);
        mainScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        String maxWindowWidthString = SettingsServiceBase.getValue(Setting.MAX_WINDOW_WIDTH);
        int maxWindowWidth = Integer.parseInt(maxWindowWidthString);
        mainScrollPane.setMaxWidth(maxWindowWidth);
        mainScrollPane.setFitToWidth(true);
        mainScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    }

    private void addSectionHeader(String headerName) {
        VBox section = new VBox();
        Text text = new Text(headerName);
        text.getStyleClass().add("text-main-header");
        section.getChildren().add(text);
        primaryPage.getChildren().add(section);
    }

    private void addAuthorNote(String authorName) {
        VBox section = new VBox();
        section.setAlignment(Pos.CENTER);
        ImageView imageView = new ImageView(AnimalService.getInstance().getCurrentAnimalImage());
        imageView.setFitHeight(SIZE_OF_AUTHOR_IMAGE_IN_PIXELS);
        imageView.setFitWidth(SIZE_OF_AUTHOR_IMAGE_IN_PIXELS);
        Tooltip.install(imageView, createTooltip(authorName));
        imageView.setOnMouseClicked(event -> {
            AnimalService.getInstance().drawNewRandomAnimal();
            imageView.setImage(AnimalService.getInstance().getCurrentAnimalImage());
        });
        Group root = new Group(imageView);
        section.getChildren().add(root);
        primaryPage.getChildren().add(section);
    }

    private void addElementsToScene(FileType type, SettingsDTO visibilitySettings) {
        List<LoadedElementDTO> loadedElements = loadElementsFromCsvFile(type);
        addElementsToScene(loadedElements, type, visibilitySettings);
    }

    private void addElementsToScene(
            List<LoadedElementDTO> loadedElements, FileType type,
            SettingsDTO visibilitySettings
    ) {
        Set<AbstractMap.SimpleEntry<Integer, String>> uniqueSectionsSet =
                loadedElements.stream()
                              .map(elem -> new AbstractMap.SimpleEntry<>(elem.getSectionDisplayOrder(), elem.getSubSectionName()))
                              .collect(Collectors.toSet());

        List<AbstractMap.SimpleEntry<Integer, String>> uniqueSections = new ArrayList<>(uniqueSectionsSet);
        uniqueSections.sort(Map.Entry.comparingByKey());

        for (AbstractMap.SimpleEntry<Integer, String> section : uniqueSections) {
            if (!isAnyElementInSubSectionEnabled(type, section.getValue(), visibilitySettings)) {
                continue;
            }
            primaryPage.getChildren().add(createHeaderForSection(section.getValue()));
            primaryPage.setBackground(new Background(new BackgroundFill(BACKGROUND_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));

            HBox rows = new HBox();
            rows.setSpacing(SPACING_BETWEEN_BUTTONS);

            List<LoadedElementDTO> sectionElements = loadedElements.stream()
                                                                   .filter(elem -> elem.getSubSectionName().equals(section.getValue()))
                                                                   .sorted(Comparator.comparing(LoadedElementDTO::getCommandOrder))
                                                                   .collect(Collectors.toList());
            for (LoadedElementDTO loadedElement : sectionElements) {
                if (!isElementVisible(loadedElement, visibilitySettings)) {
                    continue;
                }
                rows.getChildren().add(createButton(loadedElement, type));
            }
            primaryPage.getChildren().add(rows);
        }
    }

    private VBox createHeaderForSection(String sectionName) {
        VBox section = new VBox();
        Text text = new Text(sectionName);
        text.getStyleClass().add("text-header");
        section.getChildren().add(text);
        return section;
    }

    private Button createButton(LoadedElementDTO loadedElement, FileType type) {
        Button button = new Button(loadedElement.getButtonName());
        button.setStyle(BUTTON_STYLES);
        button.setMinWidth(Region.USE_PREF_SIZE);
        button.setMaxWidth(Region.USE_PREF_SIZE);
        button.setOnMouseEntered(e -> button.setStyle(BUTTON_SELECTED_STYLES));
        button.setOnMouseExited(e -> button.setStyle(BUTTON_STYLES));
        if (FileType.SERVICE_COMMANDS.equals(type)) {
            addButtonListenerForServiceCommands(button, loadedElement);
        } else if (FileType.UPDATE_DAP_FOR_TEST_COMMANDS.equals(type)) {
            addButtonListenerForServiceCommands(button, loadedElement);
        } else if (FileType.LINKS.equals(type)) {
            button.setOnMouseClicked(event -> openPageInBrowser(loadedElement.getCommand()));
        } else if (FileType.OPEN_REMOTE_APPS.equals(type)) {
            addButtonListenerForServiceCommands(button, loadedElement);
        } else if (FileType.SKAT_VPN.equals(type)) {
            addButtonListenerForServiceCommands(button, loadedElement);
        } else {
            throw new RuntimeException("Unrecognised element type provided: " + type);
        }
        button.setTooltip(createTooltip(loadedElement.getDescription()));
        return button;
    }

    private void addButtonListenerForServiceCommands(Button button, LoadedElementDTO loadedElement) {
        switch (loadedElement.getElementType()) {
            case BASH:
                addButtonListenerForBashCommand(button, loadedElement);
                break;
            case POWERSHELL:
                addButtonListenerForPowerShellCommand(button, loadedElement);
                break;
            default:
                throw new RuntimeException("Not recognised console: " + loadedElement.getElementType());
        }
    }

    private void addButtonListenerForBashCommand(Button button, LoadedElementDTO loadedElement) {
        button.setOnMouseClicked(event -> {
            if (loadedElement.isPopupInputDisplayed()) {
                Optional<String> result = createTextInputDialog(loadedElement.getPopupInputMessage());
                result.ifPresent(name -> {
                    GitBashService.runCommand(loadedElement.getScriptLocationParamName(), loadedElement.isAutoCloseConsole(), loadedElement.getCommand() + " " + name);
                });
            } else {
                GitBashService.runCommand(loadedElement.getScriptLocationParamName(), loadedElement.isAutoCloseConsole(), loadedElement.getCommand());
            }
        });
    }

    private void addButtonListenerForPowerShellCommand(Button button, LoadedElementDTO loadedElement) {
        button.setOnMouseClicked(event -> {
            if (loadedElement.isPopupInputDisplayed()) {
                Optional<String> result = createTextInputDialog(loadedElement.getPopupInputMessage());
                result.ifPresent(name -> {
                    PowerShellService.runCommand(loadedElement.getScriptLocationParamName(), loadedElement.isAutoCloseConsole(), loadedElement.getCommand() + " " + name);
                });
            } else {
                PowerShellService.runCommand(loadedElement.getScriptLocationParamName(), loadedElement.isAutoCloseConsole(), loadedElement.getCommand());
            }
        });
    }

    private Optional<String> createTextInputDialog(String popupInputMessage) {
        TextInputDialog dialog = new TextInputDialog("default_value");

        dialog.setTitle("Information required");
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        dialog.setContentText(popupInputMessage);
        Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
        dialogStage.getIcons().add(AnimalService.getInstance().getRandomAnimalImage()); // Update the path accordingly
        return dialog.showAndWait();
    }

    private void openPageInBrowser(String url) {
        try {
            URI u = new URI(url);
            java.awt.Desktop.getDesktop().browse(u);
        } catch (Exception e) {
            throw new RuntimeException("Cannot open page in browser", e);
        }
    }

    private Tooltip createTooltip(String tooltipText) {
        Tooltip tt = new Tooltip();
        tt.setStyle(TOOLTIP_STYLES);
        tt.setShowDelay(Duration.ONE);
        tt.setShowDuration(Duration.INDEFINITE);
        tt.setText(tooltipText);
        return tt;
    }

    private List<LoadedElementDTO> loadElementsFromCsvFile(FileType type) {
        try {
            return LoadFromJsonService.load(type);
        } catch (IOException e) {
            throw new RuntimeException("Cannot load files from configuration file", e);
        }
    }

    @FXML
    private void handleChangeVisibleElements() {
        Stage settingsStage = new Stage();
        settingsStage.initModality(APPLICATION_MODAL);
        settingsStage.getIcons().add(AnimalService.getInstance().getRandomAnimalImage());
        VBox settingsRoot = new VBox(10);

        ScrollPane scrollPane = new ScrollPane();
        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(10));

        List<LoadedElementDTO> allElements = loadAllElements();
        SettingsDTO visibilitySettings = InternalVisibilitySettingsService.loadVisibilitySettings();

        allElements.stream()
                   .collect(Collectors.groupingBy(LoadedElementDTO::getSubSectionName, LinkedHashMap::new, Collectors.toList()))
                   .forEach((sectionName, elements) -> {
                       VBox sectionBox = new VBox(5);
                       sectionBox.getChildren().add(new Label(sectionName));
                       elements.sort(Comparator.comparingInt(LoadedElementDTO::getSectionDisplayOrder)
                                               .thenComparingInt(LoadedElementDTO::getCommandOrder));
                       for (LoadedElementDTO element : elements) {
                           CheckBox checkBox = new CheckBox(element.getButtonName());
                           boolean isChecked = isElementVisible(element, visibilitySettings);
                           checkBox.setSelected(isChecked);
                           checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                               updateVisibilitySetting(element, newVal);
                           });
                           sectionBox.getChildren().add(checkBox);
                       }
                       contentBox.getChildren().add(sectionBox);
                   });

        scrollPane.setContent(contentBox);
        settingsRoot.getChildren().add(scrollPane);

        Scene scene = new Scene(settingsRoot, 400, 600);
        settingsStage.setTitle("Settings - Change visibility of elements");
        settingsStage.setScene(scene);

        settingsStage.setOnHidden(event -> {
            loadContent();
            resizeMainWindow();
        });
        settingsStage.showAndWait();
    }


    private void resizeMainWindow() {
        Stage mainStage = (Stage) primaryPage.getScene().getWindow();
        mainStage.sizeToScene();
    }

    private List<LoadedElementDTO> loadAllElements() {
        List<LoadedElementDTO> allElements = new ArrayList<>();
        allElements.addAll(loadElementsFromCsvFile(FileType.SERVICE_COMMANDS));
        allElements.addAll(loadElementsFromCsvFile(FileType.UPDATE_DAP_FOR_TEST_COMMANDS));
        allElements.addAll(loadElementsFromCsvFile(FileType.LINKS));
        allElements.addAll(loadElementsFromCsvFile(FileType.OPEN_REMOTE_APPS));
        allElements.addAll(loadElementsFromCsvFile(FileType.SKAT_VPN));
        return allElements;
    }

    private void updateVisibilitySetting(LoadedElementDTO element, boolean newVal) {
        InternalVisibilitySettingsService.updateVisibilitySetting(element.getSubSectionName(), element.getButtonName(), newVal);
    }

    private boolean isElementVisible(LoadedElementDTO element, SettingsDTO visibilitySettings) {
        return visibilitySettings.getSettings().stream().noneMatch(elem ->
                InternalVisibilitySettingsService.isMatchingSetting(elem, element.getSubSectionName(), element.getButtonName()));
    }

    private boolean isAnyElementInSectionEnabled(FileType section, SettingsDTO visibilitySettings) {
        return loadElementsFromCsvFile(section)
                .stream().anyMatch(elem -> isElementVisible(elem, visibilitySettings));
    }

    private boolean isAnyElementInSubSectionEnabled(FileType section, String subsection, SettingsDTO visibilitySettings) {
        return loadElementsFromCsvFile(section)
                .stream().filter(elem -> Objects.equals(elem.getSubSectionName(), subsection))
                .anyMatch(elem -> isElementVisible(elem, visibilitySettings));
    }

}
