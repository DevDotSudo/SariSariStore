package com.magalona.sarisaristore.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Product {

    @DocumentId
    private String id;
    private String name;
    private String category;
    private double unitPrice;
    private int stockQuantity;
    private String barcode;
    private String imageUri;
    private String userId;
    private boolean lowStock; // ← added for Firestore mapping

    @ServerTimestamp
    private Date createdAt;

    // Required empty constructor for Firestore
    public Product() {}

    public Product(String name, String category, double unitPrice, int stockQuantity, String barcode, String imageUri, String userId) {
        this.name = name;
        this.category = category;
        this.unitPrice = unitPrice;
        this.stockQuantity = stockQuantity;
        this.barcode = barcode;
        this.imageUri = imageUri;
        this.userId = userId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    // Computed — not stored in Firestore
    public boolean isLowStock() {
        return stockQuantity < 5;
    }

    // Setter exists so Firestore doesn't throw a warning, but value is ignored
    public void setLowStock(boolean lowStock) {
        // intentionally ignored — computed from stockQuantity
    }
}