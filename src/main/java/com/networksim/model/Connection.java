package com.networksim.model;

public class Connection {
    private final String id;
    private final Device from;
    private final Device to;
    private final String type; // "copper", "fiber", etc.
    private final javafx.beans.property.IntegerProperty weight = new javafx.beans.property.SimpleIntegerProperty(1);
    private final javafx.beans.property.BooleanProperty operational = new javafx.beans.property.SimpleBooleanProperty(true); // Default UP

    public Connection(String id, Device from, Device to, String type) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.type = type;
    }

    public javafx.beans.property.BooleanProperty operationalProperty() { return operational; }
    public boolean isOperational() { return operational.get(); }
    public void setOperational(boolean op) { this.operational.set(op); }

    public String getId() { return id; }
    public Device getFrom() { return from; }
    public Device getTo() { return to; }
    public String getType() { return type; }
    
    public javafx.beans.property.IntegerProperty weightProperty() { return weight; }
    public int getWeight() { return weight.get(); }
    public void setWeight(int weight) { this.weight.set(weight); }
}
