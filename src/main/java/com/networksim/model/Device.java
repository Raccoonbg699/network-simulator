package com.networksim.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Device {
    private final String id;
    private final ObjectProperty<DeviceType> type = new SimpleObjectProperty<>();
    private final StringProperty name = new SimpleStringProperty();
    private final DoubleProperty x = new SimpleDoubleProperty();
    private final DoubleProperty y = new SimpleDoubleProperty();
    
    // Config properties
    private final StringProperty ip = new SimpleStringProperty("");
    private final StringProperty hostname = new SimpleStringProperty("");
    private final StringProperty gateway = new SimpleStringProperty("0.0.0.0");
    
    // Physical State
    private final BooleanProperty poweredOn = new SimpleBooleanProperty(true); // Default ON
    private long bootTime = System.currentTimeMillis();
    private final StringProperty installedModule = new SimpleStringProperty(null); 
    
    private final ObservableList<String> cliHistory = FXCollections.observableArrayList();
    private final ObservableList<RouteEntry> routingTable = FXCollections.observableArrayList();
    private final ObservableList<Email> emails = FXCollections.observableArrayList();
    private final ObservableList<String> assignedCameraIds = FXCollections.observableArrayList();

    // Email Config
    private final StringProperty emailUser = new SimpleStringProperty("");
    private final StringProperty emailPass = new SimpleStringProperty("");
    private final StringProperty emailServer = new SimpleStringProperty("");

    public Device(String id, DeviceType type, String name, double x, double y) {
        this.id = id;
        this.type.set(type);
        this.name.set(name);
        this.x.set(x);
        this.y.set(y);
        
        // Update boot time when powered on
        this.poweredOn.addListener((obs, oldV, newV) -> {
            if (newV) bootTime = System.currentTimeMillis();
        });

        // Initial CLI content
        cliHistory.add("Cisco IOS Software, C2900 Software (C2900-UNIVERSALK9-M)");
        cliHistory.add("Press RETURN to get started!");
    }

    public ObservableList<Email> getEmails() { return emails; }
    public StringProperty emailUserProperty() { return emailUser; }
    public StringProperty emailPassProperty() { return emailPass; }
    public StringProperty emailServerProperty() { return emailServer; }
    
    public String getUptimeString() {
        if (!isPoweredOn()) return "00:00:00";
        long diff = (System.currentTimeMillis() - bootTime) / 1000;
        long h = diff / 3600;
        long m = (diff % 3600) / 60;
        long s = diff % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
    
    public String getId() { return id; }
    public DeviceType getType() { return type.get(); }
    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }
    public DoubleProperty xProperty() { return x; }
    public void setX(double x) { this.x.set(x); }
    public DoubleProperty yProperty() { return y; }
    public void setY(double y) { this.y.set(y); }
    
    public StringProperty ipProperty() { return ip; }
    public String getIp() { return ip.get(); }
    
    public StringProperty gatewayProperty() { return gateway; }
    public String getGateway() { return gateway.get(); }
    
    public StringProperty hostnameProperty() { return hostname; }
    public String getHostname() { return hostname.get(); }
    
    public BooleanProperty poweredOnProperty() { return poweredOn; }
    public boolean isPoweredOn() { return poweredOn.get(); }
    public void setPoweredOn(boolean on) { this.poweredOn.set(on); }
    
    public StringProperty installedModuleProperty() { return installedModule; }
    public void setInstalledModule(String mod) { this.installedModule.set(mod); }
    
    public ObservableList<String> getCliHistory() { return cliHistory; }
    
    public ObservableList<RouteEntry> getRoutingTable() { return routingTable; }
    public ObservableList<String> getAssignedCameraIds() { return assignedCameraIds; }
}