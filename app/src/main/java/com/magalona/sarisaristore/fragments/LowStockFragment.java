package com.magalona.sarisaristore.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.DocumentSnapshot;
import com.magalona.sarisaristore.adapters.LowStockAdapter;
import com.magalona.sarisaristore.database.FirestoreHelper;
import com.magalona.sarisaristore.databinding.FragmentLowStockBinding;
import com.magalona.sarisaristore.models.Product;

import java.util.ArrayList;
import java.util.List;

public class LowStockFragment extends Fragment {

    private FragmentLowStockBinding binding;
    private LowStockAdapter adapter;
    private final List<Product> lowStockList = new ArrayList<>();
    private final FirestoreHelper db = FirestoreHelper.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLowStockBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new LowStockAdapter(lowStockList);
        binding.rvLowStock.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvLowStock.setAdapter(adapter);

        listenLowStock();
    }

    private void listenLowStock() {
         db.getLowStockProducts()
                 .addSnapshotListener((snap, e) -> {
                     if (e != null || snap == null || binding == null) return;

                     lowStockList.clear();
                     for (DocumentSnapshot doc : snap.getDocuments()) {
                         Product p = doc.toObject(Product.class);
                         if (p != null) lowStockList.add(p);
                     }

                     if (binding != null) {
                         adapter.notifyDataSetChanged();
                         binding.tvEmpty.setVisibility(lowStockList.isEmpty() ? View.VISIBLE : View.GONE);
                         binding.tvCount.setText("Items below threshold: " + lowStockList.size());
                     }
                 });
     }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}