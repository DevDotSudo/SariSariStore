package com.magalona.sarisaristore.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.magalona.sarisaristore.adapters.ProductAdapter;
import com.magalona.sarisaristore.adapters.SalesHistoryAdapter;
import com.magalona.sarisaristore.database.FirestoreHelper;
import com.magalona.sarisaristore.databinding.FragmentDashboardBinding;
import com.magalona.sarisaristore.models.Product;
import com.magalona.sarisaristore.models.SaleRecord;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private final FirestoreHelper db = FirestoreHelper.getInstance();
    private ProductAdapter recentProductsAdapter;
    private SalesHistoryAdapter recentSalesAdapter;
    private final List<Product> recentProducts = new ArrayList<>();
    private final List<SaleRecord> recentSales = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadUserUsername();
        setupRecyclerViews();
        loadDashboardData();
    }

    private void loadUserUsername() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null) return;
        
        FirestoreHelper.getInstance().getDb().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null) return;
                    String username = documentSnapshot.getString("username");
                    if (username != null && !username.isEmpty()) {
                        binding.tvWelcomeMessage.setText("Welcome, " + username + "!");
                    }
                });
    }

    private void setupRecyclerViews() {
        // Recent Products RecyclerView - only edit on long-click
        recentProductsAdapter = new ProductAdapter(recentProducts, new ProductAdapter.OnProductActionListener() {
            @Override
            public void onAddStock(Product product) {
                // Do nothing on dashboard - read-only view
            }

            @Override
            public void onEditProduct(Product product) {
                // Do nothing on dashboard - read-only view
            }

            @Override
            public void onDeleteProduct(Product product) {
                // Do nothing on dashboard - read-only view
            }
        }, false); // false = hide action buttons
        binding.rvRecentProducts.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvRecentProducts.setAdapter(recentProductsAdapter);
        binding.rvRecentProducts.setHasFixedSize(true);

        // Recent Sales RecyclerView - pass null listener
        recentSalesAdapter = new SalesHistoryAdapter(recentSales, null);
        binding.rvRecentSales.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvRecentSales.setAdapter(recentSalesAdapter);
        binding.rvRecentSales.setHasFixedSize(true);
    }

    private void loadDashboardData() {
        // Total Sales & Earnings
        db.getSalesHistory().addSnapshotListener((snap, e) -> {
            if (binding == null || e != null || snap == null) return;
            double totalEarnings = 0;
            int totalSalesCount = snap.size();
            
            // Clear and reload recent sales
            recentSales.clear();
            List<SaleRecord> allSales = new ArrayList<>();
            
            for (DocumentSnapshot doc : snap.getDocuments()) {
                SaleRecord record = doc.toObject(SaleRecord.class);
                if (record != null) {
                    totalEarnings += record.getTotalAmount();
                    allSales.add(record);
                }
            }
            
            // Sort by timestamp descending and get last 5
            Collections.sort(allSales, (a, b) -> {
                if (a.getTimestamp() == null) return 1;
                if (b.getTimestamp() == null) return -1;
                return b.getTimestamp().compareTo(a.getTimestamp());
            });
            recentSales.addAll(allSales.subList(0, Math.min(5, allSales.size())));
            
            binding.tvTotalEarnings.setText(String.format(Locale.getDefault(), "₱%.2f", totalEarnings));
            binding.tvTotalSales.setText(String.valueOf(totalSalesCount));
            
            // Update recent sales UI
            recentSalesAdapter.notifyDataSetChanged();
            binding.tvEmptySales.setVisibility(recentSales.isEmpty() ? View.VISIBLE : View.GONE);
            binding.rvRecentSales.setVisibility(recentSales.isEmpty() ? View.GONE : View.VISIBLE);
        });

        // Inventory Stats
        db.getProductsCollection().orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (binding == null || e != null || snap == null) return;
                    int totalProducts = snap.size();
                    int lowStockCount = 0;
                    
                    // Clear and reload recent products
                    recentProducts.clear();
                    
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Product p = doc.toObject(Product.class);
                        if (p != null) {
                            if (p.getStockQuantity() < FirestoreHelper.LOW_STOCK_THRESHOLD) {
                                lowStockCount++;
                            }
                            // Get only first 5 products
                            if (recentProducts.size() < 5) {
                                recentProducts.add(p);
                            }
                        }
                    }
                    
                    binding.tvTotalProducts.setText(String.valueOf(totalProducts));
                    binding.tvLowStockCount.setText(String.valueOf(lowStockCount));
                    
                    // Update recent products UI
                    recentProductsAdapter.notifyDataSetChanged();
                    binding.tvEmptyProducts.setVisibility(recentProducts.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.rvRecentProducts.setVisibility(recentProducts.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
