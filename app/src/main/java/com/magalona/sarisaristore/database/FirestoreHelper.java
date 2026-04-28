package com.magalona.sarisaristore.database;

import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.magalona.sarisaristore.models.CartItem;
import com.magalona.sarisaristore.models.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreHelper {

    private static final String TAG = "FirestoreHelper";
    private static FirestoreHelper instance;
    private final FirebaseFirestore db;

    public static final String COLLECTION_PRODUCTS = "products";
    public static final String COLLECTION_SALES    = "sales_history";
    public static final int    LOW_STOCK_THRESHOLD = 5;

    private FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public static FirestoreHelper getInstance() {
        if (instance == null) {
            instance = new FirestoreHelper();
        }
        return instance;
    }

    public FirebaseFirestore getDb() { return db; }

    // ── Products ──────────────────────────────────────────────────────────────

    public CollectionReference getProductsCollection() {
        return db.collection(COLLECTION_PRODUCTS);
    }

    public Task<DocumentReference> addProduct(Product product) {
        return db.collection(COLLECTION_PRODUCTS).add(product);
    }

    public Task<Void> updateProduct(String productId, Product product) {
        if (productId == null) return Tasks.forException(new IllegalArgumentException("Product ID cannot be null"));
        return db.collection(COLLECTION_PRODUCTS).document(productId).set(product);
    }

    public Task<Void> updateProductFields(String productId, Map<String, Object> updates) {
        if (productId == null) return Tasks.forException(new IllegalArgumentException("Product ID cannot be null"));
        return db.collection(COLLECTION_PRODUCTS).document(productId).update(updates);
    }

    public Task<Void> deleteProduct(String productId) {
        if (productId == null) return Tasks.forException(new IllegalArgumentException("Product ID cannot be null"));
        return db.collection(COLLECTION_PRODUCTS).document(productId).delete();
    }

    public Task<Void> incrementStock(String productId, int addAmount) {
        if (productId == null) return Tasks.forException(new IllegalArgumentException("Product ID cannot be null"));
        return db.collection(COLLECTION_PRODUCTS)
                .document(productId)
                .update("stockQuantity", FieldValue.increment(addAmount));
    }

    public Query getProductByBarcode(String barcode) {
        return db.collection(COLLECTION_PRODUCTS)
                .whereEqualTo("barcode", barcode)
                .limit(1);
    }

    public Query getLowStockProducts() {
        return db.collection(COLLECTION_PRODUCTS)
                .whereLessThan("stockQuantity", LOW_STOCK_THRESHOLD);
    }

    // ── Sales ─────────────────────────────────────────────────────────────────

    public CollectionReference getSalesCollection() {
        return db.collection(COLLECTION_SALES);
    }

    public Task<Void> checkout(List<CartItem> cartItems, double total) {
        if (cartItems == null || cartItems.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Cart is empty"));
        }

        WriteBatch batch = db.batch();
        List<Map<String, Object>> saleItems = new ArrayList<>();

        try {
            for (CartItem item : cartItems) {
                if (item.getProduct() == null || item.getProduct().getId() == null) {
                    Log.e(TAG, "Checkout failed: Product or Product ID is null");
                    continue;
                }

                // Deduct stock
                DocumentReference productRef = db.collection(COLLECTION_PRODUCTS)
                        .document(item.getProduct().getId());
                batch.update(productRef, "stockQuantity",
                        FieldValue.increment(-item.getQuantity()));

                // Build line-item map
                Map<String, Object> line = new HashMap<>();
                line.put("productId",   item.getProduct().getId());
                line.put("productName", item.getProduct().getName());
                line.put("unitPrice",   item.getProduct().getUnitPrice());
                line.put("quantity",    item.getQuantity());
                line.put("subtotal",    item.getSubtotal());
                saleItems.add(line);
            }

            if (saleItems.isEmpty()) {
                return Tasks.forException(new Exception("No valid items in cart to checkout"));
            }

            // Sale record document
            Map<String, Object> saleRecord = new HashMap<>();
            saleRecord.put("items",       saleItems);
            saleRecord.put("totalAmount", total);
            saleRecord.put("timestamp",   FieldValue.serverTimestamp());

            DocumentReference saleRef = db.collection(COLLECTION_SALES).document();
            batch.set(saleRef, saleRecord);

            return batch.commit();
        } catch (Exception e) {
            Log.e(TAG, "Error building checkout batch", e);
            return Tasks.forException(e);
        }
    }

    public Task<Void> deleteSaleRecord(String saleId) {
        if (saleId == null) return Tasks.forException(new IllegalArgumentException("Sale ID cannot be null"));
        return db.collection(COLLECTION_SALES).document(saleId).delete();
    }

    public Query getSalesHistory() {
        return db.collection(COLLECTION_SALES)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100);
    }
}