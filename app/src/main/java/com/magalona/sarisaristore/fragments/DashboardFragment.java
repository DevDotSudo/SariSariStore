package com.magalona.sarisaristore.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.magalona.sarisaristore.database.FirestoreHelper;
import com.magalona.sarisaristore.databinding.FragmentDashboardBinding;
import com.magalona.sarisaristore.models.Product;
import com.magalona.sarisaristore.models.SaleRecord;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private final FirestoreHelper db = FirestoreHelper.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadDashboardData();
    }

    private void loadDashboardData() {
        // Total Sales & Earnings
        db.getSalesHistory().addSnapshotListener((snap, e) -> {
            if (binding == null || e != null || snap == null) return;
            double totalEarnings = 0;
            int totalSalesCount = snap.size();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                SaleRecord record = doc.toObject(SaleRecord.class);
                if (record != null) {
                    totalEarnings += record.getTotalAmount();
                }
            }
            binding.tvTotalEarnings.setText(String.format(Locale.getDefault(), "₱%.2f", totalEarnings));
            binding.tvTotalSales.setText(String.valueOf(totalSalesCount));
        });

        // Inventory Stats
        db.getProductsCollection().addSnapshotListener((snap, e) -> {
            if (binding == null || e != null || snap == null) return;
            int totalProducts = snap.size();
            int lowStockCount = 0;
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Product p = doc.toObject(Product.class);
                if (p != null && p.getStockQuantity() < FirestoreHelper.LOW_STOCK_THRESHOLD) {
                    lowStockCount++;
                }
            }
            binding.tvTotalProducts.setText(String.valueOf(totalProducts));
            binding.tvLowStockCount.setText(String.valueOf(lowStockCount));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
