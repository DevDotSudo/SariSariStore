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

    @ServerTimestamp
    private Date createdAt;

    // Required empty constructor for Firestore
    public Product() {}

    public Product(String name, String category, double unitPrice, int stockQuantity, String barcode, String imageUri) {
        this.name = name;
        this.category = category;
        this.unitPrice = unitPrice;
        this.stockQuantity = stockQuantity;
        this.barcode = barcode;
        this.imageUri = imageUri;
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

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public boolean isLowStock() {
        return stockQuantity < 5;
    }
}