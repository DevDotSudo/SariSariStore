package com.magalona.sarisaristore.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.magalona.sarisaristore.R;
import com.magalona.sarisaristore.adapters.SalesHistoryAdapter;
import com.magalona.sarisaristore.database.FirestoreHelper;
import com.magalona.sarisaristore.databinding.FragmentSalesHistoryBinding;
import com.magalona.sarisaristore.models.SaleRecord;
import com.magalona.sarisaristore.utils.DialogHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SalesHistoryFragment extends Fragment {

    private FragmentSalesHistoryBinding binding;
    private SalesHistoryAdapter adapter;
    private final List<SaleRecord> salesList = new ArrayList<>();
    private final List<SaleRecord> filteredList = new ArrayList<>();
    private final FirestoreHelper db = FirestoreHelper.getInstance();
    
    private String searchQuery = "";
    private String filterType = "date_desc"; // date_desc, date_asc, name
    private Calendar selectedDate = null;

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

        adapter = new SalesHistoryAdapter(filteredList, this::confirmDeleteSale);
        binding.rvSalesHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSalesHistory.setAdapter(adapter);

        // Search functionality
        binding.etSearchSales.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

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
                         applyFilters();
                         binding.tvEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
                         binding.tvTotalEarned.setText(
                                 String.format(Locale.getDefault(), "Total Earned: ₱%.2f", grandTotal));
                     }
                 });
     }

    private void applyFilters() {
        filteredList.clear();
        
        for (SaleRecord record : salesList) {
            boolean matchesSearch = true;
            
            // Search by food name
            if (!searchQuery.isEmpty() && record.getItems() != null) {
                matchesSearch = false;
                for (var item : record.getItems()) {
                    String productName = (String) item.get("productName");
                    if (productName != null && productName.toLowerCase().contains(searchQuery.toLowerCase())) {
                        matchesSearch = true;
                        break;
                    }
                }
            }
            
            if (matchesSearch) {
                filteredList.add(record);
            }
        }
        
        // Sort by date descending (newest first)
        Collections.sort(filteredList, (a, b) -> {
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });
        
        adapter.notifyDataSetChanged();
    }

    private void confirmDeleteSale(SaleRecord record) {
        if (record == null || record.getId() == null || getContext() == null) return;
        
        DialogHelper.showConfirmation(requireContext(),
            "Delete Sale Record?",
            "Are you sure you want to delete this transaction record? This action cannot be undone.",
            () -> {
                db.deleteSaleRecord(record.getId())
                        .addOnSuccessListener(v -> {
                            if (getContext() != null && isAdded()) {
                                DialogHelper.showSuccess(requireContext(), "Success", "Record deleted successfully", null);
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (getContext() != null && isAdded()) {
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