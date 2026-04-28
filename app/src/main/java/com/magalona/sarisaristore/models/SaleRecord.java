package com.magalona.sarisaristore.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class SaleRecord {

    @DocumentId
    private String id;
    private List<Map<String, Object>> items;
    private double totalAmount;

    @ServerTimestamp
    private Date timestamp;

    // Required empty constructor for Firestore
    public SaleRecord() {}

    public SaleRecord(List<Map<String, Object>> items, double totalAmount) {
        this.items = items;
        this.totalAmount = totalAmount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<Map<String, Object>> getItems() { return items; }
    public void setItems(List<Map<String, Object>> items) { this.items = items; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}