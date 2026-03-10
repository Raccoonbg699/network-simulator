package com.networksim.view;

import com.networksim.logic.TranslationService;
import com.networksim.logic.RoutingManager;
import com.networksim.model.Connection;
import com.networksim.model.Device;
import com.networksim.model.DeviceType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextFlow;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConfigWindow {

    public static void show(Device device, boolean isDark, List<Device> allDevices, List<Connection> allConnections) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(device.getName() + " - " + device.getType().toString());

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("config-tab-pane");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        if (isDark) tabPane.getStyleClass().add("dark");

        // --- Physical Tab ---
        Tab physicalTab = new Tab(TranslationService.get("config.tab.physical"));
        BorderPane physicalPane = new BorderPane();
        physicalPane.getStyleClass().add("physical-pane");

        // Modules List (Left) - Only if applicable
        if (hasModules(device.getType())) {
            VBox modulesList = new VBox(10);
            modulesList.setPadding(new Insets(10));
            modulesList.setPrefWidth(220);
            modulesList.getStyleClass().add("module-list");
            
            Label lblModules = new Label(TranslationService.get("config.physical.modules"));
            lblModules.setStyle("-fx-font-weight: bold; -fx-text-fill: #94a3b8;");
            modulesList.getChildren().add(lblModules);
            
            populateModules(modulesList, device.getType());
            physicalPane.setLeft(modulesList);
        }

        // Visualization
        StackPane visualWrapper = new StackPane();
        visualWrapper.setStyle("-fx-background-color: #0f172a;");
        
        Canvas canvas = new Canvas(700, 450);
        drawDevice(canvas.getGraphicsContext2D(), device);
        
        // Interactions
        canvas.setOnMouseClicked(e -> {
            if (hitTestPowerButton(device, e.getX(), e.getY())) {
                device.setPoweredOn(!device.isPoweredOn());
                drawDevice(canvas.getGraphicsContext2D(), device);
            }
        });
        
        visualWrapper.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.COPY);
                visualWrapper.setStyle("-fx-background-color: #0f172a; -fx-border-color: #3b82f6; -fx-border-width: 4; -fx-border-style: dashed;");
            }
            e.consume();
        });
        
        visualWrapper.setOnDragExited(e -> {
            visualWrapper.setStyle("-fx-background-color: #0f172a; -fx-border-width: 0;");
            e.consume();
        });
        
        visualWrapper.setOnDragDropped(e -> {
            visualWrapper.setStyle("-fx-background-color: #0f172a; -fx-border-width: 0;");
            Dragboard db = e.getDragboard();
            if (db.hasString()) {
                if (!device.isPoweredOn() || device.getType() == DeviceType.ROUTER_WIRELESS) {
                    device.setInstalledModule(db.getString());
                    drawDevice(canvas.getGraphicsContext2D(), device);
                    e.setDropCompleted(true);
                } else {
                    Alert a = new Alert(Alert.AlertType.WARNING, TranslationService.get("config.physical.power_off_warning"));
                    a.initOwner(stage);
                    a.show();
                    e.setDropCompleted(false);
                }
            }
            e.consume();
        });

        visualWrapper.getChildren().add(canvas);
        physicalPane.setCenter(visualWrapper);
        physicalTab.setContent(physicalPane);
        
        // Sync Physical View with Power State changes (e.g. from Start Menu)
        device.poweredOnProperty().addListener((obs, oldVal, newVal) -> {
            drawDevice(canvas.getGraphicsContext2D(), device);
        });

        // --- Config Tab ---
        Tab configTab = new Tab(TranslationService.get("config.tab.config"));
        VBox configRoot = new VBox(10);
        configRoot.setPadding(new Insets(20));
        configRoot.getStyleClass().add("background");
        
        GridPane globalSettings = new GridPane();
        globalSettings.setHgap(15); globalSettings.setVgap(10);
        
        Label lblGlobal = new Label(TranslationService.get("config.global.title"));
        lblGlobal.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #3b82f6;");
        
        TextField nameField = new TextField(device.getName());
        nameField.textProperty().bindBidirectional(device.nameProperty());
        TextField hostNameField = new TextField(device.getName().toLowerCase());
        
        globalSettings.addRow(0, new Label(TranslationService.get("config.global.display_name")), nameField);
        globalSettings.addRow(1, new Label(TranslationService.get("config.global.hostname")), hostNameField);
        
        Separator sep1 = new Separator();
        
        GridPane ipSettings = new GridPane();
        ipSettings.setHgap(15); ipSettings.setVgap(10);
        Label lblIP = new Label(TranslationService.get("config.ip.title"));
        lblIP.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #3b82f6;");
        
        TextField ipField = new TextField(device.getIp());
        ipField.textProperty().bindBidirectional(device.ipProperty());
        TextField subnetField = new TextField("255.255.255.0");
        TextField gatewayField = new TextField(device.getGateway());
        gatewayField.textProperty().bindBidirectional(device.gatewayProperty());
        TextField dnsField = new TextField("0.0.0.0");
        
        ipSettings.addRow(0, new Label(TranslationService.get("config.ip.address")), ipField);
        ipSettings.addRow(1, new Label(TranslationService.get("config.ip.subnet")), subnetField);
        ipSettings.addRow(2, new Label(TranslationService.get("config.ip.gateway")), gatewayField);
        ipSettings.addRow(3, new Label(TranslationService.get("config.ip.dns")), dnsField);
        
        configRoot.getChildren().addAll(lblGlobal, globalSettings, sep1, lblIP, ipSettings);
        configTab.setContent(new ScrollPane(configRoot));

        // --- Routing Tab ---
        Tab routingTab = new Tab(TranslationService.get("config.tab.routing"));
        VBox routingRoot = new VBox(15);
        routingRoot.setPadding(new Insets(20));
        
        Label lblRouting = new Label(TranslationService.get("config.routing.title"));
        lblRouting.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        
        TableView<com.networksim.model.RouteEntry> routingTable = new TableView<>(device.getRoutingTable());
        routingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        routingTable.setPrefHeight(400);
        
        TableColumn<com.networksim.model.RouteEntry, String> destCol = new TableColumn<>(TranslationService.get("routing.destination"));
        destCol.setCellValueFactory(f -> f.getValue().destinationProperty());
        
        TableColumn<com.networksim.model.RouteEntry, String> nextCol = new TableColumn<>(TranslationService.get("routing.nexthop"));
        nextCol.setCellValueFactory(f -> f.getValue().nextHopProperty());
        
        TableColumn<com.networksim.model.RouteEntry, Number> metricCol = new TableColumn<>(TranslationService.get("routing.metric"));
        metricCol.setCellValueFactory(f -> f.getValue().metricProperty());
        
        TableColumn<com.networksim.model.RouteEntry, String> ifaceCol = new TableColumn<>(TranslationService.get("routing.type"));
        ifaceCol.setCellValueFactory(f -> f.getValue().interfaceNameProperty());
        
        routingTable.getColumns().addAll(destCol, nextCol, metricCol, ifaceCol);
        
        routingRoot.getChildren().addAll(lblRouting, routingTable);
        routingTab.setContent(routingRoot);

        // --- Interfaces Tab ---
        Tab interfacesTab = new Tab(TranslationService.get("config.tab.interfaces"));
        ListView<String> interfaceList = new ListView<>();
        if (isDark) interfaceList.getStyleClass().add("dark");
        if (device.getType().name().startsWith("ROUTER")) {
            interfaceList.getItems().addAll("FastEthernet0/0", "FastEthernet0/1", "Serial0/0/0", "Serial0/0/1");
        } else if (device.getType().name().startsWith("SWITCH")) {
            for(int i=1; i<=24; i++) interfaceList.getItems().add("FastEthernet0/" + i);
            interfaceList.getItems().addAll("GigabitEthernet0/1", "GigabitEthernet0/2");
        } else {
            interfaceList.getItems().add("FastEthernet0");
        }
        
        VBox ifaceDetails = new VBox(10);
        ifaceDetails.getStyleClass().add("background");
        ifaceDetails.setPadding(new Insets(20));
        ifaceDetails.getChildren().add(new Label(TranslationService.get("config.interface.select")));
        
        interfaceList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                ifaceDetails.getChildren().clear();
                Label title = new Label(newV + " " + TranslationService.get("config.interface.config"));
                title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                CheckBox portStatus = new CheckBox(TranslationService.get("config.interface.port_status"));
                portStatus.setSelected(device.isPoweredOn());
                
                GridPane ifaceGrid = new GridPane();
                ifaceGrid.setHgap(10); ifaceGrid.setVgap(10);
                ifaceGrid.addRow(0, new Label(TranslationService.get("config.ip.address")), new TextField(device.getIp()));
                ifaceGrid.addRow(1, new Label(TranslationService.get("config.ip.subnet")), new TextField("255.255.255.0"));
                
                ifaceDetails.getChildren().addAll(title, portStatus, ifaceGrid);
            }
        });
        
        SplitPane ifaceSplit = new SplitPane(interfaceList, ifaceDetails);
        if (isDark) ifaceSplit.getStyleClass().add("dark");
        ifaceSplit.setDividerPositions(0.3);
        interfacesTab.setContent(ifaceSplit);

        // --- Attributes Tab ---
        Tab attrTab = new Tab(TranslationService.get("config.tab.attributes"));
        VBox attrRoot = new VBox(10);
        attrRoot.setPadding(new Insets(20));
        
        GridPane attrGrid = new GridPane();
        attrGrid.setHgap(15); attrGrid.setVgap(10);
        attrGrid.addRow(0, new Label(TranslationService.get("config.attr.model")), new Label(device.getType().name()));
        attrGrid.addRow(1, new Label(TranslationService.get("config.attr.os_version")), new Label("NetFlow OS 1.0"));
        attrGrid.addRow(2, new Label(TranslationService.get("config.attr.mac_address")), new Label(String.format("00:01:%02X:%02X:%02X:%02X", 
            (int)(Math.random()*255), (int)(Math.random()*255), (int)(Math.random()*255), (int)(Math.random()*255))));
        
        Label uptimeLabel = new Label(device.getUptimeString());
        attrGrid.addRow(3, new Label(TranslationService.get("config.attr.uptime")), uptimeLabel);
        
        // Timer to update uptime live
        javafx.animation.AnimationTimer uptimeTimer = new javafx.animation.AnimationTimer() {
            private long lastUpdate = 0;
            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 1_000_000_000L) { // Update every second
                    uptimeLabel.setText(device.getUptimeString());
                    lastUpdate = now;
                }
            }
        };
        uptimeTimer.start();
        stage.setOnCloseRequest(e -> uptimeTimer.stop());
        
        attrRoot.getChildren().addAll(new Label(TranslationService.get("config.attr.title")), new Separator(), attrGrid);
        attrTab.setContent(attrRoot);

        // --- Desktop Tab (PC/Laptop/Server) ---
        Tab desktopTab = null;
        if (device.getType() == DeviceType.PC || device.getType() == DeviceType.LAPTOP || device.getType() == DeviceType.SERVER) {
            desktopTab = new Tab(TranslationService.get("config.tab.desktop"));
            
            // Background (Wallpaper)
            BorderPane desktopRoot = new BorderPane();
            desktopRoot.getStyleClass().add("desktop-wallpaper");
            
            Pane windowLayer = new Pane();
            windowLayer.setPickOnBounds(false); // Let clicks pass through where there are no windows

            // Icons Grid
            FlowPane icons = new FlowPane();
            icons.getStyleClass().add("desktop-icon-grid");
            
            icons.getChildren().addAll(
                createDesktopAppIcon(TranslationService.get("desktop.app.ipconfig"), Color.web("#3b82f6"), "SETTINGS", () -> showIpConfig(device, windowLayer, isDark)),
                createDesktopAppIcon(TranslationService.get("desktop.app.terminal"), Color.BLACK, "TERMINAL", () -> showCommandPrompt(device, windowLayer, isDark)),
                createDesktopAppIcon(TranslationService.get("desktop.app.browser"), Color.web("#0ea5e9"), "BROWSER", () -> showWebBrowser(device, windowLayer, isDark)),
                createDesktopAppIcon(TranslationService.get("desktop.app.email"), Color.web("#f59e0b"), "MAIL", () -> showEmailClient(device, windowLayer, isDark)),
                createDesktopAppIcon(TranslationService.get("desktop.app.display"), Color.web("#8b5cf6"), "DISPLAY", () -> showDisplaySettings(desktopRoot, windowLayer, isDark)),
                createDesktopAppIcon(TranslationService.get("desktop.app.wireless"), Color.web("#8b5cf6"), "WIFI", () -> showAlert("Wireless", "No adapter found.")),
                createDesktopAppIcon(TranslationService.get("desktop.app.vpn"), Color.web("#ef4444"), "LOCK", () -> showAlert("VPN", "VPN Service"))
            );
            
            desktopRoot.setCenter(icons);
            
            // Taskbar
            HBox taskbar = new HBox(10);
            taskbar.getStyleClass().add("taskbar");
            taskbar.setAlignment(Pos.CENTER_LEFT);
            taskbar.setPrefHeight(40);
            
            Label startBtn = new Label(TranslationService.get("config.desktop.start"));
            startBtn.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            VBox startWrapper = new VBox(startBtn);
            startWrapper.setAlignment(Pos.CENTER);
            startWrapper.getStyleClass().add("start-button");
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            Label clock = new Label(new SimpleDateFormat("HH:mm").format(new Date()));
            clock.setStyle("-fx-text-fill: white; -fx-padding: 0 10 0 0;");
            
            taskbar.getChildren().addAll(startWrapper, spacer, clock);
            desktopRoot.setBottom(taskbar);
            
            // --- Start Menu (Hidden by default) ---
            VBox startMenu = new VBox();
            startMenu.getStyleClass().add("start-menu");
            startMenu.setVisible(false);
            startMenu.setMaxSize(220, 350);
            StackPane.setAlignment(startMenu, Pos.BOTTOM_LEFT);
            StackPane.setMargin(startMenu, new Insets(0, 0, 40, 0)); // Offset by taskbar height
            
            // Header
            HBox menuHeader = new HBox(12);
            menuHeader.setPadding(new Insets(15));
            menuHeader.setAlignment(Pos.CENTER_LEFT);
            menuHeader.setStyle("-fx-background-color: #3b82f6; -fx-background-radius: 0 8 0 0;");
            
            ImageView logoIcon = new ImageView(new Image(ConfigWindow.class.getResourceAsStream("/logo.png")));
            logoIcon.setFitWidth(30);
            logoIcon.setPreserveRatio(true);
            Label userName = new Label("NetFlow User");
            userName.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
            menuHeader.getChildren().addAll(logoIcon, userName);
            
            VBox menuItems = new VBox(2);
            menuItems.setPadding(new Insets(10, 0, 10, 0));
            menuItems.getChildren().addAll(
                createStartMenuItem("📂 " + TranslationService.get("tab.misc"), "FOLDER", () -> {}),
                createStartMenuItem("🌐 " + TranslationService.get("desktop.app.browser"), "BROWSER", () -> { showWebBrowser(device, windowLayer, isDark); startMenu.setVisible(false); }),
                createStartMenuItem("✉ " + TranslationService.get("desktop.app.email"), "MAIL", () -> { showEmailClient(device, windowLayer, isDark); startMenu.setVisible(false); }),
                createStartMenuItem("⚙ " + TranslationService.get("menu.edit.preferences"), "SETTINGS", () -> { showIpConfig(device, windowLayer, isDark); startMenu.setVisible(false); }),
                new Separator(),
                createStartMenuItem("⏻ " + TranslationService.get("menu.file.exit"), "POWER", () -> { 
                    device.setPoweredOn(false); 
                    startMenu.setVisible(false); 
                })
            );
            
            startMenu.getChildren().addAll(menuHeader, menuItems);
            
            // Toggle
            startWrapper.setOnMouseClicked(e -> {
                if (device.isPoweredOn()) {
                    startMenu.setVisible(!startMenu.isVisible());
                }
            });
            
            // Close when clicking desktop
            icons.setOnMouseClicked(e -> startMenu.setVisible(false));
            
            // Power Check
            StackPane desktopWrapper = new StackPane(desktopRoot, windowLayer, startMenu);
            Label offLabel = new Label(TranslationService.get("config.cli.off_warning"));
            offLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-background-color: rgba(0,0,0,0.85); -fx-alignment: center;");
            offLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            offLabel.setVisible(!device.isPoweredOn());
            offLabel.visibleProperty().bind(device.poweredOnProperty().not());
            desktopWrapper.getChildren().add(offLabel);
            
            desktopTab.setContent(desktopWrapper);
        }

        // --- CLI Tab ---
        Tab cliTab = new Tab(TranslationService.get("config.tab.cli"));
        VBox cliBox = new VBox();
        TextArea cliOutput = new TextArea();
        cliOutput.setEditable(false);
        cliOutput.getStyleClass().add("cli-area");
        cliOutput.setPrefHeight(400);
        cliOutput.setText(String.join("\n", device.getCliHistory()));

        TextField cliInput = new TextField();
        cliInput.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-font-family: Monospace;");
        cliInput.setOnAction(e -> {
            if (!device.isPoweredOn()) return;
            String cmd = cliInput.getText();
            cliOutput.appendText("\n" + device.getName() + "# " + cmd);
            device.getCliHistory().add(device.getName() + "# " + cmd);
            cliInput.clear();
        });
        
        cliBox.getChildren().addAll(cliOutput, cliInput);
        
        StackPane cliWrapper = new StackPane(cliBox);
        Label cliOffLabel = new Label(TranslationService.get("config.cli.off_warning"));
        cliOffLabel.setStyle("-fx-text-fill: white; -fx-background-color: rgba(0,0,0,0.9); -fx-alignment: center;");
        cliOffLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        cliOffLabel.visibleProperty().bind(device.poweredOnProperty().not());
        cliWrapper.getChildren().add(cliOffLabel);
        
        cliTab.setContent(cliWrapper);

        // --- Surveillance Tab (NVR or Camera) ---
        Tab survTab = null;
        if (device.getType().name().startsWith("NVR") || device.getType().name().startsWith("CAMERA")) {
            survTab = new Tab(TranslationService.get("config.tab.surveillance"));
            VBox survRoot = new VBox(15);
            survRoot.setPadding(new Insets(20));
            survRoot.getStyleClass().add("background");
            
            Label lblLive = new Label(TranslationService.get("config.surv.live_view"));
            lblLive.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #3b82f6;");
            
            TilePane videoGrid = new TilePane();
            videoGrid.setHgap(15); videoGrid.setVgap(15);
            videoGrid.setPrefColumns(3);
            
            if (device.getType().name().startsWith("NVR")) {
                refreshSurveillance(device, videoGrid, allDevices, allConnections);
                
                Button refreshBtn = new Button("↻ Refresh");
                refreshBtn.setOnAction(e -> refreshSurveillance(device, videoGrid, allDevices, allConnections));
                
                Button manageBtn = new Button("⚙ " + TranslationService.get("config.surv.manage_cameras"));
                manageBtn.setOnAction(e -> showCameraManager(device, allDevices, allConnections, () -> refreshSurveillance(device, videoGrid, allDevices, allConnections)));

                survRoot.getChildren().addAll(new HBox(10, lblLive, refreshBtn, manageBtn), new ScrollPane(videoGrid));
            } else {
                // Individual Camera
                Device managerNvr = findManagerNvr(device, allDevices);
                if (managerNvr != null) {
                    Label managedLbl = new Label(TranslationService.get("config.surv.managed_by_nvr") + managerNvr.getName());
                    managedLbl.setStyle("-fx-font-style: italic; -fx-text-fill: #94a3b8;");
                    survRoot.getChildren().addAll(lblLive, managedLbl);
                } else if (device.isPoweredOn()) {
                    survRoot.getChildren().addAll(lblLive, createCameraFeed(device));
                } else {
                    survRoot.getChildren().addAll(lblLive, new Label(TranslationService.get("config.surv.offline")));
                }
            }
            survTab.setContent(survRoot);
        }

        // Add tabs
        tabPane.getTabs().addAll(physicalTab, configTab);
        
        // Add Networking Specific Tabs
        if (device.getType().name().startsWith("ROUTER") || device.getType().name().startsWith("SWITCH")) {
            tabPane.getTabs().addAll(routingTab, interfacesTab);
        } else if (device.getType() != DeviceType.CLOUD) {
            // End devices still have interfaces
            tabPane.getTabs().add(interfacesTab);
        }

        if (survTab != null) tabPane.getTabs().add(survTab);
        if (desktopTab != null) tabPane.getTabs().add(desktopTab);
        
        if (device.getType() != DeviceType.PRINTER && device.getType() != DeviceType.IP_PHONE && device.getType() != DeviceType.SMARTPHONE) {
            tabPane.getTabs().add(cliTab);
        }
        
        tabPane.getTabs().add(attrTab);

        Scene scene = new Scene(tabPane, 1000, 750);
        scene.getStylesheets().add(ConfigWindow.class.getResource("/styles.css").toExternalForm());
        if (isDark) scene.getRoot().getStyleClass().add("dark");

        stage.setScene(scene);
        stage.show();
    }
    
    private static void refreshSurveillance(Device nvr, TilePane grid, List<Device> allDevices, List<Connection> allConnections) {
        grid.getChildren().clear();
        if (!nvr.isPoweredOn()) {
            grid.getChildren().add(new Label(TranslationService.get("config.surv.offline")));
            return;
        }

        List<Device> cameras = new ArrayList<>();
        for (Device d : allDevices) {
            if (d.getType().name().startsWith("CAMERA") && nvr.getAssignedCameraIds().contains(d.getId())) {
                // Reachable and Powered On
                if (d.isPoweredOn() && !RoutingManager.getShortestPath(nvr, d, allConnections).isEmpty()) {
                    cameras.add(d);
                }
            }
        }

        if (cameras.isEmpty()) {
            grid.getChildren().add(new Label(TranslationService.get("config.surv.no_cameras")));
        } else {
            for (Device cam : cameras) {
                grid.getChildren().add(createCameraFeed(cam));
            }
        }
    }

    private static Device findManagerNvr(Device camera, List<Device> allDevices) {
        for (Device d : allDevices) {
            if (d.getType().name().startsWith("NVR") && d.getAssignedCameraIds().contains(camera.getId())) {
                return d;
            }
        }
        return null;
    }

    private static void showCameraManager(Device nvr, List<Device> allDevices, List<Connection> allConnections, Runnable onUpdate) {
        Stage stage = new Stage();
        stage.setTitle(TranslationService.get("config.surv.manage_cameras") + " - " + nvr.getName());
        stage.initModality(Modality.APPLICATION_MODAL);

        HBox root = new HBox(20);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("background");

        // Available Cameras (Reachable but not assigned to this NVR)
        ListView<Device> availableList = new ListView<>();
        availableList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Device item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getName() + " (" + item.getType().name() + ")");
            }
        });

        // Assigned Cameras
        ListView<Device> assignedList = new ListView<>();
        assignedList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Device item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getName());
            }
        });

        Runnable refreshLists = () -> {
            availableList.getItems().clear();
            assignedList.getItems().clear();
            for (Device d : allDevices) {
                if (d.getType().name().startsWith("CAMERA")) {
                    if (nvr.getAssignedCameraIds().contains(d.getId())) {
                        assignedList.getItems().add(d);
                    } else if (!RoutingManager.getShortestPath(nvr, d, allConnections).isEmpty()) {
                        availableList.getItems().add(d);
                    }
                }
            }
        };
        refreshLists.run();

        VBox left = new VBox(5, new Label(TranslationService.get("config.surv.available_cameras")), availableList);
        VBox right = new VBox(5, new Label(TranslationService.get("config.surv.assigned_cameras")), assignedList);
        
        VBox center = new VBox(10);
        center.setAlignment(Pos.CENTER);
        Button addBtn = new Button(TranslationService.get("config.surv.add"));
        Button remBtn = new Button(TranslationService.get("config.surv.remove"));
        center.getChildren().addAll(addBtn, remBtn);

        addBtn.setOnAction(e -> {
            Device selected = availableList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                nvr.getAssignedCameraIds().add(selected.getId());
                refreshLists.run();
                onUpdate.run();
            }
        });

        remBtn.setOnAction(e -> {
            Device selected = assignedList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                nvr.getAssignedCameraIds().remove(selected.getId());
                refreshLists.run();
                onUpdate.run();
            }
        });

        root.getChildren().addAll(left, center, right);
        Scene scene = new Scene(root, 600, 400);
        scene.getStylesheets().add(ConfigWindow.class.getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    private static VBox createCameraFeed(Device cam) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #1e293b; -fx-padding: 10; -fx-border-color: #3b82f6; -fx-border-width: 2; -fx-border-radius: 5;");
        
        Label name = new Label(cam.getName());
        name.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        
        ImageView view = new ImageView();
        // Alternating placeholders for variety
        String imgPath = cam.getName().hashCode() % 2 == 0 ? "IMG_4244.jpg" : "IMG_4245.jpg";
        File file = new File(imgPath);
        if (file.exists()) {
            view.setImage(new Image(file.toURI().toString()));
        }
        view.setFitWidth(200);
        view.setPreserveRatio(true);
        
        // Overlay for camera name/timestamp
        StackPane feedStack = new StackPane(view);
        Label timestamp = new Label(new SimpleDateFormat("HH:mm:ss").format(new Date()));
        timestamp.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 2 5;");
        StackPane.setAlignment(timestamp, Pos.TOP_RIGHT);
        feedStack.getChildren().add(timestamp);

        box.getChildren().addAll(name, feedStack);
        return box;
    }
    
    private static HBox createStartMenuItem(String name, String type, Runnable action) {
        HBox box = new HBox(12);
        box.getStyleClass().add("start-menu-item");
        
        Label lbl = new Label(name);
        box.getChildren().add(lbl);
        
        box.setOnMouseClicked(e -> action.run());
        return box;
    }
    
    private static boolean hasModules(DeviceType t) {
        return t == DeviceType.PC || t == DeviceType.LAPTOP || t == DeviceType.SERVER || 
               (t.name().startsWith("ROUTER") && t != DeviceType.ROUTER_WIRELESS);
    }
    
    // --- Desktop Icons ---
    
    private static VBox createDesktopAppIcon(String name, Color baseColor, String type, Runnable action) {
        VBox box = new VBox(5);
        box.getStyleClass().add("desktop-shortcut");
        
        StackPane iconPane = new StackPane();
        Rectangle bg = new Rectangle(55, 55);
        bg.setArcWidth(12); bg.setArcHeight(12);
        bg.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, 
            new Stop(0, baseColor.brighter()), new Stop(1, baseColor.darker())));
        bg.setStroke(Color.WHITE); bg.setStrokeWidth(1.5);

        javafx.scene.shape.SVGPath iconShape = new javafx.scene.shape.SVGPath();
        iconShape.setFill(Color.WHITE);
        iconShape.setScaleX(1.2); iconShape.setScaleY(1.2);

        switch (type) {
            case "SETTINGS": iconShape.setContent("M12,15.5 A3.5,3.5 0 0,1 15.5,12 A3.5,3.5 0 0,1 19,15.5 A3.5,3.5 0 0,1 15.5,19 A3.5,3.5 0 0,1 12,15.5 M7,12 L10,12 M14,10 L14,7 M18,12 L21,12 M14,14 L14,17"); break;
            case "TERMINAL": iconShape.setContent("M5,5 H25 V20 H5 Z M8,12 L12,15 L8,18 M13,18 H18"); break;
            case "BROWSER": iconShape.setContent("M15,5 A10,10 0 1,0 15,25 A10,10 0 1,0 15,5 M5,15 H25 M15,5 V25 M15,15 L22,8 M15,15 L22,22 M15,15 L8,22 M15,15 L8,8"); break;
            case "MAIL": iconShape.setContent("M4,6 H26 V22 H4 Z M4,6 L15,15 L26,6"); break;
            case "DISPLAY": iconShape.setContent("M5,5 H25 V18 H5 Z M10,22 H20 M15,18 V22"); break;
            case "WIFI": iconShape.setContent("M15,22 L15,22 M10,17 A7,7 0 0,1 20,17 M7,14 A11,11 0 0,1 23,14 M4,11 A15,15 0 0,1 26,11"); break;
            case "LOCK": iconShape.setContent("M8,12 V8 A7,7 0 0,1 22,8 V12 M6,12 H24 V24 H6 Z M15,18 A2,2 0 1,0 15,18.1"); break;
            default: iconShape.setContent("M10,10 H20 V20 H10 Z");
        }
        
        iconPane.getChildren().addAll(bg, iconShape);
        Label lbl = new Label(name);
        lbl.getStyleClass().add("desktop-shortcut-label");
        
        box.getChildren().addAll(iconPane, lbl);
        box.setOnMouseClicked(e -> { if(e.getClickCount()==1) action.run(); });
        return box;
    }
    
    private static void showDisplaySettings(BorderPane desktop, Pane windowLayer, boolean isDark) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: " + (isDark ? "#1e293b" : "#f1f5f9") + ";");
        
        Label title = new Label(TranslationService.get("config.desktop.wallpaper"));
        title.setStyle("-fx-text-fill: " + (isDark ? "white" : "black") + "; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        FlowPane options = new FlowPane(15, 15);
        options.getChildren().addAll(
            createWallpaperOption("Default Azure", "wallpaper-azure", desktop, isDark),
            createWallpaperOption("Midnight Nebula", "wallpaper-nebula", desktop, isDark),
            createWallpaperOption("Forest Whisper", "wallpaper-forest", desktop, isDark),
            createWallpaperOption("Classic Gray", "wallpaper-classic", desktop, isDark)
        );
        
        root.getChildren().addAll(title, options);
        createInternalWindow(TranslationService.get("desktop.window.display"), root, windowLayer, 500, 350, isDark);
    }
    
    private static VBox createWallpaperOption(String name, String cssClass, BorderPane desktop, boolean isDark) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setCursor(javafx.scene.Cursor.HAND);
        
        Rectangle preview = new Rectangle(100, 60);
        preview.getStyleClass().add(cssClass);
        preview.setStroke(isDark ? Color.WHITE : Color.BLACK);
        preview.setStrokeWidth(2);
        
        Label lbl = new Label(name);
        lbl.setStyle("-fx-text-fill: " + (isDark ? "white" : "black") + "; -fx-font-size: 11px;");
        
        box.getChildren().addAll(preview, lbl);
        box.setOnMouseClicked(e -> {
            desktop.getStyleClass().removeAll("wallpaper-azure", "wallpaper-nebula", "wallpaper-forest", "wallpaper-classic");
            desktop.getStyleClass().add(cssClass);
        });
        return box;
    }
    
    // --- Helpers ---
    
    private static void createInternalWindow(String title, Region content, Pane windowLayer, double width, double height, boolean isDark) {
        StackPane window = new StackPane();
        String bg = isDark ? "#1e293b" : "#f1f5f9";
        String border = isDark ? "#334155" : "#94a3b8";
        String titleBg = isDark ? "#0f172a" : "#e2e8f0";
        String text = isDark ? "white" : "black";
        
        window.setStyle("-fx-background-color: " + bg + "; -fx-border-color: " + border + "; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 5);");
        window.setPrefSize(width, height);
        window.setLayoutX(Math.max(50, (windowLayer.getWidth() - width) / 2));
        window.setLayoutY(Math.max(50, (windowLayer.getHeight() - height) / 2));

        VBox layout = new VBox();
        HBox titleBar = new HBox();
        titleBar.setPadding(new Insets(5, 10, 5, 10));
        titleBar.setStyle("-fx-background-color: " + titleBg + "; -fx-cursor: move;");
        titleBar.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: " + text + ";");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeBtn = new Button("X");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> windowLayer.getChildren().remove(window));
        
        titleBar.getChildren().addAll(titleLbl, spacer, closeBtn);
        
        VBox.setVgrow(content, Priority.ALWAYS);
        layout.getChildren().addAll(titleBar, content);
        
        window.getChildren().add(layout);
        
        javafx.scene.shape.SVGPath resizeHandle = new javafx.scene.shape.SVGPath();
        resizeHandle.setContent("M0,10 L10,10 L10,0 Z");
        resizeHandle.setFill(isDark ? Color.GRAY : Color.DARKGRAY);
        resizeHandle.setCursor(javafx.scene.Cursor.SE_RESIZE);
        StackPane.setAlignment(resizeHandle, Pos.BOTTOM_RIGHT);
        window.getChildren().add(resizeHandle);

        final double[] dragDelta = new double[2];
        titleBar.setOnMousePressed(e -> {
            dragDelta[0] = window.getLayoutX() - e.getSceneX();
            dragDelta[1] = window.getLayoutY() - e.getSceneY();
            window.toFront();
        });
        titleBar.setOnMouseDragged(e -> {
            window.setLayoutX(e.getSceneX() + dragDelta[0]);
            window.setLayoutY(e.getSceneY() + dragDelta[1]);
        });
        
        resizeHandle.setOnMousePressed(e -> {
            dragDelta[0] = e.getSceneX();
            dragDelta[1] = e.getSceneY();
            e.consume();
        });
        
        resizeHandle.setOnMouseDragged(e -> {
            double deltaX = e.getSceneX() - dragDelta[0];
            double deltaY = e.getSceneY() - dragDelta[1];
            double newW = window.getWidth() + deltaX;
            double newH = window.getHeight() + deltaY;
            if (newW > 200) {
                window.setPrefWidth(newW);
                dragDelta[0] = e.getSceneX();
            }
            if (newH > 150) {
                window.setPrefHeight(newH);
                dragDelta[1] = e.getSceneY();
            }
            e.consume();
        });

        window.setOnMousePressed(e -> window.toFront());
        windowLayer.getChildren().add(window);
        window.toFront();
    }

    private static void showIpConfig(Device d, Pane windowLayer, boolean isDark) {
        if (!d.isPoweredOn()) return;
        GridPane grid = new GridPane();
        grid.setStyle("-fx-background-color: " + (isDark ? "#1e293b" : "#f8fafc") + ";");
        grid.setPadding(new Insets(20));
        grid.setHgap(10); grid.setVgap(10);
        
        Label lblIp = new Label(TranslationService.get("config.ip.address"));
        Label lblGw = new Label(TranslationService.get("config.ip.gateway"));
        Label lblSub = new Label(TranslationService.get("config.ip.subnet"));
        if (isDark) {
            lblIp.setStyle("-fx-text-fill: white;");
            lblGw.setStyle("-fx-text-fill: white;");
            lblSub.setStyle("-fx-text-fill: white;");
        }

        TextField ip = new TextField(d.getIp());
        ip.textProperty().bindBidirectional(d.ipProperty());
        TextField gw = new TextField(d.getGateway());
        gw.textProperty().bindBidirectional(d.gatewayProperty());
        grid.addRow(0, lblIp, ip);
        grid.addRow(1, lblSub, new TextField("255.255.255.0"));
        grid.addRow(2, lblGw, gw);
        
        createInternalWindow(TranslationService.get("desktop.window.ipconfig"), grid, windowLayer, 300, 200, isDark);
    }
    
    private static void showCommandPrompt(Device d, Pane windowLayer, boolean isDark) {
        if (!d.isPoweredOn()) return;
        TextArea area = new TextArea("C:\\>");
        area.setStyle("-fx-control-inner-background: black; -fx-text-fill: white; -fx-font-family: Monospace;");
        createInternalWindow(TranslationService.get("desktop.window.terminal"), area, windowLayer, 600, 400, isDark);
    }
    
    private static void showWebBrowser(Device d, Pane windowLayer, boolean isDark) {
        if (!d.isPoweredOn()) return;
        
        VBox root = new VBox();
        root.setStyle("-fx-background-color: " + (isDark ? "#1e293b" : "#f1f5f9") + ";");
        
        HBox nav = new HBox(10);
        nav.setPadding(new Insets(10));
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setStyle("-fx-background-color: " + (isDark ? "#0f172a" : "#e2e8f0") + "; -fx-border-color: " + (isDark ? "#334155" : "#cbd5e1") + "; -fx-border-width: 0 0 1 0;");
        
        TextField urlField = new TextField("https://www.google.com");
        HBox.setHgrow(urlField, Priority.ALWAYS);
        Button goBtn = new Button("Go");
        
        nav.getChildren().addAll(urlField, goBtn);
        
        javafx.scene.web.WebView webView;
        try {
            webView = new javafx.scene.web.WebView();
            javafx.scene.web.WebEngine webEngine = webView.getEngine();
            
            webEngine.locationProperty().addListener((obs, oldLoc, newLoc) -> {
                if (newLoc != null) urlField.setText(newLoc);
            });
            
            goBtn.setOnAction(e -> {
                String url = urlField.getText();
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                webEngine.load(url);
            });
            
            urlField.setOnAction(e -> goBtn.fire());
            
            webEngine.load("https://www.google.com");
            
            VBox.setVgrow(webView, Priority.ALWAYS);
            root.getChildren().addAll(nav, webView);
        } catch (Throwable t) {
            Label errorLbl = new Label(TranslationService.get("desktop.window.browser_error") + t.getMessage());
            errorLbl.setStyle("-fx-text-fill: #ef4444; -fx-padding: 20; -fx-font-weight: bold;");
            errorLbl.setWrapText(true);
            root.getChildren().addAll(nav, errorLbl);
        }
        
        createInternalWindow(TranslationService.get("desktop.window.browser") + d.getName(), root, windowLayer, 800, 600, isDark);
    }
    
    private static void showEmailClient(Device d, Pane windowLayer, boolean isDark) {
        if (!d.isPoweredOn()) return;
        
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + (isDark ? "#0f172a" : "#f8fafc") + ";");
        
        // --- Sidebar ---
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(15));
        sidebar.setPrefWidth(180);
        sidebar.setStyle("-fx-background-color: #1e293b;");
        
        Button composeBtn = new Button("Compose");
        composeBtn.setPrefWidth(150);
        composeBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold;");
        
        Button receiveBtn = new Button("Receive");
        receiveBtn.setPrefWidth(150);
        
        Label inboxLbl = new Label("Inbox (" + d.getEmails().size() + ")");
        inboxLbl.setStyle("-fx-text-fill: white; -fx-padding: 10 0;");
        
        Button setupBtn = new Button("Account Setup");
        setupBtn.setPrefWidth(150);
        setupBtn.setStyle("-fx-background-color: #475569; -fx-text-fill: white;");
        
        sidebar.getChildren().addAll(composeBtn, receiveBtn, new Separator(), inboxLbl, setupBtn);
        root.setLeft(sidebar);
        
        // --- Content Area (Inbox by default) ---
        ListView<com.networksim.model.Email> inboxList = new ListView<>(d.getEmails());
        inboxList.setPrefWidth(300);
        if (isDark) inboxList.getStyleClass().add("dark");
        
        VBox emailView = new VBox(15);
        emailView.setPadding(new Insets(30));
        emailView.setStyle("-fx-background-color: " + (isDark ? "#1e293b" : "white") + ";");
        Label subject = new Label("Select an email to read");
        subject.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + (isDark ? "white" : "black") + ";");
        Separator sep = new Separator();
        TextFlow body = new TextFlow();
        Label bodyLbl = new Label("Your inbox is currently empty.");
        bodyLbl.setStyle("-fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");
        body.getChildren().add(bodyLbl);
        emailView.getChildren().addAll(subject, sep, body);
        
        SplitPane split = new SplitPane(inboxList, emailView);
        split.setDividerPositions(0.4);
        if (isDark) split.getStyleClass().add("dark");
        root.setCenter(split);
        
        // --- Actions ---
        inboxList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                subject.setText(newV.getSubject());
                body.getChildren().clear();
                Label fullBody = new Label("From: " + newV.getFrom() + "\nTo: " + newV.getTo() + "\nDate: " + newV.getTimestamp() + "\n\n" + newV.getBody());
                fullBody.setStyle("-fx-text-fill: " + (isDark ? "white" : "black") + ";");
                body.getChildren().add(fullBody);
            }
        });

        setupBtn.setOnAction(e -> {
            VBox setup = new VBox(15);
            setup.setPadding(new Insets(40));
            setup.setAlignment(Pos.CENTER);
            setup.setStyle("-fx-background-color: " + (isDark ? "#0f172a" : "#f8fafc") + ";");
            
            Label title = new Label("E-mail Configuration");
            title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + (isDark ? "white" : "black") + ";");
            
            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(15);
            grid.setAlignment(Pos.CENTER);
            
            TextField user = new TextField();
            user.textProperty().bindBidirectional(d.emailUserProperty());
            TextField server = new TextField();
            server.textProperty().bindBidirectional(d.emailServerProperty());
            PasswordField pass = new PasswordField();
            pass.textProperty().bindBidirectional(d.emailPassProperty());
            
            String labelStyle = "-fx-text-fill: " + (isDark ? "white" : "black") + ";";
            Label l1 = new Label("Your Name:"); l1.setStyle(labelStyle);
            Label l2 = new Label("Email Address:"); l2.setStyle(labelStyle);
            Label l3 = new Label("Incoming Server:"); l3.setStyle(labelStyle);
            Label l4 = new Label("Outgoing Server:"); l4.setStyle(labelStyle);
            Label l5 = new Label("Username:"); l5.setStyle(labelStyle);
            Label l6 = new Label("Password:"); l6.setStyle(labelStyle);
            
            grid.addRow(0, l1, new TextField(d.getName()));
            grid.addRow(1, l2, user);
            grid.addRow(2, l3, server);
            grid.addRow(3, l4, new TextField());
            grid.addRow(4, l5, new TextField());
            grid.addRow(5, l6, pass);
            
            Button save = new Button("Save Settings");
            save.setOnAction(ev -> root.setCenter(split));
            
            setup.getChildren().addAll(title, grid, save);
            root.setCenter(setup);
        });

        composeBtn.setOnAction(e -> {
            VBox compose = new VBox(15);
            compose.setPadding(new Insets(30));
            compose.setStyle("-fx-background-color: " + (isDark ? "#0f172a" : "#f8fafc") + ";");
            
            Label composeTitle = new Label("New Message");
            composeTitle.setStyle("-fx-text-fill: " + (isDark ? "white" : "black") + "; -fx-font-weight: bold;");
            
            TextField to = new TextField(); to.setPromptText("To:");
            TextField sub = new TextField(); sub.setPromptText("Subject:");
            TextArea msg = new TextArea(); msg.setPromptText("Write message...");
            if (isDark) {
                to.setStyle("-fx-control-inner-background: #1e293b; -fx-text-fill: white;");
                sub.setStyle("-fx-control-inner-background: #1e293b; -fx-text-fill: white;");
                msg.setStyle("-fx-control-inner-background: #1e293b; -fx-text-fill: white;");
            }
            
            Button send = new Button("Send Mail");
            send.setOnAction(ev -> {
                showAlert("Mail Success", "Email sent to " + to.getText());
                root.setCenter(split);
            });
            compose.getChildren().addAll(composeTitle, to, sub, msg, send);
            root.setCenter(compose);
        });

        receiveBtn.setOnAction(e -> {
            if (d.emailUserProperty().get().isEmpty() || d.emailServerProperty().get().isEmpty()) {
                showAlert("Mail Error", "Please configure your account first.");
                return;
            }
            // Simulating mail fetch
            d.getEmails().add(new com.networksim.model.Email(
                "admin@"+d.emailServerProperty().get(), 
                d.emailUserProperty().get(), 
                "System Update " + (d.getEmails().size() + 1),
                "Your account configuration is verified. Welcome to the network simulation mail server.",
                new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date())
            ));
            inboxLbl.setText("Inbox (" + d.getEmails().size() + ")");
        });

        createInternalWindow(TranslationService.get("desktop.window.email") + d.getName(), root, windowLayer, 800, 500, isDark);
    }
    
    private static void showAlert(String title, String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).show();
    }
    
    private static void populateModules(VBox list, DeviceType type) {
        // ... (Same as before, abbreviated for brevity)
        if (type == DeviceType.PC || type == DeviceType.SERVER) {
            addDraggableModule(list, "Linksys-WMP300N", "Wireless-N Adapter");
            addDraggableModule(list, "PT-HOST-NM-1CGE", "Gigabit Ethernet");
        } else if (type == DeviceType.LAPTOP) {
            addDraggableModule(list, "WPC300N", "Wireless PCMCIA");
            addDraggableModule(list, "PT-LAPTOP-NM-1CE", "Ethernet PCMCIA");
        } else if (type.name().startsWith("ROUTER")) {
             addDraggableModule(list, "HWIC-2T", "2 Port Serial");
             addDraggableModule(list, "HWIC-1GE", "Gigabit Ethernet");
        }
    }
    
    private static void addDraggableModule(VBox list, String id, String desc) {
        VBox item = new VBox(2);
        item.getStyleClass().add("module-item");
        Label lId = new Label(id);
        lId.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
        Label lDesc = new Label(desc);
        lDesc.setStyle("-fx-font-size: 10px; opacity: 0.7; -fx-text-fill: gray;");
        Rectangle r = new Rectangle(50, 15, Color.web("#475569"));
        item.getChildren().addAll(lId, lDesc, r);
        item.setOnDragDetected(e -> {
            Dragboard db = item.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString(id);
            db.setContent(content);
            e.consume();
        });
        list.getChildren().add(item);
    }
    
    // --- DRAWING ---
    
    private static void drawDevice(GraphicsContext gc, Device d) {
        double w = 700, h = 450;
        gc.clearRect(0, 0, w, h);
        
        // Add subtle background pattern for realism
        gc.setFill(Color.web("#0f172a")); gc.fillRect(0,0,w,h);
        gc.setStroke(Color.web("#1e293b")); gc.setLineWidth(1);
        for(int i=0; i<w; i+=40) gc.strokeLine(i,0,i,h);
        for(int i=0; i<h; i+=40) gc.strokeLine(0,i,w,i);
        
        switch (d.getType()) {
            case PC: drawPCTower(gc, d); break;
            case LAPTOP: drawLaptop(gc, d); break;
            case SERVER: drawServer(gc, d); break;
            case PRINTER: drawPrinter(gc, d); break;
            case ROUTER_WIRELESS: drawWirelessRouter(gc, d); break;
            case IP_PHONE: drawIPPhone(gc, d); break;
            case SMARTPHONE: drawSmartphone(gc, d); break;
            case HUB: drawSwitch(gc, d, true, false); break;
            case L3_SWITCH: drawSwitch(gc, d, false, true); break;
            case FIREWALL_ASA: drawFirewall(gc, d); break;
            case ACCESS_POINT: drawAccessPoint(gc, d); break;
            case CLOUD: drawCloud(gc, d); break;
            case CAMERA_DAHUA:
            case CAMERA_HIKVISION:
            case CAMERA_TPLINK:
            case CAMERA_UBIQUITI:
            case CAMERA_AXIS:
            case CAMERA_BOSCH: drawCamera(gc, d); break;
            case NVR_DAHUA:
            case NVR_HIKVISION:
            case NVR_UBIQUITI:
            case NVR_GENERIC: drawNVR(gc, d); break;
            case IOT_SPEAKER: drawSpeaker(gc, d); break;
            case IOT_SMART_PLUG: drawSmartPlug(gc, d); break;
            case IOT_MOTION_SENSOR: drawMotionSensor(gc, d); break;
            case IOT_SIREN: drawSiren(gc, d); break;
            case IOT_FAN: drawFan(gc, d); break;
            case IOT_AC: drawAC(gc, d); break;
            case IOT_GARAGE_DOOR: drawGarageDoor(gc, d); break;
            case IOT_LIGHT: drawLight(gc, d); break;
            case IOT_THERMOSTAT: drawThermostat(gc, d); break;
            case IOT_LOCK: drawLock(gc, d); break;
            case IOT_PLC: drawPLC(gc, d); break;
            case IOT_TEMP_SENSOR: drawIndustrialSensor(gc, d, "TEMP"); break;
            case IOT_PRESSURE_SENSOR: drawIndustrialSensor(gc, d, "PRES"); break;
            case IOT_VALVE: drawValve(gc, d); break;
            case IOT_CONVEYOR: drawConveyor(gc, d); break;
            default:
                if (d.getType().name().startsWith("ROUTER")) drawRouter(gc, d);
                else if (d.getType().name().startsWith("SWITCH")) drawSwitch(gc, d, false, false);
                else drawGeneric(gc, d);
        }
    }

    private static void drawCamera(GraphicsContext gc, Device d) {
        // Base / Bracket
        gc.setFill(Color.web("#94a3b8"));
        gc.fillRoundRect(250, 200, 20, 100, 5, 5);
        gc.fillRect(250, 240, 60, 15);
        
        // Body (Bullet)
        Color bodyColor = Color.web("#f8fafc");
        String brand = "";
        if (d.getType() == DeviceType.CAMERA_DAHUA) { brand = "dahua"; }
        else if (d.getType() == DeviceType.CAMERA_HIKVISION) { brand = "HIKVISION"; }
        else { brand = "TP-Link VIGI"; }

        gc.setFill(bodyColor);
        gc.fillRoundRect(300, 210, 200, 80, 10, 10);
        gc.setStroke(Color.web("#cbd5e1"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(300, 210, 200, 80, 10, 10);
        
        // Sunshield
        gc.setFill(Color.web("#e2e8f0"));
        gc.fillRect(295, 205, 180, 15);
        
        // Lens
        gc.setFill(Color.BLACK);
        gc.fillOval(460, 220, 30, 60);
        gc.setFill(Color.web("#1e293b"));
        gc.fillOval(470, 235, 15, 30); // Glass
        
        // IR LEDs (glow if on)
        if (d.isPoweredOn()) {
            gc.setFill(Color.web("#ef4444", 0.5));
            for(int i=0; i<6; i++) {
                gc.fillOval(465, 225 + (i*8), 4, 4);
            }
            gc.setFill(Color.LIME); // Status LED
            gc.fillOval(320, 275, 6, 6);
        }
        
        gc.setFill(Color.web("#64748b"));
        gc.setFont(javafx.scene.text.Font.font("Arial", 10));
        gc.fillText(brand, 320, 240);
        
        drawPowerButton(gc, 380, 320, d.isPoweredOn());
    }
    
    private static void drawNVR(GraphicsContext gc, Device d) {
        gc.setFill(Color.web("#0f172a"));
        gc.fillRoundRect(100, 180, 500, 100, 5, 5);
        gc.setStroke(Color.web("#334155"));
        gc.setLineWidth(3);
        gc.strokeRoundRect(100, 180, 500, 100, 5, 5);
        
        // Branding
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 14));
        gc.fillText("Network Video Recorder (NVR)", 120, 205);
        
        // Drive Bays (Small indicators)
        for(int i=0; i<8; i++) {
            gc.setFill(Color.BLACK);
            gc.fillRect(120 + (i*45), 230, 35, 40);
            if(d.isPoweredOn()) {
                gc.setFill(Color.LIME);
                gc.fillOval(125 + (i*45), 260, 4, 4); // HDD activity
            }
        }
        
        // Front USB
        gc.setFill(Color.GRAY);
        gc.fillRect(500, 250, 15, 8);
        
        // Navigation wheel
        gc.setStroke(Color.GRAY);
        gc.strokeOval(540, 220, 30, 30);
        
        drawPowerButton(gc, 540, 120, d.isPoweredOn());
    }
    
    private static void drawPCTower(GraphicsContext gc, Device d) {
        gc.setFill(Color.web("#1e293b")); gc.fillRect(200, 20, 300, 380);
        gc.setStroke(Color.web("#475569")); gc.setLineWidth(4); gc.strokeRect(200, 20, 300, 380);
        
        // DVD Drive
        gc.setFill(Color.BLACK); gc.fillRect(220, 50, 260, 30);
        gc.setStroke(Color.GRAY); gc.strokeRect(220, 50, 260, 30);
        gc.setFill(Color.GRAY); gc.fillRect(450, 60, 10, 5); // Eject button
        
        // Module Bay
        gc.setFill(Color.BLACK); gc.fillRect(220, 120, 260, 100); 
        String mod = d.installedModuleProperty().get();
        if (mod == null) {
            gc.setStroke(Color.web("#3b82f6")); gc.setLineDashes(5); gc.strokeRect(220, 120, 260, 100);
            gc.setLineDashes(0); gc.setFill(Color.web("#94a3b8")); gc.fillText("Drop Module Here", 300, 170);
        } else {
            gc.setFill(Color.web("#334155")); gc.fillRect(225, 125, 250, 90);
            gc.setFill(Color.WHITE); gc.fillText(mod, 235, 145);
            // Draw a network port
            gc.setFill(Color.BLACK); gc.fillRect(430, 140, 20, 20);
            if (d.isPoweredOn()) { gc.setFill(Color.LIME); gc.fillOval(432, 165, 5, 5); }
        }
        
        // Vents
        gc.setStroke(Color.BLACK); gc.setLineWidth(2);
        for(int i=0; i<8; i++) gc.strokeLine(250, 260 + (i*10), 450, 260 + (i*10));
        
        drawPowerButton(gc, 420, 350, d.isPoweredOn());
    }
    
    private static void drawLaptop(GraphicsContext gc, Device d) {
        // Base
        gc.setFill(Color.web("#334155")); gc.fillRoundRect(100, 200, 500, 40, 15, 15); 
        // Screen
        gc.setFill(Color.web("#1e293b")); gc.fillRect(150, 20, 400, 180);
        gc.setStroke(Color.web("#475569")); gc.setLineWidth(8); gc.strokeRect(150, 20, 400, 180);
        if(d.isPoweredOn()) {
            gc.setFill(Color.web("#3b82f6")); gc.fillRect(154, 24, 392, 172);
            gc.setFill(Color.WHITE); gc.fillText("NetFlow OS", 300, 100);
        }
        
        // Keyboard
        gc.setFill(Color.BLACK); gc.fillRect(150, 205, 400, 15);
        
        // Slot
        gc.setFill(Color.BLACK); gc.fillRect(110, 212, 30, 16); 
        String mod = d.installedModuleProperty().get();
        if (mod != null) { gc.setFill(Color.web("#94a3b8")); gc.fillRect(100, 215, 45, 10); }
        
        drawPowerButton(gc, 550, 210, d.isPoweredOn());
    }
    
    private static void drawServer(GraphicsContext gc, Device d) {
        gc.setFill(Color.web("#1e293b")); gc.fillRoundRect(200, 20, 300, 400, 10, 10);
        gc.setStroke(Color.web("#334155")); gc.setLineWidth(5); gc.strokeRoundRect(200, 20, 300, 400, 10, 10);
        
        // Drive Bays
        for(int i=0;i<6;i++) { 
            gc.setFill(Color.BLACK); gc.fillRect(230, 50+(i*40), 240, 30); 
            gc.setFill(Color.web("#475569")); gc.fillRect(235, 55+(i*40), 180, 20);
            if(d.isPoweredOn() && Math.random()>0.3) {
                 gc.setFill(Color.LIME); gc.fillOval(450, 60+(i*40), 8, 8); // Activity LEDs
            }
        }
        
        // Vents
        gc.setFill(Color.BLACK); gc.fillRect(230, 300, 240, 80);
        gc.setStroke(Color.web("#475569"));
        for(int i=0; i<20; i++) gc.strokeLine(240+(i*12), 300, 240+(i*12), 380);

        drawPowerButton(gc, 420, 340, d.isPoweredOn());
    }

    private static void drawRouter(GraphicsContext gc, Device d) {
        gc.setFill(Color.web("#1e293b")); gc.fillRect(50, 100, 600, 200);
        gc.setStroke(Color.web("#475569")); gc.setLineWidth(3); gc.strokeRect(50, 100, 600, 200);
        
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 18));
        gc.fillText("CISCO " + d.getType().name().replace("ROUTER_", ""), 70, 130);
        
        // Power Switch
        gc.setFill(Color.BLACK); gc.fillRect(550, 130, 60, 40);
        if (d.isPoweredOn()) { gc.setFill(Color.LIME); gc.fillRect(580, 135, 25, 30); }
        else { gc.setFill(Color.RED); gc.fillRect(555, 135, 25, 30); }
        
        // Ports
        gc.setFill(Color.BLACK); 
        gc.fillRect(70, 180, 120, 40); // Built-in ports
        gc.setFill(Color.YELLOW); gc.fillRect(75, 190, 20, 20); gc.fillRect(100, 190, 20, 20); // GE ports
        
        // Modular Slots
        gc.setStroke(Color.web("#94a3b8")); gc.setLineDashes(4); gc.strokeRect(250, 150, 250, 100);
        gc.setLineDashes(0);
        String mod = d.installedModuleProperty().get();
        if(mod!=null) { 
            gc.setFill(Color.web("#334155")); gc.fillRect(252, 152, 246, 96);
            gc.setFill(Color.WHITE); gc.setFont(javafx.scene.text.Font.font(12)); gc.fillText(mod, 260, 170); 
            // Serial ports
            gc.setFill(Color.web("#2563eb")); gc.fillRect(270, 190, 40, 20); gc.fillRect(330, 190, 40, 20);
        } else {
             gc.setFill(Color.web("#475569")); gc.setFont(javafx.scene.text.Font.font(14)); gc.fillText("HWIC Slot", 330, 200);
        }
    }

    private static void drawSwitch(GraphicsContext gc, Device d, boolean isHub, boolean isL3) {
        gc.setFill(Color.web("#1e293b")); gc.fillRect(50, 150, 600, 120);
        gc.setStroke(isL3 ? Color.web("#f59e0b") : Color.web("#475569")); 
        gc.setLineWidth(3); gc.strokeRect(50, 150, 600, 120);
        
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 16));
        String title = isHub ? "GENERIC HUB" : (isL3 ? "CISCO Multilayer Switch" : "CISCO Switch 2960");
        gc.fillText(title, 70, 180);
        
        // Ports (24 ports)
        gc.setFill(Color.BLACK); 
        gc.fillRect(70, 200, 480, 50); // Port bay
        for(int i=0; i<12; i++) { 
            // Top row
            gc.setFill(Color.web("#222")); gc.fillRect(80 + i*35, 205, 25, 18); 
            if(d.isPoweredOn() && Math.random()>0.5) { gc.setFill(Color.LIME); gc.fillOval(80 + i*35 + 10, 200, 5, 5); }
            // Bottom row
            gc.setFill(Color.web("#222")); gc.fillRect(80 + i*35, 225, 25, 18);
            if(d.isPoweredOn() && Math.random()>0.5) { gc.setFill(Color.LIME); gc.fillOval(80 + i*35 + 10, 245, 5, 5); }
        }
        
        // Gigabit uplinks
        if (!isHub) {
            gc.setFill(Color.BLACK); gc.fillRect(560, 200, 70, 50);
            gc.setFill(Color.web("#333")); gc.fillRect(570, 210, 20, 30); gc.fillRect(600, 210, 20, 30);
        }
        
        drawPowerButton(gc, 20, 190, d.isPoweredOn()); // Power on the left side
    }
    
    private static void drawWirelessRouter(GraphicsContext gc, Device d) {
        gc.setFill(Color.web("#1e293b")); gc.fillRoundRect(200, 200, 300, 80, 20, 20);
        gc.setFill(Color.web("#2563eb")); gc.fillRect(200, 230, 300, 50); // Blue accent
        
        // Antennas
        gc.setStroke(Color.BLACK); gc.setLineWidth(6);
        gc.strokeLine(230, 200, 210, 80); 
        gc.strokeLine(470, 200, 490, 80);
        if(d.isPoweredOn()) {
             gc.setFill(Color.LIME); gc.fillOval(205, 75, 10, 10); gc.fillOval(485, 75, 10, 10);
        }
        
        // Ports on back (drawn below)
        gc.setFill(Color.BLACK); gc.fillRect(250, 250, 150, 20);
        gc.setFill(Color.YELLOW); gc.fillRect(260, 252, 15, 15); // Internet
        for(int i=0; i<4; i++) { gc.setFill(Color.GRAY); gc.fillRect(290 + i*25, 252, 15, 15); } // LAN
        
        drawPowerButton(gc, 440, 150, d.isPoweredOn());
    }
    
    private static void drawAccessPoint(GraphicsContext gc, Device d) {
        gc.setFill(Color.web("#f8fafc")); gc.fillOval(250, 150, 200, 150); // Dome shape
        gc.setStroke(Color.web("#cbd5e1")); gc.setLineWidth(3); gc.strokeOval(250, 150, 200, 150);
        
        // Logo / Center
        gc.setFill(Color.web("#3b82f6")); gc.fillOval(330, 200, 40, 40);
        
        // LED ring
        if (d.isPoweredOn()) {
            gc.setStroke(Color.web("#3b82f6"));
            gc.setLineWidth(5);
            gc.strokeOval(280, 180, 140, 90);
        }
        
        drawPowerButton(gc, 335, 330, d.isPoweredOn());
    }
    
    private static void drawFirewall(GraphicsContext gc, Device d) {
        gc.setFill(Color.web("#1e293b")); gc.fillRect(100, 150, 500, 120);
        gc.setStroke(Color.web("#ef4444")); gc.setLineWidth(4); gc.strokeRect(100, 150, 500, 120); // Red border
        
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 20));
        gc.fillText("CISCO ASA Firewall", 120, 190);
        
        // Ports
        gc.setFill(Color.BLACK); gc.fillRect(120, 210, 300, 40);
        for(int i=0; i<8; i++) { gc.setFill(Color.YELLOW); gc.fillRect(130 + i*35, 220, 20, 20); }
        
        drawPowerButton(gc, 540, 180, d.isPoweredOn());
    }
    
    private static void drawCloud(GraphicsContext gc, Device d) {
        gc.setFill(Color.web("#e2e8f0"));
        // Draw cloud using multiple overlapping circles
        gc.fillOval(200, 150, 150, 150);
        gc.fillOval(300, 100, 200, 200);
        gc.fillOval(400, 150, 150, 150);
        
        gc.setFill(Color.web("#334155"));
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 36));
        gc.fillText("INTERNET", 280, 220);
    }
    
    private static void drawPrinter(GraphicsContext gc, Device d) {
        // Base
        gc.setFill(Color.web("#e2e8f0")); gc.fillRect(200, 200, 300, 150);
        // Paper tray top
        gc.setFill(Color.web("#cbd5e1")); gc.fillPolygon(new double[]{250, 450, 400, 300}, new double[]{100, 100, 200, 200}, 4);
        gc.setFill(Color.WHITE); gc.fillRect(280, 80, 140, 120); // Paper
        
        // Output tray
        gc.setFill(Color.web("#94a3b8")); gc.fillRect(220, 300, 260, 20);
        
        // Control panel
        gc.setFill(Color.web("#1e293b")); gc.fillRect(450, 220, 40, 100);
        if(d.isPoweredOn()) { gc.setFill(Color.LIME); gc.fillRect(455, 230, 30, 20); } // Screen
        
        drawPowerButton(gc, 455, 270, d.isPoweredOn());
    }
    
    private static void drawIPPhone(GraphicsContext gc, Device d) {
        // Base
        gc.setFill(Color.web("#1e293b")); gc.fillRoundRect(250, 100, 200, 250, 20, 20);
        
        // Handset
        gc.setFill(Color.web("#0f172a")); gc.fillRoundRect(220, 120, 40, 210, 20, 20);
        // Cord
        gc.setStroke(Color.BLACK); gc.setLineWidth(3);
        gc.strokeArc(240, 330, 20, 30, 180, 180, javafx.scene.shape.ArcType.OPEN);
        
        // Screen
        gc.setFill(Color.web("#334155")); gc.fillRect(280, 120, 150, 80);
        if(d.isPoweredOn()) {
             gc.setFill(Color.web("#f8fafc")); gc.fillRect(285, 125, 140, 70);
             gc.setFill(Color.BLACK); gc.fillText("Ext: 1001", 300, 150);
             gc.fillText(new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date()), 300, 180);
        }
        
        // Keypad
        gc.setFill(Color.web("#475569"));
        for(int row=0; row<4; row++) {
            for(int col=0; col<3; col++) {
                gc.fillRoundRect(290 + col*45, 220 + row*30, 35, 20, 5, 5);
            }
        }
    }
    
    private static void drawSmartphone(GraphicsContext gc, Device d) {
        // Body
        gc.setFill(Color.BLACK);
        gc.fillRoundRect(250, 50, 200, 350, 30, 30);
        // Screen
        gc.setFill(Color.web("#1e293b"));
        gc.fillRect(265, 80, 170, 280);
        // Button
        gc.setStroke(Color.GRAY); gc.setLineWidth(2);
        gc.strokeOval(335, 365, 30, 30);
        // Camera
        gc.setFill(Color.GRAY);
        gc.fillOval(345, 60, 10, 10);
        
        if (d.isPoweredOn()) {
            gc.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, 
                new Stop(0, Color.web("#1e3a8a")), new Stop(1, Color.web("#3b82f6"))));
            gc.fillRect(265, 80, 170, 280);
            
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("System", 24));
            gc.fillText(new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date()), 320, 150);
            
            // Apps
            gc.setFill(Color.web("#10b981")); gc.fillRoundRect(280, 250, 35, 35, 8, 8);
            gc.setFill(Color.web("#f59e0b")); gc.fillRoundRect(330, 250, 35, 35, 8, 8);
            gc.setFill(Color.web("#ef4444")); gc.fillRoundRect(380, 250, 35, 35, 8, 8);
        }
        
        // Power button (side)
        gc.setFill(d.isPoweredOn() ? Color.LIME : Color.RED);
        gc.fillRect(450, 120, 5, 40); // Side button
    }

    private static void drawSpeaker(GraphicsContext gc, Device d) {
        gc.setFill(Color.web("#1e293b"));
        gc.fillRoundRect(280, 100, 140, 220, 30, 30);
        gc.setStroke(Color.web("#334155"));
        gc.strokeRoundRect(280, 100, 140, 220, 30, 30);
        
        // Grill
        gc.setFill(Color.web("#0f172a"));
        gc.fillOval(300, 130, 100, 100);
        gc.fillOval(310, 240, 80, 80);
        
        if (d.isPoweredOn()) {
            gc.setStroke(Color.web("#3b82f6", 0.6));
            gc.setLineWidth(2);
            gc.strokeOval(295, 125, 110, 110);
        }
        drawPowerButton(gc, 335, 340, d.isPoweredOn());
    }

    private static void drawSmartPlug(GraphicsContext gc, Device d) {
        gc.setFill(Color.WHITE);
        gc.fillRoundRect(300, 180, 100, 100, 15, 15);
        gc.setStroke(Color.web("#cbd5e1"));
        gc.strokeRoundRect(300, 180, 100, 100, 15, 15);
        
        // Sockets
        gc.setFill(Color.web("#e2e8f0"));
        gc.fillOval(325, 205, 15, 15);
        gc.fillOval(360, 205, 15, 15);
        
        gc.setFill(d.isPoweredOn() ? Color.LIME : Color.web("#94a3b8"));
        gc.fillOval(345, 250, 10, 10);
        
        drawPowerButton(gc, 335, 300, d.isPoweredOn());
    }

    private static void drawMotionSensor(GraphicsContext gc, Device d) {
        gc.setFill(Color.web("#f1f5f9"));
        gc.fillRoundRect(320, 150, 60, 80, 10, 10);
        
        // PIR Lens
        gc.setFill(Color.web("#e2e8f0"));
        gc.fillOval(330, 160, 40, 40);
        
        if (d.isPoweredOn()) {
            gc.setFill(Color.web("#ef4444", 0.5));
            gc.fillOval(345, 210, 10, 10);
        }
        drawPowerButton(gc, 335, 250, d.isPoweredOn());
    }

    private static void drawSiren(GraphicsContext gc, Device d) {
        gc.setFill(Color.web("#ef4444"));
        gc.fillRoundRect(300, 150, 100, 120, 10, 10);
        
        // Grill
        gc.setStroke(Color.WHITE);
        for(int i=0; i<5; i++) gc.strokeLine(320, 170 + (i*15), 380, 170 + (i*15));
        
        if (d.isPoweredOn() && (System.currentTimeMillis() / 500) % 2 == 0) {
            gc.setFill(Color.web("#ef4444", 0.3));
            gc.fillOval(280, 130, 140, 160);
        }
        drawPowerButton(gc, 335, 290, d.isPoweredOn());
    }

    private static void drawFan(GraphicsContext gc, Device d) {
        double angle = d.isPoweredOn() ? (System.currentTimeMillis() / 2) % 360 : 0;
        gc.save();
        gc.translate(350, 220);
        gc.rotate(angle);
        
        gc.setFill(Color.web("#64748b"));
        for(int i=0; i<4; i++) {
            gc.rotate(90);
            gc.fillRoundRect(-10, -80, 20, 80, 5, 5);
        }
        gc.restore();
        
        gc.setFill(Color.web("#475569"));
        gc.fillOval(335, 205, 30, 30);
        
        drawPowerButton(gc, 335, 320, d.isPoweredOn());
    }

    private static void drawAC(GraphicsContext gc, Device d) {
        gc.setFill(Color.web("#f8fafc"));
        gc.fillRoundRect(200, 100, 300, 80, 5, 5);
        gc.setStroke(Color.web("#cbd5e1"));
        gc.strokeRoundRect(200, 100, 300, 80, 5, 5);
        
        // Vent
        gc.setFill(Color.web("#e2e8f0"));
        gc.fillRect(210, 160, 280, 10);
        
        if (d.isPoweredOn()) {
            gc.setFill(Color.web("#3b82f6"));
            gc.fillText("18°C", 440, 130);
            // Air lines
            gc.setStroke(Color.web("#3b82f6", 0.3));
            for(int i=0; i<3; i++) gc.strokeLine(250 + (i*50), 180, 250 + (i*50), 210);
        }
        drawPowerButton(gc, 335, 250, d.isPoweredOn());
    }

    private static void drawGarageDoor(GraphicsContext gc, Device d) {
        gc.setFill(Color.web("#475569"));
        gc.fillRect(200, 100, 300, 200);
        
        // Panels
        gc.setStroke(Color.web("#334155"));
        for(int i=0; i<5; i++) gc.strokeRect(210, 110 + (i*35), 280, 30);
        
        if (d.isPoweredOn()) {
            gc.setFill(Color.web("#3b82f6", 0.5));
            gc.fillRect(200, 100, 300, 50); // Partially open sim
        }
        drawPowerButton(gc, 335, 320, d.isPoweredOn());
    }

    private static void drawLight(GraphicsContext gc, Device d) {
        if (d.isPoweredOn()) {
            gc.setFill(new javafx.scene.paint.RadialGradient(0, 0, 350, 200, 150, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#fde047", 0.8)), new Stop(1, Color.TRANSPARENT)));
            gc.fillOval(200, 50, 300, 300);
        }
        
        gc.setFill(Color.web("#fef08a"));
        gc.fillOval(325, 150, 50, 70);
        gc.setFill(Color.web("#94a3b8"));
        gc.fillRect(335, 215, 30, 20);
        
        drawPowerButton(gc, 335, 320, d.isPoweredOn());
    }

    private static void drawThermostat(GraphicsContext gc, Device d) {
        gc.setFill(Color.web("#0f172a"));
        gc.fillOval(275, 125, 150, 150);
        gc.setStroke(Color.web("#334155"));
        gc.setLineWidth(5);
        gc.strokeOval(275, 125, 150, 150);
        
        if (d.isPoweredOn()) {
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", 40));
            gc.fillText("22", 325, 215);
        }
        drawPowerButton(gc, 335, 300, d.isPoweredOn());
    }

    private static void drawLock(GraphicsContext gc, Device d) {
        gc.setFill(Color.web("#475569"));
        gc.fillRoundRect(310, 120, 80, 140, 10, 10);
        
        gc.setFill(Color.web("#94a3b8"));
        gc.fillOval(330, 140, 40, 40);
        
        if (d.isPoweredOn()) {
            gc.setFill(Color.LIME); // Locked
            gc.fillRect(345, 155, 10, 10);
        } else {
            gc.setFill(Color.web("#ef4444")); // Unlocked
            gc.fillRect(345, 155, 10, 10);
        }
        drawPowerButton(gc, 335, 280, d.isPoweredOn());
    }

    private static void drawPLC(GraphicsContext gc, Device d) {
        gc.setFill(Color.web("#334155"));
        gc.fillRect(200, 100, 300, 180);
        gc.setStroke(Color.web("#1e293b"));
        gc.strokeRect(200, 100, 300, 180);
        
        // Modules
        for(int i=0; i<4; i++) {
            gc.setFill(Color.web("#1e293b"));
            gc.fillRect(215 + (i*70), 120, 60, 140);
            if (d.isPoweredOn()) {
                gc.setFill(Math.random() > 0.5 ? Color.LIME : Color.web("#064e3b"));
                gc.fillOval(220 + (i*70), 130, 5, 5);
            }
        }
        drawPowerButton(gc, 335, 300, d.isPoweredOn());
    }

    private static void drawIndustrialSensor(GraphicsContext gc, Device d, String label) {
        gc.setFill(Color.web("#475569"));
        gc.fillOval(300, 150, 100, 100);
        gc.setStroke(Color.web("#1e293b"));
        gc.strokeOval(300, 150, 100, 100);
        
        // Face
        gc.setFill(Color.web("#e2e8f0"));
        gc.fillOval(310, 160, 80, 80);
        
        if (d.isPoweredOn()) {
            gc.setFill(Color.BLACK);
            gc.setFont(javafx.scene.text.Font.font(14));
            gc.fillText(label, 335, 205);
            // Needle
            gc.setStroke(Color.web("#ef4444"));
            gc.strokeLine(350, 200, 380, 180);
        }
        drawPowerButton(gc, 335, 270, d.isPoweredOn());
    }

    private static void drawValve(GraphicsContext gc, Device d) {
        gc.setFill(Color.web("#94a3b8"));
        gc.fillRect(250, 190, 200, 20);
        
        gc.setFill(Color.web("#ef4444"));
        gc.fillOval(325, 150, 50, 50);
        
        if (d.isPoweredOn()) {
            gc.setStroke(Color.LIME);
            gc.setLineWidth(5);
            gc.strokeLine(300, 200, 400, 200);
        }
        drawPowerButton(gc, 335, 220, d.isPoweredOn());
    }

    private static void drawConveyor(GraphicsContext gc, Device d) {
        gc.setFill(Color.web("#1e293b"));
        gc.fillRect(150, 200, 400, 40);
        
        // Rollers
        gc.setFill(Color.web("#475569"));
        for(int i=0; i<10; i++) gc.fillOval(160 + (i*40), 210, 20, 20);
        
        if (d.isPoweredOn()) {
            double offset = (System.currentTimeMillis() / 10) % 40;
            gc.setFill(Color.web("#3b82f6", 0.5));
            gc.fillRect(160 + offset, 190, 30, 10); // Package sim
        }
        drawPowerButton(gc, 335, 260, d.isPoweredOn());
    }

    private static void drawGeneric(GraphicsContext gc, Device d) {
        gc.setFill(Color.web("#475569")); gc.fillRect(250, 150, 200, 100);
        gc.setFill(Color.WHITE); gc.fillText("GENERIC DEVICE", 280, 200);
    }
    
    private static void drawPowerButton(GraphicsContext gc, double x, double y, boolean on) {
        // Ring
        gc.setStroke(on ? Color.LIME : Color.web("#94a3b8"));
        gc.setLineWidth(3);
        gc.strokeOval(x + 5, y + 5, 30, 30);

        // Center button
        gc.setFill(on ? Color.LIME : Color.web("#334155"));
        gc.fillOval(x + 15, y + 15, 10, 10);

        // Label
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("System", 10));
        gc.fillText(on ? "ON" : "OFF", x + 11, y + 52);
    }

    private static boolean hitTestPowerButton(Device d, double x, double y) {
        if (d.getType() == DeviceType.PC) return dist(x, y, 440, 370) < 30;
        else if (d.getType() == DeviceType.LAPTOP) return dist(x, y, 570, 230) < 30;
        else if (d.getType() == DeviceType.SERVER) return dist(x, y, 440, 360) < 30;
        else if (d.getType() == DeviceType.SMARTPHONE) return x>=450 && x<=460 && y>=120 && y<=160;
        else if (d.getType() == DeviceType.PRINTER) return dist(x, y, 475, 290) < 30;
        else if (d.getType() == DeviceType.ROUTER_WIRELESS) return dist(x,y, 460, 170) < 30;
        else if (d.getType() == DeviceType.ACCESS_POINT) return dist(x, y, 355, 350) < 30;
        else if (d.getType() == DeviceType.FIREWALL_ASA) return dist(x, y, 560, 200) < 30;
        else if (d.getType().name().startsWith("SWITCH") || d.getType() == DeviceType.HUB) return dist(x,y, 40, 210) < 30;
        else if (d.getType().name().startsWith("ROUTER"))
            return x >= 550 && x <= 610 && y >= 130 && y <= 170;
        else if (d.getType().name().startsWith("CAMERA")) return dist(x, y, 400, 340) < 30;
        else if (d.getType().name().startsWith("NVR")) return dist(x, y, 560, 140) < 30;
        else if (d.getType() == DeviceType.IOT_SPEAKER) return dist(x, y, 355, 360) < 30;
        else if (d.getType() == DeviceType.IOT_SMART_PLUG) return dist(x, y, 355, 320) < 30;
        else if (d.getType() == DeviceType.IOT_MOTION_SENSOR) return dist(x, y, 355, 270) < 30;
        else if (d.getType() == DeviceType.IOT_SIREN) return dist(x, y, 355, 310) < 30;
        else if (d.getType() == DeviceType.IOT_FAN) return dist(x, y, 355, 340) < 30;
        else if (d.getType() == DeviceType.IOT_AC) return dist(x, y, 355, 270) < 30;
        else if (d.getType() == DeviceType.IOT_GARAGE_DOOR) return dist(x, y, 355, 340) < 30;
        else if (d.getType() == DeviceType.IOT_LIGHT) return dist(x, y, 355, 340) < 30;
        else if (d.getType() == DeviceType.IOT_THERMOSTAT) return dist(x, y, 355, 320) < 30;
        else if (d.getType() == DeviceType.IOT_LOCK) return dist(x, y, 355, 300) < 30;
        else if (d.getType() == DeviceType.IOT_PLC) return dist(x, y, 355, 320) < 30;
        else if (d.getType().name().startsWith("IOT_TEMP") || d.getType().name().startsWith("IOT_PRES")) return dist(x, y, 355, 290) < 30;
        else if (d.getType() == DeviceType.IOT_VALVE) return dist(x, y, 355, 240) < 30;
        else if (d.getType() == DeviceType.IOT_CONVEYOR) return dist(x, y, 355, 280) < 30;
        return false;
    }
    
    private static double dist(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1-x2, 2) + Math.pow(y1-y2, 2));
    }
}