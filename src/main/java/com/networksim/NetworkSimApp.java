package com.networksim;

import com.networksim.logic.RoutingManager;
import com.networksim.logic.PersistenceManager;
import com.networksim.model.Connection;
import com.networksim.model.Device;
import com.networksim.model.DeviceType;
import com.networksim.model.RouteEntry;
import com.networksim.view.ConfigWindow;
import com.networksim.view.DeviceNode;
import javafx.animation.PathTransition;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import com.networksim.logic.TranslationService;
import javafx.scene.input.KeyCombination;
import java.util.Stack;
import java.util.Locale;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class NetworkSimApp extends Application {

    private final ObservableList<Device> devices = FXCollections.observableArrayList();
    private final ObservableList<Connection> connections = FXCollections.observableArrayList();
    
    private enum ToolMode { SELECT, CONNECT, DELETE, PACKET, DRAW, ERASE, VISUALIZE }
    private ToolMode currentTool = ToolMode.SELECT;
    
    private Device pendingConnectionSource = null;
    private Device packetSource = null;
    private List<RoutingManager.RoutingStep> visualSteps = null;
    private int currentStepIndex = -1;
    
    private Pane canvas;
    private Pane drawingLayer; // New layer for freehand drawings
    private Polyline currentDrawing; // The polyline currently being drawn
    private final Stack<UndoAction> undoStack = new Stack<>();
    private final Stack<UndoAction> redoStack = new Stack<>();
    
    private boolean isDark = true;
    private Scene scene;
    private Stage primaryStage;

    // UI Components for refresh
    private Menu fileMenu, editMenu, viewMenu, helpMenu;
    private MenuItem openItem, saveItem, exitItem, prefItem, undoItem, redoItem, aboutItem;
    private CheckMenuItem darkItem;
    private Button runBtn, resetBtn, stepBtn;
    private ToggleButton selectBtn, connectBtn, packetBtn, visBtn, drawBtn, eraseBtn, deleteBtn;
    private TabPane paletteTabs;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        BorderPane root = new BorderPane();
        
        // --- Header (Menus + Toolbar) ---
        VBox header = new VBox();
        
        // Menu Bar
        MenuBar menuBar = new MenuBar();
        
        fileMenu = new Menu(TranslationService.get("menu.file"));
        openItem = new MenuItem(TranslationService.get("menu.file.open"));
        openItem.setOnAction(e -> openFile());
        saveItem = new MenuItem(TranslationService.get("menu.file.save"));
        saveItem.setOnAction(e -> PersistenceManager.save(devices, connections));
        exitItem = new MenuItem(TranslationService.get("menu.file.exit"));
        exitItem.setOnAction(e -> stage.close());
        fileMenu.getItems().addAll(openItem, saveItem, new SeparatorMenuItem(), exitItem);
        
        editMenu = new Menu(TranslationService.get("menu.edit"));
        undoItem = new MenuItem(TranslationService.get("menu.edit.undo"));
        undoItem.setAccelerator(KeyCombination.keyCombination("Ctrl+Z"));
        undoItem.setOnAction(e -> undo());
        
        redoItem = new MenuItem(TranslationService.get("menu.edit.redo"));
        redoItem.setAccelerator(KeyCombination.keyCombination("Ctrl+Y"));
        redoItem.setOnAction(e -> redo());
        
        prefItem = new MenuItem(TranslationService.get("menu.edit.preferences"));
        prefItem.setOnAction(e -> showPreferences());
        editMenu.getItems().addAll(undoItem, redoItem, new SeparatorMenuItem(), prefItem);
        
        viewMenu = new Menu(TranslationService.get("menu.view"));
        darkItem = new CheckMenuItem(TranslationService.get("menu.view.darkmode"));
        darkItem.setSelected(true);
        darkItem.setOnAction(e -> {
            isDark = darkItem.isSelected();
            updateTheme();
        });
        viewMenu.getItems().add(darkItem);
        
        helpMenu = new Menu(TranslationService.get("menu.help"));
        aboutItem = new MenuItem(TranslationService.get("menu.help.about"));
        aboutItem.setOnAction(e -> showAbout());
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, helpMenu);
        
        // Toolbar
        ToolBar mainToolBar = new ToolBar();
        runBtn = new Button(TranslationService.get("toolbar.run"));
        runBtn.getStyleClass().addAll("button", "primary");
        runBtn.setOnAction(e -> runSimulation());

        stepBtn = new Button(TranslationService.get("toolbar.step"));
        stepBtn.setDisable(true);
        stepBtn.setOnAction(e -> advanceStep());

        resetBtn = new Button(TranslationService.get("toolbar.reset"));
        resetBtn.setOnAction(e -> resetNetwork());
        
        mainToolBar.getItems().addAll(runBtn, stepBtn, resetBtn);
        
        header.getChildren().addAll(menuBar, mainToolBar);
        root.setTop(header);
        
        // --- Left Toolbar (Tools) ---
        VBox leftToolBar = new VBox(5);
        leftToolBar.getStyleClass().add("toolbar-left"); 
        
        ToggleGroup toolGroup = new ToggleGroup();
        selectBtn = createToolBtn(TranslationService.get("tool.select"), "➤", ToolMode.SELECT, toolGroup);
        connectBtn = createToolBtn(TranslationService.get("tool.connect"), "⚡", ToolMode.CONNECT, toolGroup);
        packetBtn = createToolBtn(TranslationService.get("tool.packet"), "✉", ToolMode.PACKET, toolGroup);
        visBtn = createToolBtn(TranslationService.get("tool.visualize"), "👁", ToolMode.VISUALIZE, toolGroup);
        drawBtn = createToolBtn(TranslationService.get("tool.draw"), "✎", ToolMode.DRAW, toolGroup);
        eraseBtn = createToolBtn(TranslationService.get("tool.erase"), "⌫", ToolMode.ERASE, toolGroup);
        deleteBtn = createToolBtn(TranslationService.get("tool.delete"), "✖", ToolMode.DELETE, toolGroup);
        
        leftToolBar.getChildren().addAll(selectBtn, connectBtn, packetBtn, visBtn, drawBtn, eraseBtn, deleteBtn);
        root.setLeft(leftToolBar);

        visBtn.selectedProperty().addListener((o, old, newVal) -> {
            if (newVal) {
                stepBtn.setDisable(false);
                clearHighlights();
            } else {
                stepBtn.setDisable(true);
                clearHighlights();
            }
        });
        
        // --- Center Canvas ---
        canvas = new Pane();
        canvas.getStyleClass().add("background");
        
        drawingLayer = new Pane();
        drawingLayer.setMouseTransparent(true);
        canvas.getChildren().add(drawingLayer);

        canvas.setOnMousePressed(e -> {
            if (currentTool == ToolMode.DRAW) {
                Polyline poly = new Polyline();
                poly.setStroke(isDark ? javafx.scene.paint.Color.WHITE : javafx.scene.paint.Color.BLACK);
                poly.setStrokeWidth(2);
                poly.getPoints().addAll(e.getX(), e.getY());
                drawingLayer.getChildren().add(poly);
                currentDrawing = poly;
                e.consume();
            } else if (currentTool == ToolMode.ERASE) {
                eraseAt(e.getX(), e.getY());
                e.consume();
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (currentTool == ToolMode.DRAW && currentDrawing != null) {
                currentDrawing.getPoints().addAll(e.getX(), e.getY());
                e.consume();
            } else if (currentTool == ToolMode.ERASE) {
                eraseAt(e.getX(), e.getY());
                e.consume();
            }
        });

        canvas.setOnMouseReleased(e -> {
            if (currentTool == ToolMode.DRAW && currentDrawing != null) {
                undoStack.push(new UndoAction(UndoAction.Type.ADD_DRAWING, currentDrawing));
                redoStack.clear();
                currentDrawing = null;
                e.consume();
            }
        });
        
        // Drop Logic
        canvas.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) e.acceptTransferModes(TransferMode.COPY);
            e.consume();
        });
        
        canvas.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasString()) {
                try {
                    DeviceType type = DeviceType.valueOf(db.getString());
                    // Improved positioning: Center the device on the mouse cursor
                    createDevice(type, e.getX() - 25, e.getY() - 25);
                    e.setDropCompleted(true);
                } catch (Exception ex) { e.setDropCompleted(false); }
            }
            e.consume();
        });
        
        root.setCenter(canvas);
        
        // --- Bottom Palette ---
        paletteTabs = new TabPane();
        paletteTabs.getStyleClass().add("palette-bottom");
        paletteTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        paletteTabs.setPrefHeight(120);
        
        paletteTabs.getTabs().add(createPaletteTab(TranslationService.get("tab.routers"), 
            DeviceType.ROUTER_1941, DeviceType.ROUTER_2901, DeviceType.ROUTER_2911, DeviceType.ROUTER_1841));
        paletteTabs.getTabs().add(createPaletteTab(TranslationService.get("tab.switches"), 
            DeviceType.SWITCH_2960, DeviceType.SWITCH_PT, DeviceType.L3_SWITCH, DeviceType.HUB));
        paletteTabs.getTabs().add(createPaletteTab(TranslationService.get("tab.wireless"), 
            DeviceType.ROUTER_WIRELESS, DeviceType.ACCESS_POINT, DeviceType.FIREWALL_ASA));
        paletteTabs.getTabs().add(createPaletteTab(TranslationService.get("tab.surveillance"), 
            DeviceType.CAMERA_DAHUA, DeviceType.CAMERA_HIKVISION, DeviceType.CAMERA_TPLINK, DeviceType.CAMERA_UBIQUITI, 
            DeviceType.CAMERA_AXIS, DeviceType.CAMERA_BOSCH, DeviceType.NVR_DAHUA, DeviceType.NVR_HIKVISION, 
            DeviceType.NVR_UBIQUITI, DeviceType.NVR_GENERIC));
        paletteTabs.getTabs().add(createPaletteTab(TranslationService.get("tab.iot_home"), 
            DeviceType.IOT_LIGHT, DeviceType.IOT_SMART_PLUG, DeviceType.IOT_SPEAKER, DeviceType.IOT_MOTION_SENSOR, 
            DeviceType.IOT_SIREN, DeviceType.IOT_FAN, DeviceType.IOT_AC, DeviceType.IOT_GARAGE_DOOR, 
            DeviceType.IOT_THERMOSTAT, DeviceType.IOT_LOCK));
        paletteTabs.getTabs().add(createPaletteTab(TranslationService.get("tab.iot_industrial"), 
            DeviceType.IOT_PLC, DeviceType.IOT_TEMP_SENSOR, DeviceType.IOT_PRESSURE_SENSOR, DeviceType.IOT_VALVE, 
            DeviceType.IOT_CONVEYOR));
        paletteTabs.getTabs().add(createPaletteTab(TranslationService.get("tab.enddevices"), 
            DeviceType.PC, DeviceType.LAPTOP, DeviceType.SERVER, DeviceType.PRINTER, DeviceType.IP_PHONE, DeviceType.SMARTPHONE));
        paletteTabs.getTabs().add(createPaletteTab(TranslationService.get("tab.misc"), 
            DeviceType.CLOUD));
            
        root.setBottom(paletteTabs);
        
        // --- Scene ---
        scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        updateTheme();
        
        stage.setTitle(TranslationService.get("app.title"));
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/logo.png")));
        stage.setScene(scene);
        
        // --- AUTO-LOAD & AUTO-SAVE ---
        try {
            PersistenceManager.ProjectData loadedData = PersistenceManager.load();
            if (loadedData != null) {
                restoreProject(loadedData);
            }
        } catch (Exception ex) {
            System.err.println("Failed to load project state: " + ex.getMessage());
            // Optional: delete corrupted cache
            new File(".network_cache.json").delete();
        }

        stage.setOnCloseRequest(e -> PersistenceManager.save(devices, connections));
        stage.show();

        // Listen for connection changes to trigger auto-recalc if desired
        connections.addListener((ListChangeListener<Connection>) c -> runSimulation());
    }

    private void restoreProject(PersistenceManager.ProjectData data) {
        Map<String, Device> idMap = new HashMap<>();
        for (PersistenceManager.DeviceData dd : data.devices) {
            Device device = new Device(dd.id, DeviceType.valueOf(dd.type), dd.name, dd.x, dd.y);
            device.ipProperty().set(dd.ip);
            device.gatewayProperty().set(dd.gateway != null ? dd.gateway : "0.0.0.0");
            device.setPoweredOn(dd.poweredOn);
            device.setInstalledModule(dd.installedModule);
            devices.add(device);
            idMap.put(dd.id, device);
            
            // Re-create visual node
            createDeviceNode(device);
        }
        for (PersistenceManager.ConnectionData cd : data.connections) {
            Device from = idMap.get(cd.fromId);
            Device to = idMap.get(cd.toId);
            if (from != null && to != null) {
                createConnection(from, to, cd.weight);
            }
        }
        runSimulation();
    }
    
    private void eraseAt(double x, double y) {
        List<Node> toRemove = new ArrayList<>();
        for (Node n : drawingLayer.getChildren()) {
            if (n instanceof Polyline) {
                Polyline p = (Polyline) n;
                if (isNearPolyline(p, x, y)) {
                    toRemove.add(p);
                }
            }
        }
        for (Node n : toRemove) {
            drawingLayer.getChildren().remove(n);
            undoStack.push(new UndoAction(UndoAction.Type.REMOVE_DRAWING, n));
            redoStack.clear();
        }
    }

    private boolean isNearPolyline(Polyline p, double x, double y) {
        ObservableList<Double> points = p.getPoints();
        for (int i = 0; i < points.size() - 3; i += 2) {
            double x1 = points.get(i);
            double y1 = points.get(i + 1);
            double x2 = points.get(i + 2);
            double y2 = points.get(i + 3);
            
            // Distance from point to line segment
            double dist = distanceToSegment(x, y, x1, y1, x2, y2);
            if (dist < 10) return true; // 10px tolerance
        }
        return false;
    }

    private double distanceToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double l2 = Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2);
        if (l2 == 0) return Math.sqrt(Math.pow(px - x1, 2) + Math.pow(py - y1, 2));
        double t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2;
        t = Math.max(0, Math.min(1, t));
        return Math.sqrt(Math.pow(px - (x1 + t * (x2 - x1)), 2) + Math.pow(py - (y1 + t * (y2 - y1)), 2));
    }

    private ToggleButton createToolBtn(String name, String symbol, ToolMode mode, ToggleGroup group) {
        VBox content = new VBox(5);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        
        Label iconLabel = new Label(symbol);
        iconLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: white;");
        
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        
        content.getChildren().addAll(iconLabel, nameLabel);
        
        ToggleButton btn = new ToggleButton();
        btn.setGraphic(content);
        btn.setTooltip(new Tooltip(name));
        btn.setUserData(mode);
        btn.setToggleGroup(group);
        btn.getStyleClass().add("tool-sidebar-button");
        btn.setPrefWidth(75);
        btn.setPrefHeight(65);
        
        if (mode == ToolMode.SELECT) btn.setSelected(true);
        
        // Logic
        btn.selectedProperty().addListener((o, old, newVal) -> {
            if (newVal) {
                currentTool = mode;
                pendingConnectionSource = null;
                packetSource = null;
                nameLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: white; -fx-font-weight: bold;");
            } else {
                nameLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
            }
        });
        return btn;
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            UndoAction action = undoStack.pop();
            redoStack.push(action);
            if (action.type == UndoAction.Type.ADD_DRAWING) {
                drawingLayer.getChildren().remove(action.node);
            } else if (action.type == UndoAction.Type.REMOVE_DRAWING) {
                drawingLayer.getChildren().add(action.node);
            }
        }
    }

    private void redo() {
        if (!redoStack.isEmpty()) {
            UndoAction action = redoStack.pop();
            undoStack.push(action);
            if (action.type == UndoAction.Type.ADD_DRAWING) {
                drawingLayer.getChildren().add(action.node);
            } else if (action.type == UndoAction.Type.REMOVE_DRAWING) {
                drawingLayer.getChildren().remove(action.node);
            }
        }
    }

    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(TranslationService.get("about.title"));
        alert.setHeaderText(null);
        
        javafx.scene.image.ImageView logoView = new javafx.scene.image.ImageView(new Image(getClass().getResourceAsStream("/logo.png")));
        logoView.setFitWidth(100);
        logoView.setPreserveRatio(true);
        alert.setGraphic(logoView);
        
        alert.setContentText(TranslationService.get("about.text"));
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        if (isDark) alert.getDialogPane().getStyleClass().add("dark");
        alert.show();
    }

    private static class UndoAction {
        enum Type { ADD_DRAWING, REMOVE_DRAWING }
        Type type;
        Node node;
        UndoAction(Type t, Node n) { this.type = t; this.node = n; }
    }
    
    private Tab createPaletteTab(String title, DeviceType... types) {
        Tab tab = new Tab(title);
        HBox box = new HBox(10);
        box.setPadding(new Insets(10));
        
        for (DeviceType t : types) {
            addPaletteItem(box, t);
        }
        
        ScrollPane sp = new ScrollPane(box);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setFitToHeight(true);
        tab.setContent(sp);
        return tab;
    }
    
    private void addPaletteItem(HBox container, DeviceType type) {
        VBox item = new VBox(5);
        item.getStyleClass().add("palette-item");
        item.setPrefSize(70, 60);
        item.setAlignment(javafx.geometry.Pos.CENTER);
        
        Label icon = new Label(type.name().substring(0, 1)); // Placeholder if needed, but we rely on text
        Label text = new Label(friendlyName(type));
        text.setStyle("-fx-font-size: 9px; -fx-text-alignment: center;");
        text.setWrapText(true);
        
        item.getChildren().add(text); 
        
        item.setOnDragDetected(e -> {
            Dragboard db = item.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString(type.name());
            db.setContent(content);
            e.consume();
        });
        
        container.getChildren().add(item);
    }
    
    private String friendlyName(DeviceType t) {
        switch(t) {
            case ROUTER_WIRELESS: return "WRT300N";
            case SWITCH_PT: return "Switch-PT";
            case CAMERA_DAHUA: return "Dahua Bullet Cam";
            case CAMERA_HIKVISION: return "Hikvision Dome";
            case CAMERA_TPLINK: return "VIGI TP-Link";
            case CAMERA_UBIQUITI: return "UniFi G4 Pro";
            case CAMERA_AXIS: return "Axis P3245";
            case CAMERA_BOSCH: return "Bosch FLEXIDOME";
            case NVR_DAHUA: return "Dahua NVR5216";
            case NVR_HIKVISION: return "Hikvision DS-7608";
            case NVR_UBIQUITI: return "UniFi Dream Machine";
            case NVR_GENERIC: return "Generic NVR";
            case FIREWALL_ASA: return "Cisco ASA";
            case IOT_SPEAKER: return "Smart Speaker";
            case IOT_SMART_PLUG: return "Smart Plug";
            case IOT_MOTION_SENSOR: return "Motion Sensor";
            case IOT_SIREN: return "Alarm Siren";
            case IOT_FAN: return "Ceiling Fan";
            case IOT_AC: return "Air Conditioner";
            case IOT_GARAGE_DOOR: return "Garage Opener";
            case IOT_LIGHT: return "Smart Light";
            case IOT_THERMOSTAT: return "Smart Nest";
            case IOT_LOCK: return "Smart Lock";
            case IOT_PLC: return "Industrial PLC";
            case IOT_TEMP_SENSOR: return "Temp/Humidity Pro";
            case IOT_PRESSURE_SENSOR: return "Pressure Valve";
            case IOT_VALVE: return "Electronic Valve";
            case IOT_CONVEYOR: return "Conveyor Belt";
            default: return t.name().replace("ROUTER_", "").replace("SWITCH_", "").replace("IOT_", "").replace("_", " ");
        }
    }

    private void createDevice(DeviceType type, double x, double y) {
        Device device = new Device(UUID.randomUUID().toString(), type, friendlyName(type) + "-" + devices.size(), x, y);
        devices.add(device);
        createDeviceNode(device);
    }

    private void createDeviceNode(Device device) {
        DeviceNode node = new DeviceNode(device);
        
        // Add Context Menu for Routing Table
        if (device.getType().name().startsWith("ROUTER")) {
            ContextMenu cm = new ContextMenu();
            MenuItem routeTableItem = new MenuItem(TranslationService.get("device.context.routingtable"));
            routeTableItem.setOnAction(e -> showRoutingTable(device));
            cm.getItems().add(routeTableItem);
            node.setOnContextMenuRequested(e -> cm.show(node, e.getScreenX(), e.getScreenY()));
        }

        // Drag Handling
        final Delta dragDelta = new Delta();
        node.setOnMousePressed(e -> {
            if (currentTool == ToolMode.SELECT) {
                dragDelta.x = node.getLayoutX() - e.getSceneX();
                dragDelta.y = node.getLayoutY() - e.getSceneY();
                node.setSelected(true);
                e.consume();
            }
        });
        node.setOnMouseReleased(e -> node.setSelected(false));
        node.setOnMouseDragged(e -> {
            if (currentTool == ToolMode.SELECT) {
                device.setX(e.getSceneX() + dragDelta.x);
                device.setY(e.getSceneY() + dragDelta.y);
            }
        });
        
        node.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                if (currentTool == ToolMode.PACKET) {
                    handlePacketSimulation(device);
                } else if (currentTool == ToolMode.VISUALIZE) {
                    startVisualization(device);
                } else if (e.getClickCount() == 2 && currentTool == ToolMode.SELECT) {
                    ConfigWindow.show(device, isDark, devices, connections);
                } else if (currentTool == ToolMode.CONNECT) {
                    handleConnection(device);
                } else if (currentTool == ToolMode.DELETE) {
                    removeDevice(device, node);
                }
            }
        });
        
        canvas.getChildren().add(node);
    }

    private void startVisualization(Device source) {
        if (!source.getType().name().startsWith("ROUTER")) {
            new Alert(Alert.AlertType.WARNING, TranslationService.get("vis.router_only")).show();
            return;
        }
        visualSteps = RoutingManager.getDijkstraSteps(source, devices, connections);
        currentStepIndex = -1;
        clearHighlights();
        advanceStep();
    }

    private void advanceStep() {
        if (visualSteps == null || visualSteps.isEmpty()) return;
        currentStepIndex++;
        if (currentStepIndex >= visualSteps.size()) {
            new Alert(Alert.AlertType.INFORMATION, TranslationService.get("vis.finished")).show();
            visualSteps = null;
            currentStepIndex = -1;
            return;
        }
        
        RoutingManager.RoutingStep step = visualSteps.get(currentStepIndex);
        clearHighlights();
        
        for (Node n : canvas.getChildren()) {
            if (n instanceof DeviceNode) {
                DeviceNode dn = (DeviceNode) n;
                Device d = devices.stream().filter(dev -> dev.getName().equals(((Label)dn.getChildren().get(1)).getText())).findFirst().orElse(null);
                
                if (d != null) {
                    if (d == step.currentNode) {
                        dn.setHighlight(javafx.scene.paint.Color.YELLOW);
                    } else if (d == step.updatedNeighbor) {
                        dn.setHighlight(javafx.scene.paint.Color.ORANGE);
                    } else if (step.visitedNodes.contains(d)) {
                        dn.setHighlight(javafx.scene.paint.Color.LIME);
                    }
                }
            }
        }
    }

    private void clearHighlights() {
        for (Node n : canvas.getChildren()) {
            if (n instanceof DeviceNode) {
                ((DeviceNode) n).setHighlight(null);
            }
        }
    }
    
    private void handleConnection(Device device) {
        if (pendingConnectionSource == null) {
            pendingConnectionSource = device;
        } else if (pendingConnectionSource != device) {
            createConnection(pendingConnectionSource, device, 1);
            pendingConnectionSource = null;
        }
    }
    
    private void createConnection(Device from, Device to, int weight) {
        Connection conn = new Connection(UUID.randomUUID().toString(), from, to, "Copper");
        conn.setWeight(weight);
        connections.add(conn);
        
        Line line = new Line();
        line.setUserData(conn);
        line.getStyleClass().add("connection-line");
        
        line.startXProperty().bind(from.xProperty().add(30));
        line.startYProperty().bind(from.yProperty().add(30));
        line.endXProperty().bind(to.xProperty().add(30));
        line.endYProperty().bind(to.yProperty().add(30));
        
        // Link Failure Visuals
        conn.operationalProperty().addListener((obs, oldV, newV) -> {
            if (newV) {
                line.getStyleClass().remove("connection-line-broken");
                line.getStyleClass().add("connection-line");
            } else {
                line.getStyleClass().remove("connection-line");
                line.getStyleClass().add("connection-line-broken");
            }
        });
        
        // Context menu for link failure
        ContextMenu cm = new ContextMenu();
        MenuItem toggleLinkItem = new MenuItem(TranslationService.get("link.toggle"));
        toggleLinkItem.setOnAction(e -> {
            conn.setOperational(!conn.isOperational());
            runSimulation();
        });
        cm.getItems().add(toggleLinkItem);
        line.setOnContextMenuRequested(e -> cm.show(line, e.getScreenX(), e.getScreenY()));
        
        // Weight Label
        Label weightLabel = new Label();
        weightLabel.textProperty().bind(conn.weightProperty().asString());
        weightLabel.getStyleClass().add("connection-weight");
        weightLabel.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-padding: 2 6; -fx-font-size: 11px; -fx-border-color: #3b82f6; -fx-border-radius: 4; -fx-background-radius: 4;");
        weightLabel.layoutXProperty().bind(line.startXProperty().add(line.endXProperty()).divide(2).subtract(15));
        weightLabel.layoutYProperty().bind(line.startYProperty().add(line.endYProperty()).divide(2).subtract(10));
        weightLabel.setUserData(conn);

        weightLabel.setOnMouseClicked(e -> {
            if (currentTool == ToolMode.SELECT || currentTool == ToolMode.CONNECT) {
                TextInputDialog dialog = new TextInputDialog(String.valueOf(conn.getWeight()));
                dialog.setTitle(TranslationService.get("dialog.editcost.title"));
                dialog.setHeaderText(TranslationService.get("dialog.editcost.header"));
                Optional<String> result = dialog.showAndWait();
                result.ifPresent(val -> {
                    try {
                        int w = Integer.parseInt(val);
                        conn.setWeight(w);
                        runSimulation();
                    } catch (NumberFormatException ex) { /* ignore */ }
                });
                e.consume();
            } else if (currentTool == ToolMode.DELETE) {
                removeConnection(conn);
                e.consume();
            }
        });

        line.setOnMouseClicked(e -> {
             if (currentTool == ToolMode.DELETE) {
                 removeConnection(conn);
             }
        });

        canvas.getChildren().add(0, weightLabel);
        canvas.getChildren().add(0, line);
    }
    
    private void removeDevice(Device device, DeviceNode node) {
        devices.remove(device);
        canvas.getChildren().remove(node);
        
        List<Connection> toRemove = new ArrayList<>();
        for (Connection c : connections) {
            if (c.getFrom() == device || c.getTo() == device) {
                toRemove.add(c);
            }
        }
        
        for (Connection c : toRemove) {
            removeConnection(c);
        }
        runSimulation();
    }
    
    private void removeConnection(Connection conn) {
        connections.remove(conn);
        canvas.getChildren().removeIf(n -> n.getUserData() == conn);
        runSimulation();
    }
    
    private void runSimulation() {
        RoutingManager.recalculateRoutes(devices, connections);
    }
    
    private void showRoutingTable(Device device) {
        Stage stage = new Stage();
        stage.setTitle(TranslationService.get("device.context.routingtable") + ": " + device.getName());
        
        TableView<RouteEntry> table = new TableView<>();
        table.setItems(device.getRoutingTable());
        
        TableColumn<RouteEntry, String> destCol = new TableColumn<>(TranslationService.get("routing.destination"));
        destCol.setCellValueFactory(new PropertyValueFactory<>("destination"));
        
        TableColumn<RouteEntry, String> hopCol = new TableColumn<>(TranslationService.get("routing.nexthop"));
        hopCol.setCellValueFactory(new PropertyValueFactory<>("nextHop"));
        
        TableColumn<RouteEntry, Integer> metricCol = new TableColumn<>(TranslationService.get("routing.metric"));
        metricCol.setCellValueFactory(new PropertyValueFactory<>("metric"));
        
        TableColumn<RouteEntry, String> typeCol = new TableColumn<>(TranslationService.get("routing.type"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("interfaceName"));
        
        table.getColumns().addAll(destCol, hopCol, metricCol, typeCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        VBox root = new VBox(table);
        root.setPadding(new Insets(10));
        root.getStyleClass().add("background");
        
        Scene s = new Scene(root, 500, 300);
        s.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        if (isDark) root.getStyleClass().add("dark");
        
        stage.setScene(s);
        stage.show();
    }
    
    // --- Packet Simulation ---
    
    private void handlePacketSimulation(Device clickedDevice) {
        if (packetSource == null) {
            packetSource = clickedDevice;
            Alert a = new Alert(Alert.AlertType.INFORMATION, TranslationService.get("packet.source_selected") + ": " + clickedDevice.getName() + "\n" + TranslationService.get("packet.select_dest"));
            a.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            if (isDark) a.getDialogPane().getStyleClass().add("dark");
            a.show();
        } else {
            if (packetSource == clickedDevice) return;
            
            Device dest = clickedDevice;
            List<Connection> path = RoutingManager.getShortestPath(packetSource, dest, connections);
            
            if (path.isEmpty()) {
                Alert a = new Alert(Alert.AlertType.ERROR, TranslationService.get("packet.no_path") + " " + packetSource.getName() + " " + TranslationService.get("packet.and") + " " + dest.getName());
                a.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                if (isDark) a.getDialogPane().getStyleClass().add("dark");
                a.show();
            } else {
                animatePacket(packetSource, dest, path);
            }
            
            packetSource = null;
        }
    }
    
    private void animatePacket(Device src, Device dest, List<Connection> path) {
        Circle packet = new Circle(8, javafx.scene.paint.Color.RED);
        packet.setStroke(javafx.scene.paint.Color.BLACK);
        canvas.getChildren().add(packet);
        
        Polyline polyline = new Polyline();
        polyline.getPoints().add(src.xProperty().get() + 30);
        polyline.getPoints().add(src.yProperty().get() + 30);
        
        Device current = src;
        for (Connection c : path) {
            Device next = (c.getFrom() == current) ? c.getTo() : c.getFrom();
            polyline.getPoints().add(next.xProperty().get() + 30);
            polyline.getPoints().add(next.yProperty().get() + 30);
            current = next;
        }
        
        PathTransition transition = new PathTransition();
        transition.setNode(packet);
        transition.setPath(polyline);
        transition.setDuration(Duration.seconds(2));
        transition.setOnFinished(e -> {
            canvas.getChildren().remove(packet);
            new Alert(Alert.AlertType.INFORMATION, TranslationService.get("packet.arrived") + " " + dest.getName()).show();
        });
        transition.play();
    }
    
    private void resetNetwork() {
        devices.clear();
        connections.clear();
        canvas.getChildren().clear();
        drawingLayer.getChildren().clear();
        canvas.getChildren().add(drawingLayer); // Re-add layer after clear
        undoStack.clear();
        redoStack.clear();
    }
    
    private void refreshUI() {
        primaryStage.setTitle(TranslationService.get("app.title"));
        
        fileMenu.setText(TranslationService.get("menu.file"));
        openItem.setText(TranslationService.get("menu.file.open"));
        saveItem.setText(TranslationService.get("menu.file.save"));
        exitItem.setText(TranslationService.get("menu.file.exit"));
        
        editMenu.setText(TranslationService.get("menu.edit"));
        undoItem.setText(TranslationService.get("menu.edit.undo"));
        redoItem.setText(TranslationService.get("menu.edit.redo"));
        prefItem.setText(TranslationService.get("menu.edit.preferences"));
        
        viewMenu.setText(TranslationService.get("menu.view"));
        darkItem.setText(TranslationService.get("menu.view.darkmode"));
        
        helpMenu.setText(TranslationService.get("menu.help"));
        aboutItem.setText(TranslationService.get("menu.help.about"));
        
        runBtn.setText(TranslationService.get("toolbar.run"));
        stepBtn.setText(TranslationService.get("toolbar.step"));
        resetBtn.setText(TranslationService.get("toolbar.reset"));
        
        updateToolBtn(selectBtn, "tool.select");
        updateToolBtn(connectBtn, "tool.connect");
        updateToolBtn(packetBtn, "tool.packet");
        updateToolBtn(visBtn, "tool.visualize");
        updateToolBtn(drawBtn, "tool.draw");
        updateToolBtn(eraseBtn, "tool.erase");
        updateToolBtn(deleteBtn, "tool.delete");
        
        String[] paletteKeys = {"tab.routers", "tab.switches", "tab.wireless", "tab.surveillance", "tab.iot_home", "tab.iot_industrial", "tab.enddevices", "tab.misc"};
        for (int i = 0; i < paletteTabs.getTabs().size() && i < paletteKeys.length; i++) {
            paletteTabs.getTabs().get(i).setText(TranslationService.get(paletteKeys[i]));
        }

        // Update existing drawings color
        for (javafx.scene.Node n : drawingLayer.getChildren()) {
            if (n instanceof Polyline) {
                ((Polyline) n).setStroke(isDark ? javafx.scene.paint.Color.WHITE : javafx.scene.paint.Color.BLACK);
            }
        }
    }

    private void updateToolBtn(ToggleButton btn, String key) {
        VBox content = (VBox) btn.getGraphic();
        Label nameLabel = (Label) content.getChildren().get(1);
        nameLabel.setText(TranslationService.get(key));
        btn.getTooltip().setText(TranslationService.get(key));
    }
    
    private void updateTheme() {
        if (isDark) scene.getRoot().getStyleClass().add("dark");
        else scene.getRoot().getStyleClass().remove("dark");
    }
    
    private void saveFile() {
        PersistenceManager.save(devices, connections);
        Alert a = new Alert(Alert.AlertType.INFORMATION, TranslationService.get("alert.saved"));
        a.show();
    }
    
    private void openFile() {
        PersistenceManager.ProjectData loadedData = PersistenceManager.load();
        if (loadedData != null) {
            resetNetwork();
            restoreProject(loadedData);
            Alert a = new Alert(Alert.AlertType.INFORMATION, TranslationService.get("alert.loaded"));
            a.show();
        }
    }
    
    private void showPreferences() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(TranslationService.get("pref.title"));
        dialog.initOwner(primaryStage);
        
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        if (isDark) dialog.getDialogPane().getStyleClass().add("dark");
        
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        ComboBox<String> langBox = new ComboBox<>();
        langBox.getItems().addAll("English", "Bulgarian");
        langBox.getSelectionModel().select(TranslationService.getLocale().getLanguage().equals("bg") ? 1 : 0);
        
        CheckBox themeBox = new CheckBox(TranslationService.get("pref.theme"));
        themeBox.setSelected(isDark);
        
        grid.addRow(0, new Label(TranslationService.get("pref.language")), langBox);
        grid.addRow(1, new Label(TranslationService.get("pref.theme")), themeBox);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                isDark = themeBox.isSelected();
                updateTheme();
                darkItem.setSelected(isDark);
                
                String selectedLang = langBox.getValue();
                if ("Bulgarian".equals(selectedLang)) {
                    TranslationService.setLocale(new Locale("bg"));
                } else {
                    TranslationService.setLocale(Locale.ENGLISH);
                }
                refreshUI();
            }
            return null;
        });
        
        dialog.showAndWait();
    }

    private static class Delta { double x, y; }

    public static void main(String[] args) {
        launch(args);
    }
}