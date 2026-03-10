package com.networksim.view;

import com.networksim.model.Device;
import com.networksim.model.DeviceType;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;

public class DeviceNode extends VBox {
    private final Device device;

    // --- SVG CONSTANTS ---
    private static final String SVG_ROUTER = "M20,10 A15,15 0 1,1 20,40 A15,15 0 1,1 20,10 M12,25 L28,25 M28,25 L24,21 M28,25 L24,29 M12,25 L16,21 M12,25 L16,29 M20,17 L20,33 M20,17 L16,21 M20,17 L24,21 M20,33 L16,29 M20,33 L24,29";
    private static final String SVG_SWITCH = "M5,10 H35 V30 H5 Z M8,16 H32 M32,16 L29,13 M32,16 L29,19 M8,24 H32 M8,24 L11,21 M8,24 L11,27";
    private static final String SVG_L3_SWITCH = "M5,10 H35 V30 H5 Z M20,15 L25,20 L20,25 L15,20 Z"; // Simple diamond in middle
    private static final String SVG_HUB = "M5,10 H35 V30 H5 Z M8,20 H32 M32,20 L29,17 M32,20 L29,23"; // Single arrow
    private static final String SVG_PC = "M10,5 H30 V40 H10 Z M12,8 H28 V12 H12 Z M12,15 H28 M12,35 A2,2 0 1,0 12,39 A2,2 0 1,0 12,35"; 
    private static final String SVG_LAPTOP = "M5,25 H35 L38,35 H2 Z M7,5 H33 V25 H7 Z";
    private static final String SVG_SERVER = "M10,2 H30 V42 H10 Z M12,5 H28 M12,10 H28 M12,15 H28 M12,20 H28 M12,25 H28 M12,30 H28";
    private static final String SVG_PRINTER = "M10,15 H30 V30 H10 Z M12,10 H28 V15 H12 Z M12,30 V35 H28 V30";
    private static final String SVG_PHONE = "M12,10 H28 V30 H12 Z M15,12 H25 M15,16 H18 M20,16 H23 M15,20 H18 M20,20 H23 M15,24 H18 M20,24 H23";
    private static final String SVG_WIFI = "M10,15 H30 V25 H10 Z M12,15 V5 M28,15 V5 M20,10 A5,5 0 0,1 25,5";
    private static final String SVG_AP = "M10,20 H30 V25 H10 Z M15,20 V10 M25,20 V10 M20,15 A5,5 0 0,1 25,10";
    private static final String SVG_FIREWALL = "M5,10 H35 V30 H5 Z M10,10 V30 M15,10 V30 M20,10 V30 M25,10 V30 M30,10 V30 M5,15 H35 M5,20 H35 M5,25 H35"; // Brick wall pattern
    private static final String SVG_CAMERA = "M10,15 L30,15 L35,25 L5,25 Z M15,25 V35 M10,35 H20 M20,20 A3,3 0 1,1 14,20 A3,3 0 1,1 20,20"; // Bullet camera
    private static final String SVG_NVR = "M5,15 H35 V30 H5 Z M8,20 H12 M15,20 H32 M30,25 L32,25 M32,25 L34,25"; 
    private static final String SVG_CLOUD = "M10,20 Q10,10 20,10 Q25,0 35,10 Q45,10 45,20 Q50,25 45,30 Q35,40 20,30 Q5,35 0,25 Q0,15 10,20";
    
    // IoT SVGs
    private static final String SVG_LIGHT = "M15,10 A10,10 0 1,1 25,10 M12,30 H28 M15,35 H25 M20,15 V25";
    private static final String SVG_FAN = "M20,10 V30 M10,20 H30 M13,13 L27,27 M13,27 L27,13"; // Simple fan blades
    private static final String SVG_SPEAKER = "M12,5 H28 V35 H12 Z M15,15 A5,5 0 1,0 25,15 M16,28 A4,4 0 1,0 24,28";
    private static final String SVG_LOCK = "M15,20 H25 V35 H15 Z M17,20 V15 A3,3 0 0,1 23,15 V20 M20,28 A2,2 0 1,0 20,28.1";
    private static final String SVG_SENSOR = "M10,15 H30 V35 H10 Z M20,20 A5,5 0 1,0 20,20.1";
    private static final String SVG_PLUG = "M15,10 H25 V30 H15 Z M18,15 H18.5 M21.5,15 H22 M20,22 V25";
    private static final String SVG_INDUSTRIAL = "M5,5 H35 V35 H5 Z M10,10 H15 M10,15 H15 M10,20 H15 M20,10 H30 V30 H20 Z";
    
    // Smartphone: Simple rectangle with button
    private static final String SVG_SMARTPHONE = "M15,5 H25 A2,2 0 0,1 27,7 V33 A2,2 0 0,1 25,35 H15 A2,2 0 0,1 13,33 V7 A2,2 0 0,1 15,5 M18,32 H22";

