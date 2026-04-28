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
import com.magalona.sarisaristore.adapters.SalesHistoryAdapter;
import com.magalona.sarisaristore.database.FirestoreHelper;
import com.magalona.sarisaristore.databinding.FragmentSalesHistoryBinding;
import com.magalona.sarisaristore.models.SaleRecord;
import com.magalona.sarisaristore.utils.DialogHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SalesHistoryFragment extends Fragment {

    private FragmentSalesHistoryBinding binding;
    private SalesHistoryAdapter adapter;
    private final List<SaleRecord> salesList = new ArrayList<>();
    private final FirestoreHelper db = FirestoreHelper.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSalesHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new SalesHistoryAdapter(salesList, this::confirmDeleteSale);
        binding.rvSalesHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSalesHistory.setAdapter(adapter);

        listenSales();
    }

    private void listenSales() {
         db.getSalesHistory()
                 .addSnapshotListener((snap, e) -> {
                     if (e != null || snap == null || binding == null) return;

                     salesList.clear();
                     double grandTotal = 0;

                     for (DocumentSnapshot doc : snap.getDocuments()) {
                         SaleRecord record = doc.toObject(SaleRecord.class);
                         if (record != null) {
                             record.setId(doc.getId());
                             salesList.add(record);
                             grandTotal += record.getTotalAmount();
                         }
                     }

                     if (binding != null) {
                         adapter.notifyDataSetChanged();
                         binding.tvEmpty.setVisibility(salesList.isEmpty() ? View.VISIBLE : View.GONE);
                         binding.tvTotalEarned.setText(
                                 String.format(Locale.getDefault(), "Total Earned: ₱%.2f", grandTotal));
                     }
                 });
     }

    private void confirmDeleteSale(SaleRecord record) {
        DialogHelper.showConfirmation(requireContext(),
            "Delete Sale Record?",
            "Are you sure you want to delete this transaction record? This action cannot be undone.",
            () -> {
                db.deleteSaleRecord(record.getId())
                        .addOnSuccessListener(v -> {
                            if (getContext() != null) {
                                DialogHelper.showSuccess(requireContext(), "Success", "Record deleted successfully", null);
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (getContext() != null) {
                                DialogHelper.showError(requireContext(), "Error", 
                                    "Failed to delete record: " + e.getMessage(), null);
                            }
                        });
            },
            null
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}