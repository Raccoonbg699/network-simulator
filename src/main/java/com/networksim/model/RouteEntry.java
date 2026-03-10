package com.networksim.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class RouteEntry {
    private final StringProperty destination = new SimpleStringProperty();
    private final StringProperty nextHop = new SimpleStringProperty();
    private final IntegerProperty metric = new SimpleIntegerProperty();
    private final StringProperty interfaceName = new SimpleStringProperty(); // e.g. "Fa0/0"

    public RouteEntry(String destination, String nextHop, int metric, String interfaceName) {
        this.destination.set(destination);
        this.nextHop.set(nextHop);
        this.metric.set(metric);
        this.interfaceName.set(interfaceName);
    }

    public StringProperty destinationProperty() { return destination; }
    public String getDestination() { return destination.get(); }

    public StringProperty nextHopProperty() { return nextHop; }
    public String getNextHop() { return nextHop.get(); }

    public IntegerProperty metricProperty() { return metric; }
    public int getMetric() { return metric.get(); }

    public StringProperty interfaceNameProperty() { return interfaceName; }
    public String getInterfaceName() { return interfaceName.get(); }
}