    public DeviceNode(Device device) {
        this.device = device;
        this.setAlignment(Pos.CENTER);
        this.setSpacing(2);
        this.getStyleClass().add("device-node");

        StackPane iconContainer = new StackPane();
        
        Shape bg;
        String pathData;
        Color iconColor;
        double scale = 0.9;
        
        if (device.getType() == DeviceType.ROUTER_WIRELESS) {
            bg = new Circle(22, Color.web("#6366f1"));
            pathData = SVG_WIFI;
            iconColor = Color.WHITE;
        } else if (device.getType() == DeviceType.ACCESS_POINT) {
            bg = new Circle(22, Color.web("#8b5cf6"));
            pathData = SVG_AP;
            iconColor = Color.WHITE;
        } else if (device.getType() == DeviceType.FIREWALL_ASA) {
            Rectangle r = new Rectangle(40, 26);
            r.setArcWidth(4); r.setArcHeight(4);
            r.setFill(Color.web("#ef4444")); // Red for firewall
            bg = r;
            pathData = SVG_FIREWALL;
            iconColor = Color.WHITE;
        } else if (device.getType().name().startsWith("CAMERA")) {
            bg = new Circle(20, Color.TRANSPARENT);
            pathData = SVG_CAMERA;
            if (device.getType() == DeviceType.CAMERA_DAHUA) iconColor = Color.web("#dc2626"); // Red
            else if (device.getType() == DeviceType.CAMERA_HIKVISION) iconColor = Color.web("#4b5563"); // Dark gray
            else if (device.getType() == DeviceType.CAMERA_UBIQUITI) iconColor = Color.web("#3b82f6"); // Blue
            else iconColor = Color.web("#0d9488"); // Teal
            scale = 1.1;
        } else if (device.getType().name().startsWith("NVR")) {
            Rectangle r = new Rectangle(44, 20);
            r.setArcWidth(2); r.setArcHeight(2);
            r.setFill(Color.web("#111827"));
            bg = r;
            pathData = SVG_NVR;
            iconColor = Color.LIME;
        } else if (device.getType().name().startsWith("IOT_")) {
            bg = new Circle(20, Color.web("#1e293b"));
            iconColor = Color.web("#fde047");
            if (device.getType().name().contains("LIGHT")) pathData = SVG_LIGHT;
            else if (device.getType().name().contains("FAN")) pathData = SVG_FAN;
            else if (device.getType().name().contains("SPEAKER")) pathData = SVG_SPEAKER;
            else if (device.getType().name().contains("LOCK")) pathData = SVG_LOCK;
            else if (device.getType().name().contains("PLUG")) pathData = SVG_PLUG;
            else if (device.getType().name().startsWith("IOT_PLC")) {
                bg = new Rectangle(30, 30, Color.web("#334155"));
                pathData = SVG_INDUSTRIAL;
                iconColor = Color.WHITE;
            } else {
                pathData = SVG_SENSOR;
            }
        } else if (device.getType().name().startsWith("ROUTER")) {
            bg = new Circle(22, Color.web("#3b82f6"));
            pathData = SVG_ROUTER;
            iconColor = Color.WHITE;
        } else if (device.getType().name().startsWith("SWITCH") || device.getType() == DeviceType.HUB) {
            Rectangle r = new Rectangle(44, 26);
            r.setArcWidth(4); r.setArcHeight(4);
            
            if (device.getType() == DeviceType.L3_SWITCH) {
                r.setFill(Color.web("#64748b")); // slightly different gray
                pathData = SVG_L3_SWITCH;
                iconColor = Color.web("#f59e0b"); // yellow/orange diamond
            } else if (device.getType() == DeviceType.HUB) {
                r.setFill(Color.web("#d97706")); // Orange for hub
                pathData = SVG_HUB;
                iconColor = Color.WHITE;
            } else {
                r.setFill(Color.web("#475569"));
                pathData = SVG_SWITCH;
                iconColor = Color.WHITE;
            }
            bg = r;
        } else if (device.getType() == DeviceType.CLOUD) {
            bg = new Circle(26, Color.TRANSPARENT);
            pathData = SVG_CLOUD;
            iconColor = Color.web("#64748b");
        } else {
            // End devices
            bg = new Circle(0, Color.TRANSPARENT); 
            iconColor = Color.web("#64748b");
            
            if (device.getType() == DeviceType.PC) pathData = SVG_PC;
            else if (device.getType() == DeviceType.LAPTOP) pathData = SVG_LAPTOP;
            else if (device.getType() == DeviceType.SERVER) pathData = SVG_SERVER;
            else if (device.getType() == DeviceType.PRINTER) pathData = SVG_PRINTER;
            else if (device.getType() == DeviceType.IP_PHONE) pathData = SVG_PHONE;
            else if (device.getType() == DeviceType.SMARTPHONE) {
                pathData = SVG_SMARTPHONE;
                scale = 1.0;
            }
            else pathData = SVG_PC;
            
            if (device.getType() == DeviceType.PC || device.getType() == DeviceType.SERVER) {
                 scale = 1.2;
            }
        }
        
        SVGPath icon = new SVGPath();
        icon.setContent(pathData);
        icon.setFill(iconColor);
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        
        // For devices without BG, add a hit rect
        if (bg instanceof Circle && ((Circle)bg).getRadius() == 0) {
            Rectangle hit = new Rectangle(40, 40, Color.TRANSPARENT);
            iconContainer.getChildren().addAll(hit, icon);
        } else {
            iconContainer.getChildren().addAll(bg, icon);
        }

        Label nameLabel = new Label();
        nameLabel.textProperty().bind(device.nameProperty());
        nameLabel.getStyleClass().add("device-label");
        
        // Ensure label is readable on dark background
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: rgba(15, 23, 42, 0.8); -fx-background-radius: 4; -fx-padding: 2 6;");

        this.getChildren().addAll(iconContainer, nameLabel);
        
        this.layoutXProperty().bind(device.xProperty());
        this.layoutYProperty().bind(device.yProperty());
    }

    public void setSelected(boolean selected) {
        if (selected) {
            this.setEffect(new DropShadow(15, 0, 0, Color.web("#3b82f6")));
            this.setScaleX(1.05);
            this.setScaleY(1.05);
        } else {
            this.setEffect(null);
            this.setScaleX(1.0);
            this.setScaleY(1.0);
        }
    }

    public void setHighlight(Color color) {
        if (color == null) {
            this.setEffect(null);
        } else {
            this.setEffect(new DropShadow(20, 0, 0, color));
        }
    }
}