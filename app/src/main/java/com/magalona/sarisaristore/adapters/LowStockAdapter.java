package com.magalona.sarisaristore.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.magalona.sarisaristore.databinding.ItemLowStockBinding;
import com.magalona.sarisaristore.models.Product;

import java.util.List;
import java.util.Locale;

public class LowStockAdapter extends RecyclerView.Adapter<LowStockAdapter.ViewHolder> {

    private final List<Product> items;

    public LowStockAdapter(List<Product> items) { this.items = items; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLowStockBinding b = ItemLowStockBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemLowStockBinding b;

        ViewHolder(ItemLowStockBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(Product p) {
            if (p == null) return;
            b.tvLowName.setText(p.getName() != null ? p.getName() : "");
            b.tvLowCategory.setText(p.getCategory() != null ? p.getCategory() : "");
            b.tvLowStock.setText("Stock: " + p.getStockQuantity());
            b.tvLowPrice.setText(String.format(Locale.getDefault(), "₱%.2f", p.getUnitPrice()));
        }
    }
}