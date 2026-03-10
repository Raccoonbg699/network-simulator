package com.networksim.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Email {
    private final StringProperty from = new SimpleStringProperty();
    private final StringProperty to = new SimpleStringProperty();
    private final StringProperty subject = new SimpleStringProperty();
    private final StringProperty body = new SimpleStringProperty();
    private final StringProperty timestamp = new SimpleStringProperty();

    public Email(String from, String to, String subject, String body, String timestamp) {
        this.from.set(from);
        this.to.set(to);
        this.subject.set(subject);
        this.body.set(body);
        this.timestamp.set(timestamp);
    }

    public String getFrom() { return from.get(); }
    public String getTo() { return to.get(); }
    public String getSubject() { return subject.get(); }
    public String getBody() { return body.get(); }
    public String getTimestamp() { return timestamp.get(); }
    
    @Override
    public String toString() {
        return getSubject() + " - " + getFrom();
    }
}
